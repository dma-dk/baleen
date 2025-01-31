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
package dk.dma.baleen.product.s124.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.locationtech.jts.geom.Geometry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dk.dma.baleen.product.s124.model.S124DatasetInstanceEntity;

/**
 *
 */
@Repository
public interface S124DatasetInstanceRepository extends JpaRepository<S124DatasetInstanceEntity, Long> {

    @Query("""
            SELECT s FROM S124DatasetInstanceEntity s
            WHERE (:uuid IS NULL OR s.uuid = :uuid)
            AND (:fromTime IS NULL OR s.validTo >= :fromTime)
            AND (:toTime IS NULL OR s.validFrom <= :toTime)
            """)
// removed             AND (:geometry IS NULL OR ST_Intersects(s.geometry, :geometry) = true)

    Page<S124DatasetInstanceEntity> findDatasets(
            @Param("uuid") UUID uuid,
            @Param("geometry") Geometry geometry,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            Pageable pageable
    );

    /**
     * Retrieves an entity by its mrn.
     *
     * @param mrn must not be {@literal null}.
     * @return the entity with the given mrn or {@literal Optional#empty()} if none found.
     * @throws IllegalArgumentException if {@literal mrn} is {@literal null}.
     */
    Optional<S124DatasetInstanceEntity> findByMrn(String mrn);
}
