package com.secret.blackholeglow.scenes;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   🎮 SceneCallbacks - Interfaz para eventos de escena            ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * Define callbacks que las escenas pueden usar para comunicarse con
 * WallpaperDirector (efectos visuales, impactos, etc.)
 *
 * Esto permite que las escenas modulares tengan lógica de juego
 * sin estar acopladas directamente al renderer.
 */
public interface SceneCallbacks {

    /**
     * 💥 Llamado cuando ocurre una explosión en la escena
     * @param x Posición X de la explosión
     * @param y Posición Y de la explosión
     * @param intensity Intensidad de la explosión (0.0 - 1.0)
     */
    void onExplosion(float x, float y, float intensity);

    /**
     * 💥 Llamado cuando un meteorito impacta la pantalla
     * @param screenX Posición X en pantalla (0.0 - 1.0)
     * @param screenY Posición Y en pantalla (0.0 - 1.0)
     */
    void onScreenImpact(float screenX, float screenY);

    /**
     * 🎵 Obtiene los niveles de audio actuales
     * @return Array con [bass, mid, treble, volume, beatIntensity]
     */
    float[] getMusicLevels();

    /**
     * 📊 Obtiene las estadísticas del jugador
     * @return Objeto PlayerStats o null si no está disponible
     */
    Object getPlayerStats();

    /**
     * 🏆 Actualiza el leaderboard
     */
    void updateLeaderboard();

    /**
     * 🎵 Llamado cuando se recibe una canción compartida
     * @param userName Nombre del usuario que comparte
     * @param songTitle Título de la canción
     */
    void onSongShared(String userName, String songTitle);
}
