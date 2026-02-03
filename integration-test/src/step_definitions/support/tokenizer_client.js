const axios = require("axios");

// Configuring a dedicated instance
const tokenizerClient = axios.create({
    baseURL: process.env.TOKENIZER_URL,
    headers: {
        'x-api-key': process.env.TOKENIZER_API_KEY || ""
    }
});

async function createToken(fiscalCode) {
	return await tokenizerClient.put("", { "pii": fiscalCode })
		.then(res => {
			return res.data;
		})
		.catch(error => {
			return error.response;
		});
}

module.exports = {
    createToken
};