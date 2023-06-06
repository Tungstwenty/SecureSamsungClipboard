package tungstwenty.xposed.securesamsungclipboard;

import static de.robv.android.xposed.XposedBridge.hookMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findConstructorExact;
import static de.robv.android.xposed.XposedHelpers.findMethodExact;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import java.lang.reflect.Method;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.XModuleResources;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import ;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {

	private static final String PACKAGE_ANDROID = "android";
	private static final String CLASS_CLIPBOARDDATATEXT = "android.sec.clipboard.data.list.ClipboardDataText";

	private static Handler mHandler = null;

	private String modulePath;
	private String msgClipboardCleared = null;;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		modulePath = startupParam.modulePath;
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {

		if (lpparam.packageName.equals(PACKAGE_ANDROID)) {
			try {
				hookMethod(
				    findConstructorExact(findClass("com.android.server.sec.InternalClipboardExService", null),
				        Context.class), new XC_MethodHook() {
					    @SuppressLint("HandlerLeak")
					    @Override
					    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						    if (mHandler == null && param.args[0] instanceof Context) {
							    final Context ctx = (Context) param.args[0];
							    mHandler = new Handler() {
								    public void handleMessage(Message msg) {
									    if (msg.arg1 == 1)
										    Toast.makeText(ctx, msgClipboardCleared, Toast.LENGTH_SHORT).show();
								    }
							    };
						    }
					    }
				    });
			} catch (Throwable t) {
				XposedBridge.log(t);
			}
			try {
				final Class<?> clsClipDataMgr = findClass("android.sec.clipboard.data.ClipboardDataMgr",
				    lpparam.classLoader);
				final Method mthSize = findMethodExact(clsClipDataMgr, "size");
				final Method mthRemoveData = findMethodExact(clsClipDataMgr, "removeData", int.class);
				findAndHookMethod(clsClipDataMgr, "addData", "android.sec.clipboard.data.ClipboardData",
				    new XC_MethodHook() {
					    @Override
					    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						    // Check if adding empty string, which triggers clearing the clipboard

						    if (!param.args[0].getClass().getName().equals(CLASS_CLIPBOARDDATATEXT))
							    return; // Not adding a text clip

						    String value = (String) getObjectField(param.args[0], "mValue");
						    if (value != null && value.length() > 0)
							    return; // Not adding an empty string

						    // Erase all entries in the clipboard history
						    int i = (Integer) mthSize.invoke(param.thisObject);
						    while (i-- > 0)
							    mthRemoveData.invoke(param.thisObject, 0);

						    // Notify the user that the history was cleared
						    if (mHandler != null && msgClipboardCleared != null) {
							    Message msg = mHandler.obtainMessage();
							    msg.arg1 = 1;
							    mHandler.sendMessageDelayed(msg, 200L);
						    }
					    }
				    });
			} catch (Throwable t) {
				XposedBridge.log(t);
			}
		}
	}

	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
		if (msgClipboardCleared == null && resparam.packageName.equals(PACKAGE_ANDROID)) {
			// Reference the resources from this mod
			XModuleResources modRes = XModuleResources.createInstance(modulePath, resparam.res);
			msgClipboardCleared = modRes.getString(R.string.msg_clipboard_cleared);
		}
	}

}
