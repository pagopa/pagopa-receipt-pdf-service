package it.gov.pagopa.receipt.pdf.service.client.impl;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedIterable;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.BizEventNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.biz.BizEvent;
import it.gov.pagopa.receipt.pdf.service.model.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.service.producer.bizevent.containers.BizEventContainer;
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
class BizCosmosClientImplTest {

    private static Stream<BizEvent> mockBizEventStream;

    @Inject
    private BizCosmosClientImpl sut;

    @BeforeAll
    static void setUp() {
        CosmosContainer containerBizEventMock = Mockito.mock(CosmosContainer.class);
        QuarkusMock.installMockForType(
                containerBizEventMock,
                CosmosContainer.class,
                new AnnotationLiteral<BizEventContainer>() {
                }
        );

        CosmosPagedIterable<Receipt> mockReceiptIterable = Mockito.mock(CosmosPagedIterable.class);
        mockBizEventStream = Mockito.mock(Stream.class);

        doReturn(mockReceiptIterable).when(containerBizEventMock)
                .queryItems(any(SqlQuerySpec.class), any(), eq(BizEvent.class));
        doReturn(mockBizEventStream).when(mockReceiptIterable).stream();
    }

    @SneakyThrows
    @Test
    void getBizEventDocumentByOrganizationFiscalCodeAndIUVSuccess() {
        BizEvent bizEvent = new BizEvent();

        doReturn(Optional.of(bizEvent)).when(mockBizEventStream).findFirst();

        BizEvent result = assertDoesNotThrow(
                () -> sut.getBizEventDocumentByOrganizationFiscalCodeAndIUV("id", "iuv")
        );

        assertEquals(bizEvent, result);
    }

    @SneakyThrows
    @Test
    void getBizEventDocumentByOrganizationFiscalCodeAndIUVNotFound() {
        doReturn(Optional.empty()).when(mockBizEventStream).findFirst();

        BizEventNotFoundException e = assertThrows(
                BizEventNotFoundException.class,
                () -> sut.getBizEventDocumentByOrganizationFiscalCodeAndIUV("id", "iuv")
        );

        assertNotNull(e);
        assertEquals(AppErrorCodeEnum.PDFS_100, e.getErrorCode());
    }

}