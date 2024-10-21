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
package dk.dma.baleen.service.secom;

import static java.util.Objects.requireNonNull;

import org.grad.secom.core.base.SecomCertificateProvider;
import org.grad.secom.core.base.SecomCompressionProvider;
import org.grad.secom.core.base.SecomEncryptionProvider;
import org.grad.secom.core.base.SecomSignatureProvider;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The configuration of the SECOM library.
 */
@ApplicationScoped
public final class SecomConfiguration {

    private final SecomCertificateProvider certificateProvider;

    SecomCompressionProvider compressionProvider;

    SecomEncryptionProvider encryptionProvider;

    private final SecomSignatureProvider signatureProvider;

    SecomConfiguration(SecomCertificateProvider certificateProvider, SecomSignatureProvider signatureProvider) {
        this.certificateProvider = requireNonNull(certificateProvider);
        this.signatureProvider = requireNonNull(signatureProvider);
    }

    public SecomCertificateProvider certificateProvider() {
        return requireNonNull(certificateProvider);
    }

    public SecomSignatureProvider signatureProvider() {
        return requireNonNull(signatureProvider);
    }
}
