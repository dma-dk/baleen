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

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;

import org.grad.secomv2.springboot3.components.SecomConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

/**
 * One stop stop for everything MCP security related.
 */
@Service
public class MCPSecurityService {

    /** The truststore alias keytool gives the MCP trust anchor when it is imported under its own name. */
    private static final String DEFAULT_TRUST_STORE_ROOT_ALIAS = "mcp identity registry (mcp root certificate)";

    /** The subject common name of the MCP trust anchor, used when it is stored under some other alias. */
    private static final String TRUST_STORE_ROOT_COMMON_NAME = "MCP Identity Registry";

    /** The hashes a SECOM peer might have used to fingerprint a CA, in the order they are tried. */
    private static final List<String> THUMBPRINT_HASHES = List.of("SHA-256", "SHA-384", "SHA-1", "SHA-512");

    /** The logger of this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MCPSecurityService.class);

    /** The configuration of this class. */
    final MCPSecurityConfig config;

    private final KeyStore keystore;

    /** The keystore alias the service key pair is stored under. */
    private final String keyStoreAlias;

    /** The MCP root certificate, currently we only support one. */
    private final X509Certificate MCP_ROOT_CERTIFICATE;

    /** The MCP service certificate for this Baleen instance. */
    private final X509Certificate MCP_SERVICE_CERTIFICATE;

    /** The CA certificates between the service certificate and the trust anchor. */
    private final List<X509Certificate> MCP_INTERMEDIATE_CERTIFICATES;

    /** Every CA certificate the truststore trusts. */
    private final List<X509Certificate> TRUST_ANCHORS;

    private final PrivateKey PRIVATE_KEY;

    private final KeyStore truststore;

    /** The truststore alias {@link #MCP_ROOT_CERTIFICATE} is stored under. */
    private final String trustStoreRootAlias;

    private final ResourceLoader resourceLoader;

    @Autowired
    public MCPSecurityService(SecomConfigProperties config, ResourceLoader resourceLoader,
            @Value("${secom.security.ssl.key-alias:}") String configuredKeyStoreAlias) throws Exception {
        this(new MCPSecurityConfig(config), resourceLoader, configuredKeyStoreAlias);
    }

    private MCPSecurityService(MCPSecurityConfig config, ResourceLoader resourceLoader, String configuredKeyStoreAlias) throws Exception {
        this.resourceLoader = requireNonNull(resourceLoader);
        this.keystore = loadKeyStore(config);
        this.truststore = loadTrustStore(config);
        this.config = requireNonNull(config);

        String keyAlias = resolveKeyStoreAlias(keystore, configuredKeyStoreAlias, config.keyStoreFile());
        this.keyStoreAlias = keyAlias;
        MCP_SERVICE_CERTIFICATE = requireNonNull(loadCertificate(keystore, keyAlias));
        MCP_INTERMEDIATE_CERTIFICATES = loadIntermediateCertificates(keystore, keyAlias);

        this.trustStoreRootAlias = resolveTrustStoreRootAlias(truststore, config.trustStoreFile());
        MCP_ROOT_CERTIFICATE = (X509Certificate) truststore.getCertificate(trustStoreRootAlias);
        TRUST_ANCHORS = loadTrustAnchors(truststore);
        PRIVATE_KEY = (PrivateKey) requireNonNull(keystore.getKey(keyAlias, config.keyStorePassword().toCharArray()),
                () -> "No private key stored under the alias '" + keyAlias + "' in the keystore at " + config.keyStoreFile());
    }

    private KeyStore loadKeyStore(MCPSecurityConfig config) throws Exception {
        String location = config.keyStoreFile(); // e.g. "classpath:secom/keystore.p12" or "/run/secrets/keystore.p12"
        Resource resource = resourceLoader.getResource(location.startsWith("classpath:") || location.startsWith("file:")
            ? location
            : (location.startsWith("/") ? "file:" + location : "classpath:" + location));

        if (!resource.exists()) {
          throw new IllegalArgumentException("Keystore not found at: " + location);
        }

        // Read the entire file content first
        byte[] keystoreData;
        try (InputStream in = resource.getInputStream()) {
            keystoreData = in.readAllBytes();
        }

        // Check if the content is base64-encoded and decode if necessary
        if (isBase64Encoded(keystoreData)) {
            LOGGER.info("Detected base64-encoded keystore, decoding...");
            String base64String = new String(keystoreData, StandardCharsets.UTF_8).trim();
            keystoreData = Base64.getDecoder().decode(base64String);
        }

        // Load the keystore from the (possibly decoded) binary data
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (ByteArrayInputStream bis = new ByteArrayInputStream(keystoreData)) {
            ks.load(bis, config.keyStorePassword().toCharArray());
        }

        LOGGER.info("Loaded keystore from: {}", location);
        return ks;
    }

