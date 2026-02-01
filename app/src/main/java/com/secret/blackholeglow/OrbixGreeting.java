package com.secret.blackholeglow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   🤖 OrbixGreeting - Sistema Completo de Bienvenida               ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * DISEÑO:
 * - "Orbix iA" (arriba)
 * - Botón Play/Pause (centro)
 * - Saludo motivador personalizado (debajo)
 * - Reloj actual HH:MM:SS:mmm
 * - 💓 Reloj de vida (tiempo vivido desde nacimiento)
 * - ⏳ Countdown al próximo cumpleaños
 *
 * Usa OpenGL ES 3.0
 */
public class OrbixGreeting implements SceneObject {
    private static final String TAG = "OrbixGreeting";

    // UserManager para acceso a datos del usuario
    private UserManager userManager;

    // ═══════════════════════════════════════════════════════════════
    // 🎨 SALUDOS MOTIVADORES
    // ═══════════════════════════════════════════════════════════════

    private static final String[] MORNING_GREETINGS = {
        "Buenos días", "Feliz mañana", "Buen despertar",
        "Arriba y a brillar", "El sol te saluda", "Nuevo día, nuevas metas"
    };

    private static final String[] AFTERNOON_GREETINGS = {
        "Buenas tardes", "Feliz tarde", "Sigue adelante",
        "Energía de tarde", "Lo estás haciendo genial", "Que siga bien tu día"
    };

    private static final String[] NIGHT_GREETINGS = {
        "Buenas noches", "Dulces sueños", "Paz nocturna",
        "La luna te guía", "Descansa y recarga", "Noche de estrellas"
    };

    private static final String[] MOTIVATIONAL_GREETINGS = {
        "Eres increíble", "Todo es posible", "Brilla con fuerza",
        "Tu momento es ahora", "Confía en ti", "Hoy será un gran día",
        "Sigue tus sueños", "Eres más fuerte de lo que crees",
        "Cada día es una oportunidad", "El éxito te espera",
        "Nunca te rindas", "Tú puedes con todo", "Cree en ti mismo",
        "La vida es bella", "Sonríe, lo mereces", "Eres único"
    };

    // Estado
    private Context context;
    private boolean isVisible = false;
    private float alpha = 0f;
    private float targetAlpha = 0f;
    private float time = 0f;
    private static final float TIME_CYCLE = 62.831853f;  // 10 * TWO_PI - evita overflow
    private String currentGreeting = "";
    private long lastGreetingChange = 0;
    private static final long GREETING_CHANGE_INTERVAL = 60000;

    // Texturas
    private int titleTextureId = 0;
    private int greetingTextureId = 0;
    private int clockTextureId = 0;
    private int lifeClockTextureId = 0;
    private int birthdayTextureId = 0;
    private int heartTextureId = 0;

    private Bitmap titleBitmap, greetingBitmap, clockBitmap;
    private Bitmap lifeClockBitmap, birthdayBitmap, heartBitmap;
    private Canvas titleCanvas, greetingCanvas, clockCanvas;
    private Canvas lifeClockCanvas, birthdayCanvas, heartCanvas;

    // Dimensiones de textura
    private static final int TITLE_TEX_WIDTH = 512;
    private static final int TITLE_TEX_HEIGHT = 64;
    private static final int GREETING_TEX_WIDTH = 768;
    private static final int GREETING_TEX_HEIGHT = 56;
    private static final int CLOCK_TEX_WIDTH = 512;
    private static final int CLOCK_TEX_HEIGHT = 48;
    private static final int LIFE_CLOCK_TEX_WIDTH = 600;
    private static final int LIFE_CLOCK_TEX_HEIGHT = 48;
    private static final int BIRTHDAY_TEX_WIDTH = 512;
    private static final int BIRTHDAY_TEX_HEIGHT = 40;
    private static final int HEART_TEX_SIZE = 64;

    // OpenGL
    private int shaderProgram = 0;
    private int aPositionLoc = -1;
    private int aTexCoordLoc = -1;
    private int uTextureLoc = -1;
    private int uAlphaLoc = -1;
    private int uTimeLoc = -1;
    private int uTypeLoc = -1;
    private FloatBuffer vertexBuffer;

    // ⚡ OPTIMIZACIÓN: VBO para evitar uploads cada frame
    private int vboId = 0;
    private boolean vboInitialized = false;

