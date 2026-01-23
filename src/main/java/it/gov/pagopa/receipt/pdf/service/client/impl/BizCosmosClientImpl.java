package it.gov.pagopa.receipt.pdf.service.client.impl;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.util.CosmosPagedIterable;
import it.gov.pagopa.receipt.pdf.service.client.BizCosmosClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.BizEventNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.biz.BizEvent;
import it.gov.pagopa.receipt.pdf.service.producer.bizevent.containers.BizEventContainer;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Client for the CosmosDB database
 */
@ApplicationScoped
public class BizCosmosClientImpl implements BizCosmosClient {


    @BizEventContainer
    CosmosContainer cosmosContainer;

    BizCosmosClientImpl() {
    }

    @Override
    public BizEvent getBizEventDocumentByOrganizationFiscalCodeAndIUV(String organizationFiscalCode, String iuv) throws BizEventNotFoundException {
        String query = String.format("SELECT * FROM c WHERE c.creditor.idPA = '%s' AND c.debtorPosition.iuv = '%s'",
                organizationFiscalCode, iuv);

        CosmosPagedIterable<BizEvent> queryResponse = this.cosmosContainer
                .queryItems(query, new CosmosQueryRequestOptions(), BizEvent.class);

        if (queryResponse.iterator().hasNext()) {
            return queryResponse.iterator().next();
        }
        throw new BizEventNotFoundException(AppErrorCodeEnum.PDFS_100, AppErrorCodeEnum.PDFS_100.getErrorMessage());
    }

}
