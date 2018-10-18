package com.plum.hook;

import android.app.Application;
import android.content.res.AssetManager;

/**
 * Created by baby on 2018/4/2.
 */

public class MyApplication extends Application {

    private AssetManager mAssetManager;

    @Override
    public void onCreate() {
        super.onCreate();
        HookUtil hookUtil = new HookUtil();
        hookUtil.hookStartActivity(this);
        hookUtil.hookHookMh(this);
        // 初始化插件的dex
        hookUtil.injectPluginDex(this);

        try {
            mAssetManager = AssetManager.class.newInstance();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
