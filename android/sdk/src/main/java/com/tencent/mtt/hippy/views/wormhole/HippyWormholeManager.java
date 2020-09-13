package com.tencent.mtt.hippy.views.wormhole;


import android.content.Context;
import android.text.TextUtils;
import android.util.Size;
import android.view.View;
import android.view.ViewGroup;
import com.tencent.mtt.hippy.HippyEngine;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.uimanager.HippyViewEvent;
import com.tencent.mtt.hippy.uimanager.RenderManager;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.utils.PixelUtil;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;

public class HippyWormholeManager implements HippyWormholeProxy {
  public static final String WORMHOLE_TAG                   = "hippy_wormhole";

  public static final String WORMHOLE_PARAMS                = "params";
  public static final String WORMHOLE_BUSINESS_ID           = "businessId";
  public static final String WORMHOLE_TYPE                  = "type";
  public static final String WORMHOLE_CLIENT_DATA_RECEIVED  = "Wormhole.dataReceived";
  public static final String WORMHOLE_SERVER_BATCH_COMPLETE = "onServerBatchComplete";

  private static volatile HippyWormholeManager INSTANCE;
  private HippyEngine mWormholeEngine;
  private ConcurrentHashMap<String, ViewGroup> mTkdWormholeMap = new ConcurrentHashMap<String, ViewGroup>();

  private HippyWormholeManager() {

  }

  public static HippyWormholeManager getInstance() {
    if (INSTANCE == null) {
      synchronized (HippyWormholeManager.class) {
        if (INSTANCE == null) {
          INSTANCE = new HippyWormholeManager();
        }
      }
    }
    return INSTANCE;
  }

  public void setServerEngine(HippyEngine engine) {
    mWormholeEngine = engine;
  }

  private RenderNode getWormholeNode(HippyWormholeView wormhole) {
    Context context = wormhole.getContext();
    if (context instanceof HippyInstanceContext) {
      HippyEngineContext engineContext = ((HippyInstanceContext) context).getEngineContext();
      if (engineContext != null) {
        RenderManager rm = engineContext.getRenderManager();
        RenderNode node = rm.getRenderNode(wormhole.getId());
        return node;
      }
    }

    return null;
  }

  private void sendDataReceivedMessageToServer(HippyMap initProps) {
    HippyMap paramsMap = initProps.getMap(WORMHOLE_PARAMS);
    if (paramsMap != null) {
      HippyMap bundle = paramsMap.copy();
      bundle.pushInt(WORMHOLE_TYPE, 1);
      JSONArray jsonArray = new JSONArray();
      jsonArray.put(bundle);
      mWormholeEngine.sendEvent(WORMHOLE_CLIENT_DATA_RECEIVED, jsonArray);
    }
  }

  private void sendBatchCompleteMessageToClient(float width, float height, View view) {
    HippyMap layoutMeasurement = new HippyMap();
    layoutMeasurement.pushDouble("width", PixelUtil.px2dp(width));
    layoutMeasurement.pushDouble("height", PixelUtil.px2dp(height));
    HippyViewEvent event = new HippyViewEvent(WORMHOLE_SERVER_BATCH_COMPLETE);
    event.send(view, layoutMeasurement);
  }

  public void onServerBatchComplete(HippyWormholeView wormholeView) {
    if (wormholeView == null) {
      return;
    }

    RenderNode node = getWormholeNode(wormholeView);
    if (node != null) {
      String businessId = wormholeView.getBusinessId();
      ViewGroup newParent = mTkdWormholeMap.get(businessId);
      if (newParent == null) {
        return;
      }

      ViewGroup oldParent = (ViewGroup)(wormholeView.getParent());
      if (oldParent != newParent) {
        oldParent.removeView(wormholeView);
        newParent.addView(wormholeView);
      }

      float width = node.getWidth();
      float height = node.getHeight();
      sendBatchCompleteMessageToClient(width, height, newParent);
    }
  }

  public String getBusinessId(HippyMap props) {
    HippyMap paramsMap = props.getMap(WORMHOLE_PARAMS);
    if (paramsMap == null) {
      return null;
    }

    String businessId = paramsMap.getString(WORMHOLE_BUSINESS_ID);
    return businessId;
  }

  @Override
  public void createWormhole(HippyMap initProps, ViewGroup parent) {
    if (mWormholeEngine == null) {
      return;
    }

    if (initProps == null || parent == null) {
      return;
    }

    String businessId = getBusinessId(initProps);
    if (!TextUtils.isEmpty(businessId)) {
      if (!mTkdWormholeMap.contains(businessId)) {
        mTkdWormholeMap.put(businessId, parent);
      }
      sendDataReceivedMessageToServer(initProps);
    }
  }
}