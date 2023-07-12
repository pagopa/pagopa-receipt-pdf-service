package it.gov.pagopa.receipt.pdf.service.enumeration;

import lombok.Getter;

/**
 * Enumeration for application error codes and messages
 */
@Getter
public enum AppErrorCodeEnum {

    PDFS_700("PDFS_700", "Fiscal code not authorized to access the requested receipts"),
    PDFS_701("PDFS_701", "The retrieved receipt is null"),
    PDFS_702("PDFS_702", "The retrieved receipt has null event data"),
    PDFS_703("PDFS_703", "The retrieved receipt has null payer/debtor fiscal codes"),
    PDFS_704("PDFS_704", "The retrieved receipt has null debtor attachment info"),
    PDFS_705("PDFS_705", "The retrieved receipt has null payer attachment info"),

    PDFS_800("PDFS_800", "Receipt not found with the provided third party id"),

    PDFS_901("PDFS_901", "Invalid fiscal code header");

    private final String errorCode;
    private final String errorMessage;

    AppErrorCodeEnum(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
