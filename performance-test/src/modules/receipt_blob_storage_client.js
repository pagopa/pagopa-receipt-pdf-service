import http from 'k6/http';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';

const blobMsVersion = "2020-04-08";

function getStorageSharedKeyLiteAuthToken(verb, authorizationSignature, dateUtc, accountName, containerId, fileName) {
  //Decode Auth Signature
  let key = encoding.b64decode(authorizationSignature);

  //Compone string to sign
  let stringToSign = verb + "\n" + //VERB
    "" + "\n" + //Content-MD5
    "" + "\n" + //Content-Type
    dateUtc + "\n" + //Date
    `x-ms-date:${dateUtc}\nx-ms-version:${blobMsVersion}` + "\n" + //CanonicalizedHeaders
    `/${accountName}/${containerId}/${fileName}.pdf`; //CanconicalizedResources

    console.log("STRING", stringToSign);

  //Encode string
  let hmacSha256 = crypto.createHMAC("sha256", key);
  hmacSha256.update(stringToSign);

  return `SharedKeyLite ${accountName}:${hmacSha256.digest("base64")}`;
}


function getStorageAPIHeaders(method, accountName, containerId, key, length, fileName) {
  const strTime = new Date().toUTCString();

  return {
    'Authorization': getStorageSharedKeyLiteAuthToken(method, key, strTime, accountName, containerId, fileName),
    'x-ms-version': blobMsVersion,
    'x-ms-date': strTime,
    "Content-Length": length
  };
}

export async function uploadDocumentToAzure(accountName, storageKey, containerId, blobId, document) {
  const headers = getStorageAPIHeaders("PUT", accountName, containerId, storageKey, document.length, blobId);

  const body = document;

  let resp = http.put(`https://${accountName}.blob.core.windows.net/${containerId}/${blobId}.pdf`, body, { headers });

  console.log("SAMU", resp.status_text, headers);

  return resp;
};

export async function deleteDocumentFromAzure(accountName, storageKey, containerId, blobId) {

  const headers = getStorageAPIHeaders("DELETE", accountName, containerId, storageKey, 0, blobId);

  let response = http.del(`https://${accountName}.blob.core.windows.net/${containerId}/${blobId}.pdf`, { headers });
  if (response._response.status !== 202) {
    throw new Error(`Error deleting ${blobId}`);
  }

  return response;
};
