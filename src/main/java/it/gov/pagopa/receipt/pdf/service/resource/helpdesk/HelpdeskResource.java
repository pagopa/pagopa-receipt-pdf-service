package it.gov.pagopa.receipt.pdf.service.resource.helpdesk;

import it.gov.pagopa.receipt.pdf.service.client.BizCosmosClient;
import it.gov.pagopa.receipt.pdf.service.exception.*;
import it.gov.pagopa.receipt.pdf.service.filters.LoggedAPI;
import it.gov.pagopa.receipt.pdf.service.model.IOMessage;
import it.gov.pagopa.receipt.pdf.service.model.biz.BizEvent;
import it.gov.pagopa.receipt.pdf.service.model.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.service.model.receipt.ReceiptError;
import it.gov.pagopa.receipt.pdf.service.service.AttachmentsService;
import it.gov.pagopa.receipt.pdf.service.service.impl.ReceiptCosmosService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;

import static it.gov.pagopa.receipt.pdf.service.utils.CommonUtils.createProblemJson;
import static it.gov.pagopa.receipt.pdf.service.utils.CommonUtils.sanitize;


@Tag(name = "Helpdesk", description = "Helpdesk operations")
@Path("/helpdesk")
@LoggedAPI
public class HelpdeskResource {
    private final Logger logger = LoggerFactory.getLogger(HelpdeskResource.class);

    private ReceiptCosmosService receiptCosmosService;
    private BizCosmosClient bizEventCosmosClient;
    private AttachmentsService attachmentsService;

    @Inject
    public HelpdeskResource(ReceiptCosmosService receiptCosmosService, BizCosmosClient bizEventCosmosClient, AttachmentsService attachmentsService) {
        this.receiptCosmosService = receiptCosmosService;
        this.bizEventCosmosClient = bizEventCosmosClient;
        this.attachmentsService = attachmentsService;
    }

    @Path("/receipts/{event-id}")
    @GET
    public RestResponse<Object> getReceipt(
            @PathParam("event-id") String eventId) {

        if (eventId == null || eventId.isBlank()) {
            return RestResponse.status(Response.Status.BAD_REQUEST,
                    createProblemJson(Response.Status.BAD_REQUEST, "Please pass a valid biz-event id"));
        }

        try {
            var receipt = receiptCosmosService.getReceipt(eventId);
            return RestResponse.status(Response.Status.OK, receipt);
        } catch (ReceiptNotFoundException e) {
            String responseMsg = String.format("Unable to retrieve the receipt with eventId %s", sanitize(eventId));
            logger.error("[{}] {}", "getReceipt", responseMsg);
            return RestResponse.status(Response.Status.NOT_FOUND,
                    createProblemJson(Response.Status.NOT_FOUND, responseMsg));
        }
    }

    @Path("/receipts/organizations/{organization-fiscal-code}/iuvs/{iuv}")
    @GET
    public RestResponse<Object> getReceiptByOrganizationFiscalCodeAndIUV(
            @PathParam("organization-fiscal-code") String organizationFiscalCode,
            @PathParam("iuv") String iuv) {

        if (organizationFiscalCode == null
                || organizationFiscalCode.isBlank()
                || iuv == null
                || iuv.isBlank()
        ) {
            return RestResponse.status(Response.Status.BAD_REQUEST,
                    createProblemJson(Response.Status.BAD_REQUEST, "Please pass a valid organization fiscal code and iuv"));
        }

        BizEvent bizEvent;
        try {
            bizEvent = this.bizEventCosmosClient
                    .getBizEventDocumentByOrganizationFiscalCodeAndIUV(sanitize(organizationFiscalCode), sanitize(iuv));
        } catch (BizEventNotFoundException e) {
            String responseMsg = String.format("Unable to retrieve the biz-event with organization fiscal code %s and iuv %s",
                    organizationFiscalCode, iuv);
            logger.error("[{}] {}", "getReceiptByOrganizationFiscalCodeAndIUV", responseMsg, e);
            return RestResponse.status(Response.Status.NOT_FOUND,
                    createProblemJson(Response.Status.NOT_FOUND, responseMsg));
        }

        try {
            Receipt receipt = this.receiptCosmosService.getReceipt(bizEvent.getId());
            return RestResponse.status(Response.Status.OK, receipt);

        } catch (ReceiptNotFoundException e) {
            String responseMsg = String.format("Unable to retrieve the receipt with eventId %s", bizEvent.getId());
            logger.error("[{}] {}", "getReceiptByOrganizationFiscalCodeAndIUV", responseMsg, e);
            return RestResponse.status(Response.Status.NOT_FOUND,
                    createProblemJson(Response.Status.NOT_FOUND, responseMsg));
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

    @Path("pdf-receipts/{file-name}")
    @GET
    public RestResponse<Object> getReceiptPdf(
            @PathParam("file-name") String fileName) {
        logger.info("[{}] API called at {}", "getReceiptPdf", LocalDateTime.now());

        if (fileName == null || fileName.isBlank()) {
            return RestResponse.status(Response.Status.BAD_REQUEST,
                    createProblemJson(Response.Status.BAD_REQUEST, "Please pass a valid file name"));
        }

        try {
            byte[] result = attachmentsService.getAttachmentBytesFromBlobStorage(fileName);
            return RestResponse.ok(result);
        } catch (BlobStorageClientException | AttachmentNotFoundException | IOException e) {
            String responseMsg = String.format("Unable to retrieve the receipt pdf with file name %s", fileName);
            logger.error("[{}] {}", "getReceiptPdf", responseMsg, e);
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
