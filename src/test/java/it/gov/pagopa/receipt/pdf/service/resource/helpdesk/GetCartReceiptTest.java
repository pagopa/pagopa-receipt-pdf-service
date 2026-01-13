package it.gov.pagopa.receipt.pdf.service.resource.helpdesk;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.CartNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartStatusType;
import it.gov.pagopa.receipt.pdf.service.model.receipt.ReasonError;
import it.gov.pagopa.receipt.pdf.service.service.impl.CartReceiptCosmosService;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

@QuarkusTest
class GetCartReceiptTest {

    private static final String CART_ID = "cartId";

    @InjectMock
    private CartReceiptCosmosService cartReceiptCosmosService;

    // --- TEST getCartReceipt ---

    @Test
    void getCartReceiptSuccess() throws CartNotFoundException {
        CartForReceipt receipt = buildReceipt();
        when(cartReceiptCosmosService.getCartReceipt(CART_ID)).thenReturn(receipt);

        given()
                .pathParam("cart-id", CART_ID)
                .when()
                .get("/helpdesk/receipts/cart/{cart-id}")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("id", equalTo(CART_ID));
    }

    @Test
    void getCartReceiptForMissingCartId() {
        given()
                .pathParam("cart-id", " ")
                .when()
                .get("/helpdesk/receipts/cart/{cart-id}")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("title", equalTo("BAD_REQUEST"))
                .body("detail", equalTo("Please pass a valid cartId"));
    }

    @Test
    void getCartReceiptNotFound() throws CartNotFoundException {
        String errorMessage = String.format("Unable to retrieve the receipt with cartId %s", CART_ID);
        when(cartReceiptCosmosService.getCartReceipt(CART_ID)).thenThrow(new CartNotFoundException(AppErrorCodeEnum.PDFS_800, errorMessage));

        given()
                .pathParam("cart-id", CART_ID)
                .when()
                .get("/helpdesk/receipts/cart/{cart-id}")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("title", equalTo("NOT_FOUND"))
                .body("detail", equalTo(errorMessage));
    }

    // --- HELPER ---

    private CartForReceipt buildReceipt() {
        return CartForReceipt.builder()
                .id(CART_ID)
                .status(CartStatusType.IO_ERROR_TO_NOTIFY)
                .reasonErr(ReasonError.builder()
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