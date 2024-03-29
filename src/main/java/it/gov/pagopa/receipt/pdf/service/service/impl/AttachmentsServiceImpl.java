package it.gov.pagopa.receipt.pdf.service.service.impl;

import it.gov.pagopa.receipt.pdf.service.client.PDVTokenizerClient;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptBlobClient;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.*;
import it.gov.pagopa.receipt.pdf.service.model.Attachment;
import it.gov.pagopa.receipt.pdf.service.model.AttachmentsDetailsResponse;
import it.gov.pagopa.receipt.pdf.service.model.SearchTokenRequest;
import it.gov.pagopa.receipt.pdf.service.model.SearchTokenResponse;
import it.gov.pagopa.receipt.pdf.service.model.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.service.model.receipt.ReceiptMetadata;
import it.gov.pagopa.receipt.pdf.service.service.AttachmentsService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;

@ApplicationScoped
public class AttachmentsServiceImpl implements AttachmentsService {

    private final Logger logger = LoggerFactory.getLogger(AttachmentsServiceImpl.class);

    @Inject
    private ReceiptCosmosClient cosmosClient;

    @Inject
    @RestClient
    private PDVTokenizerClient pdvTokenizerClient;

    @Inject
    private ReceiptBlobClient receiptBlobClient;

    @Override
    public AttachmentsDetailsResponse getAttachmentsDetails(
            String thirdPartyId, String requestFiscalCode)
            throws ReceiptNotFoundException, InvalidReceiptException, FiscalCodeNotAuthorizedException {
        Receipt receiptDocument = getReceipt(thirdPartyId);

      SearchTokenResponse searchTokenResponse = getSearchTokenResponse(thirdPartyId, requestFiscalCode);;

        String token = searchTokenResponse.getToken();

        if (isFiscalCodeNotAuthorized(token, receiptDocument)) {
            String errMsg =
                    String.format(
                            "Fiscal code is not authorized to access the receipts with id: %s",
                            thirdPartyId);
            logger.error(errMsg);
            throw new FiscalCodeNotAuthorizedException(AppErrorCodeEnum.PDFS_700, errMsg);
        }

        if (receiptDocument.getEventData().getDebtorFiscalCode().equals(token)) {
            return buildAttachmentDetails(receiptDocument, receiptDocument.getMdAttach());
        }
        return buildAttachmentDetails(receiptDocument, receiptDocument.getMdAttachPayer());
    }

    @Override
    public File getAttachment(String thirdPartyId, String requestFiscalCode, String attachmentUrl)
            throws ReceiptNotFoundException, InvalidReceiptException, FiscalCodeNotAuthorizedException,
            BlobStorageClientException, AttachmentNotFoundException {
        Receipt receiptDocument = getReceipt(thirdPartyId);

      SearchTokenResponse searchTokenResponse = getSearchTokenResponse(thirdPartyId, requestFiscalCode);

      if (isFiscalCodeNotAuthorized(searchTokenResponse.getToken(), attachmentUrl, receiptDocument)) {
            String errMsg =
                    String.format(
                            "Fiscal code is not authorized to access the receipts with name: %s, for receipt with id %s",
                            attachmentUrl, thirdPartyId);
            logger.error(errMsg);
            throw new FiscalCodeNotAuthorizedException(AppErrorCodeEnum.PDFS_706, errMsg);
        }

        return receiptBlobClient.getAttachmentFromBlobStorage(attachmentUrl);
    }

  private SearchTokenResponse getSearchTokenResponse(String thirdPartyId, String requestFiscalCode)
      throws FiscalCodeNotAuthorizedException {
    SearchTokenResponse searchTokenResponse;
    try {
      searchTokenResponse =
          pdvTokenizerClient.searchToken(new SearchTokenRequest(requestFiscalCode));
      if (searchTokenResponse == null || searchTokenResponse.getToken() == null) {
        throw new FiscalCodeNotAuthorizedException(AppErrorCodeEnum.PDFS_700, "Missing token");
      }
    } catch (Exception e) {
      String errMsg =
          String.format(
              "Could not recover fiscal code token for authentication in the request with id: %s",
              thirdPartyId);
      logger.error(errMsg, e);
      throw new FiscalCodeNotAuthorizedException(AppErrorCodeEnum.PDFS_700, errMsg);
    }
    return searchTokenResponse;
  }

