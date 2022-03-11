package sh.siava.AOSPMods.systemui;

import android.view.MotionEvent;

import androidx.preference.ListPreference;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.ResourceManager;
import sh.siava.AOSPMods.XPrefs;

public class QSQuickPullDown implements IXposedHookLoadPackage {
    public static final String listenPackage = "com.android.systemui";
    public static int pullDownSide = 1; // 1 is right, 2 is left
    public static boolean oneFingerPulldownEnabled = false;
    public static float statusbarPortion = 0.25f; // now set to 25% of the screen. it can be anything between 0 to 100%

    public static void updatePrefs()
    {
        oneFingerPulldownEnabled = XPrefs.Xprefs.getBoolean("QSPullodwnEnabled", false);
        statusbarPortion =  XPrefs.Xprefs.getInt("QSPulldownPercent", 25) / 100f;
        pullDownSide = Integer.parseInt(XPrefs.Xprefs.getString("QSPulldownSide", "1"));
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        XposedHelpers.findAndHookMethod(" com.android.systemui.statusbar.phone.NotificationPanelViewController", lpparam.classLoader,
                "isOpenQsEvent", MotionEvent.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!oneFingerPulldownEnabled) return;
                        Object mView = XposedHelpers.getObjectField(param.thisObject, "mView");
                        int w = (int) XposedHelpers.callMethod(mView, "getMeasuredWidth");

                        MotionEvent event = (MotionEvent) param.args[0];
                        float x = event.getX();

                        float region = w * statusbarPortion;

                        boolean showQsOverride = false;

                        boolean isRtl = (boolean) XposedHelpers.callMethod(mView, "isLayoutRtl");

                        switch (pullDownSide) {
                            case 1: // Right side pulldown
                                showQsOverride = isRtl ? x < region : w - region < x;
                                break;
                            case 2: // Left side pulldown
                                showQsOverride = isRtl ? w - region < x : x < region;
                                break;
                        }
                        int mBarState = (int) XposedHelpers.getObjectField(param.thisObject, "mBarState");

                        showQsOverride &= mBarState == 0; // Statusbar SHADE mode is 0

                        boolean prevResult = (boolean) param.getResult();
                        param.setResult(prevResult || showQsOverride);
                        return;
                    }
                });
    }
}