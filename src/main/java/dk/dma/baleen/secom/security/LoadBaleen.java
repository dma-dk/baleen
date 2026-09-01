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

    /*
     * There is deliberately no SecomTrustStoreProvider bean.
     *
     * SecomSignatureFilter only runs its checkCertificate when one is present, and that check compares the caller's
     * envelopeRootCertificateThumbprint against a single certificate under a single hash, hardcoded to SHA-256. It
     * therefore refuses callers that name the same MCP certification authority correctly but differently - naming
     * the root rather than the intermediate, or hashing with the SHA-384 that S-100 Part 15 mandates. Leaving the
     * bean out is what turns that check off; nothing else in the library consumes the provider.
     *
     * The check itself is not lost: SecomRootCertificateFilter does it before the signature filter runs, with the
     * same validity and chain verification and a thumbprint comparison that accepts every way of naming an anchor
     * we hold. Removing that filter without restoring a provider here would leave signed requests unauthenticated,
     * so the two belong together. BaleenTrustStoreProvider is kept for whenever the library's comparison is fixed
     * upstream.
     */
}
