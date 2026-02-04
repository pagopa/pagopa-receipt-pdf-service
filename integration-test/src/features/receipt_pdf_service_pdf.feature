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
    Then response has a 404 Http status
    And application error code is "PDFS_714"

  Scenario: Error PDFS_715 when retrieving a pdf with a retryable error
    Given a receipt with id "receipt-service-pdf-int-test-id-3" and debtorFiscalCode "VALID_FISCALCODE" and mdAttachmentName "VALID_ATTACHMENT_NAME.pdf" and status "FAILED" and reasonErrorCode 900 stored on receipts datastore
    When an Http GET request is sent to the receipt-service getReceiptPdf with thirdPartyId "receipt-service-pdf-int-test-id-3" and fiscal_code param with value "VALID_FISCALCODE"
    Then response has a 404 Http status
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

  Scenario: Successfully retrieve payer cart pdf attachment
    Given a pdf with name "VALID_ATTACHMENT_NAME.pdf" stored on Blob Storage
    And a cart receipt with id "receipt-cart-service-pdf-int-test-id-1" a payerFiscalCode "VLD_PAYER_FISCAL" with bizEventId "payer-31b43ccf-9cbb-4637-9027-415303e7c1d1" and debtorFiscalCode "VLD_DEBTOR_FISCA" with bizEventId "debtor-363eb6c9-781a-4b62-87d7-b5365d2e9b55", pdfName "VALID_ATTACHMENT_NAME.pdf", status "IO_NOTIFIED" and payerReasonErrCode 0 and debtorReasonErrCode 0 stored on cart-for-receipts datastore
    When an Http GET request is sent to the receipt-service getReceiptPdf with thirdPartyId "receipt-cart-service-pdf-int-test-id-1_CART_" and fiscal_code param with value "VLD_PAYER_FISCAL"
    Then response has a 200 Http status
    And response body has the expected data content

  Scenario: Successfully retrieve debtor cart pdf attachment
    Given a pdf with name "VALID_ATTACHMENT_NAME.pdf" stored on Blob Storage
    And a cart receipt with id "receipt-cart-service-pdf-int-test-id-2" a payerFiscalCode "VLD_PAYER_FISCAL" with bizEventId "payer-31b43ccf-9cbb-4637-9027-415303e7c1d1" and debtorFiscalCode "VLD_DEBTOR_FISCA" with bizEventId "debtor-363eb6c9-781a-4b62-87d7-b5365d2e9b55", pdfName "VALID_ATTACHMENT_NAME.pdf", status "IO_NOTIFIED" and payerReasonErrCode 0 and debtorReasonErrCode 0 stored on cart-for-receipts datastore
    When an Http GET request is sent to the receipt-service getReceiptPdf with thirdPartyId "receipt-cart-service-pdf-int-test-id-2_CART_debtor-363eb6c9-781a-4b62-87d7-b5365d2e9b55" and fiscal_code param with value "VLD_DEBTOR_FISCA"
    Then response has a 200 Http status
    And response body has the expected data content

  Scenario: Error PDFS_714 when retrieving a cart pdf not yet generated
    Given a cart receipt with id "receipt-cart-service-pdf-int-test-id-3" a payerFiscalCode "VLD_PAYER_FISCAL" with bizEventId "payer-31b43ccf-9cbb-4637-9027-415303e7c1d1" and debtorFiscalCode "VLD_DEBTOR_FISCA" with bizEventId "debtor-363eb6c9-781a-4b62-87d7-b5365d2e9b55", pdfName "null", status "INSERTED" and payerReasonErrCode 0 and debtorReasonErrCode 0 stored on cart-for-receipts datastore
    When an Http GET request is sent to the receipt-service getReceiptPdf with thirdPartyId "receipt-cart-service-pdf-int-test-id-3_CART_" and fiscal_code param with value "VLD_PAYER_FISCAL"
    Then response has a 404 Http status
    And application error code is "PDFS_714"

  Scenario: Error PDFS_715 when retrieving a cart pdf with a retryable error
    Given a cart receipt with id "receipt-cart-service-pdf-int-test-id-4" a payerFiscalCode "VLD_PAYER_FISCAL" with bizEventId "payer-31b43ccf-9cbb-4637-9027-415303e7c1d1" and debtorFiscalCode "VLD_DEBTOR_FISCA" with bizEventId "debtor-363eb6c9-781a-4b62-87d7-b5365d2e9b55", pdfName "null", status "FAILED" and payerReasonErrCode 900 and debtorReasonErrCode 900 stored on cart-for-receipts datastore
    When an Http GET request is sent to the receipt-service getReceiptPdf with thirdPartyId "receipt-cart-service-pdf-int-test-id-4_CART_" and fiscal_code param with value "VLD_PAYER_FISCAL"
    Then response has a 404 Http status
    And application error code is "PDFS_715"

  Scenario: Error PDFS_716 when retrieving a cart pdf with debtor critical error that needs review
    Given a cart receipt with id "receipt-cart-service-pdf-int-test-id-5" a payerFiscalCode "VLD_PAYER_FISCAL" with bizEventId "payer-31b43ccf-9cbb-4637-9027-415303e7c1d1" and debtorFiscalCode "VLD_DEBTOR_FISCA" with bizEventId "debtor-363eb6c9-781a-4b62-87d7-b5365d2e9b55", pdfName "null", status "FAILED" and payerReasonErrCode 900 and debtorReasonErrCode 903 stored on cart-for-receipts datastore
    When an Http GET request is sent to the receipt-service getReceiptPdf with thirdPartyId "receipt-cart-service-pdf-int-test-id-5_CART_debtor-363eb6c9-781a-4b62-87d7-b5365d2e9b55" and fiscal_code param with value "VLD_DEBTOR_FISCA"
    Then response has a 500 Http status
    And application error code is "PDFS_716"

  Scenario: Error PDFS_716 when retrieving a cart pdf with payer critical error that needs review
    Given a cart receipt with id "receipt-cart-service-pdf-int-test-id-6" a payerFiscalCode "VLD_PAYER_FISCAL" with bizEventId "payer-31b43ccf-9cbb-4637-9027-415303e7c1d1" and debtorFiscalCode "VLD_DEBTOR_FISCA" with bizEventId "debtor-363eb6c9-781a-4b62-87d7-b5365d2e9b55", pdfName "null", status "FAILED" and payerReasonErrCode 903 and debtorReasonErrCode 900 stored on cart-for-receipts datastore
    When an Http GET request is sent to the receipt-service getReceiptPdf with thirdPartyId "receipt-cart-service-pdf-int-test-id-6_CART_" and fiscal_code param with value "VLD_PAYER_FISCAL"
    Then response has a 500 Http status
    And application error code is "PDFS_716"

  Scenario: Error PDFS_706 when the fiscal code is not authorized to retrieve the pdf of the given cart
    Given a cart receipt with id "receipt-cart-service-pdf-int-test-id-7" a payerFiscalCode "VLD_PAYER_FISCAL" with bizEventId "payer-31b43ccf-9cbb-4637-9027-415303e7c1d1" and debtorFiscalCode "VLD_DEBTOR_FISCA" with bizEventId "debtor-363eb6c9-781a-4b62-87d7-b5365d2e9b55", pdfName "null", status "IO_NOTIFIED" and payerReasonErrCode 0 and debtorReasonErrCode 0 stored on cart-for-receipts datastore
    When an Http GET request is sent to the receipt-service getReceiptPdf with thirdPartyId "receipt-cart-service-pdf-int-test-id-7_CART_" and fiscal_code param with value "INVALID_FISCCODE"
    Then response has a 500 Http status
    And application error code is "PDFS_706"
