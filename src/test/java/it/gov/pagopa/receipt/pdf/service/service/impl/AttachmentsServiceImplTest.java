package it.gov.pagopa.receipt.pdf.service.service.impl;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.FiscalCodeNotAuthorizedException;
import it.gov.pagopa.receipt.pdf.service.exception.InvalidReceiptException;
import it.gov.pagopa.receipt.pdf.service.model.*;
import it.gov.pagopa.receipt.pdf.service.service.AttachmentsService;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

@QuarkusTest
class AttachmentsServiceImplTest {

    private static final String FISCAL_CODE_A = "AAAAAAAAAAAAAAAA";
    private static final String FISCAL_CODE_B = "BBBBBBBBBBBBBBBB";

    @InjectMock(convertScopes = true)
    private ReceiptCosmosClient cosmosClient;

    @Inject
    private AttachmentsService sut;

    @Test
    @SneakyThrows
    void getAttachmentDetailsSuccessWithDifferentPayerDebtor() {
        String id = UUID.randomUUID().toString();
        String fileNameDebtor = "file1.pdf";
        String urlDebtor = "file/" + fileNameDebtor;
        String fileNamePayer = "file2.pdf";
        String urlPayer = "file/" + fileNamePayer;
        Receipt receipt = Receipt.builder()
                .id(id)
                .eventData(
                        EventData.builder()
                                .debtorFiscalCode(FISCAL_CODE_A)
                                .payerFiscalCode(FISCAL_CODE_B)
                                .build()
                )
                .mdAttach(
                        ReceiptMetadata.builder()
                                .name(fileNameDebtor)
                                .url(urlDebtor)
                                .build()
                )
                .mdAttachPayer(
                        ReceiptMetadata.builder()
                                .name(fileNamePayer)
                                .url(urlPayer)
                                .build()
                )
                .numRetry(0)
                .build();

        doReturn(receipt).when(cosmosClient).getReceiptDocument(anyString());

        AttachmentDetailsResponse result = sut.getAttachmentDetails(anyString(), FISCAL_CODE_B);

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
        String urlDebtor = "file/" + fileNameDebtor;
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

        doReturn(receipt).when(cosmosClient).getReceiptDocument(anyString());

        AttachmentDetailsResponse result = sut.getAttachmentDetails(anyString(), FISCAL_CODE_A);

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
        doReturn(null).when(cosmosClient).getReceiptDocument(anyString());

        InvalidReceiptException e = assertThrows(InvalidReceiptException.class, () -> sut.getAttachmentDetails(anyString(), FISCAL_CODE_A));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_701, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void getAttachmentDetailsFailEventDataNull() {
        doReturn(Receipt.builder().numRetry(0).build()).when(cosmosClient).getReceiptDocument(anyString());

        InvalidReceiptException e = assertThrows(InvalidReceiptException.class, () -> sut.getAttachmentDetails(anyString(), FISCAL_CODE_A));

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

        doReturn(receipt).when(cosmosClient).getReceiptDocument(anyString());

        InvalidReceiptException e = assertThrows(InvalidReceiptException.class, () -> sut.getAttachmentDetails(anyString(), FISCAL_CODE_A));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_703, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void getAttachmentDetailsFailPayerFiscalCodeNull() {
        Receipt receipt = Receipt.builder()
                .eventData(
                        EventData.builder()
                                .debtorFiscalCode(UUID.randomUUID().toString())
                                .build()
                )
                .numRetry(0)
                .build();

        doReturn(receipt).when(cosmosClient).getReceiptDocument(anyString());

        InvalidReceiptException e = assertThrows(InvalidReceiptException.class, () -> sut.getAttachmentDetails(anyString(), FISCAL_CODE_A));

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

        doReturn(receipt).when(cosmosClient).getReceiptDocument(anyString());

        InvalidReceiptException e = assertThrows(InvalidReceiptException.class, () -> sut.getAttachmentDetails(anyString(), FISCAL_CODE_A));

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

        doReturn(receipt).when(cosmosClient).getReceiptDocument(anyString());

        InvalidReceiptException e = assertThrows(InvalidReceiptException.class, () -> sut.getAttachmentDetails(anyString(), FISCAL_CODE_A));

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

        doReturn(receipt).when(cosmosClient).getReceiptDocument(anyString());

        FiscalCodeNotAuthorizedException e = assertThrows(FiscalCodeNotAuthorizedException.class, () -> sut.getAttachmentDetails(anyString(), FISCAL_CODE_B));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_700, e.getErrorCode());
    }
}