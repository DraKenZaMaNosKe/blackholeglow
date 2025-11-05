package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 🌊 SOLAR WINDS - Vientos Solares Procedurales
 * ═══════════════════════════════════════════════════════════════════════════════
 * Efecto de nubes cósmicas en movimiento usando shaders procedurales con noise.
 * Renderiza un plano 2D grande con transparencia y blending aditivo.
 * ═══════════════════════════════════════════════════════════════════════════════
 */
public class SolarWinds extends BaseShaderProgram implements SceneObject, CameraAware {

    private static final String TAG = "SolarWinds";

    // ══════════════════════════════════════════════════════════════════════════
    // 📦 BUFFERS Y GEOMETRÍA
    // ══════════════════════════════════════════════════════════════════════════

    private final FloatBuffer vertexBuffer;
    private final FloatBuffer texCoordBuffer;
    private final ShortBuffer indexBuffer;
    private final int indexCount;

    // ══════════════════════════════════════════════════════════════════════════
    // 🎨 LOCACIONES DE ATRIBUTOS Y UNIFORMS
    // ══════════════════════════════════════════════════════════════════════════

    private final int aPositionLoc;
    private final int aTexCoordLoc;
    private final int uMVPLoc;
    private final int uTimeLoc;
    private final int uResolutionLoc;

    // ══════════════════════════════════════════════════════════════════════════
    // 🎬 CONTROL DE CÁMARA Y TIEMPO
    // ══════════════════════════════════════════════════════════════════════════

    private CameraController camera;
    private final long startTime;

    // ══════════════════════════════════════════════════════════════════════════
    // 📐 PARÁMETROS DEL PLANO
    // ══════════════════════════════════════════════════════════════════════════

    private float positionX = 0.0f;
    private float positionY = 0.0f;
    private float positionZ = 0.0f;
    private float scale = 15.0f;  // Plano grande para cubrir la escena
    private float rotationX = 0.0f;
    private float rotationY = 0.0f;
    private float rotationZ = 0.0f;

    // Matriz de modelo para transformaciones
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // ══════════════════════════════════════════════════════════════════════════
    // 🏗️ CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════════════════

