package com.secret.blackholeglow.scenes;

import android.util.Log;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.TextureManager;
import com.secret.blackholeglow.ZombieBody3D;
import com.secret.blackholeglow.ZombieHead3D;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║              🧟 THE WALKING DEAD SCENE - Cementerio Zombie               ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Video de cementerio con manos zombie emergiendo bajo la luna llena.     ║
 * ║  Niebla verde/morada, atmósfera apocalíptica y terror gótico.            ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  📖 EJEMPLO DE ESCENA COMPLEJA CON MÚLTIPLES OBJETOS 3D:                 ║
 * ║  Esta escena demuestra cómo manejar múltiples objetos 3D                 ║
 * ║  con diferentes comportamientos (touch vs automático).                   ║
 * ║                                                                          ║
 * ║  ✅ Video: walkingdeathscene.mp4                                         ║
 * ║  ✅ Tema: WALKING_DEAD (verde tóxico/rojo sangre)                        ║
 * ║  ✅ Objetos 3D:                                                          ║
 * ║     • ZombieHead3D - Cabeza colgante con giro 360° por touch             ║
 * ║     • ZombieBody3D - Cuerpo asomándose con movimiento orgánico           ║
 * ║  ✅ Clock + Battery incluidos automáticamente                            ║
 * ║                                                                          ║
 * ║  🎮 INTERACCIÓN:                                                         ║
 * ║  - Deslizar horizontalmente: Gira la cabeza zombie 360°                  ║
 * ║  - El cuerpo tiene movimiento automático (respiración, temblor)          ║
 * ║                                                                          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * @see BaseVideoScene para documentación completa de la clase base
 * @see ZombieHead3D para la cabeza con giroscopio y touch
 * @see ZombieBody3D para el cuerpo con movimiento orgánico
 */
public class WalkingDeadScene extends BaseVideoScene {
    private static final String TAG = "WalkingDeadScene";

    // ═══════════════════════════════════════════════════════════════════════════
    // 🧟 OBJETOS 3D ESPECÍFICOS DE ESTA ESCENA
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Cabeza zombie colgante.
     *
     * CARACTERÍSTICAS:
     * - Gira 360° con deslizamiento horizontal (touch)
     * - Efecto péndulo con giroscopio
     * - Shader con efectos de sangre, ojos brillantes, niebla
     * - Posición calibrada: x=-0.44, y=2.21, z=-1.94, scale=0.77
     */
    private ZombieHead3D zombieHead;

    /**
     * Cuerpo zombie asomándose desde abajo.
     *
     * CARACTERÍSTICAS:
     * - Movimiento orgánico automático (respiración, temblor, reaching)
     * - Shader con oscurecimiento y ojos brillantes
     * - NO responde al touch (solo la cabeza gira)
     * - Posición calibrada: x=0.63, y=-2.29, z=-0.32, scale=2.38
     */
    private ZombieBody3D zombieBody;

    /** TextureManager propio para los modelos 3D de esta escena */
    private TextureManager localTextureManager;

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎯 MÉTODOS OBLIGATORIOS - Configuración básica de la escena
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public String getName() {
        return "WALKING_DEAD";
    }

    @Override
    public String getDescription() {
        return "The Walking Dead - Cementerio Zombie";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_walkingdead;
    }

