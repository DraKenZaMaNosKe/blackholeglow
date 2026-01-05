# 🎬 Sistema de Reproducción de Video - Documentación para Grok

## Objetivo
Revisar y optimizar el sistema de reproducción de video en los live wallpapers de Android.
Buscamos recomendaciones sobre:
- Codecs y formatos óptimos
- Mejoras de rendimiento
- Alternativas tecnológicas (ExoPlayer, etc.)
- Gestión de memoria y batería
- Compatibilidad con dispositivos

---

## 📁 Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────────────┐
│                         ESCENAS                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐   │
│  │  LabScene    │  │OceanFloorScene│  │ PanelModeRenderer   │   │
│  │(PYRALIS)     │  │(ABYSSIA)      │  │ (Panel de Control)  │   │
│  │cielovolando  │  │marZerg.mp4   │  │ thehouse.mp4        │   │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────┘   │
│         │                  │                      │              │
│         └──────────────────┼──────────────────────┘              │
│                            ▼                                     │
│              ┌─────────────────────────────┐                     │
│              │  MediaCodecVideoRenderer    │                     │
│              │  (Decodificación + Render)  │                     │
│              └─────────────┬───────────────┘                     │
│                            │                                     │
│         ┌──────────────────┼──────────────────┐                  │
│         ▼                  ▼                  ▼                  │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐          │
│  │VideoConfig  │    │VideoDownload│    │ MediaCodec  │          │
│  │(URLs/Sizes) │    │Manager      │    │ (Android)   │          │
│  └─────────────┘    └─────────────┘    └─────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📦 Clase Principal: MediaCodecVideoRenderer.java

### Propósito
Reproducción de video usando `MediaCodec` directamente (sin ExoPlayer/MediaPlayer).

### ¿Por qué MediaCodec directo?
```java
/**
 * Ventajas sobre ExoPlayer/MediaPlayer:
 * - Control TOTAL del lifecycle
 * - Podemos reiniciar el decoder cuando queramos
 * - No dependemos de abstracciones que pueden fallar
 */
```

### Variables Principales

```java
// === COMPONENTES MEDIACODEC ===
private MediaExtractor extractor;          // Extrae datos del archivo de video
private MediaCodec decoder;                // Decodificador de hardware
private int videoTrackIndex = -1;          // Índice del track de video

// === SURFACE PARA FRAMES ===
private SurfaceTexture surfaceTexture;     // Recibe frames decodificados
private Surface surface;                   // Surface conectada al decoder
private int videoTextureId = -1;           // Textura OES para OpenGL

// === ESTADO ===
private boolean isInitialized = false;
private volatile boolean isRunning = false;
private Thread decoderThread;              // Thread dedicado para decodificación

// === OPENGL ===
private int shaderProgram;
private FloatBuffer vertexBuffer, texCoordBuffer;
private final float[] mvpMatrix = new float[16];
private final float[] stMatrix = new float[16];   // Transform matrix del video

// === SINCRONIZACIÓN ===
private volatile boolean frameAvailable = false;
private final Object frameLock = new Object();
```

### Shaders (OpenGL ES 2.0)

```glsl
// VERTEX SHADER
attribute vec4 aPosition;
attribute vec2 aTexCoord;
uniform mat4 uMVP;
uniform mat4 uST;            // Transform matrix del SurfaceTexture
varying vec2 vTexCoord;
void main() {
    gl_Position = uMVP * aPosition;
    vTexCoord = (uST * vec4(aTexCoord, 0.0, 1.0)).xy;
}

// FRAGMENT SHADER
#extension GL_OES_EGL_image_external : require
precision mediump float;
uniform samplerExternalOES uTexture;   // Textura externa (video)
varying vec2 vTexCoord;
void main() {
    gl_FragColor = texture2D(uTexture, vTexCoord);
}
```

### Métodos Principales

#### `initialize()`
```java
public void initialize() {
    // 1. Crear buffers OpenGL (vértices, UVs)
    // 2. Compilar shaders
    // 3. Crear textura OES (GL_TEXTURE_EXTERNAL_OES)
    // 4. Crear SurfaceTexture + Surface
    // 5. Configurar listener de frames
    // 6. Iniciar thread de decodificación
}
```

#### `decoderLoop()` - Thread de Decodificación
```java
private void decoderLoop() {
    Process.setThreadPriority(Process.THREAD_PRIORITY_VIDEO);  // Prioridad alta

    while (isRunning) {
        // 1. Inicializar MediaCodec
        if (!initializeMediaCodec()) {
            Thread.sleep(1000);  // Reintentar
            continue;
        }

        // 2. Loop de decodificación
        decodeLoop();

        // 3. Al terminar video, reiniciar (loop infinito)
        releaseDecoder();
    }
}
```

