// CollisionSystem.java - Sistema de colisiones para batalla espacial
package com.secret.blackholeglow;

import android.util.Log;

import java.util.List;

/**
 * Sistema de colisiones optimizado usando detección circular.
 * Maneja colisiones entre proyectiles y naves.
 */
public class CollisionSystem {
    private static final String TAG = "CollisionSystem";

    /**
     * Verifica colisiones entre proyectiles y una nave.
     * Los proyectiles que colisionan se desactivan.
     *
     * @param projectiles Lista de proyectiles a verificar
     * @param ship Nave objetivo
     * @param checkPlayerProjectiles true = verificar proyectiles del jugador, false = enemigos
     * @return Número de colisiones detectadas
     */
    public static int checkProjectileShipCollisions(List<Projectile> projectiles,
                                                     Spaceship ship,
                                                     boolean checkPlayerProjectiles) {
        if (ship.isDead) return 0;

        int collisionCount = 0;

        for (Projectile projectile : projectiles) {
            if (!projectile.active) continue;

            // Solo verificar proyectiles del tipo correcto
            if (projectile.isPlayerProjectile != checkPlayerProjectiles) continue;

            // Detección de colisión circular optimizada (sin sqrt)
            float dx = projectile.x - ship.x;
            float dy = projectile.y - ship.y;
            float dz = projectile.z - ship.z;

            float distanceSquared = dx * dx + dy * dy + dz * dz;
            float minDistance = projectile.collisionRadius + ship.collisionRadius;
            float minDistanceSquared = minDistance * minDistance;

            if (distanceSquared < minDistanceSquared) {
                // ¡Colisión detectada!
                ship.takeDamage(projectile.damage);
                projectile.deactivate();
                collisionCount++;

                Log.d(TAG, "Collision! Ship HP: " + ship.currentHealth + "/" + ship.maxHealth);
            }
        }

        return collisionCount;
    }

    /**
     * Verifica colisiones entre proyectiles del jugador y múltiples enemigos
     *
     * @param projectiles Pool de proyectiles
     * @param enemies Lista de naves enemigas
     * @return Total de colisiones
     */
    public static int checkPlayerProjectilesVsEnemies(List<Projectile> projectiles,
                                                       List<Spaceship> enemies) {
        int totalCollisions = 0;

        for (Spaceship enemy : enemies) {
            if (enemy.isDead) continue;
            totalCollisions += checkProjectileShipCollisions(projectiles, enemy, true);
        }

        return totalCollisions;
    }

    /**
     * Verifica colisiones entre proyectiles enemigos y el jugador
     *
     * @param projectiles Pool de proyectiles
     * @param player Nave del jugador
     * @return Total de colisiones
     */
    public static int checkEnemyProjectilesVsPlayer(List<Projectile> projectiles,
                                                     Spaceship player) {
        return checkProjectileShipCollisions(projectiles, player, false);
    }

    /**
     * Verifica colisión entre dos naves (opcional, para choques directos)
     *
     * @param ship1 Primera nave
     * @param ship2 Segunda nave
     * @return true si hay colisión
     */
    public static boolean checkShipShipCollision(Spaceship ship1, Spaceship ship2) {
        if (ship1.isDead || ship2.isDead) return false;

        float dx = ship1.x - ship2.x;
        float dy = ship1.y - ship2.y;
        float dz = ship1.z - ship2.z;

        float distanceSquared = dx * dx + dy * dy + dz * dz;
        float minDistance = ship1.collisionRadius + ship2.collisionRadius;
        float minDistanceSquared = minDistance * minDistance;

        return distanceSquared < minDistanceSquared;
    }

    /**
     * Calcula la distancia al cuadrado entre dos puntos (sin sqrt - más rápido)
     */
    public static float distanceSquared(float x1, float y1, float z1,
                                       float x2, float y2, float z2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Calcula la distancia real entre dos puntos
     */
    public static float distance(float x1, float y1, float z1,
                                float x2, float y2, float z2) {
        return (float) Math.sqrt(distanceSquared(x1, y1, z1, x2, y2, z2));
    }
}
