package top.niunaijun.blackbox.utils.compat;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.app.BRActivity;
import black.android.app.BRActivityClient;
import black.android.app.BRActivityManagerNative;
import black.android.app.BRIActivityManager;
import black.android.app.BRIActivityManagerL;
import black.android.app.BRIActivityManagerN;
import top.niunaijun.blackbox.utils.Slog;

public class ActivityManagerCompat {
    private static final String TAG = "ActivityManagerCompat";
	
	public static final int SERVICE_DONE_EXECUTING_ANON = 0;
	
	public static final int SERVICE_DONE_EXECUTING_START = 1;
	
	public static final int SERVICE_DONE_EXECUTING_STOP = 2;

























	
	public static final int INTENT_SENDER_BROADCAST = 1;

	
	public static final int INTENT_SENDER_ACTIVITY = 2;

	
	public static final int INTENT_SENDER_ACTIVITY_RESULT = 3;

	
	public static final int INTENT_SENDER_SERVICE = 4;

	
	public static final int USER_OP_SUCCESS = 0;

	public static final int START_FLAG_DEBUG = 1<<1;
	public static final int START_FLAG_TRACK_ALLOCATION = 1<<2;
	public static final int START_FLAG_NATIVE_DEBUGGING = 1<<3;

	public static boolean finishActivity(IBinder token, int code, Intent data) {
		if (token == null || !token.isBinderAlive()) {
			return false;
		}
		if (BuildCompat.isS()) {
			try {
				Object activityClient = BRActivityClient.get().getInstance();
				if (activityClient != null) {
					Method finishMethod = activityClient.getClass().getMethod("finishActivity", IBinder.class, int.class, Intent.class, int.class);
					Object res = finishMethod.invoke(activityClient, token, code, data, 0);
					if (res instanceof Boolean) {
						return (Boolean) res;
					}
					return true;
				}
			} catch (Throwable t) {
				Slog.w(TAG, "Failed to finishActivity via ActivityClient: " + t.getMessage());
			}
			return false;
		}
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				return BRIActivityManagerN.get(BRActivityManagerNative.get().getDefault()).finishActivity(
						token, code, data, 0);
			} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				return BRIActivityManagerL.get(BRActivityManagerNative.get().getDefault()).finishActivity(
						token, code, data, false);
			}
		} catch (Throwable t) {
			Slog.w(TAG, "Failed to finishActivity via IActivityManager: " + t.getMessage());
		}
		return false;
	}

	public static int getTaskForActivity(IBinder token, boolean onlyRoot) {
		if (token == null || !token.isBinderAlive()) {
			return -1;
		}
		if (BuildCompat.isS()) {
			try {
				Object activityClient = BRActivityClient.get().getInstance();
				if (activityClient != null) {
					Method getTaskMethod = activityClient.getClass().getMethod("getTaskForActivity", IBinder.class, boolean.class);
					Object res = getTaskMethod.invoke(activityClient, token, onlyRoot);
					if (res instanceof Integer) {
						return (Integer) res;
					}
				}
			} catch (Throwable t) {
				Slog.w(TAG, "Failed to getTaskForActivity via ActivityClient: " + t.getMessage());
			}
			return -1;
		}
		try {
			return BRIActivityManager.get(BRActivityManagerNative.get().getDefault()).getTaskForActivity(token, onlyRoot);
		} catch (Throwable t) {
			Slog.w(TAG, "Failed to getTaskForActivity via IActivityManager: " + t.getMessage());
		}
		return -1;
	}

    public static void setActivityOrientation(Activity activity, int orientation) {
        try {
            activity.setRequestedOrientation(orientation);
        } catch (Throwable e) {
            e.printStackTrace();
            
            Activity parent = BRActivity.get(activity).mParent();
            while (parent != null) {
                Activity tmp = BRActivity.get(parent).mParent();
                if (tmp != null) {
                    parent = tmp;
                } else {
                    break;
                }
            }
            IBinder token = parent != null ? BRActivity.get(parent).mToken() : null;
            if (token != null) {
                if (BuildCompat.isS()) {
                    try {
                        Object activityClient = BRActivityClient.get().getInstance();
                        if (activityClient != null) {
                            Method setOrientMethod = activityClient.getClass().getMethod("setRequestedOrientation", IBinder.class, int.class);
                            setOrientMethod.invoke(activityClient, token, orientation);
                            return;
                        }
                    } catch (Throwable t) {
                        Slog.w(TAG, "Failed to setRequestedOrientation via ActivityClient", t);
                    }
                    return;
                }
                try {
                    BRIActivityManager.get(BRActivityManagerNative.get().getDefault()).setRequestedOrientation(token, orientation);
                } catch (Throwable ex) {
                    Slog.w(TAG, "Failed to setRequestedOrientation via IActivityManager", ex);
                }
            }
        }
    }
}
