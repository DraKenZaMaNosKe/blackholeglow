package com.secret.blackholeglow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.secret.blackholeglow.R;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   📊 LoadingBar - Barra de Progreso Visual con Glow              ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * Barra de carga elegante con:
 * - Efecto de glow pulsante
 * - Gradiente de colores (cyan → magenta)
 * - Animación suave del progreso
 * - Texto de estado opcional
 *
 * Usa OpenGL ES 2.0 para máxima compatibilidad
 */
public class LoadingBar implements SceneObject {
    private static final String TAG = "LoadingBar";

    // Estado
    // 🔧 FIX THREADING: volatile porque background thread escribe (setProgress desde ResourcePreloader)
    // y GL thread lee (update en onDrawFrame). Sin volatile, GL thread puede cachear el valor
    // y nunca ver el progreso actualizado → loading bar nunca completa.
    private float currentProgress = 0f;      // 0.0 - 1.0
    private volatile float targetProgress = 0f;       // Progreso objetivo (para animación suave)
    private float displayProgress = 0f;      // Progreso mostrado (animado) - solo GL thread
    private volatile boolean isVisible = false;
    private volatile boolean isComplete = false;
    private float alpha = 0f;
    private float time = 0f;
    private static final float TIME_CYCLE = 62.83f;  // 2π * 10, evita overflow

    // Callback cuando termina la carga
    private OnLoadingCompleteListener completeListener;

    // Dimensiones (coordenadas normalizadas -1 a 1)
    private float barY = -0.15f;             // Posición Y (debajo del botón play)
    private float barWidth = 0.7f;           // Ancho total (70% de pantalla)
    private float barHeight = 0.025f;        // Alto de la barra
    private float aspectRatio = 1.0f;

    // OpenGL
    private int shaderProgram = 0;
    private int aPositionLoc = -1;
    private int uColorLoc = -1;
    private int uAlphaLoc = -1;
    private int uTimeLoc = -1;
    private int uProgressLoc = -1;
    private int uIsGlowLoc = -1;
    private FloatBuffer vertexBuffer;

    // Colores dinamicos (se actualizan segun el wallpaper)
    private float[] themeColorPrimary = {0.0f, 0.83f, 1.0f};   // Cyan Orbix
    private float[] themeColorSecondary = {1.0f, 0.84f, 0.0f}; // Dorado Orbix
    private static final float[] COLOR_BG = {0.04f, 0.055f, 0.10f};  // Fondo oscuro
    private static final float[] COLOR_WHITE = {1f, 1f, 1f};

    // Datos del wallpaper seleccionado
    private String wallpaperDisplayName = "";
    private int wallpaperGlowColor = 0xFF00D4FF;

    // ═══════════════════════════════════════════════════════════════════════════
    // TEXTO "Loading..." animado
    // ═══════════════════════════════════════════════════════════════════════════
    private int textShaderProgram = 0;
    private int textTextureId = 0;
    private Bitmap textBitmap;
    private Canvas textCanvas;
    private Paint titlePaint;       // Nombre del wallpaper (grande)
    private Paint titleGlowPaint;    // Glow del titulo
    private Paint textPaint;         // "Cargando..." (mediano)
    private Paint textGlowPaint;
    private Paint reassuringPaint;   // Texto motivacional (pequeno)
    private int textAPositionLoc = -1;
    private int textATexCoordLoc = -1;
    private int textUTextureLoc = -1;
    private int textUAlphaLoc = -1;
    private FloatBuffer textVertexBuffer;

    // ⚡ OPTIMIZACIÓN: Cache de vértices para evitar crear arrays cada frame
    private final float[] quadVertexCache = new float[8];
    private final float[] progressColorCache = new float[3];  // 🔧 FIX: Cache para color de progreso

    private static final int TEXT_TEX_WIDTH = 512;   // Mas ancho para nombres largos
    private static final int TEXT_TEX_HEIGHT = 140;  // 3 lineas: nombre + cargando + motivacional
    private int currentDots = 0;           // 0, 1, 2, 3 para "Loading", "Loading.", "Loading..", "Loading..."
    private float dotAnimTimer = 0f;
    private static final float DOT_ANIM_SPEED = 0.4f;  // segundos por punto

