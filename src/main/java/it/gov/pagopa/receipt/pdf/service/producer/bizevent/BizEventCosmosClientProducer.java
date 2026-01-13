package it.gov.pagopa.receipt.pdf.service.producer.bizevent;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Producer class for {@link CosmosClient} bean
 */
@Singleton
public class BizEventCosmosClientProducer {

    @ConfigProperty(name = "cosmos.bizevent.key")
    String azureKey;

    @ConfigProperty(name = "cosmos.bizevent.endpoint")
    String serviceEndpoint;

    BizEventCosmosClientProducer() {
    }

    @Produces
    @ApplicationScoped
    @BizEventCosmos
    public CosmosClient cosmosBizEventClient() {
        return new CosmosClientBuilder()
                .endpoint(serviceEndpoint)
                .key(azureKey)
                .buildClient();
    }
}