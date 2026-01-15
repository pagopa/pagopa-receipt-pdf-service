package it.gov.pagopa.receipt.pdf.service.resource.helpdesk;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import it.gov.pagopa.receipt.pdf.service.exception.IoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.IOMessage;
import it.gov.pagopa.receipt.pdf.service.service.impl.CartReceiptCosmosService;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

@QuarkusTest
class GetCartReceiptIOMessageTest {

    private static final String MESSAGE_ID = "message-12345";

    @InjectMock
    private CartReceiptCosmosService cartReceiptCosmosService;

    @Test
    void getCartReceiptIOMessage_BadRequest_MessageIdIsBlank() {
        given()
                .pathParam("message-id", " ")
                .when()
                .get("/helpdesk/cart-receipts/io-message/{message-id}")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("title", equalTo("BAD_REQUEST"))
                .body("detail", equalTo("Please pass a valid messageId"));
    }

    @Test
    void getCartReceiptIOMessage_Success() throws IoMessageNotFoundException {
        IOMessage mockMessage = new IOMessage();
        mockMessage.setMessageId(MESSAGE_ID);

        when(cartReceiptCosmosService.getCartReceiptMessage(MESSAGE_ID)).thenReturn(mockMessage);

        given()
                .pathParam("message-id", MESSAGE_ID)
                .when()
                .get("/helpdesk/cart-receipts/io-message/{message-id}")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("messageId", equalTo(MESSAGE_ID));
    }

    @Test
    void getCartReceiptIOMessage_NotFound_ExceptionThrown() throws IoMessageNotFoundException {
        String expectedMessage = String.format("Unable to retrieve the receipt message with messageId %s", MESSAGE_ID);

        when(cartReceiptCosmosService.getCartReceiptMessage(MESSAGE_ID))
                .thenThrow(new IoMessageNotFoundException(expectedMessage));

        given()
                .pathParam("message-id", MESSAGE_ID)
                .when()
                .get("/helpdesk/cart-receipts/io-message/{message-id}")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("title", equalTo("NOT_FOUND"))
                .body("detail", equalTo(expectedMessage));
    }
}