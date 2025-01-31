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
package dk.dma.baleen.connector.secom.service;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.grad.secom.core.exceptions.SecomNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import dk.dma.baleen.connector.secom.model.SecomNodeEntity;
import dk.dma.baleen.connector.secom.model.SecomTransactionalUploadLinkEntity;
import dk.dma.baleen.connector.secom.repository.SecomNodeRepository;
import dk.dma.baleen.connector.secom.repository.SecomUploadedLinkRepository;
import dk.dma.baleen.connector.secom.spi.AuthenticatedMcpNode;
import jakarta.transaction.Transactional;

/**
 * This service can store and retrieve upload links.
 */
@Service
public class SecomLinkStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecomLinkStorageService.class);

    /** A repository handling links */
    private final SecomUploadedLinkRepository linkRepository;

    /** A repository handling nodes. */
    private final SecomNodeRepository nodeRepository;

    @Autowired
    public SecomLinkStorageService(SecomUploadedLinkRepository linkRepository, SecomNodeRepository nodeRepository) {
        this.linkRepository = requireNonNull(linkRepository);
        this.nodeRepository = requireNonNull(nodeRepository);
    }

    @Scheduled(cron = "0 0 * * * ?") // Runs at the start of every hour
    @Transactional
    public void cleanupExpiredLinks() {
        try {
            LOGGER.info("Starting cleanup of expired upload links");
            linkRepository.deleteAllExpired(Instant.now());
            LOGGER.info("Completed cleanup of expired upload links");
        } catch (Exception e) {
            LOGGER.error("Error during cleanup of expired upload links", e);
            throw e;
        }
    }

    /**
     * Retrieves data from a link represented by the transaction identifier that created the link.
     *
     * @param node
     *            that is trying to retrieve the link
     * @param transactionIdentifier
     *            the link represented by an transaction identifier
     * @return the linked data
     *
     * @throws org.grad.secom.core.exceptions.SecomNotFoundException
     *             if the transactionIdentifier could not be found. Or if it a transactionIdentifier could be found, but the
     *             MRN of the specified node does not match. Or if the link has timed out.
     */
    @Transactional
    public byte[] getLink(AuthenticatedMcpNode node, UUID transactionIdentifier) {
        Optional<SecomTransactionalUploadLinkEntity> e = linkRepository.findById(transactionIdentifier);
        if (e.isEmpty()) {
            throw new SecomNotFoundException("A link with the specified UUID could not be found or it has previously expired, UUID=" + transactionIdentifier);
        }

        SecomTransactionalUploadLinkEntity upload = e.get();
        // Validate owner
        if (!upload.getNode().getMrn().equals(node.mrn())) {
            throw new SecomNotFoundException(
                    "A link with the specified UUID was found, but the requesting node is not the owner, UUID=" + transactionIdentifier);
        }

        // Check that the link is still valid
        if (upload.getExpiresAt().isAfter(Instant.now())) {
            throw new SecomNotFoundException("A link with the specified UUID has expired, please request a new one, UUID=" + transactionIdentifier);
        }

        return upload.getData();
    }

    /**
     * Stores the specified byte array as a link that can be retrieved at a later point.
     *
     * @param node
     *            the node we are storing the link for
     * @param expiresAt
     *            when the upload expires
     * @param data
     *            the data to store
     * @return an UUID that can be used to retrieve the data at a later point
     */
    @Transactional
    public UUID storeLink(AuthenticatedMcpNode node, Instant expiresAt, byte[] data) {
        SecomTransactionalUploadLinkEntity e = new SecomTransactionalUploadLinkEntity();

        // Set the node for the transaction
        SecomNodeEntity nodeEntity = nodeRepository.findOrCreate(node.mrn());
        e.setNode(nodeEntity);

        // Set specific properties for the upload
        e.setData(data);
        e.setExpiresAt(expiresAt);
        e.setLinkSize(data.length);

        // Persist the entity, and return the transaction UUID
        return linkRepository.save(e).getTransactionIdentifier();
    }
}
