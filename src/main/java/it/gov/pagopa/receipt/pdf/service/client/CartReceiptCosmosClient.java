package it.gov.pagopa.receipt.pdf.service.client;

import it.gov.pagopa.receipt.pdf.service.exception.CartNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.ReceiptNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartForReceipt;

public interface CartReceiptCosmosClient {

  /**
   * This method retrieves the cart document from the CosmosDB database.
   *
   * @param cartId the id of the cart to be retrieved
   * @return the cart for receipt
   * @throws ReceiptNotFoundException
   */
  CartForReceipt getCartForReceiptDocument(String cartId) throws CartNotFoundException;
}
