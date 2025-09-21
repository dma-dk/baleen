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
package dk.dma.baleen.secom.serviceold;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
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
import org.grad.secom.core.models.enums.SECOM_DataProductType;
import org.grad.secom.core.models.enums.SubscriptionEventEnum;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.dma.baleen.secom.controllers.SecomNode;
import dk.dma.baleen.secom.model.SecomNodeEntity;
import dk.dma.baleen.secom.model.SecomSubscriberEntity;
import dk.dma.baleen.secom.model.SecomTransactionalUploadEntity;
import dk.dma.baleen.secom.repository.SecomNodeRepository;
import dk.dma.baleen.secom.repository.SecomSubscriberRepository;
import dk.dma.baleen.secom.service.SecomServiceRegistryService;
import dk.dma.baleen.secom.serviceold.SecomOutboxService.SecomOperationType;

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

    /** {@inheritDoc} */
    public void onPublication(Object message) {}

    @Transactional
    public void publish(SECOM_DataProductType dataProductType, String productVersion, UUID dataReference, Geometry geometry,
            TransmissibleDatasetGenerator generator) {
        List<SecomSubscriberEntity> subscribers = subscriptionRepository.findActiveSubscribers(dataProductType, productVersion, dataReference, geometry,
                Instant.now());
        System.out.println("Found " + subscribers.size() + " subscribers");
        for (SecomSubscriberEntity e : subscribers) {
            SecomTransactionalUploadEntity upl = new SecomTransactionalUploadEntity();
//            upl = uploRepository.save(upl);

            // Build the data envelope
            EnvelopeUploadObject envelope = new EnvelopeUploadObject();
            envelope.setDataProductType(dataProductType);
            envelope.setFromSubscription(true);
            envelope.setAckRequest(AckRequestEnum.DELIVERED_ACK_REQUESTED);
            envelope.setTransactionIdentifier(upl.getTransactionIdentifier());
            envelope.setContainerType(ContainerTypeEnum.S100_DataSet);

            envelope.setData(generator.getDataset());
            requireNonNull(envelope.getData());
//
//            if (e.getContainerType() == ContainerTypeEnum.S100_DataSet) {
//                envelope.setData(generator.getDataset());
//            } else if (e.getContainerType() == ContainerTypeEnum.S100_ExchangeSet) {
//                envelope.setData(generator.getExchangeSet());
//            }

            // Set the envelope to the upload object
            UploadObject uploadObject = new UploadObject();
            uploadObject.setEnvelope(envelope);

            outbox.sendTo(new SecomNode(e.getNode().getMrn()), SecomOperationType.UPLOAD, uploadObject);
        }
    }

    @Autowired
    SecomNodeRepository nodeRepository;

    @Transactional
    public UUID subscribe(SecomNode node, SubscriptionRequestObject request) {

        logger.info("Subscription created from {}", node.mrn());

        // For now we only allow 1 subscription per mrn
        Optional<SecomSubscriberEntity> existing = subscriptionRepository.findByNode_Mrn(node.mrn());
        if (existing.isPresent()) {
            logger.info("Existing subscription found for {}", node.mrn());
            return request.getDataReference();
        }

        SecomSubscriberEntity subscription = new SecomSubscriberEntity();

        SecomNodeEntity sne = nodeRepository.findOrCreate(node.mrn());

        subscription.setNode(sne);

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