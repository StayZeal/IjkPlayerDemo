package co.gofun.customijk.view;


import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import cn.weidoo.customijk.R;


/**
 * 播放器标题栏
 * Created by yangfeng on 2016/3/30.
 */

public class MiniMediaTitleBar extends FrameLayout {

    private View mRoot;

    private MiniMediaController.MediaPlayerControl mPlayer;

    private View playerBack;

    private TextView title;
    private ImageView vipImage;
    private ImageView menuImage;
    private ImageView changeSceneImage;

    private View mAnchor;

    /**
     * 设置返回键点击事件
     *
     * @param listener
     */
    public void setBackOnClickListener(OnClickListener listener) {
        playerBack.setOnClickListener(listener);
    }


    public void setTitleText(String titleText) {
        title.setText(titleText + "");
    }

    public void setVipClickListener(OnClickListener listener) {
        vipImage.setOnClickListener(listener);
    }

    public void setMenuClickListener(OnClickListener listener) {
        menuImage.setOnClickListener(listener);
    }

    public void setChangeSceneClickListener(OnClickListener listener) {
        changeSceneImage.setOnClickListener(listener);
    }


    public MiniMediaTitleBar(Context context) {
        super(context);
        init();
    }

    public MiniMediaTitleBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MiniMediaTitleBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MiniMediaTitleBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }


    public void setMediaPlayer(MiniMediaController.MediaPlayerControl player) {
        mPlayer = player;
    }

    public void setAnchorView(ViewGroup view) {

        mAnchor = view;
        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.TOP);

        removeAllViews();
        View v = makeTitleView();
        addView(v, frameParams);
    }

    private void init() {
        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);

        removeAllViews();
        View v = makeTitleView();
        addView(v, frameParams);
    }


    private View makeTitleView() {

        mRoot = LayoutInflater.from(getContext()).inflate(R.layout.mini_media_title_bar, null);

        playerBack = mRoot.findViewById(R.id.mini_player_back);

        title = (TextView) mRoot.findViewById(R.id.mini_player_title);
        vipImage = (ImageView) mRoot.findViewById(R.id.mini_player_vip);
        menuImage = (ImageView) mRoot.findViewById(R.id.mini_player_menu);
        changeSceneImage = (ImageView) mRoot.findViewById(R.id.mini_player_change_scene);

        return mRoot;
    }

    /**
     * 设置是否显示开通会员
     *
     * @param show
     * @deprecated use rightView(int type)
     */
    public void showVipView(boolean show) {
        if (show == true) {
            vipImage.setVisibility(View.VISIBLE);
            menuImage.setVisibility(View.GONE);
        } else {
            vipImage.setVisibility(View.GONE);
            menuImage.setVisibility(View.VISIBLE);
        }
    }

    /**
     * @param show
     * @deprecated use rightView(int type)
     */
    public void showMenuView(boolean show) {
        if (show == true) {
            menuImage.setVisibility(View.VISIBLE);
        } else {
            menuImage.setVisibility(View.GONE);
        }
    }

    /**
     * 右侧不显示图标
     */
    public static final int NO_RITHT_VIEW = 0;
    /**
     * 右侧显示VIP图标
     */
    public static final int VIP_RITHT_VIEW = 1;
    /**
     * 右侧显示菜单图标
     */
    public static final int MENU_RITHT_VIEW = 2;
    /**
     * 右侧显示切换镜头图标
     */
    public static final int CHANGE_SENCE_RITHT_VIEW = 3;

    public void rightView(int type) {
        switch (type) {
            case NO_RITHT_VIEW:
                vipImage.setVisibility(GONE);
                menuImage.setVisibility(GONE);
                changeSceneImage.setVisibility(GONE);
                break;
            case VIP_RITHT_VIEW:
                vipImage.setVisibility(VISIBLE);
                menuImage.setVisibility(GONE);
                changeSceneImage.setVisibility(GONE);
                break;
            case MENU_RITHT_VIEW:
                vipImage.setVisibility(GONE);
                menuImage.setVisibility(VISIBLE);
                changeSceneImage.setVisibility(GONE);
                break;
            case CHANGE_SENCE_RITHT_VIEW:
                vipImage.setVisibility(GONE);
                menuImage.setVisibility(GONE);
                changeSceneImage.setVisibility(VISIBLE);
                break;
        }
    }


}
