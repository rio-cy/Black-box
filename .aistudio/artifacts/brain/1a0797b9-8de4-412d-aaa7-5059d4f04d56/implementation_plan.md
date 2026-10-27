# Implementation Plan: App & Virtualization Framework Updates

This plan addresses remote endpoint removal, crash log local storage, fake location preview cleanup, Indian SIM operator spoofing, Android 11 identity spoofing compatibility, and dynamic UI responsiveness across screen sizes.

---

## User Review Required

> [!IMPORTANT]
> **Key Architectural Decisions**
> 1. **Crash Log Storage**: Remote endpoint HTTP uploads will be replaced with local file logging under app-private storage (`context.getExternalFilesDir("crash_logs")` / `context.getFilesDir()`).
> 2. **Indian SIM Carriers**: Identity spoofing will include preset telemetry for **Jio**, **Airtel**, **Vi (Vodafone Idea)**, and **BSNL** with authentic Indian Mobile Country Codes (MCC 404/405), Mobile Network Codes (MNC), operator names, and realistic 10-digit mobile numbers (+91 / 6x, 7x, 8x, 9x series). Carrier profiles will be randomly assigned per virtual app profile.
> 3. **Android 11 (API 30) Virtual Hooks**: Fix `TelephonyManager`, `SubscriptionManager`, and `Build` reflection proxies in `Bcore` to properly handle API 30+ IPC signature changes and hidden API restrictions.
> 4. **Dynamic Responsiveness**: Layouts and text sizing in Compose screens will adapt dynamically to window width and floating multi-window dimensions.

---

## Proposed Changes

### 1. Remote Log Upload Removal & Local Storage
- **`Bcore/src/main/java/top/niunaijun/blackbox/utils/LogSender.java`**
  - Remove remote upload endpoint URL (`https://logs-sender-api.vercel.app/...`).
  - Redirect crash log output to write formatted log files locally in app private storage (`/storage/emulated/0/Android/data/<package>/files/crash_logs/`).
- **`Bcore/src/main/java/top/niunaijun/blackbox/app/configuration/ClientConfiguration.java`** & **`SimpleCrashFix.java`**
  - Update crash handler hooks to save stack traces locally without initiating network requests.

### 2. Fake Location Preview Removal
- **`app/src/main/java/top/niunaijun/blackboxa/view/location/`** (and related location composables/activities)
  - Remove map preview, location preview UI elements, and unnecessary preview rendering handlers.
  - Retain core virtual location coordinates config without remote preview dependencies.

### 3. Indian SIM Operator Identity Spoofing
- **`Bcore/src/main/java/top/niunaijun/blackbox/fake/frameworks/BTelephonyManager.java`** & **`BSubManager.java`**
  - Define Indian SIM provider profiles:
    - **Jio**: MCC `405`, MNC `854`, Operator Name `"Jio"`, Country `"in"`.
    - **Airtel**: MCC `404`, MNC `45`, Operator Name `"Airtel"`, Country `"in"`.
    - **Vi**: MCC `404`, MNC `20`, Operator Name `"Vi"`, Country `"in"`.
    - **BSNL**: MCC `404`, MNC `34`, Operator Name `"BSNL"`, Country `"in"`.
  - Implement random provider assignment logic per virtual profile with realistic 10-digit Indian phone numbers (`9XXXXXXXXX`, `8XXXXXXXXX`, `7XXXXXXXXX`, `6XXXXXXXXX`).

### 4. Android 11 (API 30) Spoofing Compatibility
- **`Bcore/src/main/java/top/niunaijun/blackbox/fake/service/ITelephonyManagerProxy.java`** & **`ITelephonyRegistryProxy.java`**
  - Update method proxies for API 30+ method signatures (e.g. `getNetworkOperatorName`, `getSimOperatorName`, `getNetworkCountryIso`, `getSimCountryIso`, `getSubscriberId`, `getLine1Number`).
  - Add fallback reflection guards for Android 11 system service restrictions.

### 5. Responsive UI & Dynamic Typography
- **`app/src/main/java/top/niunaijun/blackboxa/` UI Composables**
  - Integrate `BoxWithConstraints` and screen width breakpoints (`Compact`, `Medium`, `Expanded`).
  - Dynamically compute text sizes and icon sizes based on available window width so text remains clear and readable in floating window or split-screen modes.

---

## Verification Plan

### Automated Build Verification
- Execute `compile_applet` to verify zero compilation errors across `Bcore` and `app` modules.

### Logical Verification
- Check that `LogSender.java` contains no HTTP network code or URL references.
- Verify that Indian carrier MCC/MNC codes and random number generation function properly in `BTelephonyManager`.
- Verify multi-window/small screen dynamic font size rules in UI components.
