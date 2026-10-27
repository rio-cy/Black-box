package top.niunaijun.blackbox.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class StoragePermissionHelper {
    
    private static final String TAG = "StoragePermissionHelper";
    
    public static final int REQUEST_CODE_STORAGE_PERMISSION = 1001;
    public static final int REQUEST_CODE_MANAGE_STORAGE = 1002;
    
    
    public static boolean hasAllFilesAccess() {
        return true;
    }

    public static boolean hasStoragePermission(Context context) {
        return true;
    }

    public static boolean hasFullFileAccess(Context context) {
        return true;
    }

    public static void requestAllFilesAccess(Activity activity) {
        // SAF & Private isolated storage model: no broad storage permission required
    }

    public static void requestAllFilesAccessForPackage(Activity activity, String packageName) {
        // SAF & Private isolated storage model: no broad storage permission required
    }

    public static void requestStoragePermission(Activity activity) {
        // SAF & Private isolated storage model: no broad storage permission required
    }

    public static void requestFullFileAccess(Activity activity) {
        // SAF & Private isolated storage model: no broad storage permission required
    }

    public static boolean shouldShowStorageRationale(Activity activity) {
        return false;
    }
    
    
    public static String getPermissionRationale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return "This app needs access to all files to properly manage sandboxed applications. " +
                   "Please grant 'All files access' permission in the settings.";
        } else {
            return "This app needs storage permission to properly manage sandboxed applications. " +
                   "Please grant storage permission.";
        }
    }
    
    
    public static boolean handlePermissionResult(Activity activity, int requestCode, 
            String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    Slog.d(TAG, "Storage permission granted");
                    return true;
                }
            }
            Slog.w(TAG, "Storage permission denied");
            return false;
        }
        return false;
    }
    
    
    public static boolean handleAllFilesAccessResult(int requestCode) {
        if (requestCode == REQUEST_CODE_MANAGE_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                boolean granted = Environment.isExternalStorageManager();
                if (granted) {
                    Slog.d(TAG, "All files access granted");
                } else {
                    Slog.w(TAG, "All files access denied");
                }
                return granted;
            }
        }
        return false;
    }
}
