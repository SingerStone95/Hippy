package com.tencent.mtt.hippy.example;

import android.content.Context;

import com.tencent.mtt.hippy.HippyAPIProvider;
import com.tencent.mtt.hippy.HippyEngine;
import com.tencent.mtt.hippy.HippyRootView;
import com.tencent.mtt.hippy.adapter.exception.HippyExceptionHandlerAdapter;
import com.tencent.mtt.hippy.common.HippyJsException;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.example.adapter.MyImageLoader;
import com.tencent.mtt.hippy.example.nv.NativeVueAdapter;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.views.wormhole.HippyWormholeManager;
import com.tencent.mtt.hippy.views.wormhole.NativeVueManager;
import com.tencent.mtt.hippy.views.wormhole.event.HippyEventObserverAdapter;
import com.tencent.nativevue.NativeVueEngine;
import com.tencent.nativevue.NativeVueLogAdapter;
import com.tencent.nativevue.NativeVuePreconditionAdapter;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.tencent.mtt.hippy.views.wormhole.HippyWormholeManager.WORMHOLE_TAG;

public class HippyWormholeEngine
{

  private static final String TAG = "HippyWormholeEngine";
	private HippyEngine mHippyEngine;
	private HippyRootView mHippyRootView;

	public HippyWormholeEngine() {}

	public void init(final Context context) {
		// 1/3. 初始化hippy引擎
		{
			HippyEngine.EngineInitParams initParams = new HippyEngine.EngineInitParams();
			// 必须：宿主（Hippy的使用者）的Context
			// 若存在多个Activity加载多个业务jsbundle的情况，则这里初始化引擎时建议使用Application的Context
			initParams.context = context;
			// 必须：图片加载器
			initParams.imageLoader = new MyImageLoader();

			// 可选：是否设置为debug模式，默认为false。调试模式下，所有jsbundle都是从debug server上下载
			initParams.debugMode = true;
			// 可选：是否打印引擎的完整的log。默认为false
			initParams.enableLog = true;
			// 可选：debugMode = false 时必须设置coreJSAssetsPath或coreJSFilePath（debugMode = true时，所有jsbundle都是从debug server上下载）
			initParams.coreJSAssetsPath = "vendor.android.js";
			//虫洞引擎注册消息分发器
			initParams.eventObserverAdapter = new HippyEventObserverAdapter() {
				@Override
				public void onClientMessageReceived(HippyMap data) {
					//虫洞自己收到了这个数据之后需要广播给业务方
					HippyWormholeManager.getInstance().sendMessageToAllClient(data);
				}
			};
			// 可选：异常处理器
			initParams.exceptionHandler = new HippyExceptionHandlerAdapter() {
				// JavaScript执行异常
				@Override
				public void handleJsException(HippyJsException exception) {
					LogUtils.e("hippy", exception.getMessage() + exception.getStack());
				}

				// Native代码执行异常：包括sdk和业务定制代码
				@Override
				public void handleNativeException(Exception exception, boolean haveCaught) {
					LogUtils.e("hippy", exception.getMessage());
				}

				// JavaScript代码Trace，业务层一般不需要
				@Override
				public void handleBackgroundTracing(String details) {
					LogUtils.e("hippy", details);
				}
			};
			List<HippyAPIProvider> providers = new ArrayList<>();
			providers.add(new MyAPIProvider());
			// 可选：自定义的，用来提供Native modules、JavaScript modules、View controllers的管理器。1个或多个
			initParams.providers = providers;

			initParams.hippyNativeVueAdapter = new NativeVueAdapter();

			// 根据EngineInitParams创建引擎实例
			mHippyEngine = HippyEngine.create(initParams);
			// 异步初始化Hippy引擎
			mHippyEngine.initEngine(new HippyEngine.EngineListener() {
				// Hippy引擎初始化完成
				/**
				 * @param  statusCode
				 *         status code from initializing procedure
				 * @param  msg
				 *         Message from initializing procedure
				 */
				@Override
				public void onInitialized(int statusCode, String msg) {
					if (statusCode != 0)
						LogUtils.e("MyActivity", "hippy engine init failed code:" + statusCode + ", msg=" + msg);
					// else
					{
						// 2/3. 加载hippy前端模块

						HippyEngine.ModuleLoadParams loadParams = new HippyEngine.ModuleLoadParams();
						// 必须：该Hippy模块将要挂在的Activity or Dialog的context
						loadParams.context = context;
						/*
						  必须：指定要加载的Hippy模块里的组件（component）。componentName对应的是js文件中的"appName"，比如：
						  var hippy = new Hippy({
						      appName: "Demo",
						      entryPage: App
						  });
						  */
						loadParams.componentName = "AdsTemplates";
						/*
						  可选：二选一设置。自己开发的业务模块的jsbundle的assets路径（assets路径和文件路径二选一，优先使用assets路径）
						  debugMode = false 时必须设置jsAssetsPath或jsFilePath（debugMode = true时，所有jsbundle都是从debug server上下载）
						 */
						loadParams.jsAssetsPath = "index.android.js";
						/*
						  可选：二选一设置。自己开发的业务模块的jsbundle的文件路径（assets路径和文件路径二选一，优先使用assets路径）
						  debugMode = false 时必须设置jsAssetsPath或jsFilePath（debugMode = true时，所有jsbundle都是从debug server上下载）
						 */
						loadParams.jsFilePath = null;
						// 可选：发送给Hippy前端模块的参数
						loadParams.jsParams = new HippyMap();
						loadParams.jsParams.pushString("msgFromNative", "Hi js developer, I come from native code!");
						// 加载Hippy前端模块
						mHippyRootView = mHippyEngine.loadModule(loadParams, new HippyEngine.ModuleListener() {
							public void onInitialized(int statusCode, String msg, HippyRootView hippyRootView) {
								if (statusCode == HippyEngine.STATUS_OK) {
								  initNV(mHippyEngine);
									HippyWormholeManager.getInstance().setServerEngine(mHippyEngine, hippyRootView);
								} else {
									LogUtils.e(WORMHOLE_TAG, "Hippy: init worm engine failed statusCode:" + statusCode + ",msg:" + msg);
								}
							}

							public boolean onJsException(HippyJsException exception) {
								LogUtils.e(WORMHOLE_TAG, "Hippy: loadModule onJsException:" + exception);
								return true;
							}
						});
					}
				}
			});
		}
	}

