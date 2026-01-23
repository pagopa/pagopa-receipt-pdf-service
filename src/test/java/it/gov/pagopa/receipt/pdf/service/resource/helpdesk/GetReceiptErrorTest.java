package it.gov.pagopa.receipt.pdf.service.resource.helpdesk;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import it.gov.pagopa.receipt.pdf.service.exception.ReceiptNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.receipt.ReceiptError;
import it.gov.pagopa.receipt.pdf.service.service.impl.ReceiptCosmosService;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.PDFS_800;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
class GetReceiptErrorTest {

    private static final String BIZ_EVENT_ID = "1";

    @InjectMock
    private ReceiptCosmosService receiptCosmosService;

    @Test
    void getReceiptErrorByEventId_BadRequest_MissingParam() {
        given()
                .pathParam("bizevent-id", " ")
                .when()
                .get("/errors-toreview/{bizevent-id}")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("detail", equalTo("[HELPDESK] Missing valid search parameter"));
    }

    @Test
    void getReceiptErrorByEventId_Success() throws ReceiptNotFoundException {
        ReceiptError mockError = ReceiptError.builder()
                .bizEventId(BIZ_EVENT_ID)
                .messageError("test error")
                .messagePayload("{\"data\":\"test\"}")
                .build();

        when(receiptCosmosService.getReceiptError(BIZ_EVENT_ID)).thenReturn(mockError);

        given()
                .pathParam("bizevent-id", BIZ_EVENT_ID)
                .when()
                .get("/errors-toreview/{bizevent-id}")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("bizEventId", equalTo(BIZ_EVENT_ID))
                .body("messagePayload", equalTo("{\"data\":\"test\"}"));
    }

    @Test
    void getReceiptErrorByEventId_NotFound() throws ReceiptNotFoundException {
        String expectedDetail = "No Receipt Error to process on bizEvent with id " + BIZ_EVENT_ID;

        when(receiptCosmosService.getReceiptError(BIZ_EVENT_ID))
                .thenThrow(new ReceiptNotFoundException(PDFS_800, PDFS_800.getErrorMessage()));

        given()
                .pathParam("bizevent-id", BIZ_EVENT_ID)
                .when()
                .get("/errors-toreview/{bizevent-id}")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("detail", equalTo("[HELPDESK] " + expectedDetail));
    }

    @Test
    void getReceiptErrorByEventId_InternalServerError() throws ReceiptNotFoundException {
        String errorMessage = "Unexpected Error";

        when(receiptCosmosService.getReceiptError(anyString()))
                .thenAnswer(invocation -> { throw new RuntimeException(errorMessage); });

        given()
                .pathParam("bizevent-id", BIZ_EVENT_ID)
                .when()
                .get("/errors-toreview/{bizevent-id}")
                .then()
                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .contentType(ContentType.JSON)
                .body("status", equalTo(500))
                .body("detail", equalTo("[HELPDESK] " + errorMessage));
    }
}