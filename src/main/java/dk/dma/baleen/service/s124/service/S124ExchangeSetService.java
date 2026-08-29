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

import static java.util.Objects.requireNonNull;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.grad.eNav.s100.utils.S100ExchangeSetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dk.dma.baleen.secom.security.MCPSecurityService;
import dk.dma.baleen.service.spi.DataSet;
import dk.dma.niord.s100.catalog._5_2.S100SEDigitalSignatureReference;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.DataSetIdentificationType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.exchangesets.S124ExchangeSetFactory;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util.S124Utils;
import jakarta.xml.bind.JAXBException;

/**
 * Packages S-124 datasets as S-100 Part 17 exchange sets, using the S124ExchangeSetFactory of the niord-xml-bindings
 * library. The library writes the catalogue and the ZIP, this class supplies the producer identity and the signing key.
 */
@Service
public class S124ExchangeSetService {

    /**
     * The JCA name of ECDSA-384-SHA2, the only algorithm S-100 Part 15, clause 15-8.7, allows. The P1363 variant is
     * required because the factory base64 encodes the raw R,S pair into the catalogue; the DER sequence that
     * {@code SHA384withECDSA} produces would be decoded as something else by a receiving system.
     */
    private static final String ECDSA_384_SHA2_JCA_NAME = "SHA384withECDSAinP1363Format";

    /** The logger of this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(S124ExchangeSetService.class);

    private final List<String> emails;

    private final String datasetMrnPrefix;

    private final String onlineResource;

    private final String organization;

    /** Provides the data server certificate and the private key the exchange set is signed with. */
    private final MCPSecurityService pki;

    private final String producerCode;

    private final String schemeAdministrator;

    /** The S-100 Part 17, clause 17-4.3, dataset file name pattern for our producer code. */
    private final Pattern datasetFileNamePattern;

    S124ExchangeSetService(MCPSecurityService pki,
            @Value("${baleen.exchange-set.organization:Danish Maritime Authority}") String organization,
            @Value("${baleen.exchange-set.producer-code:DK00}") String producerCode,
            @Value("${baleen.exchange-set.scheme-administrator:IHO}") String schemeAdministrator,
            @Value("${baleen.exchange-set.dataset-mrn-prefix:urn:mrn:dk:dma:s-124}") String datasetMrnPrefix,
            @Value("${baleen.exchange-set.online-resource:https://www.dma.dk}") String onlineResource,
            @Value("${baleen.exchange-set.emails:}") String emails) {
        this.pki = requireNonNull(pki);
        this.organization = organization;
        this.producerCode = producerCode;
        this.schemeAdministrator = schemeAdministrator;
        this.datasetMrnPrefix = datasetMrnPrefix;
        this.onlineResource = onlineResource.isBlank() ? null : onlineResource;
        this.emails = Arrays.stream(emails.split(",")).map(String::trim).filter(e -> !e.isEmpty()).toList();
        this.datasetFileNamePattern = Pattern.compile("124" + Pattern.quote(producerCode) + "[A-Za-z0-9]+\\.GML");
    }

    /**
     * {@return the datasets packaged and signed as a single S-100 Part 17 exchange set (a ZIP)}
     *
     * @param datasets
     *            the datasets to package, must not be empty
     */
    public byte[] createExchangeSet(List<? extends DataSet> datasets) {
        List<Dataset> parsed = new ArrayList<>();
        for (DataSet ds : datasets) {
            String gml = new String(ds.toByteArray(), StandardCharsets.UTF_8);
            Dataset dataset;
            try {
                dataset = S124Utils.unmarshallS124(gml);
            } catch (JAXBException e) {
                LOGGER.warn("Leaving dataset {} out of the exchange set, it could not be parsed", ds.uuid(), e);
                continue;
            }
            nameDatasetFile(dataset, ds);
            parsed.add(dataset);
        }
        if (parsed.isEmpty()) {
            throw new IllegalStateException("None of the " + datasets.size() + " datasets could be parsed, so there is no exchange set to build");
        }

        return packageWhatCanBePackaged(parsed, this::build);
    }

