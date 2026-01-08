package it.gov.pagopa.receipt.pdf.service.service.impl;

import it.gov.pagopa.receipt.pdf.service.client.ReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.exception.Aes256Exception;
import it.gov.pagopa.receipt.pdf.service.exception.ReceiptNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.service.model.receipt.ReceiptError;
import it.gov.pagopa.receipt.pdf.service.utils.Aes256Utils;
import jakarta.enterprise.context.ApplicationScoped;

import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.PDFS_800;

@ApplicationScoped
public class ReceiptCosmosService {

    private ReceiptCosmosClient receiptCosmosClient;

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
            }
        }
    }

}
