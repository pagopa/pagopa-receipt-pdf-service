package it.gov.pagopa.receipt.pdf.service.service.impl;

import it.gov.pagopa.receipt.pdf.service.client.CartReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptBlobClient;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.ReceiptStatusType;
import it.gov.pagopa.receipt.pdf.service.exception.*;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartStatusType;
import it.gov.pagopa.receipt.pdf.service.model.cart.Payload;
import it.gov.pagopa.receipt.pdf.service.model.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.service.model.receipt.ReceiptMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.File;
import java.util.Objects;

import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.*;
import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.PDFS_716;
import static it.gov.pagopa.receipt.pdf.service.utils.CommonUtils.sanitize;
import static it.gov.pagopa.receipt.pdf.service.utils.Constants.CART;

@ApplicationScoped
public class PDFService {
    public static final int PDF_TEMPLATE_ERROR_CODE = 903;

    private final ReceiptCosmosClient cosmosClient;
    private final CartReceiptCosmosClient cartReceiptCosmosClient;
    private final TokenizerService tokenizerService;
    private final ReceiptBlobClient receiptBlobClient;

    @Inject
    public PDFService(
            ReceiptCosmosClient cosmosClient, CartReceiptCosmosClient cartReceiptCosmosClient,
            TokenizerService tokenizerService,
            ReceiptBlobClient receiptBlobClient
    ) {
        this.cosmosClient = cosmosClient;
        this.cartReceiptCosmosClient = cartReceiptCosmosClient;
        this.tokenizerService = tokenizerService;
        this.receiptBlobClient = receiptBlobClient;
    }

    /**
     * Retrieve the PDF of a receipt using the given third party id and fiscalCode, only if the fiscal code is authorized to access it
     *
     * @param thirdPartyId      the id of the biz event for single receipt, the id of the transaction concatenated with the biz event for cart receipts
     * @param requestFiscalCode the fiscal code of the user that request the receipt
     * @return the details of the requested attachments
     * @throws AttachmentNotFoundException      thrown if the requested attachment was not found
     * @throws FiscalCodeNotAuthorizedException thrown if the fiscal code is not authorized to access the requested receipt
     * @throws BlobStorageClientException       thrown for error when retrieving the attachment from the Blob Storage
     * @throws ReceiptNotFoundException         thrown if the single receipt is not found
     * @throws CartNotFoundException            thrown if the cart receipt is not found
     * @throws InvalidReceiptException          thrown if the single receipt is in invalid state
     * @throws InvalidCartException             thrown if the cart receipt is in invalid state
     */
    public File getReceiptPdf(String thirdPartyId, String requestFiscalCode) throws
            FiscalCodeNotAuthorizedException, BlobStorageClientException, AttachmentNotFoundException,
            ReceiptNotFoundException, CartNotFoundException, InvalidReceiptException, InvalidCartException {
        String attachmentName;

        if (thirdPartyId.contains(CART)) {
            attachmentName = getCartAttachmentName(thirdPartyId, requestFiscalCode);
        } else {
            attachmentName = getReceiptAttachmentName(thirdPartyId, requestFiscalCode);
        }

        if (attachmentName == null || attachmentName.isEmpty() || attachmentName.isBlank()) {
            throw new AttachmentNotFoundException(PDFS_716, PDFS_716.getErrorMessage());
        }

        return receiptBlobClient.getAttachmentFromBlobStorage(attachmentName);
    }

    private String getReceiptAttachmentName(String thirdPartyId, String requestFiscalCode) throws ReceiptNotFoundException, InvalidReceiptException, FiscalCodeNotAuthorizedException, InvalidCartException {
        String attachmentName;
        Receipt receipt = cosmosClient.getReceiptDocument(thirdPartyId);

        if (ReceiptStatusType.pdfWaitingToBeGenerated().contains(receipt.getStatus())) {
            throw new InvalidReceiptException(PDFS_714, PDFS_714.getErrorMessage());
        }
        if (ReceiptStatusType.pdfFailedToBeGenerated().contains(receipt.getStatus())) {
            if (isSingleReceiptInCriticalFailure(receipt)) {
                throw new InvalidReceiptException(PDFS_716, PDFS_716.getErrorMessage());
            }
            throw new InvalidReceiptException(PDFS_715, PDFS_715.getErrorMessage());
        }

        String fiscalCode = this.tokenizerService.getSearchTokenResponse(thirdPartyId, requestFiscalCode).getToken();
        if (Objects.equals(receipt.getEventData().getDebtorFiscalCode(), fiscalCode)) {
            if (receipt.getMdAttach() == null) {
                throw new InvalidCartException(PDFS_716, PDFS_716.getErrorMessage());
            }
            attachmentName = receipt.getMdAttach().getName();
        } else if (Objects.equals(receipt.getEventData().getPayerFiscalCode(), fiscalCode)) {
            if (receipt.getMdAttachPayer() == null) {
                throw new InvalidCartException(PDFS_716, PDFS_716.getErrorMessage());
            }
            attachmentName = receipt.getMdAttachPayer().getName();
        } else {
            throw new FiscalCodeNotAuthorizedException(PDFS_706, String.format(
                    "Fiscal code is not authorized to access the receipt with id %s", sanitize(thirdPartyId)));
        }
        return attachmentName;
    }

