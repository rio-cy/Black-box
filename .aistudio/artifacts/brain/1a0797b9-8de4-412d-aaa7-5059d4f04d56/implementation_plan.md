# SAF Migration & Storage Permission Removal Plan

Complete removal of legacy storage permissions (`READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `MANAGE_EXTERNAL_STORAGE`) in favor of Storage Access Framework (`ACTION_OPEN_DOCUMENT`) for APK importing and virtualized isolated storage for cloned applications.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following choices were confirmed by the user during the Phase 1 interview:

- **Confirmed Decision 1 (APK Importing)**: Use standard Android SAF file picker (`ACTION_OPEN_DOCUMENT`) without requesting any broad storage permissions.
- **Confirmed Decision 2 (Virtual Storage)**: Isolate cloned applications inside BlackBox's private app directory (`/Android/data/top.niunaijun.blackbox/files/blackbox/`) and spoof virtual storage checks (`checkSelfPermission`) to return `PERMISSION_GRANTED` to virtual apps.
- **Confirmed Decision 3 (Manifest Cleanup)**: Completely remove legacy storage permission declarations from `AndroidManifest.xml` and eliminate runtime storage permission prompts from `MainActivity.kt` and `StoragePermissionHelper.java`.

---

## 1. Overview & Core Concept

- **What It Does**: Upgrades BlackBox to a zero-storage-permission model. APK files can be imported via system SAF file picking. Cloned applications operate seamlessly inside BlackBox's isolated storage sandbox without prompting the host user for phone storage permissions.
- **Target Audience**: Users seeking privacy-respecting virtual app cloning without granting sensitive device-wide storage access permissions.
- **Key Value**: Enhanced user privacy, full Google Play policy compliance, and clean installation UX using native Android document pickers.

---

## 2. User Experience & Visual Design

- **App Import Flow**:
  1. User taps the **"+" (Add App)** floating action button on the main screen.
  2. Selecting "Select Local APK File" opens Android's system document picker (`ACTION_OPEN_DOCUMENT`).
  3. User selects an `.apk` file from any directory (Downloads, Drive, internal storage).
  4. BlackBox streams the selected file into internal cache (`/data/data/.../cache`) via `ContentResolver` and installs it into the virtual environment.
  5. No runtime permission dialogs or system settings redirects are presented to the user.

- **UI Polish & Feedback**:
  - Direct progress indicator during SAF stream copying and installation.
  - Toast notification confirming successful app clone/install.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: SAF Uri Stream Copying for Installation**
  - *Chosen Approach*: Stream APK bytes directly from `content://` Uri via `ContentResolver` into private cache directory before invoking `BPackageInstaller`.
  - *Why*: Allows installing APKs from any folder without needing file path read permissions on `/sdcard`.

- **Decision 2: Virtual Storage Permission Interception**
  - *Chosen Approach*: Override `IActivityManagerProxy`, `IPackageManagerProxy`, and `IAppOpsManagerProxy` so virtual permission checks for `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` return `PackageManager.PERMISSION_GRANTED`.
  - *Why*: Cloned apps expect storage permission to be granted. Since BlackBox redirects their file operations to isolated private app folders, returning `PERMISSION_GRANTED` prevents cloned apps from crashing or stuck permission loops.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                    Host UI (MainActivity)                   │
└──────────────────────────────┬──────────────────────────────┘
                               │ SAF (ACTION_OPEN_DOCUMENT)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│             ContentResolver (InputStream Copy)              │
└──────────────────────────────┬──────────────────────────────┘
                               │ Stream to cache
                               ▼
┌─────────────────────────────────────────────────────────────┐
│          BlackBox Core Virtual Environment (Bcore)          │
│  - BEnvironment (Private Sandbox Redirection)               │
│  - IOCore (Redirects /sdcard -> /Android/data/.../blackbox) │
│  - PermissionProxy (Spoofs PERMISSION_GRANTED to virtual)   │
└─────────────────────────────────────────────────────────────┘
```

### Affected Files & Modifications Summary

1. **`app/src/main/AndroidManifest.xml` & `Bcore/src/main/AndroidManifest.xml`**:
   - Remove `<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>`
   - Remove `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>`
   - Remove `<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"/>`

2. **`StoragePermissionHelper.java`**:
   - Simplify permission helper to return `true` by default for storage access, bypassing system permission prompts.

3. **`MainActivity.kt`**:
   - Remove runtime storage permission request triggers during app initialization and APK selection.
   - Wire local APK selection directly to `ActivityResultContracts.StartActivityForResult()` with `Intent.ACTION_OPEN_DOCUMENT`.

4. **`IActivityManagerProxy.java` / `IPackageManagerProxy.java` / `IAppOpsManagerProxy.java`**:
   - Ensure virtual permission checks for `READ_EXTERNAL_STORAGE` and `WRITE_EXTERNAL_STORAGE` return `PERMISSION_GRANTED` to virtualized apps.
