package com.secret.blackholeglow.wallpaper.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.secret.blackholeglow.wallpaper.model.Wallpaper;
import com.secret.blackholeglow.wallpaper.model.WallpaperRepository;

/**
 * ═══════════════════════════════════════════════════════════════
 * WallpaperManager - Controller (MVC Pattern)
 * ═══════════════════════════════════════════════════════════════
 *
 * Gestiona la lógica de negocio de wallpapers.
 *
 * Responsabilidades:
 *  - Seleccionar wallpaper activo
 *  - Persistir preferencias del usuario
 *  - Proveer wallpaper actual al renderer
 *
 * Patrón: Singleton Controller
 */
public class WallpaperManager {

    private static final String TAG = "WallpaperManager";
    private static final String PREFS_NAME = "blackholeglow_prefs";
    private static final String KEY_SELECTED_WALLPAPER_ID = "selected_wallpaper_id";
    private static final String DEFAULT_WALLPAPER_ID = "universo";  // Wallpaper por defecto

    private static WallpaperManager instance;
    private final WallpaperRepository repository;
    private final SharedPreferences prefs;

    private Wallpaper currentWallpaper;

    /**
     * Constructor privado (Singleton)
     */
    private WallpaperManager(Context context) {
        this.repository = WallpaperRepository.getInstance();
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Cargar wallpaper guardado
        loadSavedWallpaper();
    }

    /**
     * Obtener instancia única
     */
    public static synchronized WallpaperManager getInstance(Context context) {
        if (instance == null) {
            instance = new WallpaperManager(context);
        }
        return instance;
    }

    // ═══════════════════════════════════════════════════════════
    // Lógica de Negocio
    // ═══════════════════════════════════════════════════════════

    /**
     * Cargar wallpaper guardado desde SharedPreferences
     */
    private void loadSavedWallpaper() {
        String savedId = prefs.getString(KEY_SELECTED_WALLPAPER_ID, DEFAULT_WALLPAPER_ID);
        currentWallpaper = repository.getWallpaperById(savedId);

        if (currentWallpaper == null) {
            // Fallback: usar wallpaper por defecto
            currentWallpaper = repository.getWallpaperById(DEFAULT_WALLPAPER_ID);
            Log.w(TAG, "Wallpaper guardado no encontrado, usando default: " + DEFAULT_WALLPAPER_ID);
        }

        Log.d(TAG, "╔════════════════════════════════════════════╗");
        Log.d(TAG, "║   WALLPAPER MANAGER INITIALIZED           ║");
        Log.d(TAG, "╠════════════════════════════════════════════╣");
        Log.d(TAG, "║ Current: " + String.format("%-34s", currentWallpaper.getDisplayName()) + "║");
        Log.d(TAG, "╚════════════════════════════════════════════╝");
    }

    /**
     * Seleccionar nuevo wallpaper
     *
     * @param wallpaperId ID del wallpaper a activar
     * @return true si se cambió exitosamente
     */
    public boolean selectWallpaper(String wallpaperId) {
        Wallpaper wallpaper = repository.getWallpaperById(wallpaperId);

        if (wallpaper == null) {
            Log.e(TAG, "✗ Wallpaper no encontrado: " + wallpaperId);
            return false;
        }

        // Actualizar wallpaper actual
        currentWallpaper = wallpaper;

        // Guardar en SharedPreferences
        prefs.edit()
                .putString(KEY_SELECTED_WALLPAPER_ID, wallpaperId)
                .apply();

        Log.d(TAG, "✓ Wallpaper seleccionado: " + wallpaper.getDisplayName());
        return true;
    }

    /**
     * Seleccionar wallpaper por nombre (usado por SceneRenderer)
     */
    public boolean selectWallpaperByName(String name) {
        Wallpaper wallpaper = repository.getWallpaperByName(name);

        if (wallpaper == null) {
            Log.e(TAG, "✗ Wallpaper no encontrado por nombre: " + name);
            return false;
        }

        return selectWallpaper(wallpaper.getId());
    }

    // ═══════════════════════════════════════════════════════════
    // API Pública - Getters
    // ═══════════════════════════════════════════════════════════

    /**
     * Obtener wallpaper actualmente seleccionado
     */
    public Wallpaper getCurrentWallpaper() {
        return currentWallpaper;
    }

    /**
     * Obtener nombre del wallpaper actual (para SceneRenderer)
     */
    public String getCurrentWallpaperName() {
        return currentWallpaper != null ? currentWallpaper.getName() : DEFAULT_WALLPAPER_ID;
    }

    /**
     * Obtener repositorio (para UI que necesite lista completa)
     */
    public WallpaperRepository getRepository() {
        return repository;
    }
}
