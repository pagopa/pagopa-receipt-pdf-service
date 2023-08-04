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