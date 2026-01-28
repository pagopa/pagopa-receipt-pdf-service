package it.gov.pagopa.receipt.pdf.service.resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.gov.pagopa.receipt.pdf.service.service.impl.PdfService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.restassured.RestAssured.given;
import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.PDFS_500;
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

    @InjectMock
    private PdfService pdfService;

    @Test
    @SneakyThrows
    void getReceiptPdf_200() {
        Path workingDirectory = Files.createTempDirectory("receipt-test");
        Path pdfPath = workingDirectory.resolve("file.pdf");
        byte[] pdfContent = "fake-pdf-content".getBytes();
        Files.write(pdfPath, pdfContent);

        File file = pdfPath.toFile();

        when(pdfService.getReceiptPdf(THIRD_PARTY_ID, FISCAL_CODE))
                .thenReturn(file);

        byte[] responseBytes =
                given()
                        .queryParam("fiscal_code", FISCAL_CODE)
                        .when()
                        .get("/pdf/" + THIRD_PARTY_ID)
                        .then()
                        .statusCode(200)
                        .contentType("application/pdf")
                        .header("content-disposition", "attachment;")
                        .header(FILENAME_RESPONSE_HEADER, "file.pdf")
                        .extract()
                        .asByteArray();

        assertArrayEquals(pdfContent, responseBytes);
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

    @Test
    @SneakyThrows
    void getReceiptPdf_KO_IOException_While_Reading_Pdf_REST() {
        Path tempDirectory = Files.createTempDirectory("receipt-pdf-io-error");
        File invalidFile = tempDirectory.toFile();

        when(pdfService.getReceiptPdf(THIRD_PARTY_ID, FISCAL_CODE))
                .thenReturn(invalidFile);

        String responseString = given()
                .queryParam("fiscal_code", FISCAL_CODE)
                .when()
                .get("/pdf/" + THIRD_PARTY_ID)
                .then()
                .statusCode(500)
                .contentType("application/json")
                .extract()
                .asString();

        assertTrue(responseString.contains(PDFS_500.getErrorCode()));
    }


}
