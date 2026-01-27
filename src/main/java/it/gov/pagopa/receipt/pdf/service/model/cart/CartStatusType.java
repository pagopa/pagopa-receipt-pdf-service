package it.gov.pagopa.receipt.pdf.service.model.cart;

import java.util.List;

public enum CartStatusType {

    WAITING_FOR_BIZ_EVENT,
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

    public static List<CartStatusType> pdfWaitingToBeGenerated(){
        return List.of(WAITING_FOR_BIZ_EVENT, INSERTED, RETRY);
    }

    public static List<CartStatusType> pdfGenerated(){
        return List.of(GENERATED, SIGNED, IO_NOTIFIED, IO_ERROR_TO_NOTIFY, IO_NOTIFIER_RETRY, UNABLE_TO_SEND, NOT_TO_NOTIFY);
    }

    public static List<CartStatusType> pdfFailedToBeGenerated(){
        return List.of(NOT_QUEUE_SENT, FAILED, TO_REVIEW);
    }

}
