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
package dk.dma.baleen.secom.controllers;

import static java.util.Objects.requireNonNull;

import java.util.UUID;

import org.grad.secomv2.core.interfaces.RemoveSubscriptionServiceInterface;
import org.grad.secomv2.core.interfaces.SubscriptionServiceInterface;
import org.grad.secomv2.core.models.RemoveSubscriptionResponseObject;
import org.grad.secomv2.core.models.SubscriptionRequestObject;
import org.grad.secomv2.core.models.SubscriptionResponseObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import dk.dma.baleen.secom.serviceold.SecomSubscriberService;
import jakarta.validation.Valid;
import jakarta.ws.rs.Path;

/** Implements {@link SubscriptionServiceInterface} */
@Component
@Path("/")
@Validated
public class SecomSubscriptionController extends AbstractSecomController implements RemoveSubscriptionServiceInterface , SubscriptionServiceInterface {

    final SecomSubscriberService secom;

    @Autowired
    public SecomSubscriptionController(SecomSubscriberService service) {
        this.secom = requireNonNull(service);
    }

    /** {@inheritDoc} */
    @Override
    public RemoveSubscriptionResponseObject removeSubscription(UUID subscriptionIdentifier) {
        UUID uuid = requireAttribute("subscriptionIdentifier", subscriptionIdentifier);

        // Must throw if missing
        secom.unsubscribe(mrn(), uuid);

        return new RemoveSubscriptionResponseObject();
    }

    /** {@inheritDoc} */
    @Override
    public SubscriptionResponseObject subscription(@Valid SubscriptionRequestObject request) {
        UUID subscriptionIdentifier;
        try {
        subscriptionIdentifier = secom.subscribe(mrn(), request);
        }
        catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
        SubscriptionResponseObject response = new SubscriptionResponseObject();
        response.setSubscriptionIdentifier(subscriptionIdentifier);
        return response;
    }
}
