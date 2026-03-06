# Black Hole Glow - Project Knowledge Base

## Quick Reference

| Key | Value |
|-----|-------|
| **Package** | `com.secret.blackholeglow` |
| **Language** | Java 11 (NO Kotlin) |
| **SDK** | Min 24, Target 35 |
| **Build** | Gradle Kotlin DSL |
| **Play Store** | v40 (5.4.0), Alpha track |
| **Current branch** | `version1.1.2026` |
| **Device** | Samsung SM-A155M (Android 16) |

## Build Commands

```bash
./gradlew assembleDebug    # Debug APK
./gradlew installDebug     # Install on connected device
./gradlew bundleRelease    # Play Store AAB (needs keystore)
./gradlew clean            # Clean build
```

---

## Architecture

### Rendering Pipeline
```
LiveWallpaperService
  └── GLWallpaperEngine
        ├── GLWallpaperSurfaceView (OpenGL ES 3.0)
        ├── WallpaperDirector (renderer, manages scenes)
        │     ├── SceneFactory → creates WallpaperScene instances
        │     ├── Panel mode (control panel overlay)
        │     └── Auto-play (500ms delay after returning to home)
        └── Auto-rotate system (5 min interval, pre-download)
```

### Activity Flow
```
SplashActivity → MainActivity (drawer + wallpaper catalog) → WallpaperPreviewActivity
```

### Key Classes

| Class | Location | Purpose |
|-------|----------|---------|
| `LiveWallpaperService` | `LiveWallpaperService.java` | WallpaperService, manages GL engine lifecycle |
| `WallpaperDirector` | `core/WallpaperDirector.java` | GLSurfaceView.Renderer, scene orchestration |
| `SceneFactory` | `core/SceneFactory.java` | Creates scene instances by name |
| `WallpaperCatalog` | `systems/WallpaperCatalog.java` | All wallpapers (static + dynamic) |
| `WallpaperPreferences` | `WallpaperPreferences.java` | SharedPreferences + Firebase sync |
| `ResourcePreloader` | `core/ResourcePreloader.java` | Downloads/caches scene resources |
| `PreFlightCheck` | `core/PreFlightCheck.java` | Verifies scene resources are ready |
| `DynamicCatalog` | `systems/DynamicCatalog.java` | Fetches server-driven wallpapers |

### Scene Types

| Base Class | Type | Auto-includes |
|------------|------|---------------|
| `BaseVideoScene` | Video wallpaper | Video renderer, equalizer, clock, battery |
| `BaseParallaxScene` | Multi-layer parallax | Parallax camera, touch interaction |
| `DynamicImageScene` | Server-driven image | Fullscreen image render |
| `DynamicVideoScene` | Server-driven video | Video + equalizer/clock/battery |
| `ImageWallpaperScene` | Static image | Single fullscreen image |

---

## Supabase (Backend/Storage)

### Connection
| Key | Value |
|-----|-------|
| **Project ref** | `vzuwvsmlyigjtsearxym` |
| **URL** | `https://vzuwvsmlyigjtsearxym.supabase.co` |
| **Public storage URL** | `https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/public/` |
| **Service role token** | `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ6dXd2c21seWlnanRzZWFyeHltIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc1ODY0ODcwOSwiZXhwIjoyMDc0MjI0NzA5fQ.xDs_HCkdqcEVJktzTdjGIXnG-V--j86jbkrUA4SjAOs` |

### Storage Buckets
| Bucket | Content |
|--------|---------|
| `wallpaper-videos/` | MP4 videos for video scenes |
| `wallpaper-images/` | WebP images, previews, `dynamic_catalog.json` |
| `wallpaper-models/` | OBJ 3D models |

### Upload via cURL
```bash
# Upload image
curl -X POST "https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/wallpaper-images/FILENAME" \
  -H "Authorization: Bearer SERVICE_ROLE_TOKEN" \
  -H "Content-Type: image/webp" \
  -H "x-upsert: true" \
  --data-binary @LOCAL_FILE

# Upload video
curl -X POST "https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/wallpaper-videos/FILENAME.mp4" \
  -H "Authorization: Bearer SERVICE_ROLE_TOKEN" \
  -H "Content-Type: video/mp4" \
  -H "x-upsert: true" \
  --data-binary @LOCAL_FILE
```

