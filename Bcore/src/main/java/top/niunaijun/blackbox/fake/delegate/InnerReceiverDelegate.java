package top.niunaijun.blackbox.fake.delegate;

import android.content.IIntentReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import black.android.app.BRLoadedApkReceiverDispatcherInnerReceiver;
import black.android.content.BRIIntentReceiver;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.proxy.record.ProxyBroadcastRecord;
import top.niunaijun.blackbox.utils.Reflector;


public class InnerReceiverDelegate extends IIntentReceiver.Stub {
    public static final String TAG = "InnerReceiverDelegate";

    private static final Map<IBinder, InnerReceiverDelegate> sInnerReceiverDelegate = new HashMap<>();
    private final IIntentReceiver mIntentReceiver;
    private final Object mDispatcher;

    private InnerReceiverDelegate(IIntentReceiver iIntentReceiver, Object dispatcher) {
        this.mIntentReceiver = iIntentReceiver;
        this.mDispatcher = dispatcher;
    }

    public static InnerReceiverDelegate getDelegate(IBinder iBinder) {
        return sInnerReceiverDelegate.get(iBinder);
    }

    public static IIntentReceiver createProxy(IIntentReceiver base) {
        return createProxy(base, null);
    }

    public static IIntentReceiver createProxy(IIntentReceiver base, Object dispatcher) {
        if (base instanceof InnerReceiverDelegate) {
            return base;
        }
        if (base == null) {
            return null;
        }
        final IBinder iBinder = base.asBinder();
        InnerReceiverDelegate delegate = sInnerReceiverDelegate.get(iBinder);
        if (delegate == null) {
            try {
                iBinder.linkToDeath(new DeathRecipient() {
                    @Override
                    public void binderDied() {
                        sInnerReceiverDelegate.remove(iBinder);
                        try {
                            iBinder.unlinkToDeath(this, 0);
                        } catch (Exception ignored) {
                        }
                    }
                }, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            delegate = new InnerReceiverDelegate(base, dispatcher);
            sInnerReceiverDelegate.put(iBinder, delegate);
        }
        return delegate;
    }

    @Override
    public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) throws RemoteException {
        if (intent == null) return;
        try {
            if (BlackBoxCore.getApplication() != null) {
                intent.setExtrasClassLoader(BlackBoxCore.getApplication().getClassLoader());
            }
        } catch (Throwable ignored) {
        }

        ProxyBroadcastRecord proxyBroadcastRecord = ProxyBroadcastRecord.create(intent);
        Intent perIntent;
        if (proxyBroadcastRecord != null && proxyBroadcastRecord.mIntent != null) {
            try {
                if (BlackBoxCore.getApplication() != null) {
                    proxyBroadcastRecord.mIntent.setExtrasClassLoader(BlackBoxCore.getApplication().getClassLoader());
                }
            } catch (Throwable ignored) {
            }
            perIntent = proxyBroadcastRecord.mIntent;
        } else {
            perIntent = intent;
        }

        if (mDispatcher != null && mIntentReceiver != null) {
            try {
                BRLoadedApkReceiverDispatcherInnerReceiver.get(mIntentReceiver)._set_mDispatcher(new WeakReference<>(mDispatcher));
            } catch (Throwable ignored) {
            }
        }

        if (mIntentReceiver != null) {
            try {
                BRIIntentReceiver.get(mIntentReceiver).performReceive(perIntent, resultCode, data, extras, ordered, sticky, sendingUser);
                return;
            } catch (Throwable e) {
                if (mDispatcher != null) {
                    try {
                        Reflector.QuietReflector.with(mDispatcher)
                                .method("performReceive", Intent.class, int.class, String.class, Bundle.class, boolean.class, boolean.class, int.class)
                                .call(perIntent, resultCode, data, extras, ordered, sticky, sendingUser);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
    }
}
