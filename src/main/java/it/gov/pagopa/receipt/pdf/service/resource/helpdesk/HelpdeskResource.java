package it.gov.pagopa.receipt.pdf.service.resource.helpdesk;

import it.gov.pagopa.receipt.pdf.service.client.BizCosmosClient;
import it.gov.pagopa.receipt.pdf.service.exception.*;
import it.gov.pagopa.receipt.pdf.service.filters.LoggedAPI;
import it.gov.pagopa.receipt.pdf.service.model.IOMessage;
import it.gov.pagopa.receipt.pdf.service.model.biz.BizEvent;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartReceiptError;
import it.gov.pagopa.receipt.pdf.service.model.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.service.model.receipt.ReceiptError;
import it.gov.pagopa.receipt.pdf.service.service.AttachmentsService;
import it.gov.pagopa.receipt.pdf.service.service.impl.CartReceiptCosmosService;
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
    public static final String RECEIPT_NOT_FOUND_BY_EVENTID = "Unable to retrieve the receipt with eventId %s";
    private final Logger logger = LoggerFactory.getLogger(HelpdeskResource.class);

    private final ReceiptCosmosService receiptCosmosService;
    private final CartReceiptCosmosService cartReceiptCosmosService;
    private final BizCosmosClient bizEventCosmosClient;
    private final AttachmentsService attachmentsService;

    @Inject
    public HelpdeskResource(ReceiptCosmosService receiptCosmosService, CartReceiptCosmosService cartReceiptCosmosService, BizCosmosClient bizEventCosmosClient, AttachmentsService attachmentsService) {
        this.receiptCosmosService = receiptCosmosService;
        this.cartReceiptCosmosService = cartReceiptCosmosService;
        this.bizEventCosmosClient = bizEventCosmosClient;
        this.attachmentsService = attachmentsService;
    }

    // Receipts
    @Path("/receipts/{event-id}")
    @GET
    public RestResponse<Object> getReceipt(
            @PathParam("event-id") String eventId) {

        if (eventId == null || eventId.isBlank()) {
            return RestResponse.status(Response.Status.BAD_REQUEST,
                    createProblemJson(Response.Status.BAD_REQUEST, "Please pass a valid biz-event id"));
        }

        try {
            Receipt receipt = receiptCosmosService.getReceipt(eventId);
            return RestResponse.ok(receipt);
        } catch (ReceiptNotFoundException e) {
            String responseMsg = String.format(RECEIPT_NOT_FOUND_BY_EVENTID, sanitize(eventId));
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
                    sanitize(organizationFiscalCode), sanitize(iuv));
            logger.error("[{}] {}", "getReceiptByOrganizationFiscalCodeAndIUV", responseMsg, e);
            return RestResponse.status(Response.Status.NOT_FOUND,
                    createProblemJson(Response.Status.NOT_FOUND, responseMsg));
        }

        try {
            Receipt receipt = this.receiptCosmosService.getReceipt(bizEvent.getId());
            return RestResponse.ok(receipt);
        } catch (ReceiptNotFoundException e) {
            String responseMsg = String.format(RECEIPT_NOT_FOUND_BY_EVENTID, bizEvent.getId());
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
            String responseMsg = String.format("Unable to retrieve the receipt message with messageId %s", sanitize(messageId));
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
            String responseMsg = String.format("Unable to retrieve the receipt pdf with file name %s", sanitize(fileName));
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
                    createProblemJson(Response.Status.NOT_FOUND, "No Receipt Error to process on bizEvent with id " + sanitize(eventId)));
        } catch (Exception e) {
            return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR,
                    createProblemJson(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }

    // Cart Receipts
    @Path("/receipts/cart/{cart-id}")
    @GET
    public RestResponse<Object> getCartReceipt(
            @PathParam("cart-id") String cartId) {

        if (cartId == null || cartId.isBlank()) {
            return RestResponse.status(Response.Status.BAD_REQUEST);
        }

        try {
            CartForReceipt receipt = cartReceiptCosmosService.getCartReceipt(cartId);
            return RestResponse.ok(receipt);
        } catch (CartNotFoundException e) {
            String responseMsg = String.format("Unable to retrieve the receipt with cartId %s", cartId);
            return RestResponse.status(Response.Status.NOT_FOUND, responseMsg);
        }
    }


    @Path("/receipts/cart/organizations/{organization-fiscal-code}/iuvs/{iuv}")
    @GET
    public RestResponse<Object> getCartReceiptByOrganizationFiscalCodeAndIUV(
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
            return RestResponse.status(Response.Status.NOT_FOUND, responseMsg);
        }

        try {
            CartForReceipt receipt = this.cartReceiptCosmosService.getCartReceiptFromEventId(bizEvent.getId());
            return RestResponse.ok(receipt);
        } catch (CartNotFoundException e) {
            String responseMsg = String.format(RECEIPT_NOT_FOUND_BY_EVENTID, bizEvent.getId());
            return RestResponse.status(Response.Status.NOT_FOUND, responseMsg);
        }
    }

    @Path("/receipts/cart/io-message/{message-id}")
    @GET
    public RestResponse<Object> getCartReceiptMessage(
            @PathParam("message-id") String messageId) {

        if (messageId == null || messageId.isBlank()) {
            return RestResponse.status(Response.Status.BAD_REQUEST,
                    createProblemJson(Response.Status.BAD_REQUEST, "Please pass a valid messageId"));
        }

        try {
            IOMessage ioMessage = this.cartReceiptCosmosService.getCartReceiptMessage(messageId);
            return RestResponse.ok(ioMessage);
        } catch (IoMessageNotFoundException e) {
            String responseMsg = String.format("Unable to retrieve the receipt message with messageId %s", messageId);
            return RestResponse.status(Response.Status.NOT_FOUND,
                    createProblemJson(Response.Status.NOT_FOUND, responseMsg));
        }
    }

    @Path("/errors-toreview/cart/{cart-id}")
    @GET
    public RestResponse<Object> getCartReceiptErrorByCartId(
            @PathParam("cart-id") String cartId) {

        if (cartId == null || cartId.isBlank()) {
            return RestResponse.status(Response.Status.BAD_REQUEST,
                    createProblemJson(Response.Status.BAD_REQUEST, "Missing valid search parameter"));
        }

        try {
            CartReceiptError receiptError = this.cartReceiptCosmosService.getCartReceiptError(cartId);
            return RestResponse.ok(receiptError);
        } catch (CartNotFoundException e) {
            return RestResponse.status(Response.Status.NOT_FOUND,
                    createProblemJson(Response.Status.NOT_FOUND, "No Receipt Error to process on cartId with id " + cartId));
        } catch (Exception e) {
            return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR,
                    createProblemJson(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }
}
