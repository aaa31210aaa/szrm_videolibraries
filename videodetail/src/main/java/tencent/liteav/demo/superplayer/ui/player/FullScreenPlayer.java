package tencent.liteav.demo.superplayer.ui.player;

import static common.utils.ShareUtils.toShare;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.zhouwei.library.CustomPopWindow;
import com.szrm.videolibraries.R;
import com.tencent.rtmp.TXImageSprite;
import com.wdcs.model.PlayImageSpriteInfo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import common.constants.Constants;
import common.manager.BuriedPointModelManager;
import common.manager.FinderBuriedPointManager;
import common.model.DataDTO;
import common.model.PlayKeyFrameDescInfo;
import common.model.VideoCollectionModel;
import common.model.VideoQuality;
import tencent.liteav.demo.superplayer.SuperPlayerDef;
import tencent.liteav.demo.superplayer.contants.Contants;
import tencent.liteav.demo.superplayer.model.net.LogReport;
import tencent.liteav.demo.superplayer.model.utils.VideoGestureDetector;
import tencent.liteav.demo.superplayer.ui.view.PointSeekBar;
import tencent.liteav.demo.superplayer.ui.view.VideoProgressLayout;
import tencent.liteav.demo.superplayer.ui.view.VodMoreView;
import tencent.liteav.demo.superplayer.ui.view.VodQualityView;
import tencent.liteav.demo.superplayer.ui.view.VolumeBrightnessProgressLayout;


/**
 * 全屏模式播放控件
 * <p>
 * 除{@link WindowPlayer}基本功能外，还包括进度条关键帧打点信息显示与跳转、快进快退时缩略图的显示、切换画质
 * 镜像播放、硬件加速、倍速播放、弹幕、截图等功能
 * <p>
 * 1、点击事件监听{@link #onClick(View)}
 * <p>
 * 2、触摸事件监听{@link #onTouchEvent(MotionEvent)}
 * <p>
 * 3、进度条滑动事件监听{@link #onProgressChanged(PointSeekBar, int, boolean)}
 * {@link #onStartTrackingTouch(PointSeekBar)}{@link #onStopTrackingTouch(PointSeekBar)}
 * <p>
 * 4、进度条打点信息点击监听{@link #onSeekBarPointClick(View, int)}
 * <p>
 * 5、切换画质监听{@link #onQualitySelect(VideoQuality)}
 * <p>
 * 6、倍速播放监听{@link #onSpeedChange(float)}
 * <p>
 * 7、镜像播放监听{@link #onMirrorChange(boolean)}
 * <p>
 * 8、硬件加速监听{@link #onHWAcceleration(boolean)}
 */
