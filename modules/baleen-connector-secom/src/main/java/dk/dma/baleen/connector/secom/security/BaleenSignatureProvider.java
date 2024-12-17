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
package dk.dma.baleen.connector.secom.security;

import static java.util.Objects.requireNonNull;

import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.cert.X509Certificate;

import org.grad.secom.core.base.DigitalSignatureCertificate;
import org.grad.secom.core.base.SecomSignatureProvider;
import org.grad.secom.core.models.enums.DigitalSignatureAlgorithmEnum;
import org.grad.secom.core.utils.SecomPemUtils;
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
    public byte[] generateSignature(DigitalSignatureCertificate signatureCertificate, DigitalSignatureAlgorithmEnum algorithm, byte[] payload) {
        // Create a new signature to sign the provided content
        try {
            //
//          System.out.println("Algorithm " + algorithm.getValue());
//          System.out.println("Signature hash" +  Arrays.hashCode(signature));
//          System.out.println("Payload hash " + Arrays.hashCode(payload));
//
//          System.out.println("_-------- trying to validate");
//
//          sign = Signature.getInstance(algorithm.getValue());
//          String pem = SecomPemUtils.getMinifiedPemFromCert(SecomPKI.mcpServiceCertificate());
//          X509Certificate cert = SecomPemUtils.getCertFromPem(pem);
//          sign.initVerify(cert);
//          sign.update(payload);
//          System.out.println(sign.verify(signature));
            return pki.sign(algorithm.getValue(), payload);
        } catch (GeneralSecurityException ex) {
            LOGGER.error("Failed to sign outgoing message", ex);
            throw new SecurityException(ex);
        }
    }

    /** {@inheritDoc} */
    @Override
    public DigitalSignatureAlgorithmEnum getSignatureAlgorithm() {
        return DigitalSignatureAlgorithmEnum.SHA3_384_WITH_ECDSA;
    }

    /** {@inheritDoc} */
    @Override
    public boolean validateSignature(String signatureCertificate, DigitalSignatureAlgorithmEnum algorithm, byte[] signature, byte[] content) {
        // Get the X.509 certificate from the request
        try {
            // Get the client cert from the request
            X509Certificate cert = SecomPemUtils.getCertFromPem(signatureCertificate);
            Signature verification = Signature.getInstance(algorithm.getValue());
            verification.initVerify(cert);
            verification.update(content);
            return verification.verify(signature);
        } catch (GeneralSecurityException ex) {
            LOGGER.error("Failed to validate outgoing message", ex);
            throw new SecurityException(ex);
        }
    }
}
