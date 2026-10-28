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
import top.niunaijun.blackbox.utils.Reflector;
import top.niunaijun.blackbox.utils.Slog;

public class ICameraServiceProxy extends BinderInvocationStub {
    public static final String TAG = "CameraServiceProxy";

    public ICameraServiceProxy() {
        super(BRServiceManager.get().getService(Context.CAMERA_SERVICE));
    }

    @Override
    protected Object getWho() {
        IBinder binder = BRServiceManager.get().getService(Context.CAMERA_SERVICE);
        if (binder == null) {
            Slog.e(TAG, "Failed to get CAMERA_SERVICE binder");
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
                        Slog.e(TAG, "All reflection paths failed for ICameraService", e3);
                        return null;
                    }
                }
            }

            if (iface != null) {
                Slog.d(TAG, "Successfully obtained ICameraService interface");
                return (IInterface) iface;
            } else {
                Slog.e(TAG, "Reflection succeeded but returned null interface");
                return null;
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to get ICameraService interface", e);
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.CAMERA_SERVICE);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if (methodName.contains("open") || methodName.contains("connect")) {
            Slog.d(TAG, "Camera service method called: " + methodName + " -> showing white screen");
            WhiteScreenCameraActivity.show(BlackBoxCore.getContext());
        }

        try {
            return super.invoke(proxy, method, args);
        } catch (SecurityException e) {
            Slog.w(TAG, "CameraService SecurityException intercepted (no host camera permission): " + e.getMessage());
            WhiteScreenCameraActivity.show(BlackBoxCore.getContext());
            if (method.getReturnType() == int.class) {
                return 0;
            } else if (method.getReturnType() == boolean.class) {
                return true;
            } else {
                return null;
            }
        } catch (Exception e) {
            Slog.e(TAG, "CameraService exception in " + methodName, e);
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
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                Slog.w(TAG, "openCamera fallback due to missing camera hardware/permission");
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
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                Slog.w(TAG, "connectDevice fallback");
                return null;
            }
        }
    }
}
