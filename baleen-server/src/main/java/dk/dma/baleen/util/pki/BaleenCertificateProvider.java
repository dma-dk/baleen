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
package dk.dma.baleen.util.pki;

import org.grad.secom.core.base.DigitalSignatureCertificate;
import org.grad.secom.core.base.SecomCertificateProvider;

import dk.dma.baleen.service.mcp.MCPSecurityService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** The SECOM Certificate Provider Implementation. */
@ApplicationScoped
public class BaleenCertificateProvider implements SecomCertificateProvider {

    @Inject
    MCPSecurityService pki;

    /** {@inheritDoc} */
    @Override
    public DigitalSignatureCertificate getDigitalSignatureCertificate() {
        // Initialise SECOM the digital signature certificate
        final DigitalSignatureCertificate digitalSignatureCertificate = new DigitalSignatureCertificate();

        digitalSignatureCertificate.setCertificateAlias("1");
        digitalSignatureCertificate.setCertificate(pki.mcpServiceCertificate());
        digitalSignatureCertificate.setPublicKey(pki.mcpServiceCertificate().getPublicKey());
        digitalSignatureCertificate.setRootCertificate(pki.mcpRootCertificate());

        // Return the output
        return digitalSignatureCertificate;
    }
}
