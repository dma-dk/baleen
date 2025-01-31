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
package dk.dma.baleen.connector.secom.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.grad.secom.core.models.enums.SECOM_DataProductType;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dk.dma.baleen.connector.secom.model.SecomSubscriberEntity;

@Repository
public interface SecomSubscriberRepository extends JpaRepository<SecomSubscriberEntity, UUID> {

    // This will join with secom_node table through the node relationship
    Optional<SecomSubscriberEntity> findByNode_Mrn(String mrn);

    // Find active subscribers matching the criteria
    @Query("SELECT s FROM SecomSubscriberEntity s " + ""
            /*         "WHERE s.dataProductType = :dataProductType " +
           "AND s.productVersion = :productVersion " +
           "AND s.subscriptionStart <= :now " +
           "AND s.subscriptionEnd >= :now " +
        "AND s.isActive = true " +
           "AND (:dataReference IS NULL OR (s.dataReference IS NOT NULL AND s.dataReference = :dataReference))" // +
  /*         "AND (:geometry IS NULL OR ST_Intersects(s.geometry, :geometry) = true)" */
)
    List<SecomSubscriberEntity> findActiveSubscribers(
            @Param("dataProductType") SECOM_DataProductType dataProductType,
            @Param("productVersion") String productVersion,
            @Param("dataReference") UUID dataReference,
            @Param("geometry") Geometry geometry,
            @Param("now") Instant now
    );

    static String toID(String mrn, UUID uuid) {
        return mrn + uuid.toString();
    }
}