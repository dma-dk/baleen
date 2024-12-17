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
package dk.dma.baleen.connector.secom.controllers;

import static java.util.Objects.requireNonNull;

import java.util.UUID;

import org.grad.secom.core.interfaces.RemoveSubscriptionSecomInterface;
import org.grad.secom.core.interfaces.SubscriptionSecomInterface;
import org.grad.secom.core.models.RemoveSubscriptionObject;
import org.grad.secom.core.models.RemoveSubscriptionResponseObject;
import org.grad.secom.core.models.SubscriptionRequestObject;
import org.grad.secom.core.models.SubscriptionResponseObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import dk.dma.baleen.connector.secom.serviceold.SecomSubscriberService;
import jakarta.validation.Valid;
import jakarta.ws.rs.Path;

/** Implements {@link SubscriptionSecomInterface} */
@Component
@Path("/")
@Validated
public class SecomSubscriptionController extends AbstractSecomController implements RemoveSubscriptionSecomInterface , SubscriptionSecomInterface {

    final SecomSubscriberService secom;

    @Autowired
    public SecomSubscriptionController(SecomSubscriberService service) {
        this.secom = requireNonNull(service);
    }

    /** {@inheritDoc} */
    @Override
    public RemoveSubscriptionResponseObject removeSubscription(@Valid RemoveSubscriptionObject removeSubscriptionObject) {
        UUID uuid = requireAttribute("subscriptionIdentifier", removeSubscriptionObject.getSubscriptionIdentifier());

        // Must throw if missing
        secom.unsubscribe(mrn(), uuid);

        RemoveSubscriptionResponseObject response = new RemoveSubscriptionResponseObject();
        response.setMessage(String.format("Subscription " + uuid + " removed"));
        return response;
    }

    /** {@inheritDoc} */
    @Override
    public SubscriptionResponseObject subscription(@Valid SubscriptionRequestObject request) {
        UUID subscriptionIdentifier = secom.subscribe(mrn(), request);

        SubscriptionResponseObject response = new SubscriptionResponseObject();
        response.setSubscriptionIdentifier(subscriptionIdentifier);
        response.setMessage("Subscription completed successfully");
        return response;
    }
}
