package it.gov.pagopa.receipt.pdf.service.utils;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorAPICategory;
import it.gov.pagopa.receipt.pdf.service.utils.profiles.HelpdeskProfile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

@QuarkusTest
@TestProfile(HelpdeskProfile.class)
class OpenApiGenerationHelpdeskTest {
    @Test
    void generateOpenApi() throws Exception {
        CommonUtilsTest.generateOpenApi("openapi-helpdesk.json", AppErrorAPICategory.HELPDESK);
        Assertions.assertTrue(new File("openapi/openapi-helpdesk.json").isFile());
    }
}
