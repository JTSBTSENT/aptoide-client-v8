/*
 * Copyright (c) 2016.
 * Modified by SithEngineer on 16/08/2016.
 */

package cm.aptoide.pt.v8engine.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.dataprovider.DataProvider;
import cm.aptoide.pt.dataprovider.exception.AptoideWsV7Exception;
import cm.aptoide.pt.dataprovider.repository.IdsRepositoryImpl;
import cm.aptoide.pt.dataprovider.ws.v7.BaseRequestWithStore;
import cm.aptoide.pt.dataprovider.ws.v7.store.GetStoreMetaRequest;
import cm.aptoide.pt.interfaces.AptoideClientUUID;
import cm.aptoide.pt.model.v7.BaseV7Response;
import cm.aptoide.pt.preferences.secure.SecurePreferencesImplementation;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.utils.GenericDialogs;
import cm.aptoide.pt.utils.design.ShowMessage;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.util.StoreUtils;

/**
 * Created with IntelliJ IDEA. User: rmateus Date: 29-11-2013 Time: 15:56 To change this template
 * use File | Settings |
 * File Templates.
 */
public class PrivateStoreDialog extends DialogFragment {

  public static final String TAG = "PrivateStoreDialog";
  private final AptoideClientUUID aptoideClientUUID;
  private ProgressDialog loadingDialog;
  private String storeName;
  private String storeUser;
  private String storePassSha1;
  private boolean isInsideStore;

  public PrivateStoreDialog() {
    aptoideClientUUID = new IdsRepositoryImpl(SecurePreferencesImplementation.getInstance(),
        DataProvider.getContext());
  }

  public static PrivateStoreDialog newInstance(Fragment returnFragment, int requestCode,
      String storeName, boolean isInsideStore) {
    final PrivateStoreDialog fragment = new PrivateStoreDialog();
    Bundle args = new Bundle();

    args.putString(BundleArgs.STORE_NAME.name(), storeName);

    fragment.setArguments(args);
    fragment.setIsInsideStore(isInsideStore);
    fragment.setRetainInstance(true);
    fragment.setTargetFragment(returnFragment, requestCode);
    return fragment;
  }

  public void setIsInsideStore(boolean isInsideStore) {
    this.isInsideStore = isInsideStore;
  }

  @Override public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final Bundle args = getArguments();
    if (args != null) {
      storeName = args.getString(BundleArgs.STORE_NAME.name());
    }
  }

  @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    Context context = getActivity();
    final View rootView =
        LayoutInflater.from(context).inflate(R.layout.dialog_add_pvt_store, null, false);

    return new AlertDialog.Builder(context).setTitle(R.string.subscribe_pvt_store)
        .setView(rootView)
        .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
          storeUser =
              ((EditText) rootView.findViewById(R.id.edit_store_username)).getText().toString();
          storePassSha1 = AptoideUtils.AlgorithmU.computeSha1(
              ((EditText) rootView.findViewById(R.id.edit_store_password)).getText().toString());

          StoreUtils.subscribeStore(buildRequest(), getStoreMeta -> {
            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
            dismissLoadingDialog();
            dismiss();
          }, e -> {
            dismissLoadingDialog();
            if (e instanceof AptoideWsV7Exception) {
              BaseV7Response baseResponse = ((AptoideWsV7Exception) e).getBaseResponse();

              if (StoreUtils.PRIVATE_STORE_WRONG_CREDENTIALS.equals(
                  baseResponse.getError().getCode())) {
                storeUser = null;
                storePassSha1 = null;
                ShowMessage.asSnack(rootView, R.string.ws_error_invalid_grant);
              }
            } else {
              e.printStackTrace();
              ShowMessage.asSnack(getView(), R.string.error_occured);
              dismiss();
            }
          });
          showLoadingDialog();
        })
        .create();
  }

  @Override public void onDismiss(DialogInterface dialog) {
    if (isInsideStore) {
      getActivity().onBackPressed();
    }
    super.onDismiss(dialog);
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putString(BundleArgs.STORE_NAME.name(), storeName);
  }

  @Override public void onDestroyView() {
    Dialog dialog = getDialog();

    // Work around to the bug... : http://code.google.com/p/android/issues/detail?id=17423
    if ((dialog != null) && getRetainInstance()) dialog.setDismissMessage(null);

    super.onDestroyView();
  }

  private GetStoreMetaRequest buildRequest() {
    return GetStoreMetaRequest.of(
        new BaseRequestWithStore.StoreCredentials(storeName, storeUser, storePassSha1),
        AptoideAccountManager.getAccessToken(), aptoideClientUUID.getUniqueIdentifier());
  }

  private void dismissLoadingDialog() {
    loadingDialog.dismiss();
  }

  private void showLoadingDialog() {
    if (loadingDialog == null) {
      loadingDialog = GenericDialogs.createGenericPleaseWaitDialog(getActivity());
    }
    loadingDialog.show();
  }

  private enum BundleArgs {
    STORE_NAME,
  }
}