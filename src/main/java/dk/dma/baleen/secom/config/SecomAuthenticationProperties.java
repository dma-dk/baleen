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

package dk.dma.baleen.secom.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * How much a SECOM caller has to prove about its identity before Baleen answers it.
 * <p>
 * The properties are bound with {@link Value} rather than
 * {@link org.springframework.boot.context.properties.ConfigurationProperties} because {@code BaleenApp} declares
 * no {@code @ConfigurationPropertiesScan} and nothing declares {@code @EnableConfigurationProperties}: a
 * {@code @ConfigurationProperties} class in this package is never registered and would silently keep its
 * defaults - {@link MCPSecomConfig} is exactly that, and is bound to nothing. The {@code baleen.exchange-set.*}
 * properties are bound the same way, in {@code S124ExchangeSetService}.
 */
@Component
public class SecomAuthenticationProperties {

    /** The logger of this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SecomAuthenticationProperties.class);

    private final boolean allowAnonymous;

    public SecomAuthenticationProperties(@Value("${baleen.secom.allow-anonymous:true}") boolean allowAnonymous) {
        this.allowAnonymous = allowAnonymous;
    }

    /**
     * {@return whether a caller that presents no usable client certificate is served anyway}
     * <p>
     * True - the default, and what baleen-test deploys - means the SECOM endpoints are open: a caller without a
     * certificate is served as an anonymous node. False means such a caller is refused with 401 Unauthorized.
     * Note that this says nothing about callers that <em>do</em> present a certificate: their identity is taken
     * from the {@code X-Secom-Cert} header without any verification either way, see
     * {@code MRNExtractorRequestFilter}.
     */
    public boolean allowAnonymous() {
        return allowAnonymous;
    }

    /** Makes the state of SECOM caller authentication impossible to miss in the log at startup. */
    @PostConstruct
    void reportAuthenticationMode() {
        if (allowAnonymous) {
            LOGGER.warn("""


                    ****************************************************************************************
                    *  SECOM CALLER IDENTITY IS NOT VERIFIED (baleen.secom.allow-anonymous=true)            *
                    *                                                                                      *
                    *  Callers that present no client certificate are served as anonymous, and the identity *
                    *  of callers that do present one is read straight out of the X-Secom-Cert header with  *
                    *  no chain validation, no validity check and no truststore lookup. Every SECOM         *
                    *  endpoint, DELETE /v2/subscription included, is effectively unauthenticated.          *
                    *                                                                                      *
                    *  MUST NOT be used in production. Set SECOM_ALLOW_ANONYMOUS=false to refuse callers    *
                    *  that present no certificate.                                                        *
                    ****************************************************************************************
                    """);
        } else {
            LOGGER.info("SECOM callers that present no client certificate are refused (baleen.secom.allow-anonymous=false). "
                    + "A certificate that is presented is still not chain-validated, see MRNExtractorRequestFilter.");
        }
    }
}
