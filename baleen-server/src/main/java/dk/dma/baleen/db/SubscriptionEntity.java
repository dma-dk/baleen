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
package dk.dma.baleen.db;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.grad.secom.core.models.enums.ContainerTypeEnum;
import org.grad.secom.core.models.enums.SECOM_DataProductType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * A single subscription.
 */
@Entity
@Table(name = "subscriptions")
public class SubscriptionEntity {

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PublicationRecipiantEntity> allMessages;

    @Column
    private ContainerTypeEnum containerType;

    @Column
    private SECOM_DataProductType dataProductType;

    @Column
    private UUID dataReference;

    // I think we drop this, and check the configuration for mappings
    @Column
    private String forceEndpoints;

    /** unlocode and wkt */
//    @Column(name = "geometry")
//    private Geometry geometry;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "mrn", nullable = false, updatable = false)
    private String mrn;

    @Column
    private String productVersion;

    @Column(name = "subscription_end")
    private LocalDateTime subscriptionEnd;

    @Column(name = "subscription_start")
    private LocalDateTime subscriptionStart;

    @Column
    private String unlocode;

    @Column
    private String wkt;

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
     * @return the forceEndpoints
     */
    public String getForceEndpoints() {
        return forceEndpoints;
    }

//    /**
//     * @return the geometry
//     */
//    public Geometry getGeometry() {
//        return geometry;
//    }

    /**
     * @return the id
     */
    public UUID getId() {
        return id;
    }

    /**
     * @return the mrn
     */
    public String getMrn() {
        return mrn;
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
    public LocalDateTime getSubscriptionEnd() {
        return subscriptionEnd;
    }

    /**
     * @return the subscriptionStart
     */
    public LocalDateTime getSubscriptionStart() {
        return subscriptionStart;
    }

    /**
     * @return the unlocode
     */
    public String getUnlocode() {
        return unlocode;
    }

    /**
     * @return the wkt
     */
    public String getWkt() {
        return wkt;
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
     * @param forceEndpoints
     *            the forceEndpoints to set
     */
    public void setForceEndpoints(String forceEndpoints) {
        this.forceEndpoints = forceEndpoints;
    }

//    /**
//     * @param geometry the geometry to set
//     */
//    public void setGeometry(Geometry geometry) {
//        this.geometry = geometry;
//    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * @param mrn
     *            the mrn to set
     */
    public void setMrn(String mrn) {
        this.mrn = mrn;
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
    public void setSubscriptionEnd(LocalDateTime subscriptionEnd) {
        this.subscriptionEnd = subscriptionEnd;
    }

    /**
     * @param subscriptionStart
     *            the subscriptionStart to set
     */
    public void setSubscriptionStart(LocalDateTime subscriptionStart) {
        this.subscriptionStart = subscriptionStart;
    }

    /**
     * @param unlocode
     *            the unlocode to set
     */
    public void setUnlocode(String unlocode) {
        this.unlocode = unlocode;
    }

    /**
     * @param wkt
     *            the wkt to set
     */
    public void setWkt(String wkt) {
        this.wkt = wkt;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "SubscriptionEntity [id=" + id + ", mrn=" + mrn + ", geometry=" + null + ", subscriptionStart=" + subscriptionStart + ", subscriptionEnd="
                + subscriptionEnd + "]";
    }
}
