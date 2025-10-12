package com.secret.blackholeglow;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * UserManager - Gestiona la sesión del usuario con Google
 *
 * Almacena y recupera datos del usuario:
 * - ID de usuario
 * - Nombre completo
 * - Email
 * - URL de la foto de perfil
 *
 * Usa SharedPreferences para persistencia de sesión
 */
public class UserManager {
    private static final String TAG = "UserManager";
    private static final String PREFS_NAME = "blackholeglow_user_prefs";

    // Claves para SharedPreferences
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_PHOTO_URL = "user_photo_url";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private final SharedPreferences prefs;
    private static UserManager instance;

    /**
     * Constructor privado (Singleton)
     */
    private UserManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Obtiene la instancia única de UserManager
     */
    public static synchronized UserManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserManager(context);
        }
        return instance;
    }

    /**
     * Guarda los datos del usuario después del login
     */
    public void saveUserData(String userId, String userName, String userEmail, String photoUrl) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, userName);
        editor.putString(KEY_USER_EMAIL, userEmail);
        editor.putString(KEY_USER_PHOTO_URL, photoUrl);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();

        Log.d(TAG, "✓ Datos de usuario guardados: " + userName + " (" + userEmail + ")");
    }

    /**
     * Verifica si hay un usuario con sesión iniciada
     */
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Obtiene el ID del usuario
     */
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    /**
     * Obtiene el nombre del usuario
     */
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "Usuario");
    }

    /**
     * Obtiene el email del usuario
     */
    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    /**
     * Obtiene la URL de la foto de perfil del usuario
     */
    public String getUserPhotoUrl() {
        return prefs.getString(KEY_USER_PHOTO_URL, null);
    }

    /**
     * Cierra la sesión del usuario
     */
    public void logout() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        Log.d(TAG, "✓ Sesión cerrada");
    }

    /**
     * Imprime información del usuario (debug)
     */
    public void printUserInfo() {
        if (isLoggedIn()) {
            Log.d(TAG, "=== INFORMACIÓN DEL USUARIO ===");
            Log.d(TAG, "  ID: " + getUserId());
            Log.d(TAG, "  Nombre: " + getUserName());
            Log.d(TAG, "  Email: " + getUserEmail());
            Log.d(TAG, "  Foto URL: " + getUserPhotoUrl());
        } else {
            Log.d(TAG, "No hay usuario con sesión iniciada");
        }
    }
}
