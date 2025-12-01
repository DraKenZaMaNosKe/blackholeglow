package com.secret.blackholeglow;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   🎂 BirthdayManager - Sistema de Cumpleaños Comunitario          ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * CARACTERÍSTICAS:
 * - Detecta si hoy es el cumpleaños del usuario
 * - Muestra cumpleañeros del día aleatoriamente (para no saturar)
 * - Sistema de likes para felicitar
 * - Gemini genera mensaje especial cuando hay likes acumulados
 * - Cache para no repetir felicitaciones
 *
 * ECONOMÍA:
 * - Solo selecciona ALGUNOS cumpleañeros para mostrar (no todos)
 * - Gemini solo se llama cuando hay 5+ likes nuevos
 * - 1 llamada máximo por hora
 */
public class BirthdayManager {
    private static final String TAG = "BirthdayManager";
    private static final String PREFS_NAME = "birthday_prefs";

    // Firebase collections
    private static final String COLLECTION_BIRTHDAYS = "birthdays";
    private static final String COLLECTION_BIRTHDAY_LIKES = "birthday_likes";

    // Cache keys
    private static final String KEY_LAST_LIKE_CHECK = "last_like_check";
    private static final String KEY_TOTAL_LIKES = "total_likes";
    private static final String KEY_LAST_GEMINI_CALL = "last_gemini_call";
    private static final String KEY_LAST_GEMINI_MESSAGE = "last_gemini_message";

    // Configuración
    private static final int MAX_BIRTHDAYS_TO_SHOW = 3;      // Máximo cumpleañeros a mostrar a la vez
    private static final float SHOW_PROBABILITY = 0.3f;      // 30% probabilidad de mostrar un cumpleañero
    private static final long GEMINI_COOLDOWN_MS = 3600000;  // 1 hora entre llamadas Gemini
    private static final int MIN_LIKES_FOR_GEMINI = 5;       // Mínimo likes para llamar Gemini

    // Estado
    private final Context context;
    private final SharedPreferences prefs;
    private final FirebaseFirestore db;
    private final UserManager userManager;
    private final GeminiService geminiService;
    private final Random random = new Random();

    private boolean isTodayMyBirthday = false;
    private int myBirthdayLikes = 0;
    private String lastGeminiMessage = "";
    private List<BirthdayPerson> todaysBirthdays = new ArrayList<>();

    // Singleton
    private static BirthdayManager instance;

    public static synchronized BirthdayManager getInstance(Context context) {
        if (instance == null) {
            instance = new BirthdayManager(context);
        }
        return instance;
    }

    /**
     * Datos de una persona que cumple años
     */
    public static class BirthdayPerson {
        public String odiseasId;
        public String displayName;
        public String photoUrl;
        public int age;  // Años que cumple
        public int likes;

        public BirthdayPerson(String odiseasId, String displayName, String photoUrl, int age) {
            this.odiseasId = odiseasId;
            this.displayName = displayName;
            this.photoUrl = photoUrl;
            this.age = age;
            this.likes = 0;
        }
    }

    // Callbacks
    public interface BirthdayCallback {
        void onBirthdaysLoaded(List<BirthdayPerson> birthdays);
        void onError(String error);
    }

    public interface LikeCallback {
        void onLikeSent(int totalLikes);
        void onError(String error);
    }

    public interface GeminiMessageCallback {
        void onMessageReady(String message);
    }

    private BirthdayManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.db = FirebaseFirestore.getInstance();
        this.userManager = UserManager.getInstance(context);
        this.geminiService = GeminiService.getInstance();

        // Verificar si hoy es mi cumpleaños
        checkIfTodayIsMyBirthday();

        // Cargar mensaje de Gemini cacheado
        lastGeminiMessage = prefs.getString(KEY_LAST_GEMINI_MESSAGE, "");

