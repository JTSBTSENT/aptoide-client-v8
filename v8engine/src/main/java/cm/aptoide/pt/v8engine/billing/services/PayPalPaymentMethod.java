/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 11/08/2016.
 */

package cm.aptoide.pt.v8engine.billing.services;

import cm.aptoide.pt.v8engine.billing.PaymentMethod;
import cm.aptoide.pt.v8engine.billing.Product;
import cm.aptoide.pt.v8engine.billing.exception.PaymentLocalProcessingRequiredException;
import cm.aptoide.pt.v8engine.billing.repository.PaymentRepositoryFactory;
import rx.Completable;

public class PayPalPaymentMethod implements PaymentMethod {

  private final int id;
  private final String name;
  private final String description;
  private final PaymentRepositoryFactory paymentRepositoryFactory;

  public PayPalPaymentMethod(int id, String name, String description,
      PaymentRepositoryFactory paymentRepositoryFactory) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.paymentRepositoryFactory = paymentRepositoryFactory;
  }

  @Override public int getId() {
    return id;
  }

  @Override public String getName() {
    return name;
  }

  @Override public String getDescription() {
    return description;
  }

  @Override public Completable process(Product product) {
    return paymentRepositoryFactory.getPaymentConfirmationRepository(product)
        .getTransaction(product)
        .first()
        .toSingle()
        .flatMapCompletable(confirmation -> {
          if (confirmation.isCompleted()) {
            return Completable.complete();
          }
          return Completable.error(new PaymentLocalProcessingRequiredException(
              "PayPal SDK local processing of the payment required"));
        });
  }

  public Completable process(Product product, String payPalConfirmationId) {
    return paymentRepositoryFactory.getPaymentConfirmationRepository(product)
        .createTransaction(product, getId(), payPalConfirmationId);
  }
}