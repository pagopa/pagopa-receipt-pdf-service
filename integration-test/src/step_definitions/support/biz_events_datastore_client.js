const { CosmosClient } = require("@azure/cosmos");
const { createEvent, createEventWithIUVAndOrgCode } = require("./common");

const cosmos_db_conn_string = process.env.BIZEVENTS_COSMOS_CONN_STRING;
const databaseId = process.env.BIZ_EVENT_COSMOS_DB_NAME;  // es. db
const containerId = process.env.BIZ_EVENT_COSMOS_DB_CONTAINER_NAME; // es. biz-events

const client = new CosmosClient(cosmos_db_conn_string);
const container = client.database(databaseId).container(containerId);

async function createDocumentInBizEventsDatastore(id, status) {
    let event = createEvent(id, status, "org", "iuv");
    try {
        return await container.items.create(event);
    } catch (err) {
        console.log(err);
    }
}

async function createDocumentInBizEventsDatastoreWithIUVAndOrgCode(id, status, orgCode, iuv) {
    let event = createEventWithIUVAndOrgCode(id, status, orgCode, iuv);
    try {
        return await container.items.create(event);
    } catch (err) {
        console.log(err);
    }
}

async function deleteDocumentFromBizEventsDatastore(id) {
    try {
        return await container.item(id, id).delete();
    } catch (error) {
        if (error.code !== 404) {
            console.log(error)
        }
    }
}

module.exports = {
    createDocumentInBizEventsDatastore, 
    createDocumentInBizEventsDatastoreWithIUVAndOrgCode,
    deleteDocumentFromBizEventsDatastore,
}