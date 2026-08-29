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
package dk.dma.baleen.service.s124.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dk.dma.baleen.service.s124.model.S124DatasetInstanceEntity;

/**
 *
 */
@Repository
public interface S124DatasetInstanceRepository extends JpaRepository<S124DatasetInstanceEntity, Long> {

    /**
     * {@return the stored datasets matching the query}
     * <p>
     * A dataset with no validFrom or no validTo is open ended at that end, so it must not be filtered out by a bound
     * it never declared - hence the explicit null checks rather than a bare comparison, which SQL evaluates to unknown
     * and therefore excludes.
     * <p>
     * The times are {@link Instant}s because that is what the entity stores; they used to be declared as
     * {@code LocalDateTime}, which cannot be compared against an {@code Instant} column.
     */
    @Query("""
            SELECT s FROM S124DatasetInstanceEntity s
            WHERE (:uuid IS NULL OR s.uuid = :uuid)
            AND (:fromTime IS NULL OR s.validTo IS NULL OR s.validTo >= :fromTime)
            AND (:toTime IS NULL OR s.validFrom IS NULL OR s.validFrom <= :toTime)
            """)
    Page<S124DatasetInstanceEntity> findDatasets(
            @Param("uuid") UUID uuid,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime,
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
