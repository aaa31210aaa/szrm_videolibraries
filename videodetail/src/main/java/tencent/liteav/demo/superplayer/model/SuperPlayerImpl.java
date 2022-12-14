package tencent.liteav.demo.superplayer.model;

import static com.tencent.rtmp.TXLiveConstants.PLAY_EVT_PLAY_PROGRESS;

import static tencent.liteav.demo.superplayer.SuperPlayerDef.PlayerState.PLAYING;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.liteav.basic.log.TXCLog;
import com.tencent.rtmp.ITXLivePlayListener;
import com.tencent.rtmp.ITXVodPlayListener;
import com.tencent.rtmp.TXBitrateItem;
import com.tencent.rtmp.TXLiveBase;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePlayConfig;
import com.tencent.rtmp.TXLivePlayer;
import com.tencent.rtmp.TXVodPlayConfig;
import com.tencent.rtmp.TXVodPlayer;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.wdcs.model.PlayImageSpriteInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import common.model.PlayKeyFrameDescInfo;
import common.model.ResolutionName;
import common.model.VideoQuality;
import tencent.liteav.demo.superplayer.SuperPlayerCode;
import tencent.liteav.demo.superplayer.SuperPlayerDef;
import tencent.liteav.demo.superplayer.SuperPlayerGlobalConfig;
import tencent.liteav.demo.superplayer.SuperPlayerModel;
import tencent.liteav.demo.superplayer.SuperPlayerVideoId;
import tencent.liteav.demo.superplayer.SuperPlayerView;
import tencent.liteav.demo.superplayer.model.net.LogReport;
import tencent.liteav.demo.superplayer.model.protocol.IPlayInfoProtocol;
import tencent.liteav.demo.superplayer.model.protocol.IPlayInfoRequestCallback;
import tencent.liteav.demo.superplayer.model.protocol.PlayInfoParams;
import tencent.liteav.demo.superplayer.model.protocol.PlayInfoProtocolV2;
import tencent.liteav.demo.superplayer.model.protocol.PlayInfoProtocolV4;
import tencent.liteav.demo.superplayer.model.utils.VideoQualityUtils;

public class SuperPlayerImpl implements SuperPlayer, ITXVodPlayListener, ITXLivePlayListener {

    private static final String TAG = "SuperPlayerImpl";
    private static final int SUPERPLAYER_MODE = 1;
    private static final int SUPPORT_MAJOR_VERSION = 8;
    private static final int SUPPORT_MINOR_VERSION = 5;

    private Context mContext;
    private TXCloudVideoView mVideoView;        // ?????????????????????view

    private IPlayInfoProtocol mCurrentProtocol; // ???????????????????????????
    public TXVodPlayer mVodPlayer;       // ???????????????
    private TXVodPlayConfig mVodPlayConfig;   // ?????????????????????
    private TXLivePlayer mLivePlayer;      // ???????????????
    private TXLivePlayConfig mLivePlayConfig;  // ?????????????????????

    private SuperPlayerModel mCurrentModel;  // ???????????????model
    private SuperPlayerObserver mObserver;
    private VideoQuality mVideoQuality;

    private SuperPlayerDef.PlayerType mCurrentPlayType = SuperPlayerDef.PlayerType.VOD;       // ??????????????????
    private SuperPlayerDef.PlayerMode mCurrentPlayMode = SuperPlayerDef.PlayerMode.WINDOW;    // ??????????????????
    private SuperPlayerDef.PlayerState mCurrentPlayState = SuperPlayerDef.PlayerState.END;  // ??????????????????

    public static String mCurrentPlayVideoURL;    // ???????????????URL

    private int mSeekPos;                   // ????????????????????????????????????

    private long mReportLiveStartTime = -1; // ?????????????????????????????????????????????
    public long mReportVodStartTime = -1;  // ?????????????????????????????????????????????
    private long mMaxLiveProgressTime;      // ???????????????????????????

    private boolean mIsMultiBitrateStream;  // ??????????????????url??????
    private boolean mIsPlayWithFileId;      // ??????????????????fileId??????
    private boolean mDefaultQualitySet;     // ?????????????????????url??????????????????????????????
    private boolean mChangeHWAcceleration;  // ?????????????????????????????????????????????????????????
    private String mFileId;
    private int mAppId;
    public int demouration;
    public SuperPlayerView superPlayerView;
    private boolean noFirst;
    private boolean xkshNoFirst;

    public SuperPlayerImpl(Context context, TXCloudVideoView videoView, SuperPlayerView superPlayerView) {
        initialize(context, videoView);
        this.superPlayerView = superPlayerView;
    }

