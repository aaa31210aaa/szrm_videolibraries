package ui.fragment;

import static common.constants.Constants.CATEGORYNAME;
import static common.constants.Constants.CLICK_INTERVAL_TIME;
import static common.constants.Constants.CONTENTID;
import static common.constants.Constants.PANELCODE;
import static common.constants.Constants.TOCURRENTTAB;
import static common.constants.Constants.VIDEOTAG;
import static common.constants.Constants.success_code;
import static common.constants.Constants.token_error;
import static common.utils.SPUtils.isVisibleNoWifiView;
import static tencent.liteav.demo.superplayer.ui.player.WindowPlayer.mDuration;
import static tencent.liteav.demo.superplayer.ui.player.WindowPlayer.mProgress;
import static ui.activity.VideoHomeActivity.isPause;
import static ui.activity.VideoHomeActivity.lsDuration;
import static ui.activity.VideoHomeActivity.maxPercent;
import static ui.activity.VideoHomeActivity.uploadBuriedPoint;
import static utils.NetworkUtil.setDataWifiState;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.example.zhouwei.library.CustomPopWindow;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.cache.CacheMode;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;
import com.szrm.videolibraries.R;
import com.tencent.rtmp.TXLiveConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import adpter.CommentPopRvAdapter;
import adpter.XkshVideoAdapter;
import common.callback.JsonCallback;
import common.callback.VideoInteractiveParam;
import common.constants.Constants;
import common.http.ApiConstants;
import common.manager.BuriedPointModelManager;
import common.manager.ContentBuriedPointManager;
import common.manager.FinderBuriedPointManager;
import common.manager.OnViewPagerListener;
import common.manager.ViewPagerLayoutManager;
import common.model.CollectionLabelModel;
import common.model.CommentLv1Model;
import common.model.ContentStateModel;
import common.model.DataDTO;
import common.model.FinderPointModel;
import common.model.RecommendModel;
import common.model.ReplyLv2Model;
import common.model.ShareInfo;
import common.model.TokenModel;
import common.model.TrackingUploadModel;
import common.model.VideoChannelModel;
import common.model.VideoDetailModel;
import common.utils.AppInit;
import common.utils.ButtonSpan;
import common.utils.DateUtils;
import common.utils.KeyboardUtils;
import common.utils.NoScrollViewPager;
import common.utils.NumberFormatTool;
import common.utils.PersonInfoManager;
import common.utils.SPUtils;
import common.utils.ScreenUtils;
import common.utils.SoftKeyBoardListener;
import common.utils.ToastUtils;
import common.widget.YALikeAnimationView;
import flyco.tablayout.SlidingTabLayout;
import model.bean.ActivityRuleBean;
import tencent.liteav.demo.superplayer.SuperPlayerDef;
import tencent.liteav.demo.superplayer.SuperPlayerModel;
import tencent.liteav.demo.superplayer.SuperPlayerView;
import tencent.liteav.demo.superplayer.model.SuperPlayerImpl;
import tencent.liteav.demo.superplayer.model.utils.SystemUtils;
import tencent.liteav.demo.superplayer.ui.player.WindowPlayer;
import ui.activity.UploadActivity;
import ui.activity.VideoDetailActivity;
import ui.activity.VideoHomeActivity;
import widget.CollectionClickble;
import widget.CustomLoadMoreView;
import widget.LoadingView;


public class XkshFragment extends Fragment implements View.OnClickListener {
    private RecyclerView videoDetailRv;
    public XkshVideoAdapter adapter;
    private CommentPopRvAdapter commentPopRvAdapter;
    //视频列表数据
    public List<DataDTO> mDatas = new ArrayList<>();
    //评论列表数据
    private List<MultiItemEntity> mCommentPopRvData;
    private List<CommentLv1Model.DataDTO.RecordsDTO> mCommentPopDtoData;
    private ImageView videoStaticBg;
    private ImageView startPlay;

    //评论列表弹窗
    public CustomPopWindow popupWindow;

    private View contentView;
    private View chooseContentView;
    private RelativeLayout dismissPop;
    private RecyclerView commentPopRv;
    private TextView commentEdtInput;
    private RelativeLayout commentPopRl;
    //附着在软键盘上的输入弹出窗
    public CustomPopWindow inputAndSendPop;
    private View sendPopContentView;
    private View rootView;
    private LinearLayout edtParent;
    private EditText edtInput;
    private TextView tvSend;
    //选择集数弹窗
    public CustomPopWindow choosePop;

    public ViewPagerLayoutManager xkshManager;
    private ImageView choosePopDismiss;
    public SmartRefreshLayout refreshLayout;
    private String transformationToken = "";
    private String panelCode = "";
    private boolean initialize = true;
    private String mVideoSize = "15"; //每页视频多少条
    private int mPageIndex = 1; //评论列表页数
    private int mPageSize = 10; //评论列表每页多少条
    public String myContentId = ""; //记录当前视频id
    public int currentIndex = 0; //记录当前视频列表的位置
    private TextView commentPopCommentTotal;

    private String videoType; //视频类型
    private VideoInteractiveParam param;
    public String playUrl;
    private String videoTag = "videoTag";
    private String recommendTag = "recommend";
    private boolean isLoadComplate = false;
    private BaseQuickAdapter.RequestLoadMoreListener requestLoadMoreListener;
    public View decorView;
    private SoftKeyBoardListener softKeyBoardListener;
    public CustomPopWindow noLoginTipsPop;
    private View noLoginTipsView;
    private TextView noLoginTipsCancel;
    private TextView noLoginTipsOk;
    public CustomPopWindow sharePop;
    private View sharePopView;
    private ImageView shareWxBtn;
    private ImageView shareCircleBtn;
    private ImageView shareQqBtn;
    public DataDTO mDataDTO;
    private List<RecommendModel.DataDTO.RecordsDTO> recommondList;
    public ViewGroup rlLp;
    private VideoChannelModel videoChannelModel;

    private VideoChannelModel channelModel;
    private Bundle args;
    public boolean mIsVisibleToUser;
    private LoadingView loadingProgress;
    private RelativeLayout.LayoutParams lp;
    public static boolean isFollow; //是否关注
    public ImageView rankList; //排行榜
    public ImageView activityRuleImg; //活动规则图
    public ImageView activityRuleAbbreviation;
    public ImageView activityToAbbreviation; //变为缩略图按钮
    public LinearLayout fullLin;
    public double pointPercent;// 每一次记录的节点播放百分比
    //    private long everyOneDuration; //每一次记录需要上报的播放时长 用来分段上报埋点
    public ActivityRuleBean activityRuleBean;
    public boolean isAbbreviation; //当前是否是缩略图
    private RelativeLayout.LayoutParams playViewParams;
    private View footView;
    public long xkshOldSystemTime;
    public long xkshReportTime;
    private boolean isReply = false;
    private String replyId;
    private List<CollectionLabelModel.DataDTO.ListDTO> collectionList;
    private List<String> collectionTvList;
    private List<String> collectionStrList;
    private SuperPlayerView playerView;
    private View commentEmptyView;
    private TextView commentListTips;
    private String topicName;
    private YALikeAnimationView loveIcon;
    private Handler handler = new Handler();
    private boolean likeIsRequesting;
    private DataDTO negativeScreenDto;
    private String mCategoryName = "";
    private long beforeClickTime = 0;
    private int toCurrentTab;
    private View footerView;
    private boolean isFirst = true;


    public XkshFragment() {

    }

