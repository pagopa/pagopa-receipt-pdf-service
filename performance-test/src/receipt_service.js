import { check } from 'k6';
import { getToService } from './modules/receipt_service_client.js';
import { SharedArray } from 'k6/data';

const varsArray = new SharedArray('vars', function () {
    return JSON.parse(open(`./${__ENV.VARS}`)).environment;
});
export const ENV_VARS = varsArray[0];
export let options = JSON.parse(open(__ENV.TEST_TYPE));

const fiscalCode = "JHNDOE00A01F205N";
const receiptId = ENV_VARS.receiptTestId;
const receiptServiceURIBasePath = `${ENV_VARS.receiptServiceURIBasePath}`;

export default function () {
    // getAttachmentDetails
    let response = getToService(
        `${receiptServiceURIBasePath}/${receiptId}`,
        fiscalCode,
        'getAttachmentDetails'
    );

    const responseBody = response.body ? JSON.parse(response.body) : null;
    const attachmentUrl =
        responseBody &&
        responseBody.attachments &&
        responseBody.attachments.length > 0 &&
        responseBody.attachments[0] &&
        responseBody.attachments[0].url;

    check(response, {
        'getAttachmentDetails status is 200': (r) => r.status === 200,
        'getAttachmentDetails body has attachment url': () => !!attachmentUrl,
    });

    if (attachmentUrl) {
        // getAttachment
        response = getToService(
            `${receiptServiceURIBasePath}/${receiptId}/${attachmentUrl}`,
            fiscalCode,
            'getAttachment'
        );

        check(response, {
            'getAttachment status is 200': (r) => r.status === 200,
            'getAttachment content_type is application/pdf':
                (r) => r.headers["Content-Type"] === "application/pdf",
            'getAttachment body not null': (r) => r.body !== null,
        });
    }
}

