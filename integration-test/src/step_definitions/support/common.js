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
	let queryParams = '';
	let headers = {};
	if (environment === "local") {	
		queryParams = fiscalCode ? `?fiscal_code=${fiscalCode}` : '';
	} else {
		headers = fiscalCode ? {"fiscal_code": fiscalCode} : {};
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
              console.log(error.request._header);
              console.log("RESPONSE WITH STATUS " + error.response.status + " AND DATA ");
              console.log(error.response.data);
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