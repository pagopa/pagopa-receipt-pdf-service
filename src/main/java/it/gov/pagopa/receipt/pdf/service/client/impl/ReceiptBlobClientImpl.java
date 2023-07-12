package it.gov.pagopa.receipt.pdf.service.client.impl;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.DownloadRetryOptions;
import com.azure.storage.blob.options.BlobDownloadToFileOptions;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptBlobClient;
import it.gov.pagopa.receipt.pdf.service.exception.BlobStorageClientException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;

import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.PDFS_600;
import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.PDFS_601;

/**
 * Client for the Blob Storage
 */
@ApplicationScoped
public class ReceiptBlobClientImpl implements ReceiptBlobClient {

    private final Logger logger = LoggerFactory.getLogger(ReceiptBlobClientImpl.class);

    @ConfigProperty(name = "blob.storage.container.name")
    private String containerName;

    @ConfigProperty(name = "blob.storage.client.max-retry-request")
    private int maxRetryRequests;

    @ConfigProperty(name = "blob.storage.client.timeout")
    private int timeout;

    @Inject
    private BlobServiceClient blobServiceClient;

    /**
     * Retrieve a PDF receipt from the blob storage
     *
     * @param fileName file name of the PDF receipt
     * @return the file where the PDF receipt was stored
     */
    public File getAttachmentFromBlobStorage(String fileName) throws BlobStorageClientException {
        BlobClient blobClient = this.blobServiceClient.getBlobContainerClient(containerName).getBlobClient(fileName);

        Path tempDirectory;
        try {
             tempDirectory = Files.createTempDirectory("receipt-pdf-service");
        } catch (IOException e) {
            logger.error("Error creating the temp directory to download the PDF receipt from Blob Storage");
            throw new BlobStorageClientException(PDFS_600, PDFS_600.getErrorMessage(),  e);
        }
        String filePath = tempDirectory.toAbsolutePath() + "/receiptPdf.pdf";

        try {
            blobClient.downloadToFileWithResponse(
                    getBlobDownloadToFileOptions(filePath),
                    Duration.ofSeconds(timeout),
                    Context.NONE);
        } catch (Exception e) {
            logger.error("Error downloading the PDF receipt from Blob Storage");
            throw new BlobStorageClientException(PDFS_601, PDFS_601.getErrorMessage(),  e);
        }
        return new File(filePath);
    }

    private BlobDownloadToFileOptions getBlobDownloadToFileOptions(String filePath) {
        return new BlobDownloadToFileOptions(filePath)
                .setDownloadRetryOptions(new DownloadRetryOptions().setMaxRetryRequests(maxRetryRequests))
                .setOpenOptions(new HashSet<>(
                        Arrays.asList(
                                StandardOpenOption.CREATE_NEW,
                                StandardOpenOption.WRITE,
                                StandardOpenOption.READ
                        ))
                );
    }
}
