package it.gov.pagopa.receipt.pdf.service.client;

import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import it.gov.pagopa.receipt.pdf.service.exception.PDVTokenizerClientException;
import it.gov.pagopa.receipt.pdf.service.exception.TooManyRequestsException;
import it.gov.pagopa.receipt.pdf.service.model.SearchTokenRequest;
import it.gov.pagopa.receipt.pdf.service.model.SearchTokenResponse;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import java.time.temporal.ChronoUnit;

@Path("/tokenizer/v1/tokens")
@RegisterRestClient
public interface PDVTokenizerClient {

  @POST
  @Path("/search")
  @Retry(delay = 1000L, delayUnit = ChronoUnit.MILLIS, maxRetries = 3,
      retryOn = TooManyRequestsException.class)
  @ExponentialBackoff(maxDelay = 60000L, factor = 2)
  @ClientHeaderParam(name = "x-api-key", value = "${pdv.tokenizer.apiKey}")
  SearchTokenResponse searchToken(SearchTokenRequest searchTokenRequest);

  @ClientExceptionMapper
  static RuntimeException toException(Response response) {
    if (response.getStatus() == 429) {
      return new TooManyRequestsException("The remote service responded with HTTP 429");
    } else if (response.getStatus() != 200) {
      return new PDVTokenizerClientException("Tokenizer client returned status "  +
       response.getStatus());
    }
    return null;
  }

}
