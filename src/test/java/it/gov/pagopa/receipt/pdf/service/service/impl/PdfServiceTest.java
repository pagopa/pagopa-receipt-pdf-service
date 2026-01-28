package it.gov.pagopa.receipt.pdf.service.service.impl;

import io.quarkus.test.junit.QuarkusTest;
import it.gov.pagopa.receipt.pdf.service.client.CartReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptBlobClient;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.enumeration.ReceiptStatusType;
import it.gov.pagopa.receipt.pdf.service.exception.*;
import it.gov.pagopa.receipt.pdf.service.model.SearchTokenResponse;
import it.gov.pagopa.receipt.pdf.service.model.receipt.EventData;
import it.gov.pagopa.receipt.pdf.service.model.receipt.ReasonError;
import it.gov.pagopa.receipt.pdf.service.model.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.service.model.receipt.ReceiptMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;

import java.io.File;

import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class PdfServiceTest {
    private static final String GENERIC_EVENT_ID = "eventId";
    private static final String THIRD_PARTY_ID_RECEIPT = GENERIC_EVENT_ID;
    private static final String DEBTOR_1_EVENT_ID = "eventId1";
    private static final String DEBTOR_2_EVENT_ID = "eventId2";
    private static final String THIRD_PARTY_ID_CART_PAYER = "cartId_CART_";
    private static final String THIRD_PARTY_ID_CART_DEBTOR_1 = "cartId_CART_" + DEBTOR_1_EVENT_ID;
    private static final String THIRD_PARTY_ID_CART_DEBTOR_2 = "cartId_CART_" + DEBTOR_2_EVENT_ID;
    private static final String PAYER_FISCAL_CODE = "payer";
    private static final String PAYER_FISCAL_CODE_TOKENIZED = "payerTokenized";
    private static final String DEBTOR_FISCAL_CODE_1 = "debtor1";
    private static final String DEBTOR_FISCAL_CODE_2 = "debtor2";
    private static final String DEBTOR_FISCAL_CODE_1_TOKENIZED = "debtor1Tokenized";
    private static final String DEBTOR_FISCAL_CODE_2_TOKENIZED = "debtor2Tokenized";
    private static final String ATTACHMENT_NAME_PAYER = "attachmentNamePayer";
    private static final String ATTACHMENT_NAME_DEBTOR_1 = "attachmentNameDebtor1";
    private static final String ATTACHMENT_NAME_DEBTOR_2 = "attachmentNameDebtor2";
    public static final int ERROR_CODE_RETRYABLE = 900;
    public static final int ERROR_CODE_CRITICAL = 903;
    public static final String INVALID_FISCAL_CODE = "invalidFiscalCode";
    public static final String INVALID_FISCAL_CODE_TOKENIZED = "invalidFiscalCodeTokenized";

    @Mock
    private ReceiptCosmosClient receiptCosmosClient;
    @Mock
    private CartReceiptCosmosClient cartReceiptCosmosClient;
    @Mock
    private TokenizerService tokenizerService;
    @Mock
    private ReceiptBlobClient receiptBlobClient;

    private PdfService sut;

    @BeforeEach
    void setUp() {
        receiptCosmosClient = mock(ReceiptCosmosClient.class);
        cartReceiptCosmosClient = mock(CartReceiptCosmosClient.class);
        tokenizerService = mock(TokenizerService.class);
        receiptBlobClient = mock(ReceiptBlobClient.class);

        sut = new PdfService(receiptCosmosClient, cartReceiptCosmosClient, tokenizerService, receiptBlobClient);
    }

    @Nested
    @DisplayName("Receipt")
    class PdfPayerTests {
        @ParameterizedTest
        @EnumSource(value = ReceiptStatusType.class, names = {
                "GENERATED",
                "SIGNED",
                "IO_NOTIFIED",
                "IO_ERROR_TO_NOTIFY",
                "IO_NOTIFIER_RETRY",
                "UNABLE_TO_SEND",
                "NOT_TO_NOTIFY"
        })
        void getReceiptPdf_Receipt_Payer_OK(ReceiptStatusType status) throws ReceiptNotFoundException, FiscalCodeNotAuthorizedException, AttachmentNotFoundException, BlobStorageClientException, CartNotFoundException {
            Receipt receipt = getReceipt(status, null, null);
            when(receiptCosmosClient.getReceiptDocument(GENERIC_EVENT_ID)).thenReturn(receipt);

            SearchTokenResponse searchTokenResponse = new SearchTokenResponse();
            searchTokenResponse.setToken(PAYER_FISCAL_CODE_TOKENIZED);
            when(tokenizerService.getSearchTokenResponse(GENERIC_EVENT_ID, PAYER_FISCAL_CODE)).thenReturn(searchTokenResponse);

            when(receiptBlobClient.getAttachmentFromBlobStorage(ATTACHMENT_NAME_PAYER)).thenReturn(new File("temp"));

            assertDoesNotThrow(() -> sut.getReceiptPdf(THIRD_PARTY_ID_RECEIPT, PAYER_FISCAL_CODE));
            verify(cartReceiptCosmosClient, never()).getCartForReceiptDocument(anyString());
        }

        @ParameterizedTest
        @EnumSource(value = ReceiptStatusType.class, names = {
                "GENERATED",
                "SIGNED",
                "IO_NOTIFIED",
                "IO_ERROR_TO_NOTIFY",
                "IO_NOTIFIER_RETRY",
                "UNABLE_TO_SEND",
                "NOT_TO_NOTIFY"
        })
        void getReceiptPdf_Receipt_Debtor_OK(ReceiptStatusType status) throws ReceiptNotFoundException, FiscalCodeNotAuthorizedException, AttachmentNotFoundException, BlobStorageClientException, CartNotFoundException {
            Receipt receipt = getReceipt(status, null, null);
            when(receiptCosmosClient.getReceiptDocument(GENERIC_EVENT_ID)).thenReturn(receipt);

            SearchTokenResponse searchTokenResponse = new SearchTokenResponse();
            searchTokenResponse.setToken(DEBTOR_FISCAL_CODE_1_TOKENIZED);
            when(tokenizerService.getSearchTokenResponse(GENERIC_EVENT_ID, DEBTOR_FISCAL_CODE_1)).thenReturn(searchTokenResponse);

            when(receiptBlobClient.getAttachmentFromBlobStorage(ATTACHMENT_NAME_DEBTOR_1)).thenReturn(new File("temp"));

            assertDoesNotThrow(() -> sut.getReceiptPdf(THIRD_PARTY_ID_RECEIPT, DEBTOR_FISCAL_CODE_1));
            verify(cartReceiptCosmosClient, never()).getCartForReceiptDocument(anyString());
        }

        @ParameterizedTest
        @EnumSource(value = ReceiptStatusType.class, names = {
                "INSERTED",
                "RETRY"
        })
        void getReceiptPdf_Receipt_KO_Still_Generating(ReceiptStatusType status) throws ReceiptNotFoundException, FiscalCodeNotAuthorizedException, AttachmentNotFoundException, BlobStorageClientException, CartNotFoundException {
            Receipt receipt = getReceipt(status, null, null);
            when(receiptCosmosClient.getReceiptDocument(GENERIC_EVENT_ID)).thenReturn(receipt);

            InvalidReceiptException exception = assertThrows(
                    InvalidReceiptException.class,
                    () -> sut.getReceiptPdf(THIRD_PARTY_ID_RECEIPT, PAYER_FISCAL_CODE)
            );
            assertEquals(PDFS_714, exception.getErrorCode());

            verify(tokenizerService, never()).getSearchTokenResponse(anyString(), anyString());
            verify(cartReceiptCosmosClient, never()).getCartForReceiptDocument(anyString());
            verify(receiptBlobClient, never()).getAttachmentFromBlobStorage(anyString());
        }

        @ParameterizedTest
        @EnumSource(value = ReceiptStatusType.class, names = {
                "NOT_QUEUE_SENT",
                "FAILED"
        })
        void getReceiptPdf_Receipt_KO_RetryableFailure(ReceiptStatusType status) throws ReceiptNotFoundException, FiscalCodeNotAuthorizedException, AttachmentNotFoundException, BlobStorageClientException, CartNotFoundException {
            Receipt receipt = getReceipt(status, ERROR_CODE_RETRYABLE, null);
            when(receiptCosmosClient.getReceiptDocument(GENERIC_EVENT_ID)).thenReturn(receipt);

            InvalidReceiptException exception = assertThrows(
                    InvalidReceiptException.class,
                    () -> sut.getReceiptPdf(THIRD_PARTY_ID_RECEIPT, PAYER_FISCAL_CODE)
            );
            assertEquals(PDFS_715, exception.getErrorCode());

            verify(tokenizerService, never()).getSearchTokenResponse(anyString(), anyString());
            verify(cartReceiptCosmosClient, never()).getCartForReceiptDocument(anyString());
            verify(receiptBlobClient, never()).getAttachmentFromBlobStorage(anyString());
        }

        @ParameterizedTest
        @EnumSource(value = ReceiptStatusType.class, names = {
                "NOT_QUEUE_SENT",
                "FAILED"
        })
        void getReceiptPdf_Receipt_KO_CriticalFailure_Payer(ReceiptStatusType status) throws ReceiptNotFoundException, FiscalCodeNotAuthorizedException, AttachmentNotFoundException, BlobStorageClientException, CartNotFoundException {
            Receipt receipt = getReceipt(status, ERROR_CODE_CRITICAL, null);
            when(receiptCosmosClient.getReceiptDocument(GENERIC_EVENT_ID)).thenReturn(receipt);

            InvalidReceiptException exception = assertThrows(
                    InvalidReceiptException.class,
                    () -> sut.getReceiptPdf(THIRD_PARTY_ID_RECEIPT, PAYER_FISCAL_CODE)
            );
            assertEquals(PDFS_716, exception.getErrorCode());

            verify(tokenizerService, never()).getSearchTokenResponse(anyString(), anyString());
            verify(cartReceiptCosmosClient, never()).getCartForReceiptDocument(anyString());
            verify(receiptBlobClient, never()).getAttachmentFromBlobStorage(anyString());
        }

        @ParameterizedTest
        @EnumSource(value = ReceiptStatusType.class, names = {
                "NOT_QUEUE_SENT",
                "FAILED"
        })
        void getReceiptPdf_Receipt_KO_CriticalFailure_Debtor(ReceiptStatusType status) throws ReceiptNotFoundException, FiscalCodeNotAuthorizedException, AttachmentNotFoundException, BlobStorageClientException, CartNotFoundException {
            Receipt receipt = getReceipt(status, null, ERROR_CODE_CRITICAL);
            when(receiptCosmosClient.getReceiptDocument(GENERIC_EVENT_ID)).thenReturn(receipt);

            InvalidReceiptException exception = assertThrows(
                    InvalidReceiptException.class,
                    () -> sut.getReceiptPdf(THIRD_PARTY_ID_RECEIPT, DEBTOR_FISCAL_CODE_1)
            );
            assertEquals(PDFS_716, exception.getErrorCode());

            verify(tokenizerService, never()).getSearchTokenResponse(anyString(), anyString());
            verify(cartReceiptCosmosClient, never()).getCartForReceiptDocument(anyString());
            verify(receiptBlobClient, never()).getAttachmentFromBlobStorage(anyString());
        }

        @ParameterizedTest
        @EnumSource(value = ReceiptStatusType.class, names = {
                "NOT_QUEUE_SENT",
                "FAILED"
        })
        void getReceiptPdf_Receipt_KO_OneCriticalFailure_OneRetryableFailure(ReceiptStatusType status) throws ReceiptNotFoundException, FiscalCodeNotAuthorizedException, AttachmentNotFoundException, BlobStorageClientException, CartNotFoundException {
            Receipt receipt = getReceipt(status, ERROR_CODE_RETRYABLE, ERROR_CODE_CRITICAL);
            when(receiptCosmosClient.getReceiptDocument(GENERIC_EVENT_ID)).thenReturn(receipt);

            InvalidReceiptException exception = assertThrows(
                    InvalidReceiptException.class,
                    () -> sut.getReceiptPdf(THIRD_PARTY_ID_RECEIPT, DEBTOR_FISCAL_CODE_1)
            );
            assertEquals(PDFS_716, exception.getErrorCode());

            verify(tokenizerService, never()).getSearchTokenResponse(anyString(), anyString());
            verify(cartReceiptCosmosClient, never()).getCartForReceiptDocument(anyString());
            verify(receiptBlobClient, never()).getAttachmentFromBlobStorage(anyString());
        }

        @ParameterizedTest
        @EnumSource(value = ReceiptStatusType.class, names = {
                "NOT_QUEUE_SENT",
                "FAILED"
        })
        void getReceiptPdf_Receipt_KO_CriticalFailure_EmptyReasonErr(ReceiptStatusType status) throws ReceiptNotFoundException, FiscalCodeNotAuthorizedException, AttachmentNotFoundException, BlobStorageClientException, CartNotFoundException {
            Receipt receipt = getReceipt(status, null, null);
            when(receiptCosmosClient.getReceiptDocument(GENERIC_EVENT_ID)).thenReturn(receipt);

            InvalidReceiptException exception = assertThrows(
                    InvalidReceiptException.class,
                    () -> sut.getReceiptPdf(THIRD_PARTY_ID_RECEIPT, DEBTOR_FISCAL_CODE_1)
            );
            assertEquals(PDFS_716, exception.getErrorCode());

            verify(tokenizerService, never()).getSearchTokenResponse(anyString(), anyString());
            verify(cartReceiptCosmosClient, never()).getCartForReceiptDocument(anyString());
            verify(receiptBlobClient, never()).getAttachmentFromBlobStorage(anyString());
        }

        @Test
        void getReceiptPdf_Receipt_Debtor_KO_TokenizedFiscalCodeNotFound() throws ReceiptNotFoundException, FiscalCodeNotAuthorizedException, AttachmentNotFoundException, BlobStorageClientException, CartNotFoundException {
            Receipt receipt = getReceipt(ReceiptStatusType.IO_NOTIFIED, null, null);
            when(receiptCosmosClient.getReceiptDocument(GENERIC_EVENT_ID)).thenReturn(receipt);

            doThrow(new FiscalCodeNotAuthorizedException(AppErrorCodeEnum.PDFS_700, "")).when(tokenizerService).getSearchTokenResponse(GENERIC_EVENT_ID, PAYER_FISCAL_CODE);

            FiscalCodeNotAuthorizedException exception = assertThrows(
                    FiscalCodeNotAuthorizedException.class,
                    () -> sut.getReceiptPdf(THIRD_PARTY_ID_RECEIPT, PAYER_FISCAL_CODE)
            );
            assertEquals(PDFS_700, exception.getErrorCode());

            verify(cartReceiptCosmosClient, never()).getCartForReceiptDocument(anyString());
            verify(receiptBlobClient, never()).getAttachmentFromBlobStorage(anyString());
        }

        @Test
        void getReceiptPdf_Receipt_KO_FiscalCodeNotFoundInReceipt() throws ReceiptNotFoundException, FiscalCodeNotAuthorizedException, AttachmentNotFoundException, BlobStorageClientException, CartNotFoundException {
            Receipt receipt = getReceipt(ReceiptStatusType.IO_NOTIFIED, null, null);
            when(receiptCosmosClient.getReceiptDocument(GENERIC_EVENT_ID)).thenReturn(receipt);

            SearchTokenResponse searchTokenResponse = new SearchTokenResponse();
            searchTokenResponse.setToken(INVALID_FISCAL_CODE_TOKENIZED);
            when(tokenizerService.getSearchTokenResponse(GENERIC_EVENT_ID, INVALID_FISCAL_CODE)).thenReturn(searchTokenResponse);

            FiscalCodeNotAuthorizedException exception = assertThrows(
                    FiscalCodeNotAuthorizedException.class,
                    () -> sut.getReceiptPdf(THIRD_PARTY_ID_RECEIPT, INVALID_FISCAL_CODE)
            );
            assertEquals(PDFS_706, exception.getErrorCode());

            verify(cartReceiptCosmosClient, never()).getCartForReceiptDocument(anyString());
            verify(receiptBlobClient, never()).getAttachmentFromBlobStorage(anyString());
        }

        @Test
        void getReceiptPdf_Receipt_Payer_KO_MdAttachPayerNull() throws ReceiptNotFoundException, FiscalCodeNotAuthorizedException, AttachmentNotFoundException, BlobStorageClientException, CartNotFoundException {
            Receipt receipt = getReceipt(ReceiptStatusType.IO_NOTIFIED, null, null);
            receipt.setMdAttachPayer(null);
            when(receiptCosmosClient.getReceiptDocument(GENERIC_EVENT_ID)).thenReturn(receipt);

            SearchTokenResponse searchTokenResponse = new SearchTokenResponse();
            searchTokenResponse.setToken(PAYER_FISCAL_CODE_TOKENIZED);
            when(tokenizerService.getSearchTokenResponse(GENERIC_EVENT_ID, PAYER_FISCAL_CODE)).thenReturn(searchTokenResponse);

            InvalidCartException exception = assertThrows(
                    InvalidCartException.class,
                    () -> sut.getReceiptPdf(THIRD_PARTY_ID_RECEIPT, PAYER_FISCAL_CODE)
            );
            assertEquals(PDFS_716, exception.getErrorCode());

            verify(cartReceiptCosmosClient, never()).getCartForReceiptDocument(anyString());
            verify(receiptBlobClient, never()).getAttachmentFromBlobStorage(anyString());
        }

        @Test
        void getReceiptPdf_Receipt_Debtor_KO_MdAttachNull() throws ReceiptNotFoundException, FiscalCodeNotAuthorizedException, AttachmentNotFoundException, BlobStorageClientException, CartNotFoundException {
            Receipt receipt = getReceipt(ReceiptStatusType.IO_NOTIFIED, null, null);
            receipt.setMdAttach(null);
            when(receiptCosmosClient.getReceiptDocument(GENERIC_EVENT_ID)).thenReturn(receipt);

            SearchTokenResponse searchTokenResponse = new SearchTokenResponse();
            searchTokenResponse.setToken(DEBTOR_FISCAL_CODE_1_TOKENIZED);
            when(tokenizerService.getSearchTokenResponse(GENERIC_EVENT_ID, DEBTOR_FISCAL_CODE_1)).thenReturn(searchTokenResponse);

            InvalidCartException exception = assertThrows(
                    InvalidCartException.class,
                    () -> sut.getReceiptPdf(THIRD_PARTY_ID_RECEIPT, DEBTOR_FISCAL_CODE_1)
            );
            assertEquals(PDFS_716, exception.getErrorCode());

            verify(cartReceiptCosmosClient, never()).getCartForReceiptDocument(anyString());
            verify(receiptBlobClient, never()).getAttachmentFromBlobStorage(anyString());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void getReceiptPdf_Receipt_Payer_KO_AttachmentNameInvalid(String attachmentName) throws ReceiptNotFoundException, FiscalCodeNotAuthorizedException, AttachmentNotFoundException, BlobStorageClientException, CartNotFoundException {
            Receipt receipt = getReceipt(ReceiptStatusType.IO_NOTIFIED, null, null);
            receipt.setMdAttach(ReceiptMetadata.builder().name(attachmentName).build());
            when(receiptCosmosClient.getReceiptDocument(GENERIC_EVENT_ID)).thenReturn(receipt);

            SearchTokenResponse searchTokenResponse = new SearchTokenResponse();
            searchTokenResponse.setToken(DEBTOR_FISCAL_CODE_1_TOKENIZED);
            when(tokenizerService.getSearchTokenResponse(GENERIC_EVENT_ID, DEBTOR_FISCAL_CODE_1)).thenReturn(searchTokenResponse);

            AttachmentNotFoundException exception = assertThrows(
                    AttachmentNotFoundException.class,
                    () -> sut.getReceiptPdf(THIRD_PARTY_ID_RECEIPT, DEBTOR_FISCAL_CODE_1)
            );
            assertEquals(PDFS_716, exception.getErrorCode());

            verify(cartReceiptCosmosClient, never()).getCartForReceiptDocument(anyString());
            verify(receiptBlobClient, never()).getAttachmentFromBlobStorage(anyString());
        }

        private Receipt getReceipt(ReceiptStatusType receiptStatusType, Integer errorCodePayer, Integer errorCodeDebtor) {
            return Receipt.builder()
                    .eventId(GENERIC_EVENT_ID)
                    .eventData(EventData.builder().payerFiscalCode(PAYER_FISCAL_CODE_TOKENIZED).debtorFiscalCode(DEBTOR_FISCAL_CODE_1_TOKENIZED).build())
                    .mdAttachPayer(ReceiptMetadata.builder().name(ATTACHMENT_NAME_PAYER).build())
                    .mdAttach(ReceiptMetadata.builder().name(ATTACHMENT_NAME_DEBTOR_1).build())
                    .reasonErrPayer(errorCodePayer != null ? ReasonError.builder().code(errorCodePayer).build() : null)
                    .reasonErr(errorCodeDebtor != null ? ReasonError.builder().code(errorCodeDebtor).build() : null)
                    .status(receiptStatusType)
                    .build();
        }
    }

}
