package com.secret.blackholeglow.systems;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.secret.blackholeglow.BuildConfig;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                         AdsManager                                ║
 * ║                    "El Monetizador"                               ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  Actor especializado en gestión de anuncios con Google AdMob.     ║
 * ║                                                                   ║
 * ║  RESPONSABILIDADES:                                               ║
 * ║  • Inicializar el SDK de AdMob                                    ║
 * ║  • Cargar y mostrar anuncios intersticiales                       ║
 * ║  • Cargar y mostrar anuncios recompensados (rewarded)             ║
 * ║  • Dar puntos al usuario por ver anuncios                         ║
 * ║                                                                   ║
 * ║  TIPOS DE ANUNCIOS:                                               ║
 * ║  • Interstitial: Pantalla completa al establecer wallpaper        ║
 * ║  • Rewarded: Video opcional para ganar puntos extra               ║
 * ║                                                                   ║
 * ║  PRINCIPIOS:                                                      ║
 * ║  • Máximo 10 métodos públicos                                     ║
 * ║  • Un solo propósito: gestión de anuncios                         ║
 * ║  • Singleton para acceso global                                   ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * CONFIGURACIÓN EN ADMOB:
 * 1. Crear cuenta en admob.google.com
 * 2. Registrar la app (com.secret.blackholeglow)
 * 3. Crear unidades de anuncio:
 *    - Interstitial para "establecer wallpaper"
 *    - Rewarded para "ganar puntos extra"
 * 4. Copiar los IDs de unidad y reemplazar los de prueba
 */
public class AdsManager {
    private static final String TAG = "AdsManager";

    // ═══════════════════════════════════════════════════════════════
    // IDS DE ANUNCIOS
    // ═══════════════════════════════════════════════════════════════

    // 🧪 IDs DE PRUEBA (usar durante desarrollo)
    private static final String TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712";
    private static final String TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917";

    // 🚀 IDs DE PRODUCCIÓN - Black Hole Glow (AdMob)
    private static final String PROD_INTERSTITIAL_ID = "ca-app-pub-6734758230109098/1797212684";
    private static final String PROD_REWARDED_ID = "ca-app-pub-6734758230109098/9484131013";

    // IDs activos - USAR TEST mientras la cuenta no esté aprobada
    // TODO: Cambiar a PROD_ cuando AdMob apruebe la cuenta
    private String interstitialAdUnitId = TEST_INTERSTITIAL_ID;
    private String rewardedAdUnitId = TEST_REWARDED_ID;

    // ═══════════════════════════════════════════════════════════════
    // SINGLETON
    // ═══════════════════════════════════════════════════════════════

    private static AdsManager instance;
    private boolean isInitialized = false;

    // Anuncios cargados
    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;

    // Callbacks
    private AdCallback currentCallback;

    public static AdsManager get() {
        if (instance == null) {
            instance = new AdsManager();
        }
        return instance;
    }

    /**
     * Inicializa el SDK de AdMob
     * Llamar desde Application o MainActivity
     */
    public static void init(Context context) {
        if (instance == null) {
            instance = new AdsManager();
        }

        if (instance.isInitialized) {
            Log.d(TAG, "AdMob ya inicializado");
            return;
        }

        MobileAds.initialize(context, initializationStatus -> {
            instance.isInitialized = true;
            Log.d(TAG, "╔════════════════════════════════════════╗");
            Log.d(TAG, "║   💰 AdsManager Inicializado (AdMob)   ║");
            Log.d(TAG, "╚════════════════════════════════════════╝");

            // Pre-cargar anuncios
            instance.loadInterstitialAd(context);
            instance.loadRewardedAd(context);
        });
    }

