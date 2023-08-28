package it.gov.pagopa.receipt.pdf.service.exception.mapper;

import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.AttachmentNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.InvalidFiscalCodeHeaderException;
import it.gov.pagopa.receipt.pdf.service.exception.PdfServiceException;
import it.gov.pagopa.receipt.pdf.service.exception.ReceiptNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.ErrorResponse;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.PDFS_400;
import static jakarta.ws.rs.core.Response.Status.*;

public class ExceptionMapper {

    private final Logger logger = LoggerFactory.getLogger(ExceptionMapper.class);

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> mapMissingFiscalCodeHeaderException(InvalidFiscalCodeHeaderException invalidFiscalCodeHeaderException) {
        Response.Status status = BAD_REQUEST;
        String message = "The provided fiscal code is invalid.";
        logger.error(message, invalidFiscalCodeHeaderException);
        return RestResponse.status(status, buildErrorResponse(invalidFiscalCodeHeaderException.getErrorCode(), status, message));
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> mapReceiptNotFoundException(ReceiptNotFoundException receiptNotFoundException) {
        Response.Status status = NOT_FOUND;
        String message = String.format("Receipt with the provided third party id: %s not found", receiptNotFoundException.getReceiptId());
        logger.error(message, receiptNotFoundException);
        return RestResponse.status(status, buildErrorResponse(receiptNotFoundException.getErrorCode(), status, message));
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> mapAttachmentNotFoundException(AttachmentNotFoundException attachmentNotFoundException) {
        Response.Status status = NOT_FOUND;
        String message = String.format("Attachment with the provided name: %s not found", attachmentNotFoundException.getAttachmentName());
        logger.error(message, attachmentNotFoundException);
        return RestResponse.status(status, buildErrorResponse(attachmentNotFoundException.getErrorCode(), status, message));
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> mapPdfServiceException(PdfServiceException pdfServiceException) {
        AppErrorCodeEnum errorCode = pdfServiceException.getErrorCode();
        String message = String.format("Error during request elaboration, id: %s. Please contact support.", errorCode);
        return getInternalServerErrorResponse(pdfServiceException, errorCode, message);
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> mapGenericException(Exception exception) {
        AppErrorCodeEnum errorCode = PDFS_400;
        String message = String.format("An unexpected error has occurred, id: %s. Please contact support.", errorCode);
        return getInternalServerErrorResponse(exception, errorCode, message);
    }

    private RestResponse<ErrorResponse> getInternalServerErrorResponse(Throwable t, AppErrorCodeEnum errorCode, String message) {
        Response.Status status = INTERNAL_SERVER_ERROR;
        logger.error(message, t);
        return RestResponse.status(status, buildErrorResponse(errorCode, status, message));
    }

    private ErrorResponse buildErrorResponse(AppErrorCodeEnum errorCode, Response.Status status, String message) {
        return ErrorResponse.builder()
                .title(status.getReasonPhrase())
                .status(status.getStatusCode())
                .detail(message)
                .instance(errorCode.getErrorCode())
                .build();
    }
}
