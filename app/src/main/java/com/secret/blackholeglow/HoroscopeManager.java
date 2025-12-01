package com.secret.blackholeglow;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Calendar;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   ✨ HoroscopeManager - Horóscopo Semanal Personalizado           ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * CARACTERÍSTICAS:
 * - Calcula el signo zodiacal del usuario basado en fecha de nacimiento
 * - Genera horóscopo semanal usando Gemini AI
 * - Cache semanal para minimizar llamadas a API (1 por semana)
 * - Fallback a mensajes locales si Gemini no está disponible
 *
 * COSTO: ~0.0001 USD por semana (prácticamente gratis)
 * - 1 llamada a Gemini Flash por semana
 * - ~100 tokens de entrada, ~150 tokens de salida
 */
public class HoroscopeManager {
    private static final String TAG = "HoroscopeManager";
    private static final String PREFS_NAME = "horoscope_cache";

    // Claves de cache
    private static final String KEY_CACHED_HOROSCOPE = "cached_horoscope";
    private static final String KEY_CACHED_WEEK = "cached_week";
    private static final String KEY_CACHED_SIGN = "cached_sign";
    private static final String KEY_CACHED_SYMBOL = "cached_symbol";

    // Signos zodiacales
    public enum ZodiacSign {
        ARIES("Aries", "♈", 3, 21, 4, 19),
        TAURUS("Tauro", "♉", 4, 20, 5, 20),
        GEMINI("Géminis", "♊", 5, 21, 6, 20),
        CANCER("Cáncer", "♋", 6, 21, 7, 22),
        LEO("Leo", "♌", 7, 23, 8, 22),
        VIRGO("Virgo", "♍", 8, 23, 9, 22),
        LIBRA("Libra", "♎", 9, 23, 10, 22),
        SCORPIO("Escorpio", "♏", 10, 23, 11, 21),
        SAGITTARIUS("Sagitario", "♐", 11, 22, 12, 21),
        CAPRICORN("Capricornio", "♑", 12, 22, 1, 19),
        AQUARIUS("Acuario", "♒", 1, 20, 2, 18),
        PISCES("Piscis", "♓", 2, 19, 3, 20);

        public final String name;
        public final String symbol;
        public final int startMonth;
        public final int startDay;
        public final int endMonth;
        public final int endDay;

        ZodiacSign(String name, String symbol, int startMonth, int startDay, int endMonth, int endDay) {
            this.name = name;
            this.symbol = symbol;
            this.startMonth = startMonth;
            this.startDay = startDay;
            this.endMonth = endMonth;
            this.endDay = endDay;
        }
    }

    // Estado
    private final Context context;
    private final SharedPreferences prefs;
    private final UserManager userManager;
    private GeminiService geminiService;

    private ZodiacSign userSign = null;
    private String currentHoroscope = "";
    private boolean isLoading = false;

    // Callbacks
    public interface HoroscopeCallback {
        void onHoroscopeReady(String horoscope, ZodiacSign sign);
        void onError(String error);
    }

    // Singleton
    private static HoroscopeManager instance;

    public static synchronized HoroscopeManager getInstance(Context context) {
        if (instance == null) {
            instance = new HoroscopeManager(context);
        }
        return instance;
    }

    private HoroscopeManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.userManager = UserManager.getInstance(context);
        this.geminiService = GeminiService.getInstance();

        // Calcular signo del usuario
        calculateUserSign();

