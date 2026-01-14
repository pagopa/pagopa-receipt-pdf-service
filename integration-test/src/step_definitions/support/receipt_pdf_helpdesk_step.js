const assert = require('assert');
const { After, Given, When, Then, setDefaultTimeout } = require('@cucumber/cucumber');
let fs = require('fs');
const {
    createDocumentInBizEventsDatastore,
    createDocumentInBizEventsDatastoreWithIUVAndOrgCode,
    deleteDocumentFromBizEventsDatastore } = require("./biz_events_datastore_client");
const {
    createDocumentInReceiptsDatastore,
    deleteDocumentFromReceiptsDatastore,
    createDocumentInReceiptErrorDatastore,
    deleteDocumentFromReceiptsErrorDatastore,
    createDocumentInReceiptIoMessageDatastore,
    deleteDocumentInReceiptIoMessageDatastore,

    createDocumentInReceiptsCartDatastore,
    deleteDocumentFromReceiptsCartDatastore,
    createDocumentInReceiptsCartErrorDatastore,
    deleteDocumentFromReceiptsCartErrorDatastore,
    createDocumentInReceiptCartIoMessageDatastore,
    deleteDocumentInReceiptCartIoMessageDatastore,

    deleteMultipleDocumentsFromReceiptsDatastoreByEventId,
    deleteMultipleDocumentFromReceiptErrorDatastoreByEventId
} = require("./receipts_datastore_client");
const {
    getReceipt,
    getReceiptByOrganizationFiscalCodeAndIUV,
    getReceiptError,
    getReceiptMessage,
    getReceiptPdf,
    getCartReceipt,
    getCartReceiptByOrganizationFiscalCodeAndIUV,
    getCartReceiptError,
    getCartReceiptMessage
} = require("./api_helpdesk_client");
const { uploadBlobFromLocalPath, deleteBlob } = require("./blob_storage_client");

// set timeout for Hooks function, it allows to wait for long task
setDefaultTimeout(360 * 1000);

// initialize variables
let eventId = null;
let messageId = null;
let responseAPI = null;
let receipt = null;
let receiptError = null;
let receiptMessage = null;
let receiptPdfFileName = null;
let cartId = null;
let cartErrorId = null;

// After each Scenario
After(async function () {
    // remove event
    if (eventId != null) {
        await deleteDocumentFromBizEventsDatastore(eventId);
        await deleteMultipleDocumentsFromReceiptsDatastoreByEventId(eventId);
        await deleteMultipleDocumentFromReceiptErrorDatastoreByEventId(eventId);
    }
    if (receipt != null) {
        await deleteDocumentFromReceiptsDatastore(receipt.id);
        await deleteDocumentFromReceiptsCartDatastore(receipt.id);
    }
    if (receiptError != null) {
        await deleteDocumentFromReceiptsErrorDatastore(receipt.id);
        await deleteDocumentFromReceiptsCartErrorDatastore(receipt.id);
    }
    if (receiptPdfFileName != null) {
        await deleteBlob(receiptPdfFileName);
        if (fs.existsSync(receiptPdfFileName)) {
            fs.unlinkSync(receiptPdfFileName);
        }
    }
    if (messageId != null) {
        await deleteDocumentInReceiptIoMessageDatastore(messageId);
        await deleteDocumentInReceiptCartIoMessageDatastore(messageId);
    }
    if (cartId != null) {
        await deleteDocumentFromReceiptsCartDatastore(cartId);
    }
    if (cartErrorId != null) {
        await deleteDocumentFromReceiptsCartErrorDatastore(cartErrorId);
    }

    eventId = null;
    responseAPI = null;
    receipt = null;
    receiptError = null;
    receiptPdfFileName = null;
    messageId = null;
    cartId = null;
    cartErrorId = null;
});

//Given
Given('a biz event with id {string} and status {string} stored on biz-events datastore', async function (id, status) {
    eventId = id;
    // prior cancellation to avoid dirty cases
    await deleteDocumentFromBizEventsDatastore(eventId);

    let bizEventStoreResponse = await createDocumentInBizEventsDatastore(eventId, status);
    assert.strictEqual(bizEventStoreResponse.statusCode, 201);
});

Given('a biz event with id {string} and status {string} and organizationFiscalCode {string} and IUV {string} stored on biz-events datastore', async function (id, status, orgCode, iuv) {
    eventId = id;
    // prior cancellation to avoid dirty cases
    await deleteDocumentFromBizEventsDatastore(eventId);

    let bizEventStoreResponse = await createDocumentInBizEventsDatastoreWithIUVAndOrgCode(id, status, orgCode, iuv);
    assert.strictEqual(bizEventStoreResponse.statusCode, 201);
});

Given('a receipt with eventId {string} and status {string} stored into receipt datastore', async function (id, status) {
    eventId = id;
    // prior cancellation to avoid dirty cases
    await deleteDocumentFromReceiptsDatastore(id);

    let receiptsStoreResponse = await createDocumentInReceiptsDatastore(id, status);
    assert.strictEqual(receiptsStoreResponse.statusCode, 201);
});

Given('a receipt-error with bizEventId {string} and status {string} stored into receipt-error datastore', async function (id, status) {
    eventId = id;
    // prior cancellation to avoid dirty cases
    await deleteDocumentFromReceiptsErrorDatastore(id);

    let receiptsStoreResponse = await createDocumentInReceiptErrorDatastore(id, status);
    assert.strictEqual(receiptsStoreResponse.statusCode, 201);
});

