package co.gofun.customijk.view;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.weidoo.customijk.R;
import co.gofun.customijk.application.Settings;
import co.gofun.customijk.player.IMiniMediaController;
import co.gofun.customijk.player.IRenderView;
import co.gofun.customijk.services.MediaPlayerService;
import co.gofun.customijk.util.ScreenUtil;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.TextureMediaPlayer;


public class MiniVideoView extends FrameLayout implements MiniMediaController.MediaPlayerControl {

    private static final String TAG = "MiniVideoView";
    //-------------------------
    // Extend: Background
    private Uri mUri;
    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;

    private static final int STATE_PLAYBACK_COMPLETED = 5;
    private int mCurrentPlayState;
    private int mTargetPlayState;
    private Context mAppContext;
    private long mSeekWhenPrepared;  // recording the seek position while preparing

    private IMediaPlayer mMediaPlayer;


    private GestureDetectorCompat mDetector;


    //-------------------------
    private boolean mEnableBackgroundPlay = false;
    //-------------------------
    // Extend: Aspect Ratio

    private IRenderView.ISurfaceHolder mSurfaceHolder;
    private IRenderView mRenderView;
    private Settings mSettings;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    /**
     * 是否全屏
     */
    private boolean IS_FULL_SCREEN = false;


    private int mVideoSarNum;
    private int mVideoSarDen;
    //-------------------------
    // Extend: Render
    //-------------------------
    public static final int RENDER_NONE = 0;
    public static final int RENDER_SURFACE_VIEW = 1;
    public static final int RENDER_TEXTURE_VIEW = 2;
    private List<Integer> mAllRenders = new ArrayList<Integer>();
    private int mCurrentRenderIndex = 0;

    private int mCurrentRender = RENDER_NONE;
    //-------------------------
    private static final int[] s_allAspectRatio = {
            IRenderView.AR_ASPECT_FIT_PARENT,
            IRenderView.AR_ASPECT_FILL_PARENT,
            IRenderView.AR_ASPECT_WRAP_CONTENT,
            // IRenderView.AR_MATCH_PARENT,
            IRenderView.AR_16_9_FIT_PARENT,
            IRenderView.AR_4_3_FIT_PARENT};
    private int mCurrentAspectRatioIndex = 0;
    private int mCurrentAspectRatio = s_allAspectRatio[0];
    private int mVideoRotationDegree;

    private int mCurrentBufferPercentage;
    private Map<String, String> mHeaders;
    private IMiniMediaController mMediaController;
    private IMediaPlayer.OnPreparedListener mOnPreparedListener;
    private IMediaPlayer.OnCompletionListener mOnCompletionListener;
    private IMediaPlayer.OnInfoListener mOnInfoListener;
    private IMediaPlayer.OnErrorListener mOnErrorListener;

    private OnFullScreenChangeListener onFullScreenChangeListener;

    private Context mContext;

    private boolean firstGetBrightnessFlag = true;

    private VolumeAndBrightnessView volumeAndBrightnessView;
    private LoadingView loadingView;
    private int centerX;
    private int mMaxVolume;


    public MiniVideoView(Context context) {
        super(context);

        initView(context);
    }

    public void setOnFullScreenChangeListener(OnFullScreenChangeListener listener) {
        this.onFullScreenChangeListener = listener;
    }

    public MiniVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public MiniVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MiniVideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    private void initView(Context context) {

        mContext = context;
        mDetector = new GestureDetectorCompat(context, new MiniOnGestureListener());

        mAppContext = context.getApplicationContext();
        mSettings = new Settings(mAppContext);


        initBackground();
        initRenderView();
        mVideoWidth = 0;
        mVideoHeight = 0;
        // REMOVED: getHolder().addCallback(mSHCallback);
        // REMOVED: getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        // REMOVED: mPendingSubtitleTracks = new Vector<Pair<InputStream, MediaFormat>>();
        mCurrentPlayState = STATE_IDLE;
        mTargetPlayState = STATE_IDLE;

        mMaxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        volumeAndBrightnessView = new VolumeAndBrightnessView(mContext);
        loadingView = new LoadingView(mContext);

        volumeAndBrightnessView.setAnchorView(MiniVideoView.this);
        loadingView.setAnchorView(MiniVideoView.this);

    }

