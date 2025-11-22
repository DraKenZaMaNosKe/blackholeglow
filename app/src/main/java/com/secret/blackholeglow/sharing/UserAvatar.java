package com.secret.blackholeglow.sharing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 👤 AVATAR DE USUARIO
 *
 * Muestra la foto de perfil de Google del usuario que compartió una canción.
 * Renderizado con OpenGL ES 2.0.
 *
 * Características:
 * - Carga asíncrona de imagen desde URL
 * - Placeholder con inicial mientras carga
 * - Forma circular
 * - Borde brillante
 */
public class UserAvatar {
    private static final String TAG = "UserAvatar";
    private static final int AVATAR_SIZE = 128;  // Tamaño en píxeles de la textura

    // Posición y tamaño
    private float x = 0.0f;
    private float y = 0.0f;
    private float size = 0.08f;

    // OpenGL
    private int programId;
    private int textureId = -1;
    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;
    private boolean isInitialized = false;

    // Estado
    private String currentUserName = "";
    private String currentPhotoUrl = "";
    private boolean hasLoadedImage = false;
    private Bitmap pendingBitmap = null;

    // Colores para placeholder (basados en el nombre)
    private static final int[] AVATAR_COLORS = {
            0xFFE91E63,  // Rosa
            0xFF9C27B0,  // Púrpura
            0xFF673AB7,  // Violeta
            0xFF3F51B5,  // Azul índigo
            0xFF2196F3,  // Azul
            0xFF03A9F4,  // Azul claro
            0xFF00BCD4,  // Cyan
            0xFF009688,  // Verde azulado
            0xFF4CAF50,  // Verde
            0xFFFF5722,  // Naranja oscuro
    };

    // Executor para carga asíncrona
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // Shaders
    private static final String VERTEX_SHADER =
            "attribute vec4 a_Position;\n" +
            "attribute vec2 a_TexCoord;\n" +
            "varying vec2 v_TexCoord;\n" +
            "uniform mat4 u_MVPMatrix;\n" +
            "void main() {\n" +
            "    gl_Position = u_MVPMatrix * a_Position;\n" +
            "    v_TexCoord = a_TexCoord;\n" +
            "}";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "varying vec2 v_TexCoord;\n" +
            "uniform sampler2D u_Texture;\n" +
            "void main() {\n" +
            "    vec2 center = vec2(0.5, 0.5);\n" +
            "    float dist = distance(v_TexCoord, center);\n" +
            "    if (dist > 0.5) {\n" +
            "        discard;\n" +  // Hacer circular
            "    }\n" +
            "    gl_FragColor = texture2D(u_Texture, v_TexCoord);\n" +
            "}";

    // Handles
    private int positionHandle;
    private int texCoordHandle;
    private int mvpMatrixHandle;
    private int textureHandle;

    public UserAvatar() {
        // Constructor vacío
    }

    /**
     * 🎨 Inicializa OpenGL
     */
    public void init() {
        if (isInitialized) return;

        // Crear programa
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        programId = GLES20.glCreateProgram();
        GLES20.glAttachShader(programId, vertexShader);
        GLES20.glAttachShader(programId, fragmentShader);
        GLES20.glLinkProgram(programId);

        // Obtener handles
        positionHandle = GLES20.glGetAttribLocation(programId, "a_Position");
        texCoordHandle = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(programId, "u_MVPMatrix");
        textureHandle = GLES20.glGetUniformLocation(programId, "u_Texture");

        // Crear geometría (cuadrado)
        createGeometry();

        isInitialized = true;
        Log.d(TAG, "👤 UserAvatar inicializado");
    }