    /**
     * ???????????????????????????
     *
     * @param event
     * @param param
     */
    @Override
    public void onPlayEvent(int event, Bundle param) {
        if (event != PLAY_EVT_PLAY_PROGRESS) {
            String playEventLog = "TXLivePlayer onPlayEvent event: " + event + ", " + param.getString(TXLiveConstants.EVT_DESCRIPTION);
            TXCLog.d(TAG, playEventLog);
        }
        switch (event) {
            case TXLiveConstants.PLAY_EVT_VOD_PLAY_PREPARED: //??????????????????
            case TXLiveConstants.PLAY_EVT_PLAY_BEGIN:
                updatePlayerState(PLAYING);
                break;
            case TXLiveConstants.PLAY_ERR_NET_DISCONNECT:
            case TXLiveConstants.PLAY_EVT_PLAY_END:
                if (mCurrentPlayType == SuperPlayerDef.PlayerType.LIVE_SHIFT) {  // ?????????????????????????????????
                    mLivePlayer.resumeLive();
                    updatePlayerType(SuperPlayerDef.PlayerType.LIVE);
                    onError(SuperPlayerCode.LIVE_SHIFT_FAIL, "????????????,????????????");
                    updatePlayerState(PLAYING);
                } else {
                    stop();
                    updatePlayerState(SuperPlayerDef.PlayerState.END);
                    if (event == TXLiveConstants.PLAY_ERR_NET_DISCONNECT) {
                        onError(SuperPlayerCode.NET_ERROR, "???????????????,????????????");
                    } else {
                        onError(SuperPlayerCode.LIVE_PLAY_END, param.getString(TXLiveConstants.EVT_DESCRIPTION));
                    }
                }
                break;
            case TXLiveConstants.PLAY_EVT_PLAY_LOADING:
//            case TXLiveConstants.PLAY_WARNING_RECONNECT:  //?????????????????????????????????????????????????????????????????????loading ???????????? TXLiveConstants.PLAY_EVT_PLAY_LOADING ??????
                updatePlayerState(SuperPlayerDef.PlayerState.LOADING);
                break;
            case TXLiveConstants.PLAY_EVT_RCV_FIRST_I_FRAME:
                break;
            case TXLiveConstants.PLAY_EVT_STREAM_SWITCH_SUCC:
                updateStreamEndStatus(true, SuperPlayerDef.PlayerType.LIVE, mVideoQuality);
                break;
            case TXLiveConstants.PLAY_ERR_STREAM_SWITCH_FAIL:
                updateStreamEndStatus(false, SuperPlayerDef.PlayerType.LIVE, mVideoQuality);
                break;
            case PLAY_EVT_PLAY_PROGRESS:
                int progress = param.getInt(TXLiveConstants.EVT_PLAY_PROGRESS_MS);
                mMaxLiveProgressTime = progress > mMaxLiveProgressTime ? progress : mMaxLiveProgressTime;
                updatePlayProgress(progress / 1000, mMaxLiveProgressTime / 1000);
                break;
            default:
                break;
        }
    }

    /**
     * ?????????????????????????????????
     *
     * @param bundle
     */
    @Override
    public void onNetStatus(Bundle bundle) {

    }

    public static ReadPlayCallBack readPlayCallBack;

    public interface ReadPlayCallBack {
        void ReadPlayCallback();
    }

    public static void setReadPlayCallBack(ReadPlayCallBack callBack) {
        readPlayCallBack = callBack;
    }

    public static AutoPlayOverCallBack autoPlayOverCallBack;

    public interface AutoPlayOverCallBack {
        void AutoPlayOverCallBack();
    }

    public static void setAutoPlayOverCallBack(AutoPlayOverCallBack callBack) {
        autoPlayOverCallBack = callBack;
    }

    public static DetailAutoPlayOverCallBack detailAutoPlayOverCallBack;

    public interface DetailAutoPlayOverCallBack {
        void DetailAutoPlayOverCallBack();
    }

    public static void setDetailAutoPlayOverCallBack(DetailAutoPlayOverCallBack callBack) {
        detailAutoPlayOverCallBack = callBack;
    }

