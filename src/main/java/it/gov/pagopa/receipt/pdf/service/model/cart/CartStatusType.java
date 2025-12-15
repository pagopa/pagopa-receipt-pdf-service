package it.gov.pagopa.receipt.pdf.service.model.cart;

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
  TO_REVIEW
}
