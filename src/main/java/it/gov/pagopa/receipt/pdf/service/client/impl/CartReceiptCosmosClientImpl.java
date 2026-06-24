package it.gov.pagopa.receipt.pdf.service.client.impl;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import it.gov.pagopa.receipt.pdf.service.client.CartReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.CartNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.IoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartIOMessage;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartReceiptError;
import it.gov.pagopa.receipt.pdf.service.producer.receipt.containers.CartContainer;
import it.gov.pagopa.receipt.pdf.service.producer.receipt.containers.CartReceiptsErrorContainer;
import it.gov.pagopa.receipt.pdf.service.producer.receipt.containers.CartReceiptsIOMessagesContainer;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static it.gov.pagopa.receipt.pdf.service.utils.CommonUtils.sanitize;

/**
 * Client for the CosmosDB database
 */
@ApplicationScoped
public class CartReceiptCosmosClientImpl implements CartReceiptCosmosClient {

    private static final String DOCUMENT_NOT_FOUND_ERR_MSG = "Document not found in the defined container";
    private final Logger logger = LoggerFactory.getLogger(CartReceiptCosmosClientImpl.class);

    @CartContainer
    CosmosContainer containerCartReceipts;

    @CartReceiptsIOMessagesContainer
    CosmosContainer containerCartReceiptsIOMessagesEvent;

    @CartReceiptsErrorContainer
    CosmosContainer containerCartReceiptsError;

    CartReceiptCosmosClientImpl() {
    }

    public CartForReceipt getCartForReceiptDocument(String cartId) throws CartNotFoundException {
        // Build query
        SqlQuerySpec querySpec = new SqlQuerySpec(
                "SELECT * FROM c WHERE c.cartId = @cartId",
                List.of(new SqlParameter("@cartId", cartId))
        );

        // Query the container
        return this.containerCartReceipts.queryItems(
                        querySpec, new CosmosQueryRequestOptions(), CartForReceipt.class)
                .stream()
                .findFirst()
                .orElseThrow(() -> {
                    String errMsg = String.format(
                            "Cart with id %s not found in the defined container: %s",
                            sanitize(cartId), containerCartReceipts.getId());
                    logger.error(errMsg);
                    return new CartNotFoundException(AppErrorCodeEnum.PDFS_801, errMsg, cartId);
                });
    }

    @Override
    public CartIOMessage getCartIoMessage(String messageId) throws IoMessageNotFoundException {
        //Build query
        SqlQuerySpec querySpec = new SqlQuerySpec(
                "SELECT * FROM c WHERE c.messageId = @messageId",
                List.of(new SqlParameter("@messageId", messageId))
        );

        //Query the container
        return this.containerCartReceiptsIOMessagesEvent
                .queryItems(querySpec, new CosmosQueryRequestOptions(), CartIOMessage.class)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IoMessageNotFoundException(DOCUMENT_NOT_FOUND_ERR_MSG));
    }

    @Override
    public CartReceiptError getCartReceiptError(String cartId) throws CartNotFoundException {
        //Build query
        SqlQuerySpec querySpec = new SqlQuerySpec(
                "SELECT * FROM c WHERE c.id = @cartId",
                List.of(new SqlParameter("@id", cartId))
        );

        //Query the container
        return this.containerCartReceiptsError
                .queryItems(querySpec, new CosmosQueryRequestOptions(), CartReceiptError.class)
                .stream()
                .findFirst()
                .orElseThrow(() -> new CartNotFoundException(AppErrorCodeEnum.PDFS_801, DOCUMENT_NOT_FOUND_ERR_MSG));
    }
}
