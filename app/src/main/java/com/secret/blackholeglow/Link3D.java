package com.secret.blackholeglow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.util.ObjLoader;
import com.secret.blackholeglow.image.ImageDownloadManager;
import com.secret.blackholeglow.model.ModelDownloadManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   🗡️ Link3D - Modelo 3D de Link para Zelda BOTW Scene                    ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  CARACTERÍSTICAS:                                                         ║
 * ║  • Modelo 3D estático de Link (Meshy AI)                                 ║
 * ║  • Carga desde assets locales                                            ║
 * ║  • Proyección propia (independiente de CameraController)                 ║
 * ║  • Rotación suave automática                                             ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class Link3D implements SceneObject {
    private static final String TAG = "Link3D";

    private final Context context;

    // Buffers del modelo
    private FloatBuffer vertexBuffer;
    private FloatBuffer uvBuffer;
    private IntBuffer indexBuffer;
    private int indexCount;
    private boolean modelLoaded = false;

    // Textura
    private int textureId = -1;

    // Shader
    private int shaderProgram = -1;
    private int aPositionHandle;
    private int aTexCoordHandle;
    private int uMVPMatrixHandle;
    private int uTextureHandle;

    // ═══════════════════════════════════════════════════════════════════════
    // 🗡️ POSICIÓN Y TRANSFORMACIÓN
    // ═══════════════════════════════════════════════════════════════════════

    private float x = 0f;
    private float y = -0.5f;      // Hacia abajo
    private float z = 0f;
    private float scale = 1.5f;

    // Rotación
    private float rotationY = 180f;  // Mirando hacia la cámara
    private float rotationSpeed = 0f; // Rotación automática (0 = sin rotación)

    // Pantalla y proyección
    private int screenWidth = 1080;
    private int screenHeight = 1920;
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] vpMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // ═══════════════════════════════════════════════════════════════════════
    // SHADERS - Simple textured
    // ═══════════════════════════════════════════════════════════════════════

    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "precision highp float;\n" +
        "in vec3 aPosition;\n" +
        "in vec2 aTexCoord;\n" +
        "uniform mat4 uMVPMatrix;\n" +
        "out vec2 vTexCoord;\n" +
        "void main() {\n" +
        "    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);\n" +
        "    vTexCoord = aTexCoord;\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "uniform sampler2D uTexture;\n" +
        "in vec2 vTexCoord;\n" +
        "out vec4 fragColor;\n" +
        "void main() {\n" +
        "    vec4 texColor = texture(uTexture, vTexCoord);\n" +
        "    // Descartar píxeles muy transparentes\n" +
        "    if (texColor.a < 0.1) discard;\n" +
        "    fragColor = texColor;\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════

    public Link3D(Context context) {
        this.context = context;

        setupProjection();
        loadModel();
        loadTexture();
        compileShader();

        Log.d(TAG, "🗡️ Link3D creado");
        Log.d(TAG, "📍 Posición: x=" + x + " y=" + y + " z=" + z + " scale=" + scale);
    }

    private void setupProjection() {
        // View matrix - cámara mirando hacia -Z
        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, 3f,    // Posición cámara
            0f, 0f, 0f,    // Mirando a
            0f, 1f, 0f);   // Up vector

        // Projection matrix
        float ratio = (float) screenWidth / screenHeight;
        Matrix.perspectiveM(projectionMatrix, 0, 60f, ratio, 0.1f, 100f);

        // VP matrix
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
    }

    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        setupProjection();
        Log.d(TAG, "📐 Screen size: " + width + "x" + height);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 📦 CARGA DE MODELO DESDE ASSETS
    // ═══════════════════════════════════════════════════════════════════════

    private void loadModel() {
        try {
            ObjLoader.Mesh mesh;

            // Prioridad: descarga remota (Supabase) > assets locales
            ModelDownloadManager modelMgr = ModelDownloadManager.getInstance(context);
            String modelPath = modelMgr.getModelPath("link_3d.obj");

            if (modelPath != null && new File(modelPath).exists()) {
                Log.d(TAG, "🌐 Cargando link_3d.obj desde descarga: " + modelPath);
                mesh = ObjLoader.loadObjFromFile(modelPath, true);
            } else {
                Log.d(TAG, "📂 Cargando link_3d.obj desde assets (fallback)...");
                mesh = ObjLoader.loadObj(context, "link_3d.obj", true);
            }

            Log.d(TAG, "✓ Modelo cargado: " + mesh.vertexCount + " vértices");

            this.vertexBuffer = mesh.vertexBuffer;
            this.uvBuffer = mesh.uvBuffer;

            // Convertir faces a índices triangulares
            int totalIndices = 0;
            for (int[] face : mesh.faces) {
                totalIndices += (face.length - 2) * 3;
            }

            int[] indices = new int[totalIndices];
            int idx = 0;
            for (int[] face : mesh.faces) {
                int v0 = face[0];
                for (int i = 1; i < face.length - 1; i++) {
                    indices[idx++] = v0;
                    indices[idx++] = face[i];
                    indices[idx++] = face[i + 1];
                }
            }

            this.indexCount = totalIndices;

            ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 4);
            ibb.order(ByteOrder.nativeOrder());
            indexBuffer = ibb.asIntBuffer();
            indexBuffer.put(indices);
            indexBuffer.position(0);

            modelLoaded = true;
            Log.d(TAG, "✅ Modelo listo: " + indexCount + " índices");

        } catch (IOException e) {
            Log.e(TAG, "❌ Error cargando modelo: " + e.getMessage());
            modelLoaded = false;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🎨 CARGA DE TEXTURA - Primero cache (Supabase), luego assets
    // ═══════════════════════════════════════════════════════════════════════

    private static final String TEXTURE_FILE = "link_3d_texture.webp";

    private void loadTexture() {
        Bitmap bitmap = null;

        // Opciones para reducir uso de RAM (1024x1024 en vez de 2048x2048)
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;  // Carga a mitad de resolución = 75% menos RAM
        options.inPreferredConfig = Bitmap.Config.RGB_565;  // 🔧 FIX MEMORY: 50% less GPU for opaque 3D texture

        // 1. Intentar cargar desde cache (descargado de Supabase)
        File imageDir = ImageDownloadManager.getInstance(context).getImageDirectory();
        File cachedFile = new File(imageDir, TEXTURE_FILE);
        if (cachedFile.exists()) {
            try {
                Log.d(TAG, "🎨 Cargando textura desde cache (1024x1024): " + cachedFile.getPath());
                FileInputStream fis = new FileInputStream(cachedFile);
                bitmap = BitmapFactory.decodeStream(fis, null, options);
                fis.close();
                Log.d(TAG, "✅ Textura cargada desde cache (" + bitmap.getWidth() + "x" + bitmap.getHeight() + ")");
            } catch (IOException e) {
                Log.w(TAG, "⚠️ Error leyendo cache, intentando assets...");
                bitmap = null;
            }
        }

        // 2. Fallback: cargar desde assets
        if (bitmap == null) {
            try {
                Log.d(TAG, "🎨 Cargando textura desde assets...");
                InputStream is = context.getAssets().open(TEXTURE_FILE);
                bitmap = BitmapFactory.decodeStream(is, null, options);
                is.close();
                Log.d(TAG, "✅ Textura cargada desde assets");
            } catch (IOException e) {
                Log.e(TAG, "❌ Error cargando textura: " + e.getMessage());
                return;
            }
        }

        if (bitmap == null) {
            Log.e(TAG, "❌ No se pudo decodificar textura");
            return;
        }

        // Crear textura OpenGL
        int[] textureIds = new int[1];
        GLES30.glGenTextures(1, textureIds, 0);
        textureId = textureIds[0];

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);

        // Configurar filtrado
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);

        bitmap.recycle();

        Log.d(TAG, "✅ Textura OpenGL creada: ID=" + textureId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🔧 COMPILACIÓN DE SHADER
    // ═══════════════════════════════════════════════════════════════════════

    private void compileShader() {
        int vs = compileShaderCode(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShaderCode(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        if (vs == 0 || fs == 0) {
            Log.e(TAG, "❌ Error compilando shaders");
            return;
        }

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        aPositionHandle = GLES30.glGetAttribLocation(shaderProgram, "aPosition");
        aTexCoordHandle = GLES30.glGetAttribLocation(shaderProgram, "aTexCoord");
        uMVPMatrixHandle = GLES30.glGetUniformLocation(shaderProgram, "uMVPMatrix");
        uTextureHandle = GLES30.glGetUniformLocation(shaderProgram, "uTexture");

        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        Log.d(TAG, "✅ Shader compilado");
    }

    private int compileShaderCode(int type, String code) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, code);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader error: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UPDATE & DRAW
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void update(float deltaTime) {
        // Rotación automática (si está habilitada)
        if (rotationSpeed != 0) {
            rotationY += rotationSpeed * deltaTime;
            if (rotationY > 360f) rotationY -= 360f;
        }
    }

    @Override
    public void draw() {
        if (!modelLoaded || shaderProgram <= 0 || textureId <= 0) {
            return;
        }

        // Usar shader
        GLES30.glUseProgram(shaderProgram);

        // Construir model matrix
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f);
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        // Calcular MVP
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);

        // Pasar uniforms
        GLES30.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);

        // Textura
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(uTextureHandle, 0);

        // Vertices
        GLES30.glEnableVertexAttribArray(aPositionHandle);
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        // UVs
        GLES30.glEnableVertexAttribArray(aTexCoordHandle);
        GLES30.glVertexAttribPointer(aTexCoordHandle, 2, GLES30.GL_FLOAT, false, 0, uvBuffer);

        // Dibujar
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, indexBuffer);

        // Limpiar
        GLES30.glDisableVertexAttribArray(aPositionHandle);
        GLES30.glDisableVertexAttribArray(aTexCoordHandle);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SETTERS PARA CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════════════

    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setRotationY(float angle) {
        this.rotationY = angle;
    }

    public void setRotationSpeed(float speed) {
        this.rotationSpeed = speed;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 👆 TOUCH INTERACTIVO - Para ajustar posición/escala/rotación
    // ═══════════════════════════════════════════════════════════════════════

    private boolean touchEnabled = true;
    private float lastTouchX, lastTouchY;
    private float lastPinchDistance = 0f;
    private float lastRotationAngle = 0f;
    private int touchMode = 0; // 0=none, 1=move, 2=pinch/rotate

    // Sensibilidad de controles
    private static final float MOVE_SENSITIVITY = 0.005f;
    private static final float SCALE_SENSITIVITY = 0.01f;
    private static final float ROTATE_SENSITIVITY = 0.5f;

    public void setTouchEnabled(boolean enabled) {
        this.touchEnabled = enabled;
    }

    /**
     * Procesa eventos de touch para ajustar Link interactivamente.
     * @param action MotionEvent action (ACTION_DOWN, ACTION_MOVE, etc)
     * @param x coordenada X del primer dedo
     * @param y coordenada Y del primer dedo
     * @param x2 coordenada X del segundo dedo (-1 si no hay)
     * @param y2 coordenada Y del segundo dedo (-1 si no hay)
     * @param pointerCount número de dedos tocando
     */
    public void onTouch(int action, float x, float y, float x2, float y2, int pointerCount) {
        if (!touchEnabled) return;

        // ACTION_DOWN = 0, ACTION_UP = 1, ACTION_MOVE = 2, ACTION_POINTER_DOWN = 5

        if (action == 0) { // ACTION_DOWN
            lastTouchX = x;
            lastTouchY = y;
            touchMode = 1;
            Log.d(TAG, "👆 Touch START");
        }
        else if (action == 5 && pointerCount >= 2) { // ACTION_POINTER_DOWN
            // Segundo dedo tocó - cambiar a modo pinch/rotate
            touchMode = 2;
            lastPinchDistance = distance(x, y, x2, y2);
            lastRotationAngle = angle(x, y, x2, y2);
            Log.d(TAG, "👆 Pinch/Rotate START");
        }
        else if (action == 2) { // ACTION_MOVE
            if (touchMode == 1 && pointerCount == 1) {
                // Un dedo: mover X/Y
                float dx = (x - lastTouchX) * MOVE_SENSITIVITY;
                float dy = -(y - lastTouchY) * MOVE_SENSITIVITY; // Invertir Y

                this.x += dx;
                this.y += dy;

                lastTouchX = x;
                lastTouchY = y;
            }
            else if (touchMode == 2 && pointerCount >= 2) {
                // Dos dedos: pinch para escala, rotación para rotar
                float currentDistance = distance(x, y, x2, y2);
                float currentAngle = angle(x, y, x2, y2);

                // Escala con pinch
                float scaleDelta = (currentDistance - lastPinchDistance) * SCALE_SENSITIVITY;
                this.scale = Math.max(0.1f, Math.min(5.0f, this.scale + scaleDelta));

                // Rotación con giro de dedos
                float angleDelta = (currentAngle - lastRotationAngle) * ROTATE_SENSITIVITY;
                this.rotationY += angleDelta;
                if (this.rotationY > 360f) this.rotationY -= 360f;
                if (this.rotationY < 0f) this.rotationY += 360f;

                lastPinchDistance = currentDistance;
                lastRotationAngle = currentAngle;
            }
        }
        else if (action == 1 || action == 6) { // ACTION_UP or ACTION_POINTER_UP
            if (pointerCount <= 1) {
                touchMode = 0;
                // Log valores finales
                logCurrentValues();
            } else {
                touchMode = 1; // Volver a modo de un dedo
                lastTouchX = x;
                lastTouchY = y;
            }
        }
    }

    private float distance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }

    private float angle(float x1, float y1, float x2, float y2) {
        return (float) Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
    }

    /**
     * Log de valores actuales para copiar al código
     */
    public void logCurrentValues() {
        Log.d(TAG, "════════════════════════════════════════════════════════════");
        Log.d(TAG, "🗡️ LINK3D VALORES ACTUALES:");
        Log.d(TAG, "   setPosition(" + String.format("%.3f", x) + "f, " +
                                       String.format("%.3f", y) + "f, " +
                                       String.format("%.3f", z) + "f);");
        Log.d(TAG, "   setScale(" + String.format("%.3f", scale) + "f);");
        Log.d(TAG, "   setRotationY(" + String.format("%.1f", rotationY) + "f);");
        Log.d(TAG, "════════════════════════════════════════════════════════════");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DISPOSE
    // ═══════════════════════════════════════════════════════════════════════

    public void dispose() {
        if (textureId > 0) {
            GLES30.glDeleteTextures(1, new int[]{textureId}, 0);
            textureId = -1;
        }
        if (shaderProgram > 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = -1;
        }
        modelLoaded = false;
        Log.d(TAG, "🗑️ Link3D disposed");
    }
}
