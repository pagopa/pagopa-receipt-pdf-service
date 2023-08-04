import { BlobServiceClient } from "@azure/storage-blob";

const getContainerClient = (blobConnString, blobContainerId) => {
    const blobServiceClient = BlobServiceClient.fromConnectionString(
        blobConnString
      );
      const containerClient = blobServiceClient.getContainerClient(
        blobContainerId
      );

      return containerClient;
}

export const uploadDocumentToAzure = async (blobConnString, blobContainerId, pdfName, pdfFile) => {
    const containerClient = getContainerClient(blobConnString, blobContainerId);

    const data = Buffer.from(pdfFile, "base64");
    const blockBlobClient = containerClient.getBlockBlobClient(pdfName);
    const response = await blockBlobClient.uploadData(data, {
      blobHTTPHeaders: {
        blobContentType: "application/pdf",
      },
    });
    if (response._response.status !== 201) {
      throw new Error(
        `Error uploading document ${blockBlobClient.name} to container ${blockBlobClient.containerName}`
      );
    }

    return response;
  };

export const deleteDocumentFromAzure = async (blobConnString, blobContainerId, pdfName) => {
    const containerClient = getContainerClient(blobConnString, blobContainerId);

    const response = await containerClient.deleteBlob(pdfName);
    if (response._response.status !== 202) {
      throw new Error(`Error deleting ${pdfName}`);
    }

    return response;
  };