export function createReceipt(id, pdfName, pdfUrl) {
	let receipt =
	{
		"eventId": id,
		"eventData": {
			"payerFiscalCode": "JHNDOE00A01F205N",
			"debtorFiscalCode": "JHNDOE00A01F205N"
		},
        "mdAttach":{
            name: pdfName,
            url: pdfUrl
        },
		"status": "IO_NOTIFIED",
		"numRetry": 0
	}
	return receipt
}