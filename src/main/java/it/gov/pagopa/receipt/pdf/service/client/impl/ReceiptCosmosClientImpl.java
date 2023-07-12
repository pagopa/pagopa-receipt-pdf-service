package it.gov.pagopa.receipt.pdf.service.client.impl;

import com.azure.cosmos.CosmosClient;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client for the CosmosDB database
 */
@ApplicationScoped
public class ReceiptCosmosClientImpl implements ReceiptCosmosClient {

    private final Logger logger = LoggerFactory.getLogger(ReceiptCosmosClientImpl.class);

    @ConfigProperty(name = "cosmos.db.name")
    private String databaseId;

    @ConfigProperty(name = "cosmos.container.name")
    private String containerId;

    @Inject
    private CosmosClient cosmosClient;

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
