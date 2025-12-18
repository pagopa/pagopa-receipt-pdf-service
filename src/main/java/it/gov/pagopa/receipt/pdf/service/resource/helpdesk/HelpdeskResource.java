package it.gov.pagopa.receipt.pdf.service.resource.helpdesk;

import it.gov.pagopa.receipt.pdf.service.client.BizCosmosClient;
import it.gov.pagopa.receipt.pdf.service.exception.BizEventNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.ReceiptNotFoundException;
import it.gov.pagopa.receipt.pdf.service.filters.LoggedAPI;
import it.gov.pagopa.receipt.pdf.service.model.biz.BizEvent;
import it.gov.pagopa.receipt.pdf.service.model.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.service.service.impl.ReceiptCosmosService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;


@Tag(name = "Helpdesk", description = "Helpdesk operations")
@Path("/helpdesk")
@LoggedAPI
public class HelpdeskResource {

    private ReceiptCosmosService receiptCosmosService;
    private BizCosmosClient bizEventCosmosClient;


    @Path("/receipts/{event-id}")
    @GET
    public RestResponse<Receipt> getReceipt(
            @PathParam("event-id") String eventId) {

        if (eventId == null || eventId.isBlank()) {
            return RestResponse.status(Response.Status.BAD_REQUEST);
        }

        try {
            var receipt = receiptCosmosService.getReceipt(eventId);
            return RestResponse.status(Response.Status.OK, receipt);
        } catch (ReceiptNotFoundException e) {
            String responseMsg = String.format("Unable to retrieve the receipt with eventId %s", eventId);
            return RestResponse.status(Response.Status.NOT_FOUND);

        }
    }


    @Path("/receipts/organizations/{organization-fiscal-code}/iuvs/{iuv}")
    @GET
    public RestResponse<Receipt> GetReceiptByOrganizationFiscalCodeAndIUV(
            @PathParam("organization-fiscal-code") String organizationFiscalCode,
            @PathParam("iuv") String iuv) {

        if (organizationFiscalCode == null
                || organizationFiscalCode.isBlank()
                || iuv == null
                || iuv.isBlank()
        ) {
            return RestResponse.status(Response.Status.BAD_REQUEST);
        }

        BizEvent bizEvent;
        try {
            bizEvent = this.bizEventCosmosClient
                    .getBizEventDocumentByOrganizationFiscalCodeAndIUV(organizationFiscalCode, iuv);
        } catch (BizEventNotFoundException e) {
            String responseMsg = String.format("Unable to retrieve the biz-event with organization fiscal code %s and iuv %s",
                    organizationFiscalCode, iuv);
            return RestResponse.status(Response.Status.NOT_FOUND);

        }

        try {
            Receipt receipt = this.receiptCosmosService.getReceipt(bizEvent.getId());
            return RestResponse.status(Response.Status.BAD_REQUEST, receipt);

        } catch (ReceiptNotFoundException e) {
            String responseMsg = String.format("Unable to retrieve the receipt with eventId %s", bizEvent.getId());
            return RestResponse.status(Response.Status.NOT_FOUND);

        }
    }
}
