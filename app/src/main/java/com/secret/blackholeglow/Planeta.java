// Planeta.java - VERSIÓN CORREGIDA
package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.util.ObjLoader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.List;

/**
 * Planeta CORREGIDO - Ahora usa CameraController correctamente
 */
public class Planeta extends BaseShaderProgram implements SceneObject, CameraAware {
    private static final String TAG = "Planeta";

    // Parámetros de configuración
    private final float uvScale;
    private final int textureId;
    private final float orbitRadiusX, orbitRadiusZ, orbitSpeed;
    private final float scaleAmplitude, instanceScale, spinSpeed;
    private final boolean useSolidColor;
    private final float[] solidColor;
    private final float alpha;
    private final Float scaleOscPercent;

    // Uniform locations
    private final int aPosLoc, aTexLoc;
    private final int uTexLoc, uUseSolidColorLoc, uSolidColorLoc, uAlphaLoc;
    private final int uUvScaleLoc;

    // Buffers y conteos
    private final FloatBuffer vertexBuffer, texCoordBuffer;
    private final ShortBuffer indexBuffer;
    private final int indexCount;

    // Estado dinámico
    private float orbitAngle = 0f, rotation = 0f, accumulatedTime = 0f;

    // Matrices temporales
    private final float[] model = new float[16];
    private final float[] mvp = new float[16];

    // ===== CAMERA CONTROLLER =====
    private CameraController camera;

    // Constantes mejoradas
    private static final float BASE_SCALE = 1.0f; // Escala base más grande
    private static final float SCALE_OSC_FREQ = 0.2f;

    /**
     * Constructor
     */
    public Planeta(Context ctx,
                   TextureManager texMgr,
                   String vertexShaderAssetPath,
                   String fragmentShaderAssetPath,
                   int textureResId,
                   float orbitRadiusX,
                   float orbitRadiusZ,
                   float orbitSpeed,
                   float scaleAmplitude,
                   float instanceScale,
                   float spinSpeed,
                   boolean useSolidColor,
                   float[] solidColor,
                   float alpha,
                   Float scaleOscPercent,
                   float uvScale) {
        super(ctx, vertexShaderAssetPath, fragmentShaderAssetPath);

        this.uvScale = uvScale;
        this.orbitRadiusX = orbitRadiusX;
        this.orbitRadiusZ = orbitRadiusZ;
        this.orbitSpeed = orbitSpeed;
        this.scaleAmplitude = scaleAmplitude;
        this.instanceScale = instanceScale;
        this.spinSpeed = spinSpeed;
        this.useSolidColor = useSolidColor;
        this.solidColor = (solidColor != null) ? solidColor : new float[]{1f,1f,1f,1f};
        this.alpha = alpha;
        this.scaleOscPercent = scaleOscPercent;

        Log.d(TAG, String.format("Creando planeta: orbit(%.2f,%.2f) scale:%.2f spin:%.1f",
                orbitRadiusX, orbitRadiusZ, instanceScale, spinSpeed));

        // Habilitar depth test y culling
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Cargar textura
        textureId = texMgr.getTexture(textureResId);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

        // Cargar malla .obj
        ObjLoader.Mesh mesh;
        try {
            mesh = ObjLoader.loadObj(ctx, "planeta.obj");
            Log.d(TAG, "Malla cargada: " + mesh.vertexCount + " vértices");
        } catch (IOException e) {
            throw new RuntimeException("Error cargando planeta.obj", e);
        }

        vertexBuffer = mesh.vertexBuffer;
        texCoordBuffer = mesh.uvBuffer;

        // Construir índices
        List<short[]> faces = mesh.faces;
        int triCount = 0;
        for (short[] f: faces) triCount += f.length - 2;
        indexCount = triCount * 3;

        ShortBuffer ib = ByteBuffer
                .allocateDirect(indexCount * Short.BYTES)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        for (short[] f: faces) {
            short v0 = f[0];
            for (int i = 1; i < f.length-1; i++) {
                ib.put(v0).put(f[i]).put(f[i+1]);
            }
        }
        ib.position(0);
        indexBuffer = ib;

        // Obtener uniform locations
        aPosLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uTexLoc = GLES20.glGetUniformLocation(programId, "u_Texture");
        uUseSolidColorLoc = GLES20.glGetUniformLocation(programId, "u_UseSolidColor");
        uSolidColorLoc = GLES20.glGetUniformLocation(programId, "u_SolidColor");
        uAlphaLoc = GLES20.glGetUniformLocation(programId, "u_Alpha");
        uUvScaleLoc = GLES20.glGetUniformLocation(programId, "u_UvScale");

        Log.d(TAG, "Planeta inicializado correctamente");
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
        Log.d(TAG, "CameraController asignado al planeta");
    }

