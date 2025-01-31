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
import org.grad.secom.core.models.enums.AckTypeEnum;
import org.grad.secom.core.models.enums.NackTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.dma.baleen.connector.secom.model.SecomTransactionalEntity;
import dk.dma.baleen.connector.secom.repository.SecomTransactionRepository;
import dk.dma.baleen.connector.secom.spi.AuthenticatedMcpNode;

/**
 * Used for interacting with SECOM transactions.
 */
@Service
public class SecomTransactionalService {

    private final SecomTransactionRepository transactionRepository;

    @Autowired
    public SecomTransactionalService(SecomTransactionRepository transactionRepository) {
        this.transactionRepository = requireNonNull(transactionRepository);
    }

    /**
     * Acknowledge an transaction.
     *
     * @param node
     *            the remote node
     * @param transactionIdentifier
     *            the transaction identifier to acknowledge
     * @param ackType
     *            the ack type
     * @param nackType
     *            the nack type is an error
     * @throw SecomNotFoundException if the transaction with the specified identifier could not be found, or if the
     *        transaction does not belong to the specified remote node
     */
    @Transactional
    public void acknowledgment(AuthenticatedMcpNode remoteNode, UUID transactionIdentifier, AckTypeEnum ackType, @Nullable NackTypeEnum nackType) {
        Optional<SecomTransactionalEntity> e = transactionRepository.findById(transactionIdentifier);
        if (e.isEmpty()) {
            throw new SecomNotFoundException("A transaction with UUID not found, UUID=" + transactionIdentifier);
        }

        // We found a valid transaction
        SecomTransactionalEntity entity = e.get();

        // Let us check that the connected node owns the transaction
        if (!entity.getNode().getMrn().equals(remoteNode.mrn())) {
            throw new SecomNotFoundException(
                    "A transaction was found, but does not belong to the remote node's MRN, UUID=" + transactionIdentifier + ", mrn=" + remoteNode.mrn());
        }

        // TODO, There should probably be some kind of state-machine here.
        // For example, I don't think an error is valid after both delivered and opened.
        switch (ackType) {
        case DELIVERED_ACK -> entity.setAckedAt(Instant.now());
        case OPENED_ACK -> entity.setOpenedAt(Instant.now());
        case ERROR -> {
            entity.setError(nackType);
            entity.setErrorAt(Instant.now());
        }
        }
    }
}
