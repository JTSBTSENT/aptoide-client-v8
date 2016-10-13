/*
 * Copyright (c) 2016.
 * Modified by SithEngineer on 03/08/2016.
 */

package cm.aptoide.pt.dataprovider.ws.v3;

import android.support.annotation.NonNull;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.accountmanager.ws.AptoideWsV3Exception;
import cm.aptoide.accountmanager.ws.BaseBody;
import cm.aptoide.accountmanager.ws.responses.GenericResponseV3;
import cm.aptoide.pt.dataprovider.DataProvider;
import cm.aptoide.pt.dataprovider.repository.IdsRepository;
import cm.aptoide.pt.dataprovider.ws.v2.GenericResponseV2;
import cm.aptoide.pt.model.v3.BaseV3Response;
import cm.aptoide.pt.model.v3.ErrorResponse;
import cm.aptoide.pt.model.v3.GetPushNotificationsResponse;
import cm.aptoide.pt.model.v3.InAppBillingAvailableResponse;
import cm.aptoide.pt.model.v3.InAppBillingPurchasesResponse;
import cm.aptoide.pt.model.v3.InAppBillingSkuDetailsResponse;
import cm.aptoide.pt.model.v3.PaidApp;
import cm.aptoide.pt.model.v3.PaymentResponse;
import cm.aptoide.pt.networkclient.WebService;
import cm.aptoide.pt.networkclient.okhttp.OkHttpClientFactory;
import cm.aptoide.pt.networkclient.okhttp.cache.RequestCache;
import cm.aptoide.pt.preferences.Application;
import cm.aptoide.pt.preferences.secure.SecurePreferencesImplementation;
import java.io.IOException;
import retrofit2.adapter.rxjava.HttpException;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by sithengineer on 21/07/16.
 */
public abstract class V3<U> extends WebService<V3.Interfaces, U> {

  protected static final String BASE_HOST = "http://webservices.aptoide.com/webservices/3/";
  protected final BaseBody map;
  private final String INVALID_ACCESS_TOKEN_CODE = "invalid_token";
  private boolean accessTokenRetry = false;

  protected V3(String baseHost) {
    this(baseHost, new BaseBody());
  }

  protected V3(String baseHost, BaseBody baseBody) {
    super(Interfaces.class, OkHttpClientFactory.getSingletonClient(new IdsRepository(
            SecurePreferencesImplementation.getInstance(), DataProvider
            .getContext())),
        WebService.getDefaultConverter(), baseHost);
    this.map = baseBody;
  }

  @NonNull public static String getErrorMessage(BaseV3Response response) {
    final StringBuilder builder = new StringBuilder();
    if (response != null) {
      for (ErrorResponse error : response.getErrors()) {
        builder.append(error.msg);
        builder.append(". ");
      }
      if (builder.length() == 0) {
        builder.append("Server failed with empty error list.");
      }
    } else {
      builder.append("Server returned null response.");
    }
    return builder.toString();
  }

  @Override public Observable<U> observe(boolean bypassCache) {
    return super.observe(bypassCache).onErrorResumeNext(throwable -> {
      if (throwable instanceof HttpException) {
        try {

          GenericResponseV3 genericResponseV3 =
              (GenericResponseV3) converterFactory.responseBodyConverter(GenericResponseV3.class,
                  null, null).convert(((HttpException) throwable).response().errorBody());

          if (INVALID_ACCESS_TOKEN_CODE.equals(genericResponseV3.getError())) {

            if (!accessTokenRetry) {
              accessTokenRetry = true;
              return AptoideAccountManager.invalidateAccessToken(Application.getContext())
                  .flatMap(s -> {
                    this.map.setAccess_token(s);
                    return V3.this.observe(bypassCache).observeOn(AndroidSchedulers.mainThread());
                  });
            }
          } else {
            return Observable.error(
                new AptoideWsV3Exception(throwable).setBaseResponse(genericResponseV3));
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      return Observable.error(throwable);
    }).observeOn(AndroidSchedulers.mainThread());
  }

  interface Interfaces {

    @POST("getPushNotifications") @FormUrlEncoded
    Observable<GetPushNotificationsResponse> getPushNotifications(@FieldMap BaseBody arg,
        @Header(RequestCache.BYPASS_HEADER_KEY) boolean bypassCache);

    @POST("addApkFlag") @FormUrlEncoded Observable<GenericResponseV2> addApkFlag(
        @FieldMap BaseBody arg,
        @Header(RequestCache.BYPASS_HEADER_KEY) boolean bypassCache);

    @POST("getApkInfo") @FormUrlEncoded Observable<PaidApp> getApkInfo(@FieldMap BaseBody args,
        @Header(RequestCache.BYPASS_HEADER_KEY) boolean bypassCache);

    @POST("processInAppBilling") @FormUrlEncoded
    Observable<InAppBillingAvailableResponse> getInAppBillingAvailable(@FieldMap BaseBody args);

    @POST("processInAppBilling") @FormUrlEncoded
    Observable<InAppBillingSkuDetailsResponse> getInAppBillingSkuDetails(@FieldMap BaseBody args);

    @POST("processInAppBilling") @FormUrlEncoded
    Observable<InAppBillingPurchasesResponse> getInAppBillingPurchases(@FieldMap BaseBody args);

    @POST("processInAppBilling") @FormUrlEncoded
    Observable<BaseV3Response> deleteInAppBillingPurchase(@FieldMap BaseBody args);

    @POST("checkProductPayment") @FormUrlEncoded Observable<PaymentResponse> checkProductPayment(
        @FieldMap BaseBody args);
  }
}
