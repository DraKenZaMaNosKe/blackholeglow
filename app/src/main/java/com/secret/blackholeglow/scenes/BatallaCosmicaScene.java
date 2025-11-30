package com.secret.blackholeglow.scenes;

import android.content.Context;
import android.util.Log;

import com.secret.blackholeglow.AvatarLoader;
import com.secret.blackholeglow.AvatarSphere;
import com.secret.blackholeglow.BatteryPowerBar;
import com.secret.blackholeglow.CameraAware;
import com.secret.blackholeglow.CameraController;
import com.secret.blackholeglow.EarthShield;
import com.secret.blackholeglow.EstrellaBailarina;
import com.secret.blackholeglow.ForceField;
import com.secret.blackholeglow.GreetingText;
import com.secret.blackholeglow.HPBar;
import com.secret.blackholeglow.LeaderboardManager;
import com.secret.blackholeglow.MeteorShower;
import com.secret.blackholeglow.MusicIndicator;
import com.secret.blackholeglow.Planeta;
import com.secret.blackholeglow.PlayerStats;
import com.secret.blackholeglow.PlayerWeapon;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.SceneObject;
import com.secret.blackholeglow.SimpleTextRenderer;
import com.secret.blackholeglow.SolProcedural;
import com.secret.blackholeglow.Spaceship3D;
import com.secret.blackholeglow.StarryBackground;
import com.secret.blackholeglow.SunHeatEffect;
import com.secret.blackholeglow.TextureManager;

import java.util.ArrayList;
import java.util.List;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   🚀 BatallaCosmicaScene - Escena de Batalla Espacial Modular    ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * Esta escena es una COPIA MODULAR de UniverseScene, implementada
 * usando la arquitectura de WallpaperScene para servir como template
 * para futuras escenas con lógica de juego.
 *
 * CARACTERÍSTICAS:
 * - 🌍 Planeta Tierra con HP y sistema de daño
 * - 🛡️ Campo de fuerza protector
 * - ☀️ Sol procedural con efectos de calor
 * - 🛸 OVNI con IA y armas láser
 * - ☄️ Sistema de meteoritos
 * - 🎮 Arma del jugador
 * - 🎵 Indicador de música reactivo
 * - 🏆 Sistema de leaderboard
 * - ✨ Efectos visuales varios
 */
public class BatallaCosmicaScene extends WallpaperScene implements Planeta.OnExplosionListener {

    private static final String TAG = "BatallaCosmicaScene";

    // ═══════════════════════════════════════════════════════════════
    // 🎮 REFERENCIAS DE OBJETOS DEL JUEGO
    // ═══════════════════════════════════════════════════════════════
    private Planeta tierra;
    private Planeta planetaTierra;  // Referencia para colisiones
    private ForceField forceField;
    private EarthShield earthShield;
    private Spaceship3D ovni;
    private MeteorShower meteorShower;
    private PlayerWeapon playerWeapon;
    private BatteryPowerBar powerBar;

    // ═══════════════════════════════════════════════════════════════
    // 📊 UI Y ESTADÍSTICAS
    // ═══════════════════════════════════════════════════════════════
    private HPBar hpBarTierra;
    private HPBar hpBarForceField;
    private MusicIndicator musicIndicator;
    private SimpleTextRenderer planetsDestroyedCounter;
    private SimpleTextRenderer[] leaderboardTexts = new SimpleTextRenderer[3];

    // ═══════════════════════════════════════════════════════════════
    // ✨ EFECTOS VISUALES
    // ═══════════════════════════════════════════════════════════════
    private List<EstrellaBailarina> estrellasBailarinas = new ArrayList<>();

    // ═══════════════════════════════════════════════════════════════
    // 🏆 LEADERBOARD
    // ═══════════════════════════════════════════════════════════════
    private LeaderboardManager leaderboardManager;

    // ═══════════════════════════════════════════════════════════════
    // 📊 ESTADÍSTICAS DEL JUGADOR
    // ═══════════════════════════════════════════════════════════════
    private PlayerStats playerStats;

