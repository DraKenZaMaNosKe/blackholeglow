package com.secret.blackholeglow.filament;

import android.content.Context;
import android.util.Log;

import com.google.android.filament.gltfio.Animator;
import com.google.android.filament.gltfio.FilamentAsset;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   🎅 AnimatedSanta - Santa Claus animado usando Filament                ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Carga el modelo GLB de Santa Claus con soporte para:                   ║
 * ║  • Múltiples animaciones (idle, walk, wave)                             ║
 * ║  • Transiciones suaves entre animaciones                                ║
 * ║  • Control de velocidad de animación                                    ║
 * ║  • Posición y escala configurables                                      ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class AnimatedSanta {
    private static final String TAG = "AnimatedSanta";

    // Filament
    private FilamentHelper filament;
    private FilamentAsset asset;
    private Animator animator;

    // Estado
    private boolean loaded = false;
    private int currentAnimationIndex = 0;
    private float animationTime = 0f;
    private float animationSpeed = 1.0f;
    private boolean looping = true;

    // Transformación
    private float posX = 0f;
    private float posY = 0f;
    private float posZ = 0f;
    private float scale = 1.0f;
    private float rotationY = 0f;

    // Nombres de animaciones disponibles
    private String[] animationNames;

    /**
     * Crea una instancia de Santa animado
     * @param filament Helper de Filament ya inicializado
     */
    public AnimatedSanta(FilamentHelper filament) {
        this.filament = filament;
    }

    /**
     * Carga el modelo GLB desde assets
     * @param glbPath Ruta del archivo GLB en assets
     * @return true si se cargó correctamente
     */
    public boolean load(String glbPath) {
        if (!filament.isInitialized()) {
            Log.e(TAG, "❌ Filament no está inicializado");
            return false;
        }

        Log.d(TAG, "═══════════════════════════════════════════════");
        Log.d(TAG, "🎅 Cargando Santa desde: " + glbPath);
        Log.d(TAG, "═══════════════════════════════════════════════");

        // Cargar modelo
        asset = filament.loadGlbFromAssets(glbPath);

        if (asset == null) {
            Log.e(TAG, "❌ Error cargando modelo de Santa");
            return false;
        }

        // Crear instancia y obtener animator
        asset.getInstance();
        animator = asset.getInstance().getAnimator();

        if (animator == null) {
            Log.w(TAG, "⚠️ El modelo no tiene animaciones");
        } else {
            // Guardar nombres de animaciones
            int count = animator.getAnimationCount();
            animationNames = new String[count];

            Log.d(TAG, "🎬 Animaciones encontradas: " + count);
            for (int i = 0; i < count; i++) {
                animationNames[i] = animator.getAnimationName(i);
                float duration = animator.getAnimationDuration(i);
                Log.d(TAG, "   [" + i + "] " + animationNames[i] + " (" + duration + "s)");
            }
        }

        loaded = true;
        Log.d(TAG, "✅ Santa cargado correctamente");

        return true;
    }

    /**
     * Actualiza la animación
     * @param deltaTime Tiempo desde el último frame en segundos
     */
    public void update(float deltaTime) {
        if (!loaded || animator == null) return;

        // Avanzar tiempo de animación
        animationTime += deltaTime * animationSpeed;

        // Obtener duración de la animación actual
        float duration = animator.getAnimationDuration(currentAnimationIndex);

        // Loop si es necesario
        if (looping && animationTime > duration) {
            animationTime = animationTime % duration;
        }

        // Aplicar animación
        animator.applyAnimation(currentAnimationIndex, animationTime);

        // Actualizar huesos
        animator.updateBoneMatrices();
    }

    /**
     * Cambia a una animación por índice
     * @param index Índice de la animación (0-based)
     */
    public void playAnimation(int index) {
        if (animator == null) return;

        if (index >= 0 && index < animator.getAnimationCount()) {
            currentAnimationIndex = index;
            animationTime = 0f;
            Log.d(TAG, "▶️ Reproduciendo animación [" + index + "]: " +
                  (animationNames != null ? animationNames[index] : "unknown"));
        }
    }

    /**
     * Cambia a una animación por nombre
     * @param name Nombre de la animación
     */
    public void playAnimation(String name) {
        if (animationNames == null) return;

        for (int i = 0; i < animationNames.length; i++) {
            if (animationNames[i].toLowerCase().contains(name.toLowerCase())) {
                playAnimation(i);
                return;
            }
        }

        Log.w(TAG, "⚠️ Animación no encontrada: " + name);
    }

    /**
     * Reproduce la animación de idle (si existe)
     */
    public void playIdle() {
        playAnimation("idle");
    }

    /**
     * Reproduce la animación de caminar (si existe)
     */
    public void playWalk() {
        playAnimation("walk");
    }

    /**
     * Reproduce la animación de correr (si existe)
     */
    public void playRun() {
        playAnimation("run");
    }

    /**
     * Reproduce la animación de saludar/mirarse (si existe)
     */
    public void playWave() {
        playAnimation("mirror");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TRANSFORMACIONES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Establece la posición de Santa
     */
    public void setPosition(float x, float y, float z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        updateTransform();
    }

    /**
     * Establece la escala de Santa
     */
    public void setScale(float scale) {
        this.scale = scale;
        updateTransform();
    }

    /**
     * Establece la rotación en Y (grados)
     */
    public void setRotationY(float degrees) {
        this.rotationY = degrees;
        updateTransform();
    }

    /**
     * Actualiza la matriz de transformación del modelo
     */
    private void updateTransform() {
        if (asset == null) return;

        // TODO: Aplicar transformación usando TransformManager
        // Por ahora el modelo se renderiza en su posición original
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Establece la velocidad de animación
     * @param speed 1.0 = normal, 2.0 = doble velocidad, 0.5 = mitad
     */
    public void setAnimationSpeed(float speed) {
        this.animationSpeed = speed;
    }

    /**
     * Habilita/deshabilita loop de animación
     */
    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean isLoaded() { return loaded; }
    public int getAnimationCount() { return animator != null ? animator.getAnimationCount() : 0; }
    public String[] getAnimationNames() { return animationNames; }
    public int getCurrentAnimationIndex() { return currentAnimationIndex; }
    public float getAnimationTime() { return animationTime; }
    public FilamentAsset getAsset() { return asset; }

    public float getX() { return posX; }
    public float getY() { return posY; }
    public float getZ() { return posZ; }
    public float getScale() { return scale; }

    // ═══════════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Libera recursos del modelo
     */
    public void destroy() {
        if (asset != null && filament != null) {
            filament.destroyAsset(asset);
            asset = null;
        }
        animator = null;
        loaded = false;
        Log.d(TAG, "🗑️ Santa destruido");
    }
}
