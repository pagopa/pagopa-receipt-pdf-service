package it.gov.pagopa.receipt.pdf.service.service.impl;

import io.quarkus.cache.CacheResult;
import it.gov.pagopa.receipt.pdf.service.client.CartReceiptCosmosClient;
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
import java.util.List;
import java.util.Objects;

import static it.gov.pagopa.receipt.pdf.service.utils.CommonUtils.sanitize;

@ApplicationScoped
public class AttachmentsServiceImpl implements AttachmentsService {

    public static final String CART = "_CART_";
    public static final String PAYER = "PAYER";
    public static final String ANONIMO = "ANONIMO";
    private final Logger logger = LoggerFactory.getLogger(AttachmentsServiceImpl.class);

    private final ReceiptCosmosClient cosmosClient;
    private final CartReceiptCosmosClient cartReceiptCosmosClient;

    private final PDVTokenizerClient pdvTokenizerClient;

    private final ReceiptBlobClient receiptBlobClient;

    @Inject
    public AttachmentsServiceImpl(
            ReceiptCosmosClient cosmosClient, CartReceiptCosmosClient cartReceiptCosmosClient,
            @RestClient PDVTokenizerClient pdvTokenizerClient,
            ReceiptBlobClient receiptBlobClient
    ) {
        this.cosmosClient = cosmosClient;
        this.cartReceiptCosmosClient = cartReceiptCosmosClient;
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
                            sanitize(thirdPartyId));
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
            BlobStorageClientException, AttachmentNotFoundException, InvalidCartException, CartNotFoundException {

        if (thirdPartyId.contains(CART)) {
            getCartAttachment(thirdPartyId, requestFiscalCode, attachmentUrl);
        } else {
            getSingleReceiptAttachment(thirdPartyId, requestFiscalCode, attachmentUrl);
        }

        return receiptBlobClient.getAttachmentFromBlobStorage(attachmentUrl);
    }

    private void getSingleReceiptAttachment(String thirdPartyId, String requestFiscalCode, String attachmentUrl) throws ReceiptNotFoundException, InvalidReceiptException, FiscalCodeNotAuthorizedException {
        Receipt receiptDocument = getReceipt(thirdPartyId);
        SearchTokenResponse searchTokenResponse = getSearchTokenResponse(thirdPartyId, requestFiscalCode);

        if (isFiscalCodeNotAuthorized(searchTokenResponse.getToken(), attachmentUrl, receiptDocument)) {
            String errMsg =
                    String.format(
                            "Fiscal code is not authorized to access the receipts with name: %s, for receipt with id %s",
                            sanitize(attachmentUrl), sanitize(thirdPartyId));
            logger.error(errMsg);
            throw new FiscalCodeNotAuthorizedException(AppErrorCodeEnum.PDFS_706, errMsg);
        }
    }

    private void getCartAttachment(String thirdPartyId, String requestFiscalCode, String attachmentUrl) throws FiscalCodeNotAuthorizedException, InvalidCartException, CartNotFoundException {
        var partial = thirdPartyId.split(CART);

        String cartId = partial[0];

        CartForReceipt cartForReceipt = getCartReceipt(cartId);
        SearchTokenResponse searchTokenResponse = getSearchTokenResponse(thirdPartyId, requestFiscalCode);

        boolean isFiscalCodeNotAuthorized = isFiscalCodeNotAuthorized(attachmentUrl, partial, searchTokenResponse, cartForReceipt);

        if (isFiscalCodeNotAuthorized) {
            String errMsg =
                    String.format(
                            "Fiscal code is not authorized to access the receipts with name: %s, for cart with id %s",
                            sanitize(attachmentUrl), sanitize(thirdPartyId));
            logger.error(errMsg);
            throw new FiscalCodeNotAuthorizedException(AppErrorCodeEnum.PDFS_706, errMsg);
        }
    }

