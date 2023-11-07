package it.gov.pagopa.receipt.pdf.service.client;

import it.gov.pagopa.receipt.pdf.service.exception.PDVTokenizerClientException;
import it.gov.pagopa.receipt.pdf.service.exception.TooManyRequestsException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PDVTokenizerClientTest {

  @Test
  void tooManyRequestsStatusShouldReturnProperError() {
      Response response = Response.status(Status.TOO_MANY_REQUESTS).build();
      assertInstanceOf(TooManyRequestsException.class, PDVTokenizerClient.toException(response));
  }

  @Test
  void errorStatusShouldReturnProperError() {
    Response response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
    assertInstanceOf(PDVTokenizerClientException.class, PDVTokenizerClient.toException(response));
  }

}
