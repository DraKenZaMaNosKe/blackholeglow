package com.secret.blackholeglow.core;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import com.secret.blackholeglow.ArcadeFooter;
// import com.secret.blackholeglow.ArcadePreview; // REMOVIDO
import com.secret.blackholeglow.ArcadeStartText;
import com.secret.blackholeglow.ArcadeTitle;
import com.secret.blackholeglow.christmas.ChristmasPanelBackground;
import com.secret.blackholeglow.christmas.ChristmasOrnamentButton;
import com.secret.blackholeglow.christmas.MiniStopButton;
import com.secret.blackholeglow.LoadingBar;
import com.secret.blackholeglow.OrbixGreeting;
import com.secret.blackholeglow.PlayPauseButton;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                     PanelModeRenderer                            ║
 * ║            Renderizador de UI del Panel de Control               ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  COMPONENTES:                                                    ║
 * ║  • OrbixGreeting: Saludo + reloj + cuenta regresiva              ║
 * ║  • PlayPauseButton: Botón de play central                        ║
 * ║  • LoadingBar: Barra de carga                                    ║
 * ║  • MiniStopButton: Botón stop pequeño (modo wallpaper)           ║
 * ║                                                                  ║
 * ║  RESPONSABILIDADES:                                              ║
 * ║  • Inicializar componentes de UI                                 ║
 * ║  • Actualizar y dibujar según el modo                            ║
 * ║  • Gestionar visibilidad de componentes                          ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class PanelModeRenderer {
    private static final String TAG = "PanelModeRenderer";

    // Componentes UI estándar
    private PlayPauseButton playPauseButton;
    private OrbixGreeting orbixGreeting;
    private LoadingBar loadingBar;
    private MiniStopButton miniStopButton;

    // 🎮 Componentes ARCADE (para Batalla Cósmica)
    private ArcadeTitle arcadeTitle;
    private ArcadeStartText arcadeStartText;
    private ArcadeFooter arcadeFooter;
    // private ArcadePreview arcadePreview; // REMOVIDO
    private boolean arcadeModeEnabled = false;  // Modo arcade para Batalla

    // 🎄 Componentes CHRISTMAS (para Bosque Navideño) - SIMPLIFICADO
    private ChristmasPanelBackground christmasBackground;   // Fondo estático (imagen)
    private ChristmasOrnamentButton christmasOrnament;      // Botón esfera navideña
    private boolean christmasModeEnabled = false;           // Modo navideño para Bosque Navideño

    // Estado
    private boolean initialized = false;
    private final Context context;
    private boolean greetingEnabled = true;  // Deshabilitado para algunos wallpapers

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

        // PlayPauseButton
        playPauseButton = new PlayPauseButton();
        playPauseButton.setPlaying(false);
        Log.d(TAG, "▶️ PlayPauseButton inicializado");

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

        // MiniStopButton
        miniStopButton = new MiniStopButton();
        miniStopButton.hide();
        Log.d(TAG, "⏹️ MiniStopButton inicializado");

        // 🎮 Inicializar componentes ARCADE
        initArcadeComponents();

        // 🎄 Inicializar componentes CHRISTMAS
        initChristmasComponents();

        initialized = true;
        Log.d(TAG, "✅ Panel de Control inicializado");
    }

    /**
     * 🎮 Inicializa los componentes del panel arcade
     */
    private void initArcadeComponents() {
        try {
            arcadeTitle = new ArcadeTitle();
            Log.d(TAG, "🎮 ArcadeTitle inicializado");

            arcadeStartText = new ArcadeStartText();
            Log.d(TAG, "🕹️ ArcadeStartText inicializado");

            arcadeFooter = new ArcadeFooter();
            Log.d(TAG, "© ArcadeFooter inicializado");

            // arcadePreview = new ArcadePreview(context);
            Log.d(TAG, "🖼️ ArcadePreview inicializado");

            Log.d(TAG, "🎮 ═══════════════════════════════════════");
            Log.d(TAG, "🎮 MODO ARCADE COMPONENTES LISTOS");
            Log.d(TAG, "🎮 ═══════════════════════════════════════");
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando componentes arcade: " + e.getMessage());
        }
    }

    /**
     * 🎄 Inicializa los componentes del panel navideño (SIMPLIFICADO)
     * Solo fondo estático + botón esfera = RÁPIDO Y BONITO
     */
    private void initChristmasComponents() {
        try {
            // Fondo estático con la imagen christmas_background.png
            christmasBackground = new ChristmasPanelBackground(context);
            Log.d(TAG, "🎄 ChristmasPanelBackground inicializado (imagen estática)");

            // Botón en forma de esfera navideña (pequeño y centrado)
            christmasOrnament = new ChristmasOrnamentButton();
            christmasOrnament.setPosition(0.0f, -0.1f);  // Ligeramente abajo del centro
            christmasOrnament.setSize(0.09f);            // Tamaño pequeño
            Log.d(TAG, "🔴 ChristmasOrnamentButton inicializado (pequeño + magia)");

            Log.d(TAG, "🎄 ═══════════════════════════════════════");
            Log.d(TAG, "🎄 MODO CHRISTMAS SIMPLIFICADO LISTO");
            Log.d(TAG, "🎄 Fondo: Imagen estática (sin shaders)");
            Log.d(TAG, "🎄 Botón: Esfera navideña");
            Log.d(TAG, "🎄 ═══════════════════════════════════════");
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando componentes Christmas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔄 UPDATE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Actualiza componentes para PANEL_MODE
     */
    public void updatePanelMode(float deltaTime) {
        if (arcadeModeEnabled) {
            // 🎮 MODO ARCADE
            if (arcadeTitle != null) arcadeTitle.update(deltaTime);
            if (arcadeStartText != null) arcadeStartText.update(deltaTime);
            if (arcadeFooter != null) arcadeFooter.update(deltaTime);
            // if (arcadePreview != null) arcadePreview.update(deltaTime);
            if (playPauseButton != null) playPauseButton.update(deltaTime);
        } else if (christmasModeEnabled) {
            // 🎄 MODO CHRISTMAS (simplificado - solo botón necesita update)
            if (christmasOrnament != null) christmasOrnament.update(deltaTime);
        } else {
            // Modo estándar
            if (orbixGreeting != null) {
                orbixGreeting.update(deltaTime);
            }
            if (playPauseButton != null) {
                playPauseButton.update(deltaTime);
            }
        }
    }

    /**
     * Actualiza componentes para LOADING_MODE
     */
    public void updateLoadingMode(float deltaTime) {
        if (loadingBar != null) {
            loadingBar.update(deltaTime);
        }
        if (orbixGreeting != null) {
            orbixGreeting.update(deltaTime);
        }
    }

    /**
     * Actualiza componentes para WALLPAPER_MODE
     */
    public void updateWallpaperMode(float deltaTime) {
        if (miniStopButton != null) {
            miniStopButton.update(deltaTime);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🎨 DRAW
    // ═══════════════════════════════════════════════════════════════

    /**
     * Dibuja UI para PANEL_MODE
     */
    public void drawPanelMode() {
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

        if (arcadeModeEnabled) {
            // 🎮 MODO ARCADE - Panel estilo Street Fighter
            drawArcadePanel();
        } else if (christmasModeEnabled) {
            // 🎄 MODO CHRISTMAS - Panel navideño
            drawChristmasPanel();
        } else {
            // Modo estándar
            if (orbixGreeting != null) {
                orbixGreeting.draw();
            }
            if (playPauseButton != null) {
                playPauseButton.draw();
            }
        }

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }

    /**
     * 🎮 Dibuja el panel arcade estilo Street Fighter
     */
    private void drawArcadePanel() {
        // Título "HUMANS vs ALIENS"
        if (arcadeTitle != null) {
            arcadeTitle.draw();
        }

        // Botón de play (más pequeño, centrado)
        if (playPauseButton != null) {
            playPauseButton.draw();
        }

        // "PRESS START TO PLAY" parpadeante
        if (arcadeStartText != null) {
            arcadeStartText.draw();
        }

        // Preview de la batalla - REMOVIDO

        // Footer "© Orbix iA 2025"
        if (arcadeFooter != null) {
            arcadeFooter.draw();
        }
    }

    /**
     * 🎄 Dibuja el panel navideño (SIMPLIFICADO)
     * Solo fondo estático + botón esfera = RÁPIDO
     */
    private void drawChristmasPanel() {
        // 1. Fondo estático (imagen christmas_background.png)
        if (christmasBackground != null) {
            christmasBackground.draw();
        }

        // 2. Botón esfera navideña (centrado)
        if (christmasOrnament != null) {
            christmasOrnament.draw();
        }
    }

    /**
     * Dibuja UI para LOADING_MODE
     */
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

    /**
     * Dibuja MiniStopButton (overlay sobre wallpaper)
     */
    public void drawWallpaperOverlay() {
        // Guardar estado
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

        // 🔧 FIX: Habilitar blending para que el botón sea visible
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        if (miniStopButton != null && miniStopButton.isVisible()) {
            miniStopButton.draw();
        }

        // Restaurar estado
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔄 TRANSICIONES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Prepara UI para modo de carga
     */
    public void onStartLoading() {
        if (orbixGreeting != null) {
            orbixGreeting.hide();
        }
        if (loadingBar != null) {
            loadingBar.reset();
            loadingBar.show();
            loadingBar.setProgress(1.0f);
        }
    }

    /**
     * Prepara UI para modo wallpaper
     * Oculta el panel y muestra el MiniStopButton
     */
    public void onWallpaperActivated() {
        Log.d(TAG, "🎬 Wallpaper activado - mostrando MiniStopButton");

        // Mostrar botón stop
        if (miniStopButton != null) {
            miniStopButton.show();
        }

        // 🎄 Si modo Christmas, ocultar panel navideño
        if (christmasModeEnabled) {
            if (christmasBackground != null) christmasBackground.hide();
            if (christmasOrnament != null) christmasOrnament.hide();
        }

        // 🎮 Si modo Arcade, ocultar panel arcade
        if (arcadeModeEnabled) {
            if (arcadeTitle != null) arcadeTitle.hide();
            if (arcadeStartText != null) arcadeStartText.hide();
            if (arcadeFooter != null) arcadeFooter.hide();
        }

        // Ocultar elementos estándar
        if (orbixGreeting != null) orbixGreeting.hide();
        if (playPauseButton != null) {
            playPauseButton.setPlaying(true);
            playPauseButton.setVisible(false);
        }
    }

    /**
     * Prepara UI para volver al panel
     * PRESERVA el modo activo (Christmas, Arcade, etc.)
     */
    public void onReturnToPanel() {
        if (miniStopButton != null) {
            miniStopButton.hide();
        }

        // 🎄 Si está en modo Christmas, restaurar el panel navideño
        if (christmasModeEnabled) {
            Log.d(TAG, "🎄 Volviendo al panel CHRISTMAS");
            if (christmasBackground != null) christmasBackground.show();
            if (christmasOrnament != null) christmasOrnament.show();
            // NO mostrar greeting ni playPauseButton en modo Christmas
            return;
        }

        // 🎮 Si está en modo Arcade, restaurar el panel arcade
        if (arcadeModeEnabled) {
            Log.d(TAG, "🎮 Volviendo al panel ARCADE");
            if (arcadeTitle != null) arcadeTitle.show();
            if (arcadeStartText != null) arcadeStartText.show();
            if (arcadeFooter != null) arcadeFooter.show();
            if (playPauseButton != null) {
                playPauseButton.setVisible(true);
                playPauseButton.setPlaying(false);
            }
            return;
        }

        // Modo ESTÁNDAR: mostrar greeting y playPauseButton
        greetingEnabled = true;
        if (orbixGreeting != null) {
            orbixGreeting.show();
        }
        if (playPauseButton != null) {
            playPauseButton.setVisible(true);
            playPauseButton.setPlaying(false);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 👆 TOUCH DETECTION
    // ═══════════════════════════════════════════════════════════════

    /**
     * ¿Toque en PlayPauseButton?
     * En modo Christmas, verifica el botón esfera navideña
     */
    public boolean isPlayButtonTouched(float nx, float ny) {
        // En modo Christmas, usar el botón esfera
        if (christmasModeEnabled && christmasOrnament != null && christmasOrnament.isVisible()) {
            return christmasOrnament.contains(nx, ny);
        }
        // Modo normal o arcade
        return playPauseButton != null && playPauseButton.isInside(nx, ny);
    }

    /**
     * ¿Toque en MiniStopButton?
     */
    public boolean isStopButtonTouched(float nx, float ny) {
        boolean exists = miniStopButton != null;
        boolean visible = exists && miniStopButton.isVisible();
        boolean inside = visible && miniStopButton.isInside(nx, ny);

        // 🔍 DEBUG: Log touch detection
        Log.d(TAG, String.format("⏹️ StopButton check: exists=%b, visible=%b, inside=%b, nx=%.2f, ny=%.2f",
            exists, visible, inside, nx, ny));

        return inside;
    }

    // ═══════════════════════════════════════════════════════════════
    // 📐 SCREEN SIZE
    // ═══════════════════════════════════════════════════════════════

    public void setScreenSize(int width, int height) {
        float aspectRatio = (float) width / height;

        if (playPauseButton != null) {
            playPauseButton.setAspectRatio(aspectRatio);
            // Configurar botón según el modo activo
            if (arcadeModeEnabled) {
                playPauseButton.setSize(0.10f);  // 50% más pequeño
                playPauseButton.setPosition(0.0f, 0.18f);  // Más arriba
            } else if (christmasModeEnabled) {
                playPauseButton.setSize(0.15f);  // Tamaño medio
                playPauseButton.setPosition(0.0f, 0.05f);  // Ligeramente arriba
            } else {
                playPauseButton.setSize(0.18f);  // Tamaño normal
                playPauseButton.setPosition(0.0f, 0.0f);  // Centro
            }
        }
        if (orbixGreeting != null) {
            orbixGreeting.setAspectRatio(aspectRatio);
        }
        if (miniStopButton != null) {
            miniStopButton.setScreenSize(width, height);  // Pasa width/height para u_Resolution
        }

        // 🎮 Componentes arcade
        if (arcadeTitle != null) {
            arcadeTitle.setAspectRatio(aspectRatio);
        }
        if (arcadeStartText != null) {
            arcadeStartText.setAspectRatio(aspectRatio);
        }
        if (arcadeFooter != null) {
            arcadeFooter.setAspectRatio(aspectRatio);
        }
        // arcadePreview removido

        // 🎄 Componentes Christmas (simplificado)
        if (christmasBackground != null) {
            christmasBackground.setAspectRatio(aspectRatio);
        }
        if (christmasOrnament != null) {
            christmasOrnament.setScreenSize(width, height);  // Pass actual resolution
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

    // Getters para acceso directo si es necesario
    public PlayPauseButton getPlayPauseButton() { return playPauseButton; }
    public OrbixGreeting getOrbixGreeting() { return orbixGreeting; }
    public LoadingBar getLoadingBar() { return loadingBar; }
    public MiniStopButton getMiniStopButton() { return miniStopButton; }

    /**
     * Deshabilita el saludo de Gemini (para wallpapers que no lo usan)
     */
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

    /**
     * 🎮 Activa/desactiva el modo ARCADE (para Batalla Cósmica)
     */
    public void setArcadeModeEnabled(boolean enabled) {
        this.arcadeModeEnabled = enabled;

        if (enabled) {
            // 🔧 FIX: Desactivar modo Christmas cuando se activa Arcade
            christmasModeEnabled = false;
            if (christmasBackground != null) christmasBackground.hide();
            if (christmasOrnament != null) christmasOrnament.hide();

            // Activar componentes arcade, ocultar estándar
            if (orbixGreeting != null) orbixGreeting.hide();
            if (arcadeTitle != null) arcadeTitle.show();
            if (arcadeStartText != null) arcadeStartText.show();
            if (arcadeFooter != null) arcadeFooter.show();
            // if (arcadePreview \!= null) arcadePreview.show();

            // Configurar botón para modo arcade (más pequeño)
            if (playPauseButton != null) {
                playPauseButton.setSize(0.10f);
                playPauseButton.setPosition(0.0f, 0.18f);
            }

            Log.d(TAG, "🎮 ═══════════════════════════════════════");
            Log.d(TAG, "🎮 MODO ARCADE ACTIVADO");
            Log.d(TAG, "🎮 ═══════════════════════════════════════");
        } else {
            // Desactivar componentes arcade, mostrar estándar
            if (arcadeTitle != null) arcadeTitle.hide();
            if (arcadeStartText != null) arcadeStartText.hide();
            if (arcadeFooter != null) arcadeFooter.hide();
            // if (arcadePreview \!= null) arcadePreview.hide();
            if (orbixGreeting != null && greetingEnabled) orbixGreeting.show();

            // Restaurar botón a tamaño normal
            if (playPauseButton != null) {
                playPauseButton.setSize(0.18f);
                playPauseButton.setPosition(0.0f, 0.0f);
            }

            Log.d(TAG, "🎮 Modo arcade desactivado");
        }
    }

    /**
     * @return true si el modo arcade está activo
     */
    public boolean isArcadeModeEnabled() {
        return arcadeModeEnabled;
    }

    /**
     * 🎄 Activa/desactiva el modo CHRISTMAS (para Bosque Navideño)
     * Versión simplificada: fondo estático + botón esfera
     */
    public void setChristmasModeEnabled(boolean enabled) {
        this.christmasModeEnabled = enabled;

        if (enabled) {
            // Desactivar otros modos
            arcadeModeEnabled = false;

            // Ocultar componentes estándar y arcade
            if (orbixGreeting != null) orbixGreeting.hide();
            if (arcadeTitle != null) arcadeTitle.hide();
            if (arcadeStartText != null) arcadeStartText.hide();
            if (arcadeFooter != null) arcadeFooter.hide();
            if (playPauseButton != null) playPauseButton.setVisible(false);  // Usamos el ornament en su lugar

            // Activar componentes navideños simplificados
            if (christmasBackground != null) christmasBackground.show();
            if (christmasOrnament != null) christmasOrnament.show();

            Log.d(TAG, "🎄 ═══════════════════════════════════════");
            Log.d(TAG, "🎄 MODO CHRISTMAS ACTIVADO (Simplificado)");
            Log.d(TAG, "🎄 Fondo: Imagen estática");
            Log.d(TAG, "🎄 Botón: Esfera navideña");
            Log.d(TAG, "🎄 ═══════════════════════════════════════");
        } else {
            // Ocultar componentes navideños
            if (christmasBackground != null) christmasBackground.hide();
            if (christmasOrnament != null) christmasOrnament.hide();

            // Restaurar componentes estándar si no hay otro modo activo
            if (!arcadeModeEnabled) {
                if (orbixGreeting != null && greetingEnabled) orbixGreeting.show();
                if (playPauseButton != null) {
                    playPauseButton.setVisible(true);
                    playPauseButton.setSize(0.18f);
                    playPauseButton.setPosition(0.0f, 0.0f);
                }
            }

            Log.d(TAG, "🎄 Modo Christmas desactivado");
        }
    }

    /**
     * @return true si el modo navideño está activo
     */
    public boolean isChristmasModeEnabled() {
        return christmasModeEnabled;
    }

    /**
     * Libera recursos
     */
    public void release() {
        if (orbixGreeting != null) {
            orbixGreeting.dispose();
            orbixGreeting = null;
        }

        // 🎮 Liberar recursos arcade
        if (arcadeTitle != null) {
            arcadeTitle.dispose();
            arcadeTitle = null;
        }
        if (arcadeStartText != null) {
            arcadeStartText.dispose();
            arcadeStartText = null;
        }
        if (arcadeFooter != null) {
            arcadeFooter.dispose();
            arcadeFooter = null;
        }
        // arcadePreview removido - ya no se usa

        // 🎄 Liberar recursos Christmas (simplificado)
        if (christmasBackground != null) {
            christmasBackground.dispose();
            christmasBackground = null;
        }
        if (christmasOrnament != null) {
            christmasOrnament.dispose();
            christmasOrnament = null;
        }

        Log.d(TAG, "🧹 PanelModeRenderer recursos liberados");
    }
}
