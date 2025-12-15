package it.gov.pagopa.receipt.pdf.service.model;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Builder
@Jacksonized
public class Detail {

  @Schema(example = "Questo è il titolo del messaggio")
  private String subject;

  @Schema(example = "Questo è il corpo del messaggio in formato **markdown**")
  private String markdown;
}
