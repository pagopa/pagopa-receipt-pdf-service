import {BlobServiceClient} from "@azure/storage-blob";
import {CosmosClient} from "@azure/cosmos";
import {createRequire} from 'node:module';

const require = createRequire(import.meta.url);

const axios = require("axios");

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

//CLIENTS
const blobServiceClient = BlobServiceClient.fromConnectionString(
blobStorageConnString || ""
);
export const blobContainerClient = blobServiceClient.getContainerClient(
blobStorageContainerID || ""
);
const client = new CosmosClient(receiptCosmosDBConnString);
export const receiptContainer = client.database(receiptCosmosDBDatabaseId).container(receiptCosmosDBContainerId);

// Configuring a dedicated instance (declared AFTER environmentVars to avoid TDZ)
const pdfServiceClient = axios.create({
    baseURL: environmentVars.tokenizerUrl,
    headers: {
        "x-api-key": process.env.TOKENIZER_API_KEY || ""
    }
});

//METHODS

export function createToken(fiscalCode) {
  	return pdfServiceClient.put('/search',
  	 { "pii": fiscalCode });
}

export async function createReceipt(id) {
  const tokenResponse = await createToken("JHNDOE00A01F205N");
  const responseBody = tokenResponse.data;

    return {
        "id": id,
        "eventId": id,
        "eventData": {
            "payerFiscalCode": responseBody.token,
            "debtorFiscalCode": responseBody.token
        },
        "mdAttach": {
            name: PDF_NAME,
            url: PDF_NAME
        },
        "status": "IO_NOTIFIED",
        "numRetry": 0
    }
}