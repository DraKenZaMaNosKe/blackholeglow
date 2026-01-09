package com.secret.blackholeglow.sharing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import com.secret.blackholeglow.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                    🎵 LIKE BUTTON - Song Sharing                         ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Botón de like temático para compartir canciones.                        ║
 * ║                                                                          ║
 * ║  TEMAS DISPONIBLES:                                                      ║
 * ║  • DEFAULT       - Corazón rosa procedural                               ║
 * ║  • ABYSSIA       - Huevo Zerg (textura) cyan                            ║
 * ║  • PYRALIS       - Orbe de fuego (textura) naranja                      ║
 * ║  • ADVENTURE_TIME - Gema de Ooo (procedural) verde esmeralda            ║
 * ║  • GOKU          - Esfera del Dragón (procedural) naranja con estrellas ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class LikeButton {
    private static final String TAG = "LikeButton";

    public enum Theme { DEFAULT, ABYSSIA, PYRALIS, ADVENTURE_TIME, GOKU, SYNTHWAVE, COSMOS }
    private Theme currentTheme = Theme.DEFAULT;

    private float x = 0.85f;
    private float y = -0.75f;
    private float size = 0.08f;

    private boolean isPressed = false;
    private boolean isOnCooldown = false;
    private float pulsePhase = 0f;
    private float floatOffset = 0f;

    private int programIdTexture;
    private int programIdColor;
    private FloatBuffer quadBuffer;
    private FloatBuffer heartBuffer;
    private FloatBuffer gemBuffer;      // 💎 Gema de Ooo
    private FloatBuffer sphereBuffer;   // 🟠 Esfera del Dragón
    private boolean isInitialized = false;

    // Texturas solo para ABYSSIA y PYRALIS
    private int textureAbyssia = -1;
    private int textureFireOrb = -1;
    private Context context;

    // Colores para corazón DEFAULT
    private float[] colorNormal = {1.0f, 0.0f, 0.5f, 0.85f};
    private float[] colorPressed = {0.0f, 0.85f, 1.0f, 1.0f};
    private float[] colorCooldown = {0.3f, 0.3f, 0.35f, 0.6f};
    private float[] glowCyan = {0.0f, 0.85f, 1.0f};
    private float[] glowPink = {1.0f, 0.0f, 0.5f};

    // Colores para Gema de Ooo (verde esmeralda)
    private float[] gemColorMain = {0.1f, 0.9f, 0.4f, 0.95f};
    private float[] gemColorHighlight = {0.5f, 1.0f, 0.7f, 1.0f};
    private float[] gemGlow = {0.0f, 1.0f, 0.5f};

    // Colores para Esfera del Dragón (naranja dorado)
    private float[] dragonBallMain = {1.0f, 0.6f, 0.1f, 0.95f};
    private float[] dragonBallHighlight = {1.0f, 0.9f, 0.5f, 1.0f};
    private float[] dragonBallGlow = {1.0f, 0.5f, 0.0f};
    private float[] starColor = {0.8f, 0.2f, 0.1f, 1.0f};  // Estrellas rojas

    // 🌅 Colores para Sol Synthwave (Hot Pink → Yellow gradient)
    private float[] synthwaveSunTop = {1.0f, 1.0f, 0.2f, 0.95f};      // Amarillo brillante
    private float[] synthwaveSunBottom = {1.0f, 0.08f, 0.58f, 0.95f}; // Hot Pink
    private float[] synthwaveGlow = {1.0f, 0.0f, 1.0f};               // Magenta glow
    private float[] synthwaveLine = {0.2f, 0.0f, 0.3f, 0.8f};         // Líneas púrpura oscuro

    private final float[] modelMatrix = new float[16];
    private final float[] finalMatrix = new float[16];
    private final float[] colorCache = new float[4];

    private static final String TEXTURE_VERTEX_SHADER =
            "attribute vec4 a_Position;\n" +
            "attribute vec2 a_TexCoord;\n" +
            "varying vec2 v_TexCoord;\n" +
            "uniform mat4 u_MVPMatrix;\n" +
            "void main() {\n" +
            "    gl_Position = u_MVPMatrix * a_Position;\n" +
            "    v_TexCoord = a_TexCoord;\n" +
            "}";

    private static final String TEXTURE_FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "varying vec2 v_TexCoord;\n" +
            "uniform sampler2D u_Texture;\n" +
            "uniform float u_Alpha;\n" +
            "uniform float u_Pulse;\n" +
            "void main() {\n" +
            "    vec4 texColor = texture2D(u_Texture, v_TexCoord);\n" +
            "    texColor.rgb *= (0.9 + u_Pulse * 0.2);\n" +
            "    texColor.a *= u_Alpha;\n" +
            "    gl_FragColor = texColor;\n" +
            "}";

    private static final String COLOR_VERTEX_SHADER =
            "attribute vec4 a_Position;\n" +
            "uniform mat4 u_MVPMatrix;\n" +
            "void main() {\n" +
            "    gl_Position = u_MVPMatrix * a_Position;\n" +
            "}";

    private static final String COLOR_FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform vec4 u_Color;\n" +
            "void main() {\n" +
            "    gl_FragColor = u_Color;\n" +
            "}";

    private int positionHandleTex, texCoordHandle, mvpMatrixHandleTex, textureHandle, alphaHandle, pulseHandle;
    private int positionHandleColor, mvpMatrixHandleColor, colorHandle;

    public LikeButton(Context context) {
        this.context = context;
    }

    public void init() {
        if (isInitialized) return;

        // Shader de texturas
        int vertexShaderTex = loadShader(GLES30.GL_VERTEX_SHADER, TEXTURE_VERTEX_SHADER);
        int fragmentShaderTex = loadShader(GLES30.GL_FRAGMENT_SHADER, TEXTURE_FRAGMENT_SHADER);
        programIdTexture = GLES30.glCreateProgram();
        GLES30.glAttachShader(programIdTexture, vertexShaderTex);
        GLES30.glAttachShader(programIdTexture, fragmentShaderTex);
        GLES30.glLinkProgram(programIdTexture);
        positionHandleTex = GLES30.glGetAttribLocation(programIdTexture, "a_Position");
        texCoordHandle = GLES30.glGetAttribLocation(programIdTexture, "a_TexCoord");
        mvpMatrixHandleTex = GLES30.glGetUniformLocation(programIdTexture, "u_MVPMatrix");
        textureHandle = GLES30.glGetUniformLocation(programIdTexture, "u_Texture");
        alphaHandle = GLES30.glGetUniformLocation(programIdTexture, "u_Alpha");
        pulseHandle = GLES30.glGetUniformLocation(programIdTexture, "u_Pulse");

        // Shader de color sólido
        int vertexShaderCol = loadShader(GLES30.GL_VERTEX_SHADER, COLOR_VERTEX_SHADER);
        int fragmentShaderCol = loadShader(GLES30.GL_FRAGMENT_SHADER, COLOR_FRAGMENT_SHADER);
        programIdColor = GLES30.glCreateProgram();
        GLES30.glAttachShader(programIdColor, vertexShaderCol);
        GLES30.glAttachShader(programIdColor, fragmentShaderCol);
        GLES30.glLinkProgram(programIdColor);
        positionHandleColor = GLES30.glGetAttribLocation(programIdColor, "a_Position");
        mvpMatrixHandleColor = GLES30.glGetUniformLocation(programIdColor, "u_MVPMatrix");
        colorHandle = GLES30.glGetUniformLocation(programIdColor, "u_Color");

        // Crear geometrías
        createQuadGeometry();
        createHeartGeometry();
        createGemGeometry();      // 💎 Gema de Ooo
        createSphereGeometry();   // 🟠 Esfera del Dragón

        // Cargar texturas (solo para ABYSSIA y PYRALIS)
        loadTextures();

        isInitialized = true;
        Log.d(TAG, "LikeButton init (con Gema y Dragon Ball procedurales)");
    }

    private void createQuadGeometry() {
        float[] quadVertices = { -1.0f, -1.0f, 0.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f };
        ByteBuffer bb = ByteBuffer.allocateDirect(quadVertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        quadBuffer = bb.asFloatBuffer();
        quadBuffer.put(quadVertices);
        quadBuffer.position(0);
    }

    private void createHeartGeometry() {
        int segments = 64;
        float[] vertices = new float[(segments + 2) * 2];
        vertices[0] = 0f; vertices[1] = -0.1f;
        for (int i = 0; i <= segments; i++) {
            float t = (float) (2.0 * Math.PI * i / segments);
            float hx = (float) (16.0 * Math.pow(Math.sin(t), 3));
            float hy = (float) (13.0 * Math.cos(t) - 5.0 * Math.cos(2*t) - 2.0 * Math.cos(3*t) - Math.cos(4*t));
            vertices[(i + 1) * 2] = hx / 17.0f;
            vertices[(i + 1) * 2 + 1] = hy / 17.0f;
        }
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        heartBuffer = bb.asFloatBuffer();
        heartBuffer.put(vertices);
        heartBuffer.position(0);
    }

    /**
     * 💎 Crea la geometría de la Gema de Ooo (hexágono con punta)
     * Forma de diamante/cristal típico de Adventure Time
     */
    private void createGemGeometry() {
        // Diamante de 6 puntas (forma de gema)
        float[] vertices = {
            // Centro
            0.0f, 0.0f,
            // Punta superior
            0.0f, 1.0f,
            // Esquina superior derecha
            0.6f, 0.5f,
            // Esquina inferior derecha
            0.6f, -0.3f,
            // Punta inferior
            0.0f, -0.8f,
            // Esquina inferior izquierda
            -0.6f, -0.3f,
            // Esquina superior izquierda
            -0.6f, 0.5f,
            // Cerrar (volver a punta superior)
            0.0f, 1.0f
        };
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        gemBuffer = bb.asFloatBuffer();
        gemBuffer.put(vertices);
        gemBuffer.position(0);
    }

    /**
     * 🟠 Crea la geometría de la Esfera del Dragón (círculo)
     */
    private void createSphereGeometry() {
        int segments = 48;
        float[] vertices = new float[(segments + 2) * 2];
        vertices[0] = 0f; vertices[1] = 0f;  // Centro
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (2.0 * Math.PI * i / segments);
            vertices[(i + 1) * 2] = (float) Math.cos(angle);
            vertices[(i + 1) * 2 + 1] = (float) Math.sin(angle);
        }
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        sphereBuffer = bb.asFloatBuffer();
        sphereBuffer.put(vertices);
        sphereBuffer.position(0);
    }

    private void loadTextures() {
        textureAbyssia = loadTexture(R.drawable.huevo_zerg);
        textureFireOrb = loadTexture(R.drawable.fire_orb);
        Log.d(TAG, "Textures loaded (ABYSSIA & PYRALIS)");
    }

    private int loadTexture(int resourceId) {
        final int[] texHandle = new int[1];
        GLES30.glGenTextures(1, texHandle, 0);
        if (texHandle[0] != 0) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texHandle[0]);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();
        }
        return texHandle[0];
    }

    public void setTheme(Theme theme) {
        this.currentTheme = theme;
        Log.d(TAG, "Theme: " + theme);
    }

    public void draw(float[] mvpMatrix, float time) {
        if (!isInitialized) return;
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        pulsePhase = time;
        float pulse = (float) Math.sin(pulsePhase * 2.5) * 0.5f + 0.5f;
        floatOffset = (float) Math.sin(pulsePhase * 1.5) * 0.01f;

        switch (currentTheme) {
            case DEFAULT:
                drawHeartProcedural(mvpMatrix, pulse);
                break;
            case ABYSSIA:
            case PYRALIS:
                drawTexturedButton(mvpMatrix, pulse);
                break;
            case ADVENTURE_TIME:
                drawGemProcedural(mvpMatrix, pulse);
                break;
            case GOKU:
                drawDragonBallProcedural(mvpMatrix, pulse);
                break;
            case SYNTHWAVE:
                drawSynthwaveSunProcedural(mvpMatrix, pulse);
                break;
            case COSMOS:
                drawCosmosStarProcedural(mvpMatrix, pulse);
                break;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 💎 GEMA DE OOO - Adventure Time (Procedural)
    // ═══════════════════════════════════════════════════════════════════════════

    private void drawGemProcedural(float[] mvpMatrix, float pulse) {
        GLES30.glUseProgram(programIdColor);
        float scale = size * (1.0f + pulse * 0.15f);
        float currentY = y + floatOffset;

        // Glow verde esmeralda
        if (!isOnCooldown) {
            drawGemGlow(mvpMatrix, scale * 1.6f, 0.15f, pulse);
            drawGemGlow(mvpMatrix, scale * 1.3f, 0.25f, pulse);
        }

        // Gema principal
        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, x, currentY, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, scale, scale, 1);
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
        GLES30.glUniformMatrix4fv(mvpMatrixHandleColor, 1, false, finalMatrix, 0);

        // Color de la gema (varía con pulse para efecto brillante)
        float brightness = 0.8f + pulse * 0.2f;
        colorCache[0] = gemColorMain[0] * brightness;
        colorCache[1] = gemColorMain[1] * brightness;
        colorCache[2] = gemColorMain[2] * brightness;
        colorCache[3] = isOnCooldown ? 0.4f : (isPressed ? 1.0f : 0.95f);
        GLES30.glUniform4fv(colorHandle, 1, colorCache, 0);

        gemBuffer.position(0);
        GLES30.glEnableVertexAttribArray(positionHandleColor);
        GLES30.glVertexAttribPointer(positionHandleColor, 2, GLES30.GL_FLOAT, false, 0, gemBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 8);

        // Borde brillante
        float borderPulse = (float) (Math.sin(pulsePhase * 3.5) * 0.3 + 0.7);
        colorCache[0] = gemColorHighlight[0] * borderPulse;
        colorCache[1] = gemColorHighlight[1] * borderPulse;
        colorCache[2] = gemColorHighlight[2] * borderPulse;
        colorCache[3] = 1.0f;
        GLES30.glUniform4fv(colorHandle, 1, colorCache, 0);
        GLES30.glLineWidth(2.5f);
        GLES30.glDrawArrays(GLES30.GL_LINE_LOOP, 1, 7);

        // Destello interior (línea vertical brillante)
        drawGemHighlight(mvpMatrix, scale * 0.6f, pulse);

        GLES30.glDisableVertexAttribArray(positionHandleColor);
    }

    private void drawGemGlow(float[] mvpMatrix, float glowSize, float alpha, float pulse) {
        float currentY = y + floatOffset;
        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, x, currentY, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, glowSize, glowSize, 1);
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
        GLES30.glUniformMatrix4fv(mvpMatrixHandleColor, 1, false, finalMatrix, 0);

        colorCache[0] = gemGlow[0];
        colorCache[1] = gemGlow[1];
        colorCache[2] = gemGlow[2];
        colorCache[3] = alpha * pulse;
        GLES30.glUniform4fv(colorHandle, 1, colorCache, 0);

        gemBuffer.position(0);
        GLES30.glEnableVertexAttribArray(positionHandleColor);
        GLES30.glVertexAttribPointer(positionHandleColor, 2, GLES30.GL_FLOAT, false, 0, gemBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 8);
        GLES30.glDisableVertexAttribArray(positionHandleColor);
    }

    private void drawGemHighlight(float[] mvpMatrix, float highlightSize, float pulse) {
        // Línea de brillo vertical dentro de la gema
        float[] highlight = { 0.0f, 0.6f, 0.0f, -0.4f };
        ByteBuffer bb = ByteBuffer.allocateDirect(highlight.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer highlightBuffer = bb.asFloatBuffer();
        highlightBuffer.put(highlight);
        highlightBuffer.position(0);

        float currentY = y + floatOffset;
        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, x, currentY, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, highlightSize, highlightSize, 1);
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
        GLES30.glUniformMatrix4fv(mvpMatrixHandleColor, 1, false, finalMatrix, 0);

        colorCache[0] = 1.0f;
        colorCache[1] = 1.0f;
        colorCache[2] = 1.0f;
        colorCache[3] = 0.4f + pulse * 0.3f;
        GLES30.glUniform4fv(colorHandle, 1, colorCache, 0);

        GLES30.glLineWidth(2.0f);
        GLES30.glEnableVertexAttribArray(positionHandleColor);
        GLES30.glVertexAttribPointer(positionHandleColor, 2, GLES30.GL_FLOAT, false, 0, highlightBuffer);
        GLES30.glDrawArrays(GLES30.GL_LINES, 0, 2);
        GLES30.glDisableVertexAttribArray(positionHandleColor);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 🟠 ESFERA DEL DRAGÓN - Goku (Procedural)
    // ═══════════════════════════════════════════════════════════════════════════

    private void drawDragonBallProcedural(float[] mvpMatrix, float pulse) {
        GLES30.glUseProgram(programIdColor);
        float scale = size * (1.0f + pulse * 0.12f);
        float currentY = y + floatOffset;

        // Glow naranja/dorado
        if (!isOnCooldown) {
            drawDragonBallGlow(mvpMatrix, scale * 1.5f, 0.2f, pulse);
            drawDragonBallGlow(mvpMatrix, scale * 1.25f, 0.3f, pulse);
        }

        // Esfera principal (naranja)
        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, x, currentY, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, scale, scale, 1);
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
        GLES30.glUniformMatrix4fv(mvpMatrixHandleColor, 1, false, finalMatrix, 0);

        float brightness = 0.85f + pulse * 0.15f;
        colorCache[0] = dragonBallMain[0] * brightness;
        colorCache[1] = dragonBallMain[1] * brightness;
        colorCache[2] = dragonBallMain[2] * brightness;
        colorCache[3] = isOnCooldown ? 0.4f : (isPressed ? 1.0f : 0.95f);
        GLES30.glUniform4fv(colorHandle, 1, colorCache, 0);

        sphereBuffer.position(0);
        GLES30.glEnableVertexAttribArray(positionHandleColor);
        GLES30.glVertexAttribPointer(positionHandleColor, 2, GLES30.GL_FLOAT, false, 0, sphereBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 50);

        // Borde dorado brillante
        float borderPulse = (float) (Math.sin(pulsePhase * 3.0) * 0.3 + 0.7);
        colorCache[0] = dragonBallHighlight[0] * borderPulse;
        colorCache[1] = dragonBallHighlight[1] * borderPulse;
        colorCache[2] = dragonBallHighlight[2] * borderPulse;
        colorCache[3] = 0.9f;
        GLES30.glUniform4fv(colorHandle, 1, colorCache, 0);
        GLES30.glLineWidth(2.0f);
        GLES30.glDrawArrays(GLES30.GL_LINE_LOOP, 1, 49);

        GLES30.glDisableVertexAttribArray(positionHandleColor);

        // ⭐ Dibujar 4 estrellas (como la esfera de 4 estrellas de Goku)
        drawDragonBallStars(mvpMatrix, scale * 0.7f, pulse);
    }

    private void drawDragonBallGlow(float[] mvpMatrix, float glowSize, float alpha, float pulse) {
        float currentY = y + floatOffset;
        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, x, currentY, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, glowSize, glowSize, 1);
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
        GLES30.glUniformMatrix4fv(mvpMatrixHandleColor, 1, false, finalMatrix, 0);

        colorCache[0] = dragonBallGlow[0];
        colorCache[1] = dragonBallGlow[1];
        colorCache[2] = dragonBallGlow[2];
        colorCache[3] = alpha * pulse;
        GLES30.glUniform4fv(colorHandle, 1, colorCache, 0);

        sphereBuffer.position(0);
        GLES30.glEnableVertexAttribArray(positionHandleColor);
        GLES30.glVertexAttribPointer(positionHandleColor, 2, GLES30.GL_FLOAT, false, 0, sphereBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 50);
        GLES30.glDisableVertexAttribArray(positionHandleColor);
    }

    /**
     * ⭐ Dibuja las 4 estrellas rojas dentro de la esfera del dragón
     * Disposición: una arriba, una abajo, dos a los lados
     */
    private void drawDragonBallStars(float[] mvpMatrix, float starScale, float pulse) {
        // Posiciones de las 4 estrellas (patrón de diamante)
        float[][] starPositions = {
            { 0.0f,  0.35f},  // Arriba
            { 0.0f, -0.35f},  // Abajo
            {-0.3f,  0.0f},   // Izquierda
            { 0.3f,  0.0f}    // Derecha
        };

        float currentY = y + floatOffset;
        float starSize = 0.15f * starScale;

        for (float[] pos : starPositions) {
            drawSingleStar(mvpMatrix, x + pos[0] * size, currentY + pos[1] * size, starSize, pulse);
        }
    }

    /**
     * Dibuja una estrella de 5 puntas
     */
    private void drawSingleStar(float[] mvpMatrix, float starX, float starY, float starSize, float pulse) {
        // Geometría de estrella de 5 puntas
        int points = 5;
        float[] starVerts = new float[(points * 2 + 2) * 2];
        starVerts[0] = 0f; starVerts[1] = 0f;  // Centro

        for (int i = 0; i <= points * 2; i++) {
            float angle = (float) (Math.PI / 2 + 2.0 * Math.PI * i / (points * 2));
            float radius = (i % 2 == 0) ? 1.0f : 0.4f;  // Puntas largas y cortas alternadas
            starVerts[(i + 1) * 2] = (float) Math.cos(angle) * radius;
            starVerts[(i + 1) * 2 + 1] = (float) Math.sin(angle) * radius;
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(starVerts.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer starBuffer = bb.asFloatBuffer();
        starBuffer.put(starVerts);
        starBuffer.position(0);

        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, starX, starY, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, starSize, starSize, 1);
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
        GLES30.glUniformMatrix4fv(mvpMatrixHandleColor, 1, false, finalMatrix, 0);

        // Estrellas rojas con brillo
        float starBrightness = 0.9f + pulse * 0.1f;
        colorCache[0] = starColor[0] * starBrightness;
        colorCache[1] = starColor[1] * starBrightness;
        colorCache[2] = starColor[2] * starBrightness;
        colorCache[3] = isOnCooldown ? 0.3f : 0.95f;
        GLES30.glUniform4fv(colorHandle, 1, colorCache, 0);

        GLES30.glEnableVertexAttribArray(positionHandleColor);
        GLES30.glVertexAttribPointer(positionHandleColor, 2, GLES30.GL_FLOAT, false, 0, starBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, points * 2 + 2);
        GLES30.glDisableVertexAttribArray(positionHandleColor);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 🌅 SOL SYNTHWAVE - NeonCity (Procedural)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Dibuja el icónico sol synthwave con gradiente y líneas horizontales
     */
    private void drawSynthwaveSunProcedural(float[] mvpMatrix, float pulse) {
        GLES30.glUseProgram(programIdColor);
        float scale = size * (1.0f + pulse * 0.12f);
        float currentY = y + floatOffset;

        // 1. Glow magenta exterior
        if (!isOnCooldown) {
            drawSynthwaveGlow(mvpMatrix, scale * 1.8f, 0.15f, pulse);
            drawSynthwaveGlow(mvpMatrix, scale * 1.4f, 0.25f, pulse);
        }

        // 2. Semicírculo superior (amarillo → naranja)
        drawSynthwaveSunHalf(mvpMatrix, scale, currentY, true, pulse);

        // 3. Semicírculo inferior (naranja → hot pink)
        drawSynthwaveSunHalf(mvpMatrix, scale, currentY, false, pulse);

        // 4. Líneas horizontales (scanlines estilo retrowave)
        drawSynthwaveScanlines(mvpMatrix, scale, currentY, pulse);
    }

    private void drawSynthwaveGlow(float[] mvpMatrix, float glowSize, float alpha, float pulse) {
        float currentY = y + floatOffset;

        // Crear círculo para glow
        int segments = 32;
        float[] glowVerts = new float[(segments + 2) * 2];
        glowVerts[0] = 0f; glowVerts[1] = 0f;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (2.0 * Math.PI * i / segments);
            glowVerts[(i + 1) * 2] = (float) Math.cos(angle);
            glowVerts[(i + 1) * 2 + 1] = (float) Math.sin(angle);
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(glowVerts.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer glowBuffer = bb.asFloatBuffer();
        glowBuffer.put(glowVerts);
        glowBuffer.position(0);

        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, x, currentY, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, glowSize, glowSize, 1);
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
        GLES30.glUniformMatrix4fv(mvpMatrixHandleColor, 1, false, finalMatrix, 0);

        // Glow magenta pulsante
        colorCache[0] = synthwaveGlow[0];
        colorCache[1] = synthwaveGlow[1];
        colorCache[2] = synthwaveGlow[2];
        colorCache[3] = alpha * (0.8f + pulse * 0.2f);
        GLES30.glUniform4fv(colorHandle, 1, colorCache, 0);

        GLES30.glEnableVertexAttribArray(positionHandleColor);
        GLES30.glVertexAttribPointer(positionHandleColor, 2, GLES30.GL_FLOAT, false, 0, glowBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, segments + 2);
        GLES30.glDisableVertexAttribArray(positionHandleColor);
    }

    private void drawSynthwaveSunHalf(float[] mvpMatrix, float scale, float currentY, boolean isTop, float pulse) {
        // Semicírculo (mitad superior o inferior)
        int segments = 20;
        float[] halfVerts = new float[(segments + 2) * 2];
        halfVerts[0] = 0f; halfVerts[1] = 0f;

        float startAngle = isTop ? 0f : (float) Math.PI;
        float endAngle = isTop ? (float) Math.PI : (float) (2 * Math.PI);

        for (int i = 0; i <= segments; i++) {
            float angle = startAngle + (endAngle - startAngle) * i / segments;
            halfVerts[(i + 1) * 2] = (float) Math.cos(angle);
            halfVerts[(i + 1) * 2 + 1] = (float) Math.sin(angle);
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(halfVerts.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer halfBuffer = bb.asFloatBuffer();
        halfBuffer.put(halfVerts);
        halfBuffer.position(0);

        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, x, currentY, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, scale, scale, 1);
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
        GLES30.glUniformMatrix4fv(mvpMatrixHandleColor, 1, false, finalMatrix, 0);

        // Gradiente: amarillo arriba, hot pink abajo
        float brightness = 0.85f + pulse * 0.15f;
        float[] color = isTop ? synthwaveSunTop : synthwaveSunBottom;
        colorCache[0] = color[0] * brightness;
        colorCache[1] = color[1] * brightness;
        colorCache[2] = color[2] * brightness;
        colorCache[3] = isOnCooldown ? 0.4f : (isPressed ? 1.0f : 0.95f);
        GLES30.glUniform4fv(colorHandle, 1, colorCache, 0);

        GLES30.glEnableVertexAttribArray(positionHandleColor);
        GLES30.glVertexAttribPointer(positionHandleColor, 2, GLES30.GL_FLOAT, false, 0, halfBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, segments + 2);
        GLES30.glDisableVertexAttribArray(positionHandleColor);
    }

    private void drawSynthwaveScanlines(float[] mvpMatrix, float scale, float currentY, float pulse) {
        // Líneas horizontales en la parte inferior del sol
        int numLines = 5;
        float lineHeight = 0.04f * scale;
        float lineWidth = scale * 0.95f;

        for (int i = 0; i < numLines; i++) {
            float lineY = currentY - (0.1f + i * 0.15f) * scale;

            // Cada línea es un rectángulo pequeño
            float[] lineVerts = {
                -lineWidth, lineY - lineHeight,
                 lineWidth, lineY - lineHeight,
                -lineWidth, lineY + lineHeight,
                 lineWidth, lineY + lineHeight
            };

            ByteBuffer bb = ByteBuffer.allocateDirect(lineVerts.length * 4);
            bb.order(ByteOrder.nativeOrder());
            FloatBuffer lineBuffer = bb.asFloatBuffer();
            lineBuffer.put(lineVerts);
            lineBuffer.position(0);

            android.opengl.Matrix.setIdentityM(modelMatrix, 0);
            android.opengl.Matrix.translateM(modelMatrix, 0, x, 0, 0);
            android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
            GLES30.glUniformMatrix4fv(mvpMatrixHandleColor, 1, false, finalMatrix, 0);

            // Líneas púrpura oscuro (como el fondo synthwave)
            colorCache[0] = synthwaveLine[0];
            colorCache[1] = synthwaveLine[1];
            colorCache[2] = synthwaveLine[2];
            colorCache[3] = isOnCooldown ? 0.3f : synthwaveLine[3];
            GLES30.glUniform4fv(colorHandle, 1, colorCache, 0);

            GLES30.glEnableVertexAttribArray(positionHandleColor);
            GLES30.glVertexAttribPointer(positionHandleColor, 2, GLES30.GL_FLOAT, false, 0, lineBuffer);
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
            GLES30.glDisableVertexAttribArray(positionHandleColor);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ⭐ COSMOS STAR - Saint Seiya (Procedural)
    // ═══════════════════════════════════════════════════════════════════════════

    // Colores COSMOS (dorado/púrpura celestial)
    private static final float[] cosmosGold = {1.0f, 0.84f, 0.0f};      // Dorado (#FFD700)
    private static final float[] cosmosPurple = {0.58f, 0.44f, 0.86f}; // Púrpura (#9370DB)
    private static final float[] cosmosGlow = {0.8f, 0.6f, 1.0f};       // Glow púrpura claro

    private void drawCosmosStarProcedural(float[] mvpMatrix, float pulse) {
        GLES30.glUseProgram(programIdColor);
        float scale = size * (1.0f + pulse * 0.15f);
        float currentY = y + floatOffset;

        // 1. Glow púrpura exterior
        if (!isOnCooldown) {
            drawCosmosGlow(mvpMatrix, scale * 2.0f, 0.12f, pulse);
            drawCosmosGlow(mvpMatrix, scale * 1.5f, 0.25f, pulse);
        }

        // 2. Estrella de 5 puntas (pentagrama)
        drawCosmosStar5Points(mvpMatrix, scale, currentY, pulse);

        // 3. Centro brillante
        drawCosmosCore(mvpMatrix, scale * 0.4f, currentY, pulse);
    }

    private void drawCosmosGlow(float[] mvpMatrix, float glowSize, float alpha, float pulse) {
        float currentY = y + floatOffset;

        // Círculo para glow
        int segments = 32;
        float[] glowVerts = new float[(segments + 2) * 2];
        glowVerts[0] = 0f; glowVerts[1] = 0f;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (2.0 * Math.PI * i / segments);
            glowVerts[(i + 1) * 2] = (float) Math.cos(angle);
            glowVerts[(i + 1) * 2 + 1] = (float) Math.sin(angle);
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(glowVerts.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer glowBuffer = bb.asFloatBuffer();
        glowBuffer.put(glowVerts);
        glowBuffer.position(0);

        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, x, currentY, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, glowSize, glowSize, 1);
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
        GLES30.glUniformMatrix4fv(mvpMatrixHandleColor, 1, false, finalMatrix, 0);

        // Glow púrpura pulsante
        colorCache[0] = cosmosGlow[0];
        colorCache[1] = cosmosGlow[1];
        colorCache[2] = cosmosGlow[2];
        colorCache[3] = alpha * (0.7f + pulse * 0.3f);
        GLES30.glUniform4fv(colorHandle, 1, colorCache, 0);

        GLES30.glEnableVertexAttribArray(positionHandleColor);
        GLES30.glVertexAttribPointer(positionHandleColor, 2, GLES30.GL_FLOAT, false, 0, glowBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, segments + 2);
        GLES30.glDisableVertexAttribArray(positionHandleColor);
    }

    private void drawCosmosStar5Points(float[] mvpMatrix, float scale, float currentY, float pulse) {
        // Estrella de 5 puntas (10 vértices + centro)
        float[] starVerts = new float[22];  // (10 + 1) * 2
        starVerts[0] = 0f; starVerts[1] = 0f;  // Centro

        float outerRadius = 1.0f;
        float innerRadius = 0.4f;

        for (int i = 0; i < 10; i++) {
            float angle = (float) (Math.PI / 2 + 2 * Math.PI * i / 10);  // Empezar desde arriba
            float radius = (i % 2 == 0) ? outerRadius : innerRadius;
            starVerts[(i + 1) * 2] = (float) (Math.cos(angle) * radius);
            starVerts[(i + 1) * 2 + 1] = (float) (Math.sin(angle) * radius);
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(starVerts.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer starBuffer = bb.asFloatBuffer();
        starBuffer.put(starVerts);
        starBuffer.position(0);

        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, x, currentY, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, scale, scale, 1);
        // Rotación sutil con el tiempo
        float rotation = pulsePhase * 0.3f;
        android.opengl.Matrix.rotateM(modelMatrix, 0, rotation * 57.3f, 0, 0, 1);
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
        GLES30.glUniformMatrix4fv(mvpMatrixHandleColor, 1, false, finalMatrix, 0);

        // Color dorado brillante
        float brightness = 0.85f + pulse * 0.15f;
        colorCache[0] = cosmosGold[0] * brightness;
        colorCache[1] = cosmosGold[1] * brightness;
        colorCache[2] = cosmosGold[2] * brightness;
        colorCache[3] = isOnCooldown ? 0.4f : (isPressed ? 1.0f : 0.95f);
        GLES30.glUniform4fv(colorHandle, 1, colorCache, 0);

        GLES30.glEnableVertexAttribArray(positionHandleColor);
        GLES30.glVertexAttribPointer(positionHandleColor, 2, GLES30.GL_FLOAT, false, 0, starBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 11);

        // Borde púrpura
        colorCache[0] = cosmosPurple[0];
        colorCache[1] = cosmosPurple[1];
        colorCache[2] = cosmosPurple[2];
        colorCache[3] = 0.8f;
        GLES30.glUniform4fv(colorHandle, 1, colorCache, 0);
        GLES30.glLineWidth(2.0f);
        GLES30.glDrawArrays(GLES30.GL_LINE_LOOP, 1, 10);

        GLES30.glDisableVertexAttribArray(positionHandleColor);
    }

    private void drawCosmosCore(float[] mvpMatrix, float coreSize, float currentY, float pulse) {
        // Centro brillante blanco-dorado
        int segments = 16;
        float[] coreVerts = new float[(segments + 2) * 2];
        coreVerts[0] = 0f; coreVerts[1] = 0f;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (2.0 * Math.PI * i / segments);
            coreVerts[(i + 1) * 2] = (float) Math.cos(angle);
            coreVerts[(i + 1) * 2 + 1] = (float) Math.sin(angle);
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(coreVerts.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer coreBuffer = bb.asFloatBuffer();
        coreBuffer.put(coreVerts);
        coreBuffer.position(0);

        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, x, currentY, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, coreSize * (1.0f + pulse * 0.2f), coreSize * (1.0f + pulse * 0.2f), 1);
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
        GLES30.glUniformMatrix4fv(mvpMatrixHandleColor, 1, false, finalMatrix, 0);

        // Blanco brillante con tinte dorado
        colorCache[0] = 1.0f;
        colorCache[1] = 0.95f;
        colorCache[2] = 0.8f;
        colorCache[3] = 0.9f + pulse * 0.1f;
        GLES30.glUniform4fv(colorHandle, 1, colorCache, 0);

        GLES30.glEnableVertexAttribArray(positionHandleColor);
        GLES30.glVertexAttribPointer(positionHandleColor, 2, GLES30.GL_FLOAT, false, 0, coreBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, segments + 2);
        GLES30.glDisableVertexAttribArray(positionHandleColor);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEXTURAS (ABYSSIA y PYRALIS)
    // ═══════════════════════════════════════════════════════════════════════════

    private void drawTexturedButton(float[] mvpMatrix, float pulse) {
        int textureId = (currentTheme == Theme.ABYSSIA) ? textureAbyssia : textureFireOrb;
        if (textureId <= 0) return;
        float alpha = isOnCooldown ? 0.4f : (isPressed ? 1.0f : 0.9f);
        float scale = size * (1.0f + pulse * 0.08f);
        float currentY = y + floatOffset;
        drawGlow(mvpMatrix, scale * 1.4f, pulse);
        GLES30.glUseProgram(programIdTexture);
        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, x, currentY, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, scale, scale, 1);
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
        GLES30.glUniformMatrix4fv(mvpMatrixHandleTex, 1, false, finalMatrix, 0);
        GLES30.glUniform1f(alphaHandle, alpha);
        GLES30.glUniform1f(pulseHandle, pulse);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(textureHandle, 0);
        quadBuffer.position(0);
        GLES30.glEnableVertexAttribArray(positionHandleTex);
        GLES30.glVertexAttribPointer(positionHandleTex, 2, GLES30.GL_FLOAT, false, 16, quadBuffer);
        quadBuffer.position(2);
        GLES30.glEnableVertexAttribArray(texCoordHandle);
        GLES30.glVertexAttribPointer(texCoordHandle, 2, GLES30.GL_FLOAT, false, 16, quadBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(positionHandleTex);
        GLES30.glDisableVertexAttribArray(texCoordHandle);
    }

    private void drawGlow(float[] mvpMatrix, float glowSize, float pulse) {
        if (isOnCooldown) return;
        GLES30.glUseProgram(programIdColor);
        float currentY = y + floatOffset;
        float[] glowColor = (currentTheme == Theme.ABYSSIA)
            ? new float[]{0.4f, 0.8f, 1.0f, 0.25f * pulse}   // Cyan
            : new float[]{1.0f, 0.5f, 0.1f, 0.3f * pulse};   // Orange fire
        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, x, currentY, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, glowSize, glowSize, 1);
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
        GLES30.glUniformMatrix4fv(mvpMatrixHandleColor, 1, false, finalMatrix, 0);
        GLES30.glUniform4fv(colorHandle, 1, glowColor, 0);
        drawCircle();
    }

    private void drawCircle() {
        int segments = 32;
        float[] circleVerts = new float[(segments + 2) * 2];
        circleVerts[0] = 0f; circleVerts[1] = 0f;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (2.0 * Math.PI * i / segments);
            circleVerts[(i + 1) * 2] = (float) Math.cos(angle);
            circleVerts[(i + 1) * 2 + 1] = (float) Math.sin(angle);
        }
        ByteBuffer bb = ByteBuffer.allocateDirect(circleVerts.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer circleBuffer = bb.asFloatBuffer();
        circleBuffer.put(circleVerts);
        circleBuffer.position(0);
        GLES30.glEnableVertexAttribArray(positionHandleColor);
        GLES30.glVertexAttribPointer(positionHandleColor, 2, GLES30.GL_FLOAT, false, 0, circleBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, segments + 2);
        GLES30.glDisableVertexAttribArray(positionHandleColor);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CORAZÓN DEFAULT (Procedural)
    // ═══════════════════════════════════════════════════════════════════════════

    private void drawHeartProcedural(float[] mvpMatrix, float pulse) {
        GLES30.glUseProgram(programIdColor);
        float scale = size * (1.0f + pulse * 0.12f);
        float currentY = y + floatOffset;
        if (!isOnCooldown) {
            drawHeartGlow(mvpMatrix, scale * 1.5f, 0.2f);
            drawHeartGlow(mvpMatrix, scale * 1.3f, 0.3f);
        }
        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, x, currentY, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, scale, scale, 1);
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
        GLES30.glUniformMatrix4fv(mvpMatrixHandleColor, 1, false, finalMatrix, 0);
        float[] color = isOnCooldown ? colorCooldown : (isPressed ? colorPressed : colorNormal);
        GLES30.glUniform4fv(colorHandle, 1, color, 0);
        GLES30.glEnableVertexAttribArray(positionHandleColor);
        GLES30.glVertexAttribPointer(positionHandleColor, 2, GLES30.GL_FLOAT, false, 0, heartBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 66);
        float borderPulse = (float) (Math.sin(pulsePhase * 4.0) * 0.3 + 0.7);
        colorCache[0] = glowCyan[0] * borderPulse + 0.3f;
        colorCache[1] = glowCyan[1] * borderPulse + 0.1f;
        colorCache[2] = glowCyan[2] * borderPulse;
        colorCache[3] = 1.0f;
        GLES30.glUniform4fv(colorHandle, 1, colorCache, 0);
        GLES30.glLineWidth(3.0f);
        GLES30.glDrawArrays(GLES30.GL_LINE_LOOP, 1, 65);
        GLES30.glDisableVertexAttribArray(positionHandleColor);
    }

    private void drawHeartGlow(float[] mvpMatrix, float glowSize, float alpha) {
        float currentY = y + floatOffset;
        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, x, currentY, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, glowSize, glowSize, 1);
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
        GLES30.glUniformMatrix4fv(mvpMatrixHandleColor, 1, false, finalMatrix, 0);
        float colorMix = (float) (Math.sin(pulsePhase * 2.5) * 0.5 + 0.5);
        colorCache[0] = glowCyan[0] * (1 - colorMix) + glowPink[0] * colorMix;
        colorCache[1] = glowCyan[1] * (1 - colorMix) + glowPink[1] * colorMix;
        colorCache[2] = glowCyan[2] * (1 - colorMix) + glowPink[2] * colorMix;
        colorCache[3] = alpha * 0.8f;
        GLES30.glUniform4fv(colorHandle, 1, colorCache, 0);
        GLES30.glEnableVertexAttribArray(positionHandleColor);
        GLES30.glVertexAttribPointer(positionHandleColor, 2, GLES30.GL_FLOAT, false, 0, heartBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 66);
        GLES30.glDisableVertexAttribArray(positionHandleColor);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean isTouched(float touchX, float touchY) {
        float dx = touchX - x;
        float dy = touchY - y;
        return Math.sqrt(dx * dx + dy * dy) <= size * 1.5f;
    }

    public void onPress() { isPressed = true; Log.d(TAG, "Press"); }
    public void onRelease() { isPressed = false; }
    public void setCooldown(boolean cooldown) { isOnCooldown = cooldown; }
    public void setPosition(float x, float y) { this.x = x; this.y = y; }
    public void setSize(float size) { this.size = size; }
    public float getX() { return x; }
    public float getY() { return y; }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);
        return shader;
    }

    public void cleanup() {
        if (programIdTexture != 0) GLES30.glDeleteProgram(programIdTexture);
        if (programIdColor != 0) GLES30.glDeleteProgram(programIdColor);
        if (textureAbyssia > 0) GLES30.glDeleteTextures(1, new int[]{textureAbyssia}, 0);
        if (textureFireOrb > 0) GLES30.glDeleteTextures(1, new int[]{textureFireOrb}, 0);
        isInitialized = false;
    }
}
