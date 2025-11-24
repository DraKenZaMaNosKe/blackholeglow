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

---

## 🚀 Play Store Release Configuration (October 2024)

### Release Keystore Information

**⚠️ CRITICAL - KEEP THIS INFORMATION SAFE**

The app is configured for Google Play Store release with a signing keystore:

- **Keystore File**: `blackholeglow-release-key.jks`
- **Location**: Project root directory (`C:\Users\eduar\AndroidStudioProjects\blackholeglow\`)
- **Store Password**: `blackholeglow2025`
- **Key Alias**: `blackholeglow`
- **Key Password**: `blackholeglow2025`
- **Validity**: 10,000 days (~27 years)
- **Algorithm**: RSA 2048-bit

**⚠️ WARNING**: If this keystore is lost, the app can NEVER be updated on Play Store. Back it up in multiple secure locations (Google Drive, Dropbox, external drive, etc.)

### Build Commands for Release

```bash
# Build release AAB for Play Store
./gradlew bundleRelease

# Build release APK (for manual distribution)
./gradlew assembleRelease

# Clean and rebuild
./gradlew clean bundleRelease --no-daemon
```

### Release Artifacts Location

After building, find the signed artifacts at:
- **AAB (Play Store)**: `app/build/outputs/bundle/release/app-release.aab`
- **APK (Manual)**: `app/build/outputs/apk/release/app-release.apk`

### Signing Configuration

The signing is configured in `app/build.gradle.kts`:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("${rootProject.projectDir}/blackholeglow-release-key.jks")
        storePassword = "blackholeglow2025"
        keyAlias = "blackholeglow"
        keyPassword = "blackholeglow2025"
    }
}

buildTypes {
    release {
        isMinifyEnabled = false
        signingConfig = signingConfigs.getByName("release")
    }
}
```

---

## 🎨 Wallpaper Catalog - 10 Themed Items

The app features a catalog of 10 thematically distinct wallpapers, each with:
- Unique gradient background drawable
- Emoji icon
- Captivating Spanish description
- Animated border shader (in development)

### Current Wallpaper List

Defined in `AnimatedWallpaperListFragment.java`:

1. **🌌 Viaje Espacial** (`preview_space.xml`)
   - Dark blue gradient
   - Shader: `frame_space_fragment.glsl` (stars effect - under development)

2. **🌲 Bosque Encantado** (`preview_forest.xml`)
   - Dark green/teal gradient
   - Shader: `frame_forest_fragment.glsl` (fireflies effect - under development)

3. **🏙️ Neo Tokyo 2099** (`preview_cyberpunk.xml`)
   - Pink to cyan neon gradient
   - Shader: `frame_cyberpunk_fragment.glsl` (neon effect - under development)

4. **🏖️ Paraíso Dorado** (`preview_beach.xml`)
   - Orange to gold sunset gradient
   - Shader: `beam_fragment.glsl` (✅ WORKING - this one displays correctly)

5. **🦁 Safari Salvaje** (`preview_safari.xml`)
   - Orange to yellow savanna gradient
   - Shader: `particula_fragment.glsl`

6. **🌧️ Lluvia Mística** (`preview_rain.xml`)
   - Gray stormy gradient
   - Shader: `frame_rain_fragment.glsl` (rain effect - under development)

7. **🎮 Pixel Quest** (`preview_retro.xml`)
   - Vibrant pink/purple 8-bit gradient
   - Shader: `battery_fragment.glsl`

8. **🕳️ Portal Infinito** (`preview_blackhole.xml`)
   - Black to purple radial gradient
   - Shader: `forcefield_fragment.glsl`

9. **🌸 Jardín Zen** (`preview_zen.xml`)
   - Pink sakura to light blue gradient
   - Shader: `test_border_fragment.glsl` (✅ WORKING - this one displays correctly)

10. **⚡ Furia Celestial** (`preview_storm.xml`)
    - Dark blue to electric yellow gradient
    - Shader: `frame_storm_fragment.glsl` (lightning effect - under development)

### Gradient Drawables

Each wallpaper has a matching gradient drawable in `app/src/main/res/drawable/`:
- Format: `preview_[theme].xml`
- Type: `<shape>` with `<gradient>` using theme-specific colors
- These are displayed in the RecyclerView as background for each CardView

---

## 🔧 Known Issues & Work in Progress

### Animated Border Shaders (PENDING FIX)

**Problem**: Items 3 and 9 display their animated borders correctly, but other items do not show their shader effects.

**Affected Shaders**:
- `frame_space_fragment.glsl` (Item 0)
- `frame_forest_fragment.glsl` (Item 1)
- `frame_cyberpunk_fragment.glsl` (Item 2)
- `frame_rain_fragment.glsl` (Item 5)
- `frame_storm_fragment.glsl` (Item 9)

**Investigation Findings**:
1. Layout structure is correct (`item_wallpaper_card_textureview.xml`)
2. `AnimatedBorderTextureView` is properly configured with OpenGL ES 2.0
3. `AnimatedBorderRendererThread` manages EGL surface and provides `u_Time`, `u_Resolution` uniforms
4. The `beam_fragment.glsl` (Item 3) shader works correctly
5. Some shaders may require additional uniforms (`u_Reveal`, `u_HaloWidth`) that are not being set
6. Attempted fixes with border masking and solid colors did not resolve the issue

**Next Steps** (for tomorrow):
- Investigate why `beam_fragment.glsl` and `test_border_fragment.glsl` work but others don't
- Check if certain shaders need different vertex shaders
- Review shader compilation errors in LogCat
- Consider simplifying shaders to match working examples

### Layout Notes

- **CardView margin changed**: `android:layout_margin="4dp"` (was 20dp, user adjusted)
- `AnimatedBorderTextureView` has `match_parent` dimensions
- Blending is enabled in renderer: `GL_SRC_ALPHA`, `GL_ONE_MINUS_SRC_ALPHA`
- Clear color with alpha=0 for transparency

---

## 📱 Current Version Status

- **Version**: 3.0.0
- **Version Code**: 1
- **Version Name**: "1.0"
- **Branch**: `version-3.0.0`
- **Status**: Ready for internal testing on Play Store
- **Last Release Build**: October 14, 2024
- **AAB Size**: ~39MB

### Features Completed
✅ OpenGL ES 2.0 live wallpaper rendering
✅ 3D space scenes with planets and black holes
✅ 10 themed wallpapers with unique gradients
✅ Firebase authentication integration
✅ Google Sign-In support
✅ Material Design UI with navigation drawer
✅ Wallpaper preview system
✅ RecyclerView catalog with animated borders (partial)
✅ Release build configuration with signing

### Features In Progress
🔄 Animated border shaders for all 10 wallpapers
🔄 Shader effects optimization and debugging

---

## 🛠️ Development Environment

- **IDE**: Android Studio (Ladybug 2024.2.1 or newer)
- **JDK**: 11 (from Android Studio JBR)
- **Gradle**: 8.13
- **Build Tools**: AGP 8.12.3
- **Testing Device**: Connected via ADB

### Windows-Specific Commands

```bash
# Set JAVA_HOME for Gradle (Windows Git Bash)
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export PATH="$JAVA_HOME/bin:$PATH"

# Build commands
./gradlew.bat assembleDebug
./gradlew.bat bundleRelease

# Install to device
"C:/Users/eduar/AppData/Local/Android/Sdk/platform-tools/adb.exe" install -r "app/build/outputs/apk/debug/app-debug.apk"
```

---

## 📝 Session Notes (October 14, 2024)

### Work Completed
1. Created 10 thematically distinct wallpapers with unique gradients and descriptions
2. Attempted to implement animated border shaders for each theme
3. Discovered shader rendering issues (only 2 out of 10 display correctly)
4. Created release keystore for Play Store
5. Successfully built signed AAB for Play Store upload
6. Configured signing in build.gradle.kts

### Deferred Work
- Fix animated border shaders to work consistently across all items
- Investigate shader compilation/uniform issues
- Test release build on multiple devices via Play Store internal testing

### Important Decisions
- Reverted shader experiments to working baseline
- Kept original shader assignments that were partially working
- Prioritized Play Store release over completing shader animations
- Documented all keystore information for future releases

---

## 📝 Session Notes (November 23, 2024) - Version 4.0.0

### Work Completed

#### 🎵 MusicIndicator (Ecualizador Visual)
- Barras con gradiente de colores (Rosa → Rojo → Naranja → Verde → Cyan)
- Peak holders con colores arcoíris basados en altura
- Sistema de chispas que explotan al pasar el peak
- Sensibilidad progresiva para barras de treble (BAR_SENSITIVITY hasta 12x)
- LEDs que se encienden gradualmente con música
- Beat detection para reactividad mejorada

#### 🛸 OVNI/Spaceship3D con IA Inteligente
- Sistema de exploración libre con deambulación orgánica
- Esquiva automáticamente la Tierra (nunca atraviesa)
- Rebote suave en límites de pantalla (optimizado para portrait)
- Rotación suave mirando hacia dirección de movimiento
- **Optimización**: Cache de valores random (cada 10 frames en lugar de cada frame)

#### 🌍 Escena Universo
- Barra de countdown de meteoritos OCULTA (funcionalidad activa, visual deshabilitada)
- OVNI habilitado en escena con parámetros optimizados
- Sol procedural optimizado (576 triángulos vs 7,936)

#### ⚡ Optimizaciones de Rendimiento
- FPS estable: 36-43 FPS (aceptable para live wallpaper 3D)
- Random reutilizable en Spaceship3D (evita Math.random() costoso)
- Cache de valores aleatorios actualizados cada 10 frames

### Archivos Modificados
- `MusicIndicator.java` - Sistema de ecualizador completo reescrito
- `SceneRenderer.java` - OVNI habilitado, barra countdown oculta
- `Spaceship3D.java` - IA de exploración libre + optimizaciones

### Próximas Mejoras Potenciales
- 🔫 Sistema de armas láser para el OVNI (disparar a la Tierra)
- 💥 Impactos visuales de láser en el planeta
- 🎯 IA de ataque del OVNI

### Notas Técnicas
- El OVNI usa `safeDistance = 2.0` para evitar atravesar la Tierra
- Límites de pantalla: X(-2,2), Y(-1.8,2.5), Z(-3,2)
- Velocidad del OVNI: 0.2-0.7 unidades/segundo
