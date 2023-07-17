package it.gov.pagopa.receipt.pdf.service.client.impl;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.options.BlobDownloadToFileOptions;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptBlobClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.BlobStorageClientException;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.UncheckedIOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
class ReceiptBlobClientImplTest {

    @Inject
    private ReceiptBlobClient sut;

    @Inject
    private BlobContainerClient blobContainerClientMock;

    private static BlobClient blobClientMockMock;


    @BeforeAll
    static void setUp() {
        blobClientMockMock = mock(BlobClient.class);
        BlobContainerClient blobContainerClientMock = mock(BlobContainerClient.class);
        doReturn(blobClientMockMock).when(blobContainerClientMock).getBlobClient(anyString());
        QuarkusMock.installMockForType(blobContainerClientMock, BlobContainerClient.class);
    }


    @Test
    @SneakyThrows
    void getAttachmentFromBlobStorageSuccess() {
        doReturn(null).when(blobClientMockMock)
                .downloadToFileWithResponse(
                        any(BlobDownloadToFileOptions.class),
                        any(Duration.class),
                        any(Context.class)
                );

        File result = sut.getAttachmentFromBlobStorage(anyString());

        assertNotNull(result);
    }

    @Test
    @SneakyThrows
    void getAttachmentFromBlobStorageFailDownloadThrowsException() {
        doThrow(UncheckedIOException.class).when(blobClientMockMock)
                .downloadToFileWithResponse(
                        any(BlobDownloadToFileOptions.class),
                        any(Duration.class),
                        any(Context.class)
                );

        BlobStorageClientException e = assertThrows(BlobStorageClientException.class, () -> sut.getAttachmentFromBlobStorage(anyString()));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_601, e.getErrorCode());
    }
}