public class FullScreenPlayer extends AbsPlayer implements View.OnClickListener,
        VodMoreView.Callback, VodQualityView.Callback, PointSeekBar.OnSeekBarPointClickListener, RadioGroup.OnCheckedChangeListener {

    // UI控件
    private LinearLayout mLayoutTop;                             // 顶部标题栏布局
    private RelativeLayout mLayoutBottom;                          // 底部进度条所在布局
    private ImageView mIvPause;                               // 暂停播放按钮
    private TextView mTvTitle;                               // 视频名称文本
    private TextView mTvBackToLive;                          // 返回直播文本
    private ImageView mIvWatermark;                           // 水印
    private TextView mTvCurrent;                             // 当前进度文本
    private TextView mTvDuration;                            // 总时长文本
    public PointSeekBar mSeekBarProgress;                       // 播放进度条
    public ProgressBar mFullLoadBar;
    public LinearLayout mLayoutReplay;                          // 重播按钮所在布局
    private ProgressBar mPbLiveLoading;                         // 加载圈
    private VolumeBrightnessProgressLayout mGestureVolumeBrightnessProgressLayout; // 音量亮度调节布局
    public VideoProgressLayout mGestureVideoProgressLayout;            // 手势快进提示布局

    private ImageView mIvBack;                                // 顶部标题栏中的返回按钮
    public ImageView mLike;                                  // 点赞按钮
    public TextView fullscreenLikeNum;                       // 点赞数
    public ImageView mCollection;                            // 收藏按钮
    public TextView fullscreenCollection;                    // 收藏数
    private ImageView mShare;                                 // 分享按钮
    private ImageView mIvSnapshot;                            // 截屏按钮
    private ImageView mIvLock;                                // 锁屏按钮
    private ImageView mIvMore;                                // 更多设置弹窗按钮
    private VodQualityView mVodQualityView;                        // 画质列表弹窗
    public VodMoreView mVodMoreView;                           // 更多设置弹窗
    private TextView mTvVttText;                             // 关键帧打点信息文本

    private HideLockViewRunnable mHideLockViewRunnable;                  // 隐藏锁屏按钮子线程
    private GestureDetector mGestureDetector;                       // 手势检测监听器
    private VideoGestureDetector mVideoGestureDetector;                      // 手势控制工具

    private boolean isShowing;                              // 自身是否可见
    private boolean mIsChangingSeekBarProgress;             // 进度条是否正在拖动，避免SeekBar由于视频播放的update而跳动
    public SuperPlayerDef.PlayerType mPlayType;                              // 当前播放视频类型
    private SuperPlayerDef.PlayerState mCurrentPlayState = SuperPlayerDef.PlayerState.END;                 // 当前播放状态
    private long mDuration;                              // 视频总时长
    private long mLivePushDuration;                      // 直播推流总时长
    private long mProgress;                              // 当前播放进度

    private Bitmap mBackgroundBmp;                         // 背景图
    private Bitmap mWaterMarkBmp;                          // 水印图
    private float mWaterMarkBmpX;                         // 水印x坐标
    private float mWaterMarkBmpY;                         // 水印y坐标

    private boolean mBarrageOn;                             // 弹幕是否开启
    public boolean mLockScreen;                            // 是否锁屏
    private TXImageSprite mTXImageSprite;                         // 雪碧图信息
    private List<PlayKeyFrameDescInfo> mTXPlayKeyFrameDescInfoList;            // 关键帧信息
    private int mSelectedPos = -1;                      // 点击的关键帧时间点

    private VideoQuality mDefaultVideoQuality;                   // 默认画质
    private List<VideoQuality> mVideoQualityList;                      // 画质列表
    private boolean mFirstShowQuality;                      // 是都是首次显示画质信息
    public TextView superplayerSpeed;                       // 倍速按钮
    public PopupWindow popupWindow;
    //    private View contentView;
    private RadioGroup mRadioGroup;                            // 倍速选择radioGroup
    private RadioButton mRbSpeed05;                              // 0.5倍速按钮
    private RadioButton mRbSpeed075;                            // 0.75倍速按钮
    public RadioButton mRbSpeed1;                              // 1.0倍速按钮
    private RadioButton mRbSpeed125;                            // 1.25倍速按钮
    private RadioButton mRbSpeed15;                             // 1.5倍速按钮
    private RadioButton mRbSpeed2;                              // 2.0倍速按钮
    private Context mContext;
    public int phoneWidth;
    public int phoneHeight;
    public String strSpeed;
    public CustomPopWindow sharePop;
    private View shareView;
    private ImageView fullscreenShareWx;
    private ImageView fullscreenShareCircle;
    private ImageView fullscreenShareQq;
    public DataDTO item;
    public VideoCollectionModel.DataDTO.RecordsDTO recordsDTO;
    private boolean mIsTurnPage;

    private TranslateAnimation translateAniRightShow, translateAniRightHide, translateAniBottomShow, translateAniBottomHide;

    public FullScreenPlayer(Context context) {
        super(context);
        initialize(context);
    }

    public FullScreenPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public FullScreenPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    /**
     * 初始化控件、手势检测监听器、亮度/音量/播放进度的回调
     */
    private void initialize(Context context) {
        mContext = context;
        initView(context);

        mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (mLockScreen) {
                    return false;
                }
                togglePlayState();
                show();
                if (mHideViewRunnable != null) {
                    removeCallbacks(mHideViewRunnable);
                    postDelayed(mHideViewRunnable, Contants.delayMillis);
                }
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggle();
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent downEvent, MotionEvent moveEvent, float distanceX, float distanceY) {
                if (mLockScreen) {
                    return false;
                }
                if (downEvent == null || moveEvent == null) {
                    return false;
                }
                if (mVideoGestureDetector != null && mGestureVolumeBrightnessProgressLayout != null) {
                    mVideoGestureDetector.check(mGestureVolumeBrightnessProgressLayout.getHeight(), downEvent, moveEvent, distanceX, distanceY);
                }
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                if (mLockScreen) {
                    return true;
                }
                if (mVideoGestureDetector != null) {
                    mVideoGestureDetector.reset(getWidth(), mSeekBarProgress.getProgress());
                }
                return true;
            }

        });
        mGestureDetector.setIsLongpressEnabled(false);

        mVideoGestureDetector = new VideoGestureDetector(getContext());
        mVideoGestureDetector.setVideoGestureListener(new VideoGestureDetector.VideoGestureListener() {
            @Override
            public void onBrightnessGesture(float newBrightness) {
                if (mGestureVolumeBrightnessProgressLayout != null) {
                    mGestureVolumeBrightnessProgressLayout.setProgress((int) (newBrightness * 100));
                    mVodMoreView.setBrightProgress((int) (newBrightness * 100));
                    mGestureVolumeBrightnessProgressLayout.setImageResource(R.drawable.superplayer_ic_light_max);
                    mGestureVolumeBrightnessProgressLayout.show();
                }
            }

            @Override
            public void onVolumeGesture(float volumeProgress) {
                if (mGestureVolumeBrightnessProgressLayout != null) {
                    mGestureVolumeBrightnessProgressLayout.setImageResource(R.drawable.superplayer_ic_volume_max);
                    mGestureVolumeBrightnessProgressLayout.setProgress((int) volumeProgress);
                    mGestureVolumeBrightnessProgressLayout.show();
                }
            }

            @Override
            public void onSeekGesture(int progress) {
                mIsChangingSeekBarProgress = true;
                if (mGestureVideoProgressLayout != null) {

                    if (progress > mSeekBarProgress.getMax()) {
                        progress = mSeekBarProgress.getMax();
                    }
                    if (progress < 0) {
                        progress = 0;
                    }
                    mGestureVideoProgressLayout.setProgress(progress);
                    mGestureVideoProgressLayout.show();

                    float percentage = ((float) progress) / mSeekBarProgress.getMax();
                    float currentTime = (mDuration * percentage);
                    if (mPlayType == SuperPlayerDef.PlayerType.LIVE || mPlayType == SuperPlayerDef.PlayerType.LIVE_SHIFT) {
                        if (mLivePushDuration > MAX_SHIFT_TIME) {
                            currentTime = (int) (mLivePushDuration - MAX_SHIFT_TIME * (1 - percentage));
                        } else {
                            currentTime = mLivePushDuration * percentage;
                        }
                        mGestureVideoProgressLayout.setTimeText(formattedTime((long) currentTime));
                    } else {
                        mGestureVideoProgressLayout.setTimeText(formattedTime((long) currentTime) + " / " + formattedTime((long) mDuration));
                    }
                    setThumbnail(progress);
                }
                if (mSeekBarProgress != null) {
                    mSeekBarProgress.setProgress(progress);
                }
            }
        });
    }

    /**
     * 初始化view
     */
    private void initView(Context context) {
        phoneWidth = getResources().getDisplayMetrics().widthPixels;
        phoneHeight = getResources().getDisplayMetrics().heightPixels;
        mHideLockViewRunnable = new HideLockViewRunnable(this);
        LayoutInflater.from(context).inflate(R.layout.superplayer_vod_player_fullscreen, this);
        mRadioGroup = findViewById(R.id.superplayer_rg);
        mRbSpeed05 = findViewById(R.id.superplayer_rb_speed05);
        mRbSpeed05.setOnClickListener(this);
        mRbSpeed075 = findViewById(R.id.superplayer_rb_speed075);
        mRbSpeed075.setOnClickListener(this);
        mRbSpeed1 = findViewById(R.id.superplayer_rb_speed1);
        mRbSpeed1.setOnClickListener(this);
        mRbSpeed125 = findViewById(R.id.superplayer_rb_speed125);
        mRbSpeed125.setOnClickListener(this);
        mRbSpeed15 = findViewById(R.id.superplayer_rb_speed15);
        mRbSpeed15.setOnClickListener(this);
        mRbSpeed2 = findViewById(R.id.superplayer_rb_speed2);
        mRbSpeed2.setOnClickListener(this);
        mRadioGroup.setOnCheckedChangeListener(this);
        shareView = View.inflate(context, R.layout.share_popwindow, null);
        fullscreenShareWx = shareView.findViewById(R.id.fullscreen_share_wx);
        fullscreenShareWx.setOnClickListener(this);
        fullscreenShareCircle = shareView.findViewById(R.id.fullscreen_share_circle);
        fullscreenShareCircle.setOnClickListener(this);
        fullscreenShareQq = shareView.findViewById(R.id.fullscreen_share_qq);
        fullscreenShareQq.setOnClickListener(this);

        translateAnimation();

        mLayoutTop = findViewById(R.id.superplayer_rl_top);
        mLayoutTop.setOnClickListener(this);
        mLayoutBottom = findViewById(R.id.superplayer_ll_bottom);
        mLayoutBottom.setOnClickListener(this);
        mLayoutReplay = (LinearLayout) findViewById(R.id.superplayer_ll_replay);

        mIvBack = (ImageView) findViewById(R.id.superplayer_iv_back);
        mIvLock = (ImageView) findViewById(R.id.superplayer_iv_lock);
        mTvTitle = (TextView) findViewById(R.id.superplayer_tv_title);
        mTvTitle.setSelected(true);
        mIvPause = (ImageView) findViewById(R.id.superplayer_iv_pause);
        mIvMore = (ImageView) findViewById(R.id.superplayer_iv_more);
        mLike = findViewById(R.id.fullscreen_like);
        fullscreenLikeNum = findViewById(R.id.fullscreen_like_num);
        mCollection = findViewById(R.id.fullscreen_collection);
        fullscreenCollection = findViewById(R.id.fullscreen_collection_num);
        mShare = findViewById(R.id.superplayer_fullscreen_share);
        mIvSnapshot = (ImageView) findViewById(R.id.superplayer_iv_snapshot);
        mTvCurrent = (TextView) findViewById(R.id.superplayer_tv_current);
        mTvDuration = (TextView) findViewById(R.id.superplayer_tv_duration);

        mSeekBarProgress = (PointSeekBar) findViewById(R.id.superplayer_seekbar_progress);
        mSeekBarProgress.setProgress(0);
        mSeekBarProgress.setOnPointClickListener(this);
//        mSeekBarProgress.setOnSeekBarChangeListener(this);
        mFullLoadBar = findViewById(R.id.superplayer_loadbar_progress);
        mFullLoadBar.setProgress(0);
        mFullLoadBar.setMax(100);
        mTvBackToLive = (TextView) findViewById(R.id.superplayer_tv_back_to_live);
        mPbLiveLoading = (ProgressBar) findViewById(R.id.superplayer_pb_live);

        mVodQualityView = (VodQualityView) findViewById(R.id.superplayer_vod_quality);
        mVodQualityView.setCallback(this);
        mVodMoreView = (VodMoreView) findViewById(R.id.superplayer_vod_more);
        mVodMoreView.setCallback(this);

        mTvBackToLive.setOnClickListener(this);
        mLayoutReplay.setOnClickListener(this);
        mIvLock.setOnClickListener(this);
        mIvBack.setOnClickListener(this);
        mIvPause.setOnClickListener(this);
        mIvSnapshot.setOnClickListener(this);
        mIvMore.setOnClickListener(this);
        mShare.setOnClickListener(this);
        mTvVttText = (TextView) findViewById(R.id.superplayer_large_tv_vtt_text);
        mTvVttText.setOnClickListener(this);
        mGestureVolumeBrightnessProgressLayout = (VolumeBrightnessProgressLayout) findViewById(R.id.superplayer_gesture_progress);
        mGestureVideoProgressLayout = (VideoProgressLayout) findViewById(R.id.superplayer_video_progress_layout);
        mIvWatermark = (ImageView) findViewById(R.id.superplayer_large_iv_water_mark);

        superplayerSpeed = findViewById(R.id.superplayer_speed);
        superplayerSpeed.setOnClickListener(this);
    }

    public void setDataDTO(DataDTO mItem) {
        this.item = mItem;
    }

    public void setRecordsDTO(VideoCollectionModel.DataDTO.RecordsDTO mRecordsDTO){
        this.recordsDTO = mRecordsDTO;
    }

    public DataDTO getDataDTO() {
        return item;
    }

    public void setIsTurnPages(boolean isTurnPages){
        this.mIsTurnPage = isTurnPages;
    }


    //位移动画
    private void translateAnimation() {


        //向左位移显示动画
        translateAniRightShow = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF,//RELATIVE_TO_SELF表示操作自身
                1,//fromXValue表示开始的X轴位置
                Animation.RELATIVE_TO_SELF,
                0,//fromXValue表示结束的X轴位置
                Animation.RELATIVE_TO_SELF,
                0,//fromXValue表示开始的Y轴位置
                Animation.RELATIVE_TO_SELF,
                0);//fromXValue表示结束的Y轴位置
        translateAniRightShow.setRepeatMode(Animation.REVERSE);
        translateAniRightShow.setDuration(500);

        //向右位移隐藏动画
        translateAniRightHide = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF,//RELATIVE_TO_SELF表示操作自身
                0,//fromXValue表示开始的X轴位置
                Animation.RELATIVE_TO_SELF,
                1,//fromXValue表示结束的X轴位置
                Animation.RELATIVE_TO_SELF,
                0,//fromXValue表示开始的Y轴位置
                Animation.RELATIVE_TO_SELF,
                0);//fromXValue表示结束的Y轴位置
        translateAniRightHide.setRepeatMode(Animation.REVERSE);
        translateAniRightHide.setDuration(500);

        translateAniBottomShow = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF,//RELATIVE_TO_SELF表示操作自身
                0,//fromXValue表示开始的X轴位置
                Animation.RELATIVE_TO_SELF,
                0,//fromXValue表示结束的X轴位置
                Animation.RELATIVE_TO_SELF,
                0,//fromXValue表示开始的Y轴位置
                Animation.RELATIVE_TO_SELF,
                1);//fromXValue表示结束的Y轴位置
        translateAniRightHide.setRepeatMode(Animation.REVERSE);
        translateAniRightHide.setDuration(500);

        translateAniBottomHide = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF,//RELATIVE_TO_SELF表示操作自身
                0,//fromXValue表示开始的X轴位置
                Animation.RELATIVE_TO_SELF,
                0,//fromXValue表示结束的X轴位置
                Animation.RELATIVE_TO_SELF,
                1,//fromXValue表示开始的Y轴位置
                Animation.RELATIVE_TO_SELF,
                0);//fromXValue表示结束的Y轴位置
        translateAniRightHide.setRepeatMode(Animation.REVERSE);
        translateAniRightHide.setDuration(500);


    }

    /**
     * 弹出分享框
     */
    private void showSharePop(Context context) {
        if (sharePop == null) {
            //创建并显示popWindow
            sharePop = new CustomPopWindow.PopupWindowBuilder(context)
                    .setView(shareView)
                    .setOutsideTouchable(true)
                    .setFocusable(false)
                    .size(phoneHeight, phoneWidth / 3)
                    .setAnimationStyle(R.style.take_popwindow_anim)
                    .create()
                    .showAtLocation(((Activity) context).getWindow().getDecorView(), Gravity.BOTTOM, 0, 0);
        } else {
            sharePop.showAtLocation(((Activity) context).getWindow().getDecorView(), Gravity.BOTTOM, 0, 0);
        }
        toggle();
    }

    /**
     * 切换播放状态
     * <p>
     * 双击和点击播放/暂停按钮会触发此方法
     */
    private void togglePlayState() {
        switch (mCurrentPlayState) {
            case PAUSE:
            case END:
                if (mControllerCallback != null) {
                    mControllerCallback.onResume();
                }
                break;
            case PLAYING:
            case LOADING:
                if (mControllerCallback != null) {
                    mControllerCallback.onPause();
                }
                mLayoutReplay.setVisibility(View.GONE);
                break;
        }
        show();
    }


    /**
     * 切换自身的可见性
     */
    private void toggle() {
        if (!mLockScreen) {
            if (isShowing) {
                hide();
            } else {
                show();
                if (mHideViewRunnable != null) {
                    removeCallbacks(mHideViewRunnable);
                    postDelayed(mHideViewRunnable, Contants.delayMillis);
                }
            }
        } else {
            mIvLock.setVisibility(VISIBLE);
            if (mHideLockViewRunnable != null) {
                removeCallbacks(mHideLockViewRunnable);
                postDelayed(mHideLockViewRunnable, Contants.delayMillis);
            }
        }
        if (mVodMoreView.getVisibility() == VISIBLE) {
            mVodMoreView.startAnimation(translateAniRightHide);
            mVodMoreView.setVisibility(GONE);
        }

        if (mRadioGroup.getVisibility() == VISIBLE) {
            mRadioGroup.startAnimation(translateAniRightHide);
            mRadioGroup.setVisibility(GONE);
        }
    }

    /**
     * 设置水印
     *
     * @param bmp 水印图
     * @param x   水印的x坐标
     * @param y   水印的y坐标
     */
    @Override
    public void setWatermark(Bitmap bmp, float x, float y) {
        mWaterMarkBmp = bmp;
        mWaterMarkBmpY = y;
        mWaterMarkBmpX = x;
    }

    /**
     * 显示控件
     */
    @Override
    public void show() {
        isShowing = true;
        mLayoutTop.setVisibility(View.VISIBLE);
        mLayoutBottom.setVisibility(View.VISIBLE);
        superplayerSpeed.setVisibility(VISIBLE);
        if (mHideLockViewRunnable != null) {
            removeCallbacks(mHideLockViewRunnable);
        }
        mIvLock.setVisibility(VISIBLE);
        if (mPlayType == SuperPlayerDef.PlayerType.LIVE_SHIFT) {
            if (mLayoutBottom.getVisibility() == VISIBLE) {
                mTvBackToLive.setVisibility(View.VISIBLE);
            }
        }
        List<PointSeekBar.PointParams> pointParams = new ArrayList<>();
        if (mTXPlayKeyFrameDescInfoList != null) {
            for (PlayKeyFrameDescInfo info : mTXPlayKeyFrameDescInfoList) {
                int progress = (int) (info.time / mDuration * mSeekBarProgress.getMax());
                pointParams.add(new PointSeekBar.PointParams(progress, Color.WHITE));
            }
        }
        mSeekBarProgress.setPointList(pointParams);
    }

    /**
     * 隐藏控件
     */
    @Override
    public void hide() {
        isShowing = false;
        mLayoutTop.setVisibility(View.GONE);
        mLayoutBottom.setVisibility(View.GONE);
        mVodQualityView.setVisibility(View.GONE);
        mTvVttText.setVisibility(GONE);
        mIvLock.setVisibility(GONE);
        superplayerSpeed.setVisibility(GONE);
        if (mPlayType == SuperPlayerDef.PlayerType.LIVE_SHIFT) {
            mTvBackToLive.setVisibility(View.GONE);
        }
    }

    /**
     * 释放控件的内存
     */
    @Override
    public void release() {
        releaseTXImageSprite();
    }

    @Override
    public void updatePlayState(SuperPlayerDef.PlayerState playState) {
        switch (playState) {
            case PLAYING:
                mIvPause.setImageResource(R.drawable.superplayer_ic_vod_pause_normal);
                toggleView(mPbLiveLoading, false);
                toggleView(mLayoutReplay, false);
                break;
            case LOADING:
                mIvPause.setImageResource(R.drawable.superplayer_ic_vod_pause_normal);
                toggleView(mPbLiveLoading, true);
                toggleView(mLayoutReplay, false);
                break;
            case PAUSE:
                mIvPause.setImageResource(R.drawable.superplayer_ic_vod_play_normal);
                toggleView(mLayoutReplay, false);
                break;
            case END:
                mIvPause.setImageResource(R.drawable.superplayer_ic_vod_play_normal);
                toggleView(mLayoutReplay, true);
                break;
        }
        mCurrentPlayState = playState;
    }


    /**
     * 设置视频画质信息
     *
     * @param list 画质列表
     */
    @Override
    public void setVideoQualityList(List<VideoQuality> list) {
        mVideoQualityList = list;
        mFirstShowQuality = false;
    }

    /**
     * 更新视频名称
     *
     * @param title 视频名称
     */
    @Override
    public void updateTitle(String title) {
        mTvTitle.setText(title);
    }

    /**
     * 更新视频播放进度
     *
     * @param current  当前进度(秒)
     * @param duration 视频总时长(秒)
     */
    @Override
    public void updateVideoProgress(long current, long duration) {
        mProgress = current < 0 ? 0 : current;
        mDuration = duration < 0 ? 0 : duration;
        mTvCurrent.setText(formattedTime(mProgress));
        float percentage = mDuration > 0 ? ((float) mProgress / (float) mDuration) : 1.0f;
        if (mProgress == 0) {
            mLivePushDuration = 0;
            percentage = 0;
        }
        if (mPlayType == SuperPlayerDef.PlayerType.LIVE || mPlayType == SuperPlayerDef.PlayerType.LIVE_SHIFT) {
            mLivePushDuration = mLivePushDuration > mProgress ? mLivePushDuration : mProgress;
            long leftTime = mDuration - mProgress;
            mDuration = mDuration > MAX_SHIFT_TIME ? MAX_SHIFT_TIME : mDuration;
            percentage = 1 - (float) leftTime / (float) mDuration;
        }

        if (percentage >= 0 && percentage <= 1) {
            int progress = Math.round(percentage * mSeekBarProgress.getMax());
            if (!mIsChangingSeekBarProgress) {
                mSeekBarProgress.setProgress(progress);
            }
            mTvDuration.setText(formattedTime(mDuration));
        }
    }

    @Override
    public void updatePlayType(SuperPlayerDef.PlayerType type) {
        mPlayType = type;
        switch (type) {
            case VOD:
                mTvBackToLive.setVisibility(View.GONE);
                mVodMoreView.updatePlayType(SuperPlayerDef.PlayerType.VOD);
                mTvDuration.setVisibility(View.VISIBLE);
                break;
            case LIVE:
                mTvBackToLive.setVisibility(View.GONE);
                mTvDuration.setVisibility(View.GONE);
                mVodMoreView.updatePlayType(SuperPlayerDef.PlayerType.LIVE);
                mSeekBarProgress.setProgress(100);
                break;
            case LIVE_SHIFT:
                if (mLayoutBottom.getVisibility() == VISIBLE) {
                    mTvBackToLive.setVisibility(View.VISIBLE);
                }
                mTvDuration.setVisibility(View.GONE);
                mVodMoreView.updatePlayType(SuperPlayerDef.PlayerType.LIVE_SHIFT);
                break;
        }
    }

    /**
     * 更新雪碧图信息
     *
     * @param info 雪碧图信息
     */
    @Override
    public void updateImageSpriteInfo(PlayImageSpriteInfo info) {
        if (mTXImageSprite != null) {
            releaseTXImageSprite();
        }
        // 有缩略图的时候不显示进度
        mGestureVideoProgressLayout.setProgressVisibility(info == null || info.imageUrls == null || info.imageUrls.size() == 0);
        if (mPlayType == SuperPlayerDef.PlayerType.VOD) {
            mTXImageSprite = new TXImageSprite(getContext());
            if (info != null) {
                // 雪碧图ELK上报
                LogReport.getInstance().uploadLogs(LogReport.ELK_ACTION_IMAGE_SPRITE, 0, 0);
                mTXImageSprite.setVTTUrlAndImageUrls(info.webVttUrl, info.imageUrls);
            } else {
                mTXImageSprite.setVTTUrlAndImageUrls(null, null);
            }
        }
    }

    private void releaseTXImageSprite() {
        if (mTXImageSprite != null) {
            mTXImageSprite.release();
            mTXImageSprite = null;
        }
    }

    /**
     * 更新关键帧信息
     *
     * @param list 关键帧信息列表
     */
    @Override
    public void updateKeyFrameDescInfo(List<PlayKeyFrameDescInfo> list) {
        mTXPlayKeyFrameDescInfoList = list;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mGestureDetector != null)
            mGestureDetector.onTouchEvent(event);

        if (!mLockScreen) {
            if (event.getAction() == MotionEvent.ACTION_UP && mVideoGestureDetector != null && mVideoGestureDetector.isVideoProgressModel()) {
                int progress = mVideoGestureDetector.getVideoProgress();
                if (progress > mSeekBarProgress.getMax()) {
                    progress = mSeekBarProgress.getMax();
                }
                if (progress < 0) {
                    progress = 0;
                }
                mSeekBarProgress.setProgress(progress);

                int seekTime = 0;
                float percentage = progress * 1.0f / mSeekBarProgress.getMax();
                if (mPlayType == SuperPlayerDef.PlayerType.LIVE || mPlayType == SuperPlayerDef.PlayerType.LIVE_SHIFT) {
                    if (mLivePushDuration > MAX_SHIFT_TIME) {
                        seekTime = (int) (mLivePushDuration - MAX_SHIFT_TIME * (1 - percentage));
                    } else {
                        seekTime = (int) (mLivePushDuration * percentage);
                    }
                } else {
                    seekTime = (int) (percentage * mDuration);
                }
                if (mControllerCallback != null) {
                    mControllerCallback.onSeekTo(seekTime);
                }
                mIsChangingSeekBarProgress = false;
            }
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            removeCallbacks(mHideViewRunnable);
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            postDelayed(mHideViewRunnable, Contants.delayMillis);
        }
        return true;
    }

    public FullIsReplayClick isReplayClick;

    public interface FullIsReplayClick {
        void getFullReplayClick();
    }

    public void setFullIsReplayClick(FullIsReplayClick callBack) {
        this.isReplayClick = callBack;
    }


    /**
     * 设置点击事件监听
     */
    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.superplayer_iv_back || i == R.id.superplayer_tv_title) { //顶部标题栏
            if (mControllerCallback != null) {
                mControllerCallback.onBackPressed(SuperPlayerDef.PlayerMode.FULLSCREEN);
                hide();
            }
        } else if (i == R.id.superplayer_iv_pause) {            //暂停\播放按钮
            togglePlayState();
        } else if (i == R.id.superplayer_fullscreen_share) {    //分享
            FinderBuriedPointManager.setFinderLikeFavoriteShare(Constants.CONTENT_TRANSMIT, recordsDTO);
            FinderBuriedPointManager.setFinderClick("分享");
            showSharePop(mContext);
        } else if (i == R.id.fullscreen_share_wx) {
            if (null != item) {
                toShare(item, Constants.SHARE_WX);
            } else if (null != recordsDTO) {
                toShare(recordsDTO, Constants.SHARE_WX);
            }
        } else if (i == R.id.fullscreen_share_circle) {
            if (null != item) {
                toShare(item, Constants.SHARE_CIRCLE);
            } else if (null != recordsDTO) {
                toShare(recordsDTO, Constants.SHARE_CIRCLE);
            }
        } else if (i == R.id.fullscreen_share_qq) {
            if (null != item) {
                toShare(item, Constants.SHARE_QQ);
            } else if (null != recordsDTO) {
                toShare(recordsDTO, Constants.SHARE_QQ);
            }
        } else if (i == R.id.superplayer_iv_snapshot) {         //截屏按钮
            if (mControllerCallback != null) {
                mControllerCallback.onSnapshot();
            }
        } else if (i == R.id.superplayer_iv_more) {             //更多设置按钮
            showMoreView();
        } else if (i == R.id.superplayer_tv_quality) {          //画质按钮
            showQualityView();
        } else if (i == R.id.superplayer_iv_lock) {             //锁屏按钮
            toggleLockState();
        } else if (i == R.id.superplayer_ll_replay) {           //重播按钮
//            isReplayClick.getFullReplayClick();
            replay();
        } else if (i == R.id.superplayer_tv_back_to_live) {     //返回直播按钮
            if (mControllerCallback != null) {
                mControllerCallback.onResumeLive();
            }
        } else if (i == R.id.superplayer_large_tv_vtt_text) {   //关键帧打点信息按钮
            seekToKeyFramePos();
        } else if (i == R.id.superplayer_speed) {               //调整倍速
            hide();
//            showPopWindow(mContext);
            mRadioGroup.startAnimation(translateAniRightShow);
            mRadioGroup.setVisibility(VISIBLE);
        } else if (i == R.id.superplayer_rb_speed05) {
            FinderBuriedPointManager.setFinderSpeed(Constants.VIDEO_CLICK_SPEED,item,"0.5");
            pointSpeed(0.5+"");
            mRbSpeed05.setChecked(true);
            strSpeed = "0.5";
            if (mVodMoreView.mCallback != null) {
                mVodMoreView.mCallback.onSpeedChange(0.5f);
            }
            superplayerSpeed.setText(mRbSpeed05.getText());
        } else if (i == R.id.superplayer_rb_speed075) {
            FinderBuriedPointManager.setFinderSpeed(Constants.VIDEO_CLICK_SPEED,item,"0.75");
            pointSpeed(0.75+"");
            mRbSpeed075.setChecked(true);
            strSpeed = "0.75";
            if (mVodMoreView.mCallback != null) {
                mVodMoreView.mCallback.onSpeedChange(0.75f);
            }
            superplayerSpeed.setText(mRbSpeed075.getText());
        } else if (i == R.id.superplayer_rb_speed1) {
            mRbSpeed1.setChecked(true);
            strSpeed = "1";
            if (mVodMoreView.mCallback != null) {
                mVodMoreView.mCallback.onSpeedChange(1.0f);
            }
            superplayerSpeed.setText("倍速");
        } else if (i == R.id.superplayer_rb_speed125) {
            FinderBuriedPointManager.setFinderSpeed(Constants.VIDEO_CLICK_SPEED,item,"1.25");
            pointSpeed(1.25+"");
            mRbSpeed125.setChecked(true);
            strSpeed = "1.25";
            if (mVodMoreView.mCallback != null) {
                mVodMoreView.mCallback.onSpeedChange(1.25f);
            }
            superplayerSpeed.setText(mRbSpeed125.getText());
        } else if (i == R.id.superplayer_rb_speed15) {
            FinderBuriedPointManager.setFinderSpeed(Constants.VIDEO_CLICK_SPEED,item,"1.5");
            pointSpeed(1.5+"");
            mRbSpeed15.setChecked(true);
            strSpeed = "1.5";
            if (mVodMoreView.mCallback != null) {
                mVodMoreView.mCallback.onSpeedChange(1.5f);
            }
            superplayerSpeed.setText(mRbSpeed15.getText());
        } else if (i == R.id.superplayer_rb_speed2) {
            FinderBuriedPointManager.setFinderSpeed(Constants.VIDEO_CLICK_SPEED,item,"2");
            pointSpeed(2+"");
            mRbSpeed2.setChecked(true);
            strSpeed = "2";
            if (mVodMoreView.mCallback != null) {
                mVodMoreView.mCallback.onSpeedChange(2f);
            }
            superplayerSpeed.setText(mRbSpeed2.getText());
        }
    }

    private void pointSpeed(String speed) {
        if (null != item) {
            String jsonString = BuriedPointModelManager.getVideoPlaySpeed(speed, item.getId() + "", item.getTitle(), "", "",
                    "", "", item.getIssueTimeStamp());
        }
    }

    /**
     * 从右侧弹出倍速选择框
     *
     * @param context
     */
