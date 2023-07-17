package it.gov.pagopa.receipt.pdf.service.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({"path", "message"})
@RegisterForReflection
public class ErrorMessage {

    @Schema(example = "demo.test")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String path;

    @Schema(example = "An unexpected error has occurred. Please contact support.")
    private String message;
}