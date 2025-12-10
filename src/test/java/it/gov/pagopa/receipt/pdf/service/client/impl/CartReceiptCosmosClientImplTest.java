package it.gov.pagopa.receipt.pdf.service.client.impl;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.util.CosmosPagedIterable;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import it.gov.pagopa.receipt.pdf.service.client.CartReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.CartNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.service.model.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.service.producer.CartContainer;
import it.gov.pagopa.receipt.pdf.service.producer.ReceiptsContainer;
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

    @Inject
    private CartReceiptCosmosClient sut;

    private static Iterator<Receipt> iteratorMock;

    @BeforeAll
    static void setUp() {
        CosmosPagedIterable<Receipt> cosmosPagedIterableMock = mock(CosmosPagedIterable.class);
        iteratorMock = mock(Iterator.class);

        CosmosContainer cosmosContainerMock = mock(CosmosContainer.class);
        doReturn(cosmosPagedIterableMock).when(cosmosContainerMock).queryItems(anyString(), any(), any());
        Annotation qualifier = new AnnotationLiteral<CartContainer>() {};
        QuarkusMock.installMockForType(cosmosContainerMock, CosmosContainer.class, qualifier);


        doReturn(iteratorMock).when(cosmosPagedIterableMock).iterator();
    }


    @SneakyThrows
    @Test
    void getCartForReceiptDocumentNotFound() {
        doReturn(false).when(iteratorMock).hasNext();

        CartNotFoundException e = assertThrows(CartNotFoundException.class, () -> sut.getCartForReceiptDocument("id"));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_801, e.getErrorCode());

    }

    @SneakyThrows
    @Test
    void getCartForReceiptDocumentSuccess() {
        CartForReceipt cart = new CartForReceipt();

        doReturn(true).when(iteratorMock).hasNext();
        doReturn(cart).when(iteratorMock).next();

        CartForReceipt result = sut.getCartForReceiptDocument("id");

        assertEquals(cart, result);

    }
}