package com.secret.blackholeglow.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import com.secret.blackholeglow.GamingController3D;
import com.secret.blackholeglow.LoadingBar;
import com.secret.blackholeglow.OrbixGreeting;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.WallpaperPreferences;
import com.secret.blackholeglow.models.WallpaperItem;
import com.secret.blackholeglow.systems.WallpaperCatalog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                     PanelModeRenderer                            ║
 * ║            Renderizador de UI del Panel de Control               ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  COMPONENTES:                                                    ║
 * ║  • Imagen de fondo: Preview del wallpaper seleccionado           ║
 * ║  • GamingController3D: Control Xbox 3D flotante (toca para play) ║
 * ║  • OrbixGreeting: Saludo + reloj + cuenta regresiva              ║
 * ║  • LoadingBar: Barra de carga                                    ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  OPTIMIZACIÓN v5.0.8:                                            ║
 * ║  • Gaming Controller reemplaza ArcaneGrimoire (libro mágico)     ║
 * ║  • UX intuitiva: control = tap para jugar                        ║
 * ║  • Shader cyberpunk neón (cyan/magenta glow)                     ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class PanelModeRenderer {
    private static final String TAG = "PanelModeRenderer";

    // ═══════════════════════════════════════════════════════════════
    // 🖼️ FONDO ESTÁTICO (Preview del wallpaper seleccionado)
    // ═══════════════════════════════════════════════════════════════
    private int bgShaderProgram = 0;
    private int bgTextureId = 0;
    private int bgAPositionLoc = -1;
    private int bgATexCoordLoc = -1;
    private int bgUTextureLoc = -1;
    private int bgUAlphaLoc = -1;
    private int bgUDarkenLoc = -1;
    private FloatBuffer bgVertexBuffer;
    private boolean backgroundLoaded = false;
    private int currentPreviewResourceId = 0;
    private float bgDarkenAmount = 0.25f;  // Oscurecer 25% para que el libro resalte

    // Shaders para el fondo
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
        "    vec3 darkened = texColor.rgb * (1.0 - u_Darken);\n" +
        "    gl_FragColor = vec4(darkened, texColor.a * u_Alpha);\n" +
        "}\n";

    // Componentes UI
    private GamingController3D controller;
    private OrbixGreeting orbixGreeting;
    private LoadingBar loadingBar;

    // Estado
    private boolean initialized = false;
    private final Context context;
    private boolean greetingEnabled = true;
    private int screenWidth, screenHeight;
    private float backgroundAlpha = 1.0f;

    // Listener para eventos de carga
    public interface LoadingCompleteListener {
        void onLoadingComplete();
    }
    private LoadingCompleteListener loadingListener;

    public PanelModeRenderer(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Inicializa todos los componentes de UI
     */
    public void initialize() {
        if (initialized) return;

        Log.d(TAG, "🎛️ Inicializando Panel de Control...");

        // Cargar preview del wallpaper seleccionado como fondo
        loadSelectedWallpaperPreview();

        // GamingController3D - Control Xbox 3D cyberpunk
        controller = new GamingController3D(context);
        controller.initialize();
        Log.d(TAG, "🎮 GamingController3D inicializado");

        // OrbixGreeting
        orbixGreeting = new OrbixGreeting(context);
        orbixGreeting.show();
        Log.d(TAG, "🤖 OrbixGreeting inicializado");

        // LoadingBar
        loadingBar = new LoadingBar();
        loadingBar.setOnLoadingCompleteListener(() -> {
            if (loadingListener != null) {
                loadingListener.onLoadingComplete();
            }
        });
        Log.d(TAG, "📊 LoadingBar inicializado");

        initialized = true;
        Log.d(TAG, "✅ Panel de Control inicializado (Gaming Controller + preview estático)");
    }

    /**
     * 🖼️ Carga el preview del wallpaper actualmente seleccionado
     */
    private void loadSelectedWallpaperPreview() {
        String selectedScene = WallpaperPreferences.getInstance(context).getSelectedWallpaperSync();

        if (selectedScene == null || selectedScene.isEmpty()) {
            selectedScene = "ABYSSIA"; // Default
        }

        WallpaperItem item = WallpaperCatalog.get().getBySceneName(selectedScene);

        if (item != null) {
            currentPreviewResourceId = item.getResourceIdPreview();
            Log.d(TAG, "🖼️ Preview seleccionado: " + selectedScene + " -> " + currentPreviewResourceId);
        } else {
            currentPreviewResourceId = R.drawable.preview_oceano_sc; // Fallback
            Log.w(TAG, "⚠️ Wallpaper no encontrado, usando fallback");
        }

        backgroundLoaded = false; // Forzar recarga en GL thread
    }

    /**
     * 🖼️ Actualiza el fondo cuando cambia el wallpaper seleccionado
     */
    public void updateBackgroundForSelectedWallpaper() {
        loadSelectedWallpaperPreview();
    }

    /**
     * 🖼️ Inicializa los recursos OpenGL del fondo (llamar desde GL thread)
     */
    private void initBackgroundOpenGL() {
        if (currentPreviewResourceId == 0 || backgroundLoaded) return;

        // Liberar textura anterior si existe
        if (bgTextureId != 0) {
            int[] textures = {bgTextureId};
            GLES30.glDeleteTextures(1, textures, 0);
            bgTextureId = 0;
        }

        // Crear shader program (solo la primera vez)
        if (bgShaderProgram == 0) {
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

            bgAPositionLoc = GLES30.glGetAttribLocation(bgShaderProgram, "a_Position");
            bgATexCoordLoc = GLES30.glGetAttribLocation(bgShaderProgram, "a_TexCoord");
            bgUTextureLoc = GLES30.glGetUniformLocation(bgShaderProgram, "u_Texture");
            bgUAlphaLoc = GLES30.glGetUniformLocation(bgShaderProgram, "u_Alpha");
            bgUDarkenLoc = GLES30.glGetUniformLocation(bgShaderProgram, "u_Darken");

            // Vertex buffer para fullscreen quad
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

            GLES30.glDeleteShader(vs);
            GLES30.glDeleteShader(fs);
        }

        // Cargar textura del preview
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), currentPreviewResourceId, options);

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
            Log.e(TAG, "❌ Error cargando bitmap del preview");
        }

        backgroundLoaded = true;
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

    // ═══════════════════════════════════════════════════════════════
    // 🔄 UPDATE
    // ═══════════════════════════════════════════════════════════════

    public void updatePanelMode(float deltaTime) {
        // Inicializar fondo si está pendiente (debe hacerse en GL thread)
        if (!backgroundLoaded && currentPreviewResourceId != 0) {
            initBackgroundOpenGL();
        }

        if (controller != null) {
            controller.update(deltaTime);
        }
        if (orbixGreeting != null) {
            orbixGreeting.update(deltaTime);
        }
    }

    public void updateLoadingMode(float deltaTime) {
        if (loadingBar != null) {
            loadingBar.update(deltaTime);
        }
        if (orbixGreeting != null) {
            orbixGreeting.update(deltaTime);
        }
    }

    public void updateWallpaperMode(float deltaTime) {
        // Nothing to update in wallpaper mode
    }

    // ═══════════════════════════════════════════════════════════════
    // 🎨 DRAW
    // ═══════════════════════════════════════════════════════════════

    /**
     * 🖼️ Dibuja el fondo (preview del wallpaper)
     */
    private void drawBackground() {
        if (!backgroundLoaded || bgShaderProgram == 0 || bgTextureId == 0) return;

        GLES30.glUseProgram(bgShaderProgram);

        GLES30.glUniform1f(bgUAlphaLoc, backgroundAlpha);
        GLES30.glUniform1f(bgUDarkenLoc, bgDarkenAmount);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bgTextureId);
        GLES30.glUniform1i(bgUTextureLoc, 0);

        GLES30.glEnableVertexAttribArray(bgAPositionLoc);
        GLES30.glEnableVertexAttribArray(bgATexCoordLoc);

        bgVertexBuffer.position(0);
        GLES30.glVertexAttribPointer(bgAPositionLoc, 2, GLES30.GL_FLOAT, false, 16, bgVertexBuffer);
        bgVertexBuffer.position(2);
        GLES30.glVertexAttribPointer(bgATexCoordLoc, 2, GLES30.GL_FLOAT, false, 16, bgVertexBuffer);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(bgAPositionLoc);
        GLES30.glDisableVertexAttribArray(bgATexCoordLoc);
    }

    public void drawPanelMode() {
        // 1. Dibujar fondo (preview del wallpaper)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glDisable(GLES30.GL_BLEND);
        drawBackground();

        // 2. Dibujar UI encima del fondo
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        if (controller != null) {
            controller.draw();
        }

        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        if (orbixGreeting != null) {
            orbixGreeting.draw();
        }

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }

    public void drawLoadingMode() {
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

        if (orbixGreeting != null) {
            orbixGreeting.draw();
        }
        if (loadingBar != null) {
            loadingBar.draw();
        }

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }

    public void drawWallpaperOverlay() {
        // No overlay elements to draw
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔄 TRANSICIONES
    // ═══════════════════════════════════════════════════════════════

    /**
     * 🖼️ Inicia la pantalla de carga con preview real del wallpaper
     * @param sceneName Nombre de la escena
     * @param displayName Nombre para mostrar
     * @param glowColor Color del tema
     * @param previewResourceId ID del drawable del preview (0 si no hay)
     */
    public void onStartLoadingWithPreview(String sceneName, String displayName, int glowColor, int previewResourceId) {
        if (orbixGreeting != null) {
            orbixGreeting.hide();
        }
        if (loadingBar != null) {
            loadingBar.reset();
            loadingBar.setWallpaperTheme(displayName, glowColor);
            // 🖼️ Usar preview real del wallpaper como fondo
            if (previewResourceId != 0) {
                loadingBar.setBackgroundImage(context, previewResourceId);
            } else {
                loadingBar.setBackgroundForScene(context, sceneName);
            }
            loadingBar.show();
            // NO llamar setProgress(1.0f) - el progreso vendrá del ResourcePreloader
        }
        Log.d(TAG, "📊 Cargando: " + displayName + " (escena: " + sceneName + ", preview: " + previewResourceId + ")");
    }

    /**
     * 📊 Actualiza el progreso de carga desde ResourcePreloader
     * @param progress Progreso 0.0 - 1.0
     * @param currentTask Nombre de la tarea actual (para mostrar)
     */
    public void updateLoadingProgress(float progress, String currentTask) {
        if (loadingBar != null) {
            loadingBar.setProgress(progress);
            if (currentTask != null) {
                loadingBar.setResourceName(currentTask);
            }
        }
    }

    // Legacy methods (mantener compatibilidad)
    public void onStartLoading(String sceneName, String displayName, int glowColor) {
        onStartLoadingWithPreview(sceneName, displayName, glowColor, 0);
    }

    public void onStartLoading(String sceneName) {
        onStartLoading(sceneName, sceneName, 0xFF00D4FF);
    }

    public void onStartLoading() {
        onStartLoading(null, "Orbix", 0xFF00D4FF);
    }

    public void onWallpaperActivated() {
        Log.d(TAG, "🎬 Wallpaper activado");
        if (orbixGreeting != null) orbixGreeting.hide();

        // Liberar recursos del panel para ahorrar memoria
        releaseForWallpaperMode();
    }

    private void releaseForWallpaperMode() {
        Log.d(TAG, "🧹 Liberando recursos del panel para wallpaper mode...");

        // Liberar textura de fondo (~5-10 MB GPU)
        if (bgTextureId != 0) {
            int[] textures = {bgTextureId};
            GLES30.glDeleteTextures(1, textures, 0);
            bgTextureId = 0;
            Log.d(TAG, "  ✓ Textura de fondo liberada");
        }
        backgroundLoaded = false;

        // Liberar controller (~15 MB GPU)
        if (controller != null) {
            controller.release();
            controller = null;
            Log.d(TAG, "  ✓ Gaming Controller liberado");
        }

        Log.d(TAG, "✅ Recursos del panel liberados - ~20-25 MB GPU recuperados");
    }

    public void onReturnToPanel() {
        greetingEnabled = true;
        if (orbixGreeting != null) {
            orbixGreeting.show();
        }

        // Recargar recursos del panel
        reloadForPanelMode();
    }

    private void reloadForPanelMode() {
        Log.d(TAG, "🔄 Recargando recursos del panel...");

        // Recargar preview del wallpaper seleccionado (puede haber cambiado)
        loadSelectedWallpaperPreview();

        // Recargar controller
        if (controller == null) {
            Log.d(TAG, "  🎮 Recargando Gaming Controller...");
            controller = new GamingController3D(context);
            controller.initialize();
            if (screenWidth > 0 && screenHeight > 0) {
                controller.setScreenSize(screenWidth, screenHeight);
                controller.setAspectRatio((float) screenWidth / screenHeight);
            }
        }

        Log.d(TAG, "✅ Recursos del panel recargados");
    }

    // ═══════════════════════════════════════════════════════════════
    // 👆 TOUCH DETECTION
    // ═══════════════════════════════════════════════════════════════

    public boolean isPlayButtonTouched(float nx, float ny) {
        if (controller != null) {
            return controller.isInside(nx, ny);
        }
        return true;
    }

    // ═══════════════════════════════════════════════════════════════
    // 📐 SCREEN SIZE
    // ═══════════════════════════════════════════════════════════════

    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        float aspectRatio = (float) width / height;

        if (controller != null) {
            controller.setScreenSize(width, height);
            controller.setAspectRatio(aspectRatio);
        }

        if (orbixGreeting != null) {
            orbixGreeting.setAspectRatio(aspectRatio);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔧 UTILIDADES
    // ═══════════════════════════════════════════════════════════════

    public boolean isLoadingComplete() {
        return loadingBar != null && loadingBar.isComplete();
    }

    public void setLoadingListener(LoadingCompleteListener listener) {
        this.loadingListener = listener;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public OrbixGreeting getOrbixGreeting() { return orbixGreeting; }
    public LoadingBar getLoadingBar() { return loadingBar; }

    public void setGreetingEnabled(boolean enabled) {
        this.greetingEnabled = enabled;
        if (orbixGreeting != null) {
            if (enabled) {
                orbixGreeting.show();
            } else {
                orbixGreeting.hide();
            }
        }
        Log.d(TAG, "🤖 Greeting " + (enabled ? "habilitado" : "deshabilitado"));
    }

    // ═══════════════════════════════════════════════════════════════
    // ⏸️ PAUSE / RESUME - Ya no hay video, métodos simplificados
    // ═══════════════════════════════════════════════════════════════

    public void pause() {
        // Sin video, no hay nada que pausar
        Log.d(TAG, "⏸️ Panel pausado (sin video)");
    }

    public void resume() {
        // Sin video, no hay nada que reanudar
        Log.d(TAG, "▶️ Panel reanudado (sin video)");
    }

    public void release() {
        // Liberar textura de fondo
        if (bgTextureId != 0) {
            int[] textures = {bgTextureId};
            GLES30.glDeleteTextures(1, textures, 0);
            bgTextureId = 0;
        }

        // Liberar shader
        if (bgShaderProgram != 0) {
            GLES30.glDeleteProgram(bgShaderProgram);
            bgShaderProgram = 0;
        }

        if (controller != null) {
            controller.release();
            controller = null;
        }

        if (orbixGreeting != null) {
            orbixGreeting.dispose();
            orbixGreeting = null;
        }

        Log.d(TAG, "🧹 PanelModeRenderer recursos liberados");
    }

    /**
     * @return true siempre (ya no hay video que esperar)
     */
    public boolean isVideoReady() {
        return true; // Imagen siempre lista
    }

    /**
     * @return 100 siempre (ya no hay descarga de video)
     */
    public int getDownloadProgress() {
        return 100;
    }

    /**
     * @return false siempre (ya no hay descarga de video)
     */
    public boolean isVideoDownloading() {
        return false;
    }
}
