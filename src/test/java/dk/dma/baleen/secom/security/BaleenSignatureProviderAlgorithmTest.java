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
 * Pins that an incoming signature is accepted under either 384-bit ECDSA algorithm, whatever the filter claims
 * was declared.
 * <p>
 * The algorithm the filter passes cannot be trusted to be the sender's: only exchange-metadata-bearing envelopes
 * can declare one, and the filter envelopes - {@code EnvelopeGetFilterObject} on {@code /v2/object/search} among
 * them - have no field for it, so {@code SecomSignatureFilter} silently substitutes our own default. S-100
 * Part 15 mandates ECDSA-384-SHA2 while the GRAD reference stack signs SHA3-384; verifying with exactly one of
 * them rejects a correct signature from half the ecosystem - which is what
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
        return sign(jcaAlgorithm, CONTENT);
    }

    /** {@return a signature over the given bytes produced with the given JCA algorithm} */
    private static byte[] sign(String jcaAlgorithm, byte[] content) throws Exception {
        Signature signature = Signature.getInstance(jcaAlgorithm);
        signature.initSign(keyPair.getPrivate());
        signature.update(content);
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
    void aSha2SignatureVerifiesEvenUnderTheFiltersSha3Fallback() throws Exception {
        // The regression this guards: the filter substitutes our own SHA3-384 for envelopes that cannot declare
        // an algorithm, and verifying with only that rejected AMSA's good SHA-2 signatures on /v2/object/search.
        byte[] signature = sign("SHA384withECDSA");

        assertThat(provider.validateSignature(certificatePem, DigitalSignatureAlgorithmEnum.SHA3_384_WITH_ECDSA, signature,
                CONTENT)).isTrue();
    }

    @Test
    void algorithmsOutsideThe384FamilyAreNotBlindlyTried() throws Exception {
        // The accepted set is declared + the two 384-bit algorithms, nothing wider.
        byte[] signature = sign("SHA256withECDSA");

        assertThat(provider.validateSignature(certificatePem, DigitalSignatureAlgorithmEnum.SHA3_384_WITH_ECDSA, signature,
                CONTENT)).isFalse();
    }

    @Test
    void aDeclared256AlgorithmIsStillHonoured() throws Exception {
        // A sender that genuinely declares something outside the 384 family still verifies under its declaration.
        byte[] signature = sign("SHA256withECDSA");

        assertThat(provider.validateSignature(certificatePem, DigitalSignatureAlgorithmEnum.SHA2_256_WITH_ECDSA, signature,
                CONTENT)).isTrue();
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
    void aSignatureOverACsvRenderingVariantIsDiagnosticOnlyAndStillRejected() throws Exception {
        // The failure log reports when a signature would verify over an alternative CSV rendering - here the
        // certificate array without Arrays.toString brackets - but reporting must never become accepting.
        byte[] contentWithBrackets = "[MIICert].AB12CD.1756713600.".getBytes();
        byte[] signature = sign("SHA384withECDSA", "MIICert.AB12CD.1756713600.".getBytes());

        assertThat(provider.validateSignature(certificatePem, DigitalSignatureAlgorithmEnum.SHA2_384_WITH_ECDSA, signature,
                contentWithBrackets)).isFalse();
    }

    @Test
    void nothingPresentedIsNotAValidSignature() {
        assertThat(provider.validateSignature(null, DigitalSignatureAlgorithmEnum.SHA2_384_WITH_ECDSA, new byte[] { 1 },
                CONTENT)).isFalse();
        assertThat(provider.validateSignature(new String[0], DigitalSignatureAlgorithmEnum.SHA2_384_WITH_ECDSA,
                new byte[] { 1 }, CONTENT)).isFalse();
    }
}
