package it.gov.pagopa.receipt.pdf.service.client;

import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class ReceiptPdfGeneratorClientTest {

    @Test
    void postsToBothGeneratorEndpointsWithSubscriptionKey() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> request = new AtomicReference<>();
        AtomicInteger status = new AtomicInteger(200);
        server.createContext("/", exchange -> {
            request.set(exchange.getRequestMethod() + " " + exchange.getRequestURI().getRawPath() + " " +
                    exchange.getRequestHeaders().getFirst("Ocp-Apim-Subscription-Key"));
            byte[] body = "OK".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            ReceiptPdfGeneratorClient client = RestClientBuilder.newBuilder()
                    .baseUri(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/receipts/helpdesk/generator/v1"))
                    .build(ReceiptPdfGeneratorClient.class);
            try (Response response = client.regenerateReceipt("event-1")) {
                assertEquals(200, response.getStatus());
            }
            String apiKey = ConfigProvider.getConfig().getValue("receipt.pdf.generator.api-key", String.class);
            assertEquals("POST /receipts/helpdesk/generator/v1/receipts/event-1/regenerate-receipt-pdf " + apiKey, request.get());
            try (Response response = client.regenerateCart("cart-1")) {
                assertEquals(200, response.getStatus());
            }
            assertEquals("POST /receipts/helpdesk/generator/v1/cart-receipts/cart-1/regenerate-receipt-pdf " + apiKey, request.get());
            status.set(500);
            WebApplicationException failure = assertThrows(WebApplicationException.class, () -> client.regenerateReceipt("event-1"));
            assertEquals(500, failure.getResponse().getStatus());
            ((AutoCloseable) client).close();
        } finally {
            server.stop(0);
        }
    }
}
