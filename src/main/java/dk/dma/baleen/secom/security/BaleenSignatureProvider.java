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

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
     * Verifies with the declared algorithm and, failing that, with the other 384-bit ECDSA algorithm.
     * <p>
     * The {@code algorithm} handed to us is not necessarily the sender's: only envelopes that carry exchange
     * metadata can declare one ({@code EnvelopeSignatureBearer.getEnvelopeSignatureAlgorithm}), and the filter
     * envelopes - {@code EnvelopeGetFilterObject} on {@code /v2/object/search} among them - have no such field, so
     * for those {@code SecomSignatureFilter} always passes our own default. SECOM v2 CD3 deprecated the field that
     * used to carry it ({@code envelopeSignatureReference}), so the sender has no way to tell us. S-100 Part 15
     * mandates ECDSA-384-SHA2 while the GRAD reference stack signs SHA3-384, so both are in circulation and the
     * only interoperable verification is to accept either. The signature must still verify against the presented
     * certificate over the same bytes; which of two strong hashes it used grants nothing.
     * <p>
     * An envelope CSV is accepted in two renderings: exactly as the library computed it, and without the
     * CD3-deprecated {@code envelopeSignatureReference} trailing field. The library's {@code CsvStringGenerator}
     * still appends that field to every envelope CSV, but it is {@code @JsonIgnore} so an incoming request can
     * never populate it - it is always a trailing empty column that exists only in the GRAD rendering. A sender
     * whose envelope model post-dates the deprecation signs the same values without it, which is exactly what
     * AMSA's {@code s124-secom-client} does. Dropping a single trailing separator from otherwise-identical bytes
     * authenticates the same envelope fields. Data signatures - the filter routes those through this overload
     * too - get no such leeway: their content must match the signed bytes exactly, since anything else would
     * authenticate a modified payload.
     * <p>
     * When nothing verifies, this logs the exact CSV string the signature was checked against - the bytes we
     * computed from the sender's envelope - plus any algorithm/CSV-variant combination that <em>would</em> have
     * verified, so a failing peer can be diagnosed from the log alone. Those variants are log-only; they never
     * make a signature acceptable.
     * <p>
     * What we <em>produce</em> is unchanged - {@link #getSignatureAlgorithm()} and {@link #generateSignature} still
     * sign with SHA3-384 over the library's own rendering, so nothing about our outgoing messages moves.
     */
    @Override
    public boolean validateSignature(String[] signatureCertificates, DigitalSignatureAlgorithmEnum algorithm, byte[] signature,
            byte[] content) {
        if (signatureCertificates == null || signatureCertificates.length == 0) {
            return false;
        }
        DigitalSignatureAlgorithmEnum declared = Optional.ofNullable(algorithm).orElseGet(this::getSignatureAlgorithm);

        // Get the X.509 certificate from the request (first cert in chain is the signer)
        X509Certificate cert;
        try {
            cert = SecomPemUtils.getCertFromPem(signatureCertificates[0]);
        } catch (GeneralSecurityException ex) {
            LOGGER.error("Failed to read the certificate of an incoming message signed with {}", declared.getValue(), ex);
            throw new SecurityException(ex);
        }

        Set<DigitalSignatureAlgorithmEnum> accepted = new LinkedHashSet<>();
        accepted.add(declared);
        accepted.add(DigitalSignatureAlgorithmEnum.SHA2_384_WITH_ECDSA);
        accepted.add(DigitalSignatureAlgorithmEnum.SHA3_384_WITH_ECDSA);

        Map<String, byte[]> renderings = new LinkedHashMap<>();
        renderings.put("the CSV as this library renders it", content);
        // The trailing-field rendering is for envelope CSVs only. This same overload also verifies data
        // signatures over raw payload bytes (the filter's second call), where accepting content that differs
        // from the signed bytes - even by one appended byte - would authenticate a modified payload. An envelope
        // CSV provably embeds the presented certificate chain in the library's Arrays.toString rendering (the
        // filter builds both arguments from the same envelope field), so its presence distinguishes the two; a
        // payload only contains that rendering if its signer deliberately put it there.
        if (content.length > 0 && content[content.length - 1] == '.'
                && new String(content, StandardCharsets.UTF_8).contains(Arrays.toString(signatureCertificates))) {
            renderings.put("the CSV without the deprecated trailing envelopeSignatureReference field",
                    Arrays.copyOf(content, content.length - 1));
        }

        GeneralSecurityException firstFailure = null;
        boolean anyAttemptCompleted = false;
        for (Map.Entry<String, byte[]> rendering : renderings.entrySet()) {
            for (DigitalSignatureAlgorithmEnum candidate : accepted) {
                try {
                    if (verifies(cert, candidate.getValue(), signature, rendering.getValue())) {
                        if (candidate != declared || rendering.getValue() != content) {
                            LOGGER.info("A signature from {} verified with {} over {}, not the {} over the library rendering the filter assumed",
                                    cert.getSubjectX500Principal(), candidate.getValue(), rendering.getKey(), declared.getValue());
                        }
                        return true;
                    }
                    anyAttemptCompleted = true;
                } catch (GeneralSecurityException ex) {
                    firstFailure = firstFailure == null ? ex : firstFailure;
                }
            }
        }
        if (!anyAttemptCompleted && firstFailure != null) {
            LOGGER.error("Failed to validate an incoming message signed with {}", declared.getValue(), firstFailure);
            throw new SecurityException(firstFailure);
        }

        logVerificationFailure(cert, signatureCertificates, signature, content, accepted);
        return false;
    }

    /**
     * Logs everything needed to diagnose a signature that did not verify. For an envelope signature that is the
     * exact CSV we computed - which is the half of the comparison we own; the other half is whatever the sender
     * actually signed - and any algorithm/CSV rendering under which the signature would have verified. The
     * renderings cover the Java-specific choices in the library's {@code CsvStringGenerator} that another
     * implementation would plausibly make differently: {@code Arrays.toString} putting {@code [...]} around the
     * certificate array, and the CD3-deprecated {@code envelopeSignatureReference} contributing a trailing empty
     * field.
     * <p>
     * A failed <em>data</em> signature - recognised, as in {@code validateSignature}, by the content not embedding
     * the presented certificate chain - is a decoded, possibly decrypted, payload that must not end up in the log;
     * it is reported by length and digest only, and the CSV rendering sweep does not apply to it.
     */
    private void logVerificationFailure(X509Certificate cert, String[] signatureCertificates, byte[] signature,
            byte[] content, Set<DigitalSignatureAlgorithmEnum> accepted) {
        String csv = new String(content, StandardCharsets.UTF_8);
        var algorithms = accepted.stream().map(DigitalSignatureAlgorithmEnum::getValue).toList();
        if (!csv.contains(Arrays.toString(signatureCertificates))) {
            LOGGER.warn("A data signature from {} over {} bytes (SHA-256 {}) did not verify with any of {}",
                    cert.getSubjectX500Principal(), content.length, sha256Hex(content), algorithms);
            return;
        }
        LOGGER.warn("A signature from {} did not verify with any of {}. The CSV this server computed and checked it against was\n>>>{}<<<",
                cert.getSubjectX500Principal(), algorithms, csv);

        Map<String, String> variants = new LinkedHashMap<>();
        variants.put("the CSV as computed", csv);
        if (csv.endsWith(".")) {
            variants.put("the CSV without the deprecated trailing envelopeSignatureReference field", csv.substring(0, csv.length() - 1));
        }
        if (csv.contains("[")) {
            String stripped = csv.replace("[", "").replace("]", "");
            variants.put("the CSV without brackets around the certificate array", stripped);
            if (stripped.endsWith(".")) {
                variants.put("the CSV without brackets and without the trailing field",
                        stripped.substring(0, stripped.length() - 1));
            }
        }
        for (Map.Entry<String, String> variant : variants.entrySet()) {
            for (DigitalSignatureAlgorithmEnum candidate : DigitalSignatureAlgorithmEnum.values()) {
                // The DSA and CVC-ECDSA algorithm names are not resolvable in a standard JCA, and no MCP peer uses them
                if (candidate == DigitalSignatureAlgorithmEnum.DSA || candidate == DigitalSignatureAlgorithmEnum.CVC_ECDSA) {
                    continue;
                }
                try {
                    if (verifies(cert, candidate.getValue(), signature, variant.getValue().getBytes(StandardCharsets.UTF_8))) {
                        LOGGER.warn("DIAGNOSTIC ONLY: the failed signature from {} WOULD verify with {} over {}",
                                cert.getSubjectX500Principal(), candidate.getValue(), variant.getKey());
                    }
                } catch (GeneralSecurityException ignored) {
                    // a candidate that cannot even be attempted proves nothing
                }
            }
        }
    }

    /** {@return whether the signature verifies against the certificate over the given bytes with the given JCA algorithm} */
    private static boolean verifies(X509Certificate cert, String jcaAlgorithm, byte[] signature, byte[] content)
            throws GeneralSecurityException {
        Signature verification = Signature.getInstance(jcaAlgorithm);
        verification.initVerify(cert);
        verification.update(content);
        return verification.verify(signature);
    }

    /** {@return the SHA-256 digest of the given bytes as hex, identifying a payload without disclosing it} */
    private static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            return "unavailable";
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
