Feature: Retrieve receipt attachments

  Scenario: Successfully retrieve receipt pdf attachment
    Given a pdf with name "VALID_ATTACHMENT_NAME.pdf" stored on Blob Storage
    And a receipt with id "receipt-service-pdf-int-test-id-1" and debtorFiscalCode "VALID_FISCALCODE" and mdAttachmentName "VALID_ATTACHMENT_NAME.pdf" and status "IO_NOTIFIED" and reasonErrorCode 0 stored on receipts datastore
    When an Http GET request is sent to the receipt-service getReceiptPdf with thirdPartyId "receipt-service-pdf-int-test-id-1" and fiscal_code param with value "VALID_FISCALCODE"
    Then response has a 200 Http status
    And response body has the expected data content

  Scenario: Error PDFS_714 when retrieving a pdf not yet generated
    Given a receipt with id "receipt-service-pdf-int-test-id-2" and debtorFiscalCode "VALID_FISCALCODE" and mdAttachmentName "VALID_ATTACHMENT_NAME.pdf" and status "INSERTED" and reasonErrorCode 0 stored on receipts datastore
    When an Http GET request is sent to the receipt-service getReceiptPdf with thirdPartyId "receipt-service-pdf-int-test-id-2" and fiscal_code param with value "VALID_FISCALCODE"
    Then response has a 500 Http status
    And application error code is "PDFS_714"

  Scenario: Error PDFS_715 when retrieving a pdf with a retryable error
    Given a receipt with id "receipt-service-pdf-int-test-id-3" and debtorFiscalCode "VALID_FISCALCODE" and mdAttachmentName "VALID_ATTACHMENT_NAME.pdf" and status "FAILED" and reasonErrorCode 900 stored on receipts datastore
    When an Http GET request is sent to the receipt-service getReceiptPdf with thirdPartyId "receipt-service-pdf-int-test-id-3" and fiscal_code param with value "VALID_FISCALCODE"
    Then response has a 500 Http status
    And application error code is "PDFS_715"

  Scenario: Error PDFS_716 when retrieving a pdf with a critical error that needs review
    Given a receipt with id "receipt-service-pdf-int-test-id-4" and debtorFiscalCode "VALID_FISCALCODE" and mdAttachmentName "VALID_ATTACHMENT_NAME.pdf" and status "FAILED" and reasonErrorCode 903 stored on receipts datastore
    When an Http GET request is sent to the receipt-service getReceiptPdf with thirdPartyId "receipt-service-pdf-int-test-id-4" and fiscal_code param with value "VALID_FISCALCODE"
    Then response has a 500 Http status
    And application error code is "PDFS_716"

 Scenario: Error PDFS_706 when the fiscal code is not authorized to retrieve the pdf of the given receipt
    Given a receipt with id "receipt-service-pdf-int-test-id-5" and debtorFiscalCode "VALID_FISCALCODE" and mdAttachmentName "VALID_ATTACHMENT_NAME.pdf" and status "IO_NOTIFIED" and reasonErrorCode 0 stored on receipts datastore
    When an Http GET request is sent to the receipt-service getReceiptPdf with thirdPartyId "receipt-service-pdf-int-test-id-5" and fiscal_code param with value "INVALID_FISCCODE"
    Then response has a 500 Http status
    And application error code is "PDFS_706"

  ####Cart####
