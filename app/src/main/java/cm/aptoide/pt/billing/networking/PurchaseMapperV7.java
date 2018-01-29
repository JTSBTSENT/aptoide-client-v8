/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 25/08/2016.
 */

package cm.aptoide.pt.billing.networking;

import cm.aptoide.pt.billing.binder.BillingBinderSerializer;
import cm.aptoide.pt.billing.purchase.Purchase;
import cm.aptoide.pt.billing.purchase.PurchaseFactory;
import cm.aptoide.pt.dataprovider.ws.v7.billing.PurchaseResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.List;

public class PurchaseMapperV7 {

  private final BillingBinderSerializer serializer;
  private final PurchaseFactory purchaseFactory;

  public PurchaseMapperV7(BillingBinderSerializer serializer, PurchaseFactory purchaseFactory) {
    this.serializer = serializer;
    this.purchaseFactory = purchaseFactory;
  }

  public List<Purchase> map(List<PurchaseResponse> responseList) {

    final List<Purchase> purchases = new ArrayList<>(responseList.size());

    for (PurchaseResponse response : responseList) {
      purchases.add(map(response));
    }
    return purchases;
  }

  public Purchase map(PurchaseResponse response) {
    try {
      return purchaseFactory.create(response.getProduct()
              .getId(), response.getSignature(), serializer.serializePurchase(response.getData()
              .getDeveloperPurchase()), response.getData()
              .getDeveloperPurchase()
              .getPurchaseState() == 0 ? Purchase.Status.COMPLETED : Purchase.Status.FAILED,
          response.getProduct()
              .getSku());
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
