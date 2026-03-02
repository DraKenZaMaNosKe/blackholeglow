package com.secret.blackholeglow.scenes;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic static image wallpaper scene.
 * Configured via static config map - call configure(sceneName) after instantiation.
 */
public class ImageWallpaperScene extends BaseParallaxScene {

    // ═══════════════════════════════════════════════════════════════
    // STATIC CONFIGURATION MAP
    // ═══════════════════════════════════════════════════════════════

    private static final Map<String, ImageSceneConfig> CONFIGS = new HashMap<>();

    static {
        reg("GOKU_IMG", "Goku - Static", R.drawable.preview_goku, "goku_bg.webp", EqualizerBarsDJ.Theme.KAMEHAMEHA);
        reg("ADVENTURE_TIME_IMG", "Adventure Time - Static", R.drawable.hdapreview, "adventure_time_bg.webp", EqualizerBarsDJ.Theme.PYRALIS);
        reg("NEON_CITY_IMG", "Neon City - Static", R.drawable.preview_neoncity, "neon_city_bg.webp", EqualizerBarsDJ.Theme.SYNTHWAVE);
        reg("SAINT_SEIYA_IMG", "Saint Seiya - Static", R.drawable.preview_saintseiya, "saint_seiya_bg.webp", EqualizerBarsDJ.Theme.COSMOS);
        reg("WALKING_DEAD_IMG", "Walking Dead - Static", R.drawable.preview_walkingdead, "walking_dead_bg.webp", EqualizerBarsDJ.Theme.WALKING_DEAD);
        reg("ZELDA_BOTW_IMG", "Zelda BOTW - Static", R.drawable.preview_zelda, "zelda_botw_bg.webp", EqualizerBarsDJ.Theme.ZELDA);
        reg("SUPERMAN_IMG", "Superman - Static", R.drawable.preview_superman, "superman_bg.webp", EqualizerBarsDJ.Theme.SUPERMAN);
        reg("AOT_IMG", "Attack on Titan - Static", R.drawable.preview_aot, "aot_bg.webp", EqualizerBarsDJ.Theme.AOT);
        reg("SPIDER_IMG", "Spider - Static", R.drawable.preview_spider, "spider_bg.webp", EqualizerBarsDJ.Theme.SPIDER);
        reg("LOST_ATLANTIS_IMG", "Lost Atlantis - Static", R.drawable.preview_lost_atlantis, "lost_atlantis_bg.webp", EqualizerBarsDJ.Theme.ATLANTIS);
        reg("THE_HUMAN_PREDATOR_IMG", "Human Predator - Static", R.drawable.preview_human_predator, "human_predator_bg.webp", EqualizerBarsDJ.Theme.WALKING_DEAD);
        reg("MOONLIT_CAT_IMG", "Moonlit Cat - Static", R.drawable.preview_moonlit_cat, "moonlit_cat_bg.webp", EqualizerBarsDJ.Theme.ABYSSIA);
        reg("KEN_IMG", "Ken - Static", R.drawable.preview_ken, "ken_bg.webp", EqualizerBarsDJ.Theme.PYRALIS);
        reg("SCORPION_IMG", "Scorpion - Static", R.drawable.preview_scorpion, "scorpion_bg.webp", EqualizerBarsDJ.Theme.PYRALIS);
        reg("TREN_NOCTURNO_IMG", "Tren Nocturno - Static", R.drawable.preview_tren_nocturno, "tren_nocturno_bg.webp", EqualizerBarsDJ.Theme.SYNTHWAVE);
        reg("THE_EYE_IMG", "The Eye - Static", R.drawable.preview_the_eye, "the_eye_bg.webp", EqualizerBarsDJ.Theme.SPIDER);
        reg("GATITO_IMG", "Gatito - Static", R.drawable.preview_gatito, "gatito_bg.webp", EqualizerBarsDJ.Theme.DEFAULT);
        reg("GATITO_DJ_IMG", "Gatito DJ - Static", R.drawable.preview_gatito_dj, "gatito_dj_bg.webp", EqualizerBarsDJ.Theme.DEFAULT);
        reg("PIXEL_CITY_IMG", "Pixel City - Static", R.drawable.preview_pixel_city, "pixel_city_bg.webp", EqualizerBarsDJ.Theme.DEFAULT);
        reg("KRATOS_IMG", "Kratos - Static", R.drawable.preview_kratos, "kratos_bg.webp", EqualizerBarsDJ.Theme.KRATOS);
        reg("ITACHI_IMG", "Itachi - Static", R.drawable.preview_itachi, "itachi_bg.webp", EqualizerBarsDJ.Theme.DEFAULT);
        reg("SUPERCAMPEONES_IMG", "Supercampeones - Static", R.drawable.preview_supercampeones, "supercampeones_bg.webp", EqualizerBarsDJ.Theme.DEFAULT);
        reg("GOKU_KAMEHAME_IMG", "Goku Kamehameha - Static", R.drawable.preview_goku_kamehame, "goku_kamehame_bg.webp", EqualizerBarsDJ.Theme.KAMEHAMEHA);
    }

