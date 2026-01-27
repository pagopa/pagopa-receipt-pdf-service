package it.gov.pagopa.receipt.pdf.service.service.impl;

import io.quarkus.cache.CacheResult;
import it.gov.pagopa.receipt.pdf.service.client.CartReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.client.PDVTokenizerClient;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptBlobClient;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.enumeration.ReceiptStatusType;
import it.gov.pagopa.receipt.pdf.service.exception.*;
import it.gov.pagopa.receipt.pdf.service.model.*;
import it.gov.pagopa.receipt.pdf.service.model.cart.*;
import it.gov.pagopa.receipt.pdf.service.model.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.service.model.receipt.ReceiptMetadata;
import it.gov.pagopa.receipt.pdf.service.service.AttachmentsService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.*;
import static it.gov.pagopa.receipt.pdf.service.utils.CommonUtils.sanitize;

@ApplicationScoped
public class AttachmentsServiceImpl implements AttachmentsService {

    public static final String CART = "_CART_";
    public static final String ANONIMO = "ANONIMO";
    public static final int PDF_TEMPLATE_ERROR_CODE = 903;
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
            throws ReceiptNotFoundException, InvalidReceiptException, FiscalCodeNotAuthorizedException, InvalidCartException, CartNotFoundException {

        if (thirdPartyId.contains(CART)) {
            return handleCartAttachmentDetails(thirdPartyId, requestFiscalCode);
        } else {
            return handleSingleReceiptAttachmentDetails(thirdPartyId, requestFiscalCode);
        }

    }

    @Override
    public byte[] getAttachmentBytesFromBlobStorage(String fileName)
            throws IOException, AttachmentNotFoundException, BlobStorageClientException {
        File pdfFile = this.receiptBlobClient.getAttachmentFromBlobStorage(fileName);
        try (FileInputStream inputStream = new FileInputStream(pdfFile)) {
            return IOUtils.toByteArray(inputStream);
        } finally {
            if (pdfFile != null) {
                Files.deleteIfExists(pdfFile.toPath());
            }
        }
    }


    /**
     * Retrieves the attachment details for a single receipt, validating the authorization
     * of the provided fiscal code to access the receipt.
     *
     * @param thirdPartyId      the unique identifier of the receipt
     * @param requestFiscalCode the fiscal code requesting access
     * @return the details of the attachment for the specified receipt
     * @throws ReceiptNotFoundException         if the receipt is not found
     * @throws InvalidReceiptException          if the receipt is invalid
     * @throws FiscalCodeNotAuthorizedException if the fiscal code is not authorized to access the receipt
     */
    private AttachmentsDetailsResponse handleSingleReceiptAttachmentDetails(String thirdPartyId, String requestFiscalCode)
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


    /**
     * Retrieves the attachment details for a cart, handling both payer and debtor cases.
     *
     * <p>
     * If the {@code thirdPartyId} contains a business event ID (debtor case), it fetches the corresponding
     * cart payment and its attachment details. If not (payer case), it fetches the payer's attachment details.
     * The method validates the authorization of the provided fiscal code to access the cart's attachments.
     * </p>
     *
     * @param thirdPartyId      the unique identifier of the cart, possibly including a business event ID
     * @param requestFiscalCode the fiscal code requesting access
     * @return the details of the attachment for the specified cart
     * @throws CartNotFoundException            if the cart is not found
     * @throws InvalidReceiptException          if the cart or its attachments are invalid
     * @throws FiscalCodeNotAuthorizedException if the fiscal code is not authorized to access the cart's attachments
     * @throws InvalidCartException             if the cart is invalid
     */
    private AttachmentsDetailsResponse handleCartAttachmentDetails(String thirdPartyId, String requestFiscalCode) throws CartNotFoundException, InvalidReceiptException, FiscalCodeNotAuthorizedException, InvalidCartException {
        var thirdPartyIdParts = thirdPartyId.split(CART);

        String cartId = thirdPartyIdParts[0];
        String bizEventId = null;
        if (thirdPartyIdParts.length > 1) {
            bizEventId = thirdPartyIdParts[1];
        }

        CartForReceipt cartForReceipt = getCartReceipt(cartId);
        SearchTokenResponse searchTokenResponse = getSearchTokenResponse(thirdPartyId, requestFiscalCode);
        String token = searchTokenResponse.getToken();
        if (isFiscalCodeNotAuthorized(token, bizEventId, cartForReceipt)) {
            String errMsg =
                    String.format(
                            "Fiscal code is not authorized to access the receipts for cart with id %s", sanitize(thirdPartyId));
            logger.error(errMsg);
            throw new FiscalCodeNotAuthorizedException(AppErrorCodeEnum.PDFS_700, errMsg);
        }
        //Payer case -> no need for bizEventId
        if (bizEventId == null) {

            if (cartForReceipt.getPayload().getMdAttachPayer() == null) {
                String errMsg =
                        String.format("The retrieved receipt metadata for cart %s has null payer attachment info",
                                sanitize(cartId));
                logger.error(errMsg);
                throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_712, errMsg);
            }

            if (cartForReceipt.getPayload().getMessagePayer() == null) {
                String errMsg =
                        String.format("The retrieved receipt metadata for cart %s has null payer message data",
                                sanitize(cartId));
                logger.error(errMsg);
                throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_713, errMsg);
            }

