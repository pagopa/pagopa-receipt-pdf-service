package it.gov.pagopa.receipt.pdf.service.client.impl;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.util.CosmosPagedIterable;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import it.gov.pagopa.receipt.pdf.service.client.CartReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.CartNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.IoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.IOMessage;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartReceiptError;
import it.gov.pagopa.receipt.pdf.service.producer.receipt.containers.CartContainer;
import it.gov.pagopa.receipt.pdf.service.producer.receipt.containers.CartReceiptsErrorContainer;
import it.gov.pagopa.receipt.pdf.service.producer.receipt.containers.CartReceiptsIOMessagesContainer;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@QuarkusTest
class CartReceiptCosmosClientImplTest {

    private static final String DOCUMENT_NOT_FOUND_ERR_MSG = "Document not found in the defined container";
    @Inject
    private CartReceiptCosmosClient sut;

    private static Iterator<CartForReceipt> iteratorCartReceiptMock;
    private static Iterator<IOMessage> iteratorIOMessageMock;
    private static Iterator<CartReceiptError> iteratorCartReceiptErrorMock;


    @BeforeAll
    static void setUp() {
        // Cart receipt
        CosmosPagedIterable<CartForReceipt> cosmosPagedCartReceiptIterableMock = mock(CosmosPagedIterable.class);
        iteratorCartReceiptMock = mock(Iterator.class);
        CosmosContainer cosmosContainerCartReceiptMock = mock(CosmosContainer.class);
        doReturn(cosmosPagedCartReceiptIterableMock).when(cosmosContainerCartReceiptMock).queryItems(anyString(), any(), any());
        Annotation cartQualifier = new AnnotationLiteral<CartContainer>() {
        };
        QuarkusMock.installMockForType(cosmosContainerCartReceiptMock, CosmosContainer.class, cartQualifier);
        doReturn(iteratorCartReceiptMock).when(cosmosPagedCartReceiptIterableMock).iterator();

        // IO Messages
        CosmosPagedIterable<CartForReceipt> cosmosPagedIOMessageIterableMock = mock(CosmosPagedIterable.class);
        iteratorIOMessageMock = mock(Iterator.class);
        CosmosContainer cosmosContainerIOMessagesMock = mock(CosmosContainer.class);
        doReturn(cosmosPagedIOMessageIterableMock).when(cosmosContainerIOMessagesMock).queryItems(anyString(), any(), any());
        Annotation IOQualifier = new AnnotationLiteral<CartReceiptsIOMessagesContainer>() {
        };
        QuarkusMock.installMockForType(cosmosContainerIOMessagesMock, CosmosContainer.class, IOQualifier);
        doReturn(iteratorIOMessageMock).when(cosmosPagedIOMessageIterableMock).iterator();

        // Cart Receipt Error
        CosmosPagedIterable<CartForReceipt> cosmosPagedCartReceiptErrorIterableMock = mock(CosmosPagedIterable.class);
        iteratorCartReceiptErrorMock = mock(Iterator.class);
        CosmosContainer cosmosContainerCartReceiptErrorMock = mock(CosmosContainer.class);
        doReturn(cosmosPagedCartReceiptErrorIterableMock).when(cosmosContainerCartReceiptErrorMock).queryItems(anyString(), any(), any());
        Annotation cartReceiptErrorQualifier = new AnnotationLiteral<CartReceiptsErrorContainer>() {
        };
        QuarkusMock.installMockForType(cosmosContainerCartReceiptErrorMock, CosmosContainer.class, cartReceiptErrorQualifier);
        doReturn(iteratorCartReceiptErrorMock).when(cosmosPagedCartReceiptErrorIterableMock).iterator();
    }

    @SneakyThrows
    @Test
    void getCartForReceiptDocumentSuccess() {
        CartForReceipt cart = new CartForReceipt();

        doReturn(true).when(iteratorCartReceiptMock).hasNext();
        doReturn(cart).when(iteratorCartReceiptMock).next();

        CartForReceipt result = sut.getCartForReceiptDocument("id");

        assertEquals(cart, result);
    }

    @SneakyThrows
    @Test
    void getCartForReceiptDocumentNotFound() {
        doReturn(false).when(iteratorCartReceiptMock).hasNext();

        CartNotFoundException e = assertThrows(CartNotFoundException.class, () -> sut.getCartForReceiptDocument("id"));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_801, e.getErrorCode());
    }

    @SneakyThrows
    @Test
    void getCartIoMessageSuccess() {
        IOMessage ioMessage = new IOMessage();

        doReturn(true).when(iteratorIOMessageMock).hasNext();
        doReturn(ioMessage).when(iteratorIOMessageMock).next();

        IOMessage result = sut.getCartIoMessage("messageId");

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

        doReturn(true).when(iteratorCartReceiptErrorMock).hasNext();
        doReturn(cartReceiptError).when(iteratorCartReceiptErrorMock).next();

        CartReceiptError result = sut.getCartReceiptError("cartId");

        assertEquals(cartReceiptError, result);
    }

    @SneakyThrows
    @Test
    void getCartReceiptErrorFound() {
        doReturn(false).when(iteratorCartReceiptErrorMock).hasNext();

        CartNotFoundException e = assertThrows(CartNotFoundException.class, () -> sut.getCartReceiptError("cartId"));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_801, e.getErrorCode());
    }
}