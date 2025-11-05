// EnemyAI.java - Inteligencia artificial para naves enemigas
package com.secret.blackholeglow;

import android.util.Log;

import java.util.Random;

/**
 * IA simple pero efectiva para naves enemigas.
 * Comportamientos:
 * - Perseguir al jugador
 * - Mantener distancia de combate
 * - Movimiento evasivo
 * - Disparar periódicamente
 */
public class EnemyAI {
    private static final String TAG = "EnemyAI";
    private static final Random random = new Random();

    // Parámetros de comportamiento
    private static final float PURSUIT_SPEED = 2.0f;        // Velocidad de persecución
    private static final float COMBAT_DISTANCE = 3.0f;      // Distancia ideal de combate
    private static final float EVASION_DISTANCE = 1.5f;     // Distancia mínima antes de evadir
    private static final float EVASION_STRENGTH = 3.0f;     // Fuerza del movimiento evasivo
    private static final float WANDER_AMOUNT = 1.0f;        // Cantidad de movimiento aleatorio

    /**
     * Actualiza el comportamiento de una nave enemiga
     *
     * @param enemy Nave enemiga a controlar
     * @param player Nave del jugador (objetivo)
     * @param deltaTime Tiempo delta
     */
    public static void updateBehavior(Spaceship enemy, Spaceship player, float deltaTime) {
        if (enemy.isDead || player.isDead) {
            enemy.vx = 0;
            enemy.vy = 0;
            return;
        }

        // Calcular vector hacia el jugador
        float dx = player.x - enemy.x;
        float dy = player.y - enemy.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        // Normalizar dirección
        float dirX = 0, dirY = 0;
        if (distance > 0) {
            dirX = dx / distance;
            dirY = dy / distance;
        }

        // Decidir comportamiento según distancia
        if (distance < EVASION_DISTANCE) {
            // Muy cerca: evadir (alejarse)
            enemy.vx = -dirX * EVASION_STRENGTH;
            enemy.vy = -dirY * EVASION_STRENGTH;

        } else if (distance > COMBAT_DISTANCE * 1.5f) {
            // Muy lejos: perseguir agresivamente
            enemy.vx = dirX * PURSUIT_SPEED;
            enemy.vy = dirY * PURSUIT_SPEED;

        } else {
            // Distancia óptima: movimiento evasivo lateral
            // Movimiento perpendicular al jugador
            float perpX = -dirY;
            float perpY = dirX;

            // Alternar dirección basándose en posición
            float side = (enemy.x + enemy.y) % 2 < 1 ? 1f : -1f;

            enemy.vx = perpX * PURSUIT_SPEED * 0.7f * side;
            enemy.vy = perpY * PURSUIT_SPEED * 0.7f * side;

            // Agregar un poco de movimiento hacia/desde el jugador
            float approachFactor = (distance - COMBAT_DISTANCE) / COMBAT_DISTANCE;
            enemy.vx += dirX * PURSUIT_SPEED * approachFactor * 0.3f;
            enemy.vy += dirY * PURSUIT_SPEED * approachFactor * 0.3f;
        }

        // Agregar un poco de movimiento aleatorio (wandering)
        enemy.vx += (random.nextFloat() - 0.5f) * WANDER_AMOUNT;
        enemy.vy += (random.nextFloat() - 0.5f) * WANDER_AMOUNT;
    }

    /**
     * Decide si el enemigo debe disparar
     *
     * @param enemy Nave enemiga
     * @param player Nave del jugador
     * @return true si debe disparar
     */
    public static boolean shouldFire(Spaceship enemy, Spaceship player) {
        if (enemy.isDead || player.isDead) return false;
        if (!enemy.canFire()) return false;

        // Calcular distancia al jugador
        float dx = player.x - enemy.x;
        float dy = player.y - enemy.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        // Solo disparar si está en rango razonable
        if (distance > COMBAT_DISTANCE * 2.0f) return false;

        // Disparar con probabilidad (para variación)
        return random.nextFloat() < 0.3f; // 30% de probabilidad cada frame que puede disparar
    }

    /**
     * Calcula la dirección de disparo del enemigo hacia el jugador
     *
     * @param enemy Nave enemiga
     * @param player Nave del jugador
     * @return Array [dirX, dirY, dirZ] normalizado
     */
    public static float[] calculateFireDirection(Spaceship enemy, Spaceship player) {
        float dx = player.x - enemy.x;
        float dy = player.y - enemy.y;
        float dz = player.z - enemy.z;

        float magnitude = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (magnitude > 0) {
            return new float[]{
                dx / magnitude,
                dy / magnitude,
                dz / magnitude
            };
        }

        return new float[]{0f, -1f, 0f}; // Default: hacia abajo
    }
}