    public XkshFragment newInstance(XkshFragment fragment, VideoChannelModel videoChannelModel, String contentId, String categoryName, int toCurrentTab) {
        args = new Bundle();
        args.putString(PANELCODE, videoChannelModel.getColumnBean().getPanelCode());
        args.putString(CONTENTID, contentId);
        args.putInt(TOCURRENTTAB, toCurrentTab);
        if (!TextUtils.isEmpty(categoryName)) {
            args.putString(CATEGORYNAME, categoryName);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            panelCode = getArguments().getString(PANELCODE);
            myContentId = getArguments().getString(CONTENTID);
            mCategoryName = getArguments().getString(CATEGORYNAME);
            toCurrentTab = getArguments().getInt(TOCURRENTTAB);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        View view = inflater.inflate(R.layout.fragment_xksh, container, false);
        decorView = getActivity().getWindow().getDecorView();
        initView(view);
        if (!TextUtils.isEmpty(myContentId) && 0 == toCurrentTab) {
            getOneVideo(myContentId);
        } else {
            getPullDownData(mVideoSize, panelCode, "false", Constants.REFRESH_TYPE);
        }
        return view;
    }

    public void setPlayView(SuperPlayerView mPlayView) {
        this.playerView = mPlayView;
    }

    private void initView(View view) {
        param = VideoInteractiveParam.getInstance();
        mDataDTO = new DataDTO();
        recommondList = new ArrayList<>();
        loadingProgress = view.findViewById(R.id.xksh_loading_progress);
        loadingProgress.setVisibility(View.VISIBLE);
        videoDetailRv = view.findViewById(R.id.video_detail_rv);
        videoDetailRv.setHasFixedSize(true);
        rankList = view.findViewById(R.id.rank_list);
        rankList.setOnClickListener(this);
        activityRuleImg = view.findViewById(R.id.activity_rule_img);
        activityRuleImg.setOnClickListener(this);
        loveIcon = view.findViewById(R.id.love_icon);
        activityToAbbreviation = view.findViewById(R.id.activity_to_abbreviation);
        activityToAbbreviation.setOnClickListener(this);
        activityRuleAbbreviation = view.findViewById(R.id.activity_rule_abbreviation);
        activityRuleAbbreviation.setOnClickListener(this);
        footerView = View.inflate(getActivity(), R.layout.footer_view, null);
        xkshManager = new ViewPagerLayoutManager(getActivity());
        videoDetailRv.setLayoutManager(xkshManager);
        footView = View.inflate(getActivity(), R.layout.footer_view, null);
        commentEmptyView = View.inflate(getActivity(), R.layout.comment_list_empty, null);
        commentListTips = commentEmptyView.findViewById(R.id.comment_list_tips);
        setSoftKeyBoardListener();
        xkshManager.setOnViewPagerListener(new OnViewPagerListener() {


            @Override
            public void onInitComplete() {
                if (initialize) {
                    return;
                }
                initialize = true;

                if (mDatas.isEmpty()) {
                    return;
                }
                mDataDTO = mDatas.get(0);

                if (null != adapter.getViewByPosition(0, R.id.superplayer_iv_fullscreen)) {
                    if (TextUtils.equals("2", videoIsNormal(Integer.parseInt(NumberFormatTool.getNumStr(mDatas.get(0).getWidth())),
                            Integer.parseInt(NumberFormatTool.getNumStr(mDatas.get(0).getHeight()))))) {
                        adapter.getViewByPosition(0, R.id.superplayer_iv_fullscreen).setVisibility(View.VISIBLE);
                    } else {
                        adapter.getViewByPosition(0, R.id.superplayer_iv_fullscreen).setVisibility(View.GONE);
                    }
                }

//                playerView = SuperPlayerView.getInstance(getActivity(), decorView);
                playerView.mWindowPlayer.setDataDTO(mDataDTO, mDataDTO);
                playerView.mWindowPlayer.setViewpager((NoScrollViewPager) getActivity().findViewById(R.id.video_vp));
                playerView.mWindowPlayer.setIsTurnPages(false);
                playerView.mWindowPlayer.setManager(xkshManager);
                playerView.mFullScreenPlayer.setDataDTO(mDataDTO);
                myContentId = String.valueOf(mDatas.get(0).getId());
                addPageViews(myContentId);
                OkGo.getInstance().cancelTag("contentState");
                getContentState(myContentId);

                SuperPlayerImpl.mCurrentPlayVideoURL = mDatas.get(0).getPlayUrl();
                if (isVisibleNoWifiView(getActivity())) {
                    playerView.setOrientation(false);
                } else {
                    playerView.setOrientation(true);
                }
                currentIndex = 0;
                mPageIndex = 1;
                if (mDatas.get(0).getDisableComment()) {
                    commentListTips.setText("当前页面评论功能已关闭");
                    commentPopRl.setEnabled(false);
                    commentEdtInput.setHint("当前页面评论功能已关闭");
                } else {
                    commentListTips.setText("暂无任何评论，快来抢沙发吧！");
                    commentPopRl.setEnabled(true);
                    commentEdtInput.setHint("写评论...");
                }
                commentPopRvAdapter.setEmptyView(commentEmptyView);
                getCommentList("1", String.valueOf(mPageSize), true);
                videoType = mDatas.get(0).getType();
                rlLp = (ViewGroup) xkshManager.findViewByPosition(0);
                OkGo.getInstance().cancelTag(recommendTag);
                //获取推荐列表
                getRecommend(myContentId, 0);
//                initChoosePop();
            }

            @Override
            public void onPageRelease(boolean isNext, int position) {
            }

            @Override
            public void onPageSelected(final int position, boolean isBottom) {
                if (null == playerView) {
                    return;
                }

                if (null != playerView.getTag() && position == (int) playerView.getTag()) {
                    return;
                }
                //避免越界addOrCancelLike
                if (mDatas.isEmpty()) {
                    return;
                }
                if (null == mDatas.get(position)) {
                    return;
                }

                playerView.mWindowPlayer.hide();

                if (!TextUtils.isEmpty(mDataDTO.getVolcCategory())) {
                    if (mDuration != 0 && mProgress != 0) {
                        //上报埋点
                        long evePlayTime = Math.abs(mProgress - lsDuration);
                        double currentPercent = (evePlayTime * 1.0 / mDuration);
                        double uploadPercent = 0;
                        if (null == playerView.buriedPointModel.getXksh_renew() || TextUtils.equals("false", playerView.buriedPointModel.getXksh_renew())) {
                            //不为重播
                            if (currentPercent > maxPercent) {
                                uploadPercent = currentPercent;
                                maxPercent = currentPercent;
                            } else {
                                uploadPercent = maxPercent;
                            }
                        } else {
                            uploadPercent = 1;
                        }
                        xkshReportTime = DateUtils.getTimeCurrent() - xkshOldSystemTime;
                        BigDecimal two = new BigDecimal(uploadPercent);
                        double pointPercentTwo = two.setScale(2, BigDecimal.ROUND_DOWN).doubleValue();
                        uploadBuriedPoint(ContentBuriedPointManager.setContentBuriedPoint(getActivity(), mDataDTO.getThirdPartyId(), String.valueOf(xkshReportTime), String.valueOf(Math.floor(pointPercentTwo * 100)), Constants.CMS_VIDEO_OVER_AUTO, mDataDTO.getVolcCategory(), mDataDTO.getRequestId()), Constants.CMS_VIDEO_OVER_AUTO);
                        Log.e("xksh_md", "埋点事件：" + Constants.CMS_VIDEO_OVER_AUTO + "播放时长:" + xkshReportTime + "---" + "播放百分比:" + pointPercentTwo);
                    }
                }
                String isFinish;
                if (null == playerView.buriedPointModel.getIs_renew() || TextUtils.equals("false", playerView.buriedPointModel.getIs_renew())) {
                    isFinish = "否";
                } else {
                    isFinish = "是";
                }
                FinderBuriedPointManager.setFinderVideo(Constants.CONTENT_VIDEO_DURATION, "", mDataDTO, xkshReportTime, isFinish);


                mDataDTO = mDatas.get(position);

                if (isVisibleNoWifiView(getActivity())) {
                    playerView.setOrientation(false);
                } else {
                    playerView.setOrientation(true);
                }

                if (null != adapter.getViewByPosition(position, R.id.superplayer_iv_fullscreen)) {
                    if (TextUtils.equals("2", videoIsNormal(Integer.parseInt(NumberFormatTool.getNumStr(mDatas.get(position).getWidth())),
                            Integer.parseInt(NumberFormatTool.getNumStr(mDatas.get(position).getHeight()))))) {
                        adapter.getViewByPosition(position, R.id.superplayer_iv_fullscreen).setVisibility(View.VISIBLE);
                    } else {
                        adapter.getViewByPosition(position, R.id.superplayer_iv_fullscreen).setVisibility(View.GONE);
                    }
                }


                //重置重播标识
                if (null != playerView && null != playerView.buriedPointModel) {
                    playerView.buriedPointModel.setXksh_renew("false");
                }
                //滑动下一条或者上一条视频
                playerView.mWindowPlayer.setRecordDuration(0);
                lsDuration = 0;
                maxPercent = 0;
                SuperPlayerImpl.mCurrentPlayVideoURL = mDatas.get(position).getPlayUrl();
                playUrl = mDatas.get(position).getPlayUrl();
                playerView.mWindowPlayer.setDataDTO(mDataDTO, mDatas.get(currentIndex));
                playerView.mFullScreenPlayer.setDataDTO(mDataDTO);
                playerView.mWindowPlayer.setIsTurnPages(true);
                playerView.mFullScreenPlayer.setIsTurnPages(true);
                currentIndex = position;
                reset();
                myContentId = String.valueOf(mDatas.get(position).getId());

                addPageViews(myContentId);
                videoType = mDatas.get(position).getType();
                mPageIndex = 1;
                if (mDatas.get(position).getDisableComment()) {
                    commentListTips.setText("当前页面评论功能已关闭");
                    commentPopRl.setEnabled(false);
                    commentEdtInput.setHint("当前页面评论功能已关闭");
                } else {
                    commentListTips.setText("暂无任何评论，快来抢沙发吧！");
                    commentPopRl.setEnabled(true);
                    commentEdtInput.setHint("写评论...");
                }
                commentPopRvAdapter.setEmptyView(commentEmptyView);
                getCommentList("1", String.valueOf(mPageSize), true);
                getContentState(myContentId);
                String localUserId = PersonInfoManager.getInstance().getUserId();
                String userId = mDataDTO.getCreateBy();
                if (TextUtils.isEmpty(mDataDTO.getIssuerId()) || TextUtils.equals(localUserId, userId)) {
                    adapter.getViewByPosition(position, R.id.follow).setVisibility(View.GONE);
                } else {
                    adapter.getViewByPosition(position, R.id.follow).setVisibility(View.VISIBLE);
                }

                rlLp = (ViewGroup) xkshManager.findViewByPosition(position);
                OkGo.getInstance().cancelTag(recommendTag);
                getRecommend(myContentId, position);

                if (!"1".equals(playerView.mFullScreenPlayer.strSpeed)) {
                    playerView.mFullScreenPlayer.mVodMoreView.mCallback.onSpeedChange(1.0f);
                    playerView.mFullScreenPlayer.superplayerSpeed.setText("倍速");
                    playerView.mFullScreenPlayer.mRbSpeed1.setChecked(true);
                }
            }
        });

        initSmartRefresh(view);

        contentView = View.inflate(getActivity(), R.layout.fragment_video_comment_pop, null);
        sendPopContentView = View.inflate(getActivity(), R.layout.layout_input_window, null);
        commentPopCommentTotal = contentView.findViewById(R.id.comment_pop_comment_total);
        edtParent = sendPopContentView.findViewById(R.id.edt_parent);
        edtInput = sendPopContentView.findViewById(R.id.edtInput);
        tvSend = sendPopContentView.findViewById(R.id.tvSend);

        noLoginTipsView = View.inflate(getActivity(), R.layout.no_login_tips, null);
        noLoginTipsCancel = noLoginTipsView.findViewById(R.id.no_login_tips_cancel);
        noLoginTipsOk = noLoginTipsView.findViewById(R.id.no_login_tips_ok);
        noLoginTipsCancel.setOnClickListener(this);
        noLoginTipsOk.setOnClickListener(this);

        sharePopView = View.inflate(getActivity(), R.layout.share_pop_view, null);
        shareWxBtn = sharePopView.findViewById(R.id.share_wx_btn);
        shareWxBtn.setOnClickListener(this);
        shareCircleBtn = sharePopView.findViewById(R.id.share_circle_btn);
        shareCircleBtn.setOnClickListener(this);
        shareQqBtn = sharePopView.findViewById(R.id.share_qq_btn);
        shareQqBtn.setOnClickListener(this);

        /**
         * 发送评论
         */
        tvSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty(edtInput.getText())) {
                    Toast.makeText(getActivity(), "请输入评论", Toast.LENGTH_SHORT).show();
                } else {
                    if (isReply) {
                        toReply(replyId);
                    } else {
                        toComment(edtInput.getText().toString(), myContentId);
                    }
                    edtInput.setText("");
                }
            }
        });

        rootView = view.findViewById(R.id.root);

        dismissPop = contentView.findViewById(R.id.dismiss_pop);
        dismissPop.setOnClickListener(this);
        commentPopRv = contentView.findViewById(R.id.comment_pop_rv);
        commentEdtInput = contentView.findViewById(R.id.comment_edtInput);
        commentPopRl = contentView.findViewById(R.id.comment_pop_rl);
        commentPopRl.setOnClickListener(this);
        initCommentPopRv();
        adapter = new XkshVideoAdapter(R.layout.xksh_video_item_layout, mDatas, getActivity(),
                playerView, refreshLayout, xkshManager);
        adapter.setLoadMoreView(new CustomLoadMoreView());
        adapter.setPreLoadNumber(2);
        adapter.openLoadAnimation();
        handler = new Handler() {
            @Override
            public void dispatchMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case 1:
                        //单击
                        playerView.mWindowPlayer.togglePlayState();
                        break;
                    case 2:
                        //双击
                        if (TextUtils.isEmpty(PersonInfoManager.getInstance().getTransformationToken())) {
                            noLoginTipsPop();
                        } else {
                            loveIcon.startAnimation();
                            if (TextUtils.equals("false", playerView.contentStateModel.getWhetherLike()) && !likeIsRequesting) {
                                likeIsRequesting = true;
                                ImageView likeImage = (ImageView) adapter.getViewByPosition(currentIndex, R.id.video_detail_likes_image);
                                TextView likeNum = (TextView) adapter.getViewByPosition(currentIndex, R.id.likes_num);
                                FinderBuriedPointManager.setFinderLikeFavoriteShare(Constants.CONTENT_LIKE, mDataDTO);
                                addOrCancelLike(myContentId, videoType, likeImage, likeNum);
                            }
                        }
                        break;
                }
            }
        };
        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(final BaseQuickAdapter adapter, View view, int position) {
                if (System.currentTimeMillis() - beforeClickTime < CLICK_INTERVAL_TIME) {
                    handler.removeMessages(1);
                    handler.sendEmptyMessage(2);
                } else {
                    if (!handler.hasMessages(2) && !handler.hasMessages(1))
                        handler.sendEmptyMessageDelayed(1, CLICK_INTERVAL_TIME);
                }
                beforeClickTime = System.currentTimeMillis();
            }
        });

        adapter.setOnLoadMoreListener(requestLoadMoreListener, videoDetailRv);
        /**
         * 无wifi 继续播放点击
         */
        adapter.setToAddPlayerViewClick(new XkshVideoAdapter.ToAddPlayerViewClick() {
            @Override
            public void clickNoWifi(int position) {
                SPUtils.getInstance().put(Constants.AGREE_NETWORK, "1");
                for (int i = 0; i < mDatas.size(); i++) {
                    if (null != mDatas.get(i)) {
                        mDatas.get(i).setWifi(true);
                    }
                }

                for (int i = 0; i < ((VideoHomeActivity) getActivity()).videoDetailFragment.mDatas.size(); i++) {
                    if (null != ((VideoHomeActivity) getActivity()).videoDetailFragment.mDatas.get(i)) {
                        ((VideoHomeActivity) getActivity()).videoDetailFragment.mDatas.get(i).setWifi(true);
                    }
                }
                addPlayView(position);
                adapter.notifyDataSetChanged();
                ((VideoHomeActivity) getActivity()).videoDetailFragment.adapter.notifyDataSetChanged();
            }
        });

        //点赞
        adapter.setlikeListener(new XkshVideoAdapter.LikeListener() {
            @Override
            public void likeClick(View view, DataDTO item, ImageView likeImage, TextView likeNum) {
                FinderBuriedPointManager.setFinderLikeFavoriteShare(Constants.CONTENT_LIKE, mDataDTO);
                if (TextUtils.isEmpty(PersonInfoManager.getInstance().getTransformationToken())) {
                    noLoginTipsPop();
                } else {
                    addOrCancelLike(myContentId, videoType, likeImage, likeNum);
                }
            }
        });

        //收藏
        adapter.setCollectionListener(new XkshVideoAdapter.CollectionListener() {
            @Override
            public void collectionClick(View view, DataDTO item, ImageView collectionImage, TextView collectionNum) {
                FinderBuriedPointManager.setFinderLikeFavoriteShare(Constants.CONTENT_FAVORITE, mDataDTO);
                if (TextUtils.isEmpty(PersonInfoManager.getInstance().getTransformationToken())) {
                    noLoginTipsPop();
                } else {
                    addOrCancelFavor(myContentId, videoType, collectionImage, collectionNum);
                }
            }
        });

        //评论
        adapter.setCommentListener(new XkshVideoAdapter.CommentListener() {
            @Override
            public void commentClick(View view, DataDTO item, TextView commentNum) {
                FinderBuriedPointManager.setFinderClick("评论");
                showCommentPopWindow();
            }
        });

        //转发
        adapter.setShareListener(new XkshVideoAdapter.ShareListener() {
            @Override
            public void shareClick(View view, DataDTO item) {
                FinderBuriedPointManager.setFinderLikeFavoriteShare(Constants.CONTENT_TRANSMIT, mDataDTO);
                FinderBuriedPointManager.setFinderClick("分享");
                sharePop();
            }
        });

        //发布
        adapter.setPublishWorksListener(new XkshVideoAdapter.PublishWorksListener() {
            @Override
            public void publishWorksClick(View view, DataDTO item) {
                //行为埋点 点击视频发布
                if (TextUtils.isEmpty(PersonInfoManager.getInstance().getTransformationToken())) {
                    try {
                        noLoginTipsPop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    startActivity(new Intent(getActivity(), UploadActivity.class));
                }
            }
        });


        /**
         * 关注按钮
         */
        adapter.setFollowViewClick(new XkshVideoAdapter.FollowViewClick() {
            @Override
            public void followClick(int position) {
                if (TextUtils.isEmpty(PersonInfoManager.getInstance().getTransformationToken())) {
                    noLoginTipsPop();
                } else {
                    if (isFollow) {
                        //调用取消关注接口
                        cancelFollow(mDatas.get(position).getCreateBy());
                    } else {
                        //调用关注接口
                        toFollow(mDatas.get(position).getCreateBy());
                    }
                }
                FinderPointModel model = new FinderPointModel();
                model.setUser_id(mDatas.get(position).getCreateBy());
                FinderBuriedPointManager.setFinderCommon(Constants.NOTICE_USER, model);
            }
        });

        //窗口视频双击点击监听
        if (null != playerView) {
            playerView.mWindowPlayer.setOnDoubleClickxksh(new WindowPlayer.DoubleClickxksh() {
                @Override
                public void onDoubleClickxksh(DataDTO item) {
                    if (mIsVisibleToUser) {
                        if (System.currentTimeMillis() - beforeClickTime < CLICK_INTERVAL_TIME) {
                            handler.removeMessages(1);
                            handler.sendEmptyMessage(2);
                        } else {
                            if (!handler.hasMessages(2) && !handler.hasMessages(1))
                                handler.sendEmptyMessageDelayed(1, CLICK_INTERVAL_TIME);
                        }
                        beforeClickTime = System.currentTimeMillis();
                    }
                }
            });
        }

        videoDetailRv.setAdapter(adapter);
    }

    /**
     * 获取单条视频详情
     */
    private void getOneVideo(final String contentId) {
        OkGo.<String>get(ApiConstants.getInstance().getVideoDetailUrl() + contentId)
                .tag(VIDEOTAG)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        if (null == response.body()) {
                            ToastUtils.showShort(R.string.data_err);
                        } else {
                            try {
                                JSONObject jsonObject = new JSONObject(response.body());
                                if (jsonObject.get("code").toString().equals(success_code)) {
                                    String json = jsonObject.optJSONObject("data").toString();
                                    if (null == json || TextUtils.isEmpty(json)) {
                                        ToastUtils.showShort(R.string.data_err);
                                        return;
                                    }
                                    DataDTO dataDTO = JSON.parseObject(json, DataDTO.class);
                                    if (null == dataDTO) {
                                        return;
                                    }

                                    negativeScreenDto = dataDTO;
                                    getPullDownData(mVideoSize, panelCode, "false", Constants.REFRESH_TYPE);
                                } else {
                                    ToastUtils.showShort(jsonObject.get("message").toString());
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        try {
                            if (null == response.body()) {
                                ToastUtils.showShort(R.string.net_err);
                            } else {
                                JSONObject jsonObject = new JSONObject(response.body());
                                ToastUtils.showShort(jsonObject.getString("message"));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }

                    @Override
                    public void onFinish() {
                        super.onFinish();
                    }
                });
    }

    /**
     * 当前fragment是否显示
     *
     * @param isVisibleToUser
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        this.mIsVisibleToUser = isVisibleToUser;
        if (null == playerView || mDatas.isEmpty()) {
            return;
        }
        if (null == adapter) {
            return;
        }
        if (isVisibleToUser) {
            if (!isVisibleNoWifiView(getActivity())) {
                if (null != playerView && null != playerView.getParent()) {
                    ((ViewGroup) playerView.getParent()).removeView(playerView);
                }
                addPlayView(currentIndex);
            }
            getContentState(myContentId);
        } else {
            if (!TextUtils.isEmpty(mDataDTO.getVolcCategory())) {
                if (mDuration != 0 && mProgress != 0) {
                    //上报埋点
                    long evePlayTime = Math.abs(mProgress - lsDuration);
                    float currentPercent = (evePlayTime / mDuration);
                    double uploadPercent = 0;
                    if (null == playerView.buriedPointModel.getXksh_renew() || TextUtils.equals("false", playerView.buriedPointModel.getXksh_renew())) {
//                      //不为重播
                        if (currentPercent > maxPercent) {
                            uploadPercent = currentPercent;
                            maxPercent = currentPercent;
                        } else {
                            uploadPercent = maxPercent;
                        }
                    } else {
                        uploadPercent = 1;
                    }
                    xkshReportTime = DateUtils.getTimeCurrent() - xkshOldSystemTime;
                    BigDecimal two = new BigDecimal(uploadPercent);
                    double pointPercentTwo = two.setScale(2, BigDecimal.ROUND_DOWN).doubleValue();
                    uploadBuriedPoint(ContentBuriedPointManager.setContentBuriedPoint(getActivity(), mDataDTO.getThirdPartyId(), String.valueOf(xkshReportTime), String.valueOf(Math.floor(pointPercentTwo * 100)), Constants.CMS_VIDEO_OVER_AUTO, mDataDTO.getVolcCategory(), mDataDTO.getRequestId()), Constants.CMS_VIDEO_OVER_AUTO);
                    Log.e("xksh_md", "埋点事件：" + Constants.CMS_VIDEO_OVER_AUTO + "播放时长:" + xkshReportTime + "---" + "播放百分比:" + pointPercentTwo);
                }
            }
            String isFinish;
            if (null == playerView.buriedPointModel.getIs_renew() || TextUtils.equals("false", playerView.buriedPointModel.getIs_renew())) {
                isFinish = "否";
            } else {
                isFinish = "是";
            }
            FinderBuriedPointManager.setFinderVideo(Constants.CONTENT_VIDEO_DURATION, "", mDataDTO, xkshReportTime, isFinish);


            playerView.mSuperPlayer.pause();
            if (null != rlLp) {
                rlLp.removeView(playerView);
            }
        }
        lsDuration = 0;
        maxPercent = 0;
        //重置重播标识
        if (null != playerView && null != playerView.buriedPointModel) {
            playerView.buriedPointModel.setXksh_renew("false");
        }
    }

    /**
     * 获取活动规则
     */
    private void getActivityRule() {
        OkGo.<ActivityRuleBean>get(ApiConstants.getInstance().getActivityRule())
                .tag(VIDEOTAG)
                .params("panelCode", "activity.xksh.link")
                .cacheMode(CacheMode.NO_CACHE)
                .execute(new JsonCallback<ActivityRuleBean>() {
                    @Override
                    public void onSuccess(Response<ActivityRuleBean> response) {
                        if (null == response.body()) {
                            ToastUtils.showShort(R.string.data_err);
                            return;
                        }

                        try {
                            if (response.body().getCode().equals(success_code)) {
                                activityRuleBean = response.body();
                                if (null == activityRuleBean || null == activityRuleBean.getData()) {
                                    return;
                                }

                                if (null != activityRuleBean.getData().getConfig().getJumpUrl()
                                        && !TextUtils.isEmpty(activityRuleBean.getData().getConfig().getJumpUrl())) {
                                    ((SlidingTabLayout) getActivity().findViewById(R.id.video_tab)).showMsg(0, "活动");
                                    ((SlidingTabLayout) getActivity().findViewById(R.id.video_tab)).setMsgMargin(0, ButtonSpan.dip2px(7), ButtonSpan.dip2px(10));
                                    if (null != activityRuleImg) {
                                        activityRuleImg.setVisibility(View.VISIBLE);
                                    }
                                    if (null != activityToAbbreviation) {
                                        activityToAbbreviation.setVisibility(View.VISIBLE);
                                    }
                                }

                                /**
                                 * 设置活动规则图，缩略图
                                 */
                                if (null != getActivity() && !getActivity().isFinishing()
                                        && !getActivity().isDestroyed()) {
                                    Glide.with(getActivity())
                                            .load(activityRuleBean.getData().getConfig().getImageUrl())
                                            .into(activityRuleImg);
                                    Glide.with(getActivity())
                                            .load(activityRuleBean.getData().getConfig().getBackgroundImageUrl())
                                            .into(activityRuleAbbreviation);
                                }

                                /**
                                 * 活动规则图点击 跳转活动链接
                                 */
                                activityRuleImg.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        try {
                                            FinderBuriedPointManager.setFinderClick("活动规则");
                                            param.recommendUrl(activityRuleBean.getData().getConfig().getJumpUrl(), null);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });

                                /**
                                 * 变成缩略图
                                 */
                                activityToAbbreviation.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (null != activityRuleImg) {
                                            activityRuleImg.setVisibility(View.GONE);
                                        }
                                        if (null != activityRuleAbbreviation) {
                                            activityRuleAbbreviation.setVisibility(View.VISIBLE);
                                        }
                                        isAbbreviation = true;
                                    }
                                });

                                /**
                                 * 点击展示完整活动图
                                 */
                                activityRuleAbbreviation.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (null != activityRuleImg) {
                                            activityRuleImg.setVisibility(View.VISIBLE);
                                        }
                                        if (null != activityRuleAbbreviation) {
                                            activityRuleAbbreviation.setVisibility(View.GONE);
                                        }

                                        isAbbreviation = false;
                                    }
                                });

                            } else {
                                ToastUtils.showShort(response.body().getMessage());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Response<ActivityRuleBean> response) {
                        super.onError(response);
                    }

                    @Override
                    public void onFinish() {
                        super.onFinish();
                    }
                });
    }


    /**
     * 在视频playview位置上添加各种view
     *
     * @param position
     */
    public void addPlayView(final int position) {
        if (!mIsVisibleToUser) {
            return;
        }

        if (isVisibleNoWifiView(getActivity())) {
            return;
        }

        if (null != playerView.mWindowPlayer && null != playerView.mWindowPlayer.mLayoutBottom && null != playerView.mWindowPlayer.mLayoutBottom.getParent()) {
            ((ViewGroup) playerView.mWindowPlayer.mLayoutBottom.getParent()).removeView(playerView.mWindowPlayer.mLayoutBottom);
        }

        if (null != playerView && null != playerView.getParent()) {
            ((ViewGroup) playerView.getParent()).removeView(playerView);
        }
        LinearLayout linearLayout = (LinearLayout) adapter.getViewByPosition(currentIndex, R.id.introduce_lin);
//        RelativeLayout.LayoutParams mLayoutBottomParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        RelativeLayout itemRelativelayout = (RelativeLayout) adapter.getViewByPosition(position, R.id.item_relativelayout);

        String videoType = videoIsNormal(Integer.parseInt(NumberFormatTool.getNumStr(mDatas.get(position).getWidth())),
                Integer.parseInt(NumberFormatTool.getNumStr(mDatas.get(position).getHeight())));
        if (TextUtils.equals("0", videoType)) {
            double percent = Double.parseDouble(mDatas.get(position).getWidth()) / Double.parseDouble(mDatas.get(position).getHeight());
            double mHeight;
            mHeight = getResources().getDisplayMetrics().widthPixels / percent;
            playViewParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) mHeight);
            playViewParams.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
            playViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            playViewParams.setMargins(0, 0, 0, 0);
            playerView.mSuperPlayer.setRenderMode(TXLiveConstants.RENDER_MODE_FULL_FILL_SCREEN);

