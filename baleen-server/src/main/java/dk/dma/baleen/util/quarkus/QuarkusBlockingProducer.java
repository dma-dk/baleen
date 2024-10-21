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

import java.io.IOException;

import org.grad.secom.core.base.SecomCompressionProvider;
import org.grad.secom.core.base.SecomEncryptionProvider;
import org.grad.secom.core.base.SecomSignatureProvider;
import org.grad.secom.core.base.SecomTrustStoreProvider;
import org.grad.secom.core.components.SecomSignatureFilter;

import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * This is needed in order to inject null into some services
 */
@ApplicationScoped
public class QuarkusBlockingProducer {

    @Produces
    @Unremovable
    public SecomSignatureFilter produceFoo(SecomCompressionProvider compressionProvider, SecomEncryptionProvider encryptionProvider,
            SecomTrustStoreProvider trustStoreProvider, SecomSignatureProvider signatureProvider) {
        return new BaleenSecomSignatureFilter(compressionProvider, encryptionProvider, trustStoreProvider, signatureProvider);
    }

    public static class BaleenSecomSignatureFilter extends SecomSignatureFilter {

        /**
         * @param compressionProvider
         * @param encryptionProvider
         * @param trustStoreProvider
         * @param signatureProvider
         */
        public BaleenSecomSignatureFilter(SecomCompressionProvider compressionProvider, SecomEncryptionProvider encryptionProvider,
                SecomTrustStoreProvider trustStoreProvider, SecomSignatureProvider signatureProvider) {
            super(compressionProvider, encryptionProvider, trustStoreProvider, signatureProvider);
        }

        /** {@inheritDoc} */
        @Override
        public void filter(ContainerRequestContext rqstCtx) throws IOException {
            // super.filter(rqstCtx);
            // final byte[] data = is.readAllBytes(); is blocking!!
        }
    }
}