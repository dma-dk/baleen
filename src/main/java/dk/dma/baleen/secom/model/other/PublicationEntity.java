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
package dk.dma.baleen.secom.model.other;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * A dataset that was published.
 */
@Entity
public class PublicationEntity {

    @Column(nullable = false, columnDefinition = "LONGVARBINARY")
    private byte[] envelopeUploadObject; // The actual data to be published

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private UUID transactionIdentifier; // Unique identifier for the transaction

    // Getters and setters

    public byte[] getEnvelopeUploadObject() {
        return envelopeUploadObject;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTransactionIdentifier() {
        return transactionIdentifier;
    }

    public void setEnvelopeUploadObject(byte[] envelopeUploadObject) {
        this.envelopeUploadObject = envelopeUploadObject;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setTransactionIdentifier(UUID transactionIdentifier) {
        this.transactionIdentifier = transactionIdentifier;
    }

    /**
     * @return the createdAt
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * @param createdAt the createdAt to set
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}