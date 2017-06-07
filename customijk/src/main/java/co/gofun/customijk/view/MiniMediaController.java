package co.gofun.customijk.view;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Formatter;
import java.util.Locale;

import cn.weidoo.customijk.R;
import co.gofun.customijk.player.IMiniMediaController;


public class MiniMediaController extends FrameLayout implements IMiniMediaController {

    private static final String TAG = "MiniMediaController";
    private MediaPlayerControl mPlayer;


    private Context mContext;
    private View mRoot;

    private ImageView fullScreenBtn;
    private SeekBar mSeekbar;
    private TextView mCurrentTime;
    private TextView mEndTime;
    /**
     * controller中的播放按钮
     */
    private CheckBox playBtnOfController;

    private Formatter mFormatter;
    private StringBuilder mFormatBuilder;
    private boolean mDragging = false;
    private boolean mShowing = false;

    private ViewGroup mAnchor;

    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;
    private AccessibilityManager mAccessibilityManager;
    private static final int sDefaultTimeout = 3000;
    private MiniMediaTitleBar mediaTitleBar = new MiniMediaTitleBar(getContext());
    ;

    private View playBtnView;
    private boolean IS_SCREEN_LOCK = false;

    private LinearLayout screenLockLayout;
    private boolean playBtnShowStatus = false;


    @Override
    public MiniMediaTitleBar getMediaTitleBar() {
        return mediaTitleBar;
    }


