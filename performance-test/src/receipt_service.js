import { check } from 'k6';
import { getAttachment, getAttachmentDetails } from './modules/receipt_service_client';
import { createReceipt } from './modules/common';
import { createDocument, deleteDocument } from './modules/receipt_cosmos_client';

export let options = JSON.parse(open(__ENV.TEST_TYPE));

const fiscalCode = "JHNDOE00A01F205N";
const partitionId = "receipt-service-perf-test-id-1";
let receiptId = "";
let attachmentUrl = "";

const varsArray = new SharedArray('vars', function () {
    return JSON.parse(open(`./${__ENV.VARS}`)).environment;
});
const vars = varsArray[0];
const receiptServiceURIBasePath = `${vars.receiptServiceURIBasePath}`;
const receiptServiceGetAttachmentPath = `${vars.receiptServiceGetAttachmentPath}`;
const receiptServiceGetAttachmentDetailsPath = `${vars.receiptServiceGetAttachmentDetailsPath}`;

const receiptCosmosDBURI = `${vars.receiptCosmosDBURI}`;
const receiptDatabaseID = `${vars.receiptDatabaseID}`;
const receiptContainerID = `${vars.receiptContainerID}`;
const receiptCosmosDBPrimaryKey = `${__ENV.COSMOS_RECEIPT_KEY}`;

const blobStorageConnString = `${__ENV.BLOB_STORAGE_CONN_STRING}`;
const blobStorageContainerID = `${vars.blobStorageContainerID}`;

export function setup() {
    // 2. setup code
    //Save pdf on blob storage
    let pdfUrl = "";
    let pdfFile = converPdfToBase64("testPDF.pdf", "../resources/.testPDF.pdf");
    let response  = uploadDocumentToAzure(blobStorageConnString, blobStorageContainerID, partitionId, pdfFile);
    console.log("RESPONSE SAVE PDF", response, response.status, response.body);

    //Save receipt on CosmosDB
    response = createDocument(
        receiptCosmosDBURI,
        receiptDatabaseID,
        receiptContainerID,
        receiptCosmosDBPrimaryKey,
        createReceipt(partitionId, partitionId, pdfUrl),
        partitionId
    );
    console.log("RESPONSE CREATE RECEIPT", response, response.status, response.body);
}

export default function (data) {
    // 3. VU code
    //getAttachmentDetails
    let postURI = receiptServiceURIBasePath + receiptServiceGetAttachmentDetailsPath;
    let response = getAttachmentDetails(postURI, { receiptId, fiscalCode });

    console.log("Receipt Service getAttachmentDetails call, Status " + response.status);

    check(response, {
        'Receipt Service getAttachmentDetails status is 200': (response) => response.status === 200,
        'Receipt Service getAttachmentDetails body has attachment url': (response) => response?.body?.attachmentUrl
    });

    attachmentUrl = response.body.attachmentUrl;

    //getAttachment
    postURI = receiptServiceURIBasePath + receiptServiceGetAttachmentPath;
    response = getAttachment(postURI, { receiptId, fiscalCode, attachmentUrl });

    console.log("Receipt Service getAttachment call, Status " + response.status);

    check(response, {
        'Receipt Service getAttachment status is 200': (response) => response.status === 200,
        'Receipt Service getAttachment content_type is the expected application/pdf': (response) => response.headers["Content-Type"] === "application/pdf",
        'Receipt Service getAttachment body not null': (response) => response.body !== null
    });
}

export function teardown(data) {
    // 4. teardown code
    //Delete pdf from blob storage
    let response = deleteDocument(receiptCosmosDBURI, receiptDatabaseID, receiptContainerID, receiptCosmosDBPrimaryKey, partitionId);
    console.log("RESPONSE DELETE RECEIPT", response, response.status, response.body);
    //Delete receipt from blob storage
    response = deleteDocumentFromAzure(blobStorageConnString, blobStorageContainerID, partitionId);
    console.log("RESPONSE DELETE PDF", response, response.status, response.body);

}