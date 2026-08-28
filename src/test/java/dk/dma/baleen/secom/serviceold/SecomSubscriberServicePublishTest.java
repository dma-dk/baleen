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
package dk.dma.baleen.secom.serviceold;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.grad.secomv2.core.models.UploadObject;
import org.grad.secomv2.core.models.enums.ContainerTypeEnum;
import org.grad.secomv2.core.models.enums.SECOM_DataProductType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import dk.dma.baleen.secom.model.SecomNodeEntity;
import dk.dma.baleen.secom.model.SecomSubscriberEntity;
import dk.dma.baleen.secom.repository.SecomSubscriberRepository;
import dk.dma.baleen.secom.serviceold.SecomOutboxService.SecomOperationType;

/**
 * The container type a subscriber signed up for decides what it is sent, since S-124 advertises a subscription
 * capability for both datasets and exchange sets.
 */
class SecomSubscriberServicePublishTest {

    private static final byte[] DATASET = "<Dataset/>".getBytes(StandardCharsets.UTF_8);

    private static final byte[] EXCHANGE_SET = "PK-zipped-exchange-set".getBytes(StandardCharsets.UTF_8);

    private static final TransmissibleDatasetGenerator GENERATOR = new TransmissibleDatasetGenerator() {

        @Override
        protected byte[] createDataset() {
            return DATASET;
        }

        @Override
        protected byte[] createExchangeSet() {
            return EXCHANGE_SET;
        }
    };

    @Test
    void exchangeSetSubscriberIsSentAnExchangeSet() {
        UploadObject uploaded = publishTo(subscriber("urn:mrn:test:exchange-set", ContainerTypeEnum.S100_ExchangeSet), GENERATOR);

        assertThat(uploaded.getEnvelope().getContainerType()).isEqualTo(ContainerTypeEnum.S100_ExchangeSet);
        assertThat(uploaded.getEnvelope().getData()).isEqualTo(EXCHANGE_SET);
    }

    @Test
    void datasetSubscriberIsSentADataset() {
        UploadObject uploaded = publishTo(subscriber("urn:mrn:test:dataset", ContainerTypeEnum.S100_DataSet), GENERATOR);

        assertThat(uploaded.getEnvelope().getContainerType()).isEqualTo(ContainerTypeEnum.S100_DataSet);
        assertThat(uploaded.getEnvelope().getData()).isEqualTo(DATASET);
    }

    /** Subscriptions taken before the container type was recorded were all served datasets. */
    @Test
    void subscriberWithoutAContainerTypeIsSentADataset() {
        UploadObject uploaded = publishTo(subscriber("urn:mrn:test:legacy", null), GENERATOR);

        assertThat(uploaded.getEnvelope().getContainerType()).isEqualTo(ContainerTypeEnum.S100_DataSet);
        assertThat(uploaded.getEnvelope().getData()).isEqualTo(DATASET);
    }

    /**
     * The outbox neither persists nor retries, so a delivery failure that is swallowed here loses the
     * publication for that subscriber for good. It must come back out to the uploader instead.
     */
    @Test
    void aDeliveryFailureIsNotSwallowed() {
        SecomSubscriberService service = new SecomSubscriberService();
        service.outbox = mock(SecomOutboxService.class);
        service.subscriptionRepository = mock(SecomSubscriberRepository.class);
        when(service.subscriptionRepository.findActiveSubscribers(any(), any(), any(), any(), any()))
                .thenReturn(List.of(subscriber("urn:mrn:test:unreachable", ContainerTypeEnum.S100_DataSet)));
        doThrow(new IllegalStateException("subscriber is down")).when(service.outbox).sendTo(any(), any(), any());

        assertThatThrownBy(() -> service.publish(SECOM_DataProductType.S124, "1.0.0", UUID.randomUUID(), null, GENERATOR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("subscriber is down");
    }

    /** A dataset that cannot be packaged as an exchange set must not cost the other subscribers their delivery. */
    @Test
    void aSubscriberThatCannotBeServedDoesNotStopTheOthers() {
        TransmissibleDatasetGenerator unpackageable = new TransmissibleDatasetGenerator() {

            @Override
            protected byte[] createDataset() {
                return DATASET;
            }

            @Override
            protected byte[] createExchangeSet() {
                throw new IllegalStateException("not conformant");
            }
        };

        SecomSubscriberService service = new SecomSubscriberService();
        service.outbox = mock(SecomOutboxService.class);
        service.subscriptionRepository = mock(SecomSubscriberRepository.class);
        when(service.subscriptionRepository.findActiveSubscribers(any(), any(), any(), any(), any()))
                .thenReturn(List.of(subscriber("urn:mrn:test:exchange-set", ContainerTypeEnum.S100_ExchangeSet),
                        subscriber("urn:mrn:test:dataset", ContainerTypeEnum.S100_DataSet)));

        service.publish(SECOM_DataProductType.S124, "1.0.0", UUID.randomUUID(), null, unpackageable);

        ArgumentCaptor<Object> message = ArgumentCaptor.forClass(Object.class);
        verify(service.outbox).sendTo(any(), eq(SecomOperationType.UPLOAD), message.capture());
        UploadObject uploaded = (UploadObject) message.getValue();
        assertThat(uploaded.getEnvelope().getContainerType()).isEqualTo(ContainerTypeEnum.S100_DataSet);
        assertThat(uploaded.getEnvelope().getData()).isEqualTo(DATASET);
    }

    private static UploadObject publishTo(SecomSubscriberEntity subscriber, TransmissibleDatasetGenerator generator) {
        SecomSubscriberService service = new SecomSubscriberService();
        service.outbox = mock(SecomOutboxService.class);
        service.subscriptionRepository = mock(SecomSubscriberRepository.class);
        when(service.subscriptionRepository.findActiveSubscribers(any(), any(), any(), any(), any())).thenReturn(List.of(subscriber));

        service.publish(SECOM_DataProductType.S124, "1.0.0", UUID.randomUUID(), null, generator);

        ArgumentCaptor<Object> message = ArgumentCaptor.forClass(Object.class);
        verify(service.outbox).sendTo(any(), eq(SecomOperationType.UPLOAD), message.capture());
        return (UploadObject) message.getValue();
    }

    private static SecomSubscriberEntity subscriber(String mrn, ContainerTypeEnum containerType) {
        SecomNodeEntity node = new SecomNodeEntity();
        node.setMrn(mrn);

        SecomSubscriberEntity subscriber = new SecomSubscriberEntity();
        subscriber.setNode(node);
        subscriber.setContainerType(containerType);
        subscriber.setSubscriptionStart(Instant.now());
        return subscriber;
    }
}
