package com.secret.blackholeglow;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.scenes.SceneConstants;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   🎮 PLAYER INDICATOR - Indicador visual del jugador                     ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  CARACTERÍSTICAS:                                                         ║
 * ║  • Muestra "P1" flotando sobre la nave del jugador                       ║
 * ║  • Color cyan brillante para alta visibilidad                            ║
 * ║  • Parpadea cuando HP está bajo                                          ║
 * ║  • Se oculta cuando la nave está destruida                               ║
 * ║  • Muestra contador de respawn cuando está en espera                     ║
 * ║                                                                           ║
 * ║  INTEGRACIÓN:                                                             ║
 * ║  • Implementa SceneObject + CameraAware                                  ║
 * ║  • Se agrega a la escena con addSceneObject()                            ║
 * ║  • Recibe referencia a HumanInterceptor                                  ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class PlayerIndicator implements SceneObject, CameraAware {
    private static final String TAG = "PlayerIndicator";

    // ═══════════════════════════════════════════════════════════════════════
    // 🔗 REFERENCIAS
    // ═══════════════════════════════════════════════════════════════════════
    private HumanInterceptor player;
    private CameraController camera;

    // ═══════════════════════════════════════════════════════════════════════
    // 🎨 RENDERIZADO
    // ═══════════════════════════════════════════════════════════════════════
    private int shaderProgram;
    private FloatBuffer vertexBuffer;
    private int aPositionHandle;
    private int uMVPMatrixHandle;
    private int uColorHandle;
    private int uAlphaHandle;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // ═══════════════════════════════════════════════════════════════════════
    // 📊 ESTADO
    // ═══════════════════════════════════════════════════════════════════════
    private float timeAccumulator = 0f;
    private boolean initialized = false;

    // Posición actual (sincronizada con player)
    private float x, y, z;

    // ═══════════════════════════════════════════════════════════════════════
    // 🎯 CONFIGURACIÓN (desde SceneConstants)
    // ═══════════════════════════════════════════════════════════════════════
    private final String label = SceneConstants.PlayerIndicator.LABEL;
    private final float offsetY = SceneConstants.PlayerIndicator.OFFSET_Y;
    private final float textScale = SceneConstants.PlayerIndicator.TEXT_SCALE;
    private final float colorR = SceneConstants.PlayerIndicator.COLOR_R;
    private final float colorG = SceneConstants.PlayerIndicator.COLOR_G;
    private final float colorB = SceneConstants.PlayerIndicator.COLOR_B;
    private final float lowHpThreshold = SceneConstants.PlayerIndicator.LOW_HP_THRESHOLD;
    private final float blinkSpeed = SceneConstants.PlayerIndicator.BLINK_SPEED;

    /**
     * Constructor
     */
    public PlayerIndicator() {
        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   🎮 PLAYER INDICATOR CREADO           ║");
        Log.d(TAG, "╚════════════════════════════════════════╝");
        initGL();
    }

    /**
     * Asigna el jugador a seguir
     */
    public void setPlayer(HumanInterceptor player) {
        this.player = player;
        Log.d(TAG, "🎮 Player asignado: " + (player != null ? "HumanInterceptor" : "null"));
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🎨 INICIALIZACIÓN OPENGL
    // ═══════════════════════════════════════════════════════════════════════

    private void initGL() {
        // Crear geometría para "P1" - Usamos líneas para dibujar las letras
        // P: rectángulo + semicírculo arriba
        // 1: línea vertical + base

        // Vértices para dibujar "P1" con líneas
        float[] vertices = createP1Vertices();

        vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(vertices).position(0);

        // Crear shaders
        String vertexShaderCode =
                "uniform mat4 u_MVPMatrix;" +
                "attribute vec4 a_Position;" +
                "void main() {" +
                "    gl_Position = u_MVPMatrix * a_Position;" +
                "}";

        String fragmentShaderCode =
                "precision mediump float;" +
                "uniform vec4 u_Color;" +
                "uniform float u_Alpha;" +
                "void main() {" +
                "    gl_FragColor = vec4(u_Color.rgb, u_Color.a * u_Alpha);" +
                "}";

        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode);

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vertexShader);
        GLES30.glAttachShader(shaderProgram, fragmentShader);
        GLES30.glLinkProgram(shaderProgram);

        aPositionHandle = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        uMVPMatrixHandle = GLES30.glGetUniformLocation(shaderProgram, "u_MVPMatrix");
        uColorHandle = GLES30.glGetUniformLocation(shaderProgram, "u_Color");
        uAlphaHandle = GLES30.glGetUniformLocation(shaderProgram, "u_Alpha");

        initialized = true;
        Log.d(TAG, "✅ OpenGL inicializado");
    }

    /**
     * Crea los vértices para dibujar "P1" con líneas
     * Coordenadas locales centradas en (0,0)
     */
    private float[] createP1Vertices() {
        // "P" - Letra P estilizada
        // "1" - Número 1
        // Escala base: 1.0 = altura total

        float w = 0.3f;  // Ancho de cada caracter
        float h = 0.5f;  // Alto
        float spacing = 0.15f;  // Espacio entre P y 1

        // Offset para centrar "P1"
        float totalWidth = w * 2 + spacing;
        float offsetX = -totalWidth / 2;

        return new float[] {
            // ═══ LETRA "P" ═══
            // Línea vertical izquierda
            offsetX, -h/2, 0,
            offsetX, h/2, 0,

            // Línea superior horizontal
            offsetX, h/2, 0,
            offsetX + w, h/2, 0,

            // Línea derecha superior (mitad)
            offsetX + w, h/2, 0,
            offsetX + w, 0, 0,

            // Línea horizontal media
            offsetX + w, 0, 0,
            offsetX, 0, 0,

            // ═══ NÚMERO "1" ═══
            // Línea vertical principal
            offsetX + w + spacing + w/2, -h/2, 0,
            offsetX + w + spacing + w/2, h/2, 0,

            // Línea diagonal arriba (opcional, estilo)
            offsetX + w + spacing + w/4, h/4, 0,
            offsetX + w + spacing + w/2, h/2, 0,

            // Base inferior
            offsetX + w + spacing, -h/2, 0,
            offsetX + w + spacing + w, -h/2, 0,
        };
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);
        return shader;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🔄 UPDATE
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void update(float deltaTime) {
        timeAccumulator += deltaTime;

        if (player == null) return;

        // Sincronizar posición con el jugador
        x = player.getX();
        y = player.getY() + offsetY;  // Arriba de la nave
        z = player.getZ();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🎨 DRAW
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void draw() {
        if (!initialized || camera == null || player == null) return;
        if (shaderProgram == 0) return;

        // Determinar visibilidad y alpha
        float alpha = 1.0f;
        boolean shouldDraw = true;

        if (player.isDestroyed()) {
            // Nave destruida - mostrar semi-transparente
            alpha = SceneConstants.PlayerIndicator.RESPAWN_ALPHA;
        } else {
            // Verificar HP bajo para parpadeo
            float hpRatio = (float) player.getHealth() / player.getMaxHealth();
            if (hpRatio <= lowHpThreshold) {
                // Parpadeo cuando HP bajo
                float blink = (float) Math.sin(timeAccumulator * blinkSpeed * Math.PI * 2);
                if (blink < 0) {
                    alpha = 0.3f;  // Semi-visible durante parpadeo
                }
            }
        }

        // Configurar OpenGL
        GLES30.glUseProgram(shaderProgram);

        // Configurar blending para transparencia
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Calcular matriz modelo (posición + escala + billboard hacia cámara)
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        Matrix.scaleM(modelMatrix, 0, textScale, textScale, textScale);

        // Calcular MVP
        camera.computeMvp(modelMatrix, mvpMatrix);

        // Pasar uniforms
        GLES30.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES30.glUniform4f(uColorHandle, colorR, colorG, colorB, 1.0f);
        GLES30.glUniform1f(uAlphaHandle, alpha);

        // Dibujar líneas gruesas
        GLES30.glLineWidth(3.0f);

        // Pasar vértices
        GLES30.glEnableVertexAttribArray(aPositionHandle);
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        // Dibujar como líneas
        GLES30.glDrawArrays(GLES30.GL_LINES, 0, 14);  // 7 líneas = 14 vértices

        GLES30.glDisableVertexAttribArray(aPositionHandle);
        GLES30.glDisable(GLES30.GL_BLEND);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🧹 CLEANUP
    // ═══════════════════════════════════════════════════════════════════════

    public void dispose() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        Log.d(TAG, "🧹 PlayerIndicator disposed");
    }
}
