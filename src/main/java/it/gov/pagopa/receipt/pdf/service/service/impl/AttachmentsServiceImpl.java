package it.gov.pagopa.receipt.pdf.service.service.impl;

import it.gov.pagopa.receipt.pdf.service.client.ReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.FiscalCodeNotAuthorizedException;
import it.gov.pagopa.receipt.pdf.service.exception.InvalidReceiptException;
import it.gov.pagopa.receipt.pdf.service.exception.PdfServiceException;
import it.gov.pagopa.receipt.pdf.service.model.Attachment;
import it.gov.pagopa.receipt.pdf.service.model.AttachmentDetailsResponse;
import it.gov.pagopa.receipt.pdf.service.model.Receipt;
import it.gov.pagopa.receipt.pdf.service.model.ReceiptMetadata;
import it.gov.pagopa.receipt.pdf.service.service.AttachmentsService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

@ApplicationScoped
public class AttachmentsServiceImpl implements AttachmentsService {

    private final Logger logger = LoggerFactory.getLogger(AttachmentsServiceImpl.class);

    @Inject
    private ReceiptCosmosClient cosmosClient;

    @Override
    public AttachmentDetailsResponse getAttachmentDetails(String thirdPartyId, String requestFiscalCode) throws PdfServiceException {
        Receipt receiptDocument = cosmosClient.getReceiptDocument(thirdPartyId);

        validateReceipt(thirdPartyId, receiptDocument);

        if (isFiscalCodeNotAuthorized(requestFiscalCode, receiptDocument)) {
            String errMsg = String.format("Fiscal code: %s, is not authorized to access the receipts with id: %s",
                    requestFiscalCode, thirdPartyId);
            logger.error(errMsg);
            throw new FiscalCodeNotAuthorizedException(AppErrorCodeEnum.PDFS_700, errMsg);
        }

        if (isReceiptUnique(receiptDocument) || receiptDocument.getEventData().getDebtorFiscalCode().equals(requestFiscalCode)) {
            return buildAttachmentDetails(receiptDocument, receiptDocument.getMdAttach());
        }
        return buildAttachmentDetails(receiptDocument, receiptDocument.getMdAttachPayer());
    }

    private void validateReceipt(String thirdPartyId, Receipt receiptDocument) throws InvalidReceiptException {
        if (receiptDocument == null) {
            String errMsg = String.format("The retrieved receipt with id: %s, is null", thirdPartyId);
            logger.error(errMsg);
            throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_701, errMsg);
        }
        if (receiptDocument.getEventData() == null) {
            String errMsg = String.format("The retrieved receipt with id: %s, has null event data", thirdPartyId);
            logger.error(errMsg);
            throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_702, errMsg);
        }
        if (receiptDocument.getEventData().getPayerFiscalCode() == null
                || receiptDocument.getEventData().getDebtorFiscalCode() == null) {
            String errMsg = String.format("The retrieved receipt with id: %s, has null payer (%s) or debtor (%s) fiscal code",
                    thirdPartyId,
                    receiptDocument.getEventData().getPayerFiscalCode(),
                    receiptDocument.getEventData().getDebtorFiscalCode()
            );
            logger.error(errMsg);
            throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_703, errMsg);
        }
        if (receiptDocument.getMdAttach() == null) {
            String errMsg = String.format("The retrieved receipt with id: %s, has null attachment info for debtor", thirdPartyId);
            logger.error(errMsg);
            throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_704, errMsg);
        }
        if (!isReceiptUnique(receiptDocument) && receiptDocument.getMdAttachPayer() == null) {
            String errMsg = String.format("The retrieved receipt with id: %s, has null attachment info for payer", thirdPartyId);
            logger.error(errMsg);
            throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_705, errMsg);
        }
    }

    private AttachmentDetailsResponse buildAttachmentDetails(Receipt receiptDocument, ReceiptMetadata receiptMetadata) {
        return AttachmentDetailsResponse.builder()
                .attachments(
                        Collections.singletonList(
                                Attachment.builder()
                                        .id(receiptDocument.getId())
                                        .contentType("application/pdf") // TODO manca l'informazione application/zip o application/pdf
                                        .url(receiptMetadata.getUrl())
                                        .name(receiptMetadata.getName())
                                        .build()
                        )).build();
    }

    private boolean isFiscalCodeNotAuthorized(String requestFiscalCode, Receipt receiptDocument) {
        return !receiptDocument.getEventData().getDebtorFiscalCode().equals(requestFiscalCode)
                && !receiptDocument.getEventData().getPayerFiscalCode().equals(requestFiscalCode);
    }

    private boolean isReceiptUnique(Receipt receiptDocument) {
        return receiptDocument.getEventData().getDebtorFiscalCode().equals(receiptDocument.getEventData().getPayerFiscalCode());
    }
}
