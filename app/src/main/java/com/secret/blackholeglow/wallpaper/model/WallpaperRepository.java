package com.secret.blackholeglow.wallpaper.model;

import com.secret.blackholeglow.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ═══════════════════════════════════════════════════════════════
 * WallpaperRepository - Fuente de Datos (MVC Pattern)
 * ═══════════════════════════════════════════════════════════════
 *
 * Singleton que gestiona el catálogo completo de wallpapers.
 *
 * Responsabilidades:
 *  - Proveer lista completa de wallpapers disponibles
 *  - Búsqueda por ID
 *  - Fácil agregar nuevos wallpapers en el futuro
 *
 * Patrón: Repository Pattern + Singleton
 */
public class WallpaperRepository {

    private static WallpaperRepository instance;
    private final List<Wallpaper> wallpapers;
    private final Map<String, Wallpaper> wallpaperMap;

    /**
     * Constructor privado (Singleton)
     */
    private WallpaperRepository() {
        wallpapers = new ArrayList<>();
        wallpaperMap = new HashMap<>();
        initializeWallpapers();
    }

    /**
     * Obtener instancia única (thread-safe lazy initialization)
     */
    public static synchronized WallpaperRepository getInstance() {
        if (instance == null) {
            instance = new WallpaperRepository();
        }
        return instance;
    }

