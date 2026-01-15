package it.gov.pagopa.receipt.pdf.service.client.impl;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.util.CosmosPagedIterable;
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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static it.gov.pagopa.receipt.pdf.service.utils.CommonUtils.sanitize;

/**
 * Client for the CosmosDB database
 */
@ApplicationScoped
public class ReceiptCosmosClientImpl implements ReceiptCosmosClient {

    private static final String DOCUMENT_NOT_FOUND_ERR_MSG = "Document not found in the defined container";

    private static final String FIND_RECEIPT_QUERY = "SELECT * FROM c WHERE c.eventId = '%s'";

    private final Logger logger = LoggerFactory.getLogger(ReceiptCosmosClientImpl.class);

    @ReceiptsContainer
    CosmosContainer containerReceipts;

    @ReceiptsIOMessagesEventContainer
    CosmosContainer containerReceiptsIOMessagesEvent;

    @ReceiptsErrorContainer
    CosmosContainer containerReceiptsError;

    @Inject
    public ReceiptCosmosClientImpl(@ReceiptsContainer CosmosContainer containerReceipts) {
        this.containerReceipts = containerReceipts;
    }

    /**
     * Retrieve receipt document from CosmosDB database
     *
     * @param thirdPartyId the third party id
     * @return receipt document
     * @throws ReceiptNotFoundException in case no receipt has been found with the given idEvent
     */
    public Receipt getReceiptDocument(String thirdPartyId) throws ReceiptNotFoundException {
        String query = String.format(FIND_RECEIPT_QUERY, thirdPartyId);

        //Query the container
        CosmosPagedIterable<Receipt> queryResponse = this.containerReceipts
                .queryItems(query, new CosmosQueryRequestOptions(), Receipt.class);

        if (!queryResponse.iterator().hasNext()) {
            String errMsg = String.format("Receipt with id %s not found in the defined container: %s", sanitize(thirdPartyId), containerReceipts.getId());
            logger.error(errMsg);
            throw new ReceiptNotFoundException(AppErrorCodeEnum.PDFS_800, errMsg, thirdPartyId);
        }
        return queryResponse.iterator().next();
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
        String query = String.format("SELECT * FROM c WHERE c.messageId = '%s'", messageId);

        //Query the container
        CosmosPagedIterable<IOMessage> queryResponse = this.containerReceiptsIOMessagesEvent
                .queryItems(query, new CosmosQueryRequestOptions(), IOMessage.class);

        if (queryResponse.iterator().hasNext()) {
            return queryResponse.iterator().next();
        }
        throw new IoMessageNotFoundException(DOCUMENT_NOT_FOUND_ERR_MSG);
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
        String query = "SELECT * FROM c WHERE c.bizEventId = " + "'" + bizEventId + "'";

        //Query the container
        CosmosPagedIterable<ReceiptError> queryResponse = containerReceiptsError
                .queryItems(query, new CosmosQueryRequestOptions(), ReceiptError.class);

        if (queryResponse.iterator().hasNext()) {
            return queryResponse.iterator().next();
        }
        throw new ReceiptNotFoundException(AppErrorCodeEnum.PDFS_800, DOCUMENT_NOT_FOUND_ERR_MSG);
    }

}
