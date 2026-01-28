package it.gov.pagopa.receipt.pdf.service.enumeration;

import java.util.List;

/**
 * Enumeration of the receipt status
 */
public enum ReceiptStatusType {
    NOT_QUEUE_SENT,
    INSERTED,
    RETRY,
    GENERATED,
    SIGNED,
    FAILED,
    IO_NOTIFIED,
    IO_ERROR_TO_NOTIFY,
    IO_NOTIFIER_RETRY,
    UNABLE_TO_SEND,
    NOT_TO_NOTIFY,
    TO_REVIEW;

    public static List<ReceiptStatusType> pdfWaitingToBeGenerated(){
        return List.of(INSERTED, RETRY);
    }

    public static List<ReceiptStatusType> pdfGenerated(){
        return List.of(GENERATED, SIGNED, IO_NOTIFIED, IO_ERROR_TO_NOTIFY, IO_NOTIFIER_RETRY, UNABLE_TO_SEND, NOT_TO_NOTIFY);
    }

    public static List<ReceiptStatusType> pdfFailedToBeGenerated(){
        return List.of(NOT_QUEUE_SENT, FAILED, TO_REVIEW);
    }
}
