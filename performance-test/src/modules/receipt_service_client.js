import http from 'k6/http';

const subKey = `${__ENV.SUBSCRIPTION_KEY}`;

export function postToService(url, request) {
    const form = request;

      let headers = { 
        'Ocp-Apim-Subscription-Key': subKey
    };

    return http.post(url, form, {headers});
}

