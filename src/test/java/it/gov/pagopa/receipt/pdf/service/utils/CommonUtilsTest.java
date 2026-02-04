package it.gov.pagopa.receipt.pdf.service.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorAPICategory;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class CommonUtilsTest {

    @Test
    void testSanitize_NullInput() {
        assertNull(CommonUtils.sanitize(null));
    }

    @Test
    void testSanitize_EmptyString() {
        assertEquals("", CommonUtils.sanitize(""));
    }

    @Test
    void testSanitize_TrimWhitespace() {
        assertEquals("abc", CommonUtils.sanitize("  abc  "));
    }

    @ParameterizedTest
    @MethodSource("sanitizeTestCases")
    void testSanitize_VariousInputs(String input, String expected) {
        assertEquals(expected, CommonUtils.sanitize(input));
    }

    private static Stream<Arguments> sanitizeTestCases() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of("<script>alert('XSS')</script>", "&lt;script&gt;alert(&#x27;XSS&#x27;)&lt;&#x2F;script&gt;"),
                org.junit.jupiter.params.provider.Arguments.of("a\nb\rc\td", "abcd"),
                org.junit.jupiter.params.provider.Arguments.of("&<>", "&amp;&lt;&gt;"),
                org.junit.jupiter.params.provider.Arguments.of("abc\0def", "abcdef")
        );
    }

    public static void generateOpenApi(String filename, AppErrorAPICategory category) throws Exception {
        String responseString =
                given()
                        .when().get("/q/openapi?format=json")
                        .then()
                        .statusCode(200)
                        .contentType("application/json")
                        .extract()
                        .asString();

        ObjectMapper objectMapper = new ObjectMapper();
        Object swagger = objectMapper.readValue(responseString, Object.class);
        String formatted = objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(swagger);
        formatted = formatted.replace("placeholder-for-replace", getAppErrorCodes(category));

        Path basePath = Paths.get("openapi/");
        Files.createDirectories(basePath);
        Files.write(basePath.resolve(filename), formatted.getBytes());
    }

    private static String getAppErrorCodes(AppErrorAPICategory category) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("\\n ### APP ERROR CODES ### \\n\\n\\n <details><summary>Details</summary>\\n **NAME** | **DESCRIPTION** \\n- | - ");
        for (AppErrorCodeEnum errorCode : AppErrorCodeEnum.values()) {
            if (errorCode.getCategory().contains(category)) {
                stringBuilder
                        .append("\\n **")
                        .append(errorCode.getErrorCode())
                        .append("** | ")
                        .append(errorCode.getErrorMessage());
            }
        }
        stringBuilder.append(" \\n\\n </details> \\n");
        return stringBuilder.toString();
    }
}

