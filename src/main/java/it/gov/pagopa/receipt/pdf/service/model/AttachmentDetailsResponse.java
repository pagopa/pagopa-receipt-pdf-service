package it.gov.pagopa.receipt.pdf.service.model;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Builder
@Jacksonized
public class AttachmentDetailsResponse {

    private List<Attachment> attachments;
}
