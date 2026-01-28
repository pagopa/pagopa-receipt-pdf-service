package it.gov.pagopa.receipt.pdf.service.enumeration;

import java.util.Set;

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

    public static final Set<ReceiptStatusType> PDF_WAITING_TO_BE_GENERATED = Set.of(
            INSERTED,
            RETRY
    );

    public static final Set<ReceiptStatusType> PDF_GENERATED = Set.of(
            GENERATED,
            SIGNED,
            IO_NOTIFIED,
            IO_ERROR_TO_NOTIFY,
            IO_NOTIFIER_RETRY,
            UNABLE_TO_SEND,
            NOT_TO_NOTIFY
    );

    public static final Set<ReceiptStatusType> PDF_FAILED_TO_BE_GENERATED = Set.of(
            NOT_QUEUE_SENT,
            FAILED,
            TO_REVIEW
    );
}
