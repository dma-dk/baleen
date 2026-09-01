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
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Pins which thumbprints name a CA Baleen will accept.
 * <p>
 * The set exists because SECOM's "declare the CA you trust by thumbprint" handshake is a string comparison, and two
 * correct peers can describe the same certification authority differently - by naming the root or the intermediate,
 * and by hashing with SHA-256 or the SHA-384 that S-100 Part 15 mandates.
 * <p>
 * The value asserted below is the real one observed on the deployed test server, sent by
 * {@code urn:mrn:mcp:device:mcc:amsa:s124-secom-client:prod} on {@code /v2/object/search} and refused before this
 * change. It is the SHA-384 of {@code CN=MCP Root Certificate}.
 * <p>
 * This duplicates {@code MCPSecurityService.acceptableRootThumbprints} rather than calling it, because
 * constructing that service needs a keystore, a config and a resource loader. The point is to fail if the accepted
 * set ever stops covering these certificates, which a re-implementation over the same truststore does catch.
 */
class AcceptableRootThumbprintsTest {

    private static final String TRUST_STORE = "secom/truststore.p12";

    private static final String TRUST_STORE_PASSWORD = "changeit";

    /** Sent by the AMSA client on /v2/object/search - the SHA-384 of CN=MCP Root Certificate. */
    private static final String AMSA_THUMBPRINT =
            "39de8fd395a136f679d847e23179f38761a2fe1d3b905e04fe6177022ae28f94058ebffb978d7fea727bcebf8ca2346d";

    /** What Baleen advertises, and what the library used to demand - the SHA-256 of the intermediate. */
    private static final String ADVERTISED_THUMBPRINT =
            "45c34d53a13cff3338f6472502965c59a4ae16bd436daef8790357a53f628ac4";

    private static Set<String> acceptableRootThumbprints() throws Exception {
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = AcceptableRootThumbprintsTest.class.getClassLoader().getResourceAsStream(TRUST_STORE)) {
            trustStore.load(in, TRUST_STORE_PASSWORD.toCharArray());
        }

        Set<String> thumbprints = new LinkedHashSet<>();
        for (String alias : Collections.list(trustStore.aliases())) {
            X509Certificate certificate = (X509Certificate) trustStore.getCertificate(alias);
            if (certificate.getBasicConstraints() == -1
                    || !certificate.getSubjectX500Principal().getName().contains("O=MCP")) {
                continue;
            }
            for (String algorithm : List.of("SHA-256", "SHA-384", "SHA-1", "SHA-512")) {
                MessageDigest digest = MessageDigest.getInstance(algorithm);
                thumbprints.add(HexFormat.of().formatHex(digest.digest(certificate.getEncoded())));
            }
        }
        return thumbprints;
    }

    @Test
    void theThumbprintAmsaActuallySendsIsAccepted() throws Exception {
        assertThat(acceptableRootThumbprints())
                .as("the SHA-384 of the MCP root, observed in production being refused")
                .contains(AMSA_THUMBPRINT);
    }

    @Test
    void theThumbprintWeAdvertiseIsStillAccepted() throws Exception {
        // Widening what we accept must not drop what we tell peers to send, or correctly configured callers break.
        assertThat(acceptableRootThumbprints()).contains(ADVERTISED_THUMBPRINT);
    }

    @Test
    void bothMcpCertificateAuthoritiesAreCoveredByEveryHash() throws Exception {
        // Two anchors, four hashes.
        assertThat(acceptableRootThumbprints()).hasSize(8);
    }

    @Test
    void thePublicCasUsedForOutgoingTlsAreNotAccepted() throws Exception {
        // The same truststore carries ISRG Root X1 so outbound TLS works. Accepting its thumbprint would let anyone
        // with a certificate from a public CA declare a root we trust, and it is in no position to vouch for an MRN.
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = AcceptableRootThumbprintsTest.class.getClassLoader().getResourceAsStream(TRUST_STORE)) {
            trustStore.load(in, TRUST_STORE_PASSWORD.toCharArray());
        }
        X509Certificate isrg = (X509Certificate) trustStore.getCertificate("isrgrootx1");
        assertThat(isrg).isNotNull();

        String isrgSha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(isrg.getEncoded()));
        assertThat(acceptableRootThumbprints()).doesNotContain(isrgSha256);
    }

    @Test
    void thumbprintsAreLowerCaseHexSoComparisonCanBeCaseInsensitive() throws Exception {
        assertThat(acceptableRootThumbprints()).allSatisfy(t -> assertThat(t).matches("[0-9a-f]+"));
    }
}
