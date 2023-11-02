package it.gov.pagopa.receipt.pdf.service.client.exceptions;

public class TooManyRequestsException extends RuntimeException {

  public TooManyRequestsException(String error) {
    super(error);
  }

}
