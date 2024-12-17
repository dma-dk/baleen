package dk.dma.baleen.connector.secom.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration
 */
@ConfigurationProperties(prefix = "mcp.secom")
public record MCPSecomConfig(
    String keystore,
    String keystoreType,
    String keystorePassword,
    String truststore,
    String truststoreType,
    String truststorePassword,
    Boolean insecureSslPolicy
) {}