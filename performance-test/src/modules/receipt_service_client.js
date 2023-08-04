import http from 'k6/http';

const subKey = `${__ENV.SUBSCRIPTION_KEY}`;

const varsArray = new SharedArray('vars', function () {
    return JSON.parse(open(`./${__ENV.VARS}`)).environment;
  });
const vars = varsArray[0];
const receiptServiceURIBasePath = `${vars.receiptServiceURIBasePath}`;
const receiptServiceGetAttachmentPath = `${vars.receiptServiceGetAttachmentPath}`;
const receiptServiceGetAttachmentDetailsPath = `${vars.receiptServiceGetAttachmentDetailsPath}`;

export function getAttachment(receiptId, requestFiscalCode, attachmentUrl) {
    const form = {
        thirdPartyId:receiptId, requestFiscalCode, attachmentUrl
      };

      let headers = { 
        'Ocp-Apim-Subscription-Key': subKey
    };

    return http.post(receiptServiceURIBasePath+receiptServiceGetAttachmentPath, form, {headers});
}

export function getAttachmentDetails(receiptId, requestFiscalCode) {
    const form = {
        thirdPartyId:receiptId, requestFiscalCode
      };

      let headers = { 
        'Ocp-Apim-Subscription-Key': subKey
    };

    return http.post(receiptServiceURIBasePath+receiptServiceGetAttachmentDetailsPath, form, {headers});
}