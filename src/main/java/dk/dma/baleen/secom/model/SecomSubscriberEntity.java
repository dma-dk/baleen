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
package dk.dma.baleen.secom.model;

import java.time.Instant;
import java.util.UUID;

import org.grad.secom.core.models.enums.ContainerTypeEnum;
import org.grad.secom.core.models.enums.SECOM_DataProductType;
import org.locationtech.jts.geom.Geometry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A single subscription.
 */
@Entity
@Table(name = "secom_subscriber")
public class SecomSubscriberEntity {

    @Column
    private String callback;

    @Enumerated(EnumType.STRING) // H2 does not support default tinyint mapping
    @Column
    private ContainerTypeEnum containerType;

    @Enumerated(EnumType.STRING) // H2 does not support default tinyint mapping
    @Column
    private SECOM_DataProductType dataProductType;

    @Column
    private UUID dataReference;

    /** unlocode and wkt */
    @Column
    private Geometry geometry;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id", nullable = false)
    private SecomNodeEntity node;

    @Column
    private String originalUnlocode;

    @Lob
    @Column
    private String originalWkt;

    @Column
    private String productVersion;

    @Column(name = "subscription_end")
    private Instant subscriptionEnd;

    @Column(name = "subscription_start")
    private Instant subscriptionStart;

    /**
     * @return the containerType
     */
    public ContainerTypeEnum getContainerType() {
        return containerType;
    }

    /**
     * @return the dataProductType
     */
    public SECOM_DataProductType getDataProductType() {
        return dataProductType;
    }

    /**
     * @return the dataReference
     */
    public UUID getDataReference() {
        return dataReference;
    }

    /**
     * @return the geometry
     */
    public Geometry getGeometry() {
        return geometry;
    }

    /**
     * @return the id
     */
    public UUID getId() {
        return id;
    }

    /**
     * @return the node
     */
    public SecomNodeEntity getNode() {
        return node;
    }

    /**
     * @return the originalUnlocode
     */
    public String getOriginalUnlocode() {
        return originalUnlocode;
    }

    /**
     * @return the originalWkt
     */
    public String getOriginalWkt() {
        return originalWkt;
    }

    /**
     * @return the productVersion
     */
    public String getProductVersion() {
        return productVersion;
    }

    /**
     * @return the subscriptionEnd
     */
    public Instant getSubscriptionEnd() {
        return subscriptionEnd;
    }

    /**
     * @return the subscriptionStart
     */
    public Instant getSubscriptionStart() {
        return subscriptionStart;
    }

    /**
     * @return the unlocode
     */
    public String getUnlocode() {
        return originalUnlocode;
    }

    /**
     * @return the wkt
     */
    public String getWkt() {
        return originalWkt;
    }

    /**
     * @param containerType
     *            the containerType to set
     */
    public void setContainerType(ContainerTypeEnum containerType) {
        this.containerType = containerType;
    }

    /**
     * @param dataProductType
     *            the dataProductType to set
     */
    public void setDataProductType(SECOM_DataProductType dataProductType) {
        this.dataProductType = dataProductType;
    }

    /**
     * @param dataReference
     *            the dataReference to set
     */
    public void setDataReference(UUID dataReference) {
        this.dataReference = dataReference;
    }

    /**
     * @param geometry
     *            the geometry to set
     */
    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    /**
     * @param node the node to set
     */
    public void setNode(SecomNodeEntity node) {
        this.node = node;
    }

    /**
     * @param originalUnlocode
     *            the originalUnlocode to set
     */
    public void setOriginalUnlocode(String originalUnlocode) {
        this.originalUnlocode = originalUnlocode;
    }

    /**
     * @param originalWkt
     *            the originalWkt to set
     */
    public void setOriginalWkt(String originalWkt) {
        this.originalWkt = originalWkt;
    }

    /**
     * @param productVersion
     *            the productVersion to set
     */
    public void setProductVersion(String productVersion) {
        this.productVersion = productVersion;
    }

    /**
     * @param subscriptionEnd
     *            the subscriptionEnd to set
     */
    public void setSubscriptionEnd(Instant subscriptionEnd) {
        this.subscriptionEnd = subscriptionEnd;
    }

    /**
     * @param subscriptionStart
     *            the subscriptionStart to set
     */
    public void setSubscriptionStart(Instant subscriptionStart) {
        this.subscriptionStart = subscriptionStart;
    }

    /**
     * @param unlocode
     *            the unlocode to set
     */
    public void setUnlocode(String unlocode) {
        this.originalUnlocode = unlocode;
    }

    /**
     * @param wkt
     *            the wkt to set
     */
    public void setWkt(String wkt) {
        this.originalWkt = wkt;
    }
}
