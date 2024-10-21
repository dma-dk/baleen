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

import org.grad.secom.core.interfaces.SubscriptionNotificationSecomInterface;
import org.grad.secom.core.models.SubscriptionNotificationObject;
import org.grad.secom.core.models.SubscriptionNotificationResponseObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
import jakarta.ws.rs.Path;

/**
 * Implementation of the SECOM Subscription Notification Interface. This implementation simply logs the notifications.
 */
@Path(AbstractSecomApi.SECOM_ROOT_PATH)
public class SecomSubscriptionNotificationApi extends AbstractSecomApi implements SubscriptionNotificationSecomInterface {
    private final Logger logger = LoggerFactory.getLogger(SecomSubscriptionNotificationApi.class);

    @Override
    public SubscriptionNotificationResponseObject subscriptionNotification(@Valid SubscriptionNotificationObject subscriptionNotificationObject) {
        logger.info("Received subscription notification: " + subscriptionNotificationObject.getSubscriptionIdentifier() + " is now status " + subscriptionNotificationObject.getEventEnum().name());
        System.out.println();

        return new SubscriptionNotificationResponseObject();
    }
}