    /**
     * ???????????????????????????
     *
     * @param player
     * @param event
     * @param param
     */
    @Override
    public void onPlayEvent(TXVodPlayer player, int event, Bundle param) {
        if (event == PLAY_EVT_PLAY_PROGRESS) {
            // ????????????, ????????????
            demouration = param.getInt(TXLiveConstants.EVT_PLAY_DURATION);
            // ????????????, ????????????
            final int progressress = param.getInt(TXLiveConstants.EVT_PLAY_PROGRESS);
//            if (mDuration != 0) {
//                float percentage = mDuration > 0 ? ((float) demouration / (float) mDuration) : 1.0f;
//                float videoPercentage = mDuration > 0 ? ((float) progressress / (float) mDuration) : 1.0f;
//                final int progress = Math.round(percentage * superPlayerView.mWindowPlayer.mLoadBar.getMax());
//                final int videoProgress = Math.round(videoPercentage * superPlayerView.mWindowPlayer.mLoadBar.getMax());
////                superPlayerView.mWindowPlayer.mLoadBar.setProgress(progress);
//                superPlayerView.mFullScreenPlayer.mFullLoadBar.setProgress(progress);
//            }

            // ????????????, ????????????
//            int duration = param.getInt(TXLiveConstants.EVT_PLAY_DURATION);
//            // ????????????????????????????????????
        }

        if (event != PLAY_EVT_PLAY_PROGRESS) {
            String playEventLog = "TXVodPlayer onPlayEvent event: " + event + ", " + param.getString(TXLiveConstants.EVT_DESCRIPTION);
            TXCLog.d(TAG, playEventLog);
        }
        switch (event) {
            case TXLiveConstants.PLAY_EVT_VOD_PLAY_PREPARED://??????????????????
                updatePlayerState(PLAYING);
                if (mIsMultiBitrateStream) {
                    List<TXBitrateItem> bitrateItems = mVodPlayer.getSupportedBitrates();
                    if (bitrateItems == null || bitrateItems.size() == 0) {
                        return;
                    }
                    Collections.sort(bitrateItems); //masterPlaylist????????????????????????????????????????????????
                    List<VideoQuality> videoQualities = new ArrayList<>();
                    int size = bitrateItems.size();
                    List<ResolutionName> resolutionNames = (mCurrentProtocol != null) ? mCurrentProtocol.getResolutionNameList() : null;
                    for (int i = 0; i < size; i++) {
                        TXBitrateItem bitrateItem = bitrateItems.get(i);
                        VideoQuality quality;
                        if (resolutionNames != null) {
                            quality = VideoQualityUtils.convertToVideoQuality(bitrateItem, mCurrentProtocol.getResolutionNameList());
                        } else {
                            quality = VideoQualityUtils.convertToVideoQuality(bitrateItem, i);
                        }
                        videoQualities.add(quality);
                    }
                    if (!mDefaultQualitySet) {
                        mVodPlayer.setBitrateIndex(bitrateItems.get(bitrateItems.size() - 1).index); //???????????????????????????
                        mDefaultQualitySet = true;
                    }
                    updateVideoQualityList(videoQualities, null);
                }
                break;
            case TXLiveConstants.PLAY_EVT_RCV_FIRST_I_FRAME:
                //?????????????????????????????? ???????????????????????????????????????
                readPlayCallBack.ReadPlayCallback();
                if (null != superPlayerView) {
                    superPlayerView.detailIsLoad = true;
                    superPlayerView.homeVideoIsLoad = true;
                }

                if (mChangeHWAcceleration) { //?????????????????????????????????seek??????
                    TXCLog.i(TAG, "seek pos:" + mSeekPos);
                    seek(mSeekPos);
                    mChangeHWAcceleration = false;
                }
                break;
            case TXLiveConstants.PLAY_EVT_PLAY_END:
                if (null != autoPlayOverCallBack) {
                    autoPlayOverCallBack.AutoPlayOverCallBack();
                }

                if (null != detailAutoPlayOverCallBack) {
                    detailAutoPlayOverCallBack.DetailAutoPlayOverCallBack();
                }
                updatePlayerState(SuperPlayerDef.PlayerState.END);
                break;
            case PLAY_EVT_PLAY_PROGRESS:
                int progress = param.getInt(TXLiveConstants.EVT_PLAY_PROGRESS_MS);
                int duration = param.getInt(TXLiveConstants.EVT_PLAY_DURATION_MS);
                updatePlayProgress(progress / 1000, duration / 1000);
                break;
            case TXLiveConstants.PLAY_EVT_PLAY_BEGIN:
                updatePlayerState(PLAYING);
                break;
            default:
                break;
        }
        if (event < 0) {// ????????????????????????
            mVodPlayer.stopPlay(true);
            updatePlayerState(SuperPlayerDef.PlayerState.END);
//            onError(SuperPlayerCode.VOD_PLAY_FAIL, param.getString(TXLiveConstants.EVT_DESCRIPTION));
            if (event == TXLiveConstants.PLAY_ERR_NET_DISCONNECT) {
                onError(SuperPlayerCode.NET_ERROR, "???????????????,????????????");
            } else {
                onError(SuperPlayerCode.LIVE_PLAY_END, param.getString(TXLiveConstants.EVT_DESCRIPTION));
            }

        }
    }

    /**
     * ?????????????????????????????????
     *
     * @param player
     * @param bundle
     */
    @Override
    public void onNetStatus(TXVodPlayer player, Bundle bundle) {

    }

    private void initialize(Context context, TXCloudVideoView videoView) {
        mContext = context;
        mVideoView = videoView;
        initLivePlayer(mContext);
        initVodPlayer(mContext);
    }

    /**
     * ????????????????????????
     *
     * @param context
     */
    private void initVodPlayer(Context context) {
        mVodPlayer = new TXVodPlayer(context);
        SuperPlayerGlobalConfig config = SuperPlayerGlobalConfig.getInstance();
        mVodPlayConfig = new TXVodPlayConfig();

        File sdcardDir = context.getExternalFilesDir(null);
        if (sdcardDir != null) {
            mVodPlayConfig.setCacheFolderPath(sdcardDir.getPath() + "/txcache");
        }
        mVodPlayConfig.setMaxCacheItems(config.maxCacheItem);
        mVodPlayer.setConfig(mVodPlayConfig);
        mVodPlayer.setRenderMode(config.renderMode);
        mVodPlayer.setVodListener(this);
        mVodPlayer.enableHardwareDecode(config.enableHWAcceleration);
    }

