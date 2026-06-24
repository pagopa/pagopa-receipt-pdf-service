package it.gov.pagopa.receipt.pdf.service.client.impl;

import com.azure.cosmos.CosmosContainer;
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
import it.gov.pagopa.receipt.pdf.service.model.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.service.model.receipt.ReceiptError;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

@QuarkusTest
class CartReceiptCosmosClientImplTest {

    private static final String DOCUMENT_NOT_FOUND_ERR_MSG = "Document not found in the defined container";
    @Inject
    private CartReceiptCosmosClient sut;

    private static Stream<CartForReceipt> cartReceiptStreamMock;
    private static Stream<CartIOMessage> ioMessageStreamMock;
    private static Stream<CartReceiptError> cartReceiptErrorStreamMock;


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

        CosmosPagedIterable<Receipt> mockCartReceiptsIterable = Mockito.mock(CosmosPagedIterable.class);
        cartReceiptStreamMock = Mockito.mock(Stream.class);
        CosmosPagedIterable<IOMessage> mockIOMessageIterable = Mockito.mock(CosmosPagedIterable.class);
        ioMessageStreamMock = Mockito.mock(Stream.class);
        CosmosPagedIterable<ReceiptError> mockCartReceiptErrorIterable = Mockito.mock(CosmosPagedIterable.class);
        cartReceiptErrorStreamMock = Mockito.mock(Stream.class);

        doReturn(mockCartReceiptsIterable).when(containerCartReceiptMock)
                .queryItems(any(SqlQuerySpec.class), any(), eq(CartForReceipt.class));
        doReturn(cartReceiptStreamMock).when(mockCartReceiptsIterable).stream();
        doReturn(mockIOMessageIterable)
                .when(containerIOMessagesMock).queryItems(any(SqlQuerySpec.class), any(), eq(CartIOMessage.class));
        doReturn(ioMessageStreamMock).when(mockIOMessageIterable).stream();
        doReturn(mockCartReceiptErrorIterable)
                .when(containerCartReceiptErrorMock).queryItems(any(SqlQuerySpec.class), any(), eq(CartReceiptError.class));
        doReturn(cartReceiptErrorStreamMock).when(mockCartReceiptErrorIterable).stream();
    }

    @SneakyThrows
    @Test
    void getCartForReceiptDocumentSuccess() {
        CartForReceipt cart = new CartForReceipt();

        doReturn(Optional.of(cart)).when(cartReceiptStreamMock).findFirst();

        CartForReceipt result = assertDoesNotThrow(() -> sut.getCartForReceiptDocument("id"));

        assertEquals(cart, result);
    }

    @SneakyThrows
    @Test
    void getCartForReceiptDocumentNotFound() {
        doReturn(Optional.empty()).when(cartReceiptStreamMock).findFirst();

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

        doReturn(Optional.of(cartReceiptError)).when(cartReceiptErrorStreamMock).findFirst();

        CartReceiptError result = assertDoesNotThrow(() -> sut.getCartReceiptError("cartId"));

        assertEquals(cartReceiptError, result);
    }

    @SneakyThrows
    @Test
    void getCartReceiptErrorFound() {
        doReturn(Optional.empty()).when(cartReceiptErrorStreamMock).findFirst();

        CartNotFoundException e =
                assertThrows(CartNotFoundException.class, () -> sut.getCartReceiptError("cartId"));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_801, e.getErrorCode());
    }
}