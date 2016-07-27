/*
 * Copyright (c) 2016.
 * Modified by SithEngineer on 25/07/2016.
 */

package cm.aptoide.pt.v8engine.view.recycler.widget.implementations.appView;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.actions.PermissionManager;
import cm.aptoide.pt.actions.PermissionRequest;
import cm.aptoide.pt.database.Database;
import cm.aptoide.pt.database.realm.Download;
import cm.aptoide.pt.database.realm.FileToDownload;
import cm.aptoide.pt.database.realm.Installed;
import cm.aptoide.pt.database.realm.Rollback;
import cm.aptoide.pt.dataprovider.util.DataproviderUtils;
import cm.aptoide.pt.downloadmanager.AptoideDownloadManager;
import cm.aptoide.pt.downloadmanager.DownloadServiceHelper;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.model.v7.GetApp;
import cm.aptoide.pt.model.v7.GetAppMeta;
import cm.aptoide.pt.model.v7.listapp.ListAppVersions;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.utils.ShowMessage;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.fragment.implementations.AppViewFragment;
import cm.aptoide.pt.v8engine.fragment.implementations.OtherVersionsFragment;
import cm.aptoide.pt.v8engine.interfaces.AppMenuOptions;
import cm.aptoide.pt.v8engine.interfaces.FragmentShower;
import cm.aptoide.pt.v8engine.util.DownloadFactory;
import cm.aptoide.pt.v8engine.util.FragmentUtils;
import cm.aptoide.pt.v8engine.util.RollbackUtils;
import cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.appView.AppViewInstallDisplayable;
import cm.aptoide.pt.v8engine.view.recycler.widget.Displayables;
import cm.aptoide.pt.v8engine.view.recycler.widget.Widget;
import io.realm.Realm;
import lombok.Cleanup;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;

/**
 * Created by sithengineer on 06/05/16.
 */
@Displayables({AppViewInstallDisplayable.class})
public class AppViewInstallWidget extends Widget<AppViewInstallDisplayable> {

	private RelativeLayout downloadProgressLayout;
	private RelativeLayout installAndLatestVersionLayout;

	//
	// downloading views
	//
	private CheckBox shareInTimeline;
	private ProgressBar downloadProgress;
	private TextView textProgress;
	private ImageView actionPauseResume;
	private ImageView actionCancel;

	// get app, upgrade and downgrade button
	private Button actionButton;

	// app info
	private TextView versionName;
	private TextView otherVersions;
	private String cpdUrl;
	private String cpiUrl;

	public AppViewInstallWidget(View itemView) {
		super(itemView);
	}

	@Override
	protected void assignViews(View itemView) {
		downloadProgressLayout = (RelativeLayout) itemView.findViewById(R.id.download_progress_layout);
		installAndLatestVersionLayout = (RelativeLayout) itemView.findViewById(R.id.install_and_latest_version_layout);
		shareInTimeline = (CheckBox) itemView.findViewById(R.id.share_in_timeline);
		downloadProgress = (ProgressBar) itemView.findViewById(R.id.download_progress);
		textProgress = (TextView) itemView.findViewById(R.id.text_progress);
		actionPauseResume = (ImageView) itemView.findViewById(R.id.ic_action_pause_resume);
		actionCancel = (ImageView) itemView.findViewById(R.id.ic_action_cancel);
		actionButton = (Button) itemView.findViewById(R.id.action_btn);
		versionName = (TextView) itemView.findViewById(R.id.store_version_name);
		otherVersions = (TextView) itemView.findViewById(R.id.other_versions);
	}

	@Override
	public void bindView(AppViewInstallDisplayable displayable) {

		cpdUrl = displayable.getCpdUrl();
		cpiUrl = displayable.getCpdUrl();
		GetApp getApp = displayable.getPojo();
		GetAppMeta.App app = getApp.getNodes().getMeta().getData();

		versionName.setText(app.getFile().getVername());
		otherVersions.setOnClickListener(v -> {
			OtherVersionsFragment fragment = OtherVersionsFragment.newInstance(app.getName(), app.getIcon(), app.getPackageName());
			((FragmentShower) getContext()).pushFragmentV4(fragment);
		});

		@Cleanup
		Realm realm = Database.get();
		String packageName = app.getPackageName();
		Installed installed = Database.InstalledQ.get(packageName, realm);

		final FragmentShower fragmentShower = (FragmentShower) getContext();

		//check if the app is installed
		if (installed == null) {
			// app not installed
			setupInstallButton(app, displayable);
			((AppMenuOptions) fragmentShower.getLastV4()).setUnInstallMenuOptionVisible(null);
		} else {
			// app installed

			// setup un-install button in menu
			((AppMenuOptions) fragmentShower.getLastV4()).setUnInstallMenuOptionVisible(new Listeners().newUninstallListener(itemView, installed
					.getPackageName(), displayable));

			// is it an upgrade, downgrade or open app?
			setupUpgradeDowngradeOpenActions(getApp, installed, displayable);
		}
	}

