# Black Hole Glow - Wallpapers

```
╔═══════════════════════════════════════════════════════════════════╗
║                    🌌 BLACK HOLE GLOW v4.1.1                      ║
║              Live Wallpaper con OpenGL ES 3.0                     ║
╚═══════════════════════════════════════════════════════════════════╝
```

## Resumen de la App

**Black Hole Glow** es una aplicación de fondos de pantalla animados para Android que utiliza OpenGL ES 3.0 para renderizar escenas 3D interactivas con efectos visuales avanzados.

| Característica | Detalle |
|----------------|---------|
| **Package** | `com.secret.blackholeglow` |
| **Versión** | 4.1.1 (código 11) |
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 35 (Android 15) |
| **Lenguaje** | Java 11 |
| **Renderizado** | OpenGL ES 3.0 |

---

## Wallpapers Disponibles

### 1. Batalla Cósmica
```
🔥 POPULAR
```

| Aspecto | Descripción |
|---------|-------------|
| **Escena** | `BatallaCosmicaScene.java` |
| **Descripción** | Defiende la Tierra de meteoritos mientras el OVNI patrulla el cosmos |
| **Modelos 3D** | Sol, Tierra, Saturno, OVNI, Nave defensora, Estación espacial |
| **Efectos** | Meteoritos, lásers, escudos, explosiones |
| **Ecualizador** | ✅ Reacciona a la música |
| **Interactividad** | Sistema de combate espacial |

**Componentes principales:**
- `SolMeshy.java` - Sol procedural con corona
- `TierraMeshy.java` - Tierra con escudo protector
- `Spaceship3D.java` - OVNI con IA de exploración
- `MeteorManager.java` - Sistema de meteoritos
- `EarthShield.java` - Escudo de la Tierra

---

### 2. Navidad
```
🎄 FESTIVO
```

| Aspecto | Descripción |
|---------|-------------|
| **Escena** | Panel Mode con componentes navideños |
| **Descripción** | Disfruta de la magia de la navidad en tu celular |
| **Fondo** | Imagen de bosque navideño con auroras |
| **Efectos** | Nieve cayendo, humo con distorsión, auroras animadas |
| **Ecualizador** | ✅ EqualizerBarsDJ integrado |
| **Interactividad** | Touch sparkles navideños |

**Componentes principales:**
- `ChristmasPanelBackground.java` - Fondo con distorsión UV (humo + auroras)
- `ChristmasSnowEffect.java` - Sistema de partículas de nieve
- `ChristmasTouchSparkles.java` - Chispas al tocar (oro, rojo, verde, blanco)
- `ChristmasTreeLights.java` - Luces del árbol animadas
- `EqualizerBarsDJ.java` - Barras de ecualizador con gradiente

---

### 3. Fondo del Mar
```
🌊 NUEVO
```

| Aspecto | Descripción |
|---------|-------------|
| **Escena** | `OceanFloorScene.java` |
| **Descripción** | Sumérgete en las profundidades de un océano alienígena |
| **Video** | `escena_fondoSC.mp4` (15.5 MB, 10s loop ping-pong) |
| **Efectos** | Plantas bioluminescentes, pez alienígena con profundidad |
| **Ecualizador** | ✅ Reacciona a la música |
| **Tecnología** | MediaCodec + SurfaceTexture + OES Texture |

**Componentes principales:**
- `MediaCodecVideoRenderer.java` - Reproductor de video estable con loop infinito
- `AlienFishSprite.java` - Pez 2D con ilusión de profundidad (escala + posición Y)
- `ForegroundMask.java` - Máscara de plantas con shader animado (ondulación + pulso cyan)

**Arquitectura del video:**
```
MP4 → MediaExtractor → MediaCodec → SurfaceTexture → OES Texture → Shader
```

---

## Características Globales

### Panel de Inicio (Panel Mode)
- **OrbixGreeting** - Título "Orbix iA"
- **OrbixMascotButton** - Gatito naranja pixelado con estrellas orbitando
- **LoadingBar** - Barra de carga con fondo negro
- **MiniStopButton** - Botón para detener wallpaper (oculto en video)

### Ecualizador Visual (MusicIndicator)
Todos los wallpapers incluyen visualización de audio:
- Barras con gradiente de colores (Rosa → Rojo → Naranja → Verde → Cyan)
- Peak holders con colores arcoíris
- Sistema de chispas que explotan al pasar el peak
- Sensibilidad progresiva para frecuencias altas
- Beat detection para reactividad mejorada

### Sistema de Modos
```
PANEL_MODE → LOADING_MODE → WALLPAPER_MODE
     ↑                            │
     └────────────────────────────┘
           (MiniStopButton)
```

---

## Estructura de Archivos Clave

```
blackholeglow/
├── app/src/main/
│   ├── assets/
│   │   ├── escena_fondoSC.mp4          # Video océano alienígena
│   │   ├── foreground_plants.png       # Máscara de plantas
│   │   └── shaders/*.glsl              # Shaders GLSL
│   │
│   ├── java/com/secret/blackholeglow/
│   │   ├── core/
│   │   │   ├── WallpaperDirector.java  # Director principal
│   │   │   ├── PanelModeRenderer.java  # Renderizador del panel
│   │   │   ├── RenderModeController.java
│   │   │   └── TouchRouter.java
│   │   │
│   │   ├── scenes/
│   │   │   ├── BatallaCosmicaScene.java
│   │   │   ├── OceanFloorScene.java
│   │   │   └── WallpaperScene.java     # Clase base
│   │   │
│   │   ├── video/
│   │   │   ├── MediaCodecVideoRenderer.java
│   │   │   ├── AlienFishSprite.java
│   │   │   └── ForegroundMask.java
│   │   │
│   │   └── systems/
│   │       └── WallpaperCatalog.java   # Catálogo de wallpapers
│   │
│   └── res/drawable/
│       ├── preview_batalla_cosmica.png
│       ├── preview_navidad.png
│       ├── preview_oceano_sc.png
│       └── gatito_orbix.png            # Mascota Orbix
```

---

## Optimizaciones Realizadas

| Optimización | Resultado |
|--------------|-----------|
| Eliminación de assets no usados | 226 MB → 128 MB (-43%) |
| MediaCodec vs MediaPlayer | Video estable, sin pausas |
| Cache de random en Spaceship3D | Reduce llamadas Math.random() |
| Sol procedural | 576 triángulos vs 7,936 |

---

## Tecnologías Utilizadas

- **OpenGL ES 3.0** - Renderizado 3D
- **MediaCodec** - Decodificación de video hardware
- **SurfaceTexture** - Textura externa OES para video
- **GLSL Shaders** - Efectos visuales en GPU
- **AudioRecord/Visualizer** - Captura de audio para ecualizador

---

*Última actualización: Diciembre 2024*
*Generado con Claude Code*
