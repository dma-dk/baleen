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
package dk.dma.baleen.service.s124;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.baleen.s100.xmlbindings.s124.v1_0_0.utils.S124Utils;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import jakarta.xml.bind.JAXBException;

/**
 * This is temporary hack to get all messages in Niord. Even though that hasn't been promulgated
 * <p>
 * So please disregard the state of the code.
 */
// TODO move to Baleen "push"
@Service
public class NiordApiCallerOld {

    private static final Logger logger = LoggerFactory.getLogger(NiordApiCallerOld.class);

    private final HttpClient client;
    private final ObjectMapper objectMapper;

    public NiordApiCallerOld() {
        this.client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        this.objectMapper = new ObjectMapper();
    }

    public static void main(String[] args) throws Exception {
        NiordApiCallerOld caller = new NiordApiCallerOld();
        List<Result> fetchAll = caller.fetchAll();
//        for (Result r : fetchAll) {
//            System.out.println();
//            System.out.println(r.xml);
//        }
        System.out.println("Got " + fetchAll.size());
    }

    @Scheduled(fixedRate = 60000) // Run every minute (60000 milliseconds)
    public void fetchData() {
        try {
            fetchAll();
        } catch (Exception e) {
            logger.error("Error occurred during API call", e);
        }
    }

    volatile List<Result> cache;

    public List<Result> getIt() {
        List<Result> c = cache;
        if (c == null) {
            try {
                c = fetchAll();
            } catch (Exception e) {
                logger.error("Could not fetch latest messages", e);
            }
        }
        return c;
    }

    public List<Result> fetchAlXl() throws IOException, InterruptedException, JAXBException {
        String datasetString = Files.readString(Paths.get("/Users/kaspernielsen/dma/madame/documents/124CCCC00000001_240424.gml"));
        Dataset dm = S124Utils.unmarshallS124(datasetString);
        return List.of(new Result(datasetString, dm));
    }

    public List<Result> fetchAll() throws IOException, InterruptedException, JAXBException {
        String endpoint = "https://niord.t-dma.dk";

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint + "/rest/public/v1/messages")).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        ArrayList<Result> result = new ArrayList<>();
        if (response.statusCode() == 200) {
            JsonNode jsonArray = objectMapper.readTree(response.body());
            for (JsonNode jsonObject : jsonArray) {
                String id = jsonObject.path("id").asText();
                String shortId = jsonObject.path("shortId").asText();
                String mainType = jsonObject.path("mainType").asText();

                if ("NW".equals(mainType) && shortId != null && !shortId.isEmpty()) {
                    String xmlUrl = endpoint + "/rest/S-124/messages/" + id;
                    HttpRequest xmlRequest = HttpRequest.newBuilder().uri(URI.create(xmlUrl)).GET().build();

                    HttpResponse<String> xmlResponse = client.send(xmlRequest, HttpResponse.BodyHandlers.ofString());
                    String datasetString = xmlResponse.body();

                    Dataset dm = null;
                    try {
                       // System.out.println(xmlUrl);
                       // System.out.println(datasetString);
                        dm = S124Utils.unmarshallS124(datasetString);
                    } catch (Exception e) {
                        logger.error("Could not deserialize dataset for shortId: {}", shortId, e);
                        logger.debug("Dataset content: {}", datasetString);
                    }

                    if (dm != null) {
                        result.add(new Result(datasetString, dm));
                    }
                }
            }
        }
        return cache = result;
    }

    public record Result(String xml, Dataset dataset) {}
}