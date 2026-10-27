package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.Random;

import top.niunaijun.blackbox.BlackBoxCore;

/**
 * Helper to retrieve, manage, and inject spoofed device hardware identities
 * (Build fields, Android ID, IMEI, Serial) and Indian SIM Operator Telephony profiles
 * into virtual client processes.
 */
public class DeviceSpoofHelper {
    public static final String TAG = "DeviceSpoofHelper";
    private static final String PREFS_NAME = "device_spoof_prefs";

    public static class DevicePreset {
        public final String brand;
        public final String model;
        public final String manufacturer;
        public final String device;
        public final String fingerprint;

        public DevicePreset(String brand, String model, String manufacturer, String device, String fingerprint) {
            this.brand = brand;
            this.model = model;
            this.manufacturer = manufacturer;
            this.device = device;
            this.fingerprint = fingerprint;
        }
    }

    public static class IndianCarrier {
        public final String name;
        public final String mcc;
        public final String mnc;
        public final String operatorCode;

        public IndianCarrier(String name, String mcc, String mnc) {
            this.name = name;
            this.mcc = mcc;
            this.mnc = mnc;
            this.operatorCode = mcc + mnc;
        }
    }

    private static final DevicePreset[] PRESETS = new DevicePreset[]{
            new DevicePreset("google", "Pixel 8 Pro", "Google", "husky",
                    "google/husky/husky:14/UD1A.231105.004/11010452:user/release-keys"),
            new DevicePreset("samsung", "SM-S928B", "samsung", "e3q",
                    "samsung/e3qxeea/e3q:14/UP1A.231005.007/S928BXXU1AXB5:user/release-keys"),
            new DevicePreset("Xiaomi", "23116PN5BC", "Xiaomi", "shennong",
                    "Xiaomi/shennong/shennong:14/UKQ1.230804.001/V816.0.4.0.UNBCNXM:user/release-keys"),
            new DevicePreset("OnePlus", "CPH2581", "OnePlus", "OP595DL1",
                    "OnePlus/CPH2581/OP595DL1:14/UKQ1.230924.001/T.11867c4-1-2:user/release-keys")
    };

    private static final IndianCarrier[] INDIAN_CARRIERS = new IndianCarrier[]{
            new IndianCarrier("Jio", "405", "854"),
            new IndianCarrier("Airtel", "404", "45"),
            new IndianCarrier("Vi", "404", "20"),
            new IndianCarrier("BSNL", "404", "34")
    };

