package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.image.ImageDownloadManager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   🔥 ThrusterFlames - Llamas de propulsión animadas (Sprite Sheet)       ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  CARACTERÍSTICAS:                                                         ║
 * ║  • Sprite sheet 6x2 (12 frames)                                          ║
 * ║  • Animación de UV para recorrer frames                                  ║
 * ║  • Se posiciona detrás de la nave                                        ║
 * ║  • Escala y alpha según estado del motor                                 ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class ThrusterFlames implements SceneObject, CameraAware {
    private static final String TAG = "ThrusterFlames";

    // Sprite sheet config
    private static final int COLS = 6;  // Columnas en el sprite sheet
    private static final int ROWS = 2;  // Filas en el sprite sheet
    private static final int TOTAL_FRAMES = COLS * ROWS;  // 12 frames
    private static final float FRAME_DURATION = 0.05f;  // Segundos por frame (20 FPS)

    // Buffers
    private FloatBuffer vertexBuffer;
    private FloatBuffer uvBuffer;

    // OpenGL
    private int shaderProgram;
    private int textureId;
    private int aPositionHandle;
    private int aTexCoordHandle;
    private int uMVPMatrixHandle;
    private int uTextureHandle;
    private int uAlphaHandle;
    private int uFrameOffsetHandle;

    // Animación
    private float animTime = 0f;
    private int currentFrame = 0;

    // ═══════════════════════════════════════════════════════════════════════
    // VALORES DE REFERENCIA (Dic 29, 2024)
    // ═══════════════════════════════════════════════════════════════════════
    // INICIALES (nave en posición inicial):
    //   offsetX=1.148, offsetY=0.389, offsetZ=-0.176
    //   rotX=18.1, rotY=-29.2, rotZ=41.6
    //   scaleX=1.158, scaleY=0.269
    //
    // FINALES (después de viaje Z, ajustados manualmente):
    //   offsetX=1.333, offsetY=0.380, offsetZ=0.009
    //   rotX=39.4, rotY=-22.7, rotZ=42.1
    //   scaleX=0.945, scaleY=0.241
    // ═══════════════════════════════════════════════════════════════════════

    // Transformación - CALIBRADO con offset escalado (Dic 29, 2024)
    private float offsetX = 1.055f;
    private float offsetY = 0.852f;
    private float offsetZ = 1.639f;
    private float scaleX = 0.482f;
    private float scaleY = 0.426f;
    private float pulseScaleX = 1f;
    private float pulseScaleY = 1f;
    private float intensityScaleY = 1f;
    private float alpha = 1.0f;

    // Rotación propia de las llamas - CALIBRADO con offset escalado
    private float flameRotX = -17.5f;
    private float flameRotY = 43.0f;
    private float flameRotZ = 6.0f;

    // Modo de ajuste (para posicionar las llamas con touch)
    private boolean adjustMode = false;  // false = llamas SIGUEN a la nave
    private int adjustPhase = 0;  // RESET: Empezar desde OFFSET X
    private float lastTouchX = 0f;
    private boolean isTouching = false;
    private static final String[] PHASE_NAMES = {
        "OFFSET X", "OFFSET Y", "OFFSET Z",
        "ROTACIÓN X", "ROTACIÓN Y", "ROTACIÓN Z",
        "ESCALA X", "ESCALA Y"
    };

    // Cámara y matrices
    private CameraController camera;
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // Referencia a posición de la nave
    private float shipX, shipY, shipZ;
    private float shipRotationX = 0f;
    private float shipRotationY = 0f;
    private float shipRotationZ = 0f;
    private float shipScale = 1f;

    private final Context context;
    private final TextureLoader textureLoader;

    // ═══════════════════════════════════════════════════════════════════════
    // SHADERS
    // ═══════════════════════════════════════════════════════════════════════

    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "precision highp float;\n" +
        "in vec3 aPosition;\n" +
        "in vec2 aTexCoord;\n" +
        "uniform mat4 uMVPMatrix;\n" +
        "uniform vec2 uFrameOffset;\n" +
        "out vec2 vTexCoord;\n" +
        "void main() {\n" +
        "    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);\n" +
        "    // Ajustar UV según el frame actual\n" +
        "    vec2 frameSize = vec2(1.0 / 6.0, 1.0 / 2.0);\n" +
        "    vTexCoord = aTexCoord * frameSize + uFrameOffset;\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "uniform sampler2D uTexture;\n" +
        "uniform float uAlpha;\n" +
        "in vec2 vTexCoord;\n" +
        "out vec4 fragColor;\n" +
        "void main() {\n" +
        "    vec4 texColor = texture(uTexture, vTexCoord);\n" +
        "    // Additive blending para efecto de brillo\n" +
        "    fragColor = vec4(texColor.rgb * texColor.a, texColor.a * uAlpha);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════

    public ThrusterFlames(Context context, TextureLoader textureLoader) {
        this.context = context;
        this.textureLoader = textureLoader;

        createQuad();
        loadTexture();
        compileShader();

        Log.d(TAG, "🔥 ThrusterFlames creado - 12 frames de animación");
    }

    private void createQuad() {
        // Quad vertical (billBoard hacia la cámara)
        float[] vertices = {
            -0.5f, -1.0f, 0f,  // Abajo izquierda
             0.5f, -1.0f, 0f,  // Abajo derecha
             0.5f,  0.5f, 0f,  // Arriba derecha
            -0.5f,  0.5f, 0f   // Arriba izquierda
        };

        // UVs para un frame (se ajustan en el shader)
        float[] uvs = {
            0f, 1f,  // Abajo izquierda
            1f, 1f,  // Abajo derecha
            1f, 0f,  // Arriba derecha
            0f, 0f   // Arriba izquierda
        };

        // Vertex buffer
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // UV buffer
        ByteBuffer ubb = ByteBuffer.allocateDirect(uvs.length * 4);
        ubb.order(ByteOrder.nativeOrder());
        uvBuffer = ubb.asFloatBuffer();
        uvBuffer.put(uvs);
        uvBuffer.position(0);
    }

    private void loadTexture() {
        // Cargar desde archivos descargados (ResourcePreloader garantiza disponibilidad)
        ImageDownloadManager imageMgr = ImageDownloadManager.getInstance(context);
        String texturePath = imageMgr.getImagePath("thruster_flames.png");

        if (texturePath != null && textureLoader instanceof TextureManager) {
            Log.d(TAG, "🌐 Cargando textura desde descarga: " + texturePath);
            textureId = ((TextureManager) textureLoader).loadTextureFromFile(texturePath);
        } else {
            Log.e(TAG, "❌ Textura no disponible: thruster_flames.png");
            return;
        }
        Log.d(TAG, "✅ Textura de llamas cargada: " + textureId);
    }

    private void compileShader() {
        int vs = compileShaderCode(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShaderCode(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        aPositionHandle = GLES30.glGetAttribLocation(shaderProgram, "aPosition");
        aTexCoordHandle = GLES30.glGetAttribLocation(shaderProgram, "aTexCoord");
        uMVPMatrixHandle = GLES30.glGetUniformLocation(shaderProgram, "uMVPMatrix");
        uTextureHandle = GLES30.glGetUniformLocation(shaderProgram, "uTexture");
        uAlphaHandle = GLES30.glGetUniformLocation(shaderProgram, "uAlpha");
        uFrameOffsetHandle = GLES30.glGetUniformLocation(shaderProgram, "uFrameOffset");

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
    // UPDATE
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void update(float deltaTime) {
        // Frame fijo (el sprite sheet mueve la llama horizontalmente entre frames)
        currentFrame = 0;

        // Efecto de PULSO - calcular multiplicadores
        if (!adjustMode) {
            animTime += deltaTime;
            pulseScaleY = 1.0f + 0.15f * (float) Math.sin(animTime * 8.0f);
            pulseScaleX = 1.0f + 0.08f * (float) Math.sin(animTime * 6.0f);
        } else {
            // Sin pulso en modo ajuste
            pulseScaleX = 1f;
            pulseScaleY = 1f;
        }
    }

    /**
     * Actualiza la posición para seguir a la nave
     */
    public void followShip(float x, float y, float z, float rotX, float rotY, float rotZ, float scale) {
        this.shipX = x;
        this.shipY = y;
        this.shipZ = z;
        this.shipRotationX = rotX;
        this.shipRotationY = rotY;
        this.shipRotationZ = rotZ;
        this.shipScale = scale;
    }

    /**
     * Ajusta la intensidad de las llamas (0 = apagado, 1 = máximo)
     * NOTA: Usa intensityScaleY separado para NO sobrescribir scaleY calibrado
     */
    public void setIntensity(float intensity) {
        this.alpha = Math.max(0f, Math.min(1f, intensity));
        // Multiplicador separado - no modifica scaleY base
        this.intensityScaleY = 0.8f + intensity * 0.4f;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DRAW
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void draw() {
        if (shaderProgram == 0 || camera == null || alpha <= 0.01f) return;

        GLES30.glUseProgram(shaderProgram);

        // Habilitar blending aditivo para efecto de fuego
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE);  // Additive

        // Deshabilitar depth write para transparencia
        GLES30.glDepthMask(false);

        // Matriz de modelo
        Matrix.setIdentityM(modelMatrix, 0);

        if (adjustMode) {
            // MODO AJUSTE: Llamas FIJAS en centro, solo offset y rotación propias
            // 1. Posición fija en centro de pantalla
            Matrix.translateM(modelMatrix, 0, offsetX, offsetY, -3f + offsetZ);
            // 2. Rotaciones propias de las llamas (ajustables con touch)
            Matrix.rotateM(modelMatrix, 0, flameRotX, 1f, 0f, 0f);
            Matrix.rotateM(modelMatrix, 0, flameRotY, 0f, 1f, 0f);
            Matrix.rotateM(modelMatrix, 0, flameRotZ, 0f, 0f, 1f);
            // 3. Escala ajustable con touch (sin pulso)
            Matrix.scaleM(modelMatrix, 0, scaleX * pulseScaleX, scaleY * pulseScaleY, 1f);
        } else {
            // MODO NORMAL: Seguir a la nave con BILLBOARD
            // ═══════════════════════════════════════════════════════════════
            // BILLBOARD: Las llamas siempre miran hacia la cámara
            // ═══════════════════════════════════════════════════════════════

            // 1. Posición = nave + offset * escala
            float flameX = shipX + offsetX * shipScale;
            float flameY = shipY + offsetY * shipScale;
            float flameZ = shipZ + offsetZ * shipScale;

            // 2. Obtener matriz de vista de la cámara
            float[] viewMatrix = camera.getViewMatrix();

            // 3. Crear billboard: extraer vectores right y up de la vista
            // La matriz de vista tiene: [right.x, up.x, forward.x, ...]
            //                           [right.y, up.y, forward.y, ...]
            //                           [right.z, up.z, forward.z, ...]
            float rightX = viewMatrix[0];
            float rightY = viewMatrix[4];
            float rightZ = viewMatrix[8];
            float upX = viewMatrix[1];
            float upY = viewMatrix[5];
            float upZ = viewMatrix[9];

            // 4. Construir matriz de modelo con billboard
            // La matriz hace que el quad siempre mire a la cámara
            modelMatrix[0] = rightX;
            modelMatrix[1] = rightY;
            modelMatrix[2] = rightZ;
            modelMatrix[3] = 0;

            modelMatrix[4] = upX;
            modelMatrix[5] = upY;
            modelMatrix[6] = upZ;
            modelMatrix[7] = 0;

            // Forward es el producto cruz de right y up
            modelMatrix[8] = rightY * upZ - rightZ * upY;
            modelMatrix[9] = rightZ * upX - rightX * upZ;
            modelMatrix[10] = rightX * upY - rightY * upX;
            modelMatrix[11] = 0;

            modelMatrix[12] = flameX;
            modelMatrix[13] = flameY;
            modelMatrix[14] = flameZ;
            modelMatrix[15] = 1;

            // 5. Escalar (base * pulso * intensidad * nave)
            Matrix.scaleM(modelMatrix, 0,
                scaleX * pulseScaleX * shipScale,
                scaleY * pulseScaleY * intensityScaleY * shipScale,
                1f);
        }

        // MVP
        camera.computeMvp(modelMatrix, mvpMatrix);
        GLES30.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);

        // Frame offset para sprite sheet
        int col = currentFrame % COLS;
        int row = currentFrame / COLS;
        float frameOffsetX = col / (float) COLS;
        float frameOffsetY = row / (float) ROWS;
        GLES30.glUniform2f(uFrameOffsetHandle, frameOffsetX, frameOffsetY);

        // Alpha
        GLES30.glUniform1f(uAlphaHandle, alpha);

        // Textura
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(uTextureHandle, 0);

        // Vértices
        GLES30.glEnableVertexAttribArray(aPositionHandle);
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        // UVs
        GLES30.glEnableVertexAttribArray(aTexCoordHandle);
        GLES30.glVertexAttribPointer(aTexCoordHandle, 2, GLES30.GL_FLOAT, false, 0, uvBuffer);

        // Dibujar quad
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 4);

        // Limpiar
        GLES30.glDisableVertexAttribArray(aPositionHandle);
        GLES30.glDisableVertexAttribArray(aTexCoordHandle);

        // Restaurar estado
        GLES30.glDepthMask(true);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    public void setOffset(float x, float y, float z) {
        this.offsetX = x;
        this.offsetY = y;
        this.offsetZ = z;
    }

    public void setScale(float x, float y) {
        this.scaleX = x;
        this.scaleY = y;
    }

    /**
     * Ajusta la rotación propia de las llamas para orientarlas
     */
    public void setFlameRotation(float rotX, float rotY, float rotZ) {
        this.flameRotX = rotX;
        this.flameRotY = rotY;
        this.flameRotZ = rotZ;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MODO AJUSTE CON TOUCH
    // ═══════════════════════════════════════════════════════════════════════

    public void setAdjustMode(boolean enabled) {
        this.adjustMode = enabled;
        Log.d(TAG, "🔧 Modo ajuste: " + (enabled ? "ACTIVADO" : "DESACTIVADO"));
    }

    /**
     * Loggea todos los valores actuales de las llamas
     */
    public void logCurrentValues(String label) {
        Log.d(TAG, "🔥   [" + label + "] Offset   → X=" + offsetX + ", Y=" + offsetY + ", Z=" + offsetZ);
        Log.d(TAG, "🔥   [" + label + "] Rotación → X=" + flameRotX + "°, Y=" + flameRotY + "°, Z=" + flameRotZ + "°");
        Log.d(TAG, "🔥   [" + label + "] Escala   → X=" + scaleX + ", Y=" + scaleY);
        Log.d(TAG, "🔥   [" + label + "] Ship pos → X=" + shipX + ", Y=" + shipY + ", Z=" + shipZ);
    }

    public boolean isAdjustMode() {
        return adjustMode;
    }

    public void onTouchDown(float x, float y) {
        lastTouchX = x;
        isTouching = true;
        Log.d(TAG, "🔥 ══════════════════════════════════════════");
        Log.d(TAG, "🔥 MODO " + (adjustPhase + 1) + "/6: " + PHASE_NAMES[adjustPhase]);
        Log.d(TAG, "🔥 ══════════════════════════════════════════");
    }

    public void onTouchMove(float x, float y) {
        if (!isTouching) return;

        float deltaX = x - lastTouchX;
        lastTouchX = x;

        float sensitivity = (adjustPhase < 3) ? 0.01f : 0.5f;  // Offset vs Rotación

        switch (adjustPhase) {
            case 0: offsetX += deltaX * sensitivity;
                Log.d(TAG, "🔥 OFFSET X → " + String.format("%.3f", offsetX)); break;
            case 1: offsetY += deltaX * sensitivity;
                Log.d(TAG, "🔥 OFFSET Y → " + String.format("%.3f", offsetY)); break;
            case 2: offsetZ += deltaX * sensitivity;
                Log.d(TAG, "🔥 OFFSET Z → " + String.format("%.3f", offsetZ)); break;
            case 3: flameRotX += deltaX * sensitivity;
                Log.d(TAG, "🔥 ROT X → " + String.format("%.1f°", flameRotX)); break;
            case 4: flameRotY += deltaX * sensitivity;
                Log.d(TAG, "🔥 ROT Y → " + String.format("%.1f°", flameRotY)); break;
            case 5: flameRotZ += deltaX * sensitivity;
                Log.d(TAG, "🔥 ROT Z → " + String.format("%.1f°", flameRotZ)); break;
            case 6: scaleX += deltaX * 0.01f;
                Log.d(TAG, "🔥 ESCALA X → " + String.format("%.3f", scaleX)); break;
            case 7: scaleY += deltaX * 0.01f;
                Log.d(TAG, "🔥 ESCALA Y → " + String.format("%.3f", scaleY)); break;
        }
    }

    public void onTouchUp(float x, float y) {
        isTouching = false;

        // Mostrar TODOS los valores actuales
        Log.d(TAG, "");
        Log.d(TAG, "🔥═══════════════════════════════════════════════════════");
        Log.d(TAG, "🔥              L L A M A S  (ThrusterFlames)            ");
        Log.d(TAG, "🔥═══════════════════════════════════════════════════════");
        Log.d(TAG, "🔥 Offset   → X=" + String.format("%.3f", offsetX) + "f, Y=" + String.format("%.3f", offsetY) + "f, Z=" + String.format("%.3f", offsetZ) + "f");
        Log.d(TAG, "🔥 Rotación → X=" + String.format("%.1f", flameRotX) + "°, Y=" + String.format("%.1f", flameRotY) + "°, Z=" + String.format("%.1f", flameRotZ) + "°");
        Log.d(TAG, "🔥 Escala   → X=" + String.format("%.3f", scaleX) + ", Y=" + String.format("%.3f", scaleY));
        Log.d(TAG, "🔥═══════════════════════════════════════════════════════");
        Log.d(TAG, "🚀              N A V E  (TravelingShip)                 ");
        Log.d(TAG, "🚀═══════════════════════════════════════════════════════");
        Log.d(TAG, "🚀 Posición → X=" + String.format("%.2f", shipX) + ", Y=" + String.format("%.2f", shipY) + ", Z=" + String.format("%.2f", shipZ));
        Log.d(TAG, "🚀 Rotación → X=" + String.format("%.1f", shipRotationX) + "°, Y=" + String.format("%.1f", shipRotationY) + "°, Z=" + String.format("%.1f", shipRotationZ) + "°");
        Log.d(TAG, "🚀 Escala   → " + String.format("%.3f", shipScale));
        Log.d(TAG, "🚀═══════════════════════════════════════════════════════");
        Log.d(TAG, "");

        // Cambiar al siguiente modo (ciclo de 8)
        adjustPhase = (adjustPhase + 1) % 8;
        Log.d(TAG, "🔥 >>> SIGUIENTE: " + PHASE_NAMES[adjustPhase] + " <<<");
    }

    public void release() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        Log.d(TAG, "🗑️ ThrusterFlames liberado");
    }
}
