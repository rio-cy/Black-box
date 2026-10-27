# Implementation Plan: Profile Deletion Safety & Selected Profile Location Preview

Enhance BlackBox virtual sandbox with explicit profile deletion safety controls (including cloned application audit and a 3-second countdown timer) and interactive location preview on the map for selected profiles.

## Confirmed User Decisions

1. **Profile Deletion Safety & Timer:**
   - Long-pressing a profile tab displays profile management options.
   - Deleting a selected profile (User 1+) triggers an alert dialog displaying total cloned virtual applications in that profile (*"Do you want to delete this profile? Total app is clone [X]"*).
   - Enforces a mandatory **3-second safety countdown lock** (`Delete (3s)` $\to$ `Delete (2s)` $\to$ `Delete (1s)` $\to$ `Delete Profile`) before the delete action button unlocks.
   - Primary Profile 0 remains protected from accidental deletion.

2. **Fake Location Preview for Selected Profile:**
   - Fullscreen world map powered by OpenStreetMap (`org.osmdroid`).
   - Displays an interactive single location pin with instant live preview.
   - Tapping anywhere on the world map positions the location pin and synchronizes GPS coordinates (`latitude`, `longitude`) to `BLocationManager` specifically for the active user profile (`userID`).
   - Also retains route walking motion simulation options for advanced users.

---

## Technical Architecture & Data Flow

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Main Dashboard (MainActivity)                   │
├────────────────────────────────────────────────────────────────────────┤
│ • Scrollable Profile Tabs (User 0, User 1, User 2...)                  │
│ • Long Press Gesture -> ProfileOptionsBottomSheet                      │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │               Delete Profile Safety Alert Dialog                  │  │
│  ├──────────────────────────────────────────────────────────────────┤  │
│  │ ⚠️ Warning: Deleting Profile "User 1"                             │  │
│  │ Total Cloned Apps: X                                             │  │
│  │ Safety Timer: [3s Countdown Lock] -> Enables Delete Button       │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                    Location Manager (FakeManagerActivity)              │
├────────────────────────────────────────────────────────────────────────┤
│ • Launches FollowMyLocationOverlay for selected profile (userID)       │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                 FollowMyLocationOverlay (OSMDroid)                │  │
│  ├──────────────────────────────────────────────────────────────────┤  │
│  │ • Interactive World Map with single location pin                 │  │
│  │ • Real-time GPS coordinate preview (Latitude, Longitude)         │  │
│  │ • Immediate location sync: BLocationManager.setLocation(userId)  │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Proposed Changes & Source Verification

### 1. User Profile Deletion Safety (`MainActivity.kt`)
- **Long Press Detection:** Ensure `ScrollableTabRow` detects long presses on profile tabs (`pointerInput` with `detectTapGestures`).
- **Profile Options:** Provide "Rename Profile", "Device Identity Spoofing", and "Delete Profile".
- **Safety Dialog:**
  - Audits cloned applications using `BlackBoxCore.get().getInstalledApplications(0, targetUser).size`.
  - Prominently displays: `"Do you want to delete profile [Name]? Total app in profile is [X]..."`.
  - Implements `LaunchedEffect` 3-second ticker state (`countdown` 3 $\to$ 2 $\to$ 1 $\to$ 0).
  - Keeps the "Delete Profile" button disabled until `countdown == 0`.
  - On confirm: calls `BlackBoxCore.get().deleteUser(targetUser)`, cleans SharedPreferences, updates `availableUsers`, and switches back to User 0.

### 2. Fake Location Preview for Selected Profile (`FollowMyLocationOverlay.kt` & `FakeManagerActivity.kt`)
- **Profile Location Synchronization:** Pass `userID` from `FakeManagerActivity` to `FollowMyLocationOverlay`.
- **Interactive Map:**
  - Render OpenStreetMap `MapView` with single location pin.
  - Tapping map drops the target location marker, shows floating coordinate badge, and syncs coordinates to `BLocationManager.get().setLocation(userId, pkg, BLocation(lat, lng))`.
  - Clicking "Apply" returns coordinates to `FakeManagerActivity` and refreshes active profile location state.

---

## Verification Plan

### Manual Verification Steps
1. **Profile Deletion Safety Test:**
   - Create a secondary profile (User 1) and clone 1 or 2 applications.
   - Long-press User 1 profile tab to open profile options.
   - Tap "Delete Profile". Verify alert displays total cloned apps count and countdown starts at 3s.
   - Verify Delete button is disabled until 3 seconds elapse, then tap Delete to verify profile and app data cleanup.
2. **Fake Location Preview Test:**
   - Open Fake Location from dashboard or settings for selected profile.
   - Tap anywhere on the interactive map. Verify single pin updates position, preview shows current latitude/longitude, and location is saved to the selected profile.
