import { check } from 'k6';
import { getToService } from './modules/receipt_service_client.js';
import { SharedArray } from 'k6/data';

const varsArray = new SharedArray('vars', function () {
    return JSON.parse(open(`./${__ENV.VARS}`)).environment;
});
export const ENV_VARS = varsArray[0];

// Carica la config del test (stages/vus/ecc.) e aggiunge le threshold per endpoint.
// Le sub-metriche taggate compaiono nel summary finale di k6 SOLO se hanno una threshold.
const testTypeOptions = JSON.parse(open(__ENV.TEST_TYPE));
export let options = {
    ...testTypeOptions,
    thresholds: {
        ...(testTypeOptions.thresholds || {}),
        'http_req_duration{endpoint:getAttachmentDetails}': ['p(95)<2000', 'p(99)<5000'],
        'http_req_duration{endpoint:getAttachment}':        ['p(95)<3000', 'p(99)<8000'],
        'http_req_failed{endpoint:getAttachmentDetails}':   ['rate<0.01'],
        'http_req_failed{endpoint:getAttachment}':          ['rate<0.01'],
    },
};

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
