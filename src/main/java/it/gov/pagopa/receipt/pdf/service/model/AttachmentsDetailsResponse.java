package it.gov.pagopa.receipt.pdf.service.model;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/** Model class for the attachment details response */
@Getter
@Builder
@Jacksonized
public class AttachmentsDetailsResponse {

  private List<Attachment> attachments;
  private Detail details;
}
