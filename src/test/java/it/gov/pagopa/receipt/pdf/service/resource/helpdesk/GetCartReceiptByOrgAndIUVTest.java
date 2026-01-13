package it.gov.pagopa.receipt.pdf.service.resource.helpdesk;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import it.gov.pagopa.receipt.pdf.service.client.BizCosmosClient;
import it.gov.pagopa.receipt.pdf.service.exception.BizEventNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.CartNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.biz.BizEvent;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.service.service.impl.CartReceiptCosmosService;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.PDFS_800;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

@QuarkusTest
class GetCartReceiptByOrgAndIUVTest {

    private static final String ORGANIZATION_FISCAL_CODE = "12345678901";
    private static final String IUV = "IUV_TEST_123";
    private static final String EVENT_ID = "EVENT_ID_XYZ";

    @InjectMock
    private BizCosmosClient bizEventCosmosClient;

    @InjectMock
    private CartReceiptCosmosService cartReceiptCosmosService;

    @Test
    void getCartReceiptByOrgAndIUV_BadRequest_MissingOrganizationFiscalCodeParam() {
        given()
                .pathParam("organization-fiscal-code", " ")
                .pathParam("iuv", "iuv")
                .when()
                .get("/helpdesk/receipts/cart/organizations/{organization-fiscal-code}/iuvs/{iuv}")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("title", equalTo("BAD_REQUEST"))
                .body("detail", equalTo("Please pass a valid organization fiscal code"));
    }

    @Test
    void getCartReceiptByOrgAndIUV_BadRequest_MissingIuvParams() {
        given()
                .pathParam("organization-fiscal-code", "org")
                .pathParam("iuv", " ")
                .when()
                .get("/helpdesk/receipts/cart/organizations/{organization-fiscal-code}/iuvs/{iuv}")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("title", equalTo("BAD_REQUEST"))
                .body("detail", equalTo("Please pass a valid iuv"));
    }

    @Test
    void getCartReceiptByOrgAndIUV_NotFound_BizEventNotFound() throws BizEventNotFoundException {
        String expectedMessage = String.format("Unable to retrieve the biz-event with organization fiscal code %s and iuv %s",
                ORGANIZATION_FISCAL_CODE, IUV);

        when(bizEventCosmosClient.getBizEventDocumentByOrganizationFiscalCodeAndIUV(ORGANIZATION_FISCAL_CODE, IUV))
                .thenThrow(new BizEventNotFoundException(PDFS_800, expectedMessage));

        given()
                .pathParam("organization-fiscal-code", ORGANIZATION_FISCAL_CODE)
                .pathParam("iuv", IUV)
                .when()
                .get("/helpdesk/receipts/cart/organizations/{organization-fiscal-code}/iuvs/{iuv}")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("title", equalTo("NOT_FOUND"))
                .body("detail", equalTo(expectedMessage));
    }

    @Test
    void getCartReceiptByOrgAndIUV_NotFound_ReceiptNotFound() throws BizEventNotFoundException, CartNotFoundException {
        BizEvent mockBizEvent = new BizEvent();
        mockBizEvent.setId(EVENT_ID);

        String expectedMessage = String.format("Unable to retrieve the receipt with eventId %s", EVENT_ID);

        when(bizEventCosmosClient.getBizEventDocumentByOrganizationFiscalCodeAndIUV(ORGANIZATION_FISCAL_CODE, IUV))
                .thenReturn(mockBizEvent);
        when(cartReceiptCosmosService.getCartReceiptFromEventId(EVENT_ID))
                .thenThrow(new CartNotFoundException(PDFS_800, expectedMessage));

        given()
                .pathParam("organization-fiscal-code", ORGANIZATION_FISCAL_CODE)
                .pathParam("iuv", IUV)
                .when()
                .get("/helpdesk/receipts/cart/organizations/{organization-fiscal-code}/iuvs/{iuv}")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("title", equalTo("NOT_FOUND"))
                .body("detail", equalTo(expectedMessage));
    }

    @Test
    void getCartReceiptByOrgAndIUV_Success() throws BizEventNotFoundException, CartNotFoundException {
        BizEvent mockBizEvent = new BizEvent();
        mockBizEvent.setId(EVENT_ID);

        CartForReceipt mockReceipt = new CartForReceipt();
        mockReceipt.setEventId(EVENT_ID);

        when(bizEventCosmosClient.getBizEventDocumentByOrganizationFiscalCodeAndIUV(ORGANIZATION_FISCAL_CODE, IUV))
                .thenReturn(mockBizEvent);
        when(cartReceiptCosmosService.getCartReceiptFromEventId(EVENT_ID))
                .thenReturn(mockReceipt);

        given()
                .pathParam("organization-fiscal-code", ORGANIZATION_FISCAL_CODE)
                .pathParam("iuv", IUV)
                .when()
                .get("/helpdesk/receipts/cart/organizations/{organization-fiscal-code}/iuvs/{iuv}")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("eventId", equalTo(EVENT_ID));
    }
}