    /**
     * ????????????????????????
     *
     * @param context
     */
    private void initLivePlayer(Context context) {
        mLivePlayer = new TXLivePlayer(context);
        SuperPlayerGlobalConfig config = SuperPlayerGlobalConfig.getInstance();
        mLivePlayConfig = new TXLivePlayConfig();
        mLivePlayer.setConfig(mLivePlayConfig);
        mLivePlayer.setRenderMode(config.renderMode);
        mLivePlayer.setRenderRotation(TXLiveConstants.RENDER_ROTATION_PORTRAIT);
        mLivePlayer.setPlayListener(this);
        mLivePlayer.enableHardwareDecode(config.enableHWAcceleration);
    }

    /**
     * ????????????
     *
     * @param model
     */
    public void playWithModel(final SuperPlayerModel model) {
        mCurrentModel = model;
        PlayInfoParams params = new PlayInfoParams();
        params.appId = model.appId;
        if (model.videoId != null) {
            params.fileId = model.videoId.fileId;
            params.videoId = model.videoId;
            mCurrentProtocol = new PlayInfoProtocolV4(params);
        } else if (model.videoIdV2 != null) {
            params.fileId = model.videoIdV2.fileId;
            params.videoIdV2 = model.videoIdV2;
            mCurrentProtocol = new PlayInfoProtocolV2(params);
        } else {
            mCurrentProtocol = null;    // ?????????????????????v2???v4???????????????????????????
        }
        mFileId = params.fileId;
        mAppId = params.appId;
        updateVideoImageSpriteAndKeyFrame(null, null);
        if (model.videoId != null || model.videoIdV2 != null) { // ??????FileId??????
            mCurrentProtocol.sendRequest(new IPlayInfoRequestCallback() {
                @Override
                public void onSuccess(IPlayInfoProtocol protocol, PlayInfoParams param) {
                    TXCLog.i(TAG, "onSuccess: protocol params = " + param.toString());
                    mReportVodStartTime = System.currentTimeMillis();
                    mVodPlayer.setPlayerView(mVideoView);
                    playModeVideo(mCurrentProtocol);
                    updatePlayerType(SuperPlayerDef.PlayerType.VOD);
                    updatePlayProgress(0, 0);
                    updateVideoImageSpriteAndKeyFrame(mCurrentProtocol.getImageSpriteInfo(), mCurrentProtocol.getKeyFrameDescInfo());
                }

                @Override
                public void onError(int errCode, String message) {
                    TXCLog.i(TAG, "onFail: errorCode = " + errCode + " message = " + message);
                    SuperPlayerImpl.this.onError(SuperPlayerCode.VOD_REQUEST_FILE_ID_FAIL, "???????????????????????? code = " + errCode + " msg = " + message);
                }
            });
        } else { // ??????URL??????
            String videoURL = null;
            List<VideoQuality> videoQualities = new ArrayList<>();
            VideoQuality defaultVideoQuality = null;
            if (model.multiURLs != null && !model.multiURLs.isEmpty()) {// ?????????URL??????
                int i = 0;
                for (SuperPlayerModel.SuperPlayerURL superPlayerURL : model.multiURLs) {
                    if (i == model.playDefaultIndex) {
                        videoURL = superPlayerURL.url;
                    }
                    videoQualities.add(new VideoQuality(i++, superPlayerURL.qualityName, superPlayerURL.url));
                }
                defaultVideoQuality = videoQualities.get(model.playDefaultIndex);
            } else if (!TextUtils.isEmpty(model.url)) { // ??????URL????????????
                videoURL = model.url;
            }

            if (TextUtils.isEmpty(videoURL)) {
                onError(SuperPlayerCode.PLAY_URL_EMPTY, "???????????????????????????????????????");
                return;
            }
            if (isRTMPPlay(videoURL)) { // ????????????????????????RTMP?????????
                mReportLiveStartTime = System.currentTimeMillis();
                mLivePlayer.setPlayerView(mVideoView);
                playLiveURL(videoURL, TXLivePlayer.PLAY_TYPE_LIVE_RTMP);
            } else if (isFLVPlay(videoURL)) { // ????????????????????????FLV?????????
                mReportLiveStartTime = System.currentTimeMillis();
                mLivePlayer.setPlayerView(mVideoView);
                playTimeShiftLiveURL(model.appId, videoURL);
                if (model.multiURLs != null && !model.multiURLs.isEmpty()) {
                    startMultiStreamLiveURL(videoURL);
                }
            } else { // ????????????????????????????????????
                mReportVodStartTime = System.currentTimeMillis();
                mVodPlayer.setPlayerView(mVideoView);
                mObserver.onPlayTimeShiftVod(mVodPlayer, videoURL);
                playVodURL(videoURL);
            }
            boolean isLivePlay = (isRTMPPlay(videoURL) || isFLVPlay(videoURL));
            updatePlayerType(isLivePlay ? SuperPlayerDef.PlayerType.LIVE : SuperPlayerDef.PlayerType.VOD);
            updatePlayProgress(0, 0);
            updateVideoQualityList(videoQualities, defaultVideoQuality);
        }
    }

