package it.gov.pagopa.receipt.pdf.service.resource;

import io.quarkus.arc.profile.IfBuildProfile;
import it.gov.pagopa.receipt.pdf.service.exception.*;
import it.gov.pagopa.receipt.pdf.service.filters.LoggedAPI;
import it.gov.pagopa.receipt.pdf.service.service.impl.PDFService;
import it.gov.pagopa.receipt.pdf.service.utils.CommonUtils;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
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

@Tag(name = "PDF", description = "PDF operations")
@Path("/pdf")
@LoggedAPI
@IfBuildProfile(anyOf = {"build", "dev", "uat", "prod", "test", "pdf"})
public class PdfResource {
    private final Logger logger = LoggerFactory.getLogger(PdfResource.class);
    private final PDFService pdfService;

    @Inject
    public PdfResource(PDFService pdfService) {
        this.pdfService = pdfService;
    }

    @Operation(
            summary = "Get receipt pdf",
            description = "Retrieve the pdf of the receipt with the provided third party id"
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
    @Path("/{tp_id}")
    @GET
    public RestResponse<byte[]> getReceiptPdf(
            @PathParam(THIRD_PARTY_ID_PARAM) String thirdPartyId,
            @QueryParam(FISCAL_CODE_HEADER) String requestFiscalCode
    )
            throws InvalidFiscalCodeHeaderException, FiscalCodeNotAuthorizedException, ErrorHandlingPdfAttachmentFileException,
            AttachmentNotFoundException, BlobStorageClientException, ReceiptNotFoundException, CartNotFoundException, InvalidReceiptException, InvalidCartException {

        // replace new line and tab from user input to avoid log injection
        thirdPartyId = CommonUtils.sanitize(thirdPartyId);

        logger.info("Received get attachment details for receipt with id {}", thirdPartyId);
        if (requestFiscalCode == null || requestFiscalCode.length() != FISCAL_CODE_LENGTH) {
            throw new InvalidFiscalCodeHeaderException(PDFS_901, PDFS_901.getErrorMessage());
        }
        // replace new line and tab from user input to avoid log injection
        requestFiscalCode = CommonUtils.sanitize(requestFiscalCode);


        File attachment = this.pdfService.getReceiptPdf(thirdPartyId, requestFiscalCode);
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(attachment))) {
            return RestResponse.ResponseBuilder.ok(inputStream.readAllBytes())
                    .header("content-type", "application/pdf")
                    .header("content-disposition", "attachment;")
                    .header(FILENAME_RESPONSE_HEADER, attachment.getName())
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
