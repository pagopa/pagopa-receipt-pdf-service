package it.gov.pagopa.receipt.pdf.service.client.impl;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.util.CosmosPagedIterable;
import it.gov.pagopa.receipt.pdf.service.client.CartReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.CartNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.service.producer.CartContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static it.gov.pagopa.receipt.pdf.service.utils.CommonUtils.sanitize;

/** Client for the CosmosDB database */
@ApplicationScoped
public class CartReceiptCosmosClientImpl implements CartReceiptCosmosClient {

  private final Logger logger = LoggerFactory.getLogger(CartReceiptCosmosClientImpl.class);

  @CartContainer private final CosmosContainer containerCartReceipts;

  @Inject
  public CartReceiptCosmosClientImpl(@CartContainer CosmosContainer containerCartReceipts) {
    this.containerCartReceipts = containerCartReceipts;
  }

  public CartForReceipt getCartForReceiptDocument(String cartId) throws CartNotFoundException {
    // Build query
    String query = String.format("SELECT * FROM c WHERE c.eventId = '%s'", cartId);

    // Query the container
    CosmosPagedIterable<CartForReceipt> queryResponse =
        containerCartReceipts.queryItems(
            query, new CosmosQueryRequestOptions(), CartForReceipt.class);

    if (!queryResponse.iterator().hasNext()) {
      String errMsg =
          String.format(
              "Cart with id %s not found in the defined container: %s",
              sanitize(cartId), containerCartReceipts.getId());
      logger.error(errMsg);
      throw new CartNotFoundException(AppErrorCodeEnum.PDFS_801, errMsg, cartId);
    }
    return queryResponse.iterator().next();
  }
}