    // Texto del recurso actual (opcional)
    private String currentResourceName = null;

    // ═══════════════════════════════════════════════════════════════════════════
    // 🖼️ FONDO DE IMAGEN (Bosque Navideño)
    // ═══════════════════════════════════════════════════════════════════════════
    private int bgShaderProgram = 0;
    private int bgTextureId = 0;
    private int bgAPositionLoc = -1;
    private int bgATexCoordLoc = -1;
    private int bgUTextureLoc = -1;
    private int bgUAlphaLoc = -1;
    private int bgUDarkenLoc = -1;
    private FloatBuffer bgVertexBuffer;
    private Context context;
    private int backgroundResourceId = 0;
    private boolean backgroundLoaded = false;
    private float bgDarkenAmount = 0.3f;  // Oscurecer 30% para que el texto resalte

    // Vertex shader
    private static final String VERTEX_SHADER =
        "attribute vec2 aPosition;\n" +
        "void main() {\n" +
        "    gl_Position = vec4(aPosition, 0.0, 1.0);\n" +
        "}\n";

    // Fragment shader con glow y gradiente
    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "uniform vec3 uColor;\n" +
        "uniform float uAlpha;\n" +
        "uniform float uTime;\n" +
        "uniform float uProgress;\n" +
        "uniform float uIsGlow;\n" +
        "\n" +
        "void main() {\n" +
        "    if (uIsGlow > 0.5) {\n" +
        "        // Efecto glow pulsante\n" +
        "        float pulse = 0.5 + 0.5 * sin(uTime * 3.0);\n" +
        "        float glowAlpha = uAlpha * 0.4 * pulse;\n" +
        "        gl_FragColor = vec4(uColor, glowAlpha);\n" +
        "    } else {\n" +
        "        // Barra sólida con brillo\n" +
        "        float shine = 0.9 + 0.1 * sin(uTime * 2.0);\n" +
        "        gl_FragColor = vec4(uColor * shine, uAlpha);\n" +
        "    }\n" +
        "}\n";

    // Shader de texto con textura
    private static final String TEXT_VERTEX_SHADER =
        "attribute vec2 a_Position;\n" +
        "attribute vec2 a_TexCoord;\n" +
        "varying vec2 v_TexCoord;\n" +
        "void main() {\n" +
        "    v_TexCoord = a_TexCoord;\n" +
        "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
        "}\n";

    private static final String TEXT_FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "varying vec2 v_TexCoord;\n" +
        "uniform sampler2D u_Texture;\n" +
        "uniform float u_Alpha;\n" +
        "void main() {\n" +
        "    vec4 texColor = texture2D(u_Texture, v_TexCoord);\n" +
        "    gl_FragColor = vec4(texColor.rgb, texColor.a * u_Alpha);\n" +
        "}\n";

    // 🖼️ Shader para el fondo con oscurecimiento
    private static final String BG_VERTEX_SHADER =
        "attribute vec2 a_Position;\n" +
        "attribute vec2 a_TexCoord;\n" +
        "varying vec2 v_TexCoord;\n" +
        "void main() {\n" +
        "    v_TexCoord = a_TexCoord;\n" +
        "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
        "}\n";

    private static final String BG_FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "varying vec2 v_TexCoord;\n" +
        "uniform sampler2D u_Texture;\n" +
        "uniform float u_Alpha;\n" +
        "uniform float u_Darken;\n" +
        "void main() {\n" +
        "    vec4 texColor = texture2D(u_Texture, v_TexCoord);\n" +
        "    // Oscurecer la imagen para que el texto resalte\n" +
        "    vec3 darkened = texColor.rgb * (1.0 - u_Darken);\n" +
        "    gl_FragColor = vec4(darkened, texColor.a * u_Alpha);\n" +
        "}\n";

    public LoadingBar() {
        initOpenGL();
        initTextOpenGL();
    }

    private void initOpenGL() {
        // Crear shader program
        int vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        if (vertexShader == 0 || fragmentShader == 0) {
            Log.e(TAG, "Error compilando shaders");
            return;
        }

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vertexShader);
        GLES30.glAttachShader(shaderProgram, fragmentShader);
        GLES30.glLinkProgram(shaderProgram);

