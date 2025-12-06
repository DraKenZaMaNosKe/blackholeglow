package com.secret.blackholeglow.scenes;

import android.content.Context;
import android.util.Log;

import com.secret.blackholeglow.AvatarLoader;
import com.secret.blackholeglow.AvatarSphere;
import com.secret.blackholeglow.BackgroundStars;
import com.secret.blackholeglow.BatteryPowerBar;
import com.secret.blackholeglow.BirthdayMarquee;
import com.secret.blackholeglow.CameraAware;
import com.secret.blackholeglow.CameraController;
// EarthShield REMOVIDO
import com.secret.blackholeglow.EstrellaBailarina;
// ForceField REMOVIDO
import com.secret.blackholeglow.GreetingText;
import com.secret.blackholeglow.HPBar;
import com.secret.blackholeglow.LeaderboardManager;
import com.secret.blackholeglow.MagicLeaderboard;
import com.secret.blackholeglow.MeteorShower;
import com.secret.blackholeglow.MusicStars;
import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.Planeta;
import com.secret.blackholeglow.PlayerStats;
import com.secret.blackholeglow.PlayerWeapon;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.SceneObject;
import com.secret.blackholeglow.SimpleTextRenderer;
import com.secret.blackholeglow.SolMeshy;
import com.secret.blackholeglow.SolProcedural;
import com.secret.blackholeglow.SpaceDust;
import com.secret.blackholeglow.SpaceStation;
import com.secret.blackholeglow.TierraMeshy;
import com.secret.blackholeglow.DefenderShip;
import com.secret.blackholeglow.UfoScout;
import com.secret.blackholeglow.StarryBackground;
// SunHeatEffect REMOVIDO
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
    private Planeta planetaTierra;  // Referencia para colisiones (legacy)

    // 🌍☀️ NUEVOS MODELOS DE MESHY AI
    private TierraMeshy tierraMeshy;
    private SolMeshy solMeshy;

    private DefenderShip defenderShip;  // 🚀 Nave defensora
    private SpaceStation spaceStation;  // 🛰️ Estación espacial
    private UfoScout ufoScout;          // 🛸 UFO Scout (Meshy model)
    private MeteorShower meteorShower;
    private PlayerWeapon playerWeapon;
    private BatteryPowerBar powerBar;

    // ═══════════════════════════════════════════════════════════════
    // 📊 UI Y ESTADÍSTICAS
    // ═══════════════════════════════════════════════════════════════
    private HPBar hpBarTierra;
    // hpBarForceField REMOVIDO
    private EqualizerBarsDJ equalizerDJ;             // 🎵 Ecualizador estilo DJ
    private SimpleTextRenderer planetsDestroyedCounter;
    private MagicLeaderboard magicLeaderboard;  // ✨ Leaderboard mágico con partículas
    private BirthdayMarquee birthdayMarquee;    // 🎂 Marquesina de cumpleaños

    // ═══════════════════════════════════════════════════════════════
    // ✨ EFECTOS VISUALES
    // ═══════════════════════════════════════════════════════════════
    private List<EstrellaBailarina> estrellasBailarinas = new ArrayList<>();
    private BackgroundStars backgroundStars;  // ✨ Estrellas parpadeantes de fondo (con parallax)
    private SpaceDust spaceDust;              // 🚀 Polvo espacial (ilusión de viaje)
    private MusicStars musicStars;  // 🌀 Estrellas espirales musicales

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
        // 4️⃣ ESCUDO Y CAMPO DE FUERZA - REMOVIDOS
        // ═══════════════════════════════════════════════════════════
        // setupShields(); // DESHABILITADO - Tierra y Sol serán modelos de Meshy

        // ═══════════════════════════════════════════════════════════
        // 5️⃣ OVNI - Solo usamos UfoScout (modelo Meshy)
        // ═══════════════════════════════════════════════════════════

        // ═══════════════════════════════════════════════════════════
        // 5.5️⃣ NAVE DEFENSORA
        // ═══════════════════════════════════════════════════════════
        setupDefenderShip();  // NAVE1 - el objetivo del UfoScout

        // ═══════════════════════════════════════════════════════════
        // 5.6️⃣ 🛰️ ESTACIÓN ESPACIAL
        // ═══════════════════════════════════════════════════════════
        setupSpaceStation();

        // ═══════════════════════════════════════════════════════════
        // 5.7️⃣ 🛸 UFO SCOUT (NUEVO MODELO MESHY)
        // ═══════════════════════════════════════════════════════════
        setupUfoScout();

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

            // ✨ Estrellas parpadeantes de fondo (efecto de profundidad + PARALLAX)
            backgroundStars = new BackgroundStars(context);
            addSceneObject(backgroundStars);
            Log.d(TAG, "  ✓ ✨ Estrellas de fondo con parallax agregadas");

            // 🚀 POLVO ESPACIAL - Partículas que pasan creando ilusión de viaje
            spaceDust = new SpaceDust(context);
            addSceneObject(spaceDust);
            Log.d(TAG, "  ✓ 🚀 Polvo espacial agregado (ilusión de viaje)");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando fondo: " + e.getMessage());
        }
    }

    private void setupSun() {
        try {
            // ☀️ SOL MESHY - Modelo 3D realista de Meshy AI
            solMeshy = new SolMeshy(context, textureManager);
            solMeshy.setPosition(
                SceneConstants.Sun.POSITION_X,
                SceneConstants.Sun.POSITION_Y,
                SceneConstants.Sun.POSITION_Z
            );
            solMeshy.setScale(SceneConstants.Sun.SCALE);
            solMeshy.setSpinSpeed(3.0f);  // Rotación lenta del sol
            solMeshy.setCameraController(camera);
            addSceneObject(solMeshy);
            Log.d(TAG, "  ✓ ☀️ Sol Meshy agregado (modelo 3D realista)");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando sol Meshy: " + e.getMessage());
        }
    }

    private void setupEarth() {
        try {
            // 🌍 TIERRA MESHY - Modelo 3D realista de Meshy AI
            tierraMeshy = new TierraMeshy(context, textureManager);
            // Posición inicial (será modificada por órbita)
            tierraMeshy.setPosition(
                SceneConstants.Earth.POSITION_X,
                SceneConstants.Earth.POSITION_Y,
                SceneConstants.Earth.POSITION_Z
            );
            tierraMeshy.setScale(SceneConstants.Earth.SCALE);
            tierraMeshy.setSpinSpeed(SceneConstants.Earth.ROTATION_SPEED);
            tierraMeshy.setMaxHP(SceneConstants.Earth.MAX_HP);
            tierraMeshy.setCameraController(camera);

            // 🌍 CONFIGURAR ÓRBITA ALREDEDOR DEL SOL
            tierraMeshy.setOrbit(
                SceneConstants.Sun.POSITION_X,   // Centro X = posición del Sol
                SceneConstants.Sun.POSITION_Y,   // Centro Y
                SceneConstants.Sun.POSITION_Z,   // Centro Z
                SceneConstants.Earth.ORBIT_RADIUS_X,  // Radio horizontal
                SceneConstants.Earth.ORBIT_RADIUS_Z,  // Radio en profundidad
                SceneConstants.Earth.ORBIT_SPEED      // Velocidad de órbita
            );

            // Callback de explosión
            tierraMeshy.setExplosionCallback((x, y, z) -> {
                onExplosion(x, y, z, 1.0f);  // Intensidad máxima
            });

            addSceneObject(tierraMeshy);

            // También mantener referencia legacy para sistemas que usan Planeta
            // (como MeteorShower que verifica sol.isDead())
            tierra = new Planeta(
                    context, textureManager,
                    "shaders/tierra_vertex.glsl",
                    "shaders/tierra_fragment.glsl",
                    R.drawable.texturaplanetatierra,
                    0, 0, 0,  // Sin órbita
                    SceneConstants.Earth.POSITION_Y,
                    0, 0.001f,  // Escala mínima (invisible)
                    0, false, null, 0.0f,
                    null, 1.0f
            );
            tierra.setMaxHealth(SceneConstants.Earth.MAX_HP);
            tierra.setOnExplosionListener(this);
            tierra.setPlayerStats(playerStats);
            int savedPlanetHP = playerStats.getSavedPlanetHealth();
            tierra.setHealth(savedPlanetHP);
            planetaTierra = tierra;
            // NO agregar a la escena (es solo para referencia de sistemas legacy)

            Log.d(TAG, "  ✓ 🌍 Tierra Meshy agregada (modelo 3D realista)");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando Tierra Meshy: " + e.getMessage());
        }
    }

    // setupShields() REMOVIDO COMPLETAMENTE
    // ForceField y EarthShield serán reemplazados por modelos de Meshy

    // setupOvni() REMOVIDO - Solo usamos UfoScout (modelo Meshy)

    private void setupDefenderShip() {
        try {
            defenderShip = new DefenderShip(
                    context,
                    textureManager,
                    SceneConstants.Earth.POSITION_X + 2.0f,  // Empezar a un lado de la Tierra
                    SceneConstants.Earth.POSITION_Y,
                    SceneConstants.Earth.POSITION_Z,
                    0.25f  // Escala más grande para que sea visible
            );
            defenderShip.setCameraController(camera);

            // Configurar posición de la Tierra para órbita
            defenderShip.setEarthPosition(
                SceneConstants.Earth.POSITION_X,
                SceneConstants.Earth.POSITION_Y,
                SceneConstants.Earth.POSITION_Z
            );

            // Configurar parámetros de órbita
            defenderShip.setOrbitParams(1.8f, 0.6f);  // Radio 1.8, velocidad 0.6

            addSceneObject(defenderShip);
            Log.d(TAG, "  ✓ 🚀 Nave defensora agregada (batalla bidireccional configurada)");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando DefenderShip: " + e.getMessage(), e);
        }
    }

    private void setupSpaceStation() {
        try {
            // ═══════════════════════════════════════════════════════════
            // 🛰️ ESTACIÓN ESPACIAL FUTURISTA - CONFIGURACIÓN
            // ═══════════════════════════════════════════════════════════
            // POSICIÓN:
            float stationX = -1.2f;   // Horizontal: -izquierda, +derecha
            float stationY = 2.5f;    // Vertical: -abajo, +arriba
            float stationZ = 0.3f;    // Profundidad: -lejos, +cerca cámara

            // TAMAÑO:
            float stationScale = 0.75f;  // Escala general (0.1=pequeño, 0.5=grande)

            // ═══════════════════════════════════════════════════════════

            spaceStation = new SpaceStation(
                    context,
                    textureManager,
                    stationX,
                    stationY,
                    stationZ,
                    stationScale
            );
            spaceStation.setCameraController(camera);

            // Posición fija (sin órbita) - solo rota sobre sí misma
            spaceStation.setFixedPosition(stationX, stationY, stationZ);

            addSceneObject(spaceStation);

            // ═══════════════════════════════════════════════════════════
            // 🛰️ CONECTAR ESTACIÓN A OTROS OBJETOS PARA COLISIONES
            // ═══════════════════════════════════════════════════════════
            // DefenderShip debe esquivar la estación
            if (defenderShip != null) {
                defenderShip.setSpaceStation(spaceStation);
                Log.d(TAG, "    → DefenderShip conectado para esquivar estación");
            }

            Log.d(TAG, "  ✓ 🛰️ Estación espacial: pos(" + stationX + "," + stationY + "," + stationZ + ") scale=" + stationScale);
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando SpaceStation: " + e.getMessage(), e);
        }
    }

    private void setupUfoScout() {
        try {
            // ═══════════════════════════════════════════════════════════
            // 🛸 OVNI1 - PLATILLO VOLADOR CLÁSICO DE MESHY AI
            // ═══════════════════════════════════════════════════════════
            // POSICIÓN INICIAL:
            float scoutX = 2.5f;    // Lado derecho de la escena
            float scoutY = 1.5f;    // A media altura
            float scoutZ = -1.0f;   // Un poco hacia atrás

            // TAMAÑO: Reducido para que se vea proporcional
            float scoutScale = 0.18f;  // Más pequeño

            // ═══════════════════════════════════════════════════════════

            ufoScout = new UfoScout(
                    context,
                    textureManager,
                    scoutX,
                    scoutY,
                    scoutZ,
                    scoutScale
            );
            ufoScout.setCameraController(camera);

            // Configurar referencia de la Tierra para colisiones de láser
            if (tierraMeshy != null) {
                ufoScout.setEarthReference(
                    tierraMeshy.getX(),
                    tierraMeshy.getY(),
                    tierraMeshy.getZ(),
                    tierraMeshy.getScale() * 0.5f  // Radio aproximado
                );
            }

            // Configurar DefenderShip como objetivo
            if (defenderShip != null) {
                ufoScout.setTarget(defenderShip);
                Log.d(TAG, "    → OVNI1 tiene a NAVE1 como objetivo");

                // 🔗 Conectar NAVE1 para que dispare a OVNI1
                defenderShip.setTargetUfoScout(ufoScout);
                Log.d(TAG, "    → NAVE1 tiene a OVNI1 como objetivo adicional");
            }

            addSceneObject(ufoScout);

            Log.d(TAG, "  ✓ 🛸 OVNI1 agregado: pos(" + scoutX + "," + scoutY + "," + scoutZ + ") scale=" + scoutScale);
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando UfoScout: " + e.getMessage(), e);
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

        // 🌀 MusicStars - Estrellas con efecto espiral galáctico
        try {
            musicStars = new MusicStars(context);
            addSceneObject(musicStars);
            Log.d(TAG, "  ✓ 🌀 MusicStars agregadas (efecto espiral galáctico)");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando MusicStars: " + e.getMessage());
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

        // HP Bar de Tierra (ForceField HP Bar REMOVIDA)
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

            // hpBarForceField REMOVIDA

            // No agregar a sceneObjects (oculta)
            Log.d(TAG, "  ✓ HP Bar Tierra creada (oculta)");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando HP Bar: " + e.getMessage());
        }

        // 🎵 Ecualizador DJ (barras en la parte inferior)
        try {
            equalizerDJ = new EqualizerBarsDJ();
            equalizerDJ.initialize();
            // No agregar a sceneObjects - se dibuja manualmente después de todo
            Log.d(TAG, "  ✓ 🎵 EqualizerBarsDJ agregado (estilo DJ en bottom)");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando EqualizerBarsDJ: " + e.getMessage());
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

        // Leaderboard (OCULTO - deshabilitado temporalmente)
        // setupLeaderboard();

        // NOTA: FireButton y LikeButton son manejados por WallpaperDirector/SongSharingController
    }

    private void setupLeaderboard() {
        try {
            leaderboardManager = LeaderboardManager.getInstance(context);

            // ✨ Crear MagicLeaderboard con efectos de polvo estelar
            magicLeaderboard = new MagicLeaderboard(context);
            addSceneObject(magicLeaderboard);

            Log.d(TAG, "  ✓ ✨ MagicLeaderboard creado con efectos de polvo estelar");

            // 🎂 Crear BirthdayMarquee para celebrar cumpleaños (DESHABILITADO temporalmente)
            // birthdayMarquee = new BirthdayMarquee(context);
            // addSceneObject(birthdayMarquee);
            // Log.d(TAG, "  ✓ 🎂 BirthdayMarquee creado");

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

            // Sistema HP simplificado (sin ForceField)
            if (tierra != null && hpBarTierra != null) {
                meteorShower.setHPSystem(tierra, null, hpBarTierra, null);
            }

            // Registrar solo planetas como colisionables
            for (SceneObject obj : sceneObjects) {
                if (obj instanceof Planeta) {
                    meteorShower.registrarObjetoColisionable(obj);
                }
            }


            // 🌍 Conectar Tierra para posición dinámica (órbita)
            if (tierraMeshy != null) {
                meteorShower.setTierra(tierraMeshy);
            }

            // 🛰️ Conectar Estación Espacial para colisiones
            if (spaceStation != null) {
                meteorShower.setSpaceStation(spaceStation);
            }

            // ☀️ Conectar Sol Meshy para colisiones (asteroides explotan al impactar)
            if (solMeshy != null) {
                meteorShower.setSolMeshy(solMeshy);
            }

            addSceneObject(meteorShower);
            Log.d(TAG, "  ✓ ☄️ Sistema de meteoritos agregado (con colisiones en estación y sol)");
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
        // forceField y earthShield REMOVIDOS
        defenderShip = null;
        spaceStation = null;
        ufoScout = null;
        meteorShower = null;
        playerWeapon = null;
        powerBar = null;
        equalizerDJ = null;
        backgroundStars = null;
        musicStars = null;

        // Liberar MagicLeaderboard
        if (magicLeaderboard != null) {
            magicLeaderboard.release();
            magicLeaderboard = null;
        }

        // Liberar BirthdayMarquee
        if (birthdayMarquee != null) {
            birthdayMarquee.release();
            birthdayMarquee = null;
        }

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
        // 🎵 EqualizerBarsDJ
        if (equalizerDJ != null) {
            equalizerDJ.updateMusicLevels(bass, mid, treble);
        }
        // 🌀 MusicStars - espirales galácticas
        if (musicStars != null) {
            musicStars.updateMusicLevels(bass, mid, treble);
        }
    }

    /**
     * 🎵 NUEVO: Actualiza usando las 32 bandas de frecuencia para mejor visualización
     */
    public void updateMusicBands(float[] bands) {
        if (bands == null) return;

        // 🎵 EqualizerBarsDJ usa las 32 bandas directamente
        if (equalizerDJ != null) {
            equalizerDJ.updateFromBands(bands);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🎮 GETTERS PARA INTERACCIÓN
    // ═══════════════════════════════════════════════════════════════════════

    public Planeta getTierra() {
        return tierra;
    }

    // getForceField() REMOVIDO
    // getOvni() REMOVIDO - Solo usamos UfoScout

    public DefenderShip getDefenderShip() {
        return defenderShip;
    }

    public MeteorShower getMeteorShower() {
        return meteorShower;
    }

    public EqualizerBarsDJ getEqualizerDJ() {
        return equalizerDJ;
    }

    /**
     * ✨ Actualiza el MagicLeaderboard con datos de Firebase
     */
    public void updateLeaderboardUI() {
        if (leaderboardManager == null || magicLeaderboard == null) return;

        leaderboardManager.getTop3(new LeaderboardManager.Top3Callback() {
            @Override
            public void onSuccess(List<LeaderboardManager.LeaderboardEntry> top3) {
                if (top3 == null || top3.isEmpty()) return;

                // Actualizar el MagicLeaderboard con los datos
                magicLeaderboard.updateEntries(top3);
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

        // 🎵 Actualizar ecualizador DJ (no está en sceneObjects)
        if (equalizerDJ != null) {
            equalizerDJ.update(deltaTime);
        }

        // Actualizar leaderboard periódicamente
        long now = System.currentTimeMillis();
        if (now - lastLeaderboardUpdate > SceneConstants.Timing.LEADERBOARD_UPDATE_INTERVAL) {
            lastLeaderboardUpdate = now;
            updateLeaderboardUI();
        }
    }

    @Override
    public void draw() {
        // Dibujar todos los objetos de la escena primero
        super.draw();

        // 🎵 Dibujar ecualizador DJ encima de todo (overlay 2D)
        if (equalizerDJ != null) {
            equalizerDJ.draw();
        }
    }

    /**
     * Sobrescribe setScreenSize para pasar dimensiones al ecualizador
     */
    @Override
    public void setScreenSize(int width, int height) {
        super.setScreenSize(width, height);

        // 🎵 Pasar dimensiones al ecualizador DJ
        if (equalizerDJ != null) {
            equalizerDJ.setScreenSize(width, height);
        }
    }
}
