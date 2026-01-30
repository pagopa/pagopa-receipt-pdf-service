const { CosmosClient } = require("@azure/cosmos");
const { createReceipt, createCartReceipt, createReceiptError, createReceiptMessage, createReceiptCartError, createReceiptCartMessage } = require("./common");

const cosmos_db_conn_string = process.env.RECEIPTS_COSMOS_CONN_STRING || "";
const databaseId = process.env.RECEIPT_COSMOS_DB_NAME;
const receiptContainerId = process.env.RECEIPT_COSMOS_DB_CONTAINER_NAME;
const receiptErrorContainerId = process.env.RECEIPT_ERROR_COSMOS_DB_CONTAINER_NAME;
const receiptMessageContainerId = process.env.RECEIPT_MESSAGE_COSMOS_DB_CONTAINER_NAME;
const receiptCartContainerId = process.env.RECEIPT_CART_COSMOS_DB_CONTAINER_NAME;
const receiptCartErrorContainerId = process.env.RECEIPT_CART_ERROR_COSMOS_DB_CONTAINER_NAME;
const receiptCartMessageContainerId = process.env.RECEIPT_CART_MESSAGE_COSMOS_DB_CONTAINER_NAME;

const client = new CosmosClient(cosmos_db_conn_string);
const receiptContainer = client.database(databaseId).container(receiptContainerId);
const receiptErrorContainer = client.database(databaseId).container(receiptErrorContainerId);
const receiptMessageContainer = client.database(databaseId).container(receiptMessageContainerId);
const receiptCartContainer = client.database(databaseId).container(receiptCartContainerId);
const receiptCartErrorContainer = client.database(databaseId).container(receiptCartErrorContainerId);
const receiptCartMessageContainer = client.database(databaseId).container(receiptCartMessageContainerId);

// RECEIPT
async function createDocumentInReceiptsDatastore(id, fiscalCode, pdfName, status, reasonErrorCode) {
    let receipt = createReceipt(id, fiscalCode, pdfName, status, reasonErrorCode);
    try {
        return await receiptContainer.items.create(receipt);
    } catch (err) {
        console.log(err);
        throw err;
    }
}

async function deleteDocumentFromReceiptsDatastore(id, partitionKey) {
    try {
        return await receiptContainer.item(id, id || partitionKey).delete();
    } catch (error) {
        if (error.code !== 404) {
            console.log(error)
        }
    }
}

//RECEIPT ERROR
async function createDocumentInReceiptErrorDatastore(id, status) {
    let receipt = createReceiptError(id, status);
    try {
        return await receiptErrorContainer.items.create(receipt);
    } catch (err) {
        console.log(err);
    }
}

async function deleteDocumentFromReceiptsErrorDatastore(id, partitionKey) {
    try {
        return await receiptErrorContainer.item(id, id || partitionKey).delete();
    } catch (error) {
        if (error.code !== 404) {
            console.log(error)
        }
    }
}

//RECEIPT MESSAGE
async function createDocumentInReceiptIoMessageDatastore(eventId, messageId) {
    let message = createReceiptMessage(eventId, messageId);
    try {
        return await receiptMessageContainer.items.create(message);
    } catch (err) {
        console.log(err);
    }
}

async function deleteDocumentInReceiptIoMessageDatastore(id, partitionKey) {
    try {
        return await receiptMessageContainer.item(id, partitionKey ?? id).delete();
    } catch (error) {
        if (error.code !== 404) {
            console.log(error)
        }
    }
}

// CART RECEIPT
async function createDocumentInReceiptsCartDatastore(id, payerFiscalCode, payerBizEventId, debtorFiscalCode, debtorBizEventId, pdfName, status, payerReasonErrCode, debtorReasonErrCode) {
    let receipt = createCartReceipt(id, payerFiscalCode, payerBizEventId, debtorFiscalCode, debtorBizEventId, pdfName, status, payerReasonErrCode, debtorReasonErrCode);
    try {
        return await receiptCartContainer.items.create(receipt);
    } catch (err) {
        console.log(err);
    }
}

async function deleteDocumentFromReceiptsCartDatastore(id, partitionKey) {
    try {
        return await receiptCartContainer.item(id, id || partitionKey).delete();
    } catch (error) {
        if (error.code !== 404) {
            console.log(error)
        }
    }
}

// CART RECEIPT ERROR
async function createDocumentInReceiptsCartErrorDatastore(id, status) {
    let receipt = createReceiptCartError(id, status);
    try {
        return await receiptCartErrorContainer.items.create(receipt);
    } catch (err) {
        console.log(err);
    }
}

async function deleteDocumentFromReceiptsCartErrorDatastore(id, partitionKey) {
    try {
        return await receiptCartErrorContainer.item(id, id || partitionKey).delete();
    } catch (error) {
        if (error.code !== 404) {
            console.log(error)
        }
    }
}

//CART RECEIPT MESSAGE
async function createDocumentInReceiptCartIoMessageDatastore(eventId, messageId) {
    let message = createReceiptCartMessage(eventId, messageId);
    try {
        return await receiptCartMessageContainer.items.create(message);
    } catch (err) {
        console.log(err);
    }
}

async function deleteDocumentInReceiptCartIoMessageDatastore(id, partitionKey) {
    try {
        return await receiptCartMessageContainer.item(id, id || partitionKey).delete();
    } catch (error) {
        if (error.code !== 404) {
            console.log(error)
        }
    }
}

// CLEANUP METHODS
async function getDocumentFromReceiptsDatastoreByEventId(id) {
    return await receiptContainer.items
        .query({
            query: "SELECT * from c WHERE c.eventId=@eventId",
            parameters: [{ name: "@eventId", value: id }]
        })
        .fetchNext();
}
async function deleteMultipleDocumentsFromReceiptsDatastoreByEventId(eventId) {
    let documents = await getDocumentFromReceiptsDatastoreByEventId(eventId);

    for(let doc of documents?.resources) {
        await deleteDocumentFromReceiptsDatastore(doc.id, doc.id);
    }
}

async function getDocumentFromReceiptsErrorDatastoreByBizEventId(id) {
    return await receiptErrorContainer.items
        .query({
            query: "SELECT * from c WHERE c.bizEventId=@bizEventId",
            parameters: [{ name: "@bizEventId", value: id }]
        })
        .fetchNext();
}
async function deleteMultipleDocumentFromReceiptErrorDatastoreByEventId(id) {
    let documents = await getDocumentFromReceiptsErrorDatastoreByBizEventId(id);

    for(let doc of documents?.resources) {
        await deleteDocumentFromReceiptsErrorDatastore(doc.id, doc.id);
    }
}

module.exports = {
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
}