        Log.d(TAG, "✨ HoroscopeManager inicializado");
    }

    /**
     * Calcula el signo zodiacal basado en la fecha de nacimiento
     */
    private void calculateUserSign() {
        if (!userManager.hasBirthDate()) {
            Log.d(TAG, "📅 Sin fecha de nacimiento, no se puede calcular signo");
            return;
        }

        int month = userManager.getBirthMonth();
        int day = userManager.getBirthDay();

        userSign = getZodiacSign(month, day);
        Log.d(TAG, "✨ Signo del usuario: " + userSign.symbol + " " + userSign.name);
    }

    /**
     * Determina el signo zodiacal para una fecha dada
     */
    public static ZodiacSign getZodiacSign(int month, int day) {
        for (ZodiacSign sign : ZodiacSign.values()) {
            if (sign == ZodiacSign.CAPRICORN) {
                // Capricornio cruza el año
                if ((month == 12 && day >= 22) || (month == 1 && day <= 19)) {
                    return sign;
                }
            } else {
                if ((month == sign.startMonth && day >= sign.startDay) ||
                    (month == sign.endMonth && day <= sign.endDay)) {
                    return sign;
                }
            }
        }
        return ZodiacSign.ARIES; // Fallback
    }

    /**
     * Obtiene el horóscopo semanal
     * Usa cache si está disponible y es de esta semana
     */
    public void getWeeklyHoroscope(HoroscopeCallback callback) {
        if (userSign == null) {
            calculateUserSign();
            if (userSign == null) {
                callback.onError("No hay fecha de nacimiento configurada");
                return;
            }
        }

        // Verificar cache
        int currentWeek = getCurrentWeekNumber();
        int cachedWeek = prefs.getInt(KEY_CACHED_WEEK, -1);
        String cachedSign = prefs.getString(KEY_CACHED_SIGN, "");

        if (cachedWeek == currentWeek && cachedSign.equals(userSign.name)) {
            String cachedHoroscope = prefs.getString(KEY_CACHED_HOROSCOPE, "");
            if (!cachedHoroscope.isEmpty()) {
                Log.d(TAG, "📦 Usando horóscopo cacheado de semana " + currentWeek);
                currentHoroscope = cachedHoroscope;
                callback.onHoroscopeReady(cachedHoroscope, userSign);
                return;
            }
        }

        // Generar nuevo horóscopo con Gemini
        generateHoroscopeWithGemini(callback);
    }

    /**
     * Genera un horóscopo usando Gemini AI
     */
    private void generateHoroscopeWithGemini(HoroscopeCallback callback) {
        if (isLoading) {
            Log.d(TAG, "⏳ Ya se está generando horóscopo");
            return;
        }

        isLoading = true;

        String prompt = buildHoroscopePrompt();
        Log.d(TAG, "🤖 Solicitando horóscopo a Gemini para " + userSign.name);

        geminiService.generateContent(prompt, new GeminiService.GeminiCallback() {
            @Override
            public void onResponse(String response) {
                isLoading = false;

                // Limpiar respuesta
                String cleanHoroscope = cleanHoroscopeResponse(response);
                currentHoroscope = cleanHoroscope;

                // Guardar en cache
                saveToCache(cleanHoroscope);

                Log.d(TAG, "✨ Horóscopo generado: " + cleanHoroscope.substring(0, Math.min(50, cleanHoroscope.length())) + "...");
                callback.onHoroscopeReady(cleanHoroscope, userSign);
            }

            @Override
            public void onError(String error) {
                isLoading = false;
                Log.w(TAG, "⚠️ Error Gemini: " + error + ", usando fallback");

                // Usar horóscopo local
                String localHoroscope = getLocalHoroscope();
                currentHoroscope = localHoroscope;
                callback.onHoroscopeReady(localHoroscope, userSign);
            }
        });
    }

    /**
     * Construye el prompt para Gemini
     */
    private String buildHoroscopePrompt() {
        String userName = userManager.isLoggedIn() ? userManager.getFirstName() : "amigo";

        return "Eres un astrólogo amigable y positivo. Genera un horóscopo semanal corto (máximo 3 oraciones) " +
               "para " + userName + " que es " + userSign.name + " " + userSign.symbol + ". " +
               "El tono debe ser motivador, esperanzador y personalizado. " +
               "NO uses comillas ni asteriscos. " +
               "Menciona amor, trabajo o salud brevemente. " +
               "Termina con un emoji relacionado al tema principal.";
    }

    /**
     * Limpia la respuesta de Gemini
     */
    private String cleanHoroscopeResponse(String response) {
        String clean = response
            .replace("\"", "")
            .replace("*", "")
            .replace("#", "")
            .trim();

        // Limitar longitud (máximo 350 caracteres para que quepa en el display)
        if (clean.length() > 350) {
            clean = clean.substring(0, 347) + "...";
        }

        return clean;
    }

    /**
     * Guarda el horóscopo en cache
     */
    private void saveToCache(String horoscope) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_CACHED_HOROSCOPE, horoscope);
        editor.putInt(KEY_CACHED_WEEK, getCurrentWeekNumber());
        editor.putString(KEY_CACHED_SIGN, userSign.name);
        editor.putString(KEY_CACHED_SYMBOL, userSign.symbol);
        editor.apply();

        Log.d(TAG, "💾 Horóscopo guardado en cache para semana " + getCurrentWeekNumber());
    }

    /**
     * Obtiene el número de semana actual del año
     */
    private int getCurrentWeekNumber() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.WEEK_OF_YEAR);
    }

    /**
     * Horóscopo local (fallback sin internet)
     */
    private String getLocalHoroscope() {
        String[] localHoroscopes = {
            "Esta semana las estrellas te favorecen. Confía en tu intuición y toma decisiones con el corazón. El amor te sorprenderá. 💫",
            "Es momento de brillar en el trabajo. Tu creatividad está en su punto más alto. Aprovecha para iniciar nuevos proyectos. ⭐",
            "La energía cósmica te impulsa hacia adelante. Cuida tu salud y mantén el equilibrio. Grandes cosas vienen. 🌟",
            "El universo conspira a tu favor. Mantén una actitud positiva y verás cómo todo fluye. El amor está cerca. 💖",
            "Tu signo está especialmente fuerte esta semana. Es momento de tomar la iniciativa y perseguir tus sueños. ✨"
        };

        int index = (int) (System.currentTimeMillis() / (7 * 24 * 60 * 60 * 1000)) % localHoroscopes.length;
        return localHoroscopes[index];
    }

    // Getters
    public ZodiacSign getUserSign() {
        return userSign;
    }

    public String getCurrentHoroscope() {
        return currentHoroscope;
    }

    public boolean hasValidSign() {
        return userSign != null;
    }

    public boolean isLoading() {
        return isLoading;
    }

    /**
     * Fuerza recarga del horóscopo (ignora cache)
     */
    public void forceRefresh(HoroscopeCallback callback) {
        prefs.edit().remove(KEY_CACHED_WEEK).apply();
        getWeeklyHoroscope(callback);
    }
}
