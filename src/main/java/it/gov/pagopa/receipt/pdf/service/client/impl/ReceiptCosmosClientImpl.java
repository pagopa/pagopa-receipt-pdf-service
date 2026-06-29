package it.gov.pagopa.receipt.pdf.service.client.impl;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.IoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.ReceiptNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.IOMessage;
import it.gov.pagopa.receipt.pdf.service.model.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.service.model.receipt.ReceiptError;
import it.gov.pagopa.receipt.pdf.service.producer.receipt.containers.ReceiptsContainer;
import it.gov.pagopa.receipt.pdf.service.producer.receipt.containers.ReceiptsErrorContainer;
import it.gov.pagopa.receipt.pdf.service.producer.receipt.containers.ReceiptsIOMessagesEventContainer;
import it.gov.pagopa.receipt.pdf.service.utils.PerfTracer;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static it.gov.pagopa.receipt.pdf.service.utils.CommonUtils.sanitize;

/**
 * Client for the CosmosDB database
 */
@ApplicationScoped
public class ReceiptCosmosClientImpl implements ReceiptCosmosClient {

    private static final String DOCUMENT_NOT_FOUND_ERR_MSG = "Document not found in the defined container";

    private final Logger logger = LoggerFactory.getLogger(ReceiptCosmosClientImpl.class);

    @ReceiptsContainer
    CosmosContainer containerReceipts;

    @ReceiptsIOMessagesEventContainer
    CosmosContainer containerReceiptsIOMessagesEvent;

    @ReceiptsErrorContainer
    CosmosContainer containerReceiptsError;

    ReceiptCosmosClientImpl() {
    }

    /**
     * Retrieve receipt document from CosmosDB database
     *
     * @param thirdPartyId the third party id
     * @return receipt document
     * @throws ReceiptNotFoundException in case no receipt has been found with the given idEvent
     */
    public Receipt getReceiptDocument(String thirdPartyId) throws ReceiptNotFoundException {
        SqlQuerySpec querySpec = new SqlQuerySpec(
                "SELECT * FROM c WHERE c.eventId = @eventId",
                List.of(new SqlParameter("@eventId", thirdPartyId))
        );

        //Query the container
        try (PerfTracer t = PerfTracer.start(logger, "cosmos.receipts.queryItems")
                .tag("container", containerReceipts.getId())) {
        return this.containerReceipts
                .queryItems(querySpec, new CosmosQueryRequestOptions(), Receipt.class)
                .stream()
                .findFirst()
                .orElseThrow(() -> {
                    String errMsg = String.format("Receipt with id %s not found in the defined container: %s",
                            sanitize(thirdPartyId), containerReceipts.getId());
                    logger.error(errMsg);
                    return new ReceiptNotFoundException(AppErrorCodeEnum.PDFS_800, errMsg, thirdPartyId);
                });
        }
    }

    /**
     * Retrieve receipt document from CosmosDB database
     *
     * @param messageId IO Message id
     * @return io message document
     * @throws IoMessageNotFoundException in case no receipt has been found with the given messageId
     */
    @Override
    public IOMessage getIoMessage(String messageId) throws IoMessageNotFoundException {
        //Build query
        SqlQuerySpec querySpec = new SqlQuerySpec(
                "SELECT * FROM c WHERE c.messageId = @messageId",
                List.of(new SqlParameter("@messageId", messageId))
        );

        //Query the container
        return this.containerReceiptsIOMessagesEvent
                .queryItems(querySpec, new CosmosQueryRequestOptions(), IOMessage.class)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IoMessageNotFoundException(DOCUMENT_NOT_FOUND_ERR_MSG));
    }

    /**
     * Retrieve receiptError document from CosmosDB database
     *
     * @param bizEventId BizEvent ID
     * @return ReceiptError found
     * @throws ReceiptNotFoundException If the document isn't found
     */
    @Override
    public ReceiptError getReceiptError(String bizEventId) throws ReceiptNotFoundException {
        //Build query
        SqlQuerySpec querySpec = new SqlQuerySpec(
                "SELECT * FROM c WHERE c.bizEventId = @bizEventId",
                List.of(new SqlParameter("@bizEventId", bizEventId))
        );

        //Query the container
        return containerReceiptsError
                .queryItems(querySpec, new CosmosQueryRequestOptions(), ReceiptError.class)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ReceiptNotFoundException(AppErrorCodeEnum.PDFS_800, DOCUMENT_NOT_FOUND_ERR_MSG));
    }

}
