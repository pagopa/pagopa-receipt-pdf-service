package it.gov.pagopa.receipt.pdf.service.resource.helpdesk;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.enumeration.ReceiptStatusType;
import it.gov.pagopa.receipt.pdf.service.exception.ReceiptNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.receipt.ReasonError;
import it.gov.pagopa.receipt.pdf.service.model.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.service.service.impl.ReceiptCosmosService;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

@QuarkusTest
public class GetReceiptTest {

    private static final String EVENT_ID = "eventId";

    @InjectMock
    private ReceiptCosmosService receiptCosmosService;

    // --- TEST getReceipt ---

    @Test
    void getReceiptReceiptSuccess() throws ReceiptNotFoundException {
        Receipt receipt = buildReceipt();
        when(receiptCosmosService.getReceipt(EVENT_ID)).thenReturn(receipt);

        given()
                .pathParam("event-id", EVENT_ID)
                .when()
                .get("/helpdesk/receipts/{event-id}")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("eventId", equalTo(EVENT_ID));
    }

    @Test
    void getReceiptForMissingEventId() {
        given()
                .pathParam("event-id", " ")
                .when()
                .get("/helpdesk/receipts/{event-id}")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("title", equalTo("BAD_REQUEST"))
                .body("detail", equalTo("Please pass a valid biz-event id"));
    }

    @Test
    void getReceiptNotFound() throws ReceiptNotFoundException {
        String errorMessage = String.format("Unable to retrieve the receipt with eventId %s", EVENT_ID);
        when(receiptCosmosService.getReceipt(EVENT_ID)).thenThrow(new ReceiptNotFoundException(AppErrorCodeEnum.PDFS_800, errorMessage));

        given()
                .pathParam("event-id", EVENT_ID)
                .when()
                .get("/helpdesk/receipts/{event-id}")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("title", equalTo("NOT_FOUND"))
                .body("detail", equalTo(errorMessage));
    }

    // --- HELPER ---

    private Receipt buildReceipt() {
        return Receipt.builder()
                .eventId(EVENT_ID)
                .status(ReceiptStatusType.IO_ERROR_TO_NOTIFY)
                .reasonErr(ReasonError.builder()
                        .code(500)
                        .message("error message")
                        .build())
                .reasonErrPayer(ReasonError.builder()
                        .code(500)
                        .message("error message")
                        .build())
                .numRetry(0)
                .notificationNumRetry(6)
                .inserted_at(0)
                .generated_at(0)
                .notified_at(0)
                .build();
    }
}