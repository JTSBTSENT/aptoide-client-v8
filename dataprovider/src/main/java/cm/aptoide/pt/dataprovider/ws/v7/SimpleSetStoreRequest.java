package cm.aptoide.pt.dataprovider.ws.v7;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import cm.aptoide.pt.dataprovider.BuildConfig;
import cm.aptoide.pt.dataprovider.interfaces.TokenInvalidator;
import cm.aptoide.pt.dataprovider.ws.BodyInterceptor;
import cm.aptoide.pt.model.v7.BaseV7Response;
import cm.aptoide.pt.preferences.toolbox.ToolboxManager;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import rx.Observable;

/**
 * Created by pedroribeiro on 15/12/16.
 */

public class SimpleSetStoreRequest extends V7<BaseV7Response, SimpleSetStoreRequest.Body> {

  @NonNull public static String getHost(SharedPreferences sharedPreferences) {
    return (ToolboxManager.isToolboxEnableHttpScheme(sharedPreferences) ? "http"
        : BuildConfig.APTOIDE_WEB_SERVICES_SCHEME)
        + "://"
        + BuildConfig.APTOIDE_WEB_SERVICES_WRITE_V7_HOST
        + "/api/7/";
  }

  protected SimpleSetStoreRequest(Body body, BodyInterceptor<BaseBody> bodyInterceptor,
      OkHttpClient httpClient, Converter.Factory converterFactory,
      TokenInvalidator tokenInvalidator, SharedPreferences sharedPreferences) {
    super(body, getHost(sharedPreferences), httpClient, converterFactory, bodyInterceptor, tokenInvalidator);
  }

  public static SimpleSetStoreRequest of(String storeName, String storeTheme,
      BodyInterceptor<BaseBody> bodyInterceptor, OkHttpClient httpClient,
      Converter.Factory converterFactory, TokenInvalidator tokenInvalidator,
      SharedPreferences sharedPreferences) {
    Body body = new Body(storeName, storeTheme);
    return new SimpleSetStoreRequest(body, bodyInterceptor, httpClient, converterFactory,
        tokenInvalidator, sharedPreferences);
  }

  public static SimpleSetStoreRequest of(long storeId, String storeTheme, String storeDescription,
      BodyInterceptor<BaseBody> bodyInterceptor, OkHttpClient httpClient,
      Converter.Factory converterFactory, TokenInvalidator tokenInvalidator,
      SharedPreferences sharedPreferences) {
    Body body = new Body(storeId, storeTheme, storeDescription);
    return new SimpleSetStoreRequest(body, bodyInterceptor, httpClient, converterFactory,
        tokenInvalidator, sharedPreferences);
  }

  @Override protected Observable<BaseV7Response> loadDataFromNetwork(Interfaces interfaces,
      boolean bypassCache) {
    return interfaces.editStore(body);
  }

  @Data @EqualsAndHashCode(callSuper = true) public static class Body extends BaseBody {

    private String storeName;
    private Long storeId;
    @Getter @Setter private StoreProperties storeProperties;

    public Body(String storeName, String storeTheme) {
      this.storeName = storeName;
      storeProperties = new StoreProperties(storeTheme, null);
    }

    public Body(long storeId, String storeTheme, String storeDescription) {
      this.storeId = storeId;
      storeProperties = new StoreProperties(storeTheme, storeDescription);
    }
  }

  @Data public static class StoreProperties {

    @JsonProperty("theme") private String theme;
    @JsonProperty("description") private String description;

    public StoreProperties(String theme, String description) {
      this.theme = theme;
      this.description = description;
    }
  }
}
