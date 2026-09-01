/*
 * Copyright (c) 2008 Kasper Nielsen.
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
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The keystore alias cannot be hardcoded: the bundled development keystore holds its key pair under
 * {@code localdev}, while the deployed keystore is believed to use the openssl convention {@code 1}. These tests
 * pin the resolution that has to serve both without configuration, and the truststore lookup that used to fail
 * with a bare NullPointerException whenever the anchor was stored under another alias.
 */
class MCPSecurityServiceAliasResolutionTest {

    private static final String DEV_KEY_STORE = "secom/keystore-localdev.p12";

    private static final String TRUST_STORE = "secom/truststore.p12";

    private static final String MCP_ALIAS = "mcp identity registry (mcp root certificate)";

    private static final char[] PASSWORD = "changeit".toCharArray();

    @Test
    void theOnlyPrivateKeyEntryIsUsedWhenNothingIsConfigured() throws Exception {
        assertThat(MCPSecurityService.resolveKeyStoreAlias(devKeyStore(), "", DEV_KEY_STORE)).isEqualTo("localdev");
        assertThat(MCPSecurityService.resolveKeyStoreAlias(devKeyStore(), null, DEV_KEY_STORE)).isEqualTo("localdev");
        assertThat(MCPSecurityService.resolveKeyStoreAlias(devKeyStore(), "   ", DEV_KEY_STORE)).isEqualTo("localdev");
    }

    @Test
    void aConfiguredAliasDecidesWhenThereIsMoreThanOneKey() throws Exception {
        assertThat(MCPSecurityService.resolveKeyStoreAlias(twoKeyEntries(), "localdev", DEV_KEY_STORE)).isEqualTo("localdev");
        assertThat(MCPSecurityService.resolveKeyStoreAlias(twoKeyEntries(), " deployed ", DEV_KEY_STORE)).isEqualTo("deployed");
    }

    @Test
    void aConfiguredAliasThatIsNotThereNamesTheOnesThatAre() throws Exception {
        assertThatThrownBy(() -> MCPSecurityService.resolveKeyStoreAlias(devKeyStore(), "1", DEV_KEY_STORE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'1'")
                .hasMessageContaining("localdev")
                .hasMessageContaining(DEV_KEY_STORE);
    }

    @Test
    void severalPrivateKeyEntriesAskForConfiguration() throws Exception {
        assertThatThrownBy(() -> MCPSecurityService.resolveKeyStoreAlias(twoKeyEntries(), "", DEV_KEY_STORE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("localdev")
                .hasMessageContaining("deployed")
                .hasMessageContaining("secom.security.ssl.key-alias");
    }

    @Test
    void aKeyStoreWithoutAPrivateKeyNamesItself() throws Exception {
        KeyStore empty = KeyStore.getInstance("PKCS12");
        empty.load(null, PASSWORD);

        assertThatThrownBy(() -> MCPSecurityService.resolveKeyStoreAlias(empty, "", "/run/secrets/keystore.p12"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("/run/secrets/keystore.p12");
    }

    @Test
    void theBundledTrustStoreResolvesToTheMcpIdentityRegistry() throws Exception {
        KeyStore truststore = trustStore();

        String alias = MCPSecurityService.resolveTrustStoreRootAlias(truststore, TRUST_STORE);

        // The thumbprint of this certificate is advertised in every SECOM response, so it must not change.
        assertThat(alias).isEqualTo(MCP_ALIAS);
        assertThat(truststore.getCertificate(alias)).isEqualTo(truststore.getCertificate(MCP_ALIAS));
    }

    @Test
    void aRenamedAnchorIsStillFoundBySubject() throws Exception {
        KeyStore renamed = KeyStore.getInstance("PKCS12");
        renamed.load(null, PASSWORD);
        renamed.setCertificateEntry("mcp-idreg-new", trustStore().getCertificate(MCP_ALIAS));

        assertThat(MCPSecurityService.resolveTrustStoreRootAlias(renamed, TRUST_STORE)).isEqualTo("mcp-idreg-new");
    }

    @Test
    void aTrustStoreWithoutTheAnchorNamesWhatItHolds() throws Exception {
        KeyStore withoutAnchor = KeyStore.getInstance("PKCS12");
        withoutAnchor.load(null, PASSWORD);
        withoutAnchor.setCertificateEntry("baleen-localdev", devKeyStore().getCertificate("localdev"));

        assertThatThrownBy(() -> MCPSecurityService.resolveTrustStoreRootAlias(withoutAnchor, TRUST_STORE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MCP Identity Registry")
                .hasMessageContaining("baleen-localdev");
    }

    @Test
    void onlyCaCertificatesAreTrustAnchors() throws Exception {
        KeyStore truststore = trustStore();
        // The certificate of the development keystore stands in for the end-entity certificates the truststore
        // pins for outgoing TLS: trusted, but never something a certification path can be anchored on.
        KeyStore mixed = KeyStore.getInstance("PKCS12");
        mixed.load(null, PASSWORD);
        mixed.setCertificateEntry("mcp", truststore.getCertificate(MCP_ALIAS));
        mixed.setCertificateEntry("baleen-localdev", devKeyStore().getCertificate("localdev"));

        List<X509Certificate> anchors = MCPSecurityService.loadTrustAnchors(mixed);

        assertThat(anchors).containsExactly((X509Certificate) truststore.getCertificate(MCP_ALIAS));
    }

    @Test
    void theBundledTrustStoreAnchorsIncludeTheMcpIdentityRegistry() throws Exception {
        KeyStore truststore = trustStore();

        List<X509Certificate> anchors = MCPSecurityService.loadTrustAnchors(truststore);

        assertThat(anchors).contains((X509Certificate) truststore.getCertificate(MCP_ALIAS));
        assertThat(anchors).allSatisfy(anchor -> assertThat(anchor.getBasicConstraints())
                .as("%s is a CA", anchor.getSubjectX500Principal()).isNotEqualTo(-1));
    }

    /** {@return the bundled development keystore, with its key pair stored under a second alias as well} */
    private static KeyStore twoKeyEntries() throws Exception {
        KeyStore keystore = devKeyStore();
        Key key = keystore.getKey("localdev", PASSWORD);
        keystore.setKeyEntry("deployed", key, PASSWORD, keystore.getCertificateChain("localdev"));
        return keystore;
    }

    private static KeyStore devKeyStore() throws Exception {
        return load(DEV_KEY_STORE);
    }

    private static KeyStore trustStore() throws Exception {
        return load(TRUST_STORE);
    }

    private static KeyStore load(String resource) throws Exception {
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        try (InputStream in = MCPSecurityServiceAliasResolutionTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).as("%s on the classpath", resource).isNotNull();
            keystore.load(in, PASSWORD);
        }
        return keystore;
    }
}
