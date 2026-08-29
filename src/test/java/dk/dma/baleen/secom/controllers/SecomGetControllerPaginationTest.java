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
package dk.dma.baleen.secom.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.grad.secomv2.core.models.GetResponseObject;
import org.grad.secomv2.core.models.GetSummaryResponseObject;
import org.grad.secomv2.core.models.enums.ContainerTypeEnum;
import org.grad.secomv2.core.models.enums.SECOM_DataProductType;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import dk.dma.baleen.secom.service.SecomGetService;
import dk.dma.baleen.service.spi.DataSet;
import jakarta.servlet.http.HttpServletRequest;

/**
 * A paged response must report how many datasets matched the query, not how many objects this page carries. An
 * exchange set packages a whole page into a single response object, so counting those would tell the client
 * everything arrived and leave the remaining pages unfetched.
 * <p>
 * The page numbers here are the SECOM ones, which start at 1.
 */
class SecomGetControllerPaginationTest {

    private static final int PAGE_SIZE = 10;

    private static final int TOTAL = 25;

    @Test
    void exchangeSetPageReportsAllMatchingDatasetsAsTotal() {
        SecomGetService service = pagedService();
        SecomGetController controller = controller(service);
        when(service.createExchangeSet(eq(SECOM_DataProductType.S124), any())).thenReturn(new byte[] { 'P', 'K' });

        GetResponseObject response = controller.get(null, ContainerTypeEnum.S100_ExchangeSet, SECOM_DataProductType.S124, null, null, null, null, null, 1,
                PAGE_SIZE);

        assertThat(response.getDataResponseObject()).hasSize(1); // the page, packaged as one exchange set
        assertThat(response.getPagination().getTotalItems()).isEqualTo(TOTAL);
        assertThat(response.getPagination().getMaxItemsPerPage()).isEqualTo(PAGE_SIZE);
    }

    @Test
    void datasetPageReportsAllMatchingDatasetsAsTotal() {
        SecomGetController controller = controller(pagedService());

        GetResponseObject response = controller.get(null, ContainerTypeEnum.S100_DataSet, SECOM_DataProductType.S124, null, null, null, null, null, 1, PAGE_SIZE);

        assertThat(response.getDataResponseObject()).hasSize(PAGE_SIZE);
        assertThat(response.getPagination().getTotalItems()).isEqualTo(TOTAL);
    }

    @Test
    void summaryPageReportsAllMatchingDatasetsAsTotal() {
        SecomGetController controller = controller(pagedService());

        GetSummaryResponseObject response = controller.getSummary(ContainerTypeEnum.S100_DataSet, SECOM_DataProductType.S124, null, null, null, null, null, 1,
                PAGE_SIZE);

        assertThat(response.getSummaryObject()).hasSize(PAGE_SIZE);
        assertThat(response.getPagination().getTotalItems()).isEqualTo(TOTAL);
    }

    /** A service holding {@value #TOTAL} datasets and returning them one {@value #PAGE_SIZE} sized page at a time. */
    private static SecomGetService pagedService() {
        List<DataSet> page = new ArrayList<>();
        for (int i = 0; i < PAGE_SIZE; i++) {
            page.add(dataset());
        }
        Page<DataSet> paged = new PageImpl<>(page, PageRequest.of(0, PAGE_SIZE), TOTAL);

        SecomGetService service = mock(SecomGetService.class);
        // doReturn, because the wildcard in the declared Page<? extends DataSet> does not accept a
        // Page<DataSet> through the type checked when(...).thenReturn(...)
        doReturn(paged).when(service).get(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt());
        return service;
    }

    private static SecomGetController controller(SecomGetService service) {
        SecomGetController controller = new SecomGetController(service);
        controller.httpServletRequest = mock(HttpServletRequest.class); // no client certificate, so no MRN
        return controller;
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