    // ⚡ OPTIMIZACIÓN: Cache de vertices para evitar allocations
    private final float[] vertexCache = new float[16];

    // ⚡ OPTIMIZACIÓN: Reducir updates de texturas
    private long lastClockUpdate = 0;
    private static final long CLOCK_UPDATE_INTERVAL = 100; // ⚡ OPTIMIZADO: 10fps para reloj (no necesita ms precision)

    // ⚡ OPTIMIZACIÓN CRÍTICA: Cache de objetos para evitar allocations en update()
    // Estos objetos se crean UNA vez y se reutilizan
    private final Calendar calendarCache = Calendar.getInstance();
    private final Calendar birthdayCalendarCache = Calendar.getInstance();  // Para cálculo de cumpleaños
    private final StringBuilder clockStringBuilder = new StringBuilder(20);
    private final StringBuilder lifeStringBuilder = new StringBuilder(30);
    private final StringBuilder birthdayStringBuilder = new StringBuilder(30);

    // ⚡ OPTIMIZACIÓN: Paint objects cacheados (NO crear en cada update)
    private Paint clockPaintCache;
    private Paint clockGlowPaintCache;
    private Paint lifePaintCache;
    private Paint lifeGlowPaintCache;
    private Paint bdPaintCache;
    private Paint bdGlowPaintCache;

    // Posiciones Y (coordenadas normalizadas -1 a 1)
    // AJUSTADO: Subir todos los elementos para no tapar controles del launcher
    private float titleY = 0.65f;       // "Orbix iA" arriba
    private float greetingY = -0.28f;   // Saludo debajo del botón (subido)
    private float clockY = -0.38f;      // Reloj actual (subido)
    private float lifeClockY = -0.48f;  // Reloj de vida (subido)
    private float birthdayY = -0.58f;   // Countdown cumpleaños (subido)
    private float heartY = -0.48f;      // Corazón junto al reloj de vida
    private float heartX = -0.38f;      // A la izquierda del reloj de vida

    private float aspectRatio = 1.0f;
    private Random random = new Random();
    private boolean needsTitleUpdate = true;
    private boolean needsGreetingUpdate = true;
    private boolean needsHeartUpdate = true;
    private String lastClockText = "";
    private String lastLifeClockText = "";
    private String lastBirthdayText = "";

    // Gemini AI para saludos inteligentes
    private GeminiService geminiService;
    private AtomicBoolean isLoadingSmartGreeting = new AtomicBoolean(false);
    private boolean useSmartGreetings = true;  // Usar Gemini para saludos
    private boolean hasUsedGeminiThisSession = false;  // ⚡ Solo usar Gemini una vez por sesión

    // Animación del corazón
    private float heartScale = 1.0f;
    private float heartBeat = 0f;

    public OrbixGreeting(Context context) {
        this.context = context;
        if (context != null) {
            this.userManager = UserManager.getInstance(context);
        }
        loadUserData();
        initBitmaps();
        initOpenGL();
        initGemini();
        generateNewGreeting();
    }

    /**
     * Inicializa el servicio de Gemini AI
     */
    private void initGemini() {
        geminiService = GeminiService.getInstance();
        Log.d(TAG, "🤖 Gemini AI inicializado para saludos inteligentes");
    }

    // Constructor sin context (fallback)
    public OrbixGreeting() {
        this.context = null;
        this.userManager = null;
        initBitmaps();
        initOpenGL();
        generateNewGreeting();
    }

    /**
     * Carga datos del usuario desde UserManager
     */
    private void loadUserData() {
        if (userManager == null) {
            Log.w(TAG, "UserManager no disponible");
            return;
        }
        Log.d(TAG, "👤 UserManager disponible, datos cargados");
    }

    /**
     * Refresca los datos del usuario desde UserManager
     * Útil para actualizar después de que el usuario configure su fecha de nacimiento
     */
    public void refreshUserData() {
        loadUserData();
        needsGreetingUpdate = true;
        Log.d(TAG, "🔄 Datos de usuario actualizados");
    }

