package top.niunaijun.blackbox.fake.hook;

import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import top.niunaijun.blackbox.utils.MethodParameterUtils;


public abstract class ClassInvocationStub implements InvocationHandler, IInjectHook {
    public static final String TAG = ClassInvocationStub.class.getSimpleName();

    private final Map<String, MethodHook> mMethodHookMap = new HashMap<>();
    private Object mBase;
    private Object mProxyInvocation;
    private boolean onlyProxy;

    protected abstract Object getWho();

    protected abstract void inject(Object baseInvocation, Object proxyInvocation);

    protected void onBindMethod() {

    }

    protected Object getProxyInvocation() {
        return mProxyInvocation;
    }

    protected Object getBase() {
        return mBase;
    }

    protected void onlyProxy(boolean o) {
        onlyProxy = o;
    }

    @Override
    public void injectHook() {
        mBase = getWho();
        
        if (mBase == null) {
            return;
        }
        mProxyInvocation = Proxy.newProxyInstance(mBase.getClass().getClassLoader(), MethodParameterUtils.getAllInterface(mBase.getClass()), this);
        if (!onlyProxy) {
            inject(mBase, mProxyInvocation);
        }

        onBindMethod();
        Class<?>[] declaredClasses = this.getClass().getDeclaredClasses();
        for (Class<?> declaredClass : declaredClasses) {
            initAnnotation(declaredClass);
        }
        ScanClass scanClass = this.getClass().getAnnotation(ScanClass.class);
        if (scanClass != null) {
            for (Class<?> aClass : scanClass.value()) {
                for (Class<?> declaredClass : aClass.getDeclaredClasses()) {
                    initAnnotation(declaredClass);
                }
            }
        }
    }

    protected void initAnnotation(Class<?> clazz) {
        ProxyMethod proxyMethod = clazz.getAnnotation(ProxyMethod.class);
        if (proxyMethod != null) {
            final String name = proxyMethod.value();
            if (!TextUtils.isEmpty(name)) {
                try {
                    addMethodHook(name, (MethodHook) clazz.newInstance());
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
        ProxyMethods proxyMethods = clazz.getAnnotation(ProxyMethods.class);
        if (proxyMethods != null) {
            String[] value = proxyMethods.value();
            for (String name : value) {
                try {
                    addMethodHook(name, (MethodHook) clazz.newInstance());
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }

    protected void addMethodHook(MethodHook methodHook) {
        mMethodHookMap.put(methodHook.getMethodName(), methodHook);
    }

    protected void addMethodHook(String name, MethodHook methodHook) {
        mMethodHookMap.put(name, methodHook);
    }

    public static Object getSafeDefaultValue(Class<?> returnType) {
        if (returnType == void.class || returnType == Void.class) {
            return null;
        }
        if (returnType == boolean.class || returnType == Boolean.class) {
            return false;
        }
        if (returnType == int.class || returnType == Integer.class) {
            return 0;
        }
        if (returnType == long.class || returnType == Long.class) {
            return 0L;
        }
        if (returnType == float.class || returnType == Float.class) {
            return 0f;
        }
        if (returnType == double.class || returnType == Double.class) {
            return 0.0;
        }
        if (returnType == short.class || returnType == Short.class) {
            return (short) 0;
        }
        if (returnType == byte.class || returnType == Byte.class) {
            return (byte) 0;
        }
        if (returnType == char.class || returnType == Character.class) {
            return (char) 0;
        }
        return null;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MethodHook methodHook = mMethodHookMap.get(method.getName());
        if (methodHook == null || !methodHook.isEnable()) {
            try {
                if (mBase == null) {
                    return getSafeDefaultValue(method.getReturnType());
                }
                return method.invoke(mBase, args);
            } catch (Throwable e) {
                Throwable cause = e.getCause();
                if (cause instanceof android.os.RemoteException || cause instanceof SecurityException || cause instanceof android.os.DeadObjectException) {
                    Log.w(TAG, "Safely handled IPC exception in unhooked method " + method.getName() + ": " + (cause != null ? cause.getMessage() : e.getMessage()));
                    return getSafeDefaultValue(method.getReturnType());
                }
                if (cause != null) {
                    throw cause;
                }
                throw e;
            }
        }

        try {
            Object result = methodHook.beforeHook(mBase, method, args);
            if (result != null) {
                return result;
            }
            result = methodHook.hook(mBase, method, args);
            result = methodHook.afterHook(result);
            return result;
        } catch (Throwable e) {
            Throwable cause = e.getCause();
            if (cause instanceof android.os.RemoteException || cause instanceof SecurityException || cause instanceof android.os.DeadObjectException) {
                Log.w(TAG, "Safely handled IPC exception in hooked method " + method.getName() + ": " + (cause != null ? cause.getMessage() : e.getMessage()));
                return getSafeDefaultValue(method.getReturnType());
            }
            if (cause != null) {
                throw cause;
            }
            throw e;
        }
    }
}
