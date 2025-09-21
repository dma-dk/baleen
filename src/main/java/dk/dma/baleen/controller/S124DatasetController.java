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

package dk.dma.baleen.controller;

import dk.dma.baleen.service.s124.model.S124DatasetInstanceEntity;
import dk.dma.baleen.service.s124.repository.S124DatasetInstanceRepository;
import dk.dma.baleen.service.s124.NiordApiCaller2;
import dk.dma.baleen.service.s124.service.S124Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/s124-datasets")
public class S124DatasetController {

    private static final Logger log = LoggerFactory.getLogger(S124DatasetController.class);
    
    private final S124DatasetInstanceRepository repository;
    private final NiordApiCaller2 niordApiCaller;
    private final S124Service s124Service;

    public S124DatasetController(S124DatasetInstanceRepository repository, 
                                NiordApiCaller2 niordApiCaller,
                                S124Service s124Service) {
        this.repository = repository;
        this.niordApiCaller = niordApiCaller;
        this.s124Service = s124Service;
    }

    @GetMapping
    public ResponseEntity<Page<S124DatasetDto>> getAllDatasets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<S124DatasetInstanceEntity> datasets = repository.findAll(pageRequest);
        Page<S124DatasetDto> dtoPage = datasets.map(S124DatasetDto::fromEntity);
        
        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<S124DatasetDto> getDataset(@PathVariable Long id) {
        Optional<S124DatasetInstanceEntity> dataset = repository.findById(id);
        return dataset.map(entity -> ResponseEntity.ok(S124DatasetDto.fromEntity(entity)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<S124DatasetDetailDto> getDatasetDetails(@PathVariable Long id) {
        Optional<S124DatasetInstanceEntity> dataset = repository.findById(id);
        return dataset.map(entity -> ResponseEntity.ok(S124DatasetDetailDto.fromEntity(entity)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getDatasetCount() {
        return ResponseEntity.ok(repository.count());
    }
    
    @GetMapping("/niord-status")
    public ResponseEntity<NiordStatus> getNiordStatus() {
        boolean configured = niordApiCaller.isNiordEndpointConfigured();
        return ResponseEntity.ok(new NiordStatus(configured));
    }
    
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearAllDatasets() {
        log.info("Clearing all S124 datasets");
        repository.deleteAll();
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/reload-from-niord")
    public ResponseEntity<ReloadResult> reloadFromNiord() {
        if (!niordApiCaller.isNiordEndpointConfigured()) {
            return ResponseEntity.badRequest().body(new ReloadResult(false, 0, "Niord endpoint not configured"));
        }
        
        try {
            log.info("Starting reload of S124 datasets from Niord");
            
            // Clear existing datasets
            repository.deleteAll();
            
            // Fetch all datasets from Niord
            List<NiordApiCaller2.Result> results = niordApiCaller.fetchAll();
            
            int successCount = 0;
            for (NiordApiCaller2.Result result : results) {
                try {
                    s124Service.upload(result.xml());
                    successCount++;
                } catch (Exception e) {
                    log.error("Failed to upload dataset: {}", e.getMessage());
                }
            }
            
            log.info("Reload completed. Uploaded {} out of {} datasets", successCount, results.size());
            return ResponseEntity.ok(new ReloadResult(true, successCount, 
                String.format("Successfully loaded %d datasets from Niord", successCount)));
            
        } catch (Exception e) {
            log.error("Failed to reload datasets from Niord", e);
            return ResponseEntity.internalServerError()
                .body(new ReloadResult(false, 0, "Failed to reload: " + e.getMessage()));
        }
    }
    
    // Additional DTOs
    public record NiordStatus(boolean configured) {}
    
    public record ReloadResult(boolean success, int datasetsLoaded, String message) {}

    // DTO class for API responses
    public record S124DatasetDto(
            Long id,
            String mrn,
            String uuid,
            Instant createdAt,
            Instant validFrom,
            Instant validTo,
            String dataProductVersion,
            String geometryWkt,
            List<Long> referencedDatasetIds
    ) {
        public static S124DatasetDto fromEntity(S124DatasetInstanceEntity entity) {
            return new S124DatasetDto(
                    entity.getId(),
                    entity.getMrn(),
                    entity.getUuid() != null ? entity.getUuid().toString() : null,
                    entity.getCreatedAt(),
                    entity.getValidFrom(),
                    entity.getValidTo(),
                    entity.getDataProductVersion(),
                    entity.getGeometry() != null ? entity.getGeometry().toText() : null,
                    entity.getReferences().stream().map(S124DatasetInstanceEntity::getId).toList()
            );
        }
    }

    // Detailed DTO class for API responses including GML content
    public record S124DatasetDetailDto(
            Long id,
            String mrn,
            String uuid,
            Instant createdAt,
            Instant validFrom,
            Instant validTo,
            String dataProductVersion,
            String geometryWkt,
            List<Long> referencedDatasetIds,
            String gml
    ) {
        public static S124DatasetDetailDto fromEntity(S124DatasetInstanceEntity entity) {
            return new S124DatasetDetailDto(
                    entity.getId(),
                    entity.getMrn(),
                    entity.getUuid() != null ? entity.getUuid().toString() : null,
                    entity.getCreatedAt(),
                    entity.getValidFrom(),
                    entity.getValidTo(),
                    entity.getDataProductVersion(),
                    entity.getGeometry() != null ? entity.getGeometry().toText() : null,
                    entity.getReferences().stream().map(S124DatasetInstanceEntity::getId).toList(),
                    entity.getGml()
            );
        }
    }
}