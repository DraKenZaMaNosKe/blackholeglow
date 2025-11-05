package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * HPBar - Barra de vida para objetos (Sol, Campo de Fuerza, etc.)
 * Renderiza en espacio de pantalla (2D overlay)
 */
public class HPBar implements SceneObject {
    private static final String TAG = "depurar";

    private int programId;
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private FloatBuffer uvBuffer;

    // Ubicación y tamaño
    private float x, y;           // Posición en pantalla (0-1)
    private final float width;    // Ancho de la barra
    private final float height;   // Alto de la barra

    // Estado de vida
    private int maxHealth;
    private int currentHealth;

    // Colores
    private final float[] colorFull;      // Color cuando está llena
    private final float[] colorEmpty;     // Color cuando está vacía
    private final float[] colorBorder;    // Color del borde

    // Parpadeo cuando HP está bajo
    private float blinkTime = 0f;
    private boolean blinkVisible = true;
    private static final float BLINK_SPEED = 3.0f;  // Parpadeos por segundo
    private final boolean enableBlinking;  // Habilitar/deshabilitar parpadeo

    // Label de texto
    private final String label;

    public HPBar(Context context, String label, float x, float y, float width, float height,
                 int maxHealth, float[] colorFull, float[] colorEmpty) {
        this(context, label, x, y, width, height, maxHealth, colorFull, colorEmpty, true);  // Blinking enabled by default
    }

    public HPBar(Context context, String label, float x, float y, float width, float height,
                 int maxHealth, float[] colorFull, float[] colorEmpty, boolean enableBlinking) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
        this.colorFull = colorFull;
        this.colorEmpty = colorEmpty;
        this.colorBorder = new float[]{1.0f, 1.0f, 1.0f, 1.0f};  // Blanco
        this.enableBlinking = enableBlinking;

        initShader(context);
        setupGeometry();

