package it.gov.pagopa.receipt.pdf.service.service;

import it.gov.pagopa.receipt.pdf.service.exception.PdfServiceException;
import it.gov.pagopa.receipt.pdf.service.model.AttachmentDetailsResponse;

import java.io.File;

public interface AttachmentsService {

    AttachmentDetailsResponse getAttachmentDetails(String thirdPartyId, String requestFiscalCode) throws PdfServiceException;
    File getAttachment(String thirdPartyId, String requestFiscalCode, String attachmentUrl) throws PdfServiceException;
}
