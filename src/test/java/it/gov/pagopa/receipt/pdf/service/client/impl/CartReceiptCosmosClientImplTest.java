package it.gov.pagopa.receipt.pdf.service.client.impl;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.implementation.NotFoundException;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedIterable;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import it.gov.pagopa.receipt.pdf.service.client.CartReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.CartNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.IoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.IOMessage;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartIOMessage;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartReceiptError;
import it.gov.pagopa.receipt.pdf.service.producer.receipt.containers.CartContainer;
import it.gov.pagopa.receipt.pdf.service.producer.receipt.containers.CartReceiptsErrorContainer;
import it.gov.pagopa.receipt.pdf.service.producer.receipt.containers.CartReceiptsIOMessagesContainer;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@QuarkusTest
class CartReceiptCosmosClientImplTest {

    private static final String DOCUMENT_NOT_FOUND_ERR_MSG = "Document not found in the defined container";
    @Inject
    private CartReceiptCosmosClient sut;

    private static CosmosItemResponse<CartForReceipt> cartReceiptItemResponseMock;
    private static Stream<CartIOMessage> ioMessageStreamMock;
    private static CosmosItemResponse<CartReceiptError> cartReceiptErrorItemResponseMock;


    @BeforeAll
    static void setUp() {
        CosmosContainer containerCartReceiptMock = Mockito.mock(CosmosContainer.class);
        QuarkusMock.installMockForType(
                containerCartReceiptMock,
                CosmosContainer.class,
                new AnnotationLiteral<CartContainer>() {
                }
        );

        CosmosContainer containerIOMessagesMock = Mockito.mock(CosmosContainer.class);
        QuarkusMock.installMockForType(
                containerIOMessagesMock,
                CosmosContainer.class,
                new AnnotationLiteral<CartReceiptsIOMessagesContainer>() {
                }
        );

        CosmosContainer containerCartReceiptErrorMock = Mockito.mock(CosmosContainer.class);
        QuarkusMock.installMockForType(
                containerCartReceiptErrorMock,
                CosmosContainer.class,
                new AnnotationLiteral<CartReceiptsErrorContainer>() {
                }
        );

        cartReceiptItemResponseMock = Mockito.mock(CosmosItemResponse.class);
        CosmosPagedIterable<IOMessage> mockIOMessageIterable = Mockito.mock(CosmosPagedIterable.class);
        ioMessageStreamMock = Mockito.mock(Stream.class);
        cartReceiptErrorItemResponseMock = Mockito.mock(CosmosItemResponse.class);

        doReturn(cartReceiptItemResponseMock).when(containerCartReceiptMock)
                .readItem(anyString(), any(), eq(CartForReceipt.class));
        doReturn(mockIOMessageIterable)
                .when(containerIOMessagesMock).queryItems(any(SqlQuerySpec.class), any(), eq(CartIOMessage.class));
        doReturn(ioMessageStreamMock).when(mockIOMessageIterable).stream();
        doReturn(cartReceiptErrorItemResponseMock)
                .when(containerCartReceiptErrorMock).readItem(anyString(), any(), eq(CartReceiptError.class));
    }

    @SneakyThrows
    @Test
    void getCartForReceiptDocumentSuccess() {
        CartForReceipt cart = new CartForReceipt();

        doReturn(cart).when(cartReceiptItemResponseMock).getItem();

        CartForReceipt result = assertDoesNotThrow(() -> sut.getCartForReceiptDocument("id"));

        assertEquals(cart, result);
    }

    @SneakyThrows
    @Test
    void getCartForReceiptDocumentNotFound() {
        doThrow(NotFoundException.class).when(cartReceiptItemResponseMock).getItem();

        CartNotFoundException e = assertThrows(CartNotFoundException.class, () -> sut.getCartForReceiptDocument("id"));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_801, e.getErrorCode());
    }

    @SneakyThrows
    @Test
    void getCartIoMessageSuccess() {
        CartIOMessage ioMessage = new CartIOMessage();

        doReturn(Optional.of(ioMessage)).when(ioMessageStreamMock).findFirst();

        CartIOMessage result = assertDoesNotThrow(() -> sut.getCartIoMessage("messageId"));

        assertEquals(ioMessage, result);
    }

    @SneakyThrows
    @Test
    void getCartIoMessageNotFound() {
        doReturn(Optional.empty()).when(ioMessageStreamMock).findFirst();

        IoMessageNotFoundException e =
                assertThrows(IoMessageNotFoundException.class, () -> sut.getCartIoMessage("messageId"));

        assertNotNull(e);
        assertEquals(DOCUMENT_NOT_FOUND_ERR_MSG, e.getMessage());
    }

    @SneakyThrows
    @Test
    void getCartReceiptErrorSuccess() {
        CartReceiptError cartReceiptError = new CartReceiptError();

        doReturn(cartReceiptError).when(cartReceiptErrorItemResponseMock).getItem();

        CartReceiptError result = assertDoesNotThrow(() -> sut.getCartReceiptError("cartId"));

        assertEquals(cartReceiptError, result);
    }

    @SneakyThrows
    @Test
    void getCartReceiptErrorFound() {
        doThrow(NotFoundException.class).when(cartReceiptErrorItemResponseMock).getItem();

        CartNotFoundException e =
                assertThrows(CartNotFoundException.class, () -> sut.getCartReceiptError("cartId"));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_801, e.getErrorCode());
    }
}