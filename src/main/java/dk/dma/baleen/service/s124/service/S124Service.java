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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.grad.secomv2.core.models.CapabilityObject;
import org.grad.secomv2.core.models.ImplementedInterfaces;
import org.grad.secomv2.core.models.enums.ContainerTypeEnum;
import org.grad.secomv2.core.models.enums.SECOM_DataProductType;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    /** The logger of this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(S124Service.class);

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
            // Datasets are served as plain GML and packaged into S-100 Part 17 exchange sets, one capability each.
            for (ContainerTypeEnum containerType : List.of(ContainerTypeEnum.S100_DataSet, ContainerTypeEnum.S100_ExchangeSet)) {
                ImplementedInterfaces implementedInterfaces = new ImplementedInterfaces();
                implementedInterfaces.setGetSummary(true);
                implementedInterfaces.setGet(true);
                implementedInterfaces.setSubscription(true);

                CapabilityObject capabilityObject = new CapabilityObject();
                capabilityObject.setContainerType(containerType);
                capabilityObject.setDataProductType(SECOM_DataProductType.S124);
                capabilityObject.setImplementedInterfaces(implementedInterfaces);
                capabilityObject.setServiceVersion(v.serviceVersion());

                all.add(capabilityObject);
            }
        }
        return List.copyOf(all);
    }

    @Autowired
    NiordApiCaller niordApi;

    @Autowired
    S124ExchangeSetService exchangeSetService;

    /** {@inheritDoc} */
    @Override
    public byte[] createExchangeSet(List<? extends DataSet> datasets) {
        return exchangeSetService.createExchangeSet(datasets);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The datasets are the ones {@link NiordApiCaller} keeps current, not the ones {@link #upload(String)} has stored:
     * nothing feeds the store on its own, so serving from it would hand out whatever was last loaded by hand. The
     * store therefore still has {@code findDatasets} for when something does keep it in sync; until then this is where
     * the current warnings are, and the query has to be answered against them.
     */
    @Override
    public Page<? extends DataSet> findAll(@Nullable UUID uuid, @Nullable Geometry geometry, @Nullable Instant fromTime, @Nullable Instant toTime,
            @Nullable Pageable pageable) {
        List<NiordDataSet> matching = datasets().stream().filter(ds -> ds.matches(uuid, geometry, fromTime, toTime)).toList();
        return page(matching, pageable);
    }

    /**
     * {@return the page of {@code matching} that {@code pageable} asks for}
     * <p>
     * The total is the number of matches rather than the size of the page, so a client that pages knows how much is
     * still to come.
     */
    private static <T> Page<T> page(List<T> matching, @Nullable Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return new PageImpl<>(matching);
        }
        long offset = pageable.getOffset();
        if (offset >= matching.size()) {
            return new PageImpl<>(List.of(), pageable, matching.size());
        }
        int from = (int) offset;
        int to = (int) Math.min(matching.size(), offset + pageable.getPageSize());
        return new PageImpl<>(matching.subList(from, to), pageable, matching.size());
    }

    /** The datasets last polled from Niord, and the poll they were derived from. */
    private volatile List<NiordDataSet> view = List.of();

    private volatile List<Result> viewSource;

    /**
     * {@return the current Niord datasets, in a form the query can be answered against}
     * <p>
     * Deriving a data reference, an extent and a validity period means parsing the dataset, which is too much to redo
     * for every request. {@link NiordApiCaller} replaces its whole list on each poll, so the list it hands back
     * identifies the poll it came from and the derived view can be reused until that changes. Two requests arriving
     * together on a fresh poll may both derive it, which costs a little work and no correctness.
     */
    private List<NiordDataSet> datasets() {
        List<Result> source = niordApi.getIt();
        if (source == null) {
            return List.of();
        }
        if (source != viewSource) {
            view = source.stream().map(NiordDataSet::of).toList();
            viewSource = source;
        }
        return view;
    }

    /**
     * A dataset as Niord serves it, with the attributes a SECOM query filters on read out of it once.
     *
     * @param uuid
     *            the data reference, derived from the warning's MRN so that a reference taken from a summary can be
     *            used to get the dataset back - which a fresh random id could not
     * @param gml
     *            the dataset as Niord served it, byte for byte
     * @param geometry
     *            the extent of the warning, or null if it declares none or we could not read it
     * @param validity
     *            the period the warning is in force, or null if we could not read it - which is not the same as a
     *            warning that declares no bounds, and must not be read as one
     */
    private record NiordDataSet(UUID uuid, byte[] gml, @Nullable Geometry geometry, @Nullable Validity validity) implements DataSet {

        static NiordDataSet of(Result result) {
            byte[] gml = result.xml().getBytes(StandardCharsets.UTF_8);
            Dataset dataset = result.dataset();
            // Read once and share: each dimension fails on its own, so a dataset whose geometry we cannot parse still
            // answers time queries from a preamble that reads perfectly well, and the other way round.
            NavwarnPreamble preamble = preambleOf(dataset);
            return new NiordDataSet(uuidOf(dataset, preamble, gml), gml, extentOf(dataset), validityOf(preamble));
        }

        /** {@return the warning's preamble, or null if the dataset does not carry exactly one we can read} */
        private static NavwarnPreamble preambleOf(Dataset dataset) {
            try {
                return S124DatasetReader.findPreamble(dataset);
            } catch (RuntimeException e) {
                LOGGER.warn("Could not read the preamble of dataset {}, so neither its validity nor its MRN is known",
                        dataset.getId(), e);
                return null;
            }
        }

        /** {@return the extent of the warning, or null if it declares none or we could not read it} */
        private static Geometry extentOf(Dataset dataset) {
            try {
                return S124DatasetReader.calculateGeometry(dataset);
            } catch (RuntimeException e) {
                LOGGER.warn("Could not read the extent of dataset {}, so it will not match a query naming an area",
                        dataset.getId(), e);
                return null;
            }
        }

        /**
         * {@return the period the warning is in force, or null if that is unknown}
         * <p>
         * A warning that declares no publication time or no cancellation is open at that end, which is a statement
         * about the warning. A preamble we could not read is not: it says nothing, and null keeps the two apart.
         */
        private static Validity validityOf(@Nullable NavwarnPreamble preamble) {
            if (preamble == null) {
                return null;
            }
            OffsetDateTime published = preamble.getPublicationTime();
            OffsetDateTime cancelled = preamble.getCancellationDate();
            return new Validity(published == null ? null : published.toInstant(), cancelled == null ? null : cancelled.toInstant());
        }

        /**
         * {@return a data reference that is the same every time for the same warning}
         * <p>
         * The MRN names the warning, so the reference derived from it survives a poll, a restart and a redeploy. A
         * dataset we cannot get an MRN out of falls back to its own content, which is stable for as long as the
         * warning is unchanged - still better than the random id this used to hand out, which made every reference a
         * summary reported unusable in the get that followed.
         */
        private static UUID uuidOf(Dataset dataset, @Nullable NavwarnPreamble preamble, byte[] gml) {
            try {
                if (preamble != null && preamble.getMessageSeriesIdentifier() != null) {
                    return MRNToUUID.createUUIDFromMRN(S124DatasetReader.toMRN(preamble.getMessageSeriesIdentifier()));
                }
            } catch (Exception e) {
                LOGGER.warn("Could not derive a data reference from the MRN of dataset {}, falling back to its content", dataset.getId(), e);
            }
            return UUID.nameUUIDFromBytes(gml);
        }

        /** {@return whether this dataset satisfies every dimension the query constrains} */
        boolean matches(@Nullable UUID uuid, @Nullable Geometry geometry, @Nullable Instant fromTime, @Nullable Instant toTime) {
            if (uuid != null && !uuid.equals(this.uuid)) {
                return false;
            }
            // A dataset that declares no extent cannot be shown to lie inside the area of interest, so it does not
            // match a query that names one - the same answer a spatial store gives for a null geometry.
            if (geometry != null && (this.geometry == null || !geometry.intersects(this.geometry))) {
                return false;
            }
            if (fromTime == null && toTime == null) {
                return true;
            }
            // A period was asked for and we could not read this warning's own, so there is no honest way to say it
            // falls inside one. Answering yes would put a warning that may have been cancelled years ago in front of
            // a mariner asking what is in force now, which is the worse of the two mistakes.
            return validity != null && validity.overlaps(fromTime, toTime);
        }

        @Override
        public byte[] toByteArray() {
            return gml;
        }
    }

    /**
     * The period a warning is in force, as the warning itself declares it.
     *
     * @param from
     *            when it was published, or null if it does not say and so has always been in force
     * @param to
     *            when it was cancelled, or null if it is still in force
     */
    private record Validity(@Nullable Instant from, @Nullable Instant to) {

        /**
         * {@return whether this period overlaps the one asked about}
         * <p>
         * An end that neither side declares is open, and an open end cannot exclude anything.
         */
        boolean overlaps(@Nullable Instant fromTime, @Nullable Instant toTime) {
            if (fromTime != null && to != null && to.isBefore(fromTime)) {
                return false;
            }
            return !(toTime != null && from != null && from.isAfter(toTime));
        }
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
                return exchangeSetService.createExchangeSet(List.of(entity));
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