    private void initBitmaps() {
        titleBitmap = Bitmap.createBitmap(TITLE_TEX_WIDTH, TITLE_TEX_HEIGHT, Bitmap.Config.ARGB_8888);
        titleCanvas = new Canvas(titleBitmap);

        greetingBitmap = Bitmap.createBitmap(GREETING_TEX_WIDTH, GREETING_TEX_HEIGHT, Bitmap.Config.ARGB_8888);
        greetingCanvas = new Canvas(greetingBitmap);

        clockBitmap = Bitmap.createBitmap(CLOCK_TEX_WIDTH, CLOCK_TEX_HEIGHT, Bitmap.Config.ARGB_8888);
        clockCanvas = new Canvas(clockBitmap);

        lifeClockBitmap = Bitmap.createBitmap(LIFE_CLOCK_TEX_WIDTH, LIFE_CLOCK_TEX_HEIGHT, Bitmap.Config.ARGB_8888);
        lifeClockCanvas = new Canvas(lifeClockBitmap);

        birthdayBitmap = Bitmap.createBitmap(BIRTHDAY_TEX_WIDTH, BIRTHDAY_TEX_HEIGHT, Bitmap.Config.ARGB_8888);
        birthdayCanvas = new Canvas(birthdayBitmap);

        heartBitmap = Bitmap.createBitmap(HEART_TEX_SIZE, HEART_TEX_SIZE, Bitmap.Config.ARGB_8888);
        heartCanvas = new Canvas(heartBitmap);

        // ⚡ OPTIMIZACIÓN: Inicializar Paint caches UNA sola vez
        initPaintCaches();
    }

    /**
     * ⚡ OPTIMIZACIÓN CRÍTICA: Inicializa los Paint objects una sola vez
     * Estos se reutilizan en cada update en lugar de crear nuevos
     */
    private void initPaintCaches() {
        // Clock paint
        clockPaintCache = new Paint(Paint.ANTI_ALIAS_FLAG);
        clockPaintCache.setColor(0xFFFFFFFF);
        clockPaintCache.setTextSize(28);
        clockPaintCache.setTextAlign(Paint.Align.CENTER);
        clockPaintCache.setTypeface(Typeface.MONOSPACE);

        clockGlowPaintCache = new Paint(clockPaintCache);
        clockGlowPaintCache.setColor(0xFF00FF88);
        clockGlowPaintCache.setMaskFilter(new android.graphics.BlurMaskFilter(4, android.graphics.BlurMaskFilter.Blur.NORMAL));

        // Life clock paint
        lifePaintCache = new Paint(Paint.ANTI_ALIAS_FLAG);
        lifePaintCache.setColor(0xFFFFFFFF);
        lifePaintCache.setTextSize(22);
        lifePaintCache.setTextAlign(Paint.Align.CENTER);
        lifePaintCache.setTypeface(Typeface.MONOSPACE);

        lifeGlowPaintCache = new Paint(lifePaintCache);
        lifeGlowPaintCache.setColor(0xFFFF6688);
        lifeGlowPaintCache.setMaskFilter(new android.graphics.BlurMaskFilter(4, android.graphics.BlurMaskFilter.Blur.NORMAL));

        // Birthday paint
        bdPaintCache = new Paint(Paint.ANTI_ALIAS_FLAG);
        bdPaintCache.setColor(0xFFFFFFFF);
        bdPaintCache.setTextSize(20);
        bdPaintCache.setTextAlign(Paint.Align.CENTER);
        bdPaintCache.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        bdGlowPaintCache = new Paint(bdPaintCache);
        bdGlowPaintCache.setColor(0xFF00FFAA);
        bdGlowPaintCache.setMaskFilter(new android.graphics.BlurMaskFilter(4, android.graphics.BlurMaskFilter.Blur.NORMAL));

        Log.d(TAG, "⚡ Paint caches inicializados");
    }