Given("a receipt pdf with filename {string} stored into blob storage", async function (fileName) {
    receiptPdfFileName = fileName;
    // prior cancellation to avoid dirty cases
    await deleteBlob(fileName);

    fs.writeFileSync(fileName, "", "binary");
    let blobStorageResponse = await uploadBlobFromLocalPath(fileName, fileName);
    assert.notStrictEqual(blobStorageResponse.status, 500);
});

Given('a receipt-io-message with bizEventId {string} and messageId {string} stored into receipt-io-message datastore', async function (eventId, messageId) {
    messageId = messageId;
    // prior cancellation to avoid dirty cases
    await deleteDocumentInReceiptIoMessageDatastore(messageId);

    let receiptsMessageStoreResponse = await createDocumentInReceiptIoMessageDatastore(eventId, messageId);
    assert.strictEqual(receiptsMessageStoreResponse.statusCode, 201);
});

Given('a cart with id {string} stored into cart datastore', async function (id) {
    cartId = id;
    // prior cancellation to avoid dirty cases
    await deleteDocumentFromReceiptsCartDatastore(id, id);

    let cartStoreResponse = await createDocumentInReceiptsCartDatastore(id, "PAYER_FISCAL_CODE", id + "_PAYER", "DEBTOR_FISCAL_CODE", id + "_DEBTOR", "pdfName" /* TODO */);
    assert.strictEqual(cartStoreResponse.statusCode, 201);
});

Given('a cart-receipt-error with cartId {string} and status {string} stored into cart-receipt-error datastore', async function (id, status) {
    cartErrorId = id;
    // prior cancellation to avoid dirty cases
    await deleteDocumentFromReceiptsCartErrorDatastore(id);

    let receiptsStoreResponse = await createDocumentInReceiptsCartErrorDatastore(id, status);
    assert.strictEqual(receiptsStoreResponse.statusCode, 201);
});

Given('a cart-receipt-io-message with bizEventId {string} and messageId {string} stored into cart-receipt-io-message datastore', async function (eventId, messageId) {
    messageId = messageId;
    // prior cancellation to avoid dirty cases
    await deleteDocumentInReceiptCartIoMessageDatastore(messageId);

    let receiptsMessageStoreResponse = await createDocumentInReceiptCartIoMessageDatastore(eventId, messageId);
    assert.strictEqual(receiptsMessageStoreResponse.statusCode, 201);
});

//When
When("getReceipt API is called with eventId {string}", async function (id) {
    responseAPI = await getReceipt(id);
    receipt = responseAPI.data;
});

When("getReceiptByOrganizationFiscalCodeAndIUV API is called with organizationFiscalCode {string} and IUV {string}", async function (orgCode, iuv) {
    responseAPI = await getReceiptByOrganizationFiscalCodeAndIUV(orgCode, iuv);
    receipt = responseAPI.data;
});

When("getReceiptError API is called with bizEventId {string}", async function (id) {
    responseAPI = await getReceiptError(id);
    receiptError = responseAPI.data;
});

When("getReceiptPdf API is called with filename {string}", async function (filename) {
    responseAPI = await getReceiptPdf(filename);
});

When("getReceiptMessage API is called with messageId {string}", async function (id) {
    responseAPI = await getReceiptMessage(id);
    receiptMessage = responseAPI.data;
});

When("getCartReceipt API is called with cartId {string}", async function (id) {
    responseAPI = await getCartReceipt(id);
    receipt = responseAPI.data;
});

When("getCartReceiptByOrganizationFiscalCodeAndIUV API is called with organizationFiscalCode {string} and IUV {string}", async function (orgCode, iuv) {
    responseAPI = await getCartReceiptByOrganizationFiscalCodeAndIUV(orgCode, iuv);
    receipt = responseAPI.data;
});

When("getCartReceiptError API is called with cartId {string}", async function (id) {
    responseAPI = await getCartReceiptError(id);
    receiptError = responseAPI.data;
});

When("getCartReceiptMessage API is called with messageId {string}", async function (id) {
    responseAPI = await getCartReceiptMessage(id);
    receiptMessage = responseAPI.data;
});

//Then
Then('the api response has a {int} Http status', function (expectedStatus) {
    assert.strictEqual(responseAPI.status, expectedStatus);
});

Then('the receipt has eventId {string}', function (targetId) {
    assert.strictEqual(receipt.eventId, targetId);
});

Then("the receipt-error has bizEventId {string}", async function (id) {
    assert.strictEqual(receiptError.bizEventId, id);
});

Then("the receipt-error payload has bizEvent decrypted with eventId {string}", async function (id) {
    let messagePayload = JSON.parse(receiptError.messagePayload);
    assert.strictEqual(messagePayload.id, id);
});

Then("the receipt-message has eventId {string}", async function (id) {
    assert.strictEqual(receiptMessage.eventId, id);
});

Then("the receipt-message has messageId {string}", async function (id) {
    assert.strictEqual(receiptMessage.messageId, id);
});

Then('the receipt has cartId {string}', function (id) {
    assert.strictEqual(receipt.id, id);
});

Then("the cart-receipt-error has cartId {string}", async function (id) {
    assert.strictEqual(receiptError.id, id);
});

Then("the cart-receipt-error payload has bizEvent decrypted with eventId {string}", async function (id) {
    let messagePayload = JSON.parse(receiptError.messagePayload);
    assert.strictEqual(messagePayload.id, id);
});