    private void initRenderView() {

        mAllRenders.clear();

        if (mSettings.getEnableSurfaceView())
            mAllRenders.add(RENDER_SURFACE_VIEW);
        if (mSettings.getEnableTextureView() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            mAllRenders.add(RENDER_TEXTURE_VIEW);
        if (mSettings.getEnableNoView())
            mAllRenders.add(RENDER_NONE);

        if (mAllRenders.isEmpty())
            mAllRenders.add(RENDER_SURFACE_VIEW);
        //TUDO 选择合适的renderView
        mCurrentRender = mAllRenders.get(mCurrentRenderIndex);
        setRender(mCurrentRender);

    }

    public void setMediaController(IMiniMediaController controller) {
        if (mMediaController != null) {
            mMediaController.hide();
        }
        mMediaController = controller;
        attachMediaController();
    }


    /**
     * 添加标题栏
     *
     * @param mediaTitleBar
     */
    public void setMediaTitleBar(MiniMediaTitleBar mediaTitleBar) {
        mediaTitleBar.setAnchorView(this);
    }


    private IRenderView.IRenderCallback mSHCallback = new IRenderView.IRenderCallback() {
        @Override
        public void onSurfaceCreated(@NonNull IRenderView.ISurfaceHolder holder, int width, int height) {

            if (holder.getRenderView() != mRenderView) {
                Log.e(TAG, "onSurfaceCreated: unmatched render callback\n");
                return;
            }

            mSurfaceHolder = holder;
            if (mMediaPlayer != null)
                bindSurfaceHolder(mMediaPlayer, holder);
            else
                openVideo();
        }

        @Override
        public void onSurfaceChanged(@NonNull IRenderView.ISurfaceHolder holder, int format, int width, int height) {
            if (holder.getRenderView() != mRenderView) {
                Log.e(TAG, "onSurfaceChanged: unmatched render callback\n");
                return;
            }

            mSurfaceWidth = width;

            mSurfaceHeight = height;
            Log.i(TAG, "surface width:" + mSurfaceWidth);
            Log.i(TAG, "surface height:" + mSurfaceHeight);
            boolean isValidState = (mTargetPlayState == STATE_PLAYING);
            boolean hasValidSize = !mRenderView.shouldWaitForResize() || (mVideoWidth == width && mVideoHeight == height);
            if (mMediaPlayer != null && isValidState && hasValidSize) {
                if (mSeekWhenPrepared != 0) {
                    seekTo(mSeekWhenPrepared);
                }
                /**
                 *
                 */
                Log.e(TAG, "onSurfaceChanged: start");
                start();
            }
        }

        @Override
        public void onSurfaceDestroyed(@NonNull IRenderView.ISurfaceHolder holder) {

            if (holder.getRenderView() != mRenderView) {
                Log.e(TAG, "onSurfaceDestroyed: unmatched render callback\n");
                return;
            }

            // after we return from this we can't use the surface any more
            mSurfaceHolder = null;
            // REMOVED: if (mMediaController != null) mMediaController.hide();
            // REMOVED: release(true);
            releaseWithoutStop();
        }
    };

    public void releaseWithoutStop() {
        if (mMediaPlayer != null)
            mMediaPlayer.setDisplay(null);
    }


    /**
     * Sets video path.
     *
     * @param path the path of the video.
     */
    public void setVideoPath(String path) {
        setVideoURI(Uri.parse(path));
    }

    /**
     * Sets video URI.
     *
     * @param uri the URI of the video.
     */
    public void setVideoURI(Uri uri) {
        setVideoURI(uri, null);
    }

    /**
     * Sets video URI using specific headers.
     *
     * @param uri     the URI of the video.
     * @param headers the headers for the URI request.
     *                Note that the cross domain redirection is allowed by default, but that can be
     *                changed with key/value pairs through the headers parameter with
     *                "android-allow-cross-domain-redirect" as the key and "0" or "1" as the value
     *                to disallow or allow cross domain redirection.
     */
    private void setVideoURI(Uri uri, Map<String, String> headers) {
        mUri = uri;
        mHeaders = headers;
        mSeekWhenPrepared = 0;
        openVideo();
        requestLayout();
        invalidate();
    }


    /**
     * 生成一个renderView来渲染Video
     *
     * @param renderType
     */
    private void setRender(int renderType) {
        switch (renderType) {
            case RENDER_NONE:
                setRenderView(null);
                break;
            case RENDER_TEXTURE_VIEW:
                TextureRenderView renderView = new TextureRenderView(getContext());
                if (mMediaPlayer != null) {
                    renderView.getSurfaceHolder().bindToMediaPlayer(mMediaPlayer);
                    renderView.setVideoSize(mMediaPlayer.getVideoWidth(), mMediaPlayer.getVideoHeight());
                    renderView.setVideoSampleAspectRatio(mMediaPlayer.getVideoSarNum(), mMediaPlayer.getVideoSarDen());
                    renderView.setAspectRatio(mCurrentAspectRatio);
                }
                setRenderView(renderView);
                break;
            case RENDER_SURFACE_VIEW:
                IRenderView surfaceView = new SurfaceRenderView(getContext());
                setRenderView(surfaceView);
                break;
            default:
                Log.e(TAG, String.format(Locale.getDefault(), "invalid render %d\n", renderType));
                break;

        }
    }

    private void setRenderView(IRenderView renderView) {
        if (mRenderView != null) {
            if (mMediaPlayer != null) {
                mMediaPlayer.setDisplay(null);
            }
            View renderUIView = mRenderView.getView();
            mRenderView.removeRenderCallback(mSHCallback);
            mRenderView = null;
            removeView(renderUIView);
        }
        if (renderView == null) {
            return;
        }

        mRenderView = renderView;
        mRenderView.setAspectRatio(mCurrentAspectRatio);
        if (mVideoWidth > 0 && mVideoHeight > 0)
            renderView.setVideoSize(mVideoWidth, mVideoHeight);
        if (mVideoSarNum > 0 && mVideoSarDen > 0)
            renderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);

        View renderUIView = mRenderView.getView();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        renderUIView.setLayoutParams(lp);
        addView(renderUIView);

        mRenderView.addRenderCallback(mSHCallback);
        mRenderView.setVideoRotation(mVideoRotationDegree);
    }

