package com.secret.blackholeglow.scenes;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   📐 SceneConstants - Constantes Configurables de Escenas         ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * Este archivo centraliza todos los valores numéricos de las escenas
 * para facilitar ajustes sin buscar en el código.
 *
 * BENEFICIOS:
 * - Fácil de ajustar valores sin buscar en el código
 * - Documentación clara de cada parámetro
 * - Posibilidad de cambiar a configuración dinámica (Remote Config)
 * - Evita "números mágicos" dispersos
 */
public final class SceneConstants {

    private SceneConstants() {
        // No instanciable
    }

    // ═══════════════════════════════════════════════════════════════════
    // ☀️ SOL PROCEDURAL
    // ═══════════════════════════════════════════════════════════════════

    public static final class Sun {
        /** Posición X del sol (hacia la izquierda) */
        public static final float POSITION_X = -1.5f;

        /** Posición Y del sol (centro vertical) */
        public static final float POSITION_Y = 0.5f;

        /** Posición Z del sol (profundidad) */
        public static final float POSITION_Z = -5.0f;

        /** Escala del sol - MÁS GRANDE que la Tierra (realista) */
        public static final float SCALE = 0.8f;
    }

    // ═══════════════════════════════════════════════════════════════════
    // 🌍 TIERRA
    // ═══════════════════════════════════════════════════════════════════

    public static final class Earth {
        /** Posición X inicial de la Tierra (será modificada por órbita) */
        public static final float POSITION_X = 0.0f;

        /** Posición Y de la Tierra (misma altura que el Sol para órbita horizontal) */
        public static final float POSITION_Y = 0.5f;

        /** Posición Z inicial (será modificada por órbita) */
        public static final float POSITION_Z = -5.0f;

        /** Tamaño/escala de la Tierra - MENOR que el Sol (realista) */
        public static final float SCALE = 0.25f;

        /** Velocidad de rotación sobre su eje (grados/segundo) */
        public static final float ROTATION_SPEED = 5.0f;

        /** HP máximo de la Tierra */
        public static final int MAX_HP = 200;

        /** Radio de órbita X (horizontal) - La Tierra orbita el Sol */
        public static final float ORBIT_RADIUS_X = 1.8f;

        /** Radio de órbita Z (profundidad) - Órbita elíptica */
        public static final float ORBIT_RADIUS_Z = 1.2f;

        /** Velocidad de órbita alrededor del Sol */
        public static final float ORBIT_SPEED = 0.15f;

        /** Variación de escala (0 = sin pulsación) */
        public static final float SCALE_VARIATION = 0.0f;
    }

    // ═══════════════════════════════════════════════════════════════════
    // 🛡️ ESCUDOS
    // ═══════════════════════════════════════════════════════════════════

    public static final class Shield {
        /** Radio del EarthShield (escudo de impactos) */
        public static final float EARTH_SHIELD_RADIUS = 1.30f;

        /** Radio del ForceField (campo visible) */
        public static final float FORCE_FIELD_RADIUS = 1.55f;

        /** HP máximo del ForceField */
        public static final int FORCE_FIELD_MAX_HP = 50;

        /** Color del ForceField - Rojo (0-1) */
        public static final float FORCE_FIELD_COLOR_R = 0.3f;

        /** Color del ForceField - Verde (0-1) */
        public static final float FORCE_FIELD_COLOR_G = 0.9f;

        /** Color del ForceField - Azul (0-1) */
        public static final float FORCE_FIELD_COLOR_B = 1.0f;

        /** Intensidad base del ForceField */
        public static final float FORCE_FIELD_INTENSITY = 0.08f;

        /** Velocidad de pulso del ForceField */
        public static final float FORCE_FIELD_PULSE_SPEED = 0.028f;

        /** Amplitud de pulso del ForceField */
        public static final float FORCE_FIELD_PULSE_AMPLITUDE = 0.240f;
    }

    // ═══════════════════════════════════════════════════════════════════
    // 🛸 OVNI
    // ═══════════════════════════════════════════════════════════════════

    public static final class Ufo {
        /** Posición inicial X */
        public static final float START_POSITION_X = 1.8f;

        /** Posición inicial Y */
        public static final float START_POSITION_Y = 1.5f;

        /** Posición inicial Z */
        public static final float START_POSITION_Z = -1.0f;

        /** Escala del OVNI */
        public static final float SCALE = 0.07f;

        /** Radio de órbita alrededor de la Tierra */
        public static final float ORBIT_RADIUS = 2.5f;

        /** Velocidad de órbita */
        public static final float ORBIT_SPEED = 0.4f;

        /** Fase inicial de órbita */
        public static final float ORBIT_PHASE = 0.0f;

        /** Distancia segura de la Tierra (para IA de esquiva) */
        public static final float SAFE_DISTANCE = 1.8f;
    }

    // ═══════════════════════════════════════════════════════════════════
    // ✨ ESTRELLAS BAILARINAS
    // ═══════════════════════════════════════════════════════════════════

    public static final class DancingStars {
        /** Escala de cada estrella */
        public static final float SCALE = 0.02f;

