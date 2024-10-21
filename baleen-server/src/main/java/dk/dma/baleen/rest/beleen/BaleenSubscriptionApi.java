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
package dk.dma.baleen.rest.beleen;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.dma.baleen.db.SubscriptionEntity;
import dk.dma.baleen.db.repos.SubscriptionRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** API for getting current subscriptions. No security for now. */
@Path("/api")
public class BaleenSubscriptionApi {

    @Inject
    SubscriptionRepository subscriptionRepo;

    @Path("subscriptions")
    @Transactional
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<SubscriptionEntity> subscribers() throws Throwable {
        PanacheQuery<SubscriptionEntity> all = subscriptionRepo.findAll();
        ObjectMapper om = new ObjectMapper();
        om.writeValueAsString(all.stream().map(SubscriptionDTO::new).toList());
        return all.list(); // Return the list of subscriptions as JSON
    }

    public record SubscriptionDTO(UUID id, String mrn) {
        SubscriptionDTO(SubscriptionEntity e) {
            this(e.getId(), e.getMrn());
        }
    }
}
