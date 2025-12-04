package it.gov.pagopa.receipt.pdf.service.client;


import it.gov.pagopa.receipt.pdf.service.exception.ReceiptNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.service.model.receipt.Receipt;

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

    /**
     * This method retrieves the cart document from the CosmosDB database.
     *
     * @param cartId the id of the cart to be retrieved
     * @return the cart for receipt
     * @throws ReceiptNotFoundException
     */
    CartForReceipt getCartForReceiptDocument(String cartId) throws ReceiptNotFoundException;
}
