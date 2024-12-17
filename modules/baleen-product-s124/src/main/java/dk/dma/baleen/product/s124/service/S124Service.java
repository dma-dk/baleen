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
package dk.dma.baleen.product.s124.service;

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
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import dk.baleen.s100.xmlbindings.s124.v1_0_0.utils.S124Utils;
import dk.dma.baleen.connector.secom.util.MRNToUUID;
import dk.dma.baleen.product.dto.DatasetUploadGmlDto;
import dk.dma.baleen.product.s124.S124SupportedVersions;
import dk.dma.baleen.product.s124.model.S124DatasetInstanceEntity;
import dk.dma.baleen.product.s124.repository.S124DatasetInstanceRepository;
import dk.dma.baleen.product.s124.util.S124DatasetReader;
import dk.dma.baleen.product.spi.DataSet;
import dk.dma.baleen.product.spi.S100DataProductService;
import dk.dma.baleen.product.spi.S100DataProductType;
import dk.dma.baleen.s100.xmlbindings.s124.v1_0_0.Dataset;
import dk.dma.baleen.s100.xmlbindings.s124.v1_0_0.MessageSeriesIdentifierType;
import dk.dma.baleen.s100.xmlbindings.s124.v1_0_0.NAVWARNPreamble;

/**
 *
 */
@Service
public class S124Service extends S100DataProductService {

    @Autowired
    S124DatasetInstanceRepository repository;

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
        }
        return List.copyOf(all);
    }

    /** {@inheritDoc} */
    @Override
    public Page<? extends DataSet> findAll(@Nullable UUID uuid, Geometry geometry, LocalDateTime fromTime, LocalDateTime toTime, Pageable pageable) {
        return repository.findDatasets(uuid, geometry, fromTime, toTime, pageable);
    }

    /** {@inheritDoc} */
    @Override
    public void upload(DatasetUploadGmlDto d) throws Exception {
        if (!d.dataProductVersion().equals(S124SupportedVersions.V1_0_0.productVersion())) {
            throw new IllegalArgumentException(
                    "Version " + d.dataProductVersion() + " not support for upload, supported versions=" + S124SupportedVersions.V1_0_0.serviceVersion());
        }
        Dataset dataset = S124Utils.unmarshallS124(d.gml());

        UUID uuid = MRNToUUID.createUUIDFromMRN(dataset.getId());

        // TODO check for existing

        // TODO we should have some kind of
        // Create new instance entity
        S124DatasetInstanceEntity entity = new S124DatasetInstanceEntity();

        // Set basic properties
        entity.setDataProductVersion(d.dataProductVersion());
        entity.setUuid(uuid); // Generate new UUID for this instance

        // Convert geometries.
        Geometry geometry = S124DatasetReader.calculateGeometry(dataset);
        entity.setGeometry(geometry);

        // Store the original XML
        entity.setGml(d.gml());

        // Set validity
        NAVWARNPreamble preamble = S124DatasetReader.findPreamble(dataset);

        String mrn = S124DatasetReader.toMRN(preamble.getMessageSeriesIdentifier());

        entity.setMrn(mrn);

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

        // Notify subscripers.

        // Tror faktisk den skal vaere single threaded, og i samme transaction.

        /// Dataset (As string?), Product Type
        /// We probably have a special GML notification instead of a generic one
    }
}
