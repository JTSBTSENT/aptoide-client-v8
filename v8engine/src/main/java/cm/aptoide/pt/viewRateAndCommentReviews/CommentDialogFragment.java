package cm.aptoide.pt.viewRateAndCommentReviews;

import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.dataprovider.DataProvider;
import cm.aptoide.pt.dataprovider.repository.IdsRepositoryImpl;
import cm.aptoide.pt.dataprovider.util.CommentType;
import cm.aptoide.pt.dataprovider.ws.v7.PostCommentForReview;
import cm.aptoide.pt.dataprovider.ws.v7.PostCommentForTimelineArticle;
import cm.aptoide.pt.dataprovider.ws.v7.store.PostCommentForStore;
import cm.aptoide.pt.interfaces.AptoideClientUUID;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.model.v7.BaseV7Response;
import cm.aptoide.pt.preferences.secure.SecurePreferencesImplementation;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.utils.design.ShowMessage;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.fragment.CommentListFragment;
import com.jakewharton.rxbinding.view.RxView;
import com.trello.rxlifecycle.android.FragmentEvent;
import com.trello.rxlifecycle.components.RxDialogFragment;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

public class CommentDialogFragment extends RxDialogFragment {

  private static final String TAG = CommentDialogFragment.class.getName();

  private static final String APP_OR_STORE_NAME = "app_or_store_name";
  private static final String RESOURCE_ID_AS_LONG = "resource_id_as_long";
  private static final String RESOURCE_ID_AS_STRING = "resource_id_as_string";
  private static final String COMMENT_TYPE = "comment_type";
  private static final String PREVIOUS_COMMENT_ID = "previous_comment_id";
  private final AptoideClientUUID aptoideClientUUID;
  private final String onEmptyTextError;
  private String appOrStoreName;
  private long idAsLong;
  private String idAsString;
  private CommentType commentType;
  private Long previousCommentId;
  private TextInputLayout textInputLayout;
  private Button commentButton;
  private boolean reply;
  private CommentListFragment commentDialogCallbackContract;

  public CommentDialogFragment() {
    onEmptyTextError = AptoideUtils.StringU.getResString(R.string.error_MARG_107);
    aptoideClientUUID = new IdsRepositoryImpl(SecurePreferencesImplementation.getInstance(),
        DataProvider.getContext());
  }

  public static CommentDialogFragment newInstanceStoreCommentReply(long storeId,
      long previousCommentId, String storeName) {
    Bundle args = new Bundle();
    args.putString(COMMENT_TYPE, CommentType.STORE.name());
    args.putLong(RESOURCE_ID_AS_LONG, storeId);
    args.putLong(PREVIOUS_COMMENT_ID, previousCommentId);

    if (!TextUtils.isEmpty(storeName)) {
      args.putString(APP_OR_STORE_NAME, storeName);
    }

    CommentDialogFragment fragment = new CommentDialogFragment();
    fragment.setArguments(args);
    return fragment;
  }

  public static CommentDialogFragment newInstanceReview(long id, String appName) {
    Bundle args = new Bundle();
    args.putString(COMMENT_TYPE, CommentType.REVIEW.name());
    args.putLong(RESOURCE_ID_AS_LONG, id);

    if (!TextUtils.isEmpty(appName)) {
      args.putString(APP_OR_STORE_NAME, appName);
    }

    CommentDialogFragment fragment = new CommentDialogFragment();
    fragment.setArguments(args);
    return fragment;
  }

  public static CommentDialogFragment newInstanceStoreComment(long storeId, String storeName) {
    Bundle args = new Bundle();
    args.putString(COMMENT_TYPE, CommentType.STORE.name());
    args.putLong(RESOURCE_ID_AS_LONG, storeId);

    if (!TextUtils.isEmpty(storeName)) {
      args.putString(APP_OR_STORE_NAME, storeName);
    }

    CommentDialogFragment fragment = new CommentDialogFragment();
    fragment.setArguments(args);
    return fragment;
  }

  public static CommentDialogFragment newInstanceTimelineArticleComment(String timelineArticleId) {
    Bundle args = new Bundle();
    args.putString(COMMENT_TYPE, CommentType.TIMELINE.name());
    args.putString(RESOURCE_ID_AS_STRING, timelineArticleId);

    CommentDialogFragment fragment = new CommentDialogFragment();
    fragment.setArguments(args);
    return fragment;
  }

