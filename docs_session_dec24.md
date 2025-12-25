# Session Notes - December 23-24, 2024

## Video Wallpaper "Fondo del Mar" + APK Optimization

---

## APK Optimization: 226 MB → 128 MB (-43%)

| Stage | Size | Savings |
|-------|------|---------|
| Original | 226 MB | - |
| After GLBs (7 files) | 170 MB | -56 MB |
| After Images Batch 2 (12 files) | 138 MB | -32 MB |
| After Images Batch 3 (5 files) | 131 MB | -7 MB |
| After ChristmasTree files + classes | **128 MB** | -3 MB |

### Deleted Files:
- **Filament GLBs**: santa.glb, santa_look.glb, santa_wave.glb, santa_walk.glb, terrain.glb, christmas_background.glb, christmas_tree.glb
- **Unused images**: christmas_background01.png, preview_universo.png, PBR textures (roughness, displacement, diffuse, normal), projectile sprites
- **Unused classes**: ChristmasTree.java, ChristmasScene.java

---

## NEW: Video Wallpaper System ("Fondo del Mar")

### Architecture (GRUBL-style):
```
MP4 Video → MediaPlayer → SurfaceTexture (OES) → OpenGL Shader → Screen
                                                      ↑
                                              AlienFishSprite + ForegroundMask
```

### New Files Created:
| File | Description |
|------|-------------|
| `video/VideoWallpaperRenderer.java` | MediaPlayer + SurfaceTexture + OES texture rendering (~270 lines) |
| `video/AlienFishSprite.java` | 2D procedural fish with depth illusion (size/Y/speed based on depth) |
| `video/ForegroundMask.java` | PNG overlay with animated shader (ondulation + cyan bioluminescent pulse) |
| `scenes/OceanFloorScene.java` | Orchestrates video + fish + mask layers (~107 lines) |

### Video Assets:
| File | Size | Description |
|------|------|-------------|
| `escena_fondoSC.mp4` | 15.5 MB | Ping-pong loop of alien ocean (10 sec, CapCut) |
| `foreground_plants.png` | ~500 KB | Mask for depth layering (plants in front) |
| `alien_ocean_wallpaper.mp4` | 4.0 MB | Original short loop (kept for reference) |

### Depth Layer System:
3 layers rendered in order:
1. **Video** (bottom) - Ocean background
2. **AlienFishSprite** (middle) - Swims between front plants and background
3. **ForegroundMask** (top) - Plants that occlude the fish

Fish depth illusion:
- `SIZE_CLOSE=0.18f` → `SIZE_FAR=0.06f`
- `Y_CLOSE=-0.4f` → `Y_FAR=0.2f`
- Speed also varies with depth (slower when far)

---

## Lifecycle Fixes for Video Wallpaper

### Problem:
Video stopped playing when switching apps or pausing music.

### Solutions Applied:

1. **`WallpaperDirector.setPlaying()`** - Completely disabled
   - Wallpaper no longer reacts to music play/pause
   - Was calling `switchToPanelMode()` which destroyed the video scene

2. **`WallpaperDirector.pause()`** - Modified
   - Active scenes are NOT destroyed, only paused
   - Added check: `if (!hasActiveScene) switchToPanelMode()`

3. **`VideoWallpaperRenderer.draw()`** - Auto-recovery
   - If video stops, automatically restarts: `mediaPlayer.start()`

---

## Known Issue (PENDING FIX)

### MiniStopButton capturing accidental touches

**Symptom:** Video stops unexpectedly when user touches screen or switches apps

**Root Cause:**
- `TouchRouter.handleWallpaperModeTouch()` checks `isStopButtonTouched(nx, ny)`
- MiniStopButton is invisible but intercepts touch events in WALLPAPER_MODE
- Button area might be too large or positioned incorrectly

**Stack Trace:**
```
TouchRouter.handleWallpaperModeTouch(TouchRouter.java:165)
 → onStopButtonTapped()
 → WallpaperDirector.switchToPanelMode()
 → SceneFactory.destroyCurrentScene()
 → OceanFloorScene.releaseSceneResources()
 → VideoWallpaperRenderer.release()
```

**Potential Fixes to Investigate:**
1. Reduce MiniStopButton hit area (currently `size * 1.5f`)
2. Disable MiniStopButton for video wallpapers
3. Move button to a different position

---

## Video Loop Best Practices Learned

### What Breaks Loops:
- ❌ Fish/creatures swimming across frame (pop back at loop restart)
- ❌ Zoom effects (frame 1 ≠ frame last)
- ❌ Any camera movement (pan, tilt, dolly)

### What Works:
- ✅ Floating particles (drift continuously)
- ✅ God rays / light beams (cyclic movement)
- ✅ Swaying plants / anemones (repetitive motion)
- ✅ **Ping-pong playback** (forward → reverse → forward) - eliminates jump entirely

### Tools Used:
- **CapCut** (free) - Created ping-pong loop easily
- **Gemini Image→Video** - Generated alien ocean concept
- **GIMP** - Cleaned up foreground mask PNG

---

## AI Prompts Used

### Gemini Image Prompt (Alien Ocean):
```
Bioluminescent alien ocean floor. Purple and cyan glowing tentacle plants.
Crystalline rock formations. Volcanic black sand. Twin suns visible through
water surface. StarCraft Zerg aesthetic. Deep sea atmosphere.
```

### Video Prompt:
```
Gentle swaying motion on alien plants. Floating particles. Subtle light ray
shifts. Static camera, no zoom. 8 seconds seamless loop.
```

---

## Git Commit

```
🌊 Video Wallpaper "Fondo del Mar" + Optimización APK

- VideoWallpaperRenderer: MediaPlayer + SurfaceTexture + OES texture
- OceanFloorScene: Video + pez + máscara de profundidad
- AlienFishSprite: Pez 2D con shader, sistema de profundidad
- ForegroundMask: PNG overlay con shader animado
- Optimización APK: 226 MB → 128 MB (-43%)
- Fixes para lifecycle de video wallpaper
```

**Branch:** beta1.0
**Commit:** f764d8a
