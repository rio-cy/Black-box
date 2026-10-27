package top.niunaijun.blackbox.fake.service;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import black.android.app.ActivityThreadActivityClientRecordContext;
import black.android.app.BRActivityClient;
import black.android.app.BRActivityClientActivityClientControllerSingleton;
import black.android.app.BRActivityManagerNative;
import black.android.app.BRActivityThread;
import black.android.app.BRActivityThreadActivityClientRecord;
import black.android.app.BRActivityThreadCreateServiceData;
import black.android.app.BRActivityThreadH;
import black.android.app.BRIActivityManager;
import black.android.app.servertransaction.BRClientTransaction;
import black.android.app.servertransaction.BRLaunchActivityItem;
import black.android.app.servertransaction.LaunchActivityItemContext;
import black.android.os.BRHandler;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.fake.hook.IInjectHook;
import top.niunaijun.blackbox.proxy.ProxyManifest;
import top.niunaijun.blackbox.proxy.record.ProxyActivityRecord;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.compat.ActivityManagerCompat;
import top.niunaijun.blackbox.utils.compat.BuildCompat;



public class HCallbackProxy implements IInjectHook, Handler.Callback {
    public static final String TAG = "HCallbackStub";
    private Handler.Callback mOtherCallback;
    private AtomicBoolean mBeing = new AtomicBoolean(false);

    private Handler.Callback getHCallback() {
        return BRHandler.get(getH()).mCallback();
    }

    private Handler getH() {
        Object currentActivityThread = BlackBoxCore.mainThread();
        return BRActivityThread.get(currentActivityThread).mH();
    }

    @Override
    public void injectHook() {
        mOtherCallback = getHCallback();
        if (mOtherCallback != null && (mOtherCallback == this || mOtherCallback.getClass().getName().equals(this.getClass().getName()))) {
            mOtherCallback = null;
        }
        BRHandler.get(getH())._set_mCallback(this);
    }

    @Override
    public boolean isBadEnv() {
        Handler.Callback hCallback = getHCallback();
        return hCallback != null && hCallback != this;
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (!mBeing.getAndSet(true)) {
            try {
                if (BuildCompat.isPie()) {
                    if (msg.what == BRActivityThreadH.get().EXECUTE_TRANSACTION()) {
                        if (handleLaunchActivity(msg.obj)) {
                            getH().sendMessageAtFrontOfQueue(Message.obtain(msg));
                            return true;
                        }
                    }
                } else {
                    if (msg.what == BRActivityThreadH.get().LAUNCH_ACTIVITY()) {
                        if (handleLaunchActivity(msg.obj)) {
                            getH().sendMessageAtFrontOfQueue(Message.obtain(msg));
                            return true;
                        }
                    }
                }
                if (msg.what == BRActivityThreadH.get().CREATE_SERVICE()) {
                    return handleCreateService(msg.obj);
                }
                if (mOtherCallback != null) {
                    return mOtherCallback.handleMessage(msg);
                }
                return false;
            } finally {
                mBeing.set(false);
            }
        }
        return false;
    }

