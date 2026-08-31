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
package dk.dma.baleen.secom.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Guards the bundled truststore against the two ways it has silently rotted before.
 * <p>
 * A trust anchor expiring is invisible until a peer is refused in production: the MCP Identity Registry CA expired
 * in November 2025 and every current MCP client was rejected with "the provided SECOM CA root certificate is not
 * recognised", which reads like a misbehaving client rather than our own stale anchor. Three server certificates
 * pinned alongside it had already been expired for up to a year without anyone noticing.
 * <p>
 * The other failure is pinning a leaf. A server certificate is not a CA, so it only ever matches the one host it
 * was issued for, and it stops matching the moment that host rotates - which is what makes the expiry invisible.
 * Trust the issuing CA instead.
 * <p>
 * The truststore is also the anchor set {@code PkiUtils.verifyCertificateChain} validates SECOM payload
 * certificates against, not merely outbound TLS trust, so every entry added here widens what Baleen will accept as
 * a signer. Keep it small.
 */
class TrustStoreHygieneTest {

    private static final String TRUST_STORE = "secom/truststore.p12";

    private static final String TRUST_STORE_PASSWORD = "changeit";

    /** How much warning we want before an anchor expires, so there is time to source a replacement. */
    private static final Duration RENEWAL_HEADROOM = Duration.ofDays(90);

    private static KeyStore trustStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = TrustStoreHygieneTest.class.getClassLoader().getResourceAsStream(TRUST_STORE)) {
            assertThat(in).as("truststore %s is missing from the classpath", TRUST_STORE).isNotNull();
            keyStore.load(in, TRUST_STORE_PASSWORD.toCharArray());
        }
        return keyStore;
    }

    private static List<String> aliases() throws Exception {
        return Collections.list(trustStore().aliases());
    }

    @Test
    void everyAnchorIsCurrentAndHasRoomToRenew() throws Exception {
        KeyStore trustStore = trustStore();
        Instant deadline = Instant.now().plus(RENEWAL_HEADROOM);

        List<String> expiring = new ArrayList<>();
        for (String alias : aliases()) {
            X509Certificate certificate = (X509Certificate) trustStore.getCertificate(alias);
            if (certificate.getNotAfter().toInstant().isBefore(deadline)) {
                expiring.add(alias + " expires " + certificate.getNotAfter().toInstant());
            }
        }

        assertThat(expiring)
                .as("trust anchors expiring within %s. Source a replacement from the issuer - do not simply delete "
                        + "the entry, or peers chaining to it stop being trusted", RENEWAL_HEADROOM)
                .isEmpty();
    }

    @Test
    void everyAnchorIsACertificateAuthority() throws Exception {
        KeyStore trustStore = trustStore();

        List<String> leaves = new ArrayList<>();
        for (String alias : aliases()) {
            X509Certificate certificate = (X509Certificate) trustStore.getCertificate(alias);
            // getBasicConstraints() returns -1 for anything that is not a CA, and the path length otherwise.
            if (certificate.getBasicConstraints() == -1) {
                leaves.add(alias + " (" + certificate.getSubjectX500Principal().getName() + ")");
            }
        }

        assertThat(leaves)
                .as("server certificates pinned as trust anchors. Trust the issuing CA instead: a leaf stops "
                        + "matching as soon as the host rotates, which hides its own expiry")
                .isEmpty();
    }

    @Test
    void theMcpAnchorIsPresentAndIsTheOneAdvertisedToPeers() throws Exception {
        KeyStore trustStore = trustStore();

        List<X509Certificate> mcpAnchors = new ArrayList<>();
        for (String alias : aliases()) {
            X509Certificate certificate = (X509Certificate) trustStore.getCertificate(alias);
            if (certificate.getSubjectX500Principal().getName().contains("CN=MCP Identity Registry")) {
                mcpAnchors.add(certificate);
            }
        }

        // Baleen publishes this certificate's thumbprint in every SECOM response and exchange set, so replacing it
        // changes what peers have to be configured with. It is the current registry CA, valid until 2030-09-15.
        assertThat(mcpAnchors).as("the MCP Identity Registry CA must be a trust anchor").hasSize(1);
        assertThat(mcpAnchors.get(0).getSubjectX500Principal().getName())
                .as("the anchor must be the rotated registry CA, not the one retired in November 2025")
                .contains("UID=urn:mrn:mcp:ca:mcc:mcp-idreg-new");
    }

    @Test
    void theAdvertisedAliasStillHoldsTheCertificatePeersAreConfiguredWith() throws Exception {
        // MCPSecurityService reads the certificate it advertises out of this one alias. Adding anchors must never
        // rebind it - a peer compares the thumbprint it was given against the one we send, so moving this alias to
        // the root would silently refuse every peer that is correctly configured today.
        X509Certificate advertised = (X509Certificate) trustStore().getCertificate("mcp identity registry (mcp root certificate)");

        assertThat(advertised).as("the alias MCPSecurityService advertises from must exist").isNotNull();
        assertThat(thumbprint(advertised, "SHA-256"))
                .as("the SHA-256 thumbprint Baleen publishes to every SECOM peer")
                .isEqualTo("45c34d53a13cff3338f6472502965c59a4ae16bd436daef8790357a53f628ac4");
    }

    /** {@return the hex thumbprint of the certificate's DER encoding under the given hash} */
    private static String thumbprint(X509Certificate certificate, String algorithm) throws Exception {
        byte[] digest = java.security.MessageDigest.getInstance(algorithm).digest(certificate.getEncoded());
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append("%02x".formatted(b));
        }
        return sb.toString();
    }
}
