package it.gov.pagopa.receipt.pdf.service.service.impl;

import it.gov.pagopa.receipt.pdf.service.client.PDVTokenizerClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.FiscalCodeNotAuthorizedException;
import it.gov.pagopa.receipt.pdf.service.model.SearchTokenRequest;
import it.gov.pagopa.receipt.pdf.service.model.SearchTokenResponse;
import it.gov.pagopa.receipt.pdf.service.utils.PerfTracer;
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

    public String getFiscalCodeToken(String requestFiscalCode)
            throws FiscalCodeNotAuthorizedException {
        try (PerfTracer tracer = PerfTracer.start(logger, "tokenizer.searchToken.http")) {
            SearchTokenResponse searchTokenResponse =
                    pdvTokenizerClient.searchToken(new SearchTokenRequest(requestFiscalCode));
            boolean hasToken = searchTokenResponse != null && searchTokenResponse.getToken() != null;
            tracer.tag("hasToken", hasToken).tag("status", hasToken ? "ok" : "missingToken");
            if (!hasToken) {
                throw new FiscalCodeNotAuthorizedException(AppErrorCodeEnum.PDFS_700, "Missing token");
            }
            return searchTokenResponse.getToken();
        } catch (FiscalCodeNotAuthorizedException e) {
            throw e;
        } catch (Exception e) {
            String errMsg = "Could not recover fiscal code token for authentication in the request";
            logger.error(errMsg, e);
            throw new FiscalCodeNotAuthorizedException(AppErrorCodeEnum.PDFS_700, errMsg);
        }
    }
}
