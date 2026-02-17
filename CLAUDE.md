# CLAUDE.md

## Project Overview

**Black Hole Glow** - Android live wallpaper with OpenGL ES 2.0 3D space scenes.

- **Package**: `com.secret.blackholeglow`
- **Language**: Java (no Kotlin) | **Java**: 11
- **SDK**: Min 24, Target 35 | **Build**: Gradle Kotlin DSL
- **Deps**: `gradle/libs.versions.toml` (version catalogs)

## Build Commands

```bash
./gradlew assembleDebug      # Debug APK
./gradlew assembleRelease    # Release APK
./gradlew bundleRelease      # Play Store AAB
./gradlew installDebug       # Install on device
./gradlew clean              # Clean
./gradlew test               # Unit tests
```

Release artifacts: `app/build/outputs/bundle/release/app-release.aab`

## Architecture

### Rendering Pipeline
1. **LiveWallpaperService** - Entry point, creates GLWallpaperEngine. Prefs: `blackholeglow_prefs`
2. **SceneRenderer** - GLSurfaceView.Renderer, render loop, manages CameraController + TextureManager
3. **CameraController** - Multi-mode camera (ORBIT default). Static 3/4 isometric view (4,3,6 -> 0,0,0)
4. **BaseShaderProgram** - Abstract shader base. Uniforms: `u_Time`, `u_MVP`, `u_Resolution`

### Scene Objects
- Implement `SceneObject` interface + `CameraAware` for camera reference
- Key classes: `Planeta`, `UniverseBackground`, `Asteroide`, `Spaceship3D`

### Key Patterns
- **Camera assignment**: Always `((CameraAware) obj).setCameraController(sharedCamera)`
- **Shaders**: Loaded from `assets/shaders/*.glsl` via `ShaderUtils.createProgramFromAssets()`
- **Textures**: `TextureManager` with lazy-load cache. Resources in `res/drawable/`
- **Models**: OBJ files in `assets/`, loaded via `ObjLoader.loadObj()`
- **Threading**: GL calls on GL thread, UI on main thread
- **ShaderUtils duplication**: `com.secret.blackholeglow.ShaderUtils` (rendering) vs `com.secret.blackholeglow.opengl.ShaderUtils` (UI)

### OpenGL Config
- ES 2.0, RGB565, 16-bit depth, no stencil
- Depth test + face culling + alpha blending (SRC_ALPHA, ONE_MINUS_SRC_ALPHA)

### Activity Flow
SplashActivity -> MainActivity (DrawerLayout + AnimatedWallpaperListFragment) -> WallpaperPreviewActivity

## Notes
- Live wallpaper project - main entry is WallpaperService, not Activity
- Touch interaction disabled for stability
- Spanish comments throughout codebase
- LogCat tags: `LiveWallpaperService`, `SceneRenderer`, `Planeta`, `TextureManager`, `CameraController`

## Current Version
- **versionCode**: 36 | **versionName**: 5.1.1 (published to Play Console)

## Recent Changes (v5.1.1)
- **Auto-panel on visibility restore**: `LiveWallpaperService.startRendering()` calls `switchToPanelMode()` when returning to home screen (saves CPU ~90% → ~20%). Guarded by `!isSystemPreviewMode` so wallpaper picker still works.
- Texture compression + lazy unloading for low-RAM devices
- Moonlit Cat: UV blink system + Supabase integration

## Next Task: "Enchanted Garden" Wallpaper Scene

### Concept
Magical garden at night with luminous flowers, a cat, moon, fireflies. **Flat vector / storybook illustration style**.

### Reference Image
Generated via Grok — flat illustration, dark background, glowing flowers (pink/purple/blue), black cat, moon, fireflies, layered foliage for depth.

### Approach: 2.5D Billboard in Blender
Create the scene using **2D planes positioned in 3D space** in Blender:

1. **Generate assets in Grok** (each element with transparent background):
   - Sky + moon (background layer)
   - Back foliage/leaves (light colored)
   - Flowers + stems (multiple, with glow)
   - Cat (center focal point)
   - Front foliage/leaves (dark, foreground)

2. **Assemble in Blender**:
   - Create a plane for each layer
   - Apply textures with alpha transparency
   - Position planes at different Z depths
   - Animate with subtle wind/sway keyframes
   - Test camera movement for natural parallax

3. **Export for blackholeglow**:
   - Export OBJ + textures per layer
   - Load in OpenGL with existing parallax/scene architecture
   - Add shaders: glow on flowers, particle system for fireflies, eye blink on cat

### Layer Structure (front to back)
| Z | Layer | Animation |
|---|-------|-----------|
| 0 | Front dark leaves | Strong parallax sway |
| 1 | Cat | Eye blink (texture swap) |
| 2 | Flowers + stems | Gentle wind sway |
| 3 | Back light leaves | Subtle parallax |
| 4 | Sky + moon | Static or slow glow pulse |
| -- | Particles/fireflies | Shader-driven floating |

### Key Decisions Pending
- Final art style approval after Grok generates separated layers
- Whether to use vertex animation (shader) or keyframe animation for wind
- Camera behavior: static with touch parallax, or slow auto-orbit