	private void initNV(HippyEngine engine) {
    NativeVueEngine.Builder builder = new NativeVueEngine.Builder();

    JSONObject config = NativeVueManager.getInstance().getNVConfig(engine);
    Iterator<String> iterator = config.keys();
    while (iterator.hasNext()) {
      String key = iterator.next();
      builder.env(key, config.opt(key));
    }

    builder.preconditionAdapter(new NativeVuePreconditionAdapter() {
      @Override
      public void loadNativeVueSo(LoadSoResult loadSoResult) {
        System.loadLibrary("mtt_shared");
        System.loadLibrary("nativevue");
        loadSoResult.onLoadSuccess();
      }
    });
    builder.logAdapter(new NativeVueLogAdapter() {
      @Override
      public void logD(String s) {
        LogUtils.d(TAG, s);
      }

      @Override
      public void logE(String s) {
        LogUtils.d(TAG, s);
      }

      @Override
      public void onNativeVueError(String s) {
        LogUtils.d(TAG, s);
      }
    });
    NativeVueEngine.getInstance().init(builder);
    NativeVueManager.getInstance().parseTemplates(template); //TODO：挪动到sdk里面
  }

	String template = "{\n" +
    "    \"1\":\"{\\n  \\\"data\\\" : \\\"{\\\\n\\\\n\\\\n      }\\\",\\n  \\\"initData\\\" : \\\"{\\\\n     }\\\",\\n  \\\"preData\\\" : \\\"{\\\\n\\\\n    }\\\",\\n  \\\"template\\\" : \\\"{\\\\\\\"type\\\\\\\":\\\\\\\"Wormhole\\\\\\\",\\\\\\\"style\\\\\\\":{\\\\\\\"height\\\\\\\":\\\\\\\"118\\\\\\\",\\\\\\\"position\\\\\\\":\\\\\\\"absolute\\\\\\\",\\\\\\\"left\\\\\\\":\\\\\\\"0\\\\\\\",\\\\\\\"right\\\\\\\":\\\\\\\"0\\\\\\\"},\\\\\\\"styleBind\\\\\\\":{\\\\\\\"width\\\\\\\":\\\\\\\"Dimensions.window.width-12 * 2 \\\\\\\"},\\\\\\\"children\\\\\\\":[{\\\\\\\"type\\\\\\\":\\\\\\\"Text\\\\\\\",\\\\\\\"attr\\\\\\\":{\\\\\\\"text\\\\\\\":\\\\\\\"广告\\\\\\\"},\\\\\\\"style\\\\\\\":{\\\\\\\"position\\\\\\\":\\\\\\\"absolute\\\\\\\",\\\\\\\"fontSize\\\\\\\":\\\\\\\"12\\\\\\\",\\\\\\\"lineHeight\\\\\\\":\\\\\\\"16\\\\\\\",\\\\\\\"top\\\\\\\":\\\\\\\"2\\\\\\\",\\\\\\\"right\\\\\\\":\\\\\\\"2\\\\\\\",\\\\\\\"color\\\\\\\":\\\\\\\"#6b6c70\\\\\\\",\\\\\\\"backgroundColor\\\\\\\":\\\\\\\"#38cfd1d4\\\\\\\"}},{\\\\\\\"type\\\\\\\":\\\\\\\"Text\\\\\\\",\\\\\\\"attrBind\\\\\\\":{\\\\\\\"text\\\\\\\":\\\\\\\"'Foo Card! #'+data.title\\\\\\\"},\\\\\\\"style\\\\\\\":{\\\\\\\"height\\\\\\\":\\\\\\\"30\\\\\\\",\\\\\\\"marginHorizontal\\\\\\\":\\\\\\\"12\\\\\\\",\\\\\\\"marginTop\\\\\\\":\\\\\\\"12\\\\\\\",\\\\\\\"color\\\\\\\":\\\\\\\"rgb(228,61,36)\\\\\\\",\\\\\\\"fontSize\\\\\\\":\\\\\\\"16\\\\\\\",\\\\\\\"fontWeight\\\\\\\":\\\\\\\"bold\\\\\\\"}},{\\\\\\\"type\\\\\\\":\\\\\\\"Image\\\\\\\",\\\\\\\"attrBind\\\\\\\":{\\\\\\\"source\\\\\\\":\\\\\\\"data.coverUrl\\\\\\\"},\\\\\\\"style\\\\\\\":{\\\\\\\"backgroundColor\\\\\\\":\\\\\\\"#d3d3d3\\\\\\\",\\\\\\\"height\\\\\\\":\\\\\\\"70\\\\\\\",\\\\\\\"width\\\\\\\":\\\\\\\"117\\\\\\\",\\\\\\\"marginLeft\\\\\\\":\\\\\\\"12\\\\\\\",\\\\\\\"marginBottom\\\\\\\":\\\\\\\"6\\\\\\\"}}]}\\\"\\n}\",\n" +
    "    \"2\":\"{\\n  \\\"data\\\" : \\\"{\\\\n\\\\n\\\\n      }\\\",\\n  \\\"initData\\\" : \\\"{\\\\n     }\\\",\\n  \\\"preData\\\" : \\\"{\\\\n\\\\n    }\\\",\\n  \\\"template\\\" : \\\"{\\\\\\\"type\\\\\\\":\\\\\\\"Wormhole\\\\\\\",\\\\\\\"style\\\\\\\":{\\\\\\\"height\\\\\\\":\\\\\\\"118\\\\\\\",\\\\\\\"position\\\\\\\":\\\\\\\"absolute\\\\\\\",\\\\\\\"left\\\\\\\":\\\\\\\"0\\\\\\\",\\\\\\\"right\\\\\\\":\\\\\\\"0\\\\\\\"},\\\\\\\"styleBind\\\\\\\":{\\\\\\\"width\\\\\\\":\\\\\\\"Dimensions.window.width-12 * 2 \\\\\\\"},\\\\\\\"children\\\\\\\":[{\\\\\\\"type\\\\\\\":\\\\\\\"Text\\\\\\\",\\\\\\\"attr\\\\\\\":{\\\\\\\"text\\\\\\\":\\\\\\\"广告\\\\\\\"},\\\\\\\"style\\\\\\\":{\\\\\\\"position\\\\\\\":\\\\\\\"absolute\\\\\\\",\\\\\\\"fontSize\\\\\\\":\\\\\\\"12\\\\\\\",\\\\\\\"lineHeight\\\\\\\":\\\\\\\"16\\\\\\\",\\\\\\\"top\\\\\\\":\\\\\\\"2\\\\\\\",\\\\\\\"right\\\\\\\":\\\\\\\"2\\\\\\\",\\\\\\\"color\\\\\\\":\\\\\\\"#6b6c70\\\\\\\",\\\\\\\"backgroundColor\\\\\\\":\\\\\\\"#38cfd1d4\\\\\\\"}},{\\\\\\\"type\\\\\\\":\\\\\\\"Text\\\\\\\",\\\\\\\"attrBind\\\\\\\":{\\\\\\\"text\\\\\\\":\\\\\\\"'Bar Card! #'+data.title\\\\\\\"},\\\\\\\"style\\\\\\\":{\\\\\\\"height\\\\\\\":\\\\\\\"30\\\\\\\",\\\\\\\"marginHorizontal\\\\\\\":\\\\\\\"12\\\\\\\",\\\\\\\"marginTop\\\\\\\":\\\\\\\"12\\\\\\\",\\\\\\\"color\\\\\\\":\\\\\\\"rgb(228,61,36)\\\\\\\",\\\\\\\"fontSize\\\\\\\":\\\\\\\"16\\\\\\\",\\\\\\\"fontWeight\\\\\\\":\\\\\\\"bold\\\\\\\"}},{\\\\\\\"type\\\\\\\":\\\\\\\"Image\\\\\\\",\\\\\\\"attrBind\\\\\\\":{\\\\\\\"source\\\\\\\":\\\\\\\"data.coverUrl\\\\\\\"},\\\\\\\"style\\\\\\\":{\\\\\\\"backgroundColor\\\\\\\":\\\\\\\"#d3d3d3\\\\\\\",\\\\\\\"height\\\\\\\":\\\\\\\"70\\\\\\\",\\\\\\\"width\\\\\\\":\\\\\\\"117\\\\\\\",\\\\\\\"marginLeft\\\\\\\":\\\\\\\"12\\\\\\\",\\\\\\\"marginBottom\\\\\\\":\\\\\\\"6\\\\\\\"}}]}\\\"\\n}\",\n" +
    "    \"3\":\"{\\n  \\\"data\\\" : \\\"{\\\\n\\\\n\\\\n      }\\\",\\n  \\\"initData\\\" : \\\"{\\\\n     }\\\",\\n  \\\"preData\\\" : \\\"{\\\\n\\\\n    }\\\",\\n  \\\"template\\\" : \\\"{\\\\\\\"type\\\\\\\":\\\\\\\"Wormhole\\\\\\\",\\\\\\\"style\\\\\\\":{\\\\\\\"height\\\\\\\":\\\\\\\"118\\\\\\\",\\\\\\\"position\\\\\\\":\\\\\\\"absolute\\\\\\\",\\\\\\\"left\\\\\\\":\\\\\\\"0\\\\\\\",\\\\\\\"right\\\\\\\":\\\\\\\"0\\\\\\\"},\\\\\\\"styleBind\\\\\\\":{\\\\\\\"width\\\\\\\":\\\\\\\"Dimensions.window.width-12 * 2 \\\\\\\"},\\\\\\\"children\\\\\\\":[{\\\\\\\"type\\\\\\\":\\\\\\\"Text\\\\\\\",\\\\\\\"attr\\\\\\\":{\\\\\\\"text\\\\\\\":\\\\\\\"广告\\\\\\\"},\\\\\\\"style\\\\\\\":{\\\\\\\"position\\\\\\\":\\\\\\\"absolute\\\\\\\",\\\\\\\"fontSize\\\\\\\":\\\\\\\"12\\\\\\\",\\\\\\\"lineHeight\\\\\\\":\\\\\\\"16\\\\\\\",\\\\\\\"top\\\\\\\":\\\\\\\"2\\\\\\\",\\\\\\\"right\\\\\\\":\\\\\\\"2\\\\\\\",\\\\\\\"color\\\\\\\":\\\\\\\"#6b6c70\\\\\\\",\\\\\\\"backgroundColor\\\\\\\":\\\\\\\"#38cfd1d4\\\\\\\"}},{\\\\\\\"type\\\\\\\":\\\\\\\"Text\\\\\\\",\\\\\\\"attrBind\\\\\\\":{\\\\\\\"text\\\\\\\":\\\\\\\"'Bar Card! #'+data.title\\\\\\\"},\\\\\\\"style\\\\\\\":{\\\\\\\"height\\\\\\\":\\\\\\\"30\\\\\\\",\\\\\\\"marginHorizontal\\\\\\\":\\\\\\\"12\\\\\\\",\\\\\\\"marginTop\\\\\\\":\\\\\\\"12\\\\\\\",\\\\\\\"color\\\\\\\":\\\\\\\"rgb(228,61,36)\\\\\\\",\\\\\\\"fontSize\\\\\\\":\\\\\\\"16\\\\\\\",\\\\\\\"fontWeight\\\\\\\":\\\\\\\"bold\\\\\\\"}},{\\\\\\\"type\\\\\\\":\\\\\\\"Image\\\\\\\",\\\\\\\"attrBind\\\\\\\":{\\\\\\\"source\\\\\\\":\\\\\\\"data.coverUrl\\\\\\\"},\\\\\\\"style\\\\\\\":{\\\\\\\"backgroundColor\\\\\\\":\\\\\\\"#d3d3d3\\\\\\\",\\\\\\\"height\\\\\\\":\\\\\\\"70\\\\\\\",\\\\\\\"width\\\\\\\":\\\\\\\"117\\\\\\\",\\\\\\\"marginLeft\\\\\\\":\\\\\\\"12\\\\\\\",\\\\\\\"marginBottom\\\\\\\":\\\\\\\"6\\\\\\\"}}]}\\\"\\n}\"\n" +
    "}\n";
}