        Log.d(TAG, "[HPBar] ✓ Barra HP creada: " + label + " - HP: " + maxHealth + " - Blinking: " + enableBlinking);
    }

    private void initShader(Context context) {
        // Shader con esquinas redondeadas para barras HP
        String vertexShader =
            "attribute vec2 a_Position;\n" +
            "attribute vec4 a_Color;\n" +
            "attribute vec2 a_TexCoord;\n" +  // UV coordinates
            "varying vec4 v_Color;\n" +
            "varying vec2 v_TexCoord;\n" +
            "void main() {\n" +
            "    v_Color = a_Color;\n" +
            "    v_TexCoord = a_TexCoord;\n" +
            "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
            "}\n";

        String fragmentShader =
            "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec4 v_Color;\n" +
            "varying vec2 v_TexCoord;\n" +
            "uniform float u_CornerRadius;\n" +
            "\n" +
            "void main() {\n" +
            "    // Calcular distancia a las esquinas para bordes redondeados\n" +
            "    vec2 uv = v_TexCoord;\n" +
            "    vec2 d = abs(uv - 0.5) - 0.5 + u_CornerRadius;\n" +
            "    float dist = length(max(d, 0.0)) - u_CornerRadius;\n" +
            "    \n" +
            "    // Crear borde suave (anti-aliasing)\n" +
            "    float alpha = 1.0 - smoothstep(-0.01, 0.01, dist);\n" +
            "    \n" +
            "    gl_FragColor = vec4(v_Color.rgb, v_Color.a * alpha);\n" +
            "}\n";

        Log.d(TAG, "[HPBar] Compilando vertex shader...");
        int vShader = ShaderUtils.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        if (vShader == 0) {
            Log.e(TAG, "[HPBar] ✗ Vertex shader falló al compilar");
            return;
        }

        Log.d(TAG, "[HPBar] Compilando fragment shader...");
        int fShader = ShaderUtils.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);
        if (fShader == 0) {
            Log.e(TAG, "[HPBar] ✗ Fragment shader falló al compilar");
            GLES20.glDeleteShader(vShader);
            return;
        }

        Log.d(TAG, "[HPBar] Enlazando programa shader...");
        programId = GLES20.glCreateProgram();
        GLES20.glAttachShader(programId, vShader);
        GLES20.glAttachShader(programId, fShader);
        GLES20.glLinkProgram(programId);

        // Verificar link
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            String errorLog = GLES20.glGetProgramInfoLog(programId);
            Log.e(TAG, "[HPBar] ✗✗✗ Shader link failed!");
            Log.e(TAG, "[HPBar] Error details: " + errorLog);
            GLES20.glDeleteProgram(programId);
            programId = 0;
        } else {
            Log.d(TAG, "[HPBar] ✓ Shader enlazado exitosamente - programId: " + programId);
        }

        GLES20.glDeleteShader(vShader);
        GLES20.glDeleteShader(fShader);
    }

    private void setupGeometry() {
        // Dos rectángulos: borde y relleno
        // Por ahora solo vértices (se actualizan en draw según HP)
        float[] vertices = new float[24];  // 12 vértices (6 por rectángulo, 2 componentes)
        vertexBuffer = createFloatBuffer(vertices);

        float[] colors = new float[48];  // 12 vértices, 4 componentes RGBA
        colorBuffer = createFloatBuffer(colors);

        // UV coordinates para esquinas redondeadas (0-1 para cada quad)
        float[] uvs = {
            0.0f, 0.0f,  // Bottom-left
            1.0f, 0.0f,  // Bottom-right
            0.0f, 1.0f,  // Top-left
            1.0f, 0.0f,  // Bottom-right
            1.0f, 1.0f,  // Top-right
            0.0f, 1.0f   // Top-left
        };
        uvBuffer = createFloatBuffer(uvs);
    }

    private FloatBuffer createFloatBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data);
        fb.position(0);
        return fb;
    }

    public void setHealth(int health) {
        this.currentHealth = Math.max(0, Math.min(health, maxHealth));
    }

    public void damage(int amount) {
        this.currentHealth = Math.max(0, currentHealth - amount);
        Log.d(TAG, "[HPBar] " + label + " dañado: " + currentHealth + "/" + maxHealth);
    }

    public void heal(int amount) {
        this.currentHealth = Math.min(maxHealth, currentHealth + amount);
    }

    public void reset() {
        this.currentHealth = maxHealth;
        Log.d(TAG, "[HPBar] " + label + " HP restaurado: " + maxHealth);
    }

    public boolean isDead() {
        return currentHealth <= 0;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    @Override
    public void update(float deltaTime) {
        // ⚠️ Parpadeo cuando HP está bajo (menos de 20% o HP = 0)
        // Solo si está habilitado el blinking
        if (enableBlinking) {
            float hpPercent = (float) currentHealth / maxHealth;
            if (hpPercent <= 0.2f) {  // HP bajo o destruido
                blinkTime += deltaTime * BLINK_SPEED;
                if (blinkTime >= 1.0f) {
                    blinkTime = 0f;
                    blinkVisible = !blinkVisible;
                }
            } else {
                // HP normal - siempre visible
                blinkVisible = true;
                blinkTime = 0f;
            }
        } else {
            // Blinking deshabilitado - siempre visible
            blinkVisible = true;
            blinkTime = 0f;
        }
    }

    @Override
    public void draw() {
        if (!GLES20.glIsProgram(programId)) return;

        // ⚠️ Si está parpadeando y no es visible, no dibujar
        if (!blinkVisible) return;

        GLES20.glUseProgram(programId);

        // Desactivar depth test (UI en 2D)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Convertir de coordenadas 0-1 a NDC (-1 a 1)
        float ndcX = x * 2.0f - 1.0f;
        float ndcY = y * 2.0f - 1.0f;
        float ndcW = width * 2.0f;
        float ndcH = height * 2.0f;

        // Calcular porcentaje de vida
        float healthPercent = (float) currentHealth / maxHealth;

        // Interpolar color según vida
        float[] currentColor = new float[4];
        for (int i = 0; i < 4; i++) {
            currentColor[i] = colorEmpty[i] + (colorFull[i] - colorEmpty[i]) * healthPercent;
        }

        // RECTÁNGULO DE FONDO (borde/vacío)
        float[] bgVertices = {
            ndcX, ndcY,                    // Bottom-left
            ndcX + ndcW, ndcY,            // Bottom-right
            ndcX, ndcY + ndcH,            // Top-left

            ndcX + ndcW, ndcY,            // Bottom-right
            ndcX + ndcW, ndcY + ndcH,     // Top-right
            ndcX, ndcY + ndcH             // Top-left
        };

        float[] bgColors = new float[24];  // 6 vértices * 4 componentes
        for (int i = 0; i < 6; i++) {
            bgColors[i * 4] = colorBorder[0] * 0.3f;
            bgColors[i * 4 + 1] = colorBorder[1] * 0.3f;
            bgColors[i * 4 + 2] = colorBorder[2] * 0.3f;
            bgColors[i * 4 + 3] = 0.8f;
        }

        // Dibujar fondo
        vertexBuffer.clear();
        vertexBuffer.put(bgVertices);
        vertexBuffer.position(0);

        colorBuffer.clear();
        colorBuffer.put(bgColors);
        colorBuffer.position(0);

        drawQuad();

        // RECTÁNGULO DE VIDA (relleno según HP)
        float fillWidth = ndcW * healthPercent;
        float padding = 0.005f;  // Pequeño padding interno

        float[] fillVertices = {
            ndcX + padding, ndcY + padding,
            ndcX + fillWidth - padding, ndcY + padding,
            ndcX + padding, ndcY + ndcH - padding,

            ndcX + fillWidth - padding, ndcY + padding,
            ndcX + fillWidth - padding, ndcY + ndcH - padding,
            ndcX + padding, ndcY + ndcH - padding
        };

        float[] fillColors = new float[24];
        for (int i = 0; i < 6; i++) {
            fillColors[i * 4] = currentColor[0];
            fillColors[i * 4 + 1] = currentColor[1];
            fillColors[i * 4 + 2] = currentColor[2];
            fillColors[i * 4 + 3] = currentColor[3];
        }

        // Dibujar relleno
        vertexBuffer.clear();
        vertexBuffer.put(fillVertices);
        vertexBuffer.position(0);

        colorBuffer.clear();
        colorBuffer.put(fillColors);
        colorBuffer.position(0);

        drawQuad();

        // Restaurar estado
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    private void drawQuad() {
        int aPosLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        int aColorLoc = GLES20.glGetAttribLocation(programId, "a_Color");
        int aTexCoordLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        int uCornerRadiusLoc = GLES20.glGetUniformLocation(programId, "u_CornerRadius");

        // Radio de las esquinas (0.15 = esquinas suavemente redondeadas)
        GLES20.glUniform1f(uCornerRadiusLoc, 0.15f);

        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aColorLoc);
        GLES20.glVertexAttribPointer(aColorLoc, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);

        GLES20.glEnableVertexAttribArray(aTexCoordLoc);
        uvBuffer.position(0);
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

        GLES20.glDisableVertexAttribArray(aPosLoc);
        GLES20.glDisableVertexAttribArray(aColorLoc);
        GLES20.glDisableVertexAttribArray(aTexCoordLoc);
    }
}
