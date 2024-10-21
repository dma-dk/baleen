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
package dk.dma.baleen.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import _int.iho.s124._1.Dataset;
import dma.dk.baleen.s124.utils.S124Utils;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBException;

/**
 * This is temporary hack to get all messages in Niord. Even though that hasn't been promulgated
 * <p>
 * So please disregard the state of the code.
 */
@ApplicationScoped
public class NiordApiCaller {

    private final HttpClient client;
    private final ObjectMapper objectMapper;

    @Inject
    public NiordApiCaller() {
        // Initialize HttpClient with automatic redirects enabled
        this.client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        this.objectMapper = new ObjectMapper();
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        List<Result> fetchAll = new NiordApiCaller().fetchAll();
        System.out.println("Got " + fetchAll.size());
    }

    @Scheduled(every = "1m") // This schedules the method to run every 1 minute
    public void fetchData() {
        try {
            fetchAll();
        } catch (

        Exception e) {
            e.printStackTrace();
            // .error("Error occurred during API call", e);
        }
    }

    public List<Result> fetchAlXl() throws IOException, InterruptedException, JAXBException {
        String datasetString = Files.readString(Paths.get("/Users/kaspernielsen/dma/madame/documents/124CCCC00000001_240424.gml"));
        Dataset dm = S124Utils.unmarshallS124(datasetString);
        // System.out.println(dm.getBoundedBy());
        return List.of(new Result(datasetString, dm));
    }

    public List<Result> fetchAll() throws IOException, InterruptedException, JAXBException {
        String endpoint = "https://niord.t-dma.dk";
        // String endpoint = "http://localhost:8888";

        // String endpoint = "https://niord.t-dma.dk";
//        String endpoint = "https://localhost:8080/rest/public/v1/messages";

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint + "/rest/public/v1/messages")).GET().build();

        // Send the request and get the response
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        ArrayList<Result> result = new ArrayList<>();
        // Check if the response status code is 200 (OK)
        if (response.statusCode() == 200) {
            // Parse the response body to a JSON array
            JsonNode jsonArray = objectMapper.readTree(response.body());
            // Iterate over the JSON array
            for (JsonNode jsonObject : jsonArray) {
                // Extract "shortId" and "mainType"
                String id = jsonObject.path("id").asText();
                String shortId = jsonObject.path("shortId").asText();
                String mainType = jsonObject.path("mainType").asText();

                if ("NW".equals(mainType)) {
                    if (shortId != null && !shortId.isEmpty()) {
                        // Log the extracted values
//                        LOG.info("id: " + id);
//                        LOG.info("shortId: " + shortId);
//                        LOG.info("mainType: " + mainType);
//                        LOG.info("----------------------------");

                        // Create a GET request for the XML data using the shortId
                        String xmlUrl = endpoint + "/rest/S-124-MaDaMe/messages/" + id;
                        HttpRequest xmlRequest = HttpRequest.newBuilder().uri(URI.create(xmlUrl)).GET().build();
//                        LOG.info(xmlUrl);

                        // Send the request and get t   he XML response
                        HttpResponse<String> xmlResponse = client.send(xmlRequest, HttpResponse.BodyHandlers.ofString());

                        String datasetString = xmlResponse.body();

                        Dataset dm = null;
                        // System.out.println(datasetString);
                        try {
                            dm = S124Utils.unmarshallS124(datasetString);
                        } catch (Exception e) {
                            System.err.println(shortId);
                            System.err.println(datasetString          );
                            System.err.println("Could not deserialize: " + xmlResponse.body());
                        }

                        // System.out.println(dm.getBoundedBy());
                        if (dm != null) {
                            result.add(new Result(datasetString, dm));
                        }
//                        return List.of();
                    }
                }
            }
        }
        return result;
    }

    public record Result(String xml, Dataset dataset) {}
}