    private List<?> getTransactionItems(Object clientTransaction) {
        if (clientTransaction == null) {
            return null;
        }
        try {
            List<Object> callbacks = BRClientTransaction.get(clientTransaction).mActivityCallbacks();
            if (callbacks != null && !callbacks.isEmpty()) {
                return callbacks;
            }
        } catch (Throwable ignored) {
        }
        try {
            Method getItemsMethod = clientTransaction.getClass().getMethod("getTransactionItems");
            Object items = getItemsMethod.invoke(clientTransaction);
            if (items instanceof List) {
                return (List<?>) items;
            }
        } catch (Throwable ignored) {
        }
        try {
            Field field = clientTransaction.getClass().getDeclaredField("mTransactionItems");
            field.setAccessible(true);
            Object items = field.get(clientTransaction);
            if (items instanceof List) {
                return (List<?>) items;
            }
        } catch (Throwable ignored) {
        }
        try {
            Field field = clientTransaction.getClass().getDeclaredField("transactionItems");
            field.setAccessible(true);
            Object items = field.get(clientTransaction);
            if (items instanceof List) {
                return (List<?>) items;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Object getLaunchActivityItem(Object clientTransaction) {
        List<?> items = getTransactionItems(clientTransaction);
        if (items == null) {
            return null;
        }

        String targetClassName = null;
        try {
            Class<?> realClass = BRLaunchActivityItem.getRealClass();
            if (realClass != null) {
                targetClassName = realClass.getName();
            }
        } catch (Throwable ignored) {
        }

        for (Object obj : items) {
            if (obj != null) {
                String className = obj.getClass().getName();
                if ((targetClassName != null && targetClassName.equals(className))
                        || className.endsWith(".LaunchActivityItem")) {
                    return obj;
                }
            }
        }
        return null;
    }

    private IBinder extractTokenFromItem(Object item) {
        if (item == null) {
            return null;
        }
        try {
            Method method = item.getClass().getMethod("getActivityToken");
            Object res = method.invoke(item);
            if (res instanceof IBinder) {
                return (IBinder) res;
            }
        } catch (Throwable ignored) {
        }
        try {
            Field field = item.getClass().getDeclaredField("mActivityToken");
            field.setAccessible(true);
            Object res = field.get(item);
            if (res instanceof IBinder) {
                return (IBinder) res;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private boolean handleLaunchActivity(Object client) {
        Object r;
        if (BuildCompat.isPie()) {
            r = getLaunchActivityItem(client);
        } else {
            r = client;
        }
        if (r == null)
            return false;

        Intent intent;
        IBinder token;
        if (BuildCompat.isPie()) {
            intent = BRLaunchActivityItem.get(r).mIntent();
            IBinder transactionToken = null;
            try {
                transactionToken = BRClientTransaction.get(client).mActivityToken();
            } catch (Throwable ignored) {
            }
            if (transactionToken != null) {
                token = transactionToken;
            } else {
                token = extractTokenFromItem(r);
            }
        } else {
            ActivityThreadActivityClientRecordContext clientRecordContext = BRActivityThreadActivityClientRecord.get(r);
            intent = clientRecordContext.intent();
            token = clientRecordContext.token();
        }

        if (intent == null)
            return false;

        ProxyActivityRecord stubRecord = ProxyActivityRecord.create(intent);
        ActivityInfo activityInfo = stubRecord.mActivityInfo;
        if (activityInfo != null) {
            if (BActivityThread.getAppConfig() == null) {
                BlackBoxCore.getBActivityManager().restartProcess(activityInfo.packageName, activityInfo.processName, stubRecord.mUserId);

                Intent launchIntentForPackage = BlackBoxCore.getBPackageManager().getLaunchIntentForPackage(activityInfo.packageName, stubRecord.mUserId);
                intent.setExtrasClassLoader(this.getClass().getClassLoader());
                ProxyActivityRecord.saveStub(intent, launchIntentForPackage, stubRecord.mActivityInfo, stubRecord.mActivityRecord, stubRecord.mUserId);
                if (BuildCompat.isPie()) {
                    LaunchActivityItemContext launchActivityItemContext = BRLaunchActivityItem.get(r);
                    launchActivityItemContext._set_mIntent(intent);
                    launchActivityItemContext._set_mInfo(activityInfo);
                } else {
                    ActivityThreadActivityClientRecordContext clientRecordContext = BRActivityThreadActivityClientRecord.get(r);
                    clientRecordContext._set_intent(intent);
                    clientRecordContext._set_activityInfo(activityInfo);
                }
                return true;
            }
            
            if (!BActivityThread.currentActivityThread().isInit()) {
                BActivityThread.currentActivityThread().bindApplication(activityInfo.packageName,
                        activityInfo.processName);
                return true;
            }

            int taskId = ActivityManagerCompat.getTaskForActivity(token, false);
            if (token != null) {
                BlackBoxCore.getBActivityManager().onActivityCreated(taskId, token, stubRecord.mActivityRecord);
            }

            if (BuildCompat.isS()) {
                checkActivityClient();
            }

            if (BuildCompat.isTiramisu()) {
                LaunchActivityItemContext launchActivityItemContext = BRLaunchActivityItem.get(r);
                launchActivityItemContext._set_mIntent(stubRecord.mTarget);
                launchActivityItemContext._set_mInfo(activityInfo);
            } else if (BuildCompat.isS()) {
                Object record = BRActivityThread.get(BlackBoxCore.mainThread()).getLaunchingActivity(token);
                if (record != null) {
                    ActivityThreadActivityClientRecordContext clientRecordContext = BRActivityThreadActivityClientRecord.get(record);
                    clientRecordContext._set_intent(stubRecord.mTarget);
                    clientRecordContext._set_activityInfo(activityInfo);
                    clientRecordContext._set_packageInfo(BActivityThread.currentActivityThread().getPackageInfo());
                }
            } else if (BuildCompat.isPie()) {
                LaunchActivityItemContext launchActivityItemContext = BRLaunchActivityItem.get(r);
                launchActivityItemContext._set_mIntent(stubRecord.mTarget);
                launchActivityItemContext._set_mInfo(activityInfo);
            } else {
                ActivityThreadActivityClientRecordContext clientRecordContext = BRActivityThreadActivityClientRecord.get(r);
                clientRecordContext._set_intent(stubRecord.mTarget);
                clientRecordContext._set_activityInfo(activityInfo);
            }
        }
        return false;
    }

    private boolean handleCreateService(Object data) {
        if (BActivityThread.getAppConfig() != null) {
            String appPackageName = BActivityThread.getAppPackageName();
            assert appPackageName != null;

            ServiceInfo serviceInfo = BRActivityThreadCreateServiceData.get(data).info();
            if (!serviceInfo.name.equals(ProxyManifest.getProxyService(BActivityThread.getAppPid()))
                    && !serviceInfo.name.equals(ProxyManifest.getProxyJobService(BActivityThread.getAppPid()))) {
                Slog.d(TAG, "handleCreateService: " + data);
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(appPackageName, serviceInfo.name));
                BlackBoxCore.getBActivityManager().startService(intent, null, false, BActivityThread.getUserId());
                return true;
            }
        }
        return false;
    }

    private void checkActivityClient() {
        try {
            Object activityClientController = BRActivityClient.get().getActivityClientController();
            if (!(activityClientController instanceof Proxy)) {
                IActivityClientProxy iActivityClientProxy = new IActivityClientProxy(activityClientController);
                iActivityClientProxy.onlyProxy(true);
                iActivityClientProxy.injectHook();
                Object instance = BRActivityClient.get().getInstance();
                Object o = BRActivityClient.get(instance).INTERFACE_SINGLETON();
                BRActivityClientActivityClientControllerSingleton.get(o)._set_mKnownInstance(iActivityClientProxy.getProxyInvocation());
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
