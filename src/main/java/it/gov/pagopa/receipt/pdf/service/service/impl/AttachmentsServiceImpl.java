package it.gov.pagopa.receipt.pdf.service.service.impl;

import io.quarkus.cache.CacheResult;
import it.gov.pagopa.receipt.pdf.service.client.PDVTokenizerClient;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptBlobClient;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.*;
import it.gov.pagopa.receipt.pdf.service.model.Attachment;
import it.gov.pagopa.receipt.pdf.service.model.AttachmentsDetailsResponse;
import it.gov.pagopa.receipt.pdf.service.model.SearchTokenRequest;
import it.gov.pagopa.receipt.pdf.service.model.SearchTokenResponse;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartPayment;
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
import java.util.Objects;

@ApplicationScoped
public class AttachmentsServiceImpl implements AttachmentsService {

    private final Logger logger = LoggerFactory.getLogger(AttachmentsServiceImpl.class);

    private final ReceiptCosmosClient cosmosClient;

    private final PDVTokenizerClient pdvTokenizerClient;

    private final ReceiptBlobClient receiptBlobClient;

    @Inject
    public AttachmentsServiceImpl(
            ReceiptCosmosClient cosmosClient,
            @RestClient PDVTokenizerClient pdvTokenizerClient,
            ReceiptBlobClient receiptBlobClient
    ) {
        this.cosmosClient = cosmosClient;
        this.pdvTokenizerClient = pdvTokenizerClient;
        this.receiptBlobClient = receiptBlobClient;
    }

    @CacheResult(cacheName = "getAttachmentsDetails")
    @Override
    public AttachmentsDetailsResponse getAttachmentsDetails(
            String thirdPartyId, String requestFiscalCode)
            throws ReceiptNotFoundException, InvalidReceiptException, FiscalCodeNotAuthorizedException {
        Receipt receiptDocument = getReceipt(thirdPartyId);

        SearchTokenResponse searchTokenResponse = getSearchTokenResponse(thirdPartyId, requestFiscalCode);

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
        if (thirdPartyId.contains("_CART_")) {
            var partial = thirdPartyId.split("_CART_");


            String cartId = partial[0];
            String eventId = "PAYER";
            if (partial.length > 1) {
                eventId = partial[1];
            }

            CartForReceipt cartForReceipt = getCartReceipt(cartId);
            SearchTokenResponse searchTokenResponse = getSearchTokenResponse(thirdPartyId, requestFiscalCode);
            if (isFiscalCodeNotAuthorized(searchTokenResponse.getToken(), attachmentUrl, eventId, cartForReceipt)) {
                String errMsg =
                        String.format(
                                "Fiscal code is not authorized to access the receipts with name: %s, for cart with id %s",
                                attachmentUrl, thirdPartyId);
                logger.error(errMsg);
                throw new FiscalCodeNotAuthorizedException(AppErrorCodeEnum.PDFS_706, errMsg);
            }

        } else {

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

    private CartForReceipt getCartReceipt(String cartId) throws ReceiptNotFoundException, InvalidReceiptException {
        CartForReceipt cartForReceipt = cosmosClient.getCartForReceiptDocument(cartId);

        if (cartForReceipt == null) {
            String errMsg = String.format("The retrieved cart with id: %s, is null", cartId);
            logger.error(errMsg);
            throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_707, errMsg);
        }
        if (cartForReceipt.getPayload() == null) {
            String errMsg =
                    String.format("The retrieved receipt with id: %s, has null payload", cartId);
            logger.error(errMsg);
            throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_708, errMsg);
        }

        var allDebtorsAreNull = cartForReceipt.getPayload().getCart().stream()
                .filter(Objects::nonNull)
                .map(CartPayment::getDebtorFiscalCode)
                .noneMatch(Objects::nonNull);

        if (allDebtorsAreNull) {
            String errMsg =
                    String.format(
                            "The retrieved cart with id: %s, has null debtors fiscal code",
                            cartId);
            logger.error(errMsg);
            throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_709, errMsg);
        }

        var allDebtorsAttachAreNull = cartForReceipt.getPayload().getCart().stream()
                .filter(Objects::nonNull)
                .map(CartPayment::getMdAttach)
                .filter(Objects::nonNull)
                .map(ReceiptMetadata::getName)
                .noneMatch(Objects::nonNull);

        if (allDebtorsAttachAreNull) {
            String errMsg =
                    String.format(
                            "The retrieved cart with id: %s, has null attachment info for debtors",
                            cartId);
            logger.error(errMsg);
            throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_710, errMsg);
        }


        if (cartForReceipt.getPayload().getMdAttachPayer() == null) {
            String errMsg =
                    String.format(
                            "The retrieved cart with id: %s, has null attachment info for payer",
                            cartId);
            logger.error(errMsg);
            throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_711, errMsg);
        }
        return cartForReceipt;
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

    /**
     * This method checks if the fiscal code is authorized to access the attachment
     *
     * @param requestFiscalCode the fiscal code to check
     * @param attachmentUrl the attachment url to get
     * @param eventId the event id
     * @param cartForReceipt the cart for receipt
     * @return true if the fiscal code is not authorized, false otherwise
     */
    private boolean isFiscalCodeNotAuthorized(
            String requestFiscalCode, String attachmentUrl, String eventId, CartForReceipt cartForReceipt) {

        // check null cart or payload
        if (cartForReceipt == null || cartForReceipt.getPayload() == null) {
            return true;
        }


        if (eventId.equals("PAYER")) {
            // if third_party_id is a payer then check payer attachment

            boolean isPayerAuthorized = cartForReceipt.getPayload().getMdAttachPayer() != null
                    && attachmentUrl.equals(cartForReceipt.getPayload().getMdAttachPayer().getName())
                    && requestFiscalCode.equals(cartForReceipt.getPayload().getPayerFiscalCode());

            return !isPayerAuthorized;

        } else {
            // else check debtor attachment

            boolean isDebtorAuthorized = cartForReceipt.getPayload().getCart() != null
                    && cartForReceipt.getPayload().getCart().stream()
                    .filter(elem ->
                            elem != null
                                    && elem.getMdAttach() != null
                                    && attachmentUrl.equals(elem.getMdAttach().getName()))
                    .allMatch(elem ->
                            requestFiscalCode.equals(elem.getDebtorFiscalCode())
                                    && eventId.equals(elem.getBizEventId())
                    );

            return !isDebtorAuthorized;

        }


    }

    private boolean isReceiptUnique(Receipt receiptDocument) {
        return receiptDocument.getEventData().getPayerFiscalCode() == null ||
                receiptDocument
                        .getEventData()
                        .getDebtorFiscalCode()
                        .equals(receiptDocument.getEventData().getPayerFiscalCode());
    }
}
