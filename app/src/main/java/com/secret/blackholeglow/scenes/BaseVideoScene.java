package com.secret.blackholeglow.scenes;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import com.secret.blackholeglow.Battery3D;
import com.secret.blackholeglow.CameraController;
import com.secret.blackholeglow.Clock3D;
import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.TextureManager;
import com.secret.blackholeglow.video.MediaCodecVideoRenderer;
import com.secret.blackholeglow.video.VideoDownloadManager;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                              ║
 * ║   📖 GUÍA PARA CLAUDE CODE - BaseVideoScene                                  ║
 * ║                                                                              ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                              ║
 * ║   🎯 PROPÓSITO:                                                              ║
 * ║   Clase base para TODAS las escenas que usan video de fondo + UI estándar.   ║
 * ║   Elimina ~180 líneas de código duplicado por escena.                        ║
 * ║                                                                              ║
 * ║   📦 COMPONENTES INCLUIDOS AUTOMÁTICAMENTE:                                  ║
 * ║   • MediaCodecVideoRenderer - Video de fondo con auto-recovery               ║
 * ║   • EqualizerBarsDJ - Ecualizador de 32 barras con temas                     ║
 * ║   • Clock3D - Reloj digital 3D (opcional)                                    ║
 * ║   • Battery3D - Indicador de batería 3D (opcional)                           ║
 * ║                                                                              ║
 * ║   🔧 FUNCIONALIDADES HEREDADAS:                                              ║
 * ║   • Descarga automática de video desde Supabase                              ║
 * ║   • Auto-recovery si el video se detiene                                     ║
 * ║   • Pause/Resume automático del video                                        ║
 * ║   • updateMusicBands() para visualización de audio                           ║
 * ║   • Gestión de ciclo de vida completa                                        ║
 * ║                                                                              ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                              ║
 * ║   🆕 CÓMO CREAR UNA NUEVA ESCENA DE VIDEO:                                   ║
 * ║                                                                              ║
 * ║   1. Crear clase que extienda BaseVideoScene                                 ║
 * ║   2. Implementar los métodos abstractos obligatorios:                        ║
 * ║      - getName() → Identificador único ("GOKU", "NARUTO", etc.)              ║
 * ║      - getDescription() → Descripción para UI                                ║
 * ║      - getPreviewResourceId() → R.drawable.preview_xxx                       ║
 * ║      - getVideoFileName() → "nombre_video.mp4"                               ║
 * ║      - getTheme() → EqualizerBarsDJ.Theme.XXX                                ║
 * ║                                                                              ║
 * ║   3. (Opcional) Override métodos para personalizar:                          ║
 * ║      - useClockAndBattery() → false si no quieres reloj/batería              ║
 * ║      - getClockPosition() → new float[]{x, y}                                ║
 * ║      - getBatteryPosition() → new float[]{x, y}                              ║
 * ║      - setupSceneSpecific() → Agregar objetos 3D adicionales                 ║
 * ║      - updateSceneSpecific(dt) → Lógica de update adicional                  ║
 * ║      - drawSceneSpecific() → Dibujar objetos adicionales                     ║
 * ║      - releaseSceneSpecificResources() → Liberar recursos adicionales        ║
 * ║                                                                              ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                              ║
 * ║   📝 EJEMPLO MÍNIMO (Escena simple solo con video):                          ║
 * ║                                                                              ║
 * ║   public class MiEscena extends BaseVideoScene {                             ║
 * ║       @Override public String getName() { return "MI_ESCENA"; }              ║
 * ║       @Override public String getDescription() { return "Mi escena"; }       ║
 * ║       @Override public int getPreviewResourceId() {                          ║
 * ║           return R.drawable.preview_mi_escena;                               ║
 * ║       }                                                                      ║
 * ║       @Override public String getVideoFileName() {                           ║
 * ║           return "mi_video.mp4";                                             ║
 * ║       }                                                                      ║
 * ║       @Override public EqualizerBarsDJ.Theme getTheme() {                    ║
 * ║           return EqualizerBarsDJ.Theme.DEFAULT;                              ║
 * ║       }                                                                      ║
 * ║   }                                                                          ║
 * ║                                                                              ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                              ║
 * ║   📝 EJEMPLO COMPLETO (Escena con objetos 3D adicionales):                   ║
 * ║                                                                              ║
 * ║   public class NarutoScene extends BaseVideoScene {                          ║
 * ║       private Naruto3D naruto;                                               ║
 * ║                                                                              ║
 * ║       @Override public String getName() { return "NARUTO"; }                 ║
 * ║       @Override public String getDescription() {                             ║
 * ║           return "Naruto Rasengan";                                          ║
 * ║       }                                                                      ║
 * ║       @Override public int getPreviewResourceId() {                          ║
 * ║           return R.drawable.preview_naruto;                                  ║
 * ║       }                                                                      ║
 * ║       @Override public String getVideoFileName() {                           ║
 * ║           return "naruto_rasengan.mp4";                                      ║
 * ║       }                                                                      ║
 * ║       @Override public EqualizerBarsDJ.Theme getTheme() {                    ║
 * ║           return EqualizerBarsDJ.Theme.NARUTO;  // Naranja chakra            ║
 * ║       }                                                                      ║
 * ║                                                                              ║
 * ║       @Override                                                              ║
 * ║       protected void setupSceneSpecific() {                                  ║
 * ║           naruto = new Naruto3D(context, textureManager);                    ║
 * ║           naruto.setPosition(0f, -0.5f, 0f);                                 ║
 * ║           naruto.setScreenSize(screenWidth, screenHeight);                   ║
 * ║       }                                                                      ║
 * ║                                                                              ║
 * ║       @Override                                                              ║
 * ║       protected void updateSceneSpecific(float deltaTime) {                  ║
 * ║           if (naruto != null) naruto.update(deltaTime);                      ║
 * ║       }                                                                      ║
 * ║                                                                              ║
 * ║       @Override                                                              ║
 * ║       protected void drawSceneSpecific() {                                   ║
 * ║           if (naruto != null) naruto.draw();                                 ║
 * ║       }                                                                      ║
 * ║                                                                              ║
 * ║       @Override                                                              ║
 * ║       protected void releaseSceneSpecificResources() {                       ║
 * ║           if (naruto != null) { naruto.dispose(); naruto = null; }           ║
 * ║       }                                                                      ║
 * ║   }                                                                          ║
 * ║                                                                              ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                              ║
 * ║   🎨 TEMAS DISPONIBLES (EqualizerBarsDJ.Theme):                              ║
 * ║   • DEFAULT      → Rosa → Cyan (original)                                    ║
 * ║   • ABYSSIA      → Púrpura → Turquesa (océano profundo)                      ║
 * ║   • PYRALIS      → Rojo → Naranja → Amarillo (fuego)                         ║
 * ║   • KAMEHAMEHA   → Azul → Cyan → Blanco (energía Ki)                         ║
 * ║   • SYNTHWAVE    → Hot Pink → Cyan → Magenta (retrowave 80s)                 ║
 * ║   • COSMOS       → Azul → Dorado → Púrpura (Saint Seiya)                     ║
 * ║   • WALKING_DEAD → Verde tóxico → Rojo sangre (zombie)                       ║
 * ║   • ZELDA        → Verde Hyrule → Azul → Dorado Triforce                     ║
 * ║                                                                              ║
 * ║   💡 Para agregar un nuevo tema, editar EqualizerBarsDJ.java                 ║
 * ║                                                                              ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                              ║
 * ║   📍 POSICIONES POR DEFECTO:                                                 ║
 * ║   • Clock3D:   x=0.0f (centro), y=0.75f (arriba)                             ║
 * ║   • Battery3D: x=0.81f (derecha), y=-0.34f (abajo-derecha)                   ║
 * ║                                                                              ║
 * ║   Para cambiar posiciones, override:                                         ║
 * ║   @Override protected float[] getClockPosition() {                           ║
 * ║       return new float[]{0.5f, 0.8f};  // Más a la derecha y arriba          ║
 * ║   }                                                                          ║
 * ║                                                                              ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                              ║
 * ║   ⚠️ NOTAS IMPORTANTES:                                                      ║
 * ║   • El video se descarga automáticamente de Supabase si no existe            ║
 * ║   • El auto-recovery revisa cada 2 segundos si el video se detuvo            ║
 * ║   • Los recursos se liberan automáticamente en onDestroy()                   ║
 * ║   • Para escenas SIN video, usar WallpaperScene directamente                 ║
 * ║                                                                              ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * @author Claude Code + Eduardo
 * @version 1.0.0
 * @since 2026-01-21
 */
