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
package dk.dma.baleen.connector.secom.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** A SECOM node represents a remote node we have communicated with. */
@Entity
public class SecomNodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    /** Last interaction with the node. */
    @Column
    private Instant lastInteraction;

    @Column(nullable = false, unique = true, updatable = false)
    private String mrn;

    /**
     * @return the id
     */
    public UUID getId() {
        return id;
    }

    /**
     * @return the lastInteraction
     */
    public Instant getLastInteraction() {
        return lastInteraction;
    }

    /**
     * @return the mrn
     */
    public String getMrn() {
        return mrn;
    }

    /**
     * @param lastInteraction
     *            the lastInteraction to set
     */
    public void setLastInteraction(Instant lastInteraction) {
        this.lastInteraction = lastInteraction;
    }

    /**
     * @param mrn
     *            the mrn to set
     */
    public void setMrn(String mrn) {
        this.mrn = mrn;
    }
}
