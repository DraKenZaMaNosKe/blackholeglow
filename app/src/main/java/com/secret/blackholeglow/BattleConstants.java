package com.secret.blackholeglow;

/**
 * ============================================================================
 *  BattleConstants - Constantes de la Batalla Cosmica
 * ============================================================================
 *
 *  Centraliza todos los valores de configuracion para la escena de batalla.
 *  Evita "magic numbers" dispersos en el codigo.
 *
 *  Organizado por categoria:
 *  - BOUNDS: Limites de movimiento
 *  - DEFENDER: DefenderShip
 *  - INTERCEPTOR: HumanInterceptor
 *  - UFO_SCOUT: UfoScout
 *  - UFO_ATTACKER: UfoAttacker
 *  - LASER: Configuracion de laseres
 *  - COMBAT: Parametros de combate
 *
 * ============================================================================
 */
public final class BattleConstants {

    private BattleConstants() {} // No instanciar

    // =========================================================================
    // LIMITES DE MOVIMIENTO (Compartidos)
    // =========================================================================
    public static final float BOUND_X = 2.5f;
    public static final float BOUND_Y_MIN = 0.3f;
    public static final float BOUND_Y_MAX = 3.5f;
    public static final float BOUND_Z_MIN = -3.5f;
    public static final float BOUND_Z_MAX = 2.5f;

    // =========================================================================
    // DEFENDER SHIP - Nave Defensora Humana
    // =========================================================================
    public static final class Defender {
        // Vida
        public static final int MAX_HEALTH = 5;
        public static final float RESPAWN_DELAY = 6.0f;
        public static final float INVINCIBILITY_DURATION = 1.5f;

        // Movimiento
        public static final float MOVE_SPEED = 0.8f;
        public static final float MAX_SPEED = 2.0f;
        public static final float ACCELERATION = 3.0f;
        public static final float DECELERATION = 0.94f;
        public static final float SAFE_DISTANCE = 1.2f;

        // Disparo
        public static final int MAX_LASERS = 8;
        public static final float SHOOT_INTERVAL = 2.0f;
        public static final float MIN_SHOOT_INTERVAL = 1.2f;
        public static final float MAX_SHOOT_INTERVAL = 2.5f;
        public static final float ATTACK_RANGE = 12.0f;

        // Escudo de Energia
        public static final float SHIELD_DURATION = 4.0f;
        public static final float SHIELD_COOLDOWN = 15.0f;
        public static final int SHIELD_SEGMENTS = 32;

        // Misiles Rastreadores
        public static final int MAX_MISSILES = 4;
        public static final float MISSILE_SPEED = 4.0f;
        public static final float MISSILE_TURN_RATE = 3.0f;
        public static final float MISSILE_COOLDOWN = 12.0f;

        // Explosion
        public static final float EXPLOSION_DURATION = 1.5f;
        public static final int EXPLOSION_PARTICLES = 20;
    }

    // =========================================================================
    // HUMAN INTERCEPTOR - Caza Interceptor Humano
    // =========================================================================
    public static final class Interceptor {
        // Vida
        public static final int MAX_HEALTH = 4;
        public static final float RESPAWN_DELAY = 7.0f;
        public static final float INVINCIBILITY_DURATION = 1.2f;

        // Movimiento (mas rapido que Defender)
        public static final float MOVE_SPEED = 1.2f;
        public static final float MAX_SPEED = 2.5f;
        public static final float ACCELERATION = 2.5f;
        public static final float DECELERATION = 0.92f;
        public static final float SAFE_DISTANCE = 1.2f;

        // Disparo
        public static final int MAX_LASERS = 10;
        public static final float MIN_SHOOT_INTERVAL = 0.5f;
        public static final float MAX_SHOOT_INTERVAL = 1.0f;
        public static final float ATTACK_RANGE = 15.0f;
        public static final float EVADE_RANGE = 0.6f;

        // Boost/Afterburner
        public static final float BOOST_DURATION = 3.0f;
        public static final float BOOST_COOLDOWN = 10.0f;
        public static final float BOOST_SPEED_MULT = 1.8f;

        // Teletransporte Tactico
        public static final float TELEPORT_COOLDOWN = 10.0f;
        public static final float TELEPORT_EFFECT_DURATION = 0.5f;
        public static final int TELEPORT_PARTICLES = 32;

        // Rafaga Triple
        public static final float TRIPLE_BURST_DURATION = 5.0f;
        public static final float TRIPLE_BURST_COOLDOWN = 12.0f;
        public static final float TRIPLE_BURST_SPREAD = 0.15f; // radianes

        // Curva de vuelo
        public static final float CURVE_AMPLITUDE = 0.4f;
        public static final float CURVE_FREQUENCY = 2.0f;
    }

    // =========================================================================
    // UFO SCOUT - OVNI Explorador
    // =========================================================================
    public static final class UfoScout {
        // Vida
        public static final int MAX_HEALTH = 3;
        public static final float RESPAWN_DELAY = 5.0f;
        public static final float INVINCIBILITY_DURATION = 1.0f;

