package co.gofun.customijk.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import cn.weidoo.customijk.R;


public class LoadingView extends FrameLayout {

    private static final String TAG = "LoadingView";
    private Context mContext;
    private boolean showFlag = false;
    private ViewGroup mAnchorView;
    private View loadindView;

    public LoadingView(Context context) {
        super(context);
        initView(context);
    }

    public LoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public LoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LoadingView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    private void initView(Context context) {
        this.mContext = context;
        loadindView = LayoutInflater.from(mContext).inflate(R.layout.player_loading, null);

    }


    public void setAnchorView(ViewGroup view) {
        this.mAnchorView = view;
    }


    public void show() {

        if (!showFlag && mAnchorView != null) {
            LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT, Gravity.CENTER);
            mAnchorView.addView(loadindView, params);
            showFlag = true;
        }

    }

    public void hide() {

        if (mAnchorView == null) {
            return;
        }

        if (showFlag && mAnchorView != null) {
            try {
                mAnchorView.removeView(loadindView);
            } catch (IllegalArgumentException ex) {
                Log.w(TAG, "mideia controller already removed");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        showFlag = false;
    }


}
