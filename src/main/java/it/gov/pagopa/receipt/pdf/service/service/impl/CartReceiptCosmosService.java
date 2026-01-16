package it.gov.pagopa.receipt.pdf.service.service.impl;

import it.gov.pagopa.receipt.pdf.service.client.CartReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.exception.Aes256Exception;
import it.gov.pagopa.receipt.pdf.service.exception.CartNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.IoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.IOMessage;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartReceiptError;
import it.gov.pagopa.receipt.pdf.service.utils.Aes256Utils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.PDFS_800;


@ApplicationScoped
public class CartReceiptCosmosService {

    private final CartReceiptCosmosClient cartReceiptCosmosClient;

    @Inject
    public CartReceiptCosmosService(
            CartReceiptCosmosClient cartReceiptCosmosClient
    ) {
        this.cartReceiptCosmosClient = cartReceiptCosmosClient;
    }

    public CartForReceipt getCartReceipt(String cartId) throws CartNotFoundException {
        CartForReceipt receipt;
        try {
            receipt = this.cartReceiptCosmosClient.getCartForReceiptDocument(cartId);
        } catch (CartNotFoundException e) {
            String errorMsg = String.format("Receipt not found with the cart id %s", cartId);
            throw new CartNotFoundException(PDFS_800, errorMsg, e);

        }

        if (receipt == null) {
            String errorMsg = String.format("Receipt retrieved with the cart id %s is null", cartId);
            throw new CartNotFoundException(PDFS_800, errorMsg);
        }
        return receipt;
    }

    public IOMessage getCartReceiptMessage(String messageId) throws IoMessageNotFoundException {
        IOMessage message;
        try {
            message = this.cartReceiptCosmosClient.getCartIoMessage(messageId);
        } catch (IoMessageNotFoundException e) {
            String errorMsg = String.format("CartReceipt Message to IO not found with the message id %s", messageId);
            throw new IoMessageNotFoundException(errorMsg, e);
        }

        if (message == null) {
            String errorMsg = String.format("CartReceipt retrieved with the message id %s is null", messageId);
            throw new IoMessageNotFoundException(errorMsg);
        }
        return message;
    }

    public CartReceiptError getCartReceiptError(String cartId) throws CartNotFoundException {
        if (cartId == null || cartId.isBlank()) {
            throw new IllegalArgumentException("Event ID cannot be null or empty");
        }

        CartReceiptError receiptError = this.cartReceiptCosmosClient.getCartReceiptError(cartId);
        decryptPayload(receiptError);

        return receiptError;
    }

    private void decryptPayload(CartReceiptError receiptError) {
        String payload = receiptError.getMessagePayload();
        if (payload != null && !payload.isBlank()) {
            try {
                receiptError.setMessagePayload(Aes256Utils.decrypt(payload));
            } catch (IllegalArgumentException | Aes256Exception ignored) {
                // Return encrypted payload
            }
        }
    }
}