        // Verificar linkeo
        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(shaderProgram, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Error linkeando programa: " + GLES30.glGetProgramInfoLog(shaderProgram));
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
            return;
        }

        // Obtener locations
        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "aPosition");
        uColorLoc = GLES30.glGetUniformLocation(shaderProgram, "uColor");
        uAlphaLoc = GLES30.glGetUniformLocation(shaderProgram, "uAlpha");
        uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "uTime");
        uProgressLoc = GLES30.glGetUniformLocation(shaderProgram, "uProgress");
        uIsGlowLoc = GLES30.glGetUniformLocation(shaderProgram, "uIsGlow");

        // Crear vertex buffer (lo actualizamos en cada draw)
        ByteBuffer bb = ByteBuffer.allocateDirect(8 * 4);  // 4 vertices * 2 floats * 4 bytes
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();

        // Eliminar shaders (ya están en el programa)
        GLES30.glDeleteShader(vertexShader);
        GLES30.glDeleteShader(fragmentShader);

        Log.d(TAG, "✓ LoadingBar OpenGL inicializado");
    }

    private void initTextOpenGL() {
        // Crear bitmap y canvas para el texto
        textBitmap = Bitmap.createBitmap(TEXT_TEX_WIDTH, TEXT_TEX_HEIGHT, Bitmap.Config.ARGB_8888);
        textCanvas = new Canvas(textBitmap);

        // === TITULO: Nombre del wallpaper (grande, bold) ===
        titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setTextSize(42);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setColor(0xFFFFFFFF);  // Blanco brillante

        titleGlowPaint = new Paint(titlePaint);
        titleGlowPaint.setColor(wallpaperGlowColor);
        titleGlowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(12, android.graphics.BlurMaskFilter.Blur.NORMAL));

        // === "Cargando..." (mediano) ===
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(28);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(wallpaperGlowColor);

        textGlowPaint = new Paint(textPaint);
        textGlowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(6, android.graphics.BlurMaskFilter.Blur.NORMAL));

        // === Texto motivacional (pequeno, italic) ===
        reassuringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        reassuringPaint.setTextSize(18);
        reassuringPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        reassuringPaint.setTextAlign(Paint.Align.CENTER);
        reassuringPaint.setColor(0xAAB8C5D6);  // Gris claro semi-transparente

        // Crear shader program para texto
        int vs = compileShader(GLES30.GL_VERTEX_SHADER, TEXT_VERTEX_SHADER);
        int fs = compileShader(GLES30.GL_FRAGMENT_SHADER, TEXT_FRAGMENT_SHADER);
        if (vs == 0 || fs == 0) {
            Log.e(TAG, "Error compilando shaders de texto");
            return;
        }

        textShaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(textShaderProgram, vs);
        GLES30.glAttachShader(textShaderProgram, fs);
        GLES30.glLinkProgram(textShaderProgram);

        // Obtener locations
        textAPositionLoc = GLES30.glGetAttribLocation(textShaderProgram, "a_Position");
        textATexCoordLoc = GLES30.glGetAttribLocation(textShaderProgram, "a_TexCoord");
        textUTextureLoc = GLES30.glGetUniformLocation(textShaderProgram, "u_Texture");
        textUAlphaLoc = GLES30.glGetUniformLocation(textShaderProgram, "u_Alpha");

        // Vertex buffer para texto (pos + texcoord interleaved)
        // 4 vértices * 4 floats (x, y, u, v) * 4 bytes
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * 4 * 4);
        bb.order(ByteOrder.nativeOrder());
        textVertexBuffer = bb.asFloatBuffer();

        // Crear textura
        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        textTextureId = textures[0];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textTextureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        // Eliminar shaders
        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        // Renderizar texto inicial
        updateTextTexture();

        Log.d(TAG, "✓ LoadingBar texto inicializado");
    }

    /**
     * 🖼️ Configura el fondo de imagen para la pantalla de carga
     * @param ctx Context de la aplicación
     * @param resourceId ID del drawable (ej: R.drawable.christmas_background)
     */
    public void setBackgroundImage(Context ctx, int resourceId) {
        this.context = ctx;
        this.backgroundResourceId = resourceId;
        this.backgroundLoaded = false;
        Log.d(TAG, "🖼️ Fondo configurado: " + resourceId);
    }

    /**
     * 🖼️ Configura el fondo automáticamente según el nombre de la escena/wallpaper
     * @param ctx Context de la aplicación
     * @param sceneName Nombre de la escena (ej: "Bosque Navideño", "Batalla Cósmica")
     */
    public void setBackgroundForScene(Context ctx, String sceneName) {
        this.context = ctx;

        // Mapeo de escenas a fondos
        // Nota: christmas_background ahora se descarga de Supabase, usar placeholder
        if (sceneName == null) {
            this.backgroundResourceId = R.drawable.preview_oceano_sc;  // Default
        } else if (sceneName.contains("Navide") || sceneName.contains("Christmas")) {
            this.backgroundResourceId = R.drawable.preview_oceano_sc;  // Placeholder (imagen real se descarga)
        } else if (sceneName.contains("Batalla") || sceneName.contains("Universo") || sceneName.contains("Cósmica")) {
            this.backgroundResourceId = R.drawable.preview_oceano_sc;
        } else if (sceneName.contains("Ocean") || sceneName.contains("Pearl")) {
            this.backgroundResourceId = R.drawable.preview_oceano_sc;  // TODO: agregar fondo de océano
        } else {
            this.backgroundResourceId = R.drawable.preview_oceano_sc;  // Default
        }

        this.backgroundLoaded = false;
        Log.d(TAG, "🖼️ Fondo para '" + sceneName + "': " + backgroundResourceId);
    }

    /**
     * 🖼️ Inicializa los recursos OpenGL del fondo (llamar desde GL thread)
     */
    private void initBackgroundOpenGL() {
        if (context == null || backgroundResourceId == 0 || backgroundLoaded) return;

        // Crear shader program
        int vs = compileShader(GLES30.GL_VERTEX_SHADER, BG_VERTEX_SHADER);
        int fs = compileShader(GLES30.GL_FRAGMENT_SHADER, BG_FRAGMENT_SHADER);
        if (vs == 0 || fs == 0) {
            Log.e(TAG, "Error compilando shaders de fondo");
            return;
        }

        bgShaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(bgShaderProgram, vs);
        GLES30.glAttachShader(bgShaderProgram, fs);
        GLES30.glLinkProgram(bgShaderProgram);

        // Obtener locations
        bgAPositionLoc = GLES30.glGetAttribLocation(bgShaderProgram, "a_Position");
        bgATexCoordLoc = GLES30.glGetAttribLocation(bgShaderProgram, "a_TexCoord");
        bgUTextureLoc = GLES30.glGetUniformLocation(bgShaderProgram, "u_Texture");
        bgUAlphaLoc = GLES30.glGetUniformLocation(bgShaderProgram, "u_Alpha");
        bgUDarkenLoc = GLES30.glGetUniformLocation(bgShaderProgram, "u_Darken");

        // Vertex buffer para fullscreen quad (pos + texcoord)
        float[] vertices = {
            -1f, -1f,  0f, 1f,   // Bottom-left
             1f, -1f,  1f, 1f,   // Bottom-right
            -1f,  1f,  0f, 0f,   // Top-left
             1f,  1f,  1f, 0f    // Top-right
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        bgVertexBuffer = bb.asFloatBuffer();
        bgVertexBuffer.put(vertices);
        bgVertexBuffer.position(0);

        // Cargar textura del fondo
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), backgroundResourceId, options);

        if (bitmap != null) {
            int[] textures = new int[1];
            GLES30.glGenTextures(1, textures, 0);
            bgTextureId = textures[0];

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bgTextureId);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();

            Log.d(TAG, "✅ Fondo cargado: textureId=" + bgTextureId);
        } else {
            Log.e(TAG, "❌ Error cargando bitmap del fondo");
        }

        // Eliminar shaders
        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        backgroundLoaded = true;
    }

    /**
     * 🖼️ Dibuja el fondo de imagen
     */
    private void drawBackground() {
        if (!backgroundLoaded || bgShaderProgram == 0 || bgTextureId == 0) return;

        GLES30.glUseProgram(bgShaderProgram);

        // Uniforms
        GLES30.glUniform1f(bgUAlphaLoc, alpha);
        GLES30.glUniform1f(bgUDarkenLoc, bgDarkenAmount);

        // Textura
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bgTextureId);
        GLES30.glUniform1i(bgUTextureLoc, 0);

        // Vertex attributes
        GLES30.glEnableVertexAttribArray(bgAPositionLoc);
        GLES30.glEnableVertexAttribArray(bgATexCoordLoc);

        bgVertexBuffer.position(0);
        GLES30.glVertexAttribPointer(bgAPositionLoc, 2, GLES30.GL_FLOAT, false, 16, bgVertexBuffer);
        bgVertexBuffer.position(2);
        GLES30.glVertexAttribPointer(bgATexCoordLoc, 2, GLES30.GL_FLOAT, false, 16, bgVertexBuffer);

        // Dibujar fullscreen quad
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(bgAPositionLoc);
        GLES30.glDisableVertexAttribArray(bgATexCoordLoc);
    }

    /**
     * Actualiza la textura del texto con nombre del wallpaper y "Cargando..."
     */
    private void updateTextTexture() {
        if (textBitmap == null || textCanvas == null) return;

        textBitmap.eraseColor(Color.TRANSPARENT);

        // Construir puntos animados
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < currentDots; i++) {
            dots.append(".");
        }

        float centerX = TEXT_TEX_WIDTH / 2f;
        float line1Y = 45f;   // Titulo: nombre del wallpaper
        float line2Y = 85f;   // "Cargando..."
        float line3Y = 120f;  // Texto motivacional

        // === LINEA 1: Nombre del wallpaper ===
        String title = wallpaperDisplayName;
        if (title == null || title.isEmpty()) {
            title = "Orbix";
        }
        // Glow del titulo
        if (titleGlowPaint != null) {
            textCanvas.drawText(title, centerX, line1Y, titleGlowPaint);
        }
        // Titulo principal
        if (titlePaint != null) {
            textCanvas.drawText(title, centerX, line1Y, titlePaint);
        }

        // === LINEA 2: Nombre del recurso actual o "Verificando..." ===
        String loadingText;
        if (currentResourceName != null && !currentResourceName.isEmpty()) {
            loadingText = currentResourceName + dots.toString();
        } else {
            loadingText = "Verificando recursos" + dots.toString();
        }
        if (textGlowPaint != null) {
            textCanvas.drawText(loadingText, centerX, line2Y, textGlowPaint);
        }
        if (textPaint != null) {
            textCanvas.drawText(loadingText, centerX, line2Y, textPaint);
        }

        // === LINEA 3: Porcentaje de progreso ===
        int percent = (int)(displayProgress * 100);
        String motivational = percent > 0 ? percent + "% completado" : "Preparando experiencia...";
        if (reassuringPaint != null) {
            textCanvas.drawText(motivational, centerX, line3Y, reassuringPaint);
        }

        // Subir textura a GPU
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textTextureId);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, textBitmap, 0);
    }

    private int compileShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Error compilando shader: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    @Override
    public void update(float dt) {
        time += dt;
        if (time > TIME_CYCLE) time -= TIME_CYCLE;  // Evitar overflow

        // Animación de fade in/out
        if (isVisible && alpha < 1f) {
            alpha = Math.min(1f, alpha + dt * 4f);  // Fade in rápido
        } else if (!isVisible && alpha > 0f) {
            alpha = Math.max(0f, alpha - dt * 4f);  // Fade out rápido
        }

        // Animación suave del progreso
        if (displayProgress < targetProgress) {
            displayProgress = Math.min(targetProgress, displayProgress + dt * 0.8f);  // ~1.25 segundos para llenar
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // Animación de los puntos y actualización del texto
        // ═══════════════════════════════════════════════════════════════════════════
        if (isVisible) {
            dotAnimTimer += dt;
            if (dotAnimTimer >= DOT_ANIM_SPEED) {
                dotAnimTimer = 0f;
                currentDots = (currentDots + 1) % 4;  // 0, 1, 2, 3, 0, 1, 2, 3...
                updateTextTexture();  // Actualiza puntos y porcentaje
            }
        }

        // Detectar cuando llega al 100%
        if (displayProgress >= 0.99f && !isComplete) {
            isComplete = true;
            if (completeListener != null) {
                completeListener.onLoadingComplete();
            }
        }
    }

    @Override
    public void draw() {
        if (alpha <= 0.01f || shaderProgram == 0) return;

        // 🖼️ Inicializar y dibujar fondo (preview del wallpaper)
        if (backgroundResourceId != 0 && !backgroundLoaded) {
            initBackgroundOpenGL();
        }
        if (backgroundLoaded) {
            drawBackground();
        }

        GLES30.glUseProgram(shaderProgram);

        // ⚡ OPTIMIZADO: Habilitar vertex attrib UNA vez para todos los quads
        GLES30.glEnableVertexAttribArray(aPositionLoc);

        // Uniforms comunes
        GLES30.glUniform1f(uTimeLoc, time);
        GLES30.glUniform1f(uProgressLoc, displayProgress);

        // Calcular dimensiones ajustadas por aspect ratio
        float adjustedHeight = barHeight;
        float adjustedWidth = barWidth;
        if (aspectRatio > 1f) {
            adjustedHeight = barHeight * aspectRatio;
        } else {
            adjustedWidth = barWidth / aspectRatio;
        }

        // ═══════════════════════════════════════════════════════════
        // 1. GLOW EXTERIOR (color del tema del wallpaper)
        // ═══════════════════════════════════════════════════════════
        float glowPadding = 0.015f;
        drawQuad(
            -adjustedWidth/2 - glowPadding,
            barY - adjustedHeight - glowPadding,
            adjustedWidth + glowPadding*2,
            adjustedHeight*2 + glowPadding*2,
            themeColorPrimary,
            alpha * 0.3f,
            true
        );

        // ═══════════════════════════════════════════════════════════
        // 2. FONDO DE LA BARRA (gris oscuro)
        // ═══════════════════════════════════════════════════════════
        drawQuad(
            -adjustedWidth/2,
            barY - adjustedHeight,
            adjustedWidth,
            adjustedHeight*2,
            COLOR_BG,
            alpha * 0.9f,
            false
        );

        // ═══════════════════════════════════════════════════════════
        // 3. BARRA DE PROGRESO (gradiente cyan → magenta)
        // ═══════════════════════════════════════════════════════════
        if (displayProgress > 0.01f) {
            float progressWidth = adjustedWidth * displayProgress;

            // 🔧 FIX: Usar cache en lugar de new float[3] cada frame
            for (int i = 0; i < 3; i++) {
                progressColorCache[i] = themeColorPrimary[i] + (themeColorSecondary[i] - themeColorPrimary[i]) * displayProgress;
            }

            drawQuad(
                -adjustedWidth/2,
                barY - adjustedHeight,
                progressWidth,
                adjustedHeight*2,
                progressColorCache,
                alpha,
                false
            );

            // ═══════════════════════════════════════════════════════════
            // 4. BRILLO EN EL BORDE DEL PROGRESO
            // ═══════════════════════════════════════════════════════════
            float edgeX = -adjustedWidth/2 + progressWidth;
            float edgeWidth = 0.02f;
            // ⚡ OPTIMIZADO: Usar COLOR_WHITE cacheado
            drawQuad(
                edgeX - edgeWidth/2,
                barY - adjustedHeight,
                edgeWidth,
                adjustedHeight*2,
                COLOR_WHITE,
                alpha * 0.6f * (0.7f + 0.3f * (float)Math.sin(time * 5f)),
                false
            );
        }

        // ═══════════════════════════════════════════════════════════
        // 5. BORDE EXTERIOR (color del tema)
        // ═══════════════════════════════════════════════════════════
        // Top
        drawQuad(-adjustedWidth/2, barY + adjustedHeight, adjustedWidth, 0.003f, themeColorPrimary, alpha * 0.5f, false);
        // Bottom
        drawQuad(-adjustedWidth/2, barY - adjustedHeight - 0.003f, adjustedWidth, 0.003f, themeColorPrimary, alpha * 0.5f, false);
        // Left
        drawQuad(-adjustedWidth/2 - 0.003f, barY - adjustedHeight, 0.003f, adjustedHeight*2, themeColorPrimary, alpha * 0.5f, false);
        // Right
        drawQuad(adjustedWidth/2, barY - adjustedHeight, 0.003f, adjustedHeight*2, themeColorPrimary, alpha * 0.5f, false);

        // ⚡ OPTIMIZADO: Deshabilitar vertex attrib UNA vez después de todos los quads
        GLES30.glDisableVertexAttribArray(aPositionLoc);

        // ═══════════════════════════════════════════════════════════
        // 6. TEXTO "Loading..." ARRIBA DE LA BARRA
        // ═══════════════════════════════════════════════════════════
        drawText();
    }

    private void drawQuad(float x, float y, float w, float h, float[] color, float quadAlpha, boolean isGlow) {
        // ⚡ OPTIMIZADO: Usar cache en lugar de crear nuevo array cada frame
        quadVertexCache[0] = x;
        quadVertexCache[1] = y;
        quadVertexCache[2] = x + w;
        quadVertexCache[3] = y;
        quadVertexCache[4] = x;
        quadVertexCache[5] = y + h;
        quadVertexCache[6] = x + w;
        quadVertexCache[7] = y + h;

        vertexBuffer.clear();
        vertexBuffer.put(quadVertexCache);
        vertexBuffer.position(0);

        // Uniforms
        GLES30.glUniform3fv(uColorLoc, 1, color, 0);
        GLES30.glUniform1f(uAlphaLoc, quadAlpha);
        GLES30.glUniform1f(uIsGlowLoc, isGlow ? 1f : 0f);

        // ⚡ OPTIMIZADO: vertexAttribPointer sin enable/disable repetitivo
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        // Dibujar
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
    }

    /**
     * Dibuja el texto "Loading..." encima de la barra de progreso
     */
    private void drawText() {
        if (textShaderProgram == 0 || textTextureId == 0) return;

        GLES30.glUseProgram(textShaderProgram);

        // Posición del texto (encima de la barra)
        float textY = barY + 0.06f;   // Un poco arriba de la barra
        float textWidth = 0.35f;      // Ancho del texto
        float textHeight = 0.10f;     // Alto del texto (aumentado para 2 líneas)

        // Ajustar por aspect ratio
        if (aspectRatio < 1f) {
            textWidth = textWidth / aspectRatio;
        }

        // Vertices con posición y texcoord interleaved
        float[] vertices = {
            -textWidth, textY,              0f, 1f,   // Bottom-left
            textWidth,  textY,              1f, 1f,   // Bottom-right
            -textWidth, textY + textHeight, 0f, 0f,   // Top-left
            textWidth,  textY + textHeight, 1f, 0f    // Top-right
        };

        textVertexBuffer.clear();
        textVertexBuffer.put(vertices);
        textVertexBuffer.position(0);

        // Activar textura
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textTextureId);
        GLES30.glUniform1i(textUTextureLoc, 0);
        GLES30.glUniform1f(textUAlphaLoc, alpha);

        // Vertex attributes
        GLES30.glEnableVertexAttribArray(textAPositionLoc);
        GLES30.glEnableVertexAttribArray(textATexCoordLoc);

        textVertexBuffer.position(0);
        GLES30.glVertexAttribPointer(textAPositionLoc, 2, GLES30.GL_FLOAT, false, 16, textVertexBuffer);
        textVertexBuffer.position(2);
        GLES30.glVertexAttribPointer(textATexCoordLoc, 2, GLES30.GL_FLOAT, false, 16, textVertexBuffer);

        // Dibujar
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(textAPositionLoc);
        GLES30.glDisableVertexAttribArray(textATexCoordLoc);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // API Pública
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Muestra la barra de carga
     */
    public void show() {
        isVisible = true;
        isComplete = false;
        displayProgress = 0f;
        targetProgress = 0f;
        Log.d(TAG, "📊 LoadingBar visible");
    }

    /**
     * Oculta la barra de carga
     */
    public void hide() {
        isVisible = false;
        Log.d(TAG, "📊 LoadingBar oculta");
    }

    /**
     * Establece el progreso (0.0 - 1.0)
     * La barra animará suavemente hacia este valor
     */
    public void setProgress(float progress) {
        targetProgress = Math.max(0f, Math.min(1f, progress));
    }

    /**
     * Establece el progreso inmediatamente sin animación
     */
    public void setProgressImmediate(float progress) {
        targetProgress = Math.max(0f, Math.min(1f, progress));
        displayProgress = targetProgress;
    }

    /**
     * @return Progreso actual (0.0 - 1.0)
     */
    public float getProgress() {
        return displayProgress;
    }

    /**
     * @return true si la carga está completa
     */
    public boolean isComplete() {
        return isComplete;
    }

    /**
     * @return true si la barra está visible
     */
    public boolean isVisible() {
        return isVisible && alpha > 0.01f;
    }

    /**
     * Configura el aspect ratio para ajustar proporciones
     */
    public void setAspectRatio(float ratio) {
        this.aspectRatio = ratio;
    }

    /**
     * Configura la posición Y de la barra
     */
    public void setPositionY(float y) {
        this.barY = y;
    }

    /**
     * Resetea la barra para una nueva carga
     */
    public void reset() {
        displayProgress = 0f;
        targetProgress = 0f;
        isComplete = false;
        time = 0f;
        dotAnimTimer = 0f;
        currentDots = 0;
        currentResourceName = null;
        updateTextTexture();
    }

    /**
     * Listener para cuando la carga termina
     */
    public interface OnLoadingCompleteListener {
        void onLoadingComplete();
    }

    public void setOnLoadingCompleteListener(OnLoadingCompleteListener listener) {
        this.completeListener = listener;
    }

    /**
     * Establece el nombre del recurso que se está cargando
     * (para mostrar qué se está cargando)
     */
    public void setResourceName(String name) {
        this.currentResourceName = name;
        updateTextTexture();
    }

    /**
     * Configura el tema segun el wallpaper seleccionado
     * @param displayName Nombre bonito del wallpaper (ej: "ABYSSIA")
     * @param glowColor Color glow del wallpaper (ej: 0xFF00CED1)
     */
    public void setWallpaperTheme(String displayName, int glowColor) {
        this.wallpaperDisplayName = displayName != null ? displayName : "";
        this.wallpaperGlowColor = glowColor;

        // Extraer RGB del color glow y normalizar a 0-1
        float r = ((glowColor >> 16) & 0xFF) / 255f;
        float g = ((glowColor >> 8) & 0xFF) / 255f;
        float b = (glowColor & 0xFF) / 255f;

        themeColorPrimary = new float[]{r, g, b};

        // Color secundario: version mas clara/dorada
        themeColorSecondary = new float[]{
            Math.min(1f, r + 0.3f),
            Math.min(1f, g + 0.2f),
            Math.min(1f, b * 0.5f)
        };

        // Actualizar colores de paint si existen
        if (textPaint != null) {
            textPaint.setColor(glowColor);
        }
        if (titleGlowPaint != null) {
            titleGlowPaint.setColor(glowColor);
        }

        updateTextTexture();
        Log.d(TAG, "Tema configurado: " + displayName + " color=#" + Integer.toHexString(glowColor));
    }

    /**
     * Libera recursos OpenGL
     */
    public void release() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        if (textShaderProgram != 0) {
            GLES30.glDeleteProgram(textShaderProgram);
            textShaderProgram = 0;
        }
        if (textTextureId != 0) {
            int[] textures = {textTextureId};
            GLES30.glDeleteTextures(1, textures, 0);
            textTextureId = 0;
        }
        if (textBitmap != null) {
            textBitmap.recycle();
            textBitmap = null;
        }
        // 🖼️ Liberar recursos del fondo
        if (bgShaderProgram != 0) {
            GLES30.glDeleteProgram(bgShaderProgram);
            bgShaderProgram = 0;
        }
        if (bgTextureId != 0) {
            int[] textures = {bgTextureId};
            GLES30.glDeleteTextures(1, textures, 0);
            bgTextureId = 0;
        }
        backgroundLoaded = false;
    }
}
