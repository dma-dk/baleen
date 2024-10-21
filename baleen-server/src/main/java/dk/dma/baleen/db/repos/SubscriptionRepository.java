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
package dk.dma.baleen.db.repos;

import java.util.Optional;
import java.util.UUID;

import dk.dma.baleen.db.SubscriptionEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SubscriptionRepository implements PanacheRepositoryBase<SubscriptionEntity, UUID> {
    // Custom query methods can be added here if needed

    public Optional<SubscriptionEntity> findByClientMrn(String clientMrn) {
        return find("mrn", clientMrn).firstResultOptional();
    }

    public static String toID(String mrn, UUID uuid) {
        return mrn + uuid.toString();
    }
}