package it.gov.pagopa.receipt.pdf.service.utils;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorAPICategory;
import it.gov.pagopa.receipt.pdf.service.utils.profiles.PdfProfile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

@QuarkusTest
@TestProfile(PdfProfile.class)
class OpenApiGenerationPDFTest {
    @Test
    void generateOpenApi() throws Exception {
        CommonUtilsTest.generateOpenApi("openapi-pdf.json", AppErrorAPICategory.PDF);
        Assertions.assertTrue(new File("openapi/openapi-pdf.json").isFile());
    }
}
