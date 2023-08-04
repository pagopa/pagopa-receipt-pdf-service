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

export const fileToBase64 = (filename, filepath) => {
    return new Promise(resolve => {
      let file = new File([filename], filepath);
      let reader = new FileReader();
      // Read file content on file loaded event
      reader.onload = function(event) {
        resolve(event.target.result);
      };
      
      // Convert data to base64 
      reader.readAsDataURL(file);
    });
};

export const converPdfToBase64 = (filename, filepath) => {
    let finalResult;
    fileToBase64(filename, filepath).then(result => {
        finalResult = result;
    })

    return finalResult;
};