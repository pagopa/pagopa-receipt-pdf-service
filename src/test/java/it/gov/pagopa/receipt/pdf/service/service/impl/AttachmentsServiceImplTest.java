package it.gov.pagopa.receipt.pdf.service.service.impl;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import it.gov.pagopa.receipt.pdf.service.client.PDVTokenizerClient;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptBlobClient;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.FiscalCodeNotAuthorizedException;
import it.gov.pagopa.receipt.pdf.service.exception.InvalidReceiptException;
import it.gov.pagopa.receipt.pdf.service.model.Attachment;
import it.gov.pagopa.receipt.pdf.service.model.AttachmentsDetailsResponse;
import it.gov.pagopa.receipt.pdf.service.model.SearchTokenRequest;
import it.gov.pagopa.receipt.pdf.service.model.SearchTokenResponse;
import it.gov.pagopa.receipt.pdf.service.model.receipt.EventData;
import it.gov.pagopa.receipt.pdf.service.model.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.service.model.receipt.ReceiptMetadata;
import it.gov.pagopa.receipt.pdf.service.service.AttachmentsService;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.Mockito;

import java.io.File;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class AttachmentsServiceImplTest {

    private static final String FISCAL_CODE_A = "AAAAAAAAAAAAAAAA";
    private static final String FISCAL_CODE_B = "BBBBBBBBBBBBBBBB";

    private static final String TOKEN_A = "TOKEN_A";
    private static final String TOKEN_B = "TOKEN_B";

    private static final String MISSING_FISCAL_CODE = "MISSING_FISCAL_CODE";

    @InjectMock(convertScopes = true)
    private ReceiptCosmosClient cosmosClientMock;

    @InjectMock(convertScopes = true)
    private ReceiptBlobClient receiptBlobClientMock;

    @InjectMock(convertScopes = true)
    @RestClient
    private PDVTokenizerClient restClientMock;

    @Inject
    private AttachmentsService sut;

    @BeforeEach
    public void init() {
      Mockito.reset(cosmosClientMock, receiptBlobClientMock, restClientMock);

      when(restClientMock.searchToken(
          eq(new SearchTokenRequest(FISCAL_CODE_A))))
          .thenReturn(new SearchTokenResponse(TOKEN_A));
      when(restClientMock.searchToken(
          eq(new SearchTokenRequest(FISCAL_CODE_B))))
          .thenReturn(new SearchTokenResponse(TOKEN_B));
      when(restClientMock.searchToken(
          eq(new SearchTokenRequest(MISSING_FISCAL_CODE))))
          .thenThrow(new RuntimeException());

    }


    @Test
    @SneakyThrows
    void getAttachmentDetailsSuccessWithDifferentPayerDebtor() {
        String id = UUID.randomUUID().toString();
        String fileNameDebtor = "file1.pdf";
        String fileNamePayer = "file2.pdf";
        Receipt receipt = buildReceiptWithDifferentPayerDebtor(id, fileNameDebtor, fileNamePayer);

        doReturn(receipt).when(cosmosClientMock).getReceiptDocument(anyString());

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

        doReturn(receipt).when(cosmosClientMock).getReceiptDocument(anyString());

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
        doReturn(null).when(cosmosClientMock).getReceiptDocument(anyString());

        InvalidReceiptException e = assertThrows(InvalidReceiptException.class, () -> sut.getAttachmentsDetails(anyString(), FISCAL_CODE_A));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_701, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void getAttachmentDetailsFailEventDataNull() {
        doReturn(Receipt.builder().numRetry(0).build()).when(cosmosClientMock).getReceiptDocument(anyString());

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

        doReturn(receipt).when(cosmosClientMock).getReceiptDocument(anyString());

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

        doReturn(receipt).when(cosmosClientMock).getReceiptDocument(anyString());

        InvalidReceiptException e = assertThrows(InvalidReceiptException.class, () -> sut.getAttachmentsDetails(anyString(), FISCAL_CODE_A));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_704, e.getErrorCode());
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

        doReturn(receipt).when(cosmosClientMock).getReceiptDocument(anyString());

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

        doReturn(receipt).when(cosmosClientMock).getReceiptDocument(anyString());

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

        doReturn(receipt).when(cosmosClientMock).getReceiptDocument(anyString());
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

        doReturn(receipt).when(cosmosClientMock).getReceiptDocument(anyString());
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

        doReturn(receipt).when(cosmosClientMock).getReceiptDocument(anyString());
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

        doReturn(receipt).when(cosmosClientMock).getReceiptDocument(anyString());
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

        doReturn(receipt).when(cosmosClientMock).getReceiptDocument(anyString());
        doReturn(mock(File.class)).when(receiptBlobClientMock).getAttachmentFromBlobStorage(anyString());

        FiscalCodeNotAuthorizedException e = assertThrows(
                FiscalCodeNotAuthorizedException.class,
                () -> sut.getAttachment(anyString(), FISCAL_CODE_A, UUID.randomUUID().toString()));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_706, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void getAttachmentFailWithDifferentPayerDebtorDebtorRequestNotExistingReceipt() {
        String id = UUID.randomUUID().toString();
        String fileNameDebtor = "file1.pdf";
        String fileNamePayer = "file2.pdf";
        Receipt receipt = buildReceiptWithDifferentPayerDebtor(id, fileNameDebtor, fileNamePayer);

        doReturn(receipt).when(cosmosClientMock).getReceiptDocument(anyString());
        doReturn(mock(File.class)).when(receiptBlobClientMock).getAttachmentFromBlobStorage(anyString());

        FiscalCodeNotAuthorizedException e = assertThrows(
                FiscalCodeNotAuthorizedException.class,
                () -> sut.getAttachment(anyString(), FISCAL_CODE_A, fileNamePayer));

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

    doReturn(receipt).when(cosmosClientMock).getReceiptDocument(anyString());

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

    doReturn(receipt).when(cosmosClientMock).getReceiptDocument(anyString());
    doReturn(mock(File.class)).when(receiptBlobClientMock).getAttachmentFromBlobStorage(anyString());

    assertThrows(
        FiscalCodeNotAuthorizedException.class,
        () -> sut.getAttachment(anyString(), MISSING_FISCAL_CODE, fileNameDebtor));

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

}
