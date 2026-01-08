package it.gov.pagopa.receipt.pdf.service.resource.helpdesk;

import it.gov.pagopa.receipt.pdf.service.client.BizCosmosClient;
import it.gov.pagopa.receipt.pdf.service.exception.BizEventNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.IoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.ReceiptNotFoundException;
import it.gov.pagopa.receipt.pdf.service.filters.LoggedAPI;
import it.gov.pagopa.receipt.pdf.service.model.IOMessage;
import it.gov.pagopa.receipt.pdf.service.model.ProblemJson;
import it.gov.pagopa.receipt.pdf.service.model.biz.BizEvent;
import it.gov.pagopa.receipt.pdf.service.model.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.service.model.receipt.ReceiptError;
import it.gov.pagopa.receipt.pdf.service.service.impl.ReceiptCosmosService;
import it.gov.pagopa.receipt.pdf.service.utils.CommonUtils;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

import java.time.LocalDateTime;

import static it.gov.pagopa.receipt.pdf.service.utils.CommonUtils.createProblemJson;


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

    @Path("/receipts/io-message/{message-id}")
    @GET
    public RestResponse<Object> getReceiptMessage(
            @PathParam("message-id") String messageId) {

        if (messageId == null || messageId.isBlank()) {
            return RestResponse.status(Response.Status.BAD_REQUEST,
                    createProblemJson(Response.Status.BAD_REQUEST, "Please pass a valid messageId"));
        }

        try {
            IOMessage receipt = this.receiptCosmosService.getReceiptMessage(messageId);
            return RestResponse.ok(receipt);
        } catch (IoMessageNotFoundException e) {
            String responseMsg = String.format("Unable to retrieve the receipt message with messageId %s", messageId);
            return RestResponse.status(Response.Status.NOT_FOUND,
                    createProblemJson(Response.Status.NOT_FOUND, responseMsg));
        }
    }

    @Path("/errors-toreview/{bizevent-id}")
    @GET
    public RestResponse<Object> getReceiptErrorByEventId(
            @PathParam("bizevent-id") String eventId) {

        if (eventId == null || eventId.isBlank()) {
            return RestResponse.status(Response.Status.BAD_REQUEST,
                    createProblemJson(Response.Status.BAD_REQUEST, "Missing valid search parameter"));
        }

        try {
            ReceiptError receiptError = receiptCosmosService.getReceiptError(eventId);
            return RestResponse.ok(receiptError);
        } catch (ReceiptNotFoundException e) {
            return RestResponse.status(Response.Status.NOT_FOUND,
                    createProblemJson(Response.Status.NOT_FOUND, "No Receipt Error to process on bizEvent with id " + eventId));
        } catch (Exception e) {
            return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR,
                    createProblemJson(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }
}
