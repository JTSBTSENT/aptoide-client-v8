/*
 * Copyright (c) 2016.
 * Modified by SithEngineer on 09/06/2016.
 */

package cm.aptoide.pt.v8engine.view.recycler.widget.implementations.appView;

import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import cm.aptoide.pt.database.Database;
import cm.aptoide.pt.database.realm.Installed;
import cm.aptoide.pt.dataprovider.ws.v7.listapps.StoreUtils;
import cm.aptoide.pt.imageloader.ImageLoader;
import cm.aptoide.pt.model.v7.GetApp;
import cm.aptoide.pt.model.v7.GetAppMeta;
import cm.aptoide.pt.model.v7.store.Store;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.utils.ShowMessage;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.fragment.implementations.AppViewFragment;
import cm.aptoide.pt.v8engine.fragment.implementations.StoreFragment;
import cm.aptoide.pt.v8engine.util.FragmentUtils;
import cm.aptoide.pt.v8engine.util.RollbackUtils;
import cm.aptoide.pt.v8engine.util.StoreThemeEnum;
import cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.appView.AppViewInstallDisplayable;
import cm.aptoide.pt.v8engine.view.recycler.widget.Displayables;
import cm.aptoide.pt.v8engine.view.recycler.widget.Widget;
import io.realm.Realm;
import lombok.Cleanup;

/**
 * Created by sithengineer on 06/05/16.
 */
@Displayables({AppViewInstallDisplayable.class})
public class AppViewInstallWidget extends Widget<AppViewInstallDisplayable> {

	//
	// downloading views
	//
	private RelativeLayout downloadProgressLayout;
	private CheckBox btinstallshare;
	private ProgressBar downloadProgress;
	private TextView textProgress;
	private ImageView actionResume;
	private ImageView actionCancel;

	//
	// to (un)install views
	//
	private LinearLayout latestVersionLayout;
	private Button getLatestButton;
	private Button uninstallButton;
	private Button installButton;

	// Store info
	private ImageView storeAvatarView;
	private TextView storeNameView;
	private TextView storeNumberUsersView;
	private Button subscribeButton;
	private View storeLayout;

	public AppViewInstallWidget(View itemView) {
		super(itemView);
	}

	@Override
	protected void assignViews(View itemView) {

		// bind "download" views
		downloadProgressLayout = (RelativeLayout) itemView.findViewById(R.id.download_progress_layout);
		btinstallshare = (CheckBox) itemView.findViewById(R.id.btinstallshare);
		downloadProgress = (ProgressBar) itemView.findViewById(R.id.downloading_progress);
		textProgress = (TextView) itemView.findViewById(R.id.text_progress);

		actionResume = (ImageView) itemView.findViewById(R.id.ic_action_resume);
		actionCancel = (ImageView) itemView.findViewById(R.id.ic_action_cancel);

		// bind "install and latest versions" views
		latestVersionLayout = (LinearLayout) itemView.findViewById(R.id.latestversion_layout);

		getLatestButton = (Button) itemView.findViewById(R.id.btn_get_latest);
		installButton = (Button) itemView.findViewById(R.id.btn_install);
		uninstallButton = (Button) itemView.findViewById(R.id.btn_uninstall);

		// Store info
		storeAvatarView = ((ImageView) itemView.findViewById(R.id.store_avatar));
		storeNameView = ((TextView) itemView.findViewById(R.id.store_name));
		storeNumberUsersView = ((TextView) itemView.findViewById(R.id.store_number_users));
		subscribeButton = ((Button) itemView.findViewById(R.id.btn_subscribe));
		storeLayout = itemView.findViewById(R.id.store_layout);
	}

	@Override
	public void bindView(AppViewInstallDisplayable displayable) {

		GetApp getApp = displayable.getPojo();
		GetAppMeta.App app = getApp.getNodes().getMeta().getData();

		setupInstallButton(app);
		setupUninstalButton(getApp);
		setupStoreInfo(getApp);
	}

	public void setupInstallButton(GetAppMeta.App app) {
		@Cleanup Realm realm = Database.get();
		Installed installed = Database.InstalledQ.get(app.getPackageName(), realm);

		//check if the app is installed
		if (installed == null) {
			//check if the app is payed
			if (app.getPay() != null && app.getPay().getPrice() > 0) {
				installButton.setText(R.string.buy);
				installButton.setOnClickListener(new Listeners().newBuyListener());
			} else {
				installButton.setText(R.string.install);
				installButton.setOnClickListener(new Listeners().newInstallListener());
			}
		} else {
			if (app.getFile().getVercode() > installed.getVersionCode()) {
				installButton.setText(R.string.update);
				installButton.setOnClickListener(new Listeners().newUpdateListener());
			} else if (app.getFile().getVercode() < installed.getVersionCode()) {
				installButton.setText(R.string.downgrade);
				installButton.setOnClickListener(new Listeners().newDowngradeListener());
			} else {
				installButton.setText(R.string.open);
				installButton.setOnClickListener(new Listeners().newOpenAppListener(app.getPackageName()));
			}
		}
	}

