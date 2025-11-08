package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

/**
 * ╔════════════════════════════════════════════════════════════════════════╗
 * ║  🌟 LLUVIA DE ESTRELLAS MUSICAL                                       ║
 * ║  Estrellas que pulsan según frecuencias musicales                     ║
 * ╚════════════════════════════════════════════════════════════════════════╝
 *
 * Distribución por frecuencia:
 * - GRAVES (bajo, bombo):     Estrellas grandes, zona INFERIOR (cerca de Tierra)
 * - MEDIOS (guitarra, piano): Estrellas medianas, zona CENTRO
 * - AGUDOS (voces, vientos):  Estrellas pequeñas, zona SUPERIOR (cerca del Sol)
 */
public class MusicStars implements SceneObject {
    private static final String TAG = "depurar";

    // ════════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE ESTRELLAS
    // ════════════════════════════════════════════════════════════════════════
    private static final int NUM_ESTRELLAS_GRAVES = 8;   // Estrellas grandes (bajo, bombo)
    private static final int NUM_ESTRELLAS_MEDIOS = 12;  // Estrellas medianas (guitarra, piano, voces)
    private static final int NUM_ESTRELLAS_AGUDOS = 15;  // Estrellas pequeñas (vientos, platillos)
    private static final int TOTAL_ESTRELLAS = NUM_ESTRELLAS_GRAVES + NUM_ESTRELLAS_MEDIOS + NUM_ESTRELLAS_AGUDOS;

    private static final float SMOOTHING_FACTOR = 0.7f;  // Suavizado de pulsaciones

    // Tamaños base de las estrellas (en coordenadas NDC)
    private static final float SIZE_GRAVES_BASE = 0.015f;   // Grandes
    private static final float SIZE_MEDIOS_BASE = 0.010f;   // Medianas
    private static final float SIZE_AGUDOS_BASE = 0.006f;   // Pequeñas

    // Zonas verticales (NDC: -1.0 a +1.0)
    private static final float ZONA_GRAVES_MIN = -0.9f;  // Zona inferior (cerca de Tierra)
    private static final float ZONA_GRAVES_MAX = -0.3f;

    private static final float ZONA_MEDIOS_MIN = -0.4f;  // Zona central
    private static final float ZONA_MEDIOS_MAX = 0.4f;

    private static final float ZONA_AGUDOS_MIN = 0.3f;   // Zona superior (cerca del Sol)
    private static final float ZONA_AGUDOS_MAX = 0.9f;

    // ════════════════════════════════════════════════════════════════════════
    // DATOS DE CADA ESTRELLA
    // ════════════════════════════════════════════════════════════════════════
    private static class Star {
        float x, y;           // Posición
        float baseSize;       // Tamaño base
        float currentSize;    // Tamaño actual (con pulsación)
        float targetSize;     // Tamaño objetivo
        float[] color;        // Color RGBA
        int freqType;         // 0=graves, 1=medios, 2=agudos
    }

    private Star[] stars;

    // Niveles de música
    private float bassLevel = 0f;
    private float midLevel = 0f;
    private float trebleLevel = 0f;

    // Shader
    private int programId;
    private int aPositionLoc;
    private int aColorLoc;

    private int frameCount = 0;

    public MusicStars(Context context) {
        Log.d(TAG, "╔══════════════════════════════════════════════╗");
        Log.d(TAG, "║      CREANDO LLUVIA DE ESTRELLAS MUSICAL    ║");
        Log.d(TAG, "╚══════════════════════════════════════════════╝");

        initShader(context);
        initStars();

        Log.d(TAG, "[MusicStars] ✓ Constructor completado");
        Log.d(TAG, "[MusicStars] Total estrellas: " + TOTAL_ESTRELLAS);
        Log.d(TAG, "[MusicStars]   Graves: " + NUM_ESTRELLAS_GRAVES + " (grandes, zona inferior)");
        Log.d(TAG, "[MusicStars]   Medios: " + NUM_ESTRELLAS_MEDIOS + " (medianas, zona centro)");
        Log.d(TAG, "[MusicStars]   Agudos: " + NUM_ESTRELLAS_AGUDOS + " (pequeñas, zona superior)");
    }

