package com.secret.blackholeglow;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.os.BatteryManager;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 🔋 BATTERY3D - INDICADOR DE BATERÍA ESTILO REACTOR
 *
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║  PYRALIS: Reactor de Plasma                                          ║
 * ║  - Núcleo de energía que brilla con fuego/plasma                     ║
 * ║  - Color: Dorado → Naranja → Rojo según nivel                        ║
 * ║  - Partículas de fuego cuando carga                                  ║
 * ║                                                                       ║
 * ║  ABYSSIA: Orbe Bioluminiscente                                       ║
 * ║  - Esfera de energía marina brillante                                ║
 * ║  - Color: Cyan → Verde → Amarillo según nivel                        ║
 * ║  - Ondas de luz cuando carga                                         ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 */
public class Battery3D implements SceneObject {
    private static final String TAG = "Battery3D";

    // ═══════════════════════════════════════════════════════════════
    // TEMAS
    // ═══════════════════════════════════════════════════════════════
    public static final int THEME_ABYSSIA = 0;
    public static final int THEME_PYRALIS = 1;
    public static final int THEME_KAMEHAMEHA = 2;
    public static final int THEME_SYNTHWAVE = 3;

    // Colores PYRALIS (Fuego/Plasma)
    private static final int PYRALIS_HIGH = 0xFFFFD700;      // Gold (>50%)
    private static final int PYRALIS_MED = 0xFFFF6B00;       // Orange (20-50%)
    private static final int PYRALIS_LOW = 0xFFFF2200;       // Red (<20%)
    private static final int PYRALIS_GLOW = 0xFFFF8C00;      // Dark Orange glow
    private static final int PYRALIS_CORE = 0xFFFFE4B5;      // Moccasin (núcleo)

    // Colores ABYSSIA (Océano/Bio)
    private static final int ABYSSIA_HIGH = 0xFF00CED1;      // Cyan (>50%)
    private static final int ABYSSIA_MED = 0xFF00FF7F;       // Spring Green (20-50%)
    private static final int ABYSSIA_LOW = 0xFFFFD700;       // Gold warning (<20%)
    private static final int ABYSSIA_GLOW = 0xFF00FFFF;      // Cyan glow
    private static final int ABYSSIA_CORE = 0xFFE0FFFF;      // Light Cyan (núcleo)

    // Colores KAMEHAMEHA (Energía Ki - Azul)
    private static final int KAMEHAMEHA_HIGH = 0xFF4FC3F7;   // Light Blue (>50%)
    private static final int KAMEHAMEHA_MED = 0xFF29B6F6;    // Blue (20-50%)
    private static final int KAMEHAMEHA_LOW = 0xFFFF5722;    // Orange warning (<20%)
    private static final int KAMEHAMEHA_GLOW = 0xFF00E5FF;   // Cyan accent glow

    // Colores SYNTHWAVE (Retrowave 80s - Pink/Cyan)
    private static final int SYNTHWAVE_HIGH = 0xFF00FFFF;    // Cyan (>50%)
    private static final int SYNTHWAVE_MED = 0xFFFF1493;     // Hot Pink (20-50%)
    private static final int SYNTHWAVE_LOW = 0xFFFF0066;     // Red-Pink (<20%)
    private static final int SYNTHWAVE_GLOW = 0xFFFF00FF;    // Magenta glow
    private static final int SYNTHWAVE_CORE = 0xFFFFB6C1;    // Light Pink (núcleo)
    private static final int KAMEHAMEHA_CORE = 0xFFE1F5FE;   // Light Blue (núcleo)

    // ═══════════════════════════════════════════════════════════════
    // ESTADO DE BATERÍA
    // ═══════════════════════════════════════════════════════════════
    private int batteryLevel = 100;        // 0-100
    private boolean isCharging = false;
    private int lastBatteryLevel = -1;
    private boolean lastChargingState = false;
    private boolean needsUpdate = true;

