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
import java.util.Optional;

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
     * Verifies with the algorithm the sender said it signed with, rather than the one we sign with.
     * <p>
     * {@code SecomSignatureFilter} reads {@code digitalSignatureAlgorithm} off the envelope and passes it here, but
     * secom-v2 0.1.0 makes this a default method that drops it and calls the overload without an algorithm - so a
     * provider that only implements that overload silently verifies everything with its own. Ours is
     * {@link DigitalSignatureAlgorithmEnum#SHA3_384_WITH_ECDSA}, while S-100 Part 15 mandates ECDSA-384-SHA2, which
     * is a different hash family and fails to verify a perfectly good signature. That is what
     * {@code urn:mrn:mcp:device:mcc:amsa:s124-secom-client:prod} runs into on {@code /v2/object/search} once its
     * certificate is accepted.
     * <p>
     * A sender that declares nothing still gets our own algorithm, which is the previous behaviour. What we
     * <em>produce</em> is unchanged - {@link #getSignatureAlgorithm()} and {@link #generateSignature} still sign
     * with SHA3-384, so nothing about our outgoing messages moves.
     */
    @Override
    public boolean validateSignature(String[] signatureCertificates, DigitalSignatureAlgorithmEnum algorithm, byte[] signature,
            byte[] content) {
        if (signatureCertificates == null || signatureCertificates.length == 0) {
            return false;
        }
        DigitalSignatureAlgorithmEnum declared = Optional.ofNullable(algorithm).orElseGet(this::getSignatureAlgorithm);
        // Get the X.509 certificate from the request (first cert in chain is the signer)
        try {
            X509Certificate cert = SecomPemUtils.getCertFromPem(signatureCertificates[0]);
            Signature verification = Signature.getInstance(declared.getValue());
            verification.initVerify(cert);
            verification.update(content);
            boolean valid = verification.verify(signature);
            if (!valid) {
                LOGGER.warn("A {} signature from {} did not verify", declared.getValue(), cert.getSubjectX500Principal());
            }
            return valid;
        } catch (GeneralSecurityException ex) {
            LOGGER.error("Failed to validate an incoming message signed with {}", declared.getValue(), ex);
            throw new SecurityException(ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * secom-v2 0.1.0 made the algorithm-carrying overload a default method that delegates here, so this is the
     * fallback for a sender that declared no algorithm at all.
     */
    @Override
    public boolean validateSignature(String[] signatureCertificate, byte[] signature, byte[] content) {
        return validateSignature(signatureCertificate, getSignatureAlgorithm(), signature, content);
    }
}
