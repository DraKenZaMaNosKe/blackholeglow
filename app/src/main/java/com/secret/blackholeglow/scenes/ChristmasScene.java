package com.secret.blackholeglow.scenes;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import com.secret.blackholeglow.CameraAware;
import com.secret.blackholeglow.CameraController;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.SceneObject;
import com.secret.blackholeglow.TextureManager;
import com.secret.blackholeglow.christmas.SnowParticles;
import com.secret.blackholeglow.christmas.ChristmasBackground;
import com.secret.blackholeglow.christmas.ChristmasTree;
import com.secret.blackholeglow.christmas.SnowGround;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                                                                           ║
 * ║   🎄 ChristmasScene - Bosque Navideño Mágico 🎄                          ║
 * ║                                                                           ║
 * ║   ❄️ "Un bosque encantado donde la magia de la Navidad cobra vida" ❄️    ║
 * ║                                                                           ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                           ║
 * ║   ELEMENTOS DE LA ESCENA:                                                 ║
 * ║   ┌─────────────────────────────────────────────────────────────────┐    ║
 * ║   │  🌲 Fondo: Bosque nevado nocturno con aurora boreal             │    ║
 * ║   │  🎄 Árbol: Pino navideño con ramas que se mueven con el viento  │    ║
 * ║   │  🔴 Esferas: Ornamentos brillantes con reflejos                 │    ║
 * ║   │  ⭐ Estrella: Corona luminosa en la punta con glow              │    ║
 * ║   │  💡 Luces: Lucecitas parpadeantes multicolor                    │    ║
 * ║   │  ❄️ Nieve: Partículas GPU cayendo con física de viento          │    ║
 * ║   │  🏔️ Suelo: Textura de nieve con relieve                         │    ║
 * ║   │  🎁 Regalos: Cajas decorativas bajo el árbol                    │    ║
 * ║   └─────────────────────────────────────────────────────────────────┘    ║
 * ║                                                                           ║
 * ║   SHADERS ESPECIALES:                                                     ║
 * ║   • Wind Vertex Shader - Ondulación de ramas                             ║
 * ║   • Snow Particle Shader - Física de copos con turbulencia               ║
 * ║   • Glow Fragment Shader - Brillo de estrella y luces                    ║
 * ║   • Reflective Shader - Reflejos en esferas                              ║
 * ║                                                                           ║
 * ║   INTERACTIVIDAD:                                                         ║
 * ║   • Touch en árbol → Ráfaga de nieve                                     ║
 * ║   • Touch en esferas → Brillan y giran                                   ║
 * ║   • Música → Luces parpadean al ritmo                                    ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */
public class ChristmasScene extends WallpaperScene {

    private static final String TAG = "ChristmasScene";

    // ═══════════════════════════════════════════════════════════════
    // 🎄 OBJETOS DE LA ESCENA
    // ═══════════════════════════════════════════════════════════════

    // Fondo y ambiente
    private ChristmasBackground background;      // 🌲 Fondo de bosque nevado
    private SnowGround snowGround;               // 🏔️ Suelo con nieve

    // Partículas
    private SnowParticles snowParticles;         // ❄️ Sistema de nieve cayendo

    // Árbol de Navidad
    private ChristmasTree christmasTree;         // 🎄 Árbol principal
    // private ChristmasOrnaments ornaments;     // 🔴 Esferas decorativas
    // private ChristmasLights lights;           // 💡 Luces parpadeantes
    // private TreeStar treeStar;                // ⭐ Estrella en la punta
    // private GiftBox[] giftBoxes;              // 🎁 Cajas de regalo

    // ═══════════════════════════════════════════════════════════════
    // 🎮 CONFIGURACIÓN DE LA ESCENA (desde SceneConstants.Christmas)
    // ═══════════════════════════════════════════════════════════════
    // Todos los parámetros configurables están en SceneConstants.Christmas
    // para facilitar ajustes sin buscar en el código

    // ═══════════════════════════════════════════════════════════════
    // 📋 MÉTODOS ABSTRACTOS DE WallpaperScene
    // ═══════════════════════════════════════════════════════════════

    @Override
    public String getName() {
        return "Bosque Navideño";
    }

    @Override
    public String getDescription() {
        return "Un mágico bosque nevado con un hermoso árbol de Navidad. " +
               "Observa la nieve caer mientras las luces parpadean al ritmo de tu música.";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.christmas_background;
    }

