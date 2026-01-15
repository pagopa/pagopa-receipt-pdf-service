package it.gov.pagopa.receipt.pdf.service.utils.profiles;

import io.quarkus.test.junit.QuarkusTestProfile;

public class AttachmentsProfile implements QuarkusTestProfile {
    @Override
    public String getConfigProfile() {
        return "attachments";
    }
}