    private void initOpenGL() {
        String vertexShader =
            "#version 300 es\n" +
            "in vec2 a_Position;\n" +
            "in vec2 a_TexCoord;\n" +
            "out vec2 v_TexCoord;\n" +
            "void main() {\n" +
            "    v_TexCoord = a_TexCoord;\n" +
            "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
            "}\n";

        String fragmentShader =
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "in vec2 v_TexCoord;\n" +
            "out vec4 fragColor;\n" +
            "uniform sampler2D u_Texture;\n" +
            "uniform float u_Alpha;\n" +
            "uniform float u_Time;\n" +
            "uniform float u_Type;\n" +
            "\n" +
            "void main() {\n" +
            "    vec4 texColor = texture(u_Texture, v_TexCoord);\n" +
            "    float pulse = sin(u_Time * 2.0) * 0.1 + 0.9;\n" +
            "    float colorShift = sin(v_TexCoord.x * 3.14159 + u_Time) * 0.5 + 0.5;\n" +
            "    \n" +
            "    vec3 color1 = vec3(0.0, 1.0, 1.0);\n" +   // Cyan
            "    vec3 color2 = vec3(1.0, 0.4, 1.0);\n" +   // Magenta
            "    vec3 color3 = vec3(0.4, 1.0, 0.6);\n" +   // Verde
            "    vec3 color4 = vec3(1.0, 0.3, 0.4);\n" +   // Rojo (corazón)
            "    \n" +
            "    vec3 gradientColor;\n" +
            "    if (u_Type < 0.5) {\n" +
            "        gradientColor = mix(color1, color2, colorShift);\n" +  // Título
            "    } else if (u_Type < 1.5) {\n" +
            "        gradientColor = mix(color2, color1, colorShift);\n" +  // Saludo
            "    } else if (u_Type < 2.5) {\n" +
            "        gradientColor = mix(color3, color1, colorShift);\n" +  // Reloj
            "        pulse = sin(u_Time * 4.0) * 0.08 + 0.92;\n" +
            "    } else if (u_Type < 3.5) {\n" +
            "        gradientColor = mix(color4, vec3(1.0,0.6,0.7), colorShift);\n" +  // Reloj de vida
            "        pulse = sin(u_Time * 2.5) * 0.1 + 0.9;\n" +
            "    } else if (u_Type < 4.5) {\n" +
            "        gradientColor = mix(color1, color3, colorShift);\n" +  // Cumpleaños
            "    } else {\n" +
            "        // Corazón - pulso más intenso\n" +
            "        float heartPulse = sin(u_Time * 5.0) * 0.3 + 0.7;\n" +
            "        gradientColor = color4 * (heartPulse + 0.3);\n" +
            "        pulse = 1.0;\n" +
            "    }\n" +
            "    \n" +
            "    vec3 finalColor = gradientColor * texColor.rgb * pulse;\n" +
            "    finalColor += gradientColor * texColor.a * 0.2;\n" +
            "    fragColor = vec4(finalColor, texColor.a * u_Alpha);\n" +
            "}\n";

        int vs = compileShader(GLES30.GL_VERTEX_SHADER, vertexShader);
        int fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShader);

        if (vs == 0 || fs == 0) {
            Log.e(TAG, "Error compilando shaders GL3.0");
            return;
        }

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(shaderProgram, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Error linking: " + GLES30.glGetProgramInfoLog(shaderProgram));
            return;
        }

        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        aTexCoordLoc = GLES30.glGetAttribLocation(shaderProgram, "a_TexCoord");
        uTextureLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Texture");
        uAlphaLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Alpha");
        uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Time");
        uTypeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Type");

        float[] vertices = {
            -1.0f, -1.0f,   0.0f, 1.0f,
             1.0f, -1.0f,   1.0f, 1.0f,
            -1.0f,  1.0f,   0.0f, 0.0f,
             1.0f,  1.0f,   1.0f, 0.0f
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // Crear texturas
        int[] textures = new int[6];
        GLES30.glGenTextures(6, textures, 0);
        titleTextureId = textures[0];
        greetingTextureId = textures[1];
        clockTextureId = textures[2];
        lifeClockTextureId = textures[3];
        birthdayTextureId = textures[4];
        heartTextureId = textures[5];

        for (int tex : textures) {
            setupTexture(tex);
        }

        Log.d(TAG, "🤖 OrbixGreeting GL3.0 inicializado");
    }

    private void setupTexture(int textureId) {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
    }

