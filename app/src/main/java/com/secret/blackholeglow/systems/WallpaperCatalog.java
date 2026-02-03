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
        // ║  Solo wallpapers funcionales y probados                    ║
        // ╚════════════════════════════════════════════════════════════╝

        // ─────────────────────────────────────────────────────────────
        // 🆓 WALLPAPERS DISPONIBLES
        // ─────────────────────────────────────────────────────────────

        // 🌊 ABYSSIA - Océano alienígena con video de fondo
        catalog.add(new WallpaperItem.Builder("✧ ABYSSIA ✧")
                .descripcion("Desciende a las profundidades de un océano alienígena. " +
                        "Bioluminiscencia ancestral y criaturas de otro mundo.")
                .preview(R.drawable.preview_oceano_sc)
                .sceneName("ABYSSIA")
                .tier(WallpaperTier.FREE)
                .badge("🌊 NUEVO")
                .glow(0xFF00CED1)  // Dark Turquoise
                .featured()
                .build());

        // 🔥 PYRALIS - Portal de fuego cósmico
        catalog.add(new WallpaperItem.Builder("✦ PYRALIS ✦")
                .descripcion("Atraviesa el portal de fuego eterno. " +
                        "Nubes cósmicas ardientes y dimensiones desconocidas.")
                .preview(R.drawable.preview_portal_cosmico)
                .sceneName("PYRALIS")
                .tier(WallpaperTier.FREE)
                .badge("🔥 POPULAR")
                .glow(0xFFFF4500)  // Orange Red
                .featured()
                .build());

        // 🐉 GOKU - Dragon Ball Kamehameha
        catalog.add(new WallpaperItem.Builder("✧ GOKU ✧")
                .descripcion("¡KAME-HAME-HAAA! Libera el poder del Ultra Instinto. " +
                        "La energía Ki más poderosa del universo.")
                .preview(R.drawable.preview_goku)
                .sceneName("GOKU")
                .tier(WallpaperTier.FREE)
                .badge("🐉 NUEVO")
                .glow(0xFF00BFFF)  // Deep Sky Blue (energía Ki)
                .featured()
                .build());

        // 🌳 ADVENTURE TIME - Fogata con Finn y Jake
        catalog.add(new WallpaperItem.Builder("✧ ADVENTURE TIME ✧")
                .descripcion("¡Hora de Aventura! Finn, Jake, Tronquitos y Dulce Princesa " +
                        "alrededor de una acogedora fogata en el bosque.")
                .preview(R.drawable.hdapreview)
                .sceneName("ADVENTURE_TIME")
                .tier(WallpaperTier.FREE)
                .badge("🌳 NUEVO")
                .glow(0xFFFF8C00)  // Dark Orange (fogata)
                .featured()
                .build());

        // 🚗 NEON CITY - Synthwave DeLorean
        catalog.add(new WallpaperItem.Builder("✧ NEON CITY ✧")
                .descripcion("Carretera infinita hacia el horizonte neón. " +
                        "DeLorean, palmeras y un sol eterno estilo synthwave 80s.")
                .preview(R.drawable.preview_neoncity)
                .sceneName("NEON_CITY")
                .tier(WallpaperTier.FREE)
                .badge("🚗 NUEVO")
                .glow(0xFFFF1493)  // Deep Pink (synthwave)
                .featured()
                .build());

        // ⭐ SAINT SEIYA - Caballeros del Zodiaco
        catalog.add(new WallpaperItem.Builder("⭐ SAINT SEIYA ⭐")
                .descripcion("¡Enciende tu cosmos! Los Caballeros del Zodiaco " +
                        "protegen a Athena con energía dorada y constelaciones.")
                .preview(R.drawable.preview_saintseiya)
                .sceneName("SAINT_SEIYA")
                .tier(WallpaperTier.FREE)
                .badge("⭐ COSMOS")
                .glow(0xFFFFD700)  // Gold (cosmos)
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // 🧟 THE WALKING DEAD - Cementerio Zombie Apocalíptico
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("🧟 THE WALKING DEAD 🧟")
                .descripcion("Cementerio maldito bajo la luna llena. " +
                        "Manos zombie emergiendo entre niebla verde y púrpura.")
                .preview(R.drawable.preview_walkingdead)
                .sceneName("WALKING_DEAD")
                .tier(WallpaperTier.FREE)
                .badge("🧟 ZOMBIE")
                .glow(0xFF00FF00)  // Green zombie
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // 🗡️ ZELDA BOTW - Parallax Breath of the Wild
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("🗡️ ZELDA BOTW 🗡️")
                .descripcion("Link contempla el reino de Hyrule desde lo alto. " +
                        "Efecto parallax con giroscopio estilo Breath of the Wild.")
                .preview(R.drawable.preview_zelda)
                .sceneName("ZELDA_BOTW")
                .tier(WallpaperTier.FREE)
                .badge("🛡️ PARALLAX")
                .glow(0xFF4CAF50)  // Zelda green
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // 🦸 SUPERMAN - Man of Steel
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("🦸 SUPERMAN 🦸")
                .descripcion("El Hombre de Acero surca los cielos con su capa ondeando. " +
                        "Poder, esperanza y justicia en cada frame.")
                .preview(R.drawable.preview_superman)
                .sceneName("SUPERMAN")
                .tier(WallpaperTier.FREE)
                .badge("🦸 HERO")
                .glow(0xFFDC143C)  // Superman red
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // ⚔️ AOT - Attack on Titan (Eren Jaeger)
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("⚔️ ATTACK ON TITAN ⚔️")
                .descripcion("Eren Jaeger con el Titán Colosal. " +
                        "La humanidad contraataca. Shinzou wo Sasageyo!")
                .preview(R.drawable.preview_aot)
                .sceneName("AOT")
                .tier(WallpaperTier.FREE)
                .badge("⚔️ TITAN")
                .glow(0xFF00E5B0)  // Eren's green eyes
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // 🕷️ SPIDER - Black Spider Horror
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("🕷️ BLACK SPIDER 🕷️")
                .descripcion("Araña gigante con ojos rojos brillantes. " +
                        "Terror aracnofóbico en la oscuridad más profunda.")
                .preview(R.drawable.preview_spider)
                .sceneName("SPIDER")
                .tier(WallpaperTier.FREE)
                .badge("🕷️ HORROR")
                .glow(0xFFDC143C)  // Crimson red (spider eyes)
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // 🏛️ LOST ATLANTIS - Ciudad Sumergida
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("🏛️ LOST ATLANTIS 🏛️")
                .descripcion("Templo ancestral sumergido en aguas turquesa. " +
                        "Energía mística, pétalos de cerezo y linternas doradas.")
                .preview(R.drawable.preview_lost_atlantis)
                .sceneName("LOST_ATLANTIS")
                .tier(WallpaperTier.FREE)
                .badge("🌊 NUEVO")
                .glow(0xFF00CED1)  // Dark Turquoise
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // 🦁 THE HUMAN PREDATOR - Guerrero vs León
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("🦁 THE HUMAN PREDATOR 🦁")
                .descripcion("Guerrero prehistórico con garras venenosas vs león gigante. " +
                        "Jungla volcánica, combate épico a muerte.")
                .preview(R.drawable.preview_human_predator)
                .sceneName("THE_HUMAN_PREDATOR")
                .tier(WallpaperTier.FREE)
                .badge("🦁 NUEVO")
                .glow(0xFF8B0000)  // Dark Red (sangre)
                .featured()
                .build());

        Log.d(TAG, "📚 Catálogo inicializado: " + catalog.size() + " wallpapers");
    }
}




