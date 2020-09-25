package com.tencent.mtt.hippy.views.wormhole;

import android.content.Context;
import android.view.View;

import com.tencent.mtt.hippy.HippyRootView;
import com.tencent.mtt.hippy.annotation.HippyController;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.uimanager.ControllerManager;
import com.tencent.mtt.hippy.uimanager.HippyViewController;
import com.tencent.mtt.hippy.uimanager.HippyViewEvent;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.views.wormhole.node.TKDWormholeRenderNode;

@HippyController(name = "TKDWormhole")
public class TKDWormholeController extends HippyViewController<TKDWormholeView> {
  private HippyWormholeProxy mWormholeProxy = HippyWormholeManager.getInstance();

  @Override
  protected View createViewImpl(final Context context) {
    return new TKDWormholeView(context);
  }

  @Override
  protected View createViewImpl(final Context context, HippyMap initProps) {
    final TKDWormholeView tkdWormholeView = new TKDWormholeView(context);
    return tkdWormholeView;
  }

  @Override
  public RenderNode createRenderNode(int id, HippyMap props, String className,
          HippyRootView hippyRootView, ControllerManager controllerManager, boolean lazy) {
    TKDWormholeRenderNode node = new TKDWormholeRenderNode(id, props, className, hippyRootView, controllerManager, lazy);
    HippyWormholeManager.getInstance().onCreateTKDWormholeNode(props, node);
    return node;
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

}
