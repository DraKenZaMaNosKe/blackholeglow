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

    // 🎂 Fecha de nacimiento
    private static final String KEY_BIRTH_YEAR = "birth_year";
    private static final String KEY_BIRTH_MONTH = "birth_month";
    private static final String KEY_BIRTH_DAY = "birth_day";
    private static final String KEY_HAS_BIRTH_DATE = "has_birth_date";

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
            if (hasBirthDate()) {
                Log.d(TAG, "  Fecha de nacimiento: " + getBirthDay() + "/" + getBirthMonth() + "/" + getBirthYear());
            }
        } else {
            Log.d(TAG, "No hay usuario con sesión iniciada");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 🎂 FECHA DE NACIMIENTO
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Guarda la fecha de nacimiento del usuario
     * @param year Año de nacimiento (ej: 1990)
     * @param month Mes de nacimiento (1-12)
     * @param day Día de nacimiento (1-31)
     */
    public void saveBirthDate(int year, int month, int day) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_BIRTH_YEAR, year);
        editor.putInt(KEY_BIRTH_MONTH, month);
        editor.putInt(KEY_BIRTH_DAY, day);
        editor.putBoolean(KEY_HAS_BIRTH_DATE, true);
        editor.apply();

        Log.d(TAG, "🎂 Fecha de nacimiento guardada: " + day + "/" + month + "/" + year);
    }

    /**
     * Verifica si el usuario ha proporcionado su fecha de nacimiento
     */
    public boolean hasBirthDate() {
        return prefs.getBoolean(KEY_HAS_BIRTH_DATE, false);
    }

    /**
     * Obtiene el año de nacimiento
     */
    public int getBirthYear() {
        return prefs.getInt(KEY_BIRTH_YEAR, 2000);
    }

    /**
     * Obtiene el mes de nacimiento (1-12)
     */
    public int getBirthMonth() {
        return prefs.getInt(KEY_BIRTH_MONTH, 1);
    }

    /**
     * Obtiene el día de nacimiento (1-31)
     */
    public int getBirthDay() {
        return prefs.getInt(KEY_BIRTH_DAY, 1);
    }

    /**
     * Obtiene la fecha de nacimiento en milisegundos desde epoch
     * Útil para calcular tiempo de vida
     */
    public long getBirthDateMillis() {
        if (!hasBirthDate()) {
            return 0;
        }
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(getBirthYear(), getBirthMonth() - 1, getBirthDay(), 0, 0, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Obtiene solo el primer nombre del usuario (para saludos cortos)
     */
    public String getFirstName() {
        String fullName = getUserName();
        if (fullName != null && !fullName.isEmpty()) {
            String[] parts = fullName.split(" ");
            return parts[0];
        }
        return "Amigo";
    }
}
