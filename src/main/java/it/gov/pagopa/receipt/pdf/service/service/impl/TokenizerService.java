package it.gov.pagopa.receipt.pdf.service.service.impl;

import it.gov.pagopa.receipt.pdf.service.client.PDVTokenizerClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.FiscalCodeNotAuthorizedException;
import it.gov.pagopa.receipt.pdf.service.model.SearchTokenRequest;
import it.gov.pagopa.receipt.pdf.service.model.SearchTokenResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class TokenizerService {
    private final Logger logger = LoggerFactory.getLogger(TokenizerService.class);
    private final PDVTokenizerClient pdvTokenizerClient;

    @Inject
    public TokenizerService(@RestClient PDVTokenizerClient pdvTokenizerClient) {
        this.pdvTokenizerClient = pdvTokenizerClient;
    }

    public SearchTokenResponse getSearchTokenResponse(String requestFiscalCode)
            throws FiscalCodeNotAuthorizedException {
        SearchTokenResponse searchTokenResponse;
        try {
            searchTokenResponse =
                    pdvTokenizerClient.searchToken(new SearchTokenRequest(requestFiscalCode));
            if (searchTokenResponse == null || searchTokenResponse.getToken() == null) {
                throw new FiscalCodeNotAuthorizedException(AppErrorCodeEnum.PDFS_700, "Missing token");
            }
        } catch (Exception e) {
            String errMsg = "Could not recover fiscal code token for authentication in the request";
            logger.error(errMsg, e);
            throw new FiscalCodeNotAuthorizedException(AppErrorCodeEnum.PDFS_700, errMsg);
        }
        return searchTokenResponse;
    }
}
