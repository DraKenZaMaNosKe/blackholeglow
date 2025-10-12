package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Random;

/**
 * Sistema de campo de estrellas con geometría real y shaders animados
 * Sin usar GL_PROGRAM_POINT_SIZE para máxima compatibilidad
 */
public class StarField implements SceneObject, CameraAware {
    private static final String TAG = "StarField";

    // Configuración
    private static final int NUM_STARS = 200;  // Más estrellas para mejor efecto
    private static final float FIELD_SIZE = 20.0f;  // Campo más concentrado
    private static final float FIELD_DEPTH = 15.0f;  // Profundidad variada

    // Arrays para cada estrella
    private Star[] stars;

    // Shader program
    private int programId;
    private int aPositionLoc;
    private int uMvpLoc;
    private int uColorLoc;
    private int uTimeLoc;
    private int uStarIdLoc;
    private int uPulseLoc;

    // Referencias
    private CameraController camera;
    private float time = 0;

    // Mesh compartido para todas las estrellas (un pequeño quad)
    private static FloatBuffer quadVertexBuffer;
    private static ShortBuffer quadIndexBuffer;
    private static final int INDICES_PER_STAR = 6;

    // Context para cargar shaders
    private final Context context;

    // Clase interna para cada estrella
    private class Star {
        float x, y, z;
        float size;
        float[] color = new float[4];
        float pulseSpeed;
        float pulsePhase;
        float brightness;
        boolean isPulsing = false;
        float pulseTimer = 0;
        float id;  // ID único para variación en shader

        Star(int index) {
            Random rand = new Random();

            // Posición aleatoria con mejor distribución
            x = (rand.nextFloat() - 0.5f) * FIELD_SIZE;
            y = (rand.nextFloat() - 0.5f) * FIELD_SIZE;

            // Profundidad variada: algunas estrellas cerca, otras lejos
            float depthLayer = rand.nextFloat();
            if (depthLayer < 0.3f) {
                // 30% estrellas cercanas (más grandes y brillantes)
                z = -2.0f - rand.nextFloat() * 3.0f;
                size = 0.15f + rand.nextFloat() * 0.2f;
            } else if (depthLayer < 0.7f) {
                // 40% estrellas medias
                z = -5.0f - rand.nextFloat() * 5.0f;
                size = 0.08f + rand.nextFloat() * 0.12f;
            } else {
                // 30% estrellas lejanas (más pequeñas y tenues)
                z = -10.0f - rand.nextFloat() * FIELD_DEPTH;
                size = 0.04f + rand.nextFloat() * 0.06f;
            }

            // Color de estrella
            float type = rand.nextFloat();
            if (type < 0.3f) {
                // Azulada
                color[0] = 0.6f + rand.nextFloat() * 0.3f;
                color[1] = 0.7f + rand.nextFloat() * 0.3f;
                color[2] = 1.0f;
            } else if (type < 0.7f) {
                // Blanca
                color[0] = 1.0f;
                color[1] = 1.0f;
                color[2] = 0.9f + rand.nextFloat() * 0.1f;
            } else {
                // Amarillenta/Rojiza
                color[0] = 1.0f;
                color[1] = 0.8f + rand.nextFloat() * 0.2f;
                color[2] = 0.5f + rand.nextFloat() * 0.3f;
            }

            // Brillo basado en profundidad
            if (z > -5.0f) {
                brightness = 0.8f + rand.nextFloat() * 0.2f;  // Estrellas cercanas más brillantes
            } else if (z > -10.0f) {
                brightness = 0.5f + rand.nextFloat() * 0.3f;  // Estrellas medias
            } else {
                brightness = 0.3f + rand.nextFloat() * 0.2f;  // Estrellas lejanas más tenues
            }
            color[3] = brightness;

            pulseSpeed = 1.0f + rand.nextFloat() * 3.0f;
            pulsePhase = rand.nextFloat() * (float)Math.PI * 2.0f;

            id = (float)index / (float)NUM_STARS;  // ID normalizado
        }

        void update(float dt) {
            // Actualizar pulso ocasional
            if (Math.random() < 0.0002f && !isPulsing) {
                isPulsing = true;
                pulseTimer = 1.0f;
            }

            if (isPulsing) {
                pulseTimer -= dt * 2.0f;
                if (pulseTimer <= 0) {
                    isPulsing = false;
                    pulseTimer = 0;
                }
            }
        }

