package co.gofun.customijk.view;


import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.view.Gravity;
import android.widget.ProgressBar;

import cn.weidoo.customijk.R;

/**
 * Created by yangfeng on 2016/12/9.
 */
public class VolumeAndBrightnessView extends FrameLayout {

    private static final String TAG = "VolumeAndBrightnessView";
    public static final int VOLUME = 0;
    public static final int BRIGHTNESS = 1;
    private static final int SHOW_VIEW = 1;
    private static final int HIDE_VIEW = 0;
    private ViewGroup mAnchorView;
    private boolean showFlag = false;
    private Context mContext;
    private View volumeView;
    private View brightnessView;
    private ProgressBar volumeBar;
    private ProgressBar brightnessBar;

    public VolumeAndBrightnessView(Context context) {
        super(context);
        initView(context);
    }

    public VolumeAndBrightnessView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public VolumeAndBrightnessView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public VolumeAndBrightnessView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    public void setAnchorView(FrameLayout view) {
        mAnchorView = view;
    }

    private void initView(Context context) {
        this.mContext = context;
        volumeView = LayoutInflater.from(mContext).inflate(R.layout.player_volume_set, null);
        brightnessView = LayoutInflater.from(mContext).inflate(R.layout.player_brightness_set, null);
        volumeBar = (ProgressBar) volumeView.findViewById(R.id.progress);
        brightnessBar = (ProgressBar) brightnessView.findViewById(R.id.progress);
    }


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_VIEW:
                    break;
                case HIDE_VIEW:
                    hide();
                    break;

            }
        }
    };


    public void show(int type, int progress) {
        hide();
        if (!showFlag) {
            switch (type) {
                case VOLUME:
                    LayoutParams bParams = new LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER);
                    mAnchorView.addView(volumeView, bParams);

                    volumeBar.setProgress(progress);
                    showFlag = true;
                    break;
                case BRIGHTNESS:
                    LayoutParams vParams = new LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER);
                    mAnchorView.addView(brightnessView, vParams);

                    brightnessBar.setProgress(progress);
                    showFlag = true;
                    break;
            }
//            Message msg = mHandler.obtainMessage(HIDE_VIEW);
//            mHandler.sendMessageDelayed(msg, 1000);
        }

    }

    public void hide() {
        if (mAnchorView == null)
            return;

        if (showFlag) {
            try {
                mAnchorView.removeView(this);
                mAnchorView.removeView(volumeView);
                mAnchorView.removeView(brightnessView);
                mHandler.removeMessages(SHOW_VIEW);
            } catch (IllegalArgumentException ex) {
                Log.w(TAG, "mideia controller already removed");
            } catch (Exception e) {
                e.printStackTrace();
            }
            showFlag = false;
        }
    }

}
