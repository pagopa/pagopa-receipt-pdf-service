package it.gov.pagopa.receipt.pdf.service.resource.helpdesk;

import io.quarkus.arc.profile.IfBuildProfile;
import it.gov.pagopa.receipt.pdf.service.client.BizCosmosClient;
import it.gov.pagopa.receipt.pdf.service.exception.*;
import it.gov.pagopa.receipt.pdf.service.filters.LoggedAPI;
import it.gov.pagopa.receipt.pdf.service.model.IOMessage;
import it.gov.pagopa.receipt.pdf.service.model.biz.BizEvent;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartIOMessage;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;

import static it.gov.pagopa.receipt.pdf.service.utils.CommonUtils.createProblemJson;
import static it.gov.pagopa.receipt.pdf.service.utils.CommonUtils.sanitize;


@Tag(name = "Helpdesk", description = "Helpdesk operations")
@LoggedAPI
@Path("")
@IfBuildProfile(anyOf = {"build", "dev", "uat", "prod", "test", "helpdesk"})
public class HelpdeskResource {
    public static final String RECEIPT_NOT_FOUND_BY_EVENTID = "Unable to retrieve the receipt with eventId %s";
    public static final String LOG_ERROR_MESSAGE = "[{}] {}";
    private static final String HELPDESK_SUFFIX = "[HELPDESK] ";

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
    @Operation(
            summary = "Get Receipt",
            description = "Retrieve the receipt document by event id"
    )
    @APIResponses(
            value = {
                    @APIResponse(ref = "#/components/responses/InternalServerError"),
                    @APIResponse(ref = "#/components/responses/AppException400"),
                    @APIResponse(ref = "#/components/responses/AppException404"),
                    @APIResponse(
                            responseCode = "200",
                            description = "Success",
                            content =
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = Receipt.class)
                            )
                    )
            }
    )
    @Path("/receipts/{event-id}")
    @GET
    public RestResponse<Object> getReceipt(
            @PathParam("event-id") String eventId) {

        if (eventId == null || eventId.isBlank()) {
            return RestResponse.status(Response.Status.BAD_REQUEST,
                    createProblemJson(Response.Status.BAD_REQUEST, HELPDESK_SUFFIX, "Please pass a valid biz-event id"));
        }

        try {
            Receipt receipt = receiptCosmosService.getReceipt(eventId);
            return RestResponse.ok(receipt);
        } catch (ReceiptNotFoundException e) {
            String responseMsg = String.format(RECEIPT_NOT_FOUND_BY_EVENTID, sanitize(eventId));
            logger.error(LOG_ERROR_MESSAGE, "getReceipt", responseMsg);
            return RestResponse.status(Response.Status.NOT_FOUND,
                    createProblemJson(Response.Status.NOT_FOUND, HELPDESK_SUFFIX, responseMsg));
        }
    }

    @Operation(
            summary = "Get Receipt by organization fiscal code and iuv",
            description = "Retrieve the receipt document by organization fiscal code and iuv"
    )
    @APIResponses(
            value = {
                    @APIResponse(ref = "#/components/responses/InternalServerError"),
                    @APIResponse(ref = "#/components/responses/AppException400"),
                    @APIResponse(ref = "#/components/responses/AppException404"),
                    @APIResponse(
                            responseCode = "200",
                            description = "Success",
                            content =
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = Receipt.class)
                            )
                    )
            }
    )
    @Path("/receipts/organizations/{organization-fiscal-code}/iuvs/{iuv}")
    @GET
    public RestResponse<Object> getReceiptByOrganizationFiscalCodeAndIUV(
            @PathParam("organization-fiscal-code") String organizationFiscalCode,
            @PathParam("iuv") String iuv) {

        if (organizationFiscalCode == null || organizationFiscalCode.isBlank()) {
            return RestResponse.status(Response.Status.BAD_REQUEST, createProblemJson(Response.Status.BAD_REQUEST, HELPDESK_SUFFIX, "Please pass a valid organization fiscal code"));
        }

        if (iuv == null || iuv.isBlank()) {
            return RestResponse.status(Response.Status.BAD_REQUEST, createProblemJson(Response.Status.BAD_REQUEST, HELPDESK_SUFFIX, "Please pass a valid iuv"));
        }

        BizEvent bizEvent;
        try {
            bizEvent = this.bizEventCosmosClient
                    .getBizEventDocumentByOrganizationFiscalCodeAndIUV(sanitize(organizationFiscalCode), sanitize(iuv));
        } catch (BizEventNotFoundException e) {
            String responseMsg = String.format("Unable to retrieve the biz-event with organization fiscal code %s and iuv %s",
                    sanitize(organizationFiscalCode), sanitize(iuv));
            logger.error(LOG_ERROR_MESSAGE, "getReceiptByOrganizationFiscalCodeAndIUV", responseMsg, e);
            return RestResponse.status(Response.Status.NOT_FOUND,
                    createProblemJson(Response.Status.NOT_FOUND, HELPDESK_SUFFIX, responseMsg));
        }

        try {
            Receipt receipt = this.receiptCosmosService.getReceipt(bizEvent.getId());
            return RestResponse.ok(receipt);
        } catch (ReceiptNotFoundException e) {
            String responseMsg = String.format(RECEIPT_NOT_FOUND_BY_EVENTID, bizEvent.getId());
            logger.error(LOG_ERROR_MESSAGE, "getReceiptByOrganizationFiscalCodeAndIUV", responseMsg, e);
            return RestResponse.status(Response.Status.NOT_FOUND,
                    createProblemJson(Response.Status.NOT_FOUND, HELPDESK_SUFFIX, responseMsg));
        }
    }

    @Operation(
            summary = "Get Receipt Message",
            description = "Retrieve the receipt-message document by message id"
    )
    @APIResponses(
            value = {
                    @APIResponse(ref = "#/components/responses/InternalServerError"),
                    @APIResponse(ref = "#/components/responses/AppException400"),
                    @APIResponse(ref = "#/components/responses/AppException404"),
                    @APIResponse(
                            responseCode = "200",
                            description = "Success",
                            content =
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = IOMessage.class)
                            )
                    )
            }
    )
    @Path("/receipts/io-message/{message-id}")
    @GET
    public RestResponse<Object> getReceiptMessage(
            @PathParam("message-id") String messageId) {

        if (messageId == null || messageId.isBlank()) {
            return RestResponse.status(Response.Status.BAD_REQUEST,
                    createProblemJson(Response.Status.BAD_REQUEST, HELPDESK_SUFFIX, "Please pass a valid messageId"));
        }

        try {
            IOMessage receipt = this.receiptCosmosService.getReceiptMessage(messageId);
            return RestResponse.ok(receipt);
        } catch (IoMessageNotFoundException e) {
            String responseMsg = String.format("Unable to retrieve the receipt message with messageId %s", sanitize(messageId));
            logger.error(LOG_ERROR_MESSAGE, "getReceiptMessage", responseMsg, e);
            return RestResponse.status(Response.Status.NOT_FOUND,
                    createProblemJson(Response.Status.NOT_FOUND, HELPDESK_SUFFIX, responseMsg));
        }
    }

    @Operation(
            summary = "Get Receipt PDF",
            description = "Retrieve the receipt pdf by file name"
    )
    @APIResponses(
            value = {
                    @APIResponse(ref = "#/components/responses/InternalServerError"),
                    @APIResponse(ref = "#/components/responses/AppException400"),
                    @APIResponse(ref = "#/components/responses/AppException404"),
                    @APIResponse(
                            responseCode = "200",
                            description = "Success",
                            content =
                            @Content(
                                    mediaType = "application/pdf"
                            )
                    )
            }
    )
    @Path("pdf-receipts/{file-name}")
    @GET
    public RestResponse<Object> getReceiptPdf(
            @PathParam("file-name") String fileName) {
        logger.info("[{}] API called at {}", "getReceiptPdf", LocalDateTime.now());

        if (fileName == null || fileName.isBlank()) {
            return RestResponse.status(Response.Status.BAD_REQUEST,
                    createProblemJson(Response.Status.BAD_REQUEST, HELPDESK_SUFFIX, "Please pass a valid file name"));
        }

        try {
            byte[] result = attachmentsService.getAttachmentBytesFromBlobStorage(fileName);
            return RestResponse.ResponseBuilder.ok((Object) result)
                    .header("content-type", "application/pdf")
                    .header("content-disposition", "attachment;")
                    .build();
        } catch (BlobStorageClientException | AttachmentNotFoundException | IOException e) {
            String responseMsg = String.format("Unable to retrieve the receipt pdf with file name %s", sanitize(fileName));
            logger.error(LOG_ERROR_MESSAGE, "getReceiptPdf", responseMsg, e);
            return RestResponse.status(Response.Status.NOT_FOUND,
                    createProblemJson(Response.Status.NOT_FOUND, HELPDESK_SUFFIX, responseMsg));
        }
    }

    @Operation(
            summary = "Get Receipt Error by event id",
            description = "Retrieve the receipt-error document by event id"
    )
    @APIResponses(
            value = {
                    @APIResponse(ref = "#/components/responses/InternalServerError"),
                    @APIResponse(ref = "#/components/responses/AppException400"),
                    @APIResponse(ref = "#/components/responses/AppException404"),
                    @APIResponse(
                            responseCode = "200",
                            description = "Success",
                            content =
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ReceiptError.class)
                            )
                    )
            }
    )
    @Path("/errors-toreview/{bizevent-id}")
    @GET
    public RestResponse<Object> getReceiptErrorByEventId(
            @PathParam("bizevent-id") String eventId) {

        if (eventId == null || eventId.isBlank()) {
            return RestResponse.status(Response.Status.BAD_REQUEST,
                    createProblemJson(Response.Status.BAD_REQUEST, HELPDESK_SUFFIX, "Missing valid search parameter"));
        }

        try {
            ReceiptError receiptError = receiptCosmosService.getReceiptError(eventId);
            return RestResponse.ok(receiptError);
        } catch (ReceiptNotFoundException e) {
            String responseMsg = "No Receipt Error to process on bizEvent with id " + sanitize(eventId);
            logger.error(LOG_ERROR_MESSAGE, "getReceiptErrorByEventId", responseMsg, e);
            return RestResponse.status(Response.Status.NOT_FOUND,
                    createProblemJson(Response.Status.NOT_FOUND, HELPDESK_SUFFIX, responseMsg));
        } catch (Exception e) {
            logger.error(LOG_ERROR_MESSAGE, "getReceiptErrorByEventId", e.getMessage(), e);
            return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR,
                    createProblemJson(Response.Status.INTERNAL_SERVER_ERROR, HELPDESK_SUFFIX, e.getMessage()));
        }
    }

    // Cart Receipts
    @Operation(
            summary = "Get CartReceipt",
            description = "Retrieve the cart-receipt document by cart id"
    )
    @APIResponses(
            value = {
                    @APIResponse(ref = "#/components/responses/InternalServerError"),
                    @APIResponse(ref = "#/components/responses/AppException400"),
                    @APIResponse(ref = "#/components/responses/AppException404"),
                    @APIResponse(
                            responseCode = "200",
                            description = "Success",
                            content =
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = CartForReceipt.class)
                            )
                    )
            }
    )
    @Path("/cart-receipts/{cart-id}")
    @GET
    public RestResponse<Object> getCartReceipt(
            @PathParam("cart-id") String cartId) {

        if (cartId == null || cartId.isBlank()) {
            return RestResponse.status(Response.Status.BAD_REQUEST, createProblemJson(Response.Status.BAD_REQUEST, HELPDESK_SUFFIX, "Please pass a valid cartId"));
        }

        try {
            CartForReceipt receipt = cartReceiptCosmosService.getCartReceipt(cartId);
            return RestResponse.ok(receipt);
        } catch (CartNotFoundException e) {
            String responseMsg = String.format("Unable to retrieve the receipt with cartId %s", sanitize(cartId));
            logger.error(LOG_ERROR_MESSAGE, "getCartReceipt", responseMsg, e);
            return RestResponse.status(Response.Status.NOT_FOUND, createProblemJson(Response.Status.NOT_FOUND, HELPDESK_SUFFIX, responseMsg));
        }
    }

    @Operation(
            summary = "Get CartReceipt by organization fiscal code and iuv",
            description = "Retrieve the cart-receipt document by organization fiscal code and iuv"
    )
    @APIResponses(
            value = {
                    @APIResponse(ref = "#/components/responses/InternalServerError"),
                    @APIResponse(ref = "#/components/responses/AppException400"),
                    @APIResponse(ref = "#/components/responses/AppException404"),
                    @APIResponse(
                            responseCode = "200",
                            description = "Success",
                            content =
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = CartForReceipt.class)
                            )
                    )
            }
    )
    @Path("/cart-receipts/organizations/{organization-fiscal-code}/iuvs/{iuv}")
    @GET
    public RestResponse<Object> getCartReceiptByOrganizationFiscalCodeAndIUV(
            @PathParam("organization-fiscal-code") String organizationFiscalCode,
            @PathParam("iuv") String iuv) {

        if (organizationFiscalCode == null || organizationFiscalCode.isBlank()) {
            return RestResponse.status(Response.Status.BAD_REQUEST, createProblemJson(Response.Status.BAD_REQUEST, HELPDESK_SUFFIX, "Please pass a valid organization fiscal code"));
        }

        if (iuv == null || iuv.isBlank()) {
            return RestResponse.status(Response.Status.BAD_REQUEST, createProblemJson(Response.Status.BAD_REQUEST, HELPDESK_SUFFIX, "Please pass a valid iuv"));
        }

        BizEvent bizEvent;
        try {
            bizEvent = this.bizEventCosmosClient
                    .getBizEventDocumentByOrganizationFiscalCodeAndIUV(organizationFiscalCode, iuv);
        } catch (BizEventNotFoundException e) {
            String responseMsg = String.format("Unable to retrieve the biz-event with organization fiscal code %s and iuv %s",
                    sanitize(organizationFiscalCode), sanitize(iuv));
            logger.error(LOG_ERROR_MESSAGE, "getCartReceiptByOrganizationFiscalCodeAndIUV", responseMsg, e);
            return RestResponse.status(Response.Status.NOT_FOUND, createProblemJson(Response.Status.NOT_FOUND, HELPDESK_SUFFIX, responseMsg));
        }

        try {
            CartForReceipt receipt = this.cartReceiptCosmosService.getCartReceipt(bizEvent.getTransactionDetails().getTransaction().getTransactionId());
            return RestResponse.ok(receipt);
        } catch (CartNotFoundException e) {
            String responseMsg = String.format(RECEIPT_NOT_FOUND_BY_EVENTID, bizEvent.getId());
            logger.error(LOG_ERROR_MESSAGE, "getCartReceiptByOrganizationFiscalCodeAndIUV", responseMsg, e);
            return RestResponse.status(Response.Status.NOT_FOUND, createProblemJson(Response.Status.NOT_FOUND, HELPDESK_SUFFIX, responseMsg));
        }
    }

    @Operation(
            summary = "Get CartReceipt Message",
            description = "Retrieve the cart-receipt-message document by message id"
    )
    @APIResponses(
            value = {
                    @APIResponse(ref = "#/components/responses/InternalServerError"),
                    @APIResponse(ref = "#/components/responses/AppException400"),
                    @APIResponse(ref = "#/components/responses/AppException404"),
                    @APIResponse(
                            responseCode = "200",
                            description = "Success",
                            content =
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = IOMessage.class)
                            )
                    )
            }
    )
    @Path("/cart-receipts/io-message/{message-id}")
    @GET
    public RestResponse<Object> getCartReceiptMessage(
            @PathParam("message-id") String messageId) {

        if (messageId == null || messageId.isBlank()) {
            return RestResponse.status(Response.Status.BAD_REQUEST,
                    createProblemJson(Response.Status.BAD_REQUEST, HELPDESK_SUFFIX, "Please pass a valid messageId"));
        }

        try {
            CartIOMessage ioMessage = this.cartReceiptCosmosService.getCartReceiptMessage(messageId);
            return RestResponse.ok(ioMessage);
        } catch (IoMessageNotFoundException e) {
            String responseMsg = String.format("Unable to retrieve the cart receipt message with messageId %s", sanitize(messageId));
            logger.error(LOG_ERROR_MESSAGE, "getCartReceiptMessage", responseMsg, e);
            return RestResponse.status(Response.Status.NOT_FOUND,
                    createProblemJson(Response.Status.NOT_FOUND, HELPDESK_SUFFIX, responseMsg));
        }
    }

    @Operation(
            summary = "Get CartReceipt Error",
            description = "Retrieve the cart-receipt-error document by cart id"
    )
    @APIResponses(
            value = {
                    @APIResponse(ref = "#/components/responses/InternalServerError"),
                    @APIResponse(ref = "#/components/responses/AppException400"),
                    @APIResponse(ref = "#/components/responses/AppException404"),
                    @APIResponse(
                            responseCode = "200",
                            description = "Success",
                            content =
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = CartReceiptError.class)
                            )
                    )
            }
    )
    @Path("/cart-errors-toreview/{cart-id}")
    @GET
    public RestResponse<Object> getCartReceiptErrorByCartId(
            @PathParam("cart-id") String cartId) {

        if (cartId == null || cartId.isBlank()) {
            return RestResponse.status(Response.Status.BAD_REQUEST,
                    createProblemJson(Response.Status.BAD_REQUEST, HELPDESK_SUFFIX, "Missing valid search parameter"));
        }

        try {
            CartReceiptError receiptError = this.cartReceiptCosmosService.getCartReceiptError(cartId);
            return RestResponse.ok(receiptError);
        } catch (CartNotFoundException e) {
            String responseMsg = "No Receipt Error to process on cartId with id " + sanitize(cartId);
            logger.error(LOG_ERROR_MESSAGE, "getCartReceiptErrorByCartId", responseMsg, e);
            return RestResponse.status(Response.Status.NOT_FOUND,
                    createProblemJson(Response.Status.NOT_FOUND, HELPDESK_SUFFIX, responseMsg));
        } catch (Exception e) {
            logger.error(LOG_ERROR_MESSAGE, "getCartReceiptErrorByCartId", e.getMessage(), e);
            return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR,
                    createProblemJson(Response.Status.INTERNAL_SERVER_ERROR, HELPDESK_SUFFIX, e.getMessage()));
        }
    }
}