    /**
     * ??????v2???v4????????????
     *
     * @param protocol
     */
    private void playModeVideo(IPlayInfoProtocol protocol) {
        playVodURL(protocol.getUrl());
        List<VideoQuality> videoQualities = protocol.getVideoQualityList();
        mIsMultiBitrateStream = videoQualities == null;
        VideoQuality defaultVideoQuality = protocol.getDefaultVideoQuality();
        updateVideoQualityList(videoQualities, defaultVideoQuality);
    }

    /**
     * ?????????v2???v4????????????
     *
     * @param model
     */
    private void playModeVideo(SuperPlayerModel model) {
        if (model.multiURLs != null && !model.multiURLs.isEmpty()) {// ?????????URL??????
            for (int i = 0; i < model.multiURLs.size(); i++) {
                if (i == model.playDefaultIndex) {
                    playVodURL(model.multiURLs.get(i).url);
                }
            }
        } else if (!TextUtils.isEmpty(model.url)) {
            playVodURL(model.url);
        }
    }

    /**
     * ????????????URL
     */
    private void playLiveURL(String url, int playType) {
        mCurrentPlayVideoURL = url;
        if (mLivePlayer != null) {
            mLivePlayer.setPlayListener(this);
            int result = mLivePlayer.startPlay(url, playType); // result????????????0 success;  -1 empty url; -2 invalid url; -3 invalid playType;
            if (result != 0) {
                TXCLog.e(TAG, "playLiveURL videoURL:" + url + ",result:" + result);
            } else {
                updatePlayerState(PLAYING);
            }
        }
    }

    /**
     * ????????????url
     */
    private void playVodURL(String url) {
        if (url == null || "".equals(url)) {
            mCurrentPlayVideoURL = url;
            return;
        }
        mCurrentPlayVideoURL = url;
        if (url.contains(".m3u8")) {
            mIsMultiBitrateStream = true;
        }
        if (mVodPlayer != null) {
            mDefaultQualitySet = false;
            mVodPlayer.setStartTime(0);
            mVodPlayer.setAutoPlay(true);
            mVodPlayer.setVodListener(this);
            String drmType = "plain";
            if (mCurrentProtocol != null) {
                TXCLog.d(TAG, "TOKEN: " + mCurrentProtocol.getToken());
                mVodPlayer.setToken(mCurrentProtocol.getToken());
                String type = mCurrentProtocol.getDRMType();
                if (type != null && !type.isEmpty()) {
                    drmType = type;
                }
            } else {
                mVodPlayer.setToken(null);
            }
            int ret = 0;
            if (isVersionSupportAppendUrl()) {
                Uri uri = Uri.parse(url);
                String query = uri.getQuery();
                if (query == null || query.isEmpty()) {
                    query = "";
                } else {
                    query = query + "&";
                    if (query.contains("spfileid") || query.contains("spdrmtype") || query.contains("spappid")) {
                        TXCLog.e(TAG, "url contains superplay key. " + query);
                    }
                }
                query += "spfileid=" + mFileId + "&spdrmtype=" + drmType + "&spappid=" + mAppId;
                Uri newUri = uri.buildUpon().query(query).build();
                TXCLog.i(TAG, "playVodURL: newurl = " + Uri.decode(newUri.toString()) + " ;url= " + url);
                ret = mVodPlayer.startPlay(Uri.decode(newUri.toString()));
            } else {
                ret = mVodPlayer.startPlay(url); //??????????????????
            }
            superPlayerView.detailIsLoad = false;
            superPlayerView.homeVideoIsLoad = false;
            if (ret == 0) {
                updatePlayerState(SuperPlayerDef.PlayerState.LOADING);
            }
        }
        mIsPlayWithFileId = false;
    }

    private boolean isVersionSupportAppendUrl() {
        String strVersion = TXLiveBase.getSDKVersionStr();
        String[] strVers = strVersion.split("\\.");
        if (strVers.length <= 1) {
            return false;
        }
        int majorVer = 0;
        int minorVer = 0;
        try {
            majorVer = Integer.parseInt(strVers[0]);
            minorVer = Integer.parseInt(strVers[1]);
        } catch (NumberFormatException e) {
            TXCLog.e(TAG, "parse version failed.", e);
            majorVer = 0;
            minorVer = 0;
        }
        Log.i(TAG, strVersion + " , " + majorVer + " , " + minorVer);
        return majorVer > SUPPORT_MAJOR_VERSION || (majorVer == SUPPORT_MAJOR_VERSION && minorVer >= SUPPORT_MINOR_VERSION);
    }

