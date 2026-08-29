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
package dk.dma.baleen.secom.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.grad.secomv2.core.models.enums.SECOM_DataProductType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import dk.dma.baleen.service.spi.DataSet;
import dk.dma.baleen.service.spi.S100DataProductService;
import dk.dma.baleen.service.spi.S100DataProductType;

/**
 * SECOM numbers pages from 1 and Spring Data numbers them from 0. Handing the SECOM number straight to a
 * {@code PageRequest} made page 1 mean the second page, so the first page worth of datasets was skipped and never
 * returned. A page size on its own was ignored outright.
 */
class SecomGetServicePagingTest {

    private S100DataProductService product;

    private SecomGetService service;

    @BeforeEach
    void setUp() {
        product = mock(S100DataProductService.class);
        doReturn(new PageImpl<DataSet>(List.of())).when(product).findAll(any(), any(), any(), any(), any());

        S100DataProductManager manager = mock(S100DataProductManager.class);
        when(manager.find(S100DataProductType.S124)).thenReturn(Optional.of(product));
        service = new SecomGetService(manager);
    }

    @Test
    void theFirstSecomPageIsTheFirstPage() {
        Pageable first = pageableFor(1, 10);

        assertThat(first.getPageNumber()).isZero();
        assertThat(first.getOffset()).isZero();
    }

    @Test
    void theSecondSecomPageSkipsExactlyOnePage() {
        Pageable second = pageableFor(2, 10);

        assertThat(second.getPageNumber()).isEqualTo(1);
        assertThat(second.getOffset()).isEqualTo(10);
    }

    /** Asking for a size without a number can only mean the first page of that size. */
    @Test
    void aPageSizeOnItsOwnLimitsTheFirstPage() {
        Pageable pageable = pageableFor(null, 10);

        assertThat(pageable.isPaged()).isTrue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
    }

    @Test
    void aQueryThatDoesNotPageIsUnpaged() {
        assertThat(pageableFor(null, null).isUnpaged()).isTrue();
    }

    /**
     * Bean validation rejects a page below 1 before the service sees it. Should anything get past it, the first page
     * is a better answer than the exception {@code PageRequest.of(-1, ...)} would throw.
     */
    @Test
    void aPageNumberBelowTheFirstDoesNotBlowUp() {
        assertThat(pageableFor(0, 10).getPageNumber()).isZero();
    }

    /** {@return the page the product service is asked for when the request names {@code page} and {@code pageSize}} */
    private Pageable pageableFor(Integer page, Integer pageSize) {
        service.get(null, null, SECOM_DataProductType.S124, null, null, null, null, null, null, page, pageSize);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(product).findAll(any(), any(), any(), any(), pageable.capture());
        return pageable.getValue();
    }
}
