package it.gov.pagopa.receipt.pdf.service.model.cart;

import java.util.Set;

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

    public static final Set<CartStatusType> PDF_WAITING_TO_BE_GENERATED = Set.of(
            WAITING_FOR_BIZ_EVENT,
            INSERTED,
            RETRY
    );

    public static final Set<CartStatusType> PDF_GENERATED = Set.of(
            GENERATED,
            SIGNED,
            IO_NOTIFIED,
            IO_ERROR_TO_NOTIFY,
            IO_NOTIFIER_RETRY,
            UNABLE_TO_SEND,
            NOT_TO_NOTIFY
    );

    public static final Set<CartStatusType> PDF_FAILED_TO_BE_GENERATED = Set.of(
            NOT_QUEUE_SENT,
            FAILED,
            TO_REVIEW
    );

}
