package com.plum.hook;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

/**
 * Created by mei on 2018/10/14.
 * Description:
 * 通过Hook技术，在Activity启动的时候，绕过系统的检查，
 * 从而启动没有在Manifest文件中注册的Activity
 * <p>
 * 实现步骤：
 */

public class HookUtil {

    /**
     * Hook 系统的startActivity方法，增加我们自己的实现逻辑
     * <p>
     * 实现原理：Hook（反射）+动态代理
     */
    public void hookStartActivity(Context context) {
        try {
            Class<?> activityManagerClass = Class.forName("android.app.ActivityManager");
            Field iActivityManagerSingletonField = activityManagerClass.getDeclaredField("IActivityManagerSingleton");
            iActivityManagerSingletonField.setAccessible(true);
            // 静态变量，反射取值时，直接传null
            // 因为是静态变量  所以获取的到的是系统值  hook   伪hook
            Object iActivityManagerSingleton = iActivityManagerSingletonField.get(null);

            //mInstance对象
            Class<?> SingletonClass = Class.forName("android.util.Singleton");
            Field iActivityManagerField = SingletonClass.getDeclaredField("mInstance");
            iActivityManagerField.setAccessible(true);
            // 拿到系统的IActivityManager对象  系统对象
            Object IActivityManagerInstance = iActivityManagerField.get(iActivityManagerSingleton);

            // 通过动态代理，代理系统的IActivityManager对象，从而实现添加自己逻辑的代码

            // Proxy.newProxyInstance 方法需要的参数
            // 1.当前线程的ClassLoader
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            // 2.Proxy.newProxyInstance即将返回的对象 需要实现那些接口
            // 被代理的对象所实现的接口的类型,我们需要代理的对象是一个实现了IActivityManager接口的对象，
            Class<?> iActivityManagerClass = Class.forName("android.app.IActivityManager");
            // 3.实现InvocationHandler接口的类对象
            InvocationHandler invocationHandler = new StartActivity(IActivityManagerInstance, context);

            // oldIActivityManager返回来的代理对象，该对象实现了我们指定的接口，如：IActivityManager
            Object oldIActivityManager = Proxy.newProxyInstance(classLoader,
                    new Class[]{iActivityManagerClass}, invocationHandler);

            // 将系统的iActivityManager  替换成    自己通过动态代理实现的对象   oldIactivityManager对象  实现了 IActivityManager这个接口的所有方法
            // 代理对象实现了IActivityManager接口，所以系统是认识的
            // 把代理对象设置给系统,这样当系统使用该对象的时候，我们就可以在代理类中做相应的代码拦截
            // 即在我们实现了InvocationHandler接口的invoke方法中拦截
            iActivityManagerField.set(iActivityManagerSingleton, oldIActivityManager);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //  低版本的时候，使用这个方法
    public void hookStartActivity2(Context context) {
//        还原 gDefault 成员变量  反射  调用一次
        try {
            Class<?> ActivityManagerNativecls = Class.forName("android.app.ActivityManagerNative");
            Field gDefault = ActivityManagerNativecls.getDeclaredField("gDefault");
            gDefault.setAccessible(true);
//            因为是静态变量  所以获取的到的是系统值  hook   伪hook
            Object defaltValue = gDefault.get(null);
            //mInstance对象
            Class<?> SingletonClass = Class.forName("android.util.Singleton");

            Field mInstance = SingletonClass.getDeclaredField("mInstance");
//        还原 IactivityManager对象  系统对象
            mInstance.setAccessible(true);
            Object iActivityManagerObject = mInstance.get(defaltValue);
            Class<?> IActivityManagerIntercept = Class.forName("android.app.IActivityManager");
            StartActivity startActivtyMethod = new StartActivity(iActivityManagerObject, context);
//            第二参数  是即将返回的对象 需要实现那些接口
            Object oldIactivityManager = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader()
                    , new Class[]{IActivityManagerIntercept, View.OnClickListener.class}
                    , startActivtyMethod);
//            将系统的iActivityManager  替换成    自己通过动态代理实现的对象   oldIactivityManager对象  实现了 IActivityManager这个接口的所有方法
            mInstance.set(defaltValue, oldIactivityManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 在代理Activity通过了系统AMS的检查之后，回到当前的App启动对应的Activity的时候，再进行一次Hook操作。
     * 把要启动的Activity替换成我们真正要启动的Activity，还可以对一些代码控制，如页面做登陆前检查等。
     */
    public void hookHookMh(Context context) {
        try {
            // 1.反射得到ActivityThread的静态变量：sCurrentActivityThread
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Field sCurrentActivityThreadField = activityThreadClass.getDeclaredField("sCurrentActivityThread");
            sCurrentActivityThreadField.setAccessible(true);
            // 静态变量，直接传null得到对应字段的值
            Object sCurrentActivityThread = sCurrentActivityThreadField.get(null);

            // 2.反射得到ActivityThread的字段：mH,mH是一个handler对象
            Field mHField = activityThreadClass.getDeclaredField("mH");
            mHField.setAccessible(true);
            Handler mHInstance = (Handler) mHField.get(sCurrentActivityThread);

            // 3.反射Handler，给我们的mH对象设置一个CallBack接口，方便外部处理启动逻辑,如替换真正的Activity
            Field mCallbackField = Handler.class.getDeclaredField("mCallback");
            mCallbackField.setAccessible(true);
            mCallbackField.set(mHInstance, new HandlerCallback(mHInstance, context));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 代理实现类
     */
    class StartActivity implements InvocationHandler {

        private Object mIActivityManager;

        private Context mContext;

        public StartActivity(Object IActivityManager, Context context) {
            mIActivityManager = IActivityManager;
            mContext = context;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Log.i("INFO", "invoke    " + method.getName());
            if ("startActivity".equals(method.getName())) {
                Log.i("INFO", "-----------------startActivity--------------------------");
                // 瞒天过海
                // 寻找传进来的intent
                Intent intent = null;
                int index = 0;
                // startActivity 方法有很多参数，遍历找到我们需要的参数
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    if (arg instanceof Intent) {
                        intent = (Intent) arg;
                        index = i;
                    }
                }

                // 目的  ---载入acgtivity  将它还原
                // 这里把跳转的Activity做一个替换，目的是使没有在Manifest文件中注册的Activity，
                // 跳过AMS的检查，即先使用一个ProxyActivity，去接受系统的检查，当系统检查通过后，
                // 在将要启动ProxyActivity的时候，又替换回我们真实要启动的Activity(这是另外一个Hook点,看hookHookMh()方法)，
                // 这样即使没有在Manifest文件中注册的Activity也可以正常启动了
                Intent newIntent = new Intent();
                ComponentName componentName = new ComponentName(mContext, ProxyActivity.class);
                newIntent.setComponent(componentName);
                // 真实的意图 被我隐藏到了  键值对
                // 真正需要跳转的意图，存放起来
                newIntent.putExtra("oldIntent", intent);
                // 使用newIntent替换原来的意图,这样就会去启动ProxyActivity了，
                // 即ProxyActivity接受系统AMS的检查,(ProxyActivity必须在Manifest文件中注册)
                args[index] = newIntent;
            }
            return method.invoke(mIActivityManager, args);
        }
    }

    /**
     * 在代理Activity通过系统检查之后，回到我们的应用启动对应的Activity的时候，会给ActivityThread的mH（Handler）
     * 发送消息，Handler可以设置一个CallBack来处理消息，利用这一特性，我们可以拦截系统代码，在真正启动Activity的
     * 时候，做一个替换，即替换成我们真正要启动的Activity
     */
    class HandlerCallback implements Handler.Callback {

        private Handler mHandler;

        private Context mContext;

        public HandlerCallback(Handler handler, Context context) {
            mHandler = handler;
            mContext = context;
        }

        @Override
        public boolean handleMessage(Message msg) {
            // 拦截启动Activity的信号
            // LAUNCH_ACTIVITY ==100 即将要加载一个activity了
            if (msg.what == 100) {
                // 加工 --完  一定丢给系统  secondActivity  -hook->proxyActivity---hook->secondeActivtiy
                handleLaunchActivity(msg);
            }
            // 做了真正的跳转
            mHandler.handleMessage(msg);
            return true;
        }

        /**
         * 还原真正需要启动的Activity
         */
        private void handleLaunchActivity(Message msg) {
            // 拿到Message持有的obj对象
            Object obj = msg.obj;
            try {
                // 1.拿到ActivityClientRecord的Intent字段
                Field intentField = obj.getClass().getDeclaredField("intent");
                intentField.setAccessible(true);
                Intent realIntent = (Intent) intentField.get(obj);

                // 2.得到我们存放的真实的意图
                Intent oldIntent = realIntent.getParcelableExtra("oldIntent");
                if (oldIntent != null) {
                    // 在这里我们可以做集中式登陆
                    // 在这里我们还可以做一些免登陆白名单，让一些不需要登陆态的页面可以直接跳转
                    if (isUserLogin(mContext)) {
                        // 如果用户已经登陆了，直接跳转到真正需要启动的页面
                        // 把原有的意图    放到realyIntent
                        realIntent.setComponent(oldIntent.getComponent());
                    } else {
                        // 如果用户没有登陆，则跳转到登陆页面（对于需要登陆态的页面来说）
                        ComponentName componentName = new ComponentName(mContext, LoginActivity.class);
                        realIntent.setComponent(componentName);
                        // 真正需要跳转的意图，则还保存在realIntent的extraIntent中，在
                        // LoginActivity登陆成功之后，取出真实意图跳转
                        realIntent.putExtra("extraIntent", oldIntent);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 用户是否登陆
     */
    private boolean isUserLogin(Context context) {
        SharedPreferences share = context.getSharedPreferences("david",
                Context.MODE_PRIVATE);
        return share.getBoolean("login", false);
    }

    /**
     * 初始化插件的dex文件，并与宿主app的dex文件进行合并,组成新的dex数组
     * 注入插件的dex文件
     */
    public void injectPluginDex(Context context) {
        // 插件下载的存储的位置
        String apkPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/plugin.apk";
        // 插件被ClassLoader缓存的路径
        String cachePath = context.getCacheDir().getAbsolutePath();
        // 系统使用的ClassLoader
        ClassLoader appClassLoader = context.getClassLoader();
        // 插件使用的ClassLoader
        DexClassLoader pluginClassLoader = new DexClassLoader(apkPath, cachePath, cachePath, appClassLoader);

        try {
            // 1.拿到插件ClassLoader的pathList对象
            Class<?> pluginBaseClassLoaderClass = Class.forName("dalvik.system.BaseDexClassLoader");
            Field pluginPathListField = pluginBaseClassLoaderClass.getDeclaredField("pathList");
            pluginPathListField.setAccessible(true);
            Object pluginPathList = pluginPathListField.get(pluginClassLoader);

            // 2.拿到插件存放dex文件的数组对象：dexElements
            Field pluginDexElementsField = pluginPathList.getClass().getDeclaredField("dexElements");
            pluginDexElementsField.setAccessible(true);
            // 得到插件的dexElements对象,是一个Element数组
            Object pluginDexElements = pluginDexElementsField.get(pluginPathList);

            // 3.拿到宿主app的pathList对象
            Class<?> appClassLoaderClass = Class.forName("dalvik.system.BaseDexClassLoader");
            Field appPathListField = appClassLoaderClass.getDeclaredField("pathList");
            appPathListField.setAccessible(true);
            Object appPathList = appPathListField.get(appClassLoader);

            // 4.拿到宿主app 存放dex文件的数组对象：dexElements
            Field appDexElementsField = appPathList.getClass().getDeclaredField("dexElements");
            appDexElementsField.setAccessible(true);
            Object appDexElements = appDexElementsField.get(appPathList);

            // 5.合并宿主app和插件的dexElements数组
            // 插件dexElements数组的长度
            int pluginDexLength = Array.getLength(pluginDexElements);
            // 宿主app的dexElements数组的长度
            int appDexLength = Array.getLength(appDexElements);
            // 宿主app的dexElements数组与插件dexElements数组合并之后的总长度
            int length = pluginDexLength + appDexLength;
            // 获取dexElements数组元素的Class类型
            Class dexElementsItemType = appDexElements.getClass().getComponentType();

            // 根据数组长度和元素类型，初始化一个新的dexElements数组，用于合并宿主app和插件的dexElements数组
            Object newDexElements = Array.newInstance(dexElementsItemType, length);
            // 合并宿主app和插件的dexElements数组
            for (int i = 0; i < length; i++) {
                if (i < pluginDexLength) {
                    // 把插件的dexElements的元素取出，放入到新的dexElements数组中
                    Array.set(newDexElements, i, Array.get(pluginDexElements, i));
                } else {
                    // 把宿主app的dexElements的元素取出，放入到新的dexElements数组中
                    Array.set(newDexElements, i, Array.get(appDexElements, i - pluginDexLength));
                }
            }

            // 6.把合并后的dexElements数组，设置给宿主app的pathList对象
            appDexElementsField.set(appPathList, newDexElements);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
