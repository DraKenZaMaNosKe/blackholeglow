package com.secret.blackholeglow.scenes;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.Battery3D;
import com.secret.blackholeglow.Clock3D;
import com.secret.blackholeglow.rubik.RubiksCube;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                    RubikScene - Cubo Rubik Interactivo                   ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  🧊 Cubo Rubik 3D con controles visuales                                 ║
 * ║  🎮 Botones en pantalla para rotar caras                                 ║
 * ║  🖐️ Drag en el cubo para rotar la vista                                  ║
 * ║  ✨ Highlight visual al tocar                                            ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class RubikScene extends WallpaperScene {
    private static final String TAG = "RubikScene";

    // Componentes de la escena
    private RubiksCube rubiksCube;
    private Clock3D clock;
    private Battery3D battery;

    // Matrices de proyección
    private float[] projectionMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] viewProjectionMatrix = new float[16];

    // Touch handling
    private float lastTouchX = 0f;
    private float lastTouchY = 0f;
    private boolean isDragging = false;
    private long touchStartTime = 0;
    private float touchStartX = 0f;
    private float touchStartY = 0f;

    // ═══════════════════════════════════════════════════════════════════════
    // 🎮 SISTEMA DE BOTONES DE CONTROL
    // ═══════════════════════════════════════════════════════════════════════

    // Botones: L, R, U, D, F, B + flechas de rotación + SHUFFLE/RESET
    private static final int BTN_L = 0;
    private static final int BTN_R = 1;
    private static final int BTN_U = 2;
    private static final int BTN_D = 3;
    private static final int BTN_F = 4;
    private static final int BTN_B = 5;
    private static final int BTN_ROT_LEFT = 6;
    private static final int BTN_ROT_RIGHT = 7;
    private static final int BTN_SHUFFLE = 8;   // 🔀 Mezclar
    private static final int BTN_RESET = 9;     // ↺ Resetear
    private static final int BTN_COUNT = 10;

    // Posiciones de botones [x, y, width, height] en NDC (-1 a 1)
    private float[][] buttonBounds = new float[BTN_COUNT][4];

    // Colores de botones (RGBA)
    private static final float[][] BTN_COLORS = {
        {0.0f, 0.8f, 0.0f, 0.8f},  // L - Verde
        {0.0f, 0.0f, 0.9f, 0.8f},  // R - Azul
        {1.0f, 1.0f, 1.0f, 0.8f},  // U - Blanco
        {1.0f, 1.0f, 0.0f, 0.8f},  // D - Amarillo
        {0.9f, 0.0f, 0.0f, 0.8f},  // F - Rojo
        {1.0f, 0.5f, 0.0f, 0.8f},  // B - Naranja
        {0.5f, 0.5f, 0.5f, 0.8f},  // Rotar izq - Gris
        {0.5f, 0.5f, 0.5f, 0.8f},  // Rotar der - Gris
        {0.2f, 0.8f, 1.0f, 0.9f},  // SHUFFLE - Cyan brillante
        {1.0f, 0.3f, 0.3f, 0.9f},  // RESET - Rojo suave
    };

    private static final String[] BTN_LABELS = {"L", "R", "U", "D", "F", "B", "◀", "▶", "🔀", "↺"};

    private int pressedButton = -1;  // Botón actualmente presionado
    private int highlightZone = -1;  // Zona del cubo destacada

    // Shader para UI 2D
    private int uiShaderProgram = -1;
    private FloatBuffer quadBuffer;

    @Override
    public String getName() { return "RUBIK"; }

    @Override
    public String getDescription() {
        return "Cubo Rubik interactivo - ¡Resuélvelo!";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_oceano_sc;
    }

    @Override
    protected void setupScene() {
        Log.d(TAG, "🧊 Configurando escena Rubik con controles");

        // Crear el cubo Rubik
        rubiksCube = new RubiksCube();

        // Clock
        try {
            clock = new Clock3D(context, Clock3D.THEME_PYRALIS, 0f, 0.92f);
            Log.d(TAG, "⏰ Clock3D inicializado");
        } catch (Exception e) {
            Log.e(TAG, "Error Clock3D: " + e.getMessage());
        }

        // Battery3D
        try {
            battery = new Battery3D(context, Battery3D.THEME_PYRALIS, 0.75f, 0.92f);
            Log.d(TAG, "🔋 Battery3D inicializado");
        } catch (Exception e) {
            Log.e(TAG, "Error Battery3D: " + e.getMessage());
        }

        // Inicializar UI
        initUIShader();
        initButtons();
        setupMatrices();

        Log.d(TAG, "✅ Escena Rubik configurada");
    }

    private void initUIShader() {
        String vertexShader =
            "attribute vec4 a_Position;" +
            "void main() {" +
            "  gl_Position = a_Position;" +
            "}";

        String fragmentShader =
            "precision mediump float;" +
            "uniform vec4 u_Color;" +
            "void main() {" +
            "  gl_FragColor = u_Color;" +
            "}";

        int vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        uiShaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(uiShaderProgram, vs);
        GLES20.glAttachShader(uiShaderProgram, fs);
        GLES20.glLinkProgram(uiShaderProgram);

        // Buffer para quad
        float[] quad = {-1, -1, 1, -1, -1, 1, 1, 1};
        ByteBuffer bb = ByteBuffer.allocateDirect(quad.length * 4);
        bb.order(ByteOrder.nativeOrder());
        quadBuffer = bb.asFloatBuffer();
        quadBuffer.put(quad);
        quadBuffer.position(0);
    }

    private int loadShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);
        return shader;
    }

    private void initButtons() {
        // ════════════════════════════════════════════════════════════════════
        // Layout optimizado para evitar el dock del sistema
        // ════════════════════════════════════════════════════════════════════

        float btnW = 0.09f;
        float btnH = 0.09f;

        // L y R - a los lados del cubo
        buttonBounds[BTN_L] = new float[]{-0.92f, -0.08f, 0.06f, 0.14f};
        buttonBounds[BTN_R] = new float[]{0.86f, -0.08f, 0.06f, 0.14f};

        // U - arriba del cubo
        buttonBounds[BTN_U] = new float[]{-btnW/2, 0.35f, btnW, btnH};

        // D, F, B - fila horizontal
        float rowY = -0.40f;
        buttonBounds[BTN_F] = new float[]{-0.34f, rowY, btnW, btnH};
        buttonBounds[BTN_D] = new float[]{-btnW/2, rowY, btnW, btnH};
        buttonBounds[BTN_B] = new float[]{0.25f, rowY, btnW, btnH};

        // Flechas de rotación - a los lados arriba
        buttonBounds[BTN_ROT_LEFT] = new float[]{-0.92f, 0.20f, 0.06f, 0.06f};
        buttonBounds[BTN_ROT_RIGHT] = new float[]{0.86f, 0.20f, 0.06f, 0.06f};

        // ════════════════════════════════════════════════════════════════════
        // BOTONES ESPECIALES: SHUFFLE / RESET (solo 2 botones)
        // ════════════════════════════════════════════════════════════════════
        float specialW = 0.22f;  // Más anchos ahora que son solo 2
        float specialH = 0.07f;
        float specialY = 0.55f;

        buttonBounds[BTN_SHUFFLE] = new float[]{-0.48f, specialY, specialW, specialH};
        buttonBounds[BTN_RESET] = new float[]{0.26f, specialY, specialW, specialH};
    }

    private void setupMatrices() {
        float ratio = (float) screenWidth / Math.max(screenHeight, 1);
        Matrix.perspectiveM(projectionMatrix, 0, 45f, ratio, 0.1f, 100f);

        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, 16f,
            0f, 0f, 0f,
            0f, 1f, 0f);

        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
    }

    @Override
    public void setScreenSize(int width, int height) {
        super.setScreenSize(width, height);
        setupMatrices();
    }

    @Override
    public void update(float deltaTime) {
        if (rubiksCube != null) rubiksCube.update(deltaTime);
        if (clock != null) clock.update(deltaTime);
        if (battery != null) battery.update(deltaTime);
    }

    @Override
    public void draw() {
        if (isDisposed) return;

        // Fondo oscuro
        GLES20.glClearColor(0.02f, 0.02f, 0.06f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Dibujar el cubo Rubik (3D)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        if (rubiksCube != null) {
            rubiksCube.draw(viewProjectionMatrix);
        }
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        // Dibujar highlight si hay zona activa
        if (highlightZone >= 0) {
            drawHighlight();
        }

        // Dibujar botones de control (2D UI)
        drawControlButtons();

        // Dibujar Clock y Battery
        if (clock != null) clock.draw();
        if (battery != null) battery.draw();
    }

    private void drawControlButtons() {
        if (uiShaderProgram == -1) return;

        GLES20.glUseProgram(uiShaderProgram);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        int posHandle = GLES20.glGetAttribLocation(uiShaderProgram, "a_Position");
        int colorHandle = GLES20.glGetUniformLocation(uiShaderProgram, "u_Color");

        for (int i = 0; i < BTN_COUNT; i++) {
            float[] bounds = buttonBounds[i];
            float[] color = BTN_COLORS[i].clone();

            // Highlight si está presionado
            if (i == pressedButton) {
                color[0] = Math.min(1f, color[0] + 0.3f);
                color[1] = Math.min(1f, color[1] + 0.3f);
                color[2] = Math.min(1f, color[2] + 0.3f);
            }

            drawRect(posHandle, colorHandle, bounds[0], bounds[1], bounds[2], bounds[3], color);
        }

        GLES20.glDisable(GLES20.GL_BLEND);
    }

    private void drawRect(int posHandle, int colorHandle, float x, float y, float w, float h, float[] color) {
        float[] vertices = {
            x, y,
            x + w, y,
            x, y + h,
            x + w, y + h
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer vb = bb.asFloatBuffer();
        vb.put(vertices);
        vb.position(0);

        GLES20.glUniform4fv(colorHandle, 1, color, 0);
        GLES20.glEnableVertexAttribArray(posHandle);
        GLES20.glVertexAttribPointer(posHandle, 2, GLES20.GL_FLOAT, false, 0, vb);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);  // ← Fixed: start from 0
        GLES20.glDisableVertexAttribArray(posHandle);
    }

    private void drawHighlight() {
        // Dibujar un rectángulo semi-transparente en la zona activa
        if (uiShaderProgram == -1) return;

        GLES20.glUseProgram(uiShaderProgram);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        int posHandle = GLES20.glGetAttribLocation(uiShaderProgram, "a_Position");
        int colorHandle = GLES20.glGetUniformLocation(uiShaderProgram, "u_Color");

        float[] color = {1f, 1f, 1f, 0.2f};  // Blanco semi-transparente
        float x = -0.4f, y = -0.4f, w = 0.8f, h = 0.8f;

        // Ajustar según la zona
        switch (highlightZone) {
            case 0: x = -0.9f; y = -0.3f; w = 0.5f; h = 0.6f; break;  // L
            case 1: x = 0.4f; y = -0.3f; w = 0.5f; h = 0.6f; break;   // R
            case 2: x = -0.3f; y = 0.1f; w = 0.6f; h = 0.4f; break;   // U
            case 3: x = -0.3f; y = -0.5f; w = 0.6f; h = 0.4f; break;  // D
            case 4: x = -0.25f; y = -0.25f; w = 0.5f; h = 0.5f; break; // F
        }

        drawRect(posHandle, colorHandle, x, y, w, h, color);
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    @Override
    public boolean onTouchEvent(float x, float y, int action) {
        float nx = x;
        float ny = y;

        switch (action) {
            case 0: // ACTION_DOWN
                touchStartTime = System.currentTimeMillis();
                touchStartX = nx;
                touchStartY = ny;
                lastTouchX = nx;
                lastTouchY = ny;
                isDragging = false;

                // Verificar si tocó un botón
                pressedButton = getButtonAt(nx, ny);
                if (pressedButton == -1) {
                    // No tocó botón, detectar zona del cubo
                    highlightZone = getZoneForPosition(nx, ny);
                }
                return true;

            case 2: // ACTION_MOVE
                if (pressedButton == -1) {
                    float deltaX = nx - lastTouchX;
                    float deltaY = ny - lastTouchY;

                    if (Math.abs(nx - touchStartX) > 0.05f || Math.abs(ny - touchStartY) > 0.05f) {
                        isDragging = true;
                        highlightZone = -1;  // Quitar highlight al arrastrar
                    }

                    if (isDragging && rubiksCube != null) {
                        rubiksCube.rotateCubeView(deltaX * 100f, deltaY * 100f);
                    }
                }
                lastTouchX = nx;
                lastTouchY = ny;
                return true;

            case 1: // ACTION_UP
                if (pressedButton >= 0) {
                    // Ejecutar acción del botón
                    executeButtonAction(pressedButton);
                    pressedButton = -1;
                } else if (!isDragging && highlightZone >= 0) {
                    // Tap en zona del cubo
                    executeCubeZoneAction(highlightZone);
                }
                highlightZone = -1;
                pressedButton = -1;
                return true;
        }

        return false;
    }

    private int getButtonAt(float x, float y) {
        for (int i = 0; i < BTN_COUNT; i++) {
            float[] b = buttonBounds[i];
            if (x >= b[0] && x <= b[0] + b[2] && y >= b[1] && y <= b[1] + b[3]) {
                return i;
            }
        }
        return -1;
    }

    private int getZoneForPosition(float x, float y) {
        // Solo detectar zona si está en el área central (donde está el cubo)
        if (Math.abs(x) > 0.6f || Math.abs(y) > 0.5f) return -1;

        if (x < -0.2f) return 0;      // L
        if (x > 0.2f) return 1;       // R
        if (y > 0.15f) return 2;      // U
        if (y < -0.15f) return 3;     // D
        return 4;                      // F (centro)
    }

    private void executeButtonAction(int button) {
        if (rubiksCube == null) return;

        // Los botones especiales funcionan incluso durante animación
        switch (button) {
            // Botones de caras - AHORA USAN applyMove() para guardar historial
            case BTN_L: rubiksCube.applyMove("L"); break;
            case BTN_R: rubiksCube.applyMove("R"); break;
            case BTN_U: rubiksCube.applyMove("U"); break;
            case BTN_D: rubiksCube.applyMove("D"); break;
            case BTN_F: rubiksCube.applyMove("F"); break;
            case BTN_B: rubiksCube.applyMove("B"); break;

            // Rotación de vista (siempre funciona)
            case BTN_ROT_LEFT:
                rubiksCube.rotateCubeView(-30f, 0f);
                break;
            case BTN_ROT_RIGHT:
                rubiksCube.rotateCubeView(30f, 0f);
                break;

            // ════════════════════════════════════════════════════════════════
            // BOTONES ESPECIALES
            // ════════════════════════════════════════════════════════════════
            case BTN_SHUFFLE:
                Log.d(TAG, "🔀 SHUFFLE presionado");
                rubiksCube.shuffle(20);  // 20 movimientos aleatorios
                break;

            case BTN_SOLVE:
                Log.d(TAG, "🤖 SOLVE presionado");
                rubiksCube.startSolve();  // Auto-resolver
                break;

            case BTN_RESET:
                Log.d(TAG, "↺ RESET presionado");
                rubiksCube.reset();  // Volver a estado resuelto
                break;
        }

        if (button < BTN_LABELS.length) {
            Log.d(TAG, "🎮 Botón: " + BTN_LABELS[button]);
        }
    }

    private void executeCubeZoneAction(int zone) {
        if (rubiksCube == null || rubiksCube.isAnimating()) return;

        switch (zone) {
            case 0: rubiksCube.rotateFace(RubiksCube.FACE_L, true); break;
            case 1: rubiksCube.rotateFace(RubiksCube.FACE_R, true); break;
            case 2: rubiksCube.rotateFace(RubiksCube.FACE_U, true); break;
            case 3: rubiksCube.rotateFace(RubiksCube.FACE_D, true); break;
            case 4: rubiksCube.rotateFace(RubiksCube.FACE_F, true); break;
        }
        Log.d(TAG, "🧊 Zona: " + zone);
    }

    @Override
    protected void releaseSceneResources() {
        Log.d(TAG, "🗑️ Liberando recursos Rubik");

        if (rubiksCube != null) {
            rubiksCube.dispose();
            rubiksCube = null;
        }

        if (clock != null) {
            clock.dispose();
            clock = null;
        }

        if (battery != null) {
            battery.dispose();
            battery = null;
        }

        if (uiShaderProgram != -1) {
            GLES20.glDeleteProgram(uiShaderProgram);
            uiShaderProgram = -1;
        }
    }
}
