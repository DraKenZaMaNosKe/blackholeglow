# SESSION_HANDOFF.md - Información Completa para Continuar Desarrollo

**Fecha de creación**: 1 de Diciembre de 2024
**Propósito**: Documento completo para continuar el desarrollo en una nueva sesión de Claude Code

---

## 🔐 API KEYS Y CREDENCIALES (CRÍTICO)

### Gemini AI API Key
```
GEMINI_API_KEY=AIzaSyBpHPKTB-3EWJg7EJw4AzFDr1FEy8I3G_Y
```
- **Ubicación**: `local.properties` (NO se sube a GitHub)
- **Uso**: Saludos con IA de Gemini en el wallpaper
- **Configurado en**: `app/build.gradle.kts` como `BuildConfig.GEMINI_API_KEY`

### Firebase Configuration
```json
{
  "project_id": "device-streaming-bab2df46",
  "project_number": "615188090674",
  "storage_bucket": "device-streaming-bab2df46.firebasestorage.app",
  "mobilesdk_app_id": "1:615188090674:android:e350d308a2cc0a9218512b",
  "api_key": "AIzaSyBcEulDz2uCAJ5BPVvgH3oL6c1f-2mDryg"
}
```
- **Archivo**: `app/google-services.json`
- **Servicios activos**: Auth, Firestore, Analytics, Remote Config

### Google OAuth Client IDs
| Tipo | Client ID | Certificate Hash |
|------|-----------|------------------|
| Debug | `615188090674-eucf0slo5up3u6etgi8cs7ahusjh8djd.apps.googleusercontent.com` | `6c1b78305439670bb154b4e0e6108be87527f34d` |
| Release | `615188090674-lbs2pl6jd8c1lofv2a84rs9j70rt9fpp.apps.googleusercontent.com` | `6a34068ff63ac7a74abcd0017ea9e2d787115446` |
| Web | `615188090674-gu1js8k59si00dioi22itasgrugdsgtt.apps.googleusercontent.com` | N/A |

### Release Keystore (Play Store)
```
Archivo: blackholeglow-release-key.jks
Ubicación: Raíz del proyecto
Store Password: blackholeglow2025
Key Alias: blackholeglow
Key Password: blackholeglow2025
Algoritmo: RSA 2048-bit
Validez: 10,000 días
```
**⚠️ CRÍTICO**: Si se pierde este keystore, NO se puede actualizar la app en Play Store.

---

## 📱 Información del Proyecto

### Identificadores
- **Package**: `com.secret.blackholeglow`
- **Version Name**: `4.0.0`
- **Version Code**: `8`
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)

### Tecnologías
- **Lenguaje**: Java 11 (NO Kotlin)
- **Build System**: Gradle con Kotlin DSL (.kts)
- **Gradle Version**: 8.13
- **AGP Version**: 8.12.3

### Dependencias Principales
- Firebase BoM (Auth, Firestore, Analytics, Remote Config)
- Google Play Services (Auth, Ads)
- Glide (carga de imágenes)
- Material Design Components
- AndroidX Credentials

---

## 🏗️ Arquitectura del Sistema

### Actores Principales (Actor Architecture v2.0)

```
WallpaperDirector (Director Central)
├── RenderModeController - Transiciones PANEL → LOADING → WALLPAPER
├── PanelModeRenderer - UI del panel de control
├── SceneFactory - Creación/destrucción de escenas
├── SongSharingController - Like, corazones, Gemini AI
└── TouchRouter - Distribución de eventos táctiles
```

### Sistemas Singleton

| Sistema | Clase | Propósito |
|---------|-------|-----------|
| 📐 AspectRatioManager | `systems/AspectRatioManager.java` | **NUEVO** - Distribuye aspect ratio a toda la app |
| 🎮 GLStateManager | `systems/GLStateManager.java` | Configuración OpenGL, deltaTime, FPS |
| 📺 ScreenManager | `systems/ScreenManager.java` | Dimensiones de pantalla |
| 📢 EventBus | `systems/EventBus.java` | Comunicación entre componentes |
| 🔥 FirebaseQueueManager | `systems/FirebaseQueueManager.java` | Batching de operaciones Firebase |
| 🖼️ UIController | `systems/UIController.java` | Estado de UI compartido |
| 📦 ResourceManager | `systems/ResourceManager.java` | Gestión de recursos |

### AspectRatioManager (NUEVO - Diciembre 2024)

Actor centralizado para distribuir el aspect ratio:

