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
package internal.dk.dma.baleen.connector;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import dk.dma.baleen.connector.spi.AbstractConnector;
import dk.dma.baleen.connector.spi.ConnectorContext;
import dk.dma.baleen.inventory.querying.InventoryQuery;
import dk.dma.baleen.inventory.querying.InventoryQueryResult;
import dk.dma.baleen.product.dto.DatasetUploadDto;
import dk.dma.baleen.product.dto.DatasetUploadGmlDto;
import dk.dma.baleen.product.spi.S100DataProductService;

/**
 *
 */

// 3 Levels?
// Baleen Internal
// Baleen Extensions (Connectors and/or products)
// Baleen Embedded users

public class ConnectorManager {

    @Autowired
    private List<AbstractConnector> connectors;

    public void init() {
        for (AbstractConnector c : connectors) {
            c.initialize(new ConnectorContext() {

                @Override
                public void update(DatasetUploadDto bundle) {
                    update(bundle);
                }

                @Override
                public InventoryQueryResult query(InventoryQuery query) {
                    throw new UnsupportedOperationException();
                }
            });
        }
    }

    private void update(DatasetUploadDto bundle) {
        for (DatasetUploadGmlDto g : bundle.gmlDatasets()) {
            Optional<S100DataProductService> dg = pm.find(g.dataProduct());
         //   dg.get().findAll(null, null, null, null, false, null)
        }
    }

    @Autowired
    private S100DataProductManager pm;
}
