package top.niunaijun.blackbox.core;


import android.os.Process;
import android.util.Log;

import androidx.annotation.Keep;

import java.io.File;
import java.util.List;

import dalvik.system.DexFile;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.utils.compat.DexFileCompat;

import top.niunaijun.blackbox.core.system.JarManager;
import top.niunaijun.blackbox.utils.Slog;


public class NativeCore {
    public static final String TAG = "NativeCore";

    static {
        boolean loaded = false;
        try {
            System.loadLibrary("blackbox");
            Slog.d(TAG, "Successfully loaded libblackbox.so via System.loadLibrary");
            loaded = true;
        } catch (Throwable e) {
            Slog.e(TAG, "System.loadLibrary blackbox failed: " + e.getMessage());
        }

        if (!loaded) {
            try {
                if (BlackBoxCore.getContext() != null && BlackBoxCore.getContext().getApplicationInfo() != null) {
                    File libFile = new File(BlackBoxCore.getContext().getApplicationInfo().nativeLibraryDir, "libblackbox.so");
                    if (libFile.exists()) {
                        System.load(libFile.getAbsolutePath());
                        Slog.d(TAG, "Successfully loaded libblackbox.so from nativeLibraryDir: " + libFile.getAbsolutePath());
                        loaded = true;
                    } else {
                        File parentLibDir = new File(BlackBoxCore.getContext().getApplicationInfo().dataDir, "lib");
                        File altLib = new File(parentLibDir, "libblackbox.so");
                        if (altLib.exists()) {
                            System.load(altLib.getAbsolutePath());
                            Slog.d(TAG, "Successfully loaded libblackbox.so from altLib: " + altLib.getAbsolutePath());
                            loaded = true;
                        }
                    }
                }
            } catch (Throwable t) {
                Slog.e(TAG, "Fallback load from nativeLibraryDir failed: " + t.getMessage());
            }
        }

        if (!loaded) {
            try {
                if (BlackBoxCore.getContext() != null && BlackBoxCore.getContext().getApplicationInfo() != null) {
                    String apkPath = BlackBoxCore.getContext().getApplicationInfo().sourceDir;
                    File extractedLib = extractLibFromApk(apkPath, BlackBoxCore.getContext());
                    if (extractedLib != null && extractedLib.exists()) {
                        System.load(extractedLib.getAbsolutePath());
                        Slog.d(TAG, "Successfully loaded libblackbox.so extracted from APK: " + extractedLib.getAbsolutePath());
                        loaded = true;
                    }
                }
            } catch (Throwable t) {
                Slog.e(TAG, "Extraction of libblackbox.so from APK failed: " + t.getMessage());
            }
        }
    }

