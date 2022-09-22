package ui.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.example.zhouwei.library.CustomPopWindow;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.cache.CacheMode;
import com.lzy.okgo.model.Response;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;
import com.szrm.videolibraries.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import adpter.LiveRvAdapter;
import common.callback.JsonCallback;
import common.callback.VideoInteractiveParam;
import common.constants.Constants;
import common.http.ApiConstants;
import common.model.DataDTO;
import common.model.GdyTokenModel;
import common.model.ShareInfo;
import common.model.TokenModel;
import common.model.VideoChannelModel;
import common.model.VideoDetailModel;
import common.utils.AppInit;
import common.utils.PersonInfoManager;
import common.utils.ToastUtils;
import widget.LoadingView;

import static common.callback.VideoInteractiveParam.param;
import static common.constants.Constants.PANELCODE;
import static common.constants.Constants.VIDEOTAG;
import static common.constants.Constants.success_code;

public class LiveFragment extends Fragment implements View.OnClickListener {
    private RecyclerView liveRv;
    private LiveRvAdapter adapter;
    private List<DataDTO> mDatas;
    private String mPageSize = "20"; //每页视频多少条
    private Bundle args;
    private String panelCode = "";
    private SmartRefreshLayout refreshLayout;
    private boolean isLoadComplate = false;
    private BaseQuickAdapter.RequestLoadMoreListener requestLoadMoreListener;
    public CustomPopWindow noLoginTipsPop;
    private View noLoginTipsView;
    private TextView noLoginTipsCancel;
    private TextView noLoginTipsOk;
    private View footview;
    public boolean mIsVisibleToUser;
    private LoadingView videoLoadingProgress;

    public LiveFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_live, container, false);
        initView(view);
        return view;
    }

    public LiveFragment newInstance(LiveFragment fragment, VideoChannelModel videoChannelModel) {
        args = new Bundle();
        args.putString(PANELCODE, videoChannelModel.getColumnBean().getPanelCode());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            panelCode = getArguments().getString(PANELCODE);
        }
    }


    private void initView(View view) {
        videoLoadingProgress = view.findViewById(R.id.video_loading_progress);
        liveRv = view.findViewById(R.id.live_rv);
        liveRv.setHasFixedSize(true);
        mDatas = new ArrayList<>();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        liveRv.setLayoutManager(linearLayoutManager);
        noLoginTipsView = View.inflate(getActivity(), R.layout.no_login_tips, null);
        noLoginTipsCancel = noLoginTipsView.findViewById(R.id.no_login_tips_cancel);
        noLoginTipsOk = noLoginTipsView.findViewById(R.id.no_login_tips_ok);
        noLoginTipsCancel.setOnClickListener(this);
        noLoginTipsOk.setOnClickListener(this);
        adapter = new LiveRvAdapter(R.layout.live_rv_item_layout, mDatas, getActivity());
        liveRv.setAdapter(adapter);
        footview = View.inflate(getActivity(), R.layout.footer_view, null);
        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                if (TextUtils.isEmpty(PersonInfoManager.getInstance().getTransformationToken())) {
                    noLoginTipsPop();
                } else {
                    videoLoadingProgress.setVisibility(View.VISIBLE);
                    getGdyToken(PersonInfoManager.getInstance().getTransformationToken(), position);
                }

            }
        });
        initSmartRefresh(view);
//        adapter.setLoadMoreView(new CustomLoadMoreView());
        adapter.setPreLoadNumber(2);
        adapter.setOnLoadMoreListener(requestLoadMoreListener, liveRv);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        this.mIsVisibleToUser = isVisibleToUser;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getPullDownData(mPageSize, panelCode, "false", Constants.REFRESH_TYPE);
    }

    private void initSmartRefresh(View view) {

        refreshLayout = view.findViewById(R.id.refreshLayout);
        refreshLayout.setEnableScrollContentWhenLoaded(true);
        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(RefreshLayout refreshlayout) {
                isLoadComplate = false;
//                adapter.setOnLoadMoreListener(requestLoadMoreListener, liveRv);
                getPullDownData(mPageSize, panelCode, "false", Constants.REFRESH_TYPE);
            }
        });

        refreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                if (mDatas.size() > 0) {
                    loadMoreData(mDatas.get(mDatas.size() - 1).getId() + "", panelCode, "true", Constants.LOADMORE_TYPE);
                }
            }
        });

