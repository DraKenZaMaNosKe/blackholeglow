// ============================================
// WallpaperPreferences.java
// Sistema centralizado de configuración de wallpapers
// Sincroniza entre Firebase Firestore y SharedPreferences
// ============================================

package com.secret.blackholeglow;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Gestor centralizado de preferencias de wallpaper.
 *
 * ARQUITECTURA:
 * - Fuente primaria: Firebase Firestore (sincronizado en la nube)
 * - Fuente secundaria: SharedPreferences (cache local + fallback sin internet)
 * - Validación: Solo permite wallpapers válidos de la lista conocida
 * - Thread-safe: Sincronización adecuada para evitar race conditions
 *
 * FLUJO:
 * 1. Usuario selecciona wallpaper → setSelectedWallpaper()
 * 2. Se guarda en Firebase (si hay conexión) + SharedPreferences (siempre)
 * 3. Wallpaper lee con getSelectedWallpaper() → Firebase primero, SharedPreferences como fallback
 */
public class WallpaperPreferences {
    private static final String TAG = "WallpaperPrefs";

    // ============================================
    // CONFIGURACIÓN
    // ============================================
    private static final String PREFS_NAME = "blackholeglow_prefs";
    private static final String KEY_SELECTED_WALLPAPER = "selected_wallpaper";
    private static final String DEFAULT_WALLPAPER = "Batalla Cósmica";

    // Colección de Firebase Firestore
    private static final String FIREBASE_COLLECTION = "user_preferences";
    private static final String FIREBASE_FIELD_WALLPAPER = "selected_wallpaper";
    private static final String FIREBASE_FIELD_UPDATED_AT = "updated_at";

    // ============================================
    // LISTA DE WALLPAPERS VÁLIDOS (3 principales + legados)
    // ============================================
    public static final Set<String> VALID_WALLPAPERS = new HashSet<>(Arrays.asList(
        // ═══════════════════════════════════════════
        // 🎨 WALLPAPERS PRINCIPALES (v4.0)
        // ═══════════════════════════════════════════
        "Batalla Cósmica",    // Batalla espacial (modular)
        "Ocean Pearl",        // Fondo del mar con ostra y perla (modular)

        // ═══════════════════════════════════════════
        // 📦 WALLPAPERS LEGADOS (compatibilidad)
        // ═══════════════════════════════════════════
        "🌊 Océano Profundo",
        "🌲 Bosque Encantado",
        "🏙️ Neo Tokyo 2099",
        "🏖️ Paraíso Dorado",
        "🦁 Safari Salvaje",
        "🌧️ Lluvia Mística",
        "🎮 Pixel Quest",
        "🕳️ Portal Infinito",
        "Agujero Negro",
        "🌸 Jardín Zen",
        "⚡ Furia Celestial",
        "🚀 Batalla Galáctica",
        "DiscoBall"
    ));

    // ============================================
    // SINGLETON
    // ============================================
    private static WallpaperPreferences instance;
    private final Context context;
    private final SharedPreferences sharedPrefs;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;

    private WallpaperPreferences(Context context) {
        this.context = context.getApplicationContext();
        this.sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();

        Log.d(TAG, "✓ WallpaperPreferences inicializado");
    }

    public static synchronized WallpaperPreferences getInstance(Context context) {
        if (instance == null) {
            instance = new WallpaperPreferences(context);
        }
        return instance;
    }

    // ============================================
    // MÉTODOS PÚBLICOS
    // ============================================

    /**
     * Obtiene el wallpaper seleccionado actualmente.
     *
     * ORDEN DE PRIORIDAD:
     * 1. Firebase Firestore (si usuario está logueado)
     * 2. SharedPreferences (cache local)
     * 3. DEFAULT_WALLPAPER ("Universo")
     *
     * @param callback Callback que recibe el nombre del wallpaper (null-safe)
     */
    public void getSelectedWallpaper(@NonNull WallpaperCallback callback) {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            // Usuario no logueado → usar SharedPreferences
            String local = getLocalWallpaper();
            Log.d(TAG, "📱 Usuario no logueado, usando local: " + local);
            callback.onWallpaperReceived(local);
            return;
        }

        // Usuario logueado → intentar leer de Firebase primero
        String userId = user.getUid();
        DocumentReference docRef = firestore.collection(FIREBASE_COLLECTION).document(userId);

