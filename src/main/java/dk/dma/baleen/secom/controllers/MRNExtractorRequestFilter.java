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
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

import org.grad.secomv2.core.models.ResponseObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import dk.dma.baleen.secom.config.SecomAuthenticationProperties;
import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Extracts the MRN from the header of the request.
 * <p>
 * The identity this produces is <em>unverified</em>. The {@code X-Secom-Cert} header is whatever the reverse
 * proxy forwarded, and all that happens to it here is a base64 decode and a read of the subject UID: no
 * certificate chain is validated, no validity period is checked and no truststore is consulted. The MRN is
 * therefore a claim, not proof, which is why it is published together with
 * {@link #MRN_VERIFIED_ATTRIBUTE} and ends up in a {@link SecomNode} that says so.
 * <p>
 * A caller with no usable certificate is either served anonymously or refused with 401 Unauthorized, depending
 * on {@link SecomAuthenticationProperties#allowAnonymous()}.
 */
@Provider
@Component
@Priority(Priorities.AUTHORIZATION)
public class MRNExtractorRequestFilter implements ContainerRequestFilter {

    /** Request attribute holding the MRN the caller claimed, or absent when the caller is anonymous. */
    public static final String MRN_ATTRIBUTE = "X-MRN";

    /**
     * Request attribute holding whether {@link #MRN_ATTRIBUTE} was actually verified. Always {@code false}
     * today - nothing in Baleen validates a client certificate yet - but the SECOM endpoints read it rather
     * than assuming, so that turning verification on is a change to this filter alone.
     */
    public static final String MRN_VERIFIED_ATTRIBUTE = "X-MRN-Verified";

    /** The header the reverse proxy forwards the DER encoded client certificate in. */
    static final String CERTIFICATE_HEADER = "X-Secom-Cert";

    /** What Caddy forwards verbatim when its client certificate placeholder is not configured. */
    static final String CADDY_UNCONFIGURED_PLACEHOLDER = "{http.request.tls.client.certificate_der_base64}";

    /** The logger of this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MRNExtractorRequestFilter.class);

    @Context
    HttpServletRequest req;

    private final SecomAuthenticationProperties authentication;

    /** Keeps the anonymous-caller message out of the log on every single request. */
    private final AtomicBoolean anonymousReported = new AtomicBoolean();

    /** The same for the misconfigured proxy, which is a different problem with a different fix. */
    private final AtomicBoolean proxyMisconfigurationReported = new AtomicBoolean();

    public MRNExtractorRequestFilter(SecomAuthenticationProperties authentication) {
        this.authentication = authentication;
    }

    static X509Certificate convertToX509Certificate(String base64Cert) throws Exception {
        // Remove any extra whitespaces or newlines from the Base64 certificate
        base64Cert = base64Cert.replaceAll("\\s+", "");

        // Decode the Base64 encoded string to get the DER-encoded bytes
        byte[] decodedBytes = Base64.getDecoder().decode(base64Cert);

        // Create a CertificateFactory for X.509 certificates
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

        // Convert the DER-encoded bytes into an X509Certificate object
        X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(decodedBytes));

        return certificate;
    }

    static String extractUIDFromCertificate(X509Certificate certificate) {
        String subjectDN = certificate.getSubjectX500Principal().getName();
        // Use a regex or a DN parser to extract the UID
        String[] dnComponents = subjectDN.split(",");
        for (String component : dnComponents) {
            component = component.trim();
            if (component.startsWith("UID=")) {
                return component.substring(4);
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String headerString = requestContext.getHeaderString(CERTIFICATE_HEADER);

        if (CADDY_UNCONFIGURED_PLACEHOLDER.equals(headerString)) {
            // The proxy is misconfigured - the client may well have presented a certificate that was dropped
            // on the way here - so this is not the same finding as a caller that sent nothing.
            reportOnce(proxyMisconfigurationReported, true,
                    "The reverse proxy forwarded the literal placeholder '{}' as {}, so it is not passing on the client "
                            + "certificate at all. Check the Caddy tls client certificate configuration.",
                    CADDY_UNCONFIGURED_PLACEHOLDER, CERTIFICATE_HEADER);
            handleUnidentifiedCaller(requestContext);
            return;
        }

        if (headerString == null || headerString.isBlank()) {
            handleUnidentifiedCaller(requestContext);
            return;
        }

        String mrn;
        try {
            mrn = extractUIDFromCertificate(convertToX509Certificate(headerString));
        } catch (Exception e) {
            LOGGER.warn("Could not read a client certificate out of the {} header, treating the caller as unidentified", CERTIFICATE_HEADER, e);
            handleUnidentifiedCaller(requestContext);
            return;
        }

        if (mrn == null) {
            LOGGER.warn("The client certificate in {} carries no subject UID, treating the caller as unidentified", CERTIFICATE_HEADER);
            handleUnidentifiedCaller(requestContext);
            return;
        }

        // A claim, not proof: see the class javadoc for what is - and is not - checked before we get here.
        req.setAttribute(MRN_ATTRIBUTE, mrn);
        req.setAttribute(MRN_VERIFIED_ATTRIBUTE, Boolean.FALSE);
    }

    /**
     * Handles a request that carries no usable client certificate: it is either served as an anonymous caller
     * or refused, depending on {@code baleen.secom.allow-anonymous}.
     */
    private void handleUnidentifiedCaller(ContainerRequestContext requestContext) {
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();

        if (authentication.allowAnonymous()) {
            reportOnce(anonymousReported, false,
                    "Serving {} {} as an anonymous SECOM caller: no usable client certificate was presented and "
                            + "baleen.secom.allow-anonymous is true, so callers are not authenticated. Reported once, "
                            + "later anonymous requests are logged at debug level.",
                    method, path);
            return;
        }

        // TODO when the truststore work lands, this path must also verify the certificate that IS presented:
        // PkiUtils.verifyCertificateChain(chain, MCPSecurityService.trustStore()) plus X509Certificate.checkValidity(),
        // and refuse the caller when either fails. Until then an MRN read out of the header is still trusted as-is.
        LOGGER.warn("Refusing {} {}: no usable client certificate was presented and baleen.secom.allow-anonymous is false", method, path);
        ResponseObject responseObject = new ResponseObject();
        responseObject.setMessage("Not authenticated, a client certificate is required");
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON).entity(responseObject).build());
    }

    /**
     * Logs {@code message} the first time it happens, and at debug level every time after that, so that a
     * standing condition does not fill the log with one line per request.
     *
     * @param reported the flag tracking whether this message has been logged before
     * @param warning whether the first occurrence is a warning rather than merely informational
     * @param message the message to log
     * @param arguments the arguments of the message
     */
    private static void reportOnce(AtomicBoolean reported, boolean warning, String message, Object... arguments) {
        if (!reported.compareAndSet(false, true)) {
            LOGGER.debug(message, arguments);
        } else if (warning) {
            LOGGER.warn(message, arguments);
        } else {
            LOGGER.info(message, arguments);
        }
    }
}
