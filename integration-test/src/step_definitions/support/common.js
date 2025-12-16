const axios = require("axios");

const uri = process.env.SERVICE_URI;
const environment = process.env.ENVIRONMENT;
const tokenizer_url = process.env.TOKENIZER_URL;

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

function createCartReceipt(id, payerFiscalCode, payerBizEventId, debtorFiscalCode, debtorBizEventId, pdfName) {
	let receipt =
		{
			"eventId": id,
			"version": "1",
			"payload": {
				"payerFiscalCode": payerFiscalCode,
				"mdAttachPayer": {
					"name": pdfName,
					"url": pdfName
				},
				"messagePayer": {
					"subject": "Payer subject",
					"markdown": "Payer **markdown**"
				},
				"cart": [
					{
						"bizEventId": debtorBizEventId,
						"subject": "Pagamento 1",
						"debtorFiscalCode": debtorFiscalCode,
						"amount": "10,20",
						"mdAttach": {
							"name": pdfName,
							"url": pdfName
						},
						"messageDebtor": {
							"subject": "Cart Debtor subject",
							"markdown": "Cart Debtor **markdown**"
						}
					},
					{
						"bizEventId": payerBizEventId,
						"subject": "Pagamento 2",
						"debtorFiscalCode": payerFiscalCode,
						"amount": "22,15",
						"mdAttach": {
							"name": pdfName,
							"url": pdfName
						},
						"messageDebtor": {
							"subject": "Cart Payer subject",
							"markdown": "Cart Payer **markdown**"
						}
					}
				]
			},
			"status": "IO_NOTIFIED",
			"id": id
		}
	return receipt
}

async function createToken(fiscalCode) {
    let token_api_key = process.env.TOKENIZER_API_KEY;
  	let headers = {
  	  "x-api-key": token_api_key
  	};

  	return await axios.put(tokenizer_url, { "pii": fiscalCode }, { headers })
  		.then(res => {
  			return res.data;
  		})
  		.catch(error => {
  			return error.response;
  		});

}

module.exports = {
	createReceipt, getAttachmentDetails, getAttachment, createToken, createCartReceipt
}
