package dk.dma.baleen.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Database configuration for Baleen application.
 * Handles dynamic database configuration based on DATABASE_TYPE environment variable.
 * Database schema management is now handled by Liquibase.
 *
 * Supports:
 * - H2 with H2GIS spatial extensions (default for development)
 * - PostgreSQL with PostGIS extensions (for production/Docker)
 */
@Configuration
public class DatabaseConfig {

    @Value("${DATABASE_TYPE:h2}")
    private String databaseType;

    @PostConstruct
    public void init() {
        System.out.println("Initializing database configuration for type: " + databaseType);

        if ("h2".equalsIgnoreCase(databaseType)) {
            System.out.println("Using H2 database with H2GIS spatial extensions");
            // H2GIS functions are loaded via Liquibase changelog
        } else if ("postgresql".equalsIgnoreCase(databaseType)) {
            System.out.println("Using PostgreSQL database with PostGIS extensions");
            // PostGIS extension is enabled via Liquibase changelog
        } else {
            System.out.println("Unknown database type: " + databaseType + ", using default configuration");
        }
    }
}