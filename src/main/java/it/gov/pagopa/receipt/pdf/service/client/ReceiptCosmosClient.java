package it.gov.pagopa.receipt.pdf.service.client;


import it.gov.pagopa.receipt.pdf.service.exception.IoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.ReceiptNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.IOMessage;
import it.gov.pagopa.receipt.pdf.service.model.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.service.model.receipt.ReceiptError;

/**
 * Interface of the client for the receipt's Cosmos database used to retrieve the Receipts details
 */
public interface ReceiptCosmosClient {

    /**
     * Retrieve the receipt from the Cosmos
     *
     * @param thirdPartyId the id of the receipt to be retrieved
     * @return the receipt
     * @throws ReceiptNotFoundException thrown if the receipt was not found
     */
    Receipt getReceiptDocument(String thirdPartyId) throws ReceiptNotFoundException;

    IOMessage getIoMessage(String messageId) throws IoMessageNotFoundException;

    ReceiptError getReceiptError(String bizEventId) throws  ReceiptNotFoundException;
}
