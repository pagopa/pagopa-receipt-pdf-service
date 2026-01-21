package it.gov.pagopa.receipt.pdf.service.exception;

import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;

public class BizEventNotFoundException extends PdfServiceException {
    public BizEventNotFoundException(AppErrorCodeEnum errorCode, String message) {
        super(errorCode, message);
    }

    public BizEventNotFoundException(AppErrorCodeEnum errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
