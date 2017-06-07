package co.gofun.customijk.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import cn.weidoo.customijk.BuildConfig;


public class ScreenUtil {

    private static final String TAG = "ScreenUtil";
    private Context context;
    private int screenBrightness;
    private int screenMode;

    public ScreenUtil(Context context) {
        this.context = context;
    }

    public int firstGetBrightness() {

//        mParams = window.getAttributes();
        /**
         * 第一次获取这值为-1,表示跟随系统亮度
         */
//        Log.i(TAG, "Attributes:" + mParams.screenBrightness + "");

        try {

            /**
             * 获得当前屏幕亮度的模式
             * SCREEN_BRIGHTNESS_MODE_AUTOMATIC=1 为自动调节屏幕亮度
             * SCREEN_BRIGHTNESS_MODE_MANUAL=0 为手动调节屏幕亮度
             */
            screenMode = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
            Log.i(TAG, "screenMode = " + screenMode);
            if (BuildConfig.VERSION_CODE < Build.VERSION_CODES.M) {//6.0系统以下改变系统屏幕亮度需要设置亮度模式为手动调节
                setBrightnessMode(0);
            }
            // 获得当前屏幕亮度值 0--255
            screenBrightness = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            Log.i(TAG, "Global screenBrightness = " + screenBrightness);
            if (BuildConfig.VERSION_CODE < Build.VERSION_CODES.M) {//6.0系统以下改变系统屏幕亮度需要设置亮度模式为手动调节
                setBrightnessMode(1);
            }
            return screenBrightness;

        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * @param mode 1:自动调节亮度，0为手动调节亮度
     *             需要 <uses-permission android:name="android.permission.WRITE_SETTINGS" />”权限
     */
    private void setBrightnessMode(int mode) {

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_SETTINGS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    0);
        } else {
            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, mode);


        }
    }
}
