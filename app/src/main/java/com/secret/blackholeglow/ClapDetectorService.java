package com.secret.blackholeglow;

import com.secret.blackholeglow.activities.MainActivity;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.Locale;

/*
╔══════════════════════════════════════════════════════════════════════════════╗
║                                                                              ║
║   👏 ClapDetectorService.java – Encontrar Teléfono con Aplauso              ║
║                                                                              ║
║   ✨ "Aplaude dos veces y tu teléfono responderá" ✨                         ║
║      🎤 Escucha el micrófono en segundo plano                                ║
║      🐱 Responde con maullido de gato o "¡Aquí toy!"                         ║
║      🌟 Activa efectos visuales en el wallpaper                              ║
║                                                                              ║
║   🔍 Descripción General:                                                    ║
║     • ForegroundService que escucha continuamente                           ║
║     • Detecta patrón de aplauso (2 picos de sonido cortos)                  ║
║     • Responde con sonido + vibración + efectos visuales                    ║
║     • Configurable desde MainActivity                                       ║
║                                                                              ║
║   🎨 Características:                                                        ║
║     • Detección de amplitud de audio                                        ║
║     • Análisis de frecuencia para filtrar ruido                             ║
║     • Text-to-Speech para voz personalizada                                 ║
║     • Broadcast a SceneRenderer para efectos visuales                       ║
║     • Notificación persistente mientras está activo                         ║
║                                                                              ║
║   📱 Activación:                                                             ║
║     MainActivity → Switch "Encontrar con Aplauso" → Servicio activo         ║
║                                                                              ║
╚══════════════════════════════════════════════════════════════════════════════╝
*/
public class ClapDetectorService extends Service {

    private static final String TAG = "ClapDetectorService";
    private static final String CHANNEL_ID = "clap_detector_channel";
    private static final int NOTIFICATION_ID = 9001;