    /**
     * Inicializar catálogo de wallpapers
     * ═══════════════════════════════════════════════════════════
     * 🎨 10 WALLPAPERS TEMÁTICOS CON COLORES ÚNICOS
     * ═══════════════════════════════════════════════════════════
     */
    private void initializeWallpapers() {
        // 🪩 0. DISCO BALL - Bola disco musical (NEGRO + colores rainbow)
        addWallpaper(new Wallpaper(
                "disco_ball",
                "DiscoBall",
                "🪩 Disco Ball",
                "Bola disco con cuadritos espejo que gira hipnóticamente. Reacciona a tu música con efectos pulsantes y colores rainbow. ¡La fiesta nunca termina! 🎵✨",
                R.drawable.preview_oceano_sc,
                "#000000"  // Negro
        ));

        // 🌌 1. UNIVERSO - Sistema solar completo (AZUL OSCURO espacial)
        addWallpaper(new Wallpaper(
                "universo",
                "Universo",
                "🌌 Universo Infinito",
                "Flota entre planetas orbitantes y estrellas danzantes en un universo infinito. Tu avatar explora el cosmos en tiempo real con efectos de brillo pulsante.",
                R.drawable.preview_oceano_sc,
                "#050520"  // Azul oscuro espacial
        ));

        // 🌲 2. BOSQUE ENCANTADO - Naturaleza mística (VERDE OSCURO)
        addWallpaper(new Wallpaper(
                "bosque",
                "Bosque Encantado",
                "🌲 Bosque Encantado",
                "Árboles místicos brillan con luciérnagas danzantes. Neblina mágica flota entre las ramas mientras la luna ilumina el camino. Efecto: partículas de luz flotando.",
                R.drawable.preview_oceano_sc,
                "#0A2F1F"  // Verde bosque oscuro
        ));

        // 🏙️ 3. CIUDAD CYBERPUNK - Metrópolis futurista (ROSA NEÓN)
        addWallpaper(new Wallpaper(
                "cyberpunk",
                "Neo Tokyo 2099",
                "🏙️ Neo Tokyo 2099",
                "Rascacielos neón se elevan al cielo nocturno. Luces holográficas parpadean y vehículos vuelan entre edificios. Atmósfera: lluvia cyberpunk con reflejos.",
                R.drawable.preview_oceano_sc,
                "#FF0080"  // Rosa neón cyberpunk
        ));

        // 🏖️ 4. PLAYA TROPICAL - Atardecer paradisíaco (NARANJA DORADO)
        addWallpaper(new Wallpaper(
                "playa",
                "Paraíso Dorado",
                "🏖️ Paraíso Dorado",
                "El sol se funde con el océano en un atardecer de ensueño. Olas brillantes acarician la arena mientras palmeras se mecen. Colores cálidos que hipnotizan.",
                R.drawable.preview_portal_cosmico,
                "#FF8C00"  // Naranja dorado
        ));

        // 🦁 5. SAFARI SALVAJE - Sabana africana (AMARILLO TIERRA)
        addWallpaper(new Wallpaper(
                "safari",
                "Safari Salvaje",
                "🦁 Safari Salvaje",
                "Leones, elefantes y jirafas deambulan bajo el sol africano. Siluetas de animales se mueven grácilmente en la sabana dorada. Vida en cada píxel.",
                R.drawable.preview_oceano_sc,
                "#DAA520"  // Amarillo tierra/savanna
        ));

        // 🌧️ 6. LLUVIA NOCTURNA - Tormenta misteriosa (GRIS AZULADO)
        addWallpaper(new Wallpaper(
                "lluvia",
                "Lluvia Mística",
                "🌧️ Lluvia Mística",
                "Gotas de lluvia caen entre árboles oscuros. Relámpagos iluminan el cielo mientras truenos resuenan. Ambiente: tormentoso y relajante a la vez.",
                R.drawable.preview_oceano_sc,
                "#2F4F4F"  // Gris pizarra tormentoso
        ));

        // 🎮 7. MUNDO RETRO - Pixel art 8-bit (MAGENTA RETRO)
        addWallpaper(new Wallpaper(
                "retro",
                "Pixel Quest",
                "🎮 Pixel Quest",
                "Personajes de 8-bit saltan y corren en un mundo retro. Monedas giran, bloques parpadean y enemigos patrullan. Nostalgia gaming en movimiento.",
                R.drawable.preview_oceano_sc,
                "#FF00FF"  // Magenta retro gaming
        ));

        // 🕳️ 8. AGUJERO NEGRO - Portal cósmico (MORADO PROFUNDO)
        addWallpaper(new Wallpaper(
                "agujero_negro",
                "Agujero Negro",
                "🕳️ Portal Infinito",
                "Un agujero negro devora la luz con su disco de acreción brillante. El espacio-tiempo se curva ante tus ojos. Efecto: distorsión gravitacional hipnótica.",
                R.drawable.preview_oceano_sc,
                "#4B0082"  // Índigo/morado profundo
        ));

        // 🌸 9. JARDÍN ZEN - Serenidad japonesa (ROSA SAKURA)
        addWallpaper(new Wallpaper(
                "zen",
                "Jardín Zen",
                "🌸 Jardín Zen",
                "Pétalos de sakura flotan sobre un estanque tranquilo. Koi nadan en círculos mientras bambú se mece. Paz absoluta en cada fotograma.",
                R.drawable.preview_oceano_sc,
                "#FFB7C5"  // Rosa sakura suave
        ));

        // ⚡ 10. TORMENTA ELÉCTRICA - Furia elemental (AMARILLO ELÉCTRICO)
        addWallpaper(new Wallpaper(
                "tormenta",
                "Furia Celestial",
                "⚡ Furia Celestial",
                "Rayos fractales iluminan nubes tormentosas. Energía pura danza en el cielo mientras relámpagos explotan. El poder elemental al máximo.",
                R.drawable.preview_portal_cosmico,
                "#FFFF00"  // Amarillo eléctrico brillante
        ));
    }

    /**
     * Agregar wallpaper al repositorio
     */
    private void addWallpaper(Wallpaper wallpaper) {
        wallpapers.add(wallpaper);
        wallpaperMap.put(wallpaper.getId(), wallpaper);
    }

    // ═══════════════════════════════════════════════════════════
    // API Pública
    // ═══════════════════════════════════════════════════════════

    /**
     * Obtener todos los wallpapers (immutable)
     */
    public List<Wallpaper> getAllWallpapers() {
        return Collections.unmodifiableList(wallpapers);
    }

    /**
     * Buscar wallpaper por ID
     */
    public Wallpaper getWallpaperById(String id) {
        return wallpaperMap.get(id);
    }

    /**
     * Buscar wallpaper por nombre (SceneRenderer usa "name")
     */
    public Wallpaper getWallpaperByName(String name) {
        for (Wallpaper w : wallpapers) {
            if (w.getName().equals(name)) {
                return w;
            }
        }
        return null;
    }

    /**
     * Obtener total de wallpapers
     */
    public int getWallpaperCount() {
        return wallpapers.size();
    }
}
