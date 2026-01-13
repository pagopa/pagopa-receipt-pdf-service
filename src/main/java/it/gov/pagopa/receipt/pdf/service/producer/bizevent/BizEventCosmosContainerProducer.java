package it.gov.pagopa.receipt.pdf.service.producer.bizevent;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import it.gov.pagopa.receipt.pdf.service.producer.bizevent.containers.BizEventContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Producer class for {@link CosmosContainer} bean
 */
@Singleton
public class BizEventCosmosContainerProducer {

    @ConfigProperty(name = "cosmos.bizevent.db.name")
    private String databaseId;

    @ConfigProperty(name = "cosmos.container.bizevent.name")
    private String containerBizEvents;

    private final CosmosClient cosmosClient;

    @Inject
    public BizEventCosmosContainerProducer(@BizEventCosmos CosmosClient cosmosClient) {
        this.cosmosClient = cosmosClient;
    }

    private CosmosDatabase cosmosDatabase() {
        return this.cosmosClient
                .getDatabase(databaseId);
    }

    @Produces
    @ApplicationScoped
    @BizEventContainer
    public CosmosContainer containerBizEvents() {
        return cosmosDatabase()
                .getContainer(containerBizEvents);
    }
}