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

import java.time.Instant;
import java.util.List;

import org.grad.secom.core.models.EnvelopeUploadObject;
import org.grad.secom.core.models.UploadObject;
import org.grad.secom.core.models.enums.AckRequestEnum;
import org.grad.secom.core.models.enums.ContainerTypeEnum;
import org.grad.secom.core.models.enums.SECOM_DataProductType;
import org.jboss.logging.Logger;

import dk.dma.baleen.db.PublicationEntity;
import dk.dma.baleen.db.PublicationRecipiantAttemptedDeliveryEntity;
import dk.dma.baleen.db.PublicationRecipiantEntity;
import dk.dma.baleen.db.PublicationRecipiantEntity.DeliveryStatus;
import dk.dma.baleen.db.SubscriptionEntity;
import dk.dma.baleen.db.repos.PublicationRecipiantAttemptedDeliveryEntityRepository;
import dk.dma.baleen.db.repos.PublicationRecipiantEntityRepository;
import dk.dma.baleen.db.repos.SubscriptionRepository;
import dk.dma.baleen.service.mcp.MCPServiceRegistryClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * The next version of delivery. Uses a transactional outbox pattern.
 */
@ApplicationScoped
public class SecomSubscriptionServiceV2 {
    static final long MAX_BACKOFF_DELAY_SECONDS = 86400; // 1 day in seconds

    @Inject
    MCPServiceRegistryClient finder;
    @Inject
    Logger log;

    @Inject
    PublicationRecipiantAttemptedDeliveryEntityRepository publishedDataSetDeliveryAttemptRepository;

    @Inject
    PublicationRecipiantEntityRepository publishedDataSetDeliveryRepository;

    @Inject
    SubscriptionRepository sr;

    @Inject
    SubscriptionRepository subscriptionRepository;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    void processMessage(PublicationRecipiantEntity message) {
        String mrn = message.getSubscription().getMrn();
        System.out.println("Publish to " + mrn);
        SecomClient sc = finder.resolve(mrn);
        System.out.println("Publish to host " + sc.baseUri);
        UploadObject uploadObject = new UploadObject();
        EnvelopeUploadObject envelopeUploadObject = new EnvelopeUploadObject();
        envelopeUploadObject.setDataProductType(SECOM_DataProductType.S124);
        envelopeUploadObject.setFromSubscription(true);
        envelopeUploadObject.setAckRequest(AckRequestEnum.DELIVERED_ACK_REQUESTED);
        envelopeUploadObject.setContainerType(ContainerTypeEnum.S100_DataSet);
        envelopeUploadObject.setData(message.getPublishedDataSet().getEnvelopeUploadObject());
        uploadObject.setEnvelope(envelopeUploadObject);

        try {
            sc.upload(uploadObject, null);
            // Create a new delivery attempt for auditing purposes
            PublicationRecipiantAttemptedDeliveryEntity deliveryAttempt = new PublicationRecipiantAttemptedDeliveryEntity();
            deliveryAttempt.setMessage(message);
//            deliveryAttempt.setAttemptCount(message.getRetryCount() + 1);
            deliveryAttempt.setAttemptTime(Instant.now());
            deliveryAttempt.setSuccess(true);
            publishedDataSetDeliveryAttemptRepository.persist(deliveryAttempt);
        } catch (Exception uploadException) {
            log.error("Error uploading to client: " + mrn, uploadException);
            // Create a new delivery attempt for auditing purposes
            PublicationRecipiantAttemptedDeliveryEntity deliveryAttempt = new PublicationRecipiantAttemptedDeliveryEntity();
            deliveryAttempt.setMessage(message);
            // deliveryAttempt.setAttemptCount(message.getRetryCount() + 1);
            deliveryAttempt.setAttemptTime(Instant.now());
            deliveryAttempt.setSuccess(false);
            deliveryAttempt.setErrorMessage("Error occurred while processing message");
            publishedDataSetDeliveryAttemptRepository.persist(deliveryAttempt);
            throw uploadException;
        }
    }

 //   @Scheduled(every = "1m") // Runs every minute
    @Transactional
    public void processPendingMessages() {
        Instant now = Instant.now();
        List<PublicationRecipiantEntity> pendingMessages = publishedDataSetDeliveryRepository.findReadyForRetry(now);

        for (PublicationRecipiantEntity message : pendingMessages) {
            processMessage(message);
        }
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    void processSubscriber(String mrn) {

    }

    @Transactional
    public void publishToAll(String doc) {
        List<SubscriptionEntity> allSubscriptions = subscriptionRepository.findAll().list();

        if (!allSubscriptions.isEmpty()) {
            PublicationEntity ds = new PublicationEntity();
            ds.setEnvelopeUploadObject(doc.getBytes());
            ds.setCreatedAt(Instant.now());
            for (SubscriptionEntity subscription : allSubscriptions) {
                PublicationRecipiantEntity newDelivery = new PublicationRecipiantEntity();
                newDelivery.setPublishedDataSet(ds);
                newDelivery.setSubscription(subscription);
                newDelivery.setStatus(DeliveryStatus.PENDING);
                newDelivery.setRetryCount(0);
                newDelivery.setCreatedAt(ds.getCreatedAt());
                newDelivery.setNextRetryAt(ds.getCreatedAt());
            }
        }
        // Start a new thread for all that does not have messages that have not been sent
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    void publishToClient(SubscriptionEntity e, PublicationEntity publishedDataSet, String doc) {

        PublicationRecipiantEntity newDelivery = new PublicationRecipiantEntity();
        newDelivery.setPublishedDataSet(publishedDataSet);
        newDelivery.setSubscription(e);
        // newDelivery.setStatus("PENDING");
        newDelivery.setRetryCount(0);
        newDelivery.setCreatedAt(Instant.now());
        newDelivery.setNextRetryAt(Instant.now());

        // Persist the new delivery
        publishedDataSetDeliveryRepository.persist(newDelivery);

        processMessage(newDelivery);
    }
}