public abstract class BaseVideoScene extends WallpaperScene {

    private static final String TAG = "BaseVideoScene";

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎬 COMPONENTES BASE (manejados automáticamente)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Renderer de video MediaCodec - maneja decodificación eficiente de video */
    protected MediaCodecVideoRenderer videoBackground;

    /** Manager de descargas de video desde Supabase */
    protected VideoDownloadManager downloadManager;

    /** Ecualizador de 32 barras con temas personalizados */
    protected EqualizerBarsDJ equalizerDJ;

    /** Reloj digital 3D (opcional - ver useClockAndBattery()) */
    protected Clock3D clock;

    /** Indicador de batería 3D (opcional - ver useClockAndBattery()) */
    protected Battery3D battery;

    // ═══════════════════════════════════════════════════════════════════════════
    // 🔄 AUTO-RECOVERY (evita que el video se detenga)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Timer para verificar estado del video */
    private float videoCheckTimer = 0f;

    /** Intervalo de verificación en segundos */
    private static final float VIDEO_CHECK_INTERVAL = 2.0f;

    /** Flag para saber si la escena está activa */
    private boolean sceneIsActive = true;

    /** 🔧 FIX FREEZE: Timer para logging de estado de carga del video */
    private float videoLoadingLogTimer = 0f;
    private static final float VIDEO_LOADING_LOG_INTERVAL = 1.0f;

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎯 MÉTODOS ABSTRACTOS - OBLIGATORIO IMPLEMENTAR
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Nombre del archivo de video a usar (debe existir en Supabase).
     *
     * @return Nombre del archivo, ej: "goku_kamehameha.mp4"
     */
    protected abstract String getVideoFileName();

