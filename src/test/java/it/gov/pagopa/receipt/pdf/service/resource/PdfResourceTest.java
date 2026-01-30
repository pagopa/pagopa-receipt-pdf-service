package it.gov.pagopa.receipt.pdf.service.resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.gov.pagopa.receipt.pdf.service.model.ReceiptPdfResponse;
import it.gov.pagopa.receipt.pdf.service.service.impl.PdfService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.PDFS_901;
import static it.gov.pagopa.receipt.pdf.service.utils.Constants.FILENAME_RESPONSE_HEADER;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
class PdfResourceTest {
    private static final String FISCAL_CODE = "AAAAAAAAAAAAAAAA";
    public static final String THIRD_PARTY_ID = "thirdPartyId";
    public static final String INVALID_CF = "invalidCF";
    public static final String PDF_NAME = "pdfName";

    @InjectMock
    private PdfService pdfService;

    @Test
    @SneakyThrows
    void getReceiptPdf_200() {
        byte[] pdf = "temp".getBytes(StandardCharsets.UTF_8);
        ReceiptPdfResponse receiptPdfResponse = ReceiptPdfResponse.builder()
                .pdfFile(new ByteArrayInputStream(pdf))
                .attachmentName(PDF_NAME)
                .build();

        when(pdfService.getReceiptPdf(THIRD_PARTY_ID, FISCAL_CODE))
                .thenReturn(receiptPdfResponse);

        byte[] responseBytes =
                given()
                        .queryParam("fiscal_code", FISCAL_CODE)
                        .when()
                        .get("/pdf/" + THIRD_PARTY_ID)
                        .then()
                        .statusCode(200)
                        .contentType("application/pdf")
                        .header("content-disposition", "attachment;")
                        .header(FILENAME_RESPONSE_HEADER, PDF_NAME)
                        .extract()
                        .asByteArray();

        assertArrayEquals(pdf, responseBytes);
    }

    @Test
    @SneakyThrows
    void getReceiptPdf_400_InvalidFiscalCode() {
        String responseString =
                given()
                        .queryParam("fiscal_code", INVALID_CF)
                        .when()
                        .get("/pdf/" + THIRD_PARTY_ID)
                        .then()
                        .statusCode(400)
                        .contentType("application/json")
                        .extract()
                        .asString();

        assertTrue(responseString.contains(PDFS_901.getErrorCode()));
        verify(pdfService, never()).getReceiptPdf(anyString(), anyString());
    }
}