    private AdsManager() {
        // Constructor privado para singleton
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. INTERSTITIAL ADS (Pantalla completa)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Carga un anuncio intersticial
     */
    public void loadInterstitialAd(Context context) {
        if (!isInitialized) {
            Log.w(TAG, "AdMob no inicializado");
            return;
        }

        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(context, interstitialAdUnitId, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialAd = ad;
                        setupInterstitialCallbacks();
                        Log.d(TAG, "📺 Interstitial cargado");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        interstitialAd = null;
                        Log.e(TAG, "❌ Error cargando interstitial: " + loadAdError.getMessage());
                    }
                });
    }

    /**
     * Muestra el anuncio intersticial
     * @param activity Activity desde donde mostrar
     * @param callback Callback para saber cuando termina
     * @return true si se mostró, false si no estaba listo
     */
    public boolean showInterstitialAd(Activity activity, AdCallback callback) {
        // 🧪 DEBUG: Saltar ads durante desarrollo
        if (!BuildConfig.ADS_ENABLED) {
            Log.d(TAG, "🧪 DEBUG: Ads deshabilitados en desarrollo");
            if (callback != null) callback.onAdCompleted(true); // Simular que se mostró
            return true;
        }

        // Verificar si RemoteConfig dice que debemos mostrar ads
        if (!RemoteConfigManager.get().shouldShowAdsOnSet()) {
            Log.d(TAG, "Ads deshabilitados desde RemoteConfig");
            if (callback != null) callback.onAdCompleted(false);
            return false;
        }

        if (interstitialAd == null) {
            Log.w(TAG, "Interstitial no disponible");
            if (callback != null) callback.onAdCompleted(false);
            // Intentar cargar para la próxima vez
            loadInterstitialAd(activity);
            return false;
        }

        this.currentCallback = callback;
        interstitialAd.show(activity);
        return true;
    }

    /**
     * Verifica si hay un interstitial listo
     */
    public boolean isInterstitialReady() {
        return interstitialAd != null;
    }

    private void setupInterstitialCallbacks() {
        if (interstitialAd == null) return;

        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "📺 Interstitial cerrado");
                interstitialAd = null;

                if (currentCallback != null) {
                    currentCallback.onAdCompleted(true);
                    currentCallback = null;
                }

                // Publicar evento
                EventBus.get().publish("ad_completed",
                        new EventBus.EventData()
                                .put("ad_type", "interstitial")
                                .put("completed", true));
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "❌ Error mostrando interstitial: " + adError.getMessage());
                interstitialAd = null;

                if (currentCallback != null) {
                    currentCallback.onAdCompleted(false);
                    currentCallback = null;
                }
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "📺 Interstitial mostrado");
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. REWARDED ADS (Video con recompensa)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Carga un anuncio recompensado
     */
    public void loadRewardedAd(Context context) {
        if (!isInitialized) {
            Log.w(TAG, "AdMob no inicializado");
            return;
        }

        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(context, rewardedAdUnitId, adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        setupRewardedCallbacks();
                        Log.d(TAG, "🎁 Rewarded cargado");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        rewardedAd = null;
                        Log.e(TAG, "❌ Error cargando rewarded: " + loadAdError.getMessage());
                    }
                });
    }

    /**
     * Muestra el anuncio recompensado
     * @param activity Activity desde donde mostrar
     * @param callback Callback para saber cuando termina y si ganó recompensa
     * @return true si se mostró, false si no estaba listo
     */
    public boolean showRewardedAd(Activity activity, RewardCallback callback) {
        // 🧪 DEBUG: Dar recompensa sin mostrar ad
        if (!BuildConfig.ADS_ENABLED) {
            Log.d(TAG, "🧪 DEBUG: Dando recompensa sin ad");
            int rewardPoints = RemoteConfigManager.get().getAdRewardPoints();
            RewardsManager.get().addPoints(rewardPoints);
            if (callback != null) callback.onRewardEarned(true, rewardPoints);
            return true;
        }

        if (rewardedAd == null) {
            Log.w(TAG, "Rewarded no disponible");
            if (callback != null) callback.onRewardEarned(false, 0);
            // Intentar cargar para la próxima vez
            loadRewardedAd(activity);
            return false;
        }

        rewardedAd.show(activity, rewardItem -> {
            // Usuario ganó la recompensa
            int rewardPoints = RemoteConfigManager.get().getAdRewardPoints();
            Log.d(TAG, "🎁 Usuario ganó recompensa: " + rewardPoints + " puntos");

            // Dar puntos al usuario
            RewardsManager.get().addPoints(rewardPoints);

            if (callback != null) {
                callback.onRewardEarned(true, rewardPoints);
            }

            // Publicar evento
            EventBus.get().publish("reward_earned",
                    new EventBus.EventData()
                            .put("points", rewardPoints));
        });

        return true;
    }

    /**
     * Verifica si hay un rewarded listo
     */
    public boolean isRewardedReady() {
        return rewardedAd != null;
    }

    private void setupRewardedCallbacks() {
        if (rewardedAd == null) return;

        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "🎁 Rewarded cerrado");
                rewardedAd = null;

                // Publicar evento
                EventBus.get().publish("ad_completed",
                        new EventBus.EventData()
                                .put("ad_type", "rewarded")
                                .put("completed", true));
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "❌ Error mostrando rewarded: " + adError.getMessage());
                rewardedAd = null;
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "🎁 Rewarded mostrado");
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════

    /**
     * Configura IDs de producción (llamar después de obtener de RemoteConfig o hardcoded)
     */
    public void setProductionIds(String interstitialId, String rewardedId) {
        this.interstitialAdUnitId = interstitialId;
        this.rewardedAdUnitId = rewardedId;
        Log.d(TAG, "IDs de producción configurados");
    }

    /**
     * Usa IDs de prueba (para desarrollo)
     */
    public void useTestIds() {
        this.interstitialAdUnitId = TEST_INTERSTITIAL_ID;
        this.rewardedAdUnitId = TEST_REWARDED_ID;
        Log.d(TAG, "Usando IDs de prueba");
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. ESTADO
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verifica si AdMob está inicializado
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Pre-carga todos los anuncios
     */
    public void preloadAds(Context context) {
        loadInterstitialAd(context);
        loadRewardedAd(context);
    }

    // ═══════════════════════════════════════════════════════════════
    // 5. RESET
    // ═══════════════════════════════════════════════════════════════

    /**
     * Reset del singleton
     */
    public static void reset() {
        if (instance != null) {
            instance.interstitialAd = null;
            instance.rewardedAd = null;
            instance.isInitialized = false;
        }
        instance = null;
        Log.d(TAG, "AdsManager reset");
    }

    // ═══════════════════════════════════════════════════════════════
    // CALLBACKS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Callback para anuncios intersticiales
     */
    public interface AdCallback {
        void onAdCompleted(boolean shown);
    }

    /**
     * Callback para anuncios recompensados
     */
    public interface RewardCallback {
        void onRewardEarned(boolean earned, int points);
    }
}
