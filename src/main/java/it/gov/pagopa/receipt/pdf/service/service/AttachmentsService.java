package it.gov.pagopa.receipt.pdf.service.service;

import it.gov.pagopa.receipt.pdf.service.exception.PdfServiceException;
import it.gov.pagopa.receipt.pdf.service.model.AttachmentDetailsResponse;

public interface AttachmentsService {

    AttachmentDetailsResponse getAttachmentDetails(String thirdPartyId, String requestFiscalCode) throws PdfServiceException;
}
