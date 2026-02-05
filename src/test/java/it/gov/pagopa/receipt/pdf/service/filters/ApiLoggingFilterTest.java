package it.gov.pagopa.receipt.pdf.service.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.receipt.pdf.service.model.ErrorResponse;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiLoggingFilterTest {

    @Mock
    ContainerRequestContext requestContext;

    @Mock
    ContainerResponseContext responseContext;

    @Mock
    ResourceInfo resourceInfo;

    @Mock
    UriInfo uriInfo;

    private ApiLoggingFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        filter = new ApiLoggingFilter(objectMapper) {
            @Override
            protected void clearMdc() {
                // no-op for test
            }
        };

        // Inject resourceInfo into the @Context field via reflection
        Field field = ApiLoggingFilter.class.getDeclaredField("resourceInfo");
        field.setAccessible(true);
        field.set(filter, resourceInfo);

        when(resourceInfo.getResourceMethod())
                .thenReturn(ApiLoggingFilterTest.class.getDeclaredMethod("dummyEndpointMethod"));

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("fiscal_code", "AAAAAAAAAAAAAAAA");

        when(uriInfo.getQueryParameters()).thenReturn(queryParams);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    // Used only to have a method name in the logs
    void dummyEndpointMethod() {
        // no-op
    }

    @Test
    void onRequestPopulatesMdcWithBaseMetadata() {
        // when
        filter.onRequest(requestContext);

        // then
        assertEquals("dummyEndpointMethod", MDC.get(ApiLoggingFilter.METHOD));
        assertNotNull(MDC.get(ApiLoggingFilter.START_TIME));
        assertNotNull(MDC.get(ApiLoggingFilter.OPERATION_ID));
        assertNotNull(MDC.get(ApiLoggingFilter.REQUEST_ID));

        String argsJson = MDC.get(ApiLoggingFilter.ARGS);
        assertNotNull(argsJson);
        assertTrue(argsJson.contains("fiscal_code"));
        assertTrue(argsJson.contains("********AAAAAAAA"));
    }

    @Test
    void handleResponsePopulateFieldsOK() {
        filter.onRequest(requestContext);

        when(responseContext.getStatus()).thenReturn(Response.Status.OK.getStatusCode());

        // when
        filter.handleResponse(responseContext);

        // then
        assertEquals("OK", MDC.get(ApiLoggingFilter.STATUS));
        assertEquals("200", MDC.get(ApiLoggingFilter.CODE));
        assertEquals("", MDC.get(ApiLoggingFilter.RESPONSE));
        assertNull(MDC.get(ApiLoggingFilter.FAULT_CODE));
        assertNull(MDC.get(ApiLoggingFilter.FAULT_DETAIL));

        String responseTime = MDC.get(ApiLoggingFilter.RESPONSE_TIME);
        assertNotNull(responseTime);
        assertNotEquals("-", responseTime);
    }

    @Test
    void handleResponsePopulateFieldsKO() {
        filter.onRequest(requestContext);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(404)
                .title("Not Found")
                .detail("Receipt not found")
                .instance("PDFS_800")
                .build();

        when(responseContext.getStatus()).thenReturn(404);
        when(responseContext.getEntity()).thenReturn(errorResponse);

        // when
        filter.handleResponse(responseContext);

        // then
        assertEquals("KO", MDC.get(ApiLoggingFilter.STATUS));
        assertEquals("404", MDC.get(ApiLoggingFilter.CODE));

        String responseJson = MDC.get(ApiLoggingFilter.RESPONSE);
        assertNotNull(responseJson);
        assertTrue(responseJson.contains("PDFS_800"));
        assertTrue(responseJson.contains("Receipt not found"));

        assertEquals("PDFS_800", MDC.get(ApiLoggingFilter.FAULT_CODE));
        assertEquals("Receipt not found", MDC.get(ApiLoggingFilter.FAULT_DETAIL));

        String responseTime = MDC.get(ApiLoggingFilter.RESPONSE_TIME);
        assertNotNull(responseTime);
        assertNotEquals("-", responseTime);
    }

    @Test
    void handleResponseUnexpectedGenericKO() {
        filter.onRequest(requestContext);

        when(responseContext.getStatus()).thenReturn(418); // I'm a teapot status
        when(responseContext.getEntity()).thenReturn("plain body");

        // when
        filter.handleResponse(responseContext);

        // then
        assertEquals("KO", MDC.get(ApiLoggingFilter.STATUS));
        assertEquals("500", MDC.get(ApiLoggingFilter.CODE));
        assertEquals("", MDC.get(ApiLoggingFilter.RESPONSE));
        assertEquals("", MDC.get(ApiLoggingFilter.FAULT_CODE));
        assertEquals("", MDC.get(ApiLoggingFilter.FAULT_DETAIL));
    }
}