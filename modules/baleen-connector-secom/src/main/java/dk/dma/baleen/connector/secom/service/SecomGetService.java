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
package dk.dma.baleen.connector.secom.service;

import static java.util.Objects.requireNonNull;

import java.time.LocalDateTime;
import java.util.UUID;

import org.grad.secom.core.exceptions.SecomNotImplementedException;
import org.grad.secom.core.models.enums.SECOM_DataProductType;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import dk.dma.baleen.connector.secom.spi.AuthenticatedMcpNode;
import dk.dma.baleen.product.spi.DataSet;
import dk.dma.baleen.product.spi.S100DataProductService;
import dk.dma.baleen.product.spi.S100DataProductType;
import internal.dk.dma.baleen.connector.S100DataProductManager;

/**
 * Handles the SECOM get operation.
 */
@Service
public class SecomGetService {

    private final S100DataProductManager productManager;

    @Autowired
    public SecomGetService(S100DataProductManager productManager) {
        this.productManager = requireNonNull(productManager);
    }

    public Page<? extends DataSet> get(AuthenticatedMcpNode remoteNode, UUID dataReference, SECOM_DataProductType dataProductType, String productVersion, String geometry,
            String unlocode, Geometry jtsGeometry, LocalDateTime validFrom, LocalDateTime validTo, Integer page, Integer pageSize) {
        S100DataProductType pt = switch (dataProductType) {
        case S124 -> S100DataProductType.S124;
        case S125 -> S100DataProductType.S125;
        default -> throw new SecomNotImplementedException(dataProductType + " not supported, supported products: " + productManager.supportedProducts());
        };

        S100DataProductService dataProduct = productManager.find(pt)
                .orElseThrow(() -> new SecomNotImplementedException(dataProductType + " not supported, supported products: " + productManager.supportedProducts()));

        Pageable pageable = Pageable.unpaged();
        if (page != null) {
            int size = (pageSize != null) ? pageSize : Integer.MAX_VALUE;
            pageable = PageRequest.of(page, size);
        }

        return dataProduct.findAll(dataReference, jtsGeometry, validFrom, validTo, pageable);
    }
}
