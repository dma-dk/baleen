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

import dk.dma.baleen.secom.model.SecomSubscriberEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class PublicationRecipiantEntity {

    @Column(nullable = false)
    private Instant createdAt;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant nextRetryAt;

    @ManyToOne
    @JoinColumn(name = "published_data_set_id", nullable = false)
    private PublicationEntity publishedDataSet; // Reference to the actual byte[] to publish

    /**
     * @param publishedDataSet
     *            the publishedDataSet to set
     */
    public void setPublishedDataSet(PublicationEntity publishedDataSet) {
        this.publishedDataSet = publishedDataSet;
    }

    @Column(nullable = false)
    private int retryCount = 0;

    @Enumerated(EnumType.STRING) // H2 does not support default tinyint mapping
    @Column(nullable = false)
    private DeliveryStatus status;

    @ManyToOne
    @JoinColumn(name = "subscription_id", nullable = false)
    private SecomSubscriberEntity subscription;

    /**
     * @return the createdAt
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @return the nextRetryAt
     */
    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public PublicationEntity getPublishedDataSet() {
        return publishedDataSet;
    }

    /**
     * @return the retryCount
     */
    public int getRetryCount() {
        return retryCount;
    }

    /**
     * @return the status
     */
    public DeliveryStatus getStatus() {
        return status;
    }

    /**
     * @return the subscription
     */
    public SecomSubscriberEntity getSubscription() {
        return subscription;
    }

    public UUID getSubscriptionId() {
        return subscription.getId();
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    /**
     * @param createdAt
     *            the createdAt to set
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    // Getters and setters

    public void setNextRetryAt(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    /**
     * @param retryCount
     *            the retryCount to set
     */
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }

    /**
     * @param subscription
     *            the subscription to set
     */
    public void setSubscription(SecomSubscriberEntity subscription) {
        this.subscription = subscription;
    }

    public enum DeliveryStatus {
        PENDING, PROCESSING, PROCESSED, FAILED
    }
}