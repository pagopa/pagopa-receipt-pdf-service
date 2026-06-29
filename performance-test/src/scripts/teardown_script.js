import {blobContainerClient, receiptContainer, PARTITION_ID, PDF_NAME} from "./scripts_common.js";

//DELETE PDF FROM BLOB STORAGE
const deleteDocumentFromAzure = async () => {
    const blockBlobClient = blobContainerClient.getBlockBlobClient(PDF_NAME);
    // include: Delete the base blob and all of its snapshots.
    // only: Delete only the blob's snapshots and not the blob itself.
    const options = {
        deleteSnapshots: 'include' // or 'only'
    }

    const response = await blockBlobClient.deleteIfExists(options);
    if (response._response.status !== 202) {
        throw new Error(`Error deleting PDF ${PARTITION_ID}`);
    }

    return response;
};
deleteDocumentFromAzure().then((res) => {
    console.log("RESPONSE DELETE PDF STATUS", res._response.status);
});


//DELETE RECEIPT FROM COSMOSDB
async function deleteDocumentFromReceiptsDatastore() {
    try {
        return await receiptContainer.item(PARTITION_ID, PARTITION_ID).delete();
    } catch (error) {
        if (error.code !== 404) {
            throw new Error(`Error deleting receipt ${PARTITION_ID}`);
        }
    }
}
deleteDocumentFromReceiptsDatastore().then(resp => {
    console.info("RESPONSE DELETE RECEIPT STATUS", resp.statusCode);
});
