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

import org.grad.secomv2.core.base.SecomConstants;
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
 * Reports the root certificate thumbprint a caller sent when it is not the one we would accept.
 * <p>
 * {@code SecomSignatureFilter.checkCertificate} compares the caller's {@code envelopeRootCertificateThumbprint}
 * against exactly one certificate - whichever {@link dk.dma.baleen.secom.security.BaleenTrustStoreProvider}
 * names - and on a mismatch answers only "The provided SECOM CA root certificate is not recognised". It never
 * says which thumbprint arrived, so there is no way to tell a caller from a different PKI apart from one that
 * simply anchors on a different certificate of the same chain - MCP issues through an intermediate under
 * {@code CN=MCP Root Certificate}, and either can reasonably be called the root.
 * <p>
 * Without that value a rejection cannot be acted on: we cannot know whether to add a trust anchor, change which
 * one we compare against, or leave the caller refused. So we log it.
 * <p>
 * This is diagnostics only. It runs before {@code SecomSignatureFilter} - both are {@link PreMatching}, and that
 * one takes the default {@link Priorities#USER} - and it rejects nothing, changes nothing and swallows every
 * failure of its own. A request that this class cannot make sense of is left exactly as it found it.
 */
@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
@Component
public class RootCertificateThumbprintLogger implements ContainerRequestFilter {

    /** The logger of this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(RootCertificateThumbprintLogger.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final MCPSecurityService pki;

    public RootCertificateThumbprintLogger(MCPSecurityService pki) {
        this.pki = pki;
    }

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!"POST".equals(requestContext.getMethod())) {
            return; // only a signed body carries a thumbprint
        }
        byte[] body = readAndReplaceBody(requestContext);
        if (body == null || body.length == 0) {
            return;
        }
        try {
            JsonNode envelope = MAPPER.readTree(body).path("envelope");
            String received = envelope.path("envelopeRootCertificateThumbprint").asText(null);
            if (received == null || received.isBlank()) {
                return; // SECOM makes the thumbprint optional, and an absent one is not a mismatch
            }
            String expected = SecomPemUtils.getCertThumbprint(pki.mcpRootCertificate(), SecomConstants.CERTIFICATE_THUMBPRINT_HASH);
            if (!received.equalsIgnoreCase(expected)) {
                LOGGER.warn(
                        "Rejecting {}: it presented root certificate thumbprint {}, and we only accept {} ({}). "
                                + "Add that CA to the truststore, or point BaleenTrustStoreProvider at it, to accept this caller.",
                        requestContext.getUriInfo().getPath(), received, expected,
                        pki.mcpRootCertificate().getSubjectX500Principal());
            }
        } catch (Exception e) {
            // Diagnostics must never be the reason a request fails, and a body we cannot read is exactly the
            // case where SecomSignatureFilter is about to produce the better error anyway.
            LOGGER.debug("Could not read a root certificate thumbprint out of the request body", e);
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
