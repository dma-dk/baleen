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
package dk.dma.baleen.connector.secom.util;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.Optional;

import org.grad.secom.core.interfaces.SearchServiceSecomInterface;
import org.grad.secom.core.interfaces.SubscriptionNotificationSecomInterface;
import org.grad.secom.core.interfaces.UploadSecomInterface;
import org.grad.secom.core.models.EnvelopeUploadObject;
import org.grad.secom.core.models.ResponseSearchObject;
import org.grad.secom.core.models.SearchFilterObject;
import org.grad.secom.core.models.SubscriptionNotificationObject;
import org.grad.secom.core.models.UploadObject;
import org.grad.secom.core.models.UploadResponseObject;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * A client that communicates via the SECOM library.
 */
public class SecomClientX {

    public final String baseUri;
    private final HttpClient client;
    private final SecomConfiguration configuration;

    public SecomClientX(HttpClient client, String baseUri, SecomConfiguration configuration) {
        this.baseUri = requireNonNull(baseUri);
        this.client = requireNonNull(client);
        this.configuration = requireNonNull(configuration);
    }

    public Optional<ResponseSearchObject> searchService(SearchFilterObject searchFilterObject, Integer page, Integer pageSize) {
        try {
            return searchService0(searchFilterObject, page, pageSize);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public Optional<ResponseSearchObject> searchService0(SearchFilterObject searchFilterObject, Integer page, Integer pageSize) throws Exception {
        String url = baseUri + SearchServiceSecomInterface.SEARCH_SERVICE_INTERFACE_PATH;

        // System.out.println(url);
        ObjectMapper om = new ObjectMapper();
        // Server sends back a .certificate field. Which doesnt exist anymore
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Create request body (you might want to convert your object to JSON string, assuming a library like Jackson is used)
        String requestBody = om.writeValueAsString(searchFilterObject);

        // Build HttpRequest
        HttpRequest requ = HttpRequest.newBuilder().uri(new URI(url)).header("Content-Type", "application/json").header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();

        // Send the request
        HttpResponse<String> response = client.send(requ, HttpResponse.BodyHandlers.ofString());

        // Check for successful response and deserialize response body
        if (response.statusCode() == 200) {
            ResponseSearchObject responseObject = om.readValue(response.body(), ResponseSearchObject.class);
            return Optional.of(responseObject);
        } else {
            return Optional.empty();
        }
    }

    public void send(SubscriptionNotificationObject sno) throws Exception {
        String url = baseUri + SubscriptionNotificationSecomInterface.SUBSCRIPTION_NOTIFICATION_INTERFACE_PATH;

        ObjectMapper om = new ObjectMapper();

        // Create request body (you might want to convert your object to JSON string, assuming a library like Jackson is used)
        String requestBody = om.writeValueAsString(sno);

        // Build HttpRequest
        HttpRequest requ = HttpRequest.newBuilder().uri(new URI(url)).header("Content-Type", "application/json").header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();

        // Send the request
        HttpResponse<String> response = client.send(requ, HttpResponse.BodyHandlers.ofString());

        System.out.println("Sub callback " + response.statusCode());

        System.out.println(response.body());
    }

    public Optional<UploadResponseObject> upload(UploadObject uploadObject, String mrn) {
        // Prepare the upload envelope if valid
        final EnvelopeUploadObject envelope = uploadObject.getEnvelope();
        if (envelope != null) {
            envelope.prepareMetadata(configuration.signatureProvider()).signData(configuration.certificateProvider(), configuration.signatureProvider())
                    .encodeData();
        }

        // If a signature provider has been assigned, use it to sign the
        // upload object envelope data
        uploadObject.signEnvelope(configuration.certificateProvider(), configuration.signatureProvider());

        // Convert the uploadObject to JSON (or equivalent format)
        ObjectMapper objectMapper = new ObjectMapper();

        String url = baseUri + UploadSecomInterface.UPLOAD_INTERFACE_PATH;
        try {
            String requestBody = objectMapper.writeValueAsString(uploadObject);

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "application/json").header("Accept", "application/json")
                    .POST(BodyPublishers.ofString(requestBody)).build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Check for successful response and deserialize response body
            if (response.statusCode() == 200) {
                System.out.println("Publish: succesfully published message to " + url);
                UploadResponseObject responseObject = objectMapper.readValue(response.body(), UploadResponseObject.class);
                return Optional.of(responseObject);
            } else {
                System.out.println("Publish: unsuccesfully tried to message to " + url + " response = " + response.statusCode() + " " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Publish: unsuccesfully tried to message to " + url + ": error " + e.getMessage());
        }
        return Optional.empty();

    }
}
