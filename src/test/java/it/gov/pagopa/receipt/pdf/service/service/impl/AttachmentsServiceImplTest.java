package it.gov.pagopa.receipt.pdf.service.service.impl;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import it.gov.pagopa.receipt.pdf.service.client.CartReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.client.PDVTokenizerClient;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptBlobClient;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.FiscalCodeNotAuthorizedException;
import it.gov.pagopa.receipt.pdf.service.exception.InvalidCartException;
import it.gov.pagopa.receipt.pdf.service.exception.InvalidReceiptException;
import it.gov.pagopa.receipt.pdf.service.model.Attachment;
import it.gov.pagopa.receipt.pdf.service.model.AttachmentsDetailsResponse;
import it.gov.pagopa.receipt.pdf.service.model.SearchTokenRequest;
import it.gov.pagopa.receipt.pdf.service.model.SearchTokenResponse;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartPayment;
import it.gov.pagopa.receipt.pdf.service.model.cart.MessageData;
import it.gov.pagopa.receipt.pdf.service.model.cart.Payload;
import it.gov.pagopa.receipt.pdf.service.model.receipt.EventData;
import it.gov.pagopa.receipt.pdf.service.model.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.service.model.receipt.ReceiptMetadata;
import it.gov.pagopa.receipt.pdf.service.service.AttachmentsService;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
class AttachmentsServiceImplTest {

    private static final String FISCAL_CODE_A = "AAAAAAAAAAAAAAAA";
    private static final String FISCAL_CODE_B = "BBBBBBBBBBBBBBBB";

    private static final String TOKEN_A = "TOKEN_A";
    private static final String TOKEN_B = "TOKEN_B";

    private static final String MISSING_FISCAL_CODE = "MISSING_FISCAL_CODE";

    @InjectMock
    private ReceiptCosmosClient cosmosClientReceiptsMock;

    @InjectMock
    private CartReceiptCosmosClient cosmosClientCartMock;

    @InjectMock
    private ReceiptBlobClient receiptBlobClientMock;

    @InjectMock
    @RestClient
    private PDVTokenizerClient restClientMock;

    @Inject
    private AttachmentsService sut;

    @BeforeEach
    void init() {
        Mockito.reset(cosmosClientReceiptsMock, receiptBlobClientMock, restClientMock);

        when(restClientMock.searchToken(
                new SearchTokenRequest(FISCAL_CODE_A)))
                .thenReturn(new SearchTokenResponse(TOKEN_A));
        when(restClientMock.searchToken(
                new SearchTokenRequest(FISCAL_CODE_B)))
                .thenReturn(new SearchTokenResponse(TOKEN_B));
        when(restClientMock.searchToken(
                new SearchTokenRequest(MISSING_FISCAL_CODE)))
                .thenThrow(new RuntimeException());

    }


    @Test
    @SneakyThrows
    void getAttachmentDetailsSuccessWithDifferentPayerDebtor() {
        String id = UUID.randomUUID().toString();
        String fileNameDebtor = "file1.pdf";
        String fileNamePayer = "file2.pdf";
        Receipt receipt = buildReceiptWithDifferentPayerDebtor(id, fileNameDebtor, fileNamePayer);

        doReturn(receipt).when(cosmosClientReceiptsMock).getReceiptDocument(anyString());

        AttachmentsDetailsResponse result = sut.getAttachmentsDetails(anyString(), FISCAL_CODE_B);

        assertNotNull(result);
        assertNotNull(result.getAttachments());
        assertEquals(1, result.getAttachments().size());
        assertNotNull(result.getAttachments().get(0));
        Attachment attachment = result.getAttachments().get(0);
        assertEquals(id, attachment.getId());
        assertEquals("application/pdf", attachment.getContentType());
        assertEquals(fileNamePayer, attachment.getUrl());
        assertEquals(fileNamePayer, attachment.getName());
    }

