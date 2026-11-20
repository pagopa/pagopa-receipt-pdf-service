package it.gov.pagopa.receipt.pdf.service.service.impl;

import it.gov.pagopa.receipt.pdf.service.client.CartReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptBlobClient;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptPdfGeneratorClient;
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
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Objects;

import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.*;
import static it.gov.pagopa.receipt.pdf.service.utils.CommonUtils.sanitize;
import static it.gov.pagopa.receipt.pdf.service.utils.Constants.MDC_THIRD_PARTY_ID;

@ApplicationScoped
public class PdfService {
    public static final int PDF_TEMPLATE_ERROR_CODE = 903;
    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);

    private final ReceiptCosmosClient cosmosClient;
    private final CartReceiptCosmosClient cartReceiptCosmosClient;
    private final TokenizerService tokenizerService;
    private final ReceiptBlobClient receiptBlobClient;
    private final ReceiptPdfGeneratorClient generatorClient;

    @Inject
    public PdfService(
            ReceiptCosmosClient cosmosClient, CartReceiptCosmosClient cartReceiptCosmosClient,
            TokenizerService tokenizerService,
            ReceiptBlobClient receiptBlobClient,
            @RestClient ReceiptPdfGeneratorClient generatorClient
    ) {
        this.cosmosClient = cosmosClient;
        this.cartReceiptCosmosClient = cartReceiptCosmosClient;
        this.tokenizerService = tokenizerService;
        this.receiptBlobClient = receiptBlobClient;
        this.generatorClient = generatorClient;
    }

    /**
     * Retrieve the PDF of a receipt using the given third party id and fiscalCode, only if the fiscal code is authorized to access it.
     * Ownership is checked before regenerating a failed PDF, including the biz event match for cart debtors.
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
        try {
            MDC.put(MDC_THIRD_PARTY_ID, thirdPartyId);
            try {
                return retrieveReceiptPdf(thirdPartyId, requestFiscalCode);
            } catch (InvalidCartException | InvalidReceiptException e) {
                if (e.getErrorCode() != PDFS_716) {
                    throw e;
                }
                validateOwnership(thirdPartyId, requestFiscalCode);
                // If the PDF is not found, we try to regenerate it and retrieve it again
                boolean regenerated = regenerate(thirdPartyId, e);
                if (!regenerated) {
                    // If the regeneration fails, we throw the original exception to be handled by the controller
                    throw e;
                }
                return retrieveReceiptPdf(thirdPartyId, requestFiscalCode);
            }
        } finally {
            MDC.remove(MDC_THIRD_PARTY_ID);
        }
    }

    private ReceiptPdfResponse retrieveReceiptPdf(String thirdPartyId, String requestFiscalCode) throws
            FiscalCodeNotAuthorizedException, BlobStorageClientException, AttachmentNotFoundException,
            ReceiptNotFoundException, CartNotFoundException, InvalidReceiptException, InvalidCartException {
        String attachmentName = CommonUtils.isCart(thirdPartyId)
                ? getCartAttachmentName(thirdPartyId, requestFiscalCode)
                : getReceiptAttachmentName(thirdPartyId, requestFiscalCode);

        if (attachmentName == null || attachmentName.isBlank()) {
            throw new AttachmentNotFoundException(PDFS_716, PDFS_716.getErrorMessage());
        }

        return ReceiptPdfResponse.builder()
                .attachmentName(attachmentName)
                .pdfFile(this.receiptBlobClient.getAttachmentFromBlobStorage(attachmentName))
                .build();
    }

    /**
     * @param thirdPartyId      the ID of the third party
     * @param requestFiscalCode the fiscal code of the requester
     * @throws FiscalCodeNotAuthorizedException if the fiscal code is not authorized to access the receipt
     * @throws ReceiptNotFoundException         if the receipt is not found
     * @throws CartNotFoundException            if the cart is not found
     *                                          <p>
     *                                          This method validates the ownership of the receipt or cart by checking if the fiscal code matches either the payer or debtor fiscal code in the receipt or cart payload. If the fiscal code is not authorized, it throws a FiscalCodeNotAuthorizedException. If the receipt or cart is not found, it throws a ReceiptNotFoundException or CartNotFoundException respectively.
     */
    private void validateOwnership(String thirdPartyId, String requestFiscalCode) throws
            FiscalCodeNotAuthorizedException, ReceiptNotFoundException, CartNotFoundException {
        String fiscalCode = this.tokenizerService.getSearchTokenResponse(requestFiscalCode).getToken();
        boolean authorized;
        if (CommonUtils.isCart(thirdPartyId)) {
            Payload payload = this.cartReceiptCosmosClient
                    .getCartForReceiptDocument(CommonUtils.getPaymentId(thirdPartyId)).getPayload();
            String bizEventId = CommonUtils.getBizEventId(thirdPartyId);
            authorized = payload != null &&
                    (Objects.equals(payload.getPayerFiscalCode(), fiscalCode) ||
                            (payload.getCart() != null && payload.getCart().stream().anyMatch(payment ->
                                    Objects.equals(payment.getDebtorFiscalCode(), fiscalCode) &&
                                            Objects.equals(payment.getBizEventId(), bizEventId))));
        } else {
            Receipt receipt = this.cosmosClient.getReceiptDocument(thirdPartyId);
            authorized = receipt.getEventData() != null &&
                    (Objects.equals(receipt.getEventData().getDebtorFiscalCode(), fiscalCode) ||
                            Objects.equals(receipt.getEventData().getPayerFiscalCode(), fiscalCode));
        }
        if (fiscalCode == null || !authorized) {
            throw new FiscalCodeNotAuthorizedException(PDFS_706, String.format(
                    "Fiscal code is not authorized to access the receipt with id %s", sanitize(thirdPartyId)));
        }
    }

    /**
     * @param thirdPartyId      the ID of the third party
     * @param originalException the original exception that triggered the regeneration
     * @return true if the regeneration was successful, false otherwise
     */
    private boolean regenerate(String thirdPartyId, Exception originalException) {
        try (Response response = CommonUtils.isCart(thirdPartyId)
                ? generatorClient.regenerateCart(CommonUtils.getPaymentId(thirdPartyId))
                : generatorClient.regenerateReceipt(thirdPartyId)) {
            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                logger.error("PDF regeneration failed with HTTP status {}", response.getStatus());
                return false;
            }
            return true;
        } catch (Exception e) {
            originalException.addSuppressed(e);
            logger.error("PDF regeneration failed", e);
            return false;
        }
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

        String fiscalCode = this.tokenizerService.getSearchTokenResponse(requestFiscalCode).getToken();
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

        String fiscalCode = this.tokenizerService.getSearchTokenResponse(requestFiscalCode).getToken();
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
