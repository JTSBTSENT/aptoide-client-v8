package cm.aptoide.pt.view.share;

import android.content.SharedPreferences;
import android.os.Bundle;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.presenter.Presenter;
import cm.aptoide.pt.presenter.View;

/**
 * Created by pedroribeiro on 30/08/17.
 */

public class NotLoggedInSharePresenter implements Presenter {

  private final NotLoggedInShareView view;
  private final AptoideAccountManager accountManager;
  private final SharedPreferences sharedPreferences;
  private CrashReport crashReport;

  public NotLoggedInSharePresenter(NotLoggedInShareView view, AptoideAccountManager accountManager,
      SharedPreferences sharedPreferences, CrashReport crashReport) {
    this.view = view;
    this.accountManager = accountManager;
    this.sharedPreferences = sharedPreferences;
    this.crashReport = crashReport;
  }

  @Override public void present() {
    view.getLifecycle()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .doOnNext(__ -> view.facebookInit())
        .flatMap(viewCreated -> view.facebookButtonPress())
        .doOnNext(__ -> view.facebookLogin())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, throwable -> crashReport.log(throwable));
    view.getLifecycle()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(viewCreated -> view.closePress())
        .doOnNext(__ -> view.closeFragment())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, throwable -> crashReport.log(throwable));
  }

  @Override public void saveState(Bundle state) {

  }

  @Override public void restoreState(Bundle state) {

  }
}
