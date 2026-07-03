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

import java.lang.annotation.Annotation;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class CartReceiptCosmosClientImplTest {

    private static final String DOCUMENT_NOT_FOUND_ERR_MSG = "Document not found in the defined container";
    @Inject
    private CartReceiptCosmosClient sut;

    private static CosmosItemResponse<CartForReceipt> cartReceiptItemResponseMock;
    private static Iterator<CartIOMessage> iteratorIOMessageMock;
    private static CosmosItemResponse<CartReceiptError> cartReceiptErrorItemResponseMock;


    @BeforeAll
    static void setUp() {
        CosmosContainer containerCartReceiptMock = mock(CosmosContainer.class);
        QuarkusMock.installMockForType(
                containerCartReceiptMock,
                CosmosContainer.class,
                new AnnotationLiteral<CartContainer>() {
                }
        );

        CosmosContainer containerIOMessagesMock = mock(CosmosContainer.class);
        QuarkusMock.installMockForType(
                containerIOMessagesMock,
                CosmosContainer.class,
                new AnnotationLiteral<CartReceiptsIOMessagesContainer>() {
                }
        );

        CosmosContainer containerCartReceiptErrorMock = mock(CosmosContainer.class);
        QuarkusMock.installMockForType(
                containerCartReceiptErrorMock,
                CosmosContainer.class,
                new AnnotationLiteral<CartReceiptsErrorContainer>() {
                }
        );

        // IO Messages
        CosmosPagedIterable<CartForReceipt> cosmosPagedIOMessageIterableMock = mock(CosmosPagedIterable.class);
        iteratorIOMessageMock = mock(Iterator.class);
        CosmosContainer cosmosContainerIOMessagesMock = mock(CosmosContainer.class);
        doReturn(cosmosPagedIOMessageIterableMock).when(cosmosContainerIOMessagesMock).queryItems(anyString(), any(), any());
        Annotation ioQualifier = new AnnotationLiteral<CartReceiptsIOMessagesContainer>() {
        };
        QuarkusMock.installMockForType(cosmosContainerIOMessagesMock, CosmosContainer.class, ioQualifier);
        doReturn(iteratorIOMessageMock).when(cosmosPagedIOMessageIterableMock).iterator();

        cartReceiptItemResponseMock = mock(CosmosItemResponse.class);
        CosmosPagedIterable<IOMessage> mockIOMessageIterable = mock(CosmosPagedIterable.class);
        cartReceiptErrorItemResponseMock = mock(CosmosItemResponse.class);

        doReturn(cartReceiptItemResponseMock).when(containerCartReceiptMock)
                .readItem(anyString(), any(), eq(CartForReceipt.class));
        doReturn(mockIOMessageIterable)
                .when(containerIOMessagesMock).queryItems(any(SqlQuerySpec.class), any(), eq(CartIOMessage.class));
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

        doReturn(true).when(iteratorIOMessageMock).hasNext();
        doReturn(ioMessage).when(iteratorIOMessageMock).next();

        CartIOMessage result = sut.getCartIoMessage("messageId");

        assertEquals(ioMessage, result);
    }

    @SneakyThrows
    @Test
    void getCartIoMessageNotFound() {
        doReturn(false).when(iteratorIOMessageMock).hasNext();

        IoMessageNotFoundException e = assertThrows(IoMessageNotFoundException.class, () -> sut.getCartIoMessage("messageId"));

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