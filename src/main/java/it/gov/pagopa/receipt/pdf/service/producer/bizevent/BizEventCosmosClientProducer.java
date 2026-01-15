package it.gov.pagopa.receipt.pdf.service.producer.bizevent;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Collections;

/**
 * Producer class for {@link CosmosClient} bean
 */
@Singleton
public class BizEventCosmosClientProducer {

    @ConfigProperty(name = "cosmos.bizevent.key")
    String azureKey;
    @ConfigProperty(name = "cosmos.bizevent.endpoint")
    String serviceEndpoint;
    @ConfigProperty(name = "cosmos.preferred.region")
    String preferredRegion;

    BizEventCosmosClientProducer() {
    }

    @Produces
    @ApplicationScoped
    @BizEventCosmos
    public CosmosClient cosmosBizEventClient() {
        return new CosmosClientBuilder()
                .endpoint(serviceEndpoint)
                .preferredRegions(Collections.singletonList(preferredRegion))
                .key(azureKey)
                .buildClient();
    }
}
