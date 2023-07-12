package it.gov.pagopa.receipt.pdf.service.client;


import it.gov.pagopa.receipt.pdf.service.exception.BlobStorageClientException;

import java.io.File;

public interface ReceiptBlobClient {

    File getAttachmentFromBlobStorage(String fileName) throws BlobStorageClientException;
}
