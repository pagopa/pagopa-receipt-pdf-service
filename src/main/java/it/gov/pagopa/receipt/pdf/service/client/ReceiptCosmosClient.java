package it.gov.pagopa.receipt.pdf.service.client;


import it.gov.pagopa.receipt.pdf.service.exception.ReceiptNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.Receipt;

public interface ReceiptCosmosClient {

    Receipt getReceiptDocument(String thirdPartyId) throws ReceiptNotFoundException;
}