//    private void showPopWindow(Context context) {
//        if (popupWindow == null) {
//            //创建并显示popWindow
//            popupWindow = new CustomPopWindow.PopupWindowBuilder(context)
//                    .setView(contentView)
//                    .setOutsideTouchable(true)
//                    .setFocusable(false)
//                    .size(getResources().getDimensionPixelSize(R.dimen.privacy_dialog_width), phoneWidth)
//                    .setAnimationStyle(R.style.take_speed_popwindow_anim)
//                    .create()
//                    .showAtLocation(getRootView(), Gravity.RIGHT | Gravity.TOP, 0, 0);
//        } else {
//            popupWindow.showAtLocation(getRootView(), Gravity.RIGHT | Gravity.BOTTOM, 0, 0);
//        }
//    }

    /**
     * 显示更多设置弹窗
     */
    private void showMoreView() {
        hide();
        mVodMoreView.startAnimation(translateAniRightShow);
        mVodMoreView.setVisibility(View.VISIBLE);
    }

    /**
     * 显示画质列表弹窗
     */
    private void showQualityView() {
        if (mVideoQualityList == null || mVideoQualityList.size() == 0) {
            return;
        }
        if (mVideoQualityList.size() == 1 && (mVideoQualityList.get(0) == null || TextUtils.isEmpty(mVideoQualityList.get(0).title))) {
            return;
        }
        // 设置默认显示分辨率文字
        mVodQualityView.setVisibility(View.VISIBLE);
        if (!mFirstShowQuality && mDefaultVideoQuality != null) {
            for (int i = 0; i < mVideoQualityList.size(); i++) {
                VideoQuality quality = mVideoQualityList.get(i);
                if (quality != null && quality.title != null && quality.title.equals(mDefaultVideoQuality.title)) {
                    mVodQualityView.setDefaultSelectedQuality(i);
                    break;
                }
            }
            mFirstShowQuality = true;
        }
        mVodQualityView.setVideoQualityList(mVideoQualityList);
    }

    /**
     * 切换锁屏状态
     */
    private void toggleLockState() {
        mLockScreen = !mLockScreen;
        mIvLock.setVisibility(VISIBLE);
        if (mHideLockViewRunnable != null) {
            removeCallbacks(mHideLockViewRunnable);
            postDelayed(mHideLockViewRunnable, Contants.delayMillis);
        }
        if (mLockScreen) {
            mIvLock.setImageResource(R.drawable.superplayer_ic_player_lock);
            hide();
            mIvLock.setVisibility(VISIBLE);
        } else {
            mIvLock.setImageResource(R.drawable.superplayer_ic_player_unlock);
            show();
        }
    }

    /**
     * 重播
     */
    private void replay() {
        toggleView(mLayoutReplay, false);
        if (mControllerCallback != null) {
            mControllerCallback.onResume();
        }
    }

    /**
     * 跳转至关键帧打点处
     */
    private void seekToKeyFramePos() {
        float time = mTXPlayKeyFrameDescInfoList != null ? mTXPlayKeyFrameDescInfoList.get(mSelectedPos).time : 0;
        if (mControllerCallback != null) {
            mControllerCallback.onSeekTo((int) time);
            mControllerCallback.onResume();
        }
        mTvVttText.setVisibility(GONE);
        toggleView(mLayoutReplay, false);
    }

