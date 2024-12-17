package dk.dma.baleen.connector.secom.security;

import org.grad.secom.springboot3.components.SecomConfigProperties;

/**
 * Wrapper around SecomConfigProperties to map configuration to MCPSecurityConfig interface.
 */
public class MCPSecurityConfig  {

    private final SecomConfigProperties secomConfigProperties;

    public MCPSecurityConfig(SecomConfigProperties secomConfigProperties) {
        this.secomConfigProperties = secomConfigProperties;
    }

    public String keyStorePassword() {
        return secomConfigProperties.getKeystorePassword();
    }

    public String trustStorePassword() {
        return secomConfigProperties.getTruststorePassword();
    }

    public String keyStoreFile() {
        return secomConfigProperties.getKeystore();
    }

    public String trustStoreFile() {
        return secomConfigProperties.getTruststore();
    }

    public boolean trustStoreAcceptAll() {
        // Assuming the 'insecureSslPolicy' in SecomConfigProperties relates to this
        return Boolean.TRUE.equals(secomConfigProperties.getInsecureSslPolicy());
    }
}
