import {BlobServiceClient} from "@azure/storage-blob";
import {CosmosClient} from "@azure/cosmos";
import {createRequire} from 'node:module';

const require = createRequire(import.meta.url);

//ENVIRONMENTAL VARIABLES
const blobStorageConnString = process.env.BLOB_STORAGE_CONN_STRING;
const receiptCosmosDBConnString = process.env.COSMOS_RECEIPTS_CONN_STRING;

const environmentString = process.env.ENVIRONMENT_STRING || "local";
let environmentVars = require(`../${environmentString}.environment.json`)?.environment?.[0] || {};

const blobStorageContainerID = environmentVars.blobStorageContainerID;
const receiptCosmosDBDatabaseId = environmentVars.receiptDatabaseID;
export const receiptCosmosDBContainerId = environmentVars.receiptContainerID;

//CONSTANTS
export const PARTITION_ID = environmentVars.receiptTestId;
export const PDF_NAME = "pagopa-ricevuta-260512-doc-test-ricevute-21d15117-l5ef-435c-80ez-fb6ffadba7rh-p.pdf";
export const TOKENIZED_FISCAL_CODE = "311ba4fb-36e7-4861-8bf3-47bc004d6738";

//CLIENTS
const blobServiceClient = BlobServiceClient.fromConnectionString(
blobStorageConnString || ""
);
export const blobContainerClient = blobServiceClient.getContainerClient(
blobStorageContainerID || ""
);
const client = new CosmosClient(receiptCosmosDBConnString);
export const receiptContainer = client.database(receiptCosmosDBDatabaseId).container(receiptCosmosDBContainerId);

//METHODS

export function createReceipt(id) {
    return {
        "id": id,
        "eventId": id,
        "eventData": {
            "payerFiscalCode": TOKENIZED_FISCAL_CODE,
            "debtorFiscalCode": TOKENIZED_FISCAL_CODE
        },
        "mdAttach": {
            name: PDF_NAME,
            url: PDF_NAME
        },
        "status": "IO_NOTIFIED",
        "numRetry": 0
    }
}