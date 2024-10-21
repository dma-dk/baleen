/*
 * Copyright (c) 2024 Danish Maritime Authority.
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
package dk.dma.baleen.util.quarkus;

import org.grad.secom.core.models.enums.ContainerTypeEnum;

import jakarta.ws.rs.ext.ParamConverter;

public class SECOMDataProductTypeConverter implements ParamConverter<ContainerTypeEnum> {

    @Override
    public ContainerTypeEnum fromString(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            int int1 = Integer.parseInt(value);
            return ContainerTypeEnum.fromValue(int1);
        } catch (NumberFormatException e) {
            try {
                return ContainerTypeEnum.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException ee) {
                throw new IllegalArgumentException("Invalid SECOM_DataProductType value: " + value);
            }

        }
        // Custom logic to convert the string to SECOM_DataProductType
    }

    @Override
    public String toString(ContainerTypeEnum value) {
        return value != null ? String.valueOf(value.ordinal()) : null;
    }
}
