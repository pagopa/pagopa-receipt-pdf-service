package it.gov.pagopa.receipt.pdf.service.model;

import it.gov.pagopa.receipt.pdf.service.model.biz.enumeration.UserType;
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