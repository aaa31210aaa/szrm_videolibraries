package com.szrm.videolibraries;



import android.app.Application;

import common.utils.AppInit;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppInit.init(this, false);
    }

}
