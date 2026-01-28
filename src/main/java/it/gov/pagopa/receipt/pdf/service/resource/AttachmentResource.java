package it.gov.pagopa.receipt.pdf.service.resource;

import io.quarkus.arc.profile.IfBuildProfile;
import it.gov.pagopa.receipt.pdf.service.exception.*;
import it.gov.pagopa.receipt.pdf.service.filters.LoggedAPI;
import it.gov.pagopa.receipt.pdf.service.model.AttachmentsDetailsResponse;
import it.gov.pagopa.receipt.pdf.service.service.AttachmentsService;
import it.gov.pagopa.receipt.pdf.service.utils.CommonUtils;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.PDFS_500;
import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.PDFS_901;
import static it.gov.pagopa.receipt.pdf.service.utils.Constants.*;

/**
 * Resource class that expose the API to retrieve the attachments
 */
@Tag(name = "Attachments", description = "Attachments operations")
@Path("/messages")
@LoggedAPI
@IfBuildProfile(anyOf = {"build", "dev", "uat", "prod", "test", "attachments"})
public class AttachmentResource {
    private final Logger logger = LoggerFactory.getLogger(AttachmentResource.class);
    private final AttachmentsService attachmentsService;

    @Inject
    public AttachmentResource(AttachmentsService attachmentsService) {
        this.attachmentsService = attachmentsService;
    }

    @Operation(
            summary = "Get attachment details",
            description = "Retrieve the attachment details linked to the provided third party data id"
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
                                    schema = @Schema(implementation = AttachmentsDetailsResponse.class)
                            )
                    )
            }
    )
    @Path("/{tp_id}")
    @GET
    public RestResponse<AttachmentsDetailsResponse> getAttachmentDetails(
            @PathParam(THIRD_PARTY_ID_PARAM) String thirdPartyId,
            @QueryParam(FISCAL_CODE_HEADER) String requestFiscalCode)
        // @RestHeader(FISCAL_CODE_HEADER) String requestFiscalCode)
            throws InvalidFiscalCodeHeaderException, ReceiptNotFoundException, InvalidReceiptException,
            FiscalCodeNotAuthorizedException, InvalidCartException, CartNotFoundException {

        // replace new line and tab from user input to avoid log injection
        thirdPartyId = CommonUtils.sanitize(thirdPartyId);

        logger.info("Received get attachment details for receipt with id {}", thirdPartyId);
        if (requestFiscalCode == null || requestFiscalCode.length() != FISCAL_CODE_LENGTH) {
            throw new InvalidFiscalCodeHeaderException(PDFS_901, PDFS_901.getErrorMessage());
        }
        // replace new line and tab from user input to avoid log injection
        requestFiscalCode = CommonUtils.sanitize(requestFiscalCode);

        AttachmentsDetailsResponse attachmentDetails =
                attachmentsService.getAttachmentsDetails(thirdPartyId, requestFiscalCode);

        return RestResponse.status(Status.OK, attachmentDetails);
    }

    @Operation(
            summary = "Get attachment",
            description =
                    "Retrieve the attachment linked to the provided third party data id from the provided attachment url"
    )
    @APIResponses(
            value = {
                    @APIResponse(ref = "#/components/responses/InternalServerError"),
                    @APIResponse(ref = "#/components/responses/AppException400"),
                    @APIResponse(ref = "#/components/responses/AppException404"),
                    @APIResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(mediaType = "application/pdf")
                    )
            }
    )
    @Path("/{tp_id}/{attachment_url}")
    @GET
    public RestResponse<byte[]> getAttachment(
            @PathParam(THIRD_PARTY_ID_PARAM) String thirdPartyId,
            @PathParam("attachment_url") String attachmentUrl,
            @QueryParam(FISCAL_CODE_HEADER) String requestFiscalCode)
            throws InvalidFiscalCodeHeaderException, BlobStorageClientException, ReceiptNotFoundException,
            InvalidReceiptException, FiscalCodeNotAuthorizedException, AttachmentNotFoundException, ErrorHandlingPdfAttachmentFileException, InvalidCartException, CartNotFoundException {

        // replace new line and tab from user input to avoid log injection
        thirdPartyId = CommonUtils.sanitize(thirdPartyId);
        attachmentUrl = CommonUtils.sanitize(attachmentUrl);

        logger.info(
                "Received get attachment with name {} for receipt with id {}",
                attachmentUrl,
                thirdPartyId);
        if (requestFiscalCode == null || requestFiscalCode.length() != FISCAL_CODE_LENGTH) {
            throw new InvalidFiscalCodeHeaderException(PDFS_901, PDFS_901.getErrorMessage());
        }
        // replace new line and tab from user input to avoid log injection
        requestFiscalCode = CommonUtils.sanitize(requestFiscalCode);


        File attachment = attachmentsService.getAttachment(thirdPartyId, requestFiscalCode, attachmentUrl);
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(attachment))) {
            return RestResponse.ResponseBuilder.ok(inputStream.readAllBytes())
                    .header("content-type", "application/pdf")
                    .header("content-disposition", "attachment;")
                    .build();
        } catch (IOException e) {
            logger.error("Error handling the stream generated from pdf attachment");
            throw new ErrorHandlingPdfAttachmentFileException(PDFS_500, PDFS_500.getErrorMessage(), e);
        } finally {
            if (attachment != null && attachment.exists()) {
                CommonUtils.clearTempDirectory(attachment.toPath().getParent(), logger);
            }
        }
    }
}
