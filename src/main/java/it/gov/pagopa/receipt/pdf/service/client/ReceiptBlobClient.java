package it.gov.pagopa.receipt.pdf.service.client;


import it.gov.pagopa.receipt.pdf.service.exception.AttachmentNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.BlobStorageClientException;

import java.io.File;

/**
 * Interface of the client for the receipt's attachment Blob Storage used to retrieve the PDF documents
 */
public interface ReceiptBlobClient {

    /**
     * Retrieve the attachment from the Blob Storage
     *
     * @param attachmentName the name of the file to be retrieved
     * @param fileName the name of the file created locally
     * @return the File with the reference to the downloaded attachment
     * @throws BlobStorageClientException thrown for error when retrieving the attachment
     * @throws AttachmentNotFoundException thrown if the requested attachment was not found
     */
    File getAttachmentFromBlobStorage(String attachmentName, String fileName) throws BlobStorageClientException, AttachmentNotFoundException;
}