  public static CommentDialogFragment newInstanceTimelineArticleComment(String timelineArticleId,
      long previousCommentId) {
    Bundle args = new Bundle();
    args.putString(COMMENT_TYPE, CommentType.TIMELINE.name());
    args.putString(RESOURCE_ID_AS_STRING, timelineArticleId);
    args.putLong(PREVIOUS_COMMENT_ID, previousCommentId);

    CommentDialogFragment fragment = new CommentDialogFragment();
    fragment.setArguments(args);
    return fragment;
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {

    loadArguments();

    View view = inflater.inflate(R.layout.dialog_comment_on_review, container);

    TextView titleTextView = (TextView) view.findViewById(R.id.title);
    titleTextView.setVisibility(View.VISIBLE);

    switch (commentType) {
      case REVIEW:
        titleTextView.setText(getString(R.string.comment));
        break;
      case TIMELINE:
        titleTextView.setText(getString(R.string.comment));
        break;
      case STORE:
        titleTextView.setText(String.format(getString(R.string.comment_on_store),
            TextUtils.isEmpty(appOrStoreName) ? getString(R.string.word_this) : appOrStoreName));
        break;
    }

    Button cancelButton = (Button) view.findViewById(R.id.cancel_button);
    cancelButton.setOnClickListener(a -> CommentDialogFragment.this.dismiss());

    textInputLayout = (TextInputLayout) view.findViewById(R.id.input_layout_title);
    commentButton = (Button) view.findViewById(R.id.comment_button);

    setupLogic();

    return view;
  }

  private void loadArguments() {
    Bundle args = getArguments();
    this.appOrStoreName = args.getString(APP_OR_STORE_NAME, "");
    this.commentType = CommentType.valueOf(args.getString(COMMENT_TYPE));
    this.idAsString = args.getString(RESOURCE_ID_AS_STRING);
    this.idAsLong = args.getLong(RESOURCE_ID_AS_LONG);

    this.reply = args.containsKey(PREVIOUS_COMMENT_ID);
    if (this.reply) {
      this.previousCommentId = args.getLong(PREVIOUS_COMMENT_ID);
    }
  }

  //
  // logic
  //

  private void setupLogic() {

    textInputLayout.getEditText().addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
      }

      @Override
      public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
        if (charSequence.length() > 0) {
          disableError();
        }
      }

      @Override public void afterTextChanged(Editable editable) {
      }
    });

    RxView.clicks(commentButton)
        .flatMap(a -> Observable.just(getText()))
        .filter(inputText -> {
          if (TextUtils.isEmpty(inputText)) {
            enableError(onEmptyTextError);
            return false;
          }
          disableError();
          return true;
        })
        .flatMap(inputText -> submitComment(inputText, idAsLong, previousCommentId,
            idAsString).observeOn(AndroidSchedulers.mainThread()).doOnError(e -> {
          CrashReport.getInstance().log(e);
          ShowMessage.asSnack(this, R.string.error_occured);
        }).retry().compose(bindUntilEvent(FragmentEvent.DESTROY_VIEW)))
        .subscribe(resp -> {
          if (resp.isOk()) {
            this.dismiss();
            commentDialogCallbackContract.okSelected(resp, idAsLong, previousCommentId, idAsString);
          } else {
            ShowMessage.asSnack(this, R.string.error_occured);
          }
        }, throwable -> CrashReport.getInstance().log(throwable));
  }

  private void disableError() {
    textInputLayout.setErrorEnabled(false);
  }

  private String getText() {
    if (textInputLayout != null) {
      return textInputLayout.getEditText().getEditableText().toString();
    }
    return null;
  }

  private void enableError(String error) {
    textInputLayout.setError(error);
  }

  private Observable<? extends BaseV7Response> submitComment(String inputText, long idAsLong,
      Long previousCommentId, String idAsString) {
    switch (commentType) {
      case REVIEW:
        // new comment on a review
        return PostCommentForReview.of(idAsLong, inputText, AptoideAccountManager.getAccessToken(),
            aptoideClientUUID.getUniqueIdentifier()).observe();

      case STORE:
        // check if this is a new comment on a store or a reply to a previous one
        if (previousCommentId == null) {
          return PostCommentForStore.of(idAsLong, inputText, AptoideAccountManager.getAccessToken(),
              aptoideClientUUID.getUniqueIdentifier()).observe();
        }

        return PostCommentForStore.of(idAsLong, previousCommentId, inputText,
            AptoideAccountManager.getAccessToken(), aptoideClientUUID.getUniqueIdentifier())
            .observe();

      case TIMELINE:
        // check if this is a new comment on a article or a reply to a previous one
        if (previousCommentId == null) {
          return PostCommentForTimelineArticle.of(idAsString, inputText,
              AptoideAccountManager.getAccessToken(), aptoideClientUUID.getUniqueIdentifier())
              .observe();
        }

        return PostCommentForTimelineArticle.of(idAsString, previousCommentId, inputText,
            AptoideAccountManager.getAccessToken(), aptoideClientUUID.getUniqueIdentifier())
            .observe();
    }
    // default case
    Logger.e(this.getTag(), "Unable to create reply due to missing comment type");
    return Observable.empty();
  }

  public void setCommentDialogCallbackContract(CommentListFragment commentDialogCallbackContract) {
    this.commentDialogCallbackContract = commentDialogCallbackContract;
  }
}