    public MiniMediaController(Context context) {
        super(context);
        mRoot = this;
        mContext = context;
        mAccessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                show(false);
            }
        });

    }

    public MiniMediaController(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MiniMediaController(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MiniMediaController(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    View v;

    public void setAnchorView(ViewGroup view) {

        Log.w(TAG, "setAnchorView");
        mAnchor = view;
        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT

        );

        removeAllViews();
        v = makeControllerView();
        addView(v, frameParams);
        Log.w(TAG, v.toString());


        if (screenLockLayout == null) {
            screenLockLayout = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.mini_media_lock_screen, null);
            screenLockLayout.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    View lockOn = screenLockLayout.findViewById(R.id.mini_player_lock_on);
                    View lockOff = screenLockLayout.findViewById(R.id.mini_player_lock_off);
                    if (IS_SCREEN_LOCK == false) {
                        lockOff.setVisibility(GONE);
                        lockOn.setVisibility(VISIBLE);
                        IS_SCREEN_LOCK = true;
                        hide();
                    } else {
                        lockOff.setVisibility(VISIBLE);
                        lockOn.setVisibility(GONE);
                        IS_SCREEN_LOCK = false;
                        show(false);
                    }
                }
            });
        }

    }

    protected View makeControllerView() {
        Activity activity = (Activity) mContext;
        Log.w(TAG, "activity oriention:" + activity.getRequestedOrientation());
        if (activity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
            mRoot = LayoutInflater.from(mContext).inflate(R.layout.mini_media_controller_big, null);
        } else {
            mRoot = LayoutInflater.from(mContext).inflate(R.layout.mini_media_controller_small, null);
        }
        initControllerView(mRoot);
        return mRoot;
    }

    private void initControllerView(View v) {

        mSeekbar = (SeekBar) v.findViewById(R.id.player_seek_bar);
        fullScreenBtn = (ImageView) v.findViewById(R.id.player_full_screen);
        mCurrentTime = (TextView) v.findViewById(R.id.player_cur_time);
        mEndTime = (TextView) v.findViewById(R.id.player_end_time);
        playBtnOfController = (CheckBox) v.findViewById(R.id.player_controller_player_btn);

        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

        mSeekbar.setOnSeekBarChangeListener(mSeekListener);

        if (mPlayer != null) {
            setFullScreenBtnEnable(!mPlayer.isFullScreen());
            playBtnOfController.setChecked(mPlayer.isPlaying());
            fullScreenBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

                    mPlayer.setFullScreen(true);
                }
            });
        }

        playBtnOfController.setOnCheckedChangeListener(playBtnOfControllerOnCheckedChangeListener);

    }

    private CompoundButton.OnCheckedChangeListener playBtnOfControllerOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Log.i(TAG, "playBtnOfControllerOnCheckedChangeListener:" + isChecked);
            if (isChecked) {
                mPlayer.start();
            } else {
                mPlayer.pause();
            }
        }
    };

    /**
     * 设置全屏按钮是否可见
     *
     * @param enabled
     */
    public void setFullScreenBtnEnable(boolean enabled) {
        if (fullScreenBtn != null) {
            if (enabled) {
                fullScreenBtn.setVisibility(View.VISIBLE);
            } else {
                fullScreenBtn.setVisibility(View.GONE);
            }
        }

    }


    private final SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {

        long newPosition;


        @Override
        public void onStartTrackingTouch(SeekBar bar) {
            show(false, 3600000);

            mDragging = true;

            // By removing these pending progress messages we make sure
            // that a) we won't update the progress while the user adjusts
            // the seekbar and b) once the user is done dragging the thumb
            // we will post one of these messages to the queue again and
            // this ensures that there will be exactly one message queued up.
            mHandler.removeMessages(SHOW_PROGRESS);
        }

        @Override
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return;
            }

            long duration = mPlayer.getDuration();
            newPosition = (duration * progress) / 100L;
           /* mPlayer.seekTo((int) newPosition);
            if (mCurrentTime != null)
                mCurrentTime.setText(stringForTime((int) newPosition));*/
        }

        @Override
        public void onStopTrackingTouch(SeekBar bar) {

            mPlayer.seekTo((int) newPosition);
            if (mCurrentTime != null)
                mCurrentTime.setText(stringForTime((int) newPosition));

            mDragging = false;
            setProgress();
//            updatePausePlay();
            show(false, sDefaultTimeout);

            // Ensure that progress is properly updated in the future,
            // the call to show() does not guarantee this because it is a
            // no-op if we are already showing.
            mHandler.sendEmptyMessage(SHOW_PROGRESS);
        }
    };

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int pos;
            switch (msg.what) {
                case FADE_OUT:
                    hide();
                    break;
                case SHOW_PROGRESS:
                    pos = setProgress();
                    if (!mDragging && mShowing && mPlayer != null && mPlayer.isPlaying()) {
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
            }
        }
    };


    public boolean isShowing() {
        return mShowing;
    }

    @Override
    public void show(boolean reset) {
        show(reset, sDefaultTimeout);
    }

    @Override
    public void show(boolean reset, int timeout) {
        Log.i(TAG, "show()");

        if (reset == true) {
            hide();
        }


        if (!mShowing && mAnchor != null) {
            setProgress();
//            disableUnsupportedButtons();
//            updateFloatingWindowLayout();
//            mWindowManager.addView(mDecor, mDecorLayoutParams);
            mShowing = true;


            FrameLayout.LayoutParams titleLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP
            );
            FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
            );

            FrameLayout.LayoutParams lockLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.LEFT | Gravity.CENTER_VERTICAL
            );
            if (IS_SCREEN_LOCK == false) {
                mAnchor.addView(mediaTitleBar, titleLp);
                mAnchor.addView(this, tlp);
                Log.w(TAG, this.toString() + "height:" + this.getHeight());
            }
            if (mPlayer != null && mPlayer.isFullScreen() == true) {
                mAnchor.addView(screenLockLayout, lockLp);
            }


        }

