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
package dk.dma.baleen.secom.service;

import java.time.Duration;
import java.util.UUID;

import org.grad.secom.core.models.enums.AckRequestEnum;
import org.grad.secom.core.models.enums.SECOM_DataProductType;
import org.springframework.stereotype.Service;

import dk.dma.baleen.secom.model.SecomSubscriberEntity;

/**
 *
 */
@Service
public class SecomUploadService {

    // ExchangeSet can have multiple product types
    public Uploader prepareUpload(SECOM_DataProductType productType) {
        throw new UnsupportedOperationException();
    }

    interface Uploader {
        Uploader ack(AckRequestEnum ack); // default is none-requested

        Uploader exchangeSet(); // Maybe this is mandatory

        Uploader fromSubscription(SecomSubscriberEntity subscriber);

        /**
         * @param data
         * @return the transaction id of the upload.
         *
         * @see SecomU
         */
        UUID upload(byte[] data);

        UUID uploadAsLink(byte[] data, Duration timeToLive);
    }
}