    private void initShader(Context context) {
        // Vertex shader para puntos
        String vertexShader =
            "attribute vec2 a_Position;\n" +
            "attribute vec4 a_Color;\n" +
            "varying vec4 v_Color;\n" +
            "void main() {\n" +
            "    v_Color = a_Color;\n" +
            "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
            "    gl_PointSize = 8.0;\n" +  // Tamaño de punto para estrellas
            "}\n";

        // Fragment shader con efecto de brillo
        String fragmentShader =
            "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec4 v_Color;\n" +
            "\n" +
            "void main() {\n" +
            "    // Calcular distancia desde el centro del punto\n" +
            "    vec2 coord = gl_PointCoord - vec2(0.5);\n" +
            "    float dist = length(coord);\n" +
            "    \n" +
            "    // Crear efecto de estrella (más brillante en el centro)\n" +
            "    float alpha = v_Color.a * (1.0 - smoothstep(0.0, 0.5, dist));\n" +
            "    \n" +
            "    // Aumentar brillo en el centro\n" +
            "    vec3 color = v_Color.rgb * (1.0 + (1.0 - dist * 2.0) * 0.5);\n" +
            "    \n" +
            "    gl_FragColor = vec4(color, alpha);\n" +
            "}\n";

        programId = ShaderUtils.createProgram(vertexShader, fragmentShader);

        if (programId == 0) {
            Log.e(TAG, "[MusicStars] ✗✗✗ ERROR CRÍTICO: Shader NO se pudo crear!");
            return;
        }

        aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aColorLoc = GLES20.glGetAttribLocation(programId, "a_Color");

        Log.d(TAG, "[MusicStars] ✓ Shader inicializado (programId: " + programId + ")");
    }

    private void initStars() {
        stars = new Star[TOTAL_ESTRELLAS];
        Random rand = new Random();
        int index = 0;

        // ════════════════════════════════════════════════════════════════════════
        // ESTRELLAS GRAVES (zona inferior, grandes, colores cálidos)
        // ════════════════════════════════════════════════════════════════════════
        for (int i = 0; i < NUM_ESTRELLAS_GRAVES; i++) {
            stars[index] = new Star();
            stars[index].x = -0.9f + rand.nextFloat() * 1.8f;  // Ancho completo
            stars[index].y = ZONA_GRAVES_MIN + rand.nextFloat() * (ZONA_GRAVES_MAX - ZONA_GRAVES_MIN);
            stars[index].baseSize = SIZE_GRAVES_BASE;
            stars[index].currentSize = stars[index].baseSize;
            stars[index].targetSize = stars[index].baseSize;
            stars[index].freqType = 0;  // Graves

            // Color: Azul eléctrico para graves (bajo, bombo)
            stars[index].color = new float[]{0.2f, 0.5f, 1.0f, 0.8f};  // Azul brillante

            index++;
        }

        // ════════════════════════════════════════════════════════════════════════
        // ESTRELLAS MEDIOS (zona central, medianas, colores variados)
        // ════════════════════════════════════════════════════════════════════════
        for (int i = 0; i < NUM_ESTRELLAS_MEDIOS; i++) {
            stars[index] = new Star();
            stars[index].x = -0.9f + rand.nextFloat() * 1.8f;
            stars[index].y = ZONA_MEDIOS_MIN + rand.nextFloat() * (ZONA_MEDIOS_MAX - ZONA_MEDIOS_MIN);
            stars[index].baseSize = SIZE_MEDIOS_BASE;
            stars[index].currentSize = stars[index].baseSize;
            stars[index].targetSize = stars[index].baseSize;
            stars[index].freqType = 1;  // Medios

            // Color: Dorado/Amarillo para medios (guitarra, piano, voces)
            stars[index].color = new float[]{1.0f, 0.9f, 0.3f, 0.85f};  // Dorado

            index++;
        }

        // ════════════════════════════════════════════════════════════════════════
        // ESTRELLAS AGUDOS (zona superior, pequeñas, colores fríos)
        // ════════════════════════════════════════════════════════════════════════
        for (int i = 0; i < NUM_ESTRELLAS_AGUDOS; i++) {
            stars[index] = new Star();
            stars[index].x = -0.9f + rand.nextFloat() * 1.8f;
            stars[index].y = ZONA_AGUDOS_MIN + rand.nextFloat() * (ZONA_AGUDOS_MAX - ZONA_AGUDOS_MIN);
            stars[index].baseSize = SIZE_AGUDOS_BASE;
            stars[index].currentSize = stars[index].baseSize;
            stars[index].targetSize = stars[index].baseSize;
            stars[index].freqType = 2;  // Agudos

            // Color: Blanco brillante para agudos (vientos, voces agudas, platillos)
            stars[index].color = new float[]{1.0f, 1.0f, 1.0f, 0.9f};  // Blanco puro

            index++;
        }

        Log.d(TAG, "[MusicStars] ✓ " + TOTAL_ESTRELLAS + " estrellas inicializadas");
    }