    private void initBackground() {
        mEnableBackgroundPlay = mSettings.getEnableBackgroundPlay();
        if (mEnableBackgroundPlay) {
            MediaPlayerService.intentToStart(getContext());
            mMediaPlayer = MediaPlayerService.getMediaPlayer();
        }
    }


    IMediaPlayer.OnVideoSizeChangedListener mSizeChangedListener =
            new IMediaPlayer.OnVideoSizeChangedListener() {
                public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sarNum, int sarDen) {
                    mVideoWidth = mp.getVideoWidth();
                    mVideoHeight = mp.getVideoHeight();
                    mVideoSarNum = mp.getVideoSarNum();
                    mVideoSarDen = mp.getVideoSarDen();
                    if (mVideoWidth != 0 && mVideoHeight != 0) {
                        if (mRenderView != null) {
                            mRenderView.setVideoSize(mVideoWidth, mVideoHeight);
                            mRenderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);
                        }
                        // REMOVED: getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                        requestLayout();
                    }
                }
            };

    IMediaPlayer.OnPreparedListener mPreparedListener = new IMediaPlayer.OnPreparedListener() {
        public void onPrepared(IMediaPlayer mp) {
            mCurrentPlayState = STATE_PREPARED;

            // Get the capabilities of the player for this stream
            // REMOVED: Metadata

            if (mOnPreparedListener != null) {
                mOnPreparedListener.onPrepared(mMediaPlayer);
            }
            if (mMediaController != null) {
                mMediaController.setEnabled(true);
            }
            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();

            long seekToPosition = mSeekWhenPrepared;  // mSeekWhenPrepared may be changed after seekTo() call
            if (seekToPosition != 0) {
                seekTo(seekToPosition);
            }
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                //Log.i("@@@@", "video size: " + mVideoWidth +"/"+ mVideoHeight);
                // REMOVED: getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                if (mRenderView != null) {
                    mRenderView.setVideoSize(mVideoWidth, mVideoHeight);
                    mRenderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);
                    if (!mRenderView.shouldWaitForResize() || mSurfaceWidth == mVideoWidth && mSurfaceHeight == mVideoHeight) {
                        // We didn't actually change the size (it was already at the size
                        // we need), so we won't get a "surface changed" callback, so
                        // start the video here instead of in the callback.
                        if (mTargetPlayState == STATE_PLAYING) {
                            start();
                            if (mMediaController != null) {
                                mMediaController.show(false);
                            }
                        } else if (!isPlaying() &&
                                (seekToPosition != 0 || getCurrentPosition() > 0)) {
                            if (mMediaController != null) {
                                // Show the media controls when we're paused into a video and make 'em stick.
                                mMediaController.show(false, 0);
                            }
                        }
                    }
                }
            } else {
                // We don't know the video size yet, but should start anyway.
                // The video size might be reported to us later.
                if (mTargetPlayState == STATE_PLAYING) {
                    start();
                }
            }
        }
    };

    private IMediaPlayer.OnCompletionListener mCompletionListener =
            new IMediaPlayer.OnCompletionListener() {
                public void onCompletion(IMediaPlayer mp) {
                    mCurrentPlayState = STATE_PLAYBACK_COMPLETED;
                    mTargetPlayState = STATE_PLAYBACK_COMPLETED;
                    if (mMediaController != null) {
                        mMediaController.hide();
                    }
                    if (mOnCompletionListener != null) {
                        mOnCompletionListener.onCompletion(mMediaPlayer);
                    }

                    mMediaController.notifyPlayStateChange(false);
                }
            };

    private IMediaPlayer.OnInfoListener mInfoListener =
            new IMediaPlayer.OnInfoListener() {
                public boolean onInfo(IMediaPlayer mp, int arg1, int arg2) {
                    if (mOnInfoListener != null) {
                        mOnInfoListener.onInfo(mp, arg1, arg2);
                    }
                    switch (arg1) {
                        case IMediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                            Log.d(TAG, "MEDIA_INFO_VIDEO_TRACK_LAGGING:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                            Log.d(TAG, "MEDIA_INFO_VIDEO_RENDERING_START:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                            Log.d(TAG, "MEDIA_INFO_BUFFERING_START:");
                            loadingView.show();
                            break;
                        case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                            Log.d(TAG, "MEDIA_INFO_BUFFERING_END:");
                            loadingView.hide();
                            break;
                        case IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH:
                            Log.d(TAG, "MEDIA_INFO_NETWORK_BANDWIDTH: " + arg2);
                            break;
                        case IMediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                            Log.d(TAG, "MEDIA_INFO_BAD_INTERLEAVING:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                            Log.d(TAG, "MEDIA_INFO_NOT_SEEKABLE:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                            Log.d(TAG, "MEDIA_INFO_METADATA_UPDATE:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE:
                            Log.d(TAG, "MEDIA_INFO_UNSUPPORTED_SUBTITLE:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT:
                            Log.d(TAG, "MEDIA_INFO_SUBTITLE_TIMED_OUT:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED:
                            mVideoRotationDegree = arg2;
                            Log.d(TAG, "MEDIA_INFO_VIDEO_ROTATION_CHANGED: " + arg2);
                            if (mRenderView != null)
                                mRenderView.setVideoRotation(arg2);
                            break;
                        case IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
                            Log.d(TAG, "MEDIA_INFO_AUDIO_RENDERING_START:");
                            break;
                    }
                    return true;
                }
            };

    private IMediaPlayer.OnErrorListener mErrorListener =
            new IMediaPlayer.OnErrorListener() {
                public boolean onError(IMediaPlayer mp, int framework_err, int impl_err) {
                    Log.d(TAG, "Error: " + framework_err + "," + impl_err);
                    mCurrentPlayState = STATE_ERROR;
                    mTargetPlayState = STATE_ERROR;
                    if (mMediaController != null) {
                        mMediaController.hide();
                    }

                    /* If an error handler has been supplied, use it and finish. */
                    if (mOnErrorListener != null) {
                        if (mOnErrorListener.onError(mMediaPlayer, framework_err, impl_err)) {
                            return true;
                        }
                    }

                    /* Otherwise, pop up an error dialog so the user knows that
                     * something bad has happened. Only try and pop up the dialog
                     * if we're attached to a window. When we're going away and no
                     * longer have a window, don't bother showing the user an error.
                     */
                    if (getWindowToken() != null) {
                        Resources r = mAppContext.getResources();
                        int messageId;

                        if (framework_err == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
                            messageId = R.string.VideoView_error_text_invalid_progressive_playback;
                        } else {
                            messageId = R.string.VideoView_error_text_unknown;
                        }

                        new AlertDialog.Builder(getContext())
                                .setMessage(messageId)
                                .setPositiveButton(R.string.VideoView_error_button,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                            /* If we get here, there is no onError listener, so
                                             * at least inform them that the video is over.
                                             */
                                                if (mOnCompletionListener != null) {
                                                    mOnCompletionListener.onCompletion(mMediaPlayer);
                                                }
                                            }
                                        })
                                .setCancelable(false)
                                .show();
                    }
                    return true;
                }
            };

    private IMediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
            new IMediaPlayer.OnBufferingUpdateListener() {
                public void onBufferingUpdate(IMediaPlayer mp, int percent) {
                    mCurrentBufferPercentage = percent;
                }
            };


    //    @TargetApi(Build.VERSION_CODES.M)
    private void openVideo() {
        if (mUri == null || mSurfaceHolder == null) {
            // not ready for playback just yet, will try again later
            return;
        }
        // we shouldn't clear the target state, because somebody might have
        // called start() previously
        release(false);

        AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
        int i = am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        Log.i(TAG, "get audio manager:" + i);

        try {
            mMediaPlayer = createPlayer(mSettings.getPlayer());

            // TODO: create SubtitleController in MediaPlayer, but we need
            // a context for the subtitle renderers
            final Context context = getContext();
            // REMOVED: SubtitleController

            // REMOVED: mAudioSession
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnInfoListener(mInfoListener);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mCurrentBufferPercentage = 0;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                mMediaPlayer.setDataSource(mAppContext, mUri, mHeaders);
            } else {
                mMediaPlayer.setDataSource(mUri.toString());
            }
            bindSurfaceHolder(mMediaPlayer, mSurfaceHolder);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.prepareAsync();


            // REMOVED: mPendingSubtitleTracks

            // we don't set the target state here either, but preserve the
            // target state that was there before.
            mCurrentPlayState = STATE_PREPARING;
            attachMediaController();
        } catch (IOException ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
            mCurrentPlayState = STATE_ERROR;
            mTargetPlayState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
            mCurrentPlayState = STATE_ERROR;
            mTargetPlayState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        } finally {
            // REMOVED: mPendingSubtitleTracks.clear();
        }
    }

    public IMediaPlayer createPlayer(int playerType) {
        IMediaPlayer mediaPlayer = null;
        IjkMediaPlayer ijkMediaPlayer = null;
        if (mUri != null) {
            ijkMediaPlayer = new IjkMediaPlayer();
            ijkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_ERROR);
//            ijkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);

            if (mSettings.getUsingMediaCodec()) {
                Log.d(TAG, "硬件加速已开启");
                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
                if (mSettings.getUsingMediaCodecAutoRotate()) {
                    ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
                } else {
                    ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 0);
                }
            } else {
                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0);
            }

            if (mSettings.getUsingOpenSLES()) {
                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 1);
            } else {
                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 0);
            }

            String pixelFormat = mSettings.getPixelFormat();
            if (TextUtils.isEmpty(pixelFormat)) {
                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
            } else {
                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", pixelFormat);
            }
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);

            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);

            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
        }
        mediaPlayer = ijkMediaPlayer;
        if (mSettings.getEnableDetachedSurfaceTextureView()) {
            mediaPlayer = new TextureMediaPlayer(mediaPlayer);
        }

        return mediaPlayer;
    }

    private void bindSurfaceHolder(IMediaPlayer mp, IRenderView.ISurfaceHolder holder) {
        if (mp == null)
            return;

        if (holder == null) {
            mp.setDisplay(null);
            return;
        }

        holder.bindToMediaPlayer(mp);
    }

    public void toggleMediaControlsVisiblity() {

        Log.i(TAG, "mediacontroller is show:" + mMediaController.isShowing());
        if (mMediaController.isShowing()) {
            mMediaController.hide();
        } else {
            mMediaController.show(false);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i(TAG, "key code: " + keyCode);
        boolean isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK &&
                keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
                keyCode != KeyEvent.KEYCODE_VOLUME_DOWN &&
                keyCode != KeyEvent.KEYCODE_VOLUME_MUTE &&
                keyCode != KeyEvent.KEYCODE_MENU &&
                keyCode != KeyEvent.KEYCODE_CALL &&
                keyCode != KeyEvent.KEYCODE_ENDCALL;
        if (isInPlaybackState() && isKeyCodeSupported && mMediaController != null) {
            if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                    keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (mMediaPlayer.isPlaying()) {
                    pause();
                    mMediaController.show(false);
                } else {
                    start();
                    mMediaController.hide();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                if (!mMediaPlayer.isPlaying()) {
                    start();
                    mMediaController.hide();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                    || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                if (mMediaPlayer.isPlaying()) {
                    pause();
                    mMediaController.show(false);
                }
                return true;
            } else {
                toggleMediaControlsVisiblity();
            }
        }

        return super.onKeyDown(keyCode, event);
    }


    private int startX = 0;
    private int startY = 0;
    private int endX = 0;
    private int endY = 0;
    private long currentPos = 0;
    private int currentVolume = 0;
    private float currentBrightness = 0f;
    private int lengthX = 0;
    private int lengthY = 0;
    private boolean hasScroll = false;

    AudioManager audio = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);

    Window mWindow = ((Activity) getContext()).getWindow();
    WindowManager.LayoutParams mParams = null;


    public static final int FAST_REWIND_FORWORD = 0;
    //    public static final int FAST_FORWORD = 1;
    public static final int BRIGHTNESS_ADJUST = 2;
    public static final int VOLUME_ADJUST = 3;


    /**
     * 绑定MediaController
     */
    public void attachMediaController() {
        if (mMediaPlayer != null && mMediaController != null) {
            mMediaController.setMediaPlayer(this);
            mMediaController.setAnchorView(this);
            mMediaController.setEnabled(isInPlaybackState());
        }
    }

   /* @Override
    public boolean onTrackballEvent(MotionEvent event) {
        mMediaController.show(false);
        return false;
    }*/

    /*
        * release the media player in any state
        */
    public void release(boolean cleartargetstate) {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            // REMOVED: mPendingSubtitleTracks.clear();
            mCurrentPlayState = STATE_IDLE;
            if (cleartargetstate) {
                mTargetPlayState = STATE_IDLE;
            }
            AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
            am.abandonAudioFocus(null);
        }
    }

    private boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
                mCurrentPlayState != STATE_ERROR &&
                mCurrentPlayState != STATE_IDLE &&
                mCurrentPlayState != STATE_PREPARING);
    }

    public void setOnPreparedListener(IMediaPlayer.OnPreparedListener l) {
        mOnPreparedListener = l;
    }

    /**
     * Register a callback to be invoked when the end of a media file
     * has been reached during playback.
     *
     * @param l The callback that will be run
     */
    public void setOnCompletionListener(IMediaPlayer.OnCompletionListener l) {
        mOnCompletionListener = l;
    }

    /**
     * Register a callback to be invoked when an error occurs
     * during playback or setup.  If no listener is specified,
     * or if the listener returned false, VideoView will inform
     * the user of any errors.
     *
     * @param l The callback that will be run
     */
    public void setOnErrorListener(IMediaPlayer.OnErrorListener l) {
        mOnErrorListener = l;
    }

    /**
     * Register a callback to be invoked when an informational event
     * occurs during playback or setup.
     *
     * @param l The callback that will be run
     */
    public void setOnInfoListener(IMediaPlayer.OnInfoListener l) {
        mOnInfoListener = l;
    }


    @Override
    public void start() {
        if (mMediaPlayer != null) {
            mMediaPlayer.start();
            Log.i(TAG, "start()-->play state:" + mMediaPlayer.isPlaying());
            mCurrentPlayState = STATE_PLAYING;
        }
        mTargetPlayState = STATE_PLAYING;
        mMediaController.notifyPlayStateChange(true);
    }

    @Override
    public void pause() {

        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentPlayState = STATE_PAUSED;
            }
        }
        mTargetPlayState = STATE_PAUSED;
        mMediaController.notifyPlayStateChange(false);
    }

    @Override
    public int getDuration() {
        if (isInPlaybackState()) {
            return (int) mMediaPlayer.getDuration();
        }

        return -1;
    }

    @Override
    public int getBufferPercentage() {
        if (mMediaPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }

    @Override
    public int getCurrentPosition() {
//        Log.i(TAG,"MiniVideoPlayer position:"+mMediaPlayer.getCurrentPosition());

        if (isInPlaybackState()) {
            return (int) mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public void seekTo(long position) {

        if (isInPlaybackState()) {
            mMediaPlayer.seekTo(position);
            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = position;
        }
    }

    @Override
    public boolean isPlaying() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.isPlaying();
        }
        return false;
    }

    @Override
    public void stop() {

        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;

            mCurrentPlayState = STATE_IDLE;
            mTargetPlayState = STATE_IDLE;
            AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
            am.abandonAudioFocus(null);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        VelocityTracker velocityTracker = VelocityTracker.obtain();
        velocityTracker.addMovement(event);
        centerX = (int) (getX() + getWidth() / 2);
        this.mDetector.onTouchEvent(event);

        mParams = mWindow.getAttributes();


        int action = MotionEventCompat.getActionMasked(event);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                hasScroll = true;
                break;
            case MotionEvent.ACTION_UP:

                endGesture();
                if (mMediaPlayer != null && hasScroll && Math.abs(lengthX) > Math.abs(lengthY)
                        && IS_FULL_SCREEN == true && mMediaController.isScreenLocked() == false &&
                        Math.abs(lengthX) > ViewConfiguration.get(mContext).getScaledTouchSlop()) {
                    Log.i(TAG, "on touch length:" + lengthX);
                    mMediaPlayer.seekTo(mMediaPlayer.getCurrentPosition() - 100 * lengthX);
                }

                break;
        }

        return true;
    }

    public class MiniOnGestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final String TAG = "MiniOnGestureListener";