    @Test
    @SneakyThrows
    void getAttachmentDetailsSuccessWithSamePayerDebtor() {
        String id = UUID.randomUUID().toString();
        String fileNameDebtor = "file1.pdf";
        Receipt receipt = buildReceiptWithSamePayerDebtor(id, fileNameDebtor);

        doReturn(receipt).when(cosmosClientReceiptsMock).getReceiptDocument(anyString());

        AttachmentsDetailsResponse result = sut.getAttachmentsDetails(anyString(), FISCAL_CODE_A);

        assertNotNull(result);
        assertNotNull(result.getAttachments());
        assertEquals(1, result.getAttachments().size());
        assertNotNull(result.getAttachments().get(0));
        Attachment attachment = result.getAttachments().get(0);
        assertEquals(id, attachment.getId());
        assertEquals("application/pdf", attachment.getContentType());
        assertEquals(fileNameDebtor, attachment.getUrl());
        assertEquals(fileNameDebtor, attachment.getName());
    }

    @Test
    @SneakyThrows
    void getAttachmentDetailsFailReceiptNull() {
        doReturn(null).when(cosmosClientReceiptsMock).getReceiptDocument(anyString());

        InvalidReceiptException e = assertThrows(InvalidReceiptException.class, () -> sut.getAttachmentsDetails(anyString(), FISCAL_CODE_A));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_701, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void getAttachmentDetailsFailEventDataNull() {
        doReturn(Receipt.builder().numRetry(0).build()).when(cosmosClientReceiptsMock).getReceiptDocument(anyString());

        InvalidReceiptException e = assertThrows(InvalidReceiptException.class, () -> sut.getAttachmentsDetails(anyString(), FISCAL_CODE_A));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_702, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void getAttachmentDetailsFailDebtorFiscalCodeNull() {
        Receipt receipt = Receipt.builder()
                .eventData(
                        EventData.builder()
                                .payerFiscalCode(UUID.randomUUID().toString())
                                .build()
                )
                .numRetry(0)
                .build();

        doReturn(receipt).when(cosmosClientReceiptsMock).getReceiptDocument(anyString());

        InvalidReceiptException e = assertThrows(InvalidReceiptException.class, () -> sut.getAttachmentsDetails(anyString(), FISCAL_CODE_A));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_703, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void getAttachmentDetailsFailMdAttachNull() {
        Receipt receipt = Receipt.builder()
                .eventData(
                        EventData.builder()
                                .debtorFiscalCode(UUID.randomUUID().toString())
                                .payerFiscalCode(UUID.randomUUID().toString())
                                .build()
                )
                .numRetry(0)
                .build();

        doReturn(receipt).when(cosmosClientReceiptsMock).getReceiptDocument(anyString());

        InvalidReceiptException e = assertThrows(InvalidReceiptException.class, () -> sut.getAttachmentsDetails(anyString(), FISCAL_CODE_A));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_704, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void getAttachmentDetailsFailMdAttachOnAnonym() {
        String id = UUID.randomUUID().toString();
        String fileNameDebtor = "file1.pdf";
        String urlDebtor = "file/file1";
        Receipt receipt = Receipt.builder()
                .id(id)
                .eventData(
                        EventData.builder()
                                .debtorFiscalCode("ANONIMO")
                                .payerFiscalCode(UUID.randomUUID().toString())
                                .build()
                )
                .mdAttach(
                        ReceiptMetadata.builder()
                                .name(fileNameDebtor)
                                .url(urlDebtor)
                                .build()
                )
                .numRetry(0)
                .build();

        doReturn(receipt).when(cosmosClientReceiptsMock).getReceiptDocument(anyString());

        InvalidReceiptException e = assertThrows(InvalidReceiptException.class, () -> sut.getAttachmentsDetails(anyString(), FISCAL_CODE_A));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_705, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void getAttachmentDetailsFailMdAttachPayerNull() {
        Receipt receipt = Receipt.builder()
                .eventData(
                        EventData.builder()
                                .debtorFiscalCode(UUID.randomUUID().toString())
                                .payerFiscalCode(UUID.randomUUID().toString())
                                .build()
                )
                .mdAttach(ReceiptMetadata.builder().build())
                .numRetry(0)
                .build();

        doReturn(receipt).when(cosmosClientReceiptsMock).getReceiptDocument(anyString());

        InvalidReceiptException e = assertThrows(InvalidReceiptException.class, () -> sut.getAttachmentsDetails(anyString(), FISCAL_CODE_A));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_705, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void getAttachmentDetailsFailFiscalCodeNotAuthorized() {
        String id = UUID.randomUUID().toString();
        String fileNameDebtor = "file1.pdf";
        String urlDebtor = "file/file1";
        Receipt receipt = Receipt.builder()
                .id(id)
                .eventData(
                        EventData.builder()
                                .debtorFiscalCode(FISCAL_CODE_A)
                                .payerFiscalCode(FISCAL_CODE_A)
                                .build()
                )
                .mdAttach(
                        ReceiptMetadata.builder()
                                .name(fileNameDebtor)
                                .url(urlDebtor)
                                .build()
                )
                .numRetry(0)
                .build();

        doReturn(receipt).when(cosmosClientReceiptsMock).getReceiptDocument(anyString());

        FiscalCodeNotAuthorizedException e = assertThrows(FiscalCodeNotAuthorizedException.class, () -> sut.getAttachmentsDetails(anyString(), FISCAL_CODE_B));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_700, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void getAttachmentSuccessWithDifferentPayerDebtorPayerRequestPayerReceipt() {
        String id = UUID.randomUUID().toString();
        String fileNameDebtor = "file1.pdf";
        String fileNamePayer = "file2.pdf";
        Receipt receipt = buildReceiptWithDifferentPayerDebtor(id, fileNameDebtor, fileNamePayer);

        doReturn(receipt).when(cosmosClientReceiptsMock).getReceiptDocument(anyString());
        doReturn(mock(File.class)).when(receiptBlobClientMock).getAttachmentFromBlobStorage(anyString());

        File result = sut.getAttachment(anyString(), FISCAL_CODE_B, fileNamePayer);

        assertNotNull(result);
    }

    @Test
    @SneakyThrows
    void getAttachmentSuccessWithSamePayerDebtor() {
        String id = UUID.randomUUID().toString();
        String fileNameDebtor = "file1.pdf";
        Receipt receipt = buildReceiptWithSamePayerDebtor(id, fileNameDebtor);

        doReturn(receipt).when(cosmosClientReceiptsMock).getReceiptDocument(anyString());
        doReturn(mock(File.class)).when(receiptBlobClientMock).getAttachmentFromBlobStorage(anyString());

        File result = sut.getAttachment(anyString(), FISCAL_CODE_A, fileNameDebtor);

        assertNotNull(result);
    }

    @Test
    @SneakyThrows
    void getAttachmentFailWithDifferentPayerDebtorPayerRequestDebtorReceipt() {
        String id = UUID.randomUUID().toString();
        String fileNameDebtor = "file1.pdf";
        String fileNamePayer = "file2.pdf";
        Receipt receipt = buildReceiptWithDifferentPayerDebtor(id, fileNameDebtor, fileNamePayer);

        doReturn(receipt).when(cosmosClientReceiptsMock).getReceiptDocument(anyString());
        doReturn(mock(File.class)).when(receiptBlobClientMock).getAttachmentFromBlobStorage(anyString());

        FiscalCodeNotAuthorizedException e = assertThrows(
                FiscalCodeNotAuthorizedException.class,
                () -> sut.getAttachment(anyString(), FISCAL_CODE_B, fileNameDebtor));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_706, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void getAttachmentFailWithDifferentPayerDebtorDebtorRequestPayerReceipt() {
        String id = UUID.randomUUID().toString();
        String fileNameDebtor = "file1.pdf";
        String fileNamePayer = "file2.pdf";
        Receipt receipt = buildReceiptWithDifferentPayerDebtor(id, fileNameDebtor, fileNamePayer);

        doReturn(receipt).when(cosmosClientReceiptsMock).getReceiptDocument(anyString());
        doReturn(mock(File.class)).when(receiptBlobClientMock).getAttachmentFromBlobStorage(anyString());

        FiscalCodeNotAuthorizedException e = assertThrows(
                FiscalCodeNotAuthorizedException.class,
                () -> sut.getAttachment(anyString(), FISCAL_CODE_A, fileNamePayer));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_706, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void getAttachmentFailWithSamePayerDebtorDebtorRequestNotExistingReceipt() {
        String id = UUID.randomUUID().toString();
        String fileNameDebtor = "file1.pdf";
        Receipt receipt = buildReceiptWithSamePayerDebtor(id, fileNameDebtor);

        doReturn(receipt).when(cosmosClientReceiptsMock).getReceiptDocument(anyString());
        doReturn(mock(File.class)).when(receiptBlobClientMock).getAttachmentFromBlobStorage(anyString());

        FiscalCodeNotAuthorizedException e = assertThrows(
                FiscalCodeNotAuthorizedException.class,
                () -> sut.getAttachment(anyString(), FISCAL_CODE_A, UUID.randomUUID().toString()));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_706, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void getAttachmentDetailsFailWithMissingToken() {
        String id = UUID.randomUUID().toString();
        String fileNameDebtor = "file1.pdf";
        String fileNamePayer = "file2.pdf";
        Receipt receipt = buildReceiptWithDifferentPayerDebtor(id, fileNameDebtor, fileNamePayer);

        doReturn(receipt).when(cosmosClientReceiptsMock).getReceiptDocument(anyString());

        assertThrows(
                FiscalCodeNotAuthorizedException.class,
                () -> sut.getAttachmentsDetails(anyString(), MISSING_FISCAL_CODE));
    }

    @Test
    @SneakyThrows
    void getAttachmentFailWithMissingToken() {
        String id = UUID.randomUUID().toString();
        String fileNameDebtor = "file1.pdf";
        Receipt receipt = buildReceiptWithSamePayerDebtor(id, fileNameDebtor);

        doReturn(receipt).when(cosmosClientReceiptsMock).getReceiptDocument(anyString());
        doReturn(mock(File.class)).when(receiptBlobClientMock).getAttachmentFromBlobStorage(anyString());

        assertThrows(
                FiscalCodeNotAuthorizedException.class,
                () -> sut.getAttachment(anyString(), MISSING_FISCAL_CODE, fileNameDebtor));

    }

    @Test
    @SneakyThrows
    void getAttachmentCartPayerSuccess() {
        String fileNameDebtor = "file1.pdf";
        String fileNamePayer = "file2.pdf";
        String payerFiscalCode = "12345";
        CartForReceipt cart = CartForReceipt.builder()
                .payload(Payload.builder()
                        .payerFiscalCode(TOKEN_A)
                        .mdAttachPayer(ReceiptMetadata.builder()
                                .name(fileNamePayer)
                                .build())
                        .cart(List.of(CartPayment.builder()
                                .debtorFiscalCode("98765")
                                .mdAttach(ReceiptMetadata.builder()
                                        .name(fileNameDebtor)
                                        .build())
                                .build()))
                        .build())
                .build();

        doReturn(cart).when(cosmosClientCartMock).getCartForReceiptDocument(anyString());

        when(restClientMock.searchToken(
                new SearchTokenRequest(payerFiscalCode)))
                .thenReturn(new SearchTokenResponse(TOKEN_A));
        doReturn(mock(File.class)).when(receiptBlobClientMock).getAttachmentFromBlobStorage(anyString());


        File result = sut.getAttachment("test_CART_", payerFiscalCode, fileNamePayer);

        assertNotNull(result);
    }

    @Test
    @SneakyThrows
    void getAttachmentCartDebtorSuccess() {
        String fileNameDebtor = "file1.pdf";
        String fileNamePayer = "file2.pdf";
        String debtorFiscalCode = "12345";
        CartForReceipt cart = CartForReceipt.builder()
                .payload(Payload.builder()
                        .payerFiscalCode(TOKEN_A)
                        .mdAttachPayer(ReceiptMetadata.builder()
                                .name(fileNamePayer)
                                .build())
                        .cart(List.of(CartPayment.builder()
                                .bizEventId("biz1")
                                .debtorFiscalCode(TOKEN_B)
                                .mdAttach(ReceiptMetadata.builder()
                                        .name(fileNameDebtor)
                                        .build())
                                .build()))
                        .build())
                .build();

        doReturn(cart).when(cosmosClientCartMock).getCartForReceiptDocument(anyString());

        when(restClientMock.searchToken(
                new SearchTokenRequest(debtorFiscalCode)))
                .thenReturn(new SearchTokenResponse(TOKEN_B));
        doReturn(mock(File.class)).when(receiptBlobClientMock).getAttachmentFromBlobStorage(anyString());


        File result = sut.getAttachment("test_CART_biz1", debtorFiscalCode, fileNameDebtor);

        assertNotNull(result);
    }


    @Test
    @SneakyThrows
    void getAttachmentCartNull() {
        String fileNamePayer = "file2.pdf";
        String payerFiscalCode = "12345";
        CartForReceipt cart = null;

        doReturn(cart).when(cosmosClientCartMock).getCartForReceiptDocument(anyString());

        when(restClientMock.searchToken(
                new SearchTokenRequest(payerFiscalCode)))
                .thenReturn(new SearchTokenResponse(TOKEN_A));
        doReturn(mock(File.class)).when(receiptBlobClientMock).getAttachmentFromBlobStorage(anyString());


        InvalidCartException e = assertThrows(
                InvalidCartException.class,
                () -> sut.getAttachment("test_CART_", payerFiscalCode, fileNamePayer));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_707, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void getAttachmentCarPayloadNull() {
        String fileNamePayer = "file2.pdf";
        String payerFiscalCode = "12345";
        CartForReceipt cart = CartForReceipt.builder()
                .payload(null)
                .build();

        doReturn(cart).when(cosmosClientCartMock).getCartForReceiptDocument(anyString());

        when(restClientMock.searchToken(
                new SearchTokenRequest(payerFiscalCode)))
                .thenReturn(new SearchTokenResponse(TOKEN_A));
        doReturn(mock(File.class)).when(receiptBlobClientMock).getAttachmentFromBlobStorage(anyString());


        InvalidCartException e = assertThrows(
                InvalidCartException.class,
                () -> sut.getAttachment("test_CART_", payerFiscalCode, fileNamePayer));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_708, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void getAttachmentCarListNull() {
        String fileNamePayer = "file2.pdf";
        String payerFiscalCode = "12345";
        CartForReceipt cart = CartForReceipt.builder()
                .payload(Payload.builder()
                        .cart(null)
                        .build())
                .build();

        doReturn(cart).when(cosmosClientCartMock).getCartForReceiptDocument(anyString());

        when(restClientMock.searchToken(
                new SearchTokenRequest(payerFiscalCode)))
                .thenReturn(new SearchTokenResponse(TOKEN_A));
        doReturn(mock(File.class)).when(receiptBlobClientMock).getAttachmentFromBlobStorage(anyString());


        InvalidCartException e = assertThrows(
                InvalidCartException.class,
                () -> sut.getAttachment("test_CART_", payerFiscalCode, fileNamePayer));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_708, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void getAttachmentCarDebtorNull() {
        String fileNamePayer = "file2.pdf";
        String payerFiscalCode = "12345";
        CartForReceipt cart = CartForReceipt.builder()
                .payload(Payload.builder()
                        .cart(List.of(CartPayment.builder()
                                .debtorFiscalCode(null)
                                .build()))
                        .build())
                .build();

        doReturn(cart).when(cosmosClientCartMock).getCartForReceiptDocument(anyString());

        when(restClientMock.searchToken(
                new SearchTokenRequest(payerFiscalCode)))
                .thenReturn(new SearchTokenResponse(TOKEN_A));
        doReturn(mock(File.class)).when(receiptBlobClientMock).getAttachmentFromBlobStorage(anyString());


        InvalidCartException e = assertThrows(
                InvalidCartException.class,
                () -> sut.getAttachment("test_CART_", payerFiscalCode, fileNamePayer));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_709, e.getErrorCode());
    }


    @Test
    @SneakyThrows
    void getAttachmentCartPayerUnauthorized() {
        String fileNameDebtor = "file1.pdf";
        String fileNamePayer = "file2.pdf";
        String payerFiscalCode = "12345";
        CartForReceipt cart = CartForReceipt.builder()
                .payload(Payload.builder()
                        .payerFiscalCode("wrong cf")
                        .mdAttachPayer(ReceiptMetadata.builder()
                                .name(fileNamePayer)
                                .build())
                        .cart(List.of(CartPayment.builder()
                                .debtorFiscalCode("98765")
                                .mdAttach(ReceiptMetadata.builder()
                                        .name(fileNameDebtor)
                                        .build())
                                .build()))
                        .build())
                .build();

        doReturn(cart).when(cosmosClientCartMock).getCartForReceiptDocument(anyString());

        when(restClientMock.searchToken(
                new SearchTokenRequest(payerFiscalCode)))
                .thenReturn(new SearchTokenResponse(TOKEN_A));
        doReturn(mock(File.class)).when(receiptBlobClientMock).getAttachmentFromBlobStorage(anyString());


        FiscalCodeNotAuthorizedException e = assertThrows(
                FiscalCodeNotAuthorizedException.class,
                () -> sut.getAttachment("test_CART_", payerFiscalCode, fileNamePayer));

        assertNotNull(e);
    }


    @Test
    @SneakyThrows
    void getAttachmentCartDebtorUnauthorized() {
        String fileNameDebtor = "file1.pdf";
        String fileNamePayer = "file2.pdf";
        String payerFiscalCode = "12345";
        CartForReceipt cart = CartForReceipt.builder()
                .payload(Payload.builder()
                        .payerFiscalCode(TOKEN_A)
                        .mdAttachPayer(ReceiptMetadata.builder()
                                .name(fileNamePayer)
                                .build())
                        .cart(List.of(CartPayment.builder()
                                .debtorFiscalCode(TOKEN_B)
                                .mdAttach(ReceiptMetadata.builder()
                                        .name(fileNameDebtor)
                                        .build())
                                .build()))
                        .build())
                .build();

        doReturn(cart).when(cosmosClientCartMock).getCartForReceiptDocument(anyString());

        when(restClientMock.searchToken(
                new SearchTokenRequest(payerFiscalCode)))
                .thenReturn(new SearchTokenResponse(TOKEN_B));
        doReturn(mock(File.class)).when(receiptBlobClientMock).getAttachmentFromBlobStorage(anyString());


        FiscalCodeNotAuthorizedException e = assertThrows(
                FiscalCodeNotAuthorizedException.class,
                () -> sut.getAttachment("test_CART_biz1", "wrong", fileNamePayer));

        assertNotNull(e);
    }

    @Test
    @SneakyThrows
    void getAttachmentCartAttachmentsNull() {
        String fileNamePayer = "file2.pdf";
        String payerFiscalCode = "12345";
        CartForReceipt cart = CartForReceipt.builder()
                .payload(Payload.builder()
                        .payerFiscalCode(TOKEN_A)
                        .mdAttachPayer(null)
                        .cart(List.of(CartPayment.builder()
                                .debtorFiscalCode(TOKEN_B)
                                .mdAttach(null)
                                .build()))
                        .build())
                .build();

        doReturn(cart).when(cosmosClientCartMock).getCartForReceiptDocument(anyString());

        when(restClientMock.searchToken(
                new SearchTokenRequest(payerFiscalCode)))
                .thenReturn(new SearchTokenResponse(TOKEN_B));
        doReturn(mock(File.class)).when(receiptBlobClientMock).getAttachmentFromBlobStorage(anyString());


        InvalidCartException e = assertThrows(
                InvalidCartException.class,
                () -> sut.getAttachment("test_CART_biz1", payerFiscalCode, fileNamePayer));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_710, e.getErrorCode());

    }


    @Test
    @SneakyThrows
    void getAttachmentCartPayerAttachNull() {
        String fileNameDebtor = "file1.pdf";
        String fileNamePayer = "file2.pdf";
        String payerFiscalCode = "12345";
        CartForReceipt cart = CartForReceipt.builder()
                .payload(Payload.builder()
                        .payerFiscalCode(TOKEN_A)
                        .mdAttachPayer(null)
                        .cart(List.of(CartPayment.builder()
                                .debtorFiscalCode(TOKEN_B)
                                .mdAttach(ReceiptMetadata.builder()
                                        .name(fileNameDebtor)
                                        .build())
                                .build()))
                        .build())
                .build();

        doReturn(cart).when(cosmosClientCartMock).getCartForReceiptDocument(anyString());

        when(restClientMock.searchToken(
                new SearchTokenRequest(payerFiscalCode)))
                .thenReturn(new SearchTokenResponse(TOKEN_B));
        doReturn(mock(File.class)).when(receiptBlobClientMock).getAttachmentFromBlobStorage(anyString());


        InvalidCartException e = assertThrows(
                InvalidCartException.class,
                () -> sut.getAttachment("test_CART_biz1", payerFiscalCode, fileNamePayer));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_712, e.getErrorCode());

    }

    private Receipt buildReceiptWithDifferentPayerDebtor(String id, String fileNameDebtor, String fileNamePayer) {
        return Receipt.builder()
                .id(id)
                .eventData(
                        EventData.builder()
                                .debtorFiscalCode(TOKEN_A)
                                .payerFiscalCode(TOKEN_B)
                                .build()
                )
                .mdAttach(
                        ReceiptMetadata.builder()
                                .name(fileNameDebtor)
                                .url("file/" + fileNameDebtor)
                                .build()
                )
                .mdAttachPayer(
                        ReceiptMetadata.builder()
                                .name(fileNamePayer)
                                .url("file/" + fileNamePayer)
                                .build()
                )
                .numRetry(0)
                .build();
    }

    private Receipt buildReceiptWithSamePayerDebtor(String id, String fileNameDebtor) {
        return Receipt.builder()
                .id(id)
                .eventData(
                        EventData.builder()
                                .debtorFiscalCode(TOKEN_A)
                                .payerFiscalCode(TOKEN_A)
                                .build()
                )
                .mdAttach(
                        ReceiptMetadata.builder()
                                .name(fileNameDebtor)
                                .url("file/" + fileNameDebtor)
                                .build()
                )
                .numRetry(0)
                .build();
    }

    @Test
    void getAttachmentsDetails_receipt_success_debtor() throws Exception {
        String id = "id1";
        String fiscalCode = "FCODE";
        String fileName = "file.pdf";
        Receipt receipt = Receipt.builder()
                .id(id)
                .eventData(EventData.builder()
                        .debtorFiscalCode(fiscalCode)
                        .payerFiscalCode(fiscalCode)
                        .build())
                .mdAttach(ReceiptMetadata.builder().name(fileName).build())
                .numRetry(0)
                .build();

        when(cosmosClientReceiptsMock.getReceiptDocument(anyString())).thenReturn(receipt);
        when(restClientMock.searchToken(any())).thenReturn(new SearchTokenResponse(fiscalCode));

        AttachmentsDetailsResponse resp = sut.getAttachmentsDetails("id1", fiscalCode);

        assertNotNull(resp);
        assertEquals(1, resp.getAttachments().size());
        assertEquals(fileName, resp.getAttachments().get(0).getName());
    }

    @Test
    void getAttachmentsDetails_receipt_success_payer() throws Exception {
        String id = "id2";
        String debtor = "DEBTOR";
        String payer = "PAYER";
        String fileNamePayer = "payer.pdf";
        Receipt receipt = Receipt.builder()
                .id(id)
                .eventData(EventData.builder()
                        .debtorFiscalCode(debtor)
                        .payerFiscalCode(payer)
                        .build())
                .mdAttach(ReceiptMetadata.builder().name("debtor.pdf").build())
                .mdAttachPayer(ReceiptMetadata.builder().name(fileNamePayer).build())
                .numRetry(0)
                .build();

        when(cosmosClientReceiptsMock.getReceiptDocument(anyString())).thenReturn(receipt);
        when(restClientMock.searchToken(any())).thenReturn(new SearchTokenResponse(payer));

        AttachmentsDetailsResponse resp = sut.getAttachmentsDetails("id2", payer);

        assertNotNull(resp);
        assertEquals(fileNamePayer, resp.getAttachments().get(0).getName());
    }

    @Test
    @SneakyThrows
    void getAttachmentsDetails_receipt_not_authorized() {
        String id = "id3";
        String debtor = "DEBTOR";
        String payer = "PAYER";
        Receipt receipt = Receipt.builder()
                .id(id)
                .eventData(EventData.builder()
                        .debtorFiscalCode(debtor)
                        .payerFiscalCode(payer)
                        .build())
                .mdAttach(ReceiptMetadata.builder().name("debtor.pdf").build())
                .mdAttachPayer(ReceiptMetadata.builder().name("payer.pdf").build())
                .numRetry(0)
                .build();

        when(cosmosClientReceiptsMock.getReceiptDocument(anyString())).thenReturn(receipt);
        when(restClientMock.searchToken(any())).thenReturn(new SearchTokenResponse("OTHER"));

        FiscalCodeNotAuthorizedException ex = assertThrows(FiscalCodeNotAuthorizedException.class,
                () -> sut.getAttachmentsDetails("id3", "OTHER"));
        assertEquals(AppErrorCodeEnum.PDFS_700, ex.getErrorCode());
    }

    @Test
    @SneakyThrows
    void getAttachmentsDetails_receipt_null() {
        when(cosmosClientReceiptsMock.getReceiptDocument(anyString())).thenReturn(null);
        when(restClientMock.searchToken(any())).thenReturn(new SearchTokenResponse("FCODE"));

        InvalidReceiptException ex = assertThrows(InvalidReceiptException.class,
                () -> sut.getAttachmentsDetails("id4", "FCODE"));
        assertEquals(AppErrorCodeEnum.PDFS_701, ex.getErrorCode());
    }

    @Test
    void getAttachmentsDetails_cart_success_payer() throws Exception {
        String cartId = "cart1";
        String payer = "PAYER";
        String fileName = "payer.pdf";
        String debtor = "DEBTOR";
        String bizId = "BIZ";

        CartPayment payment = CartPayment.builder()
                .bizEventId(bizId)
                .debtorFiscalCode(debtor)
                .mdAttach(ReceiptMetadata.builder().name(fileName).build())
                .messageDebtor(MessageData.builder().subject("subj").markdown("md").build())
                .build();

        CartForReceipt cart = CartForReceipt.builder()
                .id(cartId)
                .payload(Payload.builder()
                        .payerFiscalCode(payer)
                        .mdAttachPayer(ReceiptMetadata.builder().name(fileName).build())
                        .cart(List.of(payment))
                        .messagePayer(MessageData.builder().subject("subj").markdown("md").build())
                        .build())
                .build();

        when(cosmosClientCartMock.getCartForReceiptDocument(anyString())).thenReturn(cart);
        when(restClientMock.searchToken(any())).thenReturn(new SearchTokenResponse(payer));

        AttachmentsDetailsResponse resp = sut.getAttachmentsDetails(cartId + AttachmentsServiceImpl.CART, payer);

        assertNotNull(resp);
        assertEquals(fileName, resp.getAttachments().get(0).getName());
        assertEquals("subj", resp.getDetails().getSubject());
    }

    @Test
    void getAttachmentsDetails_cart_success_debtor() throws Exception {
        String cartId = "cart2";
        String debtor = "DEBTOR";
        String bizId = "BIZ";
        String fileName = "debtor.pdf";
        CartPayment payment = CartPayment.builder()
                .bizEventId(bizId)
                .debtorFiscalCode(debtor)
                .mdAttach(ReceiptMetadata.builder().name(fileName).build())
                .messageDebtor(MessageData.builder().subject("subj").markdown("md").build())
                .build();
        CartForReceipt cart = CartForReceipt.builder()
                .id(cartId)
                .payload(Payload.builder()
                        .payerFiscalCode("PAYER")
                        .mdAttachPayer(ReceiptMetadata.builder().name("receipt.pdf").url("url").build())
                        .cart(List.of(payment))
                        .build())
                .build();

        when(cosmosClientCartMock.getCartForReceiptDocument(anyString())).thenReturn(cart);
        when(restClientMock.searchToken(any())).thenReturn(new SearchTokenResponse(debtor));

        AttachmentsDetailsResponse resp = sut.getAttachmentsDetails(cartId + AttachmentsServiceImpl.CART + bizId, debtor);

        assertNotNull(resp);
        assertEquals(fileName, resp.getAttachments().get(0).getName());
        assertEquals("subj", resp.getDetails().getSubject());
    }

    @Test
    @SneakyThrows
    void getAttachmentsDetails_cart_not_authorized() {
        String cartId = "cart3";
        String bizId = "BIZ";
        CartPayment payment = CartPayment.builder()
                .bizEventId(bizId)
                .debtorFiscalCode("DEBTOR")
                .mdAttach(ReceiptMetadata.builder().name("debtor.pdf").build())
                .messageDebtor(MessageData.builder().subject("subj").markdown("md").build())
                .build();
        CartForReceipt cart = CartForReceipt.builder()
                .id(cartId)
                .payload(Payload.builder()
                        .payerFiscalCode("PAYER")
                        .mdAttachPayer(ReceiptMetadata.builder().name("receipt.pdf").url("url").build())
                        .cart(List.of(payment))
                        .build())
                .build();

        when(cosmosClientCartMock.getCartForReceiptDocument(anyString())).thenReturn(cart);
        when(restClientMock.searchToken(any())).thenReturn(new SearchTokenResponse("OTHER"));

        FiscalCodeNotAuthorizedException ex = assertThrows(FiscalCodeNotAuthorizedException.class,
                () -> sut.getAttachmentsDetails(cartId + AttachmentsServiceImpl.CART + bizId, "OTHER"));
        assertEquals(AppErrorCodeEnum.PDFS_700, ex.getErrorCode());
    }

}


