<div align="center">

# Black Hole Glow

### Live Wallpapers That Breathe, React & Glow

**OpenGL ES 3.0 | Music-Reactive | 22 Scenes | Zero Lag**

[![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![API](https://img.shields.io/badge/API-24%E2%80%9335-blue)](https://developer.android.com/about/versions)
[![OpenGL](https://img.shields.io/badge/OpenGL%20ES-3.0-orange)](https://www.khronos.org/opengles/)
[![Java](https://img.shields.io/badge/Java-11-red?logo=openjdk&logoColor=white)](https://openjdk.org)
[![Version](https://img.shields.io/badge/version-5.3.0-blueviolet)](https://github.com/DraKenZaMaNosKe/blackholeglow/releases)

---

*Live wallpapers powered by real-time shaders, video compositing, and music visualization.*
*From anime battles to cosmic portals — every wallpaper is a tiny GPU-powered movie on your home screen.*

</div>

---

## What Is This?

**Black Hole Glow** is an Android live wallpaper engine that renders real-time OpenGL scenes on your home screen. Each wallpaper combines layered video, procedural shaders, 3D models, and a music-reactive equalizer — all optimized to run at 60fps on budget phones.

```
User sets wallpaper → GPU renders scene in real-time → Music plays → Equalizer reacts → Magic
```

### Key Features

- **22 handcrafted scenes** — anime, gaming, cosmic, horror, pixel art
- **Music-reactive equalizer** — bars dance to whatever you're listening to
- **3D clock & battery** — always visible, always styled to the scene
- **Runs on 3GB RAM phones** — tested on Samsung Galaxy A15 (Mali-G52)
- **Cloud assets** — videos & models stream from Supabase, keeping the APK light
- **Zero Kotlin** — pure Java 11, every line

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Android OS                            │
│  WallpaperService ──► LiveWallpaperService               │
└────────────┬────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────┐
│  SceneRenderer (GL Thread)                               │
│  ┌──────────┐  ┌──────────────┐  ┌───────────────────┐  │
│  │ SceneFactory │  ResourcePreloader │  WallpaperDirector│  │
│  │ 22 scenes │  │ async download │  │ panel + transitions│ │
│  └─────┬────┘  └──────┬───────┘  └─────────┬─────────┘  │
│        │              │                     │            │
│        ▼              ▼                     ▼            │
│  ┌─────────────────────────────────────────────────┐     │
│  │              WallpaperScene                      │     │
│  │  ┌─────────┐ ┌──────────┐ ┌─────────┐ ┌──────┐ │     │
│  │  │Video    │ │Equalizer │ │Clock3D  │ │Battery│ │     │
│  │  │Renderer │ │BarsDJ    │ │         │ │3D     │ │     │
│  │  └─────────┘ └──────────┘ └─────────┘ └──────┘ │     │
│  └─────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────┐
│  Supabase Storage (CDN)                                  │
│  wallpaper-videos/ │ wallpaper-images/ │ wallpaper-models/│
└─────────────────────────────────────────────────────────┘
```

### Pipeline

| Step | Component | What It Does |
|------|-----------|-------------|
| 1 | `LiveWallpaperService` | Android binds to this; creates GL surface |
| 2 | `SceneRenderer` | Manages GL context, frame loop, scene lifecycle |
| 3 | `SceneFactory` | Maps scene name → scene class (22 registered) |
| 4 | `ResourcePreloader` | Downloads videos/images/models from Supabase before scene starts |
| 5 | `WallpaperScene` | Abstract base — each scene implements `init()`, `update()`, `draw()` |
| 6 | `WallpaperDirector` | Orchestrates panel mode, transitions, control overlay |

### Scene Types

| Type | Base Class | Rendering | Examples |
|------|-----------|-----------|---------|
| **Video** | `BaseVideoScene` | MediaCodec → SurfaceTexture → GL quad | Goku, Neon City, Gatito DJ |
| **Parallax** | `BaseParallaxScene` | Multi-layer billboards + gyroscope | Zelda BOTW, Ken |
| **Shader** | `WallpaperScene` | Pure GLSL fragment shaders | Pyralis, The Eye |
| **Hybrid** | `BaseVideoScene` + OBJ | Video background + 3D models | Abyssia, Saint Seiya |

---

## Wallpaper Catalog

> All 22 scenes. All free. All music-reactive.

| # | Scene | Theme | Weight | Style |
|---|-------|-------|--------|-------|
| 1 | **Abyssia** | Alien bioluminescent ocean | HEAVY | Video + 3D models |
| 2 | **Pyralis** | Cosmic fire portal | MEDIUM | Shader + particles |
| 3 | **Goku** | Dragon Ball Kamehameha | LIGHT | Video |
| 4 | **Adventure Time** | Campfire with Finn & Jake | LIGHT | Video |
| 5 | **Neon City** | Synthwave DeLorean | MEDIUM | Video + 3D |
| 6 | **Saint Seiya** | Knights of the Zodiac | MEDIUM | Video + 3D |
| 7 | **The Walking Dead** | Zombie cemetery | MEDIUM | Video |
| 8 | **Zelda BOTW** | Hyrule parallax landscape | MEDIUM | Parallax + gyroscope |
| 9 | **Superman** | Man of Steel | LIGHT | Video |
| 10 | **Attack on Titan** | Eren vs Colossal Titan | LIGHT | Video |
| 11 | **Black Spider** | Horror spider | LIGHT | Video |
| 12 | **Lost Atlantis** | Submerged temple | LIGHT | Video |
| 13 | **The Human Predator** | Warrior vs lion | LIGHT | Video |
| 14 | **Moonlit Cat** | Black cat under the moon | MEDIUM | Video |
| 15 | **Frieza Death Beam** | Dragon Ball Z | MEDIUM | Video |
| 16 | **Ken** | Street Fighter pixel art | LIGHT | Parallax |
| 17 | **Scorpion** | Mortal Kombat | LIGHT | Video |
| 18 | **Tren Nocturno** | Pixel art night train | LIGHT | Video |
| 19 | **The Eye** | Mysterious iris | LIGHT | Video |
| 20 | **Gatito** | Cute sleeping cat | LIGHT | Video |
| 21 | **Gatito DJ** | Dancing DJ cat | LIGHT | Video |
| 22 | **Pixel City** | Retro neon cityscape | LIGHT | Video |

**Scene Weight** drives compatibility diagnostics:
- **LIGHT** (~50MB RAM) — runs on anything
- **MEDIUM** (~100MB RAM) — smooth on 4GB+ devices
- **HEAVY** (~180MB RAM) — needs 6GB+ for full quality

---

## Tech Stack

| Layer | Technology | Why |
|-------|-----------|-----|
| **Graphics** | OpenGL ES 3.0 | Instanced rendering, MRT, required for shader effects |
| **Video** | MediaCodec + SurfaceTexture | Hardware-decoded video as GL texture — zero CPU copy |
| **Audio** | Android Visualizer API | Real-time FFT for equalizer bars |
| **3D Models** | Custom OBJ loader | Lightweight, no bloated libraries |
| **Shaders** | GLSL ES 3.00 | Procedural effects, post-processing, pixel art |
| **Cloud** | Supabase Storage | CDN-backed asset delivery (videos, textures, models) |
| **Auth** | Firebase Auth + Google Sign-In | User accounts |
| **Analytics** | Firebase Analytics | Usage tracking |
| **Config** | Firebase Remote Config | Dynamic feature flags |
| **Ads** | Google AdMob | Monetization layer |
| **Images** | Glide | Preview loading & caching |
| **Build** | Gradle Kotlin DSL | `libs.versions.toml` version catalog |

---

## Project Structure

```
blackholeglow/
├── app/src/main/
│   ├── java/com/secret/blackholeglow/
│   │   ├── LiveWallpaperService.java    # Entry point — Android wallpaper service
│   │   ├── EqualizerBarsDJ.java         # Music visualizer (93KB, the beast)
│   │   ├── Clock3D.java                 # Real-time 3D clock overlay
│   │   ├── Battery3D.java              # Battery indicator overlay
│   │   │
│   │   ├── core/                        # Engine core
│   │   │   ├── SceneFactory.java        #   Scene registry (name → class)
│   │   │   ├── SceneRenderer.java       #   GL frame loop & lifecycle
│   │   │   ├── ResourcePreloader.java   #   Async asset downloader
│   │   │   └── WallpaperDirector.java   #   Panel mode & transitions
│   │   │
│   │   ├── scenes/                      # 30 scene implementations
│   │   │   ├── BaseVideoScene.java      #   Base for video wallpapers
│   │   │   ├── BaseParallaxScene.java   #   Base for parallax wallpapers
│   │   │   ├── WallpaperScene.java      #   Abstract scene interface
│   │   │   ├── GokuScene.java           #   Dragon Ball Kamehameha
│   │   │   ├── ZeldaParallaxScene.java  #   Zelda BOTW parallax
│   │   │   └── ...                      #   18 more scenes
│   │   │
│   │   ├── systems/                     # App-wide managers
│   │   │   ├── WallpaperCatalog.java    #   Scene registry + metadata
│   │   │   ├── SubscriptionManager.java #   FREE/PREMIUM/VIP tiers
│   │   │   ├── MusicSystem.java         #   Audio integration
│   │   │   ├── EventBus.java            #   Inter-component messaging
│   │   │   └── GLStateManager.java      #   OpenGL state tracking
│   │   │
│   │   ├── video/                       # Video pipeline
│   │   │   ├── VideoConfig.java         #   URL registry for videos
│   │   │   ├── VideoRenderer.java       #   MediaCodec → GL texture
│   │   │   └── VideoDownloadManager.java#   Async video caching
│   │   │
│   │   ├── effects/                     # Visual effects
│   │   │   ├── ProceduralPanelBackground.java  # Starfield shader
│   │   │   ├── PixelationTransition.java       # Scene transition effect
│   │   │   └── OrbixGreeting.java              # Welcome animation
│   │   │
│   │   ├── activities/                  # UI screens
│   │   │   ├── SplashActivity.java      #   Splash → Main
│   │   │   ├── MainActivity.java        #   Catalog + drawer
│   │   │   └── WallpaperPreviewActivity.java  # Live preview
│   │   │
│   │   ├── models/                      # Data classes
│   │   │   ├── WallpaperItem.java       #   Catalog entry model
│   │   │   ├── WallpaperTier.java       #   FREE/PREMIUM/VIP enum
│   │   │   └── SceneWeight.java         #   LIGHT/MEDIUM/HEAVY enum
│   │   │
│   │   └── gl3/                         # GL 3.0 utilities
│   │       ├── ShaderUtils.java         #   Compile & link shaders
│   │       └── TextureManager.java      #   Lazy-load texture cache
│   │
│   ├── assets/
│   │   ├── shaders/gl3/                 # GLSL shaders
│   │   └── *.obj, *.mtl                 # Bundled 3D models
│   │
│   └── res/
│       ├── drawable/                    # 32 preview images (WebP)
│       ├── layout/                      # 10 XML layouts
│       └── values/                      # Strings, themes, colors
│
├── gradle/libs.versions.toml            # Dependency versions
├── CLAUDE.md                            # AI assistant instructions
└── README.md                            # You are here
```

**169 Java classes** | **0 Kotlin files** | **22 wallpaper scenes**

---

## Build & Run

### Prerequisites

- Android Studio Hedgehog+ (or just Gradle CLI)
- JDK 11+
- Android SDK 35
- A device or emulator with OpenGL ES 3.0

### Commands

```bash
# Debug build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Release bundle (Play Store)
./gradlew bundleRelease
```

### Required Files (not in repo)

| File | Location | Purpose |
|------|----------|---------|
| `google-services.json` | `app/` | Firebase config (get from Firebase Console) |
| `keystore.jks` | project root | Release signing key |

---

## Adding a New Scene

> Full guide in [`CLAUDE.md`](CLAUDE.md). Quick overview:

```
1. Upload video to Supabase          →  wallpaper-videos/my_scene.mp4
2. Register in VideoConfig.java      →  filename → URL + metadata
3. Create MyScene extends BaseVideoScene  →  5 abstract methods
4. Register in SceneFactory.java     →  registerScene("MY_SCENE", MyScene.class)
5. Add to WallpaperCatalog.java      →  catalog.add(new WallpaperItem.Builder(...))
6. Register in ResourcePreloader     →  4 switch blocks
7. Add preview image                 →  res/drawable/preview_my_scene.webp
8. Build & test                      →  ./gradlew installDebug
```

Each scene automatically gets: video renderer, equalizer overlay, 3D clock, battery indicator, and music reactivity.

---

## Performance

Benchmarked on **Samsung Galaxy A15** (Mali-G52, 3GB RAM, Android 14):

| Metric | Target | Actual |
|--------|--------|--------|
| Frame time | < 16ms | 8-12ms (LIGHT), 12-15ms (MEDIUM) |
| RAM usage | < 200MB | ~120MB (LIGHT), ~180MB (HEAVY) |
| Battery drain | < 3%/hr | ~2%/hr idle, ~4%/hr with music |
| APK size | < 15MB | ~12MB (assets on Supabase) |
| Cold start | < 3s | ~2s to first frame |

### Optimizations

- **Zero-copy video**: `MediaCodec` decodes directly to `SurfaceTexture` — no CPU pixel copy
- **Lazy texture loading**: textures load on first use, not at scene init
- **Scene weight system**: heavy scenes auto-downgrade on low-RAM devices
- **Glyph atlas**: `Clock3D` renders all digits from a single pre-baked texture — zero allocations per frame
- **Instanced particles**: single draw call for hundreds of particles via `glDrawArraysInstanced`
- **Procedural shaders**: panel background uses pure math (hash + sin) — no texture lookups

---

## Music Visualization

The `EqualizerBarsDJ` (93KB, the largest class in the project) is a full-featured music visualizer:

- **Real-time FFT** from Android's Visualizer API
- **Peak detection** with decay curves
- **Beat detection** for flash effects
- **Spark particles** on bass hits
- **10+ color themes** matched to each scene
- **Smooth interpolation** between frames — no stuttering

It activates automatically when music plays from any app (Spotify, YouTube, etc.) via `MusicNotificationListener`.

---

## Cloud Architecture

Assets live on **Supabase Storage**, keeping the APK light:

```
Supabase CDN
├── wallpaper-videos/     # MP4 loops (720p-1080p, ~5-15MB each)
├── wallpaper-images/     # Textures & layer PNGs
└── wallpaper-models/     # OBJ + MTL 3D models
```

- **First launch**: scene downloads its assets in background
- **Cached locally**: subsequent launches are instant
- **Version-controlled**: `VideoConfig` / `ImageConfig` / `ModelConfig` map filenames to URLs
- **Fallback**: if download fails, scene shows loading placeholder

---

## License

All rights reserved. This is a proprietary project.

---

<div align="center">

**Built with pure Java, raw OpenGL, and an unhealthy obsession with frame rates.**

*Black Hole Glow v5.3.0*

</div>
