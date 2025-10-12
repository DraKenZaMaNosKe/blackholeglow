package com.secret.blackholeglow;

/**
 * MusicReactive - Interface para objetos que reaccionan a la música
 * Los objetos que implementen esta interface recibirán datos de audio en tiempo real
 * y podrán ajustar sus propiedades visuales (escala, velocidad, color, etc.)
 */
public interface MusicReactive {

    /**
     * Actualiza el objeto con datos de audio actuales
     *
     * @param bassLevel    Nivel de bajos (0.0 - 1.0)
     * @param midLevel     Nivel de medios (0.0 - 1.0)
     * @param trebleLevel  Nivel de agudos (0.0 - 1.0)
     * @param volumeLevel  Volumen general (0.0 - 1.0)
     * @param beatIntensity Intensidad del beat actual (0.0 - 1.0)
     * @param isBeat       ¿Hay un beat en este frame?
     */
    void onMusicData(float bassLevel, float midLevel, float trebleLevel,
                     float volumeLevel, float beatIntensity, boolean isBeat);

    /**
     * Habilita o deshabilita la reactividad musical
     */
    void setMusicReactive(boolean enabled);

    /**
     * Verifica si el objeto está en modo reactivo
     */
    boolean isMusicReactive();
}