//        private float moveLength;

        public MiniOnGestureListener() {
            super();
        }


        /**
         * 记录当前亮度和音量
         *
         * @param e
         * @return
         */
        @Override
        public boolean onDown(MotionEvent e) {
//            moveLength = 0;
            Log.i(TAG, "onDown---->");
            if (mMediaPlayer != null) {
                currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
                if (firstGetBrightnessFlag) {
                    currentBrightness = new ScreenUtil(mContext).firstGetBrightness();
                    firstGetBrightnessFlag = false;
                } else {
                    currentBrightness = mParams.screenBrightness;
                }
                currentPos = mMediaPlayer.getCurrentPosition();
                startX = (int) e.getX();
                startY = (int) e.getY();
                hasScroll = false;
            }

            lengthX = 0;
            lengthY = 0;
            return true;
        }


        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.i(TAG, "onSingleTapConfirmed--->");
            toggleMediaControlsVisiblity();
            return true;
        }

        /**
         * @param e1
         * @param e2
         * @param distanceX distancex>0向左，distancex<0向右
         * @param distanceY distancey>0向上，distancex<0向下
         * @return
         */
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            lengthX = (int) (e1.getX() - e2.getX());
            lengthY = (int) (e1.getY() - e2.getY());

            Log.i(TAG, "event1:(" + e1.getX() + "," + e1.getY() + ") 共 " + e1.getPointerCount() + "个点");
            Log.i(TAG, "event2:(" + e2.getX() + "," + e2.getY() + ") 共 " + e2.getPointerCount() + "个点");
            Log.i(TAG, "distance2:(" + distanceX + "," + distanceY + ")");