            return buildCartAttachmentDetails(cartForReceipt,
                    cartForReceipt.getPayload().getMdAttachPayer(), cartForReceipt.getPayload().getMessagePayer());
        }

        //Debtor case -> obtained receiptmetadata in cart filtered by bizEventId
        CartPayment cartItem = findDebtorCartPaymentByBizEventId(cartForReceipt, bizEventId);

        if (cartItem == null || cartItem.getMdAttach() == null) {
            String errMsg =
                    String.format("The retrieved receipt metadata for cart %s for the debtor with bizEventId: %s has null debtors attachment info",
                            sanitize(cartId), sanitize(bizEventId));
            logger.error(errMsg);
            throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_711, errMsg);
        }

        return buildCartAttachmentDetails(cartForReceipt, cartItem.getMdAttach(), cartItem.getMessageDebtor());

    }

    /**
     * Finds the debtor cart payment item in the given cart that matches the specified business event ID.
     *
     * @param cartForReceipt the cart containing the list of payments
     * @param eventId        the business event ID to match
     * @return the first matching {@link CartPayment} with non-null attachment and message for the debtor, or null if not found
     */
    private CartPayment findDebtorCartPaymentByBizEventId(CartForReceipt cartForReceipt, String eventId) {
        return cartForReceipt.getPayload().getCart() != null
                ?
                cartForReceipt.getPayload().getCart().stream()
                        .filter(elem ->
                                elem != null
                                        && elem.getMdAttach() != null
                                        && elem.getMessageDebtor() != null
                                        && eventId.equals(elem.getBizEventId())
                        )
                        .findFirst()
                        .orElse(null)
                :
                null;
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
            throw new FiscalCodeNotAuthorizedException(PDFS_706, errMsg);
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
            throw new FiscalCodeNotAuthorizedException(PDFS_706, errMsg);
        }
    }

    /**
     * This method checks if the fiscal code is not authorized to access the attachment
     *
     * @param attachmentUrl       the attachment url from the request
     * @param partial             the split thirdPartyId
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

    private AttachmentsDetailsResponse buildCartAttachmentDetails(
            CartForReceipt cartReceiptDocument, ReceiptMetadata receiptMetadata, MessageData messageData) {
        return AttachmentsDetailsResponse.builder()
                .attachments(
                        Collections.singletonList(
                                Attachment.builder()
                                        .id(cartReceiptDocument.getId())
                                        .contentType("application/pdf")
                                        .url(receiptMetadata.getName())
                                        .name(receiptMetadata.getName())
                                        .build()))
                .details(Detail.builder()
                        .subject(messageData.getSubject())
                        .markdown(messageData.getMarkdown())
                        .build())
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

    /**
     * This method checks if the fiscal code is authorized to access the attachment details
     *
     * @param requestFiscalCode the fiscal code to check
     * @param eventId           the event id
     * @param cartForReceipt    the cart for receipt
     * @return true if the fiscal code is not authorized, false otherwise
     */
    private boolean isFiscalCodeNotAuthorized(
            String requestFiscalCode, String eventId, CartForReceipt cartForReceipt) {

        // check null cart or payload
        if (cartForReceipt == null || cartForReceipt.getPayload() == null) {
            return true;
        }

        if (eventId == null) {
            // if third_party_id is a payer then check payer attachment details

            boolean isPayerAuthorized = cartForReceipt.getPayload().getMdAttachPayer() != null
                    && requestFiscalCode.equals(cartForReceipt.getPayload().getPayerFiscalCode());

            return !isPayerAuthorized;

        } else {
            // else check debtor attachment details

            boolean isDebtorAuthorized = cartForReceipt.getPayload().getCart() != null
                    && cartForReceipt.getPayload().getCart().stream()
                    .filter(elem ->
                            elem != null
                                    && elem.getMdAttach() != null
                    )
                    .anyMatch(elem ->
                            requestFiscalCode.equals(elem.getDebtorFiscalCode())
                                    && eventId.equals(elem.getBizEventId())
                    );

            return !isDebtorAuthorized;

        }
    }

    @Override
    public File getReceiptPdf(String thirdPartyId, String requestFiscalCode) throws
            FiscalCodeNotAuthorizedException, BlobStorageClientException, AttachmentNotFoundException,
            ReceiptNotFoundException, CartNotFoundException, InvalidReceiptException, InvalidCartException {
        String attachmentName;
        try {
            if (thirdPartyId.contains(CART)) {
                String cartId = thirdPartyId.split(CART)[0];

                CartForReceipt cart = cartReceiptCosmosClient.getCartForReceiptDocument(cartId);

                if (CartStatusType.pdfWaitingToBeGenerated().contains(cart.getStatus())) {
                    throw new InvalidCartException(PDFS_714, PDFS_714.getErrorMessage());
                }
                if (CartStatusType.pdfFailedToBeGenerated().contains(cart.getStatus())) {
                    if (cart.getReasonErr() != null && cart.getReasonErr().getCode() != PDF_TEMPLATE_ERROR_CODE) {
                        throw new InvalidCartException(PDFS_715, PDFS_715.getErrorMessage());
                    }
                    throw new InvalidCartException(PDFS_716, PDFS_716.getErrorMessage());
                }

                String fiscalCode = getSearchTokenResponse(thirdPartyId, requestFiscalCode).getToken();
                Payload cartPayload = cart.getPayload();
                if (Objects.equals(cartPayload.getPayerFiscalCode(), fiscalCode)) {
                    attachmentName = cartPayload.getMdAttachPayer().getName();
                } else {
                    ReceiptMetadata mdAttach = cartPayload.getCart().stream()
                            .filter(md -> Objects.equals(md.getDebtorFiscalCode(), fiscalCode))
                            .findFirst().orElseThrow(() -> new FiscalCodeNotAuthorizedException(
                                    PDFS_706,
                                    String.format("Fiscal code is not authorized to access the receipt with cart id %s", cartId)
                            ))
                            .getMdAttach();

                    attachmentName = mdAttach.getName();
                }
            } else {
                Receipt receipt = cosmosClient.getReceiptDocument(thirdPartyId);

                if (ReceiptStatusType.pdfWaitingToBeGenerated().contains(receipt.getStatus())) {
                    throw new InvalidReceiptException(PDFS_714, PDFS_714.getErrorMessage());
                }
                if (ReceiptStatusType.pdfFailedToBeGenerated().contains(receipt.getStatus())) {
                    if (receipt.getReasonErr() != null && receipt.getReasonErr().getCode() != PDF_TEMPLATE_ERROR_CODE) {
                        throw new InvalidReceiptException(PDFS_715, PDFS_715.getErrorMessage());
                    }
                    throw new InvalidReceiptException(PDFS_716, PDFS_716.getErrorMessage());
                }

                String fiscalCode = getSearchTokenResponse(thirdPartyId, requestFiscalCode).getToken();
                if (Objects.equals(receipt.getEventData().getDebtorFiscalCode(),fiscalCode)) {
                    attachmentName = receipt.getMdAttach().getName();
                } else if (Objects.equals(receipt.getEventData().getPayerFiscalCode(),fiscalCode)) {
                    attachmentName = receipt.getMdAttachPayer().getName();
                } else {
                    throw new FiscalCodeNotAuthorizedException(PDFS_706, String.format(
                            "Fiscal code is not authorized to access the receipt with id %s", sanitize(thirdPartyId)));
                }
            }
        } catch (FiscalCodeNotAuthorizedException e) {
            String errMsg = e.getMessage();
            logger.error(errMsg);
            throw new FiscalCodeNotAuthorizedException(PDFS_706, errMsg);
        }

        if (attachmentName == null || attachmentName.isEmpty() || attachmentName.isBlank()) {
            throw new AttachmentNotFoundException(PDFS_715, PDFS_715.getErrorMessage());
        }

        return receiptBlobClient.getAttachmentFromBlobStorage(attachmentName);
    }
}
