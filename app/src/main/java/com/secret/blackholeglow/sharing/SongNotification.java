package com.secret.blackholeglow.sharing;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 🎵 NOTIFICACIÓN DE CANCIÓN COMPARTIDA
 *
 * Muestra un mensaje flotante cuando un usuario comparte una canción.
 * Animación suave de fade in/out.
 *
 * Características:
 * - Fade in suave (0.5s)
 * - Visible por 5 segundos
 * - Fade out suave (0.5s)
 * - Cola de notificaciones (si llegan varias)
 * - Posición en parte superior
 */
public class SongNotification {
    private static final String TAG = "SongNotification";

    // Tiempos de animación (en milisegundos)
    private static final long FADE_IN_DURATION = 500;
    private static final long SHOW_DURATION = 5000;
    private static final long FADE_OUT_DURATION = 500;
    private static final long TOTAL_DURATION = FADE_IN_DURATION + SHOW_DURATION + FADE_OUT_DURATION;

    // Posición (parte superior elegante)
    private float x = 0.0f;       // Centro horizontal
    private float y = 0.65f;      // Arriba

    // Dimensiones (compacto y elegante)
    private float width = 1.6f;
    private float height = 0.15f;

    // Estado actual
    private SharedSong currentSong = null;
    private long startTime = 0;
    private float alpha = 0.0f;
    private boolean isVisible = false;

    // Cola de notificaciones pendientes
    private Queue<SharedSong> notificationQueue = new LinkedList<>();

    // ⚡ OPTIMIZACIÓN: Matrices reutilizables (evitar allocations en draw)
    private final float[] modelMatrixCache = new float[16];
    private final float[] finalMatrixCache = new float[16];

    // OpenGL
    private int programId;
    private FloatBuffer vertexBuffer;
    private FloatBuffer textVertexBuffer;
    private boolean isInitialized = false;

    // Shaders
    private static final String VERTEX_SHADER =
            "attribute vec4 a_Position;\n" +
            "uniform mat4 u_MVPMatrix;\n" +
            "void main() {\n" +
            "    gl_Position = u_MVPMatrix * a_Position;\n" +
            "}";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform vec4 u_Color;\n" +
            "void main() {\n" +
            "    gl_FragColor = u_Color;\n" +
            "}";

    // Handles
    private int positionHandle;
    private int mvpMatrixHandle;
    private int colorHandle;

    // Texto a mostrar (simplificado - en producción usaríamos SimpleTextRenderer)
    private String displayUserName = "";
    private String displaySongTitle = "";

    public SongNotification() {
        // Constructor vacío
    }

    /**
     * 🎨 Inicializa recursos de OpenGL
     */
    public void init() {
        if (isInitialized) return;

        // Crear programa de shaders
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        programId = GLES20.glCreateProgram();
        GLES20.glAttachShader(programId, vertexShader);
        GLES20.glAttachShader(programId, fragmentShader);
        GLES20.glLinkProgram(programId);

        // Obtener handles
        positionHandle = GLES20.glGetAttribLocation(programId, "a_Position");
        mvpMatrixHandle = GLES20.glGetUniformLocation(programId, "u_MVPMatrix");
        colorHandle = GLES20.glGetUniformLocation(programId, "u_Color");

        // Crear geometría del rectángulo redondeado (simplificado como rectángulo)
        createRoundedRectGeometry();

        isInitialized = true;
        Log.d(TAG, "🎵 SongNotification inicializado");
    }

