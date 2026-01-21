package it.gov.pagopa.receipt.pdf.service.utils.profiles;

import io.quarkus.test.junit.QuarkusTestProfile;

public class HelpdeskProfile implements QuarkusTestProfile {
    @Override
    public String getConfigProfile() {
        return "helpdesk";
    }
}