    private boolean isBase64Encoded(byte[] data) {
        // PKCS12 files typically start with 0x30 0x82 (ASN.1 SEQUENCE)
        if (data.length >= 2 && data[0] == 0x30 && (data[1] & 0xFF) == 0x82) {
            return false; // Already in binary format
        }

        // Check if the content appears to be base64
        // Base64 uses A-Z, a-z, 0-9, +, /, = and whitespace
        String content = new String(data, StandardCharsets.US_ASCII);
        return content.matches("^[A-Za-z0-9+/=\\s]+$");
    }

    private KeyStore loadTrustStore(MCPSecurityConfig config) throws Exception {
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (InputStream trustStoreStream = MCPSecurityService.class.getClassLoader().getResourceAsStream(config.trustStoreFile())) {
            if (trustStoreStream == null) {
                throw new IllegalArgumentException("Truststore not found in classpath");
            }
            trustStore.load(trustStoreStream, config.trustStorePassword().toCharArray());
        }
        LOGGER.info("Loaded keystore from: " + config.trustStoreFile());
        return trustStore;

    }

    /** {@return the MCP root certificate} */
    public X509Certificate mcpRootCertificate() {
        return MCP_ROOT_CERTIFICATE;
    }

    /** {@return the MCP service certificate for this instance} */
    public X509Certificate mcpServiceCertificate() {
        return MCP_SERVICE_CERTIFICATE;
    }

    /**
     * {@return the CA certificates between the service certificate and the trust anchor}
     *
     * The service certificate itself and any self-signed root are left out. S-100 Part 15, clause 15-8.7, requires an
     * exchange set to carry the intermediates so a receiver can build the certification path without network access,
     * while the root is installed on the receiving system and must not be shipped.
     */
    public List<X509Certificate> mcpIntermediateCertificates() {
        return MCP_INTERMEDIATE_CERTIFICATES;
    }

    /**
     * {@return every CA certificate stored as a trusted entry in the truststore}
     *
     * MCP issues through an intermediate, so a caller may anchor on either {@code CN=MCP Identity Registry} or the
     * {@code CN=MCP Root Certificate} above it. A certification path must stop below whichever anchor it is checked
     * against, which is why validation needs the full set rather than the single certificate
     * {@link #mcpRootCertificate()} names. The end-entity certificates in the truststore, pinned there for outgoing
     * TLS, are not trust anchors and are left out.
     *
     * The same truststore is used for outgoing TLS, so the list also holds public CAs such as
     * {@code CN=ISRG Root X1} that have nothing to do with MCP. A caller that validates an MCP identity must
     * therefore narrow the set itself - none of these are entitled to issue an MRN.
     */
    public List<X509Certificate> trustAnchors() {
        return TRUST_ANCHORS;
    }

    /**
     * {@return every thumbprint a caller could legitimately use to name the MCP CA it anchors on, lower case hex}
     * <p>
     * SECOM has a peer declare the CA it trusts by thumbprint, and the receiver compares that against its own. The
     * comparison is a string equality over a hash of a certificate, so it fails whenever the two sides describe the
     * same certification authority differently - and there are two independent ways to differ.
     * <p>
     * The first is <em>which</em> certificate. MCP issues through {@code CN=MCP Identity Registry} under
     * {@code CN=MCP Root Certificate}, and MCP's own documentation hands relying parties the root, so a caller
     * naming the root and one naming the intermediate are both correct.
     * <p>
     * The second is <em>which hash</em>. {@code SecomConstants.CERTIFICATE_THUMBPRINT_HASH} is hardcoded to SHA-256
     * and the library's schema documents "SHA-1 or SHA-256", while S-100 Part 15 mandates SHA-384 throughout. The
     * AMSA client observed on {@code /v2/object/search} sends the SHA-384 of the root, which is defensible and
     * which a SHA-256 comparison can never match.
     * <p>
     * So every MCP anchor is offered under every hash a peer plausibly used. This widens only what is
     * <em>accepted</em>; what Baleen advertises is untouched, so peers configured against us keep working. It
     * deliberately does not include the public CAs the same truststore carries for outgoing TLS - accepting
     * {@code CN=ISRG Root X1} here would let anyone holding a certificate from a public CA claim a trusted root,
     * and that CA is in no position to vouch for an MRN.
     */
    public Set<String> acceptableRootThumbprints() {
        Set<String> thumbprints = new LinkedHashSet<>();
        for (X509Certificate anchor : mcpTrustAnchors()) {
            for (String algorithm : THUMBPRINT_HASHES) {
                try {
                    MessageDigest digest = MessageDigest.getInstance(algorithm);
                    thumbprints.add(HexFormat.of().formatHex(digest.digest(anchor.getEncoded())));
                } catch (GeneralSecurityException e) {
                    // A JDK without one of these hashes is not a condition worth failing startup over; the
                    // remaining ones still let the common cases through.
                    LOGGER.warn("Could not compute the {} thumbprint of trust anchor {}", algorithm,
                            anchor.getSubjectX500Principal(), e);
                }
            }
        }
        return Set.copyOf(thumbprints);
    }

