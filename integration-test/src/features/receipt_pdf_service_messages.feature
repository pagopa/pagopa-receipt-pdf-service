Feature: Retrieve receipt attachments

  Scenario: Execute a request to getAttachmentDetails service
    Given a receipt with id "receipt-service-int-test-id-1" and debtorFiscalCode "VALID_FISCALCODE" stored on receipts datastore
    When an Http GET request is sent to the receipt-service getAttachmentDetails with path value "receipt-service-int-test-id-1" and fiscal_code param with value "VALID_FISCALCODE"
    Then response has a 200 Http status
    And response body contains receipt id "receipt-service-int-test-id-1"

  Scenario: Execute a request to getAttachmentDetails service with invalid request id
    When an Http GET request is sent to the receipt-service getAttachmentDetails with path value "receipt-service-int-test-invalid" and fiscal_code param with value "VALID_FISCALCODE"
    Then response has a 404 Http status
    And application error code is "PDFS_800"

  Scenario: Execute a request to getAttachmentDetails service with fiscal code not matching the resource
    Given a receipt with id "receipt-service-int-test-id-2" and debtorFiscalCode "VALID_FISCALCODE" stored on receipts datastore
    When an Http GET request is sent to the receipt-service getAttachmentDetails with path value "receipt-service-int-test-id-2" and fiscal_code param with value "INVALID_FISCCODE"
    Then response has a 500 Http status
    And application error code is "PDFS_700"

  Scenario: Execute a request to getAttachmentDetails service without fiscal_code param
    When an Http GET request is sent to the receipt-service getAttachmentDetails without fiscal_code param
    Then response has a 400 Http status
    And application error code is "PDFS_901"

  Scenario: Execute a request to getAttachment service
    Given a receipt with id "receipt-service-int-test-id-3" and debtorFiscalCode "VALID_FISCALCODE" and mdAttachmentName "VALID_ATTACHMENT_NAME.pdf" stored on receipts datastore
    Given a pdf with name "VALID_ATTACHMENT_NAME.pdf" stored on Blob Storage
    When an Http GET request is sent to the receipt-service getAttachment with path value "receipt-service-int-test-id-3" and "VALID_ATTACHMENT_NAME.pdf" and fiscal_code param with value "VALID_FISCALCODE"
    Then response has a 200 Http status
    And response body has the expected data content

  Scenario: Execute a request to getAttachment service with invalid request id
    When an Http GET request is sent to the receipt-service getAttachment with path value "receipt-service-int-test-invalid" and "VALID_ATTACHMENT_NAME.pdf" and fiscal_code param with value "VALID_FISCALCODE"
    Then response has a 404 Http status
    And application error code is "PDFS_800"

  Scenario: Execute a request to getAttachment service with fiscal code not matching the resource
    Given a receipt with id "receipt-service-int-test-id-4" and debtorFiscalCode "VALID_FISCALCODE" and mdAttachmentName "VALID_ATTACHMENT_NAME.pdf" stored on receipts datastore
    Given a pdf with name "VALID_ATTACHMENT_NAME.pdf" stored on Blob Storage
    When an Http GET request is sent to the receipt-service getAttachment with path value "receipt-service-int-test-id-4" and "VALID_ATTACHMENT_NAME.pdf" and fiscal_code param with value "INVALID_FISCCODE"
    Then response has a 500 Http status
    And application error code is "PDFS_706"

  Scenario: Execute a request to getAttachment service with invalid attachment name
    Given a receipt with id "receipt-service-int-test-id-5" and debtorFiscalCode "VALID_FISCALCODE" and mdAttachmentName "INVALID_ATTACHMENT_NAME.pdf" stored on receipts datastore
    When an Http GET request is sent to the receipt-service getAttachment with path value "receipt-service-int-test-id-5" and "INVALID_ATTACHMENT_NAME.pdf" and fiscal_code param with value "VALID_FISCALCODE"
    Then response has a 404 Http status
    And application error code is "PDFS_602"

  Scenario: Execute a request to getAttachment service without fiscal_code param
    When an Http GET request is sent to the receipt-service getAttachment without fiscal_code param
    Then response has a 400 Http status
    And application error code is "PDFS_901"
  ####Cart####

  Scenario: Execute a request to getAttachmentDetails service for cart, payer case
    Given a cart receipt with id  "receipt-cart-service-int-test-id-1" a payerFiscalCode "VLD_PAYER_FISCAL" with bizEventId "payer-31b43ccf-9cbb-4637-9027-415303e7c1d1" and debtorFiscalCode "VLD_DEBTOR_FISCA" with bizEventId "debtor-363eb6c9-781a-4b62-87d7-b5365d2e9b55" stored on cart-for-receipts datastore
    When an Http GET request is sent to the receipt-service getAttachmentDetails with path value "receipt-cart-service-int-test-id-1_CART_" and fiscal_code param with value "VLD_PAYER_FISCAL"
    Then response has a 200 Http status
    And response body contains receipt id "receipt-cart-service-int-test-id-1"
    And response body contains receipt details subject "Payer subject"
    And response body contains receipt details markdown "Payer **markdown**"

  Scenario: Execute a request to getAttachmentDetails service for cart, payer case with fiscal code not matching the resource
    Given a cart receipt with id  "receipt-cart-service-int-test-id-1" a payerFiscalCode "VLD_PAYER_FISCAL" with bizEventId "payer-31b43ccf-9cbb-4637-9027-415303e7c1d1" and debtorFiscalCode "VLD_DEBTOR_FISCA" with bizEventId "debtor-363eb6c9-781a-4b62-87d7-b5365d2e9b55" stored on cart-for-receipts datastore
    When an Http GET request is sent to the receipt-service getAttachmentDetails with path value "receipt-cart-service-int-test-id-1_CART_" and fiscal_code param with value "INVALID_FISCCODE"
    Then response has a 500 Http status
    And application error code is "PDFS_700"

  Scenario: Execute a request to getAttachmentDetails service for cart, debtor case
    Given a cart receipt with id  "receipt-cart-service-int-test-id-2" a payerFiscalCode "VLD_PAYER_FISCAL" with bizEventId "payer-31b43ccf-9cbb-4637-9027-415303e7c1d1" and debtorFiscalCode "VLD_DEBTOR_FISCA" with bizEventId "debtor-363eb6c9-781a-4b62-87d7-b5365d2e9b55" stored on cart-for-receipts datastore
    When an Http GET request is sent to the receipt-service getAttachmentDetails with path value "receipt-cart-service-int-test-id-2_CART_debtor-363eb6c9-781a-4b62-87d7-b5365d2e9b55" and fiscal_code param with value "VLD_DEBTOR_FISCA"
    Then response has a 200 Http status
    And response body contains receipt id "receipt-cart-service-int-test-id-2"
    And response body contains receipt details subject "Cart Debtor subject"
    And response body contains receipt details markdown "Cart Debtor **markdown**"

  Scenario: Execute a request to getAttachmentDetails service for cart, debtor case with fiscal code not matching the debtor bizEventId
    Given a cart receipt with id  "receipt-cart-service-int-test-id-2" a payerFiscalCode "VLD_PAYER_FISCAL" with bizEventId "payer-31b43ccf-9cbb-4637-9027-415303e7c1d1" and debtorFiscalCode "VLD_DEBTOR_FISCA" with bizEventId "debtor-363eb6c9-781a-4b62-87d7-b5365d2e9b55" stored on cart-for-receipts datastore
    When an Http GET request is sent to the receipt-service getAttachmentDetails with path value "receipt-cart-service-int-test-id-2_CART_debtor-363eb6c9-781a-4b62-87d7-b5365d2e9b55" and fiscal_code param with value "VLD_PAYER_FISCAL"
    Then response has a 500 Http status
    And application error code is "PDFS_700"
