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
package dk.dma.baleen.service.mcp;

import static java.util.Objects.requireNonNull;

import java.net.http.HttpClient;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.grad.secom.core.exceptions.SecomNotFoundException;
import org.grad.secom.core.exceptions.SecomValidationException;
import org.grad.secom.core.models.ResponseSearchObject;
import org.grad.secom.core.models.SearchFilterObject;
import org.grad.secom.core.models.SearchObjectResult;
import org.grad.secom.core.models.SearchParameters;

import dk.dma.baleen.rest.secom.AbstractSecomApi.MRNClient;
import dk.dma.baleen.service.secom.SecomClient;
import dk.dma.baleen.service.secom.SecomConfiguration;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * A client for the service registry that can resolve MRN's to hostnames.
 */
@ApplicationScoped
public class MCPServiceRegistryClient {

    final String discoveryServiceUrl = "https://msr.maritimeconnectivity.net/api/secom";

    final SecomConfiguration configuration;

    SecomClient discoveryService;


    final HttpClient client;

    public MCPServiceRegistryClient(SecomConfiguration configuration, MCPSecurityService pki) {
        this.configuration = requireNonNull(configuration);
        this.client = pki.newHttpClient();
    }

    void onStart(@Observes StartupEvent ev) {
        this.discoveryService = Optional.ofNullable(this.discoveryServiceUrl).filter(StringUtils::isNotBlank).map(url -> {
            try {
                return new SecomClient(client, url, this.configuration);
            } catch (Exception ex) {
                ex.printStackTrace();
                System.err.println("Unable to initialise the SSL context for the SECOM discovery service..." + ex);
                return null;
            }
        }).orElse(null);

    }

    public SecomClient resolve(MRNClient client) {
        if (client.forceHost() != null) {
            return new SecomClient(this.client, client.forceHost(), configuration);
        }
        return resolve(client.mrn());
    }

    public SecomClient resolve(String mrn) {
        // Validate the MRN
        Optional.ofNullable(mrn).filter(StringUtils::isNotBlank)
                .orElseThrow(() -> new SecomValidationException("Cannot request a service discovery for an empty/invalid MRN"));

        // Make sure the service registry is available
        Optional.ofNullable(this.discoveryService).filter(Objects::nonNull)
                .orElseThrow(() -> new SecomValidationException("Subscription request found for S-125 dataset updates but no connection to service registry"));

        // Create the discovery service search filter object for the provided MRN
        final SearchFilterObject searchFilterObject = new SearchFilterObject();
        final SearchParameters searchParameters = new SearchParameters();
        searchParameters.setInstanceId(mrn);
        searchFilterObject.setQuery(searchParameters);

        // Lookup the endpoints of the clients from the SECOM discovery service
        final List<SearchObjectResult> instances = Optional.ofNullable(this.discoveryService)
                .flatMap(ds -> ds.searchService(searchFilterObject, 0, Integer.MAX_VALUE)).map(ResponseSearchObject::getSearchServiceResult)
                .orElse(Collections.emptyList());

        // Extract the latest matching instance
        final SearchObjectResult instance = instances.stream().max(Comparator.comparing(SearchObjectResult::getVersion))
                .orElseThrow(() -> new SecomNotFoundException(mrn));

        // Now construct and return a SECOM client for the discovered URI
        try {
            return new SecomClient(client, instance.getEndpointUri(), configuration);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new SecomValidationException(ex.getMessage());
        }
    }

}
