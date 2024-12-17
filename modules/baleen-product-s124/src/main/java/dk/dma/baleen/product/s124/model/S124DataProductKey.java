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
package dk.dma.baleen.product.s124.model;

import java.util.UUID;

import dk.dma.baleen.s100.xmlbindings.s124.v1_0_0.Dataset;

/**
 * We do not currently have a unique key (MRN) for a S-124 dataset so we use a composite key.
 */
public class S124DataProductKey {

    public String toMNR() {
        return "urn:mrn";
    }

    public UUID toUUID() {
        throw new UnsupportedOperationException();
    }

    public S124DataProductKey of(Dataset dateset) {
        throw new UnsupportedOperationException();
    }
}
