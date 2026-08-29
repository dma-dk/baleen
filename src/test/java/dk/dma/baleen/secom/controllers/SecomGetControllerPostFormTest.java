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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.grad.secomv2.core.exceptions.SecomValidationException;
import org.grad.secomv2.core.models.EnvelopeGetFilterObject;
import org.grad.secomv2.core.models.EnvelopeGetSummaryFilterObject;
import org.grad.secomv2.core.models.GetFilterObject;
import org.grad.secomv2.core.models.GetResponseObject;
import org.grad.secomv2.core.models.GetSummaryFilterObject;
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
 * SECOM offers the same query twice: as URL parameters, and as a signed body for the callers whose geometry does
 * not fit in a URL. The capability document has a single {@code get} flag covering both, so a client that reads
 * it is entitled to either, and the two must answer alike.
 */
class SecomGetControllerPostFormTest {

    private static final int PAGE_SIZE = 10;

    private static final int TOTAL = 25;

    private static final String WKT = "POLYGON((7 54,7 58,16 58,16 54,7 54))";

    @Test
    void postGetAnswersWithTheSameDatasetsAsTheUrlForm() {
        SecomGetController controller = controller(pagedService());

        GetResponseObject viaUrl = controller.get(null, ContainerTypeEnum.S100_DataSet, SECOM_DataProductType.S124, null, null, null, null, null, 1, PAGE_SIZE);
        GetResponseObject viaBody = controller.get(getFilter(e -> {
            e.setContainerType(ContainerTypeEnum.S100_DataSet);
            e.setDataProductType(SECOM_DataProductType.S124);
            e.setPage(1);
            e.setPageSize(PAGE_SIZE);
        }));

        assertThat(viaBody.getDataResponseObject()).hasSameSizeAs(viaUrl.getDataResponseObject());
        assertThat(viaBody.getPagination().getTotalItems()).isEqualTo(viaUrl.getPagination().getTotalItems());
        assertThat(viaBody.getPagination().getMaxItemsPerPage()).isEqualTo(viaUrl.getPagination().getMaxItemsPerPage());
    }

    /** Every filter the envelope carries has to reach the query, or the body form silently answers a wider one. */
    @Test
    void postGetPassesEveryFilterOnToTheQuery() {
        SecomGetService service = pagedService();
        UUID dataReference = UUID.randomUUID();
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-12-31T00:00:00Z");

        controller(service).get(getFilter(e -> {
            e.setDataReference(dataReference);
            e.setContainerType(ContainerTypeEnum.S100_DataSet);
            e.setDataProductType(SECOM_DataProductType.S124);
            e.setProductVersion("2.0.0");
            e.setGeometry(WKT);
            e.setValidFrom(from);
            e.setValidTo(to);
            e.setPage(2);
            e.setPageSize(PAGE_SIZE);
        }));

        verify(service).get(any(), eq(dataReference), eq(SECOM_DataProductType.S124), eq("2.0.0"), eq(WKT), eq(null), any(), eq(from), eq(to), eq(2),
                eq(PAGE_SIZE));
    }

    @Test
    void postGetSummaryPassesEveryFilterOnToTheQuery() {
        SecomGetService service = pagedService();
        Instant from = Instant.parse("2026-01-01T00:00:00Z");

        GetSummaryResponseObject response = controller(service).getSummary(getSummaryFilter(e -> {
            e.setContainerType(ContainerTypeEnum.S100_DataSet);
            e.setDataProductType(SECOM_DataProductType.S124);
            e.setGeometry(WKT);
            e.setValidFrom(from);
            e.setPage(1);
            e.setPageSize(PAGE_SIZE);
        }));

        verify(service).get(any(), eq(null), eq(SECOM_DataProductType.S124), eq(null), eq(WKT), eq(null), any(), eq(from), eq(null), eq(1), eq(PAGE_SIZE));
        assertThat(response.getSummaryObject()).hasSize(PAGE_SIZE);
        assertThat(response.getPagination().getTotalItems()).isEqualTo(TOTAL);
    }