    private String getCartAttachmentName(String thirdPartyId, String requestFiscalCode) throws CartNotFoundException, InvalidCartException, FiscalCodeNotAuthorizedException {
        String attachmentName;
        String[] splitId = thirdPartyId.split(CART);
        String cartId = splitId[0];
        String bizEventId = splitId.length > 1 ? splitId[1] : "";

        CartForReceipt cart = cartReceiptCosmosClient.getCartForReceiptDocument(cartId);

        if (CartStatusType.pdfWaitingToBeGenerated().contains(cart.getStatus())) {
            throw new InvalidCartException(PDFS_714, PDFS_714.getErrorMessage());
        }
        if (CartStatusType.pdfFailedToBeGenerated().contains(cart.getStatus())) {
            if (isCartInCriticalFailure(cart)) {
                throw new InvalidCartException(PDFS_716, PDFS_716.getErrorMessage());
            }
            throw new InvalidCartException(PDFS_715, PDFS_715.getErrorMessage());
        }

        String fiscalCode = this.tokenizerService.getSearchTokenResponse(thirdPartyId, requestFiscalCode).getToken();
        Payload cartPayload = cart.getPayload();
        if (Objects.equals(cartPayload.getPayerFiscalCode(), fiscalCode)) {
            if (cartPayload.getMdAttachPayer() == null) {
                throw new InvalidCartException(PDFS_716, PDFS_716.getErrorMessage());
            }
            attachmentName = cartPayload.getMdAttachPayer().getName();
        } else {
            ReceiptMetadata mdAttach = cartPayload.getCart().stream()
                    .filter(md ->
                            Objects.equals(md.getDebtorFiscalCode(), fiscalCode) &&
                                    Objects.equals(md.getBizEventId(), bizEventId)
                    )
                    .findFirst().orElseThrow(() -> new FiscalCodeNotAuthorizedException(
                            PDFS_706,
                            String.format("Fiscal code is not authorized to access the receipt with biz event id %s", bizEventId)
                    ))
                    .getMdAttach();
            if (mdAttach == null) {
                throw new InvalidCartException(PDFS_716, PDFS_716.getErrorMessage());
            }

            attachmentName = mdAttach.getName();
        }
        return attachmentName;
    }

    private static boolean isCartInCriticalFailure(CartForReceipt cart) {
        return cart.getPayload() == null ||
                cart.getPayload().getCart() == null ||
                (cart.getPayload().getReasonErrPayer() == null && cart.getPayload().getCart().stream().allMatch(c -> c.getReasonErrDebtor() == null) ||
                        (cart.getPayload().getReasonErrPayer() != null && cart.getPayload().getReasonErrPayer().getCode() == PDF_TEMPLATE_ERROR_CODE) ||
                        (cart.getPayload().getCart().stream().anyMatch(
                                c -> c.getReasonErrDebtor() != null && c.getReasonErrDebtor().getCode() == PDF_TEMPLATE_ERROR_CODE)));
    }

    private static boolean isSingleReceiptInCriticalFailure(Receipt receipt) {
        return (receipt.getReasonErr() == null && receipt.getReasonErrPayer() == null) ||
                (receipt.getReasonErr() != null && receipt.getReasonErr().getCode() == PDF_TEMPLATE_ERROR_CODE) ||
                (receipt.getReasonErrPayer() != null && receipt.getReasonErrPayer().getCode() == PDF_TEMPLATE_ERROR_CODE);
    }
}