    /**
     * 📦 Crea la geometría del fondo
     */
    private void createRoundedRectGeometry() {
        // Rectángulo simple (en producción se puede hacer redondeado)
        float[] vertices = {
                -0.5f, -0.5f,  // Bottom left
                 0.5f, -0.5f,  // Bottom right
                 0.5f,  0.5f,  // Top right
                -0.5f,  0.5f   // Top left
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
    }

    /**
     * 🎵 Muestra una nueva notificación de canción
     */
    public void show(SharedSong song) {
        if (song == null) return;

        // Si ya hay una visible, agregar a la cola
        if (isVisible && currentSong != null) {
            notificationQueue.offer(song);
            Log.d(TAG, "🎵 Canción agregada a cola: " + song.getSongTitle());
            return;
        }

        // Mostrar esta canción
        currentSong = song;
        displayUserName = song.getUserName();
        displaySongTitle = song.getSongTitle();
        startTime = System.currentTimeMillis();
        isVisible = true;
        alpha = 0.1f;  // Empezar con algo de visibilidad

        Log.d(TAG, "🎵 MOSTRANDO NOTIFICACIÓN: " + displayUserName + " - " + displaySongTitle);
    }

    /**
     * 🔄 Actualiza el estado de la animación
     * MODO PERSISTENTE: La notificación se queda visible hasta que llegue otra
     */
    public void update() {
        if (!isVisible || currentSong == null) {
            // Verificar cola
            checkQueue();
            return;
        }

        long elapsed = System.currentTimeMillis() - startTime;

        // Fade in rápido, luego siempre visible
        if (elapsed < FADE_IN_DURATION) {
            // Fade in
            alpha = elapsed / (float) FADE_IN_DURATION;
        } else {
            // SIEMPRE VISIBLE hasta que llegue nueva canción
            alpha = 1.0f;
        }

        // Si hay canciones en cola, mostrar la siguiente
        if (!notificationQueue.isEmpty() && elapsed > 3000) {
            // Después de 3 segundos, si hay otra en cola, cambiar
            currentSong = notificationQueue.poll();
            startTime = System.currentTimeMillis();
            displayUserName = currentSong.getUserName();
            displaySongTitle = currentSong.getSongTitle();
        }
    }

    /**
     * 📋 Verifica si hay notificaciones pendientes
     */
    private void checkQueue() {
        if (!notificationQueue.isEmpty()) {
            SharedSong next = notificationQueue.poll();
            if (next != null) {
                show(next);
            }
        }
    }

    /**
     * 🎬 Dibuja la notificación
     */
    public void draw(float[] mvpMatrix) {
        if (!isInitialized || !isVisible || alpha <= 0.01f) return;

        update();

        GLES20.glUseProgram(programId);

        // Habilitar blending para transparencia
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Calcular matriz de transformación (⚡ OPTIMIZADO: usar caches)
        android.opengl.Matrix.setIdentityM(modelMatrixCache, 0);
        android.opengl.Matrix.translateM(modelMatrixCache, 0, x, y, 0);
        android.opengl.Matrix.scaleM(modelMatrixCache, 0, width, height, 1);

        android.opengl.Matrix.multiplyMM(finalMatrixCache, 0, mvpMatrix, 0, modelMatrixCache, 0);

        // Pasar matriz
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, finalMatrixCache, 0);

        // NO dibujar nada - solo el texto (se dibuja desde SceneRenderer)
        // El fondo y borde están ocultos para un look más limpio
    }

    /**
     * 🎵 Dibuja el indicador de música con animación de pulso
     */
    private void drawMusicIndicator(float[] mvpMatrix) {
        // Pequeño corazón/círculo pulsante a la izquierda
        float indicatorX = x - width / 2 + 0.10f;
        float indicatorY = y;

        // Animación de pulso
        float pulseTime = (System.currentTimeMillis() % 1000) / 1000.0f;
        float pulse = 1.0f + 0.2f * (float)Math.sin(pulseTime * Math.PI * 2);
        float indicatorSize = 0.045f * pulse;

        // ⚡ OPTIMIZADO: usar matrices cacheadas
        android.opengl.Matrix.setIdentityM(modelMatrixCache, 0);
        android.opengl.Matrix.translateM(modelMatrixCache, 0, indicatorX, indicatorY, 0);
        android.opengl.Matrix.scaleM(modelMatrixCache, 0, indicatorSize, indicatorSize, 1);

        android.opengl.Matrix.multiplyMM(finalMatrixCache, 0, mvpMatrix, 0, modelMatrixCache, 0);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, finalMatrixCache, 0);

        // Color rojo vibrante que pulsa
        float colorPulse = 0.8f + 0.2f * (float)Math.sin(pulseTime * Math.PI * 4);
        float[] indicatorColor = {1.0f, 0.2f * colorPulse, 0.3f * colorPulse, alpha};
        GLES20.glUniform4fv(colorHandle, 1, indicatorColor, 0);

        // Dibujar el indicador
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    /**
     * 📊 Retorna información para el texto (para uso con SimpleTextRenderer externo)
     */
    public String getUserNameText() {
        return displayUserName;
    }

    public String getSongTitleText() {
        return displaySongTitle;
    }

    public float getTextAlpha() {
        return alpha;
    }

    public boolean isVisible() {
        return isVisible;  // Solo verificar el flag, alpha se maneja en draw()
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    /**
     * 🎨 Carga un shader
     */
    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    /**
     * 🗑️ Libera recursos
     */
    public void cleanup() {
        if (programId != 0) {
            GLES20.glDeleteProgram(programId);
            programId = 0;
        }
        isInitialized = false;
        notificationQueue.clear();
    }
}