    /**
     * {@return the datasets packaged as one exchange set, leaving out any the packager will not accept}
     * <p>
     * The factory validates the datasets as a batch and stops at the first one it objects to, so a single bad warning
     * used to cost every other warning its delivery. Which ones it objects to is only discoverable by offering them
     * one at a time - the objection can come from the dataset, from the marshalled XML or from the discovery metadata
     * the catalogue derives from it, and only the factory knows about all three - which is far too expensive to do
     * routinely. So the batch is tried first and paid for only once it has failed.
     * <p>
     * Leaving a warning out is a poor outcome and is logged at error, but it is the better of the two on offer: the
     * alternative is that every warning in the batch goes undelivered.
     *
     * @param datasets
     *            the datasets to package
     * @param packager
     *            builds an exchange set of the datasets it is given, throwing if it will not accept one of them
     * @throws IllegalStateException
     *             if not one of the datasets can be packaged
     */
    static byte[] packageWhatCanBePackaged(List<Dataset> datasets, Function<List<Dataset>, byte[]> packager) {
        try {
            return packager.apply(datasets);
        } catch (RuntimeException e) {
            LOGGER.warn("The exchange set of {} datasets could not be built, retrying with the ones that can be packaged", datasets.size(), e);
            List<Dataset> packageable = new ArrayList<>();
            for (Dataset dataset : datasets) {
                try {
                    packager.apply(List.of(dataset));
                    packageable.add(dataset);
                } catch (RuntimeException rejected) {
                    LOGGER.error("Leaving dataset {} out of the exchange set: {}", dataset.getId(), rejected.getMessage());
                }
            }
            if (packageable.isEmpty()) {
                throw new IllegalStateException("None of the " + datasets.size() + " datasets could be packaged into an exchange set", e);
            }
            LOGGER.error("Serving an exchange set of {} datasets, {} could not be packaged and were left out", packageable.size(),
                    datasets.size() - packageable.size());
            return packager.apply(packageable);
        }
    }

    /** {@return the datasets packaged and signed as one exchange set} */
    private byte[] build(List<Dataset> datasets) {
        return S124ExchangeSetFactory.builder()
                .datasets(datasets)
                .organization(organization)
                .producerCode(producerCode)
                .schemeAdministrator(schemeAdministrator)
                .datasetMrnPrefix(datasetMrnPrefix)
                .certificatePem(toPem(pki.mcpServiceCertificate()))
                .intermediateCertificatePems(pki.mcpIntermediateCertificates().stream().map(S124ExchangeSetService::toPem).toList())
                .signer(this::sign)
                .onlineResource(onlineResource)
                .emails(emails)
                .build()
                .toBytes();
    }

    /**
     * Makes the dataset name the file it is about to be packaged in.
     *
     * S-100 Part 10b, Table 10b-4, defines datasetFileIdentifier as the name of the file the dataset is packaged in,
     * and Part 17, clause 17-4.3, names those files 124&lt;producer code&gt;&lt;unique code&gt;.GML. Producers such as Niord
     * put a human readable title there instead, which the exchange set factory rejects rather than repairs - it cannot
     * know whether the identifier or the file name is the one to keep. Here we do know: Baleen is the data server
     * packaging the dataset, so the header is rewritten to the name the file will have, before the factory marshals
     * and signs it. The unique code is derived the way the factory derives its own, from the dataset GML id, so the
     * same dataset always lands in the same file.
     */
    private void nameDatasetFile(Dataset dataset, DataSet source) {
        DataSetIdentificationType identification = dataset.getDatasetIdentificationInformation();
        if (identification == null) {
            return; // nothing declared, the factory names the file itself
        }
        String declared = identification.getDatasetFileIdentifier();
        if (declared != null && datasetFileNamePattern.matcher(declared).matches()) {
            return;
        }

        String id = dataset.getId() == null || dataset.getId().isBlank() ? source.uuid().toString() : dataset.getId();
        String uniqueCode = id.replaceAll("[^A-Za-z0-9]", "");
        String fileName = "124" + producerCode + uniqueCode + ".GML";
        LOGGER.debug("Renaming dataset file identifier '{}' to '{}' for the exchange set", declared, fileName);
        identification.setDatasetFileIdentifier(fileName);
    }

    private byte[] sign(S100SEDigitalSignatureReference algorithm, byte[] payload) {
        if (algorithm != S100SEDigitalSignatureReference.ECDSA_384_SHA_2) {
            throw new IllegalArgumentException("Cannot sign an exchange set with " + algorithm + ", only ECDSA-384-SHA2 is supported");
        }
        try {
            return pki.sign(ECDSA_384_SHA2_JCA_NAME, payload);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not sign the exchange set", e);
        }
    }

    /** {@return the certificate as the base64 encoded DER the S-100 protection scheme calls a PEM} */
    private static String toPem(X509Certificate certificate) {
        try {
            return new String(S100ExchangeSetUtils.getPemFromCert(certificate), StandardCharsets.UTF_8);
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException("Could not encode " + certificate.getSubjectX500Principal() + " for the exchange set", e);
        }
    }
}
