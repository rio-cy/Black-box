package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.os.IBinder;
import android.os.IInterface;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.MethodParameterUtils;
import top.niunaijun.blackbox.utils.Reflector;
import top.niunaijun.blackbox.utils.Slog;

public class ICameraServiceProxy extends BinderInvocationStub {
    public static final String TAG = "CameraServiceProxy";

    public ICameraServiceProxy() {
        super(getSystemCameraServiceBinder());
    }

    private static IBinder getSystemCameraServiceBinder() {
        IBinder binder = BRServiceManager.get().getService(Context.CAMERA_SERVICE);
        if (binder == null) {
            binder = BRServiceManager.get().getService("media.camera");
        }
        return binder;
    }

    @Override
    protected Object getWho() {
        IBinder binder = getSystemCameraServiceBinder();
        if (binder == null) {
            Slog.d(TAG, "Camera service binder not available in system ServiceManager");
            return null;
        }

        try {
            Object iface = null;
            try {
                iface = Reflector.on("android.hardware.ICameraService$Stub").call("asInterface", binder);
            } catch (Exception e1) {
                try {
                    iface = Reflector.on("android.hardware.ICameraService").call("asInterface", binder);
                } catch (Exception e2) {
                    try {
                        Class<?> stubClass = Class.forName("android.hardware.ICameraService$Stub");
                        Method asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
                        iface = asInterfaceMethod.invoke(null, binder);
                    } catch (Exception e3) {
                        Slog.d(TAG, "Reflection paths for ICameraService unavailable");
                        return null;
                    }
                }
            }

            if (iface != null) {
                Slog.d(TAG, "Successfully obtained ICameraService interface");
                return (IInterface) iface;
            } else {
                Slog.d(TAG, "Reflection for ICameraService returned null interface");
                return null;
            }
        } catch (Exception e) {
            Slog.d(TAG, "Failed to get ICameraService interface: " + e.getMessage());
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        if (proxyInvocation != null) {
            try {
                replaceSystemService(Context.CAMERA_SERVICE);
                replaceSystemService("media.camera");
            } catch (Throwable t) {
                Slog.d(TAG, "Could not replace camera service: " + t.getMessage());
            }
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if (methodName.contains("open") || methodName.contains("connect")) {
            Slog.d(TAG, "Camera service method called: " + methodName + " -> showing white screen");
            WhiteScreenCameraActivity.show(BlackBoxCore.getContext());
        }

        try {
            MethodParameterUtils.replaceFirstAppPkg(args);
            MethodParameterUtils.replaceLastUid(args);
            return super.invoke(proxy, method, args);
        } catch (Throwable t) {
            Slog.w(TAG, "CameraService exception intercepted (" + t.getMessage() + ") -> falling back to white screen camera");
            WhiteScreenCameraActivity.show(BlackBoxCore.getContext());
            if (method.getReturnType() == int.class) {
                return 0;
            } else if (method.getReturnType() == boolean.class) {
                return true;
            } else {
                return null;
            }
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("openCamera")
    public static class OpenCamera extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Camera: openCamera hooked - triggering white screen");
            WhiteScreenCameraActivity.show(BlackBoxCore.getContext());
            MethodParameterUtils.replaceFirstAppPkg(args);
            MethodParameterUtils.replaceLastUid(args);
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                Slog.w(TAG, "openCamera fallback due to missing camera hardware/permission: " + t.getMessage());
                return null;
            }
        }
    }

    @ProxyMethod("openCameraForUid")
    public static class OpenCameraForUid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Camera: openCameraForUid hooked - triggering white screen");
            WhiteScreenCameraActivity.show(BlackBoxCore.getContext());
            MethodParameterUtils.replaceFirstAppPkg(args);
            MethodParameterUtils.replaceLastUid(args);
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                Slog.w(TAG, "openCameraForUid fallback");
                return null;
            }
        }
    }

    @ProxyMethod("connect")
    public static class Connect extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Camera: connect hooked - triggering white screen");
            WhiteScreenCameraActivity.show(BlackBoxCore.getContext());
            MethodParameterUtils.replaceFirstAppPkg(args);
            MethodParameterUtils.replaceLastUid(args);
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                Slog.w(TAG, "connect fallback");
                return null;
            }
        }
    }

    @ProxyMethod("connectDevice")
    public static class ConnectDevice extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Camera: connectDevice hooked - triggering white screen");
            WhiteScreenCameraActivity.show(BlackBoxCore.getContext());
            MethodParameterUtils.replaceFirstAppPkg(args);
            MethodParameterUtils.replaceLastUid(args);
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                Slog.w(TAG, "connectDevice fallback");
                return null;
            }
        }
    }
}
