package tungstwenty.xposed.securesamsungclipboard;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findMethodExact;

import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage {

	private static final String PACKAGE_ANDROID = "android";

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {

		if (lpparam.packageName.equals(PACKAGE_ANDROID)) {
			try {
				final Class<?> clsClipDataMgr = findClass("android.sec.clipboard.data.ClipboardDataMgr",
				    lpparam.classLoader);
				final Method mthSize = findMethodExact(clsClipDataMgr, "size");
				final Method mthRemoveData = findMethodExact(clsClipDataMgr, "removeData", int.class);
				findAndHookMethod(clsClipDataMgr, "addData", "android.sec.clipboard.data.ClipboardData",
				    new XC_MethodHook() {
					    @Override
					    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						    // Erase all entries in the clipboard history
						    int i = (Integer) mthSize.invoke(param.thisObject);
						    while (i-- > 0)
							    mthRemoveData.invoke(param.thisObject, 0);
					    }
				    });

			} catch (Throwable t) {
				XposedBridge.log(t);
			}
		}
	}
}
