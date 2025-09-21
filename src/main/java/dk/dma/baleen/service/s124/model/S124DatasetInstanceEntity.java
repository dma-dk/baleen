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
package dk.dma.baleen.service.s124.model;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.locationtech.jts.geom.Geometry;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import dk.dma.baleen.service.spi.DataSet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

/**
 *
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
public class S124DatasetInstanceEntity extends S100GmlDatasetInstanceEntity implements DataSet {

    /** unlocode and wkt */
    @Column
    private Geometry geometry;

    @Column(unique = true)
    private String mrn;

    @ManyToMany
    @JoinTable(
        name = "s124_dataset_references",
        joinColumns = @JoinColumn(name = "dataset_id"),
        inverseJoinColumns = @JoinColumn(name = "referenced_dataset_id")
    )
    private Set<S124DatasetInstanceEntity> references = new HashSet<>();

    @Column(unique = true)
    private UUID uuid;

    @Column
    private Instant validFrom;

    @Column
    private Instant validTo;

    /**
     * Add a single reference to another dataset
     * @param reference the dataset to reference
     */
    public void addReference(S124DatasetInstanceEntity reference) {
        if (references == null) {
            references = new HashSet<>();
        }
        references.add(reference);
    }

    /**
     * @return the geometry
     */
    public Geometry getGeometry() {
        return geometry;
    }

    /**
     * @return the mrn
     */
    public String getMrn() {
        return mrn;
    }

    /**
     * @return the references to other datasets
     */
    public Set<S124DatasetInstanceEntity> getReferences() {
        return references;
    }

    /**
     * @return the uuid
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * @return the validFrom
     */
    public Instant getValidFrom() {
        return validFrom;
    }

    /**
     * @return the validTo
     */
    public Instant getValidTo() {
        return validTo;
    }

    /**
     * Remove a reference to another dataset
     * @param reference the dataset reference to remove
     */
    public void removeReference(S124DatasetInstanceEntity reference) {
        if (references != null) {
            references.remove(reference);
        }
    }

    /**
     * @param geometry
     *            the geometry to set
     */
    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    /**
     * @param mrn
     *            the mrn to set
     */
    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    /**
     * @param references the references to set
     */
    public void setReferences(Set<S124DatasetInstanceEntity> references) {
        this.references = references;
    }

    /**
     * @param uuid
     *            the uuid to set
     */
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * @param validFrom
     *            the validFrom to set
     */
    public void setValidFrom(Instant validFrom) {
        this.validFrom = validFrom;
    }

    /**
     * @param validTo
     *            the validTo to set
     */
    public void setValidTo(Instant validTo) {
        this.validTo = validTo;
    }

    /** {@inheritDoc} */
    @Override
    public byte[] toByteArray() {
        return getGml().getBytes(StandardCharsets.UTF_8);
    }

    /** {@inheritDoc} */
    @Override
    public UUID uuid() {
        return uuid;
    }
}
