package tencent.liteav.demo.superplayer;

import static common.callback.VideoInteractiveParam.param;
import static common.constants.Constants.VIDEOTAG;
import static common.constants.Constants.success_code;
import static common.constants.Constants.token_error;
import static tencent.liteav.demo.superplayer.SuperPlayerDef.Orientation.LANDSCAPE;
import static tencent.liteav.demo.superplayer.SuperPlayerDef.Orientation.LANDSCAPE_REVERSE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.Nullable;

import com.example.zhouwei.library.CustomPopWindow;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.cache.CacheMode;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.szrm.videolibraries.R;
import com.tencent.liteav.basic.log.TXCLog;
import com.tencent.rtmp.TXLivePlayer;
import com.tencent.rtmp.TXVodPlayer;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.wdcs.model.PlayImageSpriteInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import common.constants.Constants;
import common.http.ApiConstants;
import common.manager.FinderBuriedPointManager;
import common.model.BuriedPointModel;
import common.model.ContentStateModel;
import common.model.PlayKeyFrameDescInfo;
import common.model.VideoQuality;
import common.utils.ButtonSpan;
import common.utils.DebugLogUtils;
import common.utils.NumberFormatTool;
import common.utils.PersonInfoManager;
import common.utils.ToastUtils;
import tencent.liteav.demo.superplayer.SuperPlayerDef;
import tencent.liteav.demo.superplayer.SuperPlayerGlobalConfig;
import tencent.liteav.demo.superplayer.SuperPlayerModel;
import tencent.liteav.demo.superplayer.model.SuperPlayer;
import tencent.liteav.demo.superplayer.model.SuperPlayerImpl;
import tencent.liteav.demo.superplayer.model.SuperPlayerObserver;
import tencent.liteav.demo.superplayer.model.net.LogReport;
import tencent.liteav.demo.superplayer.model.utils.NetWatcher;
import tencent.liteav.demo.superplayer.model.utils.SystemUtils;
import tencent.liteav.demo.superplayer.ui.player.FloatPlayer;
import tencent.liteav.demo.superplayer.ui.player.FullScreenPlayer;
import tencent.liteav.demo.superplayer.ui.player.OrientationHelper;
import tencent.liteav.demo.superplayer.ui.player.Player;
import tencent.liteav.demo.superplayer.ui.player.WindowPlayer;
import tencent.liteav.demo.superplayer.ui.view.DanmuView;


/**
 * ???????????????view
 * <p>
 * ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
 * ??????????????????????????????????????????????????????????????????????????????????????????{@link #playWithModel(SuperPlayerModel)}??????{@link SuperPlayerModel}????????????????????????
 * <p>
 * 1???????????????{@link #playWithModel(SuperPlayerModel)}
 * 2???????????????{@link #setPlayerViewCallback(OnSuperPlayerViewCallback)}
 * 3???controller????????????{@link #mControllerCallback}
 * 4???????????????????????????{@link #resetPlayer()}
 */
