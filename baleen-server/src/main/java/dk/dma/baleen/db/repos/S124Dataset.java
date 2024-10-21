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
package dk.dma.baleen.db.repos;

import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 *
 */
@Entity
@Table(name = "s124dataset", indexes = {
        @Index(name = "idx_sha256Hash", columnList = "sha256Hash")
})
public class S124Dataset extends PanacheEntity {

    /** unlocode and wkt */
  //  @Column(name = "geometry")
    ///private Geometry geometry;

    @Column(name = "uuid")
    private UUID uuid;

    @Column(name = "sha256Hash", unique = true) // Ensures sha256Hash is unique
    private String sha256Hash;

    private String xmlContent;

//    // Getter and Setter methods
//    public Geometry getGeometry() {
//        return geometry;
//    }
//
//    public void setGeometry(Geometry geometry) {
//        this.geometry = geometry;
//    }

    /**
     * @return the xmlContent
     */
    public String getXmlContent() {
        return xmlContent;
    }

    /**
     * @param xmlContent the xmlContent to set
     */
    public void setXmlContent(String xmlContent) {
        this.xmlContent = xmlContent;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }

    // Static method to find an entity by sha256Hash
    public static S124Dataset findBySha256Hash(String sha256Hash) {
        return find("sha256Hash", sha256Hash).firstResult();
    }
}
