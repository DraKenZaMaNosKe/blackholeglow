package com.secret.blackholeglow.systems;

import android.util.Log;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.models.SceneWeight;
import com.secret.blackholeglow.models.WallpaperCategory;
import com.secret.blackholeglow.models.WallpaperItem;
import com.secret.blackholeglow.models.WallpaperTier;

import android.content.Context;

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

    private static volatile WallpaperCatalog instance;

    public static synchronized WallpaperCatalog get() {
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
    // DYNAMIC CATALOG
    // ═══════════════════════════════════════════════════════════════

    private final List<WallpaperItem> dynamicItems = new ArrayList<>();

    /**
     * Loads dynamic wallpapers from DynamicCatalog cache and appends to catalog.
     * Call after DynamicCatalog.refresh() or on app startup.
     */
    public synchronized void loadDynamicEntries(Context context) {
        // Remove previous dynamic entries
        catalog.removeAll(dynamicItems);
        dynamicItems.clear();

        List<WallpaperItem> items = DynamicCatalog.get().toWallpaperItems(context);
        dynamicItems.addAll(items);
        catalog.addAll(items);

        Log.d(TAG, "Dynamic entries loaded: " + items.size());
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. OBTENER TODOS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Obtiene todos los wallpapers del catálogo
     * @return Lista inmutable de todos los wallpapers
     */
    public synchronized List<WallpaperItem> getAll() {
        return new ArrayList<>(catalog);
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. FILTRAR POR CATEGORÍA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Obtiene wallpapers de una categoría específica.
     * Si la categoría es ALL, retorna todos.
     */
    public synchronized List<WallpaperItem> getByCategory(WallpaperCategory category) {
        if (category == WallpaperCategory.ALL) {
            return new ArrayList<>(catalog);
        }
        List<WallpaperItem> result = new ArrayList<>();
        for (WallpaperItem item : catalog) {
            if (item.getCategory() == category) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Retorna las categorías que tienen al menos 1 wallpaper.
     * Siempre incluye ALL como primera entrada.
     */
    public List<WallpaperCategory> getAvailableCategories() {
        List<WallpaperCategory> available = new ArrayList<>();
        available.add(WallpaperCategory.ALL);
        for (WallpaperCategory cat : WallpaperCategory.values()) {
            if (cat == WallpaperCategory.ALL) continue;
            for (WallpaperItem item : catalog) {
                if (item.getCategory() == cat) {
                    available.add(cat);
                    break;
                }
            }
        }
        return available;
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. FILTRAR POR TIER
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
    public synchronized WallpaperItem getBySceneName(String sceneName) {
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
    public static synchronized void reset() {
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
                .descripcion("Descend into the depths of an alien ocean. " +
                        "Ancestral bioluminescence and otherworldly creatures.")
                .preview(R.drawable.preview_oceano_sc)
                .sceneName("ABYSSIA")
                .tier(WallpaperTier.FREE)
                .badge("🌊 NEW")
                .glow(0xFF00CED1)  // Dark Turquoise
                .weight(SceneWeight.HEAVY)  // Video + 2 OBJ models + particulas = genuinamente pesado
                .category(WallpaperCategory.NATURE)
                .featured()
                .build());

        // 🔥 PYRALIS - Portal de fuego cósmico
        catalog.add(new WallpaperItem.Builder("✦ PYRALIS ✦")
                .descripcion("Cross through the eternal fire portal. " +
                        "Blazing cosmic clouds and unknown dimensions.")
                .preview(R.drawable.preview_portal_cosmico)
                .sceneName("PYRALIS")
                .tier(WallpaperTier.FREE)
                .badge("🔥 POPULAR")
                .glow(0xFFFF4500)  // Orange Red
                .weight(SceneWeight.MEDIUM)
                .category(WallpaperCategory.UNIVERSE)
                .featured()
                .build());

        // 🐉 GOKU - Dragon Ball Kamehameha
        catalog.add(new WallpaperItem.Builder("✧ GOKU ✧")
                .descripcion("KAME-HAME-HAAA! Unleash the power of Ultra Instinct. " +
                        "The mightiest Ki energy in the universe.")
                .preview(R.drawable.preview_goku)
                .sceneName("GOKU")
                .tier(WallpaperTier.FREE)
                .badge("🐉 NEW")
                .glow(0xFF00BFFF)  // Deep Sky Blue (energía Ki)
                .weight(SceneWeight.LIGHT)
                .category(WallpaperCategory.ANIME)
                .featured()
                .build());

        // 🌳 ADVENTURE TIME - Fogata con Finn y Jake
        catalog.add(new WallpaperItem.Builder("✧ ADVENTURE TIME ✧")
                .descripcion("Adventure Time! Finn, Jake, Tree Trunks and Princess Bubblegum " +
                        "around a cozy campfire in the woods.")
                .preview(R.drawable.hdapreview)
                .sceneName("ADVENTURE_TIME")
                .tier(WallpaperTier.FREE)
                .badge("🌳 NEW")
                .glow(0xFFFF8C00)  // Dark Orange (fogata)
                .weight(SceneWeight.LIGHT)
                .category(WallpaperCategory.ANIME)
                .featured()
                .build());

        // 🚗 NEON CITY - Synthwave DeLorean
        catalog.add(new WallpaperItem.Builder("✧ NEON CITY ✧")
                .descripcion("Endless highway toward the neon horizon. " +
                        "DeLorean, palm trees and an eternal 80s synthwave sun.")
                .preview(R.drawable.preview_neoncity)
                .sceneName("NEON_CITY")
                .tier(WallpaperTier.FREE)
                .badge("🚗 NEW")
                .glow(0xFFFF1493)  // Deep Pink (synthwave)
                .weight(SceneWeight.MEDIUM)
                .category(WallpaperCategory.SCENES)
                .featured()
                .build());

        // ⭐ SAINT SEIYA - Caballeros del Zodiaco
        catalog.add(new WallpaperItem.Builder("⭐ SAINT SEIYA ⭐")
                .descripcion("Ignite your cosmos! The Knights of the Zodiac " +
                        "protect Athena with golden energy and constellations.")
                .preview(R.drawable.preview_saintseiya)
                .sceneName("SAINT_SEIYA")
                .tier(WallpaperTier.FREE)
                .badge("⭐ COSMOS")
                .glow(0xFFFFD700)  // Gold (cosmos)
                .weight(SceneWeight.MEDIUM)
                .category(WallpaperCategory.ANIME)
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // 🧟 THE WALKING DEAD - Cementerio Zombie Apocalíptico
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("🧟 THE WALKING DEAD 🧟")
                .descripcion("Cursed cemetery under the full moon. " +
                        "Zombie hands rising through green and purple fog.")
                .preview(R.drawable.preview_walkingdead)
                .sceneName("WALKING_DEAD")
                .tier(WallpaperTier.FREE)
                .badge("🧟 ZOMBIE")
                .glow(0xFF00FF00)  // Green zombie
                .weight(SceneWeight.MEDIUM)
                .category(WallpaperCategory.SCENES)
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // 🗡️ ZELDA BOTW - Parallax Breath of the Wild
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("🗡️ ZELDA BOTW 🗡️")
                .descripcion("Link gazes upon the kingdom of Hyrule from above. " +
                        "Gyroscope parallax effect Breath of the Wild style.")
                .preview(R.drawable.preview_zelda)
                .sceneName("ZELDA_BOTW")
                .tier(WallpaperTier.FREE)
                .badge("🛡️ PARALLAX")
                .glow(0xFF4CAF50)  // Zelda green
                .weight(SceneWeight.MEDIUM)  // Sin video: parallax layers + 1 OBJ = MEDIUM
                .category(WallpaperCategory.GAMING)
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // 🦸 SUPERMAN - Man of Steel
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("🦸 SUPERMAN 🦸")
                .descripcion("The Man of Steel soars through the skies with his cape flowing. " +
                        "Power, hope and justice in every frame.")
                .preview(R.drawable.preview_superman)
                .sceneName("SUPERMAN")
                .tier(WallpaperTier.FREE)
                .badge("🦸 HERO")
                .glow(0xFFDC143C)  // Superman red
                .weight(SceneWeight.LIGHT)
                .category(WallpaperCategory.ANIME)
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // ⚔️ AOT - Attack on Titan (Eren Jaeger)
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("⚔️ ATTACK ON TITAN ⚔️")
                .descripcion("Eren Jaeger with the Colossal Titan. " +
                        "Humanity strikes back. Shinzou wo Sasageyo!")
                .preview(R.drawable.preview_aot)
                .sceneName("AOT")
                .tier(WallpaperTier.FREE)
                .badge("⚔️ TITAN")
                .glow(0xFF00E5B0)  // Eren's green eyes
                .weight(SceneWeight.LIGHT)
                .category(WallpaperCategory.ANIME)
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // 🕷️ SPIDER - Black Spider Horror
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("🕷️ BLACK SPIDER 🕷️")
                .descripcion("Giant spider with glowing red eyes. " +
                        "Arachnophobic terror in the deepest darkness.")
                .preview(R.drawable.preview_spider)
                .sceneName("SPIDER")
                .tier(WallpaperTier.FREE)
                .badge("🕷️ HORROR")
                .glow(0xFFDC143C)  // Crimson red (spider eyes)
                .weight(SceneWeight.LIGHT)
                .category(WallpaperCategory.ANIMALS)
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // 🏛️ LOST ATLANTIS - Ciudad Sumergida
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("🏛️ LOST ATLANTIS 🏛️")
                .descripcion("Ancient temple submerged in turquoise waters. " +
                        "Mystic energy, cherry blossom petals and golden lanterns.")
                .preview(R.drawable.preview_lost_atlantis)
                .sceneName("LOST_ATLANTIS")
                .tier(WallpaperTier.FREE)
                .badge("🌊 NEW")
                .glow(0xFF00CED1)  // Dark Turquoise
                .weight(SceneWeight.LIGHT)  // Video-only, sin modelos 3D = LIGHT
                .category(WallpaperCategory.SCENES)
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // 🦁 THE HUMAN PREDATOR - Guerrero vs León
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("🦁 THE HUMAN PREDATOR 🦁")
                .descripcion("Prehistoric warrior with venomous claws vs giant lion. " +
                        "Volcanic jungle, epic battle to the death.")
                .preview(R.drawable.preview_human_predator)
                .sceneName("THE_HUMAN_PREDATOR")
                .tier(WallpaperTier.FREE)
                .badge("🦁 NEW")
                .glow(0xFF8B0000)  // Dark Red (sangre)
                .weight(SceneWeight.LIGHT)  // Video-only, sin modelos 3D = LIGHT
                .category(WallpaperCategory.SCENES)
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // 🌙 MOONLIT CAT - Gato Negro Bajo la Luna
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("🌙 MOONLIT CAT 🌙")
                .descripcion("Black cat sitting on a fence gazing at the giant moon. " +
                        "Night sky with twinkling stars and building silhouettes.")
                .preview(R.drawable.preview_moonlit_cat)
                .sceneName("MOONLIT_CAT")
                .tier(WallpaperTier.FREE)
                .badge("🌙 NEW")
                .glow(0xFF4169E1)  // Royal Blue (moonlight)
                .weight(SceneWeight.MEDIUM)  // Sin video: shaders + 2 OBJ + texturas optimizadas = MEDIUM
                .category(WallpaperCategory.ANIMALS)
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // 💜 FRIEZA DEATH BEAM - Dragon Ball Z
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("💜 FRIEZA DEATH BEAM 💜")
                .descripcion("Frieza Final Form firing his Death Beam. " +
                        "3D model with animated energy ray and anime background.")
                .preview(R.drawable.preview_frieza_deathbeam)
                .sceneName("FRIEZA_DEATHBEAM")
                .tier(WallpaperTier.FREE)
                .badge("💜 NEW")
                .glow(0xFF8B00FF)  // Purple (Frieza energy)
                .weight(SceneWeight.MEDIUM)
                .category(WallpaperCategory.ANIME)
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // 🥊 KEN - Side Scroll Fighter
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("🥊 KEN 🥊")
                .descripcion("Street Fighter pixel art with side scroll. " +
                        "Ken walks and throws hadoukens with retro parallax.")
                .preview(R.drawable.preview_ken)
                .sceneName("KEN")
                .tier(WallpaperTier.FREE)
                .badge("🥊 NEW")
                .glow(0xFFFF4500)  // Orange Red (Ken's gi)
                .weight(SceneWeight.LIGHT)
                .category(WallpaperCategory.GAMING)
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // 🦂 SCORPION - Mortal Kombat
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("🦂 SCORPION 🦂")
                .descripcion("GET OVER HERE! Scorpion from Mortal Kombat. " +
                        "Hellfire and the deadliest ninja of the Netherrealm.")
                .preview(R.drawable.preview_scorpion)
                .sceneName("SCORPION")
                .tier(WallpaperTier.FREE)
                .badge("🔥 NEW")
                .glow(0xFFFF8C00)  // Dark Orange (Scorpion fire)
                .weight(SceneWeight.LIGHT)
                .category(WallpaperCategory.GAMING)
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // 🚂 TREN NOCTURNO - Pixel Art Night Train
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("🚂 TREN NOCTURNO 🚂")
                .descripcion("Pixel train crossing the starry night. " +
                        "Warm lights and nocturnal landscapes in retro style.")
                .preview(R.drawable.preview_tren_nocturno)
                .sceneName("TREN_NOCTURNO")
                .tier(WallpaperTier.FREE)
                .badge("🌃 NEW")
                .glow(0xFF6A0DAD)  // Purple (night sky)
                .weight(SceneWeight.LIGHT)
                .category(WallpaperCategory.SCENES)
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // 👁️ THE EYE - Ojo Misterioso
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("👁️ THE EYE 👁️")
                .descripcion("Giant eye with hypnotic and detailed iris. " +
                        "Mystery, depth and a gaze that sees all.")
                .preview(R.drawable.preview_the_eye)
                .sceneName("THE_EYE")
                .tier(WallpaperTier.FREE)
                .badge("👁️ NEW")
                .glow(0xFF00FF7F)  // Spring Green (iris glow)
                .weight(SceneWeight.LIGHT)
                .category(WallpaperCategory.UNIVERSE)
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // 🐱 GATITO - Cute Cat Animation
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("🐱 GATITO 🐱")
                .descripcion("Adorable kitten in a charming animation. " +
                        "Pure cuteness for your home screen.")
                .preview(R.drawable.preview_gatito)
                .sceneName("GATITO")
                .tier(WallpaperTier.FREE)
                .badge("🐱 NEW")
                .glow(0xFFFFB6C1)  // Light Pink (cute)
                .weight(SceneWeight.LIGHT)
                .category(WallpaperCategory.ANIMALS)
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // 🎧 GATITO DJ - Dancing DJ Cat
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("🎧 GATITO DJ 🎧")
                .descripcion("The coolest DJ cat in the universe! " +
                        "Dancing and spinning beats with neon lights.")
                .preview(R.drawable.preview_gatito_dj)
                .sceneName("GATITO_DJ")
                .tier(WallpaperTier.FREE)
                .badge("🎧 NEW")
                .glow(0xFFDA70D6)  // Orchid purple (neon)
                .weight(SceneWeight.LIGHT)
                .category(WallpaperCategory.ANIMALS)
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // 🏙️ PIXEL CITY - Retro Pixel Art City
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("🏙️ PIXEL CITY 🏙️")
                .descripcion("Retro pixel art city glowing under the night sky. " +
                        "Neon lights and nostalgic 8-bit vibes.")
                .preview(R.drawable.preview_pixel_city)
                .sceneName("PIXEL_CITY")
                .tier(WallpaperTier.FREE)
                .badge("🏙️ NEW")
                .glow(0xFF00BFFF)  // Deep Sky Blue (neon city)
                .weight(SceneWeight.LIGHT)
                .category(WallpaperCategory.SCENES)
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // ⚔️ KRATOS - God of War
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("⚔️ KRATOS ⚔️")
                .descripcion("BOY! The Ghost of Sparta unleashes divine fury. " +
                        "Blades of Chaos burn with the rage of a god.")
                .preview(R.drawable.preview_kratos)
                .sceneName("KRATOS")
                .tier(WallpaperTier.FREE)
                .badge("⚔️ NEW")
                .glow(0xFFCC2200)  // Rojo oscuro (sangre espartana)
                .weight(SceneWeight.LIGHT)
                .category(WallpaperCategory.GAMING)
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // ⚔️ KRATOS vs CYCLOPS - God of War (Imagen + Aura)
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("⚔️ KRATOS vs CYCLOPS")
                .descripcion("The Ghost of Sparta leaps into battle against a colossal cyclops. " +
                        "Green cursed energy radiates from the ancient beast.")
                .preview(R.drawable.preview_kratos_cyclops)
                .sceneName("KRATOS_CYCLOPS")
                .tier(WallpaperTier.FREE)
                .badge("⚔️ NEW")
                .glow(0xFF00CC44)  // Verde energía del cíclope
                .weight(SceneWeight.LIGHT)
                .category(WallpaperCategory.GAMING)
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // 👁️ ITACHI - Sharingan Eye
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("👁️ ITACHI")
                .descripcion("Sharingan awakened.")
                .preview(R.drawable.preview_itachi)
                .sceneName("ITACHI")
                .tier(WallpaperTier.FREE)
                .badge("NEW")
                .glow(0xFFCC0000)  // Rojo Sharingan
                .weight(SceneWeight.LIGHT)
                .category(WallpaperCategory.ANIME)
                .featured()
                .build());

        // ═══════════════════════════════════════════════════════════════════
        // ⚽ SUPERCAMPEONES - Captain Tsubasa
        // ═══════════════════════════════════════════════════════════════════
        catalog.add(new WallpaperItem.Builder("⚽ SUPERCAMPEONES")
                .descripcion("Super Strike!")
                .preview(R.drawable.preview_supercampeones)
                .sceneName("SUPERCAMPEONES")
                .tier(WallpaperTier.FREE)
                .badge("NEW")
                .glow(0xFFFFAA00)  // Dorado fuego
                .weight(SceneWeight.LIGHT)
                .category(WallpaperCategory.ANIME)
                .featured()
                .build());

        Log.d(TAG, "📚 Catálogo inicializado: " + catalog.size() + " wallpapers");
    }
}




