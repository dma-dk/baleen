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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.grad.secom.core.models.CapabilityObject;
import org.grad.secom.core.models.ImplementedInterfaces;
import org.grad.secom.core.models.enums.ContainerTypeEnum;
import org.grad.secom.core.models.enums.SECOM_DataProductType;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import dk.baleen.s100.xmlbindings.s124.v1_0_0.utils.S124Utils;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.MessageSeriesIdentifierType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnPreamble;
import dk.dma.baleen.secom.serviceold.SecomSubscriberService;
import dk.dma.baleen.secom.serviceold.TransmissibleDatasetGenerator;
import dk.dma.baleen.secom.util.MRNToUUID;
import dk.dma.baleen.service.dto.DatasetUploadGmlDto;
import dk.dma.baleen.service.s124.NiordApiCaller;
import dk.dma.baleen.service.s124.S124SupportedVersions;
import dk.dma.baleen.service.s124.NiordApiCaller.Result;
import dk.dma.baleen.service.s124.model.S124DatasetInstanceEntity;
import dk.dma.baleen.service.s124.repository.S124DatasetInstanceRepository;
import dk.dma.baleen.service.s124.util.S124DatasetReader;
import dk.dma.baleen.service.spi.DataSet;
import dk.dma.baleen.service.spi.S100DataProductService;
import dk.dma.baleen.service.spi.S100DataProductType;

/**
 *
 */
@Service
public class S124Service extends S100DataProductService {

    @Autowired
    S124DatasetInstanceRepository repository;

    @Autowired
    SecomSubscriberService subscriberService;

    public S124Service() {
        super(S100DataProductType.S124);
    }

    /** {@inheritDoc} */
    @Override
    public List<CapabilityObject> secomCapabilities() {
        ArrayList<CapabilityObject> all = new ArrayList<>();

        for (S124SupportedVersions v : S124SupportedVersions.values()) {
            ImplementedInterfaces implementedInterfaces = new ImplementedInterfaces();
            implementedInterfaces.setGetSummary(true);
            implementedInterfaces.setGet(true);
            implementedInterfaces.setSubscription(true);

            CapabilityObject capabilityObject = new CapabilityObject();
            capabilityObject.setContainerType(ContainerTypeEnum.S100_DataSet);
            capabilityObject.setDataProductType(SECOM_DataProductType.S124);
            capabilityObject.setImplementedInterfaces(implementedInterfaces);
            capabilityObject.setServiceVersion(v.serviceVersion());

            all.add(capabilityObject);
        }
        return List.copyOf(all);
    }

    @Autowired
    NiordApiCaller niordApi;

    /** {@inheritDoc} */
    @Override
    public Page<? extends DataSet> findAll(@Nullable UUID uuid, Geometry geometry, LocalDateTime fromTime, LocalDateTime toTime, Pageable pageable) {
//        return repository.findDatasets(uuid, geometry, fromTime, toTime, pageable);
        return getStatic();
    }

    private Page<? extends DataSet> getStatic() {
        List<DataSet> l = new ArrayList<>();
        List<Result> fetchAll = niordApi.getIt();
        for (Result result : fetchAll) {
            l.add(new DataSet() {

                @Override
                public UUID uuid() {
                    return UUID.randomUUID();
                }

                @Override
                public byte[] toByteArray() {
                    return result.xml().getBytes(StandardCharsets.UTF_8);
                }
            });
        }
        // Set Data (Xml document)
        return new PageImpl<DataSet>(l);
    }

    /** {@inheritDoc} */
    @Override
    public void upload(DatasetUploadGmlDto d) throws Exception {
//        if (!d.dataProductVersion().equals(S124SupportedVersions.V1_0_0.productVersion())) {
//            throw new IllegalArgumentException(
//                    "Version " + d.dataProductVersion() + " not support for upload, supported versions=" + S124SupportedVersions.V1_0_0.serviceVersion());
//        }
        String gml = d.gml();
        upload(gml);
    }

