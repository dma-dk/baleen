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
package dk.dma.baleen.connector.secom.spi;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.grad.secom.core.exceptions.SecomNotImplementedException;
import org.grad.secom.core.models.CapabilityObject;
import org.grad.secom.core.models.SubscriptionNotificationObject;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

import dk.dma.baleen.product.spi.DataSet;

/**
 *
 */
public interface SecomProductSpi {

    default List<CapabilityObject> capabilities() {
        throw new SecomNotImplementedException("capabilitites not implemented");
    }

    default void onSubscriptionNotificate(SubscriptionNotificationObject sno) {
        // ignore by default
    }

    default Page<DataSet> getAll(@Nullable UUID uuid, @Nullable Geometry geometry, @Nullable LocalDateTime fromTime, @Nullable LocalDateTime toTime,
            boolean includeCancelled, Pageable pageable) {
        throw new SecomNotImplementedException("get not implemented");
    }
}
