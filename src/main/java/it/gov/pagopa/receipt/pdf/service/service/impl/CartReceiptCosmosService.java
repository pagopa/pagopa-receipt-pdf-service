package it.gov.pagopa.receipt.pdf.service.service.impl;

import it.gov.pagopa.receipt.pdf.service.client.CartReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.exception.Aes256Exception;
import it.gov.pagopa.receipt.pdf.service.exception.CartNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.IoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartIOMessage;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartReceiptError;
import it.gov.pagopa.receipt.pdf.service.model.cart.Payload;
import it.gov.pagopa.receipt.pdf.service.utils.PerfTracer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.PDFS_801;

@Slf4j
@ApplicationScoped
public class CartReceiptCosmosService {

    private final CartReceiptCosmosClient cartReceiptCosmosClient;
    private final Aes256Service aes256Service;

    @Inject
    public CartReceiptCosmosService(
            CartReceiptCosmosClient cartReceiptCosmosClient,
            Aes256Service aes256Service
    ) {
        this.cartReceiptCosmosClient = cartReceiptCosmosClient;
        this.aes256Service = aes256Service;
    }

    public CartForReceipt getCartReceipt(String cartId) throws CartNotFoundException {
        CartForReceipt cart;
        try (PerfTracer t = PerfTracer.start(log, "cosmos.getCartForReceiptDocument").tag("cartId", cartId)) {
            try {
                cart = this.cartReceiptCosmosClient.getCartForReceiptDocument(cartId);
            } catch (CartNotFoundException e) {
                String errorMsg = String.format("Receipt not found with the cart id %s", cartId);
                throw new CartNotFoundException(PDFS_801, errorMsg, e);

            }

            if (cart == null) {
                String errorMsg = String.format("Receipt retrieved with the cart id %s is null", cartId);
                throw new CartNotFoundException(PDFS_801, errorMsg);
            }

            Payload payload = cart.getPayload();
            int cartSize = payload != null && payload.getCart() != null ? payload.getCart().size() : 0;
            t.tag("cartSize", cartSize);

            return cart;
        }
    }

    public CartIOMessage getCartReceiptMessage(String messageId) throws IoMessageNotFoundException {
        CartIOMessage message;
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
            throw new IllegalArgumentException("Cart ID cannot be null or empty");
        }

        CartReceiptError receiptError = this.cartReceiptCosmosClient.getCartReceiptError(cartId);
        decryptPayload(receiptError);

        return receiptError;
    }

    private void decryptPayload(CartReceiptError receiptError) {
        String payload = receiptError.getMessagePayload();
        if (payload != null && !payload.isBlank()) {
            try {
                receiptError.setMessagePayload(aes256Service.decrypt(payload));
            } catch (IllegalArgumentException | Aes256Exception ignored) {
                // Return encrypted payload
            }
        }
    }
}
