package common.utils;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.ViewGroup;


import com.szrm.videolibraries.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ButtonSpan extends ClickableSpan {
    private static String regEx = "[\u4e00-\u9fa5]"; // 中文范围
    View.OnClickListener onClickListener;
    private Context context;
    private int colorId;

    public ButtonSpan(Context context, View.OnClickListener onClickListener) {
        this(context, onClickListener, R.color.white);
    }

    public ButtonSpan(Context context, View.OnClickListener onClickListener, int colorId) {
        this.onClickListener = onClickListener;
        this.context = context;
        this.colorId = colorId;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        ds.setColor(context.getResources().getColor(colorId));
        ds.setTextSize(dip2px(16));
        ds.setUnderlineText(false);
    }

    @Override
    public void onClick(View widget) {
        if (onClickListener != null) {
            onClickListener.onClick(widget);
        }
    }

    public static int dip2px(float dipValue) {
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(float pxValue) {
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static void setMargins (View v, int left, int top, int right, int bottom) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins(left, top, right, bottom);
            v.requestLayout();
        }
    }

    /**
     *
     * 截取字符串，超出最大字数截断并显示"..."
     * @param str 原始字符串
     * @param length 最大字数限制（以最大字数限制7个为例，当含中文时，length应设为2*7，不含中文时设为7）
     * @return 处理后的字符串
     */
    public static String subStrByLen(String str, int length) {
        if (str == null || str.length() == 0) {
            return "";
        }
        int chCnt = getStrLen(str);
        // 超出进行截断处理
        if (chCnt > length) {
            int cur = 0;
            int cnt = 0;
            StringBuilder sb = new StringBuilder();
            while (cnt <= length && cur < str.length()) {
                char nextChar = str.charAt(cur);
                if (isChCharacter(String.valueOf(nextChar))) {
                    cnt += 2;
                } else {
                    cnt++;
                }
                if (cnt <= length) {
                    sb.append(nextChar);
                } else {
                    return sb.toString() + "...";
                }
                cur++;
            }
            return sb.toString() + "...";
        }
        // 未超出直接返回
        return str;
    }

    /**
     * 判断字符是不是中文
     */
    private static boolean isChCharacter(String str) {
        if (str == null || str.length() == 0) {
            return false;
        }
        if (str.length() > 1) {
            return false;
        }
        return Pattern.matches(regEx, str);
    }

    /**
     * 获取字符长度，中文算作2个字符，其他都算1个字符
     */
    public static int getStrLen(String str) {
        if (str == null || str.length() == 0) {
            return 0;
        }
        return str.length() + getChCount(str);
    }



    /**
     * 获取字符串中的中文字数
     */
    private static int getChCount(String str) {
        int cnt = 0;
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(str);;
        while(matcher.find()) {
            cnt++;
        }
        return cnt;
    }
}