        /** Posiciones predefinidas: {x, y, z, velocidad} */
        public static final float[][] POSITIONS = {
            {1.8f, 0.8f, 0.5f, 45.0f},
            {-1.5f, 0.3f, -0.8f, 38.0f},
            {0.5f, -0.6f, 1.2f, 52.0f}
        };
    }

    // ═══════════════════════════════════════════════════════════════════
    // 📊 UI ELEMENTS
    // ═══════════════════════════════════════════════════════════════════

    public static final class UI {
        // HP Bar Tierra
        public static final float HP_BAR_EARTH_X = 0.05f;
        public static final float HP_BAR_EARTH_Y = 0.92f;
        public static final float HP_BAR_EARTH_WIDTH = 0.25f;
        public static final float HP_BAR_EARTH_HEIGHT = 0.03f;

        // HP Bar ForceField
        public static final float HP_BAR_SHIELD_X = 0.05f;
        public static final float HP_BAR_SHIELD_Y = 0.87f;
        public static final float HP_BAR_SHIELD_WIDTH = 0.25f;
        public static final float HP_BAR_SHIELD_HEIGHT = 0.03f;

        // Music Indicator (2D - legacy)
        public static final float MUSIC_INDICATOR_X = -0.250f;
        public static final float MUSIC_INDICATOR_Y = -0.35f;
        public static final float MUSIC_INDICATOR_WIDTH = 0.50f;
        public static final float MUSIC_INDICATOR_HEIGHT = 0.12f;

        // Music Indicator 3D - Posición base del grupo completo
        public static final float MUSIC_INDICATOR_3D_X = 0.0f;        // Centro horizontal
        public static final float MUSIC_INDICATOR_3D_Y = -1.5f;       // Arriba de controles
        public static final float MUSIC_INDICATOR_3D_Z = 0.0f;        // Profundidad base
        public static final float MUSIC_INDICATOR_3D_WIDTH = 2.2f;    // Ancho total (legacy)
        public static final float MUSIC_INDICATOR_3D_HEIGHT = 0.6f;   // Altura (legacy)
        public static final float MUSIC_INDICATOR_3D_DEPTH = 0.06f;   // Profundidad (legacy)

        // Planets Counter
        public static final float PLANETS_COUNTER_X = 0.50f;
        public static final float PLANETS_COUNTER_Y = 0.60f;
        public static final float PLANETS_COUNTER_WIDTH = 0.40f;
        public static final float PLANETS_COUNTER_HEIGHT = 0.10f;

        // Leaderboard
        public static final float LEADERBOARD_X = -0.95f;
        public static final float LEADERBOARD_START_Y = 0.55f;
        public static final float LEADERBOARD_WIDTH = 0.50f;
        public static final float LEADERBOARD_HEIGHT = 0.06f;
        public static final float LEADERBOARD_SPACING = 0.10f;
    }

    // ═══════════════════════════════════════════════════════════════════
    // 🎵 ECUALIZADOR 3D - BARRAS INDIVIDUALES
    // ═══════════════════════════════════════════════════════════════════
    // Cada barra tiene: posición (X,Y,Z), rotación (X,Y,Z), tamaño (ancho,alto,profundidad)
    // Las barras van de izquierda (0) a derecha (6)
    // BASS = barras 0,1,2 | MID = barra 3 | TREBLE = barras 4,5,6

    public static final class EqBar0 {
        // 🔊 BARRA 0 - BASS (más a la izquierda, la más grande)
        public static final float POS_X = -0.9f;      // Posición X (negativo = izquierda)
        public static final float POS_Y = 0.0f;       // Posición Y (base de la barra)
        public static final float POS_Z = 0.0f;       // Posición Z (profundidad)
        public static final float ROT_X = 0.0f;       // Rotación en X (grados)
        public static final float ROT_Y = 0.0f;       // Rotación en Y (grados)
        public static final float ROT_Z = 0.0f;       // Rotación en Z (grados)
        public static final float WIDTH = 0.12f;      // Ancho de la barra
        public static final float HEIGHT = 0.6f;      // Altura máxima
        public static final float DEPTH = 0.06f;      // Profundidad
        public static final float SENSITIVITY = 1.2f; // Sensibilidad al audio
    }

    public static final class EqBar1 {
        // 🔊 BARRA 1 - BASS
        public static final float POS_X = -0.6f;
        public static final float POS_Y = 0.0f;
        public static final float POS_Z = 0.0f;
        public static final float ROT_X = 0.0f;
        public static final float ROT_Y = 0.0f;
        public static final float ROT_Z = 0.0f;
        public static final float WIDTH = 0.10f;
        public static final float HEIGHT = 0.55f;
        public static final float DEPTH = 0.06f;
        public static final float SENSITIVITY = 1.4f;
    }

    public static final class EqBar2 {
        // 🔊 BARRA 2 - BASS/MID
        public static final float POS_X = -0.3f;
        public static final float POS_Y = 0.25f;
        public static final float POS_Z = 0.0f;
        public static final float ROT_X = 0.0f;
        public static final float ROT_Y = 0.0f;
        public static final float ROT_Z = 0.0f;
        public static final float WIDTH = 0.09f;
        public static final float HEIGHT = 0.50f;
        public static final float DEPTH = 0.06f;
        public static final float SENSITIVITY = 2.0f;
    }