    @Override
    public void update(float dt) {
        // Rotación propia
        rotation = (rotation + dt * spinSpeed) % 360f;

        // Órbita
        if (orbitRadiusX > 0 && orbitRadiusZ > 0 && orbitSpeed > 0) {
            orbitAngle = (orbitAngle + dt * orbitSpeed) % (2f * (float)Math.PI);
        }

        accumulatedTime += dt;
    }

    @Override
    public void draw() {
        if (camera == null) {
            Log.e(TAG, "ERROR: CameraController no asignado!");
            return;
        }

        useProgram();

        // Fase de animación
        float phase = (accumulatedTime % 0.5f) * 2f * (float)Math.PI / 0.5f;
        setTime(phase);

        // Enviar factor de tiling
        if (uUvScaleLoc >= 0) {
            GLES20.glUniform1f(uUvScaleLoc, uvScale);
        }

        // ===== CONSTRUIR MATRIZ MODELO =====
        Matrix.setIdentityM(model, 0);

        // 1. Aplicar órbita (traslación)
        if (orbitRadiusX > 0 && orbitRadiusZ > 0) {
            float ox = orbitRadiusX * (float)Math.cos(orbitAngle);
            float oz = orbitRadiusZ * (float)Math.sin(orbitAngle);
            Matrix.translateM(model, 0, ox, 0, oz);

            Log.v(TAG, String.format("Órbita: x=%.2f z=%.2f angle=%.2f", ox, oz, orbitAngle));
        }

        // 2. Calcular escala final
        float finalScale = BASE_SCALE * instanceScale;

        // Añadir variación dinámica si está configurada
        if (scaleOscPercent != null) {
            float s = 0.5f + 0.5f * (float)Math.sin(accumulatedTime * SCALE_OSC_FREQ * 2f * Math.PI);
            float osc = scaleOscPercent + (1f - scaleOscPercent) * s;
            finalScale *= osc;
        }

        // 3. Aplicar escala
        Matrix.scaleM(model, 0, finalScale, finalScale, finalScale);

        // 4. Aplicar rotación
        Matrix.rotateM(model, 0, rotation, 0, 1, 0);

        // ===== USAR CAMERACONTROLLER PARA MVP =====
        camera.computeMvp(model, mvp);

        // Enviar MVP al shader
        setMvpAndResolution(mvp, SceneRenderer.screenWidth, SceneRenderer.screenHeight);

        // Configurar textura
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTexLoc, 0);

        // Configurar color
        GLES20.glUniform1i(uUseSolidColorLoc, useSolidColor ? 1 : 0);
        GLES20.glUniform4fv(uSolidColorLoc, 1, solidColor, 0);
        GLES20.glUniform1f(uAlphaLoc, alpha);

        // Configurar atributos de vértices
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        if (aTexLoc >= 0) {
            texCoordBuffer.position(0);
            GLES20.glEnableVertexAttribArray(aTexLoc);
            GLES20.glVertexAttribPointer(aTexLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);
        }

        // Dibujar
        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPosLoc);
        if (aTexLoc >= 0) {
            GLES20.glDisableVertexAttribArray(aTexLoc);
        }
    }
}