#### `initializeMediaCodec()`
```java
private boolean initializeMediaCodec() {
    extractor = new MediaExtractor();

    // Intentar archivo local (cache) primero
    if (localFilePath != null && new File(localFilePath).exists()) {
        extractor.setDataSource(localFilePath);
    } else {
        // Fallback a assets
        AssetFileDescriptor afd = context.getAssets().openFd(videoFileName);
        extractor.setDataSource(afd.getFileDescriptor(), ...);
    }

    // Buscar track de video
    for (int i = 0; i < extractor.getTrackCount(); i++) {
        MediaFormat format = extractor.getTrackFormat(i);
        String mime = format.getString(MediaFormat.KEY_MIME);
        if (mime.startsWith("video/")) {
            videoTrackIndex = i;
            break;
        }
    }

    // Crear decoder
    decoder = MediaCodec.createDecoderByType(mime);
    decoder.configure(format, surface, null, 0);
    decoder.start();
}
```

#### `decodeLoop()` - Loop Principal de Decodificación
```java
private void decodeLoop() {
    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    long startTime = System.nanoTime();

    while (!outputDone && isRunning) {
        // === INPUT: Enviar datos comprimidos al decoder ===
        int inputIndex = decoder.dequeueInputBuffer(10000);  // 10ms timeout
        if (inputIndex >= 0) {
            ByteBuffer inputBuffer = decoder.getInputBuffer(inputIndex);
            int sampleSize = extractor.readSampleData(inputBuffer, 0);

            if (sampleSize < 0) {
                // Fin del archivo
                decoder.queueInputBuffer(inputIndex, 0, 0, 0, BUFFER_FLAG_END_OF_STREAM);
            } else {
                long presentationTimeUs = extractor.getSampleTime();
                decoder.queueInputBuffer(inputIndex, 0, sampleSize, presentationTimeUs, 0);
                extractor.advance();
            }
        }

        // === OUTPUT: Obtener frames decodificados ===
        int outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000);
        if (outputIndex >= 0) {
            // Sincronización de tiempo (evita reproducción muy rápida)
            long presentationTimeNs = bufferInfo.presentationTimeUs * 1000;
            long elapsed = System.nanoTime() - startTime;
            long sleepTime = (presentationTimeNs - elapsed) / 1000000;

            if (sleepTime > 0 && sleepTime < 100) {
                Thread.sleep(sleepTime);
            }

            // Renderizar frame al Surface
            decoder.releaseOutputBuffer(outputIndex, true);
        }
    }
}
```

#### `draw()` - Renderizado OpenGL
```java
public void draw() {
    if (!isInitialized || surfaceTexture == null) return;

    // Actualizar textura solo si hay frame nuevo
    synchronized (frameLock) {
        if (frameAvailable) {
            surfaceTexture.updateTexImage();
            surfaceTexture.getTransformMatrix(stMatrix);
            frameAvailable = false;
        }
    }

    // Dibujar quad con textura de video
    GLES20.glUseProgram(shaderProgram);
    GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, videoTextureId);
    GLES20.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
}
```

#### `pause()` y `resume()`
```java
public void pause() {
    isRunning = false;
    decoderThread.interrupt();
    decoderThread.join(500);
    releaseDecoder();  // Libera CPU/batería
}

public void resume() {
    if (!isInitialized) return;
    startDecoder();    // Reinicia thread de decodificación
}
```

---

## 📦 VideoConfig.java

### Propósito
Configuración centralizada de URLs y tamaños de videos remotos (Supabase Storage).

```java
// Base URL de Supabase
private static final String SUPABASE_VIDEOS_URL =
    "https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/public/wallpaper-videos/";

// Mapeo de videos
static {
    VIDEOS.put("cielovolando.mp4", new ResourceInfo(
        SUPABASE_VIDEOS_URL + "cielovolando.mp4",
        10_199_549L,  // 10.2 MB
        "Portal Cosmico"
    ));

    VIDEOS.put("marZerg.mp4", new ResourceInfo(...));    // ~10 MB
    VIDEOS.put("thehouse.mp4", new ResourceInfo(...));   // ~10 MB
}
```

### Métodos
```java
public static String getRemoteUrl(String fileName);      // URL para descarga
public static long getExpectedSize(String fileName);     // Tamaño para validación
public static boolean isRemoteVideo(String fileName);    // ¿Es remoto?
```

---

## 📦 VideoDownloadManager.java

### Propósito
Descarga videos de Supabase y los guarda en cache local (filesDir).

### Variables
```java
private static final String VIDEO_DIR = "wallpaper_videos";
private final File videoDir;                              // context.getFilesDir()/wallpaper_videos
private final ExecutorService executor;                   // Thread pool para descargas
```

### Métodos Principales

