package com.secret.blackholeglow.systems;

import android.opengl.GLES30;
import android.util.Log;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                      GLStateManager                              ║
 * ║                   "El Maestro de OpenGL"                         ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  Actor especializado en configuracion y estado de OpenGL ES 3.0  ║
 * ║                                                                  ║
 * ║  RESPONSABILIDADES:                                              ║
 * ║  • Configuracion inicial de OpenGL                               ║
 * ║  • Gestion de estados GL (depth, blend, cull)                    ║
 * ║  • Tracking de FPS y rendimiento                                 ║
 * ║  • Clear de buffers                                              ║
 * ║  • Configuracion de viewport                                     ║
 * ║                                                                  ║
 * ║  PRINCIPIOS:                                                     ║
 * ║  • Maximo 10 metodos publicos                                    ║
 * ║  • Un solo proposito: estado de OpenGL                           ║
 * ║  • Sin dependencias de otros actores                             ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * USO:
 *   GLStateManager.get().initialize();
 *   GLStateManager.get().beginFrame();
 *   // ... renderizado ...
 *   GLStateManager.get().endFrame();
 */
public class GLStateManager {
    private static final String TAG = "GLStateManager";

    // ═══════════════════════════════════════════════════════════════
    // SINGLETON
    // ═══════════════════════════════════════════════════════════════
    private static GLStateManager instance;

    public static GLStateManager get() {
        if (instance == null) {
            instance = new GLStateManager();
        }
        return instance;
    }

    // ═══════════════════════════════════════════════════════════════
    // CONFIGURACION
    // ═══════════════════════════════════════════════════════════════

    // Clear color (fondo espacial oscuro por defecto)
    private float clearR = 0.02f;
    private float clearG = 0.02f;
    private float clearB = 0.05f;
    private float clearA = 1.0f;

    // Estados actuales
    private boolean depthTestEnabled = false;
    private boolean blendEnabled = false;
    private boolean cullFaceEnabled = false;
    private boolean initialized = false;

    // FPS Tracking
    private long lastFrameTime = System.nanoTime();
    private int frameCount = 0;
    private float fpsTimer = 0f;
    private float currentFPS = 0f;
    private float deltaTime = 0f;

