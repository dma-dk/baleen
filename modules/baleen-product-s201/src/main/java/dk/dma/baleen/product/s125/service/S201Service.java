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
package dk.dma.baleen.product.s125.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

import _int.iho.s201.gml.cs0._1.Dataset;
import dk.dma.baleen.iho.s201.S201SupportedVersions;
import dk.dma.baleen.iho.s201.model.S201DatasetInstanceEntity;
import dk.dma.baleen.iho.s201.repository.S201DatasetInstanceRepository;
import dk.dma.baleen.product.dto.DatasetUploadGmlDto;
import dk.dma.baleen.product.spi.DataSet;
import dk.dma.baleen.product.spi.S100DataProductService;
import dk.dma.baleen.product.spi.S100DataProductType;
import dk.dma.baleen.xmlbindings.s201.utils.S201Utils;

/**
 *
 */
@Service
public class S201Service extends S100DataProductService {

    @Autowired
    S201DatasetInstanceRepository repository;

    public S201Service() {
        super(S100DataProductType.S201);
    }

    /** {@inheritDoc} */
    @Override
    public List<CapabilityObject> secomCapabilities() {
        ArrayList<CapabilityObject> all = new ArrayList<>();

        for (S201SupportedVersions v : S201SupportedVersions.values()) {
            ImplementedInterfaces implementedInterfaces = new ImplementedInterfaces();
            implementedInterfaces.setGetSummary(true);
            implementedInterfaces.setGet(true);
            implementedInterfaces.setSubscription(true);

            CapabilityObject capabilityObject = new CapabilityObject();
            capabilityObject.setContainerType(ContainerTypeEnum.S100_DataSet);
            capabilityObject.setDataProductType(SECOM_DataProductType.S201);
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
        if (!d.dataProductVersion().equals(S201SupportedVersions.V0_0_1.productVersion())) {
            throw new IllegalArgumentException(
                    "Version " + d.dataProductVersion() + " not support for upload, supported versions=" + S201SupportedVersions.V0_0_1.serviceVersion());
        }
        Dataset dataset = S201Utils.unmarshallS201(d.gml());

        UUID uuid = UUID.randomUUID();

        // TODO check for existing

        // TODO we should have some kind of
        // Create new instance entity
        S201DatasetInstanceEntity entity = new S201DatasetInstanceEntity();

        // Set basic properties
        entity.setDataProductVersion(d.dataProductVersion());
        entity.setUuid(uuid); // Generate new UUID for this instance

        // Store the original XML
        entity.setGml(d.gml());

        // Save the entity
        repository.save(entity);

        // Notify subscripers.

        // Tror faktisk den skal vaere single threaded, og i samme transaction.

        /// Dataset (As string?), Product Type
        /// We probably have a special GML notification instead of a generic one
    }
}