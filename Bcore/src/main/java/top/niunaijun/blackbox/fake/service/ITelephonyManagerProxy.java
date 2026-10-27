package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import black.com.android.internal.telephony.BRITelephonyStub;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Md5Utils;

public class ITelephonyManagerProxy extends BinderInvocationStub {
    public static final String TAG = "ITelephonyManagerProxy";

    public ITelephonyManagerProxy() {
        super(BRServiceManager.get().getService(Context.TELEPHONY_SERVICE));
    }

    @Override
    protected Object getWho() {
        IBinder telephony = BRServiceManager.get().getService(Context.TELEPHONY_SERVICE);
        return BRITelephonyStub.get().asInterface(telephony);
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("getDeviceId")
    public static class GetDeviceId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int userId = BActivityThread.getUserId();
            if (DeviceSpoofHelper.isSpoofingEnabled(userId)) {
                return DeviceSpoofHelper.getSpoofedImei(userId);
            }
            return Md5Utils.md5(BlackBoxCore.getHostPkg());
        }
    }

    @ProxyMethod("getImeiForSlot")
    public static class getImeiForSlot extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int userId = BActivityThread.getUserId();
            if (DeviceSpoofHelper.isSpoofingEnabled(userId)) {
                return DeviceSpoofHelper.getSpoofedImei(userId);
            }
            return Md5Utils.md5(BlackBoxCore.getHostPkg());
        }
    }

    @ProxyMethod("getMeidForSlot")
    public static class GetMeidForSlot extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int userId = BActivityThread.getUserId();
            if (DeviceSpoofHelper.isSpoofingEnabled(userId)) {
                String imei = DeviceSpoofHelper.getSpoofedImei(userId);
                return imei.length() >= 14 ? imei.substring(0, 14) : imei;
            }
            return Md5Utils.md5(BlackBoxCore.getHostPkg());
        }
    }

    @ProxyMethod("getNetworkOperator")
    public static class GetNetworkOperator extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int userId = BActivityThread.getUserId();
            if (DeviceSpoofHelper.isSpoofingEnabled(userId)) {
                return DeviceSpoofHelper.getSimOperator(userId);
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable e) {
                return "405854"; // Default Jio fallback
            }
        }
    }

    @ProxyMethod("getNetworkOperatorName")
    public static class GetNetworkOperatorName extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int userId = BActivityThread.getUserId();
            if (DeviceSpoofHelper.isSpoofingEnabled(userId)) {
                return DeviceSpoofHelper.getSimOperatorName(userId);
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable e) {
                return "Jio";
            }
        }
    }

    @ProxyMethod("getNetworkOperatorForPhoneId")
    public static class GetNetworkOperatorForPhoneId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int userId = BActivityThread.getUserId();
            if (DeviceSpoofHelper.isSpoofingEnabled(userId)) {
                return DeviceSpoofHelper.getSimOperator(userId);
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable e) {
                return "405854";
            }
        }
    }

    @ProxyMethod("getNetworkOperatorNameForPhoneId")
    public static class GetNetworkOperatorNameForPhoneId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int userId = BActivityThread.getUserId();
            if (DeviceSpoofHelper.isSpoofingEnabled(userId)) {
                return DeviceSpoofHelper.getSimOperatorName(userId);
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable e) {
                return "Jio";
            }
        }
    }

    @ProxyMethod("getSimOperator")
    public static class GetSimOperator extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int userId = BActivityThread.getUserId();
            if (DeviceSpoofHelper.isSpoofingEnabled(userId)) {
                return DeviceSpoofHelper.getSimOperator(userId);
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable e) {
                return "405854";
            }
        }
    }

    @ProxyMethod("getSimOperatorName")
    public static class GetSimOperatorName extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int userId = BActivityThread.getUserId();
            if (DeviceSpoofHelper.isSpoofingEnabled(userId)) {
                return DeviceSpoofHelper.getSimOperatorName(userId);
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable e) {
                return "Jio";
            }
        }
    }

    @ProxyMethod("getSimOperatorForPhoneId")
    public static class GetSimOperatorForPhoneId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int userId = BActivityThread.getUserId();
            if (DeviceSpoofHelper.isSpoofingEnabled(userId)) {
                return DeviceSpoofHelper.getSimOperator(userId);
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable e) {
                return "405854";
            }
        }
    }

    @ProxyMethod("getSimOperatorNameForPhoneId")
    public static class GetSimOperatorNameForPhoneId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int userId = BActivityThread.getUserId();
            if (DeviceSpoofHelper.isSpoofingEnabled(userId)) {
                return DeviceSpoofHelper.getSimOperatorName(userId);
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable e) {
                return "Jio";
            }
        }
    }

    @ProxyMethod("getNetworkCountryIso")
    public static class GetNetworkCountryIso extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return "in";
        }
    }

    @ProxyMethod("getNetworkCountryIsoForPhoneId")
    public static class GetNetworkCountryIsoForPhoneId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return "in";
        }
    }

    @ProxyMethod("getSimCountryIso")
    public static class GetSimCountryIso extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return "in";
        }
    }

    @ProxyMethod("getSimCountryIsoForPhoneId")
    public static class GetSimCountryIsoForPhoneId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return "in";
        }
    }

    @ProxyMethod("getLine1Number")
    public static class GetLine1Number extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int userId = BActivityThread.getUserId();
            if (DeviceSpoofHelper.isSpoofingEnabled(userId)) {
                return DeviceSpoofHelper.getPhoneNumber(userId);
            }
            return null;
        }
    }

    @ProxyMethod("getLine1NumberForDisplay")
    public static class getLine1NumberForDisplay extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int userId = BActivityThread.getUserId();
            if (DeviceSpoofHelper.isSpoofingEnabled(userId)) {
                return DeviceSpoofHelper.getPhoneNumber(userId);
            }
            return null;
        }
    }

    @ProxyMethod("getSubscriberId")
    public static class GetSubscriberId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int userId = BActivityThread.getUserId();
            if (DeviceSpoofHelper.isSpoofingEnabled(userId)) {
                return DeviceSpoofHelper.getSubscriberId(userId);
            }
            return Md5Utils.md5(BlackBoxCore.getHostPkg());
        }
    }

    @ProxyMethod("getDeviceIdWithFeature")
    public static class GetDeviceIdWithFeature extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int userId = BActivityThread.getUserId();
            if (DeviceSpoofHelper.isSpoofingEnabled(userId)) {
                return DeviceSpoofHelper.getSpoofedImei(userId);
            }
            return Md5Utils.md5(BlackBoxCore.getHostPkg());
        }
    }

    @ProxyMethod("isUserDataEnabled")
    public static class IsUserDataEnabled extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return true;
        }
    }
}
