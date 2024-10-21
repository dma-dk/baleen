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
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.grad.secom.core.models.enums.ContainerTypeEnum;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

/**
 * The ContainerTypeEnum is sent as an integer as not as a string. Hence we need to teach Quarkus to convert it properly.
 * {@link dk.dma.baleen.rest.GetSecomApi} is the main effected class.
 */
@Provider
@ApplicationScoped
public class SECOMDataProductTypeConverterProvider implements ParamConverterProvider {

    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (rawType.equals(ContainerTypeEnum.class)) {
            return (ParamConverter<T>) new SECOMDataProductTypeConverter();
        }
        return null;
    }
}