    // ═══════════════════════════════════════════════════════════════
    // 🎬 SETUP DE LA ESCENA
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected void setupScene() {
        Log.d(TAG, "╔═══════════════════════════════════════════════════════╗");
        Log.d(TAG, "║   🎄 CONFIGURANDO ESCENA NAVIDEÑA                     ║");
        Log.d(TAG, "╚═══════════════════════════════════════════════════════╝");

        // 1️⃣ Fondo de bosque nevado
        setupBackground();

        // 2️⃣ Árbol de Navidad 3D (Meshy Pine - 9,927 triángulos)
        setupChristmasTree();

        // ❌ DESHABILITADO:
        // setupSnowGround();      // Suelo con nieve
        // setupSnowParticles();   // Partículas de nieve

        Log.d(TAG, "✅ Escena navideña: Fondo + Árbol 3D");
    }

    // ═══════════════════════════════════════════════════════════════
    // 🌲 SETUP INDIVIDUAL DE COMPONENTES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Configura el fondo de bosque nevado nocturno
     */
    private void setupBackground() {
        try {
            background = new ChristmasBackground(context, textureManager);
            background.setCameraController(camera);
            addSceneObject(background);
            Log.d(TAG, "  ✓ 🌲 Fondo de bosque nevado agregado");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando fondo: " + e.getMessage());
        }
    }

    /**
     * Configura el suelo con textura de nieve
     */
    private void setupSnowGround() {
        try {
            snowGround = new SnowGround(context, textureManager);
            snowGround.setCameraController(camera);
            addSceneObject(snowGround);
            Log.d(TAG, "  ✓ 🏔️ Suelo nevado agregado");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando suelo: " + e.getMessage());
        }
    }

    /**
     * Configura el sistema de partículas de nieve
     */
    private void setupSnowParticles() {
        try {
            snowParticles = new SnowParticles(
                context,
                SceneConstants.Christmas.SNOW_PARTICLE_COUNT,
                SceneConstants.Christmas.SNOW_FALL_SPEED,
                SceneConstants.Christmas.SNOW_WIND_STRENGTH
            );
            snowParticles.setCameraController(camera);
            addSceneObject(snowParticles);
            Log.d(TAG, "  ✓ ❄️ Sistema de nieve agregado (" + SceneConstants.Christmas.SNOW_PARTICLE_COUNT + " copos)");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando nieve: " + e.getMessage());
        }
    }

    /**
     * Configura el árbol de Navidad principal
     */
    private void setupChristmasTree() {
        try {
            christmasTree = new ChristmasTree(context, textureManager);
            christmasTree.setCameraController(camera);
            // Posición centrada, en frente y visible
            // Camera está en (4,3,6) mirando a (0,0,0)
            christmasTree.setPosition(0f, -1.0f, 1.0f);  // Z positivo = más cerca de cámara
            christmasTree.setScale(1.2f);  // Escala mayor para ser visible
            christmasTree.setWindStrength(0.015f);
            addSceneObject(christmasTree);
            Log.d(TAG, "  ✓ 🎄 Árbol de Navidad agregado");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando árbol: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🎮 INTERACTIVIDAD
    // ═══════════════════════════════════════════════════════════════

    @Override
    public boolean onTouchEvent(float normalizedX, float normalizedY, int action) {
        if (action == MotionEvent.ACTION_DOWN) {
            // Crear ráfaga de nieve al tocar
            if (snowParticles != null) {
                snowParticles.createBurst(normalizedX, normalizedY);
            }
            // Sacudir el árbol al tocar
            if (christmasTree != null) {
                christmasTree.shake(0.15f);
            }
            Log.d(TAG, "❄️ Touch en (" + normalizedX + ", " + normalizedY + ")");
            return true;
        }
        return false;
    }

    /**
     * Aumenta la intensidad de la nieve temporalmente
     */
    public void intensifySnow(float duration) {
        if (snowParticles != null) {
            snowParticles.intensify(duration);
        }
    }

    /**
     * Activa el modo musical para las luces
     * @param bassLevel Nivel de bajos (0-1)
     * @param midLevel Nivel de medios (0-1)
     * @param highLevel Nivel de agudos (0-1)
     */
    public void onMusicUpdate(float bassLevel, float midLevel, float highLevel) {
        // TODO: Implementar cuando tengamos las luces
        // if (lights != null) {
        //     lights.pulseWithMusic(bassLevel, midLevel, highLevel);
        // }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🗑️ LIBERACIÓN DE RECURSOS
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected void releaseSceneResources() {
        Log.d(TAG, "🗑️ Liberando recursos de ChristmasScene...");

        // Las referencias se limpian, los objetos se liberan en el loop de WallpaperScene
        background = null;
        snowGround = null;
        snowParticles = null;
        christmasTree = null;

        Log.d(TAG, "✓ Recursos de ChristmasScene liberados");
    }
}
