package com.secret.blackholeglow.scenes;

import android.util.Log;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.GyroscopeManager;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.TravelingShip;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                    🔥 PYRALIS SCENE - Portal Cósmico                     ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Video de fondo con nubes de fuego + nave Enterprise viajando.           ║
 * ║  Video editado en CapCut con técnica de transiciones seamless.           ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  📖 EJEMPLO DE ESCENA CON GIROSCOPIO:                                    ║
 * ║  Esta escena demuestra cómo usar el GyroscopeManager para                ║
 * ║  controlar objetos 3D por inclinación del dispositivo.                   ║
 * ║                                                                          ║
 * ║  ✅ Video: cielovolando.mp4                                              ║
 * ║  ✅ Tema: PYRALIS (rojo/naranja/amarillo - fuego cósmico)                ║
 * ║  ✅ Objeto 3D: TravelingShip (nave Enterprise)                           ║
 * ║  ✅ Control: Giroscopio + Touch                                          ║
 * ║  ✅ Clock + Battery incluidos automáticamente                            ║
 * ║                                                                          ║
 * ║  🎮 INTERACCIÓN:                                                         ║
 * ║  - Inclinar dispositivo: Mueve la nave en la dirección                   ║
 * ║  - Touch + Drag: Control alternativo de la nave                          ║
 * ║                                                                          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * @see BaseVideoScene para documentación completa de la clase base
 * @see TravelingShip para la nave con control por giroscopio
 * @see GyroscopeManager para el sistema de sensores
 */
public class LabScene extends BaseVideoScene {
    private static final String TAG = "LabScene";

    // ═══════════════════════════════════════════════════════════════════════════
    // 🚀 OBJETOS ESPECÍFICOS DE ESTA ESCENA
    // ═══════════════════════════════════════════════════════════════════════════

    /** Nave Enterprise que viaja por el portal */
    private TravelingShip travelingShip;

    /** Manager del giroscopio para control por inclinación */
    private GyroscopeManager gyroscope;

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎯 MÉTODOS OBLIGATORIOS - Configuración básica de la escena
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public String getName() {
        return "PYRALIS";
    }

    @Override
    public String getDescription() {
        return "Portal cósmico con nubes de fuego";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_space;
    }

    @Override
    protected String getVideoFileName() {
        return "cielovolando.mp4";
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.PYRALIS;  // 🔥 Tema fuego cósmico
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 🔧 HOOKS PARA NAVE + GIROSCOPIO
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Configura la nave Enterprise y el giroscopio.
     *
     * 📖 FLUJO DE CONFIGURACIÓN:
     * 1. Crear TravelingShip con textureManager
     * 2. Crear GyroscopeManager
     * 3. Si el giroscopio está disponible, habilitarlo en la nave
     * 4. Iniciar el giroscopio
     */
    @Override
    protected void setupSceneSpecific() {
        // 🚀 Nave Enterprise
        try {
            travelingShip = new TravelingShip(context, textureManager);
            travelingShip.setCameraController(camera);
            Log.d(TAG, "✅ Nave Enterprise cargada");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error TravelingShip: " + e.getMessage());
        }

        // 📱 Giroscopio para control por inclinación
        try {
            gyroscope = new GyroscopeManager(context);
            if (gyroscope.isAvailable() && travelingShip != null) {
                travelingShip.setGyroEnabled(true);
                gyroscope.start();
                Log.d(TAG, "✅ Giroscopio activado para control de nave");
            } else {
                Log.w(TAG, "⚠️ Giroscopio no disponible en este dispositivo");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error GyroscopeManager: " + e.getMessage());
        }
    }

    /**
     * Actualiza la nave y pasa los datos del giroscopio.
     */
    @Override
    protected void updateSceneSpecific(float deltaTime) {
        // 📱 Pasar datos del giroscopio a la nave
        if (gyroscope != null && travelingShip != null && gyroscope.isEnabled()) {
            travelingShip.setTiltInput(gyroscope.getTiltX(), gyroscope.getTiltY());
        }

        // 🚀 Actualizar nave
        if (travelingShip != null) {
            travelingShip.update(deltaTime);
        }
    }

    /**
     * Dibuja la nave sobre el video pero debajo de la UI.
     */
    @Override
    protected void drawSceneSpecific() {
        if (travelingShip != null) {
            travelingShip.draw();
        }
    }

    /**
     * Libera recursos de la nave y el giroscopio.
     */
    @Override
    protected void releaseSceneSpecificResources() {
        if (travelingShip != null) {
            travelingShip.release();
            travelingShip = null;
        }
        if (gyroscope != null) {
            gyroscope.release();
            gyroscope = null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ⏸️▶️ PAUSE/RESUME - Giroscopio necesita pausarse para ahorrar batería
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Pausa el giroscopio cuando el wallpaper no es visible.
     * Esto ahorra batería significativamente.
     */
    @Override
    protected void onPauseSceneSpecific() {
        if (gyroscope != null) {
            gyroscope.pause();
            Log.d(TAG, "⏸️ Giroscopio PAUSADO (ahorro de batería)");
        }
    }

    /**
     * Reanuda el giroscopio cuando el wallpaper vuelve a ser visible.
     */
    @Override
    protected void onResumeSceneSpecific() {
        if (gyroscope != null && gyroscope.isAvailable()) {
            gyroscope.resume();
            Log.d(TAG, "▶️ Giroscopio REANUDADO");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 👆 TOUCH - Control alternativo de la nave
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Maneja eventos de touch para controlar la nave.
     *
     * 📖 INTERACCIÓN:
     * - ACTION_DOWN: Inicia el arrastre
     * - ACTION_MOVE: Mueve la nave hacia la posición del dedo
     * - ACTION_UP: Termina el arrastre
     *
     * El touch funciona como alternativa/complemento al giroscopio.
     */
    @Override
    public boolean onTouchEvent(float normalizedX, float normalizedY, int action) {
        if (travelingShip == null) return false;

        // Convertir NDC (-1,1) a coordenadas de pantalla
        float screenX = (normalizedX + 1f) * 500f;  // Rango aproximado 0-1000
        float screenY = (normalizedY + 1f) * 500f;

        switch (action) {
            case android.view.MotionEvent.ACTION_DOWN:
                travelingShip.onTouchDown(screenX, screenY);
                return true;
            case android.view.MotionEvent.ACTION_MOVE:
                travelingShip.onTouchMove(screenX, screenY);
                return true;
            case android.view.MotionEvent.ACTION_UP:
            case android.view.MotionEvent.ACTION_CANCEL:
                travelingShip.onTouchUp(screenX, screenY);
                return true;
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 📝 NOTA PARA CLAUDE CODE:
    // ═══════════════════════════════════════════════════════════════════════════
    //
    // Esta escena demuestra el PATRÓN PARA USAR GIROSCOPIO:
    //
    // 1. Crear GyroscopeManager en setupSceneSpecific()
    // 2. Verificar disponibilidad con gyroscope.isAvailable()
    // 3. Iniciar con gyroscope.start()
    // 4. En updateSceneSpecific(), pasar datos al objeto:
    //    gyroscope.getTiltX(), gyroscope.getTiltY()
    // 5. IMPORTANTE: Pausar en onPauseSceneSpecific() para ahorrar batería
    // 6. Reanudar en onResumeSceneSpecific()
    // 7. Liberar en releaseSceneSpecificResources()
    //
    // El giroscopio consume batería, siempre pausarlo cuando no sea visible.
    // ═══════════════════════════════════════════════════════════════════════════
}
