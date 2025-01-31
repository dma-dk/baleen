package dk.dma.baleen.connector.secom.service;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.grad.secom.core.exceptions.SecomGenericException;
import org.grad.secom.core.exceptions.SecomNotFoundException;
import org.grad.secom.core.exceptions.SecomValidationException;
import org.grad.secom.core.models.ResponseSearchObject;
import org.grad.secom.core.models.SearchFilterObject;
import org.grad.secom.core.models.SearchObjectResult;
import org.grad.secom.core.models.SearchParameters;
import org.grad.secom.springboot3.components.SecomClient;
import org.grad.secom.springboot3.components.SecomConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dk.dma.baleen.connector.secom.util.BaleenSecomClient;
import jakarta.annotation.PostConstruct;

/**
 * Resolves a MRN to a a SecomClient
 */
@Service
public final class SecomServiceRegistryService {

    private static final Logger logger = LoggerFactory.getLogger(SecomServiceRegistryService.class);

    /** The URL of the service registry. */
    private String serviceRegistryUrl;

    /** The configuration of SECOM */
    private SecomConfigProperties secomConfig;

    /** A SECOM client to the service registry. */
    private BaleenSecomClient serviceRegistryClient;

    @Autowired
    SecomServiceRegistryService(@Value("${secom.service-registry.url:}") String serviceRegistryUrl, SecomConfigProperties secomConfig) {
        this.serviceRegistryUrl = serviceRegistryUrl;
        this.secomConfig = requireNonNull(secomConfig);
    }

    @PostConstruct
    public void init() throws Exception {
        if (StringUtils.isBlank(serviceRegistryUrl)) {
            throw new BeanCreationException("Failed to initialize SECOM service registry: service registry URL is required");
        }

        // If this fails, app will fail to start
        URI uri = URI.create(serviceRegistryUrl);
      //  secomConfig.setTruststore(null);
        SecomConfigProperties scp =new SecomConfigProperties();
        scp.setInsecureSslPolicy(true);
        serviceRegistryClient = new BaleenSecomClient(uri, scp);
    }

    public BaleenSecomClient resolveMRN(String mrn) {
        // Can only be null after shutdown. Otherwise startup would have failed.s
        if (serviceRegistryClient == null) {
            throw new SecomValidationException("Application has been shutdown");
        }

        logger.info("Resolving MRN: {}", mrn);

        // Create the search object
        SearchFilterObject filter = new SearchFilterObject();
        SearchParameters params = new SearchParameters();
        params.setInstanceId(mrn);
        filter.setQuery(params);

        // Get latest hosts
        SearchObjectResult result = serviceRegistryClient.searchService(filter, 0, Integer.MAX_VALUE).map(ResponseSearchObject::getSearchServiceResult)
                .orElse(List.of()).stream().max(Comparator.comparing(SearchObjectResult::getVersion))
                .orElseThrow(() -> new SecomNotFoundException(String.format("The MRN %s was not registered as a service with %s", mrn, serviceRegistryClient)));

        logger.info("Resolving MRN as: {}", result.getEndpointUri());

        // We got a result from the service registry. Create and return a SecomClients
        try {
            URI uri = URI.create(result.getEndpointUri());
            return new BaleenSecomClient(uri, secomConfig);
        } catch (Exception e) {
            logger.error("SecomClient could not be created: {}", e.getMessage(), e);
            throw new SecomGenericException("Failed to secom client: " + e.getMessage());
        }
    }

}