    /**
     * Actualiza los niveles de música
     */
    public void updateMusicLevels(float bass, float mid, float treble) {
        this.bassLevel = bass;
        this.midLevel = mid;
        this.trebleLevel = treble;

        // Log cada 300 frames
        if (frameCount % 300 == 0 && (bass > 0.05f || mid > 0.05f || treble > 0.05f)) {
            Log.d(TAG, String.format("[MusicStars] 🎵 Bass:%.2f Mid:%.2f Treble:%.2f",
                    bass, mid, treble));
        }
    }

    @Override
    public void update(float deltaTime) {
        frameCount++;

        // Actualizar cada estrella según su tipo de frecuencia
        for (Star star : stars) {
            float intensidad = 0f;

            switch (star.freqType) {
                case 0:  // Graves (bajo, bombo)
                    intensidad = bassLevel;
                    break;
                case 1:  // Medios (guitarra, piano, voces)
                    intensidad = midLevel;
                    break;
                case 2:  // Agudos (vientos, platillos, voces agudas)
                    intensidad = trebleLevel;
                    break;
            }

            // Calcular tamaño objetivo (base + pulsación por intensidad)
            // Multiplicador: 1.0 (sin música) a 3.0 (música máxima)
            float pulseFactor = 1.0f + intensidad * 2.0f;
            star.targetSize = star.baseSize * pulseFactor;

            // Suavizar transición de tamaño
            star.currentSize = star.currentSize * SMOOTHING_FACTOR +
                              star.targetSize * (1f - SMOOTHING_FACTOR);
        }
    }

    @Override
    public void draw() {
        if (!GLES20.glIsProgram(programId)) {
            return;
        }

        GLES20.glUseProgram(programId);

        // Configurar estados OpenGL para puntos brillantes
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);  // Blending aditivo para brillo

        // Dibujar cada estrella como un punto
        for (Star star : stars) {
            drawStar(star);
        }

        // Restaurar estados
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * Dibuja una estrella individual
     */
    private void drawStar(Star star) {
        // Crear un punto en la posición de la estrella
        float[] position = {star.x, star.y};

        // Calcular alpha basado en el tamaño (más grande = más brillante)
        float sizeRatio = star.currentSize / star.baseSize;
        float alpha = star.color[3] * Math.min(1.0f, sizeRatio * 0.6f);

        float[] color = {
            star.color[0],
            star.color[1],
            star.color[2],
            alpha
        };

        // Crear buffers
        ByteBuffer vbb = ByteBuffer.allocateDirect(position.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        FloatBuffer vb = vbb.asFloatBuffer();
        vb.put(position);
        vb.position(0);

        ByteBuffer cbb = ByteBuffer.allocateDirect(color.length * 4);
        cbb.order(ByteOrder.nativeOrder());
        FloatBuffer cb = cbb.asFloatBuffer();
        cb.put(color);
        cb.position(0);

        // Configurar atributos
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vb);

        GLES20.glEnableVertexAttribArray(aColorLoc);
        GLES20.glVertexAttribPointer(aColorLoc, 4, GLES20.GL_FLOAT, false, 0, cb);

        // Dibujar punto
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aColorLoc);
    }
}
