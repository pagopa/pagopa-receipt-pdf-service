import http from 'k6/http';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';

const authorizationType      = "master"
const authorizationVersion   = "1.0";
const cosmosDBApiVersion     = "2018-12-31";

function getCosmosDBAuthorizationToken(verb, autorizationType, autorizationVersion, authorizationSignature, resourceType, resourceLink, dateUtc) {
    // Decode authorization signature
    let key = encoding.b64decode(authorizationSignature);
    let text = (verb || "").toLowerCase() + "\n" +
        (resourceType || "").toLowerCase() + "\n" +
        (resourceLink || "") + "\n" +
        dateUtc.toLowerCase() + "\n\n";
    let hmacSha256 = crypto.createHMAC("sha256", key);
    hmacSha256.update(text);
    // Build autorization token, encode it and return
    return encodeURIComponent("type=" + autorizationType + "&ver=" + autorizationVersion + "&sig=" + hmacSha256.digest("base64"));
}

function getCosmosDBAPIHeaders(authorizationToken, date, partitionKeyArray, contentType, isQuery){

    return {'Accept': 'application/json',
        'Content-Type': contentType,
        'Authorization': authorizationToken,
        'x-ms-version': cosmosDBApiVersion,
        'x-ms-date': date,
        'x-ms-documentdb-isquery': isQuery ? isQuery : "false",
        'x-ms-query-enable-crosspartition': 'true',
        'x-ms-documentdb-partitionkey': partitionKeyArray
    };
}

export function createDocument(cosmosDbURI, databaseId, containerId, authorizationSignature, document, pk) {
	let path = `dbs/${databaseId}/colls/${containerId}/docs`;
	let resourceLink = `dbs/${databaseId}/colls/${containerId}`;
	// resource type (colls, docs...)
	let resourceType = "docs"
	let date = new Date().toUTCString();
	// request method (a.k.a. verb) to build text for authorization token
    let verb = 'post';
	let authorizationToken = getCosmosDBAuthorizationToken(verb,authorizationType,authorizationVersion,authorizationSignature,resourceType,resourceLink,date);

	let partitionKeyArray = "[\""+pk+"\"]";
	let headers = getCosmosDBAPIHeaders(authorizationToken, date, partitionKeyArray, 'application/json');

	const body = JSON.stringify(document);

    let resp = http.post(cosmosDbURI+path, body, {headers});

    return resp;
}

export function deleteDocument(cosmosDbURI, databaseId, containerId, authorizationSignature, id) {
    const path = `dbs/${databaseId}/colls/${containerId}/docs/${id}`;
    const resourceLink = path;
    const resourceType = "docs"
    const date = new Date().toUTCString();
    const verb = 'delete';
    const partitionKeyArray = "[\""+id+"\"]";

    let authorizationToken = getCosmosDBAuthorizationToken(verb,authorizationType,authorizationVersion,authorizationSignature,resourceType,resourceLink,date);
    let headers = getCosmosDBAPIHeaders(authorizationToken, date, partitionKeyArray, 'application/json');

    let resp = http.del(cosmosDbURI+path, null, {headers});

    return resp;
}