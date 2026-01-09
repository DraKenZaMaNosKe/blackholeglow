package com.secret.blackholeglow.scenes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.Battery3D;
import com.secret.blackholeglow.Clock3D;
import com.secret.blackholeglow.EqualizerBarsDJ;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                    ⭐ SAINT SEIYA SCENE - Cosmos                         ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Caballeros del Zodiaco - Shader cosmos + Imagen de Seiya.               ║
 * ║  Fondo: Nebulosa animada con estrellas (ShaderToy optimizado).           ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class SaintSeiyaScene extends WallpaperScene {
    private static final String TAG = "SaintSeiyaScene";

    // UI Components
    private EqualizerBarsDJ equalizerDJ;
    private Clock3D clock;
    private Battery3D battery;

    // ═══════════════════════════════════════════════════════════════════════
    // 🏛️ IMAGEN DE FONDO (Santuario)
    // ═══════════════════════════════════════════════════════════════════════
    private int bgTexture = -1;
    private int bgShaderProgram = -1;
    private FloatBuffer bgQuadBuffer;
    private int bgPositionLoc;
    private int bgTexCoordLoc;
    private int bgTextureLoc;
    private float time = 0f;

    // ═══════════════════════════════════════════════════════════════════════
    // 🖼️ IMAGEN DE SEIYA
    // ═══════════════════════════════════════════════════════════════════════
    private int seiyaTexture = -1;
    private int seiyaShaderProgram = -1;
    private FloatBuffer seiyaQuadBuffer;
    private int seiyaPositionLoc;
    private int seiyaTexCoordLoc;
    private int seiyaTextureLoc;
    private int seiyaAlphaLoc;
    private int seiyaTimeLoc;

    @Override
    public String getName() {
        return "SAINT_SEIYA";
    }

    @Override
    public String getDescription() {
        return "Saint Seiya - Cosmos Power";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_saintseiya;
    }

    @Override
    protected void setupScene() {
        Log.d(TAG, "⭐ Configurando Saint Seiya Cosmos...");

        // 1. Imagen de fondo (Santuario)
        setupBackgroundShader();
        loadBackgroundTexture();
        createBackgroundQuad();

        // 2. Shader y textura de Seiya
        setupSeiyaShader();
        loadSeiyaTexture();
        createSeiyaQuad();

        // 🎵 Ecualizador con tema COSMOS
        try {
            equalizerDJ = new EqualizerBarsDJ();
            equalizerDJ.initialize();
            equalizerDJ.setTheme(EqualizerBarsDJ.Theme.COSMOS);
            equalizerDJ.setScreenSize(screenWidth, screenHeight);
            Log.d(TAG, "✅ Ecualizador COSMOS activado");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error EqualizerBarsDJ: " + e.getMessage());
        }

        // ⏰ Reloj con tema COSMOS
        try {
            clock = new Clock3D(context, Clock3D.THEME_COSMOS, 0f, 0.75f);
            Log.d(TAG, "✅ Reloj COSMOS activado");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error Clock3D: " + e.getMessage());
        }

        // 🔋 Batería con tema COSMOS
        try {
            battery = new Battery3D(context, Battery3D.THEME_COSMOS, 0.81f, -0.34f);
            Log.d(TAG, "✅ Batería COSMOS activada");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error Battery3D: " + e.getMessage());
        }

        Log.d(TAG, "⭐ Saint Seiya Cosmos listo!");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🏛️ BACKGROUND SHADER (Imagen fija del Santuario)
    // ═══════════════════════════════════════════════════════════════════════

    private void setupBackgroundShader() {
        String vertexShader =
            "attribute vec4 aPosition;\n" +
            "attribute vec2 aTexCoord;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "    gl_Position = aPosition;\n" +
            "    vTexCoord = aTexCoord;\n" +
            "}\n";

        String fragmentShader =
            "precision mediump float;\n" +
            "varying vec2 vTexCoord;\n" +
            "uniform sampler2D uTexture;\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(uTexture, vTexCoord);\n" +
            "}\n";

        bgShaderProgram = createProgram(vertexShader, fragmentShader);
        bgPositionLoc = GLES30.glGetAttribLocation(bgShaderProgram, "aPosition");
        bgTexCoordLoc = GLES30.glGetAttribLocation(bgShaderProgram, "aTexCoord");
        bgTextureLoc = GLES30.glGetUniformLocation(bgShaderProgram, "uTexture");

        Log.d(TAG, "✅ Shader Background compilado");
    }

    private void loadBackgroundTexture() {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.saintseiya_background, options);

            if (bitmap != null) {
                int[] textures = new int[1];
                GLES30.glGenTextures(1, textures, 0);
                bgTexture = textures[0];

                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bgTexture);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

                GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);

                Log.d(TAG, "✅ Textura Background cargada (" + bitmap.getWidth() + "x" + bitmap.getHeight() + ")");
                bitmap.recycle();
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error cargando background: " + e.getMessage());
        }
    }

    private void createBackgroundQuad() {
        // Quad con posición (x,y) y texcoord (u,v)
        float[] quadVerts = {
            -1f, -1f, 0f, 1f,  // bottom-left
             1f, -1f, 1f, 1f,  // bottom-right
            -1f,  1f, 0f, 0f,  // top-left
             1f,  1f, 1f, 0f   // top-right
        };
        ByteBuffer bb = ByteBuffer.allocateDirect(quadVerts.length * 4);
        bb.order(ByteOrder.nativeOrder());
        bgQuadBuffer = bb.asFloatBuffer();
        bgQuadBuffer.put(quadVerts);
        bgQuadBuffer.position(0);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🖼️ SEIYA SHADER & TEXTURE
    // ═══════════════════════════════════════════════════════════════════════

    private void setupSeiyaShader() {
        String vertexShader =
            "attribute vec4 aPosition;\n" +
            "attribute vec2 aTexCoord;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "    gl_Position = aPosition;\n" +
            "    vTexCoord = aTexCoord;\n" +
            "}\n";

        // Fragment shader COSMOS CLASICO - Aura azul suave con smoothstep
        String fragmentShader =
            "precision mediump float;\n" +
            "varying vec2 vTexCoord;\n" +
            "uniform sampler2D uTexture;\n" +
            "uniform float uAlpha;\n" +
            "uniform float uTime;\n" +
            "\n" +
            "void main() {\n" +
            "    vec4 color = texture2D(uTexture, vTexCoord);\n" +
            "    vec2 ps = vec2(0.004, 0.005);\n" +
            "    \n" +
            "    // Deteccion de bordes suave (4 samples)\n" +
            "    float aU = texture2D(uTexture, vTexCoord + vec2(0.0, ps.y)).a;\n" +
            "    float aD = texture2D(uTexture, vTexCoord - vec2(0.0, ps.y)).a;\n" +
            "    float aL = texture2D(uTexture, vTexCoord - vec2(ps.x, 0.0)).a;\n" +
            "    float aR = texture2D(uTexture, vTexCoord + vec2(ps.x, 0.0)).a;\n" +
            "    float near = (aU + aD + aL + aR) * 0.25;\n" +
            "    \n" +
            "    // Capa exterior (mas lejos)\n" +
            "    float aU2 = texture2D(uTexture, vTexCoord + vec2(0.0, ps.y * 2.5)).a;\n" +
            "    float aD2 = texture2D(uTexture, vTexCoord - vec2(0.0, ps.y * 2.5)).a;\n" +
            "    float aL2 = texture2D(uTexture, vTexCoord - vec2(ps.x * 2.5, 0.0)).a;\n" +
            "    float aR2 = texture2D(uTexture, vTexCoord + vec2(ps.x * 2.5, 0.0)).a;\n" +
            "    float far = (aU2 + aD2 + aL2 + aR2) * 0.25;\n" +
            "    \n" +
            "    // Bordes suavizados con smoothstep\n" +
            "    float edgeInner = smoothstep(0.0, 0.5, near) * smoothstep(0.5, 0.0, color.a);\n" +
            "    float edgeOuter = smoothstep(0.0, 0.4, far) * smoothstep(0.3, 0.0, color.a);\n" +
            "    \n" +
            "    // Tiempo suavizado para animacion fluida\n" +
            "    float t = uTime;\n" +
            "    \n" +
            "    // Llamas cosmos - smoothstep para transiciones suaves\n" +
            "    float wave1 = sin(vTexCoord.y * 12.0 - t * 3.0 + vTexCoord.x * 5.0);\n" +
            "    float wave2 = sin(vTexCoord.y * 18.0 - t * 4.0 - vTexCoord.x * 4.0);\n" +
            "    float wave3 = sin(vTexCoord.y * 8.0 - t * 2.5);\n" +
            "    float flames = smoothstep(-0.3, 0.8, (wave1 + wave2 + wave3) / 3.0);\n" +
            "    \n" +
            "    // Pulso suave\n" +
            "    float pulse = smoothstep(0.0, 1.0, 0.5 + 0.5 * sin(t * 2.0));\n" +
            "    pulse = 0.7 + 0.3 * pulse;\n" +
            "    \n" +
            "    // Colores cosmos (azul profundo -> cyan -> blanco)\n" +
            "    vec3 cDeep = vec3(0.05, 0.15, 0.6);\n" +
            "    vec3 cMid = vec3(0.2, 0.4, 0.9);\n" +
            "    vec3 cBright = vec3(0.5, 0.7, 1.0);\n" +
            "    vec3 cosmosColor = mix(cDeep, cMid, flames);\n" +
            "    cosmosColor = mix(cosmosColor, cBright, smoothstep(0.3, 0.8, edgeInner));\n" +
            "    \n" +
            "    // Intensidad del aura con smoothstep\n" +
            "    float auraInner = smoothstep(0.0, 0.6, edgeInner * flames) * pulse * 1.2;\n" +
            "    float auraOuter = smoothstep(0.0, 0.5, edgeOuter * flames) * pulse * 0.6;\n" +
            "    float totalAura = auraInner + auraOuter;\n" +
            "    \n" +
            "    // Brillo en zonas claras\n" +
            "    float lum = dot(color.rgb, vec3(0.299, 0.587, 0.114));\n" +
            "    float energyBoost = smoothstep(0.5, 0.95, lum) * pulse * 0.2;\n" +
            "    \n" +
            "    // Combinar todo\n" +
            "    vec3 finalRGB = color.rgb;\n" +
            "    finalRGB += cosmosColor * totalAura;\n" +
            "    finalRGB += vec3(1.0, 0.85, 0.4) * energyBoost * color.a;\n" +
            "    \n" +
            "    // Alpha final suave\n" +
            "    float auraAlpha = smoothstep(0.0, 0.8, totalAura) * 0.7;\n" +
            "    float finalAlpha = max(color.a, auraAlpha);\n" +
            "    \n" +
            "    gl_FragColor = vec4(finalRGB, finalAlpha * uAlpha);\n" +
            "}\n";

        seiyaShaderProgram = createProgram(vertexShader, fragmentShader);
        seiyaPositionLoc = GLES30.glGetAttribLocation(seiyaShaderProgram, "aPosition");
        seiyaTexCoordLoc = GLES30.glGetAttribLocation(seiyaShaderProgram, "aTexCoord");
        seiyaTextureLoc = GLES30.glGetUniformLocation(seiyaShaderProgram, "uTexture");
        seiyaAlphaLoc = GLES30.glGetUniformLocation(seiyaShaderProgram, "uAlpha");
        seiyaTimeLoc = GLES30.glGetUniformLocation(seiyaShaderProgram, "uTime");

        Log.d(TAG, "✅ Shader Seiya COSMOS compilado");
    }

    private void loadSeiyaTexture() {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.seiya_character, options);

            if (bitmap != null) {
                int[] textures = new int[1];
                GLES30.glGenTextures(1, textures, 0);
                seiyaTexture = textures[0];

                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, seiyaTexture);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

                GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);

                Log.d(TAG, "✅ Textura Seiya cargada (" + bitmap.getWidth() + "x" + bitmap.getHeight() + ")");
                bitmap.recycle();
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error cargando textura: " + e.getMessage());
        }
    }

    private void createSeiyaQuad() {
        float[] quadVerts = {
            -1f, -1f, 0f, 1f,
             1f, -1f, 1f, 1f,
            -1f,  1f, 0f, 0f,
             1f,  1f, 1f, 0f
        };
        ByteBuffer bb = ByteBuffer.allocateDirect(quadVerts.length * 4);
        bb.order(ByteOrder.nativeOrder());
        seiyaQuadBuffer = bb.asFloatBuffer();
        seiyaQuadBuffer.put(quadVerts);
        seiyaQuadBuffer.position(0);
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource);
        int program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragmentShader);
        GLES30.glLinkProgram(program);
        return program;
    }

    private int loadShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);

        // Verificar errores de compilación
        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            String error = GLES30.glGetShaderInfoLog(shader);
            Log.e(TAG, "❌ Error compilando shader: " + error);
        }
        return shader;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🔄 UPDATE & DRAW
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    protected void releaseSceneResources() {
        if (seiyaTexture > 0) {
            GLES30.glDeleteTextures(1, new int[]{seiyaTexture}, 0);
            seiyaTexture = -1;
        }
        if (seiyaShaderProgram > 0) {
            GLES30.glDeleteProgram(seiyaShaderProgram);
            seiyaShaderProgram = -1;
        }
        if (bgTexture > 0) {
            GLES30.glDeleteTextures(1, new int[]{bgTexture}, 0);
            bgTexture = -1;
        }
        if (bgShaderProgram > 0) {
            GLES30.glDeleteProgram(bgShaderProgram);
            bgShaderProgram = -1;
        }
        if (clock != null) { clock.dispose(); clock = null; }
        if (battery != null) { battery.dispose(); battery = null; }
        if (equalizerDJ != null) { equalizerDJ.release(); equalizerDJ = null; }
    }

    @Override
    public void update(float deltaTime) {
        time += deltaTime;
        if (time > 628.0f) time -= 628.0f; // Reset cada ~100 ciclos (evita overflow)
        if (equalizerDJ != null) equalizerDJ.update(deltaTime);
        if (clock != null) clock.update(deltaTime);
        if (battery != null) battery.update(deltaTime);
        super.update(deltaTime);
    }

    @Override
    public void draw() {
        if (isDisposed) return;

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

        // 🔥 Seiya con cosmos
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        drawSeiyaImage();

        // 3. UI Elements
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        if (equalizerDJ != null) equalizerDJ.draw();
        if (clock != null) clock.draw();
        if (battery != null) battery.draw();

        super.draw();
    }

    private void drawBackground() {
        if (bgTexture <= 0 || bgShaderProgram <= 0) return;

        GLES30.glUseProgram(bgShaderProgram);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bgTexture);
        GLES30.glUniform1i(bgTextureLoc, 0);

        bgQuadBuffer.position(0);
        GLES30.glEnableVertexAttribArray(bgPositionLoc);
        GLES30.glVertexAttribPointer(bgPositionLoc, 2, GLES30.GL_FLOAT, false, 16, bgQuadBuffer);

        bgQuadBuffer.position(2);
        GLES30.glEnableVertexAttribArray(bgTexCoordLoc);
        GLES30.glVertexAttribPointer(bgTexCoordLoc, 2, GLES30.GL_FLOAT, false, 16, bgQuadBuffer);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(bgPositionLoc);
        GLES30.glDisableVertexAttribArray(bgTexCoordLoc);
    }

    private void drawSeiyaImage() {
        if (seiyaTexture <= 0 || seiyaShaderProgram <= 0) return;

        GLES30.glUseProgram(seiyaShaderProgram);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, seiyaTexture);
        GLES30.glUniform1i(seiyaTextureLoc, 0);
        GLES30.glUniform1f(seiyaAlphaLoc, 1.0f);
        GLES30.glUniform1f(seiyaTimeLoc, time);

        seiyaQuadBuffer.position(0);
        GLES30.glEnableVertexAttribArray(seiyaPositionLoc);
        GLES30.glVertexAttribPointer(seiyaPositionLoc, 2, GLES30.GL_FLOAT, false, 16, seiyaQuadBuffer);

        seiyaQuadBuffer.position(2);
        GLES30.glEnableVertexAttribArray(seiyaTexCoordLoc);
        GLES30.glVertexAttribPointer(seiyaTexCoordLoc, 2, GLES30.GL_FLOAT, false, 16, seiyaQuadBuffer);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(seiyaPositionLoc);
        GLES30.glDisableVertexAttribArray(seiyaTexCoordLoc);
    }

    @Override
    public void setScreenSize(int width, int height) {
        super.setScreenSize(width, height);
        if (equalizerDJ != null) equalizerDJ.setScreenSize(width, height);
    }

    public void updateMusicBands(float[] bands) {
        if (equalizerDJ != null && bands != null && bands.length > 0) {
            equalizerDJ.updateFromBands(bands);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "⏸️ Saint Seiya PAUSADO");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "▶️ Saint Seiya REANUDADO");
    }

    @Override
    public boolean onTouchEvent(float normalizedX, float normalizedY, int action) {
        return super.onTouchEvent(normalizedX, normalizedY, action);
    }
}
