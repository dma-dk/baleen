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

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.dma.baleen.secom.model.SecomNodeEntity;

@Repository
public interface SecomNodeRepository extends JpaRepository<SecomNodeEntity, UUID> {

    Optional<SecomNodeEntity> findByMrn(String mrn);

    default SecomNodeEntity findOrCreate(String mrn) {
        return findByMrn(mrn).orElseGet(() -> {
            SecomNodeEntity entity = new SecomNodeEntity();
            entity.setMrn(mrn);
            return save(entity);
        });
    }
}