	private void setupUninstalButton(GetApp getApp) {
		GetAppMeta.App app = getApp.getNodes().getMeta().getData();
		@Cleanup Realm realm = Database.get();
		Installed installed = Database.InstalledQ.get(app.getPackageName(), realm);

		if (isLatestAvailable(getApp)) {
			latestVersionLayout.setVisibility(View.GONE);
			uninstallButton.setVisibility(View.GONE);
			getLatestButton.setVisibility(View.VISIBLE);
			getLatestButton.setOnClickListener(new Listeners().newGetLatestListener((FragmentActivity) itemView
					.getContext(), getApp));
		}
		if (installed != null) {
			getLatestButton.setVisibility(View.GONE);
			latestVersionLayout.setVisibility(View.GONE);
			uninstallButton.setVisibility(View.VISIBLE);
			uninstallButton.setOnClickListener(new Listeners().newUninstallListener(itemView, installed.getPackageName
					()));
		} else {
			uninstallButton.setVisibility(View.GONE);
			getLatestButton.setVisibility(View.GONE);
			latestVersionLayout.setVisibility(View.VISIBLE);
		}
	}

	private void setupStoreInfo(GetApp getApp) {

		GetAppMeta.App app = getApp.getNodes().getMeta().getData();
		Store store = app.getStore();

		if (TextUtils.isEmpty(store.getAvatar())) {
			ImageLoader.loadWithCircleTransform(R.drawable.ic_avatar_apps, storeAvatarView);
		} else {
			ImageLoader.loadWithCircleTransform(store.getAvatar(), storeAvatarView);
		}

		StoreThemeEnum storeThemeEnum = StoreThemeEnum.get(store);

		storeNameView.setText(store.getName());
		storeNameView.setTextColor(storeThemeEnum.getStoreHeaderInt());
		storeNumberUsersView.setText(String.valueOf(store.getStats().getSubscribers()));
		subscribeButton.setBackgroundDrawable(storeThemeEnum.getButtonLayoutDrawable());
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			subscribeButton.setElevation(0);
		}
		subscribeButton.setTextColor(storeThemeEnum.getStoreHeaderInt());
		storeLayout.setOnClickListener(new Listeners().newOpenStoreListener(itemView, store.getName()));

		@Cleanup Realm realm = Database.get();
		boolean subscribed = Database.StoreQ.get(store.getId(), realm) != null;

		if (subscribed) {
			int checkmarkDrawable = storeThemeEnum.getCheckmarkDrawable();
			subscribeButton.setCompoundDrawablesWithIntrinsicBounds(checkmarkDrawable, 0, 0, 0);
			subscribeButton.setText(R.string.appview_subscribed_store_button_text);
			subscribeButton.setOnClickListener(new Listeners().newOpenStoreListener(itemView, store.getName()));
		} else {
			int plusMarkDrawable = storeThemeEnum.getPlusmarkDrawable();
			subscribeButton.setCompoundDrawablesWithIntrinsicBounds(plusMarkDrawable, 0, 0, 0);
			subscribeButton.setText(R.string.appview_subscribe_store_button_text);
			subscribeButton.setOnClickListener(new Listeners().newSubscribeStoreListener(itemView, store.getName()));
		}
	}

	private boolean isLatestAvailable(GetApp getApp) {
		return (getApp.getNodes().getVersions() != null && !getApp.getNodes().getVersions().getList().isEmpty() &&
				getApp.getNodes().getMeta().getData().getFile().getVercode() < getApp.getNodes()
						.getVersions()
						.getList()
						.get(0)
						.getFile()
						.getVercode());
	}

	private static class Listeners {

		private View.OnClickListener newBuyListener() {
			return null;
		}

		private View.OnClickListener newInstallListener() {
			return null;
		}

		private View.OnClickListener newUpdateListener() {
			return null;
		}

		private View.OnClickListener newDowngradeListener() {
			return null;
		}

		private View.OnClickListener newOpenAppListener(String packageName) {
			return v -> AptoideUtils.SystemU.openApp(packageName);
		}

		private View.OnClickListener newGetLatestListener(FragmentActivity fragmentActivity, GetApp getApp) {
			return v -> {
				long latestAppId = getApp.getNodes().getVersions().getList().get(0).getId();

				FragmentUtils.replaceFragmentV4(fragmentActivity, AppViewFragment.newInstance(latestAppId));
			};
		}

		private View.OnClickListener newUninstallListener(View itemView, String packageName) {
			return v -> {
				AptoideUtils.ThreadU.runOnIoThread(() -> RollbackUtils.addUninstallAction(packageName));
				AptoideUtils.SystemU.uninstallApp(itemView.getContext(), packageName);
			};
		}

		private View.OnClickListener newOpenStoreListener(View itemView, String storeName) {
			return v -> {
				FragmentUtils.replaceFragmentV4((FragmentActivity) itemView.getContext(), StoreFragment.newInstance
						(storeName));
			};
		}

		private View.OnClickListener newSubscribeStoreListener(View itemView, String storeName) {
			return v -> {
				StoreUtils.subscribeStore(storeName, getStoreMeta -> {
					ShowMessage.toast(itemView.getContext(), AptoideUtils.StringU.getFormattedString(R.string
							.store_subscribed, storeName));
				}, Throwable::printStackTrace);
			};
		}
	}
}
