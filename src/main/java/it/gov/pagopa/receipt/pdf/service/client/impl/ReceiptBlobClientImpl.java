package it.gov.pagopa.receipt.pdf.service.client.impl;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptBlobClient;
import it.gov.pagopa.receipt.pdf.service.exception.AttachmentNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.BlobStorageClientException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;

import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.*;
import static it.gov.pagopa.receipt.pdf.service.utils.CommonUtils.sanitize;

/**
 * Client for the Blob Storage
 */
@ApplicationScoped
public class ReceiptBlobClientImpl implements ReceiptBlobClient {

    private final Logger logger = LoggerFactory.getLogger(ReceiptBlobClientImpl.class);

    @ConfigProperty(name = "blob.storage.client.max-retry-request")
    private int maxRetryRequests;

    @ConfigProperty(name = "blob.storage.client.timeout")
    private int timeout;

    private final BlobContainerClient blobContainerClient;

    @Inject
    public ReceiptBlobClientImpl(BlobContainerClient blobContainerClient) {
        this.blobContainerClient = blobContainerClient;
    }

    /**
     * Retrieve a PDF receipt from the blob storage
     *
     * @param attachmentName file name of the PDF receipt
     * @return the file where the PDF receipt was stored
     */
    public InputStream getAttachmentFromBlobStorage(String attachmentName) throws BlobStorageClientException, AttachmentNotFoundException {
        BlobClient blobClient = blobContainerClient.getBlobClient(sanitize(attachmentName));
        return downloadAttachment(attachmentName, blobClient);
    }

    private InputStream downloadAttachment(
            String fileName,
            BlobClient blobClient
    ) throws BlobStorageClientException, AttachmentNotFoundException {

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            blobClient.downloadStreamWithResponse(
                    outputStream,
                    null,   // BlobRange (null = entire blob)
                    null,   // DownloadRetryOptions
                    null,   // RequestConditions
                    false,  // rangeGetContentMd5
                    Duration.ofSeconds(timeout),
                    Context.NONE
            );

            return new ByteArrayInputStream(outputStream.toByteArray());

        } catch (UncheckedIOException e) {
            logger.error("I/O error downloading the PDF receipt from Blob Storage", e);
            throw new BlobStorageClientException(
                    PDFS_601,
                    PDFS_601.getErrorMessage(),
                    e
            );

        } catch (BlobStorageException e) {
            String errMsg;
            if (e.getStatusCode() == 404) {
                errMsg = String.format("PDF receipt with name: %s not found in Blob Storage: %s", sanitize(fileName), blobClient.getAccountName());
                logger.error(errMsg);
                throw new AttachmentNotFoundException(PDFS_602, errMsg, fileName, e
                );
            }
            errMsg = String.format("Unable to download the PDF receipt with name: %s from Blob Storage: %s. Error message from server: %s",
                    sanitize(fileName),
                    blobClient.getAccountName(),
                    e.getServiceMessage()
            );
            logger.error(errMsg);
            throw new BlobStorageClientException(PDFS_603, errMsg, e);
        }
    }
}
