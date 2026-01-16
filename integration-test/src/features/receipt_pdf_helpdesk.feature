Feature: All about payment events to recover managed by Azure functions receipt-pdf-helpdesk

  Scenario: getReceipt API return receipt stored on datastore
    Given a receipt with eventId "receipt-helpdesk-int-test-id-1" stored into receipt datastore
    When getReceipt API is called with eventId "receipt-helpdesk-int-test-id-1"
    Then the api response has a 200 Http status
    And the receipt has eventId "receipt-helpdesk-int-test-id-1"

  Scenario: getReceiptByOrganizationFiscalCodeAndIUV API return receipt stored on datastore
    Given a receipt with eventId "receipt-helpdesk-int-test-id-2" stored into receipt datastore
    And a biz event with id "receipt-helpdesk-int-test-id-2" and status "DONE" and organizationFiscalCode "intTestOrgCode" and IUV "intTestIuv" stored on biz-events datastore
    When getReceiptByOrganizationFiscalCodeAndIUV API is called with organizationFiscalCode "intTestOrgCode" and IUV "intTestIuv"
    Then the api response has a 200 Http status
    And the receipt has eventId "receipt-helpdesk-int-test-id-2"

  Scenario: getReceiptError API return receipt-error stored on datastore
    Given a receipt-error with bizEventId "receipt-helpdesk-int-test-id-3" and status "TO_REVIEW" stored into receipt-error datastore
    When getReceiptError API is called with bizEventId "receipt-helpdesk-int-test-id-3"
    Then the api response has a 200 Http status
    And the receipt-error has bizEventId "receipt-helpdesk-int-test-id-3"
    And the receipt-error payload has bizEvent decrypted with eventId "receipt-generator-int-test-id-4"

  Scenario: getReceiptPdf API return receipt pdf store on blob storage
    Given a receipt pdf with filename "int-test-helpdesk-receipt.pdf" stored into blob storage
    When getReceiptPdf API is called with filename "int-test-helpdesk-receipt.pdf"
    Then the api response has a 200 Http status

  Scenario: getReceiptMessage API return receipt-error stored on datastore
    Given a receipt-io-message with bizEventId "receipt-helpdesk-int-test-id-3" and messageId "receipt-helpdesk-int-test-message-id-3" stored into receipt-io-message datastore
    When getReceiptMessage API is called with messageId "receipt-helpdesk-int-test-message-id-3"
    Then the api response has a 200 Http status
    And the receipt-message has messageId "receipt-helpdesk-int-test-message-id-3"
    And the receipt-message has eventId "receipt-helpdesk-int-test-id-3"
  
  ## CART RECEIPT
  Scenario: getCartReceipt API return receipt stored on datastore
    Given a cart with id "receipt-cart-helpdesk-int-test-id-1" stored into cart datastore
    When getCartReceipt API is called with cartId "receipt-cart-helpdesk-int-test-id-1"
    Then the api response has a 200 Http status
    And the receipt has cartId "receipt-cart-helpdesk-int-test-id-1"

  Scenario: getCartReceiptByOrganizationFiscalCodeAndIUV API return receipt stored on datastore
    Given a cart with id "receipt-cart-helpdesk-int-test-id-2" stored into cart datastore
    And a biz event with id "receipt-cart-helpdesk-int-test-id-2" and status "DONE" and organizationFiscalCode "intTestOrgCode" and IUV "intTestIuv" stored on biz-events datastore
    When getCartReceiptByOrganizationFiscalCodeAndIUV API is called with organizationFiscalCode "intTestOrgCode" and IUV "intTestIuv"
    Then the api response has a 200 Http status
    And the receipt has cartId "receipt-cart-helpdesk-int-test-id-2"

  Scenario: getCartReceiptError API return receipt-error stored on datastore
    Given a cart-receipt-error with cartId "receipt-cart-helpdesk-int-test-id-3" and status "TO_REVIEW" stored into cart-receipt-error datastore
    When getCartReceiptError API is called with cartId "receipt-cart-helpdesk-int-test-id-3"
    Then the api response has a 200 Http status
    And the cart-receipt-error has cartId "receipt-cart-helpdesk-int-test-id-3"
    And the cart-receipt-error payload has bizEvent decrypted with eventId "receipt-generator-int-test-id-4"

  Scenario: getCartReceiptMessage API return receipt-error stored on datastore
    Given a cart-receipt-io-message with bizEventId "receipt-cart-helpdesk-int-test-id-4" and messageId "receipt-cart-helpdesk-int-test-message-id-4" stored into cart-receipt-io-message datastore
    When getCartReceiptMessage API is called with messageId "receipt-cart-helpdesk-int-test-message-id-4"
    Then the api response has a 200 Http status
    And the receipt-message has messageId "receipt-cart-helpdesk-int-test-message-id-4"
    And the receipt-message has eventId "receipt-cart-helpdesk-int-test-id-4"

