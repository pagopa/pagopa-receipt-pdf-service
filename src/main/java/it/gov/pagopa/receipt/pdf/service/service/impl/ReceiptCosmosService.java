package it.gov.pagopa.receipt.pdf.service.service.impl;

import it.gov.pagopa.receipt.pdf.service.client.ReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.exception.Aes256Exception;
import it.gov.pagopa.receipt.pdf.service.exception.IoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.ReceiptNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.IOMessage;
import it.gov.pagopa.receipt.pdf.service.model.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.service.model.receipt.ReceiptError;
import it.gov.pagopa.receipt.pdf.service.utils.Aes256Utils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.PDFS_800;

@ApplicationScoped
public class ReceiptCosmosService {

    private final ReceiptCosmosClient receiptCosmosClient;

    @Inject
    public ReceiptCosmosService(
            ReceiptCosmosClient receiptCosmosClient
    ) {
        this.receiptCosmosClient = receiptCosmosClient;
    }

    public Receipt getReceipt(String eventId) throws ReceiptNotFoundException {
        Receipt receipt;
        try {
            receipt = this.receiptCosmosClient.getReceiptDocument(eventId);
        } catch (ReceiptNotFoundException e) {
            String errorMsg = String.format("Receipt not found with the biz-event id %s", eventId);
            throw new ReceiptNotFoundException(PDFS_800, errorMsg, e);

        }

        if (receipt == null) {
            String errorMsg = String.format("Receipt retrieved with the biz-event id %s is null", eventId);
            throw new ReceiptNotFoundException(PDFS_800, errorMsg);
        }
        return receipt;
    }

    public IOMessage getReceiptMessage(String messageId) throws IoMessageNotFoundException {
        IOMessage message;
        try {
            message = this.receiptCosmosClient.getIoMessage(messageId);
        } catch (IoMessageNotFoundException e) {
            String errorMsg = String.format("Receipt Message to IO not found with the message id %s", messageId);
            throw new IoMessageNotFoundException(errorMsg, e);
        }

        if (message == null) {
            String errorMsg = String.format("Receipt retrieved with the message id %s is null", messageId);
            throw new IoMessageNotFoundException(errorMsg);
        }
        return message;
    }


    public ReceiptError getReceiptError(String eventId) throws ReceiptNotFoundException {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Event ID cannot be null or empty");
        }

        ReceiptError receiptError = receiptCosmosClient.getReceiptError(eventId);
        decryptPayload(receiptError);

        return receiptError;
    }

    private void decryptPayload(ReceiptError receiptError) {
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