    private int compileShader(int type, String code) {
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

    public void generateNewGreeting() {
        // ⚡ OPTIMIZACIÓN: Solo usar Gemini UNA VEZ por sesión (al inicio)
        // Después usar saludos locales para no hacer llamadas de red constantes
        if (useSmartGreetings && geminiService != null && !hasUsedGeminiThisSession) {
            if (!isLoadingSmartGreeting.get()) {
                hasUsedGeminiThisSession = true;
                generateSmartGreeting();
            }
            return;
        }

        // Saludos locales (rápidos, sin red)
        generateLocalGreeting();
    }

    /**
     * Genera un saludo usando Gemini AI
     */
    private void generateSmartGreeting() {
        if (isLoadingSmartGreeting.getAndSet(true)) return;

        Log.d(TAG, "🤖 Solicitando saludo inteligente a Gemini...");
        currentGreeting = "Pensando...";
        needsGreetingUpdate = true;

        geminiService.generateSmartGreeting(new GeminiService.GeminiCallback() {
            @Override
            public void onResponse(String response) {
                // Limpiar respuesta (quitar comillas si las hay)
                String cleanResponse = response.replace("\"", "").trim();
                if (cleanResponse.length() > 50) {
                    cleanResponse = cleanResponse.substring(0, 47) + "...";
                }
                currentGreeting = cleanResponse;
                needsGreetingUpdate = true;
                lastGreetingChange = System.currentTimeMillis();
                isLoadingSmartGreeting.set(false);
                Log.d(TAG, "🤖 Saludo Gemini: " + currentGreeting);
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "⚠ Gemini error: " + error + ", usando saludo local");
                isLoadingSmartGreeting.set(false);
                generateLocalGreeting();
            }
        });
    }

    /**
     * Genera un saludo local (sin internet)
     */
    private void generateLocalGreeting() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        // 40% probabilidad de saludo motivador
        if (random.nextFloat() < 0.4f) {
            currentGreeting = MOTIVATIONAL_GREETINGS[random.nextInt(MOTIVATIONAL_GREETINGS.length)];
        } else {
            String[] greetings;
            if (hour >= 6 && hour < 12) {
                greetings = MORNING_GREETINGS;
            } else if (hour >= 12 && hour < 19) {
                greetings = AFTERNOON_GREETINGS;
            } else {
                greetings = NIGHT_GREETINGS;
            }
            currentGreeting = greetings[random.nextInt(greetings.length)];
        }

