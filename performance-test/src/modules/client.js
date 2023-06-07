import http from 'k6/http';

export function example(rootUrl, params) {
  const url = `${rootUrl}/`

  const payload = {}

  return http.post(url, JSON.stringify(payload), params);
}