    // ═══════════════════════════════════════════════════════════════
    // OPENGL
    // ═══════════════════════════════════════════════════════════════
    private int programId;
    private int aPositionLoc;
    private int aTexCoordLoc;
    private int uTextureLoc;
    private int uAlphaLoc;
    private int uTimeLoc;
    private int uChargingLoc;
    private int uLevelLoc;

    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;
    private int textureId = -1;

    // ═══════════════════════════════════════════════════════════════
    // POSICIÓN Y TAMAÑO
    // ═══════════════════════════════════════════════════════════════
    private float x, y;
    private float width = 0.25f;
    private float height = 0.06f;

    // ═══════════════════════════════════════════════════════════════
    // ANIMACIÓN
    // ═══════════════════════════════════════════════════════════════
    private float time = 0f;
    private float pulsePhase = 0f;
    private float chargeWave = 0f;

    // ═══════════════════════════════════════════════════════════════
    // TEMA Y ESTILO
    // ═══════════════════════════════════════════════════════════════
    private int currentTheme;
    private int primaryColor;
    private int glowColor;
    private int coreColor;

    // Bitmap para textura
    private static final int TEX_WIDTH = 256;
    private static final int TEX_HEIGHT = 128;
    private Bitmap bitmap;
    private Canvas canvas;

    // Context y BroadcastReceiver
    private Context context;
    private BroadcastReceiver batteryReceiver;
    private boolean receiverRegistered = false;

    // ═══════════════════════════════════════════════════════════════
    // SHADER CON EFECTOS DINÁMICOS
    // ═══════════════════════════════════════════════════════════════
    private final String vertexShader =
        "attribute vec2 a_Position;\n" +
        "attribute vec2 a_TexCoord;\n" +
        "varying vec2 v_TexCoord;\n" +
        "void main() {\n" +
        "    v_TexCoord = a_TexCoord;\n" +
        "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
        "}\n";

    private final String fragmentShader =
        "precision mediump float;\n" +
        "varying vec2 v_TexCoord;\n" +
        "uniform sampler2D u_Texture;\n" +
        "uniform float u_Alpha;\n" +
        "uniform float u_Time;\n" +
        "uniform float u_Charging;\n" +
        "uniform float u_Level;\n" +
        "\n" +
        "void main() {\n" +
        "    vec4 texColor = texture2D(u_Texture, v_TexCoord);\n" +
        "    \n" +
        "    // Pulso base que varía con el nivel\n" +
        "    float pulse = 1.0 + sin(u_Time * 2.0) * 0.15 * (1.0 - u_Level);\n" +
        "    \n" +
        "    // Efecto de carga: ondas brillantes\n" +
        "    float chargeEffect = 0.0;\n" +
        "    if (u_Charging > 0.5) {\n" +
        "        float wave = sin(v_TexCoord.x * 10.0 - u_Time * 5.0) * 0.5 + 0.5;\n" +
        "        chargeEffect = wave * 0.4;\n" +
        "    }\n" +
        "    \n" +
        "    // Brillo crítico cuando batería baja\n" +
        "    float criticalPulse = 0.0;\n" +
        "    if (u_Level < 0.2) {\n" +
        "        criticalPulse = sin(u_Time * 8.0) * 0.3;\n" +
        "    }\n" +
        "    \n" +
        "    float brightness = pulse + chargeEffect + criticalPulse;\n" +
        "    vec3 finalColor = texColor.rgb * brightness;\n" +
        "    \n" +
        "    gl_FragColor = vec4(finalColor, texColor.a * u_Alpha);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    public Battery3D(Context context, int theme, float x, float y) {
        this.context = context.getApplicationContext();
        this.x = x;
        this.y = y;
        this.currentTheme = theme;

        applyTheme(theme);
        initShader();
        setupBuffers();
        createBitmap();
        registerBatteryReceiver();