	@Override
	public void onViewAttached() {

	}

	@Override
	public void onViewDetached() {
	}

	private void setupUpgradeDowngradeOpenActions(GetApp getApp, Installed installed, AppViewInstallDisplayable displayable) {

		GetAppMeta.App app = getApp.getNodes().getMeta().getData();

		if (!isLatestAvailable(app, getApp.getNodes().getVersions()) || app.getFile().getVercode() > installed.getVersionCode()) {
			actionButton.setText(R.string.update);
			actionButton.setOnClickListener(new Listeners().newUpdateListener(app, displayable));
		} else if (app.getFile().getVercode() < installed.getVersionCode()) {
			actionButton.setText(R.string.downgrade);
			actionButton.setOnClickListener(new Listeners().newDowngradeListener(app, displayable));
		} else {
			actionButton.setText(R.string.open);
			actionButton.setOnClickListener(new Listeners().newOpenAppListener(app.getPackageName()));
		}
	}

	public void setupInstallButton(GetAppMeta.App app, AppViewInstallDisplayable displayable) {
		//check if the app is payed
		if (displayable.isPaidApp()) {
			// TODO replace that for placeholders in resources as soon as we are able to add new strings for translation.
			actionButton.setText(getContext().getString(R.string.buy) + " (" + app.getPay().getSymbol() + " " + app.getPay().getPrice() + ")");
			actionButton.setOnClickListener(new Listeners().newBuyListener());
		} else {
			actionButton.setText(R.string.install);
			actionButton.setOnClickListener(new Listeners().newInstallListener(app, displayable));
		}
	}

	private boolean isLatestAvailable(GetAppMeta.App app, @Nullable ListAppVersions appVersions) {
		return appVersions != null && !appVersions.getList().isEmpty() &&
				(app.getFile().getVercode() < appVersions.getList().get(0).getFile().getVercode());
	}

	//private static class Listeners {
	private class Listeners {

		private final String TAG = Listeners.class.getSimpleName();

		//		private final WeakReference<AppViewInstallWidget> widgetWeakRef;
		//
		//		public Listeners(AppViewInstallWidget widget) {
		//			widgetWeakRef = new WeakReference<>(widget);
		//		}

		private View.OnClickListener newBuyListener() {
			return v -> {
				if (!AptoideAccountManager.isLoggedIn()) {
					AptoideAccountManager.openAccountManager(getContext());
				}
 			};
		}

		private View.OnClickListener newInstallListener(GetAppMeta.App app, AppViewInstallDisplayable displayable) {
			return v -> innerInstallAction(app, R.string.installing_msg, v, displayable);
		}

