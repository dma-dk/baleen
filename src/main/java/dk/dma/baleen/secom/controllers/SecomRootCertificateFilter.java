/*
 * Copyright (c) 2024 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dk.dma.baleen.secom.controllers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.Set;

import org.grad.secomv2.core.exceptions.SecomInvalidCertificateException;
import org.grad.secomv2.core.utils.PkiUtils;
import org.grad.secomv2.core.utils.SecomPemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.dma.baleen.secom.security.MCPSecurityService;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;

/**
 * Decides whether the certificate a signed SECOM request arrives with is one Baleen trusts.
 * <p>
 * This is the check {@code SecomSignatureFilter.checkCertificate} would otherwise do. It is done here instead
 * because the library's version compares the caller's {@code envelopeRootCertificateThumbprint} against exactly one
 * certificate under exactly one hash - {@code SecomConstants.CERTIFICATE_THUMBPRINT_HASH}, hardcoded to SHA-256 -
 * and so refuses callers that name the same MCP certification authority in another perfectly correct way. The
 * observed case is {@code urn:mrn:mcp:device:mcc:amsa:s124-secom-client:prod} on {@code /v2/object/search}, which
 * sends the SHA-384 of {@code CN=MCP Root Certificate}: the right PKI, named the way MCP's own documentation hands
 * it out, and unmatchable by a SHA-256 comparison against the intermediate.
 * <p>
 * The library check is disabled by leaving no {@code SecomTrustStoreProvider} bean for it to use - see
 * {@code LoadBaleen} - because {@code SecomSignatureFilter} skips {@code checkCertificate} entirely when that
 * provider is absent, and keeps validating signatures. Nothing else in the library consumes the provider. This
 * filter is {@link PreMatching} at {@link Priorities#AUTHENTICATION}, so it runs before the signature filter's
 * {@code USER} priority and refuses an untrusted certificate before any signature work is done.
 * <p>
 * What is checked is what the library checked, less the thumbprint's rigidity: the declared root must be one of
 * {@link MCPSecurityService#acceptableRootThumbprints()}, every certificate presented must be inside its validity
 * period, and the chain must build to a trust anchor. The thumbprint stays optional, as SECOM specifies and as the
 * library treated it.
 */
@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
@Component
public class SecomRootCertificateFilter implements ContainerRequestFilter {

    /** The logger of this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SecomRootCertificateFilter.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final MCPSecurityService pki;

    /** The thumbprints that name a CA we trust, computed once because the truststore does not change. */
    private final Set<String> acceptableRootThumbprints;

    public SecomRootCertificateFilter(MCPSecurityService pki) {
        this.pki = pki;
        this.acceptableRootThumbprints = pki.acceptableRootThumbprints();
        LOGGER.info("SECOM callers may name a trusted CA by any of {} thumbprints, being every supported hash of {}",
                acceptableRootThumbprints.size(),
                pki.mcpTrustAnchors().stream().map(c -> c.getSubjectX500Principal().getName()).toList());
    }

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!"POST".equals(requestContext.getMethod())) {
            return; // only a signed body carries a certificate
        }
        byte[] body = readAndReplaceBody(requestContext);
        if (body == null || body.length == 0) {
            return;
        }

        JsonNode envelope;
        try {
            envelope = MAPPER.readTree(body).path("envelope");
        } catch (Exception e) {
            // Not a SECOM envelope, or not JSON at all. Refusing it here would turn a body the resource itself
            // would reject with a precise message into an opaque certificate error.
            LOGGER.debug("Could not read an envelope out of the body of {}", requestContext.getUriInfo().getPath(), e);
            return;
        }
        if (envelope.isMissingNode()) {
            return;
        }

        checkDeclaredRoot(requestContext, envelope);
        checkPresentedChain(requestContext, envelope);
    }

    /**
     * Refuses a caller that declares a certification authority we do not trust.
     * <p>
     * The thumbprint is optional in SECOM, so an absent one is not a mismatch - it just leaves the caller's claim
     * about its root unstated, and the chain check below is what actually establishes trust.
     */
    private void checkDeclaredRoot(ContainerRequestContext requestContext, JsonNode envelope) {
        String declared = envelope.path("envelopeRootCertificateThumbprint").asText(null);
        if (declared == null || declared.isBlank()) {
            return;
        }
        if (acceptableRootThumbprints.contains(declared.toLowerCase())) {
            LOGGER.debug("{} named a trusted CA by thumbprint {}", requestContext.getUriInfo().getPath(), declared);
            return;
        }
        LOGGER.warn("Refusing {}: it named root certificate thumbprint {}, which is none of the {} thumbprints of "
                + "our MCP trust anchors. If this caller should be trusted, its CA has to be added to the truststore.",
                requestContext.getUriInfo().getPath(), declared, acceptableRootThumbprints.size());
        throw new SecomInvalidCertificateException("The provided SECOM CA root certificate is not recognised");
    }

    /** Refuses a caller whose own certificate has expired, or does not chain to a trust anchor. */
    private void checkPresentedChain(ContainerRequestContext requestContext, JsonNode envelope) {
        JsonNode certificates = envelope.path("envelopeSignatureCertificate");
        if (!certificates.isArray() || certificates.isEmpty()) {
            return; // nothing presented; the signature check is about to fail on its own
        }
        String[] pems = new String[certificates.size()];
        for (int i = 0; i < pems.length; i++) {
            pems[i] = certificates.get(i).asText();
        }

        try {
            X509Certificate[] chain = SecomPemUtils.getCertsFromPem(pems);
            for (X509Certificate certificate : chain) {
                certificate.checkValidity();
            }
            if (!PkiUtils.verifyCertificateChain(chain, pki.trustStore())) {
                throw new SecomInvalidCertificateException("Failed to verify the certificate chain...");
            }
        } catch (SecomInvalidCertificateException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.warn("Refusing {}: the certificate it presented did not validate against our truststore - {}",
                    requestContext.getUriInfo().getPath(), e.getMessage());
            throw new SecomInvalidCertificateException(e.getMessage());
        }
    }

    /**
     * {@return the request body, with the entity stream replaced so the filters after this one still see it}
     * <p>
     * Reading the body consumes the stream, so it is put back as it was read. Returns null if it could not be
     * read at all, which leaves the request untouched.
     */
    private static byte[] readAndReplaceBody(ContainerRequestContext requestContext) {
        try {
            InputStream in = requestContext.getEntityStream();
            byte[] body = in.readAllBytes();
            requestContext.setEntityStream(new ByteArrayInputStream(body));
            return body;
        } catch (IOException e) {
            LOGGER.debug("Could not read the request body of {}", requestContext.getUriInfo().getPath(), e);
            return null;
        }
    }
}