    // ════════════════════════════════════════════════════════════════════════
    // PARÁMETROS DE DETECCIÓN DE AUDIO - MEJORADOS PARA EVITAR FALSOS POSITIVOS
    // ════════════════════════════════════════════════════════════════════════
    private static final int SAMPLE_RATE = 44100;           // 44.1 kHz
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
    );

    // ⚡ Umbral MÁS ALTO para evitar detectar ruido de bolsillo/música
    private static final int CLAP_AMPLITUDE_THRESHOLD = 20000;  // Aumentado de 15000 a 20000

    // Tiempo mínimo entre aplausos (ms) - evita rebotes
    private static final long MIN_TIME_BETWEEN_CLAPS = 150;  // Aumentado de 100 a 150ms

    // Tiempo máximo entre aplausos para el patrón (más estricto)
    private static final long MAX_TIME_BETWEEN_CLAPS = 600;  // Reducido de 800 a 600ms

    // 🎯 NUEVO: Requiere 4 aplausos en lugar de 2
    private static final int REQUIRED_CLAPS = 4;

    // 🎯 NUEVO: Tiempo máximo para completar los 4 aplausos
    private static final long MAX_PATTERN_TIME = 2000;  // 2 segundos para hacer 4 aplausos

    // 🎯 NUEVO: Cooldown entre activaciones (evita spam)
    private static final long COOLDOWN_TIME = 15000;  // 15 segundos

    // ════════════════════════════════════════════════════════════════════════
    // COMPONENTES
    // ════════════════════════════════════════════════════════════════════════
    private AudioRecord audioRecord;
    private Thread recordingThread;
    private boolean isRecording = false;

    private TextToSpeech textToSpeech;
    private boolean ttsReady = false;

    private Vibrator vibrator;
    private MediaPlayer meowPlayer;

    private long lastClapTime = 0;
    private int clapCount = 0;
    private long firstClapTime = 0;  // Tiempo del primer aplauso del patrón
    private long lastActivationTime = 0;  // Última vez que se activó (para cooldown)

    // ════════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ════════════════════════════════════════════════════════════════════════

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "🎤 ClapDetectorService creado");

        // Inicializar componentes
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Inicializar Text-to-Speech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(new Locale("es", "MX"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "⚠️ Idioma español no soportado, usando inglés");
                    textToSpeech.setLanguage(Locale.US);
                }

                // Configurar voz femenina
                textToSpeech.setPitch(1.3f);  // Voz femenina (más aguda)
                textToSpeech.setSpeechRate(1.0f);  // Velocidad normal y clara

                ttsReady = true;
                Log.d(TAG, "✅ Text-to-Speech listo");
            } else {
                Log.e(TAG, "❌ Error inicializando TTS");
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "🚀 Iniciando servicio de detección de aplausos");

        // Crear notificación persistente (requerido para ForegroundService)
        createNotificationChannel();
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);

        // Iniciar detección de audio
        startListening();

        return START_STICKY;  // Reiniciar si el sistema lo mata
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "🛑 Deteniendo servicio de detección de aplausos");

        stopListening();

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        if (meowPlayer != null) {
            meowPlayer.release();
            meowPlayer = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;  // No es un servicio vinculado
    }

    // ════════════════════════════════════════════════════════════════════════
    // DETECCIÓN DE AUDIO
    // ════════════════════════════════════════════════════════════════════════

    private void startListening() {
        try {
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    BUFFER_SIZE
            );

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "❌ Error: AudioRecord no inicializado");
                return;
            }

            audioRecord.startRecording();
            isRecording = true;

            recordingThread = new Thread(this::detectClaps, "ClapDetectionThread");
            recordingThread.start();

            Log.d(TAG, "🎧 Escuchando aplausos...");

        } catch (SecurityException e) {
            Log.e(TAG, "❌ Permiso de micrófono denegado: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "❌ Error al iniciar grabación: " + e.getMessage());
        }
    }

    private void stopListening() {
        isRecording = false;

        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            } catch (Exception e) {
                Log.e(TAG, "Error deteniendo AudioRecord: " + e.getMessage());
            }
        }

        if (recordingThread != null) {
            try {
                recordingThread.join(1000);
            } catch (InterruptedException e) {
                Log.e(TAG, "Error esperando thread: " + e.getMessage());
            }
        }
    }

    private void detectClaps() {
        short[] buffer = new short[BUFFER_SIZE];

        while (isRecording) {
            int readSize = audioRecord.read(buffer, 0, BUFFER_SIZE);

            if (readSize > 0) {
                // Calcular amplitud máxima en este buffer
                int maxAmplitude = 0;
                for (int i = 0; i < readSize; i++) {
                    int amplitude = Math.abs(buffer[i]);
                    if (amplitude > maxAmplitude) {
                        maxAmplitude = amplitude;
                    }
                }

                // Detectar si supera el umbral (posible aplauso)
                if (maxAmplitude > CLAP_AMPLITUDE_THRESHOLD) {
                    onClapDetected();
                }
            }
        }
    }

    private void onClapDetected() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastClap = currentTime - lastClapTime;

        // Ignorar si es demasiado pronto (rebote)
        if (timeSinceLastClap < MIN_TIME_BETWEEN_CLAPS) {
            return;
        }

        // Verificar si estamos en cooldown
        long timeSinceActivation = currentTime - lastActivationTime;
        if (timeSinceActivation < COOLDOWN_TIME && lastActivationTime > 0) {
            Log.d(TAG, "⏸️ En cooldown - ignorando aplauso (" +
                  (COOLDOWN_TIME - timeSinceActivation) / 1000 + "s restantes)");
            return;
        }

        // Si es el primer aplauso, iniciar patrón
        if (clapCount == 0) {
            clapCount = 1;
            firstClapTime = currentTime;
            lastClapTime = currentTime;
            Log.d(TAG, "👏 Aplauso 1/4 detectado - Iniciando patrón");
            return;
        }

        // Verificar si el patrón ha expirado (más de 2 segundos desde el primero)
        long timeSinceFirst = currentTime - firstClapTime;
        if (timeSinceFirst > MAX_PATTERN_TIME) {
            // Patrón expirado, reiniciar
            clapCount = 1;
            firstClapTime = currentTime;
            lastClapTime = currentTime;
            Log.d(TAG, "⏱️ Patrón expirado - Reiniciando con aplauso 1/4");
            return;
        }

        // Verificar si el tiempo entre aplausos es válido
        if (timeSinceLastClap < MAX_TIME_BETWEEN_CLAPS) {
            clapCount++;
            lastClapTime = currentTime;
            Log.d(TAG, "👏 Aplauso " + clapCount + "/4 detectado");

            // Si completamos los 4 aplausos
            if (clapCount >= REQUIRED_CLAPS) {
                Log.d(TAG, "👏👏👏👏 ¡PATRÓN COMPLETO! 4 APLAUSOS DETECTADOS");
                clapCount = 0;
                firstClapTime = 0;
                onClapPatternComplete();
            }
        } else {
            // Tiempo muy largo entre aplausos, reiniciar
            clapCount = 1;
            firstClapTime = currentTime;
            lastClapTime = currentTime;
            Log.d(TAG, "⏱️ Demasiado tiempo entre aplausos - Reiniciando con aplauso 1/4");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // RESPUESTA AL PATRÓN COMPLETO DE 4 APLAUSOS
    // ════════════════════════════════════════════════════════════════════════

    private void onClapPatternComplete() {
        // Actualizar tiempo de última activación (cooldown)
        lastActivationTime = System.currentTimeMillis();

        new Handler(Looper.getMainLooper()).post(() -> {
            Log.d(TAG, "🎉 ¡Patrón completo! Activando respuesta...");

            // 1. VIBRACIÓN (patrón único)
            vibratePhone();

            // 2. BEEP RETRO (discreto y rápido)
            playRetroBeep();

            // 3. EFECTOS VISUALES EN WALLPAPER
            triggerWallpaperEffect();
        });
    }

    private void vibratePhone() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Patrón único: 3 vibraciones cortas tipo "encontrado"
                long[] pattern = {0, 100, 50, 100, 50, 100};
                VibrationEffect effect = VibrationEffect.createWaveform(pattern, -1);
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(300);
            }
            Log.d(TAG, "📳 Vibración activada");
        }
    }

    private void playRetroBeep() {
        try {
            if (ttsReady) {
                // Configurar voz femenina con volumen alto
                textToSpeech.setPitch(1.3f);  // Voz femenina
                textToSpeech.setSpeechRate(1.0f);  // Velocidad normal

                // Usar AudioManager para aumentar volumen temporalmente
                android.media.AudioManager audioManager =
                    (android.media.AudioManager) getSystemService(AUDIO_SERVICE);

                // Guardar volumen actual
                int currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);
                int maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);

                // Subir volumen a 80% del máximo
                audioManager.setStreamVolume(
                    android.media.AudioManager.STREAM_MUSIC,
                    (int)(maxVolume * 0.8),
                    0
                );

                // Hablar con voz femenina
                textToSpeech.speak("Aquí estoy", TextToSpeech.QUEUE_FLUSH, null, "clap_response");

                // Restaurar volumen después de 2 segundos
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    audioManager.setStreamVolume(
                        android.media.AudioManager.STREAM_MUSIC,
                        currentVolume,
                        0
                    );
                }, 2000);

                Log.d(TAG, "🗣️ Voz femenina: 'Aquí estoy'");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reproduciendo voz: " + e.getMessage());
        }
    }

    private void triggerWallpaperEffect() {
        // Enviar broadcast a SceneRenderer para activar efectos visuales
        Intent intent = new Intent("com.secret.blackholeglow.CLAP_DETECTED");
        sendBroadcast(intent);
        Log.d(TAG, "🌟 Broadcast enviado para efectos visuales");
    }

    // ════════════════════════════════════════════════════════════════════════
    // NOTIFICACIÓN PERSISTENTE
    // ════════════════════════════════════════════════════════════════════════

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Encontrar con Aplauso",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Servicio activo para encontrar tu teléfono con aplausos");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("👏 Encontrar con Aplauso")
                .setContentText("Aplaude 4 veces rápido para encontrar tu teléfono 🔊")
                .setSmallIcon(R.mipmap.ic_launcher)  // Usar icono de la app
                .setContentIntent(pendingIntent)
                .setOngoing(true)  // No se puede deslizar para cerrar
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}