//            moveLength += distanceY;
//            Log.i(TAG, "moveLength:" + moveLength);

            /**
             * 在y轴上的移动距离大于在x轴上的移动距离，
             * 并且不是从屏幕最顶部开始（呼出通知栏），
             * 并且是全屏状态
             * 并且屏幕未锁定
             */
//            Log.i(TAG, "ViewConfiguration.get(mContext).getScaledTouchSlop()" +
//                    ViewConfiguration.get(mContext).getScaledTouchSlop());
            if (ViewConfiguration.get(mContext).getScaledTouchSlop() > 0
                    && Math.abs(lengthX) < Math.abs(lengthY) && startX > 0
                    && IS_FULL_SCREEN == true &&
                    mMediaController.isScreenLocked() == false
                    && e1.getPointerCount() < 2
                    && e2.getPointerCount() < 2
                    ) {
//
                if (startX < centerX) {
                    //亮度调节
                    setBrightness(distanceY);

                } else {
                    //声音调节
                    setVolume(lengthY);
                }
            }

            hasScroll = true;
            return super.onScroll(e1, e2, distanceX, distanceY);
//            return true;
        }

    }

    /**
     * 亮度调节
     *
     * @param distanceY
     */
    private void setBrightness(float distanceY) {

        float f = distanceY / getHeight();
        currentBrightness = currentBrightness + f;
        Log.i(TAG, "brightness :" + currentBrightness);

        if (currentBrightness < 0) {
            currentBrightness = 0;
        } else if (currentBrightness > 1) {
            currentBrightness = 1;
        }
        mParams.screenBrightness = currentBrightness;

        mWindow.setAttributes(mParams);

        volumeAndBrightnessView.show(VolumeAndBrightnessView.BRIGHTNESS,
                (int) (100 * currentBrightness));
    }

    /**
     * 声音调节
     *
     * @param lengthY
     */
    private void setVolume(float lengthY) {
        float f = lengthY / getHeight() * (float) mMaxVolume;
//        Log.i(TAG, "f:" + f);
        int volume = currentVolume;
    /*    if (distanceY > 0 && f < 1) {
            currentVolume++;
        } else if (distanceY < 0 && f > -1) {
            currentVolume--;
        } else {*/
        volume += f;
//        }
        if (volume > mMaxVolume) {
            volume = mMaxVolume;
        }
        if (volume < 0) {
            volume = 0;
        }

//        Log.i(TAG, "lengthY:" + lengthY);
//        Log.i(TAG, "音量为:" + volume);
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
//        audio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);
        volumeAndBrightnessView.show(VolumeAndBrightnessView.VOLUME,
                (int) (100 * volume / mMaxVolume));
    }

    private void endGesture() {
        volumeAndBrightnessView.hide();
    }


    @Override
    public void setFullScreen(boolean fullScreen) {

        IS_FULL_SCREEN = fullScreen;
        if (onFullScreenChangeListener != null) {
            onFullScreenChangeListener.onFullScreenChange(IS_FULL_SCREEN);
        }


        Log.i(TAG, "setFullScreen-->start:正在播放：" + isPlaying());


//        if (isPlaying() == false) {//当, m视频停止播放，即播放按钮显示的时候，重设播放按钮
        mMediaController.resetPlayBtn(this);
//        }

        ((MiniMediaController) mMediaController).setFullScreenBtnEnable(!IS_FULL_SCREEN);
        Activity activity = ((Activity) getContext());


        int visiable = SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | SYSTEM_UI_FLAG_FULLSCREEN | SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        /**
         * 设置标题栏和MiniVideo尺寸
         */
        if (IS_FULL_SCREEN == true) {
            setSystemUiVisibility(visiable);
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            setSystemUiVisibility(0);
//            activity.findViewById(android.R.id.content).setSystemUiVisibility(0);
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        /**
         * 设置VideoView
         */
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        ViewGroup.LayoutParams params = getLayoutParams();
//        params.width = metrics.widthPixels;
        params.width = LayoutParams.MATCH_PARENT;
        Log.i(TAG, "metrics.widthPixels:" + metrics.widthPixels);
        if (fullScreen == true) {
            params.height = metrics.heightPixels;
        } else {
            params.height = LayoutParams.WRAP_CONTENT;
        }
        Log.i(TAG, "video width:" + mVideoWidth + " height:" + mVideoHeight);
        Log.i(TAG, "videoview width:" + params.width + " height:" + params.height);
        setLayoutParams(params);
//        Log.i(TAG, "setFullScreen-->end:正在播放：" + isPlaying());

//        attachMediaController();
//        if (mMediaPlayer != null) {
//            mMediaController.show(true);
//        }
//        mMediaController.hide();

        mMediaController.setAnchorView(this);//重新加载layout-land或者layout中的MediaController布局

        Log.w(TAG, "height:" + ((MiniMediaController) mMediaController).getHeight());
    }

    @Override
    public boolean isFullScreen() {
        return IS_FULL_SCREEN;
    }

    public interface OnFullScreenChangeListener {
        void onFullScreenChange(boolean isFullScreen);
    }

}