    private static File extractLibFromApk(String apkPath, android.content.Context context) {
        if (apkPath == null || !(new File(apkPath).exists())) return null;
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(apkPath)) {
            String[] abis = android.os.Build.SUPPORTED_ABIS;
            java.util.zip.ZipEntry targetEntry = null;
            for (String abi : abis) {
                java.util.zip.ZipEntry entry = zip.getEntry("lib/" + abi + "/libblackbox.so");
                if (entry != null) {
                    targetEntry = entry;
                    break;
                }
            }
            if (targetEntry == null) return null;
            File outFile = new File(context.getCacheDir(), "libblackbox.so");
            try (java.io.InputStream is = zip.getInputStream(targetEntry);
                 java.io.FileOutputStream os = new java.io.FileOutputStream(outFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = is.read(buf)) > 0) {
                    os.write(buf, 0, len);
                }
            }
            return outFile;
        } catch (Exception e) {
            Slog.e(TAG, "Error extracting libblackbox.so from APK: " + e.getMessage());
            return null;
        }
    }

    public static native void init(int apiLevel);

    public static native void enableIO();

    public static native void addIORule(String targetPath, String relocatePath);

    public static native void hideXposed();

    public static native boolean disableHiddenApi();
    
    public static native boolean disableResourceLoading();


    @Keep
    public static int getCallingUid(int origCallingUid) {
        try {
            
            if (origCallingUid > 0 && origCallingUid < Process.FIRST_APPLICATION_UID)
                return origCallingUid;
            
            if (origCallingUid > Process.LAST_APPLICATION_UID)
                return origCallingUid;

            if (origCallingUid == BlackBoxCore.getHostUid()) {
                
                String appPackageName = BlackBoxCore.getAppPackageName();
                if (appPackageName != null && appPackageName.equals("com.google.android.gms")){
                    
                }
                
                
                
                if (appPackageName != null && appPackageName.equals("com.google.android.webview")){
                    return Process.myUid();
                }
                
                
                try {
                    int callingBUid = BlackBoxCore.getCallingBUid();
                    if (callingBUid > 0 && callingBUid < Process.LAST_APPLICATION_UID) {
                        return callingBUid;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error getting calling BUid: " + e.getMessage());
                }
                
                
                try {
                    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                    for (StackTraceElement element : stackTrace) {
                        String className = element.getClassName();
                        String methodName = element.getMethodName();
                        
                        
                        if ((className.contains("Settings") || className.contains("FeatureFlag")) &&
                            (methodName.contains("getString") || methodName.contains("getInt") || 
                             methodName.contains("getLong") || methodName.contains("getFloat"))) {
                            
                            Log.d(TAG, "System settings access detected, using system UID to prevent mismatch");
                            
                            return Process.SYSTEM_UID; 
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error analyzing stack trace for UID resolution: " + e.getMessage());
                }
                
                
                return BlackBoxCore.getHostUid();
            }
            return origCallingUid;
        } catch (Exception e) {
            Log.e(TAG, "Error in getCallingUid: " + e.getMessage());
            
            return Process.myUid();
        }
    }

    @Keep
    public static String redirectPath(String path) {
        return IOCore.get().redirectPath(path);
    }

    @Keep
    public static File redirectPath(File path) {
        return IOCore.get().redirectPath(path);
    }

    @Keep
    public static long[] loadEmptyDex() {
        try {
            File emptyJar = JarManager.getInstance().getEmptyJar();
            if (emptyJar == null) {
                Log.w(TAG, "Empty JAR not available, attempting sync initialization");
                JarManager.getInstance().initializeSync();
                emptyJar = JarManager.getInstance().getEmptyJar();
            }
            
            if (emptyJar == null || !emptyJar.exists()) {
                Log.e(TAG, "Empty JAR file not found or invalid");
                return new long[]{};
            }
            
            DexFile dexFile = new DexFile(emptyJar);
            List<Long> cookies = DexFileCompat.getCookies(dexFile);
            long[] longs = new long[cookies.size()];
            for (int i = 0; i < cookies.size(); i++) {
                longs[i] = cookies.get(i);
            }
            Log.d(TAG, "Successfully loaded empty DEX with " + cookies.size() + " cookies");
            return longs;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load empty DEX", e);
        }
        return new long[]{};
    }
    
    
    private static long[] createFallbackEmptyDex() {
        try {
            Slog.d(TAG, "Creating fallback empty DEX");
            
            
            
            byte[] emptyDexBytes = createMinimalDexBytes();
            
            
            File tempDexFile = File.createTempFile("fallback_empty", ".dex");
            tempDexFile.deleteOnExit();
            
            java.io.FileOutputStream fos = new java.io.FileOutputStream(tempDexFile);
            fos.write(emptyDexBytes);
            fos.close();
            
            
            DexFile dexFile = new DexFile(tempDexFile);
            List<Long> cookies = DexFileCompat.getCookies(dexFile);
            
            if (cookies != null && !cookies.isEmpty()) {
                long[] longs = new long[cookies.size()];
                for (int i = 0; i < cookies.size(); i++) {
                    longs[i] = cookies.get(i);
                }
                
                Slog.d(TAG, "Successfully created fallback empty DEX with " + cookies.size() + " cookies");
                return longs;
            }
            
        } catch (Exception e) {
            Slog.e(TAG, "Error creating fallback empty DEX: " + e.getMessage());
        }
        
        
        Slog.w(TAG, "Returning empty DEX array as last resort");
        return new long[]{};
    }
    
    
    private static byte[] createMinimalDexBytes() {
        
        
        
        
        
        byte[] dexHeader = {
            'd', 'e', 'x', '\n',  
            0x30, 0x33, 0x35, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x70, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00,  
            0x00, 0x00, 0x00, 0x00   
        };
        
        return dexHeader;
    }
}
