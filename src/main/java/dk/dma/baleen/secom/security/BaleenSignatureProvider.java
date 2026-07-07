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

import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.cert.X509Certificate;

import org.grad.secomv2.core.base.DigitalSignatureCertificate;
import org.grad.secomv2.core.base.SecomSignatureProvider;
import org.grad.secomv2.core.models.enums.DigitalSignatureAlgorithmEnum;
import org.grad.secomv2.core.utils.SecomPemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The SECOM Signature Provider Implementation. */
public class BaleenSignatureProvider implements SecomSignatureProvider {

    /** The logger of this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BaleenSignatureProvider.class);

    private final MCPSecurityService pki;

    /**
     * @param pki
     */
    public BaleenSignatureProvider(MCPSecurityService pki) {
        this.pki = requireNonNull(pki);
    }

    /** {@inheritDoc} */
    @Override
    public DigitalSignatureAlgorithmEnum getSignatureAlgorithm() {
        return DigitalSignatureAlgorithmEnum.SHA3_384_WITH_ECDSA;
    }

    /**
     * {@inheritDoc}
     *
     * secom-v2 0.1.0 made the algorithm-carrying overload a default method that delegates
     * here; the algorithm is taken from {@link #getSignatureAlgorithm()}.
     */
    @Override
    public byte[] generateSignature(DigitalSignatureCertificate signatureCertificate, byte[] payload) {
        try {
            return pki.sign(getSignatureAlgorithm().getValue(), payload);
        } catch (GeneralSecurityException ex) {
            LOGGER.error("Failed to sign outgoing message", ex);
            throw new SecurityException(ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * secom-v2 0.1.0 made the algorithm-carrying overload a default method that delegates
     * here; the algorithm is taken from {@link #getSignatureAlgorithm()}.
     */
    @Override
    public boolean validateSignature(String[] signatureCertificate, byte[] signature, byte[] content) {
        if (signatureCertificate == null || signatureCertificate.length == 0) {
            return false;
        }
        // Get the X.509 certificate from the request (first cert in chain is the signer)
        try {
            X509Certificate cert = SecomPemUtils.getCertFromPem(signatureCertificate[0]);
            Signature verification = Signature.getInstance(getSignatureAlgorithm().getValue());
            verification.initVerify(cert);
            verification.update(content);
            return verification.verify(signature);
        } catch (GeneralSecurityException ex) {
            LOGGER.error("Failed to validate outgoing message", ex);
            throw new SecurityException(ex);
        }
    }
}
