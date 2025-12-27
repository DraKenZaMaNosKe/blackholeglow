package com.secret.blackholeglow.scenes;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.ParallaxStars;
import com.secret.blackholeglow.SpeedLines;
import com.secret.blackholeglow.SpaceDust;
import com.secret.blackholeglow.video.MediaCodecVideoRenderer;
import com.secret.blackholeglow.video.VideoSphere3D;
import com.secret.blackholeglow.TravelingShip;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                    🧪 LAB SCENE - Laboratorio de Pruebas                 ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Escena experimental para probar nuevos efectos y componentes.           ║
 * ║                                                                          ║
 * ║  EXPERIMENTOS ACTIVOS:                                                   ║
 * ║  • SpeedLines - Efecto de velocidad/warp                                 ║
 * ║  • ParallaxStars - Estrellas multicapa                                   ║
 * ║  • SpaceDust - Polvo espacial                                            ║
 * ║  • [PRÓXIMO] Nubes tileable con scroll infinito                          ║
 * ║  • [PRÓXIMO] Naves con engine trails                                     ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class LabScene extends WallpaperScene {
    private static final String TAG = "LabScene";

    // ═══════════════════════════════════════════════════════════════
    // 🧪 COMPONENTES EXPERIMENTALES
    // ═══════════════════════════════════════════════════════════════

    private ParallaxStars parallaxStars;
    private SpeedLines speedLines;
    private SpaceDust spaceDust;
    private VideoSphere3D videoSphere;  // 🔥 Sol con video texture
    private MediaCodecVideoRenderer videoBackground;  // 🌌 Video de fondo (nubes volando)
    private TravelingShip travelingShip;  // 🚀 Nave viajando hacia el sol

    private int screenWidth = 1080;
    private int screenHeight = 1920;

    @Override
    public String getName() {
        return "Laboratorio";
    }

    @Override
    public String getDescription() {
        return "Escena experimental para probar nuevos efectos";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_space; // Placeholder
    }

    @Override
    protected void setupScene() {
        Log.d(TAG, "🧪 Configurando Laboratorio de Pruebas...");

        // ═══════════════════════════════════════════════════════════════
        // EXPERIMENTO 0: Video de Fondo (cielovolando.mp4)
        // ═══════════════════════════════════════════════════════════════
        try {
            videoBackground = new MediaCodecVideoRenderer(context, "cielovolando.mp4");
            videoBackground.initialize();
            Log.d(TAG, "✅ Video de fondo activado (cielovolando.mp4)");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error Video de fondo: " + e.getMessage());
        }

        // ═══════════════════════════════════════════════════════════════
        // EXPERIMENTO 1: ParallaxStars (DESACTIVADO - usamos video)
        // ═══════════════════════════════════════════════════════════════
        // parallaxStars desactivado temporalmente
        /*
        try {
            parallaxStars = new ParallaxStars();
            Log.d(TAG, "✅ ParallaxStars activado");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error ParallaxStars: " + e.getMessage());
        }
        */

        // ═══════════════════════════════════════════════════════════════
        // EXPERIMENTO 2: SpeedLines (DESACTIVADO - no gustó)
        // ═══════════════════════════════════════════════════════════════
        // speedLines deshabilitado por ahora

        // ═══════════════════════════════════════════════════════════════
        // EXPERIMENTO 3: SpaceDust (partículas)
        // ═══════════════════════════════════════════════════════════════
        try {
            spaceDust = new SpaceDust(context);
            Log.d(TAG, "✅ SpaceDust activado");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error SpaceDust: " + e.getMessage());
        }

        // ═══════════════════════════════════════════════════════════════
        // EXPERIMENTO 4: VideoSphere3D (sol con video texture)
        // ═══════════════════════════════════════════════════════════════
        try {
            videoSphere = new VideoSphere3D(context, "solmotion.mp4");
            videoSphere.initialize();
            videoSphere.setScreenSize(screenWidth, screenHeight);
            videoSphere.setPosition(0f, 0.5f, -4f);  // ☀️ Arriba como destino
            videoSphere.setScale(0.4f);  // Más grande (destino)
            videoSphere.setRotationSpeed(3f);  // Rotación más lenta
            Log.d(TAG, "✅ VideoSphere3D (Sol) activado");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error VideoSphere3D: " + e.getMessage());
        }

        // ═══════════════════════════════════════════════════════════════
        // EXPERIMENTO 5: TravelingShip (nave viajando hacia el sol)
        // ═══════════════════════════════════════════════════════════════
        try {
            travelingShip = new TravelingShip(context, textureManager);
            travelingShip.setCameraController(camera);
            Log.d(TAG, "✅ TravelingShip activada");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error TravelingShip: " + e.getMessage());
        }

        Log.d(TAG, "🧪 Laboratorio listo para experimentos!");
    }

    @Override
    protected void releaseSceneResources() {
        Log.d(TAG, "🧹 Liberando recursos del laboratorio...");

        if (videoBackground != null) {
            videoBackground.release();
            videoBackground = null;
        }
        if (parallaxStars != null) {
            parallaxStars.cleanup();
            parallaxStars = null;
        }
        if (speedLines != null) {
            speedLines.cleanup();
            speedLines = null;
        }
        if (spaceDust != null) {
            spaceDust = null;
        }
        if (videoSphere != null) {
            videoSphere.release();
            videoSphere = null;
        }
        if (travelingShip != null) {
            travelingShip.release();
            travelingShip = null;
        }
    }

    @Override
    public void update(float deltaTime) {
        if (parallaxStars != null) parallaxStars.update(deltaTime);
        if (speedLines != null) speedLines.update(deltaTime);
        if (spaceDust != null) spaceDust.update(deltaTime);
        if (videoSphere != null) videoSphere.update(deltaTime);
        if (travelingShip != null) travelingShip.update(deltaTime);

        super.update(deltaTime);
    }

    @Override
    public void draw() {
        if (isDisposed) return;

        // Clear
        GLES30.glClearColor(0f, 0f, 0f, 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        // ═══════════════════════════════════════════════════════════════
        // ORDEN DE RENDERIZADO (de atrás hacia adelante)
        // ═══════════════════════════════════════════════════════════════

        // 1. 🌌 VIDEO DE FONDO (nubes volando - cielovolando.mp4)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        if (videoBackground != null) videoBackground.draw();

        // 2. Estrellas parallax (desactivadas - el video ya tiene movimiento)
        // if (parallaxStars != null) parallaxStars.draw();

        // 3. 🔥 SOL 3D con video texture
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        if (videoSphere != null) videoSphere.draw();

        // 4. 🚀 NAVE VIAJANDO HACIA EL SOL
        if (travelingShip != null) travelingShip.draw();

        // 5. Polvo espacial
        if (spaceDust != null) spaceDust.draw();

        // 6. SpeedLines (efecto de velocidad)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        if (speedLines != null) speedLines.draw();

        super.draw();
    }

    @Override
    public void setScreenSize(int width, int height) {
        super.setScreenSize(width, height);
        this.screenWidth = width;
        this.screenHeight = height;
        if (videoSphere != null) videoSphere.setScreenSize(width, height);
        Log.d(TAG, "📐 Screen: " + width + "x" + height);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎛️ CONTROLES DE EXPERIMENTOS (para ajustar en tiempo real)
    // ═══════════════════════════════════════════════════════════════════════════

    public void setSpeedLinesIntensity(float intensity) {
        if (speedLines != null) speedLines.setIntensity(intensity);
    }

    public void setSpeedLinesEnabled(boolean enabled) {
        if (speedLines != null) speedLines.setEnabled(enabled);
    }

    public void setParallaxEnabled(boolean enabled) {
        if (parallaxStars != null) parallaxStars.setEnabled(enabled);
    }
}
