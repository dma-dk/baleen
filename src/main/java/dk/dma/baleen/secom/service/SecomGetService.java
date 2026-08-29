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
package dk.dma.baleen.secom.service;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.grad.secomv2.core.exceptions.SecomNotImplementedException;
import org.grad.secomv2.core.models.enums.SECOM_DataProductType;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import dk.dma.baleen.secom.spi.AuthenticatedMcpNode;
import dk.dma.baleen.service.spi.DataSet;
import dk.dma.baleen.service.spi.S100DataProductService;
import dk.dma.baleen.service.spi.S100DataProductType;

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
            String unlocode, Geometry jtsGeometry, Instant validFrom, Instant validTo, Integer page, Integer pageSize) {
        S100DataProductService dataProduct = dataProduct(dataProductType);

        return dataProduct.findAll(dataReference, jtsGeometry, validFrom, validTo, pageable(page, pageSize));
    }

    /**
     * {@return the page the request asks for}
     * <p>
     * SECOM numbers pages from 1 - {@code GetServiceInterface#get} constrains the parameter with {@code @Min(1)} -
     * while {@link PageRequest} numbers them from 0, so the two disagree by one. Passing the SECOM number straight
     * through skipped the first page worth of datasets and never returned them.
     * <p>
     * A page size on its own used to be ignored, so a client asking for 10 datasets was sent every one that matched.
     * It now means the first page of that size, which is what asking for a size without a number can only mean.
     *
     * @param page
     *            the 1 based SECOM page number, or null for the first page
     * @param pageSize
     *            the maximum number of datasets on the page, or null for all of them
     */
    private static Pageable pageable(Integer page, Integer pageSize) {
        if (page == null && pageSize == null) {
            return Pageable.unpaged();
        }
        // Bean validation rejects a page below 1 before we get here; clamping rather than letting PageRequest throw
        // keeps a caller that bypasses validation on the first page instead of costing it a 500.
        int index = page == null ? 0 : Math.max(0, page - 1);
        return PageRequest.of(index, pageSize == null ? Integer.MAX_VALUE : pageSize);
    }

    /** {@return the datasets packaged as a single S-100 exchange set} */
    public byte[] createExchangeSet(SECOM_DataProductType dataProductType, List<? extends DataSet> datasets) {
        return dataProduct(dataProductType).createExchangeSet(datasets);
    }

    private S100DataProductService dataProduct(SECOM_DataProductType dataProductType) {
        S100DataProductType pt = switch (dataProductType) {
        case S124 -> S100DataProductType.S124;
        default -> throw new SecomNotImplementedException(dataProductType + " not supported, supported products: " + productManager.supportedProducts());
        };

        return productManager.find(pt)
                .orElseThrow(() -> new SecomNotImplementedException(dataProductType + " not supported, supported products: " + productManager.supportedProducts()));
    }
}
