# Black Hole Glow - Arquitectura del Proyecto

**Versión**: 4.0.1
**Última actualización**: Diciembre 2024
**Paquete**: `com.secret.blackholeglow`
**Plataforma**: Android (Java)
**OpenGL**: ES 2.0 / 3.0

---

## Tabla de Contenidos

1. [Resumen Ejecutivo](#1-resumen-ejecutivo)
2. [Estructura de Carpetas](#2-estructura-de-carpetas)
3. [Arquitectura General](#3-arquitectura-general)
4. [Diagramas UML](#4-diagramas-uml)
5. [Catálogo de Clases](#5-catálogo-de-clases)
6. [Clases Obsoletas](#6-clases-obsoletas)
7. [Flujo de Datos](#7-flujo-de-datos)
8. [Guía para IAs](#8-guía-para-ias)

---

## 1. Resumen Ejecutivo

### ¿Qué es Black Hole Glow?

**Black Hole Glow** es un **Live Wallpaper** para Android que renderiza escenas 3D animadas usando OpenGL ES. El usuario puede seleccionar diferentes escenas temáticas que se muestran como fondo de pantalla animado.

### Características Principales

| Feature | Descripción |
|---------|-------------|
| Escenas 3D | Renderizado OpenGL ES 2.0/3.0 |
| Música Reactiva | Visualización de audio en tiempo real |
| Sistema de Combate | Meteoritos, naves, escudos (escena Batalla Cósmica) |
| Nieve Interactiva | Partículas GPU (escena Navideña) |
| IA Gemini | Saludos personalizados con Gemini AI |
| Firebase | Autenticación, estadísticas, configuración remota |

### Escenas Disponibles (Diciembre 2024)

| Escena | Estado | Clase |
|--------|--------|-------|
| Batalla Cósmica | ✅ Activa | `BatallaCosmicaScene` |
| Bosque Navideño | ✅ Activa | `ChristmasScene` |
| Ocean Pearl | 🔜 Coming Soon | `OceanPearlScene` |
| La Mansión | 🔜 Coming Soon | No implementada |

---

## 2. Estructura de Carpetas

```
blackholeglow/
├── app/
│   ├── src/main/
│   │   ├── java/com/secret/blackholeglow/
│   │   │   ├── activities/          # Activities de Android
│   │   │   ├── adapters/            # Adaptadores RecyclerView
│   │   │   ├── christmas/           # Componentes escena navideña
│   │   │   ├── core/                # Pipeline de renderizado
│   │   │   ├── effects/             # Efectos post-proceso
│   │   │   ├── fragments/           # Fragments UI
│   │   │   ├── gl3/                 # Utilidades OpenGL 3.0
│   │   │   ├── models/              # Modelos de datos
│   │   │   ├── opengl/              # Componentes OpenGL
│   │   │   ├── scenes/              # Escenas de wallpaper
│   │   │   ├── sharing/             # Sistema de compartir música
│   │   │   ├── systems/             # Sistemas globales
│   │   │   ├── ui/                  # Componentes UI custom
│   │   │   ├── util/                # Utilidades (OBJ loader, etc.)
│   │   │   ├── wallpaper/           # Gestión de wallpapers
│   │   │   └── *.java               # Clases del paquete raíz
│   │   ├── assets/
│   │   │   ├── shaders/             # Shaders GLSL
│   │   │   └── *.obj               # Modelos 3D
│   │   └── res/
│   │       ├── drawable/            # Imágenes y gradientes
│   │       ├── layout/              # Layouts XML
│   │       └── values/              # Strings, colors, styles
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml           # Versiones de dependencias
├── CLAUDE.md                        # Documentación para Claude
└── build.gradle.kts
```

---

## 3. Arquitectura General

### Diagrama de Capas

```
┌─────────────────────────────────────────────────────────────────┐
│                      CAPA ANDROID                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │ Activities   │  │  Fragments   │  │ LiveWallpaperService │  │
│  │ - Splash     │  │ - Wallpaper  │  │ (Entry Point)        │  │
│  │ - Main       │  │   List       │  │                      │  │
│  │ - Preview    │  │              │  │                      │  │
│  └──────────────┘  └──────────────┘  └──────────┬───────────┘  │
├─────────────────────────────────────────────────┼───────────────┤
│                      CAPA CORE                  │               │
│  ┌──────────────────────────────────────────────▼────────────┐ │
│  │                   WallpaperDirector                        │ │
│  │              (GLSurfaceView.Renderer)                      │ │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐  │ │
│  │  │ RenderMode  │ │ PanelMode   │ │    SceneFactory     │  │ │
│  │  │ Controller  │ │ Renderer    │ │                     │  │ │
│  │  └─────────────┘ └─────────────┘ └─────────────────────┘  │ │
│  │  ┌─────────────┐ ┌─────────────┐                          │ │
│  │  │ SongSharing │ │ TouchRouter │                          │ │
│  │  │ Controller  │ │             │                          │ │
│  │  └─────────────┘ └─────────────┘                          │ │
│  └───────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│                      CAPA SCENES                                │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │                  WallpaperScene (Abstracta)                │ │
│  └───────────────────────────────────────────────────────────┘ │
│           ▲                    ▲                    ▲           │
│  ┌────────┴───────┐   ┌───────┴────────┐   ┌──────┴───────┐   │
│  │BatallaCosmicaS.│   │ ChristmasScene │   │OceanPearlS.  │   │
│  └────────────────┘   └────────────────┘   └──────────────┘   │
├─────────────────────────────────────────────────────────────────┤
│                      CAPA OPENGL                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │BaseShaderProg│  │CameraControll│  │  TextureManager      │  │
│  │              │  │    er        │  │                      │  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                   CAPA SCENE OBJECTS                            │
│  Planeta, Meteorito, Spaceship, ChristmasTree, SnowParticles.. │
└─────────────────────────────────────────────────────────────────┘
```

### Patrón Actor Model

El proyecto usa el **patrón Actor** donde cada componente tiene una responsabilidad única:

| Actor | Responsabilidad |
|-------|-----------------|
| `WallpaperDirector` | Orquesta todo el sistema, implementa `GLSurfaceView.Renderer` |
| `RenderModeController` | Máquina de estados (PANEL → LOADING → WALLPAPER) |
| `PanelModeRenderer` | Renderiza UI del panel de control |
| `SceneFactory` | Crea y destruye escenas |
| `SongSharingController` | Gestiona música y Gemini AI |
| `TouchRouter` | Distribuye eventos táctiles |

---

## 4. Diagramas UML

### 4.1 Diagrama de Estados - RenderModeController

```
                    ┌─────────────┐
                    │   START     │
                    └──────┬──────┘
                           │
                           ▼
              ┌────────────────────────┐
              │      PANEL_MODE        │
              │  (Botón Play visible)  │
              └───────────┬────────────┘
                          │ startLoading()
                          ▼
              ┌────────────────────────┐
              │     LOADING_MODE       │
              │  (Barra de progreso)   │
              └───────────┬────────────┘
                          │ activateWallpaper()
                          ▼
              ┌────────────────────────┐
      ┌──────►│    WALLPAPER_MODE      │◄──────┐
      │       │   (Escena 3D activa)   │       │
      │       └───────────┬────────────┘       │
      │                   │                    │
      │   stopWallpaper() │                    │ goDirectToWallpaper()
      │                   │                    │ (Preview Mode)
      │                   ▼                    │
      │       ┌────────────────────────┐       │
      └───────│      PANEL_MODE        │───────┘
              └────────────────────────┘
```

### 4.2 Diagrama de Clases - Core

```
┌─────────────────────────────────────────────────────────────┐
│                    <<interface>>                             │
│                 GLSurfaceView.Renderer                       │
│  ───────────────────────────────────────────────────────────│
│  + onSurfaceCreated(GL10, EGLConfig)                        │
│  + onSurfaceChanged(GL10, int, int)                         │
│  + onDrawFrame(GL10)                                        │
└─────────────────────────────────────────────────────────────┘
                              ▲
                              │ implements
┌─────────────────────────────┴───────────────────────────────┐
│                    WallpaperDirector                         │
│  ───────────────────────────────────────────────────────────│
│  - modeController: RenderModeController                      │
│  - panelRenderer: PanelModeRenderer                          │
│  - sceneFactory: SceneFactory                                │
│  - songSharing: SongSharingController                        │
│  - touchRouter: TouchRouter                                  │
│  - camera: CameraController                                  │
│  - textureManager: TextureManager                            │
│  - musicVisualizer: MusicVisualizer                          │
│  ───────────────────────────────────────────────────────────│
│  + onSurfaceCreated()                                        │
│  + onSurfaceChanged()                                        │
│  + onDrawFrame()                                             │
│  + onTouchEvent(MotionEvent): boolean                        │
│  + changeScene(String)                                       │
│  + pause() / resume() / release()                            │
└─────────────────────────────────────────────────────────────┘
                              │
           ┌──────────────────┼──────────────────┐
           │                  │                  │
           ▼                  ▼                  ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│RenderModeContro.│ │  SceneFactory   │ │PanelModeRenderer│
│─────────────────│ │─────────────────│ │─────────────────│
│- currentMode    │ │- registeredS.   │ │- playButton     │
│- isPreviewMode  │ │- currentScene   │ │- loadingBar     │
│─────────────────│ │─────────────────│ │─────────────────│
│+ startLoading() │ │+ createScene()  │ │+ drawPanelMode()│
│+ activateWall.()│ │+ destroyScene() │ │+ drawLoading()  │
│+ stopWallpaper()│ │+ updateScene()  │ │+ drawOverlay()  │
└─────────────────┘ └─────────────────┘ └─────────────────┘
```

### 4.3 Diagrama de Clases - Scenes

```
┌─────────────────────────────────────────────────────────────┐
│                 <<abstract>>                                 │
│                 WallpaperScene                               │
│  ───────────────────────────────────────────────────────────│
│  # context: Context                                          │
│  # textureManager: TextureManager                            │
│  # camera: CameraController                                  │
│  # sceneObjects: List<SceneObject>                           │
│  # isLoaded, isPaused, isDisposed: boolean                   │
│  ───────────────────────────────────────────────────────────│
│  + {abstract} getName(): String                              │
│  + {abstract} getDescription(): String                       │
│  + {abstract} getPreviewResourceId(): int                    │
│  # {abstract} setupScene()                                   │
│  # {abstract} releaseSceneResources()                        │
│  ───────────────────────────────────────────────────────────│
│  + onCreate(Context, TextureManager, CameraController)       │
│  + onResume() / onPause() / onDestroy()                      │
│  + update(float deltaTime)                                   │
│  + draw()                                                    │
│  + onTouchEvent(float, float, int): boolean                  │
└─────────────────────────────────────────────────────────────┘
              ▲                    ▲                    ▲
              │                    │                    │
┌─────────────┴────┐  ┌────────────┴───┐  ┌────────────┴───┐
│BatallaCosmicaS.  │  │ ChristmasScene │  │ OceanPearlScene│
│──────────────────│  │────────────────│  │────────────────│
│- tierra          │  │- christmasTree │  │- ocean         │
│- meteorShower    │  │- snowParticles │  │- pearl         │
│- spaceship3D     │  │- snowGround    │  │- fish          │
│- musicIndicator  │  │- background    │  │                │
│──────────────────│  │────────────────│  │────────────────│
│+ setupScene()    │  │+ setupScene()  │  │+ setupScene()  │
│+ updateMusicBands│  │+ intensifySnow │  │                │
└──────────────────┘  └────────────────┘  └────────────────┘
```

### 4.4 Diagrama de Clases - Scene Objects

```
┌─────────────────────────────────────────────────────────────┐
│                    <<interface>>                             │
│                     SceneObject                              │
│  ───────────────────────────────────────────────────────────│
│  + update(float deltaTime)                                   │
│  + draw()                                                    │
└─────────────────────────────────────────────────────────────┘
              ▲
              │ implements
              │
┌─────────────┴─────────────────────────────────────────────┐
│                   BaseShaderProgram                        │
│  ─────────────────────────────────────────────────────────│
│  # programId: int                                          │
│  # context: Context                                        │
│  ─────────────────────────────────────────────────────────│
│  + setTime(float phase)                                    │
│  + setMvpAndResolution()                                   │
│  # loadShader(String vertexPath, String fragmentPath)      │
└───────────────────────────────────────────────────────────┘
              ▲
              │ extends
    ┌─────────┼─────────┬─────────────┬──────────────┐
    │         │         │             │              │
    ▼         ▼         ▼             ▼              ▼
┌───────┐ ┌───────┐ ┌─────────┐ ┌──────────┐ ┌───────────┐
│Planeta│ │Meteori│ │Christmas│ │SnowParti.│ │Spaceship3D│
│       │ │  to   │ │  Tree   │ │          │ │           │
└───────┘ └───────┘ └─────────┘ └──────────┘ └───────────┘


┌─────────────────────────────────────────────────────────────┐
│                    <<interface>>                             │
│                     CameraAware                              │
│  ───────────────────────────────────────────────────────────│
│  + setCameraController(CameraController camera)              │
└─────────────────────────────────────────────────────────────┘
              ▲
              │ implements (la mayoría de SceneObjects)
```

### 4.5 Diagrama de Secuencia - Render Loop

```
┌─────────┐    ┌─────────────┐    ┌───────────┐    ┌───────────┐
│GLThread │    │WallpaperDir.│    │SceneFactory│   │WallpaperS.│
└────┬────┘    └──────┬──────┘    └─────┬─────┘    └─────┬─────┘
     │                │                 │                │
     │ onDrawFrame()  │                 │                │
     │───────────────►│                 │                │
     │                │                 │                │
     │                │ getCurrentMode()│                │
     │                │────────────────►│                │
     │                │                 │                │
     │                │ [WALLPAPER_MODE]│                │
     │                │                 │                │
     │                │ updateCurrentScene(dt)           │
     │                │────────────────►│                │
     │                │                 │ update(dt)     │
     │                │                 │───────────────►│
     │                │                 │                │
     │                │                 │  [for each obj]│
     │                │                 │  obj.update(dt)│
     │                │                 │                │
     │                │ drawCurrentScene()               │
     │                │────────────────►│                │
     │                │                 │ draw()         │
     │                │                 │───────────────►│
     │                │                 │                │
     │                │                 │  [for each obj]│
     │                │                 │  obj.draw()    │
     │                │                 │                │
     │◄───────────────│                 │                │
     │   (frame done) │                 │                │
```

---

## 5. Catálogo de Clases

### 5.1 Activities (5 clases)

| Clase | Archivo | Propósito |
|-------|---------|-----------|
| `SplashActivity` | activities/SplashActivity.java | Pantalla de inicio con logo animado |
| `MainActivity` | activities/MainActivity.java | Pantalla principal con NavigationDrawer |
| `WallpaperPreviewActivity` | activities/WallpaperPreviewActivity.java | Preview antes de aplicar wallpaper |
| `WallpaperLoadingActivity` | activities/WallpaperLoadingActivity.java | Pantalla de carga |
| `GeminiChatActivity` | activities/GeminiChatActivity.java | Chat con Gemini AI |

### 5.2 Core - Pipeline de Renderizado (7 clases)

| Clase | Archivo | Propósito |
|-------|---------|-----------|
| `WallpaperDirector` | core/WallpaperDirector.java | **Director principal** - Orquesta todo el renderizado, implementa GLSurfaceView.Renderer |
| `RenderModeController` | core/RenderModeController.java | Máquina de estados (PANEL/LOADING/WALLPAPER) |
| `PanelModeRenderer` | core/PanelModeRenderer.java | Renderiza UI del panel de control |
| `SceneFactory` | core/SceneFactory.java | Factory para crear/destruir escenas |
| `SongSharingController` | core/SongSharingController.java | Gestión de música compartida y Gemini AI |
| `TouchRouter` | core/TouchRouter.java | Distribuye eventos táctiles al componente correcto |
| `ResourcePreloader` | core/ResourcePreloader.java | Precarga recursos en background |

### 5.3 Scenes - Escenas (8 clases)

| Clase | Archivo | Propósito |
|-------|---------|-----------|
| `WallpaperScene` | scenes/WallpaperScene.java | **Clase base abstracta** para todas las escenas |
| `BatallaCosmicaScene` | scenes/BatallaCosmicaScene.java | Escena de batalla espacial con meteoritos |
| `ChristmasScene` | scenes/ChristmasScene.java | Bosque navideño con nieve |
| `OceanPearlScene` | scenes/OceanPearlScene.java | Escena submarina (Coming Soon) |
| `SceneConstants` | scenes/SceneConstants.java | Constantes configurables por escena |
| `SceneManager` | scenes/SceneManager.java | Gestiona cambios entre escenas |
| `SceneCallbacks` | scenes/SceneCallbacks.java | Interface de callbacks |
| `Disposable` | scenes/Disposable.java | Interface para liberar recursos |

### 5.4 OpenGL Base (clases fundamentales)

| Clase | Archivo | Propósito |
|-------|---------|-----------|
| `BaseShaderProgram` | BaseShaderProgram.java | Clase base para shaders, compila y linkea GLSL |
| `CameraController` | CameraController.java | Sistema de cámara multi-modo (Orbit, FPS, Cinematic) |
| `TextureManager` | TextureManager.java | Cache lazy-loading de texturas OpenGL |
| `ShaderUtils` | ShaderUtils.java | Utilidades para cargar y compilar shaders |
| `SceneObject` | SceneObject.java | **Interface** - Todo objeto 3D debe implementar update() y draw() |
| `CameraAware` | CameraAware.java | **Interface** - Objetos que necesitan referencia a la cámara |
| `TextureLoader` | TextureLoader.java | Interface para cargar texturas |

### 5.5 Objetos 3D - Espacio (~30 clases)

#### Planetas y Cuerpos Celestes
| Clase | Propósito |
|-------|-----------|
| `Planeta` | Planeta genérico con órbita, rotación y textura |
| `TierraLiveHD` | Tierra con texturas de alta definición |
| `TierraMeshy` | Tierra con modelo de Meshy AI |
| `SolMeshy` | Sol con modelo 3D |
| `SolRealista` | Sol procedural con corona y llamaradas |
| `SaturnoMeshy` | Saturno con anillos |

#### Meteoritos y Partículas
| Clase | Propósito |
|-------|-----------|
| `Meteorito` | Meteorito individual |
| `AsteroideRealista` | Asteroide con texturas realistas |
| `MeteorShower` | Sistema de lluvia de meteoros |
| `MeteorTrail` | Estela de fuego del meteorito |
| `MeteorExplosion` | Explosión al impactar |
| `MeteorCountdownBar` | Barra de countdown de oleadas |
| `SpaceComets` | Cometas espaciales |
| `SpaceDust` | Polvo cósmico |

#### Naves Espaciales
| Clase | Propósito |
|-------|-----------|
| `Spaceship` | Nave espacial base |
| `Spaceship3D` | Nave 3D con modelo OBJ |
| `UfoAttacker` | OVNI atacante con IA |
| `UfoScout` | OVNI explorador |
| `DefenderShip` | Nave defensora humana |
| `HumanInterceptor` | Interceptor humano |
| `SpaceStation` | Estación espacial |

#### Fondos y Ambiente
| Clase | Propósito |
|-------|-----------|
| `UniverseBackground` | Fondo de estrellas del universo |
| `StarryBackground` | Cielo estrellado |
| `ParallaxStars` | Estrellas con efecto parallax |
| `BackgroundStars` | Estrellas de fondo simples |
| `SpaceBattleBackground` | Fondo para escena de batalla |
| `SolarWinds` | Vientos solares visuales |

### 5.6 Objetos 3D - Navidad (4 clases)

| Clase | Archivo | Propósito |
|-------|---------|-----------|
| `ChristmasTree` | christmas/ChristmasTree.java | Árbol 3D con modelo OBJ, animación de viento |
| `ChristmasBackground` | christmas/ChristmasBackground.java | Fondo de bosque nevado |
| `SnowGround` | christmas/SnowGround.java | Suelo con textura de nieve |
| `SnowParticles` | christmas/SnowParticles.java | Sistema de partículas de nieve GPU |

### 5.7 Sistema de Combate (~15 clases)

| Clase | Propósito |
|-------|-----------|
| `CollisionSystem` | Detección de colisiones entre objetos |
| `EarthShield` | Escudo protector de la Tierra |
| `Laser` | Proyectil láser |
| `PlasmaBeamWeapon` | Arma de rayo plasma |
| `PlasmaExplosion` | Explosión de plasma |
| `Projectile` | Proyectil genérico |
| `ProjectilePool` | Object pool para proyectiles (optimización) |
| `PlayerWeapon` | Sistema de armas del jugador |
| `TargetingSystem` | Sistema de apuntado automático |
| `TargetReticle` | Retícula visual de objetivo |
| `ForceField` | Campo de fuerza |
| `EnemyAI` | IA de enemigos |
| `PlayerAI` | IA automática del jugador |
| `BattleHUD` | HUD de batalla |
| `BattleConstants` | Constantes de balance del combate |

### 5.8 Sistema de Música (~10 clases)

| Clase | Propósito |
|-------|-----------|
| `MusicVisualizer` | Análisis de audio FFT en tiempo real |
| `MusicReactive` | Interface para objetos reactivos a música |
| `MusicStars` | Estrellas que pulsan con el beat |
| `MusicIndicator` | Ecualizador visual con barras |
| `EqualizerBarsDJ` | Barras de ecualizador estilo DJ |
| `MusicSystem` | Sistema central de gestión de música |
| `MusicNotificationListener` | Escucha notificaciones de reproductores |

### 5.9 UI y HUD (~15 clases)

| Clase | Propósito |
|-------|-----------|
| `HealthBar` / `HPBar` | Barra de vida |
| `ComboBar` | Barra de combo |
| `BatteryPowerBar` | Indicador de batería del dispositivo |
| `LoadingBar` | Barra de carga animada |
| `HolographicTitle` | Título con efecto holográfico |
| `ArcadeTitle` | Título estilo arcade |
| `ArcadeFooter` | Footer estilo arcade |
| `PlayPauseButton` | Botón play/pause animado |
| `FireButton` | Botón de disparo |
| `MiniStopButton` | Botón pequeño para detener wallpaper |
| `SimpleTextRenderer` | Renderizador de texto OpenGL |
| `GreetingText` | Texto de saludo |
| `OrbixGreeting` | Saludo con Gemini AI |
| `PlayerIndicator` | Indicador de posición del jugador |

### 5.10 Systems - Gestores Globales (16 clases)

| Clase | Propósito |
|-------|-----------|
| `WallpaperCatalog` | Catálogo de wallpapers disponibles |
| `WallpaperPreferences` | Preferencias guardadas en SharedPreferences |
| `MusicSystem` | Gestión central de música |
| `EventBus` | Bus de eventos para comunicación desacoplada |
| `GLStateManager` | Estado global de OpenGL |
| `ScreenManager` | Dimensiones y orientación de pantalla |
| `AspectRatioManager` | Gestión de aspect ratio |
| `ResourceManager` | Gestión de recursos (texturas, modelos) |
| `AdsManager` | Gestión de anuncios |
| `SubscriptionManager` | Suscripciones premium |
| `RemoteConfigManager` | Firebase Remote Config |
| `RewardsManager` | Sistema de recompensas |
| `MissionsManager` | Misiones y logros |
| `UsageTracker` | Tracking de uso |
| `UIController` | Controlador global de UI |
| `FirebaseQueueManager` | Batching de operaciones Firebase |

### 5.11 Utilities (6 clases)

| Clase | Propósito |
|-------|-----------|
| `ObjLoader` | Carga modelos .OBJ |
| `ObjLoaderWithMaterials` | Carga OBJ con materiales MTL |
| `MtlLoader` | Parser de archivos .MTL |
| `ProceduralSphere` | Genera esferas procedurales |
| `MaterialGroup` | Agrupa materiales por nombre |
| `TextureConfig` | Configuración de texturas |

### 5.12 Sharing - Compartir Música (7 clases)

| Clase | Propósito |
|-------|-----------|
| `SongSharingManager` | Gestión de canciones compartidas |
| `SharedSong` | Modelo de canción compartida |
| `SongNotification` | Notificación de nueva canción |
| `LikeButton` | Botón de like animado |
| `HeartParticleSystem` | Corazones voladores al dar like |
| `UserAvatar` | Avatar del usuario |
| `MusicNotificationListener` | Listener de notificaciones de música |

---

## 6. Clases Obsoletas

### 6.1 Completamente Huérfanas (Sin referencias)

Estas clases **NO** están siendo usadas en ningún lugar del código:

| Clase | Razón de Obsolescencia |
|-------|------------------------|
| `ArcadePreview.java` | Comentada como "REMOVIDO" en PanelModeRenderer |
| `EstrellaBailarina.java` | Feature removida de BatallaCosmicaScene |
| `CloudLayer.java` | Sin imports ni instanciaciones |
| `SunHeatEffect.java` | Comentada como "REMOVIDO" |
| `CircularLoadingRing.java` | Sin referencias |
| `DiscoBallShaderProgram.java` | Sin referencias |

### 6.2 Features Removidas (Código comentado)

| Clase | Razón |
|-------|-------|
| `EarthShield.java` | Comentada en BatallaCosmicaScene |
| `ForceField.java` | Parcialmente usada pero nunca instanciada |
| `BirthdayManager.java` | Feature removida |
| `BirthdayMarquee.java` | Feature removida |
| `LeaderboardManager.java` | Feature removida |
| `MagicLeaderboard.java` | Feature removida |
| `HoroscopeManager.java` | Inicialización comentada |
| `HoroscopeDisplay.java` | Inicialización comentada |

### 6.3 Recomendación

**Total: ~14 clases que podrían eliminarse** para limpiar el proyecto.

Antes de eliminar, verificar que no haya:
- Referencias en XML (layouts, manifest)
- Uso via reflexión
- Código comentado que planeas reactivar

---

## 7. Flujo de Datos

### 7.1 Flujo de Inicio de Wallpaper

```
Usuario selecciona wallpaper en MainActivity
           │
           ▼
WallpaperAdapter.onClick()
           │
           │ Guarda preferencia
           ▼
WallpaperPreferences.setSelectedWallpaper("Batalla Cósmica")
           │
           ▼
Intent → WallpaperPreviewActivity
           │
           │ Usuario presiona "Definir fondo"
           ▼
ACTION_CHANGE_LIVE_WALLPAPER
           │
           ▼
Android crea LiveWallpaperService
           │
           │ onCreateEngine()
           ▼
new GLWallpaperEngine()
           │
           │ onSurfaceCreated()
           ▼
WallpaperDirector.onSurfaceCreated()
           │
           │ Lee preferencia
           │ SceneFactory.createScene("Batalla Cósmica")
           ▼
BatallaCosmicaScene.onCreate()
           │
           │ setupScene() - crea objetos 3D
           ▼
Loop de renderizado activo (60 FPS)
```

### 7.2 Flujo de Render Loop

```
┌──────────────────────────────────────────────────────────────┐
│                    onDrawFrame() [60 FPS]                    │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  1. GLStateManager.beginFrame()                              │
│     └── Calcula deltaTime, limpia buffers                    │
│                                                              │
│  2. switch(currentMode):                                     │
│                                                              │
│     PANEL_MODE:                                              │
│     ├── panelRenderer.updatePanelMode(dt)                    │
│     └── panelRenderer.drawPanelMode()                        │
│                                                              │
│     LOADING_MODE:                                            │
│     ├── panelRenderer.updateLoadingMode(dt)                  │
│     ├── panelRenderer.drawLoadingMode()                      │
│     └── checkLoadingComplete()                               │
│                                                              │
│     WALLPAPER_MODE:                                          │
│     ├── musicVisualizer.getFrequencyBands()                  │
│     ├── scene.updateMusicBands(bands)                        │
│     ├── sceneFactory.updateCurrentScene(dt)                  │
│     │   └── [cada objeto] obj.update(dt)                     │
│     ├── bloomEffect.beginCapture()                           │
│     ├── sceneFactory.drawCurrentScene()                      │
│     │   └── [cada objeto] obj.draw()                         │
│     ├── screenEffects.draw()                                 │
│     ├── bloomEffect.endCaptureAndApply()                     │
│     ├── panelRenderer.drawWallpaperOverlay()                 │
│     └── songSharing.draw()                                   │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### 7.3 Flujo de Eventos Táctiles

```
MotionEvent del sistema
        │
        ▼
LiveWallpaperService.onTouchEvent()
        │
        ▼
WallpaperDirector.onTouchEvent()
        │
        ▼
TouchRouter.onTouchEvent()
        │
        │ ¿Dónde cayó el toque?
        │
        ├──► Botón Play → startLoading()
        │
        ├──► Botón Stop → switchToPanelMode()
        │
        ├──► Botón Like → songSharing.shareSongWithAI()
        │
        └──► Área de escena → scene.onTouchEvent(nx, ny, action)
                                    │
                                    ▼
                            [Acción específica de la escena]
                            - ChristmasScene: ráfaga de nieve
                            - BatallaCosmicaScene: disparo
```

---

## 8. Guía para IAs

### 8.1 Contexto Rápido

```
PROYECTO: Black Hole Glow
TIPO: Android Live Wallpaper
LENGUAJE: Java (NO Kotlin)
RENDERING: OpenGL ES 2.0/3.0
ARQUITECTURA: Actor Model + Scene Graph

ENTRY POINT: LiveWallpaperService.java
DIRECTOR: WallpaperDirector.java (implementa GLSurfaceView.Renderer)
ESCENAS: Extienden WallpaperScene.java
OBJETOS 3D: Implementan SceneObject interface
```

### 8.2 Patrones Usados

| Patrón | Implementación |
|--------|----------------|
| **Singleton** | WallpaperCatalog, EventBus, GLStateManager |
| **Factory** | SceneFactory crea escenas por nombre |
| **Observer** | EventBus para comunicación desacoplada |
| **State Machine** | RenderModeController (PANEL/LOADING/WALLPAPER) |
| **Scene Graph** | WallpaperScene contiene lista de SceneObjects |
| **Object Pool** | ProjectilePool para reutilizar proyectiles |
| **Dependency Injection** | Manual via setters en SceneFactory |

### 8.3 Convenciones de Código

```java
// Nombres de clases: PascalCase
public class BatallaCosmicaScene extends WallpaperScene { }

// Nombres de variables: camelCase
private CameraController camera;

// Constantes: SCREAMING_SNAKE_CASE
private static final String TAG = "BatallaCosmicaScene";

// Logging con TAG
Log.d(TAG, "Mensaje de debug");

// Documentación con ASCII art
/**
 * ╔═══════════════════════════════════════╗
 * ║   Nombre de la Clase                  ║
 * ╚═══════════════════════════════════════╝
 */
```

### 8.4 Archivos Importantes

| Archivo | Importancia |
|---------|-------------|
| `WallpaperDirector.java` | ⭐⭐⭐⭐⭐ Core del renderizado |
| `WallpaperScene.java` | ⭐⭐⭐⭐⭐ Base de todas las escenas |
| `SceneFactory.java` | ⭐⭐⭐⭐ Creación de escenas |
| `BatallaCosmicaScene.java` | ⭐⭐⭐⭐ Escena principal |
| `WallpaperCatalog.java` | ⭐⭐⭐ Catálogo de wallpapers |
| `LiveWallpaperService.java` | ⭐⭐⭐ Entry point Android |

### 8.5 Cómo Agregar una Nueva Escena

```java
// 1. Crear clase que extienda WallpaperScene
public class MiNuevaScene extends WallpaperScene {

    @Override
    public String getName() { return "Mi Nueva Escena"; }

    @Override
    public String getDescription() { return "Descripción..."; }

    @Override
    public int getPreviewResourceId() { return R.drawable.mi_preview; }

    @Override
    protected void setupScene() {
        // Crear objetos 3D aquí
        MiObjeto obj = new MiObjeto(context, textureManager);
        addSceneObject(obj);
    }

    @Override
    protected void releaseSceneResources() {
        // Liberar recursos específicos
    }
}

// 2. Registrar en SceneFactory.registerDefaultScenes()
registerScene("Mi Nueva Escena", MiNuevaScene.class);

// 3. Agregar al catálogo en WallpaperCatalog.initializeCatalog()
catalog.add(new WallpaperItem.Builder("Mi Nueva Escena")
    .descripcion("...")
    .preview(R.drawable.mi_preview)
    .sceneName("Mi Nueva Escena")
    .tier(WallpaperTier.FREE)
    .build());
```

### 8.6 Preguntas Frecuentes para IAs

**P: ¿Dónde está el main loop de renderizado?**
R: `WallpaperDirector.onDrawFrame()`

**P: ¿Cómo se cambia de escena?**
R: `WallpaperDirector.changeScene("nombre")` → `SceneFactory.createScene()`

**P: ¿Dónde se guardan las preferencias?**
R: `WallpaperPreferences` usa SharedPreferences

**P: ¿Cómo funciona la música reactiva?**
R: `MusicVisualizer` analiza audio → pasa bandas de frecuencia a la escena → objetos reaccionan

**P: ¿Dónde están los shaders?**
R: `app/src/main/assets/shaders/*.glsl`

---

## Fin del Documento

**Generado**: Diciembre 2024
**Proyecto**: Black Hole Glow v4.0.1
**Clases totales**: 176
**Clases obsoletas identificadas**: ~14
