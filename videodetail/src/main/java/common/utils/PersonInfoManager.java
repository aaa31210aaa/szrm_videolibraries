package common.utils;

import android.text.TextUtils;

import common.callback.VideoInteractiveParam;
import common.constants.Constants;


public class PersonInfoManager {
    private static PersonInfoManager instance;

    public static PersonInfoManager getInstance() {
        if (instance == null) {
            synchronized (PersonInfoManager.class) {
                if (instance == null) {
                    instance = new PersonInfoManager();
                }
            }
        }
        return instance;
    }


    /**
     * 保存用户token
     *
     * @param token
     */
    public void setToken(String token) {
        SPUtils.getInstance().put(Constants.TYPE_TOKEN, token);
    }

    /**
     * 获取用户token
     *
     * @return
     */
    public String getToken() {
        return SPUtils.getInstance().getString(Constants.TYPE_TOKEN, "");
    }

    /**
     * 获取用户id
     */
    public String getUserId() {
        return SPUtils.getInstance().getString(Constants.LOCAL_USER_ID, "");
    }

    /**
     * 保存用户id
     */
    public void setUserId(String userId) {
        SPUtils.getInstance().put(Constants.LOCAL_USER_ID, userId);
    }

    /**
     * 获取光电云token
     */
    public String getGdyToken() {
        return SPUtils.getInstance().getString(Constants.GDY_TOKEN, "");
    }

    /**
     * 保存广电云token
     */
    public void setGdyToken(String userId) {
        SPUtils.getInstance().put(Constants.GDY_TOKEN, userId);
    }

    public void setTgtCode(String tgt) {
        SPUtils.getInstance().put(Constants.TGT_CODE, tgt);
    }

    public String getTgtCode() {
        return SPUtils.getInstance().getString(Constants.TGT_CODE, "");
    }

    /**
     * 保存转换后的用户token
     *
     * @param transformationToken
     */
    public void setTransformationToken(String transformationToken) {
        SPUtils.getInstance().put(Constants.TRANSFORMATION_TOKEN, transformationToken);
    }

    /**
     * 获取转换后的用户token
     * return
     */
    public String getTransformationToken() {
        return SPUtils.getInstance().getString(Constants.TRANSFORMATION_TOKEN, "");
    }

    public void setUploadAgreement(String uploadAgreement) {
        SPUtils.getInstance().put(Constants.UPLOAD_AGREEMENT, uploadAgreement);
    }

    /**
     * 上传视频协议是否显示
     */
    public String getUploadAgreement() {
        return SPUtils.getInstance().getString(Constants.UPLOAD_AGREEMENT, "");
    }

    /**
     * 是否已经实名
     */
    public String getCertificated() {
        return SPUtils.getInstance().getString(Constants.CERTIFICATED, "");
    }

    public void setCertificated(String certificated) {
        SPUtils.getInstance().put(Constants.CERTIFICATED, certificated);
    }

    /**
     * 设置用户昵称
     */
    public void setAppId(String nickName) {
        SPUtils.getInstance().put(Constants.USER_APPID, nickName);
    }

    /**
     * 获取用户昵称
     */
    public String getAppId() {
        return SPUtils.getInstance().getString(Constants.USER_APPID, "");
    }

    /**
     * 保存用户头像
     */
    public void setUserImageUrl(String phoneNum) {
        SPUtils.getInstance().put(Constants.USER_IMAGE_URL, phoneNum);
    }

    /**
     * 获取用户头像
     */
    public String getUserImageUrl() {
        return SPUtils.getInstance().getString(Constants.USER_IMAGE_URL, "");
    }

    /**
     * 设置用户昵称
     */
    public void setNickName(String appId) {
        SPUtils.getInstance().put(Constants.USER_NICKNAME, appId);
    }

    /**
     * 获取用户昵称
     */
    public String getNickName() {
        return SPUtils.getInstance().getString(Constants.USER_NICKNAME, "");
    }

    /**
     * 保存用户手机号
     */
    public void setPhoneNum(String phoneNum) {
        SPUtils.getInstance().put(Constants.USER_PHONE_NUM, phoneNum);
    }

    /**
     * 获取用户手机号
     */
    public String getPhoneNum() {
        return SPUtils.getInstance().getString(Constants.USER_PHONE_NUM, "");
    }

    /**
     * 保存数智融媒登录成功后的用户对象
     */
    public void setSzrmUserModel(String szrmUserModel) {
        SPUtils.getInstance().put(Constants.SZRM_USER_MODEL, szrmUserModel);
    }

    /**
     * 获取转换后的用户token
     * return
     */
    public String getSzrmUserModel() {
        return SPUtils.getInstance().getString(Constants.SZRM_USER_MODEL, "");
    }


    /**
     * 清空本地token
     */
    public void clearToken() {
        PersonInfoManager.getInstance().setToken("");
        PersonInfoManager.getInstance().setTransformationToken(null);
        PersonInfoManager.getInstance().setTgtCode("");
        PersonInfoManager.getInstance().setUserId("");
        PersonInfoManager.getInstance().setGdyToken("");
    }


    /**
     * 清空本地存的token 和用户信息
     */
    public void clearThirdUserToken() {
        setAppId("");
        setTransformationToken("");
        setUserId("");
        setUserImageUrl("");
        setNickName("");
        setPhoneNum("");
        setSzrmUserModel("");
    }


    /**
     * 判断是否要去请求转换token的接口
     *
     * @return
     */
    public boolean isRequestToken() {
        try {
            String wdTgt = VideoInteractiveParam.getInstance().getCode(); //从万达拿的tgt
            String localTgt = PersonInfoManager.getInstance().getTgtCode(); //本地存储的tgt
            if (!TextUtils.isEmpty(wdTgt)) {//获取的token不为空
                if (!TextUtils.isEmpty(localTgt)) { //本地token不为空
                    if (TextUtils.equals(localTgt, wdTgt)) {
//                        DebugLogUtils.DebugLog("我的长沙已登录_数智已登");
                        return false;
                    } else {
//                        DebugLogUtils.DebugLog("获取到的tgt和本地tgt不一致,我的长沙切换了用户_重登数智融媒");
                        clearToken();
                        return true;
                    }
                } else {
//                    DebugLogUtils.DebugLog("本地tgt为空，相当于第一次登录，需要请求");
                    return true;
                }

            } else {
                //获取的tgt为空，没有登录，不需要请求  点击点赞收藏等则会跳转登录
                clearToken();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
