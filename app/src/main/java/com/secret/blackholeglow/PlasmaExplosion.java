package com.secret.blackholeglow;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ⚡ PLASMA EXPLOSION - Efecto Visual de Impacto
 *
 * Versión simplificada y funcional
 */
public class PlasmaExplosion implements SceneObject, CameraAware {
    private static final String TAG = "PlasmaExplosion";

    // Configuración - TAMAÑO REDUCIDO
    private static final float EXPLOSION_DURATION = 0.6f;
    private static final float MAX_RADIUS = 0.5f;  // Más pequeño que antes
    private static final int NUM_PARTICLES = 16;
    private static final int CIRCLE_SEGMENTS = 24;

    // Estado
    private boolean active = false;
    private float time = 0f;
    private float x, y, z;

    // Partículas
    private final float[] particleAngles = new float[NUM_PARTICLES];
    private final float[] particleSpeeds = new float[NUM_PARTICLES];

    private CameraController camera;

    // OpenGL
    private int shaderProgram;
    private int positionHandle;
    private int colorHandle;
    private int mvpMatrixHandle;

    private FloatBuffer circleBuffer;
    private FloatBuffer particleBuffer;

    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private final float[] colorCache = new float[4];

    // Shader simple
    private static final String VERTEX_SHADER =
        "uniform mat4 uMVPMatrix;" +
        "attribute vec4 vPosition;" +
        "void main() {" +
        "    gl_Position = uMVPMatrix * vPosition;" +
        "    gl_PointSize = 10.0;" +
        "}";

    private static final String FRAGMENT_SHADER =
        "precision mediump float;" +
        "uniform vec4 vColor;" +
        "void main() {" +
        "    gl_FragColor = vColor;" +
        "}";

    public PlasmaExplosion() {
        initBuffers();
        initShaders();
        Log.d(TAG, "⚡ PlasmaExplosion inicializado");
    }

    private void initBuffers() {
        // Círculo
        float[] circle = new float[(CIRCLE_SEGMENTS + 1) * 3];
        for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
            float angle = (float) (i * 2 * Math.PI / CIRCLE_SEGMENTS);
            circle[i * 3] = (float) Math.cos(angle);
            circle[i * 3 + 1] = (float) Math.sin(angle);
            circle[i * 3 + 2] = 0;
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(circle.length * 4);
        bb.order(ByteOrder.nativeOrder());
        circleBuffer = bb.asFloatBuffer();
        circleBuffer.put(circle);
        circleBuffer.position(0);

        // Partículas
        ByteBuffer pb = ByteBuffer.allocateDirect(NUM_PARTICLES * 3 * 4);
        pb.order(ByteOrder.nativeOrder());
        particleBuffer = pb.asFloatBuffer();
    }

    private void initShaders() {
        int vs = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vs, VERTEX_SHADER);
        GLES20.glCompileShader(vs);

        int fs = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fs, FRAGMENT_SHADER);
        GLES20.glCompileShader(fs);

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vs);
        GLES20.glAttachShader(shaderProgram, fs);
        GLES20.glLinkProgram(shaderProgram);

        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        colorHandle = GLES20.glGetUniformLocation(shaderProgram, "vColor");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");
    }

    public void explode(float targetX, float targetY, float targetZ) {
        this.x = targetX;
        this.y = targetY;
        this.z = targetZ;
        this.time = 0f;
        this.active = true;

        for (int i = 0; i < NUM_PARTICLES; i++) {
            particleAngles[i] = (float) (Math.random() * Math.PI * 2);
            particleSpeeds[i] = 0.7f + (float) Math.random() * 0.6f;
        }

        Log.d(TAG, "⚡ BOOM!");
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    @Override
    public void update(float deltaTime) {
        if (!active) return;
        time += deltaTime;
        if (time >= EXPLOSION_DURATION) {
            active = false;
        }
    }

    @Override
    public void draw() {
        if (!active || camera == null) return;

        GLES20.glUseProgram(shaderProgram);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        float progress = time / EXPLOSION_DURATION;
        float alpha = 1f - progress;

        // 1. NÚCLEO - círculo brillante que se expande
        if (progress < 0.5f) {
            float coreSize = 0.1f + progress * 0.3f;
            drawCircle(coreSize, 1f, 1f, 1f, (1f - progress * 2f) * 0.9f, true);
        }

        // 2. ANILLO CYAN expandiéndose
        float ringSize = 0.05f + progress * MAX_RADIUS;
        drawCircle(ringSize, 0.2f, 0.9f, 1f, alpha * 0.8f, false);

        // 3. SEGUNDO ANILLO (magenta)
        if (progress > 0.1f) {
            float ring2Size = progress * MAX_RADIUS * 0.7f;
            drawCircle(ring2Size, 1f, 0.3f, 0.8f, alpha * 0.6f, false);
        }

        // 4. PARTÍCULAS / CHISPAS
        drawParticles(progress, alpha);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void drawCircle(float radius, float r, float g, float b, float a, boolean filled) {
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        Matrix.scaleM(modelMatrix, 0, radius, radius, radius);

        float[] vpMatrix = camera.getViewProjectionMatrix();
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        colorCache[0] = r;
        colorCache[1] = g;
        colorCache[2] = b;
        colorCache[3] = a;
        GLES20.glUniform4fv(colorHandle, 1, colorCache, 0);

        circleBuffer.position(0);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, circleBuffer);

        if (filled) {
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, CIRCLE_SEGMENTS + 1);
        } else {
            GLES20.glLineWidth(3f);
            GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, CIRCLE_SEGMENTS + 1);
            GLES20.glLineWidth(1f);
        }

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private void drawParticles(float progress, float alpha) {
        // Calcular posiciones
        float[] verts = new float[NUM_PARTICLES * 3];
        for (int i = 0; i < NUM_PARTICLES; i++) {
            float dist = progress * MAX_RADIUS * particleSpeeds[i];
            verts[i * 3] = (float) Math.cos(particleAngles[i]) * dist;
            verts[i * 3 + 1] = (float) Math.sin(particleAngles[i]) * dist;
            verts[i * 3 + 2] = 0;
        }

        particleBuffer.clear();
        particleBuffer.put(verts);
        particleBuffer.position(0);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);

        float[] vpMatrix = camera.getViewProjectionMatrix();
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        // Color amarillo-blanco
        colorCache[0] = 1f;
        colorCache[1] = 0.9f;
        colorCache[2] = 0.4f;
        colorCache[3] = alpha * 0.9f;
        GLES20.glUniform4fv(colorHandle, 1, colorCache, 0);

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, particleBuffer);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, NUM_PARTICLES);
        GLES20.glDisableVertexAttribArray(positionHandle);
    }
}