    public static final class EqBar3 {
        // 🎵 BARRA 3 - MID (centro)
        public static final float POS_X = 0.0f;
        public static final float POS_Y = 0.2f;
        public static final float POS_Z = 0.0f;
        public static final float ROT_X = 0.0f;
        public static final float ROT_Y = 0.0f;
        public static final float ROT_Z = 0.0f;
        public static final float WIDTH = 0.08f;
        public static final float HEIGHT = 0.45f;
        public static final float DEPTH = 0.06f;
        public static final float SENSITIVITY = 3.0f;
    }

    public static final class EqBar4 {
        // 🎶 BARRA 4 - MID/TREBLE
        public static final float POS_X = 0.3f;
        public static final float POS_Y = 0.4f;
        public static final float POS_Z = 0.0f;
        public static final float ROT_X = 0.0f;
        public static final float ROT_Y = 0.0f;
        public static final float ROT_Z = 0.0f;
        public static final float WIDTH = 0.07f;
        public static final float HEIGHT = 0.40f;
        public static final float DEPTH = 0.06f;
        public static final float SENSITIVITY = 7.0f;
    }

    public static final class EqBar5 {
        // 🎶 BARRA 5 - TREBLE
        public static final float POS_X = 0.6f;
        public static final float POS_Y = 0.4f;
        public static final float POS_Z = 0.0f;
        public static final float ROT_X = 0.0f;
        public static final float ROT_Y = 0.0f;
        public static final float ROT_Z = 0.0f;
        public static final float WIDTH = 0.06f;
        public static final float HEIGHT = 0.35f;
        public static final float DEPTH = 0.06f;
        public static final float SENSITIVITY = 9.0f;
    }

    public static final class EqBar6 {
        // 🎶 BARRA 6 - TREBLE (más a la derecha, la más pequeña)
        public static final float POS_X = 0.9f;
        public static final float POS_Y = 0.530f;
        public static final float POS_Z = 0.0f;
        public static final float ROT_X = 0.0f;
        public static final float ROT_Y = 0.0f;
        public static final float ROT_Z = 0.0f;
        public static final float WIDTH = 0.05f;
        public static final float HEIGHT = 0.30f;
        public static final float DEPTH = 0.06f;
        public static final float SENSITIVITY = 12.0f;
    }


    // ═══════════════════════════════════════════════════════════════════
    // ⏱️ TIEMPOS Y INTERVALOS
    // ═══════════════════════════════════════════════════════════════════

    public static final class Timing {
        /** Intervalo de actualización del leaderboard (ms) */
        public static final long LEADERBOARD_UPDATE_INTERVAL = 30000;

        /** Intervalo mínimo de disparo del OVNI (segundos) */
        public static final float UFO_SHOOT_MIN_INTERVAL = 3.0f;

        /** Intervalo máximo de disparo del OVNI (segundos) */
        public static final float UFO_SHOOT_MAX_INTERVAL = 7.0f;

        /** Tiempo de invencibilidad del OVNI después de daño (segundos) */
        public static final float UFO_INVINCIBILITY_TIME = 1.5f;

        /** Tiempo de respawn del OVNI (segundos) */
        public static final float UFO_RESPAWN_DELAY = 8.0f;
    }

    // ═══════════════════════════════════════════════════════════════════
    // 🎨 COLORES
    // ═══════════════════════════════════════════════════════════════════

    public static final class Colors {
        // HP Bar Tierra - Color lleno (verde)
        public static final float[] HP_EARTH_FULL = {0.2f, 0.8f, 0.3f, 1.0f};

        // HP Bar Tierra - Color vacío (rojo)
        public static final float[] HP_EARTH_EMPTY = {1.0f, 0.0f, 0.0f, 1.0f};

        // HP Bar Escudo - Color lleno (azul)
        public static final float[] HP_SHIELD_FULL = {0.2f, 0.6f, 1.0f, 1.0f};

        // HP Bar Escudo - Color vacío (rojo)
        public static final float[] HP_SHIELD_EMPTY = {1.0f, 0.0f, 0.0f, 1.0f};

        // Leaderboard medals (RGB int)
        public static final int MEDAL_GOLD = 0xFFD700;      // Oro
        public static final int MEDAL_SILVER = 0xC0C0C0;    // Plata
        public static final int MEDAL_BRONZE = 0xCD7F32;    // Bronce

        // Planets counter
        public static final int PLANETS_COUNTER_COLOR = 0x6496FF; // Azul claro
    }

    // ═══════════════════════════════════════════════════════════════════
    // 📱 PANTALLA (valores por defecto)
    // ═══════════════════════════════════════════════════════════════════

    public static final class Screen {
        /** Ancho por defecto (se sobrescribe en runtime) */
        public static final int DEFAULT_WIDTH = 1080;

        /** Alto por defecto (se sobrescribe en runtime) */
        public static final int DEFAULT_HEIGHT = 1920;
    }
}
