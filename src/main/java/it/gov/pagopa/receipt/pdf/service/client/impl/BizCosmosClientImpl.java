package it.gov.pagopa.receipt.pdf.service.client.impl;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import it.gov.pagopa.receipt.pdf.service.client.BizCosmosClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.BizEventNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.biz.BizEvent;
import it.gov.pagopa.receipt.pdf.service.producer.bizevent.containers.BizEventContainer;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Arrays;

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
    public BizEvent getBizEventDocumentByOrganizationFiscalCodeAndIUV(
            String organizationFiscalCode,
            String iuv
    ) throws BizEventNotFoundException {
        SqlQuerySpec querySpec = new SqlQuerySpec(
                "SELECT * FROM c WHERE c.creditor.idPA = @organizationFiscalCode AND c.debtorPosition.iuv = @iuv ",
                Arrays.asList(
                        new SqlParameter("@organizationFiscalCode", organizationFiscalCode),
                        new SqlParameter("@iuv", iuv)
                )
        );

        return this.cosmosContainer
                .queryItems(querySpec, new CosmosQueryRequestOptions(), BizEvent.class)
                .stream()
                .findFirst()
                .orElseThrow(() -> new BizEventNotFoundException(AppErrorCodeEnum.PDFS_100, AppErrorCodeEnum.PDFS_100.getErrorMessage()));
    }
}
