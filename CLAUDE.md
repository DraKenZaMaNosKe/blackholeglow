# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Black Hole Glow** is an Android live wallpaper application featuring OpenGL ES 2.0-based 3D space scenes with animated planets, black holes, and orbital mechanics. The app is currently at version 3.0.0 (branch: `version-3.0.0`, main branch: `master`).

**Package**: `com.secret.blackholeglow`
**Language**: Java (no Kotlin)
**Java Version**: 11
**Min SDK**: 24, **Target SDK**: 35
**Build System**: Gradle with Kotlin DSL (`.kts`)

## Common Commands

### Build and Run
```bash
# On Windows, use gradlew.bat or ./gradlew
# On macOS/Linux, use ./gradlew

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Clean build
./gradlew clean
```

### Testing
```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

### Gradle
Dependencies are managed via `gradle/libs.versions.toml` using version catalogs.

## Architecture Overview

### Important Code Organization Notes

- **ShaderUtils Duplication**: There are two `ShaderUtils.java` files:
  - `com.secret.blackholeglow.ShaderUtils` (root package) - Used by rendering pipeline
  - `com.secret.blackholeglow.opengl.ShaderUtils` - Used by UI components
  - Be careful when modifying to edit the correct one for your use case

### Core Rendering Pipeline

The app uses a custom OpenGL ES 2.0 rendering system with a scene graph architecture:

1. **LiveWallpaperService** (`LiveWallpaperService.java`)
   - Entry point for the Android live wallpaper
   - Creates `GLWallpaperEngine` which wraps a `GLSurfaceView`
   - Manages lifecycle (onCreate, onVisibilityChanged, onDestroy)
   - Uses SharedPreferences (`blackholeglow_prefs`) to store selected wallpaper
   - Important: Touch events disabled due to stability concerns

2. **SceneRenderer** (`SceneRenderer.java`)
   - Implements `GLSurfaceView.Renderer`
   - Main render loop and scene management
   - Configures OpenGL state (depth test, blending, clear color)
   - Manages `CameraController` and `TextureManager`
   - Supports multiple scenes: "Universo" and "Agujero Negro"
   - **Camera is FIXED** by default - `sharedCamera.update(dt)` is commented out in onDrawFrame

3. **CameraController** (`CameraController.java`)
   - Professional multi-mode camera system
   - Modes: FIRST_PERSON, THIRD_PERSON, ORBIT_CAMERA, TOP_DOWN, ISOMETRIC, CINEMATIC, FREE_CAMERA
   - Handles View and Projection matrices
   - Computes MVP (Model-View-Projection) matrices for scene objects
   - Default mode: ORBIT_CAMERA with smooth transitions enabled
   - Currently configured for static 3/4 isometric view (position: 4,3,6 looking at 0,0,0)

4. **BaseShaderProgram** (`BaseShaderProgram.java`)
   - Abstract base class for all shader programs
   - Compiles and links shaders from assets
   - Caches uniform locations: `u_Time`, `u_MVP`, `u_Resolution`
   - Provides `setTime(float phase)` and `setMvpAndResolution()` methods

5. **Scene Objects**
   - All scene objects implement `SceneObject` interface (update/draw methods)
   - Most implement `CameraAware` to receive CameraController reference
   - **Planeta.java**: Renders spherical planets with textures, orbits, rotation, optional color overlay
   - **UniverseBackground.java**: Background skybox/plane
   - **Asteroide.java**: Asteroid objects
   - Others: `BlenderCubeBackground`, `BeamBackground`, `DeformableCubeBackground`

### Shader System

- Shaders stored in `app/src/main/assets/shaders/` as `.glsl` files
- Loaded via `ShaderUtils.createProgramFromAssets()`
- Common uniforms across shaders:
  - `u_Time`: Animation phase (0 to 2π)
  - `u_MVP`: Model-View-Projection matrix
  - `u_Resolution`: Screen resolution (width, height)
- Planet-specific uniforms: `u_Texture`, `u_UseSolidColor`, `u_SolidColor`, `u_Alpha`, `u_UvScale`

### Texture Management

- **TextureManager** (`TextureManager.java`) implements `TextureLoader`
- Lazy-loading texture cache using `HashMap<Integer, Integer>`
- Textures loaded on-demand via `getTexture(int resourceId)`
- Uses `ShaderUtils.loadTexture()` for actual GL texture loading
- Textures stored in `app/src/main/res/drawable/`

### 3D Models

- OBJ files stored in `app/src/main/assets/`
- Models: `planeta.obj`, `asteroide.obj`, `cube.obj`, `beam.obj`, `plano.obj`
- Loaded via `ObjLoader.loadObj()` utility (`util/ObjLoader.java`)
- Mesh data includes vertex positions, UV coordinates, and face indices

### Activity Flow

1. **SplashActivity** - Initial launcher activity
2. **MainActivity** - Main UI with navigation drawer
   - Uses `DrawerLayout` with `NavigationView`
   - Fragment container for content
   - Default fragment: `AnimatedWallpaperListFragment`
3. **WallpaperPreviewActivity** - Preview before setting wallpaper

### Fragment System

- **AnimatedWallpaperListFragment** - Displays list of available wallpapers
- Uses `WallpaperAdapter` (RecyclerView adapter)
- Model: `WallpaperItem` (`models/WallpaperItem.java`)
- Shows animated preview borders via `AnimatedBorderTextureView`

## Key Technical Details

### OpenGL Configuration

- OpenGL ES 2.0 required (`android:glEsVersion="0x00020000"`)
- EGL config: RGB565, 16-bit depth buffer, no stencil
- Depth testing enabled (`GL_DEPTH_TEST`)
- Face culling enabled (`GL_CULL_FACE`)
- Blending enabled with `GL_SRC_ALPHA`, `GL_ONE_MINUS_SRC_ALPHA`

### Scene Setup Examples

**Universe Scene** (`setupUniverseScene()`):
- Background: `UniverseBackground` with `universo03` texture
- Central sun: `Planeta` with `textura_sol`, scale 0.5, no orbit
- Sun glow: Semi-transparent planet with color overlay and scale oscillation
- Orbiting planet: Smaller planet with orbital parameters (radiusX=2.5, radiusZ=2.0, speed=0.3)

**Black Hole Scene** (`setupBlackHoleScene()`):
- Central black hole: Black solid-color planet (scale 2.0)
- Accretion disk: 3 orbiting particles with varying radii and orange glow

### Important Patterns

1. **Camera Assignment**: Always set camera on scene objects after creation:
   ```java
   if (obj instanceof CameraAware) {
       ((CameraAware) obj).setCameraController(sharedCamera);
   }
   ```

2. **Planet Constructor Parameters** (in order):
   - Context, TextureManager, vertex shader path, fragment shader path
   - Texture resource ID
   - orbitRadiusX, orbitRadiusZ, orbitSpeed
   - scaleAmplitude (dynamic variation)
   - instanceScale (base size)
   - spinSpeed (rotation)
   - useSolidColor (boolean), solidColor (float[4]), alpha
   - scaleOscPercent (Float, optional pulsing)
   - uvScale (texture tiling)

3. **Shader Loading**: All shaders loaded from assets, prefixed with `shaders/`
   ```java
   "shaders/planeta_vertex.glsl"
   "shaders/planeta_fragment.glsl"
   ```

4. **Threading**: OpenGL calls must be on GL thread, UI updates on main thread
   - LiveWallpaperService uses `Handler(Looper.getMainLooper())` for thread-safe updates

## Recent Development Focus

Based on recent commits, the project is focused on:
- Fixing planet shader texture application (alpha channel issues resolved)
- Optimizing shader performance to reduce pixelation
- Stabilizing version 3.0.0 with working alpha channel support
- Fine-tuning visual effects and textures

## Important Files Not to Modify

- `AndroidManifest.xml` - Well-documented with ASCII art, includes all necessary permissions and service declarations
- `app/src/main/res/xml/live_wallpaper.xml` - Wallpaper metadata (if exists)

## Debugging Tips

- All major classes use Android logging with `Log.d(TAG, ...)` and `Log.e(TAG, ...)`
- SceneRenderer includes FPS counter (logs every second)
- Check LogCat for tags: `LiveWallpaperService`, `SceneRenderer`, `Planeta`, `TextureManager`, `CameraController`

## Notes

- This is a live wallpaper project, not a standard app - the main entry point is the WallpaperService
- The codebase contains extensive Spanish comments and ASCII art documentation
- Touch interaction is intentionally disabled in the wallpaper service for stability
- All matrix math uses OpenGL conventions (column-major)
- The camera system is feature-rich but currently used in a static configuration
