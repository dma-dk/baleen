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

import java.security.KeyStore;

import org.grad.secom.core.base.SecomTrustStoreProvider;

/** The SECOM Trust Store Provider Implementation. */
public class BaleenTrustStoreProvider implements SecomTrustStoreProvider {

    private final MCPSecurityService pki;

    /**
     * @param pki
     */
    public BaleenTrustStoreProvider(MCPSecurityService pki) {
        this.pki = requireNonNull(pki);
    }

    /** {@inheritDoc} */
    @Override
    public String getCARootCertificateAlias() {
        return pki.trustStoreRootAlias();
    }

    /** {@inheritDoc} */
    @Override
    public KeyStore getTrustStore() {
        return pki.trustStore();
    }
}