    @Override
    public String getName() {
        return "Batalla Cósmica";
    }

    @Override
    public String getDescription() {
        return "Defiende la Tierra de meteoritos y naves alienígenas en esta épica batalla espacial.";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.universo03;  // Usar mismo preview que Universo por ahora
    }

    @Override
    protected void setupScene() {
        Log.d(TAG, "╔════════════════════════════════════════════════════════╗");
        Log.d(TAG, "║   🚀 BATALLA CÓSMICA - ESCENA MODULAR                 ║");
        Log.d(TAG, "╚════════════════════════════════════════════════════════╝");

        // Obtener PlayerStats
        playerStats = PlayerStats.getInstance(context);

        // ═══════════════════════════════════════════════════════════
        // 1️⃣ FONDO ESTRELLADO
        // ═══════════════════════════════════════════════════════════
        setupBackground();

        // ═══════════════════════════════════════════════════════════
        // 2️⃣ SOL PROCEDURAL
        // ═══════════════════════════════════════════════════════════
        setupSun();

        // ═══════════════════════════════════════════════════════════
        // 3️⃣ PLANETA TIERRA
        // ═══════════════════════════════════════════════════════════
        setupEarth();

        // ═══════════════════════════════════════════════════════════
        // 4️⃣ ESCUDO Y CAMPO DE FUERZA
        // ═══════════════════════════════════════════════════════════
        setupShields();

        // ═══════════════════════════════════════════════════════════
        // 5️⃣ OVNI CON IA
        // ═══════════════════════════════════════════════════════════
        setupOvni();

        // ═══════════════════════════════════════════════════════════
        // 6️⃣ ESTRELLAS BAILARINAS
        // ═══════════════════════════════════════════════════════════
        setupDancingStars();

        // ═══════════════════════════════════════════════════════════
        // 7️⃣ UI ELEMENTS
        // ═══════════════════════════════════════════════════════════
        setupUI();

        // ═══════════════════════════════════════════════════════════
        // 8️⃣ SISTEMA DE METEORITOS
        // ═══════════════════════════════════════════════════════════
        setupMeteorSystem();

        // ═══════════════════════════════════════════════════════════
        // 9️⃣ SISTEMA DE ARMAS
        // ═══════════════════════════════════════════════════════════
        setupWeaponSystem();

        // ═══════════════════════════════════════════════════════════
        // 🔟 AVATAR DEL USUARIO
        // ═══════════════════════════════════════════════════════════
        setupUserAvatar();

        Log.d(TAG, "✓ Batalla Cósmica scene setup complete con " + sceneObjects.size() + " objetos");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🌌 SETUP METHODS - Cada uno crea una parte de la escena
    // ═══════════════════════════════════════════════════════════════════════

    private void setupBackground() {
        try {
            StarryBackground starryBg = new StarryBackground(
                    context,
                    textureManager,
                    R.drawable.universo001
            );
            addSceneObject(starryBg);
            Log.d(TAG, "  ✓ Fondo estrellado agregado");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando fondo: " + e.getMessage());
        }
    }

    private void setupSun() {
        try {
            SolProcedural solProcedural = new SolProcedural(context, textureManager);
            solProcedural.setPosition(
                SceneConstants.Sun.POSITION_X,
                SceneConstants.Sun.POSITION_Y,
                SceneConstants.Sun.POSITION_Z
            );
            solProcedural.setScale(SceneConstants.Sun.SCALE);
            solProcedural.setCameraController(camera);
            addSceneObject(solProcedural);
            Log.d(TAG, "  ✓ ☀️ Sol procedural agregado");

            // Efecto de calor
            SunHeatEffect sunHeat = new SunHeatEffect(context);
            sunHeat.setSunPosition(
                SceneConstants.Sun.POSITION_X,
                SceneConstants.Sun.POSITION_Y,
                SceneConstants.Sun.POSITION_Z,
                SceneConstants.Sun.SCALE
            );
            sunHeat.setCameraController(camera);
            addSceneObject(sunHeat);
            Log.d(TAG, "  ✓ 🔥 Efecto de calor agregado");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando sol: " + e.getMessage());
        }
    }

    private void setupEarth() {
        try {
            tierra = new Planeta(
                    context, textureManager,
                    "shaders/tierra_vertex.glsl",
                    "shaders/tierra_fragment.glsl",
                    R.drawable.texturaplanetatierra,
                    SceneConstants.Earth.ORBIT_RADIUS_X,
                    SceneConstants.Earth.ORBIT_RADIUS_Z,
                    SceneConstants.Earth.ORBIT_SPEED,
                    SceneConstants.Earth.POSITION_Y,
                    SceneConstants.Earth.SCALE_VARIATION,
                    SceneConstants.Earth.SCALE,
                    SceneConstants.Earth.ROTATION_SPEED,
                    false, null, 1.0f,
                    null, 1.0f
            );

            if (tierra instanceof CameraAware) {
                ((CameraAware) tierra).setCameraController(camera);
            }

            tierra.setMaxHealth(SceneConstants.Earth.MAX_HP);
            tierra.setOnExplosionListener(this);

            // Cargar HP guardado
            tierra.setPlayerStats(playerStats);
            int savedPlanetHP = playerStats.getSavedPlanetHealth();
            tierra.setHealth(savedPlanetHP);

            tierra.setRealTimeRotation(false);

            addSceneObject(tierra);
            planetaTierra = tierra;

            Log.d(TAG, "  ✓ 🌍 Tierra agregada con HP: " + savedPlanetHP + "/" + SceneConstants.Earth.MAX_HP);
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando Tierra: " + e.getMessage());
        }
    }

    private void setupShields() {
        // EarthShield - Escudo invisible para impactos
        try {
            earthShield = new EarthShield(
                    context, textureManager,
                    SceneConstants.Earth.POSITION_X,
                    SceneConstants.Earth.POSITION_Y,
                    SceneConstants.Earth.POSITION_Z,
                    SceneConstants.Shield.EARTH_SHIELD_RADIUS
            );
            if (earthShield instanceof CameraAware) {
                ((CameraAware) earthShield).setCameraController(camera);
            }
            addSceneObject(earthShield);
            Log.d(TAG, "  ✓ 🛡️ EarthShield agregado");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando EarthShield: " + e.getMessage());
        }

        // ForceField - Campo de fuerza visible
        try {
            forceField = new ForceField(
                    context, textureManager,
                    SceneConstants.Earth.POSITION_X,
                    SceneConstants.Earth.POSITION_Y,
                    SceneConstants.Earth.POSITION_Z,
                    SceneConstants.Shield.FORCE_FIELD_RADIUS,
                    R.drawable.fondo_transparente,
                    new float[]{
                        SceneConstants.Shield.FORCE_FIELD_COLOR_R,
                        SceneConstants.Shield.FORCE_FIELD_COLOR_G,
                        SceneConstants.Shield.FORCE_FIELD_COLOR_B
                    },
                    SceneConstants.Shield.FORCE_FIELD_INTENSITY,
                    SceneConstants.Shield.FORCE_FIELD_PULSE_SPEED,
                    SceneConstants.Shield.FORCE_FIELD_PULSE_AMPLITUDE
            );
            forceField.setCameraController(camera);

            forceField.setPlayerStats(playerStats);
            int savedForceFieldHP = playerStats.getSavedForceFieldHealth();
            forceField.setHealth(savedForceFieldHP);

            addSceneObject(forceField);
            Log.d(TAG, "  ✓ 🛡️ ForceField agregado con HP: " + savedForceFieldHP + "/" + SceneConstants.Shield.FORCE_FIELD_MAX_HP);
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando ForceField: " + e.getMessage());
        }
    }

    private void setupOvni() {
        try {
            ovni = new Spaceship3D(
                    context,
                    textureManager,
                    SceneConstants.Ufo.START_POSITION_X,
                    SceneConstants.Ufo.START_POSITION_Y,
                    SceneConstants.Ufo.START_POSITION_Z,
                    SceneConstants.Ufo.SCALE
            );
            ovni.setCameraController(camera);

            ovni.setEarthPosition(
                SceneConstants.Earth.POSITION_X,
                SceneConstants.Earth.POSITION_Y,
                SceneConstants.Earth.POSITION_Z
            );
            ovni.setSunPosition(
                SceneConstants.Sun.POSITION_X,
                SceneConstants.Sun.POSITION_Y,
                SceneConstants.Sun.POSITION_Z
            );
            ovni.setOrbitParams(
                SceneConstants.Ufo.ORBIT_RADIUS,
                SceneConstants.Ufo.ORBIT_SPEED,
                SceneConstants.Ufo.ORBIT_PHASE
            );

            if (earthShield != null) {
                ovni.setEarthShield(earthShield);
            }

            addSceneObject(ovni);
            Log.d(TAG, "  ✓ 🛸 OVNI agregado con IA");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando OVNI: " + e.getMessage());
        }
    }

    private void setupDancingStars() {
        try {
            estrellasBailarinas.clear();

            for (float[] pos : SceneConstants.DancingStars.POSITIONS) {
                EstrellaBailarina estrella = new EstrellaBailarina(
                        context, textureManager,
                        pos[0], pos[1], pos[2],
                        SceneConstants.DancingStars.SCALE,
                        pos[3]
                );
                estrella.setCameraController(camera);
                addSceneObject(estrella);
                estrellasBailarinas.add(estrella);
            }

            Log.d(TAG, "  ✓ ✨ " + estrellasBailarinas.size() + " estrellas bailarinas agregadas");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando estrellas: " + e.getMessage());
        }
    }

    private void setupUI() {
        // Power Bar
        try {
            powerBar = new BatteryPowerBar(context);
            addSceneObject(powerBar);
            Log.d(TAG, "  ✓ 🔋 PowerBar agregada");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando PowerBar: " + e.getMessage());
        }

        // Greeting Text
        try {
            GreetingText greetingText = new GreetingText(context);
            addSceneObject(greetingText);
            Log.d(TAG, "  ✓ 👋 Greeting agregado");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando Greeting: " + e.getMessage());
        }

        // HP Bars (ocultas pero funcionales)
        try {
            hpBarTierra = new HPBar(
                    context, "🌍 TIERRA",
                    SceneConstants.UI.HP_BAR_EARTH_X,
                    SceneConstants.UI.HP_BAR_EARTH_Y,
                    SceneConstants.UI.HP_BAR_EARTH_WIDTH,
                    SceneConstants.UI.HP_BAR_EARTH_HEIGHT,
                    SceneConstants.Earth.MAX_HP,
                    SceneConstants.Colors.HP_EARTH_FULL,
                    SceneConstants.Colors.HP_EARTH_EMPTY
            );

            hpBarForceField = new HPBar(
                    context, "ESCUDO",
                    SceneConstants.UI.HP_BAR_SHIELD_X,
                    SceneConstants.UI.HP_BAR_SHIELD_Y,
                    SceneConstants.UI.HP_BAR_SHIELD_WIDTH,
                    SceneConstants.UI.HP_BAR_SHIELD_HEIGHT,
                    SceneConstants.Shield.FORCE_FIELD_MAX_HP,
                    SceneConstants.Colors.HP_SHIELD_FULL,
                    SceneConstants.Colors.HP_SHIELD_EMPTY
            );

            // No agregar a sceneObjects (ocultas)
            Log.d(TAG, "  ✓ HP Bars creadas (ocultas)");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando HP Bars: " + e.getMessage());
        }

        // Music Indicator
        try {
            musicIndicator = new MusicIndicator(
                    context,
                    SceneConstants.UI.MUSIC_INDICATOR_X,
                    SceneConstants.UI.MUSIC_INDICATOR_Y,
                    SceneConstants.UI.MUSIC_INDICATOR_WIDTH,
                    SceneConstants.UI.MUSIC_INDICATOR_HEIGHT
            );
            addSceneObject(musicIndicator);
            Log.d(TAG, "  ✓ 🎵 MusicIndicator agregado");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando MusicIndicator: " + e.getMessage());
        }

        // Planets Destroyed Counter
        try {
            planetsDestroyedCounter = new SimpleTextRenderer(
                    context,
                    SceneConstants.UI.PLANETS_COUNTER_X,
                    SceneConstants.UI.PLANETS_COUNTER_Y,
                    SceneConstants.UI.PLANETS_COUNTER_WIDTH,
                    SceneConstants.UI.PLANETS_COUNTER_HEIGHT
            );
            planetsDestroyedCounter.setColor(SceneConstants.Colors.PLANETS_COUNTER_COLOR);

            if (playerStats != null) {
                int currentPlanets = playerStats.getPlanetsDestroyed();
                planetsDestroyedCounter.setText("🪐" + currentPlanets);
            } else {
                planetsDestroyedCounter.setText("🪐0");
            }

            addSceneObject(planetsDestroyedCounter);
            Log.d(TAG, "  ✓ 🪐 Contador agregado");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando contador: " + e.getMessage());
        }

        // Leaderboard
        setupLeaderboard();

        // NOTA: FireButton y LikeButton son manejados por WallpaperDirector/SongSharingController
    }

    private void setupLeaderboard() {
        try {
            leaderboardManager = LeaderboardManager.getInstance(context);

            int[] colors = {
                    SceneConstants.Colors.MEDAL_GOLD,
                    SceneConstants.Colors.MEDAL_SILVER,
                    SceneConstants.Colors.MEDAL_BRONZE
            };

            for (int i = 0; i < 3; i++) {
                float y = SceneConstants.UI.LEADERBOARD_START_Y - (i * SceneConstants.UI.LEADERBOARD_SPACING);
                leaderboardTexts[i] = new SimpleTextRenderer(
                    context,
                    SceneConstants.UI.LEADERBOARD_X,
                    y,
                    SceneConstants.UI.LEADERBOARD_WIDTH,
                    SceneConstants.UI.LEADERBOARD_HEIGHT
                );
                leaderboardTexts[i].setColor(colors[i]);
                leaderboardTexts[i].setText("---");
                addSceneObject(leaderboardTexts[i]);
            }

            Log.d(TAG, "  ✓ 🏆 Leaderboard UI creado");

            // Actualizar inmediatamente
            updateLeaderboardUI();
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando leaderboard: " + e.getMessage());
        }
    }

    private void setupMeteorSystem() {
        try {
            meteorShower = new MeteorShower(context, textureManager);
            meteorShower.setCameraController(camera);

            if (powerBar != null) {
                meteorShower.setPowerBar(powerBar);
            }

            if (tierra != null && forceField != null && hpBarTierra != null && hpBarForceField != null) {
                meteorShower.setHPSystem(tierra, forceField, hpBarTierra, hpBarForceField);
            }

            // Registrar objetos colisionables
            for (SceneObject obj : sceneObjects) {
                if (obj instanceof Planeta || obj instanceof ForceField) {
                    meteorShower.registrarObjetoColisionable(obj);
                }
            }

            if (ovni != null) {
                meteorShower.setOvni(ovni);
            }

            addSceneObject(meteorShower);
            Log.d(TAG, "  ✓ ☄️ Sistema de meteoritos agregado");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando MeteorShower: " + e.getMessage());
        }
    }

    private void setupWeaponSystem() {
        try {
            playerWeapon = new PlayerWeapon(context, textureManager);
            playerWeapon.setCameraController(camera);

            if (meteorShower != null) {
                playerWeapon.setMeteorShower(meteorShower);
            }

            addSceneObject(playerWeapon);
            Log.d(TAG, "  ✓ 🎮 Sistema de armas agregado");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando PlayerWeapon: " + e.getMessage());
        }
    }

    private void setupUserAvatar() {
        try {
            final AvatarSphere avatarSphere = new AvatarSphere(context, textureManager, null);
            avatarSphere.setCameraController(camera);
            addSceneObject(avatarSphere);

            AvatarLoader.loadCurrentUserAvatar(context, new AvatarLoader.AvatarLoadListener() {
                @Override
                public void onAvatarLoaded(android.graphics.Bitmap bitmap) {
                    avatarSphere.updateAvatar(bitmap);
                    Log.d(TAG, "  ✓ ✨ Avatar cargado");
                }

                @Override
                public void onAvatarLoadFailed() {
                    Log.w(TAG, "  ⚠️ No se pudo cargar el avatar");
                }
            });

            Log.d(TAG, "  ✓ 👤 AvatarSphere agregado");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando AvatarSphere: " + e.getMessage());
        }
    }

    @Override
    protected void releaseSceneResources() {
        Log.d(TAG, "🧹 Liberando recursos de Batalla Cósmica...");

        // Limpiar referencias
        tierra = null;
        planetaTierra = null;
        forceField = null;
        earthShield = null;
        ovni = null;
        meteorShower = null;
        playerWeapon = null;
        powerBar = null;
        musicIndicator = null;

        estrellasBailarinas.clear();

        Log.d(TAG, "✓ Recursos liberados");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 💥 EXPLOSION LISTENER
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void onExplosion(float x, float y, float z, float intensity) {
        Log.d(TAG, "💥 ¡EXPLOSIÓN en (" + x + ", " + y + ", " + z + ") intensidad: " + intensity);

        // Actualizar contador
        if (playerStats != null) {
            playerStats.onPlanetDestroyed();
            if (planetsDestroyedCounter != null) {
                planetsDestroyedCounter.setText("🪐" + playerStats.getPlanetsDestroyed());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🎵 MÉTODOS PARA ACTUALIZAR MÚSICA
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Actualiza los niveles de música en el indicador
     */
    public void updateMusicLevels(float bass, float mid, float treble) {
        if (musicIndicator != null) {
            musicIndicator.updateMusicLevels(bass, mid, treble);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🎮 GETTERS PARA INTERACCIÓN
    // ═══════════════════════════════════════════════════════════════════════

    public Planeta getTierra() {
        return tierra;
    }

    public ForceField getForceField() {
        return forceField;
    }

    public Spaceship3D getOvni() {
        return ovni;
    }

    public MeteorShower getMeteorShower() {
        return meteorShower;
    }

    public MusicIndicator getMusicIndicator() {
        return musicIndicator;
    }

    /**
     * 🏆 Actualiza los textos del leaderboard
     */
    public void updateLeaderboardUI() {
        if (leaderboardManager == null) return;

        leaderboardManager.getTop3(new LeaderboardManager.Top3Callback() {
            @Override
            public void onSuccess(List<LeaderboardManager.LeaderboardEntry> top3) {
                if (top3 == null || top3.isEmpty()) return;

                for (int i = 0; i < Math.min(top3.size(), 3); i++) {
                    LeaderboardManager.LeaderboardEntry entry = top3.get(i);
                    if (leaderboardTexts[i] != null) {
                        String medal = (i == 0) ? "🥇" : (i == 1) ? "🥈" : "🥉";
                        String name = entry.displayName;
                        if (name.length() > 10) {
                            name = name.substring(0, 9) + "..";
                        }
                        String text = medal + " " + name + " " + entry.planetsDestroyed;
                        leaderboardTexts[i].setText(text);
                    }
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error actualizando leaderboard: " + error);
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🔄 UPDATE - Sobrescribe para actualizar leaderboard
    // ═══════════════════════════════════════════════════════════════════════

    private long lastLeaderboardUpdate = 0;

    @Override
    public void update(float deltaTime) {
        // Llamar al update base (actualiza todos los sceneObjects)
        super.update(deltaTime);

        // Actualizar leaderboard periódicamente
        long now = System.currentTimeMillis();
        if (now - lastLeaderboardUpdate > SceneConstants.Timing.LEADERBOARD_UPDATE_INTERVAL) {
            lastLeaderboardUpdate = now;
            updateLeaderboardUI();
        }
    }
}
