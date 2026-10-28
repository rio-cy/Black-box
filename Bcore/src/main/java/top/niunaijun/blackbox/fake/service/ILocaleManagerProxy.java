package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.os.IBinder;
import android.os.IInterface;
import android.os.LocaleList;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.MethodParameterUtils;
import top.niunaijun.blackbox.utils.Reflector;
import top.niunaijun.blackbox.utils.Slog;

public class ILocaleManagerProxy extends BinderInvocationStub {
    public static final String TAG = "LocaleManagerProxy";

    public ILocaleManagerProxy() {
        super(BRServiceManager.get().getService("locale"));
    }

    @Override
    protected Object getWho() {
        IBinder binder = BRServiceManager.get().getService("locale");
        if (binder == null) {
            Slog.e(TAG, "Failed to get locale service binder");
            return null;
        }

        try {
            Object iface = null;
            try {
                iface = Reflector.on("android.app.ILocaleManager$Stub").call("asInterface", binder);
            } catch (Exception e1) {
                try {
                    Class<?> stubClass = Class.forName("android.app.ILocaleManager$Stub");
                    Method asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
                    iface = asInterfaceMethod.invoke(null, binder);
                } catch (Exception e2) {
                    Slog.e(TAG, "Failed to reflect ILocaleManager interface", e2);
                    return null;
                }
            }

            if (iface != null) {
                return (IInterface) iface;
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to get ILocaleManager interface", e);
        }
        return null;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("locale");
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return super.invoke(proxy, method, args);
        } catch (Throwable t) {
            Slog.w(TAG, "Handled exception in ILocaleManager." + method.getName() + ": " + t.getMessage());
            if (method.getReturnType() == LocaleList.class) {
                return LocaleList.getEmptyLocaleList();
            } else if (method.getReturnType() == boolean.class) {
                return false;
            } else if (method.getReturnType() == int.class) {
                return 0;
            }
            return null;
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("getApplicationLocales")
    public static class GetApplicationLocales extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                if (args != null && args.length > 0 && args[0] instanceof String) {
                    String pkg = (String) args[0];
                    if (!pkg.equals(BlackBoxCore.getHostPkg())) {
                        Slog.d(TAG, "getApplicationLocales for virtual pkg: " + pkg + ", replacing with host");
                        args[0] = BlackBoxCore.getHostPkg();
                    }
                }
                return method.invoke(who, args);
            } catch (Throwable t) {
                Slog.w(TAG, "getApplicationLocales fallback: " + t.getMessage());
                return LocaleList.getEmptyLocaleList();
            }
        }
    }

    @ProxyMethod("setApplicationLocales")
    public static class SetApplicationLocales extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                if (args != null && args.length > 0 && args[0] instanceof String) {
                    args[0] = BlackBoxCore.getHostPkg();
                }
                return method.invoke(who, args);
            } catch (Throwable t) {
                Slog.w(TAG, "setApplicationLocales fallback: " + t.getMessage());
                return null;
            }
        }
    }
}
