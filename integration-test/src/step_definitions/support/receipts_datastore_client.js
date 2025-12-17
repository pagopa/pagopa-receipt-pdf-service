const { CosmosClient } = require("@azure/cosmos");
const { createReceipt, createCartReceipt } = require("./common");

const cosmos_db_conn_string     = process.env.RECEIPTS_COSMOS_CONN_STRING || "";
const databaseId                = process.env.RECEIPT_COSMOS_DB_NAME;
const receiptContainerId        = process.env.RECEIPT_COSMOS_DB_CONTAINER_NAME;
const receiptCartContainerId    = process.env.RECEIPT_CART_COSMOS_DB_CONTAINER_NAME;

const client = new CosmosClient(cosmos_db_conn_string);
const receiptContainer = client.database(databaseId).container(receiptContainerId);
const receiptCartContainer = client.database(databaseId).container(receiptCartContainerId);

async function createDocumentInReceiptsDatastore(id, fiscalCode, pdfName) {
    let event = createReceipt(id, fiscalCode, pdfName);
    try {
        return await receiptContainer.items.create(event);
    } catch (err) {
        console.log(err);
    }
}

async function createDocumentInReceiptsCartDatastore(id, payerFiscalCode, payerBizEventId, debtorFiscalCode, debtorBizEventId, pdfName) {
    let event = createCartReceipt(id, payerFiscalCode, payerBizEventId, debtorFiscalCode, debtorBizEventId, pdfName);
    try {
        return await receiptCartContainer.items.create(event);
    } catch (err) {
        console.log(err);
    }
}

async function deleteDocumentFromReceiptsDatastore(id, partitionKey) {
    try {
        return await receiptContainer.item(id, partitionKey).delete();
    } catch (error) {
        if (error.code !== 404) {
            console.log(error)
        }
    }
}

async function deleteDocumentFromReceiptsCartDatastore(id, partitionKey) {
    try {
        return await receiptCartContainer.item(id, partitionKey).delete();
    } catch (error) {
        if (error.code !== 404) {
            console.log(error)
        }
    }
}

module.exports = {
    createDocumentInReceiptsDatastore, deleteDocumentFromReceiptsDatastore,
    createDocumentInReceiptsCartDatastore, deleteDocumentFromReceiptsCartDatastore
}
