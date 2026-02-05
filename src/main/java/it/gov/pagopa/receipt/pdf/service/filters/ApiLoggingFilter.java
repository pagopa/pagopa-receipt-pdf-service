package it.gov.pagopa.receipt.pdf.service.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.gov.pagopa.receipt.pdf.service.utils.CommonUtils;
import jakarta.ws.rs.core.Response;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import it.gov.pagopa.receipt.pdf.service.model.ErrorResponse;


@ApplicationScoped
public class ApiLoggingFilter {

	private final Logger logger = LoggerFactory.getLogger(ApiLoggingFilter.class);

	public static final String START_TIME = "startTime";
	public static final String METHOD = "method";
	public static final String STATUS = "status";
	public static final String CODE = "httpCode";
	public static final String RESPONSE_TIME = "responseTime";
	public static final String RESPONSE = "response";
	public static final String FAULT_CODE = "faultCode";
	public static final String FAULT_DETAIL = "faultDetail";
	public static final String REQUEST_ID = "requestId";
	public static final String OPERATION_ID = "operationId";
	public static final String ARGS = "args";

	@Context
	ResourceInfo resourceInfo;

	private final ObjectMapper objectMapper;

	public ApiLoggingFilter(ObjectMapper objectMapper) {
		objectMapper.registerModule(new JavaTimeModule());
		this.objectMapper = objectMapper;
	}

	@LoggedAPI
	@Priority(Priorities.USER)
	@ServerRequestFilter
	public Optional<RestResponse<Void>> onRequest(ContainerRequestContext ctx) {
		String methodName =
				this.resourceInfo.getResourceMethod() != null
				? this.resourceInfo.getResourceMethod().getName()
						: "";

		MDC.put(METHOD, methodName);
		MDC.put(START_TIME, String.valueOf(System.currentTimeMillis()));
		MDC.put(OPERATION_ID, UUID.randomUUID().toString());

		if (MDC.get(REQUEST_ID) == null) {
			MDC.put(REQUEST_ID, UUID.randomUUID().toString());
		}

		MDC.put(ARGS, getParams(ctx));

		logger.info("Invoking API operation {}", methodName);

		return Optional.empty();
	}

	@LoggedAPI
	@Priority(Priorities.USER)
	@ServerResponseFilter
	public void onResponse(ContainerResponseContext responseContext) {
	    try {
	        handleResponse(responseContext);
	    } finally {
	        clearMdc();
	    }
	}
	
	void handleResponse(ContainerResponseContext responseContext) {
	    String method = MDC.get(METHOD);

	    MDC.put(RESPONSE_TIME, getExecutionTime());

	    if (responseContext.getStatus() == Response.Status.OK.getStatusCode()) {
	        MDC.put(STATUS, "OK");
	        MDC.put(CODE, String.valueOf(responseContext.getStatus()));
	        MDC.put(RESPONSE, "");
	        logger.info("Successful API operation {}", method);

	    } else if (responseContext.getEntity() instanceof ErrorResponse errorResponse) {
	        MDC.put(STATUS, "KO");
	        // HTTP status code
	        MDC.put(CODE, String.valueOf(errorResponse.getStatus()));
	        // the error is logged as JSON
	        MDC.put(RESPONSE, toJsonString(errorResponse));
	        // application code (e.g. PDFS-500)
	        MDC.put(FAULT_CODE, errorResponse.getInstance());
	        // detail
	        MDC.put(FAULT_DETAIL, errorResponse.getDetail());
	        logger.info("Failed API operation {}", method);

	    } else {
	        MDC.put(STATUS, "KO");
	        MDC.put(CODE, "500");
	        MDC.put(RESPONSE, "");
	        MDC.put(FAULT_CODE, "");
	        MDC.put(FAULT_DETAIL, "");
	        logger.info("Unexpected response for API operation {}", method);
	    }
	}
	
	protected void clearMdc() {
	    MDC.remove(METHOD);
	    MDC.remove(START_TIME);
	    MDC.remove(OPERATION_ID);
	    MDC.remove(REQUEST_ID);
	    MDC.remove(ARGS);
	    MDC.remove(STATUS);
	    MDC.remove(CODE);
	    MDC.remove(RESPONSE_TIME);
	    MDC.remove(RESPONSE);
	    MDC.remove(FAULT_CODE);
	    MDC.remove(FAULT_DETAIL);
	}

	public String getExecutionTime() {
		String startTime = MDC.get(START_TIME);
		if (startTime != null) {
			long endTime = System.currentTimeMillis();
			long executionTime = endTime - Long.parseLong(startTime);
			return String.valueOf(executionTime);
		}
		return "-";
	}

	private String getParams(ContainerRequestContext ctx) {
		Map<String, Object> params = new HashMap<>();
		MultivaluedMap<String, String> queryParams = ctx.getUriInfo().getQueryParameters();
		if (queryParams != null && !queryParams.isEmpty()) {
			for (var item : queryParams.entrySet()) {
				var key = item.getKey();
				var value = item.getValue();

				if (!"fiscal_code".equals(key) ){
					params.put(key, value);
				}

			}
		}
		return toJsonString(params);
	}

	private String toJsonString(Object param) {
		try {
			return this.objectMapper.writeValueAsString(param);
		} catch (JsonProcessingException e) {
			logger.warn("An error occurred when trying to parse a parameter", e);
			return "parsing error";
		}
	}
}