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
package dk.dma.baleen.connector.secom.serviceold;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.grad.secom.core.exceptions.SecomNotFoundException;
import org.grad.secom.core.models.EnvelopeUploadObject;
import org.grad.secom.core.models.SubscriptionNotificationObject;
import org.grad.secom.core.models.SubscriptionRequestObject;
import org.grad.secom.core.models.UploadObject;
import org.grad.secom.core.models.enums.AckRequestEnum;
import org.grad.secom.core.models.enums.ContainerTypeEnum;
import org.grad.secom.core.models.enums.SubscriptionEventEnum;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.dma.baleen.connector.secom.controllers.SecomNode;
import dk.dma.baleen.connector.secom.model.SecomSubscriberEntity;
import dk.dma.baleen.connector.secom.model.SecomTransactionalUploadEntity;
import dk.dma.baleen.connector.secom.repository.SecomSubscriberRepository;
import dk.dma.baleen.connector.secom.repository.SecomUploadRepository;
import dk.dma.baleen.connector.secom.repository.SecomUploadedLinkRepository;
import dk.dma.baleen.connector.secom.service.SecomServiceRegistryService;
import dk.dma.baleen.connector.secom.serviceold.SecomOutboxService.SecomOperationType;

/**
 * Quick and dirty subscription service. Being replaced with a {@link SecomSubscriptionServiceV2}.
 */
@Service
public class SecomSubscriberService {

    private static final Logger logger = LoggerFactory.getLogger(SecomSubscriberService.class);

    @Autowired
    SecomOutboxService outbox;

    @Autowired
    SecomServiceRegistryService serviceRegistry;

    @Autowired
    SecomSubscriberRepository subscriptionRepository;

    @Autowired
    SecomUploadedLinkRepository uploadRepository;

    @Autowired
    SecomUploadRepository uploRepository;

    /** {@inheritDoc} */
    public void onPublication(Object message) {}

    // DataProduct, PreviousArea, newArea
    public void onStateUpdate(Geometry previousArea, Geometry newArea) {

    }

    @Transactional
    public void publish(String doc) {
        List<SecomSubscriberEntity> subscribers = subscriptionRepository.findAll();
        logger.info("Publishing XML to {} subscribers", subscribers.size());

        subscribers.forEach(subscriber -> {
            try {
                publish0(subscriber, subscriber.getNode().getMrn(), doc);
            } catch (Exception e) {
                logger.error("Failed to publish to subscriber {}", subscriber.getNode().getMrn(), e);
            }
        });
    }

    private void publish0(SecomSubscriberEntity entity, String mrn, String doc) throws Exception {
        logger.info("Publishing to {}", mrn);

        SecomTransactionalUploadEntity e = new SecomTransactionalUploadEntity();
        e = uploRepository.save(e);

        // Build the data envelope
        EnvelopeUploadObject envelope = new EnvelopeUploadObject();
        envelope.setDataProductType(entity.getDataProductType());
        envelope.setFromSubscription(true);
        envelope.setAckRequest(AckRequestEnum.DELIVERED_ACK_REQUESTED);
        envelope.setTransactionIdentifier(e.getTransactionIdentifier());
        envelope.setContainerType(ContainerTypeEnum.S100_DataSet);
        envelope.setData(doc.getBytes());

        // Set the envelope to the upload object
        UploadObject uploadObject = new UploadObject();
        uploadObject.setEnvelope(envelope);

        outbox.sendTo(new SecomNode(mrn), SecomOperationType.UPLOAD, uploadObject);
    }

    @Transactional
    public UUID subscribe(SecomNode node, SubscriptionRequestObject request) {

        // For now we only allow 1 subscription per mrn
        Optional<SecomSubscriberEntity> existing = subscriptionRepository.findByNode_Mrn(node.mrn());
        if (existing.isPresent()) {
            logger.info("Existing subscription found for {}", node.mrn());
            return request.getDataReference();
        }

        SecomSubscriberEntity subscription = new SecomSubscriberEntity();
        subscription.getNode().setMrn(node.mrn());

        subscriptionRepository.save(subscription);
        UUID uuid = subscription.getId();
        logger.info("Created new subscription {}", node.mrn());

        // Create A subscription notification response object and send it to outbox
        SubscriptionNotificationObject notification = new SubscriptionNotificationObject();
        notification.setSubscriptionIdentifier(uuid);
        notification.setEventEnum(SubscriptionEventEnum.SUBSCRIPTION_CREATED);
        outbox.sendTo(node, SecomOperationType.SUBSCRIPTION_NOTIFICATION, notification);
        return uuid;

    }

    /**
     * Removes a subscription for the given client MRN and UUID.
     */
    @Transactional
    public void unsubscribe(SecomNode node, UUID uuid) {
        Optional<SecomSubscriberEntity> entityOpt = subscriptionRepository.findById(uuid);
        if (entityOpt.isPresent()) {
            SecomSubscriberEntity entity = entityOpt.get();
            // Can only remove own subscriptions
            if (entity.getNode().getMrn().equals(node.mrn())) {
                logger.info("Removing subscription with UUID {}", uuid);
                subscriptionRepository.delete(entity);
                return;
            } else {
                logger.warn("Attempted to delete subscription with UUID {}. But subscription was owned by another MRN {} than requesting mrn {}", uuid,
                        entity.getNode().getMrn(), node.mrn());
            }
        }
        throw new SecomNotFoundException("Unknown subscription with UUID" + uuid);
    }
}