		private void innerInstallAction(GetAppMeta.App app, final int msgId, View v, AppViewInstallDisplayable displayable) {
			String packageName = app.getPackageName();
			AptoideUtils.ThreadU.runOnIoThread(() -> RollbackUtils.addInstallAction(packageName));
			if (cpdUrl != null) {
				DataproviderUtils.AdNetworksUtils.knockCpd(cpdUrl);
			}

			@Cleanup
			Realm realm = Database.get();
			Rollback rollback = Database.RollbackQ.get(packageName, Rollback.Action.INSTALL, realm);
			if (rollback != null) {
				rollback.setCpiUrl(cpiUrl);
				Database.save(rollback, realm);
			}

			ContextWrapper ctx = (ContextWrapper) v.getContext();
			PermissionRequest permissionRequest = ((PermissionRequest) ctx.getBaseContext());

			permissionRequest.requestAccessToExternalFileSystem(() -> {

				ShowMessage.asSnack(v, msgId);

				DownloadFactory factory = new DownloadFactory();
				Download appDownload = factory.create(app);
				DownloadServiceHelper downloadServiceHelper = new DownloadServiceHelper(AptoideDownloadManager.getInstance(), new PermissionManager());

				actionPauseResume.setOnClickListener(view -> {
					downloadServiceHelper.pauseDownload(app.getId());

					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
						actionPauseResume.setBackgroundColor(getContext().getColor(R.color.default_color));
					} else {
						actionPauseResume.setBackgroundColor(getContext().getResources().getColor(R.color.default_color));
					}
				});

				actionCancel.setOnClickListener(view -> {
					//download.cancel();
					//downloadServiceHelper.cancelDownload(app.getId());
					downloadServiceHelper.pauseDownload(app.getId());

					installAndLatestVersionLayout.setVisibility(View.VISIBLE);
					downloadProgressLayout.setVisibility(View.GONE);
				});

				installAndLatestVersionLayout.setVisibility(View.GONE);
				downloadProgressLayout.setVisibility(View.VISIBLE);
				actionPauseResume.setImageResource(R.drawable.ic_pause);

				downloadServiceHelper.startDownload(getContext(), appDownload).subscribe(download -> {

					// TODO: 19/07/16 sithengineer logic to show / hide pause / resume download and show download progress

					switch (download.getOverallDownloadStatus()) {

						case Download.PAUSED: {
							actionPauseResume.setOnClickListener(view -> {

								downloadServiceHelper.startDownload(getContext(), download);

								//actionPauseResume.setImageResource(R.drawable.ic_); // missing the changing of the drawable
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
									actionPauseResume.setBackgroundColor(getContext().getColor(android.R.color.transparent));
								} else {
									actionPauseResume.setBackgroundColor(getContext().getResources().getColor(android.R.color.transparent));
								}
							});
							break;
						}

						case Download.PROGRESS: {
							downloadProgress.setProgress(download.getOverallProgress());
							//textProgress.setText(download.getOverallProgress() + "% - " + AptoideUtils.StringU.formatBits((long) download.getSpeed()) +
							// "/s");
							textProgress.setText(download.getOverallProgress() + "%");
							break;
						}

						case Download.ERROR: {
							installAndLatestVersionLayout.setVisibility(View.VISIBLE);
							downloadProgressLayout.setVisibility(View.GONE);
							break;
						}

						case Download.COMPLETED: {
							installAndLatestVersionLayout.setVisibility(View.VISIBLE);
							downloadProgressLayout.setVisibility(View.GONE);
							displayable.install(v.getContext())
									.observeOn(AndroidSchedulers.mainThread())
									.subscribe(success -> {
										if (actionButton.getVisibility() == View.VISIBLE) {
											actionButton.setText(R.string.open);
											// FIXME: 20/07/16 sithengineer refactor this ugly code
											((AppMenuOptions) ((FragmentShower) getContext()).getLastV4()).setUnInstallMenuOptionVisible(() -> {
												new Listeners().newUninstallListener(itemView, app.getPackageName(), displayable).call();
											});
										}
									});
							break;
						}
					}
				}, Throwable::printStackTrace);
			}, () -> {
				ShowMessage.asSnack(v, R.string.needs_permission_to_fs);
			});
		}

		private View.OnClickListener newUpdateListener(final GetAppMeta.App app, AppViewInstallDisplayable displayable) {
			return v -> {
				AptoideUtils.ThreadU.runOnIoThread(() -> RollbackUtils.addUpdateAction(app.getPackageName()));
				innerInstallAction(app, R.string.updating_msg, v, displayable);
			};
		}

		private View.OnClickListener newDowngradeListener(final GetAppMeta.App app, AppViewInstallDisplayable displayable) {
			// FIXME: 15/07/16 sithengineer show notification to user saying it will lose all his data

			return view -> {

				AptoideUtils.ThreadU.runOnIoThread(() -> RollbackUtils.addUpdateAction(app.getPackageName()));
				final Context context = view.getContext();
				ContextWrapper contextWrapper = (ContextWrapper) context;
				PermissionRequest permissionRequest = ((PermissionRequest) contextWrapper.getBaseContext());

				permissionRequest.requestAccessToExternalFileSystem(() -> {
					ShowMessage.asSnack(view, R.string.downgrading_msg);

					DownloadFactory factory = new DownloadFactory();
					Download appDownload = factory.create(app);
					DownloadServiceHelper downloadServiceHelper = new DownloadServiceHelper(AptoideDownloadManager.getInstance(), new PermissionManager());
					downloadServiceHelper.startDownload(getContext(), appDownload).subscribe(download -> {
						if (download.getOverallDownloadStatus() == Download.COMPLETED) {
							final String packageName = app.getPackageName();
							final FileToDownload downloadedFile = download.getFilesToDownload().get(0);
							displayable.downgrade(getContext()).subscribe();
						}
					});
				}, () -> {
					Logger.e(TAG, "unable to access to external FS");
				});
			};
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

		private Action0 newUninstallListener(View itemView, String packageName, AppViewInstallDisplayable displayable) {
			return () -> {
				AptoideUtils.ThreadU.runOnIoThread(() -> RollbackUtils.addUninstallAction(packageName));
				displayable.uninstall(getContext()).subscribe();
			};
		}
	}
}
