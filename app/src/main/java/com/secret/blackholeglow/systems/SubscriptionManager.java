package com.secret.blackholeglow.systems;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.secret.blackholeglow.models.WallpaperTier;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                    SubscriptionManager                            ║
 * ║               "El Guardián de Suscripciones"                      ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  Actor especializado en gestión de suscripciones de usuarios.     ║
 * ║                                                                   ║
 * ║  RESPONSABILIDADES:                                               ║
 * ║  • Almacenar y verificar nivel de suscripción del usuario         ║
 * ║  • Validar acceso a wallpapers según tier                         ║
 * ║  • Preparado para integración con Google Play Billing             ║
 * ║                                                                   ║
 * ║  NIVELES DE USUARIO:                                              ║
 * ║  • 0 = FREE     → Acceso a wallpapers gratuitos                   ║
 * ║  • 1 = PREMIUM  → Acceso a FREE + PREMIUM + BETA                  ║
 * ║  • 2 = VIP      → Acceso a TODO                                   ║
 * ║                                                                   ║
 * ║  PRINCIPIOS:                                                      ║
 * ║  • Máximo 10 métodos públicos                                     ║
 * ║  • Un solo propósito: gestión de suscripciones                    ║
 * ║  • Singleton para acceso global                                   ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * USO:
 *   // Verificar si es premium
 *   boolean isPremium = SubscriptionManager.get().isPremium();
 *
 *   // Verificar acceso a un tier
 *   boolean canAccess = SubscriptionManager.get().canAccess(WallpaperTier.PREMIUM);
 *
 *   // Obtener nivel del usuario
 *   int level = SubscriptionManager.get().getUserLevel();
 */
public class SubscriptionManager {
    private static final String TAG = "SubscriptionManager";
    private static final String PREFS_NAME = "subscription_prefs";
    private static final String KEY_USER_LEVEL = "user_level";
    private static final String KEY_EXPIRATION = "subscription_expiration";

    // ═══════════════════════════════════════════════════════════════
    // CONSTANTES DE NIVELES
    // ═══════════════════════════════════════════════════════════════

    public static final int LEVEL_FREE = 0;
    public static final int LEVEL_PREMIUM = 1;
    public static final int LEVEL_VIP = 2;

    // ═══════════════════════════════════════════════════════════════
    // SINGLETON
    // ═══════════════════════════════════════════════════════════════

    private static SubscriptionManager instance;
    private SharedPreferences prefs;
    private int cachedUserLevel = LEVEL_FREE;

    public static SubscriptionManager get() {
        if (instance == null) {
            instance = new SubscriptionManager();
        }
        return instance;
    }

    /**
     * Inicializa el manager con contexto (llamar desde Application o Activity)
     * @param context Contexto de la aplicación
     */
    public static void init(Context context) {
        if (instance == null) {
            instance = new SubscriptionManager();
        }
        instance.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        instance.loadUserLevel();
        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   💳 SubscriptionManager Inicializado  ║");
        Log.d(TAG, "║   Nivel: " + instance.getLevelName() + "                    ║");
        Log.d(TAG, "╚════════════════════════════════════════╝");
    }