    /** {@inheritDoc} */
    public void upload(String gml) throws Exception {
//        if (!d.dataProductVersion().equals(S124SupportedVersions.V1_0_0.productVersion())) {
//            throw new IllegalArgumentException(
//                    "Version " + d.dataProductVersion() + " not support for upload, supported versions=" + S124SupportedVersions.V1_0_0.serviceVersion());
//        }

        Dataset dataset = S124Utils.unmarshallS124(gml);

        // Debug: Log the incoming XML to see the structure
        System.out.println("DEBUG: Incoming XML (first 2000 chars):");
        System.out.println(gml.length() > 2000 ? gml.substring(0, 2000) + "..." : gml);
        
        // TODO check for existing

        // TODO we should have some kind of
        // Create new instance entity
        S124DatasetInstanceEntity entity = new S124DatasetInstanceEntity();

        // Set basic properties
        //entity.setDataProductVersion(d.dataProductVersion());
        entity.setDataProductVersion("1.0.0");

        // Convert geometries.
        Geometry geometry = S124DatasetReader.calculateGeometry(dataset);
        entity.setGeometry(geometry);

        // Store the original XML
        entity.setGml(gml);

        // Set validity
        NavwarnPreamble preamble = S124DatasetReader.findPreamble(dataset);
        
        // Debug: Log preamble details
        System.out.println("DEBUG: Preamble found: " + preamble);
        if (preamble != null && preamble.getMessageSeriesIdentifier() != null) {
            MessageSeriesIdentifierType identifier = preamble.getMessageSeriesIdentifier();
            System.out.println("DEBUG: MessageSeriesIdentifier details:");
            System.out.println("  - Agency: " + identifier.getAgencyResponsibleForProduction());
            System.out.println("  - Country: " + identifier.getNationality());
            System.out.println("  - WarningNumber: " + identifier.getWarningNumber());
            System.out.println("  - Year: " + identifier.getYear());
            System.out.println("  - WarningIdentifier: '" + identifier.getInteroperabilityIdentifier() + "'");
            System.out.println("  - NameOfSeries: " + identifier.getNameOfSeries());
        }

        String mrn = S124DatasetReader.toMRN(preamble.getMessageSeriesIdentifier());
        entity.setMrn(mrn);
        
        // Generate UUID from MRN instead of dataset ID to ensure uniqueness
        System.out.println("DEBUG: Dataset ID: " + dataset.getId());
        System.out.println("DEBUG: Using MRN for UUID: " + mrn);
        UUID uuid = MRNToUUID.createUUIDFromMRN(mrn);
        entity.setUuid(uuid);

        OffsetDateTime pd = preamble.getPublicationTime();
        if (pd != null) {
            entity.setValidFrom(pd.toInstant());
        }

        OffsetDateTime cd = preamble.getCancellationDate();
        if (cd != null) {
            entity.setValidTo(cd.toInstant());
        }
        // entity.setMrn(...); // Set Maritime Resource Name if available

        for (MessageSeriesIdentifierType m : S124DatasetReader.findAllReferences(dataset)) {
            String mrnRef = S124DatasetReader.toMRN(m);

            // Add reference to existing dataset if we know it.
            Optional<S124DatasetInstanceEntity> ref = repository.findByMrn(mrnRef);
            ref.ifPresent(entity::addReference);
        }

        // Save the entity
        repository.save(entity);

        subscriberService.publish(SECOM_DataProductType.S124, "1.0.0", uuid, geometry, new TransmissibleDatasetGenerator() {

            @Override
            protected byte[] createExchangeSet() {
                throw new UnsupportedOperationException();
            }

            @Override
            protected byte[] createDataset() {
                return gml.getBytes(StandardCharsets.UTF_8);
            }
        });
        // Notify subscripers.

        // Tror faktisk den skal vaere single threaded, og i samme transaction.

        /// Dataset (As string?), Product Type
        /// We probably have a special GML notification instead of a generic one
    }


//    /**
//     * @param doc
//     */
//    @Transactional
//    public void publish(String doc) {
//        List<SubscriptionEntity> list = sr.findAll().list();
//        System.out.println("Publish xml to " + list.size() + " subscribers");
//        for (SubscriptionEntity e : list) {
//            try {
//                publish(e, e.getMrn(), doc);
//            } catch (Exception e1) {
//                e1.printStackTrace();
//            }
//        }
//    }
//
//    void publish(SubscriptionEntity e, String mrn, String doc) throws Exception {
//        System.out.println("Publish to " + mrn);
//        // Build the data envelope
//        EnvelopeUploadObject envelopeUploadObject = new EnvelopeUploadObject();
//        envelopeUploadObject.setDataProductType(SECOM_DataProductType.S124);
//        envelopeUploadObject.setFromSubscription(true);
//        envelopeUploadObject.setAckRequest(AckRequestEnum.DELIVERED_ACK_REQUESTED);
//        envelopeUploadObject.setTransactionIdentifier(UUID.randomUUID());
//
//        envelopeUploadObject.setContainerType(ContainerTypeEnum.S100_DataSet);
//        // s125Dataset.getDatasetContent().getContent().getBytes()
//        envelopeUploadObject.setData(doc.getBytes());
//
//        // Set the envelope to the upload object
//        UploadObject uploadObject = new UploadObject();
//        uploadObject.setEnvelope(envelopeUploadObject);
//
//        SecomClient sc = finder.resolve(mrn);
//        System.out.println("Publish to host " + sc.baseUri);
//        sc.upload(uploadObject, null);
//    }
}