        // Movimiento
        public static final float MOVE_SPEED = 1.0f;
        public static final float MAX_SPEED = 2.2f;
        public static final float ACCELERATION = 2.0f;
        public static final float DECELERATION = 0.93f;
        public static final float SAFE_DISTANCE = 1.0f;

        // Disparo
        public static final int MAX_LASERS = 6;
        public static final float SHOOT_INTERVAL = 2.5f;
        public static final float ATTACK_RANGE = 15.0f;
        public static final float CIRCLE_RANGE = 3.0f;

        // Teletransporte
        public static final float TELEPORT_DURATION = 0.5f;
        public static final float TELEPORT_COOLDOWN = 8.0f;
    }

    // =========================================================================
    // UFO ATTACKER - OVNI Atacante
    // =========================================================================
    public static final class UfoAttacker {
        // Vida
        public static final int MAX_HEALTH = 6;
        public static final float RESPAWN_DELAY = 8.0f;
        public static final float INVINCIBILITY_DURATION = 1.5f;

        // Movimiento
        public static final float MOVE_SPEED = 0.6f;
        public static final float MAX_SPEED = 1.8f;
        public static final float ACCELERATION = 1.8f;
        public static final float DECELERATION = 0.95f;
        public static final float SAFE_DISTANCE = 1.2f;

        // Disparo
        public static final int MAX_LASERS = 10;
        public static final float SHOOT_INTERVAL = 1.8f;
        public static final float MIN_SHOOT_INTERVAL = 1.5f;
        public static final float MAX_SHOOT_INTERVAL = 2.5f;
        public static final float ATTACK_RANGE = 8.0f;

        // Overcharge
        public static final float OVERCHARGE_DURATION = 4.0f;
        public static final float OVERCHARGE_COOLDOWN = 15.0f;
        public static final float OVERCHARGE_SPEED_MULT = 2.0f;
        public static final float OVERCHARGE_SHOOT_MULT = 0.33f;
    }

    // =========================================================================
    // LASER - Configuracion de proyectiles
    // =========================================================================
    public static final class Laser {
        public static final float SPEED = 6.0f;
        public static final float MAX_DISTANCE = 15.0f;
        public static final float LIFETIME = 3.0f;

        // Dimensiones del nucleo
        public static final float CORE_LENGTH = 0.12f;
        public static final float CORE_WIDTH = 0.006f;

        // Estela
        public static final int TRAIL_SEGMENTS = 8;
        public static final float TRAIL_LENGTH = 0.15f;
        public static final float TRAIL_WIDTH = 0.004f;

        // Particulas de energia
        public static final int ENERGY_PARTICLES = 4;
        public static final float PARTICLE_SIZE = 0.008f;

        // Impacto
        public static final float IMPACT_DURATION = 0.3f;
        public static final int IMPACT_PARTICLES = 10;

        // Colores por equipo (RGBA)
        public static final float[] TEAM_HUMAN_COLOR = {0.3f, 0.7f, 1.0f, 1.0f}; // Cyan
        public static final float[] TEAM_ALIEN_COLOR = {0.0f, 1.0f, 0.3f, 1.0f}; // Verde
    }

    // =========================================================================
    // COMBATE - Parametros generales
    // =========================================================================
    public static final class Combat {
        // Colision
        public static final float COLLISION_PUSH_FORCE = 3.0f;
        public static final float ENEMY_SAFE_DISTANCE_MULT = 0.7f; // 70% de safe distance

        // Comportamiento IA
        public static final float WANDER_INTERVAL = 2.0f;
        public static final float ORBIT_RADIUS_MIN = 1.5f;
        public static final float ORBIT_RADIUS_MAX = 2.5f;

        // Estados de combate
        public static final int STATE_PATROLLING = 0;
        public static final int STATE_ATTACKING = 1;
        public static final int STATE_EVADING = 2;
        public static final int STATE_PURSUING = 3;
    }

    // =========================================================================
    // COLORES - Paleta de la batalla
    // =========================================================================
    public static final class Colors {
        // Team Human
        public static final float[] HUMAN_PRIMARY = {0.3f, 0.7f, 1.0f, 1.0f};   // Cyan
        public static final float[] HUMAN_SECONDARY = {0.2f, 0.5f, 0.8f, 1.0f}; // Azul
        public static final float[] HUMAN_ACCENT = {1.0f, 0.4f, 0.3f, 1.0f};    // Rojo

        // Team Alien
        public static final float[] ALIEN_PRIMARY = {0.0f, 1.0f, 0.3f, 1.0f};   // Verde
        public static final float[] ALIEN_SECONDARY = {0.8f, 0.2f, 1.0f, 1.0f}; // Magenta
        public static final float[] ALIEN_ACCENT = {1.0f, 0.5f, 0.0f, 1.0f};    // Naranja

        // Efectos
        public static final float[] SHIELD_COLOR = {0.3f, 0.6f, 1.0f, 0.5f};    // Azul transparente
        public static final float[] EXPLOSION_COLOR = {1.0f, 0.5f, 0.2f, 1.0f}; // Naranja fuego
        public static final float[] TELEPORT_COLOR = {0.5f, 1.0f, 1.0f, 1.0f};  // Cyan brillante
    }
}