    @Override
    protected String getVideoFileName() {
        return "walkingdeathscene.mp4";
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.WALKING_DEAD;  // 🧟 Verde tóxico / Rojo sangre
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 🔧 HOOKS PARA OBJETOS 3D (ZombieHead + ZombieBody)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Configura los zombies 3D.
     *
     * 📖 ORDEN DE CARGA:
     * 1. TextureManager local para los modelos
     * 2. ZombieHead3D (cabeza colgante interactiva)
     * 3. ZombieBody3D (cuerpo con movimiento automático)
     *
     * Los modelos y texturas se cargan desde Supabase (cache local).
     */
    @Override
    protected void setupSceneSpecific() {
        // TextureManager local para los modelos 3D
        localTextureManager = new TextureManager(context);

        // 🧟 Cabeza zombie - GIRO 360° POR TOUCH
        try {
            zombieHead = new ZombieHead3D(context, localTextureManager);
            zombieHead.setScreenSize(screenWidth, screenHeight);
            Log.d(TAG, "✅ ZombieHead3D cargado - GIRO 360° POR TOUCH");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error ZombieHead3D: " + e.getMessage());
        }

        // 🧟 Cuerpo zombie - MOVIMIENTO ORGÁNICO AUTOMÁTICO
        try {
            zombieBody = new ZombieBody3D(context, localTextureManager);
            zombieBody.setScreenSize(screenWidth, screenHeight);
            Log.d(TAG, "✅ ZombieBody3D cargado - MOVIMIENTO ORGÁNICO AUTOMÁTICO");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error ZombieBody3D: " + e.getMessage());
        }
    }

    /**
     * Actualiza los zombies cada frame.
     */
    @Override
    protected void updateSceneSpecific(float deltaTime) {
        if (zombieHead != null) zombieHead.update(deltaTime);
        if (zombieBody != null) zombieBody.update(deltaTime);
    }

    /**
     * Dibuja los zombies sobre el video pero debajo de la UI.
     *
     * 📖 ORDEN DE DIBUJADO (importante para profundidad):
     * 1. ZombieHead (cabeza colgante arriba)
     * 2. ZombieBody (cuerpo asomándose abajo)
     */
    @Override
    protected void drawSceneSpecific() {
        if (zombieHead != null) zombieHead.draw();
        if (zombieBody != null) zombieBody.draw();
    }

    /**
     * Libera recursos de los zombies.
     */
    @Override
    protected void releaseSceneSpecificResources() {
        if (zombieHead != null) {
            zombieHead.dispose();
            zombieHead = null;
        }
        if (zombieBody != null) {
            zombieBody.dispose();
            zombieBody = null;
        }
        if (localTextureManager != null) {
            localTextureManager.release();
            localTextureManager = null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 📐 SCREEN SIZE - Actualizar dimensiones de los zombies
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void setScreenSize(int width, int height) {
        super.setScreenSize(width, height);
        if (zombieHead != null) zombieHead.setScreenSize(width, height);
        if (zombieBody != null) zombieBody.setScreenSize(width, height);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 👆 TOUCH - Solo la cabeza responde (giro 360°)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Maneja eventos de touch para girar la cabeza zombie.
     *
     * 📖 INTERACCIÓN:
     * - Deslizar horizontalmente: Gira la cabeza 360° con momentum
     * - La cabeza sigue girando después de soltar (inercia)
     * - El cuerpo NO responde al touch
     *
     * El giroscopio también afecta sutilmente el movimiento.
     */
    @Override
    public boolean onTouchEvent(float normalizedX, float normalizedY, int action) {
        if (zombieHead != null) {
            return zombieHead.onTouchEvent(normalizedX, normalizedY, action);
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 📝 NOTA PARA CLAUDE CODE:
    // ═══════════════════════════════════════════════════════════════════════════
    //
    // Esta escena es un EJEMPLO COMPLEJO de BaseVideoScene con múltiples objetos.
    //
    // PATRÓN PARA MÚLTIPLES OBJETOS 3D:
    // 1. Declararlos como campos separados
    // 2. Crear TextureManager local si comparten texturas
    // 3. Inicializarlos en setupSceneSpecific() en orden de carga
    // 4. Actualizarlos todos en updateSceneSpecific()
    // 5. Dibujarlos en el orden correcto de profundidad
    // 6. Decidir cuáles responden al touch y cuáles no
    //
    // CALIBRACIÓN DE POSICIÓN:
    // Los valores de x, y, z, scale están en los archivos:
    // - ZombieHead3D.java línea 60-64
    // - ZombieBody3D.java línea 59-67
    //
    // Para recalibrar, habilitar calibrationEnabled = true y usar touch.
    // ═══════════════════════════════════════════════════════════════════════════
}
