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
    private static final String TAG = "depurar";

    // Configuración OPTIMIZADA para wallpaper
    private static final int NUM_STARS = 100;  // Reducido para mejor rendimiento
    private static final float FIELD_SIZE = 15.0f;  // Campo más pequeño
    private static final float FIELD_DEPTH = 8.0f;  // Menos profundidad

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

            // Profundidad variada OPTIMIZADA: más cerca, más visibles
            float depthLayer = rand.nextFloat();
            if (depthLayer < 0.4f) {
                // 40% estrellas cercanas (muy visibles)
                z = -1.5f - rand.nextFloat() * 1.5f;  // z: -1.5 a -3.0
                size = 0.15f + rand.nextFloat() * 0.20f;  // MÁS GRANDES
            } else if (depthLayer < 0.8f) {
                // 40% estrellas medias
                z = -3.0f - rand.nextFloat() * 2.0f;  // z: -3.0 a -5.0
                size = 0.10f + rand.nextFloat() * 0.12f;  // MÁS GRANDES
            } else {
                // 20% estrellas lejanas
                z = -5.0f - rand.nextFloat() * FIELD_DEPTH;  // z: -5.0 a -13.0
                size = 0.06f + rand.nextFloat() * 0.08f;  // MÁS GRANDES
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

            // Brillo basado en profundidad - MÁS BRILLANTES
            if (z > -5.0f) {
                brightness = 0.9f + rand.nextFloat() * 0.1f;  // Estrellas cercanas MUY brillantes
            } else if (z > -10.0f) {
                brightness = 0.7f + rand.nextFloat() * 0.3f;  // Estrellas medias más brillantes
            } else {
                brightness = 0.5f + rand.nextFloat() * 0.3f;  // Estrellas lejanas más visibles
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
        Log.d(TAG, "[StarField] ========================================");
        Log.d(TAG, "[StarField] INICIANDO CREACIÓN DE CAMPO DE ESTRELLAS");
        Log.d(TAG, "[StarField] ========================================");
        this.context = context;

        // Crear geometría del quad si no existe
        if (quadVertexBuffer == null) {
            Log.d(TAG, "[StarField] Creando geometría de quad...");
            createQuadMesh();
            Log.d(TAG, "[StarField] ✓ Quad mesh creado");
        } else {
            Log.d(TAG, "[StarField] Quad mesh ya existía (compartido)");
        }

        // Crear estrellas
        Log.d(TAG, "[StarField] Creando " + NUM_STARS + " estrellas...");
        stars = new Star[NUM_STARS];
        for (int i = 0; i < NUM_STARS; i++) {
            stars[i] = new Star(i);
        }
        Log.d(TAG, "[StarField] ✓ " + NUM_STARS + " estrellas creadas");

        // Crear shader program desde archivos
        Log.d(TAG, "[StarField] Cargando shaders...");
        programId = ShaderUtils.createProgramFromAssets(context,
            "shaders/star_vertex.glsl",
            "shaders/star_fragment.glsl");

        if (programId <= 0) {
            Log.e(TAG, "[StarField] ✗✗✗ ERROR CRÍTICO: Shader NO se creó! programId=" + programId);
            return;
        }

        Log.d(TAG, "[StarField] ✓ Shader creado exitosamente, programId=" + programId);

        // Obtener locations
        aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        uMvpLoc = GLES20.glGetUniformLocation(programId, "u_MVP");
        uColorLoc = GLES20.glGetUniformLocation(programId, "u_Color");
        uTimeLoc = GLES20.glGetUniformLocation(programId, "u_Time");
        uStarIdLoc = GLES20.glGetUniformLocation(programId, "u_StarId");
        uPulseLoc = GLES20.glGetUniformLocation(programId, "u_Pulse");

        Log.d(TAG, "[StarField] Shader locations - Pos:" + aPositionLoc + " MVP:" + uMvpLoc +
                   " Color:" + uColorLoc + " Time:" + uTimeLoc + " StarId:" + uStarIdLoc + " Pulse:" + uPulseLoc);

        if (aPositionLoc == -1 || uMvpLoc == -1) {
            Log.e(TAG, "[StarField] ✗✗✗ ERROR: Locations críticas inválidas!");
        }

        Log.d(TAG, "[StarField] ========================================");
        Log.d(TAG, "[StarField] ✓✓✓ CAMPO DE ESTRELLAS INICIALIZADO");
        Log.d(TAG, "[StarField] ========================================");
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

    // Contador para logs periódicos
    private static int drawCallCount = 0;

    @Override
    public void draw() {
        drawCallCount++;

        // Log inicial cada 300 frames
        if (drawCallCount % 300 == 0) {
            Log.d(TAG, "[StarField] ========================================");
            Log.d(TAG, "[StarField] draw() llamado, frame #" + drawCallCount);
            Log.d(TAG, "[StarField] programId=" + programId + ", time=" + time);
            Log.d(TAG, "[StarField] camera=" + (camera != null ? "válida" : "NULL"));
        }

        if (camera == null) {
            if (drawCallCount % 60 == 0) {  // Log cada segundo
                Log.w(TAG, "[StarField] ✗ draw() - camera es null! No se puede dibujar");
            }
            return;
        }

        if (programId <= 0) {
            if (drawCallCount % 60 == 0) {
                Log.e(TAG, "[StarField] ✗ draw() - programId inválido: " + programId);
            }
            return;
        }

        // Verificar si el shader es válido
        if (!GLES20.glIsProgram(programId)) {
            if (drawCallCount % 60 == 0) {
                Log.e(TAG, "[StarField] ✗ draw() - programId no es un programa GL válido!");
            }
            return;
        }

        GLES20.glUseProgram(programId);

        // Log detallado cada 300 frames
        if (drawCallCount % 300 == 0) {
            Log.d(TAG, "[StarField] ✓ Shader activo, dibujando " + stars.length + " estrellas");
            Log.d(TAG, "[StarField] aPositionLoc=" + aPositionLoc + ", uMvpLoc=" + uMvpLoc);
        }

        // Configurar estado de renderizado
        // IMPORTANTE: Las estrellas se dibujan SIN depth test como el fondo
        // para evitar conflictos con el depth buffer
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);  // SIN depth test
        GLES20.glDepthMask(false);  // No escribir al depth buffer

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);  // Aditivo para brillo

        // Configurar atributos de vértices
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, 0, quadVertexBuffer);

        // Verificar errores GL
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR && drawCallCount % 300 == 0) {
            Log.e(TAG, "[StarField] ✗ Error GL después de configurar atributos: " + error);
        }

        // Dibujar cada estrella
        for (Star star : stars) {
            star.draw(camera, programId, uMvpLoc, uColorLoc, uTimeLoc, uStarIdLoc, uPulseLoc);
        }

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPositionLoc);

        // Restaurar estados para que los objetos 3D puedan usar depth test
        GLES20.glDepthMask(true);  // Restaurar escritura al depth buffer
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);  // Restaurar depth test
        GLES20.glDepthFunc(GLES20.GL_LESS);  // Restaurar función de depth normal
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        if (drawCallCount % 300 == 0) {
            Log.d(TAG, "[StarField] ✓ Frame completado exitosamente");
            Log.d(TAG, "[StarField] ========================================");
        }
    }
}