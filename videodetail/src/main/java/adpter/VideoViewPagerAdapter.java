package adpter;

import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;


import java.util.ArrayList;
import java.util.List;

import common.model.VideoChannelModel;
import tencent.liteav.demo.superplayer.SuperPlayerView;
import ui.fragment.LiveFragment;
import ui.fragment.VideoDetailFragment;
import ui.fragment.XkshFragment;

public class VideoViewPagerAdapter extends FragmentPagerAdapter {
    public List<Fragment> fragmentList = new ArrayList<>();
    private List<VideoChannelModel> titleList = new ArrayList<>();

    @Override
    public int getCount() {
        return fragmentList.size();
    }

    public VideoViewPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
    }


    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return titleList.get(position).getColumnBean().getColumnName();
    }

    @Override
    public Fragment getItem(int position) {
        return fragmentList.get(position);
    }

    public void clearItems() {
        titleList.clear();
        fragmentList.clear();
        notifyDataSetChanged();
    }

    public void addItems(@Nullable List<VideoChannelModel> channelBeanList, String contentId, String categoryName, SuperPlayerView playerView
            , int toCurrentTab) {
        titleList.clear();
        fragmentList.clear();
        titleList.addAll(channelBeanList);
        for (VideoChannelModel videoChannelModel : channelBeanList) {
            if (TextUtils.equals("2", videoChannelModel.getColumnBean().getColumnId())) {
                LiveFragment fragment = new LiveFragment();
                fragmentList.add(fragment.newInstance(fragment, videoChannelModel));
            } else if (TextUtils.equals("1", videoChannelModel.getColumnBean().getColumnId())) {
                VideoDetailFragment fragment = new VideoDetailFragment();
                fragment.setPlayView(playerView);
                fragmentList.add(fragment.newInstance(fragment, videoChannelModel, contentId, categoryName, toCurrentTab));
            } else {
                XkshFragment fragment = new XkshFragment();
                fragment.setPlayView(playerView);
                fragmentList.add(fragment.newInstance(fragment, videoChannelModel, contentId, categoryName, toCurrentTab));
            }
        }
        notifyDataSetChanged();
    }

    public void remove(@Nullable VideoChannelModel bean) {
        int pos = titleList.indexOf(bean);
        titleList.remove(pos);
        fragmentList.remove(pos);
        notifyDataSetChanged();
    }


    @Override
    public long getItemId(int position) {
        return titleList.get(position).hashCode();
    }


    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }
}