public class SuperPlayerView extends RelativeLayout implements OrientationHelper.OnOrientationChangeListener,
        View.OnClickListener {
    private static final String TAG = "SuperPlayerView";

    private final int OP_SYSTEM_ALERT_WINDOW = 24;                      // ??????TYPE_TOAST??????????????????API??????

    private Context mContext;

    private ViewGroup mRootView;                                 // SuperPlayerView??????view
    private TXCloudVideoView mTXCloudVideoView;                         // ?????????????????????view
    public FullScreenPlayer mFullScreenPlayer;                         // ??????????????????view
    public WindowPlayer mWindowPlayer;                             // ??????????????????view
    private FloatPlayer mFloatPlayer;                              // ?????????????????????view
    private DanmuView mDanmuView;                                // ??????

    private ViewGroup.LayoutParams mLayoutParamWindowMode;          // ???????????????SuperPlayerView???????????????
    private ViewGroup.LayoutParams mLayoutParamFullScreenMode;      // ???????????????SuperPlayerView???????????????
    private LayoutParams mVodControllerWindowParams;      // ??????controller???????????????
    private LayoutParams mVodControllerFullScreenParams;  // ??????controller???????????????
    private WindowManager mWindowManager;                  // ????????????????????????
    private WindowManager.LayoutParams mWindowParams;                   // ?????????????????????

    private OnSuperPlayerViewCallback mPlayerViewCallback;              // SuperPlayerView??????
    public NetWatcher mWatcher;                         // ?????????????????????
    public SuperPlayer mSuperPlayer;
    public OrientationHelper mOrientationHelper;

    private SuperPlayerDef.Orientation mDesiredOrientation; // ?????????????????????
    private boolean sensorEnable = true;// ?????????????????????
    public static SuperPlayerDef.PlayerMode mTargetPlayerMode = SuperPlayerDef.PlayerMode.WINDOW; // ??????????????????
    private int fullScreenWidth = getResources().getDisplayMetrics().widthPixels;
    private int fullScreenHeight = getResources().getDisplayMetrics().heightPixels;
    private View decorView;
    @Nullable
    protected View mHideNavBarView;
    public ContentStateModel.DataDTO contentStateModel;
    private String mContentId;
    private String mVideoType;
    private CustomPopWindow noLoginTipsPop;
    private View noLoginTipsView;
    private TextView noLoginTipsCancel;
    private TextView noLoginTipsOk;
    public boolean isOrientation;
    private boolean isMainbool;
    public boolean detailIsLoad;//?????????????????????????????????
    public boolean homeVideoIsLoad;
    public BuriedPointModel buriedPointModel;
    public static SuperPlayerView instance;

    public static SuperPlayerView getInstance(Context context, View decorView, Boolean isMain) {
        if (instance == null) {
            synchronized (SuperPlayerView.class) {
                if (instance == null) {
                    instance = new SuperPlayerView(context, decorView, isMain);
                }
            }
        }
        return instance;
    }

    public SuperPlayerView(Context context, View decorView, Boolean isMain) {
        super(context);
        this.decorView = decorView;
        this.isMainbool = isMain;
        initialize(context);
    }


    private void initialize(Context context) {
        mContext = context;
//        addOnLayoutChangeListener();
        initView();
        initPlayer();
    }

    /**
     * ?????????view
     */
    private void initView() {
        mOrientationHelper = new OrientationHelper(getContext().getApplicationContext());
        mOrientationHelper.setOnOrientationChangeListener(this);
        mOrientationHelper.enable();
        mRootView = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.superplayer_vod_view, null);
        mTXCloudVideoView = (TXCloudVideoView) mRootView.findViewById(R.id.superplayer_cloud_video_view);
        mFullScreenPlayer = (FullScreenPlayer) mRootView.findViewById(R.id.superplayer_controller_large);

        mWindowPlayer = (WindowPlayer) mRootView.findViewById(R.id.superplayer_controller_small);
        mFloatPlayer = (FloatPlayer) mRootView.findViewById(R.id.superplayer_controller_float);
        mDanmuView = (DanmuView) mRootView.findViewById(R.id.superplayer_danmuku_view);
        if (isMainbool) {
            mWindowPlayer.mIvPause.setVisibility(GONE);
            mWindowPlayer.mIvFullScreen.setVisibility(GONE);
        }

        mVodControllerWindowParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mVodControllerFullScreenParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        mFullScreenPlayer.setCallback(mControllerCallback);
        mWindowPlayer.setCallback(mControllerCallback);
        mFloatPlayer.setCallback(mControllerCallback);

        removeAllViews();
        mRootView.removeView(mDanmuView);
        mRootView.removeView(mTXCloudVideoView);
        mRootView.removeView(mWindowPlayer);
        mRootView.removeView(mFullScreenPlayer);
        mRootView.removeView(mFloatPlayer);

        addView(mTXCloudVideoView);
        addView(mDanmuView);

        noLoginTipsView = View.inflate(mContext, R.layout.fullscreen_no_login_tips, null);
        noLoginTipsCancel = noLoginTipsView.findViewById(R.id.no_login_tips_cancel);
        noLoginTipsOk = noLoginTipsView.findViewById(R.id.no_login_tips_ok);
        noLoginTipsCancel.setOnClickListener(this);
        noLoginTipsOk.setOnClickListener(this);
        buriedPointModel = new BuriedPointModel();
    }

    private void initPlayer() {
        mSuperPlayer = new SuperPlayerImpl(mContext, mTXCloudVideoView, this);
        mSuperPlayer.setObserver(mSuperPlayerObserver);
        if (mSuperPlayer.getPlayerMode() == SuperPlayerDef.PlayerMode.FULLSCREEN) {
            addView(mFullScreenPlayer);
            mFullScreenPlayer.hide();
        } else if (mSuperPlayer.getPlayerMode() == SuperPlayerDef.PlayerMode.WINDOW) {
            addView(mWindowPlayer);
            mWindowPlayer.hide();
        }

        post(new Runnable() {
            @Override
            public void run() {
                if (mSuperPlayer.getPlayerMode() == SuperPlayerDef.PlayerMode.WINDOW) {
                    mLayoutParamWindowMode = getLayoutParams();
                }
                try {
                    // ????????????Parent???LayoutParam??????????????????????????????fullscreen????????????LayoutParam
                    Class parentLayoutParamClazz = getLayoutParams().getClass();
                    Constructor constructor = parentLayoutParamClazz.getDeclaredConstructor(int.class, int.class);
                    mLayoutParamFullScreenMode = (ViewGroup.LayoutParams) constructor.newInstance(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        LogReport.getInstance().setAppName(mContext);
        LogReport.getInstance().setPackageName(mContext);

        if (mWatcher == null) {
            mWatcher = new NetWatcher(mContext);
        }
    }

    /**
     * ????????????????????????
     */
    public void setContentStateModel(String contentId, String videoType) {
        this.mContentId = contentId;
        this.mVideoType = videoType;

        //?????????????????????????????????????????????
        if (contentStateModel.getWhetherFavor().equals("true")) {
            mFullScreenPlayer.mCollection.setImageResource(R.drawable.collection);
        } else {
            mFullScreenPlayer.mCollection.setImageResource(R.drawable.szrm_sdk_collection_icon);
        }

        if (contentStateModel.getWhetherLike().equals("true")) {
            mFullScreenPlayer.mLike.setImageResource(R.drawable.szrm_sdk_favourite_select);
        } else {
            mFullScreenPlayer.mLike.setImageResource(R.drawable.szrm_sdk_favourite);
        }

        mFullScreenPlayer.fullscreenLikeNum.setText(NumberFormatTool.formatNum(Long.parseLong(NumberFormatTool.getNumStr(contentStateModel.getLikeCountShow())), false));
        mFullScreenPlayer.fullscreenCollection.setText(NumberFormatTool.formatNum(Long.parseLong(NumberFormatTool.getNumStr(contentStateModel.getFavorCountShow())), false));

        //??????????????????
        mFullScreenPlayer.mLike.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                FinderBuriedPointManager.setFinderLikeFavoriteShare(Constants.CONTENT_LIKE, mFullScreenPlayer.item);
                if (TextUtils.isEmpty(PersonInfoManager.getInstance().getTransformationToken())) {
                    noLoginTipsPop();
                } else {
                    addOrCancelLike(mContentId, mVideoType);
                }
            }
        });

        //??????????????????
        mFullScreenPlayer.mCollection.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                FinderBuriedPointManager.setFinderLikeFavoriteShare(Constants.CONTENT_FAVORITE, mFullScreenPlayer.item);
                if (TextUtils.isEmpty(PersonInfoManager.getInstance().getTransformationToken())) {
                    noLoginTipsPop();
                } else {
                    addOrCancelFavor(mContentId, mVideoType);
                }
            }
        });
    }

    public ContentStateModel.DataDTO getContentStateModel() {
        return contentStateModel;
    }


    /**
     * ??????/????????????
     */
    private void addOrCancelLike(String targetId, String type) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("targetId", targetId);
            jsonObject.put("type", type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        OkGo.<String>post(ApiConstants.getInstance().addOrCancelLike())
                .tag(this)
                .headers("token", PersonInfoManager.getInstance().getTransformationToken())
                .upJson(jsonObject)
                .cacheMode(CacheMode.NO_CACHE)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        if (null == response.body()) {
                            ToastUtils.showShort(R.string.data_err);
                            return;
                        }

                        try {
                            JSONObject json = new JSONObject(response.body());
                            if (null != json && json.get("code").toString().equals("200")) {
                                if (json.get("data").toString().equals("1")) {
                                    int num;
                                    mFullScreenPlayer.mLike.setImageResource(R.drawable.szrm_sdk_favourite_select);
                                    if (TextUtils.isEmpty(mFullScreenPlayer.fullscreenLikeNum.getText().toString())) {
                                        num = 0;
                                    } else {
                                        if (NumberFormatTool.isNumeric(mFullScreenPlayer.fullscreenLikeNum.getText().toString())) {
                                            num = Integer.parseInt(mFullScreenPlayer.fullscreenLikeNum.getText().toString());
                                        } else {
                                            num = 0;
                                        }
                                    }
                                    num++;
                                    mFullScreenPlayer.fullscreenLikeNum.setText(NumberFormatTool.formatNum(num, false));
                                    contentStateModel.setWhetherLike("true");
                                    contentStateModel.setLikeCountShow(NumberFormatTool.formatNum(num, false).toString());
                                } else {
                                    int num;
                                    mFullScreenPlayer.mLike.setImageResource(R.drawable.szrm_sdk_favourite);
                                    if (TextUtils.isEmpty(mFullScreenPlayer.fullscreenLikeNum.getText().toString())) {
                                        num = 0;
                                    } else {
                                        if (NumberFormatTool.isNumeric(mFullScreenPlayer.fullscreenLikeNum.getText().toString())) {
                                            num = Integer.parseInt(mFullScreenPlayer.fullscreenLikeNum.getText().toString());
                                        } else {
                                            num = 0;
                                        }
                                    }
                                    if (num > 0) {
                                        num--;
                                    }
                                    mFullScreenPlayer.fullscreenLikeNum.setText(NumberFormatTool.formatNum(num, false));
                                    contentStateModel.setWhetherLike("false");
                                    contentStateModel.setLikeCountShow(NumberFormatTool.formatNum(num, false).toString());
                                }

                            } else if (json.get("code").toString().equals(token_error)) {
                                DebugLogUtils.DebugLog("???token,????????????");
                                try {
                                    param.toLogin();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                if (null != json.get("message").toString()) {
                                    ToastUtils.showShort(json.get("message").toString());
                                } else {
                                    ToastUtils.showShort("????????????");
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            ToastUtils.showShort("????????????");
                        }

                    }

                    @Override
                    public void onError(Response<String> response) {
                        ToastUtils.showShort("????????????");
                    }
                });
    }

    /**
     * ??????/????????????
     */
    private void addOrCancelFavor(String contentId, String type) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("contentId", contentId);
            jsonObject.put("type", type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        OkGo.<String>post(ApiConstants.getInstance().addOrCancelFavor())
                .tag(VIDEOTAG)
                .headers("token", PersonInfoManager.getInstance().getTransformationToken())
                .upJson(jsonObject)
                .cacheMode(CacheMode.NO_CACHE)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        if (null == response.body()) {
                            ToastUtils.showShort(R.string.data_err);
                            return;
                        }
                        try {
                            JSONObject json = new JSONObject(response.body());
                            if (json.get("code").toString().equals(success_code)) {

                                if (json.get("data").toString().equals("1")) {
                                    int num;
                                    num = Integer.parseInt(NumberFormatTool.getNumStr(mFullScreenPlayer.fullscreenCollection.getText().toString()));
                                    num++;
                                    mFullScreenPlayer.fullscreenCollection.setText(NumberFormatTool.formatNum(num, false));
                                    mFullScreenPlayer.mCollection.setImageResource(R.drawable.collection);
                                    contentStateModel.setWhetherFavor("true");
                                    contentStateModel.setFavorCountShow(NumberFormatTool.formatNum(num, false).toString());
                                } else {
                                    int num;
                                    num = Integer.parseInt(NumberFormatTool.getNumStr(mFullScreenPlayer.fullscreenCollection.getText().toString()));
                                    if (num > 0) {
                                        num--;
                                    }
                                    mFullScreenPlayer.fullscreenCollection.setText(NumberFormatTool.formatNum(num, false));
                                    mFullScreenPlayer.mCollection.setImageResource(R.drawable.szrm_sdk_collection_icon);
                                    contentStateModel.setWhetherFavor("false");
                                    contentStateModel.setFavorCountShow(NumberFormatTool.formatNum(num, false).toString());
                                }
                            } else if (json.get("code").toString().equals(token_error)) {
                                DebugLogUtils.DebugLog("???token ????????????");
                                try {
                                    param.toLogin();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                if (null != json.get("message").toString()) {
                                    ToastUtils.showShort(json.get("message").toString());
                                } else {
                                    ToastUtils.showShort("????????????");
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            ToastUtils.showShort("????????????");
                        }

                    }

                    @Override
                    public void onError(Response<String> response) {
                        ToastUtils.showShort("????????????");
                    }
                });
    }

    /**
     * ????????????
     *
     * @param model
     */
    public void playWithModel(final SuperPlayerModel model) {
        if (model.videoId != null) {
            mSuperPlayer.play(model.appId, model.videoId.fileId, model.videoId.pSign);
        } else if (model.videoIdV2 != null) {
        } else if (model.multiURLs != null && !model.multiURLs.isEmpty()) {
            mSuperPlayer.play(model.appId, model.multiURLs, model.playDefaultIndex);
        } else {
//            if (null != mWindowPlayer.getDataDTO()) {
//                String jsonString = BuriedPointModelManager.getVideoStartPlay("", "false", mWindowPlayer.getDataDTO().getId() + "", mWindowPlayer.getDataDTO().getTitle(),
//                        "", "", "", "", Constants.CONTENT_TYPE, mWindowPlayer.getDataDTO().getIssueTimeStamp());
//                Log.e("??????", "???????????????????????????---" + jsonString);
//            }
            mSuperPlayer.play(model);
        }
    }

    /**
     * ????????????
     *
     * @param appId ???????????????appId
     * @param url   ??????????????????
     */
    public void play(int appId, String url) {
        mSuperPlayer.play(appId, url);
    }

    /**
     * ????????????
     *
     * @param appId  ???????????????appId
     * @param fileId ???????????????fileId
     * @param psign  ??????????????????????????????????????????????????????????????????????????????
     */
    public void play(int appId, String fileId, String psign) {
        mSuperPlayer.play(appId, fileId, psign);
    }

    /**
     * ??????????????????
     *
     * @param appId           ???????????????appId
     * @param superPlayerURLS ?????????????????????
     * @param defaultIndex    ????????????Index
     */
    public void play(int appId, List<SuperPlayerModel.SuperPlayerURL> superPlayerURLS, int defaultIndex) {
        mSuperPlayer.play(appId, superPlayerURLS, defaultIndex);
    }

    /**
     * ????????????
     *
     * @param title ????????????
     */
    private void updateTitle(String title) {
        mWindowPlayer.updateTitle(title);
        mFullScreenPlayer.updateTitle(title);
    }

    /**
     * resume??????????????????
     */
    public void onResume() {
        if (mDanmuView != null && mDanmuView.isPrepared() && mDanmuView.isPaused()) {
            mDanmuView.resume();
        }
        mSuperPlayer.resume();
    }

    /**
     * ???????????????
     */
    public void resetPlayer() {
        if (mDanmuView != null) {
            mDanmuView.release();
            mDanmuView = null;
        }
        stopPlay();
    }

    /**
     * ????????????
     */
    public void stopPlay() {
        mSuperPlayer.stop();
        if (mWatcher != null) {
            mWatcher.stop();
        }
    }

    /**
     * ??????????????????????????????
     *
     * @param callback
     */
    public void setPlayerViewCallback(OnSuperPlayerViewCallback callback) {
        mPlayerViewCallback = callback;
    }

    /**
     * ????????????????????????
     */
    private void fullScreen(boolean isFull) {
        if (getContext() instanceof Activity) {
            Activity activity = (Activity) getContext();
            if (isFull) {
                //?????????????????????????????????
                if (decorView == null) return;
                if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
                    decorView.setSystemUiVisibility(View.GONE);
                } else if (Build.VERSION.SDK_INT >= 19) {

                }
//                ((Activity) mContext).getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                if (decorView == null) return;
                if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
                    decorView.setSystemUiVisibility(View.VISIBLE);
                } else if (Build.VERSION.SDK_INT >= 19) {
                    decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                }
//                ((Activity) mContext).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }
    }

    public PlayModeCallBack playModeCallBack;

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.no_login_tips_cancel) {
            if (null != noLoginTipsPop) {
                noLoginTipsPop.dissmiss();
            }
        } else if (id == R.id.no_login_tips_ok) {
            if (null != noLoginTipsPop) {
                noLoginTipsPop.dissmiss();
            }
            try {
                param.toLogin();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * ????????????????????? ???????????????????????? ????????????????????????
     */
    private void noLoginTipsPop() {
        if (null == noLoginTipsPop) {
            noLoginTipsPop = new CustomPopWindow.PopupWindowBuilder(mContext)
                    .setView(noLoginTipsView)
                    .setOutsideTouchable(true)
                    .setFocusable(false)
                    .setAnimationStyle(R.style.AnimCenter)
                    .size(getResources().getDisplayMetrics().widthPixels / 2, ButtonSpan.dip2px(150))
                    .create()
                    .showAtLocation(mRootView, Gravity.CENTER, 0, 0);
        } else {
            noLoginTipsPop.showAtLocation(mRootView, Gravity.CENTER, 0, 0);
        }
    }

    public interface PlayModeCallBack {
        void getPlayMode(SuperPlayerDef.PlayerMode playerMode);
    }

    /**
     * ?????????controller??????
     */
    public Player.Callback mControllerCallback = new Player.Callback() {
        @SuppressLint("WrongConstant")
        @Override
        public void onSwitchPlayMode(SuperPlayerDef.PlayerMode playerMode) {
            try {
                playModeCallBack.getPlayMode(playerMode);
                mWindowPlayer.getLayoutParams();
                if (mSuperPlayer.getPlayerMode() == SuperPlayerDef.PlayerMode.FULLSCREEN && playerMode == SuperPlayerDef.PlayerMode.WINDOW) {
                    // ???????????????
                    sensorEnable = true;
                    mTargetPlayerMode = SuperPlayerDef.PlayerMode.WINDOW;
                    showBottomUIMenu();
                } else if (mSuperPlayer.getPlayerMode() == SuperPlayerDef.PlayerMode.WINDOW && playerMode == SuperPlayerDef.PlayerMode.FULLSCREEN) {
                    // ???????????????
                    sensorEnable = false;
                    mTargetPlayerMode = SuperPlayerDef.PlayerMode.FULLSCREEN;
                }
                if (playerMode == SuperPlayerDef.PlayerMode.FULLSCREEN) {
                    fullScreen(true);
                } else {
                    fullScreen(false);
                }
                mFullScreenPlayer.hide();
                mWindowPlayer.hide();
                mFloatPlayer.hide();
                //??????????????????
                if (playerMode == SuperPlayerDef.PlayerMode.FULLSCREEN) {
                    mOrientationHelper.disable();
                    if (mLayoutParamFullScreenMode == null) {
                        return;
                    }
                    removeView(mWindowPlayer);
                    if (null != mFullScreenPlayer && null != mFullScreenPlayer.getParent()) {
                        ((ViewGroup) mFullScreenPlayer.getParent()).removeView(mFullScreenPlayer);
                    }
                    addView(mFullScreenPlayer, mVodControllerFullScreenParams);
                    setLayoutParams(mLayoutParamFullScreenMode);
                    //???????????????????????????????????????
                    if (mDesiredOrientation == LANDSCAPE_REVERSE) {
                        rotateScreenOrientation(LANDSCAPE_REVERSE);
                    } else {
                        rotateScreenOrientation(LANDSCAPE);
                    }
                    if (mPlayerViewCallback != null) {
                        mPlayerViewCallback.onStartFullScreenPlay();
                    }
                } else if (playerMode == SuperPlayerDef.PlayerMode.WINDOW) {// ??????????????????
                    mOrientationHelper.disable();
                    // ??????????????????
                    if (mSuperPlayer.getPlayerMode() == SuperPlayerDef.PlayerMode.FLOAT) {
                        Context viewContext = getContext();
                        Intent intent = null;
                        if (viewContext instanceof Activity) {
                            intent = new Intent(viewContext, viewContext.getClass());
                        } else {
                            showToast(R.string.superplayer_float_play_fail);
                            return;
                        }
                        mContext.startActivity(intent);
                        mSuperPlayer.pause();
                        if (mLayoutParamWindowMode == null) {
                            return;
                        }
                        mWindowManager.removeView(mFloatPlayer);
                        mSuperPlayer.setPlayerView(mTXCloudVideoView);
                        mSuperPlayer.resume();
                    } else if (mSuperPlayer.getPlayerMode() == SuperPlayerDef.PlayerMode.FULLSCREEN) { // ?????????????????????
                        if (mLayoutParamWindowMode == null) {
                            return;
                        }
                        removeView(mFullScreenPlayer);
                        if (null != mWindowPlayer && null != mWindowPlayer.getParent()) {
                            ((ViewGroup) mWindowPlayer.getParent()).removeView(mWindowPlayer);
                        }
                        addView(mWindowPlayer, mVodControllerWindowParams);
                        setLayoutParams(mLayoutParamWindowMode);

                        rotateScreenOrientation(SuperPlayerDef.Orientation.PORTRAIT);
                        if (mPlayerViewCallback != null) {
                            mPlayerViewCallback.onStopFullScreenPlay();
                        }
                    }
                } else if (playerMode == SuperPlayerDef.PlayerMode.FLOAT) {//?????????????????????
                    TXCLog.i(TAG, "requestPlayMode Float :" + Build.MANUFACTURER);
                    SuperPlayerGlobalConfig prefs = SuperPlayerGlobalConfig.getInstance();
                    if (!prefs.enableFloatWindow) {
                        return;
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 6.0???????????????????????????
                        if (!Settings.canDrawOverlays(mContext)) {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                            intent.setData(Uri.parse("package:" + mContext.getPackageName()));
                            mContext.startActivity(intent);
                            return;
                        }
                    } else {
                        if (!checkOp(mContext, OP_SYSTEM_ALERT_WINDOW)) {
                            showToast(R.string.superplayer_enter_setting_fail);
                            return;
                        }
                    }
                    mSuperPlayer.pause();

                    mWindowManager = (WindowManager) mContext.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
                    mWindowParams = new WindowManager.LayoutParams();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mWindowParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                    } else {
                        mWindowParams.type = WindowManager.LayoutParams.TYPE_PHONE;
                    }
                    mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                    mWindowParams.format = PixelFormat.TRANSLUCENT;
                    mWindowParams.gravity = Gravity.LEFT | Gravity.TOP;

                    SuperPlayerGlobalConfig.TXRect rect = prefs.floatViewRect;
                    mWindowParams.x = rect.x;
                    mWindowParams.y = rect.y;
                    mWindowParams.width = rect.width;
                    mWindowParams.height = rect.height;
                    try {
                        if (null != mFloatPlayer && null != mFloatPlayer.getParent()) {
                            ((ViewGroup) mFloatPlayer.getParent()).removeView(mFloatPlayer);
                        }
                        mWindowManager.addView(mFloatPlayer, mWindowParams);
                    } catch (Exception e) {
                        showToast(R.string.superplayer_float_play_fail);
                        return;
                    }

                    TXCloudVideoView videoView = mFloatPlayer.getFloatVideoView();
                    if (videoView != null) {
                        mSuperPlayer.setPlayerView(videoView);
                        mSuperPlayer.resume();
                    }
                    // ???????????????
                    LogReport.getInstance().uploadLogs(LogReport.ELK_ACTION_FLOATMOE, 0, 0);
                }
                mSuperPlayer.switchPlayMode(playerMode);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * ???????????????
         * ???????????????bar ???????????????
         */
        protected void showBottomUIMenu() {
            // must be executed in main thread :)
            decorView.setSystemUiVisibility(0);
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        }

        @Override
        public void onBackPressed(SuperPlayerDef.PlayerMode playMode) {
            switch (playMode) {
                case FULLSCREEN:// ???????????????????????????????????????????????????
                    onSwitchPlayMode(SuperPlayerDef.PlayerMode.WINDOW);
                    break;
                case WINDOW:// ?????????????????????????????????????????????
                    if (mPlayerViewCallback != null) {
                        mPlayerViewCallback.onClickSmallReturnBtn();
                    }
                    break;
                case FLOAT:// ???????????????????????????
                    mWindowManager.removeView(mFloatPlayer);
                    if (mPlayerViewCallback != null) {
                        mPlayerViewCallback.onClickFloatCloseBtn();
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onFloatPositionChange(int x, int y) {
            mWindowParams.x = x;
            mWindowParams.y = y;
            mWindowManager.updateViewLayout(mFloatPlayer, mWindowParams);
        }

        @Override
        public void onPause() {
            mSuperPlayer.pause();
            if (mSuperPlayer.getPlayerType() != SuperPlayerDef.PlayerType.VOD) {
//                if (mWatcher != null) {
//                    mWatcher.stop();
//                }
            }
        }

        @Override
        public void onResume() {
            if (mSuperPlayer.getPlayerState() == SuperPlayerDef.PlayerState.END) { //??????
                mSuperPlayer.reStart();
                buriedPointModel.setIs_renew("true");
//                if (null != mWindowPlayer.getDataDTO()) {
//                    String jsonString = BuriedPointModelManager.getVideoStartPlay("", "true", mWindowPlayer.getDataDTO().getId() + "", mWindowPlayer.getDataDTO().getTitle(),
//                            "", "", "", "", Constants.CONTENT_TYPE, mWindowPlayer.getDataDTO().getIssueTimeStamp());
//                    Log.e("??????", "?????????????????????????????????---" + jsonString);
//                }
            } else if (mSuperPlayer.getPlayerState() == SuperPlayerDef.PlayerState.PAUSE) { //????????????
                mSuperPlayer.resume();
            }
        }

        @Override
        public void onSeekTo(int position) {
            mSuperPlayer.seek(position);
        }

        @Override
        public void onResumeLive() {
            mSuperPlayer.resumeLive();
        }

        @Override
        public void onDanmuToggle(boolean isOpen) {
            if (mDanmuView != null) {
                mDanmuView.toggle(isOpen);
            }
        }

        @Override
        public void onSnapshot() {
            mSuperPlayer.snapshot(new TXLivePlayer.ITXSnapshotListener() {
                @Override
                public void onSnapshot(Bitmap bitmap) {
                    if (bitmap != null) {
                        showSnapshotWindow(bitmap);
                    } else {
                        showToast(R.string.superplayer_screenshot_fail);
                    }
                }
            });
        }

        @Override
        public void onQualityChange(VideoQuality quality) {
            mFullScreenPlayer.updateVideoQuality(quality);
            mSuperPlayer.switchStream(quality);
        }

        @Override
        public void onSpeedChange(float speedLevel) {
            mSuperPlayer.setRate(speedLevel);
        }

        @Override
        public void onMirrorToggle(boolean isMirror) {
            mSuperPlayer.setMirror(isMirror);
        }

        @Override
        public void onHWAccelerationToggle(boolean isAccelerate) {
            mSuperPlayer.enableHardwareDecode(isAccelerate);
        }
    };

    /**
     * ??????????????????
     *
     * @param bmp
     */
    private void showSnapshotWindow(final Bitmap bmp) {
        final PopupWindow popupWindow = new PopupWindow(mContext);
        popupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        View view = LayoutInflater.from(mContext).inflate(R.layout.superplayer_layout_new_vod_snap, null);
        ImageView imageView = (ImageView) view.findViewById(R.id.superplayer_iv_snap);
        imageView.setImageBitmap(bmp);
        popupWindow.setContentView(view);
        popupWindow.setOutsideTouchable(true);
        popupWindow.showAtLocation(mRootView, Gravity.TOP, 1800, 300);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                save2MediaStore(mContext, bmp);
            }
        });
        postDelayed(new Runnable() {
            @Override
            public void run() {
                popupWindow.dismiss();
            }
        }, 3000);
    }

    /**
     * ??????????????????
     *
     * @param orientation
     */
    public void rotateScreenOrientation(SuperPlayerDef.Orientation orientation) {
        switch (orientation) {
            case PORTRAIT:
                ((Activity) mContext).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

                if (mControllerCallback != null && mTargetPlayerMode == SuperPlayerDef.PlayerMode.FULLSCREEN) {
                    mControllerCallback.onSwitchPlayMode(SuperPlayerDef.PlayerMode.WINDOW);
                }
                break;
            case LANDSCAPE:
                if (((Activity) mContext).getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    return;
                }
                ((Activity) mContext).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                if (mTargetPlayerMode != SuperPlayerDef.PlayerMode.WINDOW) {
                    SystemUtils.hideSystemUI(decorView);
                }
                break;
            case LANDSCAPE_REVERSE:
                if (((Activity) mContext).getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                    return;
                }
                ((Activity) mContext).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                if (mTargetPlayerMode != SuperPlayerDef.PlayerMode.WINDOW) {
                    SystemUtils.hideSystemUI(decorView);
                }
                break;
        }
        mOrientationHelper.enable();
    }

    /**
     * ?????????????????????
     * <p>
     * API <18?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * API >= 19 ????????????????????????????????????
     * API >=23????????????manifest???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * API >25???TYPE_TOAST ?????????????????????????????????????????????????????????
     */
    private boolean checkOp(Context context, int op) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            AppOpsManager manager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            try {
                Method method = AppOpsManager.class.getDeclaredMethod("checkOp", int.class, int.class, String.class);
                return AppOpsManager.MODE_ALLOWED == (int) method.invoke(manager, op, Binder.getCallingUid(), context.getPackageName());
            } catch (Exception e) {
                TXCLog.e(TAG, Log.getStackTraceString(e));
            }
        }
        return true;
    }

    /**
     * ????????????????????????
     */
    public void setOrientation(boolean mIsOrientation) {
        this.isOrientation = mIsOrientation;
    }

//    private void addOnLayoutChangeListener() {
//        addOnLayoutChangeListener(new OnLayoutChangeListener() {
//            @Override
//            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
//            }
//        });
//    }

    @Override
    public void onOrientationChanged(int orientation) {

        if (!isOrientation) {
            return;
        }

        if (mFullScreenPlayer == null) {
            return;
        }

        if (mFullScreenPlayer.mLockScreen) {
            return;
        }

//        /**
//         * ????????????????????????  ????????????
//         */
//        if (null != mFullScreenPlayer.popupWindow && mFullScreenPlayer.popupWindow.getPopupWindow().isShowing()) {
//            return;
//        }

        /**
         * ????????????????????????  ????????????
         */
        if (null != mFullScreenPlayer.mVodMoreView && mFullScreenPlayer.mVodMoreView.getVisibility() == VISIBLE) {
            return;
        }

        if (null != mFullScreenPlayer.popupWindow && mFullScreenPlayer.popupWindow.isShowing()) {
            return;
        }

        if (null != mFullScreenPlayer.sharePop && mFullScreenPlayer.sharePop.getPopupWindow().isShowing()) {
            return;
        }

        if (null != noLoginTipsPop && noLoginTipsPop.getPopupWindow().isShowing()) {
            return;
        }


        // ???????????????????????????????????????
        if (!sensorEnable) {

            if (mTargetPlayerMode == SuperPlayerDef.PlayerMode.WINDOW) {
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    sensorEnable = true;
                }
            } else if (mTargetPlayerMode == SuperPlayerDef.PlayerMode.FULLSCREEN) {
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    sensorEnable = true;
                }
            }
            return;
        }
        if (orientation >= 350 || orientation <= 10) { //??????????????????
            if (getPlayerMode() == SuperPlayerDef.PlayerMode.WINDOW) {
                // ?????????????????????
//                switchPlayMode(SuperPlayerDef.PlayerMode.WINDOW);
                return;
            } else if (getPlayerMode() == SuperPlayerDef.PlayerMode.FULLSCREEN) {
                mDesiredOrientation = SuperPlayerDef.Orientation.PORTRAIT;
                rotateScreenOrientation(mDesiredOrientation);
            }

        } else if (orientation >= 260 && orientation <= 280) { //??????????????????
            if (mSuperPlayer.getPlayerState().equals(SuperPlayerDef.PlayerState.END)) {
                return;
            }
            mDesiredOrientation = LANDSCAPE;
            if (getPlayerMode() == SuperPlayerDef.PlayerMode.FULLSCREEN) {
                // ?????????????????????
                rotateScreenOrientation(mDesiredOrientation);
            } else {
                switchPlayMode(SuperPlayerDef.PlayerMode.FULLSCREEN);
            }
        } else if (orientation >= 80 && orientation <= 100) { //??????????????????
            if (mSuperPlayer.getPlayerState().equals(SuperPlayerDef.PlayerState.END)) {
                return;
            }
            mDesiredOrientation = LANDSCAPE_REVERSE;
            if (getPlayerMode() == SuperPlayerDef.PlayerMode.FULLSCREEN) {
                // ?????????????????????
                rotateScreenOrientation(mDesiredOrientation);
            } else {
                switchPlayMode(SuperPlayerDef.PlayerMode.FULLSCREEN);
            }
        }
    }

    /**
     * SuperPlayerView???????????????
     */
    public interface OnSuperPlayerViewCallback {

        /**
         * ??????????????????
         */
        void onStartFullScreenPlay();

        /**
         * ??????????????????
         */
        void onStopFullScreenPlay();

        /**
         * ???????????????????????????x??????
         */
        void onClickFloatCloseBtn();

        /**
         * ????????????????????????????????????
         */
        void onClickSmallReturnBtn();

        /**
         * ?????????????????????
         */
        void onStartFloatWindowPlay();
    }

    public void release() {
        if (mWindowPlayer != null) {
            mWindowPlayer.release();
        }
        if (mFullScreenPlayer != null) {
            mFullScreenPlayer.release();
        }
        if (mFloatPlayer != null) {
            mFloatPlayer.release();
        }
        mOrientationHelper.disable();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        try {
            release();
        } catch (Throwable e) {
            TXCLog.e(TAG, Log.getStackTraceString(e));
        }
    }

    /**
     * onOrientationChanged???????????????
     */
    public void switchPlayMode(SuperPlayerDef.PlayerMode playerMode) {
        if (playerMode == SuperPlayerDef.PlayerMode.WINDOW) {
            if (mControllerCallback != null) {
                mControllerCallback.onSwitchPlayMode(SuperPlayerDef.PlayerMode.WINDOW);
            }
        } else if (playerMode == SuperPlayerDef.PlayerMode.FULLSCREEN) {
            if (mControllerCallback != null) {
                mControllerCallback.onSwitchPlayMode(SuperPlayerDef.PlayerMode.FULLSCREEN);
            }
        } else if (playerMode == SuperPlayerDef.PlayerMode.FLOAT) {
            if (mPlayerViewCallback != null) {
                mPlayerViewCallback.onStartFloatWindowPlay();
            }
            if (mControllerCallback != null) {
                mControllerCallback.onSwitchPlayMode(SuperPlayerDef.PlayerMode.FLOAT);
            }
        }
    }

    public SuperPlayerDef.PlayerMode getPlayerMode() {
        return mSuperPlayer.getPlayerMode();
    }

    public SuperPlayerDef.PlayerState getPlayerState() {
        return mSuperPlayer.getPlayerState();
    }

    private SuperPlayerObserver mSuperPlayerObserver = new SuperPlayerObserver() {
        @Override
        public void onPlayBegin(String name) {
            mWindowPlayer.updatePlayState(SuperPlayerDef.PlayerState.PLAYING);
            mFullScreenPlayer.updatePlayState(SuperPlayerDef.PlayerState.PLAYING);
            updateTitle(name);
            mWindowPlayer.hideBackground();
            if (mDanmuView != null && mDanmuView.isPrepared() && mDanmuView.isPaused()) {
                mDanmuView.resume();
            }
//            if (mWatcher != null) {
//                mWatcher.exitLoading();
//            }
        }

        @Override
        public void onPlayPause() {
            mWindowPlayer.updatePlayState(SuperPlayerDef.PlayerState.PAUSE);
            mFullScreenPlayer.updatePlayState(SuperPlayerDef.PlayerState.PAUSE);
        }

        @Override
        public void onPlayStop() {
            mWindowPlayer.updatePlayState(SuperPlayerDef.PlayerState.END);
            mFullScreenPlayer.updatePlayState(SuperPlayerDef.PlayerState.END);
            // ????????????????????????????????????
//            if (mWatcher != null) {
//                mWatcher.stop();
//            }
        }

        @Override
        public void onPlayLoading(String name) {
            mWindowPlayer.updatePlayState(SuperPlayerDef.PlayerState.LOADING);
            mFullScreenPlayer.updatePlayState(SuperPlayerDef.PlayerState.LOADING);
            updateTitle(name);
            if (mWatcher != null) {
                mWatcher.enterLoading();
            }
        }

        @Override
        public void onPlayProgress(long current, long duration) {
            mWindowPlayer.updateVideoProgress(current, duration);
            mFullScreenPlayer.updateVideoProgress(current, duration);
        }

        @Override
        public void onSeek(int position) {
            if (mSuperPlayer.getPlayerType() == SuperPlayerDef.PlayerType.VOD) {
//                if (mWatcher != null) {
//                    mWatcher.stop();
//                }
                mWindowPlayer.updatePlayState(SuperPlayerDef.PlayerState.LOADING);
                if (mWatcher != null) {
                    mWatcher.enterLoading();
                }
            }

        }

        @Override
        public void onSwitchStreamStart(boolean success, SuperPlayerDef.PlayerType playerType, VideoQuality quality) {
            if (playerType == SuperPlayerDef.PlayerType.LIVE) {
                if (success) {
                    Toast.makeText(mContext, "???????????????" + quality.title + "...", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, "??????" + quality.title + "?????????????????????????????????", Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        public void onSwitchStreamEnd(boolean success, SuperPlayerDef.PlayerType playerType, VideoQuality quality) {
            if (playerType == SuperPlayerDef.PlayerType.LIVE) {
                if (success) {
                    Toast.makeText(mContext, "?????????????????????", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, "?????????????????????", Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        public void onPlayerTypeChange(SuperPlayerDef.PlayerType playType) {
            mWindowPlayer.updatePlayType(playType);
            mFullScreenPlayer.updatePlayType(playType);
        }

        @Override
        public void onPlayTimeShiftLive(TXLivePlayer player, String url) {
            if (mWatcher == null) {
                mWatcher = new NetWatcher(mContext);
            }
            mWatcher.start(url, player);
        }

        @Override
        public void onPlayTimeShiftVod(TXVodPlayer player, String url) {
            if (mWatcher == null) {
                mWatcher = new NetWatcher(mContext);
            }
            mWatcher.start(url, player);
        }

        @Override
        public void onVideoQualityListChange(List<VideoQuality> videoQualities, VideoQuality defaultVideoQuality) {
            mFullScreenPlayer.setVideoQualityList(videoQualities);
            mFullScreenPlayer.updateVideoQuality(defaultVideoQuality);
        }

        @Override
        public void onVideoImageSpriteAndKeyFrameChanged(PlayImageSpriteInfo info, List<PlayKeyFrameDescInfo> list) {
            mFullScreenPlayer.updateImageSpriteInfo(info);
            mFullScreenPlayer.updateKeyFrameDescInfo(list);
        }

        @Override
        public void onError(int code, String message) {
            showToast(message);
        }
    };

    private void showToast(String message) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    private void showToast(int resId) {
        Toast.makeText(mContext, resId, Toast.LENGTH_SHORT).show();
    }

    public static void save2MediaStore(Context context, Bitmap image) {
        File sdcardDir = context.getExternalFilesDir(null);
        if (sdcardDir == null) {
            Log.e(TAG, "sdcardDir is null");
            return;
        }
        File appDir = new File(sdcardDir, "superplayer");
        if (!appDir.exists()) {
            appDir.mkdir();
        }

        long dateSeconds = System.currentTimeMillis() / 1000;
        String fileName = dateSeconds + ".jpg";
        File file = new File(appDir, fileName);

        String filePath = file.getAbsolutePath();

        File f = new File(filePath);
        if (f.exists()) {
            f.delete();
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            image.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            // Save the screenshot to the MediaStore
            ContentValues values = new ContentValues();
            ContentResolver resolver = context.getContentResolver();
            values.put(MediaStore.Images.ImageColumns.DATA, filePath);
            values.put(MediaStore.Images.ImageColumns.TITLE, fileName);
            values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.ImageColumns.DATE_ADDED, dateSeconds);
            values.put(MediaStore.Images.ImageColumns.DATE_MODIFIED, dateSeconds);
            values.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.ImageColumns.WIDTH, image.getWidth());
            values.put(MediaStore.Images.ImageColumns.HEIGHT, image.getHeight());
            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            OutputStream out = resolver.openOutputStream(uri);
            image.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

            // update file size in the database
            values.clear();
            values.put(MediaStore.Images.ImageColumns.SIZE, new File(filePath).length());
            resolver.update(uri, values, null, null);

        } catch (Exception e) {
            TXCLog.e(TAG, Log.getStackTraceString(e));
        }
    }

}