    /**
     * Tema de colores para el ecualizador, reloj y batería.
     *
     * @return Tema del enum EqualizerBarsDJ.Theme
     * @see EqualizerBarsDJ.Theme para ver todos los temas disponibles
     */
    protected abstract EqualizerBarsDJ.Theme getTheme();

    // ═══════════════════════════════════════════════════════════════════════════
    // 🔧 MÉTODOS OPCIONALES - OVERRIDE PARA PERSONALIZAR
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Indica si la escena debe mostrar reloj y batería.
     * Override y retorna false para escenas minimalistas.
     *
     * @return true por defecto (muestra reloj y batería)
     */
    protected boolean useClockAndBattery() {
        return true;
    }

    /**
     * Posición del reloj en coordenadas normalizadas.
     * X: -1.0 (izquierda) a 1.0 (derecha)
     * Y: -1.0 (abajo) a 1.0 (arriba)
     *
     * @return float[2] con {x, y}, por defecto {0.0f, 0.75f} (centro-arriba)
     */
    protected float[] getClockPosition() {
        return new float[]{0.0f, 0.75f};
    }

    /**
     * Posición de la batería en coordenadas normalizadas.
     *
     * @return float[2] con {x, y}, por defecto {0.81f, -0.34f} (derecha-abajo)
     */
    protected float[] getBatteryPosition() {
        return new float[]{0.81f, -0.34f};
    }

    /**
     * Indica si el reloj debe mostrar milisegundos.
     *
     * @return true por defecto
     */
    protected boolean showClockMilliseconds() {
        return true;
    }

