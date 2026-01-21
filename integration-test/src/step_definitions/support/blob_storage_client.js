const { BlobServiceClient } = require('@azure/storage-blob');

const blobStorageConnString = process.env.RECEIPTS_STORAGE_CONN_STRING;
const blobStorageContainerName = process.env.BLOB_STORAGE_CONTAINER_NAME;

const blobServiceClient = BlobServiceClient.fromConnectionString(blobStorageConnString || "");
const containerClient = blobServiceClient.getContainerClient(blobStorageContainerName || "");

async function uploadBlobFromLocalPath(fileName, localFilePath) {
    const blobClient = containerClient.getBlockBlobClient(fileName);

    try {
        return await blobClient.uploadFile(localFilePath);
    } catch (err) {
        return { status: 500 }
    }
}

async function deleteBlob(blobName) {
    // include: Delete the base blob and all of its snapshots.
    // only: Delete only the blob's snapshots and not the blob itself.
    const options = {
        deleteSnapshots: 'include' // or 'only'
    }

    // Create blob client from container client
    const blockBlobClient = containerClient.getBlockBlobClient(blobName);

    await blockBlobClient.deleteIfExists(options);
}

module.exports = {
    uploadBlobFromLocalPath,
    deleteBlob,
}