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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 */

@Configuration
//@ConditionalOnProperty(name = "secom.provider.factory", havingValue = "dk.dma.baleen.secom.security.LoadBaleen")
public class LoadBaleen {

    @Bean
    BaleenCertificateProvider bcp(@Autowired MCPSecurityService pki) {
        return new BaleenCertificateProvider(pki);
    }

    @Bean
    BaleenSignatureProvider bsp(@Autowired MCPSecurityService pki) {
        return new BaleenSignatureProvider(pki);
    }

    @Bean
    BaleenTrustStoreProvider tsp(@Autowired MCPSecurityService pki) {
        return new BaleenTrustStoreProvider(pki);
    }
}
