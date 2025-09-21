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

package dk.dma.baleen.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.dma.baleen.secom.model.SecomSubscriberEntity;
import dk.dma.baleen.secom.repository.SecomSubscriberRepository;

@RestController
@RequestMapping("/api/subscribers")
public class SubscriberController {

    private static final Logger log = LoggerFactory.getLogger(SubscriberController.class);

    @Autowired
    private SecomSubscriberRepository subscriberRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public List<SubscriberDto> getAllSubscribers() {
        try {
            log.debug("Fetching all subscribers");
            List<SecomSubscriberEntity> subscribers = subscriberRepository.findAll();
            log.debug("Found {} subscribers", subscribers.size());
            return subscribers.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching subscribers", e);
            throw e;
        }
    }

    private SubscriberDto convertToDto(SecomSubscriberEntity entity) {
        try {
            // Handle lazy loading of node
            String nodeMrn = null;
            try {
                if (entity.getNode() != null) {
                    nodeMrn = entity.getNode().getMrn();
                }
            } catch (Exception e) {
                log.warn("Could not load node for subscriber {}: {}", entity.getId(), e.getMessage());
            }

            return new SubscriberDto(
                entity.getId(),
                entity.getDataProductType() != null ? entity.getDataProductType().toString() : null,
                entity.getProductVersion(),
                entity.getContainerType() != null ? entity.getContainerType().toString() : null,
                entity.getOriginalUnlocode(),
                entity.getOriginalWkt(),
                entity.getSubscriptionStart(),
                entity.getSubscriptionEnd(),
                nodeMrn
            );
        } catch (Exception e) {
            log.error("Error converting subscriber entity to DTO: {}", e.getMessage(), e);
            throw e;
        }
    }

    @DeleteMapping
    @Transactional
    public void removeAllSubscribers() {
        try {
            log.info("Remove all subscribers");
            subscriberRepository.deleteAll();
            log.info("All subscribers have been removed");
        } catch (Exception e) {
            log.error("Error clearing subscribers", e);
            throw e;
        }
    }

    public record SubscriberDto(
        java.util.UUID id,
        String dataProductType,
        String productVersion,
        String containerType,
        String unlocode,
        String wkt,
        java.time.Instant subscriptionStart,
        java.time.Instant subscriptionEnd,
        String nodeMrn
    ) {}
}