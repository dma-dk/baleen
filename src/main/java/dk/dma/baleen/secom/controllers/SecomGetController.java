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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.grad.secom.core.exceptions.SecomNotImplementedException;
import org.grad.secom.core.exceptions.SecomValidationException;
import org.grad.secom.core.interfaces.GetSecomInterface;
import org.grad.secom.core.interfaces.GetSummarySecomInterface;
import org.grad.secom.core.models.DataResponseObject;
import org.grad.secom.core.models.GetResponseObject;
import org.grad.secom.core.models.GetSummaryResponseObject;
import org.grad.secom.core.models.PaginationObject;
import org.grad.secom.core.models.SECOM_ExchangeMetadataObject;
import org.grad.secom.core.models.SummaryObject;
import org.grad.secom.core.models.enums.ContainerTypeEnum;
import org.grad.secom.core.models.enums.SECOM_DataProductType;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import dk.dma.baleen.secom.service.SecomGetService;
import dk.dma.baleen.service.spi.DataSet;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

/**
 * We implement both {@link GetSecomInterface}, {@link GetSummarySecomInterface} and {@link GetByLinkSecomInterface}
 * here.
 */
@Component
@Path("/")
@Validated
public class SecomGetController extends AbstractSecomController implements GetSecomInterface , GetSummarySecomInterface {

    /** A SECOM service that handle all get requests. */
    private SecomGetService secomGetService;

    @Autowired
    public SecomGetController(SecomGetService secomGetService) {
        this.secomGetService = requireNonNull(secomGetService);
    }

    /** {@inheritDoc} */
    @Override
    public GetResponseObject get(@QueryParam("dataReference") UUID dataReference, @QueryParam("containerType") ContainerTypeEnum containerType,
            @QueryParam("dataProductType") SECOM_DataProductType dataProductType, @QueryParam("productVersion") String productVersion,
            @QueryParam("geometry") String geometry, @QueryParam("unlocode") @Pattern(regexp = "[A-Z]{5}") String unlocode,
            @QueryParam("validFrom") @Parameter(example = "20200101T123000", schema = @Schema(implementation = String.class, pattern = "(\\d{8})T(\\d{6})")) LocalDateTime validFrom,
            @QueryParam("validTo") @Parameter(example = "20200101T123000", schema = @Schema(implementation = String.class, pattern = "(\\d{8})T(\\d{6})")) LocalDateTime validTo,
            @QueryParam("page") @Min(0) Integer page, @QueryParam("pageSize") @Min(0) Integer pageSize) {
        if (containerType == ContainerTypeEnum.NONE) {
            throw new SecomValidationException("NONE cannot be specified for containerType");
        }
        if (containerType == null) {
            containerType = ContainerTypeEnum.S100_DataSet;
        }

        // Find all data from th
        Page<? extends DataSet> data = get0(dataReference, dataProductType, productVersion, geometry, unlocode, validFrom, validTo, page, pageSize);

        List<DataResponseObject> objects = new ArrayList<>();

        if (containerType == ContainerTypeEnum.S100_DataSet) {
            for (DataSet ds : data) {
                DataResponseObject dro = new DataResponseObject();
                dro.setData(ds.toByteArray());

                SECOM_ExchangeMetadataObject emo = new SECOM_ExchangeMetadataObject();
                emo.setCompressionFlag(false);
                emo.setDataProtection(false);
                dro.setExchangeMetadata(emo);

                objects.add(dro);
            }
        } else if (containerType == ContainerTypeEnum.S100_ExchangeSet) {
            throw new SecomNotImplementedException("Baleen does not currently support exchange sets");
        }

        GetResponseObject response = new GetResponseObject();
        response.setDataResponseObject(objects);
        response.setPagination(new PaginationObject(objects.size(), Optional.ofNullable(pageSize).orElse(Integer.MAX_VALUE)));
        response.setResponseText(objects.size() + " datasets returned");
        return response;
    }

    private Page<? extends DataSet> get0(UUID dataReference, SECOM_DataProductType dataProductType, String productVersion, String geometry, String unlocode,
            LocalDateTime validFrom, LocalDateTime validTo, Integer page, Integer pageSize) {

        // It is actually legal to not specify one. But then we would need to aggregate over all products
        if (dataProductType == null) {
            throw new SecomNotImplementedException("A data product type is required to be specified");
        }
        // Going forward we should require a version.
        // We should mandata version

        Geometry jtsGeometry = parseGeometry(geometry, unlocode);

        return secomGetService.get(mrn(), dataReference, dataProductType, productVersion, geometry, unlocode, jtsGeometry, validFrom, validTo, page, pageSize);
    }

    /** {@inheritDoc} */
    @Override
    public GetSummaryResponseObject getSummary(@QueryParam("containerType") ContainerTypeEnum containerType,
            @QueryParam("dataProductType") SECOM_DataProductType dataProductType, @QueryParam("productVersion") String productVersion,
            @QueryParam("geometry") String geometry, @QueryParam("unlocode") @Pattern(regexp = "[A-Z]{5}") String unlocode,
            @QueryParam("validFrom") @Parameter(example = "20200101T123000", schema = @Schema(implementation = String.class, pattern = "(\\d{8})T(\\d{6})")) LocalDateTime validFrom,
            @QueryParam("validTo") @Parameter(example = "20200101T123000", schema = @Schema(implementation = String.class, pattern = "(\\d{8})T(\\d{6})")) LocalDateTime validTo,
            @QueryParam("page") @Min(0) Integer page, @QueryParam("pageSize") @Min(0) Integer pageSize) {

        // containerType has mandatory processing, but have no idea what do with it

        // Find all relevant data
        Page<? extends DataSet> data = get0(null, dataProductType, productVersion, geometry, unlocode, validFrom, validTo, page, pageSize);

        // Create the summary object
        List<SummaryObject> summaryObjects = new ArrayList<>();
        for (DataSet ds : data) {
            SummaryObject so = new SummaryObject();
            so.setDataReference(ds.uuid());
            so.setDataProtection(Boolean.FALSE);
            so.setDataCompression(Boolean.FALSE);
            so.setContainerType(containerType);
            so.setDataProductType(dataProductType);
            so.setInfo_size(ds.toByteArray().length % 1024L);
        }

        // Create and return the response
        GetSummaryResponseObject response = new GetSummaryResponseObject();
        response.setSummaryObject(summaryObjects);
        response.setPagination(new PaginationObject(summaryObjects.size(), pageSize == null ? Integer.MAX_VALUE : pageSize));
        return response;
    }
}
