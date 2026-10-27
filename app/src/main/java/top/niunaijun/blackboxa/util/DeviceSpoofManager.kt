package top.niunaijun.blackboxa.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.random.Random

data class DeviceSpoofConfig(
    val brand: String = "google",
    val model: String = "Pixel 8 Pro",
    val manufacturer: String = "Google",
    val device: String = "husky",
    val fingerprint: String = "google/husky/husky:14/UD1A.231105.004/11010452:user/release-keys",
    val androidId: String = "e4a789c012bf34d5",
    val imei: String = "358921094829104",
    val serialNumber: String = "1A2B3C4D5E6F",
    val isEnabled: Boolean = true
)

object DeviceSpoofManager {

    private const val PREFS_NAME = "device_spoof_prefs"

    val PRESET_PIXEL_8_PRO = DeviceSpoofConfig(
        brand = "google",
        model = "Pixel 8 Pro",
        manufacturer = "Google",
        device = "husky",
        fingerprint = "google/husky/husky:14/UD1A.231105.004/11010452:user/release-keys",
        androidId = "a1b2c3d4e5f60718",
        imei = "356789012345678",
        serialNumber = "P8P234892301",
        isEnabled = true
    )

    val PRESET_SAMSUNG_S24_ULTRA = DeviceSpoofConfig(
        brand = "samsung",
        model = "SM-S928B",
        manufacturer = "samsung",
        device = "e3q",
        fingerprint = "samsung/e3qxeea/e3q:14/UP1A.231005.007/S928BXXU1AXB5:user/release-keys",
        androidId = "b9f8e7d6c5b4a321",
        imei = "359812034982103",
        serialNumber = "R58N1234567",
        isEnabled = true
    )

    val PRESET_XIAOMI_14_PRO = DeviceSpoofConfig(
        brand = "Xiaomi",
        model = "23116PN5BC",
        manufacturer = "Xiaomi",
        device = "shennong",
        fingerprint = "Xiaomi/shennong/shennong:14/UKQ1.230804.001/V816.0.4.0.UNBCNXM:user/release-keys",
        androidId = "c3d4e5f6a1b20789",
        imei = "864521093821094",
        serialNumber = "XM14P8920194",
        isEnabled = true
    )

    val PRESET_ONEPLUS_12 = DeviceSpoofConfig(
        brand = "OnePlus",
        model = "CPH2581",
        manufacturer = "OnePlus",
        device = "OP595DL1",
        fingerprint = "OnePlus/CPH2581/OP595DL1:14/UKQ1.230924.001/T.11867c4-1-2:user/release-keys",
        androidId = "d7c8b9a0e1f23456",
        imei = "861928374650192",
        serialNumber = "OP1298472910",
        isEnabled = true
    )

    val PRESETS = listOf(
        "Pixel 8 Pro" to PRESET_PIXEL_8_PRO,
        "Galaxy S24 Ultra" to PRESET_SAMSUNG_S24_ULTRA,
        "Xiaomi 14 Pro" to PRESET_XIAOMI_14_PRO,
        "OnePlus 12" to PRESET_ONEPLUS_12
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getConfig(context: Context, userId: Int): DeviceSpoofConfig {
        val prefs = getPrefs(context)
        val prefix = "user_${userId}_"
        if (!prefs.contains("${prefix}enabled")) {
            val randomConfig = generateRandomConfig()
            saveConfig(context, userId, randomConfig)
            return randomConfig
        }
        return DeviceSpoofConfig(
            brand = prefs.getString("${prefix}brand", PRESET_PIXEL_8_PRO.brand) ?: PRESET_PIXEL_8_PRO.brand,
            model = prefs.getString("${prefix}model", PRESET_PIXEL_8_PRO.model) ?: PRESET_PIXEL_8_PRO.model,
            manufacturer = prefs.getString("${prefix}manufacturer", PRESET_PIXEL_8_PRO.manufacturer) ?: PRESET_PIXEL_8_PRO.manufacturer,
            device = prefs.getString("${prefix}device", PRESET_PIXEL_8_PRO.device) ?: PRESET_PIXEL_8_PRO.device,
            fingerprint = prefs.getString("${prefix}fingerprint", PRESET_PIXEL_8_PRO.fingerprint) ?: PRESET_PIXEL_8_PRO.fingerprint,
            androidId = prefs.getString("${prefix}android_id", PRESET_PIXEL_8_PRO.androidId) ?: PRESET_PIXEL_8_PRO.androidId,
            imei = prefs.getString("${prefix}imei", PRESET_PIXEL_8_PRO.imei) ?: PRESET_PIXEL_8_PRO.imei,
            serialNumber = prefs.getString("${prefix}serial", PRESET_PIXEL_8_PRO.serialNumber) ?: PRESET_PIXEL_8_PRO.serialNumber,
            isEnabled = prefs.getBoolean("${prefix}enabled", true)
        )
    }

    fun saveConfig(context: Context, userId: Int, config: DeviceSpoofConfig) {
        val prefs = getPrefs(context)
        val prefix = "user_${userId}_"
        prefs.edit {
            putString("${prefix}brand", config.brand)
            putString("${prefix}model", config.model)
            putString("${prefix}manufacturer", config.manufacturer)
            putString("${prefix}device", config.device)
            putString("${prefix}fingerprint", config.fingerprint)
            putString("${prefix}android_id", config.androidId)
            putString("${prefix}imei", config.imei)
            putString("${prefix}serial", config.serialNumber)
            putBoolean("${prefix}enabled", config.isEnabled)
        }
    }

    fun clearConfig(context: Context, userId: Int) {
        val prefs = getPrefs(context)
        val prefix = "user_${userId}_"
        prefs.edit {
            remove("${prefix}brand")
            remove("${prefix}model")
            remove("${prefix}manufacturer")
            remove("${prefix}device")
            remove("${prefix}fingerprint")
            remove("${prefix}android_id")
            remove("${prefix}imei")
            remove("${prefix}serial")
            remove("${prefix}enabled")
        }
    }

    fun generateRandomAndroidId(): String {
        val hexChars = "0123456789abcdef"
        return (1..16).map { hexChars[Random.nextInt(hexChars.length)] }.joinToString("")
    }

    fun generateRandomImei(): String {
        // Standard 15-digit TAC + SNR + Luhn
        val prefix = if (Random.nextBoolean()) "35" else "86"
        val remaining = (1..12).map { Random.nextInt(10) }.joinToString("")
        val base = prefix + remaining
        val checkDigit = calculateLuhnCheckDigit(base)
        return base + checkDigit
    }

    fun generateRandomSerial(): String {
        val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return (1..12).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    fun generateRandomConfig(currentConfig: DeviceSpoofConfig = PRESET_PIXEL_8_PRO): DeviceSpoofConfig {
        val basePreset = PRESETS.random().second
        return basePreset.copy(
            androidId = generateRandomAndroidId(),
            imei = generateRandomImei(),
            serialNumber = generateRandomSerial()
        )
    }

    private fun calculateLuhnCheckDigit(numberString: String): Int {
        var sum = 0
        var alternate = true
        for (i in numberString.length - 1 downTo 0) {
            var n = Character.getNumericValue(numberString[i])
            if (alternate) {
                n *= 2
                if (n > 9) n = (n % 10) + 1
            }
            sum += n
            alternate = !alternate
        }
        val mod = sum % 10
        return if (mod == 0) 0 else 10 - mod
    }
}