//            int height = (int) (Integer.parseInt(mDatas.get(position).getWidth()) / Constants.Portrait_Proportion);


            playerView.setOrientation(false);
            if (null != itemRelativelayout) {
                itemRelativelayout.addView(playerView.mWindowPlayer.mLayoutBottom);
            }
        } else if (TextUtils.equals("1", videoType)) {
            int height = (int) (ScreenUtils.getPhoneWidth(getActivity()) / Constants.Portrait_Proportion);
            playViewParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
            if (phoneIsNormal()) {
                playViewParams.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
                playViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                playerView.mSuperPlayer.setRenderMode(TXLiveConstants.RENDER_MODE_ADJUST_RESOLUTION);
                playerView.setOrientation(false);
            } else {
                playViewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                playerView.mSuperPlayer.setRenderMode(TXLiveConstants.RENDER_MODE_FULL_FILL_SCREEN);
                playerView.setOrientation(false);
            }
            if (null != itemRelativelayout) {
                itemRelativelayout.addView(playerView.mWindowPlayer.mLayoutBottom);
            }
        } else {
            int height = (int) (ScreenUtils.getPhoneWidth(getActivity()) / Constants.Horizontal_Proportion);
            playViewParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
            playViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            playerView.mSuperPlayer.setRenderMode(TXLiveConstants.RENDER_MODE_ADJUST_RESOLUTION);
            playerView.setOrientation(true);
            if (null != itemRelativelayout) {
                itemRelativelayout.addView(playerView.mWindowPlayer.mLayoutBottom);
            }
        }
        playerView.setLayoutParams(playViewParams);
        playerView.setTag(position);

        if (rlLp != null && mIsVisibleToUser) {
            rlLp.addView(playerView, 1);
            //露出即上报
            if (!TextUtils.isEmpty(mDataDTO.getVolcCategory())) {
                uploadBuriedPoint(ContentBuriedPointManager.setContentBuriedPoint(getActivity(), mDataDTO.getThirdPartyId(), "", "", Constants.CMS_CLIENT_SHOW, mDataDTO.getVolcCategory(), mDataDTO.getRequestId()), Constants.CMS_CLIENT_SHOW);
            }
            play(mDatas.get(position).getPlayUrl(), mDatas.get(position).getTitle());
        }
    }

    private void initSmartRefresh(View view) {
        refreshLayout = view.findViewById(R.id.refreshLayout);
        refreshLayout.setEnableScrollContentWhenLoaded(true);
        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(RefreshLayout refreshlayout) {
                if (playerView.mSuperPlayer.getPlayerState() == SuperPlayerDef.PlayerState.PLAYING) {
                    playerView.mSuperPlayer.pause();
                }
                isLoadComplate = false;
                adapter.setOnLoadMoreListener(requestLoadMoreListener, videoDetailRv);
                getPullDownData(String.valueOf(mVideoSize), panelCode, "false", Constants.REFRESH_TYPE);
                //重置重播标识
                if (null != playerView && null != playerView.buriedPointModel) {
                    playerView.buriedPointModel.setXksh_renew("false");
                }
            }
        });
        requestLoadMoreListener = new BaseQuickAdapter.RequestLoadMoreListener() {
            @Override
            public void onLoadMoreRequested() {
                if (!isLoadComplate) {
                    videoDetailRv.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mDatas.isEmpty()) {
                                adapter.loadMoreFail();
                                return;
                            }
                            loadMoreData(ApiConstants.getInstance().getVideoDetailListUrl(), mDatas.get(mDatas.size() - 1).getId() + "", panelCode, "true", Constants.LOADMORE_TYPE);
                        }
                    });
                }
            }
        };
    }

    /**
     * 视频是否是16：9
     * 0 :  竖版视频非16：9
     * 1 ：  竖版视频16：9
     * 2 ：  横板视频
     */
    private String videoIsNormal(int videoWidth, int videoHeight) {
        if (videoWidth == 0 && videoHeight == 0) {
            return "2";
        }
        if (videoWidth > videoHeight) {
            //横板
            if (videoWidth * 9 == videoHeight * 16) {
                return "2";
            } else {
                return "0";
            }
        } else {
            //竖版
            if (videoHeight * 9 == videoWidth * 16) {
                return "1";
            } else {
                return "0";
            }
        }
    }

    /**
     * 手机是否为16：9
     *
     * @return
     */
    private boolean phoneIsNormal() {
        int phoneWidth = ScreenUtils.getPhoneWidth(getActivity());
        int phoneHeight = ScreenUtils.getPhoneHeight(getActivity());
        if (phoneHeight * 9 == phoneWidth * 16) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 初始化评论列表
     */
    private void initCommentPopRv() {
        mCommentPopRvData = new ArrayList<>();
        mCommentPopDtoData = new ArrayList<>();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        commentPopRv.setLayoutManager(linearLayoutManager);
        commentPopRvAdapter = new CommentPopRvAdapter(mCommentPopRvData, getActivity());
        commentPopRvAdapter.bindToRecyclerView(commentPopRv);
        commentPopRvAdapter.setEmptyView(R.layout.comment_list_empty);
        commentPopRvAdapter.expandAll();
        commentPopRv.setAdapter(commentPopRvAdapter);
        commentPopRvAdapter.setOnLoadMoreListener(new BaseQuickAdapter.RequestLoadMoreListener() {
            @Override
            public void onLoadMoreRequested() {
                commentPopRv.post(new Runnable() {
                    @Override
                    public void run() {
                        mPageIndex++;
                        getCommentList(String.valueOf(mPageIndex), String.valueOf(mPageSize), false);
                    }
                });
            }
        }, commentPopRv);

    }


    /**
     * 评论列表弹出框
     */
    private void showCommentPopWindow() {
        if (null == popupWindow) {
            //创建并显示popWindow
            popupWindow = new CustomPopWindow.PopupWindowBuilder(getActivity())
                    .setView(contentView)
                    .setOutsideTouchable(false)
                    .setFocusable(true)
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
                    .size(AppInit.getContext().getResources().getDisplayMetrics().widthPixels, AppInit.getContext().getResources().getDisplayMetrics().heightPixels - ButtonSpan.dip2px(200))
                    .setAnimationStyle(R.style.take_popwindow_anim)
                    .create()
                    .showAtLocation(rootView, Gravity.BOTTOM, 0, 0);
        } else {
            popupWindow.showAtLocation(rootView, Gravity.BOTTOM, 0, 0);
        }
//        SystemUtils.hideBottomUIMenuForPopupWindow(popupWindow);
        popupWindow.getPopupWindow().setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
//                SystemUtils.hideSystemUI(decorView);
            }
        });

    }

    /**
     * 选集弹窗
     */
    public void showChoosePop() {
        if (null == choosePop) {
            //创建并显示popWindow
            choosePop = new CustomPopWindow.PopupWindowBuilder(getActivity())
                    .setView(chooseContentView)
                    .setOutsideTouchable(false)
                    .setFocusable(true)
                    .setAnimationStyle(R.style.take_popwindow_anim)
                    .create()
                    .showAtLocation(getActivity().getWindow().getDecorView(), Gravity.CENTER, 0, 0);
        } else {
            choosePop.showAtLocation(getActivity().getWindow().getDecorView(), Gravity.CENTER, 0, 0);
        }

        choosePop.getPopupWindow().setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
//                SystemUtils.hideSystemUI(decorView);
            }
        });
    }

    /**
     * 分享弹窗
     */
    private void sharePop() {
        if (null == sharePop) {
            sharePop = new CustomPopWindow.PopupWindowBuilder(getActivity())
                    .setView(sharePopView)
                    .setOutsideTouchable(true)
                    .setFocusable(true)
                    .size(AppInit.getContext().getResources().getDisplayMetrics().widthPixels, ButtonSpan.dip2px(150))
                    .setAnimationStyle(R.style.take_popwindow_anim)
                    .create()
                    .showAtLocation(rootView, Gravity.BOTTOM, 0, 0);
        } else {
            sharePop.showAtLocation(rootView, Gravity.BOTTOM, 0, 0);
        }
    }

    public void toShare(DataDTO item, String platform) {
        VideoInteractiveParam param = VideoInteractiveParam.getInstance();
        ShareInfo shareInfo = ShareInfo.getInstance(item.getShareUrl(), item.getShareImageUrl(),
                item.getShareBrief(), item.getShareTitle(), platform);
        try {
            param.shared(shareInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 播放视频
     *
     * @param playUrl
     */
    public void play(String playUrl, String title) {
//        if (null == playUrl || TextUtils.isEmpty(playUrl)) {
//            ToastUtils.showShort("当前播放地址(" + playUrl + "),是一个无效地址");
//            return;
//        }
        if (null != playerView) {
//            videoStaticBg.setVisibility(View.GONE);
            SuperPlayerModel model = new SuperPlayerModel();
            model.url = playUrl;
            model.title = title;
            model.contentId = myContentId;
            playerView.playWithModel(model);
        }
    }

    public void stop() {
        if (null != playerView && null != playerView.getParent()) {
            ((ViewGroup) playerView.getParent()).removeView(playerView);
        }
        playerView.stopPlay();
    }

    public void reset() {
        if (null != playerView && null != playerView.getParent()) {
            ((ViewGroup) playerView.getParent()).removeView(playerView);
        }

        if (null != playerView) {
            playerView.resetPlayer();
        }
    }

    /**
     * 获取下拉列表数据
     *
     * @param removeFirst
     */
    private void getPullDownData(String pageSize, String panelCode, String removeFirst, String refreshType) {
        String deviceId = "";
        if (null != playerView && null != playerView.getParent()) {
            ((ViewGroup) playerView.getParent()).removeView(playerView);
        }
        mDatas.clear();
        //从负一屏 拿到的视频  添加到集合里
        if (null != negativeScreenDto) {
            negativeScreenDto.setVolcCategory(mCategoryName);
            negativeScreenDto.setIsAutoReportEvent("1");
            mDatas.add(negativeScreenDto);
        }
        try {
            deviceId = param.getDeviceId();
        } catch (Exception e) {
            e.printStackTrace();
        }
        OkGo.<VideoDetailModel>get(ApiConstants.getInstance().getVideoDetailListUrl())
                .tag(VIDEOTAG)
                .params("pageSize", pageSize)
                .params("panelCode", panelCode)
                .params("removeFirst", removeFirst)
                .params("ssid", deviceId)
                .params("refreshType", refreshType)
                .params("type", "")
                .cacheMode(CacheMode.NO_CACHE)
                .execute(new JsonCallback<VideoDetailModel>(VideoDetailModel.class) {
                    @Override
                    public void onSuccess(Response<VideoDetailModel> response) {
                        if (null == response.body()) {
                            ToastUtils.showShort(R.string.data_err);
                            return;
                        }

                        try {
                            if (response.body().getCode().equals(success_code)) {
                                if (null == response.body().getData()) {
                                    ToastUtils.showShort(R.string.data_err);
                                    return;
                                }

                                mDatas.addAll(response.body().getData());
                                for (int i = 0; i < mDatas.size(); i++) {
                                    String videoType = videoIsNormal(Integer.parseInt(NumberFormatTool.getNumStr(mDatas.get(i).getWidth())),
                                            Integer.parseInt(NumberFormatTool.getNumStr(mDatas.get(i).getHeight())));
                                    mDatas.get(i).setLogoType(videoType);
                                }
                                setDataWifiState(mDatas, getActivity());
                                adapter.setNewData(mDatas);
                                if (mDatas.size() > 0) {
                                    initialize = false;
                                }
                            }
                            if (null != refreshLayout) {
                                refreshLayout.finishRefresh();
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Response<VideoDetailModel> response) {
                        if (null != response.body()) {
                            ToastUtils.showShort(response.body().getMessage());
                            return;
                        }
                        ToastUtils.showShort(R.string.net_err);
                    }

                    @Override
                    public void onFinish() {
                        super.onFinish();
                        getActivityRule();
                    }
                });
    }


    /**
     * 获取更多数据
     */
    private void loadMoreData(String url, String contentId, String panelCode, String removeFirst, String refreshType) {
        String deviceId = "";
        try {
            deviceId = param.getDeviceId();
        } catch (Exception e) {
            e.printStackTrace();
        }
        OkGo.<VideoDetailModel>get(url)
                .tag(VIDEOTAG)
                .params("contentId", contentId)
                .params("pageSize", 10)
                .params("panelCode", panelCode)
                .params("removeFirst", removeFirst)
                .params("ssid", deviceId)
                .params("refreshType", refreshType)
                .params("type", "")
                .cacheMode(CacheMode.NO_CACHE)
                .execute(new JsonCallback<VideoDetailModel>(VideoDetailModel.class) {
                    @Override
                    public void onSuccess(Response<VideoDetailModel> response) {
                        if (null == response.body()) {
                            ToastUtils.showShort(R.string.data_err);
                            return;
                        }

                        try {
                            if (response.body().getCode().equals(success_code)) {
                                if (null == response.body().getData()) {
                                    isLoadComplate = true;
                                    ToastUtils.showShort(R.string.data_err);
                                    return;
                                }

                                if (response.body().getData().size() == 0) {
                                    adapter.loadMoreComplete();
                                    adapter.setOnLoadMoreListener(null, videoDetailRv);
                                    if (null != footerView && null != footerView.getParent()) {
                                        ((ViewGroup) footerView.getParent()).removeView(footerView);
                                    }
                                    adapter.addFooterView(footerView);
                                    isLoadComplate = true;
                                } else {
                                    adapter.setOnLoadMoreListener(requestLoadMoreListener, videoDetailRv);
                                    isLoadComplate = false;
                                }
                                mDatas.addAll(response.body().getData());
                                setDataWifiState(mDatas, getActivity());
                                adapter.loadMoreComplete();
                            } else {
                                adapter.loadMoreFail();
                            }
                            if (null != refreshLayout) {
                                refreshLayout.finishRefresh();
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Response<VideoDetailModel> response) {
                        if (null != response.body()) {
                            ToastUtils.showShort(response.body().getMessage());
                            return;
                        }
                        ToastUtils.showShort(R.string.net_err);
                    }

                    @Override
                    public void onFinish() {
                        super.onFinish();
                        refreshLayout.setEnableRefresh(true);
                    }
                });
    }


    /**
     * 获取评论列表
     */
    public void getCommentList(String pageIndex, String pageSize, final boolean isRefresh) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("contentId", myContentId);
            jsonObject.put("pageIndex", pageIndex);
            jsonObject.put("pageSize", pageSize);
            jsonObject.put("pcommentId", "0");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        OkGo.<CommentLv1Model>post(ApiConstants.getInstance().getCommentWithReply())
                .tag(VIDEOTAG)
                .upJson(jsonObject)
                .headers("token", PersonInfoManager.getInstance().getTransformationToken())
                .cacheMode(CacheMode.NO_CACHE)
                .execute(new JsonCallback<CommentLv1Model>(CommentLv1Model.class) {
                    @Override
                    public void onSuccess(Response<CommentLv1Model> response) {
                        if (null == response.body()) {
                            ToastUtils.showShort(R.string.data_err);
                            return;
                        }

                        try {
                            if (response.body().getCode().equals("200")) {
                                if (null == response.body().getData()) {
                                    ToastUtils.showShort(R.string.data_err);
                                    return;
                                }

                                if (isRefresh) {
                                    mCommentPopRvData.clear();
                                    mCommentPopDtoData.clear();
                                }

                                //评论集合
                                List<CommentLv1Model.DataDTO.RecordsDTO> lv1List = response.body().getData().getRecords();
                                for (int i = 0; i < lv1List.size(); i++) {
                                    CommentLv1Model.DataDTO.RecordsDTO lv1Model = lv1List.get(i);
                                    lv1Model.setPosition(i);
                                    lv1Model.setShow(true);
                                    List<ReplyLv2Model.ReplyListDTO> lv2List = lv1Model.getReply().getReplyList();
                                    for (int j = 0; j < lv2List.size(); j++) {
                                        ReplyLv2Model.ReplyListDTO lv2Model = lv2List.get(j);
                                        lv2Model.setPosition(j);
                                        lv2Model.setParentPosition(i);
                                        lv1Model.addSubItem(lv2Model);
                                    }
                                    mCommentPopRvData.add(lv1Model);
                                }

                                mCommentPopDtoData.addAll(lv1List);
                                commentPopRvAdapter.setContentId(myContentId);
                                commentPopRvAdapter.setSrc(mCommentPopRvData);
                                commentPopRvAdapter.setNewData(mCommentPopRvData);

                                //第一级评论点击
                                commentPopRvAdapter.setLv1CommentClick(new CommentPopRvAdapter.Lv1CommentClick() {
                                    @Override
                                    public void Lv1Comment(String id, String replyName) {
                                        toSetHint(id, replyName);
                                    }
                                });

                                //第一级评论第一条回复点击
                                commentPopRvAdapter.setLv1No1Click(new CommentPopRvAdapter.Lv1No1Click() {
                                    @Override
                                    public void lv1No1Click(String id, String replyName) {
                                        toSetHint(id, replyName);
                                    }
                                });

                                //第一级评论第二条回复点击
                                commentPopRvAdapter.setLv1No2Click(new CommentPopRvAdapter.Lv1No2Click() {
                                    @Override
                                    public void lv1No2Click(String id, String replyName) {
                                        toSetHint(id, replyName);
                                    }
                                });

                                //第二级回复点击
                                commentPopRvAdapter.setLv2ReplyClick(new CommentPopRvAdapter.Lv2ReplyClick() {
                                    @Override
                                    public void Lv2ReplyClick(String id, String replyName) {
                                        toSetHint(id, replyName);
                                    }
                                });

                                TextView commentNum = (TextView) adapter.getViewByPosition(currentIndex, R.id.comment_num);
                                if (mCommentPopDtoData.isEmpty()) {
                                    if (null != commentNum) {
                                        commentNum.setText("评论");
                                    }
                                    commentPopCommentTotal.setText("(0)");
                                } else {
                                    if (null != commentNum) {
                                        commentNum.setText(response.body().getData().getTotal());
                                    }
                                    commentPopCommentTotal.setText("(" + response.body().getData().getTotal() + ")");
                                }

                                if (response.body().getData().getRecords().size() == 0) {
                                    commentPopRvAdapter.loadMoreEnd();
                                } else {
                                    commentPopRvAdapter.loadMoreComplete();
                                }

                            } else {
                                commentPopRvAdapter.loadMoreFail();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }

                    @Override
                    public void onError(Response<CommentLv1Model> response) {
                        commentPopRvAdapter.loadMoreFail();
                    }
                });
    }

    private void toSetHint(String id, String replyName) {
        if (TextUtils.isEmpty(PersonInfoManager.getInstance().getTransformationToken())) {
            try {
                noLoginTipsPop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            KeyboardUtils.toggleSoftInput(getActivity().getWindow().getDecorView());
            showInputEdittextAndSend();
            edtInput.setHint("回复@" + replyName);
            isReply = true;
            replyId = id;
        }
    }

    /**
     * 评论
     */
    private void toComment(String content, String contentId) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("contentId", contentId);
            jsonObject.put("content", content);
            jsonObject.put("title", "");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        OkGo.<String>post(ApiConstants.getInstance().addComment())
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
                            JSONObject mJsonObject = new JSONObject(response.body());
                            String code = mJsonObject.get("code").toString();

                            if (code.equals(success_code)) {
                                if (!mDatas.isEmpty()) {
                                    String jsonString = BuriedPointModelManager.getVideoComment(myContentId, mDatas.get(currentIndex).getTitle(), "", "",
                                            "", "", mDatas.get(currentIndex).getIssueTimeStamp(), Constants.CONTENT_TYPE);
                                    Log.e("埋点", "埋点：评论---" + jsonString);
                                }
                                ToastUtils.showShort("评论已提交，请等待审核通过！");
                                if (null != inputAndSendPop) {
                                    inputAndSendPop.dissmiss();
                                }
                                KeyboardUtils.hideKeyboard(getActivity().getWindow().getDecorView());
                                mPageIndex = 1;
                                getCommentList("1", String.valueOf(mPageSize), true);
                            } else if (code.equals(token_error)) {
                                Log.e("addComment", "无token 去跳登录");
                                try {
                                    param.toLogin();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                if (null != mJsonObject.getString("message")) {
                                    ToastUtils.showShort(mJsonObject.getString("message"));
                                } else {
                                    ToastUtils.showShort("评论失败");
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            ToastUtils.showShort("评论失败");
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        ToastUtils.showShort("评论失败");
                    }

                    @Override
                    public void onFinish() {
                        super.onFinish();
                        edtInput.setText("");
                    }
                });
    }

    /**
     * 回复
     */
    private void toReply(String id) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", id);
            jsonObject.put("reply", edtInput.getText().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        OkGo.<String>post(ApiConstants.getInstance().addUserReply())
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
                            JSONObject mJsonObject = new JSONObject(response.body());
                            String code = mJsonObject.get("code").toString();

                            if (code.equals(success_code)) {
                                ToastUtils.showShort("回复已提交，请等待审核通过！");
                                if (null != inputAndSendPop) {
                                    inputAndSendPop.dissmiss();
                                }
                                mPageIndex = 1;
                                KeyboardUtils.hideKeyboard(getActivity().getWindow().getDecorView());
                                getCommentList("1", String.valueOf(mPageSize), true);
                            } else if (code.equals(token_error)) {
                                try {
                                    param.toLogin();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                if (null != mJsonObject.getString("message")) {
                                    ToastUtils.showShort(mJsonObject.getString("message"));
                                } else {
                                    ToastUtils.showShort("回复失败");
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            ToastUtils.showShort("回复失败");
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                    }

                    @Override
                    public void onFinish() {
                        super.onFinish();
                    }
                });
    }


    /**
     * 获取收藏点赞状态
     */
    public void getContentState(String contentId) {
        OkGo.<ContentStateModel>get(ApiConstants.getInstance().queryStatsData())
                .tag(VIDEOTAG)
                .headers("token", PersonInfoManager.getInstance().getTransformationToken())
                .params("contentId", contentId)
                .cacheMode(CacheMode.NO_CACHE)
                .execute(new JsonCallback<ContentStateModel>(ContentStateModel.class) {
                    @Override
                    public void onSuccess(Response<ContentStateModel> response) {
                        if (null == response.body()) {
                            ToastUtils.showShort(R.string.data_err);
                            return;
                        }

                        try {
                            if (response.body().getCode().equals(success_code)) {
                                if (null == response.body().getData()) {
                                    ToastUtils.showShort(R.string.data_err);
                                    return;
                                }

                                playerView.contentStateModel = response.body().getData();
                                if (null != playerView.contentStateModel) {
                                    setLikeCollection(playerView.contentStateModel);
                                    playerView.setContentStateModel(myContentId, videoType);
                                }
                            } else {
                                ToastUtils.showShort(response.body().getMessage());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Response<ContentStateModel> response) {
                        if (null != response.body()) {
                            ToastUtils.showShort(response.body().getMessage());
                            return;
                        }
                        ToastUtils.showShort(R.string.net_err);
                    }
                });
    }

    /**
     * 获取专题合集标签
     */
    public void getThematicCollection(String contentId) {
        String belongTopicId = mDataDTO.getBelongTopicId();
        if (TextUtils.isEmpty(belongTopicId)) {
            belongTopicId = "0";
        }
        OkGo.<CollectionLabelModel>get(ApiConstants.getInstance().getCollectToVideo() + contentId + "/" + belongTopicId)
                .tag(VIDEOTAG)
                .headers("token", PersonInfoManager.getInstance().getTransformationToken())
                .cacheMode(CacheMode.NO_CACHE)
                .execute(new JsonCallback<CollectionLabelModel>(CollectionLabelModel.class) {
                    @Override
                    public void onSuccess(Response<CollectionLabelModel> response) {
                        if (null == response.body()) {
                            ToastUtils.showShort(R.string.data_err);
                            return;
                        }

                        try {
                            if (response.body().getCode().equals(success_code)) {
                                if (null == response.body().getData()) {
                                    return;
                                }
                                topicName = response.body().getData().getTopicName();
                                collectionList = new ArrayList<>();
                                collectionTvList = new ArrayList<>();
                                collectionStrList = new ArrayList<>();
                                String collectionStr = "";
                                if (null == response.body().getData().getList()) {
                                    List<CollectionLabelModel.DataDTO.ListDTO> listDTO = new ArrayList<>();
                                    collectionList.addAll(listDTO);
                                } else {
                                    collectionList.addAll(response.body().getData().getList());
                                }

                                if (!TextUtils.isEmpty(topicName)) {
                                    CollectionLabelModel.DataDTO.ListDTO listDTO = new CollectionLabelModel.DataDTO.ListDTO();
                                    listDTO.setTitle("#" + topicName);
                                    listDTO.setId("");
                                    collectionList.add(listDTO);
                                }

                                for (int i = 0; i < collectionList.size(); i++) {
                                    collectionStr = collectionStr + collectionList.get(i).getTitle();
                                    collectionStrList.add(collectionList.get(i).getTitle());
                                    if (collectionList.size() == 1) {
                                        collectionTvList.add("  " + collectionList.get(i).getTitle());
                                    } else {
                                        if (i == 0) {
                                            collectionTvList.add("  " + collectionList.get(i).getTitle() + "｜");
                                        } else {
                                            if (i == collectionList.size() - 1) {
                                                collectionTvList.add(collectionList.get(i).getTitle());
                                            } else {
                                                collectionTvList.add(collectionList.get(i).getTitle() + "｜");
                                            }
                                        }
                                    }
                                }
                                TextView foldTextView = (TextView) adapter.getViewByPosition(currentIndex, R.id.fold_text);
                                TextView expendTextView = (TextView) adapter.getViewByPosition(currentIndex, R.id.expend_text);
                                String brief = "";
                                String spaceStr = "";
                                DataDTO item = adapter.getItem(currentIndex);
                                if (null == item) {
                                    return;
                                }
                                if (TextUtils.isEmpty(adapter.getItem(currentIndex).getBrief())) {
                                    brief = item.getTitle();
                                } else {
                                    brief = item.getBrief();
                                }
                                SpannableStringBuilder builder = new SpannableStringBuilder();
                                if (collectionList.isEmpty()) {
                                    return;
                                } else {
                                    for (int i = 0; i < collectionList.size(); i++) {
                                        ImageSpan imgSpan = null;
                                        if (!TextUtils.isEmpty(collectionList.get(i).getId())) {
                                            imgSpan = new ImageSpan(getActivity(),
                                                    R.drawable.szrm_sdk_collection_image,
                                                    ImageSpan.ALIGN_CENTER);
                                        }

                                        final String str = collectionTvList.get(i);
                                        final String strChun = collectionStrList.get(i);
                                        SpannableString sp = new SpannableString(str);
                                        if (i == 0 && !TextUtils.isEmpty(collectionList.get(i).getId())) {
                                            sp.setSpan(imgSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                        }
                                        final String classId = String.valueOf(collectionList.get(i).getId());
                                        /**
                                         * 每一个合集标签点击事件
                                         */
                                        sp.setSpan(new CollectionClickble(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                //合集标签点击事件
                                                if (TextUtils.isEmpty(classId) && str.contains("#")) {
                                                    adapter.setTopicClick(false);
                                                    Log.e("topic", "点击的话题");
                                                } else {
                                                    adapter.setTopicClick(true);
                                                    Intent intent = new Intent(getActivity(), VideoDetailActivity.class);
                                                    intent.putExtra("classId", classId);
                                                    intent.putExtra("className", strChun.trim());
                                                    startActivity(intent);
                                                    FinderBuriedPointManager.setFinderClick("集合_" + strChun);
                                                }
                                            }
                                        }, getActivity()), 0, sp.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                        if (i == collectionList.size() - 1) {
                                            builder.append(sp);
                                            builder.append("  " + brief);
                                        } else {
                                            builder.append(sp);
                                        }
                                    }
                                    if (null != foldTextView && null != expendTextView) {
                                        foldTextView.setMovementMethod(LinkMovementMethod.getInstance());
                                        foldTextView.setText(builder);
                                        expendTextView.setMovementMethod(LinkMovementMethod.getInstance());
                                        expendTextView.setText(builder);
                                    }
                                }
                            } else {
                                ToastUtils.showShort(response.body().getMessage());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Response<CollectionLabelModel> response) {
                        super.onError(response);
                        if (null != response.body()) {
                            ToastUtils.showShort(response.body().getMessage());
                            return;
                        }
                        ToastUtils.showShort(R.string.net_err);
                    }

                    @Override
                    public void onFinish() {
                        super.onFinish();
                    }
                });
    }

    public void setLikeCollection(ContentStateModel.DataDTO contentStateModel) {
        ImageView collectionImage = (ImageView) adapter.getViewByPosition(currentIndex, R.id.video_detail_collection_image);
        TextView collectionNum = (TextView) adapter.getViewByPosition(currentIndex, R.id.collection_num);
        ImageView likeImage = (ImageView) adapter.getViewByPosition(currentIndex, R.id.video_detail_likes_image);
        TextView likeNum = (TextView) adapter.getViewByPosition(currentIndex, R.id.likes_num);

        if (null != collectionImage) {
            if (contentStateModel.getWhetherFavor().equals("true")) {
                collectionImage.setImageResource(R.drawable.collection);
            } else {
                collectionImage.setImageResource(R.drawable.szrm_sdk_collection_icon);
            }
        }

        if (null != collectionNum) {
            collectionNum.setText(NumberFormatTool.formatNum(Long.parseLong(NumberFormatTool.getNumStr(contentStateModel.getFavorCountShow())), false));
        }

        if (null != likeImage) {
            if (contentStateModel.getWhetherLike().equals("true")) {
                likeImage.setImageResource(R.drawable.szrm_sdk_favourite_select);
            } else {
                likeImage.setImageResource(R.drawable.szrm_sdk_favourite);
            }
        }

        if (null != likeNum) {
            if (contentStateModel.getLikeCountShow().equals("0")) {
                likeNum.setText("赞");
            } else {
                likeNum.setText(NumberFormatTool.formatNum(Long.parseLong(NumberFormatTool.getNumStr(contentStateModel.getLikeCountShow())), false));
            }
        }

        TextView followView = (TextView) adapter.getViewByPosition(currentIndex, R.id.follow);
        if (null != followView) {
            if (contentStateModel.getWhetherFollow().equals("true")) {
                followView.setBackgroundResource(R.drawable.followed_bg);
                followView.setText("已关注");
                isFollow = true;
            } else {
                adapter.getViewByPosition(currentIndex, R.id.follow).setBackgroundResource(R.drawable.follow_bg);
                followView.setText("关注");
                isFollow = false;
            }
        }
    }

    /**
     * 设置关注
     */
    private void setFollowView(String whetherFollow) {
        /**
         * 设置关注
         */
        TextView followText = (TextView) adapter.getViewByPosition(currentIndex, R.id.follow);
        String localUserId = PersonInfoManager.getInstance().getUserId();
        String userId = mDataDTO.getCreateBy();

        if (null == followText) {
            return;
        }
        if (TextUtils.equals(whetherFollow, "true")) {
            //已关注
            isFollow = true;
            followText.setText("已关注");
            followText.setBackgroundResource(R.drawable.followed_bg);
        } else {
            //未关注
            isFollow = false;
            followText.setText("关注");
            followText.setBackgroundResource(R.drawable.follow_bg);
        }
    }


    /**
     * 收藏/取消收藏
     */
    private void addOrCancelFavor(String contentId, String type, final ImageView collectionImage, final TextView collectionNum) {
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

                        if (null == playerView) {
                            return;
                        }

                        try {
                            JSONObject json = new JSONObject(response.body());
                            if (json.get("code").toString().equals(success_code)) {
                                if (!mDatas.isEmpty()) {
                                    String jsonString = BuriedPointModelManager.getLikeAndFavorBuriedPointData(myContentId, mDatas.get(currentIndex).getTitle(),
                                            "", "", "", "", mDatas.get(currentIndex).getIssueTimeStamp(),
                                            Constants.CONTENT_TYPE);
                                    Log.e("埋点", "埋点：收藏---" + jsonString);
                                }

                                if (json.get("data").toString().equals("1")) {
                                    int num;
                                    num = Integer.parseInt(NumberFormatTool.getNumStr(collectionNum.getText().toString()));
                                    num++;
                                    collectionNum.setText(NumberFormatTool.formatNum(num, false));
                                    collectionImage.setImageResource(R.drawable.collection);
                                    playerView.contentStateModel.setWhetherFavor("true");
                                } else {
                                    int num;
                                    num = Integer.parseInt(NumberFormatTool.getNumStr(collectionNum.getText().toString()));
                                    if (num > 0) {
                                        num--;
                                    }
                                    collectionNum.setText(NumberFormatTool.formatNum(num, false));
                                    collectionImage.setImageResource(R.drawable.szrm_sdk_collection_icon);
                                    playerView.contentStateModel.setWhetherFavor("false");
                                }
                                if (null != playerView.contentStateModel) {
                                    playerView.setContentStateModel(myContentId, videoType);
                                }
                            } else if (json.get("code").toString().equals(token_error)) {
                                Log.e("addOrCancelFavor", "无token 去跳登录");
                                try {
                                    param.toLogin();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                if (null != json.get("message").toString()) {
                                    ToastUtils.showShort(json.get("message").toString());
                                } else {
                                    ToastUtils.showShort("收藏失败");
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            ToastUtils.showShort("收藏失败");
                        }

                    }

                    @Override
                    public void onError(Response<String> response) {
                        ToastUtils.showShort("收藏失败");
                    }
                });
    }

    /**
     * 点赞/取消点赞
     */
    private void addOrCancelLike(String targetId, String type, final ImageView likeImage, final TextView likeNum) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("targetId", targetId);
            jsonObject.put("type", type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        OkGo.<String>post(ApiConstants.getInstance().addOrCancelLike())
                .tag(VIDEOTAG)
                .headers("token", PersonInfoManager.getInstance().getTransformationToken())
                .upJson(jsonObject)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        if (null == response.body()) {
                            ToastUtils.showShort(R.string.data_err);
                            return;
                        }

                        if (null == playerView) {
                            return;
                        }

                        try {
                            JSONObject json = new JSONObject(response.body());
                            if (null != json && json.get("code").toString().equals("200")) {
                                if (!mDatas.isEmpty()) {
                                    String jsonString = BuriedPointModelManager.getLikeAndFavorBuriedPointData(myContentId, mDatas.get(currentIndex).getTitle(),
                                            "", "", "", "", mDatas.get(currentIndex).getIssueTimeStamp(),
                                            Constants.CONTENT_TYPE);
                                    Log.e("埋点", "埋点：点赞---" + jsonString);
                                }

                                if (json.get("data").toString().equals("1")) {
                                    int num = 0;
                                    if (null != likeImage) {
                                        likeImage.setImageResource(R.drawable.szrm_sdk_favourite_select);
                                    }

                                    if (null != likeNum) {
                                        num = Integer.parseInt(NumberFormatTool.getNumStr(likeNum.getText().toString()));
                                        num++;
                                        likeNum.setText(NumberFormatTool.formatNum(num, false));
                                    }

                                    mDataDTO.setWhetherLike(true);
                                    playerView.contentStateModel.setWhetherLike("true");
                                    playerView.contentStateModel.setLikeCountShow(NumberFormatTool.formatNum(num, false).toString());
                                } else {
                                    int num = 0;
                                    if (null != likeImage) {
                                        likeImage.setImageResource(R.drawable.szrm_sdk_favourite);
                                    }
                                    if (null != likeNum) {
                                        num = Integer.parseInt(NumberFormatTool.getNumStr(likeNum.getText().toString()));
                                        if (num > 0) {
                                            num--;
                                        }
                                        if (num == 0) {
                                            likeNum.setText("赞");
                                        } else {
                                            likeNum.setText(NumberFormatTool.formatNum(num, false));
                                        }
                                    }
                                    mDataDTO.setWhetherLike(false);
                                    playerView.contentStateModel.setWhetherLike("false");
                                    playerView.contentStateModel.setLikeCountShow(NumberFormatTool.formatNum(num, false).toString());
                                }
                                if (null != playerView.contentStateModel) {
                                    playerView.setContentStateModel(myContentId, videoType);
                                }
                            } else if (json.get("code").toString().equals(token_error)) {
                                Log.e("addOrCancelLike", "无token,跳转登录");
                                try {
                                    param.toLogin();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                if (null != json.get("message").toString()) {
                                    ToastUtils.showShort(json.get("message").toString());
                                } else {
                                    ToastUtils.showShort("点赞失败");
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            ToastUtils.showShort("点赞失败");
                        }

                    }

                    @Override
                    public void onError(Response<String> response) {
                        ToastUtils.showShort("点赞失败");
                    }

                    @Override
                    public void onFinish() {
                        super.onFinish();
                        likeIsRequesting = false;
                    }
                });
    }

    /**
     * 浏览量+1
     */
    private void addPageViews(String contentId) {
        OkGo.<String>post(ApiConstants.getInstance().addViews() + contentId)
                .tag(VIDEOTAG)
                .cacheMode(CacheMode.NO_CACHE)
                .execute(new StringCallback() {

                    @Override
                    public void onSuccess(Response<String> response) {
                        if (null == response.body()) {
                            ToastUtils.showShort(R.string.data_err);
                            return;
                        }
                        try {
                            JSONObject jsonObject = new JSONObject(response.body());
                            Log.e("yqh", jsonObject.getString("message"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        if (null == response.body()) {
                            ToastUtils.showShort(R.string.data_err);
                            return;
                        }
                        try {
                            JSONObject jsonObject = new JSONObject(response.body());
                            ToastUtils.showShort(jsonObject.getString("message"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mIsVisibleToUser) {
            return;
        }
        if (playerView != null && null != mDataDTO && !SPUtils.isVisibleNoWifiView(getActivity())) {
            SuperPlayerImpl.mCurrentPlayVideoURL = mDataDTO.getPlayUrl();
            if (playerView.homeVideoIsLoad) {
                playerView.mSuperPlayer.resume();
                isFirst = true;
            } else {
                if (!isFirst) {
                    playerView.mSuperPlayer.reStart();
                }
            }
        }
        isPause = false;
        xkshOldSystemTime = DateUtils.getTimeCurrent();
        if (!TextUtils.isEmpty(myContentId)) {
            getContentState(myContentId);
        }

        if (PersonInfoManager.getInstance().isRequestToken()) {
            try {
                getUserToken(VideoInteractiveParam.getInstance().getCode());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (null != playerView && playerView.mSuperPlayer.getPlayerMode() == SuperPlayerDef.PlayerMode.FULLSCREEN) {
            SystemUtils.hideSystemUI(decorView);
        }
    }

    /**
     * 使用获取的code去换token
     */
    private void getUserToken(String token) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("token", token);
            jsonObject.put("ignoreGdy", 1);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        OkGo.<TokenModel>post(ApiConstants.getInstance().mycsToken())
                .tag("userToken")
                .upJson(jsonObject)
                .execute(new JsonCallback<TokenModel>(TokenModel.class) {
                    @Override
                    public void onSuccess(Response<TokenModel> response) {
                        if (null == response.body()) {
                            ToastUtils.showShort(R.string.data_err);
                            return;
                        }

                        try {
                            if (response.body().getCode() == 200) {
                                if (null == response.body().getData()) {
                                    ToastUtils.showShort(R.string.data_err);
                                    return;
                                }
                                Log.d("mycs_token", "转换成功");
                                try {
                                    PersonInfoManager.getInstance().setToken(VideoInteractiveParam.getInstance().getCode());
                                    PersonInfoManager.getInstance().setUserId(response.body().getData().getLoginSysUserVo().getId());
                                    PersonInfoManager.getInstance().setTgtCode(VideoInteractiveParam.getInstance().getCode());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                transformationToken = response.body().getData().getToken();
                                PersonInfoManager.getInstance().setTransformationToken(transformationToken);
                                if (!TextUtils.isEmpty(myContentId)) {
                                    getContentState(myContentId);
                                }
                            } else {
                                ToastUtils.showShort(response.body().getMessage());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Response<TokenModel> response) {
                        if (null != response.body()) {
                            ToastUtils.showShort(response.body().getMessage());
                            return;
                        }
                        ToastUtils.showShort(R.string.net_err);
                    }

                    @Override
                    public void onFinish() {
                        super.onFinish();
                    }
                });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (playerView == null) {
            return;
        }

        if (!mIsVisibleToUser) {
            return;
        }

        playerView.mSuperPlayer.pause();
        isPause = true;
        if (null == mDataDTO) {
            return;
        }
        if (!TextUtils.isEmpty(mDataDTO.getVolcCategory())) {
            if (playerView.mWindowPlayer.mCurrentPlayState != SuperPlayerDef.PlayerState.END) {
                if (mDuration != 0 && mProgress != 0) {
                    /**
                     * 上报内容埋点 视频播放时长
                     */
                    String event = Constants.CMS_VIDEO_OVER_AUTO;
                    long evePlayTime = Math.abs(mProgress - lsDuration);
                    double currentPercent = evePlayTime * 1.0 / mDuration;
                    double uploadPercent = 0;
                    if (null == playerView.buriedPointModel.getXksh_renew() || TextUtils.equals("false", playerView.buriedPointModel.getXksh_renew())) {
//                      //不为重播
                        if (currentPercent > maxPercent) {
                            uploadPercent = currentPercent;
                            maxPercent = currentPercent;
                        } else {
                            uploadPercent = maxPercent;
                        }
                    } else {
                        uploadPercent = 1;
                    }

                    xkshReportTime = DateUtils.getTimeCurrent() - xkshOldSystemTime;
                    BigDecimal two = new BigDecimal(uploadPercent);
                    double pointPercentTwo = two.setScale(2, BigDecimal.ROUND_DOWN).doubleValue();
                    lsDuration = mProgress;
                    //上报埋点
                    uploadBuriedPoint(ContentBuriedPointManager.setContentBuriedPoint(getActivity(), mDataDTO.getThirdPartyId(), String.valueOf(xkshReportTime), String.valueOf(Math.floor(pointPercentTwo * 100)), event, mDataDTO.getVolcCategory(), mDataDTO.getRequestId()), event);
                    Log.e("xksh_md", "埋点事件：" + event + "播放时长:" + xkshReportTime + "---" + "播放百分比:" + pointPercentTwo);
                }
            }
        }

        String isFinish;
        if (null == playerView.buriedPointModel.getIs_renew() || TextUtils.equals("false", playerView.buriedPointModel.getIs_renew())) {
            isFinish = "否";
        } else {
            isFinish = "是";
        }
        FinderBuriedPointManager.setFinderVideo(Constants.CONTENT_VIDEO_DURATION, "", mDataDTO, xkshReportTime, isFinish);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.dismiss_pop) {
            if (popupWindow != null) {
                popupWindow.dissmiss();
            }
        }
//        else if (id == R.id.video_detail_comment_btn) {
//            //Finder埋点
//            FinderBuriedPointManager.setFinderClick("评论");
//            showCommentPopWindow();
//        }
        else if (id == R.id.comment_pop_rl) {
            if (TextUtils.isEmpty(PersonInfoManager.getInstance().getTransformationToken())) {
                try {
                    noLoginTipsPop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                KeyboardUtils.toggleSoftInput(getActivity().getWindow().getDecorView());
                edtInput.setHint("留下你的精彩评论");
                isReply = false;
                showInputEdittextAndSend();
            }
        }
//        else if (id == R.id.video_detail_white_comment_rl) {
//            FinderBuriedPointManager.setFinderClick("评论");
//            if (TextUtils.isEmpty(PersonInfoManager.getInstance().getTransformationToken())) {
//                try {
//                    noLoginTipsPop();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            } else {
//                edtInput.setHint("留下你的精彩评论");
//                isReply = false;
//                showCommentPopWindow();
//            }
//        }
        else if (id == R.id.no_login_tips_cancel) {
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
        } else if (id == R.id.share_wx_btn) {
            if (mDatas.isEmpty()) {
                return;
            }
            toShare(mDataDTO, Constants.SHARE_WX);
        } else if (id == R.id.share_circle_btn) {
            if (mDatas.isEmpty()) {
                return;
            }
            toShare(mDataDTO, Constants.SHARE_CIRCLE);
        } else if (id == R.id.share_qq_btn) {
            if (mDatas.isEmpty()) {
                return;
            }
            toShare(mDataDTO, Constants.SHARE_QQ);
        } else if (id == R.id.rank_list) {
//            startActivity(new Intent(getActivity(),TgtCodeActivity.class));
            //跳转H5排行榜
            try {
                if (AppInit.mIsDebug) {
                    param.recommendUrl(Constants.RANKING_LIST, null);
                } else {
                    param.recommendUrl(Constants.RANKING_LIST_ZS, null);
                }
                FinderPointModel model = new FinderPointModel();
                model.setButton_name("排行榜");
                FinderBuriedPointManager.setFinderBuriedPoint(Constants.SHORT_VIDEO_LIKE_RANKING, model);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 弹出发送评论弹出窗
     */
    private void showInputEdittextAndSend() {
        //创建并显示popWindow
        if (null == inputAndSendPop) {
            inputAndSendPop = new CustomPopWindow.PopupWindowBuilder(getActivity())
                    .setView(sendPopContentView)
                    .setOutsideTouchable(false)
                    .setFocusable(true)
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                    .setAnimationStyle(R.style.take_popwindow_anim)
                    .size(AppInit.getContext().getResources().getDisplayMetrics().widthPixels, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .create()
                    .showAtLocation(rootView, Gravity.BOTTOM, 0, 0);
        } else {
            inputAndSendPop.showAtLocation(rootView, Gravity.BOTTOM, 0, 0);
        }
        edtInput.setFocusable(true);
        edtInput.setFocusableInTouchMode(true);
        edtInput.requestFocus();
    }

    /**
     * 没有登录情况下 点击点赞收藏评论 提示登录的提示框
     */
    private void noLoginTipsPop() {
        if (null == noLoginTipsPop) {
            noLoginTipsPop = new CustomPopWindow.PopupWindowBuilder(getActivity())
                    .setView(noLoginTipsView)
                    .enableBackgroundDark(true)
                    .setOutsideTouchable(true)
                    .setFocusable(true)
                    .setAnimationStyle(R.style.AnimCenter)
                    .size(AppInit.getContext().getResources().getDisplayMetrics().widthPixels, AppInit.getContext().getResources().getDisplayMetrics().heightPixels)
                    .create()
                    .showAtLocation(decorView, Gravity.CENTER, 0, 0);
        } else {
            noLoginTipsPop.showAtLocation(decorView, Gravity.CENTER, 0, 0);
        }
    }


    /**
     * 添加软键盘监听
     */
    private void setSoftKeyBoardListener() {
        softKeyBoardListener = new SoftKeyBoardListener(getActivity());
        //软键盘状态监听
        softKeyBoardListener.setListener(new SoftKeyBoardListener.OnSoftKeyBoardChangeListener() {
            @Override
            public void keyBoardShow(int height) {
                //软键盘已经显示，做逻辑
                Log.e("yqh", "软键盘已经显示,做逻辑");
            }

            @Override
            public void keyBoardHide(int height) {
                //软键盘已经隐藏,做逻辑
//                SystemUtils.hideSystemUI(decorView);
                if (null != inputAndSendPop) {
                    inputAndSendPop.getPopupWindow().dismiss();
                }
                Log.e("yqh", "软键盘已经隐藏,做逻辑");
            }
        });
    }

    /**
     * 获取推荐列表数据
     */
    private void getRecommend(String contentId, final int position) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("current", "1");
            jsonObject.put("pageSize", "999");
            jsonObject.put("contentId", contentId + "");
            jsonObject.put("pageIndex", "1");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        OkGo.<RecommendModel>post(ApiConstants.getInstance().recommendList())
                .tag(recommendTag)
                .upJson(jsonObject)
                .execute(new JsonCallback<RecommendModel>() {
                    @Override
                    public void onSuccess(Response<RecommendModel> response) {
                        if (null == response.body().getData()) {
                            ToastUtils.showShort(R.string.data_err);
                            return;
                        }
                        try {
                            if (response.body().getCode().equals("200")) {
                                recommondList.clear();
                                recommondList.addAll(response.body().getData().getRecords());

                                ViewFlipper viewFlipper = (ViewFlipper) adapter.getViewByPosition(position, R.id.video_flipper);

                                if (null == viewFlipper) {
                                    return;
                                }

                                if (null != adapter.getItem(position) && adapter.getItem(position).isClosed()) {
                                    viewFlipper.setVisibility(View.GONE);
                                    return;
                                }

                                if (recommondList.size() > 1) {
                                    viewFlipper.setVisibility(View.VISIBLE);
                                    viewFlipper.startFlipping();
                                    viewFlipper.setAutoStart(true);
                                } else if (recommondList.size() == 1) {
                                    viewFlipper.setVisibility(View.VISIBLE);
                                    viewFlipper.setAutoStart(false);
                                } else if (recommondList.size() == 0) {
                                    viewFlipper.setVisibility(View.GONE);
                                }
                                adapter.getViewFlipperData(recommondList, viewFlipper, mDatas.get(position));
                            } else {
                                ToastUtils.showShort(response.body().getMessage());
                            }
//                            if (SPUtils.isVisibleNoWifiView(getActivity())) {
//                                SPUtils.getInstance().put(Constants.AGREE_NETWORK, "0");
//                            } else {
//                                addPlayView(position);
//                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }

                    @Override
                    public void onError(Response<RecommendModel> response) {
                        super.onError(response);
                        if (null != response.body()) {
                            ToastUtils.showShort(response.body().getMessage());
                            return;
                        }
                        ToastUtils.showShort(R.string.net_err);
                    }

                    @Override
                    public void onFinish() {
                        super.onFinish();
                        if (isVisibleNoWifiView(getActivity())) {
                            SPUtils.getInstance().put(Constants.AGREE_NETWORK, "0");
                        }
                        loadingProgress.setVisibility(View.GONE);
                        getThematicCollection(myContentId);
                        if (!isVisibleNoWifiView(getActivity())) {
                            addPlayView(position);
                        }
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        SuperPlayerImpl.mCurrentPlayVideoURL = null;
        if (playerView != null) {
            playerView.stopPlay();
            playerView.release();
            playerView.mSuperPlayer.destroy();
        }
    }

    /**
     * 关注
     *
     * @param targetUserId
     */
    private void toFollow(String targetUserId) {
        OkGo.<TrackingUploadModel>post(ApiConstants.getInstance().toFollow() + targetUserId)
                .tag(VIDEOTAG)
                .headers("token", PersonInfoManager.getInstance().getTransformationToken())
                .execute(new JsonCallback<TrackingUploadModel>() {
                    @Override
                    public void onSuccess(Response<TrackingUploadModel> response) {
                        try {
                            if (200 == response.body().getCode()) {
                                setFollowView("true");
                                //行为埋点 关注用户 关注的用户id mDataDTO.getCreateBy()
                            } else {
                                ToastUtils.showShort(response.body().getMessage());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Response<TrackingUploadModel> response) {
                        super.onError(response);
                        if (null != response.body()) {
                            ToastUtils.showShort(response.body().getMessage());
                        }
                        ToastUtils.showShort(R.string.net_err);
                    }

                    @Override
                    public void onFinish() {
                        super.onFinish();
                    }
                });
    }

    /**
     * 取消关注
     *
     * @param targetUserId
     */
    private void cancelFollow(String targetUserId) {
        OkGo.<TrackingUploadModel>post(ApiConstants.getInstance().cancelFollow() + targetUserId)
                .tag(VIDEOTAG)
                .headers("token", PersonInfoManager.getInstance().getTransformationToken())
                .execute(new JsonCallback<TrackingUploadModel>() {
                    @Override
                    public void onSuccess(Response<TrackingUploadModel> response) {
                        if (200 == response.body().getCode()) {
                            setFollowView("false");
                        } else {
                            ToastUtils.showShort(response.body().getMessage());
                        }
                    }

                    @Override
                    public void onError(Response<TrackingUploadModel> response) {
                        super.onError(response);
                        if (null != response.body()) {
                            ToastUtils.showShort(response.body().getMessage());
                        }
                        ToastUtils.showShort(R.string.net_err);
                    }

                    @Override
                    public void onFinish() {
                        super.onFinish();
                    }
                });
    }
}