package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import com.secret.blackholeglow.systems.ScreenManager;
import com.secret.blackholeglow.util.ObjLoader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.List;

/**
 * UniverseBackground MEJORADO:
 * - Adaptación perfecta al aspect ratio del dispositivo
 * - Detección automática de orientación (portrait/landscape)
 * - Sin cortes ni espacios vacíos
 */
public class UniverseBackground
        extends BaseShaderProgram
        implements SceneObject, CameraAware {

    private static final String TAG = "UniverseBackground";

    // Buffers y conteo de índices
    private final FloatBuffer  vertexBuffer, texCoordBuffer;
    private final ShortBuffer  indexBuffer;
    private final int          indexCount;

    // Locaciones GLSL
    private final int aPosLoc, aTexLoc;
    private final int uMvpLoc, uAlphaLoc, uTimeLoc, uTexLoc, uResolutionLoc;

    // Textura y transparencia
    private final boolean hasTexture;
    private final int     textureId;
    private final float   alpha;

    // Para animaciones
    private final float timeOffset;

    // Control de cámara
    private CameraController camera;

    // Métricas para debug
    private long frameCount = 0;
    private long lastLogTime = System.currentTimeMillis();

    // ⚡ OPTIMIZACIÓN: Matrices reutilizables (evitar allocations en draw)
    private final float[] modelCache = new float[16];
    private final float[] mvpCache = new float[16];

    /**
     * Inyecta el CameraController para usar perspectiva y vista.
     */
    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    public UniverseBackground(
            Context ctx,
            TextureManager texMgr,
            String vertShaderAsset,
            String fragShaderAsset,
            Integer textureResId,
            float alpha
    ) {
        super(ctx, vertShaderAsset, fragShaderAsset);
        Log.d(TAG, "========================================");
        Log.d(TAG, "Constructor UniverseBackground");
        Log.d(TAG, "  Vertex Shader: " + vertShaderAsset);
        Log.d(TAG, "  Fragment Shader: " + fragShaderAsset);
        Log.d(TAG, "  Texture Resource: " + textureResId);
        Log.d(TAG, "  Alpha: " + alpha);
        Log.d(TAG, "========================================");

        this.alpha      = alpha;
        this.timeOffset = SystemClock.uptimeMillis() * 0.001f;

        // Habilita test de profundidad y culling
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Carga la malla del plano
        ObjLoader.Mesh mesh;
        try {
            mesh = ObjLoader.loadObj(ctx, "plano.obj");
            Log.d(TAG, "✓ Mesh loaded: vertices=" + mesh.vertexCount +
                    ", faces=" + mesh.faces.size());
        } catch (IOException e) {
            Log.e(TAG, "✗ Error loading plano.obj", e);
            throw new RuntimeException(e);
        }
        vertexBuffer   = mesh.vertexBuffer;
        texCoordBuffer = mesh.uvBuffer;

        // Índices para triangulación
        List<int[]> faces = mesh.faces;  // ✅ int[] para compatibilidad con modelos grandes
        int tris = 0;
        for (int[] f : faces) tris += f.length - 2;
        indexCount = tris * 3;
        ShortBuffer ib = ByteBuffer
                .allocateDirect(indexCount * Short.BYTES)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        for (int[] f : faces) {
            short v0 = (short) f[0];  // ✅ Cast a short (plano.obj es pequeño)
            for (int k = 1; k < f.length - 1; k++) {
                ib.put(v0).put((short) f[k]).put((short) f[k + 1]);
            }
        }
        ib.position(0);
        indexBuffer = ib;

        // Obtiene locaciones de atributos y uniforms
        aPosLoc         = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexLoc         = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uMvpLoc         = GLES20.glGetUniformLocation(programId, "u_MVP");
        uAlphaLoc       = GLES20.glGetUniformLocation(programId, "u_Alpha");
        uTimeLoc        = GLES20.glGetUniformLocation(programId, "u_Time");
        uTexLoc         = GLES20.glGetUniformLocation(programId, "u_Texture");
        uResolutionLoc  = GLES20.glGetUniformLocation(programId, "u_Resolution");

        Log.d(TAG, "Shader Locations:");
        Log.d(TAG, "  a_Position: " + aPosLoc);
        Log.d(TAG, "  a_TexCoord: " + aTexLoc);
        Log.d(TAG, "  u_MVP: " + uMvpLoc);
        Log.d(TAG, "  u_Alpha: " + uAlphaLoc);
        Log.d(TAG, "  u_Time: " + uTimeLoc);
        Log.d(TAG, "  u_Texture: " + uTexLoc);
        Log.d(TAG, "  u_Resolution: " + uResolutionLoc);

        // Textura opcional
        if (textureResId != null) {
            hasTexture = true;
            textureId  = texMgr.getTexture(textureResId);
            Log.d(TAG, "✓ Texture loaded: resourceId=" + textureResId +
                    ", textureId=" + textureId);
        } else {
            hasTexture = false;
            textureId  = -1;
            Log.d(TAG, "ℹ No texture specified");
        }
    }

    @Override
    public void update(float dt) {
        // Sin animaciones de modelo aquí
    }

    public void release() {
        if (hasTexture && textureId > 0) {
            int[] textures = { textureId };
            GLES20.glDeleteTextures(1, textures, 0);
            Log.d(TAG, "✓ Texture released: " + textureId);
        }
    }

    @Override
    public void draw() {
        frameCount++;

        if (camera == null) {
            Log.e(TAG, "✗ CameraController not injected, draw aborted.");
            return;
        }

        useProgram();

        // Desactivar depth test para que el fondo siempre esté atrás
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDepthMask(false);

        float screenWidth = ScreenManager.getWidth();
        float screenHeight = ScreenManager.getHeight();
        float screenAspect = screenWidth / screenHeight;
        boolean isPortrait = screenHeight > screenWidth;

        // Log detallado cada segundo
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLogTime > 1000) {
            Log.d(TAG, "╔══════════════════════════════════════╗");
            Log.d(TAG, "║     UNIVERSE BACKGROUND METRICS      ║");
            Log.d(TAG, "╠══════════════════════════════════════╣");
            Log.d(TAG, "║ Screen Resolution: " +
                  String.format("%-18s", (int)screenWidth + "x" + (int)screenHeight) + "║");
            Log.d(TAG, "║ Aspect Ratio: " +
                  String.format("%-23.2f", screenAspect) + "║");
            Log.d(TAG, "║ Orientation: " +
                  String.format("%-24s", isPortrait ? "PORTRAIT" : "LANDSCAPE") + "║");
            Log.d(TAG, "║ Frame Count: " +
                  String.format("%-24d", frameCount) + "║");
            Log.d(TAG, "╚══════════════════════════════════════╝");
            lastLogTime = currentTime;
        }

        // ⚡ OPTIMIZADO: Usar matriz cacheada
        Matrix.setIdentityM(modelCache, 0);

        // NUEVA LÓGICA DE POSICIONAMIENTO ADAPTATIVO
        float distancia = -30f;  // Más cerca para mejor control
        float offsetY = 0f;      // Sin offset vertical por defecto

        // Ajuste dinámico basado en orientación
        if (isPortrait) {
            distancia = -25f;    // Más cerca en portrait
            offsetY = 0f;        // Centrado
        } else {
            distancia = -35f;    // Más lejos en landscape
            offsetY = 0f;        // Centrado
        }

        Matrix.translateM(modelCache, 0, 0f, offsetY, distancia);

        // Calcula escala adaptativa MEJORADA
        float fovDegrees = camera.getFOV();
        float fovRad = (float) Math.toRadians(fovDegrees);

        // Cálculo preciso del área visible
        float alturaVisible = 2.0f * (float) Math.tan(fovRad / 2.0f) * Math.abs(distancia);
        float anchoVisible = alturaVisible * screenAspect;

        // Factor de seguridad adaptativo según orientación
        float factorSeguridad = isPortrait ? 2.2f : 1.8f;

        // Aplicar factor de escala adicional para garantizar cobertura completa
        float escalaX = anchoVisible * factorSeguridad;
        float escalaY = alturaVisible * factorSeguridad;

        // Ajuste adicional para mantener proporción de la imagen
        if (isPortrait) {
            escalaX *= 1.2f;  // Expandir más en X para portrait
        } else {
            escalaY *= 1.2f;  // Expandir más en Y para landscape
        }

        Matrix.scaleM(modelCache, 0, escalaX, escalaY, 1f);
        Matrix.rotateM(modelCache, 0, 90f, 1f, 0f, 0f);

        // Calcula MVP (⚡ OPTIMIZADO: usar mvpCache)
        camera.computeMvp(modelCache, mvpCache);
        GLES20.glUniformMatrix4fv(uMvpLoc, 1, false, mvpCache, 0);

        // Envía uniforms
        GLES20.glUniform1f(uAlphaLoc, alpha);
        float t = (SystemClock.uptimeMillis() * 0.001f - timeOffset) % 60.0f;
        GLES20.glUniform1f(uTimeLoc, t);
        GLES20.glUniform2f(uResolutionLoc, screenWidth, screenHeight);

        // Textura
        if (hasTexture) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(uTexLoc, 0);
        }

        // Dibuja
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        if (aTexLoc >= 0) {
            texCoordBuffer.position(0);
            GLES20.glEnableVertexAttribArray(aTexLoc);
            GLES20.glVertexAttribPointer(aTexLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);
        }
        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        // Restaura estados
        GLES20.glDisableVertexAttribArray(aPosLoc);
        if (aTexLoc >= 0) GLES20.glDisableVertexAttribArray(aTexLoc);
        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }
}