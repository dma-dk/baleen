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
package dk.dma.baleen.service.s124.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dk.dma.baleen.secom.security.MCPSecurityService;
import dk.dma.baleen.service.spi.DataSet;

/**
 * The exchange set the factory hands back is signed with whatever {@link S124ExchangeSetService} asked the PKI for, and
 * S-100 Part 15, clause 15-8.4, allows exactly one encoding of that signature: the ASN.1 DER {@code SEQUENCE} of the
 * ECDSA integers r and s. The raw r||s concatenation the JCA algorithm {@code SHA384withECDSAinP1363Format} returns
 * signs and verifies perfectly well within our own code, so nothing short of building a real exchange set notices that
 * an ECDIS following Part 15 cannot decode it. This builds one.
 */
class S124ExchangeSetSignatureTest {

    private static KeyPair keyPair;

    private static MCPSecurityService pki;

    @BeforeAll
    static void createSigningIdentity() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp384r1"));
        keyPair = generator.generateKeyPair();

        X500Principal subject = new X500Principal("CN=exchange set signature test");
        Instant now = Instant.now();
        X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(new JcaX509v3CertificateBuilder(
                subject, BigInteger.ONE, Date.from(now.minus(Duration.ofDays(1))), Date.from(now.plus(Duration.ofDays(1))),
                subject, keyPair.getPublic()).build(new JcaContentSignerBuilder("SHA384withECDSA").build(keyPair.getPrivate())));

        // Signs with the algorithm the service names, so the test sees the encoding the service actually ships.
        pki = mock(MCPSecurityService.class);
        when(pki.mcpServiceCertificate()).thenReturn(certificate);
        when(pki.mcpIntermediateCertificates()).thenReturn(List.of());
        when(pki.sign(anyString(), any())).thenAnswer(call -> {
            Signature signature = Signature.getInstance(call.getArgument(0));
            signature.initSign(keyPair.getPrivate());
            signature.update((byte[]) call.getArgument(1));
            return signature.sign();
        });
    }

    @Test
    void aWarningIsPackagedAndSignedInTheEncodingPart15Defines() throws Exception {
        byte[] exchangeSet = newService().createExchangeSet(List.of(warning("nw-343-26.gml")));

        assertThat(entryNames(exchangeSet)).contains("S100_ROOT/CATALOG.XML");
    }

    /**
     * The factory rejects a signature that is not the DER form, so the test above already fails when the service asks
     * for the P1363 variant. This says why in one line, rather than leaving the next reader to decode a stack trace.
     */
    @Test
    void theSigningAlgorithmProducesADerSequenceRatherThanTheRawRandS() throws Exception {
        byte[] signature = (byte[]) pki.sign(signingAlgorithm(), "the catalogue".getBytes(StandardCharsets.UTF_8));

        assertThat(signature[0]).as("the DER SEQUENCE tag S-100 Part 15, clause 15-8.4, requires").isEqualTo((byte) 0x30);
        assertThat(signature).as("96 bytes is the raw r||s concatenation, the IEEE P1363 form").hasSizeGreaterThan(96);
    }

    /** {@return the JCA algorithm the service signs an exchange set with} */
    private static String signingAlgorithm() throws Exception {
        var field = S124ExchangeSetService.class.getDeclaredField("ECDSA_384_SHA2_JCA_NAME");
        field.setAccessible(true);
        return (String) field.get(null);
    }

    private static S124ExchangeSetService newService() {
        return new S124ExchangeSetService(pki, "Danish Maritime Authority", "DK00", "IHO", "urn:mrn:dk:dma:s-124",
                "https://www.dma.dk", "");
    }

    /** {@return one of the S-124 datasets the other tests read, as the service takes them} */
    private static DataSet warning(String name) throws Exception {
        byte[] gml = Files.readAllBytes(Path.of("src/test/resources/datasets", name));
        return new DataSet() {

            @Override
            public UUID uuid() {
                return UUID.nameUUIDFromBytes(gml);
            }

            @Override
            public byte[] toByteArray() {
                return gml;
            }
        };
    }

    private static List<String> entryNames(byte[] zip) throws Exception {
        List<String> names = new java.util.ArrayList<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                names.add(entry.getName());
            }
        }
        return names;
    }
}
