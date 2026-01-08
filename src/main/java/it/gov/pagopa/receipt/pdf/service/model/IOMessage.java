package it.gov.pagopa.receipt.pdf.service.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IOMessage {

    String id;
    String messageId;
    String eventId;
    UserType userType;
}