    /**
     * {@return the trust anchors that are MCP's own certification authorities}
     * <p>
     * {@link #trustAnchors()} also holds the public CAs the same truststore carries for outgoing TLS. Those are not
     * entitled to vouch for an MRN, so anything deciding whether a SECOM peer is who it says it is wants this
     * narrower set. Matching the organisation keeps them out without pinning an exact common name, which differs
     * between the root, the registry and the test PKI.
     */
    public List<X509Certificate> mcpTrustAnchors() {
        return TRUST_ANCHORS.stream()
                .filter(c -> c.getSubjectX500Principal().getName().contains("O=MCP"))
                .toList();
    }

    public byte[] sign(String algorithm, byte[] payload) throws GeneralSecurityException {
        Signature sign = Signature.getInstance(algorithm);
        sign.initSign(PRIVATE_KEY);
        sign.update(payload);

        // Sign and return the signature
        return sign.sign();
    }

    public HttpClient newHttpClient() {
        try {
            return newHttpClient0();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private HttpClient newHttpClient0() throws Exception {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keystore, config.keyStorePassword().toCharArray());

        // Initialize TrustManagerFactory
        TrustManager tm;
        if (config.trustStoreAcceptAll()) {
            tm = new X509ExtendedTrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {}

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {}

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {}

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {}

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[] {};
                }
            };
        } else {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(truststore);
            tm = tmf.getTrustManagers()[0];
        }

        // Create SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), new TrustManager[] { tm }, null);

