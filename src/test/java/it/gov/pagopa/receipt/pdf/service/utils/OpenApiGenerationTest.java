package it.gov.pagopa.receipt.pdf.service.utils;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorAPICategory;
import it.gov.pagopa.receipt.pdf.service.utils.profiles.AttachmentsProfile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

@QuarkusTest
@TestProfile(AttachmentsProfile.class)
class OpenApiGenerationTest {
    @Test
    void generateOpenApi() throws Exception {
        CommonUtilsTest.generateOpenApi("openapi.json", AppErrorAPICategory.ATTACHMENTS);
        Assertions.assertTrue(new File("openapi/openapi.json").isFile());
    }
}
