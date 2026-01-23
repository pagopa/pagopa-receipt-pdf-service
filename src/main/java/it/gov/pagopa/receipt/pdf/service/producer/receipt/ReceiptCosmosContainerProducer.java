package it.gov.pagopa.receipt.pdf.service.producer.receipt;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import it.gov.pagopa.receipt.pdf.service.producer.receipt.containers.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Producer class for {@link CosmosContainer} bean
 */
@Singleton
public class ReceiptCosmosContainerProducer {

    @ConfigProperty(name = "cosmos.receipt.db.name")
    private String databaseId;

    @ConfigProperty(name = "cosmos.container.receipts.name")
    private String containerReceipts;
    @ConfigProperty(name = "cosmos.container.receipts-error.name")
    private String containerReceiptsError;
    @ConfigProperty(name = "cosmos.container.receipts-io-messages-event.name")
    private String containerReceiptsIOMessagesEvent;

    @ConfigProperty(name = "cosmos.container.cart.name")
    private String containerCart;
    @ConfigProperty(name = "cosmos.container.cart-receipts-error.name")
    private String containerCartReceiptsError;
    @ConfigProperty(name = "cosmos.container.cart-receipts-io-messages.name")
    private String containerCartReceiptsIOMessages;

    private final CosmosClient cosmosClient;

    @Inject
    public ReceiptCosmosContainerProducer(@ReceiptCosmos CosmosClient cosmosClient) {
        this.cosmosClient = cosmosClient;
    }

    private CosmosDatabase cosmosDatabase() {
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
    @ReceiptsErrorContainer
    public CosmosContainer containerReceiptsError() {
        return cosmosDatabase()
                .getContainer(containerReceiptsError);
    }

    @Produces
    @ApplicationScoped
    @ReceiptsIOMessagesEventContainer
    public CosmosContainer containerReceiptsIOMessagesEvent() {
        return cosmosDatabase()
                .getContainer(containerReceiptsIOMessagesEvent);
    }

    @Produces
    @ApplicationScoped
    @CartContainer
    public CosmosContainer containerCartReceipts() {
        return cosmosDatabase()
                .getContainer(containerCart);
    }

    @Produces
    @ApplicationScoped
    @CartReceiptsErrorContainer
    public CosmosContainer containerCartReceiptsError() {
        return cosmosDatabase()
                .getContainer(containerCartReceiptsError);
    }

    @Produces
    @ApplicationScoped
    @CartReceiptsIOMessagesContainer
    public CosmosContainer containerCartReceiptsIOMessages() {
        return cosmosDatabase()
                .getContainer(containerCartReceiptsIOMessages);
    }
}