    /**
     * This method checks if the fiscal code is not authorized to access the attachment
     *
     * @param attachmentUrl       the attachment url from the request
     * @param partial             the splitted thirdPartyId
     * @param searchTokenResponse the tokenized fiscal code from the PDV Tokenizer
     * @param cartForReceipt      the cart for receipt object to check from the DB
     * @return true if the fiscal code is not authorized, false otherwise
     */
    private static boolean isFiscalCodeNotAuthorized(String attachmentUrl, String[] partial, SearchTokenResponse searchTokenResponse, CartForReceipt cartForReceipt) {
        boolean isFiscalCodeNotAuthorized;
        if (partial.length > 1) {
            isFiscalCodeNotAuthorized = isDebtorFiscalCodeNotAuthorized(searchTokenResponse.getToken(), attachmentUrl, partial[1], cartForReceipt);
        } else {
            isFiscalCodeNotAuthorized = isPayerFiscalCodeNotAuthorized(searchTokenResponse.getToken(), attachmentUrl, cartForReceipt);
        }
        return isFiscalCodeNotAuthorized;
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
                            sanitize(thirdPartyId));
            logger.error(errMsg, e);
            throw new FiscalCodeNotAuthorizedException(AppErrorCodeEnum.PDFS_700, errMsg);
        }
        return searchTokenResponse;
    }

    private Receipt getReceipt(String thirdPartyId)
            throws ReceiptNotFoundException, InvalidReceiptException {
        Receipt receiptDocument = cosmosClient.getReceiptDocument(thirdPartyId);

        if (receiptDocument == null) {
            String errMsg = String.format("The retrieved receipt with id: %s, is null", sanitize(thirdPartyId));
            logger.error(errMsg);
            throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_701, errMsg);
        }
        if (receiptDocument.getEventData() == null) {
            String errMsg =
                    String.format("The retrieved receipt with id: %s, has null event data", sanitize(thirdPartyId));
            logger.error(errMsg);
            throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_702, errMsg);
        }
        if (receiptDocument.getEventData().getDebtorFiscalCode() == null) {
            String errMsg =
                    String.format(
                            "The retrieved receipt with id: %s, has null debtor fiscal code",
                            sanitize(thirdPartyId));
            logger.error(errMsg);
            throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_703, errMsg);
        }
        if (receiptDocument.getMdAttach() == null && (!ANONIMO.equals(receiptDocument.getEventData().getDebtorFiscalCode()))) {
            String errMsg =
                    String.format(
                            "The retrieved receipt with id: %s, has null attachment info for debtor",
                            sanitize(thirdPartyId));
            logger.error(errMsg);
            throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_704, errMsg);
        }
        if (!isReceiptUnique(receiptDocument) && receiptDocument.getMdAttachPayer() == null) {
            String errMsg =
                    String.format(
                            "The retrieved receipt with id: %s, has different debtor and payer fiscal codes but has null attachment info for payer",
                            sanitize(thirdPartyId));
            logger.error(errMsg);
            throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_705, errMsg);
        }
        return receiptDocument;
    }

    private CartForReceipt getCartReceipt(String cartId) throws CartNotFoundException, InvalidCartException {
        CartForReceipt cartForReceipt = cartReceiptCosmosClient.getCartForReceiptDocument(cartId);

        if (cartForReceipt == null) {
            String errMsg = String.format("The retrieved cart with id: %s, is null", sanitize(cartId));
            logger.error(errMsg);
            throw new InvalidCartException(AppErrorCodeEnum.PDFS_707, errMsg);
        }
        if (cartForReceipt.getPayload() == null || cartForReceipt.getPayload().getCart() == null) {
            String errMsg =
                    String.format("The retrieved receipt with id: %s, has null payload", sanitize(cartId));
            logger.error(errMsg);
            throw new InvalidCartException(AppErrorCodeEnum.PDFS_708, errMsg);
        }

        var allDebtorsAreNull = cartForReceipt.getPayload().getCart().stream()
                .filter(Objects::nonNull)
                .map(CartPayment::getDebtorFiscalCode)
                .noneMatch(Objects::nonNull);

        if (allDebtorsAreNull) {
            String errMsg =
                    String.format(
                            "The retrieved cart with id: %s, has null debtors fiscal code",
                            sanitize(cartId));
            logger.error(errMsg);
            throw new InvalidCartException(AppErrorCodeEnum.PDFS_709, errMsg);
        }

        var allDebtorsAttachAreNull = cartForReceipt.getPayload().getCart().stream()
                .filter(Objects::nonNull)
                .map(CartPayment::getMdAttach)
                .filter(Objects::nonNull)
                .map(ReceiptMetadata::getName)
                .noneMatch(Objects::nonNull);

        if (allDebtorsAttachAreNull && cartForReceipt.getPayload().getMdAttachPayer() == null) {
            String errMsg =
                    String.format(
                            "The retrieved cart with id: %s, has null attachment info",
                            sanitize(cartId));
            logger.error(errMsg);
            throw new InvalidCartException(AppErrorCodeEnum.PDFS_710, errMsg);
        }

        for (CartPayment elem : cartForReceipt.getPayload().getCart()) {
            if ((elem.getDebtorFiscalCode() != null
                    && !elem.getDebtorFiscalCode().equals(ANONIMO)
                    && !elem.getDebtorFiscalCode().equals(cartForReceipt.getPayload().getPayerFiscalCode()))
                    && elem.getMdAttach() == null) {
                String errMsg =
                        String.format(
                                "The retrieved cart with id: %s, has null attachment info for debtor",
                                sanitize(cartId));
                logger.error(errMsg);
                throw new InvalidCartException(AppErrorCodeEnum.PDFS_711, errMsg);
            }
        }


        if (cartForReceipt.getPayload().getPayerFiscalCode() != null && cartForReceipt.getPayload().getMdAttachPayer() == null) {
            String errMsg =
                    String.format(
                            "The retrieved cart with id: %s, has null attachment info for payer",
                            sanitize(cartId));
            logger.error(errMsg);
            throw new InvalidCartException(AppErrorCodeEnum.PDFS_712, errMsg);
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
        String debtorFileName = !ANONIMO.equals(receiptDocument.getEventData().getDebtorFiscalCode()) ?
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
     * This method checks if the debtor fiscal code is not authorized to access the attachment
     *
     * @param requestFiscalCode the fiscal code from the request
     * @param attachmentUrl     the attachment url from the request
     * @param eventId           the event id to match in the cart from the request
     * @param cartForReceipt    the cart for receipt object to check from the DB
     * @return true if the debtor fiscal code is not authorized, false otherwise
     */
    private static boolean isDebtorFiscalCodeNotAuthorized(String requestFiscalCode, String attachmentUrl, String eventId, CartForReceipt cartForReceipt) {
        List<CartPayment> cart = cartForReceipt.getPayload().getCart();

        List<CartPayment> matches = cart == null ? List.of() :
                cart.stream()
                        .filter(Objects::nonNull)
                        .filter(elem -> eventId.equals(elem.getBizEventId()))
                        .toList();

        boolean isDebtorAuthorized = false;
        if (matches.size() == 1) {
            CartPayment match = matches.get(0);
            ReceiptMetadata mdAttach = match.getMdAttach();
            isDebtorAuthorized = Objects.equals(requestFiscalCode, match.getDebtorFiscalCode())
                    && mdAttach != null
                    && Objects.equals(attachmentUrl, mdAttach.getName());
        }

        return !isDebtorAuthorized;
    }

    /**
     * This method checks if the payer fiscal code is not authorized to access the attachment
     *
     * @param requestFiscalCode the fiscal code from the request
     * @param attachmentUrl     the attachment url from the request
     * @param cartForReceipt    the cart for receipt object to check from the DB
     * @return true if the payer fiscal code is not authorized, false otherwise
     */
    private static boolean isPayerFiscalCodeNotAuthorized(String requestFiscalCode, String attachmentUrl, CartForReceipt cartForReceipt) {
        boolean isPayerAuthorized = cartForReceipt.getPayload().getMdAttachPayer() != null
                && attachmentUrl.equals(cartForReceipt.getPayload().getMdAttachPayer().getName())
                && requestFiscalCode.equals(cartForReceipt.getPayload().getPayerFiscalCode());

        return !isPayerAuthorized;
    }

    private boolean isReceiptUnique(Receipt receiptDocument) {
        return receiptDocument.getEventData().getPayerFiscalCode() == null ||
                receiptDocument
                        .getEventData()
                        .getDebtorFiscalCode()
                        .equals(receiptDocument.getEventData().getPayerFiscalCode());
    }
}