//        requestLoadMoreListener = new BaseQuickAdapter.RequestLoadMoreListener() {
//            @Override
//            public void onLoadMoreRequested() {
//                if (!isLoadComplate) {
//                    liveRv.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            if (mDatas.isEmpty()) {
//                                adapter.loadMoreFail();
//                                return;
//                            }
//                            loadMoreData(mDatas.get(mDatas.size() - 1).getId() + "", panelCode, "true", Constants.LOADMORE_TYPE);
//                        }
//                    });
//                }
//            }
//        };
    }

    /**
     * 获取下拉列表数据
     *
     * @param panelCode
     * @param removeFirst
     */
    private void getPullDownData(String pageSize, String panelCode, String removeFirst, String refreshType) {
        mDatas.clear();
        OkGo.<VideoDetailModel>get(ApiConstants.getInstance().getVideoDetailListUrl())
                .tag(VIDEOTAG)
                .params("pageSize", pageSize)
                .params("panelCode", panelCode)
                .params("removeFirst", removeFirst)
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

                        if (response.body().getCode().equals(success_code)) {
                            if (null == response.body().getData()) {
                                ToastUtils.showShort(R.string.data_err);
                                return;
                            }

                            mDatas.addAll(response.body().getData());
                            adapter.setNewData(mDatas);
                        } else {
                            ToastUtils.showShort(response.body().getMessage());
                        }
                        if (null != refreshLayout) {
                            refreshLayout.finishRefresh();
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
                    }
                });
    }

    /**
     * 获取更多数据
     */
    private void loadMoreData(String contentId, String panelCode, String removeFirst, String refreshType) {
        OkGo.<VideoDetailModel>get(ApiConstants.getInstance().getVideoDetailListUrl())
                .tag(VIDEOTAG)
                .params("contentId", contentId)
                .params("pageSize", 10)
                .params("panelCode", panelCode)
                .params("removeFirst", removeFirst)
                .params("refreshType", refreshType)
                .params("type", "")
                .cacheMode(CacheMode.NO_CACHE)
                .execute(new JsonCallback<VideoDetailModel>(VideoDetailModel.class) {
                    @Override
                    public void onSuccess(Response<VideoDetailModel> response) {
                        if (null == response.body()) {
                            isLoadComplate = true;
                            ToastUtils.showShort(R.string.data_err);
                            return;
                        }

                        if (response.body().getCode().equals(success_code)) {
                            if (null == response.body().getData()) {
                                isLoadComplate = true;
                                ToastUtils.showShort(R.string.data_err);
                                return;
                            }

                            if (response.body().getData().size() == 0) {
//                                adapter.loadMoreEnd();
//                                adapter.setOnLoadMoreListener(requestLoadMoreListener, liveRv);
                                refreshLayout.finishLoadMoreWithNoMoreData();
                                isLoadComplate = true;
                                return;
                            } else {
//                                adapter.setOnLoadMoreListener(requestLoadMoreListener, liveRv);
                                isLoadComplate = false;
                            }
                            mDatas.addAll(response.body().getData());
                            adapter.setNewData(mDatas);
//                            adapter.loadMoreComplete();
                        } else {
                            adapter.loadMoreFail();
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
                        refreshLayout.finishLoadMore();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mIsVisibleToUser) {
            return;
        }
        if (PersonInfoManager.getInstance().isRequestToken()) {
            try {
                getUserToken(VideoInteractiveParam.getInstance().getCode());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 使用获取的code去换token
     */
    public void getUserToken(String token) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("token", token);
            jsonObject.put("ignoreGdy", 1);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        OkGo.<TokenModel>post(ApiConstants.getInstance().mycsToken())
                .tag(VIDEOTAG)
                .upJson(jsonObject)
                .execute(new JsonCallback<TokenModel>(TokenModel.class) {
                    @Override
                    public void onSuccess(Response<TokenModel> response) {
                        if (null == response.body()) {
                            ToastUtils.showShort(R.string.data_err);
                            return;
                        }

                        if (response.body().getCode() == 200) {
                            try {
                                PersonInfoManager.getInstance().setToken(VideoInteractiveParam.getInstance().getCode());
                                PersonInfoManager.getInstance().setUserId(response.body().getData().getLoginSysUserVo().getId());
                                PersonInfoManager.getInstance().setTgtCode(VideoInteractiveParam.getInstance().getCode());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            PersonInfoManager.getInstance().setTransformationToken(response.body().getData().getToken());
                        } else {
                            ToastUtils.showShort(response.body().getMessage());
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

    /**
     * 换取广电云token
     * token 融媒token
     */
    private void getGdyToken(String token, final int position) {
        JSONObject jsonObject = new JSONObject();
        OkGo.<GdyTokenModel>post(ApiConstants.getInstance().gdyToken())
                .tag("getGdyToken")
                .headers("token", token)
                .upJson(jsonObject)
                .execute(new JsonCallback<GdyTokenModel>(GdyTokenModel.class) {
                    @Override
                    public void onSuccess(Response<GdyTokenModel> response) {
                        try {
                            if (response.body().getCode() == 200) {
                                if (null == response.body().getData()) {
                                    ToastUtils.showShort(R.string.data_err);
                                    return;
                                }
                                PersonInfoManager.getInstance().setGdyToken(response.body().getData());
                                if (mDatas.size() == 0) {
                                    return;
                                }
                                ShareInfo shareInfo = ShareInfo.getInstance(mDatas.get(position).getShareUrl(), mDatas.get(position).getShareImageUrl(),
                                        mDatas.get(position).getShareBrief(), mDatas.get(position).getShareTitle(), "");

                                try {
                                    String url = mDatas.get(position).getDetailUrl();
                                    if (url.contains("?")) {
                                        param.recommendUrl(url + "&token=" + PersonInfoManager.getInstance().getGdyToken(), shareInfo);
                                    } else {
                                        param.recommendUrl(url + "?token=" + PersonInfoManager.getInstance().getGdyToken(), shareInfo);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                ToastUtils.showShort(response.body().getMessage());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Response<GdyTokenModel> response) {
                        if (null != response.body()) {
                            ToastUtils.showShort(response.body().getMessage());
                            return;
                        }
                        ToastUtils.showShort(R.string.net_err);
                    }

                    @Override
                    public void onFinish() {
                        super.onFinish();
                        videoLoadingProgress.setVisibility(View.GONE);
                    }
                });
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
                    .showAtLocation(getActivity().getWindow().getDecorView(), Gravity.CENTER, 0, 0);
        } else {
            noLoginTipsPop.showAtLocation(getActivity().getWindow().getDecorView(), Gravity.CENTER, 0, 0);
        }
    }

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
}