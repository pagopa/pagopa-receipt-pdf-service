package it.gov.pagopa.receipt.pdf.service.enumeration;

import lombok.Getter;

import java.util.Set;

import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorAPICategory.ATTACHMENTS;
import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorAPICategory.HELPDESK;
import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorAPICategory.PDF;

/**
 * Enumeration for application error codes and messages
 */
@Getter
public enum AppErrorCodeEnum {
    PDFS_100("PDFS_100", Set.of(HELPDESK), "Biz Event not found with the provided organization and NAV"),

    PDFS_400("PDFS_400", Set.of(ATTACHMENTS, HELPDESK, PDF), "An unexpected error has occurred, see logs for more info"),

    PDFS_500("PDFS_500", Set.of(ATTACHMENTS, HELPDESK, PDF), "I/O error when reading the temporary file with the receipt PDF content retrieved from Blob Storage"),

    PDFS_601("PDFS_601", Set.of(ATTACHMENTS, HELPDESK, PDF), "I/O error when downloading the PDF receipt from Blob Storage"),
    PDFS_602("PDFS_602", Set.of(ATTACHMENTS, HELPDESK, PDF), "Error when downloading the PDF receipt from Blob Storage, the requested attachment was not found"),
    PDFS_603("PDFS_603", Set.of(ATTACHMENTS, HELPDESK, PDF), "Some error occurred when downloading the PDF receipt from Blob Storage"),

    PDFS_700("PDFS_700", Set.of(ATTACHMENTS, PDF), "Fiscal code not authorized to access the requested receipts details"),
    PDFS_701("PDFS_701", Set.of(ATTACHMENTS), "The retrieved receipt is null"),
    PDFS_702("PDFS_702", Set.of(ATTACHMENTS), "The retrieved receipt has null event data"),
    PDFS_703("PDFS_703", Set.of(ATTACHMENTS), "The retrieved receipt has null debtor fiscal code"),
    PDFS_704("PDFS_704", Set.of(ATTACHMENTS), "The retrieved receipt has null debtor attachment info"),
    PDFS_705("PDFS_705", Set.of(ATTACHMENTS), "The retrieved receipt has null payer attachment info"),
    PDFS_706("PDFS_706", Set.of(ATTACHMENTS, PDF), "Fiscal code not authorized to access the requested receipt document"),

    PDFS_707("PDFS_707", Set.of(ATTACHMENTS), "The retrieved cart is null"),
    PDFS_708("PDFS_708", Set.of(ATTACHMENTS), "The retrieved cart has null payload"),
    PDFS_709("PDFS_709", Set.of(ATTACHMENTS), "The retrieved cart has null debtors fiscal code"),
    PDFS_710("PDFS_710", Set.of(ATTACHMENTS), "The retrieved cart has null attachment info"),
    PDFS_711("PDFS_711", Set.of(ATTACHMENTS), "The retrieved cart has null debtor attachment info"),
    PDFS_712("PDFS_712", Set.of(ATTACHMENTS), "The retrieved cart has null payer attachment info"),
    PDFS_713("PDFS_713", Set.of(ATTACHMENTS), "The retrieved cart has null payer message data"),

    PDFS_714("PDFS_714", Set.of(PDF), "The PDF has not been generated yet."),
    PDFS_715("PDFS_715", Set.of(PDF), "The PDF generation failed. A retry is possible."),
    PDFS_716("PDFS_716", Set.of(PDF), "The PDF generation failed. Manual review is required."),

    PDFS_800("PDFS_800", Set.of(ATTACHMENTS, HELPDESK, PDF), "Receipt not found with the provided third party id"),
    PDFS_801("PDFS_801", Set.of(ATTACHMENTS, HELPDESK, PDF), "Cart not found with the provided third party id"),

    PDFS_901("PDFS_901", Set.of(ATTACHMENTS, PDF), "Invalid fiscal code header, null or length not equal to 16");

    private final String errorCode;
    private final Set<AppErrorAPICategory> category;
    private final String errorMessage;

    AppErrorCodeEnum(String errorCode, Set<AppErrorAPICategory> category, String errorMessage) {
        this.errorCode = errorCode;
        this.category = category;
        this.errorMessage = errorMessage;
    }
}