        Log.d(TAG, "✓ Battery3D creado - tema: " + (theme == THEME_PYRALIS ? "PYRALIS" : "ABYSSIA"));
    }

    public Battery3D(Context context, int theme) {
        this(context, theme, 0.75f, -0.75f);  // Esquina inferior derecha
    }

    // ═══════════════════════════════════════════════════════════════
    // TEMA
    // ═══════════════════════════════════════════════════════════════

    private void applyTheme(int theme) {
        currentTheme = theme;
        if (theme == THEME_PYRALIS) {
            primaryColor = PYRALIS_HIGH;
            glowColor = PYRALIS_GLOW;
            coreColor = PYRALIS_CORE;
        } else if (theme == THEME_KAMEHAMEHA) {
            primaryColor = KAMEHAMEHA_HIGH;
            glowColor = KAMEHAMEHA_GLOW;
            coreColor = KAMEHAMEHA_CORE;
        } else if (theme == THEME_SYNTHWAVE) {
            primaryColor = SYNTHWAVE_HIGH;
            glowColor = SYNTHWAVE_GLOW;
            coreColor = SYNTHWAVE_CORE;
        } else {
            primaryColor = ABYSSIA_HIGH;
            glowColor = ABYSSIA_GLOW;
            coreColor = ABYSSIA_CORE;
        }
    }

    private int getColorForLevel(int level) {
        if (currentTheme == THEME_PYRALIS) {
            if (level > 50) return PYRALIS_HIGH;
            if (level > 20) return PYRALIS_MED;
            return PYRALIS_LOW;
        } else if (currentTheme == THEME_KAMEHAMEHA) {
            if (level > 50) return KAMEHAMEHA_HIGH;
            if (level > 20) return KAMEHAMEHA_MED;
            return KAMEHAMEHA_LOW;
        } else if (currentTheme == THEME_SYNTHWAVE) {
            if (level > 50) return SYNTHWAVE_HIGH;
            if (level > 20) return SYNTHWAVE_MED;
            return SYNTHWAVE_LOW;
        } else {
            if (level > 50) return ABYSSIA_HIGH;
            if (level > 20) return ABYSSIA_MED;
            return ABYSSIA_LOW;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // BATTERY RECEIVER
    // ═══════════════════════════════════════════════════════════════

    private void registerBatteryReceiver() {
        if (receiverRegistered) return;

        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

                if (level >= 0 && scale > 0) {
                    batteryLevel = (level * 100) / scale;
                }
                isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING ||
                              status == BatteryManager.BATTERY_STATUS_FULL);

                // Marcar para actualizar si cambió
                if (batteryLevel != lastBatteryLevel || isCharging != lastChargingState) {
                    needsUpdate = true;
                    lastBatteryLevel = batteryLevel;
                    lastChargingState = isCharging;
                    Log.d(TAG, "🔋 Batería: " + batteryLevel + "% " + (isCharging ? "⚡CARGANDO" : ""));
                }
            }
        };

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(batteryReceiver, filter);
        receiverRegistered = true;

        // Obtener estado inicial
        Intent batteryStatus = context.registerReceiver(null, filter);
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

            if (level >= 0 && scale > 0) {
                batteryLevel = (level * 100) / scale;
            }
            isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING ||
                          status == BatteryManager.BATTERY_STATUS_FULL);
            needsUpdate = true;
        }
    }

    private void unregisterBatteryReceiver() {
        if (receiverRegistered && batteryReceiver != null) {
            try {
                context.unregisterReceiver(batteryReceiver);
            } catch (Exception e) {
                Log.w(TAG, "Error unregistering receiver: " + e.getMessage());
            }
            receiverRegistered = false;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // OPENGL SETUP
    // ═══════════════════════════════════════════════════════════════

    private void initShader() {
        programId = ShaderUtils.createProgram(vertexShader, fragmentShader);

        if (programId == 0) {
            Log.e(TAG, "✗ Error creando shader de batería");
            return;
        }

        aPositionLoc = GLES30.glGetAttribLocation(programId, "a_Position");
        aTexCoordLoc = GLES30.glGetAttribLocation(programId, "a_TexCoord");
        uTextureLoc = GLES30.glGetUniformLocation(programId, "u_Texture");
        uAlphaLoc = GLES30.glGetUniformLocation(programId, "u_Alpha");
        uTimeLoc = GLES30.glGetUniformLocation(programId, "u_Time");
        uChargingLoc = GLES30.glGetUniformLocation(programId, "u_Charging");
        uLevelLoc = GLES30.glGetUniformLocation(programId, "u_Level");

        Log.d(TAG, "✓ Shader de batería inicializado");
    }

    private void setupBuffers() {
        float halfW = width / 2f;
        float halfH = height / 2f;

        float[] vertices = {
            x - halfW, y - halfH,
            x + halfW, y - halfH,
            x - halfW, y + halfH,
            x + halfW, y + halfH
        };

        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        float[] texCoords = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f
        };

        ByteBuffer tbb = ByteBuffer.allocateDirect(texCoords.length * 4);
        tbb.order(ByteOrder.nativeOrder());
        texCoordBuffer = tbb.asFloatBuffer();
        texCoordBuffer.put(texCoords);
        texCoordBuffer.position(0);
    }

    private void createBitmap() {
        bitmap = Bitmap.createBitmap(TEX_WIDTH, TEX_HEIGHT, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
    }

    // ═══════════════════════════════════════════════════════════════
    // RENDERIZADO DE TEXTURA
    // ═══════════════════════════════════════════════════════════════

    private void updateTexture() {
        if (!needsUpdate && textureId != -1) return;
        needsUpdate = false;

        // Limpiar canvas
        bitmap.eraseColor(Color.TRANSPARENT);

        int levelColor = getColorForLevel(batteryLevel);
        float fillPercent = batteryLevel / 100f;

        // ═══════════════════════════════════════════════════════════
        // DIBUJAR REACTOR/ORBE
        // ═══════════════════════════════════════════════════════════

        float padding = 8f;
        float left = padding;
        float top = padding;
        float right = TEX_WIDTH - padding;
        float bottom = TEX_HEIGHT - padding;
        float cornerRadius = 16f;

        RectF outerRect = new RectF(left, top, right, bottom);
        RectF innerRect = new RectF(left + 6, top + 6, right - 6, bottom - 6);

        // 1. GLOW EXTERIOR
        Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setColor(glowColor);
        glowPaint.setAlpha(80);
        glowPaint.setMaskFilter(new BlurMaskFilter(12, BlurMaskFilter.Blur.OUTER));
        canvas.drawRoundRect(outerRect, cornerRadius, cornerRadius, glowPaint);

        // 2. BORDE EXTERIOR (marco del reactor)
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f);
        borderPaint.setColor(Color.argb(200, 100, 100, 100));
        canvas.drawRoundRect(outerRect, cornerRadius, cornerRadius, borderPaint);

        // 3. FONDO INTERIOR (oscuro)
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.argb(220, 20, 20, 30));
        canvas.drawRoundRect(innerRect, cornerRadius - 4, cornerRadius - 4, bgPaint);

        // 4. BARRA DE ENERGÍA (llenado)
        float fillWidth = (innerRect.width() - 8) * fillPercent;
        RectF fillRect = new RectF(
            innerRect.left + 4,
            innerRect.top + 4,
            innerRect.left + 4 + fillWidth,
            innerRect.bottom - 4
        );

        // Gradiente para la barra de energía
        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int gradientEnd = adjustBrightness(levelColor, 0.6f);
        LinearGradient gradient = new LinearGradient(
            fillRect.left, fillRect.top,
            fillRect.left, fillRect.bottom,
            levelColor, gradientEnd,
            Shader.TileMode.CLAMP
        );
        fillPaint.setShader(gradient);
        canvas.drawRoundRect(fillRect, 8, 8, fillPaint);

        // 5. BRILLO SUPERIOR EN LA BARRA
        if (fillWidth > 10) {
            Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            highlightPaint.setColor(Color.WHITE);
            highlightPaint.setAlpha(60);
            RectF highlightRect = new RectF(
                fillRect.left + 4,
                fillRect.top + 2,
                fillRect.right - 4,
                fillRect.top + (fillRect.height() * 0.3f)
            );
            canvas.drawRoundRect(highlightRect, 4, 4, highlightPaint);
        }

        // 6. NÚCLEO BRILLANTE (centro de energía)
        if (fillPercent > 0.1f) {
            Paint corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            corePaint.setColor(coreColor);
            corePaint.setAlpha(150);
            corePaint.setMaskFilter(new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL));

            float coreX = fillRect.left + fillWidth * 0.5f;
            float coreY = fillRect.centerY();
            float coreRadius = Math.min(fillRect.height() * 0.3f, fillWidth * 0.2f);
            canvas.drawCircle(coreX, coreY, coreRadius, corePaint);
        }

        // 7. ICONO DE CARGA (rayo ⚡)
        if (isCharging) {
            drawChargingBolt(canvas, innerRect.centerX(), innerRect.centerY());
        }

        // 8. TEXTO DE PORCENTAJE
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setShadowLayer(4, 0, 0, Color.BLACK);

        String percentText = batteryLevel + "%";
        float textX = innerRect.centerX();
        float textY = innerRect.centerY() + 10;
        canvas.drawText(percentText, textX, textY, textPaint);

        // Subir textura a GPU
        if (textureId == -1) {
            int[] textures = new int[1];
            GLES30.glGenTextures(1, textures, 0);
            textureId = textures[0];
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
    }

    private void drawChargingBolt(Canvas canvas, float cx, float cy) {
        Paint boltPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boltPaint.setColor(Color.YELLOW);
        boltPaint.setAlpha(220);
        boltPaint.setTextSize(40f);
        boltPaint.setTextAlign(Paint.Align.CENTER);
        boltPaint.setMaskFilter(new BlurMaskFilter(6, BlurMaskFilter.Blur.NORMAL));
        canvas.drawText("⚡", cx, cy + 14, boltPaint);
    }

    private int adjustBrightness(int color, float factor) {
        int r = (int) (Color.red(color) * factor);
        int g = (int) (Color.green(color) * factor);
        int b = (int) (Color.blue(color) * factor);
        return Color.argb(Color.alpha(color),
            Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE & DRAW
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void update(float deltaTime) {
        time += deltaTime;

        // Animación de pulso
        pulsePhase += deltaTime * 2f;
        if (pulsePhase > Math.PI * 2) {
            pulsePhase -= Math.PI * 2;
        }

        // Onda de carga
        if (isCharging) {
            chargeWave += deltaTime * 3f;
            if (chargeWave > Math.PI * 2) {
                chargeWave -= Math.PI * 2;
            }
        }
    }

    @Override
    public void draw() {
        if (programId == 0) return;

        updateTexture();

        GLES30.glUseProgram(programId);

        // Blending
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Textura
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(uTextureLoc, 0);

        // Uniforms
        GLES30.glUniform1f(uAlphaLoc, 1.0f);
        GLES30.glUniform1f(uTimeLoc, time);
        GLES30.glUniform1f(uChargingLoc, isCharging ? 1.0f : 0.0f);
        GLES30.glUniform1f(uLevelLoc, batteryLevel / 100f);

        // Vértices
        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        GLES30.glEnableVertexAttribArray(aTexCoordLoc);
        GLES30.glVertexAttribPointer(aTexCoordLoc, 2, GLES30.GL_FLOAT, false, 0, texCoordBuffer);

        // Draw
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        // Cleanup
        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aTexCoordLoc);
    }

    // ═══════════════════════════════════════════════════════════════
    // POSICIÓN
    // ═══════════════════════════════════════════════════════════════

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        setupBuffers();
    }

    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
        setupBuffers();
    }

    // ═══════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════

    public int getBatteryLevel() { return batteryLevel; }
    public boolean isCharging() { return isCharging; }

    // ═══════════════════════════════════════════════════════════════
    // DISPOSE
    // ═══════════════════════════════════════════════════════════════

    public void dispose() {
        unregisterBatteryReceiver();

        if (textureId != -1) {
            int[] textures = { textureId };
            GLES30.glDeleteTextures(1, textures, 0);
            textureId = -1;
        }

        if (programId != 0) {
            GLES30.glDeleteProgram(programId);
            programId = 0;
        }

        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }

        Log.d(TAG, "✓ Battery3D liberado");
    }
}
