package com.tencent.mtt.hippy.views.wormhole;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.tencent.mtt.hippy.annotation.HippyController;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.dom.node.StyleNode;
import com.tencent.mtt.hippy.uimanager.HippyViewController;
import com.tencent.mtt.hippy.uimanager.HippyViewEvent;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.views.wormhole.node.TKDStyleNode;

@HippyController(name = "TKDWormhole")
public class TKDWormholeController extends HippyViewController<TKDWormholeView> {

  private static final String TAG = "TKDWormholeController";

  @Override
  protected View createViewImpl(final Context context) {
    return new TKDWormholeView(context);
  }

  @Override
  protected View createViewImpl(final Context context, HippyMap initProps) {
    final TKDWormholeView tkdWormholeView = new TKDWormholeView(context);
    String wormholeId = HippyWormholeManager.getInstance().getWormholeIdFromProps(initProps);
    if(!TextUtils.isEmpty(wormholeId)) {
      tkdWormholeView.setWormholeDataProps(initProps);
      boolean hasView = HippyWormholeManager.getInstance().onCreateTKDWormholeView(tkdWormholeView, wormholeId);
      if (!hasView) {
        addNVView(wormholeId, tkdWormholeView);
      }
    }
    return tkdWormholeView;
  }

  private void addNVView(String wormholeId, ViewGroup parent) {
    View view = NativeVueManager.getInstance().getNVView(wormholeId);
    if (view != null && view.getParent() == null) {
      parent.addView(view);
      NativeVueManager.getInstance().markAddNVView(wormholeId);
      LogUtils.d(TAG, "add nv view, wormhole id: " + wormholeId);
    }
  }

  @Override
  public void dispatchFunction(TKDWormholeView view, String functionName, HippyArray dataArray) {
    super.dispatchFunction(view, functionName, dataArray);
    switch (functionName) {
      case HippyWormholeManager.FUNCTION_SENDEVENT_TO_WORMHOLEVIEW: {
        if (view != null && view.getChildCount() > 0) {
          View child = view.getChildAt(0);
          if (child != null && child instanceof HippyWormholeView) {
            //前端约定一个tkdWormhole下面只能有一个wormhole
            HippyViewEvent event = new HippyViewEvent(HippyWormholeManager.FUNCTION_ONCUSTOMEVENT);
            event.send(child, dataArray);
          }
        }
        break;
      }
    }
  }

  @Override
  protected StyleNode createNode(boolean virtual) {
    HippyWormholeManager manager = HippyWormholeManager.getInstance();
    String wormholeId = manager.generateWormholeId();
    return new TKDStyleNode(virtual, manager.getEngineContext(), manager.getHippyRootView(), wormholeId);
  }

  @Override
  public void onViewDestroy(TKDWormholeView tkdWormHoleView) {
    HippyWormholeManager.getInstance().onTKDWormholeViewDestroy(tkdWormHoleView);
  }

}
