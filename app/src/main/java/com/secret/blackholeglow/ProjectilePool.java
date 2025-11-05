// ProjectilePool.java - Pool de proyectiles para optimización
package com.secret.blackholeglow;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Pool de proyectiles para evitar crear/destruir objetos constantemente.
 * Mejora el rendimiento al reutilizar proyectiles inactivos.
 */
public class ProjectilePool {
    private static final String TAG = "ProjectilePool";

    private final List<Projectile> pool;
    private final Context context;
    private final TextureLoader textureLoader;
    private final int playerTextureId;
    private final int enemyTextureId;
    private final int maxSize;

    /**
     * Constructor
     *
     * @param context Context de Android
     * @param textureLoader Cargador de texturas
     * @param playerTextureId ID de textura para proyectiles del jugador
     * @param enemyTextureId ID de textura para proyectiles enemigos
     * @param maxSize Tamaño máximo del pool
     */
    public ProjectilePool(Context context,
                          TextureLoader textureLoader,
                          int playerTextureId,
                          int enemyTextureId,
                          int maxSize) {
        this.context = context;
        this.textureLoader = textureLoader;
        this.playerTextureId = playerTextureId;
        this.enemyTextureId = enemyTextureId;
        this.maxSize = maxSize;
        this.pool = new ArrayList<>(maxSize);

        Log.d(TAG, "ProjectilePool created with max size: " + maxSize);
    }

    /**
     * Obtiene un proyectil del pool (o crea uno nuevo si es necesario)
     */
    public Projectile obtain(boolean isPlayerProjectile) {
        // Buscar un proyectil inactivo en el pool
        for (Projectile projectile : pool) {
            if (!projectile.active) {
                return projectile;
            }
        }

        // Si no hay disponibles y no hemos alcanzado el máximo, crear uno nuevo
        if (pool.size() < maxSize) {
            int textureId = isPlayerProjectile ? playerTextureId : enemyTextureId;
            Projectile newProjectile = new Projectile(context, textureLoader, textureId);
            pool.add(newProjectile);
            Log.d(TAG, "Created new projectile. Pool size: " + pool.size());
            return newProjectile;
        }

        // Si el pool está lleno, reutilizar el más antiguo (opcional)
        // Por ahora, retornamos null para no crear más
        Log.w(TAG, "ProjectilePool is full! Cannot create more projectiles.");
        return null;
    }

    /**
     * Obtiene un proyectil y lo activa con los parámetros dados
     */
    public Projectile spawn(float x, float y, float z,
                           float dirX, float dirY, float dirZ,
                           boolean isPlayerProjectile) {
        Projectile projectile = obtain(isPlayerProjectile);
        if (projectile != null) {
            projectile.activate(x, y, z, dirX, dirY, dirZ, isPlayerProjectile);
        }
        return projectile;
    }

    /**
     * Obtiene todos los proyectiles del pool (activos e inactivos)
     */
    public List<Projectile> getAllProjectiles() {
        return pool;
    }

    /**
     * Obtiene solo los proyectiles activos
     */
    public List<Projectile> getActiveProjectiles() {
        List<Projectile> active = new ArrayList<>();
        for (Projectile projectile : pool) {
            if (projectile.active) {
                active.add(projectile);
            }
        }
        return active;
    }

    /**
     * Actualiza todos los proyectiles activos
     */
    public void updateAll(float deltaTime) {
        for (Projectile projectile : pool) {
            if (projectile.active) {
                projectile.update(deltaTime);
            }
        }
    }

    /**
     * Dibuja todos los proyectiles activos
     */
    public void drawAll() {
        for (Projectile projectile : pool) {
            if (projectile.active) {
                projectile.draw();
            }
        }
    }

    /**
     * Asigna la cámara a todos los proyectiles
     */
    public void setCameraForAll(CameraController camera) {
        for (Projectile projectile : pool) {
            projectile.setCameraController(camera);
        }
    }

    /**
     * Desactiva todos los proyectiles (limpia la escena)
     */
    public void clear() {
        for (Projectile projectile : pool) {
            projectile.deactivate();
        }
        Log.d(TAG, "All projectiles cleared");
    }

    /**
     * Obtiene estadísticas del pool
     */
    public String getStats() {
        int active = 0;
        for (Projectile projectile : pool) {
            if (projectile.active) active++;
        }
        return "Projectiles: " + active + "/" + pool.size() + " (max: " + maxSize + ")";
    }
}
