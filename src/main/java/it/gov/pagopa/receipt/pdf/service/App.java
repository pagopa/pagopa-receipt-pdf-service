package it.gov.pagopa.receipt.pdf.service;

import io.quarkus.runtime.Startup;
import it.gov.pagopa.receipt.pdf.service.model.ErrorResponse;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@OpenAPIDefinition(
    components =
        @Components(
            responses = {
              @APIResponse(
                  name = "InternalServerError",
                  responseCode = "500",
                  description = "Internal Server Error",
                  content =
                      @Content(
                          mediaType = MediaType.APPLICATION_JSON,
                          schema = @Schema(implementation = ErrorResponse.class),
                          example =
                              """
                                  {
                                    "errorId": "50905466-1881-457b-b42f-fb7b2bfb1610",
                                    "httpStatusCode": 500,
                                    "httpStatusDescription": "Internal Server Error",
                                    "appErrorCode": "PDFS_603",
                                    "errors": [
                                      {
                                        "message": "An unexpected error has occurred. Please contact support."
                                      }
                                    ]
                                  }""")),
              @APIResponse(
                  name = "AppException400",
                  responseCode = "400",
                  description = "Default app exception for status 400",
                  content =
                      @Content(
                          mediaType = MediaType.APPLICATION_JSON,
                          schema = @Schema(implementation = ErrorResponse.class),
                          examples = {
                            @ExampleObject(
                                name = "Error",
                                value =
                                    """
                                  {
                                    "httpStatusCode": 400,
                                    "httpStatusDescription": "Bad Request",
                                    "appErrorCode": "PDFS_703",
                                    "errors": [
                                      {
                                        "message": "The provided third party id [<td_id>] is invalid"
                                      }
                                    ]
                                  }"""),
                            @ExampleObject(
                                name = "Errors with path",
                                value =
                                    """
                                  {
                                    "httpStatusCode": 400,
                                    "httpStatusDescription": "Bad Request",
                                    "appErrorCode": "PDFS_703",
                                    "errors": [
                                      {
                                        "path": "<detail.path.if-exist>",
                                        "message": "<detail.message>"
                                      }
                                    ]
                                  }""")
                          })),
              @APIResponse(
                  name = "AppException404",
                  responseCode = "404",
                  description = "Default app exception for status 404",
                  content =
                      @Content(
                          mediaType = MediaType.APPLICATION_JSON,
                          schema = @Schema(implementation = ErrorResponse.class),
                          example =
                              """
                                  {
                                    "httpStatusCode": 404,
                                    "httpStatusDescription": "Not Found",
                                    "appErrorCode": "PDFS_900",
                                    "errors": [
                                      {
                                        "message": "Third party id [<td_id>] not found"
                                      }
                                    ]
                                  }""")),
            }),
    info = @Info(title = "Receipt PDF service", version = "0.0.0-SNAPSHOT"))
@Startup
public class App extends Application {}
