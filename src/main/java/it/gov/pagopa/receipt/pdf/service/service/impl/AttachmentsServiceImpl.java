package it.gov.pagopa.receipt.pdf.service.service.impl;

import io.quarkus.cache.CacheResult;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptBlobClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.AttachmentNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.BlobStorageClientException;
import it.gov.pagopa.receipt.pdf.service.exception.CartNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.FiscalCodeNotAuthorizedException;
import it.gov.pagopa.receipt.pdf.service.exception.InvalidCartException;
import it.gov.pagopa.receipt.pdf.service.exception.InvalidReceiptException;
import it.gov.pagopa.receipt.pdf.service.exception.ReceiptNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.Attachment;
import it.gov.pagopa.receipt.pdf.service.model.AttachmentsDetailsResponse;
import it.gov.pagopa.receipt.pdf.service.model.Detail;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartPayment;
import it.gov.pagopa.receipt.pdf.service.model.cart.MessageData;
import it.gov.pagopa.receipt.pdf.service.model.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.service.model.receipt.ReceiptMetadata;
import it.gov.pagopa.receipt.pdf.service.service.AttachmentsService;
import it.gov.pagopa.receipt.pdf.service.utils.CommonUtils;
import it.gov.pagopa.receipt.pdf.service.utils.PerfTracer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.PDFS_706;
import static it.gov.pagopa.receipt.pdf.service.utils.CommonUtils.sanitize;
import static it.gov.pagopa.receipt.pdf.service.utils.PerfTracer.IS_CART_TAG;
import static it.gov.pagopa.receipt.pdf.service.utils.PerfTracer.VALID_TAG;

@ApplicationScoped
public class AttachmentsServiceImpl implements AttachmentsService {

    private final Logger logger = LoggerFactory.getLogger(AttachmentsServiceImpl.class);

    public static final String ANONIMO = "ANONIMO";

    private final ReceiptCosmosService receiptCosmosService;
    private final CartReceiptCosmosService cartReceiptCosmosService;
    private final TokenizerService tokenizerService;
    private final ReceiptBlobClient receiptBlobClient;

    @Inject
    public AttachmentsServiceImpl(
            ReceiptCosmosService receiptCosmosService,
            CartReceiptCosmosService cartReceiptCosmosService,
            TokenizerService tokenizerService,
            ReceiptBlobClient receiptBlobClient
    ) {
        this.receiptCosmosService = receiptCosmosService;
        this.cartReceiptCosmosService = cartReceiptCosmosService;
        this.tokenizerService = tokenizerService;
        this.receiptBlobClient = receiptBlobClient;
    }

    @CacheResult(cacheName = "getAttachmentsDetails")
    @Override
    public AttachmentsDetailsResponse getAttachmentsDetails(String thirdPartyId, String requestFiscalCode)
            throws ReceiptNotFoundException, InvalidReceiptException, FiscalCodeNotAuthorizedException, InvalidCartException, CartNotFoundException {

        boolean isCart = CommonUtils.isCart(thirdPartyId);
        try (PerfTracer t = PerfTracer.start(logger, "getAttachmentsDetails").tag(IS_CART_TAG, isCart)) {
            if (isCart) {
                return handleCartAttachmentDetails(thirdPartyId, requestFiscalCode);
            }
            return handleSingleReceiptAttachmentDetails(thirdPartyId, requestFiscalCode);
        }

    }

    @Override
    public InputStream getAttachment(String thirdPartyId, String requestFiscalCode, String attachmentUrl)
            throws ReceiptNotFoundException, InvalidReceiptException, FiscalCodeNotAuthorizedException,
            BlobStorageClientException, AttachmentNotFoundException, InvalidCartException, CartNotFoundException {

        boolean isCart = CommonUtils.isCart(thirdPartyId);
        try (PerfTracer t = PerfTracer.start(logger, "getAttachment").tag(IS_CART_TAG, isCart)) {
            if (isCart) {
                checkIfUserIsAuthorizedToAccessCartAttachment(thirdPartyId, requestFiscalCode, attachmentUrl);
            } else {
                checkIfUserIsAuthorizedToAccessReceiptAttachment(thirdPartyId, requestFiscalCode, attachmentUrl);
            }
        }

        try (PerfTracer t = PerfTracer.start(logger, "getAttachmentFromBlobStorage")) {
            return this.receiptBlobClient.getAttachmentFromBlobStorage(attachmentUrl);
        }
    }

