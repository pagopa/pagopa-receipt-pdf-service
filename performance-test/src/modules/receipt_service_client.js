import http from 'k6/http';

const subKey = `${__ENV.OCP_APIM_SUBSCRIPTION_KEY}`;

export function getToService(url, fiscalCode, endpointName) {

  let headers = {
    'Ocp-Apim-Subscription-Key': subKey,
    fiscal_code: fiscalCode
  };

  const params = {
    headers,
    responseType: "text",
    tags: {
      // `name` groups URLs with the same logical endpoint together in k6
      // metrics, even if the path varies (e.g. different ids).
      name: endpointName || 'unknown',
      endpoint: endpointName || 'unknown',
    },
  };

  return http.get(url, params);
}

