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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NiordApiCaller_Usage {

    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        ObjectMapper objectMapper = new ObjectMapper();
        String endpoint = "https://niord.t-dma.dk";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint + "/rest/public/v1/messages")).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

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

                    System.out.println(datasetString);
                }
            }
        }
    }
}