    @Override
    public byte[] getAttachmentBytesFromBlobStorage(String attachmentName)
            throws IOException, AttachmentNotFoundException, BlobStorageClientException {
        try (InputStream inputStream = this.receiptBlobClient.getAttachmentFromBlobStorage(attachmentName)) {
            return IOUtils.toByteArray(inputStream);
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
    private AttachmentsDetailsResponse handleSingleReceiptAttachmentDetails(
            String thirdPartyId,
            String requestFiscalCode
    ) throws ReceiptNotFoundException, InvalidReceiptException, FiscalCodeNotAuthorizedException {
        Receipt receipt = this.receiptCosmosService.getReceipt(thirdPartyId);
        try (PerfTracer t = PerfTracer.start(logger, "receipt.validate")) {
            validateReceipt(thirdPartyId, receipt);
            t.tag(VALID_TAG, true);
        }

        String token = getFiscalCodeToken(requestFiscalCode);
        if (isFiscalCodeNotAuthorized(token, receipt)) {
            String errMsg =
                    String.format(
                            "Fiscal code is not authorized to access the receipts with id: %s",
                            sanitize(thirdPartyId));
            logger.error(errMsg);
            throw new FiscalCodeNotAuthorizedException(AppErrorCodeEnum.PDFS_700, errMsg);
        }

        try (PerfTracer t = PerfTracer.start(logger, "response.build").tag("flow", "singleReceipt")) {
            if (receipt.getEventData().getDebtorFiscalCode().equals(token)) {
                return buildAttachmentDetails(receipt, receipt.getMdAttach());
            }
            return buildAttachmentDetails(receipt, receipt.getMdAttachPayer());
        }
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
    private AttachmentsDetailsResponse handleCartAttachmentDetails(
            String thirdPartyId,
            String requestFiscalCode
    ) throws CartNotFoundException, InvalidReceiptException, FiscalCodeNotAuthorizedException, InvalidCartException {
        String cartId = CommonUtils.getPaymentId(thirdPartyId);

        CartForReceipt cart = this.cartReceiptCosmosService.getCartReceipt(cartId);
        try (PerfTracer t = PerfTracer.start(logger, "cart.validate").tag("cartId", cartId)) {
            validateCartReceipt(cart);
            t.tag(VALID_TAG, true);
        }

        String token = getFiscalCodeToken(requestFiscalCode);
        String bizEventId = CommonUtils.getBizEventId(thirdPartyId);
        if (isFiscalCodeNotAuthorized(token, bizEventId, cart)) {
            String errMsg =
                    String.format(
                            "Fiscal code is not authorized to access the receipts for cart with id %s", sanitize(thirdPartyId));
            logger.error(errMsg);
            throw new FiscalCodeNotAuthorizedException(AppErrorCodeEnum.PDFS_700, errMsg);
        }
        //Payer case -> no need for bizEventId
        if (bizEventId == null) {

            if (cart.getPayload().getMdAttachPayer() == null) {
                String errMsg =
                        String.format("The retrieved receipt metadata for cart %s has null payer attachment info",
                                sanitize(cartId));
                logger.error(errMsg);
                throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_712, errMsg);
            }

            if (cart.getPayload().getMessagePayer() == null) {
                String errMsg =
                        String.format("The retrieved receipt metadata for cart %s has null payer message data",
                                sanitize(cartId));
                logger.error(errMsg);
                throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_713, errMsg);
            }

            return buildCartAttachmentDetails(cart,
                    cart.getPayload().getMdAttachPayer(), cart.getPayload().getMessagePayer(), "cart-payer");
        }

        //Debtor case -> obtained receiptmetadata in cart filtered by bizEventId
        CartPayment cartItem = findDebtorCartPaymentByBizEventId(cart, bizEventId);

        if (cartItem == null || cartItem.getMdAttach() == null) {
            String errMsg =
                    String.format("The retrieved receipt metadata for cart %s for the debtor with bizEventId: %s has null debtors attachment info",
                            sanitize(cartId), sanitize(bizEventId));
            logger.error(errMsg);
            throw new InvalidReceiptException(AppErrorCodeEnum.PDFS_711, errMsg);
        }

        return buildCartAttachmentDetails(cart, cartItem.getMdAttach(), cartItem.getMessageDebtor(), "cart-debtor");

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

    private void checkIfUserIsAuthorizedToAccessReceiptAttachment(
            String thirdPartyId,
            String requestFiscalCode,
            String attachmentUrl
    ) throws ReceiptNotFoundException, InvalidReceiptException, FiscalCodeNotAuthorizedException {
        Receipt receipt = this.receiptCosmosService.getReceipt(thirdPartyId);
        try (PerfTracer t = PerfTracer.start(logger, "receipt.validate")) {
            validateReceipt(thirdPartyId, receipt);
            t.tag(VALID_TAG, true);
        }

        String token = getFiscalCodeToken(requestFiscalCode);
        if (isFiscalCodeNotAuthorized(token, attachmentUrl, receipt)) {
            String errMsg =
                    String.format(
                            "Fiscal code is not authorized to access the receipts with name: %s, for receipt with id %s",
                            sanitize(attachmentUrl), sanitize(thirdPartyId));
            logger.error(errMsg);
            throw new FiscalCodeNotAuthorizedException(PDFS_706, errMsg);
        }
    }

    private void checkIfUserIsAuthorizedToAccessCartAttachment(
            String thirdPartyId,
            String requestFiscalCode,
            String attachmentUrl
    ) throws FiscalCodeNotAuthorizedException, InvalidCartException, CartNotFoundException {
        String cartId = CommonUtils.getPaymentId(thirdPartyId);

        CartForReceipt cart = this.cartReceiptCosmosService.getCartReceipt(cartId);
        try (PerfTracer t = PerfTracer.start(logger, "cart.validate").tag("cartId", cartId)) {
            validateCartReceipt(cart);
            t.tag(VALID_TAG, true);
        }

        String token = getFiscalCodeToken(requestFiscalCode);
        String bizEventId = CommonUtils.getBizEventId(thirdPartyId);
        boolean isFiscalCodeNotAuthorized = isFiscalCodeNotAuthorized(attachmentUrl, bizEventId, token, cart);

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
     * @param attachmentUrl  the attachment url from the request
     * @param bizEventId     the payment bizEventId
     * @param token          the tokenized fiscal code from the PDV Tokenizer
     * @param cartForReceipt the cart for receipt object to check from the DB
     * @return true if the fiscal code is not authorized, false otherwise
     */
    private boolean isFiscalCodeNotAuthorized(
            String attachmentUrl,
            String bizEventId,
            String token,
            CartForReceipt cartForReceipt
    ) {
        boolean isFiscalCodeNotAuthorized;
        if (bizEventId != null) {
            isFiscalCodeNotAuthorized = isDebtorFiscalCodeNotAuthorized(token, attachmentUrl, bizEventId, cartForReceipt);
        } else {
            isFiscalCodeNotAuthorized = isPayerFiscalCodeNotAuthorized(token, attachmentUrl, cartForReceipt);
        }
        return isFiscalCodeNotAuthorized;
    }

    private void validateReceipt(String thirdPartyId, Receipt receiptDocument) throws InvalidReceiptException {
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
    }

    private void validateCartReceipt(CartForReceipt cart) throws InvalidCartException {
        String cartId = cart.getCartId();
        if (cartForReceipt == null) {
            String errMsg = String.format("The retrieved cart with id: %s, is null", sanitize(cartId));
            logger.error(errMsg);
            throw new InvalidCartException(AppErrorCodeEnum.PDFS_707, errMsg);
        }

        if (cart.getPayload() == null || cart.getPayload().getCart() == null) {
            String errMsg =
                    String.format("The retrieved receipt with id: %s, has null payload", sanitize(cartId));
            logger.error(errMsg);
            throw new InvalidCartException(AppErrorCodeEnum.PDFS_708, errMsg);
        }

        var allDebtorsAreNull = cart.getPayload().getCart().stream()
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

        var allDebtorsAttachAreNull = cart.getPayload().getCart().stream()
                .filter(Objects::nonNull)
                .map(CartPayment::getMdAttach)
                .filter(Objects::nonNull)
                .map(ReceiptMetadata::getName)
                .noneMatch(Objects::nonNull);

        if (allDebtorsAttachAreNull && cart.getPayload().getMdAttachPayer() == null) {
            String errMsg =
                    String.format(
                            "The retrieved cart with id: %s, has null attachment info",
                            sanitize(cartId));
            logger.error(errMsg);
            throw new InvalidCartException(AppErrorCodeEnum.PDFS_710, errMsg);
        }

        for (CartPayment elem : cart.getPayload().getCart()) {
            if ((elem.getDebtorFiscalCode() != null
                    && !elem.getDebtorFiscalCode().equals(ANONIMO)
                    && !elem.getDebtorFiscalCode().equals(cart.getPayload().getPayerFiscalCode()))
                    && elem.getMdAttach() == null) {
                String errMsg =
                        String.format(
                                "The retrieved cart with id: %s, has null attachment info for debtor",
                                sanitize(cartId));
                logger.error(errMsg);
                throw new InvalidCartException(AppErrorCodeEnum.PDFS_711, errMsg);
            }
        }


        if (cart.getPayload().getPayerFiscalCode() != null && cart.getPayload().getMdAttachPayer() == null) {
            String errMsg =
                    String.format(
                            "The retrieved cart with id: %s, has null attachment info for payer",
                            sanitize(cartId));
            logger.error(errMsg);
            throw new InvalidCartException(AppErrorCodeEnum.PDFS_712, errMsg);
        }
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
            CartForReceipt cartReceiptDocument, ReceiptMetadata receiptMetadata, MessageData messageData, String flow) {
        try (PerfTracer t = PerfTracer.start(logger, "response.build").tag("flow", flow)) {
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
    private boolean isDebtorFiscalCodeNotAuthorized(
            String requestFiscalCode,
            String attachmentUrl,
            String eventId,
            CartForReceipt cartForReceipt
    ) {
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
    private boolean isPayerFiscalCodeNotAuthorized(
            String requestFiscalCode,
            String attachmentUrl,
            CartForReceipt cartForReceipt
    ) {
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

    private String getFiscalCodeToken(String requestFiscalCode) throws FiscalCodeNotAuthorizedException {
        try (PerfTracer t = PerfTracer.start(logger, "tokenizer.searchToken")) {
            return this.tokenizerService.getFiscalCodeToken(requestFiscalCode);
        }
    }
}
