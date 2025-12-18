package com.secret.blackholeglow.core;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import com.secret.blackholeglow.ArcadeFooter;
// import com.secret.blackholeglow.ArcadePreview; // REMOVIDO
import com.secret.blackholeglow.ArcadeStartText;
import com.secret.blackholeglow.ArcadeTitle;
import com.secret.blackholeglow.ChristmasFooter;
import com.secret.blackholeglow.ChristmasSnowfall;
import com.secret.blackholeglow.ChristmasTitle;
import com.secret.blackholeglow.LoadingBar;
import com.secret.blackholeglow.MiniStopButton;
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

    // 🎄 Componentes CHRISTMAS (para Bosque Navideño)
    private ChristmasTitle christmasTitle;
    private ChristmasSnowfall christmasSnowfall;
    private ChristmasFooter christmasFooter;
    private boolean christmasModeEnabled = false;  // Modo navideño para Bosque Navideño

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
     * 🎄 Inicializa los componentes del panel navideño
     */
    private void initChristmasComponents() {
        try {
            christmasTitle = new ChristmasTitle();
            Log.d(TAG, "🎄 ChristmasTitle inicializado");

            christmasSnowfall = new ChristmasSnowfall();
            Log.d(TAG, "❄️ ChristmasSnowfall inicializado");

            christmasFooter = new ChristmasFooter();
            Log.d(TAG, "⭐ ChristmasFooter inicializado");

            Log.d(TAG, "🎄 ═══════════════════════════════════════");
            Log.d(TAG, "🎄 MODO CHRISTMAS COMPONENTES LISTOS");
            Log.d(TAG, "🎄 ═══════════════════════════════════════");
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando componentes Christmas: " + e.getMessage());
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
            // 🎄 MODO CHRISTMAS
            if (christmasSnowfall != null) christmasSnowfall.update(deltaTime);
            if (christmasTitle != null) christmasTitle.update(deltaTime);
            if (christmasFooter != null) christmasFooter.update(deltaTime);
            if (playPauseButton != null) playPauseButton.update(deltaTime);
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
     * 🎄 Dibuja el panel navideño
     */
    private void drawChristmasPanel() {
        // Copos de nieve cayendo (fondo)
        if (christmasSnowfall != null) {
            christmasSnowfall.draw();
        }

        // Título "FELIZ NAVIDAD"
        if (christmasTitle != null) {
            christmasTitle.draw();
        }

        // Botón de play (centrado)
        if (playPauseButton != null) {
            playPauseButton.draw();
        }

        // Footer "Toca para comenzar"
        if (christmasFooter != null) {
            christmasFooter.draw();
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
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

        if (miniStopButton != null && miniStopButton.isVisible()) {
            miniStopButton.draw();
        }

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
     */
    public void onWallpaperActivated() {
        if (miniStopButton != null) {
            miniStopButton.show();
        }
        if (playPauseButton != null) {
            playPauseButton.setPlaying(true);
        }
    }

    /**
     * Prepara UI para volver al panel
     */
    public void onReturnToPanel() {
        if (miniStopButton != null) {
            miniStopButton.hide();
        }
        // SIEMPRE rehabilitar greeting al volver al panel
        // (solo se deshabilita DURANTE ciertas escenas, no en el panel)
        greetingEnabled = true;
        if (orbixGreeting != null) {
            orbixGreeting.show();
        }
        if (playPauseButton != null) {
            playPauseButton.setPlaying(false);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 👆 TOUCH DETECTION
    // ═══════════════════════════════════════════════════════════════

    /**
     * ¿Toque en PlayPauseButton?
     */
    public boolean isPlayButtonTouched(float nx, float ny) {
        return playPauseButton != null && playPauseButton.isInside(nx, ny);
    }

    /**
     * ¿Toque en MiniStopButton?
     */
    public boolean isStopButtonTouched(float nx, float ny) {
        return miniStopButton != null && miniStopButton.isVisible() && miniStopButton.isInside(nx, ny);
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
            miniStopButton.setAspectRatio(aspectRatio);
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

        // 🎄 Componentes Christmas
        if (christmasTitle != null) {
            christmasTitle.setAspectRatio(aspectRatio);
        }
        if (christmasSnowfall != null) {
            christmasSnowfall.setAspectRatio(aspectRatio);
        }
        if (christmasFooter != null) {
            christmasFooter.setAspectRatio(aspectRatio);
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

            // Activar componentes navideños
            if (christmasSnowfall != null) christmasSnowfall.show();
            if (christmasTitle != null) christmasTitle.show();
            if (christmasFooter != null) christmasFooter.show();

            // Configurar botón para modo navideño (tamaño estándar)
            if (playPauseButton != null) {
                playPauseButton.setSize(0.15f);
                playPauseButton.setPosition(0.0f, 0.05f);
            }

            Log.d(TAG, "🎄 ═══════════════════════════════════════");
            Log.d(TAG, "🎄 MODO CHRISTMAS ACTIVADO");
            Log.d(TAG, "🎄 ═══════════════════════════════════════");
        } else {
            // Ocultar componentes navideños
            if (christmasSnowfall != null) christmasSnowfall.hide();
            if (christmasTitle != null) christmasTitle.hide();
            if (christmasFooter != null) christmasFooter.hide();

            // Restaurar componentes estándar si no hay otro modo activo
            if (!arcadeModeEnabled) {
                if (orbixGreeting != null && greetingEnabled) orbixGreeting.show();
                if (playPauseButton != null) {
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

        // 🎄 Liberar recursos Christmas
        if (christmasTitle != null) {
            christmasTitle.dispose();
            christmasTitle = null;
        }
        if (christmasSnowfall != null) {
            christmasSnowfall.dispose();
            christmasSnowfall = null;
        }
        if (christmasFooter != null) {
            christmasFooter.dispose();
            christmasFooter = null;
        }

        Log.d(TAG, "🧹 PanelModeRenderer recursos liberados");
    }
}
