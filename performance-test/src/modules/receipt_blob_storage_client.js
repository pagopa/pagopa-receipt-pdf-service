import http from 'k6/http';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';

const blobMsVersion = "2022-11-02";

function getStorageAPIHeaders(method, accountName, key, length) {
  let strTime = (new Date()).toUTCString();
  let strToSign = `${method}\n\n\n\n\n\n\n\n\n\n\n\nx-ms-date:${strTime}\nx-ms-version:${blobMsVersion}\n/${accountName}/\ncomp:block`;
  let secret = encoding.b64decode(key);
  let hmacSha256 = crypto.createHMAC("sha256", secret);
  hmacSha256.update(strToSign);
  let hashInBase64 = hmacSha256.digest("base64");

  let auth = `SharedKey ${accountName}:${hashInBase64}`;

  return {
    'Authorization': auth,
    'x-ms-version': blobMsVersion,
    'x-ms-date': strTime,
    "Content-Length": length
  };
}

export async function uploadDocumentToAzure(accountName, storageKey, containerId, blobId, document) {
  console.log("ASd", document.length);
  const headers = getStorageAPIHeaders("PUT", accountName, storageKey, document.length );

  const body = document;

  

  let resp = http.put(`https://${accountName}.blob.core.windows.net/${containerId}/${blobId}.pdf`, body, { headers });

  console.log("SAMU", resp);

  return resp;
};

export async function deleteDocumentFromAzure(accountName, storageKey, containerId, blobId) {

  const headers = getStorageAPIHeaders("DELETE", accountName, storageKey);

  let response = http.del(`https://${accountName}.blob.core.windows.net/${containerId}/${blobId}.pdf`, { headers });
  if (response._response.status !== 202) {
    throw new Error(`Error deleting ${blobId}`);
  }

  return response;
};