        Log.d(TAG, "🎂 BirthdayManager inicializado");
    }

    /**
     * Verifica si hoy es el cumpleaños del usuario actual
     */
    private void checkIfTodayIsMyBirthday() {
        if (!userManager.hasBirthDate()) {
            isTodayMyBirthday = false;
            return;
        }

        Calendar today = Calendar.getInstance();
        int todayMonth = today.get(Calendar.MONTH) + 1;  // Calendar months are 0-based
        int todayDay = today.get(Calendar.DAY_OF_MONTH);

        int birthMonth = userManager.getBirthMonth();
        int birthDay = userManager.getBirthDay();

        isTodayMyBirthday = (todayMonth == birthMonth && todayDay == birthDay);

        if (isTodayMyBirthday) {
            Log.d(TAG, "🎂🎉 ¡¡HOY ES TU CUMPLEAÑOS!! 🎉🎂");
            // Registrar en Firebase que hoy es mi cumpleaños
            registerMyBirthday();
        }
    }

    /**
     * Registra el cumpleaños del usuario en Firebase
     */
    private void registerMyBirthday() {
        if (!userManager.isLoggedIn()) return;

        String odiseasId = userManager.getUserId();
        if (odiseasId == null) return;

        Calendar today = Calendar.getInstance();
        String todayStr = String.format("%04d-%02d-%02d",
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH) + 1,
            today.get(Calendar.DAY_OF_MONTH));

        int birthYear = userManager.getBirthYear();
        int age = today.get(Calendar.YEAR) - birthYear;

        Map<String, Object> birthdayData = new HashMap<>();
        birthdayData.put("odiseasId", odiseasId);
        birthdayData.put("displayName", userManager.getUserName());
        birthdayData.put("photoUrl", userManager.getUserPhotoUrl());
        birthdayData.put("date", todayStr);
        birthdayData.put("age", age);
        birthdayData.put("likes", 0);
        birthdayData.put("timestamp", System.currentTimeMillis());

        db.collection(COLLECTION_BIRTHDAYS)
            .document(odiseasId + "_" + todayStr)
            .set(birthdayData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "🎂 Cumpleaños registrado en Firebase");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error registrando cumpleaños: " + e.getMessage());
            });
    }

    /**
     * Obtiene cumpleañeros del día para mostrar
     * Solo retorna algunos aleatorios para no saturar
     */
    public void getTodaysBirthdays(BirthdayCallback callback) {
        Calendar today = Calendar.getInstance();
        String todayStr = String.format("%04d-%02d-%02d",
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH) + 1,
            today.get(Calendar.DAY_OF_MONTH));

        String myId = userManager.isLoggedIn() ? userManager.getUserId() : "";

        db.collection(COLLECTION_BIRTHDAYS)
            .whereEqualTo("date", todayStr)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)  // Obtener máximo 50 pero mostrar solo algunos
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<BirthdayPerson> allBirthdays = new ArrayList<>();

                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    String odiseasId = doc.getString("odiseasId");

                    // No incluirme a mí mismo en la lista de otros cumpleañeros
                    if (odiseasId != null && !odiseasId.equals(myId)) {
                        BirthdayPerson person = new BirthdayPerson(
                            odiseasId,
                            doc.getString("displayName"),
                            doc.getString("photoUrl"),
                            doc.getLong("age").intValue()
                        );
                        person.likes = doc.getLong("likes") != null ? doc.getLong("likes").intValue() : 0;
                        allBirthdays.add(person);
                    }
                }

                // Seleccionar aleatoriamente algunos para mostrar
                List<BirthdayPerson> selectedBirthdays = selectRandomBirthdays(allBirthdays);
                todaysBirthdays = selectedBirthdays;

                Log.d(TAG, "🎂 " + allBirthdays.size() + " cumpleañeros hoy, mostrando " + selectedBirthdays.size());
                callback.onBirthdaysLoaded(selectedBirthdays);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error obteniendo cumpleañeros: " + e.getMessage());
                callback.onError(e.getMessage());
            });
    }

    /**
     * Selecciona aleatoriamente algunos cumpleañeros para mostrar
     */
    private List<BirthdayPerson> selectRandomBirthdays(List<BirthdayPerson> all) {
        List<BirthdayPerson> selected = new ArrayList<>();

        // Mezclar aleatoriamente
        List<BirthdayPerson> shuffled = new ArrayList<>(all);
        java.util.Collections.shuffle(shuffled, random);

        // Seleccionar con probabilidad
        for (BirthdayPerson person : shuffled) {
            if (selected.size() >= MAX_BIRTHDAYS_TO_SHOW) break;

            // Probabilidad de selección (más likes = más probabilidad)
            float probability = SHOW_PROBABILITY + (person.likes * 0.05f);
            if (random.nextFloat() < probability) {
                selected.add(person);
            }
        }

        // Si no seleccionamos ninguno pero hay cumpleañeros, tomar al menos 1
        if (selected.isEmpty() && !shuffled.isEmpty()) {
            selected.add(shuffled.get(0));
        }

        return selected;
    }

    /**
     * Envía un like a un cumpleañero
     */
    public void sendBirthdayLike(String odiseasId, LikeCallback callback) {
        Calendar today = Calendar.getInstance();
        String todayStr = String.format("%04d-%02d-%02d",
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH) + 1,
            today.get(Calendar.DAY_OF_MONTH));

        String docId = odiseasId + "_" + todayStr;

        // Incrementar likes
        db.collection(COLLECTION_BIRTHDAYS)
            .document(docId)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    int currentLikes = doc.getLong("likes") != null ? doc.getLong("likes").intValue() : 0;
                    int newLikes = currentLikes + 1;

                    doc.getReference().update("likes", newLikes)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "❤️ Like enviado! Total: " + newLikes);
                            callback.onLikeSent(newLikes);
                        })
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
                } else {
                    callback.onError("Cumpleañero no encontrado");
                }
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Obtiene los likes de mi cumpleaños
     */
    public void getMyBirthdayLikes(LikeCallback callback) {
        if (!isTodayMyBirthday || !userManager.isLoggedIn()) {
            callback.onLikeSent(0);
            return;
        }

        Calendar today = Calendar.getInstance();
        String todayStr = String.format("%04d-%02d-%02d",
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH) + 1,
            today.get(Calendar.DAY_OF_MONTH));

        String docId = userManager.getUserId() + "_" + todayStr;

        db.collection(COLLECTION_BIRTHDAYS)
            .document(docId)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    myBirthdayLikes = doc.getLong("likes") != null ? doc.getLong("likes").intValue() : 0;
                    callback.onLikeSent(myBirthdayLikes);

                    // Si hay suficientes likes, generar mensaje de Gemini
                    checkForGeminiMessage();
                } else {
                    callback.onLikeSent(0);
                }
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Verifica si debemos llamar a Gemini para mensaje especial
     */
    private void checkForGeminiMessage() {
        if (myBirthdayLikes < MIN_LIKES_FOR_GEMINI) return;

        long lastCall = prefs.getLong(KEY_LAST_GEMINI_CALL, 0);
        long now = System.currentTimeMillis();

        // Respetar cooldown
        if (now - lastCall < GEMINI_COOLDOWN_MS) {
            Log.d(TAG, "🕐 Gemini en cooldown");
            return;
        }

        int lastKnownLikes = prefs.getInt(KEY_TOTAL_LIKES, 0);
        int newLikes = myBirthdayLikes - lastKnownLikes;

        // Solo llamar si hay likes nuevos
        if (newLikes >= MIN_LIKES_FOR_GEMINI) {
            generateBirthdayMessage(newLikes);
        }
    }

    /**
     * Genera mensaje de cumpleaños con Gemini
     */
    private void generateBirthdayMessage(int newLikes) {
        String userName = userManager.getFirstName();

        String prompt = "Hoy es el cumpleaños de " + userName + " y ha recibido " + myBirthdayLikes +
                       " felicitaciones de la comunidad (¡" + newLikes + " nuevas!). " +
                       "Genera un mensaje corto (máximo 2 oraciones) celebrando esto. " +
                       "Sé cálido y usa 2-3 emojis de celebración. " +
                       "No uses comillas ni asteriscos.";

        geminiService.generateContent(prompt, new GeminiService.GeminiCallback() {
            @Override
            public void onResponse(String response) {
                lastGeminiMessage = response.replace("\"", "").replace("*", "").trim();

                // Guardar en cache
                prefs.edit()
                    .putLong(KEY_LAST_GEMINI_CALL, System.currentTimeMillis())
                    .putInt(KEY_TOTAL_LIKES, myBirthdayLikes)
                    .putString(KEY_LAST_GEMINI_MESSAGE, lastGeminiMessage)
                    .apply();

                Log.d(TAG, "🎂🤖 Gemini: " + lastGeminiMessage);
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Error Gemini: " + error);
            }
        });
    }

    // Getters
    public boolean isTodayMyBirthday() { return isTodayMyBirthday; }
    public int getMyBirthdayLikes() { return myBirthdayLikes; }
    public String getLastGeminiMessage() { return lastGeminiMessage; }
    public List<BirthdayPerson> getTodaysBirthdays() { return todaysBirthdays; }

    /**
     * Calcula la edad que cumple hoy
     */
    public int getMyAge() {
        if (!userManager.hasBirthDate()) return 0;
        Calendar today = Calendar.getInstance();
        return today.get(Calendar.YEAR) - userManager.getBirthYear();
    }
}
