import http from 'k6/http';
import {check} from 'k6';
import {SharedArray} from 'k6/data';

export let options = JSON.parse(open(__ENV.TEST_TYPE));

// read configuration
// note: SharedArray can currently only be constructed inside init code
// according to https://k6.io/docs/javascript-api/k6-data/sharedarray
const varsArray = new SharedArray('vars', function () {
  return JSON.parse(open(`./${__ENV.VARS}`)).environment;
});
// workaround to use shared array (only array should be used)
const vars = varsArray[0];
const rootUrl = `${vars.host}/${vars.basePath}`;

export function setup() {
  // Before All
  // setup code (once)
  // The setup code runs, setting up the test environment (optional) and generating data
  // used to reuse code for the same VU
  const params = {
    headers: {
      'Content-Type': 'application/json'
    },
  };
  const response = example(rootUrl, params);

  // precondition is moved to default fn because in this stage
  // __VU is always 0 and cannot be used to create env properly
}

function precondition() {
  // no pre conditions
}

function postcondition() {

  // Delete the new entity created
}

export default function () {

  // Create a new spontaneous payment.
  let tag = {
    gpsMethod: "tag",
  };

  let url = `${rootUrl}/${creditor_institution_code}/spontaneouspayments`;

  let payload = JSON.stringify(
      {}
  );

  let params = {
    headers: {
      'Content-Type': 'application/json'
    },
  };

  let r = http.post(url, payload, params);

  check(r, {
    'check status is 201': (_r) => r.status === 201,
  }, tag);

  postcondition();

}

export function teardown(data) {
  // After All
  // teardown code
}
