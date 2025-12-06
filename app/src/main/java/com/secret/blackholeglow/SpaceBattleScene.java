// SpaceBattleScene.java - Escena de batalla espacial (DEPRECATED)
// Esta escena ya no se usa - usar BatallaCosmicaScene en su lugar
package com.secret.blackholeglow;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Escena de batalla espacial - DEPRECATED
 * Esta escena ya no se usa, se mantiene por compatibilidad.
 * Usar BatallaCosmicaScene para la experiencia completa.
 */
@Deprecated
public class SpaceBattleScene implements SceneObject, CameraAware {
    private static final String TAG = "SpaceBattleScene";

    // Context y recursos
    private final Context context;
    private final TextureLoader textureLoader;

    // Camera
    private CameraController camera;

    // ✨ Fondo de nebulosa animada
    private UniverseBackground nebulaBackground;

    /**
     * Constructor
     */
    public SpaceBattleScene(Context context, TextureLoader textureLoader) {
        this.context = context;
        this.textureLoader = textureLoader;

        Log.d(TAG, "⚠️ SpaceBattleScene está DEPRECATED - usar BatallaCosmicaScene");
    }

    /**
     * Inicializa la escena (llamar después de setear la cámara)
     */
    public void initialize() {
        Log.d(TAG, "📦 Inicializando escena con Nebulosa...");

        // ═══════════════════════════════════════════════════════════════
        // 1. 🌌 FONDO DE NEBULOSA ANIMADA
        // ═══════════════════════════════════════════════════════════════
        try {
            // Cast TextureLoader a TextureManager (son compatibles)
            TextureManager texManager = (TextureManager) textureLoader;

            nebulaBackground = new UniverseBackground(
                context,
                texManager,
                "shaders/starry_vertex.glsl",
                "shaders/starry_fragment.glsl",
                R.drawable.universo03,  // Textura base
                1.0f  // Alpha
            );

            // Asignar cámara
            if (camera != null) {
                nebulaBackground.setCameraController(camera);
            }

            Log.d(TAG, "  ✓ Nebulosa creada");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando nebulosa: " + e.getMessage());
        }

        Log.d(TAG, "✅ Escena inicializada: Solo Nebulosa (OVNI removido)");
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
        Log.d(TAG, "📷 Cámara asignada");

        // Asignar cámara al fondo si ya existe
        if (nebulaBackground != null) {
            nebulaBackground.setCameraController(camera);
        }
    }

    @Override
    public void update(float deltaTime) {
        // Sin OVNI, no hay nada que actualizar
    }

    @Override
    public void draw() {
        // Configurar estado de OpenGL
        android.opengl.GLES20.glEnable(android.opengl.GLES20.GL_DEPTH_TEST);
        android.opengl.GLES20.glEnable(android.opengl.GLES20.GL_BLEND);
        android.opengl.GLES20.glBlendFunc(
            android.opengl.GLES20.GL_SRC_ALPHA,
            android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA
        );

        // ═══════════════════════════════════════════════════════════════
        // 1. DIBUJAR FONDO PRIMERO (más lejano)
        // ═══════════════════════════════════════════════════════════════
        if (nebulaBackground != null) {
            nebulaBackground.draw();
        }
    }

    /**
     * MANEJA EVENTOS DE TOUCH
     */
    public boolean handleTouch(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            Log.d(TAG, "👆 Touch detectado en (" + event.getX() + ", " + event.getY() + ")");
            return true;
        }
        return false;
    }
}
