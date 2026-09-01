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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import org.grad.secomv2.core.utils.PkiUtils;
import org.junit.jupiter.api.Test;

/**
 * Pins the reason both MCP CA certificates have to be trust anchors, not just the one whose thumbprint Baleen
 * advertises.
 * <p>
 * {@code PkiUtils.verifyCertificateChain} validates with a {@link java.security.cert.CertPathValidator}, and a PKIX
 * certification path must stop <em>below</em> the anchor it is checked against - the anchor is never part of the
 * path. So which certificates a caller may send is decided entirely by which anchors are in the truststore. With
 * only {@code CN=MCP Identity Registry} anchored, a caller that sends just its own certificate is accepted and a
 * caller that helpfully sends {@code [leaf, intermediate]} is refused, because the path then ends at the anchor
 * itself. Anchoring {@code CN=MCP Root Certificate} as well accepts both shapes, which is what MCP's own
 * documentation tells relying parties to install.
 * <p>
 * The certificates used here are the real ones from
 * {@code https://raw.githubusercontent.com/maritimeconnectivity/docs.maritimeconnectivity.net/refs/heads/files/mcp-ca-chain.pem},
 * so the intermediate stands in for the "sent one level too many" shape without needing a real client certificate:
 * it is a genuine certificate issued by the root, and it carries the digitalSignature and keyEncipherment bits
 * {@code PkiUtils} constrains the target of the path to.
 */
class McpTrustAnchorChainTest {

    private static final String TRUST_STORE = "secom/truststore.p12";

    private static final String TRUST_STORE_PASSWORD = "changeit";

    private static final String INTERMEDIATE_SUBJECT = "CN=MCP Identity Registry";

    private static final String ROOT_SUBJECT = "CN=MCP Root Certificate";

    private static KeyStore trustStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = McpTrustAnchorChainTest.class.getClassLoader().getResourceAsStream(TRUST_STORE)) {
            keyStore.load(in, TRUST_STORE_PASSWORD.toCharArray());
        }
        return keyStore;
    }

    /** {@return the anchor in the bundled truststore whose subject contains the given common name} */
    private static X509Certificate anchor(String subject) throws Exception {
        KeyStore trustStore = trustStore();
        for (String alias : Collections.list(trustStore.aliases())) {
            X509Certificate certificate = (X509Certificate) trustStore.getCertificate(alias);
            if (certificate.getSubjectX500Principal().getName().contains(subject)) {
                return certificate;
            }
        }
        throw new AssertionError("No trust anchor with subject " + subject + " in " + TRUST_STORE);
    }

    /** {@return a truststore holding only the given certificate, to isolate what a single anchor accepts} */
    private static KeyStore trustStoreOf(X509Certificate anchor) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setCertificateEntry("anchor", anchor);
        return keyStore;
    }

    @Test
    void bothMcpCertificateAuthoritiesAreAnchored() throws Exception {
        assertThat(anchor(INTERMEDIATE_SUBJECT).getSubjectX500Principal().getName())
                .contains("UID=urn:mrn:mcp:ca:mcc:mcp-idreg-new");
        assertThat(anchor(ROOT_SUBJECT).getIssuerX500Principal())
                .as("the MCP root is self signed")
                .isEqualTo(anchor(ROOT_SUBJECT).getSubjectX500Principal());
    }

    @Test
    void theRootIssuedTheIntermediateWeAdvertise() throws Exception {
        // Guards against anchoring some other CA that merely shares the common name: only the genuine root's public
        // key verifies the signature on the certificate whose thumbprint Baleen publishes to every peer.
        anchor(INTERMEDIATE_SUBJECT).verify(anchor(ROOT_SUBJECT).getPublicKey());
    }

    @Test
    void aChainThatIncludesTheIntermediateIsAcceptedNowThatTheRootIsAnchored() throws Exception {
        // The shape a caller sends when it includes one certificate more than it strictly has to. Validating it
        // needs an anchor ABOVE the intermediate, which is what the root entry provides.
        X509Certificate[] chain = { anchor(INTERMEDIATE_SUBJECT) };

        assertThat(PkiUtils.verifyCertificateChain(chain, trustStore())).isTrue();
    }

    @Test
    void thatSameChainIsRefusedWhenOnlyTheIntermediateIsAnchored() throws Exception {
        // The regression this guards: with the intermediate as the only anchor, the path ends AT the anchor and
        // PKIX rejects it. This is what refused callers that sent [leaf, intermediate] before the root was added.
        X509Certificate[] chain = { anchor(INTERMEDIATE_SUBJECT) };
        KeyStore intermediateOnly = trustStoreOf(anchor(INTERMEDIATE_SUBJECT));

        assertThatThrownBy(() -> PkiUtils.verifyCertificateChain(chain, intermediateOnly))
                .hasMessageContaining("trust anchor");
    }

    @Test
    void theBundledPemMatchesTheAnchorItDocuments() throws Exception {
        // src/main/resources/secom/*.pem are the human readable copies of what is in the truststore. They drift
        // silently, because nothing reads them at runtime.
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        for (String pem : List.of("secom/mcp-idreg-new.pem", "secom/mcp-root.pem")) {
            try (InputStream in = McpTrustAnchorChainTest.class.getClassLoader().getResourceAsStream(pem)) {
                assertThat(in).as("%s is missing from the classpath", pem).isNotNull();
                X509Certificate fromPem = (X509Certificate) factory.generateCertificate(in);
                assertThat(fromPem).isEqualTo(anchor(fromPem.getSubjectX500Principal().getName().contains(ROOT_SUBJECT)
                        ? ROOT_SUBJECT
                        : INTERMEDIATE_SUBJECT));
            }
        }
    }
}
