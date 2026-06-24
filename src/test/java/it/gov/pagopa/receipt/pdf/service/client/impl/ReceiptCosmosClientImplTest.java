package it.gov.pagopa.receipt.pdf.service.client.impl;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedIterable;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.IoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.ReceiptNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.IOMessage;
import it.gov.pagopa.receipt.pdf.service.model.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.service.model.receipt.ReceiptError;
import it.gov.pagopa.receipt.pdf.service.producer.receipt.containers.ReceiptsContainer;
import it.gov.pagopa.receipt.pdf.service.producer.receipt.containers.ReceiptsErrorContainer;
import it.gov.pagopa.receipt.pdf.service.producer.receipt.containers.ReceiptsIOMessagesEventContainer;
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
class ReceiptCosmosClientImplTest {

    private static Stream<Receipt> mockReceiptStream;
    private static Stream<IOMessage> mockIOMessageStream;
    private static Stream<ReceiptError> mockReceiptErrorStream;

    @Inject
    private ReceiptCosmosClient sut;

    @BeforeAll
    static void setUp() {
        CosmosContainer containerReceiptsMock = Mockito.mock(CosmosContainer.class);
        QuarkusMock.installMockForType(
                containerReceiptsMock,
                CosmosContainer.class,
                new AnnotationLiteral<ReceiptsContainer>() {
                }
        );

        CosmosContainer containerReceiptsIOMessagesMock = Mockito.mock(CosmosContainer.class);
        QuarkusMock.installMockForType(
                containerReceiptsIOMessagesMock,
                CosmosContainer.class,
                new AnnotationLiteral<ReceiptsIOMessagesEventContainer>() {
                }
        );

        CosmosContainer containerReceiptsErrorMock = Mockito.mock(CosmosContainer.class);
        QuarkusMock.installMockForType(
                containerReceiptsErrorMock,
                CosmosContainer.class,
                new AnnotationLiteral<ReceiptsErrorContainer>() {
                }
        );

        CosmosPagedIterable<Receipt> mockReceiptIterable = Mockito.mock(CosmosPagedIterable.class);
        mockReceiptStream = Mockito.mock(Stream.class);
        CosmosPagedIterable<IOMessage> mockIOMessageIterable = Mockito.mock(CosmosPagedIterable.class);
        mockIOMessageStream = Mockito.mock(Stream.class);
        CosmosPagedIterable<ReceiptError> mockReceiptErrorIterable = Mockito.mock(CosmosPagedIterable.class);
        mockReceiptErrorStream = Mockito.mock(Stream.class);

        doReturn(mockReceiptIterable).when(containerReceiptsMock)
                .queryItems(any(SqlQuerySpec.class), any(), eq(Receipt.class));
        doReturn(mockReceiptStream).when(mockReceiptIterable).stream();
        doReturn(mockIOMessageIterable)
                .when(containerReceiptsIOMessagesMock).queryItems(any(SqlQuerySpec.class), any(), eq(IOMessage.class));
        doReturn(mockIOMessageStream).when(mockIOMessageIterable).stream();
        doReturn(mockReceiptErrorIterable)
                .when(containerReceiptsErrorMock).queryItems(any(SqlQuerySpec.class), any(), eq(ReceiptError.class));
        doReturn(mockReceiptErrorStream).when(mockReceiptErrorIterable).stream();
    }

    @SneakyThrows
    @Test
    void getReceiptDocumentSuccess() {
        Receipt receipt = new Receipt();

        doReturn(Optional.of(receipt)).when(mockReceiptStream).findFirst();

        Receipt result = assertDoesNotThrow(() -> sut.getReceiptDocument("id"));

        assertEquals(receipt, result);
    }

    @SneakyThrows
    @Test
    void getReceiptDocumentFailureReceiptNotFound() {
        doReturn(Optional.empty()).when(mockReceiptStream).findFirst();

        ReceiptNotFoundException e = assertThrows(ReceiptNotFoundException.class, () -> sut.getReceiptDocument("id"));

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_800, e.getErrorCode());
    }

    @SneakyThrows
    @Test
    void getIoMessageSuccess() {
        IOMessage ioMessage = new IOMessage();

        doReturn(Optional.of(ioMessage)).when(mockIOMessageStream).findFirst();

        IOMessage result = assertDoesNotThrow(() -> sut.getIoMessage("id"));

        assertEquals(ioMessage, result);
    }

    @SneakyThrows
    @Test
    void getIoMessageFailureNotFound() {
        doReturn(Optional.empty()).when(mockIOMessageStream).findFirst();

        IoMessageNotFoundException e = assertThrows(IoMessageNotFoundException.class, () -> sut.getIoMessage("id"));

        assertNotNull(e);
    }

    @SneakyThrows
    @Test
    void getReceiptErrorSuccess() {
        ReceiptError receiptError = new ReceiptError();

        doReturn(Optional.of(receiptError)).when(mockReceiptErrorStream).findFirst();

        ReceiptError result = assertDoesNotThrow(() -> sut.getReceiptError("id"));

        assertEquals(receiptError, result);
    }

    @SneakyThrows
    @Test
    void getReceiptErrorFailureNotFound() {
        doReturn(Optional.empty()).when(mockReceiptErrorStream).findFirst();

        ReceiptNotFoundException e = assertThrows(ReceiptNotFoundException.class, () -> sut.getReceiptError("id"));

        assertNotNull(e);
    }

}