```java
// Verificar si video está en cache
public boolean isVideoAvailable(String fileName) {
    File videoFile = new File(videoDir, fileName);
    if (!videoFile.exists()) return false;

    // Validar tamaño (descarga parcial?)
    long expectedSize = VideoConfig.getExpectedSize(fileName);
    if (videoFile.length() < expectedSize * 0.95) {
        videoFile.delete();  // Eliminar archivo corrupto
        return false;
    }
    return true;
}

// Obtener path local
public String getVideoPath(String fileName);

// Descarga asíncrona
public void downloadVideo(String fileName, DownloadCallback callback);

// Descarga síncrona (para preloading)
public boolean downloadVideoSync(String fileName, SyncProgressCallback callback);
```

---

## 🎬 Uso en Escenas

### LabScene.java (PYRALIS)
```java
private MediaCodecVideoRenderer videoBackground;
private static final String VIDEO_FILE = "cielovolando.mp4";

public void initialize() {
    // Verificar si video está en cache
    VideoDownloadManager downloadManager = VideoDownloadManager.getInstance(context);
    String localPath = downloadManager.getVideoPath(VIDEO_FILE);

    if (localPath != null) {
        videoBackground = new MediaCodecVideoRenderer(context, VIDEO_FILE, localPath);
    } else {
        videoBackground = new MediaCodecVideoRenderer(context, VIDEO_FILE);
    }
    videoBackground.initialize();
}

public void update(float deltaTime) {
    // Auto-recovery si video se detiene
    if (sceneIsActive && videoBackground != null && !videoBackground.isPlaying()) {
        videoBackground.resume();
    }
}

public void draw() {
    // 1. Dibujar video de fondo
    videoBackground.draw();

    // 2. Dibujar objetos 3D encima
    travelingShip.draw();
    equalizerDJ.draw();
}

public void onPause() {
    videoBackground.pause();
}

public void onResume() {
    videoBackground.resume();
}
```

---

## 📊 Especificaciones Actuales de Videos

| Video | Escena | Resolución | Codec | Tamaño |
|-------|--------|------------|-------|--------|
| cielovolando.mp4 | PYRALIS | 480x480 | H.264/AVC | 10.2 MB |
| marZerg.mp4 | ABYSSIA | 480x480 | H.264/AVC | ~10 MB |
| thehouse.mp4 | Panel | 480x480 | H.264/AVC | ~10 MB |

---

## ❓ Preguntas para Grok

### 1. Codecs y Formatos
- ¿H.264 es óptimo o deberíamos usar H.265/HEVC para mejor compresión?
- ¿VP9 sería mejor para Android?
- ¿Qué resolución y bitrate recomiendas para wallpapers (balance calidad/batería)?
- ¿Formato de contenedor óptimo (MP4 vs WebM)?

### 2. Rendimiento
- ¿Hay alguna forma de reducir el consumo de batería durante playback?
- ¿El Thread.sleep() para sincronización es óptimo o hay mejor método?
- ¿Deberíamos usar `MediaCodec.Callback` (asíncrono) en lugar del loop síncrono?
- ¿Cómo podemos detectar y manejar thermal throttling?

### 3. Alternativas Tecnológicas
- ¿Vale la pena migrar a ExoPlayer para live wallpapers?
- ¿Qué ventajas tendría usar `ImageReader` + `SurfaceTexture`?
- ¿Android tiene APIs más nuevas para video en wallpapers?

### 4. Gestión de Memoria
- ¿Cómo podemos reducir el uso de memoria del decoder?
- ¿Es mejor usar `GLES20` o `GLES30` para el render del video?
- ¿Deberíamos implementar un pool de buffers?

### 5. Compatibilidad
- ¿Qué dispositivos podrían tener problemas con nuestro approach?
- ¿Hay flags de MediaFormat que deberíamos usar para mejor compatibilidad?
- ¿Cómo manejar dispositivos sin soporte de hardware para H.264?

### 6. Looping
- ¿Hay una forma más eficiente de hacer loop de video que reiniciar el decoder?
- ¿`MediaExtractor.seekTo(0)` sería mejor que recrear todo?

### 7. Power Efficiency
- ¿Cómo podemos detectar cuando el dispositivo está en modo ahorro de batería?
- ¿Deberíamos reducir framerate del video en ese caso?
- ¿Hay APIs para saber si el playback está causando drain excesivo?

---

## 🔧 Problemas Conocidos

1. **Dual Video Bug (RESUELTO)**: Cuando había 2 videos simultáneos (panel + escena), causaba lag. Solucionado con pause/resume adecuado en transiciones.

2. **Video Freeze**: A veces el video se congela al volver de otra app. Mitigado con auto-recovery en `update()`.

3. **Memory Pressure**: En dispositivos con poca RAM, el decoder puede fallar. No hay retry mechanism robusto.

---

## 📝 Notas Adicionales

- Los videos están en Supabase Storage para reducir tamaño del APK
- Se usa `filesDir` (no cache) para persistencia entre sesiones
- Thread de decodificación usa `THREAD_PRIORITY_VIDEO` para suavidad
- `GL_TEXTURE_EXTERNAL_OES` es requerido para texturas de video en Android
