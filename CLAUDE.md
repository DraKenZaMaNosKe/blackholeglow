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
