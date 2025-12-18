package com.secret.blackholeglow.scenes;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import com.secret.blackholeglow.AvatarLoader;
import com.secret.blackholeglow.AvatarSphere;
import com.secret.blackholeglow.BackgroundStars;
import com.secret.blackholeglow.BatteryPowerBar;
import com.secret.blackholeglow.BattleHUD;
import com.secret.blackholeglow.CameraAware;
import com.secret.blackholeglow.CameraController;
// EarthShield REMOVIDO
// EstrellaBailarina REMOVIDO
// ForceField REMOVIDO
import com.secret.blackholeglow.GreetingText;
import com.secret.blackholeglow.HolographicTitle;
import com.secret.blackholeglow.HPBar;
import com.secret.blackholeglow.Laser;
// LeaderboardManager REMOVIDO
// MagicLeaderboard REMOVIDO
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
import com.secret.blackholeglow.SaturnoMeshy;
// SolProcedural REMOVIDO
import com.secret.blackholeglow.SpaceDust;
import com.secret.blackholeglow.SpaceStation;
import com.secret.blackholeglow.SpaceComets;
import com.secret.blackholeglow.ParallaxStars;
import com.secret.blackholeglow.TierraMeshy;
import com.secret.blackholeglow.DefenderShip;
import com.secret.blackholeglow.UfoScout;
import com.secret.blackholeglow.UfoAttacker;
import com.secret.blackholeglow.HumanInterceptor;
import com.secret.blackholeglow.StarryBackground;
import com.secret.blackholeglow.TargetingSystem;
import com.secret.blackholeglow.TargetReticle;
import com.secret.blackholeglow.PlasmaExplosion;
import com.secret.blackholeglow.PlasmaBeamWeapon;
import com.secret.blackholeglow.PlayerIndicator;
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

    // 🌍☀️🪐 MODELOS DE MESHY AI
    private TierraMeshy tierraMeshy;
    private SolMeshy solMeshy;
    private SaturnoMeshy saturnoMeshy;  // 🪐 Saturno con anillos

    private DefenderShip defenderShip;      // 🚀 Nave defensora (Team Human)
    private HumanInterceptor humanInterceptor;  // ✈️ Interceptor humano (Team Human)
    private PlayerIndicator playerIndicator;    // 🎮 Indicador "P1" sobre el jugador
    private SpaceStation spaceStation;      // 🛰️ Estación espacial
    private UfoScout ufoScout;              // 🛸 UFO Scout (Team Alien)
    private UfoAttacker ufoAttacker;        // 👾 UFO Attacker (Team Alien)
    private MeteorShower meteorShower;
    private PlayerWeapon playerWeapon;
    private BatteryPowerBar powerBar;

    // ═══════════════════════════════════════════════════════════════
    // 📊 UI Y ESTADÍSTICAS
    // ═══════════════════════════════════════════════════════════════
    private HPBar hpBarTierra;
    // hpBarForceField REMOVIDO
    private EqualizerBarsDJ equalizerDJ;             // 🎵 Ecualizador estilo DJ
    private BattleHUD battleHUD;                     // 🎮 HUD estilo Street Fighter
    private SimpleTextRenderer planetsDestroyedCounter;
    // MagicLeaderboard REMOVIDO
    // BirthdayMarquee REMOVIDO
    private HolographicTitle holographicTitle;  // 🔮 Título holográfico "HUMANS vs ALIENS"

    // ═══════════════════════════════════════════════════════════════
    // ✨ EFECTOS VISUALES
    // ═══════════════════════════════════════════════════════════════
    // EstrellaBailarinas REMOVIDO
    private BackgroundStars backgroundStars;  // ✨ Estrellas parpadeantes de fondo (con parallax)
    private SpaceDust spaceDust;              // 🚀 Polvo espacial (ilusión de viaje)
    private MusicStars musicStars;            // 🌀 Estrellas espirales musicales
    private SpaceComets spaceComets;          // ☄️ Cometas con estela brillante
    private ParallaxStars parallaxStars;      // ✨ Estrellas multi-capa con parallax profundo
    // LeaderboardManager REMOVIDO

    // ═══════════════════════════════════════════════════════════════
    // 📊 ESTADÍSTICAS DEL JUGADOR
    // ═══════════════════════════════════════════════════════════════
    private PlayerStats playerStats;

    // ═══════════════════════════════════════════════════════════════
    // 🎯 SISTEMA DE TARGETING Y DISPARO ESPECIAL
    // ═══════════════════════════════════════════════════════════════
    private TargetingSystem targetingSystem;      // Sistema de lock-on
    private TargetReticle targetReticle;          // Mira visual
    private PlasmaExplosion plasmaExplosion;      // Efecto de explosión (legacy)
    private PlasmaBeamWeapon plasmaBeamWeapon;    // ⚡ Arma de plasma con carga + viaje + impacto

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
        // 3.5️⃣ 🪐 SATURNO - Órbita opuesta a la Tierra
        // ═══════════════════════════════════════════════════════════
        setupSaturn();

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
        // 5.8️⃣ 👾 UFO ATTACKER (NAVE DE ATAQUE PRINCIPAL)
        // ═══════════════════════════════════════════════════════════
        setupUfoAttacker();

        // ═══════════════════════════════════════════════════════════
        // 5.9️⃣ ✈️ HUMAN INTERCEPTOR (CAZA INTERCEPTOR)
        // ═══════════════════════════════════════════════════════════
        setupHumanInterceptor();

        // ═══════════════════════════════════════════════════════════
        // 5.9.1️⃣ 🎮 PLAYER INDICATOR (P1 sobre el jugador)
        // ═══════════════════════════════════════════════════════════
        setupPlayerIndicator();

        // ═══════════════════════════════════════════════════════════
        // 5.10️⃣ 🎯 CONECTAR OBJETIVOS 2v2
        // ═══════════════════════════════════════════════════════════
        connectBattleTargets();

        // ═══════════════════════════════════════════════════════════
        // 6️⃣ ESTRELLAS MUSICALES (MusicStars)
        // ═══════════════════════════════════════════════════════════
        setupMusicStars();

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

        // ═══════════════════════════════════════════════════════════
        // 1️⃣1️⃣ TÍTULO HOLOGRÁFICO "HUMANS vs ALIENS" - REMOVIDO (ya tenemos BattleHUD)
        // ═══════════════════════════════════════════════════════════
        // setupHolographicTitle();

        // ═══════════════════════════════════════════════════════════
        // 1️⃣2️⃣ 🎯 SISTEMA DE TARGETING ASISTIDO
        // ═══════════════════════════════════════════════════════════
        setupTargetingSystem();

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

            // ✨ PARALLAX STARS - Estrellas multi-capa con efecto de profundidad
            parallaxStars = new ParallaxStars();
            addSceneObject(parallaxStars);
            Log.d(TAG, "  ✓ ✨ ParallaxStars agregadas (efecto profundidad 3 capas)");

            // ☄️ COMETAS CON ESTELA - Efecto de viaje espacial
            spaceComets = new SpaceComets();
            addSceneObject(spaceComets);
            Log.d(TAG, "  ✓ ☄️ Cometas con estela agregados (efecto viaje)");
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

    private void setupSaturn() {
        try {
            // 🪐 SATURNO MESHY - Planeta con anillos (órbita cinematográfica)
            saturnoMeshy = new SaturnoMeshy(context, textureManager);
            saturnoMeshy.setCameraController(camera);

            // ═══════════════════════════════════════════════════════════════
            // 🎬 CONFIGURACIÓN CINEMATOGRÁFICA DE SATURNO
            // ═══════════════════════════════════════════════════════════════
            // • Órbita en plano SUPERIOR a la Tierra (Y = 2.0)
            // • Radio GRANDE para alejarlo del Sol (3.0, 2.2)
            // • Escala PEQUEÑA para verse lejano y elegante
            // • Velocidad LENTA para movimiento majestuoso
            // • Nunca choca con la Tierra (diferentes planos orbitales)
            // ═══════════════════════════════════════════════════════════════

            saturnoMeshy.setOrbitCenter(
                SceneConstants.Sun.POSITION_X,   // -0.9 (mismo X que el Sol)
                2.0f,                            // ALTO - arriba de la Tierra
                SceneConstants.Sun.POSITION_Z - 1.0f  // -6.0 (ligeramente atrás)
            );

            saturnoMeshy.setOrbitRadius(3.0f, 2.2f);  // Órbita GRANDE
            saturnoMeshy.setOrbitSpeed(0.04f);        // Velocidad LENTA y majestuosa
            saturnoMeshy.setScale(0.18f);             // Escala PEQUEÑA (se ve lejano)
            saturnoMeshy.setSpinSpeed(5.0f);          // Rotación suave
            saturnoMeshy.setTiltAngle(26.7f);         // Inclinación real de Saturno

            addSceneObject(saturnoMeshy);

            Log.d(TAG, "  ✓ 🪐 Saturno Meshy - Órbita cinematográfica (plano superior)");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando Saturno Meshy: " + e.getMessage(), e);
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

            // NOTA: Los objetivos se conectan en connectBattleTargets()

            addSceneObject(ufoScout);

            Log.d(TAG, "  ✓ 🛸 OVNI1 agregado: pos(" + scoutX + "," + scoutY + "," + scoutZ + ") scale=" + scoutScale);
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando UfoScout: " + e.getMessage(), e);
        }
    }

    private void setupUfoAttacker() {
        try {
            // ═══════════════════════════════════════════════════════════
            // 👾 OVNI2 - NAVE DE ATAQUE ALIENÍGENA PRINCIPAL
            // ═══════════════════════════════════════════════════════════
            // POSICIÓN INICIAL: (opuesto al Scout)
            float attackerX = -2.0f;   // Lado izquierdo
            float attackerY = 2.5f;    // Más arriba
            float attackerZ = -1.5f;   // Más atrás

            // TAMAÑO: Más grande que el Scout
            float attackerScale = 0.25f;

            // ═══════════════════════════════════════════════════════════

            ufoAttacker = new UfoAttacker(
                    context,
                    textureManager,
                    attackerX,
                    attackerY,
                    attackerZ,
                    attackerScale
            );
            ufoAttacker.setCameraController(camera);

            // NOTA: Los objetivos se conectan en connectBattleTargets()

            addSceneObject(ufoAttacker);

            Log.d(TAG, "  ✓ 👾 OVNI2 (Attacker) agregado: pos(" + attackerX + "," + attackerY + "," + attackerZ + ") scale=" + attackerScale);
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando UfoAttacker: " + e.getMessage(), e);
        }
    }

    private void setupHumanInterceptor() {
        try {
            // ═══════════════════════════════════════════════════════════
            // ✈️ INTERCEPTOR HUMANO - Caza rápido y ágil
            // ═══════════════════════════════════════════════════════════
            // POSICIÓN INICIAL: Lado opuesto a donde aparecen los aliens
            float interceptorX = 1.5f;    // Lado derecho
            float interceptorY = 1.0f;    // Bajo
            float interceptorZ = 1.5f;    // Cerca de la cámara

            // TAMAÑO: Similar al DefenderShip
            float interceptorScale = 0.22f;

            // ═══════════════════════════════════════════════════════════

            humanInterceptor = new HumanInterceptor(
                    context,
                    textureManager,
                    interceptorX,
                    interceptorY,
                    interceptorZ,
                    interceptorScale
            );
            humanInterceptor.setCameraController(camera);

            addSceneObject(humanInterceptor);

            Log.d(TAG, "  ✓ ✈️ Human Interceptor agregado: pos(" + interceptorX + "," + interceptorY + "," + interceptorZ + ") scale=" + interceptorScale);
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando HumanInterceptor: " + e.getMessage(), e);
        }
    }

    /**
     * 🎮 PLAYER INDICATOR - Indicador "P1" sobre la nave del jugador
     * Muestra al usuario cuál es su nave
     */
    private void setupPlayerIndicator() {
        try {
            playerIndicator = new PlayerIndicator();
            playerIndicator.setCameraController(camera);
            playerIndicator.setPlayer(humanInterceptor);

            addSceneObject(playerIndicator);

            Log.d(TAG, "  ✓ 🎮 Player Indicator (P1) agregado sobre HumanInterceptor");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando PlayerIndicator: " + e.getMessage(), e);
        }
    }

    /**
     * 🎯 CONECTAR OBJETIVOS PARA BATALLA 2v2
     *
     * Team Human: DefenderShip + HumanInterceptor
     * Team Alien: UfoScout + UfoAttacker
     *
     * Cada nave tiene un objetivo primario y uno secundario.
     * Cuando el primario es destruido, ataca al secundario.
     */
    private void connectBattleTargets() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Log.d(TAG, "🎯 CONECTANDO OBJETIVOS 2v2");
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // ═══════════════════════════════════════════════════════════
        // 👾 TEAM ALIEN → Ataca a Team Human
        // ═══════════════════════════════════════════════════════════

        // UfoScout: Primario = DefenderShip, Secundario = HumanInterceptor
        if (ufoScout != null) {
            if (defenderShip != null) {
                ufoScout.setPrimaryTarget(defenderShip);
                Log.d(TAG, "  🛸 UfoScout → primario: DefenderShip");
            }
            if (humanInterceptor != null) {
                ufoScout.setSecondaryTarget(humanInterceptor);
                Log.d(TAG, "  🛸 UfoScout → secundario: HumanInterceptor");
            }
        }

        // UfoAttacker: Primario = DefenderShip, Secundario = HumanInterceptor
        if (ufoAttacker != null) {
            if (defenderShip != null) {
                ufoAttacker.setPrimaryTarget(defenderShip);
                Log.d(TAG, "  👾 UfoAttacker → primario: DefenderShip");
            }
            if (humanInterceptor != null) {
                ufoAttacker.setSecondaryTarget(humanInterceptor);
                Log.d(TAG, "  👾 UfoAttacker → secundario: HumanInterceptor");
            }
        }

        // ═══════════════════════════════════════════════════════════
        // 🚀 TEAM HUMAN → Ataca a Team Alien
        // ═══════════════════════════════════════════════════════════

        // DefenderShip: Primario = UfoScout, Secundario = UfoAttacker
        if (defenderShip != null) {
            if (ufoScout != null) {
                defenderShip.setTargetUfoScout(ufoScout);
                Log.d(TAG, "  🚀 DefenderShip → primario: UfoScout");
            }
            if (ufoAttacker != null) {
                defenderShip.setTargetUfoAttacker(ufoAttacker);
                Log.d(TAG, "  🚀 DefenderShip → secundario: UfoAttacker");
            }
        }

        // HumanInterceptor: Primario = UfoAttacker, Secundario = UfoScout
        if (humanInterceptor != null) {
            if (ufoAttacker != null) {
                humanInterceptor.setPrimaryTarget(ufoAttacker);
                Log.d(TAG, "  ✈️ HumanInterceptor → primario: UfoAttacker");
            }
            if (ufoScout != null) {
                humanInterceptor.setSecondaryTarget(ufoScout);
                Log.d(TAG, "  ✈️ HumanInterceptor → secundario: UfoScout");
            }
        }

        // ═══════════════════════════════════════════════════════════
        // 🚧 REFERENCIAS DE ALIADOS (para anti-colisión)
        // ═══════════════════════════════════════════════════════════

        // Team Human
        if (defenderShip != null && humanInterceptor != null) {
            defenderShip.setAllyInterceptor(humanInterceptor);
            humanInterceptor.setAllyDefender(defenderShip);
            Log.d(TAG, "  🤝 Team Human conectados para anti-colisión");
        }

        // Team Alien
        if (ufoScout != null && ufoAttacker != null) {
            ufoScout.setAllyAttacker(ufoAttacker);
            ufoAttacker.setAllyScout(ufoScout);
            Log.d(TAG, "  🤝 Team Alien conectados para anti-colisión");
        }

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Log.d(TAG, "✅ BATALLA 2v2 CONFIGURADA");
        Log.d(TAG, "   Team Human: DefenderShip + Interceptor");
        Log.d(TAG, "   Team Alien: UfoScout + UfoAttacker");
        Log.d(TAG, "   Anti-colisión: ACTIVO");
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void setupMusicStars() {
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

        // Greeting Text - DESHABILITADO (se usará en otros wallpapers)
        // try {
        //     GreetingText greetingText = new GreetingText(context);
        //     addSceneObject(greetingText);
        //     Log.d(TAG, "  ✓ 👋 Greeting agregado");
        // } catch (Exception e) {
        //     Log.e(TAG, "  ✗ Error creando Greeting: " + e.getMessage());
        // }

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

        // 🎮 BattleHUD - Barras de vida estilo Street Fighter
        try {
            battleHUD = new BattleHUD();
            // No agregar a sceneObjects - se dibuja manualmente después de todo
            Log.d(TAG, "  ✓ 🎮 BattleHUD agregado (estilo Street Fighter)");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando BattleHUD: " + e.getMessage());
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

    // setupLeaderboard() - REMOVIDO (código muerto)

    /**
     * 🔮 Configura el título holográfico "HUMANS vs ALIENS"
     * Con efectos de glitch, aberración cromática y scan lines
     */
    private void setupHolographicTitle() {
        try {
            holographicTitle = new HolographicTitle(context);
            addSceneObject(holographicTitle);
            Log.d(TAG, "  ✓ 🔮 HolographicTitle 'HUMANS vs ALIENS' creado");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando HolographicTitle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 🎯 Configura el sistema de targeting asistido
     * Permite al usuario disparar arma especial tocando enemigos con lock-on
     * También permite tocar directamente sobre una nave enemiga para atacarla
     */
    private void setupTargetingSystem() {
        try {
            // 1. Sistema de targeting (lógica de lock-on)
            targetingSystem = new TargetingSystem();
            targetingSystem.setInterceptor(humanInterceptor);
            targetingSystem.setTargets(ufoAttacker, ufoScout);
            targetingSystem.setCamera(camera);

            // Callback cuando se dispara el arma especial
            targetingSystem.setOnSpecialFireListener((targetX, targetY, targetZ, target) -> {
                Log.d(TAG, "⚡ ¡PLASMA BEAM disparado hacia (" + targetX + ", " + targetY + ", " + targetZ + ")!");

                // Usar PlasmaBeamWeapon con las 3 fases épicas
                if (plasmaBeamWeapon != null && humanInterceptor != null) {
                    // Obtener posición del interceptor como origen del rayo
                    float srcX = humanInterceptor.getX();
                    float srcY = humanInterceptor.getY();
                    float srcZ = humanInterceptor.getZ();

                    plasmaBeamWeapon.fire(srcX, srcY, srcZ, targetX, targetY, targetZ);
                }
            });

            Log.d(TAG, "  ✓ 🎯 TargetingSystem configurado");

            // 2. Mira visual (UI del targeting)
            targetReticle = new TargetReticle();
            targetReticle.setTargetingSystem(targetingSystem);
            targetReticle.setCameraController(camera);
            addSceneObject(targetReticle);
            Log.d(TAG, "  ✓ 🎯 TargetReticle agregado");

            // 3. ⚡ PLASMA BEAM WEAPON - Arma con 3 fases (carga, viaje, impacto)
            plasmaBeamWeapon = new PlasmaBeamWeapon();
            plasmaBeamWeapon.setCameraController(camera);
            addSceneObject(plasmaBeamWeapon);
            Log.d(TAG, "  ✓ ⚡ PlasmaBeamWeapon agregado (carga + viaje + impacto)");

            // 4. Efecto de explosión plasma (legacy, por si se necesita)
            plasmaExplosion = new PlasmaExplosion();
            plasmaExplosion.setCameraController(camera);
            addSceneObject(plasmaExplosion);
            Log.d(TAG, "  ✓ 💥 PlasmaExplosion agregado (legacy)");

            Log.d(TAG, "  ✓ 🎯 Sistema de targeting completo!");

        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando sistema de targeting: " + e.getMessage());
            e.printStackTrace();
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
                meteorShower.setHPSystem(tierra, hpBarTierra);
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

        // ═══════════════════════════════════════════════════════════════
        // CLEANUP de naves de batalla (liberar recursos OpenGL)
        // ═══════════════════════════════════════════════════════════════
        if (defenderShip != null) {
            defenderShip.cleanup();
            defenderShip = null;
        }

        if (humanInterceptor != null) {
            humanInterceptor.cleanup();
            humanInterceptor = null;
        }

        if (ufoScout != null) {
            ufoScout.cleanup();
            ufoScout = null;
        }

        if (ufoAttacker != null) {
            ufoAttacker.cleanup();
            ufoAttacker = null;
        }

        // Limpiar recursos estaticos de Laser (shaders compartidos)
        Laser.cleanupStatic();

        // ═══════════════════════════════════════════════════════════════
        // Limpiar otras referencias
        // ═══════════════════════════════════════════════════════════════
        tierra = null;
        planetaTierra = null;
        spaceStation = null;
        meteorShower = null;
        playerWeapon = null;
        powerBar = null;
        equalizerDJ = null;
        backgroundStars = null;
        musicStars = null;

        // ☄️ Liberar SpaceComets
        if (spaceComets != null) {
            spaceComets.cleanup();
            spaceComets = null;
        }

        // 🎮 Liberar BattleHUD
        if (battleHUD != null) {
            battleHUD.dispose();
            battleHUD = null;
        }

        // Liberar HolographicTitle
        if (holographicTitle != null) {
            holographicTitle.cleanup();
            holographicTitle = null;
        }

        // 🎯 Liberar sistema de targeting
        if (targetReticle != null) {
            targetReticle.release();
            targetReticle = null;
        }
        targetingSystem = null;
        plasmaExplosion = null;
        plasmaBeamWeapon = null;

        Log.d(TAG, "✓ Recursos OpenGL liberados correctamente");
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

    // updateLeaderboardUI() - REMOVIDO (código muerto)

    // ═══════════════════════════════════════════════════════════════════════
    // 🔄 UPDATE
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void update(float deltaTime) {
        // Llamar al update base (actualiza todos los sceneObjects)
        super.update(deltaTime);

        // 🎵 Actualizar ecualizador DJ (no está en sceneObjects)
        if (equalizerDJ != null) {
            equalizerDJ.update(deltaTime);
        }

        // 🎯 Actualizar sistema de targeting (no está en sceneObjects)
        if (targetingSystem != null) {
            targetingSystem.update(deltaTime);
        }

        // 🎮 Actualizar BattleHUD con HP de las 4 naves
        if (battleHUD != null) {
            battleHUD.update(deltaTime);

            // P1 = humanInterceptor (jugador)
            if (humanInterceptor != null) {
                battleHUD.setHumanP1_HP(humanInterceptor.getHealth(), humanInterceptor.getMaxHealth());
            }

            // dron = defenderShip (IA aliada)
            if (defenderShip != null) {
                battleHUD.setHumanDron_HP(defenderShip.getHealth(), defenderShip.getMaxHealth());
            }

            // P2 = ufoScout (alien 1)
            if (ufoScout != null) {
                battleHUD.setAlienP2_HP(ufoScout.getHealth(), ufoScout.getMaxHealth());
            }

            // IA = ufoAttacker (alien 2)
            if (ufoAttacker != null) {
                battleHUD.setAlienIA_HP(ufoAttacker.getHealth(), ufoAttacker.getMaxHealth());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 👆 TOUCH HANDLING - Sistema de disparo especial + selección directa
    // ═══════════════════════════════════════════════════════════════════════

    // Radio de tolerancia para detectar toque sobre nave (en coordenadas normalizadas)
    private static final float TOUCH_HIT_RADIUS = 0.25f;  // Radio generoso para facilitar selección

    /**
     * Maneja eventos de toque para el sistema de targeting
     * 1. Si el usuario toca SOBRE una nave enemiga → lock + disparo inmediato
     * 2. Si ya hay lock → dispara al objetivo lockeado
     */
    @Override
    public boolean onTouchEvent(float normalizedX, float normalizedY, int action) {
        // Solo procesar ACTION_DOWN para disparar
        if (action != MotionEvent.ACTION_DOWN) {
            return false;
        }

        // ═══════════════════════════════════════════════════════════════
        // 🎮 VALIDACIÓN: El jugador (P1) debe estar vivo para disparar
        // ═══════════════════════════════════════════════════════════════
        if (humanInterceptor == null || !humanInterceptor.canFireManually()) {
            Log.d(TAG, "❌ No se puede disparar: nave P1 destruida o en respawn");
            return false;
        }

        // ═══════════════════════════════════════════════════════════════
        // 🎯 PRIMERO: Verificar si tocó directamente sobre una nave enemiga
        // ═══════════════════════════════════════════════════════════════
        Object touchedEnemy = checkTouchOnEnemy(normalizedX, normalizedY);

        if (touchedEnemy != null) {
            // ¡Tocó una nave enemiga!
            Log.d(TAG, "👆🎯 ¡Nave enemiga tocada! → " + touchedEnemy.getClass().getSimpleName());

            // 1. COLOCAR LA MIRA sobre el enemigo tocado
            if (targetingSystem != null) {
                targetingSystem.setManualTarget(touchedEnemy);
                Log.d(TAG, "🎯 Mira colocada sobre " + touchedEnemy.getClass().getSimpleName());
            }

            // 2. DISPARAR inmediatamente al enemigo tocado
            float targetX, targetY, targetZ;
            if (touchedEnemy instanceof UfoAttacker) {
                UfoAttacker ufo = (UfoAttacker) touchedEnemy;
                targetX = ufo.x;
                targetY = ufo.y;
                targetZ = ufo.z;
            } else if (touchedEnemy instanceof UfoScout) {
                UfoScout ufo = (UfoScout) touchedEnemy;
                targetX = ufo.getX();
                targetY = ufo.getY();
                targetZ = ufo.getZ();
            } else {
                return false;
            }

            // Disparar PlasmaBeamWeapon desde el interceptor
            if (plasmaBeamWeapon != null && humanInterceptor != null && !plasmaBeamWeapon.isActive()) {
                float srcX = humanInterceptor.getX();
                float srcY = humanInterceptor.getY();
                float srcZ = humanInterceptor.getZ();

                plasmaBeamWeapon.fire(srcX, srcY, srcZ, targetX, targetY, targetZ);
                Log.d(TAG, "⚡👆 ¡DISPARO activado hacia " + touchedEnemy.getClass().getSimpleName() + "!");

                // Aplicar daño al enemigo tocado
                applyDamageToEnemy(touchedEnemy);

                return true;
            }
            return true;  // Mira colocada aunque no se pueda disparar aún
        }

        // ═══════════════════════════════════════════════════════════════
        // 🎯 SEGUNDO: Si hay lock-on activo, disparar al objetivo lockeado
        // ═══════════════════════════════════════════════════════════════
        if (targetingSystem != null && targetingSystem.isLocked()) {
            // El usuario tocó mientras hay un objetivo lockeado
            if (targetingSystem.fireFromTouch()) {
                Log.d(TAG, "🎯👆 ¡Disparo especial activado por toque (lock-on)!");
                return true;
            }
        }

        return false;  // No consumido
    }

    /**
     * Verifica si el toque está cerca de alguna nave enemiga
     * @param normalizedX coordenada X normalizada (-1 a 1)
     * @param normalizedY coordenada Y normalizada (-1 a 1)
     * @return El enemigo tocado, o null si no tocó ninguno
     */
    private Object checkTouchOnEnemy(float normalizedX, float normalizedY) {
        // Convertir coordenadas de nave a coordenadas de pantalla aproximadas
        // y verificar si el toque está dentro del radio de tolerancia

        // Verificar UfoAttacker
        if (ufoAttacker != null && !ufoAttacker.isDestroyed()) {
            float[] screenPos = worldToScreenApprox(ufoAttacker.x, ufoAttacker.y, ufoAttacker.z);
            float dx = normalizedX - screenPos[0];
            float dy = normalizedY - screenPos[1];
            float distance = (float) Math.sqrt(dx*dx + dy*dy);

            if (distance <= TOUCH_HIT_RADIUS) {
                return ufoAttacker;
            }
        }

        // Verificar UfoScout
        if (ufoScout != null && !ufoScout.isDestroyed()) {
            float[] screenPos = worldToScreenApprox(ufoScout.getX(), ufoScout.getY(), ufoScout.getZ());
            float dx = normalizedX - screenPos[0];
            float dy = normalizedY - screenPos[1];
            float distance = (float) Math.sqrt(dx*dx + dy*dy);

            if (distance <= TOUCH_HIT_RADIUS) {
                return ufoScout;
            }
        }

        return null;
    }

    /**
     * Convierte coordenadas del mundo a coordenadas de pantalla normalizadas (-1 a 1)
     * Usa la matriz VP de la cámara para proyección correcta
     */
    private float[] worldToScreenApprox(float worldX, float worldY, float worldZ) {
        if (camera == null) {
            // Fallback a aproximación si no hay cámara
            return new float[]{worldX * 0.25f, worldY * 0.25f};
        }

        // Obtener matriz View-Projection
        float[] vpMatrix = camera.getViewProjectionMatrix();

        // Proyectar punto 3D: [x, y, z, 1] * VP = [clipX, clipY, clipZ, clipW]
        float clipX = vpMatrix[0] * worldX + vpMatrix[4] * worldY + vpMatrix[8] * worldZ + vpMatrix[12];
        float clipY = vpMatrix[1] * worldX + vpMatrix[5] * worldY + vpMatrix[9] * worldZ + vpMatrix[13];
        float clipW = vpMatrix[3] * worldX + vpMatrix[7] * worldY + vpMatrix[11] * worldZ + vpMatrix[15];

        // Evitar división por cero
        if (Math.abs(clipW) < 0.001f) {
            return new float[]{0f, 0f};
        }

        // Dividir por W para obtener coordenadas normalizadas (NDC)
        float ndcX = clipX / clipW;
        float ndcY = clipY / clipW;

        // Clamp a rango válido
        ndcX = Math.max(-1f, Math.min(1f, ndcX));
        ndcY = Math.max(-1f, Math.min(1f, ndcY));

        return new float[]{ndcX, ndcY};
    }

    /**
     * Aplica daño al enemigo tocado (3x daño como el plasma normal)
     */
    private void applyDamageToEnemy(Object enemy) {
        int damageMultiplier = 3;

        if (enemy instanceof UfoAttacker) {
            UfoAttacker ufo = (UfoAttacker) enemy;
            for (int i = 0; i < damageMultiplier; i++) {
                ufo.takeDamage();
            }
            Log.d(TAG, "💥 UfoAttacker recibió " + damageMultiplier + "x daño por disparo libre!");
        } else if (enemy instanceof UfoScout) {
            UfoScout ufo = (UfoScout) enemy;
            for (int i = 0; i < damageMultiplier; i++) {
                ufo.takeDamage();
            }
            Log.d(TAG, "💥 UfoScout recibió " + damageMultiplier + "x daño por disparo libre!");
        }
    }

    @Override
    public void draw() {
        // Dibujar todos los objetos de la escena primero
        super.draw();

        // 🎮 Dibujar BattleHUD (barras de vida estilo Street Fighter)
        if (battleHUD != null) {
            battleHUD.draw();
        }

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

        // 🎮 Pasar aspect ratio al BattleHUD
        if (battleHUD != null && height > 0) {
            float aspectRatio = (float) width / height;
            battleHUD.setAspectRatio(aspectRatio);
        }
    }
}
