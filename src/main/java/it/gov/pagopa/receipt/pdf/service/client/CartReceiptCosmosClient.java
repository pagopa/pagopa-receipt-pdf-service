package it.gov.pagopa.receipt.pdf.service.client;

import it.gov.pagopa.receipt.pdf.service.exception.CartNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.IoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.ReceiptNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.IOMessage;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartReceiptError;

public interface CartReceiptCosmosClient {

    /**
     * This method retrieves the cart document from the CosmosDB database.
     *
     * @param cartId the id of the cart to be retrieved
     * @return the cart for receipt
     * @throws CartNotFoundException in case cart receipt is not found
     */
    CartForReceipt getCartForReceiptDocument(String cartId) throws CartNotFoundException;

    /**
     * This method retrieves the cart document from the CosmosDB database.
     *
     * @param eventId the id of the BizEvent contained in the cart to be retrieved
     * @return the cart for receipt
     * @throws CartNotFoundException in case cart receipt is not found
     */
    CartForReceipt getCartReceiptFromEventId(String eventId) throws CartNotFoundException;

    /**
     * Retrieve cart document from CosmosDB database
     *
     * @param messageId IO Message id
     * @return io message document
     * @throws IoMessageNotFoundException in case no document has been found with the given messageId
     */
    IOMessage getCartIoMessage(String messageId) throws IoMessageNotFoundException;

    /**
     * Retrieve cartReceiptError document from CosmosDB database
     *
     * @param cartId cart ID
     * @return CartReceiptError found
     * @throws CartNotFoundException If the document isn't found
     */
    CartReceiptError getCartReceiptError(String cartId) throws CartNotFoundException;
}
