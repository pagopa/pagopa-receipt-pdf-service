package it.gov.pagopa.receipt.pdf.service.client.impl;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.util.CosmosPagedIterable;
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
import it.gov.pagopa.receipt.pdf.service.utils.PerfTracer;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        String query = String.format("SELECT * FROM c WHERE c.cartId = '%s'", cartId);

        CosmosPagedIterable<CartForReceipt> queryResponse;
        try (PerfTracer t = PerfTracer.start(logger, "cosmos.cartReceipts.queryItems")
                .tag("container", containerCartReceipts.getId())) {
            queryResponse = this.containerCartReceipts.queryItems(
                    query, new CosmosQueryRequestOptions(), CartForReceipt.class);
        }

        boolean hasNext;
        CartForReceipt cart = null;
        try (PerfTracer t = PerfTracer.start(logger, "cosmos.cartReceipts.iterateFirst")
                .tag("container", containerCartReceipts.getId())) {
            var iterator = queryResponse.iterator();
            hasNext = iterator.hasNext();
            t.tag("found", hasNext);
            if (hasNext) {
                cart = iterator.next();
            }
        }

        if (!hasNext) {
            String errMsg =
                    String.format(
                            "Cart with id %s not found in the defined container: %s",
                            sanitize(cartId), containerCartReceipts.getId());
            logger.error(errMsg);
            throw new CartNotFoundException(AppErrorCodeEnum.PDFS_801, errMsg, cartId);
        }
        return cart;
    }

    @Override
    public CartIOMessage getCartIoMessage(String messageId) throws IoMessageNotFoundException {
        //Build query
        String query = String.format("SELECT * FROM c WHERE c.messageId = '%s'", messageId);

        //Query the container
        CosmosPagedIterable<CartIOMessage> queryResponse = this.containerCartReceiptsIOMessagesEvent
                .queryItems(query, new CosmosQueryRequestOptions(), CartIOMessage.class);

        if (queryResponse.iterator().hasNext()) {
            return queryResponse.iterator().next();
        }
        throw new IoMessageNotFoundException(DOCUMENT_NOT_FOUND_ERR_MSG);
    }

    @Override
    public CartReceiptError getCartReceiptError(String cartId) throws CartNotFoundException {
        //Build query
        String query = String.format("SELECT * FROM c WHERE c.id = '%s'", cartId);

        //Query the container
        CosmosPagedIterable<CartReceiptError> queryResponse = this.containerCartReceiptsError
                .queryItems(query, new CosmosQueryRequestOptions(), CartReceiptError.class);

        if (queryResponse.iterator().hasNext()) {
            return queryResponse.iterator().next();
        }
        throw new CartNotFoundException(AppErrorCodeEnum.PDFS_801, DOCUMENT_NOT_FOUND_ERR_MSG);
    }
}
