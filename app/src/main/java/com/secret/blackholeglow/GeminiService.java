package com.secret.blackholeglow;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

/**
 * GeminiService - Servicio para interactuar con Google Gemini AI
 *
 * Proporciona saludos inteligentes y respuestas de chat usando
 * la API gratuita de Gemini 2.0 Flash
 *
 * 🔐 SEGURIDAD: La API key se lee desde BuildConfig (configurada en local.properties)
 *    Esto evita que la key se suba a GitHub
 */
public class GeminiService {
    private static final String TAG = "GeminiService";

    // 🔐 API Key se lee de BuildConfig (configurada en local.properties)
    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;

    // Usar gemini-2.0-flash (gratuito y rápido)
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;

    private static GeminiService instance;
    private Handler mainHandler;

    public interface GeminiCallback {
        void onResponse(String response);
        void onError(String error);
    }

    private GeminiService() {
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized GeminiService getInstance() {
        if (instance == null) {
            instance = new GeminiService();
        }
        return instance;
    }

    /**
     * Genera un saludo inteligente basado en la hora y contexto
     * (no envía datos personales a la API)
     */
    public void generateSmartGreeting(GeminiCallback callback) {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

        String timeContext;
        if (hour >= 5 && hour < 12) {
            timeContext = "mañana";
        } else if (hour >= 12 && hour < 19) {
            timeContext = "tarde";
        } else {
            timeContext = "noche";
        }

        String dayContext;
        switch (dayOfWeek) {
            case Calendar.MONDAY: dayContext = "lunes"; break;
            case Calendar.TUESDAY: dayContext = "martes"; break;
            case Calendar.WEDNESDAY: dayContext = "miércoles"; break;
            case Calendar.THURSDAY: dayContext = "jueves"; break;
            case Calendar.FRIDAY: dayContext = "viernes"; break;
            case Calendar.SATURDAY: dayContext = "sábado"; break;
            default: dayContext = "domingo"; break;
        }

        String prompt = "Genera un saludo corto, creativo y cálido (máximo 6 palabras). " +
                       "Es " + dayContext + " por la " + timeContext +
                       ". Sé poético o inspirador. Solo responde con el saludo, sin explicaciones ni comillas.";

        sendRequest(prompt, callback);
    }

    /**
     * Envía un mensaje de chat y obtiene respuesta
     * (no envía datos personales)
     */
    public void chat(String userMessage, GeminiCallback callback) {
        String prompt = "Eres Orbix, un asistente amigable y conciso dentro de una app de wallpapers espaciales. " +
                       "Responde de forma breve (máximo 2 oraciones), amigable y con un toque espacial/cósmico. " +
                       "Mensaje del usuario: " + userMessage;

        sendRequest(prompt, callback);
    }

    /**
     * ✨ Genera contenido con un prompt personalizado
     * Útil para horóscopos, mensajes de cumpleaños, etc.
     * @param prompt El prompt completo para Gemini
     * @param callback Callback con la respuesta
     */
    public void generateContent(String prompt, GeminiCallback callback) {
        sendRequest(prompt, callback);
    }

    /**
     * 🎵 Genera un comentario creativo sobre una canción
     * @param songTitle Título de la canción (puede incluir artista)
     * @param userName No se usa (privacidad)
     * @param callback Callback con la respuesta
     */
    public void generateSongComment(String songTitle, String userName, GeminiCallback callback) {
        String prompt = "Genera un mensaje MUY CORTO (máximo 15 palabras) y creativo sobre la canción '" + songTitle + "'. " +
                       "Sé entusiasta, usa 1-2 emojis. " +
                       "Ejemplos de estilo: '🎵 Vibrando con esto!', '♥ Disfrutando este temazo', '🔥 Gran elección!'. " +
                       "Solo responde con el mensaje, sin explicaciones ni comillas.";

        sendRequest(prompt, callback);
    }

    /**
     * Envía request a la API de Gemini
     */
    private void sendRequest(String prompt, GeminiCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);

                // Crear JSON request
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                JSONObject textPart = new JSONObject();
                textPart.put("text", prompt);
                parts.put(textPart);
                content.put("parts", parts);

                JSONArray contents = new JSONArray();
                contents.put(content);

                JSONObject requestBody = new JSONObject();
                requestBody.put("contents", contents);

                // Configuración de generación
                JSONObject generationConfig = new JSONObject();
                generationConfig.put("temperature", 0.8);
                generationConfig.put("maxOutputTokens", 100);
                requestBody.put("generationConfig", generationConfig);

                // Enviar request
                String jsonInput = requestBody.toString();
                Log.d(TAG, "Request: " + jsonInput);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Parsear respuesta
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    Log.d(TAG, "Response: " + jsonResponse.toString());

                    String text = jsonResponse
                            .getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                            .trim();

                    mainHandler.post(() -> callback.onResponse(text));
                } else {
                    // Leer error
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                    StringBuilder error = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line);
                    }
                    reader.close();

                    Log.e(TAG, "Error response: " + error.toString());
                    mainHandler.post(() -> callback.onError("Error: " + responseCode));
                }

                conn.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Error en Gemini API: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError("Error de conexión"));
            }
        }).start();
    }
}
