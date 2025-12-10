package it.gov.pagopa.receipt.pdf.service.producer;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Producer class for {@link CosmosContainer} bean
 */
@Singleton
public class CosmosContainerProducer {

    @ConfigProperty(name = "cosmos.db.name")
    private String databaseId;

    @ConfigProperty(name = "cosmos.container.receipts.name")
    private String containerReceipts;

    @ConfigProperty(name = "cosmos.container.cart.name")
    private String containerCart;

    private final CosmosClient cosmosClient;

    @Inject
    public CosmosContainerProducer(CosmosClient cosmosClient) {
        this.cosmosClient = cosmosClient;
    }

    public CosmosDatabase cosmosDatabase() {
        return this.cosmosClient
                .getDatabase(databaseId);
    }

    @Produces
    @ApplicationScoped
    @ReceiptsContainer
    public CosmosContainer containerReceipts() {
        return cosmosDatabase()
                .getContainer(containerReceipts);
    }

    @Produces
    @ApplicationScoped
    @CartContainer
    public CosmosContainer containerCartReceipts() {
        return cosmosDatabase()
                .getContainer(containerCart);
    }


}
