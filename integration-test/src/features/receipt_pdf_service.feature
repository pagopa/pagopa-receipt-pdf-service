Feature: Retrieve receipt attachments

  Scenario: Execute a request to getAttachmentDetails service
    Given a receipt with id "receipt-service-int-test-id-1" and debtorFiscalCode "VALID_FISCAL_CODE" stored on receipts datastore with status IO_NOTIFIED
    When an Http GET request is sent to the receipt-service with path value "receipt-service-int-test-id-1" and fiscal_code header with value "VALID_FISCAL_CODE"
    Then response has a 200 Http status
    And response body contains receipt id "receipt-service-int-test-id-1"

  Scenario: Execute a request to getAttachmentDetails service with invalid request id
    When an Http GET request is sent to the receipt-service with path value "receipt-service-int-test-invalid"
    Then response has a 400 Http status
    And application error code is "PDFS_800"
  
  Scenario: Execute a request to getAttachmentDetails service with fiscal code not matching the resource
    Given a receipt with id "receipt-service-int-test-id-2" and debtorFiscalCode "VALID_FISCAL_CODE" stored on receipts datastore
    When an Http GET request is sent to the receipt-service with path value "receipt-service-int-test-id-2" and fiscal_code header with value "INVALID_FISCAL_CODE"
    Then response has a 400 Http status
    And application error code is "PDFS_700"

  Scenario: Execute a request to getAttachmentDetails service without fiscal_code header
    When an Http GET request is sent to the receipt-service without fiscal_code header
    Then response has a 400 Http status
    And application error code is "PDFS_901"

  Scenario: Execute a request to getAttachment service
    Given a receipt with id "receipt-service-int-test-id-3" and debtorFiscalCode "VALID_FISCAL_CODE" and mdAttachmentName "VALID_ATTACHMENT_NAME.pdf" stored on receipts datastore
    Given a pdf with name "VALID_ATTACHMENT_NAME.pdf" stored on Blob Storage
    When an Http GET request is sent to the receipt-service with path value "receipt-service-int-test-id-3" and "VALID_ATTACHMENT_NAME.pdf" and fiscal_code header with value "VALID_FISCAL_CODE"
    Then response has a 200 Http status
    And response body is not empty
  
  Scenario: Execute a request to getAttachment service with invalid request id
    When an Http GET request is sent to the receipt-service with path value "receipt-service-int-test-invalid" and "VALID_ATTACHMENT_NAME.pdf" and fiscal_code header with value "VALID_FISCAL_CODE"
    Then response has a 400 Http status
    And application error code is "PDFS_800"

  Scenario: Execute a request to getAttachment service with fiscal code not matching the resource
    Given a receipt with id "receipt-service-int-test-id-4" and debtorFiscalCode "VALID_FISCAL_CODE" and mdAttachmentName "VALID_ATTACHMENT_NAME.pdf" stored on receipts datastore
    Given a pdf with name "VALID_ATTACHMENT_NAME.pdf" stored on Blob Storage
    When an Http GET request is sent to the receipt-service with path value "receipt-service-int-test-id-4" and "VALID_ATTACHMENT_NAME.pdf" and fiscal_code header with value "INVALID_FISCAL_CODE"
    Then response has a 400 Http status
    And application error code is "PDFS_706"

  Scenario: Execute a request to getAttachment service with invalid attachment name
    Given a receipt with id "receipt-service-int-test-id-5" and debtorFiscalCode "VALID_FISCAL_CODE" and mdAttachmentName "VALID_ATTACHMENT_NAME.pdf" stored on receipts datastore
    When an Http GET request is sent to the receipt-service with path value "receipt-service-int-test-id-5" and "INVALID_ATTACHMENT_NAME.pdf" and fiscal_code header with value "VALID_FISCAL_CODE"
    Then response has a 400 Http status
    And application error code is "PDFS_602"

  Scenario: Execute a request to getAttachment service without fiscal_code header
    When an Http GET request is sent to the receipt-service without fiscal_code header
    Then response has a 400 Http status
    And application error code is "PDFS_901"
  