  private Receipt getReceipt(String thirdPartyId)
            throws ReceiptNotFoundException, InvalidReceiptException {
        Receipt receiptDocument = cosmosClient.getReceiptDocument(thirdPartyId);

        if (receiptDocument == null) {
            String errMsg = String.format("The retrieved receipt with id: %s, is null", thirdPartyId);
            logger.error(errMsg);
            throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_701, errMsg);
        }
        if (receiptDocument.getEventData() == null) {
            String errMsg =
                    String.format("The retrieved receipt with id: %s, has null event data", thirdPartyId);
            logger.error(errMsg);
            throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_702, errMsg);
        }
        if (receiptDocument.getEventData().getDebtorFiscalCode() == null) {
            String errMsg =
                    String.format(
                            "The retrieved receipt with id: %s, has null debtor fiscal code",
                            thirdPartyId);
            logger.error(errMsg);
            throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_703, errMsg);
        }
        if (receiptDocument.getMdAttach() == null && (!"ANONIMO".equals(receiptDocument.getEventData().getDebtorFiscalCode()))) {
            String errMsg =
                    String.format(
                            "The retrieved receipt with id: %s, has null attachment info for debtor",
                            thirdPartyId);
            logger.error(errMsg);
            throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_704, errMsg);
        }
        if (!isReceiptUnique(receiptDocument) && receiptDocument.getMdAttachPayer() == null) {
            String errMsg =
                    String.format(
                            "The retrieved receipt with id: %s, has different debtor and payer fiscal codes but has null attachment info for payer",
                            thirdPartyId);
            logger.error(errMsg);
            throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_705, errMsg);
        }
        return receiptDocument;
    }

    private AttachmentsDetailsResponse buildAttachmentDetails(
            Receipt receiptDocument, ReceiptMetadata receiptMetadata) {
        return AttachmentsDetailsResponse.builder()
                .attachments(
                        Collections.singletonList(
                                Attachment.builder()
                                        .id(receiptDocument.getId())
                                        .contentType("application/pdf")
                                        .url(receiptMetadata.getName())
                                        .name(receiptMetadata.getName())
                                        .build()))
                .build();
    }

    private boolean isFiscalCodeNotAuthorized(String requestFiscalCode, Receipt receiptDocument) {
        return !requestFiscalCode.equals(receiptDocument.getEventData().getDebtorFiscalCode())
                && !requestFiscalCode.equals(receiptDocument.getEventData().getPayerFiscalCode());
    }

    private boolean isFiscalCodeNotAuthorized(
            String requestFiscalCode, String attachmentUrl, Receipt receiptDocument) {
        String debtorFiscalCode = receiptDocument.getEventData().getDebtorFiscalCode();
        String payerFiscalCode = receiptDocument.getEventData().getPayerFiscalCode();
        String debtorFileName = !"ANONIMO".equals(receiptDocument.getEventData().getDebtorFiscalCode()) ?
            receiptDocument.getMdAttach().getName() : "";

        if (requestFiscalCode.equals(debtorFiscalCode) && debtorFileName.equals(attachmentUrl)) {
            return false;
        }
        if (isReceiptUnique(receiptDocument) || !requestFiscalCode.equals(payerFiscalCode)) {
            return true;
        }
        return !receiptDocument.getMdAttachPayer().getName().equals(attachmentUrl);
    }

    private boolean isReceiptUnique(Receipt receiptDocument) {
        return receiptDocument.getEventData().getPayerFiscalCode() == null ||
                receiptDocument
                        .getEventData()
                        .getDebtorFiscalCode()
                        .equals(receiptDocument.getEventData().getPayerFiscalCode());
    }
}