    private GLStateManager() {
        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   🎮 GLStateManager Creado             ║");
        Log.d(TAG, "╚════════════════════════════════════════╝");
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. INICIALIZACION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Inicializa OpenGL con configuracion optima para el wallpaper 3D
     * Llamar en onSurfaceCreated
     */
    public void initialize() {
        Log.d(TAG, "Inicializando OpenGL ES 3.0...");

        // Depth Test: Necesario para renderizado 3D correcto
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glDepthFunc(GLES30.GL_LESS);
        depthTestEnabled = true;

        // Blending: Para transparencias y efectos
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        blendEnabled = true;

        // Face Culling: Optimizacion (no dibujar caras traseras)
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glCullFace(GLES30.GL_BACK);
        GLES30.glFrontFace(GLES30.GL_CCW);
        cullFaceEnabled = true;

        // Clear color
        GLES30.glClearColor(clearR, clearG, clearB, clearA);

        // Hints para mejor calidad
        GLES30.glHint(GLES30.GL_GENERATE_MIPMAP_HINT, GLES30.GL_NICEST);

        initialized = true;
        lastFrameTime = System.nanoTime();

        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   ✓ OpenGL ES 3.0 Inicializado         ║");
        Log.d(TAG, "╠════════════════════════════════════════╣");
        Log.d(TAG, "║   Depth Test: ON                       ║");
        Log.d(TAG, "║   Blending: ON (SRC_ALPHA)             ║");
        Log.d(TAG, "║   Face Culling: ON (BACK)              ║");
        Log.d(TAG, "╚════════════════════════════════════════╝");
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. VIEWPORT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Configura el viewport de OpenGL
     * Llamar en onSurfaceChanged
     */
    public void setViewport(int width, int height) {
        GLES30.glViewport(0, 0, width, height);
        Log.d(TAG, "Viewport: " + width + "x" + height);
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. FRAME LIFECYCLE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Inicia un nuevo frame: calcula deltaTime y limpia buffers
     * Llamar al inicio de onDrawFrame
     * @return deltaTime en segundos
     */
    public float beginFrame() {
        // Calcular delta time
        long now = System.nanoTime();
        deltaTime = (now - lastFrameTime) / 1_000_000_000f;
        lastFrameTime = now;

        // Clamp para evitar saltos grandes (ej: al volver de pausa)
        deltaTime = Math.min(deltaTime, 0.1f);

        // FPS tracking
        frameCount++;
        fpsTimer += deltaTime;
        if (fpsTimer >= 1.0f) {
            currentFPS = frameCount / fpsTimer;
            frameCount = 0;
            fpsTimer = 0f;
        }

        // Limpiar buffers
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        return deltaTime;
    }

    /**
     * Finaliza el frame actual
     * Llamar al final de onDrawFrame (opcional, para future use)
     */
    public void endFrame() {
        // Reservado para flush de comandos si es necesario
        // GLES30.glFinish(); // Solo si hay problemas de sincronizacion
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. ESTADO DE DEPTH TEST
    // ═══════════════════════════════════════════════════════════════

    /**
     * Habilita/deshabilita depth test
     * Usar false para UI 2D, true para objetos 3D
     */
    public void setDepthTest(boolean enabled) {
        if (depthTestEnabled != enabled) {
            if (enabled) {
                GLES30.glEnable(GLES30.GL_DEPTH_TEST);
            } else {
                GLES30.glDisable(GLES30.GL_DEPTH_TEST);
            }
            depthTestEnabled = enabled;
        }
    }

    /**
     * Habilita/deshabilita escritura en depth buffer
     */
    public void setDepthMask(boolean enabled) {
        GLES30.glDepthMask(enabled);
    }

    // ═══════════════════════════════════════════════════════════════
    // 5. ESTADO DE BLENDING
    // ═══════════════════════════════════════════════════════════════

    /**
     * Habilita/deshabilita blending
     */
    public void setBlending(boolean enabled) {
        if (blendEnabled != enabled) {
            if (enabled) {
                GLES30.glEnable(GLES30.GL_BLEND);
            } else {
                GLES30.glDisable(GLES30.GL_BLEND);
            }
            blendEnabled = enabled;
        }
    }

    /**
     * Configura el modo de blending
     * @param mode "alpha" (default), "additive", "multiply"
     */
    public void setBlendMode(String mode) {
        switch (mode) {
            case "additive":
                // Efectos de luz, particulas brillantes
                GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE);
                break;
            case "multiply":
                // Sombras, efectos oscuros
                GLES30.glBlendFunc(GLES30.GL_DST_COLOR, GLES30.GL_ZERO);
                break;
            case "alpha":
            default:
                // Standard alpha blending
                GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
                break;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 6. CLEAR COLOR
    // ═══════════════════════════════════════════════════════════════

    /**
     * Configura el color de fondo
     */
    public void setClearColor(float r, float g, float b, float a) {
        clearR = r;
        clearG = g;
        clearB = b;
        clearA = a;
        GLES30.glClearColor(r, g, b, a);
    }

    // ═══════════════════════════════════════════════════════════════
    // 7. GETTERS DE RENDIMIENTO
    // ═══════════════════════════════════════════════════════════════

    /** FPS actual (actualizado cada segundo) */
    public float getFPS() {
        return currentFPS;
    }

    /** Delta time del ultimo frame en segundos */
    public float getDeltaTime() {
        return deltaTime;
    }

    /** Retorna true si OpenGL fue inicializado */
    public boolean isInitialized() {
        return initialized;
    }

    // ═══════════════════════════════════════════════════════════════
    // 8. RESET
    // ═══════════════════════════════════════════════════════════════

    /**
     * Reset del singleton (para recreacion completa)
     */
    public static void reset() {
        if (instance != null) {
            instance.initialized = false;
            instance = null;
        }
        Log.d(TAG, "GLStateManager reset");
    }

    // ═══════════════════════════════════════════════════════════════
    // DEBUG
    // ═══════════════════════════════════════════════════════════════

    /**
     * Imprime estado actual de OpenGL
     */
    public void logState() {
        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   📊 GL STATE                          ║");
        Log.d(TAG, "╠════════════════════════════════════════╣");
        Log.d(TAG, "║   Initialized: " + initialized);
        Log.d(TAG, "║   Depth Test: " + depthTestEnabled);
        Log.d(TAG, "║   Blending: " + blendEnabled);
        Log.d(TAG, "║   Face Culling: " + cullFaceEnabled);
        Log.d(TAG, "║   FPS: " + String.format("%.1f", currentFPS));
        Log.d(TAG, "╚════════════════════════════════════════╝");
    }
}