        docRef.get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists() && documentSnapshot.contains(FIREBASE_FIELD_WALLPAPER)) {
                    String firebaseWallpaper = documentSnapshot.getString(FIREBASE_FIELD_WALLPAPER);

                    if (isValidWallpaper(firebaseWallpaper)) {
                        Log.d(TAG, "☁️ Wallpaper desde Firebase: " + firebaseWallpaper);

                        // Sincronizar a local (cache)
                        saveLocalWallpaper(firebaseWallpaper);

                        callback.onWallpaperReceived(firebaseWallpaper);
                    } else {
                        Log.w(TAG, "⚠️ Wallpaper inválido en Firebase: " + firebaseWallpaper);
                        String local = getLocalWallpaper();
                        callback.onWallpaperReceived(local);
                    }
                } else {
                    // Firebase vacío → usar local
                    String local = getLocalWallpaper();
                    Log.d(TAG, "📱 Firebase vacío, usando local: " + local);
                    callback.onWallpaperReceived(local);
                }
            })
            .addOnFailureListener(e -> {
                // Error de Firebase → fallback a local
                Log.w(TAG, "⚠️ Error leyendo Firebase, usando local: " + e.getMessage());
                String local = getLocalWallpaper();
                callback.onWallpaperReceived(local);
            });
    }

    /**
     * Guarda el wallpaper seleccionado.
     *
     * FLUJO:
     * 1. Valida que el wallpaper sea válido
     * 2. Guarda en SharedPreferences (siempre, instantáneo)
     * 3. Guarda en Firebase (si usuario logueado, asíncrono)
     *
     * @param wallpaperName Nombre del wallpaper ("Universo", "DiscoBall", etc.)
     * @param callback Callback opcional para saber si se guardó exitosamente
     */
    public void setSelectedWallpaper(@NonNull String wallpaperName, @Nullable SaveCallback callback) {
        // VALIDACIÓN
        if (!isValidWallpaper(wallpaperName)) {
            Log.e(TAG, "✗ Wallpaper inválido: " + wallpaperName +
                       ". Válidos: " + VALID_WALLPAPERS);
            if (callback != null) {
                callback.onSaveComplete(false, "Wallpaper inválido: " + wallpaperName);
            }
            return;
        }

        Log.d(TAG, "💾 Guardando wallpaper: " + wallpaperName);

        // 1. Guardar en SharedPreferences (siempre, instantáneo)
        boolean localSuccess = saveLocalWallpaper(wallpaperName);

        if (!localSuccess) {
            Log.e(TAG, "✗ Error guardando en SharedPreferences");
            if (callback != null) {
                callback.onSaveComplete(false, "Error guardando localmente");
            }
            return;
        }

        // 2. Guardar en Firebase (si usuario logueado)
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Log.d(TAG, "📱 Usuario no logueado, solo guardado local");
            if (callback != null) {
                callback.onSaveComplete(true, "Guardado localmente (sin Firebase)");
            }
            return;
        }

        // Guardar en Firebase
        String userId = user.getUid();
        DocumentReference docRef = firestore.collection(FIREBASE_COLLECTION).document(userId);

        Map<String, Object> data = new HashMap<>();
        data.put(FIREBASE_FIELD_WALLPAPER, wallpaperName);
        data.put(FIREBASE_FIELD_UPDATED_AT, System.currentTimeMillis());

        docRef.set(data, SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "☁️ ✓ Guardado en Firebase exitosamente");
                if (callback != null) {
                    callback.onSaveComplete(true, "Guardado en nube y local");
                }
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "⚠️ Error guardando en Firebase: " + e.getMessage());
                // Aunque falle Firebase, ya se guardó en local
                if (callback != null) {
                    callback.onSaveComplete(true, "Guardado localmente (Firebase falló)");
                }
            });
    }

    /**
     * Versión simplificada sin callback
     */
    public void setSelectedWallpaper(@NonNull String wallpaperName) {
        setSelectedWallpaper(wallpaperName, null);
    }

    /**
     * Obtiene wallpaper de forma SÍNCRONA (solo local).
     * Útil cuando necesitas el valor inmediatamente y no puedes esperar Firebase.
     *
     * @return Nombre del wallpaper guardado localmente
     */
    public String getSelectedWallpaperSync() {
        return getLocalWallpaper();
    }

    // ============================================
    // MÉTODOS PRIVADOS
    // ============================================

    /**
     * Valida si un nombre de wallpaper es válido
     */
    private boolean isValidWallpaper(@Nullable String wallpaperName) {
        return wallpaperName != null && VALID_WALLPAPERS.contains(wallpaperName);
    }

    /**
     * Lee wallpaper de SharedPreferences (local)
     */
    private String getLocalWallpaper() {
        String wallpaper = sharedPrefs.getString(KEY_SELECTED_WALLPAPER, DEFAULT_WALLPAPER);

        // Validar por si acaso hay datos corruptos
        if (!isValidWallpaper(wallpaper)) {
            Log.w(TAG, "⚠️ Wallpaper local inválido: " + wallpaper + ", usando default");
            return DEFAULT_WALLPAPER;
        }

        return wallpaper;
    }

    /**
     * Guarda wallpaper en SharedPreferences (local)
     */
    private boolean saveLocalWallpaper(@NonNull String wallpaperName) {
        return sharedPrefs.edit()
            .putString(KEY_SELECTED_WALLPAPER, wallpaperName)
            .commit();  // commit() en lugar de apply() para saber si fue exitoso
    }

    // ============================================
    // CALLBACKS
    // ============================================

    public interface WallpaperCallback {
        void onWallpaperReceived(@NonNull String wallpaperName);
    }

    public interface SaveCallback {
        void onSaveComplete(boolean success, @NonNull String message);
    }

    // ============================================
    // UTILIDADES
    // ============================================

    /**
     * Obtiene la lista de wallpapers válidos
     */
    public static Set<String> getValidWallpapers() {
        return new HashSet<>(VALID_WALLPAPERS);
    }

    /**
     * Obtiene el wallpaper por defecto
     */
    public static String getDefaultWallpaper() {
        return DEFAULT_WALLPAPER;
    }
}
