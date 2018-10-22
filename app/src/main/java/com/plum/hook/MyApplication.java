package com.plum.hook;

import android.app.Application;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;

import java.lang.reflect.Method;

/**
 * Created by baby on 2018/4/2.
 */

public class MyApplication extends Application {

    private AssetManager mAssetManager;

    private Resources mResources;

    @Override
    public void onCreate() {
        super.onCreate();
        HookUtil hookUtil = new HookUtil();
        hookUtil.hookStartActivity(this);
        hookUtil.hookHookMh(this);
        // 初始化插件的dex
        hookUtil.injectPluginDex(this);

        String apkPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/plugin.apk";

        try {
            mAssetManager = AssetManager.class.newInstance();
            //把插件apk的资源加入到AssetManager实例中,指定插件apk的资源路径
            Method addAssetPathMethod = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            addAssetPathMethod.setAccessible(true);
            addAssetPathMethod.invoke(mAssetManager, apkPath);

            // 调用ensureStringBlocks方法，生成资源对应的StringBlock数组
            Method ensureStringBlocksMethod = AssetManager.class.getDeclaredMethod("ensureStringBlocks");
            ensureStringBlocksMethod.setAccessible(true);
            ensureStringBlocksMethod.invoke(mAssetManager);

            Resources superResources = getResources();
            mResources = new Resources(mAssetManager, superResources.getDisplayMetrics(),
                    superResources.getConfiguration());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public AssetManager getAssets() {
        return mAssetManager == null ? super.getAssets() : mAssetManager;
    }

    @Override
    public Resources getResources() {
        return mResources == null ? super.getResources() : mResources;
    }
}
