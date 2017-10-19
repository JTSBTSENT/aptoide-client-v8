package cm.aptoide.pt.billing.authorization;

import cm.aptoide.pt.billing.Price;

public class AuthorizationFactory {

  public static final String WEB = "WEB";
  public static final String PAYPAL_SDK = "PAYPAL_SDK";
  public static final String ADYEN_SDK = "ADYEN_SDK";

  public Authorization create(String id, String customerId, String type,
      Authorization.Status status, String url, String redirectUrl, String metadata, Price price,
      String description, String transactionId, String session) {
    switch (type) {
      case WEB:
        return new WebAuthorization(id, customerId, status, url, redirectUrl, transactionId);
      case PAYPAL_SDK:
        return new PayPalAuthorization(id, customerId, status, transactionId, metadata, price,
            description);
      case ADYEN_SDK:
        return new AdyenAuthorization(id, customerId, status, transactionId, session, metadata);
      default:
        return new Authorization(id, customerId, status, transactionId);
    }
  }

  public String getType(Authorization authorization) {
    if (authorization instanceof AdyenAuthorization) {
      return ADYEN_SDK;
    }

    if (authorization instanceof PayPalAuthorization) {
      return PAYPAL_SDK;
    }

    if (authorization instanceof WebAuthorization) {
      return WEB;
    }

    return null;
  }
}