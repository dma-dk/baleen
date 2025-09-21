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
package dk.dma.baleen.secom.model;

import java.time.Instant;
import java.util.UUID;

import org.grad.secom.core.models.enums.NackTypeEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 *
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "secom_transactional_entity")
public abstract class SecomTransactionalEntity {

    @Column
    Instant ackedAt;

    @Enumerated(EnumType.STRING) // H2 does not support default tinyint mapping
    @Column
    NackTypeEnum error;

    @Column
    Instant errorAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id", nullable = false)
    private SecomNodeEntity node;

    @Column
    Instant openedAt;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, unique = true)
    UUID transactionIdentifier;

    /**
     * @return the ackedAt
     */
    public Instant getAckedAt() {
        return ackedAt;
    }

    /**
     * @return the error
     */
    public NackTypeEnum getError() {
        return error;
    }

    /**
     * @return the errorAt
     */
    public Instant getErrorAt() {
        return errorAt;
    }

    public SecomNodeEntity getNode() {
        return node; // Remove UnsupportedOperationException
    }

    public void setNode(SecomNodeEntity node) {
        this.node = node; // Remove UnsupportedOperationException
    }

    /**
     * @return the openedAt
     */
    public Instant getOpenedAt() {
        return openedAt;
    }

    /**
     * @return the transactionIdentifier
     */
    public UUID getTransactionIdentifier() {
        return transactionIdentifier;
    }

    /**
     * @param ackedAt
     *            the ackedAt to set
     */
    public void setAckedAt(Instant ackedAt) {
        this.ackedAt = ackedAt;
    }

    /**
     * @param error
     *            the error to set
     */
    public void setError(NackTypeEnum error) {
        this.error = error;
    }

    /**
     * @param errorAt
     *            the errorAt to set
     */
    public void setErrorAt(Instant errorAt) {
        this.errorAt = errorAt;
    }

    /**
     * @param openedAt
     *            the openedAt to set
     */
    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }
}
