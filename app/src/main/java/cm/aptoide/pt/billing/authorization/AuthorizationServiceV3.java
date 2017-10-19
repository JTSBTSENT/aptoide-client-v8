package cm.aptoide.pt.billing.authorization;

import android.content.SharedPreferences;
import android.content.res.Resources;
import cm.aptoide.pt.billing.Customer;
import cm.aptoide.pt.billing.IdResolver;
import cm.aptoide.pt.billing.transaction.TransactionMapperV3;
import cm.aptoide.pt.billing.transaction.TransactionPersistence;
import cm.aptoide.pt.dataprovider.interfaces.TokenInvalidator;
import cm.aptoide.pt.dataprovider.ws.BodyInterceptor;
import cm.aptoide.pt.dataprovider.ws.v3.BaseBody;
import cm.aptoide.pt.dataprovider.ws.v3.CreateTransactionRequest;
import cm.aptoide.pt.dataprovider.ws.v3.GetApkInfoRequest;
import cm.aptoide.pt.dataprovider.ws.v3.GetTransactionRequest;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import rx.Single;

public class AuthorizationServiceV3 implements AuthorizationService {

  private final AuthorizationFactory authorizationFactory;
  private final AuthorizationMapperV3 authorizationMapper;
  private final TransactionMapperV3 transactionMapper;
  private final TransactionPersistence transactionPersistence;
  private final BodyInterceptor<BaseBody> bodyInterceptorV3;
  private final OkHttpClient httpClient;
  private final Converter.Factory converterFactory;
  private final TokenInvalidator tokenInvalidator;
  private final SharedPreferences sharedPreferences;
  private final Customer customer;
  private final Resources resources;
  private final IdResolver idResolver;

  public AuthorizationServiceV3(AuthorizationFactory authorizationFactory,
      AuthorizationMapperV3 authorizationMapper, TransactionMapperV3 transactionMapper,
      TransactionPersistence transactionPersistence, BodyInterceptor<BaseBody> bodyInterceptorV3,
      OkHttpClient httpClient, Converter.Factory converterFactory,
      TokenInvalidator tokenInvalidator, SharedPreferences sharedPreferences, Customer customer,
      Resources resources, IdResolver idResolver) {
    this.authorizationFactory = authorizationFactory;
    this.authorizationMapper = authorizationMapper;
    this.transactionMapper = transactionMapper;
    this.transactionPersistence = transactionPersistence;
    this.bodyInterceptorV3 = bodyInterceptorV3;
    this.httpClient = httpClient;
    this.converterFactory = converterFactory;
    this.tokenInvalidator = tokenInvalidator;
    this.sharedPreferences = sharedPreferences;
    this.customer = customer;
    this.resources = resources;
    this.idResolver = idResolver;
  }

  @Override public Single<Authorization> getAuthorization(String transactionId, String customerId) {
    return GetApkInfoRequest.of(idResolver.resolveTransactionId(transactionId), bodyInterceptorV3,
        httpClient, converterFactory, tokenInvalidator, sharedPreferences, resources)
        .observe(false)
        .toSingle()
        .flatMap(response -> {

          if (response.isOk()) {
            if (response.isPaid()) {
              return GetTransactionRequest.of(response.getPayment()
                      .getMetadata()
                      .getProductId(), bodyInterceptorV3, httpClient, converterFactory,
                  tokenInvalidator, sharedPreferences)
                  .observe(true)
                  .toSingle()
                  .map(transactionResponse -> authorizationMapper.map(
                      idResolver.generateAuthorizationId(1), customerId, transactionId,
                      transactionResponse, response));
            }
            return Single.just(
                authorizationFactory.create(idResolver.generateAuthorizationId(1), customerId,
                    AuthorizationFactory.PAYPAL_SDK, Authorization.Status.REDEEMED, null, null,
                    null, null, null, transactionId, null));
          }

          return Single.just(
              authorizationFactory.create(idResolver.generateAuthorizationId(1), customerId,
                  AuthorizationFactory.PAYPAL_SDK, Authorization.Status.FAILED, null, null, null,
                  null, null, transactionId, null));
        });
  }

  @Override
  public Single<Authorization> updateAuthorization(String transactionId, String metadata) {
    return Single.zip(
        GetApkInfoRequest.of(idResolver.resolveTransactionId(transactionId), bodyInterceptorV3,
            httpClient, converterFactory, tokenInvalidator, sharedPreferences, resources)
            .observe(true)
            .toSingle(), customer.getId(), (paidApp, customerId) -> {

          if (paidApp.isOk()) {
            return CreateTransactionRequest.of(paidApp.getPayment()
                    .getMetadata()
                    .getProductId(), 1, paidApp.getPath()
                    .getStoreName(), metadata, bodyInterceptorV3, httpClient, converterFactory,
                tokenInvalidator, sharedPreferences, 3, paidApp.getApp()
                    .getName())
                .observe(true)
                .toSingle()
                .flatMap(response -> {

                  final Authorization authorization =
                      authorizationMapper.map(idResolver.generateAuthorizationId(1), customerId,
                          transactionId, response, paidApp);

                  if (authorization.isActive()) {
                    return transactionPersistence.saveTransaction(
                        transactionMapper.map(customerId, transactionId, response,
                            idResolver.generateProductId(
                                idResolver.resolveTransactionId(transactionId))))
                        .andThen(Single.just(authorization));
                  }

                  return Single.just(authorization);
                });
          }

          return Single.just(
              authorizationFactory.create(idResolver.generateAuthorizationId(1), customerId,
                  AuthorizationFactory.PAYPAL_SDK, Authorization.Status.FAILED, null, null, null,
                  null, null, transactionId, null));
        })
        .flatMap(single -> single);
  }
}