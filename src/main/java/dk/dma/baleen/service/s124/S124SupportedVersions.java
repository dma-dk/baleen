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
package dk.dma.baleen.service.s124;

import static java.util.Objects.requireNonNull;

import dk.dma.baleen.service.spi.S100SupportedServiceVersions;

/**
 * The supported versions of S-124.
 * <p>
 * The actual supported versions via SECOM and Upload might differ at some point in the future.
 */
public enum S124SupportedVersions implements S100SupportedServiceVersions {
    V2_0_0("0.0.1", "2.0.0"); /* , V1_5_0("1.5.0"); */

    private final String serviceVersion;

    private final String productVersion;

    S124SupportedVersions(String serviceVersion, String productVersion) {
        this.serviceVersion = requireNonNull(serviceVersion);
        this.productVersion = requireNonNull(productVersion);
    }

    /** {@inheritDoc} */
    @Override
    public String serviceVersion() {
        return serviceVersion;
    }

    /** {@inheritDoc} */
    @Override
    public String productVersion() {
        return productVersion;
    }
}
