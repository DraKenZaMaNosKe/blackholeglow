# CLAUDE.md

## Project Overview

**Black Hole Glow** — Android live wallpaper with OpenGL ES 2.0/3.0 scenes.

- **Package**: `com.secret.blackholeglow` | **Language**: Java 11 (no Kotlin)
- **SDK**: Min 24, Target 35 | **Build**: Gradle Kotlin DSL
- **Deps**: `gradle/libs.versions.toml` | **Version**: 5.3.0 (versionCode 39)

## Build

```bash
./gradlew assembleDebug    # Debug APK
./gradlew installDebug     # Install on device
./gradlew bundleRelease    # Play Store AAB
```

## Architecture

**Pipeline**: LiveWallpaperService → SceneRenderer → SceneFactory → WallpaperScene
**Activity flow**: SplashActivity → MainActivity (drawer + catalog) → WallpaperPreviewActivity
**Diagnostic**: Auto-reads WallpaperCatalog.getAll() + device RAM tier → calculates compatibility per wallpaper (no manual registration needed)

## Adding a New Video Scene (complete steps)

### 1. Upload video to Supabase
- Bucket: `wallpaper-videos/`
- Format: MP4, loop-friendly, ~720p/1080p

### 2. Register video in `video/VideoConfig.java`
- Add entry to the map: filename → ResourceInfo (URL, size, description, priority)

### 3. Create scene class extending `BaseVideoScene`
Implement 5 abstract methods:
- `getName()` → `"MY_SCENE"`
- `getDescription()` → user-facing text
- `getPreviewResourceId()` → `R.drawable.preview_my_scene`
- `getVideoFileName()` → `"my_scene.mp4"` (filename in Supabase)
- `getTheme()` → `EqualizerBarsDJ.Theme.XXXX`

BaseVideoScene auto-includes: video renderer, equalizer, clock, battery overlay.

### 4. Register in SceneFactory (`core/SceneFactory.java` ~line 120)
```java
registerScene("MY_SCENE", MyScene.class);
```

### 5. Add to WallpaperCatalog (`systems/WallpaperCatalog.java` ~line 240)
```java
catalog.add(new WallpaperItem.Builder("MY SCENE")
    .descripcion("Description for the user")
    .preview(R.drawable.preview_my_scene)
    .sceneName("MY_SCENE")           // must match SceneFactory ID
    .tier(WallpaperTier.FREE)
    .badge("NEW")
    .glow(0xFFRRGGBB)
    .weight(SceneWeight.LIGHT)       // LIGHT | MEDIUM | HEAVY
    .featured()
    .build());
```
The `weight` drives the diagnostic panel compatibility (auto-calculated vs device RAM).

### 6. Register in ResourcePreloader (`core/ResourcePreloader.java`)
Add scene to **4 switch blocks**:
- `prepareScene()` (~line 113) — call your prepare method
- `getRequiredVideos()` (~line 301) — `return Arrays.asList("my_scene.mp4");`
- `getRequiredImages()` (~line 377) — `return Arrays.asList();` (empty for video-only)
- `getRequiredModels()` (~line 435) — `return Arrays.asList();` (empty for video-only)

### 7. Add preview image
- Format: **WebP**, portrait (~512x768)
- Path: `res/drawable/preview_my_scene.webp`
- Naming: `preview_[lowercase_id].webp`

### 8. Build and test
```bash
./gradlew installDebug
```

## Adding Scenes with 3D Objects

Same steps as above, plus:
- Upload OBJ models + textures to Supabase (`wallpaper-models/`, `wallpaper-images/`)
- Register in `image/ImageConfig.java` (textures) and add OBJ filenames to ResourcePreloader
- Use `CALIBRATION_MODE` to position objects (see below)

## Supabase Storage

- **URL base**: `https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/public/`
- **Buckets**: `wallpaper-videos/`, `wallpaper-images/`, `wallpaper-models/`
- Upload via Supabase console or CLI, then register URL in VideoConfig/ImageConfig

## Calibration Mode

For positioning 3D objects in a scene:

```java
private static final boolean CALIBRATION_MODE = false; // true to tune

// In onTouchEvent():
if (CALIBRATION_MODE) return handleCalibrationTouch(nx, ny, action);

// In update():
if (!CALIBRATION_MODE) { /* run animations */ }
```

1. Set `CALIBRATION_MODE = true`, install
2. Drag to adjust position, double-tap to switch object, tap corner to cycle mode
3. Read values from LogCat tag `"CALIBRATE"`
4. Copy values to code, set `CALIBRATION_MODE = false`

## Blender Workflow

MCP addon on port 9876. Use for assembling 2.5D billboard scenes:
1. Generate layer assets (transparent PNGs)
2. Create planes in Blender, apply textures, position at Z depths
3. Export OBJ + textures per layer
4. Load in scene with parallax architecture

## Key Patterns

- **Shaders**: `assets/shaders/*.glsl` loaded via `ShaderUtils.createProgramFromAssets()`
- **Textures**: `TextureManager` with lazy-load cache
- **Models**: OBJ in `assets/`, loaded via `ObjLoader.loadObj()`
- **GL thread**: All GL calls on GL thread, UI on main thread
- **Shader cleanup**: Always check `GL_COMPILE_STATUS` and call `glDeleteShader()` on failure
