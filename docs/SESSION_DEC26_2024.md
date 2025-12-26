# Session Notes - December 26, 2024

## Orbix Live Wallpaper - Black Hole Glow

---

## What is Orbix?

**Orbix** is a premium Android Live Wallpaper app featuring stunning 3D OpenGL ES scenes with real-time music visualization. The app transforms your home screen into an immersive visual experience that reacts to your music.

### Key Features

| Feature | Description |
|---------|-------------|
| **3D OpenGL ES** | Real-time 3D rendering with shaders |
| **Music Reactive** | Equalizer bars dance to your music |
| **Multiple Scenes** | Different themed wallpapers |
| **Smooth Performance** | Optimized for battery life |
| **AI Integration** | Gemini AI for song sharing messages |

---

## Available Wallpapers

### 1. Batalla Cósmica (Cosmic Battle)
**Theme**: Space Defense
**Description**: Defend Earth from incoming meteorites in this epic space battle scene.

- Procedural sun with realistic heat glow
- Detailed Earth with atmosphere shader
- Saturn with rings
- Defender ships and UFO attackers
- Real-time music-reactive equalizer
- Particle effects for explosions

### 2. Bosque Navideño (Christmas Forest)
**Theme**: Christmas Magic
**Description**: A magical Christmas forest with falling snow and aurora borealis.

- Animated smoke distortion shader
- Aurora borealis color detection animation
- Falling snow particles
- Christmas tree lights
- Touch sparkles (gold, red, green particles)
- Gift box decorative button
- Music-reactive equalizer (EqualizerBarsDJ)

### 3. Fondo del Mar (Ocean Floor) - NEW!
**Theme**: Alien Ocean
**Description**: Dive into the depths of a bioluminescent alien ocean.

- **Video Background**: Seamless MP4 loop via MediaCodec
- **3D Abyssal Lurker**: Meshy AI-generated fish with depth illusion
- **Foreground Mask**: Animated plant overlay with ondulation shader
- **Bioluminescent Effects**: Cyan pulse on plants
- **Music-Reactive Equalizer**: 32-band visualization
- **Depth Illusion**: Fish scales/moves to simulate Z-depth

---

## Technical Architecture

### Rendering Pipeline
```
┌─────────────────────────────────────────────────────────────┐
│                    LiveWallpaperService                      │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                   WallpaperDirector                      ││
│  │  ┌─────────────┐  ┌──────────────┐  ┌────────────────┐ ││
│  │  │ SceneFactory│  │PanelRenderer │  │MusicVisualizer │ ││
│  │  └──────┬──────┘  └──────────────┘  └────────┬───────┘ ││
│  │         │                                     │         ││
│  │  ┌──────▼──────────────────────────────────▼───────┐  ││
│  │  │              WallpaperScene (abstract)           │  ││
│  │  │  • BatallaCosmicaScene                          │  ││
│  │  │  • OceanFloorScene                              │  ││
│  │  └─────────────────────────────────────────────────┘  ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### Video Wallpaper System (MediaCodec)
```
┌────────────────┐    ┌─────────────┐    ┌──────────────┐
│  MP4 in Assets │───▶│ MediaCodec  │───▶│SurfaceTexture│
└────────────────┘    │  Decoder    │    │  (OES)       │
                      └─────────────┘    └──────┬───────┘
                                                │
┌────────────────┐    ┌─────────────┐    ┌──────▼───────┐
│   GL Shader    │◀───│ OES Texture │◀───│ updateTexImg │
│  (fullscreen)  │    │   Sampler   │    │   per frame  │
└────────────────┘    └─────────────┘    └──────────────┘
```

### Music Visualization Flow
```
┌──────────────┐    ┌────────────────┐    ┌──────────────┐
│   Spotify/   │───▶│ AudioManager   │───▶│  Visualizer  │
│ YouTube Music│    │  Session 0     │    │   (FFT)      │
└──────────────┘    └────────────────┘    └──────┬───────┘
                                                  │
┌──────────────┐    ┌────────────────┐    ┌──────▼───────┐
│EqualizerBars │◀───│ 32 Frequency   │◀───│ processFft() │
│    DJ        │    │    Bands       │    │              │
└──────────────┘    └────────────────┘    └──────────────┘
```

---

## Session Accomplishments (Dec 26, 2024)

### Bug Fixes

#### 1. Music Auto-Resume Bug (CRITICAL)
**Problem**: Music from Spotify/YouTube Music would automatically resume at night without user action.

**Root Cause**: `MusicVisualizer.java` was sending `MEDIA_PLAY` commands via:
- Spotify broadcast: `com.spotify.mobile.android.ui.widget.PLAY`
- KeyEvent: `KEYCODE_MEDIA_PLAY` to AudioManager

**Solution**: Commented out all `sendMediaPlayCommand()` calls in both `initialize()` and `reconnect()` methods.

```java
// 🚫 DISABLED: Auto-resume removed
// handler.postDelayed(this::sendMediaPlayCommand, 800);
// handler.postDelayed(this::sendMediaPlayCommand, 1500);
// handler.postDelayed(this::sendMediaPlayCommand, 2500);
```

#### 2. Equalizer Freezing After App Switch
**Problem**: Equalizer bars would freeze when returning from another app.

**Root Cause**: Android lifecycle callbacks arrive out of order during app transitions:
```
13:41:16.264 → Resume (OK)
13:41:16.314 → Pause AGAIN (50ms later, no corresponding resume!)
```

**Solution**: Added auto-recovery in render loop:
```java
// In updateWallpaperMode():
if (!musicVisualizer.isEnabled()) {
    Log.d(TAG, "🔧 Auto-recovery: MusicVisualizer pausado durante render, reanudando...");
    musicVisualizer.resume();
}
```

#### 3. MusicVisualizer Resume Logic
**Problem**: Complex mode detection caused inconsistent resume behavior.

**Solution**: Simplified to always call `resume()` which handles reconnection automatically:
```java
// Before: Complex mode checks
// After: Simple and reliable
if (musicVisualizer != null) {
    musicVisualizer.resume();
}
```

### New Features

#### AbyssalLurker3D - 3D Fish with Depth Illusion
- Meshy AI-generated 3D model
- Smooth swimming animation
- Depth illusion via scale/Y-position interpolation
- Bioluminescent color scheme (purple/cyan)

---

## File Changes Summary

| File | Change |
|------|--------|
| `MusicVisualizer.java` | Disabled auto-resume, improved resume() |
| `WallpaperDirector.java` | Simplified resume, added auto-recovery |
| `OceanFloorScene.java` | Uses AbyssalLurker3D instead of AlienFishSprite |
| `AbyssalLurker3D.java` | NEW - 3D fish renderer |
| `abyssal_lurker.obj` | NEW - 3D model |
| `abyssal_lurker_texture.png` | NEW - Fish texture |

---

## Upcoming Features (Planned)

1. **Second Fish** - Add variety to ocean scene
2. **Plankton Particles** - Floating ambient particles
3. **Touch Food System** - Tap screen to drop food, fish swim to eat it

---

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Release AAB for Play Store
./gradlew bundleRelease
```

---

## Version Info

- **App Version**: 4.1.1 (code 11)
- **Min SDK**: 24
- **Target SDK**: 35
- **OpenGL ES**: 3.0
- **Branch**: beta1.0

---

*Generated with Claude Code - December 26, 2024*