    public SolarWinds(Context context) {
        super(context, "shaders/solar_winds_vertex.glsl", "shaders/solar_winds_fragment.glsl");

        Log.d(TAG, "═══════════════════════════════════════════════");
        Log.d(TAG, "🌊 Inicializando Solar Winds");
        Log.d(TAG, "═══════════════════════════════════════════════");

        this.startTime = System.currentTimeMillis();

        // ═════════════════════════════════════════════════════════════════════
        // 1️⃣ CREAR GEOMETRÍA DEL PLANO (QUAD)
        // ═════════════════════════════════════════════════════════════════════

        // Vértices del plano (X, Y, Z) - Centrado en origen
        float[] vertices = {
            -1.0f,  1.0f, 0.0f,  // Top-left
            -1.0f, -1.0f, 0.0f,  // Bottom-left
             1.0f, -1.0f, 0.0f,  // Bottom-right
             1.0f,  1.0f, 0.0f   // Top-right
        };

        // Coordenadas de textura (U, V)
        float[] texCoords = {
            0.0f, 1.0f,  // Top-left
            0.0f, 0.0f,  // Bottom-left
            1.0f, 0.0f,  // Bottom-right
            1.0f, 1.0f   // Top-right
        };

        // Índices para dibujar 2 triángulos (formando un quad)
        short[] indices = {
            0, 1, 2,  // Primer triángulo
            0, 2, 3   // Segundo triángulo
        };

        this.indexCount = indices.length;

        // ═════════════════════════════════════════════════════════════════════
        // 2️⃣ CREAR BUFFERS DE OPENGL
        // ═════════════════════════════════════════════════════════════════════

        // Vertex buffer
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // TexCoord buffer
        ByteBuffer tbb = ByteBuffer.allocateDirect(texCoords.length * 4);
        tbb.order(ByteOrder.nativeOrder());
        texCoordBuffer = tbb.asFloatBuffer();
        texCoordBuffer.put(texCoords);
        texCoordBuffer.position(0);

        // Index buffer
        ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        indexBuffer = ibb.asShortBuffer();
        indexBuffer.put(indices);
        indexBuffer.position(0);

        // ═════════════════════════════════════════════════════════════════════
        // 3️⃣ OBTENER LOCACIONES DE ATRIBUTOS Y UNIFORMS
        // ═════════════════════════════════════════════════════════════════════

        aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexCoordLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uMVPLoc = GLES20.glGetUniformLocation(programId, "u_MVP");
        uTimeLoc = GLES20.glGetUniformLocation(programId, "u_Time");
        uResolutionLoc = GLES20.glGetUniformLocation(programId, "u_Resolution");

        Log.d(TAG, "✅ Locaciones obtenidas:");
        Log.d(TAG, "   a_Position: " + aPositionLoc);
        Log.d(TAG, "   a_TexCoord: " + aTexCoordLoc);
        Log.d(TAG, "   u_MVP: " + uMVPLoc);
        Log.d(TAG, "   u_Time: " + uTimeLoc);
        Log.d(TAG, "   u_Resolution: " + uResolutionLoc);
        Log.d(TAG, "═══════════════════════════════════════════════");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 🎥 CAMERA AWARE
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 🔧 SETTERS PARA POSICIÓN, ESCALA Y ROTACIÓN
    // ══════════════════════════════════════════════════════════════════════════

    public void setPosition(float x, float y, float z) {
        this.positionX = x;
        this.positionY = y;
        this.positionZ = z;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setRotation(float rotX, float rotY, float rotZ) {
        this.rotationX = rotX;
        this.rotationY = rotY;
        this.rotationZ = rotZ;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 🔄 UPDATE (SceneObject)
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void update(float dt) {
        // No hay lógica de update necesaria (animación manejada por shader)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 🎨 DRAW (SceneObject)
    // ══════════════════════════════════════════════════════════════════════════

    // Debug counter
    private int frameCounter = 0;

    @Override
    public void draw() {
        if (camera == null) {
            Log.w(TAG, "⚠️ Camera no asignada, saltando render");
            return;
        }

        // ✅ DEBUG: Log cada 60 frames (~1 vez por segundo)
        if (frameCounter++ % 60 == 0) {
            Log.d(TAG, "🌊 Drawing Solar Winds - Frame " + frameCounter);
        }

        // ═════════════════════════════════════════════════════════════════════
        // 1️⃣ ACTIVAR SHADER PROGRAM
        // ═════════════════════════════════════════════════════════════════════

        GLES20.glUseProgram(programId);

        // ═════════════════════════════════════════════════════════════════════
        // 2️⃣ CONFIGURAR BLENDING (DEBUG: NORMAL, no aditivo)
        // ═════════════════════════════════════════════════════════════════════

        GLES20.glEnable(GLES20.GL_BLEND);
        // ✅ DEBUG: Blending NORMAL para máxima visibilidad
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Desactivar depth write (para no bloquear objetos detrás)
        GLES20.glDepthMask(false);

        // ✅ DEBUG: Desactivar completamente depth test
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        // ═════════════════════════════════════════════════════════════════════
        // 3️⃣ CALCULAR MATRIZ DE MODELO (transformaciones del plano)
        // ═════════════════════════════════════════════════════════════════════

        Matrix.setIdentityM(modelMatrix, 0);

        // Trasladar a posición
        Matrix.translateM(modelMatrix, 0, positionX, positionY, positionZ);

        // Rotaciones
        if (rotationX != 0.0f) {
            Matrix.rotateM(modelMatrix, 0, rotationX, 1.0f, 0.0f, 0.0f);
        }
        if (rotationY != 0.0f) {
            Matrix.rotateM(modelMatrix, 0, rotationY, 0.0f, 1.0f, 0.0f);
        }
        if (rotationZ != 0.0f) {
            Matrix.rotateM(modelMatrix, 0, rotationZ, 0.0f, 0.0f, 1.0f);
        }

        // Escalar
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        // ═════════════════════════════════════════════════════════════════════
        // 4️⃣ CALCULAR MVP (Model-View-Projection)
        // ═════════════════════════════════════════════════════════════════════

        camera.computeMvp(modelMatrix, mvpMatrix);

        // ═════════════════════════════════════════════════════════════════════
        // 5️⃣ PASAR UNIFORMS AL SHADER
        // ═════════════════════════════════════════════════════════════════════

        // u_MVP
        GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, mvpMatrix, 0);

        // u_Time (tiempo cíclico para evitar pérdida de precisión)
        float currentTime = ((System.currentTimeMillis() - startTime) / 1000.0f) % 60.0f;
        GLES20.glUniform1f(uTimeLoc, currentTime);

        // u_Resolution (ancho y alto del viewport)
        int[] viewport = new int[4];
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, viewport, 0);
        GLES20.glUniform2f(uResolutionLoc, viewport[2], viewport[3]);

        // ═════════════════════════════════════════════════════════════════════
        // 6️⃣ CONFIGURAR ATRIBUTOS DE VÉRTICES
        // ═════════════════════════════════════════════════════════════════════

        // a_Position
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        // a_TexCoord
        GLES20.glEnableVertexAttribArray(aTexCoordLoc);
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        // ═════════════════════════════════════════════════════════════════════
        // 7️⃣ DIBUJAR EL PLANO
        // ═════════════════════════════════════════════════════════════════════

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        // ═════════════════════════════════════════════════════════════════════
        // 8️⃣ RESTAURAR ESTADO DE OPENGL
        // ═════════════════════════════════════════════════════════════════════

        // Reactivar depth test
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Reactivar depth write
        GLES20.glDepthMask(true);

        // Desactivar atributos
        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aTexCoordLoc);
    }
}
