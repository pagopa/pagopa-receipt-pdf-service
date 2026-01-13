package it.gov.pagopa.receipt.pdf.service.producer.receipt;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Producer class for {@link CosmosClient} bean
 */
@Singleton
public class ReceiptCosmosClientProducer {

    @ConfigProperty(name = "cosmos.receipt.key")
    String azureKey;

    @ConfigProperty(name = "cosmos.receipt.endpoint")
    String serviceEndpoint;

    ReceiptCosmosClientProducer() {}

    @Produces
    @ReceiptCosmos
    public CosmosClient cosmosReceiptClient() {
        return new CosmosClientBuilder()
                .endpoint(serviceEndpoint)
                .key(azureKey)
                .buildClient();
    }
}
