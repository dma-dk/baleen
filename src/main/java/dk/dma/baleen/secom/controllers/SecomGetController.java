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

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.grad.secomv2.core.exceptions.SecomValidationException;
import org.grad.secomv2.core.interfaces.GetServiceInterface;
import org.grad.secomv2.core.interfaces.GetSummaryServiceInterface;
import org.grad.secomv2.core.models.DataResponseObject;
import org.grad.secomv2.core.models.GetResponseObject;
import org.grad.secomv2.core.models.GetSummaryResponseObject;
import org.grad.secomv2.core.models.PaginationObject;
import org.grad.secomv2.core.models.ExchangeMetadata;
import org.grad.secomv2.core.models.SummaryObject;
import org.grad.secomv2.core.models.enums.ContainerTypeEnum;
import org.grad.secomv2.core.models.enums.SECOM_DataProductType;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import dk.dma.baleen.secom.service.SecomGetService;
import dk.dma.baleen.service.spi.DataSet;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

/**
 * We implement both {@link GetServiceInterface}, {@link GetSummaryServiceInterface} and {@link GetByLinkSecomInterface}
 * here.
 */
@Component
@Path("/")
@Validated
public class SecomGetController extends AbstractSecomController implements GetServiceInterface , GetSummaryServiceInterface {

    /** A SECOM service that handle all get requests. */
    private SecomGetService secomGetService;

    @Autowired
    public SecomGetController(SecomGetService secomGetService) {
        this.secomGetService = requireNonNull(secomGetService);
    }

    /** {@inheritDoc} */
    // NOTE: see getSummary below - the overriding method must not redefine the interface's
    // parameter constraint configuration (HV000151). Let constraints be inherited from
    // GetServiceInterface#get instead of redeclaring @Pattern/@Min here.
    @Override
    public GetResponseObject get(@QueryParam("dataReference") UUID dataReference, @QueryParam("containerType") ContainerTypeEnum containerType,
            @QueryParam("dataProductType") SECOM_DataProductType dataProductType, @QueryParam("productVersion") String productVersion,
            @QueryParam("geometry") String geometry, @QueryParam("unlocode") String unlocode,
            @QueryParam("validFrom") @Parameter(example = "20200101T123000", schema = @Schema(implementation = String.class, pattern = "(\\d{8})T(\\d{6})")) Instant validFrom,
            @QueryParam("validTo") @Parameter(example = "20200101T123000", schema = @Schema(implementation = String.class, pattern = "(\\d{8})T(\\d{6})")) Instant validTo,
            @QueryParam("page") Integer page, @QueryParam("pageSize") Integer pageSize) {
        if (containerType == ContainerTypeEnum.NONE) {
            throw new SecomValidationException("NONE cannot be specified for containerType");
        }
        if (containerType == null) {
            containerType = ContainerTypeEnum.S100_DataSet;
        }
        dataProductType = defaultDataProductType(dataProductType);

        // Find all data from th
        Page<? extends DataSet> data = get0(dataReference, dataProductType, productVersion, geometry, unlocode, toLocal(validFrom), toLocal(validTo), page, pageSize);

        List<DataResponseObject> objects = new ArrayList<>();

        if (containerType == ContainerTypeEnum.S100_DataSet) {
            for (DataSet ds : data) {
                DataResponseObject dro = new DataResponseObject();
                dro.setData(ds.toByteArray());

                ExchangeMetadata emo = new ExchangeMetadata();
                emo.setCompressionFlag(false);
                emo.setDataProtection(false);
                dro.setExchangeMetadata(emo);

                objects.add(dro);
            }
        } else if (containerType == ContainerTypeEnum.S100_ExchangeSet && data.hasContent()) {
            // S-100 Part 17: the whole result is one exchange set - a ZIP holding the dataset files and the
            // catalogue that describes and signs them - so it is a single response object, not one per dataset.
            DataResponseObject dro = new DataResponseObject();
            dro.setData(secomGetService.createExchangeSet(dataProductType, data.getContent()));

            ExchangeMetadata emo = new ExchangeMetadata();
            emo.setCompressionFlag(false); // the exchange set is the data format, not a SECOM compressed payload
            emo.setDataProtection(false);
            dro.setExchangeMetadata(emo);

            objects.add(dro);
        }

        GetResponseObject response = new GetResponseObject();
        response.setDataResponseObject(objects);
        // The matching datasets, not the objects returned for this page: an exchange set packages the whole
        // page into a single response object, so counting those would tell a paging client that everything
        // it asked for arrived and leave the remaining pages unfetched.
        response.setPagination(new PaginationObject(totalItems(data), Optional.ofNullable(pageSize).orElse(Integer.MAX_VALUE)));
        return response;
    }

