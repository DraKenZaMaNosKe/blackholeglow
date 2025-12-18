package com.secret.blackholeglow.systems;

import android.content.Context;
import android.util.Log;

import com.secret.blackholeglow.MusicVisualizer;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                        MusicSystem                                ║
 * ║                    "DJ / Sonidista"                               ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  Gestiona todo lo relacionado con música y audio reactivo:       ║
 * ║                                                                  ║
 * ║  • Captura de audio del sistema                                  ║
 * ║  • Análisis de frecuencias (bass, mid, treble)                   ║
 * ║  • Detección de beats                                            ║
 * ║  • Visualización de audio (EqualizerBarsDJ)                      ║
 * ║  • Publicación de eventos musicales                              ║
 * ║                                                                  ║
 * ║  RESPONSABILIDADES:                                              ║
 * ║  • Inicializar/liberar MusicVisualizer                           ║
 * ║  • Proporcionar niveles de audio a quien lo necesite             ║
 * ║  • Publicar eventos de beat via EventBus                         ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class MusicSystem {
    private static final String TAG = "MusicSystem";

    // ═══════════════════════════════════════════════════════════════
    // 📦 SINGLETON
    // ═══════════════════════════════════════════════════════════════

    private static MusicSystem instance;
    private Context context;
    private boolean initialized = false;

    // ═══════════════════════════════════════════════════════════════
    // 🎵 COMPONENTES
    // ═══════════════════════════════════════════════════════════════

    private MusicVisualizer visualizer;
    private boolean enabled = true;

    // ═══════════════════════════════════════════════════════════════
    // 📊 NIVELES DE AUDIO (cached)
    // ═══════════════════════════════════════════════════════════════

    private float bassLevel = 0f;
    private float midLevel = 0f;
    private float trebleLevel = 0f;
    private float volumeLevel = 0f;
    private float beatIntensity = 0f;
    private boolean isBeat = false;

    // ═══════════════════════════════════════════════════════════════
    // 🔧 SINGLETON
    // ═══════════════════════════════════════════════════════════════

    private MusicSystem() {}

    public static MusicSystem get() {
        if (instance == null) {
            instance = new MusicSystem();
        }
        return instance;
    }

    // ═══════════════════════════════════════════════════════════════
    // 🎬 INICIALIZACIÓN
    // ═══════════════════════════════════════════════════════════════

    /**
     * Inicializar sistema de música (sin indicador visual)
     */
    public void init(Context ctx) {
        if (initialized) return;

        this.context = ctx.getApplicationContext();

        // Crear visualizador de audio (con Context para auto-resume de música)
        visualizer = new MusicVisualizer(context);
        visualizer.initialize();

        if (enabled) {
            visualizer.resume();
        }

        initialized = true;
        Log.d(TAG, "🎵 MusicSystem inicializado");
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔄 UPDATE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Actualizar niveles de audio (llamar cada frame)
     */
    public void update(float deltaTime) {
        if (!initialized || !enabled || visualizer == null) return;

        // Obtener niveles del visualizador
        bassLevel = visualizer.getBassLevel();
        midLevel = visualizer.getMidLevel();
        trebleLevel = visualizer.getTrebleLevel();
        volumeLevel = visualizer.getVolumeLevel();
        beatIntensity = visualizer.getBeatIntensity();
        isBeat = visualizer.isBeat();

        // Publicar evento de beat si hay uno
        if (isBeat) {
            EventBus.get().publish(EventBus.MUSIC_BEAT,
                new EventBus.EventData()
                    .put("intensity", beatIntensity)
                    .put("bass", bassLevel)
                    .put("mid", midLevel)
                    .put("treble", trebleLevel));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 📊 GETTERS DE NIVELES
    // ═══════════════════════════════════════════════════════════════

    public float getBassLevel() {
        return bassLevel;
    }

    public float getMidLevel() {
        return midLevel;
    }

    public float getTrebleLevel() {
        return trebleLevel;
    }

    public float getVolumeLevel() {
        return volumeLevel;
    }

    public float getBeatIntensity() {
        return beatIntensity;
    }

    public boolean isBeat() {
        return isBeat;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public MusicVisualizer getVisualizer() {
        return visualizer;
    }

    // ═══════════════════════════════════════════════════════════════
    // ⚙️ CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (visualizer != null) {
            if (enabled) {
                visualizer.resume();
            } else {
                visualizer.pause();
            }
        }
        Log.d(TAG, "🎵 MusicSystem " + (enabled ? "activado" : "desactivado"));
    }

    /**
     * Pausar captura de audio (ahorra batería)
     */
    public void pause() {
        if (visualizer != null) {
            visualizer.pause();
        }
    }

    /**
     * Reanudar captura de audio
     */
    public void resume() {
        if (visualizer != null) {
            visualizer.resume();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🗑️ LIMPIEZA
    // ═══════════════════════════════════════════════════════════════

    public void release() {
        if (visualizer != null) {
            visualizer.release();
            visualizer = null;
        }
        initialized = false;
        Log.d(TAG, "🧹 MusicSystem liberado");
    }

    public static void reset() {
        if (instance != null) {
            instance.release();
            instance = null;
        }
    }
}