//        updatePausePlay();


        // cause the progress bar to be updated even if mShowing
        // was already true.  This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
        mHandler.sendEmptyMessage(SHOW_PROGRESS);

        if (timeout != 0 && !mAccessibilityManager.isTouchExplorationEnabled()) {
            mHandler.removeMessages(FADE_OUT);
            Message msg = mHandler.obtainMessage(FADE_OUT);
            mHandler.sendMessageDelayed(msg, timeout);

        }

    }


    /**
     * 屏幕旋转时重置播放按钮
     *
     * @param archor
     */
    @Override
    public void resetPlayBtn(ViewGroup archor) {
        if (playBtnShowStatus == true) {
            showPlayBtnView(archor);
        } else {
            hidePlayBtnView();
        }
    }

    @Override
    public void showPlayBtnView(final ViewGroup anchor) {
        if (mAnchor == null) {
            mAnchor = anchor;
        }

        if (playBtnView == null) {
            playBtnView = LayoutInflater.from(mContext).inflate(R.layout.mini_media_play, null);
        }

        playBtnView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayer.start();
                hidePlayBtnView();
            }
        });

        if (mAnchor == null) {
            return;
        }

        mAnchor.post(new Runnable() {
            @Override
            public void run() {
                FrameLayout.LayoutParams playLp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        mAnchor.getHeight(),
                        Gravity.CENTER);
                if (playBtnView != null) {
                    hidePlayBtnView();
                }
                mAnchor.addView(playBtnView);
                playBtnView.setLayoutParams(playLp);
            }
        });
        playBtnShowStatus = true;


    }

    private void hidePlayBtnView() {

        if (mAnchor == null || playBtnView == null) {
            return;
        }

        try {
            mAnchor.removeView(playBtnView);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "play btn already removed");
        }
        playBtnShowStatus = false;


    }

    public void hide() {
        Log.i(TAG, "hide()");

        if (mAnchor == null)
            return;

        if (mShowing) {
            try {
                mAnchor.removeView(this);
                mAnchor.removeView(mediaTitleBar);
                mAnchor.removeView(screenLockLayout);
                mHandler.removeMessages(SHOW_PROGRESS);
            } catch (IllegalArgumentException ex) {
                Log.w(TAG, "mideia controller already removed");
            } catch (Exception e) {
                e.printStackTrace();
            }
            mShowing = false;
        }
    }

   /* @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                show(false, 0); // show until hide is called
                break;
            case MotionEvent.ACTION_UP:
                show(false, sDefaultTimeout); // start timeout
                break;
            case MotionEvent.ACTION_CANCEL:
                hide();
                break;
            default:
                break;
        }
        return true;
    }*/

    private int setProgress() {
        if (mPlayer == null || mDragging) {
            return 0;
        }
        int position = mPlayer.getCurrentPosition();
        int duration = mPlayer.getDuration();

        if (mSeekbar != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 100L * position / duration;
                mSeekbar.setProgress((int) pos);
//                Log.i(TAG, "current pos: " + position);
//                Log.i(TAG, "pos ----------->" + pos);
            }
            int percent = mPlayer.getBufferPercentage();
//            Log.i(TAG, "buffer percent:" + percent);
            mSeekbar.setSecondaryProgress(percent);
        }

        if (mEndTime != null)
            mEndTime.setText(stringForTime(duration));
        if (mCurrentTime != null)
            mCurrentTime.setText(stringForTime(position));

        return position;
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    @Override
    public void notifyPlayStateChange(boolean isPlaying) {

        if (playBtnOfController != null) {
            playBtnOfController.setChecked(isPlaying);
        }
        if (isPlaying) {
            hidePlayBtnView();
        } else {
            showPlayBtnView(mAnchor);
        }
    }


    public void setMediaPlayer(MediaPlayerControl player) {
        mPlayer = player;
    }

    @Override
    public boolean isScreenLocked() {
        return IS_SCREEN_LOCK;
    }


    public interface MediaPlayerControl {
        void start();

        void pause();

        int getDuration();

        int getBufferPercentage();

        int getCurrentPosition();

        void seekTo(long position);

        boolean isPlaying();

        boolean isFullScreen();

        void setFullScreen(boolean isFullScreen);

        void stop();
    }
}
