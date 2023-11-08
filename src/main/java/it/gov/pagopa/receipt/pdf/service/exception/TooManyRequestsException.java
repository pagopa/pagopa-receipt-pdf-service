package it.gov.pagopa.receipt.pdf.service.exception;

public class TooManyRequestsException extends RuntimeException {

  public TooManyRequestsException(String error) {
    super(error);
  }

}
