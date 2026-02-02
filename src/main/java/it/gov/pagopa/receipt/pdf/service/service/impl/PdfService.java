package it.gov.pagopa.receipt.pdf.service.service.impl;

import it.gov.pagopa.receipt.pdf.service.client.CartReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptBlobClient;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.ReceiptStatusType;
import it.gov.pagopa.receipt.pdf.service.exception.*;
import it.gov.pagopa.receipt.pdf.service.model.ReceiptPdfResponse;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartStatusType;
import it.gov.pagopa.receipt.pdf.service.model.cart.Payload;
import it.gov.pagopa.receipt.pdf.service.model.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.service.model.receipt.ReceiptMetadata;
import it.gov.pagopa.receipt.pdf.service.utils.CommonUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Objects;

import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.*;
import static it.gov.pagopa.receipt.pdf.service.utils.CommonUtils.*;

@ApplicationScoped
public class PdfService {
    public static final int PDF_TEMPLATE_ERROR_CODE = 903;

    private final ReceiptCosmosClient cosmosClient;
    private final CartReceiptCosmosClient cartReceiptCosmosClient;
    private final TokenizerService tokenizerService;
    private final ReceiptBlobClient receiptBlobClient;

    @Inject
    public PdfService(
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
    public ReceiptPdfResponse getReceiptPdf(String thirdPartyId, String requestFiscalCode) throws
            FiscalCodeNotAuthorizedException, BlobStorageClientException, AttachmentNotFoundException,
            ReceiptNotFoundException, CartNotFoundException, InvalidReceiptException, InvalidCartException {
        String attachmentName;

        if (CommonUtils.isCart(thirdPartyId)) {
            attachmentName = getCartAttachmentName(thirdPartyId, requestFiscalCode);
        } else {
            attachmentName = getReceiptAttachmentName(thirdPartyId, requestFiscalCode);
        }

        if (attachmentName == null || attachmentName.isEmpty() || attachmentName.isBlank()) {
            throw new AttachmentNotFoundException(PDFS_716, PDFS_716.getErrorMessage());
        }

        return ReceiptPdfResponse.builder()
                .attachmentName(attachmentName)
                .pdfFile(this.receiptBlobClient.getAttachmentFromBlobStorage(attachmentName))
                .build();
    }

    private String getReceiptAttachmentName(
            String thirdPartyId,
            String requestFiscalCode
    ) throws ReceiptNotFoundException, InvalidReceiptException, FiscalCodeNotAuthorizedException, InvalidCartException, AttachmentNotFoundException {
        String attachmentName;
        Receipt receipt = this.cosmosClient.getReceiptDocument(thirdPartyId);

        if (ReceiptStatusType.PDF_WAITING_TO_BE_GENERATED.contains(receipt.getStatus())) {
            throw new AttachmentNotFoundException(PDFS_714, PDFS_714.getErrorMessage());
        }
        if (ReceiptStatusType.PDF_FAILED_TO_BE_GENERATED.contains(receipt.getStatus())) {
            if (isSingleReceiptInCriticalFailure(receipt)) {
                throw new InvalidReceiptException(PDFS_716, PDFS_716.getErrorMessage());
            }
            throw new AttachmentNotFoundException(PDFS_715, PDFS_715.getErrorMessage());
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

    private String getCartAttachmentName(
            String thirdPartyId,
            String requestFiscalCode
    ) throws CartNotFoundException, InvalidCartException, FiscalCodeNotAuthorizedException, AttachmentNotFoundException {
        String attachmentName;
        String cartId = CommonUtils.getPaymentId(thirdPartyId);

        CartForReceipt cart = this.cartReceiptCosmosClient.getCartForReceiptDocument(cartId);

        if (CartStatusType.PDF_WAITING_TO_BE_GENERATED.contains(cart.getStatus())) {
            throw new AttachmentNotFoundException(PDFS_714, PDFS_714.getErrorMessage());
        }
        if (CartStatusType.PDF_FAILED_TO_BE_GENERATED.contains(cart.getStatus())) {
            if (isCartInCriticalFailure(cart)) {
                throw new InvalidCartException(PDFS_716, PDFS_716.getErrorMessage());
            }
            throw new AttachmentNotFoundException(PDFS_715, PDFS_715.getErrorMessage());
        }

        String fiscalCode = this.tokenizerService.getSearchTokenResponse(thirdPartyId, requestFiscalCode).getToken();
        Payload cartPayload = cart.getPayload();
        if (Objects.equals(cartPayload.getPayerFiscalCode(), fiscalCode)) {
            if (cartPayload.getMdAttachPayer() == null) {
                throw new InvalidCartException(PDFS_716, PDFS_716.getErrorMessage());
            }
            attachmentName = cartPayload.getMdAttachPayer().getName();
        } else {
            String bizEventId = CommonUtils.getBizEventId(thirdPartyId);
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

    private boolean isCartInCriticalFailure(CartForReceipt cart) {
        return cart.getPayload() != null &&
                ((cart.getPayload().getReasonErrPayer() != null && cart.getPayload().getReasonErrPayer().getCode() == PDF_TEMPLATE_ERROR_CODE) ||
                        (cart.getPayload().getCart() != null && cart.getPayload().getCart().stream().anyMatch(
                                c -> c.getReasonErrDebtor() != null && c.getReasonErrDebtor().getCode() == PDF_TEMPLATE_ERROR_CODE)
                        ));
    }

    private boolean isSingleReceiptInCriticalFailure(Receipt receipt) {
        return (receipt.getReasonErr() != null && receipt.getReasonErr().getCode() == PDF_TEMPLATE_ERROR_CODE) ||
                (receipt.getReasonErrPayer() != null && receipt.getReasonErrPayer().getCode() == PDF_TEMPLATE_ERROR_CODE);
    }
}
