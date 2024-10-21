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
package dk.dma.baleen.service.mcp;

import io.smallrye.config.ConfigMapping;
import jakarta.enterprise.context.Dependent;

/**
 * Used to extract information about MCP trust store and keystore from application.properties.
 */
@ConfigMapping(prefix = "baleen.mcp")
@Dependent
interface MCPSecurityConfig {
    String keyStorePassword();
    String trustStorePassword();
    String keyStoreFile();
    String trustStoreFile();
    boolean trustStoreAcceptAll();
}