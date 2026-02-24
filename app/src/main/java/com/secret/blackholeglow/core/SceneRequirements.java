package com.secret.blackholeglow.core;

import android.content.Context;

import com.secret.blackholeglow.TextureManager.MemoryTier;
import com.secret.blackholeglow.models.SceneWeight;

/**
 * Evaluador de estado por escena.
 * Determina si un wallpaper puede correr en el dispositivo actual
 * y si sus recursos estan descargados.
 */
public class SceneRequirements {

    public enum SceneStatus {
        READY,              // Compatible + recursos descargados
        NEEDS_DOWNLOAD,     // Compatible pero faltan recursos
        NOT_RECOMMENDED     // No recomendado para este device
    }

    /**
     * Evalua el estado de una escena para el dispositivo actual.
     *
     * @param context   Contexto Android
     * @param sceneName Nombre de la escena (ej: "GOKU")
     * @param weight    Peso de la escena (LIGHT/MEDIUM/HEAVY)
     * @return SceneStatus indicando estado de la escena
     */
    public static SceneStatus evaluate(Context context, String sceneName, SceneWeight weight) {
        // 1. Verificar compatibilidad con el device
        DeviceProfile.init(context);
        MemoryTier tier = DeviceProfile.get().getMemoryTier();

        if (isNotRecommended(tier, weight)) {
            return SceneStatus.NOT_RECOMMENDED;
        }

        // 2. Verificar si los recursos estan descargados
        if (!ResourcePreloader.areSceneResourcesReady(context, sceneName)) {
            return SceneStatus.NEEDS_DOWNLOAD;
        }

        return SceneStatus.READY;
    }

    /**
     * Misma logica que DiagnosticData.calculateCompat() pero simplificada
     * para uso binario (recomendado o no).
     *
     * Solo marca NOT_RECOMMENDED: LOW + HEAVY
     */
    private static boolean isNotRecommended(MemoryTier tier, SceneWeight weight) {
        return tier == MemoryTier.LOW && weight == SceneWeight.HEAVY;
    }
}
