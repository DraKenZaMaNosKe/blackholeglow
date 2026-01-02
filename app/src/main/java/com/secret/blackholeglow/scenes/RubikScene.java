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
 * ║  🧊 Controles 100% con botones - Sin touch confuso                       ║
 * ║  🎮 Cada cara: 2 flechas (↶ izquierda, ↷ derecha)                        ║
 * ║  🔄 Vista: botones para girar todo el cubo                               ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class RubikScene extends WallpaperScene {
    private static final String TAG = "RubikScene";

    private RubiksCube rubiksCube;
    private Clock3D clock;
    private Battery3D battery;

    private float[] projectionMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] viewProjectionMatrix = new float[16];

    // ═══════════════════════════════════════════════════════════════════════
    // 🎮 SISTEMA DE CONTROLES INTUITIVOS
    // ═══════════════════════════════════════════════════════════════════════

    // Estructura: Cada cara tiene 2 botones (CCW y CW)
    // Vista del cubo tiene 4 botones (arriba, abajo, izq, der)

    // CARAS - cada una con rotación izquierda (CCW) y derecha (CW)
    private static final int BTN_U_CCW = 0;   // Arriba - rotar izq
    private static final int BTN_U_CW = 1;    // Arriba - rotar der
    private static final int BTN_D_CCW = 2;   // Abajo - rotar izq
    private static final int BTN_D_CW = 3;    // Abajo - rotar der
    private static final int BTN_L_CCW = 4;   // Izquierda - rotar izq
    private static final int BTN_L_CW = 5;    // Izquierda - rotar der
    private static final int BTN_R_CCW = 6;   // Derecha - rotar izq
    private static final int BTN_R_CW = 7;    // Derecha - rotar der
    private static final int BTN_F_CCW = 8;   // Frente - rotar izq
    private static final int BTN_F_CW = 9;    // Frente - rotar der
    private static final int BTN_B_CCW = 10;  // Atrás - rotar izq
    private static final int BTN_B_CW = 11;   // Atrás - rotar der

    // VISTA - girar todo el cubo
    private static final int BTN_VIEW_LEFT = 12;
    private static final int BTN_VIEW_RIGHT = 13;
    private static final int BTN_VIEW_UP = 14;
    private static final int BTN_VIEW_DOWN = 15;

    // UTILIDAD
    private static final int BTN_SHUFFLE = 16;
    private static final int BTN_RESET = 17;

    private static final int BTN_COUNT = 18;

    private float[][] buttonBounds = new float[BTN_COUNT][4];

    // Colores según la cara del cubo (estándar Rubik)
    private static final float[] COLOR_WHITE = {1f, 1f, 1f, 0.9f};      // U - Blanco
    private static final float[] COLOR_YELLOW = {1f, 1f, 0f, 0.9f};     // D - Amarillo
    private static final float[] COLOR_GREEN = {0f, 0.8f, 0f, 0.9f};    // L - Verde
    private static final float[] COLOR_BLUE = {0f, 0.4f, 0.9f, 0.9f};   // R - Azul
    private static final float[] COLOR_RED = {0.9f, 0.1f, 0.1f, 0.9f};  // F - Rojo
    private static final float[] COLOR_ORANGE = {1f, 0.5f, 0f, 0.9f};   // B - Naranja
    private static final float[] COLOR_GRAY = {0.4f, 0.4f, 0.5f, 0.9f}; // Vista
    private static final float[] COLOR_CYAN = {0f, 0.8f, 0.9f, 0.9f};   // Shuffle
    private static final float[] COLOR_PINK = {0.9f, 0.3f, 0.5f, 0.9f}; // Reset

    private int pressedButton = -1;
    private int uiShaderProgram = -1;
    private FloatBuffer quadBuffer;

    @Override
    public String getName() { return "RUBIK"; }

    @Override
    public String getDescription() {
        return "Cubo Rubik interactivo";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_oceano_sc;
    }

    @Override
    protected void setupScene() {
        Log.d(TAG, "🧊 Configurando escena Rubik");

        rubiksCube = new RubiksCube();

        try {
            clock = new Clock3D(context, Clock3D.THEME_PYRALIS, 0f, 0.95f);
        } catch (Exception e) {
            Log.e(TAG, "Error Clock3D: " + e.getMessage());
        }

        try {
            battery = new Battery3D(context, Battery3D.THEME_PYRALIS, 0.75f, 0.95f);
        } catch (Exception e) {
            Log.e(TAG, "Error Battery3D: " + e.getMessage());
        }

        initUIShader();
        initButtons();
        setupMatrices();

        Log.d(TAG, "✅ Escena Rubik lista");
    }

    private void initUIShader() {
        String vertexShader =
            "attribute vec4 a_Position;" +
            "void main() { gl_Position = a_Position; }";

        String fragmentShader =
            "precision mediump float;" +
            "uniform vec4 u_Color;" +
            "void main() { gl_FragColor = u_Color; }";

        int vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        uiShaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(uiShaderProgram, vs);
        GLES20.glAttachShader(uiShaderProgram, fs);
        GLES20.glLinkProgram(uiShaderProgram);

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
        // LAYOUT INTUITIVO - Botones posicionados según ubicación de la cara
        // ════════════════════════════════════════════════════════════════════

        float arrowW = 0.10f;  // Ancho de flecha
        float arrowH = 0.08f;  // Alto de flecha
        float gap = 0.02f;     // Espacio entre flechas

        // ─────────────────────────────────────────────────────────────────────
        // ARRIBA (U) - Blanco - En la parte superior
        // ─────────────────────────────────────────────────────────────────────
        float uY = 0.42f;
        buttonBounds[BTN_U_CCW] = new float[]{-0.22f, uY, arrowW, arrowH};  // ↶
        buttonBounds[BTN_U_CW] = new float[]{0.12f, uY, arrowW, arrowH};    // ↷

        // ─────────────────────────────────────────────────────────────────────
        // IZQUIERDA (L) - Verde - Lado izquierdo
        // ─────────────────────────────────────────────────────────────────────
        float lX = -0.92f;
        buttonBounds[BTN_L_CCW] = new float[]{lX, 0.08f, arrowW, arrowH};   // ↶
        buttonBounds[BTN_L_CW] = new float[]{lX, -0.08f, arrowW, arrowH};   // ↷

        // ─────────────────────────────────────────────────────────────────────
        // DERECHA (R) - Azul - Lado derecho
        // ─────────────────────────────────────────────────────────────────────
        float rX = 0.82f;
        buttonBounds[BTN_R_CCW] = new float[]{rX, 0.08f, arrowW, arrowH};   // ↶
        buttonBounds[BTN_R_CW] = new float[]{rX, -0.08f, arrowW, arrowH};   // ↷

        // ─────────────────────────────────────────────────────────────────────
        // ABAJO (D) - Amarillo - Parte inferior del cubo
        // ─────────────────────────────────────────────────────────────────────
        float dY = -0.32f;
        buttonBounds[BTN_D_CCW] = new float[]{-0.22f, dY, arrowW, arrowH};  // ↶
        buttonBounds[BTN_D_CW] = new float[]{0.12f, dY, arrowW, arrowH};    // ↷

        // ─────────────────────────────────────────────────────────────────────
        // FRENTE (F) y ATRÁS (B) - Rojo y Naranja - Fila inferior
        // ─────────────────────────────────────────────────────────────────────
        float fbY = -0.48f;
        // Frente (F) - Rojo
        buttonBounds[BTN_F_CCW] = new float[]{-0.52f, fbY, arrowW * 0.8f, arrowH * 0.8f};
        buttonBounds[BTN_F_CW] = new float[]{-0.30f, fbY, arrowW * 0.8f, arrowH * 0.8f};
        // Atrás (B) - Naranja
        buttonBounds[BTN_B_CCW] = new float[]{0.22f, fbY, arrowW * 0.8f, arrowH * 0.8f};
        buttonBounds[BTN_B_CW] = new float[]{0.44f, fbY, arrowW * 0.8f, arrowH * 0.8f};

        // ─────────────────────────────────────────────────────────────────────
        // VISTA - Girar todo el cubo (esquinas)
        // ─────────────────────────────────────────────────────────────────────
        float viewSize = 0.09f;
        // Arriba del cubo
        buttonBounds[BTN_VIEW_UP] = new float[]{-viewSize/2, 0.58f, viewSize, viewSize};
        // Abajo del cubo
        buttonBounds[BTN_VIEW_DOWN] = new float[]{-viewSize/2, -0.65f, viewSize, viewSize};
        // Izquierda
        buttonBounds[BTN_VIEW_LEFT] = new float[]{-0.70f, -0.05f, viewSize, viewSize};
        // Derecha
        buttonBounds[BTN_VIEW_RIGHT] = new float[]{0.61f, -0.05f, viewSize, viewSize};

        // ─────────────────────────────────────────────────────────────────────
        // UTILIDAD - Shuffle y Reset (parte muy inferior)
        // ─────────────────────────────────────────────────────────────────────
        float utilY = -0.82f;
        float utilW = 0.28f;
        float utilH = 0.10f;
        buttonBounds[BTN_SHUFFLE] = new float[]{-0.60f, utilY, utilW, utilH};
        buttonBounds[BTN_RESET] = new float[]{0.32f, utilY, utilW, utilH};
    }

    private void setupMatrices() {
        float ratio = (float) screenWidth / Math.max(screenHeight, 1);
        Matrix.perspectiveM(projectionMatrix, 0, 45f, ratio, 0.1f, 100f);
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 14f, 0f, 0f, 0f, 0f, 1f, 0f);
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

        GLES20.glClearColor(0.02f, 0.02f, 0.06f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        if (rubiksCube != null) {
            rubiksCube.draw(viewProjectionMatrix);
        }
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        drawControlButtons();

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
            float[] color = getButtonColor(i);

            // Highlight si está presionado
            if (i == pressedButton) {
                color = new float[]{
                    Math.min(1f, color[0] + 0.3f),
                    Math.min(1f, color[1] + 0.3f),
                    Math.min(1f, color[2] + 0.3f),
                    1f
                };
            }

            drawButton(posHandle, colorHandle, bounds, color, i);
        }

        GLES20.glDisable(GLES20.GL_BLEND);
    }

    private float[] getButtonColor(int btn) {
        switch (btn) {
            case BTN_U_CCW: case BTN_U_CW: return COLOR_WHITE.clone();
            case BTN_D_CCW: case BTN_D_CW: return COLOR_YELLOW.clone();
            case BTN_L_CCW: case BTN_L_CW: return COLOR_GREEN.clone();
            case BTN_R_CCW: case BTN_R_CW: return COLOR_BLUE.clone();
            case BTN_F_CCW: case BTN_F_CW: return COLOR_RED.clone();
            case BTN_B_CCW: case BTN_B_CW: return COLOR_ORANGE.clone();
            case BTN_VIEW_LEFT: case BTN_VIEW_RIGHT:
            case BTN_VIEW_UP: case BTN_VIEW_DOWN: return COLOR_GRAY.clone();
            case BTN_SHUFFLE: return COLOR_CYAN.clone();
            case BTN_RESET: return COLOR_PINK.clone();
            default: return COLOR_GRAY.clone();
        }
    }

    private void drawButton(int posHandle, int colorHandle, float[] bounds, float[] color, int btnIndex) {
        float x = bounds[0], y = bounds[1], w = bounds[2], h = bounds[3];

        // Dibujar fondo del botón
        drawRect(posHandle, colorHandle, x, y, w, h, color);

        // Dibujar flecha indicadora (oscura sobre el botón)
        float[] arrowColor = {0f, 0f, 0f, 0.6f};
        float arrowMargin = 0.015f;
        float ax = x + arrowMargin;
        float ay = y + arrowMargin;
        float aw = w - arrowMargin * 2;
        float ah = h - arrowMargin * 2;

        // Dibujar símbolo según el tipo de botón
        if (btnIndex == BTN_U_CCW || btnIndex == BTN_D_CCW ||
            btnIndex == BTN_L_CCW || btnIndex == BTN_R_CCW ||
            btnIndex == BTN_F_CCW || btnIndex == BTN_B_CCW) {
            // Flecha izquierda (CCW) ↶
            drawArrowCCW(posHandle, colorHandle, ax, ay, aw, ah, arrowColor);
        } else if (btnIndex == BTN_U_CW || btnIndex == BTN_D_CW ||
                   btnIndex == BTN_L_CW || btnIndex == BTN_R_CW ||
                   btnIndex == BTN_F_CW || btnIndex == BTN_B_CW) {
            // Flecha derecha (CW) ↷
            drawArrowCW(posHandle, colorHandle, ax, ay, aw, ah, arrowColor);
        } else if (btnIndex == BTN_VIEW_LEFT) {
            drawTriangleLeft(posHandle, colorHandle, ax, ay, aw, ah, arrowColor);
        } else if (btnIndex == BTN_VIEW_RIGHT) {
            drawTriangleRight(posHandle, colorHandle, ax, ay, aw, ah, arrowColor);
        } else if (btnIndex == BTN_VIEW_UP) {
            drawTriangleUp(posHandle, colorHandle, ax, ay, aw, ah, arrowColor);
        } else if (btnIndex == BTN_VIEW_DOWN) {
            drawTriangleDown(posHandle, colorHandle, ax, ay, aw, ah, arrowColor);
        }
        // SHUFFLE y RESET solo tienen el color de fondo
    }

    // Flecha curva counter-clockwise (↶)
    private void drawArrowCCW(int posHandle, int colorHandle, float x, float y, float w, float h, float[] color) {
        // Línea curva simplificada - arco izquierdo
        float cx = x + w * 0.5f;
        float cy = y + h * 0.5f;
        float r = Math.min(w, h) * 0.3f;

        // Dibujar un pequeño triángulo indicando dirección
        float[] verts = {
            cx - r, cy + r * 0.5f,
            cx - r * 0.3f, cy + r,
            cx - r * 0.3f, cy
        };
        drawTriangle(posHandle, colorHandle, verts, color);

        // Línea horizontal
        drawRect(posHandle, colorHandle, cx - r, cy - r * 0.15f, r * 1.5f, r * 0.3f, color);
    }

    // Flecha curva clockwise (↷)
    private void drawArrowCW(int posHandle, int colorHandle, float x, float y, float w, float h, float[] color) {
        float cx = x + w * 0.5f;
        float cy = y + h * 0.5f;
        float r = Math.min(w, h) * 0.3f;

        // Triángulo apuntando a la derecha
        float[] verts = {
            cx + r, cy + r * 0.5f,
            cx + r * 0.3f, cy + r,
            cx + r * 0.3f, cy
        };
        drawTriangle(posHandle, colorHandle, verts, color);

        // Línea horizontal
        drawRect(posHandle, colorHandle, cx - r * 0.5f, cy - r * 0.15f, r * 1.5f, r * 0.3f, color);
    }

    private void drawTriangleLeft(int posHandle, int colorHandle, float x, float y, float w, float h, float[] color) {
        float[] verts = {
            x + w * 0.2f, y + h * 0.5f,
            x + w * 0.8f, y + h * 0.8f,
            x + w * 0.8f, y + h * 0.2f
        };
        drawTriangle(posHandle, colorHandle, verts, color);
    }

    private void drawTriangleRight(int posHandle, int colorHandle, float x, float y, float w, float h, float[] color) {
        float[] verts = {
            x + w * 0.8f, y + h * 0.5f,
            x + w * 0.2f, y + h * 0.8f,
            x + w * 0.2f, y + h * 0.2f
        };
        drawTriangle(posHandle, colorHandle, verts, color);
    }

    private void drawTriangleUp(int posHandle, int colorHandle, float x, float y, float w, float h, float[] color) {
        float[] verts = {
            x + w * 0.5f, y + h * 0.8f,
            x + w * 0.2f, y + h * 0.2f,
            x + w * 0.8f, y + h * 0.2f
        };
        drawTriangle(posHandle, colorHandle, verts, color);
    }

    private void drawTriangleDown(int posHandle, int colorHandle, float x, float y, float w, float h, float[] color) {
        float[] verts = {
            x + w * 0.5f, y + h * 0.2f,
            x + w * 0.2f, y + h * 0.8f,
            x + w * 0.8f, y + h * 0.8f
        };
        drawTriangle(posHandle, colorHandle, verts, color);
    }

    private void drawTriangle(int posHandle, int colorHandle, float[] verts, float[] color) {
        ByteBuffer bb = ByteBuffer.allocateDirect(verts.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer vb = bb.asFloatBuffer();
        vb.put(verts);
        vb.position(0);

        GLES20.glUniform4fv(colorHandle, 1, color, 0);
        GLES20.glEnableVertexAttribArray(posHandle);
        GLES20.glVertexAttribPointer(posHandle, 2, GLES20.GL_FLOAT, false, 0, vb);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
        GLES20.glDisableVertexAttribArray(posHandle);
    }

    private void drawRect(int posHandle, int colorHandle, float x, float y, float w, float h, float[] color) {
        float[] vertices = {x, y, x + w, y, x, y + h, x + w, y + h};

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer vb = bb.asFloatBuffer();
        vb.put(vertices);
        vb.position(0);

        GLES20.glUniform4fv(colorHandle, 1, color, 0);
        GLES20.glEnableVertexAttribArray(posHandle);
        GLES20.glVertexAttribPointer(posHandle, 2, GLES20.GL_FLOAT, false, 0, vb);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(posHandle);
    }

    @Override
    public boolean onTouchEvent(float x, float y, int action) {
        switch (action) {
            case 0: // ACTION_DOWN
                pressedButton = getButtonAt(x, y);
                return pressedButton >= 0;

            case 1: // ACTION_UP
                if (pressedButton >= 0) {
                    executeButtonAction(pressedButton);
                    pressedButton = -1;
                }
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

    private void executeButtonAction(int button) {
        if (rubiksCube == null) return;

        switch (button) {
            // ═══════════════════════════════════════════════════════════════
            // CARAS - Rotaciones
            // ═══════════════════════════════════════════════════════════════
            case BTN_U_CCW: rubiksCube.applyMove("U'"); break;
            case BTN_U_CW:  rubiksCube.applyMove("U"); break;
            case BTN_D_CCW: rubiksCube.applyMove("D'"); break;
            case BTN_D_CW:  rubiksCube.applyMove("D"); break;
            case BTN_L_CCW: rubiksCube.applyMove("L'"); break;
            case BTN_L_CW:  rubiksCube.applyMove("L"); break;
            case BTN_R_CCW: rubiksCube.applyMove("R'"); break;
            case BTN_R_CW:  rubiksCube.applyMove("R"); break;
            case BTN_F_CCW: rubiksCube.applyMove("F'"); break;
            case BTN_F_CW:  rubiksCube.applyMove("F"); break;
            case BTN_B_CCW: rubiksCube.applyMove("B'"); break;
            case BTN_B_CW:  rubiksCube.applyMove("B"); break;

            // ═══════════════════════════════════════════════════════════════
            // VISTA - Girar todo el cubo
            // ═══════════════════════════════════════════════════════════════
            case BTN_VIEW_LEFT:  rubiksCube.rotateCubeView(-25f, 0f); break;
            case BTN_VIEW_RIGHT: rubiksCube.rotateCubeView(25f, 0f); break;
            case BTN_VIEW_UP:    rubiksCube.rotateCubeView(0f, -25f); break;
            case BTN_VIEW_DOWN:  rubiksCube.rotateCubeView(0f, 25f); break;

            // ═══════════════════════════════════════════════════════════════
            // UTILIDAD
            // ═══════════════════════════════════════════════════════════════
            case BTN_SHUFFLE:
                Log.d(TAG, "🔀 Mezclando cubo");
                rubiksCube.shuffle(20);
                break;
            case BTN_RESET:
                Log.d(TAG, "↺ Reseteando cubo");
                rubiksCube.reset();
                break;
        }
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