    private static SharedPreferences getPrefs() {
        Context context = BlackBoxCore.getContext();
        if (context == null) return null;
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isSpoofingEnabled(int userId) {
        SharedPreferences prefs = getPrefs();
        if (prefs == null) return true;
        String prefix = "user_" + userId + "_";
        return prefs.getBoolean(prefix + "enabled", true);
    }

    public static void ensureProfileConfigExists(int userId) {
        SharedPreferences prefs = getPrefs();
        if (prefs == null) return;
        String prefix = "user_" + userId + "_";
        if (!prefs.contains(prefix + "enabled")) {
            Random random = new Random();
            DevicePreset preset = PRESETS[random.nextInt(PRESETS.length)];
            IndianCarrier carrier = INDIAN_CARRIERS[random.nextInt(INDIAN_CARRIERS.length)];
            String androidId = generateRandomHex(16);
            String imei = generateRandomImei(random);
            String serial = generateRandomAlphanumeric(12);
            String phoneNumber = generateRandomIndianPhoneNumber(random);

            prefs.edit()
                    .putString(prefix + "brand", preset.brand)
                    .putString(prefix + "model", preset.model)
                    .putString(prefix + "manufacturer", preset.manufacturer)
                    .putString(prefix + "device", preset.device)
                    .putString(prefix + "fingerprint", preset.fingerprint)
                    .putString(prefix + "android_id", androidId)
                    .putString(prefix + "imei", imei)
                    .putString(prefix + "serial", serial)
                    .putString(prefix + "sim_carrier_name", carrier.name)
                    .putString(prefix + "sim_operator", carrier.operatorCode)
                    .putString(prefix + "sim_phone_number", phoneNumber)
                    .putString(prefix + "sim_country_iso", "in")
                    .putBoolean(prefix + "enabled", true)
                    .apply();
            Log.d(TAG, "Generated spoof config for user " + userId + ": " + preset.model + ", SIM: " + carrier.name);
        }
    }

    public static String getSpoofedAndroidId(int userId) {
        ensureProfileConfigExists(userId);
        SharedPreferences prefs = getPrefs();
        if (prefs == null) return "e4a789c012bf34d5";
        String prefix = "user_" + userId + "_";
        return prefs.getString(prefix + "android_id", "e4a789c012bf34d5");
    }

    public static String getSpoofedImei(int userId) {
        ensureProfileConfigExists(userId);
        SharedPreferences prefs = getPrefs();
        if (prefs == null) return "358921094829104";
        String prefix = "user_" + userId + "_";
        return prefs.getString(prefix + "imei", "358921094829104");
    }

    public static String getSpoofedSerial(int userId) {
        ensureProfileConfigExists(userId);
        SharedPreferences prefs = getPrefs();
        if (prefs == null) return "1A2B3C4D5E6F";
        String prefix = "user_" + userId + "_";
        return prefs.getString(prefix + "serial", "1A2B3C4D5E6F");
    }

    public static String getSimOperatorName(int userId) {
        ensureProfileConfigExists(userId);
        SharedPreferences prefs = getPrefs();
        if (prefs == null) return "Jio";
        String prefix = "user_" + userId + "_";
        return prefs.getString(prefix + "sim_carrier_name", "Jio");
    }

    public static String getSimOperator(int userId) {
        ensureProfileConfigExists(userId);
        SharedPreferences prefs = getPrefs();
        if (prefs == null) return "405854";
        String prefix = "user_" + userId + "_";
        return prefs.getString(prefix + "sim_operator", "405854");
    }

    public static String getSimCountryIso(int userId) {
        return "in";
    }

    public static String getPhoneNumber(int userId) {
        ensureProfileConfigExists(userId);
        SharedPreferences prefs = getPrefs();
        if (prefs == null) return "9876543210";
        String prefix = "user_" + userId + "_";
        return prefs.getString(prefix + "sim_phone_number", "9876543210");
    }

    public static String getSubscriberId(int userId) {
        String simOp = getSimOperator(userId);
        String imei = getSpoofedImei(userId);
        if (imei != null && imei.length() >= 9) {
            return simOp + imei.substring(imei.length() - 9);
        }
        return simOp + "987654321";
    }

    /**
     * Overwrites static fields of {@link android.os.Build} via reflection in the current process.
     */
    public static void applyBuildSpoof(int userId) {
        if (!isSpoofingEnabled(userId)) {
            Log.d(TAG, "Spoofing disabled for user " + userId + ", skipping Build override");
            return;
        }
        ensureProfileConfigExists(userId);
        SharedPreferences prefs = getPrefs();
        if (prefs == null) return;

        String prefix = "user_" + userId + "_";
        String brand = prefs.getString(prefix + "brand", "google");
        String model = prefs.getString(prefix + "model", "Pixel 8 Pro");
        String manufacturer = prefs.getString(prefix + "manufacturer", "Google");
        String device = prefs.getString(prefix + "device", "husky");
        String fingerprint = prefs.getString(prefix + "fingerprint", "google/husky/husky:14/UD1A.231105.004/11010452:user/release-keys");
        String serial = prefs.getString(prefix + "serial", "P8P234892301");

        setStaticField(Build.class, "BRAND", brand);
        setStaticField(Build.class, "MODEL", model);
        setStaticField(Build.class, "MANUFACTURER", manufacturer);
        setStaticField(Build.class, "DEVICE", device);
        setStaticField(Build.class, "PRODUCT", device);
        setStaticField(Build.class, "HARDWARE", device);
        setStaticField(Build.class, "FINGERPRINT", fingerprint);
        setStaticField(Build.class, "SERIAL", serial);
        setStaticField(Build.class, "BOARD", device);

        Log.d(TAG, "Successfully applied Build spoof for user " + userId + ": " + brand + " " + model);
    }

    private static void setStaticField(Class<?> clazz, String fieldName, Object value) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, value);
        } catch (Throwable e) {
            Log.w(TAG, "Could not set Build." + fieldName + ": " + e.getMessage());
        }
    }

    private static String generateRandomHex(int length) {
        String chars = "0123456789abcdef";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static String generateRandomAlphanumeric(int length) {
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static String generateRandomImei(Random random) {
        String prefix = random.nextBoolean() ? "35" : "86";
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 12; i++) {
            sb.append(random.nextInt(10));
        }
        String base = sb.toString();
        int checkDigit = calculateLuhn(base);
        return base + checkDigit;
    }

    private static String generateRandomIndianPhoneNumber(Random random) {
        String[] startDigits = new String[]{"6", "7", "8", "9"};
        String start = startDigits[random.nextInt(startDigits.length)];
        StringBuilder sb = new StringBuilder(start);
        for (int i = 0; i < 9; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private static int calculateLuhn(String numberString) {
        int sum = 0;
        boolean alternate = true;
        for (int i = numberString.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(numberString.charAt(i));
            if (alternate) {
                n *= 2;
                if (n > 9) n = (n % 10) + 1;
            }
            sum += n;
            alternate = !alternate;
        }
        int mod = sum % 10;
        return (mod == 0) ? 0 : 10 - mod;
    }
}
