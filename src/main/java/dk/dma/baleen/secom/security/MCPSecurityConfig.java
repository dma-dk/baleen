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
