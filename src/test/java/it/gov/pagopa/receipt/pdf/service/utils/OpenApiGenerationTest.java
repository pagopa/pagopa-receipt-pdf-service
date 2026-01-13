package it.gov.pagopa.receipt.pdf.service.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.restassured.RestAssured.given;

@QuarkusTest
class OpenApiGenerationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void swaggerSpringPlugin() throws Exception {

        String responseString =
                given()
                        .when().get("q/openapi?format=json")
                        .then()
                        .statusCode(200)
                        .contentType("application/json")
                        .extract()
                        .asString();

        Object swagger = objectMapper.readValue(responseString, Object.class);
        String formatted = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(swagger);
        Path basePath = Paths.get("openapi/");
        Files.createDirectories(basePath);
        Files.write(basePath.resolve("openapi.json"), formatted.getBytes());
    }
}