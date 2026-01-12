package it.gov.pagopa.receipt.pdf.service.model.biz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.gov.pagopa.receipt.pdf.helpdesk.entity.event.enumeration.UserType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    private String fullName;
    private String name;
    private String surname;
    private UserType type;
    private String fiscalCode;
    private String notificationEmail;
    private String userId;
    private String userStatus;
    private String userStatusDescription;
}