    /** {@return the number of datasets matching the query, across all pages} */
    private static int totalItems(Page<? extends DataSet> data) {
        return (int) Math.min(data.getTotalElements(), Integer.MAX_VALUE);
    }

    private static LocalDateTime toLocal(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    /** S-124 is the only data product Baleen serves, so an unspecified data product type asks for that one. */
    private static SECOM_DataProductType defaultDataProductType(SECOM_DataProductType dataProductType) {
        return dataProductType == null ? SECOM_DataProductType.S124 : dataProductType;
    }

    private Page<? extends DataSet> get0(UUID dataReference, SECOM_DataProductType dataProductType, String productVersion, String geometry, String unlocode,
            LocalDateTime validFrom, LocalDateTime validTo, Integer page, Integer pageSize) {
        Geometry jtsGeometry = parseGeometry(geometry, unlocode);

        return secomGetService.get(mrn(), dataReference, dataProductType, productVersion, geometry, unlocode, jtsGeometry, validFrom, validTo, page, pageSize);
    }

    /** {@inheritDoc} */
    @Override
    // NOTE: Jakarta Bean Validation forbids an implementing method from adding parameter
    // constraints that the interface method does not declare (HV000151). GetSummaryServiceInterface
    // declares no parameter constraints, so getSummary must not add @Pattern/@Min here.
    public GetSummaryResponseObject getSummary(@QueryParam("containerType") ContainerTypeEnum containerType,
            @QueryParam("dataProductType") SECOM_DataProductType dataProductType, @QueryParam("productVersion") String productVersion,
            @QueryParam("geometry") String geometry, @QueryParam("unlocode") String unlocode,
            @QueryParam("validFrom") @Parameter(example = "20200101T123000", schema = @Schema(implementation = String.class, pattern = "(\\d{8})T(\\d{6})")) Instant validFrom,
            @QueryParam("validTo") @Parameter(example = "20200101T123000", schema = @Schema(implementation = String.class, pattern = "(\\d{8})T(\\d{6})")) Instant validTo,
            @QueryParam("page") Integer page, @QueryParam("pageSize") Integer pageSize) {

        // Validate/resolve the container type the same way get() does, so the summary reports a
        // container type that get() can actually deliver.
        if (containerType == ContainerTypeEnum.NONE) {
            throw new SecomValidationException("NONE cannot be specified for containerType");
        }
        if (containerType == null) {
            containerType = ContainerTypeEnum.S100_DataSet;
        }

        // Resolve this here as well, so the summary reports what was actually queried
        dataProductType = defaultDataProductType(dataProductType);

        // Find all relevant data
        Page<? extends DataSet> data = get0(null, dataProductType, productVersion, geometry, unlocode, toLocal(validFrom), toLocal(validTo), page, pageSize);

        // Create the summary object
        List<SummaryObject> summaryObjects = new ArrayList<>();
        for (DataSet ds : data) {
            SummaryObject so = new SummaryObject();
            so.setDataReference(ds.uuid());
            so.setDataProtection(Boolean.FALSE);
            so.setDataCompression(Boolean.FALSE);
            so.setContainerType(containerType);
            so.setDataProductType(dataProductType);
            if (containerType == ContainerTypeEnum.S100_DataSet) {
                // The size of what get() would return for this data reference. For an exchange set that is a
                // signed ZIP whose size is only known once the whole thing has been built and signed, which is
                // too much work for a listing - and the GML length would misstate the download - so the
                // optional attribute is left out rather than filled in wrongly.
                so.setInfo_size((long) ds.toByteArray().length);
            }
            summaryObjects.add(so);
        }

        // Create and return the response
        GetSummaryResponseObject response = new GetSummaryResponseObject();
        response.setSummaryObject(summaryObjects);
        response.setPagination(new PaginationObject(totalItems(data), pageSize == null ? Integer.MAX_VALUE : pageSize));
        return response;
    }
}