        needsGreetingUpdate = true;
        lastGreetingChange = System.currentTimeMillis();
        Log.d(TAG, "🎯 Saludo local: " + currentGreeting);
    }

    private void updateTitleTexture() {
        if (!needsTitleUpdate || titleBitmap == null) return;

        titleBitmap.eraseColor(0x00000000);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(34);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint glowPaint = new Paint(textPaint);
        glowPaint.setColor(0xFF00FFFF);
        glowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(6, android.graphics.BlurMaskFilter.Blur.NORMAL));

        float centerX = TITLE_TEX_WIDTH / 2f;
        float centerY = TITLE_TEX_HEIGHT / 2f + 12;

        titleCanvas.drawText("Orbix iA", centerX, centerY, glowPaint);
        titleCanvas.drawText("Orbix iA", centerX, centerY, textPaint);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, titleTextureId);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, titleBitmap, 0);

        needsTitleUpdate = false;
    }

    private void updateGreetingTexture() {
        if (!needsGreetingUpdate || greetingBitmap == null) return;

        greetingBitmap.eraseColor(0x00000000);

        String greetingText = currentGreeting;

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(26);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        float textWidth = textPaint.measureText(greetingText);
        float maxWidth = GREETING_TEX_WIDTH - 30;
        if (textWidth > maxWidth) {
            textPaint.setTextSize(26 * (maxWidth / textWidth));
        }

        Paint glowPaint = new Paint(textPaint);
        glowPaint.setColor(0xFFFF66FF);
        glowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(5, android.graphics.BlurMaskFilter.Blur.NORMAL));

        float centerX = GREETING_TEX_WIDTH / 2f;
        float centerY = GREETING_TEX_HEIGHT / 2f + 9;

        greetingCanvas.drawText(greetingText, centerX, centerY, glowPaint);
        greetingCanvas.drawText(greetingText, centerX, centerY, textPaint);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, greetingTextureId);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, greetingBitmap, 0);

        needsGreetingUpdate = false;
    }

    private void updateClockTexture() {
        if (clockBitmap == null || clockPaintCache == null) return;

        // ⚡ OPTIMIZACIÓN: Limitar updates a 10fps (100ms) - no necesita precisión de ms
        long now = System.currentTimeMillis();
        if (now - lastClockUpdate < CLOCK_UPDATE_INTERVAL) return;
        lastClockUpdate = now;

        // ⚡ OPTIMIZACIÓN: Reutilizar Calendar en lugar de crear uno nuevo
        calendarCache.setTimeInMillis(now);

        // ⚡ OPTIMIZACIÓN: Usar StringBuilder en lugar de String.format
        clockStringBuilder.setLength(0);
        int hour = calendarCache.get(Calendar.HOUR_OF_DAY);
        int minute = calendarCache.get(Calendar.MINUTE);
        int second = calendarCache.get(Calendar.SECOND);

        if (hour < 10) clockStringBuilder.append('0');
        clockStringBuilder.append(hour).append(':');
        if (minute < 10) clockStringBuilder.append('0');
        clockStringBuilder.append(minute).append(':');
        if (second < 10) clockStringBuilder.append('0');
        clockStringBuilder.append(second);

        String clockText = clockStringBuilder.toString();
        if (clockText.equals(lastClockText)) return;
        lastClockText = clockText;

        clockBitmap.eraseColor(0x00000000);

        // ⚡ OPTIMIZACIÓN: Usar Paint cacheados
        float centerX = CLOCK_TEX_WIDTH / 2f;
        float centerY = CLOCK_TEX_HEIGHT / 2f + 10;

        clockCanvas.drawText(clockText, centerX, centerY, clockGlowPaintCache);
        clockCanvas.drawText(clockText, centerX, centerY, clockPaintCache);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, clockTextureId);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, clockBitmap, 0);
    }

    // Funcionalidad de reloj de vida y cumpleaños eliminada por privacidad
    private void updateLifeClockTexture() { }
    private void updateBirthdayTexture() { }

    private void updateHeartTexture() {
        if (!needsHeartUpdate || heartBitmap == null) return;

        heartBitmap.eraseColor(0x00000000);

        Paint heartPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        heartPaint.setColor(0xFFFF4466);
        heartPaint.setTextSize(48);
        heartPaint.setTextAlign(Paint.Align.CENTER);

        Paint glowPaint = new Paint(heartPaint);
        glowPaint.setColor(0xFFFF2244);
        glowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(8, android.graphics.BlurMaskFilter.Blur.NORMAL));

        float centerX = HEART_TEX_SIZE / 2f;
        float centerY = HEART_TEX_SIZE / 2f + 16;

        heartCanvas.drawText("💓", centerX, centerY, glowPaint);
        heartCanvas.drawText("💓", centerX, centerY, heartPaint);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, heartTextureId);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, heartBitmap, 0);

        needsHeartUpdate = false;
    }

    @Override
    public void update(float dt) {
        // ⚡ CRÍTICO: Mantener time acotado para evitar pérdida de precisión en GPU
        time += dt;
        if (time > TIME_CYCLE) {
            time -= TIME_CYCLE;
        }

        // Animación del corazón (latido)
        heartBeat += dt * 5.0f;
        if (heartBeat > TIME_CYCLE) {
            heartBeat -= TIME_CYCLE;
        }
        heartScale = 1.0f + (float)Math.sin(heartBeat) * 0.15f;

        float alphaSpeed = 4.0f;
        if (alpha < targetAlpha) {
            alpha = Math.min(alpha + dt * alphaSpeed, targetAlpha);
        } else if (alpha > targetAlpha) {
            alpha = Math.max(alpha - dt * alphaSpeed, targetAlpha);
        }

        if (isVisible && System.currentTimeMillis() - lastGreetingChange > GREETING_CHANGE_INTERVAL) {
            generateNewGreeting();
        }
    }

    @Override
    public void draw() {
        if (alpha <= 0.01f || shaderProgram == 0) return;

        // ⚡ OPTIMIZACIÓN: Solo actualizar texturas si es necesario
        updateTitleTexture();

        // ⚡ OPTIMIZACIÓN: Configurar estado GL una sola vez
        GLES30.glUseProgram(shaderProgram);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // ⚡ OPTIMIZACIÓN: Habilitar vertex attribs UNA vez
        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glEnableVertexAttribArray(aTexCoordLoc);

        // ⚡ OPTIMIZACIÓN: Establecer uniforms comunes UNA vez
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glUniform1i(uTextureLoc, 0);
        GLES30.glUniform1f(uAlphaLoc, alpha);
        GLES30.glUniform1f(uTimeLoc, time);

        // Título "Orbix iA" arriba
        drawTextQuad(titleTextureId, titleY, 0.38f, 0.05f, 0.0f);

        // Saludo de Gemini DESHABILITADO permanentemente (solo relojes)
        // drawTextQuad(greetingTextureId, greetingY, 0.6f, 0.045f, 1.0f);

        // Reloj actual
        // drawTextQuad(clockTextureId, clockY, 0.42f, 0.04f, 2.0f);


        // ⚡ OPTIMIZACIÓN: Deshabilitar vertex attribs UNA vez al final
        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aTexCoordLoc);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        GLES30.glUseProgram(0);
    }

    private void drawTextQuad(int textureId, float posY, float width, float height, float type) {
        float halfWidth = width / aspectRatio;
        float halfHeight = height;

        // ⚡ OPTIMIZACIÓN: Usar cache en lugar de crear nuevo array
        vertexCache[0] = -halfWidth; vertexCache[1] = posY - halfHeight; vertexCache[2] = 0.0f; vertexCache[3] = 1.0f;
        vertexCache[4] = halfWidth;  vertexCache[5] = posY - halfHeight; vertexCache[6] = 1.0f; vertexCache[7] = 1.0f;
        vertexCache[8] = -halfWidth; vertexCache[9] = posY + halfHeight; vertexCache[10] = 0.0f; vertexCache[11] = 0.0f;
        vertexCache[12] = halfWidth; vertexCache[13] = posY + halfHeight; vertexCache[14] = 1.0f; vertexCache[15] = 0.0f;

        vertexBuffer.clear();
        vertexBuffer.put(vertexCache);
        vertexBuffer.position(0);

        vertexBuffer.position(0);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);

        vertexBuffer.position(2);
        GLES30.glVertexAttribPointer(aTexCoordLoc, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1f(uTypeLoc, type);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
    }

    private void drawHeartQuad(int textureId, float posY, float posX, float size, float type) {
        float halfSize = size / aspectRatio;

        // ⚡ OPTIMIZACIÓN: Usar cache en lugar de crear nuevo array
        vertexCache[0] = posX - halfSize; vertexCache[1] = posY - size; vertexCache[2] = 0.0f; vertexCache[3] = 1.0f;
        vertexCache[4] = posX + halfSize; vertexCache[5] = posY - size; vertexCache[6] = 1.0f; vertexCache[7] = 1.0f;
        vertexCache[8] = posX - halfSize; vertexCache[9] = posY + size; vertexCache[10] = 0.0f; vertexCache[11] = 0.0f;
        vertexCache[12] = posX + halfSize; vertexCache[13] = posY + size; vertexCache[14] = 1.0f; vertexCache[15] = 0.0f;

        vertexBuffer.clear();
        vertexBuffer.put(vertexCache);
        vertexBuffer.position(0);

        vertexBuffer.position(0);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);

        vertexBuffer.position(2);
        GLES30.glVertexAttribPointer(aTexCoordLoc, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1f(uTypeLoc, type);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
    }

    public void show() {
        isVisible = true;
        targetAlpha = 1.0f;
        generateNewGreeting();
    }

    public void hide() {
        isVisible = false;
        targetAlpha = 0.0f;
    }

    /**
     * ⚡ Resetea el tiempo interno - llamar cuando cambia de estado de visibilidad
     */
    public void resetTime() {
        time = 0f;
        heartBeat = 0f;
    }

    public void setAspectRatio(float aspect) {
        this.aspectRatio = aspect;
    }

    public boolean isVisible() {
        return isVisible && alpha > 0.01f;
    }

    public void dispose() {
        int[] textures = {titleTextureId, greetingTextureId, clockTextureId,
                         lifeClockTextureId, birthdayTextureId, heartTextureId};
        GLES30.glDeleteTextures(6, textures, 0);

        if (titleBitmap != null) titleBitmap.recycle();
        if (greetingBitmap != null) greetingBitmap.recycle();
        if (clockBitmap != null) clockBitmap.recycle();
        if (lifeClockBitmap != null) lifeClockBitmap.recycle();
        if (birthdayBitmap != null) birthdayBitmap.recycle();
        if (heartBitmap != null) heartBitmap.recycle();

        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
        }
    }
}
