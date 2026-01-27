package it.gov.pagopa.receipt.pdf.service.service;

import it.gov.pagopa.receipt.pdf.service.exception.*;
import it.gov.pagopa.receipt.pdf.service.model.AttachmentsDetailsResponse;

import java.io.File;
import java.io.IOException;

/**
 * Interface of the service to be used to retrieve the attachments
 */
public interface AttachmentsService {

    /**
     * Retrieve the attachment detail of the receipt with the provided id, only if the fiscal code is authorized to access it
     *
     * @param thirdPartyId      the id of the receipt
     * @param requestFiscalCode the fiscal code of the user that request the receipt
     * @return the details of the requested attachments
     * @throws ReceiptNotFoundException         thrown if a receipt with the provided id was not found
     * @throws InvalidReceiptException          thrown if the retrieved receipt is invalid
     * @throws FiscalCodeNotAuthorizedException thrown if the fiscal code is not authorized to access the requested receipt
     */
    AttachmentsDetailsResponse getAttachmentsDetails(String thirdPartyId, String requestFiscalCode) throws ReceiptNotFoundException, InvalidReceiptException, FiscalCodeNotAuthorizedException, InvalidCartException, CartNotFoundException;

    /**
     * Retrieve the attachment of the receipt with the provided id using the given attachment url, only if the fiscal code is authorized to access it
     *
     * @param thirdPartyId      the id of the receipt
     * @param requestFiscalCode the fiscal code of the user that request the receipt
     * @param attachmentUrl     the relative url to the attachment
     * @return the File with the reference to the attachment
     * @throws ReceiptNotFoundException         thrown if a receipt with the provided id was not found
     * @throws InvalidReceiptException          thrown if the retrieved receipt is invalid
     * @throws FiscalCodeNotAuthorizedException thrown if the fiscal code is not authorized to access the requested attachment
     * @throws BlobStorageClientException       thrown for error when retrieving the attachment from the Blob Storage
     * @throws AttachmentNotFoundException      thrown if the requested attachment was not found
     */
    File getAttachment(String thirdPartyId, String requestFiscalCode, String attachmentUrl)
            throws ReceiptNotFoundException, InvalidReceiptException, FiscalCodeNotAuthorizedException, BlobStorageClientException, AttachmentNotFoundException, InvalidCartException, CartNotFoundException;

    /**
     * Retrieve a PDF receipt from the blob storage
     *
     * @param fileName file name of the PDF receipt
     * @return the File with the reference to the attachment
     * @throws IOException
     * @throws BlobStorageClientException  thrown for error when retrieving the attachment from the Blob Storage
     * @throws AttachmentNotFoundException thrown if the requested attachment was not found
     */
    byte[] getAttachmentBytesFromBlobStorage(String fileName) throws IOException, AttachmentNotFoundException, BlobStorageClientException;

    /**
     * Retrieve the PDF of a receipt using the given third party id and fiscalCode, only if the fiscal code is authorized to access it
     *
     * @param thirdPartyId      the id of the biz event for single receipt, the id of the transaction concatenated with the biz event for cart receipts
     * @param requestFiscalCode the fiscal code of the user that request the receipt
     * @return the details of the requested attachments
     * @throws AttachmentNotFoundException      thrown if the requested attachment was not found
     * @throws FiscalCodeNotAuthorizedException thrown if the fiscal code is not authorized to access the requested receipt
     * @throws BlobStorageClientException       thrown for error when retrieving the attachment from the Blob Storage
     * @throws ReceiptNotFoundException         thrown if the single receipt is not found
     * @throws CartNotFoundException            thrown if the cart receipt is not found
     * @throws InvalidReceiptException          thrown if the single receipt is in invalid state
     * @throws InvalidCartException             thrown if the cart receipt is in invalid state
     */
    File getReceiptPdf(String thirdPartyId, String requestFiscalCode) throws FiscalCodeNotAuthorizedException,
            BlobStorageClientException, AttachmentNotFoundException, ReceiptNotFoundException, CartNotFoundException, InvalidReceiptException, InvalidCartException;
}