        // Create HttpClient with SSLContext
        return HttpClient.newBuilder().sslContext(sslContext).build();
    }

    public KeyStore trustStore() {
        return truststore;
    }

    /** {@return the truststore alias of the certificate whose thumbprint Baleen advertises} */
    public String trustStoreRootAlias() {
        return trustStoreRootAlias;
    }

    /** {@return the keystore alias the service key pair is stored under} */
    public String keyStoreAlias() {
        return keyStoreAlias;
    }

    /**
     * {@return the alias of the private key entry to sign with}
     *
     * The alias cannot be hardcoded: a keystore exported by openssl names its single entry {@code 1}, while the
     * bundled development keystore names its entry {@code localdev}. An explicitly configured
     * {@code secom.security.ssl.key-alias} therefore wins, and otherwise the keystore is scanned - which resolves
     * both of those, as they hold exactly one private key entry each.
     *
     * @throws IllegalStateException
     *             if the configured alias is not in the keystore, or if scanning does not find exactly one private
     *             key entry
     */
    static String resolveKeyStoreAlias(KeyStore keystore, String configuredAlias, String keyStoreLocation) throws KeyStoreException {
        if (configuredAlias != null && !configuredAlias.isBlank()) {
            String alias = configuredAlias.trim();
            if (!keystore.isKeyEntry(alias)) {
                throw new IllegalStateException("The keystore at " + keyStoreLocation + " has no private key entry under the configured alias '"
                        + alias + "' (secom.security.ssl.key-alias), the aliases it holds are " + aliasesOf(keystore));
            }
            LOGGER.info("Signing with the configured keystore alias '{}' of {}", alias, keyStoreLocation);
            return alias;
        }

        List<String> keyAliases = new ArrayList<>();
        for (String alias : aliasesOf(keystore)) {
            if (keystore.isKeyEntry(alias)) {
                keyAliases.add(alias);
            }
        }
        if (keyAliases.isEmpty()) {
            throw new IllegalStateException("The keystore at " + keyStoreLocation + " holds no private key entry, the aliases it holds are "
                    + aliasesOf(keystore));
        }
        if (keyAliases.size() > 1) {
            throw new IllegalStateException("The keystore at " + keyStoreLocation + " holds more than one private key entry " + keyAliases
                    + ", set secom.security.ssl.key-alias to the one Baleen should sign with");
        }
        String alias = keyAliases.get(0);
        LOGGER.info("Signing with the only private key entry '{}' of {}", alias, keyStoreLocation);
        return alias;
    }

    /**
     * {@return the alias of the MCP trust anchor in the truststore}
     *
     * keytool derives the alias from the subject and the issuer of the certificate, so re-importing the same
     * certificate is enough to rename it. We then find it by subject common name instead, and if that fails too we
     * say which aliases are actually there rather than leaving a bare NullPointerException at startup.
     *
     * @throws IllegalStateException
     *             if the truststore holds no {@code CN=MCP Identity Registry} certificate
     */
    static String resolveTrustStoreRootAlias(KeyStore truststore, String trustStoreLocation) throws KeyStoreException {
        if (truststore.getCertificate(DEFAULT_TRUST_STORE_ROOT_ALIAS) instanceof X509Certificate) {
            return DEFAULT_TRUST_STORE_ROOT_ALIAS;
        }
        for (String alias : aliasesOf(truststore)) {
            if (truststore.getCertificate(alias) instanceof X509Certificate certificate
                    && TRUST_STORE_ROOT_COMMON_NAME.equals(subjectCommonName(certificate))) {
                LOGGER.info("Found CN={} under the alias '{}' of {}", TRUST_STORE_ROOT_COMMON_NAME, alias, trustStoreLocation);
                return alias;
            }
        }
        throw new IllegalStateException("The truststore at " + trustStoreLocation + " holds no CN=" + TRUST_STORE_ROOT_COMMON_NAME
                + " certificate, neither under the alias '" + DEFAULT_TRUST_STORE_ROOT_ALIAS + "' nor under any other, the aliases it holds are "
                + aliasesOf(truststore));
    }

    /** {@return every CA certificate stored as a trusted entry in the given truststore, in truststore order} */
    static List<X509Certificate> loadTrustAnchors(KeyStore truststore) throws KeyStoreException {
        List<X509Certificate> anchors = new ArrayList<>();
        for (String alias : aliasesOf(truststore)) {
            if (truststore.isCertificateEntry(alias) && truststore.getCertificate(alias) instanceof X509Certificate certificate
                    && certificate.getBasicConstraints() != -1) { // -1 means the certificate is not a CA
                anchors.add(certificate);
            }
        }
        return List.copyOf(anchors);
    }

    /** {@return the common name of the subject of the certificate, or null if it has none} */
    private static String subjectCommonName(X509Certificate certificate) {
        for (String component : certificate.getSubjectX500Principal().getName().split(",")) {
            String trimmed = component.trim();
            if (trimmed.startsWith("CN=")) {
                return trimmed.substring("CN=".length());
            }
        }
        return null;
    }

    /** {@return every alias of the given keystore, in enumeration order} */
    private static List<String> aliasesOf(KeyStore store) throws KeyStoreException {
        List<String> aliases = new ArrayList<>();
        for (Enumeration<String> e = store.aliases(); e.hasMoreElements();) {
            aliases.add(e.nextElement());
        }
        return List.copyOf(aliases);
    }

    private static List<X509Certificate> loadIntermediateCertificates(KeyStore keystore, String alias) throws Exception {
        Certificate[] chain = keystore.getCertificateChain(alias);
        if (chain == null) {
            return List.of();
        }
        List<X509Certificate> intermediates = new ArrayList<>();
        for (int i = 1; i < chain.length; i++) { // the leaf at index 0 is the service certificate
            if (chain[i] instanceof X509Certificate x509Cert && !isSelfSigned(x509Cert)) {
                intermediates.add(x509Cert);
            }
        }
        return List.copyOf(intermediates);
    }

    private static boolean isSelfSigned(X509Certificate cert) {
        return cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal());
    }

    private static X509Certificate loadCertificate(KeyStore keystore, String alias) throws Exception {
        // Get the certificate from the keystore
        Certificate cert = keystore.getCertificate(alias);

        if (cert instanceof X509Certificate x509Cert) {
            return x509Cert;
        } else {
            throw new RuntimeException("Could not find certificate with alias " + alias);
        }
    }
}