```java
// Implementar la interface
public class MiClase implements AspectRatioManager.AspectRatioAware {

    public MiClase() {
        // Registrarse automáticamente
        AspectRatioManager.get().register(this);
    }

    @Override
    public void onAspectRatioChanged(int width, int height, float aspectRatio) {
        // Recibir notificación cuando cambie
        this.aspectRatio = aspectRatio;
    }
}
```

**Utilidades disponibles**:
- `getAspectRatio()` - width/height (ej: 0.46 para portrait)
- `getInverseAspectRatio()` - height/width
- `pixelToNdcX/Y()` - Conversión pixel → NDC
- `percentWidthToOrtho()` - Porcentaje → unidades ortográficas

---

## 🎵 EqualizerBarsDJ v2.0 (NUEVO - Diciembre 2024)

Ecualizador estilo DJ en la parte inferior de la pantalla.

### Características
- **32 barras** delgadas distribuidas horizontalmente
- **Diseño simétrico**: Centro más alto, lados más bajos (curva coseno)
- **Gradiente de colores**: Rosa (bass/centro) → Cyan (treble/lados)
- **Efecto Glow**: Resplandor semi-transparente detrás de cada barra
- **Peak Markers**: Líneas brillantes que marcan el máximo y caen suavemente
- **Integración con AspectRatioManager**: Se adapta automáticamente al aspect ratio

### Ubicación
```
app/src/main/java/com/secret/blackholeglow/EqualizerBarsDJ.java
```

### Configuración
```java
NUM_BARS = 32
BAR_SPACING = 0.006f
MAX_HEIGHT = 0.38f
BASE_Y = -0.95f  // Parte inferior de pantalla

// Colores
COLOR_BASS = {1.0f, 0.2f, 0.6f}    // Rosa neón (centro)
COLOR_TREBLE = {0.2f, 0.9f, 1.0f}  // Cyan neón (lados)

// Peak markers
PEAK_HOLD_TIME = 0.5f   // Segundos que se mantiene arriba
PEAK_FALL_SPEED = 0.8f  // Velocidad de caída
```

---

## 🌍 Escena Principal: BatallaCosmicaScene

### Objetos de la Escena
| Objeto | Clase | Descripción |
|--------|-------|-------------|
| 🌍 Tierra | `Planeta.java` | Planeta principal con HP y sistema de daño |
| ☀️ Sol | `SolProcedural.java` | Sol procedural optimizado (576 triángulos) |
| 🛡️ Escudo | `ForceField.java` + `EarthShield.java` | Sistema de protección |
| 🛸 OVNI | `Spaceship3D.java` | IA de exploración libre con armas láser |
| ☄️ Meteoritos | `MeteorShower.java` | Sistema de meteoritos con colisiones |
| 🎵 Ecualizador | `EqualizerBarsDJ.java` | Visualizador de música estilo DJ |
| ✨ Estrellas | `EstrellaBailarina.java` | Estrellas decorativas |
| 🏆 Leaderboard | `MagicLeaderboard.java` | Tabla de puntuaciones con efectos |

### Constantes de Configuración
```
app/src/main/java/com/secret/blackholeglow/scenes/SceneConstants.java
```

Contiene TODAS las constantes de posición, escala, colores, etc. organizadas por categoría:
- `SceneConstants.Earth.*` - Configuración de la Tierra
- `SceneConstants.Sun.*` - Configuración del Sol
- `SceneConstants.Ufo.*` - Configuración del OVNI
- `SceneConstants.Shield.*` - Configuración del escudo
- `SceneConstants.UI.*` - Posiciones de elementos UI
- `SceneConstants.Colors.*` - Paleta de colores
- `SceneConstants.EqBar0-6.*` - Configuración individual de barras del ecualizador 3D (legacy)

---

## 🎮 MusicVisualizer

Sistema de captura de audio del sistema para efectos visuales.

### Funcionamiento
```java
MusicVisualizer visualizer = new MusicVisualizer();
visualizer.initialize();

// En cada frame:
float bass = visualizer.getBassLevel();      // 0.0 - 1.0
float mid = visualizer.getMidLevel();        // 0.0 - 1.0
float treble = visualizer.getTrebleLevel();  // 0.0 - 1.0
```

### Permisos Requeridos
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS"/>
```

### Solución a Bug de Reconexión
En `WallpaperDirector.java`, se fuerza reconexión en dos puntos:
1. `onLoadingComplete()` - Al entrar en WALLPAPER_MODE
2. `resume()` - Al reanudar si ya está en WALLPAPER_MODE

```java
if (modeController.isWallpaperMode()) {
    musicVisualizer.reconnect();
}
```

---

## 🔨 Comandos de Desarrollo

### Build
```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease

