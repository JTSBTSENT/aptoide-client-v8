/*
 * Copyright (c) 2016.
 * Modified on 05/07/2016.
 */

package cm.aptoide.pt.util;

import android.support.design.widget.AppBarLayout;

/**
 * Created on 05/07/16.
 */
public abstract class AppBarStateChangeListener implements AppBarLayout.OnOffsetChangedListener {

  private State mCurrentState = State.IDLE;

  @Override public final void onOffsetChanged(AppBarLayout appBarLayout, int i) {
    if (i == 0) {
      if (mCurrentState != State.EXPANDED) {
        onStateChanged(appBarLayout, State.EXPANDED);
      }
      mCurrentState = State.EXPANDED;
    } else if (Math.abs(i) - appBarLayout.getTotalScrollRange() == 0) {
      if (mCurrentState != State.COLLAPSED_COMPLETELY) {
        onStateChanged(appBarLayout, State.COLLAPSED_COMPLETELY);
      }
      mCurrentState = State.COLLAPSED_COMPLETELY;
    } else if (Math.abs(i) >= appBarLayout.getTotalScrollRange()) {
      if (mCurrentState != State.COLLAPSED) {
        onStateChanged(appBarLayout, State.COLLAPSED);
      }
      mCurrentState = State.COLLAPSED;
    } else {
      if (mCurrentState != State.IDLE) {
        onStateChanged(appBarLayout, State.IDLE);
      }
      mCurrentState = State.IDLE;
    }
  }

  public abstract void onStateChanged(AppBarLayout appBarLayout, State state);

  public enum State {
    EXPANDED, COLLAPSED, IDLE, COLLAPSED_COMPLETELY
  }
}
