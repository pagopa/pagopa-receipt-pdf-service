package it.gov.pagopa.receipt.pdf.service.client;

import it.gov.pagopa.receipt.pdf.service.model.SearchTokenRequest;
import it.gov.pagopa.receipt.pdf.service.model.SearchTokenResponse;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("tokenizer/v1/token")
@RegisterRestClient
public interface PDVTokenizerClient {

  @POST
  @Path("/search")
  @ClientHeaderParam(name = "x-api-key", value = "${pdv.tokenizer.apiKey}")
  SearchTokenResponse searchToken(SearchTokenRequest searchTokenRequest);

}
