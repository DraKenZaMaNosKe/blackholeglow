package com.secret.blackholeglow.systems;

import android.util.Log;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.models.WallpaperItem;
import com.secret.blackholeglow.models.WallpaperTier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                      WallpaperCatalog                             ║
 * ║                  "El Curador del Catálogo"                        ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  Actor especializado en la gestión del catálogo de wallpapers.    ║
 * ║                                                                   ║
 * ║  RESPONSABILIDADES:                                               ║
 * ║  • Almacenar y proveer todos los wallpapers disponibles           ║
 * ║  • Filtrar por tier (FREE, PREMIUM, VIP, COMING_SOON)             ║
 * ║  • Filtrar por acceso del usuario (consulta SubscriptionManager)  ║
 * ║  • Obtener wallpapers destacados (featured)                       ║
 * ║  • Buscar wallpapers por nombre                                   ║
 * ║                                                                   ║
 * ║  PRINCIPIOS:                                                      ║
 * ║  • Máximo 10 métodos públicos                                     ║
 * ║  • Un solo propósito: gestión del catálogo                        ║
 * ║  • Singleton para acceso global                                   ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * USO:
 *   // Obtener todos los wallpapers
 *   List<WallpaperItem> all = WallpaperCatalog.get().getAll();
 *
 *   // Obtener solo los gratuitos
 *   List<WallpaperItem> free = WallpaperCatalog.get().getByTier(WallpaperTier.FREE);
 *
 *   // Obtener los accesibles para el usuario actual
 *   List<WallpaperItem> accessible = WallpaperCatalog.get().getAccessibleFor(userLevel);
 */
public class WallpaperCatalog {
    private static final String TAG = "WallpaperCatalog";

    // ═══════════════════════════════════════════════════════════════
    // SINGLETON
    // ═══════════════════════════════════════════════════════════════

    private static WallpaperCatalog instance;

    public static WallpaperCatalog get() {
        if (instance == null) {
            instance = new WallpaperCatalog();
        }
        return instance;
    }

    // ═══════════════════════════════════════════════════════════════
    // CATÁLOGO
    // ═══════════════════════════════════════════════════════════════

    private final List<WallpaperItem> catalog = new ArrayList<>();

