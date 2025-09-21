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

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 *
 */
@MappedSuperclass
public abstract class S100GmlDatasetInstanceEntity {

    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private Instant createdAt;

    /** The s-124 data product version */
    @Column
    private String dataProductVersion;

    @Column(columnDefinition = "TEXT")
    private String gml;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * @return the createdAt
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * @return the dataProductVersion
     */
    public String getDataProductVersion() {
        return dataProductVersion;
    }

    /**
     * @return the gml
     */
    public String getGml() {
        return gml;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param dataProductVersion the dataProductVersion to set
     */
    public void setDataProductVersion(String dataProductVersion) {
        this.dataProductVersion = dataProductVersion;
    }

    /**
     * @param gml the gml to set
     */
    public void setGml(String gml) {
        this.gml = gml;
    }

}
