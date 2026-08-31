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
package dk.dma.baleen.secom.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.grad.secomv2.core.models.enums.SECOM_DataProductType;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dk.dma.baleen.secom.model.SecomSubscriberEntity;

@Repository
public interface SecomSubscriberRepository extends JpaRepository<SecomSubscriberEntity, UUID> {

    // This will join with secom_node table through the node relationship
    Optional<SecomSubscriberEntity> findByNode_Mrn(String mrn);

    /**
     * {@return the subscriptions a publication with the given characteristics has to be delivered to}
     * <p>
     * A subscription only constrains the dimensions it actually filled in. Every filter column on
     * {@link SecomSubscriberEntity} is nullable, so a null column means "no restriction on this dimension" and must
     * still match - a bare equality would compare against null, evaluate to unknown and deliver nothing at all. The
     * subscription window is open ended at whichever end was left null, the same way the S-124 dataset query treats
     * a dataset with no validFrom or validTo. The publication side is never null at the call site, so it is compared
     * strictly: a subscription that asked for one specific data reference must not be handed an unrelated dataset.
     * <p>
     * There is no active flag to test - neither the entity nor the {@code secom_subscriber} table has one, a
     * subscription is deleted on unsubscribe rather than deactivated, so the surviving rows are the active ones.
     * <p>
     * TODO {@code geometry} is accepted but not filtered on. No query here runs a spatial function through JPQL and
     * none is proven against both H2GIS and PostGIS with agreeing SRIDs, and the subscribe path discards the
     * requested area anyway, so the clause stays out until a subscription can store a geometry and a test covers it.
     * It would read {@code AND (:geometry IS NULL OR s.geometry IS NULL OR st_intersects(s.geometry, :geometry) = TRUE)}.
     */
    @Query("""
            SELECT s FROM SecomSubscriberEntity s
            WHERE (s.dataProductType IS NULL OR s.dataProductType = :dataProductType)
            AND (s.productVersion IS NULL OR s.productVersion = :productVersion)
            AND (s.dataReference IS NULL OR s.dataReference = :dataReference)
            AND (s.subscriptionStart IS NULL OR s.subscriptionStart <= :now)
            AND (s.subscriptionEnd IS NULL OR s.subscriptionEnd >= :now)
            """)
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
