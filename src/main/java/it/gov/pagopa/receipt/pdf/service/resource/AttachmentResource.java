package it.gov.pagopa.receipt.pdf.service.resource;

import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.PdfServiceException;
import it.gov.pagopa.receipt.pdf.service.model.AttachmentDetailsResponse;
import it.gov.pagopa.receipt.pdf.service.model.ErrorMessage;
import it.gov.pagopa.receipt.pdf.service.model.ErrorResponse;
import it.gov.pagopa.receipt.pdf.service.service.AttachmentsService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestHeader;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.Collections;

import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.PDFS_900;
import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.PDFS_901;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Tag(name = "Attachments", description = "Attachments operations")
@Path("/receipts/pdf/messages")
public class AttachmentResource {

    @Inject
    private Logger logger;

    @Inject
    private AttachmentsService attachmentsService;

    @Operation(summary = "Get attachment details", description = "Retrieve the attachment details linked to the provided third party data id")
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
                                    schema = @Schema(implementation = AttachmentDetailsResponse.class)))
            })
    @Path("/{tp_id}")
    @GET
    public RestResponse<?> getAttachmentDetails(
            @PathParam("tp_id") String thirdPartyId,
            @RestHeader("fiscal_code") String requestFiscalCode
            ) {

        if (thirdPartyId == null) {
            logger.error("Third party id is null");
            return RestResponse.status(BAD_REQUEST, buildErrorResponse(PDFS_900, BAD_REQUEST));
        }

        if (requestFiscalCode == null) {
            logger.error("Fiscal code header is null");
            return RestResponse.status(BAD_REQUEST, buildErrorResponse(PDFS_901, BAD_REQUEST));
        }

        AttachmentDetailsResponse attachmentDetails;
        try {
             attachmentDetails = attachmentsService.getAttachmentDetails(thirdPartyId, requestFiscalCode);
        } catch (PdfServiceException e) {
            logger.error("Error retrieving the receipt", e);
            return RestResponse.status(
                    INTERNAL_SERVER_ERROR,
                    buildErrorResponse(e.getErrorCode(), INTERNAL_SERVER_ERROR)
            );
        }

        return RestResponse.status(Status.OK, attachmentDetails);
    }

    @Operation(summary = "Get attachment", description = "Retrieve the attachment linked to the provided third party data id from the provided attachment url")
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
                                    mediaType = "application/pdf"))
            })
    @Path("/{tp_id}/{attachment_url}")
    @GET
    public RestResponse<byte[]> getAttachment(
            @PathParam("tp_id") String thirdPartyId,
            @PathParam("attachment_url") String attachmentUrl,
            @RestHeader("fiscal_code")String requestFiscalCode

    ) {

        // TODO add GetAttachment logic

        return RestResponse.status(
                Status.OK, null);
    }

    private ErrorResponse buildErrorResponse(AppErrorCodeEnum errorCode, Status status) {
        String message;
        if (status.equals(BAD_REQUEST)) {
            message = "Invalid request";
        } else {
            message = "An error occurred when retrieving the receipt attachment details";
        }
        return ErrorResponse.builder()
                .appErrorCode(errorCode.getErrorCode())
                .httpStatusCode(status.getStatusCode())
                .httpStatusDescription(status.getReasonPhrase())
                .errors(
                        Collections.singletonList(
                                ErrorMessage.builder()
                                        .message(message)
                                        .build())
                ).build();
    }
}
