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
package dk.dma.baleen.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import dk.dma.baleen.service.s124.NiordApiCaller2;
import dk.dma.baleen.service.s124.repository.S124DatasetInstanceRepository;
import dk.dma.baleen.service.s124.service.S124Service;
import dk.dma.baleen.service.spi.DataSet;

/**
 * The stored datasets and the ones SECOM serves come from different sources and nothing keeps them in sync, so the UI
 * used to show the row count of the store as if it were what SECOM hands out. The number reported as served has to be
 * the one SECOM answers with, which means asking the service that answers it.
 */
class S124DatasetControllerServedCountTest {

    @Test
    void servedCountIsWhatSecomServes() {
        S124DatasetInstanceRepository repository = mock(S124DatasetInstanceRepository.class);
        S124Service s124Service = serviceServing(3);
        S124DatasetController controller = new S124DatasetController(repository, mock(NiordApiCaller2.class), s124Service);

        assertThat(controller.getServedDatasetCount().getBody()).isEqualTo(3L);

        // The store is a snapshot of an earlier load and says nothing about what SECOM is serving now.
        verifyNoInteractions(repository);
    }

    @Test
    void servedCountIsZeroWhenSecomServesNothing() {
        S124DatasetController controller = new S124DatasetController(mock(S124DatasetInstanceRepository.class), mock(NiordApiCaller2.class), serviceServing(0));

        assertThat(controller.getServedDatasetCount().getBody()).isZero();
    }

    /** The stored count stays the store's own, so the two endpoints keep reporting different things on purpose. */
    @Test
    void storedCountIsTheRowCountOfTheStore() {
        S124DatasetInstanceRepository repository = mock(S124DatasetInstanceRepository.class);
        when(repository.count()).thenReturn(7L);
        S124DatasetController controller = new S124DatasetController(repository, mock(NiordApiCaller2.class), mock(S124Service.class));

        assertThat(controller.getDatasetCount().getBody()).isEqualTo(7L);
    }

    /** {@return a service whose query matches {@code count} datasets, all of them on one unpaged page} */
    private static S124Service serviceServing(int count) {
        List<DataSet> datasets = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            datasets.add(dataset());
        }

        S124Service service = mock(S124Service.class);
        // doReturn, because the wildcard in the declared Page<? extends DataSet> does not accept a Page<DataSet>
        // through the type checked when(...).thenReturn(...)
        doReturn(new PageImpl<>(datasets)).when(service).findAll(any(), any(), any(), any(), any());
        return service;
    }

    private static DataSet dataset() {
        return new DataSet() {

            private final UUID uuid = UUID.randomUUID();

            @Override
            public byte[] toByteArray() {
                return "<Dataset/>".getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public UUID uuid() {
                return uuid;
            }
        };
    }
}