---

## Dynamic Catalog System (Server-Driven Wallpapers)

Add wallpapers WITHOUT code changes. App fetches `dynamic_catalog.json` from Supabase on launch, caches 6h.

### JSON Format (`wallpaper-images/dynamic_catalog.json`)
```json
{
  "version": 3,
  "wallpapers": [
    {
      "id": "unique_id",
      "type": "IMAGE",
      "name": "DISPLAY NAME",
      "description": "User-facing text",
      "imageFile": "dyn_id.webp",
      "previewFile": "dyn_id_preview.webp",
      "imageSize": 273894,
      "videoSize": 0,
      "previewSize": 74346,
      "glowColor": "#FFD700",
      "badge": "NEW",
      "sortOrder": 2,
      "category": "ANIME"
    }
  ]
}
```

**Categories**: `ANIME`, `GAMING`, `SCENES`, `ANIMALS`, `NATURE`, `UNIVERSE`, `MISC`, `CHRISTMAS`, `SUMMER`, `AUTUMN`, `WINTER`, `SPECIAL`

**Scene name convention**: App uses `DYN_IMG_<id>` for images, `DYN_VID_<id>` for videos.

### Current Dynamic Catalog (v3)
| ID | Type | Category |
|----|------|----------|
| akuma | IMAGE | GAMING |
| saga_de_geminis | IMAGE | ANIME |
| gatito_emo | IMAGE | ANIMALS |
| chucky | IMAGE | SCENES |
| gatito_payasito | IMAGE | ANIMALS |
| ovni_scene | VIDEO | SCENES |

### Pipeline: Add New Dynamic Wallpaper

#### IMAGE type:
1. Convert image to WebP:
   ```bash
   ffmpeg -y -i source.png -vf "scale='min(1080,iw)':-1" -quality 85 dyn_myid.webp
   ffmpeg -y -i source.png -vf "scale='min(512,iw)':-1" -quality 75 dyn_myid_preview.webp
   ```
2. Upload both to `wallpaper-images/` bucket
3. Download `dynamic_catalog.json`, add entry, re-upload
4. Done - app auto-fetches on next launch

#### VIDEO type:
1. Optimize video:
   ```bash
   ffmpeg -i source.mp4 -vf "scale=720:-2" -c:v libx264 -crf 23 -an -movflags +faststart dyn_myid.mp4
   ```
2. Extract preview frame:
   ```bash
   ffmpeg -ss 00:00:02 -i dyn_myid.mp4 -frames:v 1 -vf "scale=512:-1" -quality 75 dyn_myid_preview.webp
   ```
3. Upload video to `wallpaper-videos/`, preview to `wallpaper-images/`
4. Add to `dynamic_catalog.json` with `"type": "VIDEO"`, `"videoFile"`, `"videoSize"` fields

---

## Adding Static (Code-Based) Scenes

### Steps for a Video Scene:
1. Upload MP4 to Supabase `wallpaper-videos/`
2. Register in `video/VideoConfig.java` (filename → ResourceInfo with URL, size)
3. Create scene class extending `BaseVideoScene` (implement 5 methods: getName, getDescription, getPreviewResourceId, getVideoFileName, getTheme)
4. Register in `SceneFactory` (`core/SceneFactory.java` ~line 120): `registerScene("MY_SCENE", MyScene.class);`
5. Add to `WallpaperCatalog` (`systems/WallpaperCatalog.java` ~line 240)
6. Register in `ResourcePreloader` (4 switch blocks: prepareScene, getRequiredVideos, getRequiredImages, getRequiredModels)
7. Add preview WebP: `res/drawable/preview_my_scene.webp` (512x768)
8. **CRITICAL**: Add to `VALID_WALLPAPERS` in `WallpaperPreferences.java` — missing this = wallpaper won't load!
9. Build: `./gradlew installDebug`

### Steps for a Parallax Scene:
Same as above, plus:
- Upload OBJ models to `wallpaper-models/`
- Upload textures to `wallpaper-images/`
- Register textures in `image/ImageConfig.java`
- Use `CALIBRATION_MODE` for positioning (set true, drag objects, read LogCat tag `"CALIBRATE"`)

---

## Auto-Rotate System (Current State - March 2026)

