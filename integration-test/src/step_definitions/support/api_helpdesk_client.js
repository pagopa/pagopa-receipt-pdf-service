const axios = require("axios");

// Configuring a dedicated instance
const helpdeskClient = axios.create({
    baseURL: process.env.HELPDESK_URL,
    headers: {
        'Ocp-Apim-Subscription-Key': process.env.HELPDESK_SUBKEY || ""
    }
});

// Conditional management of Canary
if (process.env.canary) {
    helpdeskClient.defaults.headers.common['X-CANARY'] = 'canary';
}

/**
 * Internal helper function to handle GET calls
 * Reduces duplication of catch/return response code
 */
async function performGet(endpoint) {
    try {
        return await helpdeskClient.get(endpoint);
    } catch (error) {
        return error.response;
    }
}

// --- API Functions ---

async function getReceipt(id) {
    let endpoint = process.env.GET_RECEIPT_ENDPOINT || "receipts/{event-id}";
    endpoint = endpoint.replace("{event-id}", id);
    return await performGet(endpoint);
}

async function getReceiptMessage(id) {
    let endpoint = process.env.GET_RECEIPT_ENDPOINT || "receipts/io-message/{message-id}";
    endpoint = endpoint.replace("{message-id}", id);
    return await performGet(endpoint);
}

async function getReceiptByOrganizationFiscalCodeAndIUV(orgCode, iuv) {
    let endpoint = process.env.GET_RECEIPT_BY_ORGCODE_AND_IUV_ENDPOINT || "receipts/organizations/{organization-fiscal-code}/iuvs/{iuv}";
    endpoint = endpoint.replace("{organization-fiscal-code}", orgCode);
    endpoint = endpoint.replace("{iuv}", iuv);
    return await performGet(endpoint);
}

async function getReceiptError(id) {
    let endpoint = process.env.GET_RECEIPT_ERROR_ENDPOINT || "errors-toreview/{bizvent-id}";
    endpoint = endpoint.replace("{bizvent-id}", id);
    return await performGet(endpoint);
}

async function getReceiptPdf(fileName) {
    let endpoint = process.env.GET_RECEIPT_PDF_ENDPOINT || "pdf-receipts/{file-name}";
    endpoint = endpoint.replace("{file-name}", fileName);
    return await performGet(endpoint);
}

async function getCartReceipt(id) {
    let endpoint = process.env.GET_CART_RECEIPT_ENDPOINT || "cart-receipts/{cart-id}";
    endpoint = endpoint.replace("{cart-id}", id);
    return await performGet(endpoint);
}

async function getCartReceiptByOrganizationFiscalCodeAndIUV(orgCode, iuv) {
    let endpoint = process.env.GET_CART_RECEIPT_BY_ORGCODE_AND_IUV_ENDPOINT || "cart-receipts/organizations/{organization-fiscal-code}/iuvs/{iuv}";
    endpoint = endpoint.replace("{organization-fiscal-code}", orgCode);
    endpoint = endpoint.replace("{iuv}", iuv);
    return await performGet(endpoint);
}

async function getCartReceiptMessage(id) {
    let endpoint = process.env.GET_CART_RECEIPT_ENDPOINT || "cart-receipts/io-message/{message-id}";
    endpoint = endpoint.replace("{message-id}", id);
    return await performGet(endpoint);
}

async function getCartReceiptError(id) {
    let endpoint = process.env.GET_CART_RECEIPT_ERROR_ENDPOINT || "cart-errors-toreview/{cart-id}";
    endpoint = endpoint.replace("{cart-id}", id);
    return await performGet(endpoint);
}

module.exports = {
    getReceipt,
    getReceiptByOrganizationFiscalCodeAndIUV,
    getReceiptError,
    getReceiptMessage,
    getReceiptPdf,
    getCartReceipt,
    getCartReceiptByOrganizationFiscalCodeAndIUV,
    getCartReceiptError,
    getCartReceiptMessage
};