# Implementation Plan - In-App Fake Camera Spoofing & Host Permission Removal

Remove `android.permission.CAMERA` from the host application manifests while providing virtual camera permission granting and an in-app fake white screen camera capture interface for cloned apps inside the BlackBox sandbox.

## User Review Required

> [!IMPORTANT]
> The host `AndroidManifest.xml` will not request or hold `android.permission.CAMERA`. Cloned apps checking for camera permissions will be virtually reported as granted, and camera intent triggers or camera access attempts will be routed to an in-app `WhiteScreenCameraActivity`.

- **Virtual Permission Granting**: `IPackageManagerProxy` and `IAppOpsManagerProxy` will virtually return `PERMISSION_GRANTED` and `MODE_ALLOWED` for camera ops inside the sandbox.
- **Fake White Camera UI**: `WhiteScreenCameraActivity` provides a full-screen white camera interface with a capture action button that returns a white JPEG image result with `RESULT_OK`.
- **System Camera Stubbing**: Direct camera service IPC calls will be handled gracefully within `ICameraServiceProxy` without making raw binder calls to system `ICameraService` (preventing `SecurityException` when host permission is absent).

## Proposed Changes

### Manifest Updates
- Remove `<uses-permission android:name="android.permission.CAMERA" />` from:
  - `/Bcore/src/main/AndroidManifest.xml`
  - `/app/src/main/AndroidManifest.xml`

### Virtual Sandbox Permissions
#### `/Bcore/src/main/java/top/niunaijun/blackbox/fake/service/IPackageManagerProxy.java`
- Intercept permission checking methods (`checkPermission`, `checkUidPermission`, `grantRuntimePermission`) for `android.permission.CAMERA` and return `PackageManager.PERMISSION_GRANTED` to virtual applications.

#### `/Bcore/src/main/java/top/niunaijun/blackbox/fake/service/IAppOpsManagerProxy.java`
- Intercept `noteOp`, `noteOpNoThrow`, `checkOp`, `checkOpNoThrow`, `startOp`, and `startOpNoThrow` for `AppOpsManager.OP_CAMERA` / `OPSTR_CAMERA` to return `AppOpsManager.MODE_ALLOWED`.

### Fake Camera Interface & Service Proxy
#### `/Bcore/src/main/java/top/niunaijun/blackbox/fake/service/WhiteScreenCameraActivity.java`
- Implement a full-screen white preview with a capture button. Upon clicking capture, encode a white bitmap into a JPEG file/URI and return it via `setResult(RESULT_OK, intent)`.

#### `/Bcore/src/main/java/top/niunaijun/blackbox/fake/service/ICameraServiceProxy.java` & Activity Proxy
- Safely stub out `connect`, `open`, `getNumberOfCameras`, and `getCameraInfo` binder calls so they return mock camera device metadata instead of throwing system binder `SecurityException`.
- Redirect intent actions (`MediaStore.ACTION_IMAGE_CAPTURE`, `ACTION_VIDEO_CAPTURE`, `INTENT_ACTION_STILL_IMAGE_CAMERA`) inside virtual app contexts to launch `WhiteScreenCameraActivity`.

## Verification Plan

### Automated Verification
- Run `compile_applet` to verify clean compilation of all modules (`app`, `Bcore`, C++ native code).

### Manual Verification
- Verify that host app installs without requesting system camera permission.
- Verify cloned apps see `CAMERA` permission as virtually granted and can open the fake white screen camera to capture blank white photos.
