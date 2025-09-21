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

package dk.dma.baleen.service.s124.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@Table(name = "s100_data_set_identification_entity")
public class S100DataSetIdentificationEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String encodingSpecification;

    @Column(nullable = false)
    private String encodingSpecificationEdition;

    @Column(nullable = false)
    private String productIdentifier;

    @Column(nullable = false)
    private String productEdition;

    @Column(nullable = false)
    private String applicationProfile;

    @Column(nullable = false)
    private String datasetFileIdentifier;

    @Column(nullable = false)
    private String datasetTitle;

    @Column(nullable = false)
    @LastModifiedDate
    @Temporal(TemporalType.DATE)
    private LocalDate datasetReferenceDate;

    @Column(nullable = false, length = 3)
    private String datasetLanguage;

    @Column(columnDefinition = "TEXT")
    private String datasetAbstract;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEncodingSpecification() {
        return encodingSpecification;
    }

    public void setEncodingSpecification(String encodingSpecification) {
        this.encodingSpecification = encodingSpecification;
    }

    public String getEncodingSpecificationEdition() {
        return encodingSpecificationEdition;
    }

    public void setEncodingSpecificationEdition(String encodingSpecificationEdition) {
        this.encodingSpecificationEdition = encodingSpecificationEdition;
    }

    public String getProductIdentifier() {
        return productIdentifier;
    }

    public void setProductIdentifier(String productIdentifier) {
        this.productIdentifier = productIdentifier;
    }

    public String getProductEdition() {
        return productEdition;
    }

    public void setProductEdition(String productEdition) {
        this.productEdition = productEdition;
    }

    public String getApplicationProfile() {
        return applicationProfile;
    }

    public void setApplicationProfile(String applicationProfile) {
        this.applicationProfile = applicationProfile;
    }

    public String getDatasetFileIdentifier() {
        return datasetFileIdentifier;
    }

    public void setDatasetFileIdentifier(String datasetFileIdentifier) {
        this.datasetFileIdentifier = datasetFileIdentifier;
    }

    public String getDatasetTitle() {
        return datasetTitle;
    }

    public void setDatasetTitle(String datasetTitle) {
        this.datasetTitle = datasetTitle;
    }

    public LocalDate getDatasetReferenceDate() {
        return datasetReferenceDate;
    }

    public void setDatasetReferenceDate(LocalDate datasetReferenceDate) {
        this.datasetReferenceDate = datasetReferenceDate;
    }

    public String getDatasetLanguage() {
        return datasetLanguage;
    }

    public void setDatasetLanguage(String datasetLanguage) {
        this.datasetLanguage = datasetLanguage;
    }

    public String getDatasetAbstract() {
        return datasetAbstract;
    }

    public void setDatasetAbstract(String datasetAbstract) {
        this.datasetAbstract = datasetAbstract;
    }


    // Copy method
    public S100DataSetIdentificationEntity copy() {
        S100DataSetIdentificationEntity copy = new S100DataSetIdentificationEntity();
        copy.setEncodingSpecification(this.encodingSpecification);
        copy.setEncodingSpecificationEdition(this.encodingSpecificationEdition);
        copy.setProductIdentifier(this.productIdentifier);
        copy.setProductEdition(this.productEdition);
        copy.setApplicationProfile(this.applicationProfile);
        copy.setDatasetFileIdentifier(this.datasetFileIdentifier);
        copy.setDatasetTitle(this.datasetTitle);
        copy.setDatasetReferenceDate(this.datasetReferenceDate);
        copy.setDatasetLanguage(this.datasetLanguage);
        copy.setDatasetAbstract(this.datasetAbstract);
        return copy;
    }
}