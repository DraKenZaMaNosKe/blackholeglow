// Asteroide.java - VERSIÓN CORREGIDA
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
import java.util.Random;

/**
 * Asteroide CORREGIDO - Ahora usa CameraController y es visible
 */
public class Asteroide extends BaseShaderProgram implements SceneObject, CameraAware {
    private static final String TAG = "Asteroide";

    // Configuración de movimiento
    private static final float Z_START = -10f;    // Más cerca
    private static final float Z_END = 10f;       // Más cerca
    private static final float Z_SPEED = 3f;      // Más lento
    private static final float SCALE = 0.5f;      // Escala visible
    private static final float SPAWN_MIN = 5f;
    private static final float SPAWN_MAX = 10f;

    // Parámetros
    private final float instanceScale;
    private final boolean useSolidColor;
    private final float[] solidColor;
    private final float alpha;
    private final float uvScale;
    private final int textureId;

    // Buffers
    private final FloatBuffer vertexBuffer, texCoordBuffer;
    private final ShortBuffer indexBuffer;
    private final int indexCount;

    // Uniform locations
    private final int aPosLoc, aTexLoc;
    private final int uTexLoc, uUseSolidColorLoc, uSolidColorLoc, uAlphaLoc, uUvScaleLoc;

    // Estado
    private final Random rand = new Random();
    private float timer = 0f;
    private float nextSpawn = 0f;
    private boolean active = false;
    private float zPos = Z_START;
    private float xPos = 0f;
    private float yPos = 0f;
    private float rotation = 0f;
    private float rotSpeed;

    // Matrices
    private final float[] model = new float[16];
    private final float[] mvp = new float[16];

    // Camera
    private CameraController camera;

    public Asteroide(Context ctx,
                     TextureManager texMgr,
                     String vertexShaderPath,
                     String fragmentShaderPath,
                     int textureResId,
                     float instanceScale,
                     boolean useSolidColor,
                     float[] solidColor,
                     float alpha,
                     float uvScale) {
        super(ctx, vertexShaderPath, fragmentShaderPath);

        this.instanceScale = instanceScale;
        this.useSolidColor = useSolidColor;
        this.solidColor = (solidColor != null) ? solidColor : new float[]{1f,1f,1f,1f};
        this.alpha = alpha;
        this.uvScale = uvScale;

        Log.d(TAG, "Creando asteroide con escala: " + instanceScale);

        // Cargar textura
        textureId = texMgr.getTexture(textureResId);

        // Cargar malla
        ObjLoader.Mesh mesh;
        try {
            mesh = ObjLoader.loadObj(ctx, "asteroide.obj");
            Log.d(TAG, "Malla de asteroide cargada: " + mesh.vertexCount + " vértices");
        } catch (IOException e) {
            throw new RuntimeException("Error cargando asteroide.obj", e);
        }

        vertexBuffer = mesh.vertexBuffer;
        texCoordBuffer = mesh.uvBuffer;

        // Construir índices
        List<short[]> faces = mesh.faces;
        int triCount = 0;
        for (short[] f : faces) triCount += f.length - 2;
        indexCount = triCount * 3;

        ShortBuffer ib = ByteBuffer
                .allocateDirect(indexCount * Short.BYTES)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        for (short[] f : faces) {
            short v0 = f[0];
            for (int i = 1; i < f.length - 1; i++) {
                ib.put(v0).put(f[i]).put(f[i + 1]);
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

        // Configurar próximo spawn
        nextSpawn = SPAWN_MIN + rand.nextFloat() * (SPAWN_MAX - SPAWN_MIN);

        Log.d(TAG, "Asteroide inicializado. Próximo spawn en: " + nextSpawn + "s");
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
        Log.d(TAG, "CameraController asignado al asteroide");
    }

    @Override
    public void update(float dt) {
        timer += dt;

        // Spawn nuevo asteroide
        if (!active && timer >= nextSpawn) {
            spawn();
        }

        // Actualizar posición si está activo
        if (active) {
            zPos += Z_SPEED * dt;
            rotation += rotSpeed * dt;

            // Pequeño movimiento lateral
            xPos += Math.sin(timer * 2f) * 0.01f;
            yPos += Math.cos(timer * 3f) * 0.01f;

            // Verificar si salió de la vista
            if (zPos >= Z_END) {
                active = false;
                timer = 0f;
                nextSpawn = SPAWN_MIN + rand.nextFloat() * (SPAWN_MAX - SPAWN_MIN);
                Log.d(TAG, "Asteroide desactivado. Próximo en: " + nextSpawn + "s");
            }
        }
    }

    private void spawn() {
        active = true;
        zPos = Z_START;

        // Posición inicial aleatoria
        xPos = (rand.nextFloat() - 0.5f) * 4f;
        yPos = (rand.nextFloat() - 0.5f) * 2f;

        // Rotación aleatoria
        rotation = rand.nextFloat() * 360f;
        rotSpeed = 30f + rand.nextFloat() * 60f;

        Log.d(TAG, "Asteroide spawneado en: x=" + xPos + " y=" + yPos);
    }

    @Override
    public void draw() {
        if (!active) return;

        if (camera == null) {
            Log.e(TAG, "ERROR: CameraController no asignado al asteroide!");
            return;
        }

        useProgram();

        // Tiempo para animación
        setTime(timer);

        // Construir matriz modelo
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, xPos, yPos, zPos);
        Matrix.scaleM(model, 0, SCALE * instanceScale, SCALE * instanceScale, SCALE * instanceScale);
        Matrix.rotateM(model, 0, rotation, 0.5f, 1f, 0.3f);

        // Usar CameraController para MVP
        camera.computeMvp(model, mvp);
        setMvpAndResolution(mvp, SceneRenderer.screenWidth, SceneRenderer.screenHeight);

        // Configurar textura
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTexLoc, 0);

        // Configurar uniforms
        GLES20.glUniform1f(uUvScaleLoc, uvScale);
        GLES20.glUniform1i(uUseSolidColorLoc, useSolidColor ? 1 : 0);
        GLES20.glUniform4fv(uSolidColorLoc, 1, solidColor, 0);
        GLES20.glUniform1f(uAlphaLoc, alpha);

        // Configurar atributos
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