    private static void reg(String name, String desc, int preview, String imageFile, EqualizerBarsDJ.Theme theme) {
        CONFIGS.put(name, new ImageSceneConfig(name, desc, preview, imageFile, theme));
    }

    /**
     * Returns all registered scene names for SceneFactory registration.
     */
    public static java.util.Set<String> getRegisteredNames() {
        return CONFIGS.keySet();
    }

    /**
     * Returns the image filename for a given scene name (for ResourcePreloader).
     */
    public static String getImageFile(String sceneName) {
        ImageSceneConfig c = CONFIGS.get(sceneName);
        return c != null ? c.imageFile : null;
    }

    /**
     * Returns the preview resource ID for a given scene name (for ResourcePreloader).
     */
    public static int getPreviewRes(String sceneName) {
        ImageSceneConfig c = CONFIGS.get(sceneName);
        return c != null ? c.previewResId : R.drawable.preview_goku;
    }

    // ═══════════════════════════════════════════════════════════════
    // INSTANCE
    // ═══════════════════════════════════════════════════════════════

    private ImageSceneConfig config;

    public ImageWallpaperScene() {
        // Default - must call configure() after instantiation
    }

    /**
     * Called by SceneFactory after instantiation to set the scene identity.
     */
    public void configure(String sceneName) {
        this.config = CONFIGS.get(sceneName);
    }

    @Override
    public String getName() {
        return config != null ? config.name : "UNKNOWN_IMG";
    }

    @Override
    public String getDescription() {
        return config != null ? config.description : "Static Image";
    }

    @Override
    public int getPreviewResourceId() {
        return config != null ? config.previewResId : R.drawable.preview_goku;
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return config != null ? config.theme : EqualizerBarsDJ.Theme.DEFAULT;
    }

    @Override
    protected ParallaxLayer[] getLayers() {
        String imageFile = config != null ? config.imageFile : "goku_bg.webp";
        return new ParallaxLayer[] {
            new ParallaxLayer(imageFile, null, 0f, 1.0f, true)
        };
    }

    @Override
    protected float getGyroSensitivity() {
        return 0f;
    }

    // ═══════════════════════════════════════════════════════════════
    // CONFIG CLASS
    // ═══════════════════════════════════════════════════════════════

    private static class ImageSceneConfig {
        final String name;
        final String description;
        final int previewResId;
        final String imageFile;
        final EqualizerBarsDJ.Theme theme;

        ImageSceneConfig(String name, String description, int previewResId, String imageFile, EqualizerBarsDJ.Theme theme) {
            this.name = name;
            this.description = description;
            this.previewResId = previewResId;
            this.imageFile = imageFile;
            this.theme = theme;
        }
    }
}
