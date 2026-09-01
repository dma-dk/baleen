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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.grad.secomv2.core.models.enums.DigitalSignatureAlgorithmEnum;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Pins that an incoming signature is verified with the algorithm its sender declared.
 * <p>
 * {@code SecomSignatureFilter} reads {@code digitalSignatureAlgorithm} off the envelope and hands it to the
 * provider, but secom-v2 0.1.0 routes that through a default method which discards it. A provider implementing only
 * the algorithm-less overload therefore verifies everything with its own algorithm, and Baleen's is
 * SHA3-384-with-ECDSA while S-100 Part 15 mandates ECDSA-384-SHA2. SHA-3 and SHA-2 are different hash families, so
 * a correct signature simply fails - which is what
 * {@code urn:mrn:mcp:device:mcc:amsa:s124-secom-client:prod} hit on {@code /v2/object/search}.
 */
class BaleenSignatureProviderAlgorithmTest {

    private static final byte[] CONTENT = "dataReference.0.S-124.2.0.0".getBytes();

    private static KeyPair keyPair;

    private static String[] certificatePem;

    private static BaleenSignatureProvider provider;

    @BeforeAll
    static void createSigner() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp384r1"));
        keyPair = generator.generateKeyPair();

        X500Principal subject = new X500Principal("CN=signature algorithm test");
        Instant now = Instant.now();
        X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(new JcaX509v3CertificateBuilder(
                subject, BigInteger.ONE, Date.from(now.minus(Duration.ofDays(1))),
                Date.from(now.plus(Duration.ofDays(1))), subject, keyPair.getPublic())
                        .build(new JcaContentSignerBuilder("SHA384withECDSA").build(keyPair.getPrivate())));

        certificatePem = new String[] { Base64.getEncoder().encodeToString(certificate.getEncoded()) };

        // validateSignature never touches the PKI service - only signing does - so a stub keeps the test off disk.
        provider = new BaleenSignatureProvider(mock(MCPSecurityService.class));
    }

    /** {@return a signature over {@link #CONTENT} produced with the given JCA algorithm} */
    private static byte[] sign(String jcaAlgorithm) throws Exception {
        Signature signature = Signature.getInstance(jcaAlgorithm);
        signature.initSign(keyPair.getPrivate());
        signature.update(CONTENT);
        return signature.sign();
    }

    @Test
    void aSignatureIsVerifiedWithTheAlgorithmItsSenderDeclared() throws Exception {
        // What AMSA sends: ECDSA-384-SHA2, the algorithm S-100 Part 15 mandates.
        byte[] signature = sign("SHA384withECDSA");

        assertThat(provider.validateSignature(certificatePem, DigitalSignatureAlgorithmEnum.SHA2_384_WITH_ECDSA, signature,
                CONTENT)).isTrue();
    }

    @Test
    void theSameSignatureFailsUnderBaleensOwnAlgorithm() throws Exception {
        // The regression this guards: before the declared algorithm was honoured, every incoming signature was
        // verified as SHA3-384, so this good SHA-2 signature was rejected.
        byte[] signature = sign("SHA384withECDSA");

        assertThat(provider.validateSignature(certificatePem, DigitalSignatureAlgorithmEnum.SHA3_384_WITH_ECDSA, signature,
                CONTENT)).isFalse();
    }

    @Test
    void baleensOwnAlgorithmStillVerifies() throws Exception {
        // Widening what we accept must not drop what we already accepted.
        byte[] signature = sign("SHA3-384withECDSA");

        assertThat(provider.validateSignature(certificatePem, DigitalSignatureAlgorithmEnum.SHA3_384_WITH_ECDSA, signature,
                CONTENT)).isTrue();
    }

    @Test
    void aSenderThatDeclaresNothingFallsBackToOurAlgorithm() throws Exception {
        byte[] signature = sign("SHA3-384withECDSA");

        assertThat(provider.validateSignature(certificatePem, null, signature, CONTENT)).isTrue();
        assertThat(provider.validateSignature(certificatePem, signature, CONTENT)).isTrue();
    }

    @Test
    void aTamperedPayloadIsStillRejected() throws Exception {
        byte[] signature = sign("SHA384withECDSA");

        assertThat(provider.validateSignature(certificatePem, DigitalSignatureAlgorithmEnum.SHA2_384_WITH_ECDSA, signature,
                "dataReference.0.S-124.2.0.1".getBytes())).isFalse();
    }

    @Test
    void nothingPresentedIsNotAValidSignature() {
        assertThat(provider.validateSignature(null, DigitalSignatureAlgorithmEnum.SHA2_384_WITH_ECDSA, new byte[] { 1 },
                CONTENT)).isFalse();
        assertThat(provider.validateSignature(new String[0], DigitalSignatureAlgorithmEnum.SHA2_384_WITH_ECDSA,
                new byte[] { 1 }, CONTENT)).isFalse();
    }
}