    /**
     * 📦 Crea la geometría del cuadrado
     */
    private void createGeometry() {
        float[] vertices = {
                -0.5f, -0.5f,
                 0.5f, -0.5f,
                 0.5f,  0.5f,
                -0.5f,  0.5f
        };

        float[] texCoords = {
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f,
                0.0f, 0.0f
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        ByteBuffer tb = ByteBuffer.allocateDirect(texCoords.length * 4);
        tb.order(ByteOrder.nativeOrder());
        texCoordBuffer = tb.asFloatBuffer();
        texCoordBuffer.put(texCoords);
        texCoordBuffer.position(0);
    }

    /**
     * 👤 Establece el usuario actual
     */
    public void setUser(String userName, String photoUrl) {
        if (userName == null) userName = "";
        if (photoUrl == null) photoUrl = "";

        // Si es el mismo usuario, no hacer nada
        if (userName.equals(currentUserName) && photoUrl.equals(currentPhotoUrl)) {
            return;
        }

        currentUserName = userName;
        currentPhotoUrl = photoUrl;
        hasLoadedImage = false;

        // Crear placeholder inmediatamente
        createPlaceholderTexture(userName);

        // Cargar imagen real en background si hay URL
        if (!photoUrl.isEmpty()) {
            loadImageAsync(photoUrl);
        }
    }

    /**
     * 🎨 Crea una textura placeholder con la inicial del usuario
     */
    private void createPlaceholderTexture(String userName) {
        Bitmap bitmap = Bitmap.createBitmap(AVATAR_SIZE, AVATAR_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Color basado en el nombre
        int colorIndex = Math.abs(userName.hashCode()) % AVATAR_COLORS.length;
        int bgColor = AVATAR_COLORS[colorIndex];

        // Dibujar círculo de fondo
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(bgColor);
        canvas.drawCircle(AVATAR_SIZE / 2f, AVATAR_SIZE / 2f, AVATAR_SIZE / 2f, bgPaint);

        // Dibujar inicial
        String initial = userName.isEmpty() ? "?" : userName.substring(0, 1).toUpperCase();
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(AVATAR_SIZE * 0.5f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Centrar verticalmente
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = AVATAR_SIZE / 2f - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(initial, AVATAR_SIZE / 2f, textY, textPaint);

        pendingBitmap = bitmap;
    }

    /**
     * 🌐 Carga la imagen desde URL de forma asíncrona
     */
    private void loadImageAsync(final String imageUrl) {
        executor.execute(() -> {
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();

                InputStream input = connection.getInputStream();
                Bitmap originalBitmap = BitmapFactory.decodeStream(input);
                input.close();

                if (originalBitmap != null) {
                    // Escalar y hacer circular
                    Bitmap circularBitmap = createCircularBitmap(originalBitmap);
                    originalBitmap.recycle();

                    // Actualizar en el hilo principal
                    pendingBitmap = circularBitmap;
                    hasLoadedImage = true;
                    Log.d(TAG, "✅ Imagen de perfil cargada: " + imageUrl);
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Error cargando imagen: " + e.getMessage());
                // Mantener el placeholder
            }
        });
    }

    /**
     * ⭕ Convierte un bitmap a circular
     */
    private Bitmap createCircularBitmap(Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());
        Bitmap output = Bitmap.createBitmap(AVATAR_SIZE, AVATAR_SIZE, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);

        // Dibujar círculo
        canvas.drawCircle(AVATAR_SIZE / 2f, AVATAR_SIZE / 2f, AVATAR_SIZE / 2f, paint);

        // Aplicar imagen con modo SRC_IN para hacer circular
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        Rect srcRect = new Rect(
                (source.getWidth() - size) / 2,
                (source.getHeight() - size) / 2,
                (source.getWidth() + size) / 2,
                (source.getHeight() + size) / 2
        );
        RectF dstRect = new RectF(0, 0, AVATAR_SIZE, AVATAR_SIZE);

        canvas.drawBitmap(source, srcRect, dstRect, paint);

        return output;
    }

    /**
     * 🔄 Actualiza la textura si hay un bitmap pendiente
     */
    private void updateTextureIfNeeded() {
        if (pendingBitmap == null) return;

        // Eliminar textura anterior
        if (textureId != -1) {
            int[] textures = {textureId};
            GLES20.glDeleteTextures(1, textures, 0);
        }

        // Crear nueva textura
        int[] textureIds = new int[1];
        GLES20.glGenTextures(1, textureIds, 0);
        textureId = textureIds[0];

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, pendingBitmap, 0);

        pendingBitmap.recycle();
        pendingBitmap = null;
    }

    /**
     * 🎬 Dibuja el avatar
     */
    public void draw(float[] mvpMatrix) {
        if (!isInitialized || textureId == -1 && pendingBitmap == null) return;

        // Actualizar textura si hay bitmap pendiente
        updateTextureIfNeeded();

        if (textureId == -1) return;

        GLES20.glUseProgram(programId);

        // Habilitar blending
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Matriz de transformación
        float[] modelMatrix = new float[16];
        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, x, y, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, size, size, 1);

        float[] finalMatrix = new float[16];
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, finalMatrix, 0);

        // Textura
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(textureHandle, 0);

        // Vértices
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(texCoordHandle);
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        // Dibujar
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(texCoordHandle);
    }

    /**
     * 📍 Establece la posición
     */
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    /**
     * 📐 Establece el tamaño
     */
    public void setSize(float size) {
        this.size = size;
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
        if (textureId != -1) {
            int[] textures = {textureId};
            GLES20.glDeleteTextures(1, textures, 0);
            textureId = -1;
        }
        if (programId != 0) {
            GLES20.glDeleteProgram(programId);
            programId = 0;
        }
        executor.shutdown();
        isInitialized = false;
    }
}