    private SubscriptionManager() {
        // Constructor privado para singleton
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. OBTENER NIVEL DEL USUARIO
    // ═══════════════════════════════════════════════════════════════

    /**
     * Obtiene el nivel de suscripción del usuario
     * @return 0=FREE, 1=PREMIUM, 2=VIP
     */
    public int getUserLevel() {
        return cachedUserLevel;
    }

    /**
     * Obtiene el nombre del nivel actual
     */
    public String getLevelName() {
        switch (cachedUserLevel) {
            case LEVEL_VIP: return "VIP";
            case LEVEL_PREMIUM: return "PREMIUM";
            default: return "FREE";
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. VERIFICACIONES DE NIVEL
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verifica si el usuario es FREE (nivel 0)
     */
    public boolean isFree() {
        return cachedUserLevel == LEVEL_FREE;
    }

    /**
     * Verifica si el usuario es PREMIUM o superior
     */
    public boolean isPremium() {
        return cachedUserLevel >= LEVEL_PREMIUM;
    }

    /**
     * Verifica si el usuario es VIP
     */
    public boolean isVIP() {
        return cachedUserLevel >= LEVEL_VIP;
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. VERIFICAR ACCESO A TIERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verifica si el usuario puede acceder a un tier específico
     * @param tier El tier a verificar
     * @return true si puede acceder
     */
    public boolean canAccess(WallpaperTier tier) {
        return tier.isAccessibleBy(cachedUserLevel);
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. ACTUALIZAR SUSCRIPCIÓN
    // ═══════════════════════════════════════════════════════════════

    /**
     * Actualiza el nivel de suscripción del usuario
     * NOTA: En producción, esto debe ser llamado desde Google Play Billing
     *
     * @param newLevel Nuevo nivel (0=FREE, 1=PREMIUM, 2=VIP)
     */
    public void setUserLevel(int newLevel) {
        if (newLevel < LEVEL_FREE || newLevel > LEVEL_VIP) {
            Log.w(TAG, "Nivel inválido: " + newLevel);
            return;
        }

        cachedUserLevel = newLevel;
        saveUserLevel();

        // Publicar evento de cambio de suscripción
        EventBus.get().publish("subscription_changed",
                new EventBus.EventData()
                        .put("old_level", cachedUserLevel)
                        .put("new_level", newLevel));

        Log.d(TAG, "💳 Nivel actualizado: " + getLevelName());
    }

    /**
     * Actualiza a PREMIUM
     * NOTA: En producción, llamar después de verificar compra con Google Play
     */
    public void upgradeToPremium() {
        setUserLevel(LEVEL_PREMIUM);
    }

    /**
     * Actualiza a VIP
     * NOTA: En producción, llamar después de verificar compra con Google Play
     */
    public void upgradeToVIP() {
        setUserLevel(LEVEL_VIP);
    }

    /**
     * Degrada a FREE (cuando expira suscripción)
     */
    public void downgradeToFree() {
        setUserLevel(LEVEL_FREE);
    }

    // ═══════════════════════════════════════════════════════════════
    // 5. PERSISTENCIA
    // ═══════════════════════════════════════════════════════════════

    private void loadUserLevel() {
        if (prefs != null) {
            cachedUserLevel = prefs.getInt(KEY_USER_LEVEL, LEVEL_FREE);

            // Verificar expiración
            long expiration = prefs.getLong(KEY_EXPIRATION, 0);
            if (expiration > 0 && System.currentTimeMillis() > expiration) {
                // Suscripción expirada
                Log.d(TAG, "⚠️ Suscripción expirada, degradando a FREE");
                downgradeToFree();
            }
        }
    }

    private void saveUserLevel() {
        if (prefs != null) {
            prefs.edit()
                    .putInt(KEY_USER_LEVEL, cachedUserLevel)
                    .apply();
        }
    }

    /**
     * Establece fecha de expiración de la suscripción
     * @param expirationTimestamp Timestamp de expiración (0 = nunca expira)
     */
    public void setExpiration(long expirationTimestamp) {
        if (prefs != null) {
            prefs.edit()
                    .putLong(KEY_EXPIRATION, expirationTimestamp)
                    .apply();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 6. RESET
    // ═══════════════════════════════════════════════════════════════

    /**
     * Reset del singleton (para testing o logout)
     */
    public static void reset() {
        if (instance != null) {
            instance.cachedUserLevel = LEVEL_FREE;
            instance.prefs = null;
        }
        instance = null;
        Log.d(TAG, "SubscriptionManager reset");
    }

    // ═══════════════════════════════════════════════════════════════
    // INTEGRACIÓN FUTURA: GOOGLE PLAY BILLING
    // ═══════════════════════════════════════════════════════════════

    /**
     * TODO: Implementar cuando se agregue Google Play Billing
     *
     * Métodos a agregar:
     * - initBillingClient(Context)
     * - querySubscriptions()
     * - launchPurchaseFlow(Activity, String sku)
     * - handlePurchaseResult(Purchase)
     * - verifyPurchase(Purchase) // con servidor backend
     */
}
