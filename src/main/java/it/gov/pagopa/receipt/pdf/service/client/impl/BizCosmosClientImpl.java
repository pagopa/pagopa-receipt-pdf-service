package it.gov.pagopa.receipt.pdf.service.client.impl;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.util.CosmosPagedIterable;
import it.gov.pagopa.receipt.pdf.service.client.BizCosmosClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.BizEventNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.ReceiptNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.biz.BizEvent;
import it.gov.pagopa.receipt.pdf.service.model.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.service.producer.ReceiptsContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static it.gov.pagopa.receipt.pdf.service.utils.CommonUtils.sanitize;

/**
 * Client for the CosmosDB database
 */
@ApplicationScoped
public class BizCosmosClientImpl implements BizCosmosClient {


    @ReceiptsContainer
    CosmosContainer cosmosContainer;

    @Inject
    public BizCosmosClientImpl(@ReceiptsContainer CosmosContainer cosmosContainer) {
        this.cosmosContainer = cosmosContainer;
    }

    @Override
    public BizEvent getBizEventDocumentByOrganizationFiscalCodeAndIUV(String organizationFiscalCode, String iuv) throws BizEventNotFoundException {
        String query = String.format("SELECT * FROM c WHERE c.creditor.idPA = '%s' AND c.debtorPosition.iuv = '%s'",
                organizationFiscalCode, iuv);

        CosmosPagedIterable<BizEvent> queryResponse = cosmosContainer
                .queryItems(query, new CosmosQueryRequestOptions(), BizEvent.class);

        if (queryResponse.iterator().hasNext()) {
            return queryResponse.iterator().next();
        }
        throw new BizEventNotFoundException(AppErrorCodeEnum.PDFS_800, "Document not found in the defined container");
    }

}
