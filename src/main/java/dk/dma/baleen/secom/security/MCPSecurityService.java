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

import java.io.InputStream;
import java.net.Socket;
import java.net.http.HttpClient;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;

import org.grad.secom.springboot3.components.SecomConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

/**
 * One stop stop for everything MCP security related.
 */
@Service
public class MCPSecurityService {

    // The default keystore alias, maybe move to to configuration.
    private static final String KEYSTORE_ALIAS = "1";

    /** The logger of this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MCPSecurityService.class);

    /** The configuration of this class. */
    final MCPSecurityConfig config;

    private final KeyStore keystore;

    /** The MCP root certificate, currently we only support one. */
    private final X509Certificate MCP_ROOT_CERTIFICATE;

    /** The MCP service certificate for this Baleen instance. */
    private final X509Certificate MCP_SERVICE_CERTIFICATE;

    private final PrivateKey PRIVATE_KEY;

    private final KeyStore truststore;

    private final ResourceLoader resourceLoader;
    
    @Autowired
    public MCPSecurityService(SecomConfigProperties config, ResourceLoader resourceLoader) throws Exception {
        this(new MCPSecurityConfig(config), resourceLoader);
    }

    private MCPSecurityService(MCPSecurityConfig config, ResourceLoader resourceLoader) throws Exception {
        this.keystore = loadKeyStore(config);
        this.truststore = loadTrustStore(config);
        this.config = requireNonNull(config);
        MCP_SERVICE_CERTIFICATE = requireNonNull(loadCertificate(keystore));
        this.resourceLoader = resourceLoader;
        
        Enumeration<String> aliases = truststore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            System.out.println(alias);
            // Process each alias here
        }

//        truststore.aliases();

        MCP_ROOT_CERTIFICATE = (X509Certificate) requireNonNull(truststore.getCertificate(trustStoreRootAlias()));
        PRIVATE_KEY = (PrivateKey) requireNonNull(keystore.getKey(KEYSTORE_ALIAS, config.keyStorePassword().toCharArray()));
    }

    private KeyStore loadKeyStore(MCPSecurityConfig config) throws Exception {
        String location = config.keyStoreFile(); // e.g. "classpath:secom/keystore.p12" or "/run/secrets/keystore.p12"
        Resource resource = resourceLoader.getResource(location.startsWith("classpath:") || location.startsWith("file:")
            ? location
            : (location.startsWith("/") ? "file:" + location : "classpath:" + location));

        if (!resource.exists()) {
          throw new IllegalArgumentException("Keystore not found at: " + location);
        }

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = resource.getInputStream()) {
          ks.load(in, config.keyStorePassword().toCharArray());
        }
        LOGGER.info("Loaded keystore from: {}", location);
        return ks;
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

    public byte[] sign(String algorithm, byte[] payload) throws GeneralSecurityException {
        Signature sign = Signature.getInstance(algorithm);
        sign.initSign(PRIVATE_KEY);
        sign.update(payload);

        // Sign and return the signature
        return sign.sign();
    }

//    public PrivateKey mcpServicePrivateKey() {
//        return PRIVATE_KEY;
//    }

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

    public String trustStoreRootAlias() {
        return "mcp identity registry (mcp root certificate)";// "urn:mrn:mcp:ca:mcc:mcp";
    }

    private static X509Certificate loadCertificate(KeyStore keystore) throws Exception {
        String alias = KEYSTORE_ALIAS;

        // Get the certificate from the keystore
        Certificate cert = keystore.getCertificate(alias);

        if (cert instanceof X509Certificate x509Cert) {
            return x509Cert;
        } else {
            throw new RuntimeException("Could not find certificate with alias " + alias);
        }
    }
}
