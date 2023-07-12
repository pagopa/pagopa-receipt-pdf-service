package it.gov.pagopa.receipt.pdf.service.client.impl;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.util.CosmosPagedIterable;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.ReceiptNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.Receipt;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@QuarkusTest
class ReceiptCosmosClientImplTest {

    @Inject
    private ReceiptCosmosClient sut;

    @Inject
    private CosmosClient cosmosClientMock;

    private static Iterator<Receipt> iteratorMock;


    @BeforeEach
    void setUp() {
        CosmosDatabase cosmosDatabaseMock = mock(CosmosDatabase.class);

        CosmosClient cosmosClientMock = mock(CosmosClient.class);
        doReturn(cosmosDatabaseMock).when(cosmosClientMock).getDatabase(anyString());
        QuarkusMock.installMockForType(cosmosClientMock, CosmosClient.class);

        CosmosContainer cosmosContainerMock = mock(CosmosContainer.class);
        CosmosPagedIterable<Receipt> cosmosPagedIterableMock = mock(CosmosPagedIterable.class);
        iteratorMock = mock(Iterator.class);

        doReturn(cosmosContainerMock).when(cosmosDatabaseMock).getContainer(anyString());
        doReturn(cosmosPagedIterableMock).when(cosmosContainerMock).queryItems(anyString(), any(), any());
        doReturn(iteratorMock).when(cosmosPagedIterableMock).iterator();
    }

    @SneakyThrows
    @Test
    void getReceiptDocumentSuccess() {
        Receipt receipt = new Receipt();

        doReturn(true).when(iteratorMock).hasNext();
        doReturn(receipt).when(iteratorMock).next();

        Receipt result = sut.getReceiptDocument("id");

        assertEquals(receipt, result);
    }

    @SneakyThrows
    @Test
    void getReceiptDocumentFailureReceiptNotFound() {
        doReturn(false).when(iteratorMock).hasNext();

        ReceiptNotFoundException e = assertThrows(ReceiptNotFoundException.class, () -> sut.getReceiptDocument("id"));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_800, e.getErrorCode());

    }
}