# Release AAB (Play Store)
./gradlew bundleRelease

# Solo compilar Java
./gradlew compileDebugJavaWithJavac
```

### ADB (Windows)
```bash
# Ruta del ADB
D:/adb/platform-tools/adb.exe

# Instalar APK
D:/adb/platform-tools/adb.exe install -r app/build/outputs/apk/debug/app-debug.apk

# Ver logs
D:/adb/platform-tools/adb.exe logcat -s depurar:V

# Forzar detener app
D:/adb/platform-tools/adb.exe shell am force-stop com.secret.blackholeglow
```

### Tags de LogCat Útiles
- `depurar` - MusicVisualizer
- `BatallaCosmicaScene` - Escena principal
- `EqualizerBarsDJ` - Ecualizador
- `AspectRatioManager` - Aspect ratio
- `WallpaperDirector` - Director central
- `GLStateManager` - Estado OpenGL

---

## 📁 Estructura de Archivos Clave

```
app/src/main/java/com/secret/blackholeglow/
├── core/
│   ├── WallpaperDirector.java      # Director central
│   ├── RenderModeController.java   # Control de modos
│   ├── PanelModeRenderer.java      # Renderizado del panel
│   ├── SceneFactory.java           # Fábrica de escenas
│   └── TouchRouter.java            # Enrutador de toques
├── scenes/
│   ├── WallpaperScene.java         # Clase base abstracta
│   ├── BatallaCosmicaScene.java    # Escena principal
│   └── SceneConstants.java         # Todas las constantes
├── systems/
│   ├── AspectRatioManager.java     # NUEVO - Aspect ratio centralizado
│   ├── EventBus.java               # Sistema de eventos
│   ├── GLStateManager.java         # Estado OpenGL
│   └── ...
├── EqualizerBarsDJ.java            # NUEVO - Ecualizador DJ
├── MusicVisualizer.java            # Captura de audio
├── Spaceship3D.java                # OVNI con IA
├── Planeta.java                    # Planetas con HP
└── ...
```

---

## 🐛 Bugs Conocidos y Soluciones

### 1. MusicVisualizer no funciona después de set wallpaper
**Causa**: El visualizer se desconecta durante la transición preview → wallpaper real
**Solución**: Forzar `reconnect()` en `onLoadingComplete()` y `resume()`

### 2. Ecualizador no se ve (aspect ratio incorrecto)
**Causa**: El aspect ratio no se pasaba correctamente a los componentes 2D
**Solución**: Crear `AspectRatioManager` como actor centralizado

### 3. Overflow de tiempo en shaders
**Causa**: `u_Time` crecía indefinidamente causando pixelado
**Solución**: Limitar `totalTime` con módulo en shaders que lo necesiten

---

## 🚀 Próximas Tareas Sugeridas

1. **Mejorar el Glow del Ecualizador** - Usar shader con blur gaussiano
2. **Partículas flotantes** - Chispas que suben desde las barras altas
3. **Modo Landscape** - Adaptar UI para tablets/TV
4. **Más escenas** - Implementar las 10 escenas del catálogo
5. **Optimización** - Reducir draw calls del ecualizador

---

## 📊 Estado Actual (Diciembre 2024)

### Versión: 4.0.0 (versionCode: 8)
### Branch: beta1.0

### Características Funcionando ✅
- [x] Escena BatallaCosmicaScene completa
- [x] OVNI con IA de exploración libre
- [x] Sistema de meteoritos con colisiones
- [x] MusicVisualizer con reconexión automática
- [x] EqualizerBarsDJ v2.0 con glow, gradiente y peaks
- [x] AspectRatioManager centralizado
- [x] Firebase Auth + Firestore
- [x] Google Sign-In
- [x] Sistema de leaderboard
- [x] Gemini AI para saludos

### En Progreso 🔄
- [ ] Shaders de bordes animados para catálogo
- [ ] Más escenas temáticas
- [ ] Sistema de ads (AdMob configurado pero pendiente)

---

## 📞 Contacto del Proyecto

- **Repositorio local**: `D:\orbix\blackholeglow`
- **Usuario de desarrollo**: lalo (Windows)
- **SDK Android**: `C:\Users\lalo\AppData\Local\Android\Sdk`

---

*Documento generado automáticamente por Claude Code - 1 de Diciembre 2024*