        void draw(CameraController camera, int programId, int uMvpLoc, int uColorLoc,
                  int uTimeLoc, int uStarIdLoc, int uPulseLoc) {

            // Matriz modelo para esta estrella
            float[] modelMatrix = new float[16];
            float[] mvpMatrix = new float[16];

            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, x, y, z);
            Matrix.scaleM(modelMatrix, 0, size, size, size);

            // Rotar para que siempre mire a la cámara (billboard)
            float angle = time * pulseSpeed * 30.0f + pulsePhase * 57.3f;
            Matrix.rotateM(modelMatrix, 0, angle, 0, 0, 1);

            camera.computeMvp(modelMatrix, mvpMatrix);

            // Configurar uniforms para esta estrella
            GLES20.glUniformMatrix4fv(uMvpLoc, 1, false, mvpMatrix, 0);
            GLES20.glUniform4fv(uColorLoc, 1, color, 0);
            GLES20.glUniform1f(uTimeLoc, time + pulsePhase);
            GLES20.glUniform1f(uStarIdLoc, id);
            GLES20.glUniform1f(uPulseLoc, isPulsing ? pulseTimer : 0);

            // Dibujar el quad
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, INDICES_PER_STAR,
                                 GLES20.GL_UNSIGNED_SHORT, quadIndexBuffer);
        }
    }

    public StarField(Context context) {
        Log.d(TAG, "Creando campo de estrellas mejorado");
        this.context = context;

        // Crear geometría del quad si no existe
        if (quadVertexBuffer == null) {
            createQuadMesh();
        }

        // Crear estrellas
        stars = new Star[NUM_STARS];
        for (int i = 0; i < NUM_STARS; i++) {
            stars[i] = new Star(i);
        }

        // Crear shader program desde archivos
        programId = ShaderUtils.createProgramFromAssets(context,
            "shaders/star_vertex.glsl",
            "shaders/star_fragment.glsl");

        // Obtener locations
        aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        uMvpLoc = GLES20.glGetUniformLocation(programId, "u_MVP");
        uColorLoc = GLES20.glGetUniformLocation(programId, "u_Color");
        uTimeLoc = GLES20.glGetUniformLocation(programId, "u_Time");
        uStarIdLoc = GLES20.glGetUniformLocation(programId, "u_StarId");
        uPulseLoc = GLES20.glGetUniformLocation(programId, "u_Pulse");

        Log.d(TAG, "StarField inicializado con " + NUM_STARS + " estrellas animadas");
    }

    private static void createQuadMesh() {
        // Crear un quad simple centrado en el origen
        float[] vertices = {
            -0.5f, -0.5f, 0.0f,  // Bottom left
             0.5f, -0.5f, 0.0f,  // Bottom right
             0.5f,  0.5f, 0.0f,  // Top right
            -0.5f,  0.5f, 0.0f   // Top left
        };

        short[] indices = {
            0, 1, 2,  // Primer triángulo
            0, 2, 3   // Segundo triángulo
        };

        // Crear buffers
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        quadVertexBuffer = vbb.asFloatBuffer();
        quadVertexBuffer.put(vertices);
        quadVertexBuffer.position(0);

        ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        quadIndexBuffer = ibb.asShortBuffer();
        quadIndexBuffer.put(indices);
        quadIndexBuffer.position(0);
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    @Override
    public void update(float deltaTime) {
        time += deltaTime;

        // Actualizar cada estrella
        for (Star star : stars) {
            star.update(deltaTime);
        }
    }

    @Override
    public void draw() {
        if (camera == null) return;

        GLES20.glUseProgram(programId);

        // Configurar estado de renderizado
        // IMPORTANTE: Las estrellas deben escribir en el depth buffer
        // pero renderizarse después del fondo
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);  // Permitir dibujar a la misma profundidad
        GLES20.glDepthMask(false);  // No escribir al depth buffer para no bloquear otros objetos

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);  // Aditivo para brillo

        // Configurar atributos de vértices
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, 0, quadVertexBuffer);

        // Dibujar cada estrella
        for (Star star : stars) {
            star.draw(camera, programId, uMvpLoc, uColorLoc, uTimeLoc, uStarIdLoc, uPulseLoc);
        }

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPositionLoc);

        // Restaurar estados
        GLES20.glDepthMask(true);  // Restaurar escritura al depth buffer
        GLES20.glDepthFunc(GLES20.GL_LESS);  // Restaurar función de depth
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }
}