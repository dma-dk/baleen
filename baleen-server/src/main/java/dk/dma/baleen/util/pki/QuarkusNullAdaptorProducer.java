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
package dk.dma.baleen.util.pki;

import org.grad.secom.core.base.SecomCompressionProvider;
import org.grad.secom.core.base.SecomEncryptionProvider;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * This is needed in order to inject null into various services in org.grad.secom.core.components.
 */
@ApplicationScoped
public class QuarkusNullAdaptorProducer {

    @Produces
    public SecomEncryptionProvider produceNullEncryptionProvider() {
        return null;
    }

    @Produces
    public SecomCompressionProvider produceNullCompressionsProvider() {
        return null;
    }
}