    /** The body form packages a page into one exchange set exactly as the URL form does. */
    @Test
    void postGetBuildsAnExchangeSetLikeTheUrlForm() {
        SecomGetService service = pagedService();
        when(service.createExchangeSet(eq(SECOM_DataProductType.S124), any())).thenReturn(new byte[] { 'P', 'K' });

        GetResponseObject response = controller(service).get(getFilter(e -> {
            e.setContainerType(ContainerTypeEnum.S100_ExchangeSet);
            e.setPage(1);
            e.setPageSize(PAGE_SIZE);
        }));

        assertThat(response.getDataResponseObject()).hasSize(1);
        assertThat(response.getPagination().getTotalItems()).isEqualTo(TOTAL);
    }

    /** An unspecified container type means datasets, the same default the URL form applies. */
    @Test
    void postGetDefaultsToDatasetsWhenTheEnvelopeNamesNoContainerType() {
        GetResponseObject response = controller(pagedService()).get(getFilter(e -> {
            e.setPage(1);
            e.setPageSize(PAGE_SIZE);
        }));

        assertThat(response.getDataResponseObject()).hasSize(PAGE_SIZE);
    }

    @Test
    void postGetRejectsNoneAsAContainerTypeJustAsTheUrlFormDoes() {
        SecomGetController controller = controller(pagedService());

        assertThatThrownBy(() -> controller.get(getFilter(e -> e.setContainerType(ContainerTypeEnum.NONE))))
                .isInstanceOf(SecomValidationException.class);
    }

    @Test
    void postGetRejectsARequestWithNoEnvelope() {
        SecomGetController controller = controller(pagedService());

        assertThatThrownBy(() -> controller.get(new GetFilterObject())).isInstanceOf(SecomValidationException.class);
        assertThatThrownBy(() -> controller.get((GetFilterObject) null)).isInstanceOf(SecomValidationException.class);
    }

    @Test
    void postGetSummaryRejectsARequestWithNoEnvelope() {
        SecomGetController controller = controller(pagedService());

        assertThatThrownBy(() -> controller.getSummary(new GetSummaryFilterObject())).isInstanceOf(SecomValidationException.class);
        assertThatThrownBy(() -> controller.getSummary((GetSummaryFilterObject) null)).isInstanceOf(SecomValidationException.class);
    }

    /** A signed envelope carrying the given filters, with the attributes {@code check} insists on. */
    private static GetFilterObject getFilter(java.util.function.Consumer<EnvelopeGetFilterObject> filters) {
        EnvelopeGetFilterObject envelope = new EnvelopeGetFilterObject();
        envelope.setEnvelopeRootCertificateThumbprint("thumbprint");
        envelope.setEnvelopeSignatureTime(Instant.parse("2026-08-29T00:00:00Z"));
        filters.accept(envelope);

        GetFilterObject request = new GetFilterObject();
        request.setEnvelope(envelope);
        return request;
    }

    private static GetSummaryFilterObject getSummaryFilter(java.util.function.Consumer<EnvelopeGetSummaryFilterObject> filters) {
        EnvelopeGetSummaryFilterObject envelope = new EnvelopeGetSummaryFilterObject();
        envelope.setEnvelopeRootCertificateThumbprint("thumbprint");
        envelope.setEnvelopeSignatureTime(Instant.parse("2026-08-29T00:00:00Z"));
        filters.accept(envelope);

        GetSummaryFilterObject request = new GetSummaryFilterObject();
        request.setEnvelope(envelope);
        return request;
    }

    /** A service holding {@value #TOTAL} datasets and returning them one {@value #PAGE_SIZE} sized page at a time. */
    private static SecomGetService pagedService() {
        List<DataSet> page = new ArrayList<>();
        for (int i = 0; i < PAGE_SIZE; i++) {
            page.add(dataset());
        }
        Page<DataSet> paged = new PageImpl<>(page, PageRequest.of(0, PAGE_SIZE), TOTAL);

        SecomGetService service = mock(SecomGetService.class);
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
