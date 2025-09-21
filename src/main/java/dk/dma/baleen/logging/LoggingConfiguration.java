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

package dk.dma.baleen.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Responsible for configuring the in memory log appender.
 */
@Configuration
public class LoggingConfiguration {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LoggingConfiguration.class);

    @Value("${spring.datasource.url}")
    private String databaseUrl;
    
    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Bean
    public CommandLineRunner logStartup() {
        return args -> {
            log.info("===========================================");
            log.info("Baleen Application Starting");
            log.info("Active Profile: {}", activeProfile);
            log.info("Database URL: {}", databaseUrl);
            log.info("Environment DATABASE_PASSWORD is set: {}", 
                System.getenv("DATABASE_PASSWORD") != null ? "YES" : "NO");
            log.info("===========================================");
        };
    }

    @EventListener(ApplicationStartedEvent.class)
    public void onApplicationStarted() {
        // The logback.xml should have already configured our appender
        // Let's verify it's working
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

        boolean hasInMemoryAppender = false;
        var iterator = rootLogger.iteratorForAppenders();
        while (iterator.hasNext()) {
            var appender = iterator.next();
            if (appender instanceof InMemoryLogAppender) {
                hasInMemoryAppender = true;
                log.info("InMemoryLogAppender found: {} (status: {})",
                    appender.getName(),
                    appender.isStarted() ? "STARTED" : "STOPPED");
            }
        }

        if (!hasInMemoryAppender) {
            log.warn("InMemoryLogAppender not found! Logs will not be captured in memory.");
        }

        log.info("=== Baleen has started successfully ===");
        log.info("Logging system status: {} logs captured so far", InMemoryLogAppender.getLogCount());

        // Log some system information
        log.info("Java Version: {}", System.getProperty("java.version"));
        log.info("Available processors: {}", Runtime.getRuntime().availableProcessors());
        log.info("Max memory: {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024);
    }
}