    private WallpaperCatalog() {
        initializeCatalog();
        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   📚 WallpaperCatalog Inicializado     ║");
        Log.d(TAG, "║   Total: " + catalog.size() + " wallpapers              ║");
        Log.d(TAG, "╚════════════════════════════════════════╝");
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. OBTENER TODOS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Obtiene todos los wallpapers del catálogo
     * @return Lista inmutable de todos los wallpapers
     */
    public List<WallpaperItem> getAll() {
        return Collections.unmodifiableList(catalog);
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. FILTRAR POR TIER
    // ═══════════════════════════════════════════════════════════════

    /**
     * Obtiene wallpapers de un tier específico
     * @param tier El nivel de acceso a filtrar
     * @return Lista de wallpapers de ese tier
     */
    public List<WallpaperItem> getByTier(WallpaperTier tier) {
        List<WallpaperItem> result = new ArrayList<>();
        for (WallpaperItem item : catalog) {
            if (item.getTier() == tier) {
                result.add(item);
            }
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. FILTRAR POR ACCESO DEL USUARIO
    // ═══════════════════════════════════════════════════════════════

    /**
     * Obtiene wallpapers accesibles para el nivel del usuario
     * @param userLevel Nivel del usuario (0=FREE, 1=PREMIUM, 2=VIP)
     * @return Lista de wallpapers que el usuario puede usar
     */
    public List<WallpaperItem> getAccessibleFor(int userLevel) {
        List<WallpaperItem> result = new ArrayList<>();
        for (WallpaperItem item : catalog) {
            if (item.isAccessibleBy(userLevel)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Obtiene wallpapers accesibles usando SubscriptionManager
     * @return Lista de wallpapers que el usuario actual puede usar
     */
    public List<WallpaperItem> getAccessible() {
        int userLevel = SubscriptionManager.get().getUserLevel();
        return getAccessibleFor(userLevel);
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. OBTENER DESTACADOS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Obtiene wallpapers marcados como destacados (featured)
     * @return Lista de wallpapers destacados
     */
    public List<WallpaperItem> getFeatured() {
        List<WallpaperItem> result = new ArrayList<>();
        for (WallpaperItem item : catalog) {
            if (item.isFeatured()) {
                result.add(item);
            }
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    // 5. BUSCAR POR NOMBRE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Busca un wallpaper por su nombre
     * @param nombre Nombre del wallpaper
     * @return WallpaperItem o null si no existe
     */
    public WallpaperItem getByName(String nombre) {
        for (WallpaperItem item : catalog) {
            if (item.getNombre().equalsIgnoreCase(nombre)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Busca un wallpaper por el nombre de su escena
     * @param sceneName Nombre de la escena (para SceneFactory)
     * @return WallpaperItem o null si no existe
     */
    public WallpaperItem getBySceneName(String sceneName) {
        for (WallpaperItem item : catalog) {
            if (item.getSceneName().equalsIgnoreCase(sceneName)) {
                return item;
            }
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════
    // 6. VERIFICAR ACCESO
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verifica si un wallpaper es accesible para el usuario actual
     * @param wallpaperName Nombre del wallpaper
     * @return true si puede acceder
     */
    public boolean canAccess(String wallpaperName) {
        WallpaperItem item = getByName(wallpaperName);
        if (item == null) return false;

        int userLevel = SubscriptionManager.get().getUserLevel();
        return item.isAccessibleBy(userLevel);
    }

    // ═══════════════════════════════════════════════════════════════
    // 7. CONTADORES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Obtiene el número total de wallpapers
     */
    public int getCount() {
        return catalog.size();
    }

    /**
     * Obtiene el número de wallpapers de un tier específico
     */
    public int getCountByTier(WallpaperTier tier) {
        return getByTier(tier).size();
    }

    // ═══════════════════════════════════════════════════════════════
    // 8. RESET (para testing)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Reset del singleton (para recreación completa)
     */
    public static void reset() {
        instance = null;
        Log.d(TAG, "WallpaperCatalog reset");
    }

    // ═══════════════════════════════════════════════════════════════
    // INICIALIZACIÓN DEL CATÁLOGO
    // ═══════════════════════════════════════════════════════════════

    /**
     * Inicializa el catálogo con todos los wallpapers disponibles
     *
     * AGREGAR NUEVOS WALLPAPERS AQUÍ:
     * Usa el Builder pattern para crear entradas fácilmente
     */
    private void initializeCatalog() {
        // ╔════════════════════════════════════════════════════════════╗
        // ║  🌌 CATÁLOGO DE WALLPAPERS - BLACK HOLE GLOW               ║
        // ╚════════════════════════════════════════════════════════════╝

        // ─────────────────────────────────────────────────────────────
        // 🆓 WALLPAPERS DISPONIBLES
        // ─────────────────────────────────────────────────────────────

        catalog.add(new WallpaperItem.Builder("Batalla Cósmica")
                .descripcion("Defiende la Tierra de meteoritos mientras el OVNI patrulla el cosmos. " +
                        "Sistema de combate espacial con escudos, armas láser y efectos visuales épicos.")
                .preview(R.drawable.preview_batalla_cosmica)
                .sceneName("Batalla Cósmica")  // Nombre directo, sin alias confusos
                .tier(WallpaperTier.FREE)
                .badge("🔥 POPULAR")
                .glow(0xFFFF4500)  // Orange Red
                .featured()
                .build());

        catalog.add(new WallpaperItem.Builder("Navidad")
                .descripcion("Disfruta de la magia de la navidad en tu celular")
                .preview(R.drawable.preview_navidad)
                .sceneName("Bosque Navideño")
                .tier(WallpaperTier.FREE)
                .badge("🎄")
                .glow(0xFF00FF7F)  // Spring Green
                .featured()
                .build());

        // ─────────────────────────────────────────────────────────────
        // 🔮 WALLPAPERS EN DESARROLLO (COMING_SOON)
        // ─────────────────────────────────────────────────────────────

        catalog.add(new WallpaperItem.Builder("Fondo del Mar")
                .descripcion("Sumérgete en las profundidades de un océano alienígena. " +
                        "Plantas bioluminescentes y rayos de luz extraterrestre te esperan.")
                .preview(R.drawable.preview_oceano_sc)
                .sceneName("Fondo del Mar")
                .tier(WallpaperTier.FREE)
                .badge("🌊 NUEVO")
                .glow(0xFF9932CC)  // Purple para look alien
                .featured()
                .build());

        // 🧪 LABORATORIO - Escena de pruebas experimentales
        catalog.add(new WallpaperItem.Builder("Laboratorio")
                .descripcion("Escena experimental para probar nuevos efectos. " +
                        "SpeedLines, ParallaxStars, y más experimentos en desarrollo.")
                .preview(R.drawable.preview_space)
                .sceneName("Laboratorio")
                .tier(WallpaperTier.FREE)
                .badge("🧪 LAB")
                .glow(0xFF00FF00)  // Green para lab
                .build());

        catalog.add(new WallpaperItem.Builder("La Mansión")
                .descripcion("Explora los misterios de una mansión encantada. " +
                        "Fantasmas, velas flotantes y secretos oscuros aguardan en cada rincón de esta morada tenebrosa.")
                .preview(R.drawable.preview_storm)
                .sceneName("La Mansión")
                .tier(WallpaperTier.COMING_SOON)
                .badge("👻 PRÓXIMAMENTE")
                .glow(0xFF4B0082)  // Indigo
                .build());

        Log.d(TAG, "📚 Catálogo inicializado: " +
                getCountByTier(WallpaperTier.FREE) + " FREE, " +
                getCountByTier(WallpaperTier.COMING_SOON) + " PRÓXIMAMENTE");
    }
}




