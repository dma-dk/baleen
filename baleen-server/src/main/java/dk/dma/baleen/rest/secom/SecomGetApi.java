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
package dk.dma.baleen.rest.secom;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.grad.secom.core.interfaces.GetSecomInterface;
import org.grad.secom.core.models.DataResponseObject;
import org.grad.secom.core.models.GetResponseObject;
import org.grad.secom.core.models.PaginationObject;
import org.grad.secom.core.models.SECOM_ExchangeMetadataObject;
import org.grad.secom.core.models.enums.ContainerTypeEnum;
import org.grad.secom.core.models.enums.SECOM_DataProductType;
import org.jboss.logging.Logger;
import org.locationtech.jts.geom.Geometry;

import dk.dma.baleen.client.NiordApiCaller;
import dk.dma.baleen.client.NiordApiCaller.Result;
import dk.dma.baleen.util.GeometryUtils;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.ValidationException;

@Path(AbstractSecomApi.SECOM_ROOT_PATH)
public class SecomGetApi extends AbstractSecomApi implements GetSecomInterface {

    private static final Logger log = Logger.getLogger(SecomGetApi.class);

    @Inject
    NiordApiCaller niordApi;


    @SuppressWarnings("unused")
    @Override
    @Transactional
    public GetResponseObject get(@QueryParam("dataReference") UUID dataReference, @QueryParam("containerType") ContainerTypeEnum containerType,
            @QueryParam("dataProductType") SECOM_DataProductType dataProductType, @QueryParam("productVersion") String productVersion,
            @QueryParam("geometry") String geometry, @QueryParam("unlocode") @Pattern(regexp = "[A-Z]{5}") String unlocode,
            @QueryParam("validFrom") @Parameter(example = "20200101T123000", schema = @Schema(implementation = String.class, pattern = "(\\d{8})T(\\d{6})")) LocalDateTime validFrom,
            @QueryParam("validTo") @Parameter(example = "20200101T123000", schema = @Schema(implementation = String.class, pattern = "(\\d{8})T(\\d{6})")) LocalDateTime validTo,
            @QueryParam("page") @Min(0) Integer page, @QueryParam("pageSize") @Min(0) Integer pageSize) {
        log.debug("SECOM request to get page of Dataset");
        Optional.ofNullable(dataReference).ifPresent(v -> log.debugv("Data Reference specified as: {}", dataReference));
        Optional.ofNullable(containerType).ifPresent(v -> log.debugv("Container Type specified as: {}", containerType));
        Optional.ofNullable(dataProductType).ifPresent(v -> log.debugv("Data Product Type specified as: {}", dataProductType));
        Optional.ofNullable(geometry).ifPresent(v -> log.debugv("Geometry specified as: {}", geometry));
        Optional.ofNullable(unlocode).ifPresent(v -> log.debugv("UNLOCODE specified as: {}", unlocode));
        Optional.ofNullable(validFrom).ifPresent(v -> log.debugv("Valid From time specified as: {}", validFrom));
        Optional.ofNullable(validTo).ifPresent(v -> log.debugv("Valid To time specified as: {}", validTo));

        try {
        //    final String mrn = httpHeaders.getHeaderString("X-MRN");
         //   System.out.println("GOT A Get request from " + mrn);

            // Parse the arguments
            ContainerTypeEnum reqContainerType = containerType == null ? ContainerTypeEnum.S100_DataSet : containerType;

            SECOM_DataProductType reqDataProductType = dataProductType == null ? SECOM_DataProductType.S124 : dataProductType;

            try {
                Geometry jtsGeometry = GeometryUtils.parse(geometry, unlocode);
            } catch (ValidationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Initialise the data response object list
            final List<DataResponseObject> datasets = new ArrayList<>();

            try {
                List<Result> fetchAll = niordApi.fetchAll();
                for (Result result : fetchAll) {
                    DataResponseObject dro = new DataResponseObject();

                    // Set Data (Xml document)
                    String encoded = result.xml();
                    dro.setData(encoded.getBytes(StandardCharsets.UTF_8));

                    // Set exchange metaobject
                    SECOM_ExchangeMetadataObject emo = new SECOM_ExchangeMetadataObject();
                    emo.setCompressionFlag(false);
                    emo.setDataProtection(false);
                    dro.setExchangeMetadata(emo);

                    datasets.add(dro);
                }

            } catch (IOException | InterruptedException | JAXBException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Create the response
            GetResponseObject response = new GetResponseObject();
            response.setDataResponseObject(datasets);
            response.setPagination(new PaginationObject(datasets.size(), Optional.ofNullable(pageSize).orElse(Integer.MAX_VALUE)));
            response.setResponseText("Size " + datasets.size());
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        //
//        // We only support specifically S-125 Datasets
//        if (reqDataProductType == SECOM_DataProductType.S124) {
//            // Retrieve all matching datasets
//            Page<S125Dataset> result;
//            try {
//                result = this.datasetService.findAll(dataReference, jtsGeometry, validFrom, validTo, Boolean.FALSE, pageable);
//            } catch (Exception ex) {
//                log.error("Error while retrieving the dataset query results: {} ", ex.getMessage());
//                throw new ValidationException(ex.getMessage());
//            }
//
//            // Package as S100 Datasets
//            if (reqContainerType == ContainerTypeEnum.S100_DataSet) {
//                result.stream().map(S125Dataset::getDatasetContent).filter(Objects::nonNull).map(DatasetContent::getContent).map(String::getBytes)
//                        .map(bytes -> {
//                            // Create and populate the data response object
//                            final DataResponseObject dataResponseObject = new DataResponseObject();
//                            dataResponseObject.setData(bytes);
//
//                            // And return the data response object
//                            return dataResponseObject;
//                        }).forEach(dataResponseObjectList::add);
//
//            }
//            // Package as S100 Exchange Sets
//            else if (reqContainerType == ContainerTypeEnum.S100_ExchangeSet) {
//                // Create and populate the data response object
//                final DataResponseObject dataResponseObject = new DataResponseObject();
//                try {
//                    dataResponseObject.setData(this.s100ExchangeSetService.packageToExchangeSet(result.getContent(), validFrom, validTo));
//                } catch (IOException | JAXBException ex) {
//                    log.error("Error while packaging the exchange set response: {} ", ex.getMessage());
//                    throw new ValidationException(ex.getMessage());
//                }
//
//                // Flag that this is compressed in the exchange metadata
//                dataResponseObject.setExchangeMetadata(new SECOM_ExchangeMetadataObject());
//                dataResponseObject.getExchangeMetadata().setCompressionFlag(Boolean.TRUE);
//
//                // And add it to the data response list
//                dataResponseObjectList.add(dataResponseObject);
//            }
//        }

    }

}