    /**
     * ??????????????????url
     */
    private void playTimeShiftLiveURL(int appId, String url) {
        final String bizid = url.substring(url.indexOf("//") + 2, url.indexOf("."));
        final String domian = SuperPlayerGlobalConfig.getInstance().playShiftDomain;
        final String streamid = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("."));
        TXCLog.i(TAG, "bizid:" + bizid + ",streamid:" + streamid + ",appid:" + appId);
        playLiveURL(url, TXLivePlayer.PLAY_TYPE_LIVE_FLV);
        int bizidNum = -1;
        try {
            bizidNum = Integer.parseInt(bizid);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            TXCLog.e(TAG, "playTimeShiftLiveURL: bizidNum error = " + bizid);
        }
        mLivePlayer.prepareLiveSeek(domian, bizidNum);
    }

    /**
     * ???????????????url
     *
     * @param url
     */
    private void startMultiStreamLiveURL(String url) {
        mLivePlayConfig.setAutoAdjustCacheTime(false);
        mLivePlayConfig.setMaxAutoAdjustCacheTime(5);
        mLivePlayConfig.setMinAutoAdjustCacheTime(5);
        mLivePlayer.setConfig(mLivePlayConfig);
        if (mObserver != null) {
            mObserver.onPlayTimeShiftLive(mLivePlayer, url);
        }
    }

    /**
     * ??????????????????
     */
    public void reportPlayTime() {
        if (mReportLiveStartTime != -1) {
            long reportEndTime = System.currentTimeMillis();
            long diff = (reportEndTime - mReportLiveStartTime) / 1000;
            LogReport.getInstance().uploadLogs(LogReport.ELK_ACTION_LIVE_TIME, diff, 0);
            mReportLiveStartTime = -1;
        }
        if (mReportVodStartTime != -1) {
            long reportEndTime = System.currentTimeMillis();
            long diff = (reportEndTime - mReportVodStartTime) / 1000;
            LogReport.getInstance().uploadLogs(LogReport.ELK_ACTION_VOD_TIME, diff, mIsPlayWithFileId ? 1 : 0);
            mReportVodStartTime = -1;
        }
    }

    /**
     * ??????????????????
     *
     * @param current  ??????????????????(???)
     * @param duration ?????????(???)
     */
    private void updatePlayProgress(long current, long duration) {
        if (mObserver != null) {
            mObserver.onPlayProgress(current, duration);
        }
    }

    /**
     * ??????????????????
     *
     * @param playType
     */
    private void updatePlayerType(SuperPlayerDef.PlayerType playType) {
        if (playType != mCurrentPlayType) {
            mCurrentPlayType = playType;
        }
        if (mObserver != null) {
            mObserver.onPlayerTypeChange(playType);
        }
    }

    /**
     * ??????????????????
     *
     * @param playState
     */
    private void updatePlayerState(SuperPlayerDef.PlayerState playState) {
        mCurrentPlayState = playState;
        VideoPlayerParam param = VideoPlayerParam.getInstance();

        try {
            if (playState.equals(PLAYING)) {
                param.setVideoPlayStates(true);
            } else {
                param.setVideoPlayStates(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mObserver == null) {
            return;
        }

        switch (playState) {
            case PLAYING:
                mObserver.onPlayBegin(getPlayName());
                break;
            case PAUSE:
                mObserver.onPlayPause();
                break;
            case LOADING:
                mObserver.onPlayLoading(getPlayName());
                break;
            case END:
                mObserver.onPlayStop();
                break;
        }
    }

    private void updateStreamStartStatus(boolean success, SuperPlayerDef.PlayerType playerType, VideoQuality quality) {
        if (mObserver != null) {
            mObserver.onSwitchStreamStart(success, playerType, quality);
        }
    }

    private void updateStreamEndStatus(boolean success, SuperPlayerDef.PlayerType playerType, VideoQuality quality) {
        if (mObserver != null) {
            mObserver.onSwitchStreamEnd(success, playerType, quality);
        }
    }

    private void updateVideoQualityList(List<VideoQuality> videoQualities, VideoQuality defaultVideoQuality) {
        if (mObserver != null) {
            mObserver.onVideoQualityListChange(videoQualities, defaultVideoQuality);
        }
    }

    private void updateVideoImageSpriteAndKeyFrame(PlayImageSpriteInfo info, List<PlayKeyFrameDescInfo> list) {
        if (mObserver != null) {
            mObserver.onVideoImageSpriteAndKeyFrameChanged(info, list);
        }
    }

    private void onError(int code, String message) {
        if (mObserver != null) {
            mObserver.onError(code, message);
        }
    }

    private String getPlayName() {
        String title = "";
        if (mCurrentModel != null && !TextUtils.isEmpty(mCurrentModel.title)) {
            title = mCurrentModel.title;
        } else if (mCurrentProtocol != null && !TextUtils.isEmpty(mCurrentProtocol.getName())) {
            title = mCurrentProtocol.getName();
        }
        return title;
    }

    /**
     * ?????????RTMP??????
     *
     * @param videoURL
     * @return
     */
    private boolean isRTMPPlay(String videoURL) {
        return !TextUtils.isEmpty(videoURL) && videoURL.startsWith("rtmp");
    }

    /**
     * ?????????HTTP-FLV??????
     *
     * @param videoURL
     * @return
     */
    private boolean isFLVPlay(String videoURL) {
        return (!TextUtils.isEmpty(videoURL) && videoURL.startsWith("http://")
                || videoURL.startsWith("https://")) && videoURL.contains(".flv");
    }

    @Override
    public void play(SuperPlayerModel superPlayerModel) {
        playWithModel(superPlayerModel);
    }

    @Override
    public void play(int appId, String url) {
        SuperPlayerModel model = new SuperPlayerModel();
        model.appId = appId;
        model.url = url;
        playWithModel(model);
    }

    @Override
    public void play(int appId, String fileId, String psign) {
        SuperPlayerVideoId videoId = new SuperPlayerVideoId();
        videoId.fileId = fileId;
        videoId.pSign = psign;

        SuperPlayerModel model = new SuperPlayerModel();
        model.appId = appId;
        model.videoId = videoId;
        playWithModel(model);
    }

    @Override
    public void play(int appId, List<SuperPlayerModel.SuperPlayerURL> superPlayerURLS, int defaultIndex) {
        SuperPlayerModel model = new SuperPlayerModel();
        model.appId = appId;
        model.multiURLs = superPlayerURLS;
        model.playDefaultIndex = defaultIndex;
        playWithModel(model);
    }

    @Override
    public void reStart() {
        if (mCurrentPlayType == SuperPlayerDef.PlayerType.LIVE || mCurrentPlayType == SuperPlayerDef.PlayerType.LIVE_SHIFT) {
            if (isRTMPPlay(mCurrentPlayVideoURL)) {
                playLiveURL(mCurrentPlayVideoURL, TXLivePlayer.PLAY_TYPE_LIVE_RTMP);
            } else if (isFLVPlay(mCurrentPlayVideoURL)) {
                playTimeShiftLiveURL(mCurrentModel.appId, mCurrentPlayVideoURL);
                if (mCurrentModel.multiURLs != null && !mCurrentModel.multiURLs.isEmpty()) {
                    startMultiStreamLiveURL(mCurrentPlayVideoURL);
                }
            }
        } else {
            //??????  ??????-????????????  ??????
            if (noFirst) {
                superPlayerView.buriedPointModel.setIs_renew("true");
            }

            if (xkshNoFirst) {
                superPlayerView.buriedPointModel.setXksh_renew("true");
            }

            noFirst = true;
            xkshNoFirst = true;
            playVodURL(mCurrentPlayVideoURL);
        }
    }

    @Override
    public void pause() {
        if (mCurrentPlayType == SuperPlayerDef.PlayerType.VOD) {
            if (superPlayerView.detailIsLoad) {
                mVodPlayer.pause();
            } else {
                superPlayerView.release();
                superPlayerView.mSuperPlayer.stop();
                superPlayerView.mSuperPlayer.destroy();
                return;
            }
        } else {
            mLivePlayer.pause();
        }
        updatePlayerState(SuperPlayerDef.PlayerState.PAUSE);
    }

    @Override
    public void pauseVod() {
        if (mCurrentPlayType == SuperPlayerDef.PlayerType.VOD) {
            mVodPlayer.pause();
        }
        updatePlayerState(SuperPlayerDef.PlayerState.PAUSE);
    }

    @Override
    public void resume() {
        if (mCurrentPlayType == SuperPlayerDef.PlayerType.VOD) {
            mVodPlayer.resume();
        } else {
            mLivePlayer.resume();
        }
        updatePlayerState(PLAYING);
    }

    @Override
    public void resumeLive() {
        if (mCurrentPlayType == SuperPlayerDef.PlayerType.LIVE_SHIFT) {
            mLivePlayer.resumeLive();
        }
        updatePlayerType(SuperPlayerDef.PlayerType.LIVE);
    }

    @Override
    public void stop() {
        if (mVodPlayer != null) {
            mVodPlayer.stopPlay(false);
        }
        if (mLivePlayer != null) {
            mLivePlayer.stopPlay(false);
            mVideoView.removeVideoView();
        }
        updatePlayerState(SuperPlayerDef.PlayerState.END);
//        reportPlayTime();
    }

    @Override
    public void destroy() {

    }

    @Override
    public void switchPlayMode(SuperPlayerDef.PlayerMode playerMode) {
        if (mCurrentPlayMode == playerMode) {
            return;
        }
        mCurrentPlayMode = playerMode;
    }

    @Override
    public void enableHardwareDecode(boolean enable) {
        if (mCurrentPlayType == SuperPlayerDef.PlayerType.VOD) {
            mChangeHWAcceleration = true;
            mVodPlayer.enableHardwareDecode(enable);
            mSeekPos = (int) mVodPlayer.getCurrentPlaybackTime();
            TXCLog.i(TAG, "save pos:" + mSeekPos);
            stop();
            if (mCurrentProtocol == null) {    // ???protocol?????????????????????????????????????????????v2???v4??????
                playModeVideo(mCurrentModel);
            } else {
                playModeVideo(mCurrentProtocol);
            }
        } else {
            mLivePlayer.enableHardwareDecode(enable);
            playWithModel(mCurrentModel);
        }
        // ??????????????????
        if (enable) {
            LogReport.getInstance().uploadLogs(LogReport.ELK_ACTION_HW_DECODE, 0, 0);
        } else {
            LogReport.getInstance().uploadLogs(LogReport.ELK_ACTION_SOFT_DECODE, 0, 0);
        }
    }

    @Override
    public void setPlayerView(TXCloudVideoView videoView) {
        if (mCurrentPlayType == SuperPlayerDef.PlayerType.VOD) {
            mVodPlayer.setPlayerView(videoView);
        } else {
            mLivePlayer.setPlayerView(videoView);
        }
    }

    @Override
    public void seek(int position) {
        if (mCurrentPlayType == SuperPlayerDef.PlayerType.VOD) {
            if (mVodPlayer != null) {
                mVodPlayer.seek(position);
            }
        } else {
            updatePlayerType(SuperPlayerDef.PlayerType.LIVE_SHIFT);
            LogReport.getInstance().uploadLogs(LogReport.ELK_ACTION_TIMESHIFT, 0, 0);
            if (mLivePlayer != null) {
                mLivePlayer.seek(position);
            }
        }
        if (mObserver != null) {
            mObserver.onSeek(position);
        }
    }

    @Override
    public void snapshot(TXLivePlayer.ITXSnapshotListener listener) {
        if (mCurrentPlayType == SuperPlayerDef.PlayerType.VOD) {
            mVodPlayer.snapshot(listener);
        } else if (mCurrentPlayType == SuperPlayerDef.PlayerType.LIVE) {
            mLivePlayer.snapshot(listener);
        } else {
            listener.onSnapshot(null);
        }
    }

    @Override
    public void setRate(float speedLevel) {
        if (mCurrentPlayType == SuperPlayerDef.PlayerType.VOD) {
            mVodPlayer.setRate(speedLevel);
        }
        //??????????????????
        LogReport.getInstance().uploadLogs(LogReport.ELK_ACTION_CHANGE_SPEED, 0, 0);

    }

    @Override
    public void setMirror(boolean isMirror) {
        if (mCurrentPlayType == SuperPlayerDef.PlayerType.VOD) {
            mVodPlayer.setMirror(isMirror);
        }
        if (isMirror) {
            LogReport.getInstance().uploadLogs(LogReport.ELK_ACTION_MIRROR, 0, 0);
        }
    }

    @Override
    public void switchStream(VideoQuality quality) {
        mVideoQuality = quality;
        if (mCurrentPlayType == SuperPlayerDef.PlayerType.VOD) {
            if (mVodPlayer != null) {
                if (quality.url != null) { // br!=0;index=-1;url!=null   //br=0;index!=-1;url!=null
                    // ???????????????bitrate???m3u8?????????????????????seek
                    float currentTime = mVodPlayer.getCurrentPlaybackTime();
                    mVodPlayer.stopPlay(true);
                    TXCLog.i(TAG, "onQualitySelect quality.url:" + quality.url);
                    mVodPlayer.setStartTime(currentTime);
                    mVodPlayer.startPlay(quality.url);
                } else { //br!=0;index!=-1;url=null
                    TXCLog.i(TAG, "setBitrateIndex quality.index:" + quality.index);
                    // ????????????bitrate???m3u8????????????????????????seek
                    mVodPlayer.setBitrateIndex(quality.index);
                }
                updateStreamStartStatus(true, SuperPlayerDef.PlayerType.VOD, quality);
            }
        } else {
            boolean success = false;
            if (mLivePlayer != null && !TextUtils.isEmpty(quality.url)) {
                int result = mLivePlayer.switchStream(quality.url);
                success = result >= 0;
            }
            updateStreamStartStatus(success, SuperPlayerDef.PlayerType.LIVE, quality);
        }
        //???????????????
        LogReport.getInstance().uploadLogs(LogReport.ELK_ACTION_CHANGE_RESOLUTION, 0, 0);
    }

    @Override
    public String getPlayURL() {
        return mCurrentPlayVideoURL;
    }

    @Override
    public SuperPlayerDef.PlayerMode getPlayerMode() {
        return mCurrentPlayMode;
    }

    @Override
    public SuperPlayerDef.PlayerState getPlayerState() {
        return mCurrentPlayState;
    }

    @Override
    public SuperPlayerDef.PlayerType getPlayerType() {
        return mCurrentPlayType;
    }

    @Override
    public void setObserver(SuperPlayerObserver observer) {
        mObserver = observer;
    }

    @Override
    public void setRenderMode(int renderMode) {
        mVodPlayer.setRenderMode(renderMode);
    }
}