    /**
     * Hook para configurar objetos adicionales específicos de la escena.
     * Se llama después de configurar video, ecualizador, reloj y batería.
     *
     * Ejemplo de uso:
     * <pre>
     * @Override
     * protected void setupSceneSpecific() {
     *     miObjeto3D = new MiObjeto3D(context, textureManager);
     *     miObjeto3D.setPosition(0f, 0f, 0f);
     * }
     * </pre>
     */
    protected void setupSceneSpecific() {
        // Override en subclases si necesitan objetos adicionales
    }

    /**
     * Hook para actualizar objetos adicionales cada frame.
     * Se llama después de actualizar video, ecualizador, reloj y batería.
     *
     * @param deltaTime Tiempo transcurrido desde el último frame en segundos
     */
    protected void updateSceneSpecific(float deltaTime) {
        // Override en subclases si tienen objetos adicionales
    }

    /**
     * Hook para dibujar objetos adicionales.
     * Se llama después del video pero ANTES del ecualizador, reloj y batería.
     * Esto permite que los objetos 3D aparezcan sobre el video pero debajo de la UI.
     */
    protected void drawSceneSpecific() {
        // Override en subclases si tienen objetos adicionales
    }

    /**
     * Hook para liberar recursos adicionales específicos de la escena.
     * Se llama después de liberar video, ecualizador, reloj y batería.
     */
    protected void releaseSceneSpecificResources() {
        // Override en subclases si tienen recursos adicionales
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎬 IMPLEMENTACIÓN DEL CICLO DE VIDA
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Configura todos los componentes de la escena.
     * NO hacer override de este método - usar setupSceneSpecific() en su lugar.
     */
    @Override
    protected final void setupScene() {
        String videoFile = getVideoFileName();
        EqualizerBarsDJ.Theme theme = getTheme();

        Log.d(TAG, "╔════════════════════════════════════════════════════════════╗");
        Log.d(TAG, "║  🎬 Configurando: " + getName());
        Log.d(TAG, "║  📹 Video: " + videoFile);
        Log.d(TAG, "║  🎨 Tema: " + theme.name());
        Log.d(TAG, "╚════════════════════════════════════════════════════════════╝");

        // 1. Configurar video de fondo
        setupVideoBackground(videoFile);

        // 2. Configurar ecualizador
        setupEqualizer(theme);

        // 3. Configurar reloj y batería (si están habilitados)
        if (useClockAndBattery()) {
            setupClockAndBattery(theme);
        }

        // 4. Llamar al hook para configuración específica de la subclase
        setupSceneSpecific();

        Log.d(TAG, "✅ " + getName() + " configurado completamente");
    }

    /**
     * Configura el video de fondo.
     *
     * ⚠️ IMPORTANTE: Este método NO descarga videos.
     * El video DEBE estar pre-descargado por ResourcePreloader.
     * Si el video no existe, la escena funcionará sin video de fondo.
     */
    private void setupVideoBackground(String videoFile) {
        downloadManager = VideoDownloadManager.getInstance(context);

        try {
            String localPath = downloadManager.getVideoPath(videoFile);

            // ⚠️ NO descargar aquí - esto bloquea el GL thread y causa ANR
            // El video debe estar pre-descargado por ResourcePreloader
            if (localPath == null) {
                Log.e(TAG, "❌ Video no disponible: " + videoFile);
                Log.e(TAG, "❌ ResourcePreloader debería haberlo descargado antes.");
                Log.e(TAG, "❌ La escena funcionará sin video de fondo.");

                // 🔧 Intentar descarga en background para la próxima vez
                // (NO bloquea el thread actual)
                downloadManager.downloadVideo(videoFile, new VideoDownloadManager.DownloadCallback() {
                    @Override
                    public void onProgress(int percent, long downloadedBytes, long totalBytes) {
                        Log.d(TAG, "📥 Descarga background: " + percent + "%");
                    }
                    @Override
                    public void onComplete(String filePath) {
                        Log.d(TAG, "✅ Video descargado en background: " + filePath);
                        // TODO: Podríamos reiniciar el video aquí si queremos
                    }
                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "❌ Error descarga background: " + message);
                    }
                });
                return;
            }

            // Crear el renderer de video
            Log.d(TAG, "📦 Usando video: " + localPath);
            videoBackground = new MediaCodecVideoRenderer(context, videoFile, localPath);
            videoBackground.initialize();
            Log.d(TAG, "✅ Video inicializado correctamente");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error configurando video: " + e.getMessage());
        }
    }

    /**
     * Configura el ecualizador con el tema especificado.
     */
    private void setupEqualizer(EqualizerBarsDJ.Theme theme) {
        try {
            equalizerDJ = new EqualizerBarsDJ();
            equalizerDJ.initialize();
            equalizerDJ.setTheme(theme);
            equalizerDJ.setScreenSize(screenWidth, screenHeight);
            Log.d(TAG, "✅ Ecualizador " + theme.name() + " activado");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error configurando ecualizador: " + e.getMessage());
        }
    }

    /**
     * Configura el reloj y la batería con el tema especificado.
     */
    private void setupClockAndBattery(EqualizerBarsDJ.Theme theme) {
        // Obtener el tema correspondiente para Clock3D y Battery3D
        int clockTheme = getClockThemeFromEqualizer(theme);
        int batteryTheme = getBatteryThemeFromEqualizer(theme);

        float[] clockPos = getClockPosition();
        float[] batteryPos = getBatteryPosition();

        // Configurar reloj
        try {
            clock = new Clock3D(context, clockTheme, clockPos[0], clockPos[1]);
            clock.setShowMilliseconds(showClockMilliseconds());
            Log.d(TAG, "✅ Reloj activado en (" + clockPos[0] + ", " + clockPos[1] + ")");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error configurando reloj: " + e.getMessage());
        }

        // Configurar batería
        try {
            battery = new Battery3D(context, batteryTheme, batteryPos[0], batteryPos[1]);
            Log.d(TAG, "✅ Batería activada en (" + batteryPos[0] + ", " + batteryPos[1] + ")");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error configurando batería: " + e.getMessage());
        }
    }

    /**
     * Convierte el tema del ecualizador al tema correspondiente de Clock3D.
     */
    private int getClockThemeFromEqualizer(EqualizerBarsDJ.Theme theme) {
        switch (theme) {
            case ABYSSIA:      return Clock3D.THEME_ABYSSIA;
            case PYRALIS:      return Clock3D.THEME_PYRALIS;
            case KAMEHAMEHA:   return Clock3D.THEME_KAMEHAMEHA;
            case SYNTHWAVE:    return Clock3D.THEME_SYNTHWAVE;
            case COSMOS:       return Clock3D.THEME_COSMOS;
            case WALKING_DEAD: return Clock3D.THEME_WALKING_DEAD;
            case ZELDA:        return Clock3D.THEME_ZELDA;
            default:           return Clock3D.THEME_ABYSSIA;
        }
    }

    /**
     * Convierte el tema del ecualizador al tema correspondiente de Battery3D.
     */
    private int getBatteryThemeFromEqualizer(EqualizerBarsDJ.Theme theme) {
        switch (theme) {
            case ABYSSIA:      return Battery3D.THEME_ABYSSIA;
            case PYRALIS:      return Battery3D.THEME_PYRALIS;
            case KAMEHAMEHA:   return Battery3D.THEME_KAMEHAMEHA;
            case SYNTHWAVE:    return Battery3D.THEME_SYNTHWAVE;
            case COSMOS:       return Battery3D.THEME_COSMOS;
            case WALKING_DEAD: return Battery3D.THEME_WALKING_DEAD;
            case ZELDA:        return Battery3D.THEME_ZELDA;
            default:           return Battery3D.THEME_ABYSSIA;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 🔄 UPDATE - Lógica de cada frame
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void update(float deltaTime) {
        // Auto-recovery del video
        if (!sceneIsActive) {
            sceneIsActive = true;
        }

        // 🔧 FIX FREEZE: Log del estado de carga del video (solo mientras carga)
        if (videoBackground != null && !videoBackground.hasFirstFrame()) {
            videoLoadingLogTimer += deltaTime;
            if (videoLoadingLogTimer >= VIDEO_LOADING_LOG_INTERVAL) {
                videoLoadingLogTimer = 0f;
                Log.d(TAG, "⏳ " + getName() + " - Video cargando... (UI activa)");
            }
        }

        videoCheckTimer += deltaTime;
        if (videoCheckTimer >= VIDEO_CHECK_INTERVAL) {
            videoCheckTimer = 0f;
            if (videoBackground != null && !videoBackground.isPlaying()) {
                // 🔄 Auto-recovery: Si el video se detuvo, intentar reanudar
                // Primero intenta resume() simple (decoder thread parado pero Surface existe)
                // Si Surface fue liberada por releaseForPause(), resume() no hará nada
                // porque isInitialized sigue true pero surface es null → reinitializar
                if (videoBackground.isInitialized()) {
                    videoBackground.resume();
                } else {
                    Log.d(TAG, "🔄 Auto-recovery: reinitializeAfterPause()");
                    videoBackground.reinitializeAfterPause();
                }
            }
        }

        // Actualizar componentes base
        if (equalizerDJ != null) equalizerDJ.update(deltaTime);
        if (clock != null) clock.update(deltaTime);
        if (battery != null) battery.update(deltaTime);

        // Hook para actualización específica de la subclase
        updateSceneSpecific(deltaTime);

        super.update(deltaTime);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎨 DRAW - Dibujado de cada frame
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void draw() {
        if (isDisposed) return;

        try {
            // Limpiar buffer - SIEMPRE limpiar para que el reloj/batería se muestren
            GLES30.glClearColor(0f, 0f, 0f, 1.0f);
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

            // 1. Video de fondo (sin depth test para cubrir toda la pantalla)
            GLES30.glDisable(GLES30.GL_DEPTH_TEST);
            if (videoBackground != null) {
                // 🔧 FIX FREEZE: El video.draw() retorna inmediatamente si no hay frames
                // Esto permite que el reloj y batería sigan animándose mientras carga
                try {
                    videoBackground.draw();
                } catch (Exception e) {
                    Log.e(TAG, "⚠️ Error dibujando video (recuperable): " + e.getMessage());
                    // Intentar reiniciar el video
                    try {
                        videoBackground.resume();
                    } catch (Exception ignored) {}
                }
            }

            // 2. Objetos 3D específicos de la escena (con depth test y blending)
            GLES30.glEnable(GLES30.GL_DEPTH_TEST);
            GLES30.glEnable(GLES30.GL_BLEND);
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

            // Hook para dibujar objetos específicos de la subclase
            try {
                drawSceneSpecific();
            } catch (Exception e) {
                Log.e(TAG, "⚠️ Error en drawSceneSpecific: " + e.getMessage());
            }

            // 3. UI (ecualizador, reloj, batería) - siempre encima de todo
            try {
                if (equalizerDJ != null) equalizerDJ.draw();
            } catch (Exception e) {
                Log.e(TAG, "⚠️ Error dibujando ecualizador: " + e.getMessage());
            }

            try {
                if (clock != null) clock.draw();
            } catch (Exception e) {
                Log.e(TAG, "⚠️ Error dibujando reloj: " + e.getMessage());
            }

            try {
                if (battery != null) battery.draw();
            } catch (Exception e) {
                Log.e(TAG, "⚠️ Error dibujando batería: " + e.getMessage());
            }

            super.draw();
        } catch (Exception e) {
            Log.e(TAG, "❌ Error crítico en draw(): " + e.getMessage(), e);
            // Limpiar pantalla con color negro como fallback
            try {
                GLES30.glClearColor(0f, 0f, 0f, 1.0f);
                GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
            } catch (Exception ignored) {}
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 📐 SCREEN SIZE
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void setScreenSize(int width, int height) {
        super.setScreenSize(width, height);
        if (equalizerDJ != null) {
            equalizerDJ.setScreenSize(width, height);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎵 MÚSICA - Actualización de bandas de audio
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Actualiza el ecualizador con los datos de audio.
     * Llamado automáticamente por el sistema de música.
     *
     * @param bands Array de 32 floats con los niveles de cada banda de frecuencia
     */
    public void updateMusicBands(float[] bands) {
        if (equalizerDJ != null && bands != null && bands.length > 0) {
            equalizerDJ.updateFromBands(bands);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ⏸️▶️ PAUSE/RESUME
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void onPause() {
        super.onPause();
        sceneIsActive = false;
        videoCheckTimer = 0f;

        if (videoBackground != null) {
            // 🧹 OPTIMIZACIÓN MEMORIA: Liberar Surface/SurfaceTexture/Decoder completamente
            // Ahorra ~40-60 MB de RAM cuando el wallpaper no es visible
            videoBackground.releaseForPause();
            Log.d(TAG, "⏸️🧹 " + getName() + " video LIBERADO (ahorro de memoria)");
        }

        // Hook para pausar recursos específicos de la subclase (ej: giroscopio)
        onPauseSceneSpecific();
    }

    @Override
    public void onResume() {
        super.onResume();
        sceneIsActive = true;
        videoCheckTimer = 0f;

        if (videoBackground != null) {
            // 🔄 OPTIMIZACIÓN MEMORIA: Recrear recursos de video (reutiliza shader/buffers)
            boolean success = videoBackground.reinitializeAfterPause();
            if (success) {
                Log.d(TAG, "▶️🔄 " + getName() + " video REINICIALIZADO");
            } else {
                Log.e(TAG, "❌ " + getName() + " falló reinitializar video, intentando initialize() completo");
                videoBackground.initialize();
            }
        }

        // Hook para reanudar recursos específicos de la subclase (ej: giroscopio)
        onResumeSceneSpecific();
    }

    /**
     * 🛡️ Verifica si la escena está completamente lista para renderizar.
     * Para escenas de video, requiere que el primer frame haya sido recibido.
     */
    @Override
    public boolean isReady() {
        if (!super.isReady()) return false;
        // Si tiene video, requiere primer frame recibido
        if (videoBackground != null) {
            return videoBackground.isReadyToRender();
        }
        // Sin video (escena solo con overlays), siempre lista
        return true;
    }

    /**
     * Hook para pausar recursos específicos de la escena.
     * Override para pausar giroscopio, sensores, etc.
     */
    protected void onPauseSceneSpecific() {
        // Override en subclases que necesiten pausar recursos adicionales
    }

    /**
     * Hook para reanudar recursos específicos de la escena.
     * Override para reanudar giroscopio, sensores, etc.
     */
    protected void onResumeSceneSpecific() {
        // Override en subclases que necesiten reanudar recursos adicionales
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 🗑️ LIBERACIÓN DE RECURSOS
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    protected final void releaseSceneResources() {
        Log.d(TAG, "🗑️ Liberando recursos de " + getName() + "...");

        // Liberar video
        if (videoBackground != null) {
            videoBackground.release();
            videoBackground = null;
        }

        // Liberar reloj
        if (clock != null) {
            clock.dispose();
            clock = null;
        }

        // Liberar batería
        if (battery != null) {
            battery.dispose();
            battery = null;
        }

        // Liberar ecualizador
        if (equalizerDJ != null) {
            equalizerDJ.release();
            equalizerDJ = null;
        }

        // Hook para liberar recursos específicos de la subclase
        releaseSceneSpecificResources();

        Log.d(TAG, "✅ Recursos de " + getName() + " liberados");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 👆 TOUCH - Por defecto no maneja touch (override en subclases)
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public boolean onTouchEvent(float normalizedX, float normalizedY, int action) {
        // Override en subclases que necesiten interacción táctil
        return false;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 🔧 GETTERS ÚTILES PARA SUBCLASES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Obtiene el renderer de video (útil para subclases que necesiten verificar estado).
     */
    protected MediaCodecVideoRenderer getVideoBackground() {
        return videoBackground;
    }

    /**
     * Obtiene el ecualizador (útil para subclases que quieran sincronizar efectos).
     */
    protected EqualizerBarsDJ getEqualizer() {
        return equalizerDJ;
    }

    /**
     * Verifica si el video está reproduciendo.
     */
    protected boolean isVideoPlaying() {
        return videoBackground != null && videoBackground.isPlaying();
    }
}
