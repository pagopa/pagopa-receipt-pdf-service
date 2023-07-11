package it.gov.pagopa.receipt.pdf.service.client.impl;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.util.CosmosPagedIterable;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.ReceiptNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.Receipt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Client for the CosmosDB database
 */
@ApplicationScoped
public class ReceiptCosmosClientImpl implements ReceiptCosmosClient {

    @ConfigProperty(name = "cosmos.db.name")
    private String databaseId;

    @ConfigProperty(name = "cosmos.container.name")
    private String containerId;

    private final CosmosClient cosmosClient;

    @Inject
    private Logger logger;

    public ReceiptCosmosClientImpl(
            @ConfigProperty(name = "azure.key") String azureKey,
            @ConfigProperty(name = "cosmos.endpoint") String serviceEndpoint
    ) {
        this.cosmosClient = new CosmosClientBuilder()
                .endpoint(serviceEndpoint)
                .key(azureKey)
                .buildClient();
    }

    /**
     * Retrieve receipt document from CosmosDB database
     *
     * @param thirdPartyId the third party id
     * @return receipt document
     * @throws ReceiptNotFoundException in case no receipt has been found with the given idEvent
     */
    public Receipt getReceiptDocument(String thirdPartyId) throws ReceiptNotFoundException {
        CosmosContainer cosmosContainer = this.cosmosClient.getDatabase(databaseId).getContainer(containerId);

        //Build query
        String query = "SELECT * FROM c WHERE c.eventId = " + "'" + thirdPartyId + "'";

        //Query the container
        CosmosPagedIterable<Receipt> queryResponse = cosmosContainer
                .queryItems(query, new CosmosQueryRequestOptions(), Receipt.class);

        if (queryResponse.iterator().hasNext()) {
            return queryResponse.iterator().next();
        }
        String errMsg = String.format("Receipt with id %s not found in the defined container %s", thirdPartyId, containerId);
        logger.error(errMsg);
        throw new ReceiptNotFoundException(AppErrorCodeEnum.PDFS_800, errMsg);
    }
}
