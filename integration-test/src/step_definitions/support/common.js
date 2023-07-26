const axios = require("axios");

const uri = process.env.SERVICE_URI;
const environment = process.env.ENVIRONMENT;

axios.defaults.headers.common['Ocp-Apim-Subscription-Key'] = process.env.SUBKEY || ""; // for all requests
if (process.env.canary) {
  axios.defaults.headers.common['X-CANARY'] = 'canary' // for all requests
}


function getAttachmentDetails(receiptId, fiscalCode) {
	let url = uri + "/" + receiptId;

	return httpGET(url, fiscalCode);
}

function getAttachment(receiptId, blobName, fiscalCode) {
	let url = uri + "/" + receiptId + "/" + blobName;

	return httpGET(url, fiscalCode);
}

function httpGET(url, fiscalCode) {
	let queryParams = null;
	let headers = {};
	if (environment === "local") {	
		queryParams = fiscalCode ? `?fiscal_code=${fiscalCode}` : null;
	} else {
		headers = fiscalCode ? {"fiscal_code": fiscalCode} : null;
	}

	return axios.get(url+queryParams, { headers })
		.then(res => {
			return res;
		})
		.catch(error => {
		if (error.response) {
              // The request was made and the server responded with a status code
              // that falls out of the range of 2xx
              console.log("CALL TO " + url+queryParams + " AND HEADER ");
              console.log(headers);
              console.log("RESPONSE WITH STATUS " + error.response.status + " AND DATA ");
              console.log(error.response.data);
            } else if (error.request) {
              // The request was made but no response was received
              // `error.request` is an instance of XMLHttpRequest in the browser and an instance of
              // http.ClientRequest in node.js
              console.log("GENERIC ERROR ");
              console.log(error.request);
            }
			return error.response;
		});
}

function createReceipt(id, fiscalCode, pdfName) {
	let receipt =
	{
		"eventId": id,
		"eventData": {
			"debtorFiscalCode": fiscalCode,
			"payerFiscalCode": fiscalCode
		},
		"status": "IO_NOTIFIED",
		"mdAttach": {
			"name": pdfName,
			"url": pdfName
		},
		"id": id
	}
	return receipt
}

module.exports = {
	createReceipt, getAttachmentDetails, getAttachment
}