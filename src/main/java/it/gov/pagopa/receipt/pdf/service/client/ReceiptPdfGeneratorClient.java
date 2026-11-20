package it.gov.pagopa.receipt.pdf.service.client;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@ClientHeaderParam(name = "Ocp-Apim-Subscription-Key", value = "${receipt.pdf.generator.api-key}")
public interface ReceiptPdfGeneratorClient {

    @POST
    @Path("/receipts/{event-id}/regenerate-receipt-pdf")
    Response regenerateReceipt(@PathParam("event-id") String eventId);

    @POST
    @Path("/cart-receipts/{cart-id}/regenerate-receipt-pdf")
    Response regenerateCart(@PathParam("cart-id") String cartId);
}
