package it.gov.pagopa.receipt.pdf.service.exception.mapper;

import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.AttachmentNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.MissingFiscalCodeHeaderException;
import it.gov.pagopa.receipt.pdf.service.exception.PdfServiceException;
import it.gov.pagopa.receipt.pdf.service.exception.ReceiptNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.ErrorMessage;
import it.gov.pagopa.receipt.pdf.service.model.ErrorResponse;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.PDFS_400;
import static jakarta.ws.rs.core.Response.Status.*;

public class ExceptionMapper {

    private final Logger logger = LoggerFactory.getLogger(ExceptionMapper.class);

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> mapMissingFiscalCodeHeaderException(MissingFiscalCodeHeaderException missingFiscalCodeHeaderException) {
        Response.Status status = BAD_REQUEST;
        String message = "The provided fiscal code is invalid.";
        logger.error(message, missingFiscalCodeHeaderException);
        return RestResponse.status(status, buildErrorResponse(missingFiscalCodeHeaderException.getErrorCode(), status, message));
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
        Response.Status status = INTERNAL_SERVER_ERROR;
        String message = "An unexpected error has occurred. Please contact support.";
        logger.error(message, pdfServiceException);
        return RestResponse.status(status, buildErrorResponse(pdfServiceException.getErrorCode(), status, message));
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> mapGenericException(Throwable t) {
        Response.Status status = INTERNAL_SERVER_ERROR;
        String message = "An unexpected error has occurred. Please contact support.";
        logger.error(message, t);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        String message1 = String.format("Error message %s. Stacktrace: %s",t.getMessage(), sw.toString());
        return RestResponse.status(status, buildErrorResponse(PDFS_400, status, message1));
    }

    private ErrorResponse buildErrorResponse(AppErrorCodeEnum errorCode, Response.Status status, String message) {
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