### How it works:
- Timer fires every 5 minutes (`AUTO_ROTATE_INTERVAL_MS`)
- Picks random wallpaper from `WallpaperCatalog.getAutoRotateCandidates()`
- Downloads resources if needed, verifies with `PreFlightCheck`
- Falls back to `GATITO_IMG` if nothing works
- Max disk: current + pre-downloaded + fallback = 3 wallpapers

### Recent improvements (branch `version1.1.2026`):
1. **Instant toggle**: SharedPreferences listener reacts immediately when user toggles auto-rotate ON/OFF
2. **Pre-download**: After each rotation, downloads NEXT wallpaper in background for instant switch
3. **Samsung freeze detection**: Logs warning if timer drift >30s (FreecessHandler)
4. **Resource protection**: `additionalSceneToProtect` in ResourcePreloader protects pre-downloaded scene during cleanup

### Key fields in GLWallpaperEngine:
- `preDownloadedScene` — name of pre-downloaded scene ready for next rotation
- `preDownloadInProgress` — prevents duplicate pre-downloads
- `prefListener` — SharedPreferences.OnSharedPreferenceChangeListener for toggle
- `autoRotateScheduledAt` — epoch ms for freeze detection

---

## Key Patterns & Conventions

| Pattern | Details |
|---------|---------|
| **Shaders** | `assets/shaders/*.glsl`, loaded via `ShaderUtils.createProgramFromAssets()` |
| **Textures** | `TextureManager` with lazy-load cache |
| **Models** | OBJ in `assets/`, loaded via `ObjLoader.loadObj()` |
| **GL thread safety** | All GL calls on GL thread, UI on main thread |
| **Panel mode** | WallpaperDirector shows control panel overlay on home return |
| **Auto-play delay** | 500ms after returning to home (avoids GPU waste on fast app-switching) |
| **Panel auto-start delay** | 3.0s (`PANEL_AUTO_START_DELAY` in WallpaperDirector) |
| **GL render mode** | Always `RENDERMODE_CONTINUOUSLY` — Director handles idle via `Thread.sleep` |
| **Visibility debounce** | 300ms to prevent Samsung stuttering on fast pause/resume |

---

## Play Store & Keystore

| Key | Value |
|-----|-------|
| **Keystore file** | `blackholeglow-release-key.jks` (project root) |
| **Store/Key password** | `blackholeglow2025` |
| **Key alias** | `blackholeglow` |
| **Algorithm** | RSA 2048-bit |
| **AAB output** | `app/build/outputs/bundle/release/app-release.aab` |
| **Version bumping** | Edit `versionCode` and `versionName` in `app/build.gradle.kts` |

---

## GitHub

- **Repo**: `DraKenZaMaNosKe/blackholeglow`
- **CLI**: `gh` (authenticated)
- **Main branch**: `master`
- **Current work branch**: `version1.1.2026`

---

## Blender MCP (for 2.5D scenes)

| Key | Value |
|-----|-------|
| **Host** | `127.0.0.1` |
| **Port** | `9876` |
| **Transport** | TCP socket, JSON messages |

```json
{"type": "execute_code", "params": {"code": "import bpy; ..."}}
{"type": "get_scene_info"}
```

Workflow: Generate PNGs → Create planes in Blender → Position at Z depths → Export OBJ + textures → Load in app with parallax

---

## Hostinger API (if needed)

| Key | Value |
|-----|-------|
| **API Token** | `R0CTj2AyZ7NCFLz88dRG6NCsxuO20HVqn7RKjsRY6bef16e8` |
| **MCP command** | `npx hostinger-api-mcp@latest` |

---

## Troubleshooting

- **Wallpaper not loading (shows previous)**: Missing from `VALID_WALLPAPERS` in `WallpaperPreferences.java`
- **SharedPrefs won't clear on Android 16**: Use `adb shell pm clear com.secret.blackholeglow` (not `run-as`)
- **GL freeze**: Check `RENDERMODE_CONTINUOUSLY` is set, never switch to `WHEN_DIRTY`
- **Supabase CDN cache**: Re-upload with `x-upsert: true` header
- **Dynamic catalog not updating**: Force-refresh clears 6h cache; check `DynamicCatalog.refreshDynamicCatalogAsync()`
