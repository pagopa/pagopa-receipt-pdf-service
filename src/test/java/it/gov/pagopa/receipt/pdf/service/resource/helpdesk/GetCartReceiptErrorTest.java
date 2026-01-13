package it.gov.pagopa.receipt.pdf.service.resource.helpdesk;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import it.gov.pagopa.receipt.pdf.service.exception.CartNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartReceiptError;
import it.gov.pagopa.receipt.pdf.service.service.impl.CartReceiptCosmosService;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.PDFS_800;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
class GetCartReceiptErrorTest {

    private static final String CART_ID = "1";

    @InjectMock
    private CartReceiptCosmosService cartReceiptCosmosService;

    @Test
    void getCartReceiptErrorByEventId_BadRequest_MissingParam() {
        given()
                .pathParam("bizevent-id", " ")
                .when()
                .get("/helpdesk/errors-toreview/cart/{bizevent-id}")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("detail", equalTo("Missing valid search parameter"));
    }

    @Test
    void getCartReceiptErrorByEventId_Success() throws CartNotFoundException {
        CartReceiptError mockError = CartReceiptError.builder()
                .id(CART_ID)
                .messageError("test error")
                .messagePayload("{\"data\":\"test\"}")
                .build();

        when(cartReceiptCosmosService.getCartReceiptError(CART_ID)).thenReturn(mockError);

        given()
                .pathParam("bizevent-id", CART_ID)
                .when()
                .get("/helpdesk/errors-toreview/cart/{bizevent-id}")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("id", equalTo(CART_ID))
                .body("messagePayload", equalTo("{\"data\":\"test\"}"));
    }

    @Test
    void getCartReceiptErrorByEventId_NotFound() throws CartNotFoundException {
        String expectedDetail = "No Receipt Error to process on cartId with id " + CART_ID;

        when(cartReceiptCosmosService.getCartReceiptError(CART_ID))
                .thenThrow(new CartNotFoundException(PDFS_800, PDFS_800.getErrorMessage()));

        given()
                .pathParam("bizevent-id", CART_ID)
                .when()
                .get("/helpdesk/errors-toreview/cart/{bizevent-id}")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("detail", equalTo(expectedDetail));
    }

    @Test
    void getCartReceiptErrorByEventId_InternalServerError() throws CartNotFoundException {
        String errorMessage = "Unexpected Error";

        when(cartReceiptCosmosService.getCartReceiptError(anyString()))
                .thenAnswer(invocation -> {
                    throw new RuntimeException(errorMessage);
                });

        given()
                .pathParam("bizevent-id", CART_ID)
                .when()
                .get("/helpdesk/errors-toreview/cart/{bizevent-id}")
                .then()
                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .contentType(ContentType.JSON)
                .body("status", equalTo(500))
                .body("detail", equalTo(errorMessage));
    }
}