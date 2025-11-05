// PlayerAI.java - IA para el jugador en modo automático
package com.secret.blackholeglow;

import java.util.List;
import java.util.Random;

/**
 * IA simple para controlar la nave del jugador automáticamente.
 * Comportamientos:
 * - Esquivar proyectiles enemigos
 * - Posicionarse estratégicamente
 * - Disparar a enemigos cercanos
 */
public class PlayerAI {
    private static final String TAG = "PlayerAI";
    private static final Random random = new Random();

    // Parámetros de comportamiento
    private static final float MOVE_SPEED = 2.5f;
    private static final float EVASION_RADIUS = 2.0f;       // Distancia para esquivar proyectiles
    private static final float EVASION_STRENGTH = 4.0f;
    private static final float PREFERRED_Y = 1.5f;          // Posición Y preferida (abajo de la pantalla)
    private static final float CENTER_PULL = 1.0f;          // Fuerza para volver al centro X

    /**
     * Actualiza el comportamiento del jugador
     *
     * @param player Nave del jugador
     * @param enemies Lista de enemigos
     * @param projectiles Lista de proyectiles (para esquivar)
     * @param deltaTime Tiempo delta
     */
    public static void updateBehavior(Spaceship player,
                                      List<Spaceship> enemies,
                                      List<Projectile> projectiles,
                                      float deltaTime) {
        if (player.isDead) {
            player.vx = 0;
            player.vy = 0;
            return;
        }

        float evadeX = 0;
        float evadeY = 0;

        // 1. Esquivar proyectiles enemigos cercanos
        for (Projectile projectile : projectiles) {
            if (!projectile.active || projectile.isPlayerProjectile) continue;

            float dx = projectile.x - player.x;
            float dy = projectile.y - player.y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance < EVASION_RADIUS) {
                // Evadir perpendicularmente a la dirección del proyectil
                float dangerFactor = 1.0f - (distance / EVASION_RADIUS);
                evadeX -= dx * dangerFactor;
                evadeY -= dy * dangerFactor;
            }
        }

        // 2. Mantener posición estratégica (parte inferior de la pantalla)
        float targetY = PREFERRED_Y;
        float targetX = 0; // Centro

        // Si hay enemigos, posicionarse para tener mejor ángulo
        if (!enemies.isEmpty()) {
            // Calcular centro de masa de los enemigos
            float avgEnemyX = 0;
            int activeEnemies = 0;
            for (Spaceship enemy : enemies) {
                if (!enemy.isDead) {
                    avgEnemyX += enemy.x;
                    activeEnemies++;
                }
            }
            if (activeEnemies > 0) {
                avgEnemyX /= activeEnemies;
                // Alinearse con el promedio de enemigos (pero con límites)
                targetX = avgEnemyX * 0.5f; // 50% hacia el centro de enemigos
            }
        }

        // 3. Calcular velocidad deseada
        float desiredVx = 0;
        float desiredVy = 0;

        // Evasión (prioridad máxima)
        if (evadeX != 0 || evadeY != 0) {
            desiredVx = evadeX * EVASION_STRENGTH;
            desiredVy = evadeY * EVASION_STRENGTH;
        } else {
            // Movimiento hacia posición objetivo
            float dx = targetX - player.x;
            float dy = targetY - player.y;

            desiredVx = dx * CENTER_PULL;
            desiredVy = dy * CENTER_PULL;

            // Agregar un poco de movimiento lateral aleatorio (menos predecible)
            desiredVx += (random.nextFloat() - 0.5f) * 0.5f;
        }

        // Aplicar velocidad suavemente (no cambios bruscos)
        player.vx = desiredVx * MOVE_SPEED;
        player.vy = desiredVy * MOVE_SPEED;
    }

    /**
     * Decide si el jugador debe disparar
     *
     * @param player Nave del jugador
     * @param enemies Lista de enemigos
     * @return true si debe disparar
     */
    public static boolean shouldFire(Spaceship player, List<Spaceship> enemies) {
        if (player.isDead) return false;
        if (!player.canFire()) return false;

        // Buscar enemigos en frente
        for (Spaceship enemy : enemies) {
            if (enemy.isDead) continue;

            // Verificar si el enemigo está aproximadamente en frente
            float dx = Math.abs(enemy.x - player.x);
            float dy = enemy.y - player.y;

            // Disparar si hay un enemigo arriba y no muy desalineado
            if (dy < 0 && dx < 1.5f) {
                return true;
            }
        }

        // Si no hay nadie directamente en frente, disparar ocasionalmente
        return random.nextFloat() < 0.1f; // 10% de probabilidad
    }

    /**
     * Encuentra el enemigo más cercano
     *
     * @param player Nave del jugador
     * @param enemies Lista de enemigos
     * @return Enemigo más cercano o null
     */
    public static Spaceship findNearestEnemy(Spaceship player, List<Spaceship> enemies) {
        Spaceship nearest = null;
        float minDistance = Float.MAX_VALUE;

        for (Spaceship enemy : enemies) {
            if (enemy.isDead) continue;

            float dx = enemy.x - player.x;
            float dy = enemy.y - player.y;
            float distance = dx * dx + dy * dy; // Sin sqrt para optimización

            if (distance < minDistance) {
                minDistance = distance;
                nearest = enemy;
            }
        }

        return nearest;
    }
}
