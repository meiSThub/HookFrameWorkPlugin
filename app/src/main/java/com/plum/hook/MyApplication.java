package com.plum.hook;

import android.app.Application;

/**
 * Created by baby on 2018/4/2.
 */

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        HookUtil hookUtil = new HookUtil();
        hookUtil.hookStartActivity(this);
        hookUtil.hookHookMh(this);
    }
}
