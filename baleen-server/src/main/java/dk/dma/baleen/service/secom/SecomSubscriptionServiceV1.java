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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.grad.secom.core.models.EnvelopeUploadObject;
import org.grad.secom.core.models.SubscriptionNotificationObject;
import org.grad.secom.core.models.SubscriptionRequestObject;
import org.grad.secom.core.models.UploadObject;
import org.grad.secom.core.models.enums.AckRequestEnum;
import org.grad.secom.core.models.enums.ContainerTypeEnum;
import org.grad.secom.core.models.enums.SECOM_DataProductType;
import org.grad.secom.core.models.enums.SubscriptionEventEnum;
import org.jboss.logging.Logger;

import dk.dma.baleen.db.SubscriptionEntity;
import dk.dma.baleen.db.repos.SubscriptionRepository;
import dk.dma.baleen.rest.secom.AbstractSecomApi.MRNClient;
import dk.dma.baleen.service.mcp.MCPServiceRegistryClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Quick and dirty subscription service. Being replaced with a {@link SecomSubscriptionServiceV2}.
 */
@ApplicationScoped
public class SecomSubscriptionServiceV1 {

    private static final Logger LOG = Logger.getLogger(SecomSubscriptionServiceV1.class);

    @Inject
    SubscriptionRepository sr;

    @Inject
    MCPServiceRegistryClient finder;

    /**
     * @param uuid
     * @return
     */
    public boolean remove(String clientMrn, UUID uuid) {
        SubscriptionEntity e = sr.findById(uuid);
        if (e != null) {
            // Can only remove own subscriptions
            if (e.getMrn().equals(clientMrn)) {
                System.out.println("REMOVING SUBSCRIPTION WITH UIID " + uuid);
                sr.delete(e);
                return true;
            } else {
                LOG.warn("Attempted to delete subscription with UUID" + uuid + ". But subscription was owned by other MRN " + e.getMrn()
                        + " Then requesting mrn " + clientMrn);
            }
        }
        return false;
    }

    public void subscribe(SubscriptionRequestObject request, MRNClient client) {
        try {
            subscribe0(request, client);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Inject
    SubscriptionRepository secomSubscriptionRepo;

    @Transactional
    public void subscribe0(SubscriptionRequestObject request, MRNClient client) throws Exception {

        Optional<SubscriptionEntity> existing = sr.findByClientMrn(client.mrn());
        if (existing.isPresent()) {
            System.out.println("Existing subscription found for " + client.mrn());
            return;
        }
        System.out.println("Persisting subscription entity for " + client.mrn());
        SubscriptionEntity se = new SubscriptionEntity();
        se.setMrn(client.mrn());

        sr.persist(se);
        UUID uuid = se.getId();

        List<SubscriptionEntity> list = sr.findAll().list();
        System.out.println(list.size() + " subscribers in system now");

        System.out.println("NEW SUB with UUID" + uuid);
        // Get the SECOM client matching the provided MRN

        // Create the subscription notification response object
        SubscriptionNotificationObject subscriptionNotificationObject = new SubscriptionNotificationObject();
        subscriptionNotificationObject.setSubscriptionIdentifier(uuid);
        subscriptionNotificationObject.setEventEnum(SubscriptionEventEnum.SUBSCRIPTION_CREATED);

        SecomClient sc = finder.resolve(client);

        sc.send(subscriptionNotificationObject);

        // Send the object the return the response
    }

//    @Transactional
//    public void subscribe(String mrn, LocalDateTime start, LocalDateTime stop) {
//        SubscriptionEntity subscription = new SubscriptionEntity();
//
//        // Set the fields
//        subscription.setMrn(mrn); // Replace with your actual MRN
//        subscription.setSubscriptionStart(start);
//        subscription.setSubscriptionEnd(stop); // For example, 30 days from now
//
//        // Persist the entity to the database
//        sr.persist(subscription);
//
//        List<SubscriptionEntity> list = sr.findAll().list();
//        System.out.println(list.size() + " subscribers in system now");
//    }

    /**
     * @param doc
     */
    @Transactional
    public void publish(String doc) {
        List<SubscriptionEntity> list = sr.findAll().list();
        System.out.println("Publish xml to " + list.size() + " subscribers");
        for (SubscriptionEntity e : list) {
            try {
                publish(e, e.getMrn(), doc);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    void publish(SubscriptionEntity e, String mrn, String doc) throws Exception {
        System.out.println("Publish to " + mrn);
        // Build the data envelope
        EnvelopeUploadObject envelopeUploadObject = new EnvelopeUploadObject();
        envelopeUploadObject.setDataProductType(SECOM_DataProductType.S124);
        envelopeUploadObject.setFromSubscription(true);
        envelopeUploadObject.setAckRequest(AckRequestEnum.DELIVERED_ACK_REQUESTED);
        envelopeUploadObject.setTransactionIdentifier(UUID.randomUUID());

        envelopeUploadObject.setContainerType(ContainerTypeEnum.S100_DataSet);
        // s125Dataset.getDatasetContent().getContent().getBytes()
        envelopeUploadObject.setData(doc.getBytes());

        // Set the envelope to the upload object
        UploadObject uploadObject = new UploadObject();
        uploadObject.setEnvelope(envelopeUploadObject);

        SecomClient sc = finder.resolve(mrn);
        System.out.println("Publish to host " + sc.baseUri);
        sc.upload(uploadObject, null);
    }

}
