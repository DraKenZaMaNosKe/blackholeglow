# Session Notes - December 1, 2024

## Summary
Session focused on UI improvements for the Orbix live wallpaper app.

---

## Changes Made

### 1. Like Button - Neon Cyberpunk Style
**File:** `app/src/main/java/com/secret/blackholeglow/sharing/LikeButton.java`
**File:** `app/src/main/java/com/secret/blackholeglow/core/SongSharingController.java`

- **Size reduced:** From `0.10f` to `0.04f` (60% smaller)
- **Position:** `(0.85f, -0.50f)` - bottom right corner
- **New colors:**
  - Normal: Rosa neón semi-transparente `(1.0, 0.0, 0.5, 0.85)`
  - Pressed: Cyan brillante `(0.0, 0.85, 1.0, 1.0)`
  - Cooldown: Gris apagado `(0.3, 0.3, 0.35, 0.6)`
- **Glow effect:** Interpolates between cyan (`#00D9FF`) and pink (`#FF0080`)
- **Border:** Cyan neon pulsing border
- **Highlight:** Interior cyan glow that pulses

**Important:** The size was being overwritten in `SongSharingController.java` line 54, not in `LikeButton.java`

### 2. Avatar Sphere Repositioning
**File:** `app/src/main/java/com/secret/blackholeglow/AvatarSphere.java`

- **Old position:** `(1.85, 3.10, 0.10)` - top right, between sun and earth
- **New position:** `(-4.0, -5.2, 0.10)` - bottom left corner, at the level of the like button
- User requested it moved because it didn't fit the space scene between the sun and earth

### 3. WallpaperPreviewActivity Improvements
**File:** `app/src/main/java/com/secret/blackholeglow/activities/WallpaperPreviewActivity.java`

- Added instructions panel explaining PLAY/STOP buttons
- New gradient cyan button with glow animation for "Definir fondo de pantalla"
- Added "Desinstalar wallpaper" red button (only shows if wallpaper is installed)
- Uninstall replaces live wallpaper with a black bitmap

### 4. Pixel Cat Icon
**Files:**
- `app/src/main/res/drawable/ic_launcher_foreground.xml` - White pixel cat with cyan eyes
- `app/src/main/res/drawable/ic_launcher_background.xml` - Dark space gradient with stars

### 5. Leaderboard Hidden
**File:** `app/src/main/java/com/secret/blackholeglow/scenes/BatallaCosmicaScene.java`

- Commented out `setupLeaderboard()` call

### 6. AndroidManifest.xml
- Added `SET_WALLPAPER` permission for uninstall functionality

### 7. LiveWallpaperService Fix
**File:** `app/src/main/java/com/secret/blackholeglow/LiveWallpaperService.java`

- Moved `isPreview()` call from constructor to `onSurfaceCreated()` to avoid NullPointerException
- Fixed system preview showing loading dots instead of wallpaper

---

## Git Status
- **Branch:** `beta1.0`
- **Last commit:** `394866b` - "💜 UI Improvements: Neon Like Button + Avatar Repositioning + Preview Activity"
- **Pushed to:** `origin/beta1.0`

---

## Pending/Future Tasks
- None specified in this session

---

## Key Learnings
1. `LikeButton` size was being overwritten in `SongSharingController.java`, not in the class defaults
2. `isPreview()` cannot be called in WallpaperService Engine constructor - must wait until `onSurfaceCreated()`
3. To uninstall a live wallpaper, set a black bitmap wallpaper instead of opening the chooser

---

## Files Modified
```
.claude/settings.local.json
app/src/main/AndroidManifest.xml
app/src/main/java/com/secret/blackholeglow/AvatarSphere.java
app/src/main/java/com/secret/blackholeglow/LiveWallpaperService.java
app/src/main/java/com/secret/blackholeglow/activities/WallpaperPreviewActivity.java
app/src/main/java/com/secret/blackholeglow/core/SongSharingController.java
app/src/main/java/com/secret/blackholeglow/scenes/BatallaCosmicaScene.java
app/src/main/java/com/secret/blackholeglow/sharing/LikeButton.java
app/src/main/res/drawable/ic_launcher_background.xml
app/src/main/res/drawable/ic_launcher_foreground.xml
```
