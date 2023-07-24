const axios = require("axios");

const uri = process.env.SERVICE_URI;

axios.defaults.headers.common['Ocp-Apim-Subscription-Key'] = process.env.SUBKEY // for all requests
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
	let headers = {};//fiscalCode ? {"fiscal_code": fiscalCode} : null;

	let queryParams = fiscalCode ? `?fiscal_code=${fiscalCode}` : null;

	return axios.get(url+queryParams, { headers })

		.then(res => {

			return res;

		})

		.catch(error => {
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