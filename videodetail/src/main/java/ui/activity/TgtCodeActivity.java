package ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.szrm.videolibraries.R;

import common.callback.VideoInteractiveParam;
import common.utils.PersonInfoManager;
import common.widget.YALikeAnimationView;


public class TgtCodeActivity extends AppCompatActivity {
    private EditText wdTgt;
    private EditText localTgt;
    private EditText gdyTgt;
    private EditText localToken;
    private TextView getWdtgt;
    private YALikeAnimationView likeLayout2;
    private RelativeLayout dianzan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tgt_code);
        wdTgt = findViewById(R.id.wdTgt);
        localTgt = findViewById(R.id.localTgt);
        gdyTgt = findViewById(R.id.gdyTgt);
        getWdtgt = findViewById(R.id.getWdtgt);
        localToken = findViewById(R.id.localToken);
        dianzan = findViewById(R.id.dianzan);
        likeLayout2 = findViewById(R.id.like_love);
        dianzan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                likeLayout2.startAnimation();
            }
        });

        getWdtgt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    wdTgt.setText("从万达获取的tgt："+ VideoInteractiveParam.getInstance().getCode());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        try {
            localTgt.setText("本地tgt码:" + PersonInfoManager.getInstance().getTgtCode());
            gdyTgt.setText("广电云tgt：" + PersonInfoManager.getInstance().getGdyToken());
            localToken.setText("数智融媒token：" + PersonInfoManager.getInstance().getTransformationToken());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}