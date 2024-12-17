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
package dk.dma.baleen.connector.spi;

import dk.dma.baleen.inventory.querying.InventoryQuery;
import dk.dma.baleen.inventory.querying.InventoryQueryResult;
import dk.dma.baleen.product.dto.DatasetUploadDto;

/**
 *
 */
public interface ConnectorContext {


    // What can go wrong
    // Unsupported dataproduct/ data product version
    // not valid xml/gml, certificate missing, but signature is there
    void update(DatasetUploadDto bundle);

    InventoryQueryResult query(InventoryQuery query);

    // For now we keep subscribers connector specific
//    void subscribeNonPersistent(Object observer);
}
