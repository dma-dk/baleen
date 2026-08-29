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
package dk.dma.baleen.service.spi;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.grad.secomv2.core.exceptions.SecomNotImplementedException;
import org.grad.secomv2.core.models.CapabilityObject;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

import dk.dma.baleen.service.dto.DatasetUploadGmlDto;

/**
 *
 */
public abstract class S100DataProductService {

    public final S100DataProductType type;

    protected S100DataProductService(S100DataProductType type) {
        this.type = type;
    }

    public abstract List<CapabilityObject> secomCapabilities();

    /**
     * {@return the datasets matching the query, as one page}
     * <p>
     * A null argument means the query does not constrain that dimension. The times bound a period of interest, and a
     * dataset matches when its own validity period overlaps it; a dataset whose validity is open at either end extends
     * to infinity there.
     *
     * @param uuid
     *            the data reference of a single dataset, or null for all of them
     * @param geometry
     *            the area of interest, or null for anywhere
     * @param fromTime
     *            the start of the period of interest, or null for the beginning of time
     * @param toTime
     *            the end of the period of interest, or null for the end of time
     * @param pageable
     *            the page to return, or null for all matches in one page
     */
    public Page<? extends DataSet> findAll(@Nullable UUID uuid, @Nullable Geometry geometry, @Nullable Instant fromTime, @Nullable Instant toTime,
            @Nullable Pageable pageable) {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     * {@return the datasets packaged as a single S-100 exchange set}
     *
     * @param datasets
     *            the datasets to package, must not be empty
     */
    public byte[] createExchangeSet(List<? extends DataSet> datasets) {
        throw new SecomNotImplementedException(type + " does not support exchange sets");
    }

    public abstract void upload(DatasetUploadGmlDto d) throws Exception;
}
