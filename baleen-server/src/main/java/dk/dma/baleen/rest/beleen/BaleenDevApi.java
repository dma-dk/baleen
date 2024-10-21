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
package dk.dma.baleen.rest.beleen;

import org.grad.secom.core.exceptions.SecomNotFoundException;

import _int.iho.s124._1.Dataset;
import dk.dma.baleen.service.mcp.MCPServiceRegistryClient;
import dk.dma.baleen.service.secom.SecomSubscriptionServiceV1;
import dma.dk.baleen.s124.utils.S124Utils;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.xml.bind.JAXBException;

/**
 * A temporary API for development.
 */
@Path("/dev")
public class BaleenDevApi {

    // signature + publicKey is base 64 encoded

    // Think should return something, ADDED, Already_exist_did_nothing, UPDATE Not supported
    // We do not support updates

    @Inject
    MCPServiceRegistryClient finder;

    @Inject
    SecomSubscriptionServiceV1 service;

    @POST
    @Path("uploadamsa")
    public void accept() {
        service.publish(AmsaTestData.doc);
    }

    @GET
    @Path("mrnResolve")
    public String resolveMrn(@QueryParam("mrn") String mrn) {
        try {
            return finder.resolve(mrn).baseUri;
        } catch (SecomNotFoundException e) {
            return "MRN Not found";
        }
    }

    static class AmsaTestData {

        static final String doc = """
                            <Dataset xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xmlns:xlink="http://www.w3.org/1999/xlink"
                         xmlns:gml="http://www.opengis.net/gml/3.2"
                         xmlns:S100="http://www.iho.int/s100gml/5.0"
                         xmlns:S124="http://www.iho.int/S124/gml/cs0/1.0"
                         xmlns="http://www.iho.int/S124/1.0"
                         xsi:schemaLocation="http://www.iho.int/S124/1.0 S-124_GML_Schemas_1.0.0.xsd"
                         gml:id="ds">
                    <gml:boundedBy>
                        <gml:Envelope>
                            <gml:lowerCorner>119.3596667 -17.2875000</gml:lowerCorner>
                            <gml:upperCorner>119.3596667 -17.2875000</gml:upperCorner>
                        </gml:Envelope>
                    </gml:boundedBy>

                    <S100:DatasetIdentificationInformation>
                        <S100:encodingSpecification>S-100 Part 10b</S100:encodingSpecification>
                        <S100:encodingSpecificationEdition>1.0</S100:encodingSpecificationEdition>
                        <S100:productIdentifier>S-124</S100:productIdentifier>
                        <S100:productEdition>1.0.0</S100:productEdition>
                        <S100:applicationProfile>5.0</S100:applicationProfile>
                        <S100:datasetFileIdentifier>vdes-trial</S100:datasetFileIdentifier>
                        <S100:datasetTitle>AMSA JRCC Navigational Warning TRIAL Dataset - DO NOT USE FOR NAVIGATION</S100:datasetTitle>
                        <S100:datasetReferenceDate>2023-10-09</S100:datasetReferenceDate>
                        <S100:datasetLanguage>eng</S100:datasetLanguage>
                        <S100:datasetAbstract>This Dataset was created to trial VDES transmission - DO NOT USE FOR NAVIGATION. It is based on data from the AMSA JRCC.</S100:datasetAbstract>
                        <S100:datasetTopicCategory>transportation</S100:datasetTopicCategory>
                        <S100:datasetPurpose>base</S100:datasetPurpose>
                        <S100:updateNumber>0</S100:updateNumber>
                    </S100:DatasetIdentificationInformation>
                    <members>
                        <NAVWARNPreamble gml:id="f2">
                            <generalArea>
                                <localityIdentifier>urn:mrn:amsa:navareaX:tbd</localityIdentifier>
                                <locationName xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
                            </generalArea>
                            <messageSeriesIdentifier>
                                <agencyResponsibleForProduction>Australian Maritime Safety Authority</agencyResponsibleForProduction>
                                <countryName>AU</countryName>
                                <nameOfSeries>AUSCOAST WARNING</nameOfSeries>
                                <warningNumber>232</warningNumber>
                                <warningType code="2">Coastal Navigational Warning</warningType>
                                <year>23</year>
                            </messageSeriesIdentifier>
                            <intService xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
                            <navwarnTypeGeneral code="10">Newly Discovered Dangers</navwarnTypeGeneral>
                            <publicationTime xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
                        </NAVWARNPreamble>
                        <NAVWARNPart gml:id="ID001">
                            <gml:boundedBy xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
                            <fixedDateRange>
                                <dateStart>
                                    <S100:date>2023-10-02</S100:date>
                                </dateStart>
                            </fixedDateRange>
                            <warningInformation>
                                <information>
                                    <language>en</language>
                                    <text>SECURITE
                   FM JRCC AUSTRALIA 020228Z OCT 23
                   AUSCOAST WARNING 232/23
                   SEMI-SUBMERGED WRECK REPORTED IN POSITION 17-17.25S 119-21.58E

                NNNN</text>
                                </information>
                                <navwarnTypeDetails code="73">Dangerous Wreck</navwarnTypeDetails>
                            </warningInformation>
                            <header xlink:href="ID001"/>
                            <geometry>
                                <S100:pointProperty>
                                    <S100:Point gml:id="ac712bc3-1387-4104-b2ef-467e9f5d529b">
                                        <gml:pos>119.3596667 -17.2875000</gml:pos>
                                    </S100:Point>
                                </S100:pointProperty>
                            </geometry>
                        </NAVWARNPart>
                    </members>
                </Dataset>
                            """;

        public static void main(String[] args) throws JAXBException {
            Dataset unmarshallS124 = S124Utils.unmarshallS124(doc);
            System.out.println(unmarshallS124);
        }

    }
}
