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
package dk.dma.baleen.rest.secom;

import java.util.UUID;

import org.grad.secom.core.exceptions.SecomNotFoundException;
import org.grad.secom.core.interfaces.RemoveSubscriptionSecomInterface;
import org.grad.secom.core.interfaces.SubscriptionSecomInterface;
import org.grad.secom.core.models.RemoveSubscriptionObject;
import org.grad.secom.core.models.RemoveSubscriptionResponseObject;
import org.grad.secom.core.models.SubscriptionRequestObject;
import org.grad.secom.core.models.SubscriptionResponseObject;

import dk.dma.baleen.service.secom.SecomSubscriptionServiceV1;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.Path;

/**
 *
 */
@Path(AbstractSecomApi.SECOM_ROOT_PATH)
public class SecomSubscriptionApi extends AbstractSecomApi implements RemoveSubscriptionSecomInterface , SubscriptionSecomInterface {

    /**
     * The SECOM Certificate Header.
     */
    public static final String CERT_HEADER = "X-SECOM-CERT";

    /**
     * The SECOM MRN Header.
     */
    public static final String MRN_HEADER = "X-SECOM-MRN";

    @Inject
    SecomSubscriptionServiceV1 subscriptionService;

    /** {@inheritDoc} */
    @Override
    public RemoveSubscriptionResponseObject removeSubscription(RemoveSubscriptionObject removeSubscriptionObject) {
        if (removeSubscriptionObject == null) {
            throw new ValidationException("RemoveSubscriptionObject cannot be null");
        }

        UUID uuid = removeSubscriptionObject.getSubscriptionIdentifier();
        if (uuid == null) {
            throw new ValidationException("Subscription identifier missing");
        }

        System.out.println("GOT A unsubscription request");

        boolean isRemoved = subscriptionService.remove(mrn().mrn(), removeSubscriptionObject.getSubscriptionIdentifier());
        if (!isRemoved) {
            throw new SecomNotFoundException(uuid.toString());
        }

        RemoveSubscriptionResponseObject response = new RemoveSubscriptionResponseObject();
        response.setMessage(String.format("Subscription " + uuid + " removed"));
        return response;
    }

    /** {@inheritDoc} */
    @Override
    public SubscriptionResponseObject subscription(@Valid SubscriptionRequestObject request) {
        if (request == null) {
            throw new ValidationException("SubscriptionRequestObject cannot be null");
        }

        subscriptionService.subscribe(request, mrn());

        SubscriptionResponseObject sro = new SubscriptionResponseObject();
        sro.setMessage("Subscription completed successfully");
        return sro;
    }

}