//    @Override
//    public void onProgressChanged(PointSeekBar seekBar, int progress, boolean isFromUser) {
//        if (mGestureVideoProgressLayout != null && isFromUser) {
//            mGestureVideoProgressLayout.show();
//            float percentage = ((float) progress) / seekBar.getMax();
//            float currentTime = (mDuration * percentage);
//            if (mPlayType == SuperPlayerDef.PlayerType.LIVE || mPlayType == SuperPlayerDef.PlayerType.LIVE_SHIFT) {
//                if (mLivePushDuration > MAX_SHIFT_TIME) {
//                    currentTime = (int) (mLivePushDuration - MAX_SHIFT_TIME * (1 - percentage));
//                } else {
//                    currentTime = mLivePushDuration * percentage;
//                }
//                mGestureVideoProgressLayout.setTimeText(formattedTime((long) currentTime));
//            } else {
//                mGestureVideoProgressLayout.setTimeText(formattedTime((long) currentTime) + " / " + formattedTime((long) mDuration));
//            }
//            mGestureVideoProgressLayout.setProgress(progress);
//        }
//        // 加载点播缩略图
//        if (isFromUser && mPlayType == SuperPlayerDef.PlayerType.VOD) {
//            setThumbnail(progress);
//        }
//    }
//
//    @Override
//    public void onStartTrackingTouch(PointSeekBar seekBar) {
//        removeCallbacks(mHideViewRunnable);
//    }
//
//    @Override
//    public void onStopTrackingTouch(PointSeekBar seekBar) {
//        int curProgress = seekBar.getProgress();
//        int maxProgress = seekBar.getMax();
//
//        switch (mPlayType) {
//            case VOD:
//                if (curProgress >= 0 && curProgress <= maxProgress) {
//                    // 关闭重播按钮
//                    toggleView(mLayoutReplay, false);
//                    float percentage = ((float) curProgress) / maxProgress;
//                    int position = (int) (mDuration * percentage);
//                    if (mControllerCallback != null) {
//                        mControllerCallback.onSeekTo(position);
//                        mControllerCallback.onResume();
//                    }
//                }
//                break;
//            case LIVE:
//            case LIVE_SHIFT:
//                toggleView(mPbLiveLoading, true);
//                int seekTime = (int) (mLivePushDuration * curProgress * 1.0f / maxProgress);
//                if (mLivePushDuration > MAX_SHIFT_TIME) {
//                    seekTime = (int) (mLivePushDuration - MAX_SHIFT_TIME * (maxProgress - curProgress) * 1.0f / maxProgress);
//                }
//                if (mControllerCallback != null) {
//                    mControllerCallback.onSeekTo(seekTime);
//                }
//                break;
//        }
//        postDelayed(mHideViewRunnable, Contants.delayMillis);
//    }

    @Override
    public void onSeekBarPointClick(final View view, final int pos) {
        if (mHideLockViewRunnable != null) {
            removeCallbacks(mHideViewRunnable);
            postDelayed(mHideViewRunnable, Contants.delayMillis);
        }
        if (mTXPlayKeyFrameDescInfoList != null) {
            //ELK点击上报
            LogReport.getInstance().uploadLogs(LogReport.ELK_ACTION_PLAYER_POINT, 0, 0);
            mSelectedPos = pos;
            view.post(new Runnable() {
                @Override
                public void run() {
                    int[] location = new int[2];
                    view.getLocationInWindow(location);

                    int viewX = location[0];
                    PlayKeyFrameDescInfo info = mTXPlayKeyFrameDescInfoList.get(pos);
                    String content = info.content;

                    mTvVttText.setText(formattedTime((long) info.time) + " " + content);
                    mTvVttText.setVisibility(VISIBLE);
                    adjustVttTextViewPos(viewX);
                }
            });
        }
    }

    /**
     * 设置播放进度所对应的缩略图
     *
     * @param progress 播放进度
     */
    private void setThumbnail(int progress) {
        float percentage = ((float) progress) / mSeekBarProgress.getMax();
        float seekTime = (mDuration * percentage);
        if (mTXImageSprite != null) {
            Bitmap bitmap = mTXImageSprite.getThumbnail(seekTime);
            if (bitmap != null) {
                mGestureVideoProgressLayout.setThumbnail(bitmap);
            }
        }
    }

    /**
     * 计算并设置关键帧打点信息文本显示的位置
     *
     * @param viewX 点击的打点view
     */
    private void adjustVttTextViewPos(final int viewX) {
        mTvVttText.post(new Runnable() {
            @Override
            public void run() {
                int width = mTvVttText.getWidth();

                int marginLeft = viewX - width / 2;

                LayoutParams params = (LayoutParams) mTvVttText.getLayoutParams();
                params.leftMargin = marginLeft;

                if (marginLeft < 0) {
                    params.leftMargin = 0;
                }

                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                if (marginLeft + width > screenWidth) {
                    params.leftMargin = screenWidth - width;
                }

                mTvVttText.setLayoutParams(params);
            }
        });
    }

    @Override
    public void onSpeedChange(float speedLevel) {
        if (mControllerCallback != null) {
            mControllerCallback.onSpeedChange(speedLevel);
        }
    }

    @Override
    public void onMirrorChange(boolean isMirror) {
        if (mControllerCallback != null) {
            mControllerCallback.onMirrorToggle(isMirror);
        }
    }

    @Override
    public void onHWAcceleration(boolean isAccelerate) {
        if (mControllerCallback != null) {
            mControllerCallback.onHWAccelerationToggle(isAccelerate);
        }
    }

    @Override
    public void onQualitySelect(VideoQuality quality) {
        if (mControllerCallback != null) {
            mControllerCallback.onQualityChange(quality);
        }
        mVodQualityView.setVisibility(View.GONE);
    }

    /**
     * 倍速选择监听
     *
     * @param radioGroup
     * @param checkedId
     */
    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
        if (checkedId == R.id.superplayer_rb_speed05) {
            mRbSpeed05.setChecked(true);
            if (mVodMoreView.mCallback != null) {
                mVodMoreView.mCallback.onSpeedChange(0.5f);
            }
            superplayerSpeed.setText(mRbSpeed05.getText());
        } else if (checkedId == R.id.superplayer_rb_speed075) {
            mRbSpeed075.setChecked(true);
            if (mVodMoreView.mCallback != null) {
                mVodMoreView.mCallback.onSpeedChange(0.75f);
            }
            superplayerSpeed.setText(mRbSpeed075.getText());
        } else if (checkedId == R.id.superplayer_rb_speed1) {
            mRbSpeed1.setChecked(true);
            if (mVodMoreView.mCallback != null) {
                mVodMoreView.mCallback.onSpeedChange(1.0f);
            }
            superplayerSpeed.setText("倍速");
        } else if (checkedId == R.id.superplayer_rb_speed125) {
            mRbSpeed125.setChecked(true);
            if (mVodMoreView.mCallback != null) {
                mVodMoreView.mCallback.onSpeedChange(1.25f);
            }
            superplayerSpeed.setText(mRbSpeed125.getText());
        } else if (checkedId == R.id.superplayer_rb_speed15) {
            mRbSpeed15.setChecked(true);
            if (mVodMoreView.mCallback != null) {
                mVodMoreView.mCallback.onSpeedChange(1.5f);
            }
            superplayerSpeed.setText(mRbSpeed15.getText());
        } else if (checkedId == R.id.superplayer_rb_speed2) {
            mRbSpeed2.setChecked(true);
            if (mVodMoreView.mCallback != null) {
                mVodMoreView.mCallback.onSpeedChange(2.0f);
            }
            superplayerSpeed.setText(mRbSpeed2.getText());
        }
        if (null != popupWindow) {
            popupWindow.dismiss();
        }
    }

    /**
     * 隐藏锁屏按钮的runnable
     */
    private static class HideLockViewRunnable implements Runnable {
        private WeakReference<FullScreenPlayer> mWefControllerFullScreen;

        public HideLockViewRunnable(FullScreenPlayer controller) {
            mWefControllerFullScreen = new WeakReference<>(controller);
        }

        @Override
        public void run() {
            if (mWefControllerFullScreen != null && mWefControllerFullScreen.get() != null) {
                mWefControllerFullScreen.get().mIvLock.setVisibility(GONE);
            }
        }
    }
}
