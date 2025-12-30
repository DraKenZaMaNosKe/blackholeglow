package com.secret.blackholeglow;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.audiofx.Visualizer;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;

import java.util.List;

/**
 * MusicVisualizer - Sistema de captura y análisis de audio en tiempo real
 * Captura el audio del sistema y extrae datos de frecuencias para efectos visuales
 *
 * IMPORTANTE: NO GRABA AUDIO, solo lee datos para visualización
 */
public class MusicVisualizer {
    // 🔒 Lock para evitar condiciones de carrera
    private final Object initLock = new Object();
    private volatile boolean isInitializing = false;

    private static final String TAG = "depurar";

    // Visualizer de Android
    private Visualizer visualizer;
    private boolean isEnabled = false;

    // Datos de audio extraídos
    private float bassLevel = 0f;        // Bajos (0.0 - 1.0)
    private float midLevel = 0f;         // Medios (0.0 - 1.0)
    private float trebleLevel = 0f;      // Agudos (0.0 - 1.0)
    private float volumeLevel = 0f;      // Volumen general (0.0 - 1.0)

    // ════════════════════════════════════════════════════════════════════════
    // BANDAS DE FRECUENCIA DETALLADAS (para ecualizador realista)
    // 32 bandas que cubren todo el espectro audible
    // ════════════════════════════════════════════════════════════════════════
    private static final int NUM_BANDS = 32;
    private float[] frequencyBands = new float[NUM_BANDS];
    private float[] smoothedBands = new float[NUM_BANDS];

    // Detección de beats
    private float beatIntensity = 0f;    // Intensidad del beat actual
    private boolean isBeat = false;      // ¿Hay un beat ahora?
    private long lastBeatTime = 0;

    // Configuración
    private static final int CAPTURE_SIZE = 512;  // Tamaño de captura (debe ser potencia de 2)
    private static final float BEAT_THRESHOLD = 1.5f;  // Umbral para detectar beats
    private static final int MIN_BEAT_INTERVAL_MS = 200;  // Mínimo tiempo entre beats

    // Suavizado de valores (más alto = más reactivo, más bajo = más suave)
    private static final float SMOOTHING_FACTOR = 0.45f;  // Aumentado para mejor respuesta
    private float smoothedBass = 0f;
    private float smoothedMid = 0f;
    private float smoothedTreble = 0f;
    private float smoothedVolume = 0f;

    // Estados para debugging
    private int captureCount = 0;
    private long lastLogTime = 0;

    // Context para auto-resume de música
    private Context context;
    private AudioManager audioManager;
    private Handler handler;

    // Monitoreo de conexión (detectar si perdimos audio)
    private long lastAudioDataTime = 0;
    private long lastSignificantAudioTime = 0;  // Última vez que recibimos audio REAL (no silencio)
    private static final long AUDIO_TIMEOUT_MS = 5000;  // 5 segundos sin datos = reconectar
    private static final float SILENCE_THRESHOLD = 0.02f;  // Umbral MUY BAJO para detectar cualquier audio
    private boolean hasReceivedData = false;
    private boolean hasReceivedSignificantAudio = false;

    /**
     * Constructor - Inicializa el visualizador
     * @deprecated Usar MusicVisualizer(Context) para auto-resume de música
     */
    public MusicVisualizer() {
        Log.d(TAG, "[MusicVisualizer] Inicializando (sin context - auto-resume deshabilitado)...");
        this.context = null;
        this.audioManager = null;
        this.handler = new Handler(Looper.getMainLooper());
    }

    /**
     * Constructor con Context - Permite auto-resume de música después de crear Visualizer
     * @param context Context de la aplicación
     */
    public MusicVisualizer(Context context) {
        Log.d(TAG, "[MusicVisualizer] Inicializando con context (auto-resume habilitado)...");
        this.context = context.getApplicationContext();
        this.audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        this.handler = new Handler(Looper.getMainLooper());
    }

    /**
     * Inicializa y activa el visualizador de audio
     * Debe llamarse desde el hilo de OpenGL o UI thread
     *
     * ⚡ AUTO-RESUME: Si había música sonando antes de crear el Visualizer,
     * intentará reanudarla automáticamente después de 500ms
     */
    public boolean initialize() {
        // 🔒 Evitar inicialización concurrente
        synchronized (initLock) {
            if (isInitializing) {
                Log.w(TAG, "[MusicVisualizer] ⚠️ Inicialización ya en progreso, ignorando...");
                return false;
            }
            if (visualizer != null && isEnabled) {
                Log.d(TAG, "[MusicVisualizer] ✓ Ya inicializado y habilitado");
                return true;
            }
            isInitializing = true;
        }

        try {
            // ════════════════════════════════════════════════════════════════════════
            // 🎵 DETECTAR SI HAY MÚSICA SONANDO ANTES DE CREAR VISUALIZER
            // ════════════════════════════════════════════════════════════════════════
            boolean wasMusicPlaying = false;
            if (audioManager != null) {
                wasMusicPlaying = audioManager.isMusicActive();
                Log.d(TAG, "[MusicVisualizer] 🎵 Música activa antes de crear Visualizer: " + wasMusicPlaying);
            }

            // Crear visualizer usando session ID 0 (mezcla de audio del sistema)
            // ⚠️ ESTO PUEDE PAUSAR/CERRAR OTRAS APPS DE AUDIO EN ALGUNOS DISPOSITIVOS
            visualizer = new Visualizer(0);

            // Configurar tamaño de captura
            visualizer.setCaptureSize(CAPTURE_SIZE);

            // Establecer listener para captura de datos
            visualizer.setDataCaptureListener(
                new Visualizer.OnDataCaptureListener() {
                    @Override
                    public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                        // Procesar forma de onda para detección de beats y volumen
                        processWaveform(waveform);
                    }

                    @Override
                    public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                        // Procesar FFT para análisis de frecuencias
                        processFft(fft);
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,  // Tasa de captura (Hz)
                true,   // Capturar waveform
                true    // Capturar FFT
            );

            // Habilitar el visualizer
            visualizer.setEnabled(true);
            isEnabled = true;

            Log.d(TAG, "[MusicVisualizer] ✓ Inicializado correctamente");
            Log.d(TAG, "[MusicVisualizer] Capture Size: " + CAPTURE_SIZE);
            Log.d(TAG, "[MusicVisualizer] Sampling Rate: " + visualizer.getSamplingRate() + " Hz");

            // ════════════════════════════════════════════════════════════════════════
            // 🎵 AUTO-RESUME: Si había música, enviar comandos PLAY con reintentos
            // Samsung puede tardar en responder, así que intentamos varias veces
            // ════════════════════════════════════════════════════════════════════════
            // 🚫 DISABLED: if (wasMusicPlaying && context != null) {
                // Log.d(TAG, "[MusicVisualizer] 🎵 Programando auto-resume de música (3 intentos)...");
                // Intento 1: después de 800ms
                // handler.postDelayed(this::sendMediaPlayCommand, 800);
                // Intento 2: después de 1500ms (por si el primero no funcionó)
                // handler.postDelayed(this::sendMediaPlayCommand, 1500);
                // Intento 3: después de 2500ms (último intento)
                // handler.postDelayed(this::sendMediaPlayCommand, 2500);
            // }

            isInitializing = false;  // 🔒 Reset flag
            return true;

        } catch (Exception e) {
            Log.e(TAG, "[MusicVisualizer] ✗ Error inicializando: " + e.getMessage());
            Log.e(TAG, "[MusicVisualizer] Es posible que falten permisos de audio");
            isInitializing = false;  // 🔒 Reset flag
            return false;
        }
    }

    /**
     * Envía comando MEDIA_PLAY al sistema para reanudar cualquier reproductor de música
     * Intenta múltiples métodos para máxima compatibilidad
     */
    private void sendMediaPlayCommand() {
        Log.d(TAG, "[MusicVisualizer] 🎵 Intentando reanudar música...");

        // ════════════════════════════════════════════════════════════════════════
        // MÉTODO 1: Lanzar Spotify directamente (más confiable en Samsung)
        // ════════════════════════════════════════════════════════════════════════
        try {
            if (context != null) {
                // Intentar con Spotify
                Intent spotifyIntent = context.getPackageManager()
                    .getLaunchIntentForPackage("com.spotify.music");

                if (spotifyIntent != null) {
                    Log.d(TAG, "[MusicVisualizer] 🎵 Spotify detectado, intentando reabrir...");

                    // Enviar intent para reanudar playback (no abre la UI)
                    Intent playIntent = new Intent("com.spotify.mobile.android.ui.widget.PLAY");
                    playIntent.setPackage("com.spotify.music");
                    context.sendBroadcast(playIntent);

                    Log.d(TAG, "[MusicVisualizer] 🎵 Broadcast PLAY enviado a Spotify");
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "[MusicVisualizer] ⚠️ Error con broadcast Spotify: " + e.getMessage());
        }

        // ════════════════════════════════════════════════════════════════════════
        // MÉTODO 2: KeyEvent MEDIA_PLAY via AudioManager
        // ════════════════════════════════════════════════════════════════════════
        if (audioManager != null) {
            try {
                long eventTime = SystemClock.uptimeMillis();

                // Enviar MEDIA_PLAY
                KeyEvent downEvent = new KeyEvent(eventTime, eventTime,
                    KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY, 0);
                audioManager.dispatchMediaKeyEvent(downEvent);

                KeyEvent upEvent = new KeyEvent(eventTime, eventTime,
                    KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY, 0);
                audioManager.dispatchMediaKeyEvent(upEvent);

                Log.d(TAG, "[MusicVisualizer] 🎵 KeyEvent MEDIA_PLAY enviado");
            } catch (Exception e) {
                Log.w(TAG, "[MusicVisualizer] ⚠️ Error con KeyEvent: " + e.getMessage());
            }
        }

        // ════════════════════════════════════════════════════════════════════════
        // MÉTODO 3: Intent ACTION_MEDIA_BUTTON broadcast (legacy pero funciona)
        // ════════════════════════════════════════════════════════════════════════
        try {
            if (context != null) {
                long eventTime = SystemClock.uptimeMillis();

                Intent mediaIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                KeyEvent keyEvent = new KeyEvent(eventTime, eventTime,
                    KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY, 0);
                mediaIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
                context.sendOrderedBroadcast(mediaIntent, null);

                // También enviar UP
                mediaIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                keyEvent = new KeyEvent(eventTime, eventTime,
                    KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY, 0);
                mediaIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
                context.sendOrderedBroadcast(mediaIntent, null);

                Log.d(TAG, "[MusicVisualizer] 🎵 Broadcast ACTION_MEDIA_BUTTON enviado");
            }
        } catch (Exception e) {
            Log.w(TAG, "[MusicVisualizer] ⚠️ Error con broadcast: " + e.getMessage());
        }

        // ════════════════════════════════════════════════════════════════════════
        // MÉTODO 4: Intentar con otros reproductores populares
        // ════════════════════════════════════════════════════════════════════════
        try {
            if (context != null) {
                // YouTube Music
                Intent ytIntent = new Intent("com.google.android.music.PLAY");
                ytIntent.setPackage("com.google.android.apps.youtube.music");
                context.sendBroadcast(ytIntent);

                // Samsung Music
                Intent samsungIntent = new Intent("com.sec.android.app.music.PLAY");
                context.sendBroadcast(samsungIntent);
            }
        } catch (Exception e) {
            // Ignorar silenciosamente
        }

        Log.d(TAG, "[MusicVisualizer] 🎵 ✓ Todos los comandos de reanudación enviados");
    }

    /**
     * Procesa datos de forma de onda para calcular volumen y detectar beats
     */
    private void processWaveform(byte[] waveform) {
        if (waveform == null || waveform.length == 0) return;

        // Marcar que recibimos datos
        lastAudioDataTime = System.currentTimeMillis();
        hasReceivedData = true;

        // Calcular amplitud promedio (volumen)
        float sum = 0f;
        for (byte b : waveform) {
            sum += Math.abs(b);
        }
        float avgAmplitude = sum / waveform.length;
        volumeLevel = avgAmplitude / 128f;  // Normalizar a 0-1

        // Suavizar volumen
        smoothedVolume = smoothedVolume * (1f - SMOOTHING_FACTOR) + volumeLevel * SMOOTHING_FACTOR;

        // Detección de beat simple (pico de volumen)
        long now = System.currentTimeMillis();
        if (volumeLevel > smoothedVolume * BEAT_THRESHOLD &&
            (now - lastBeatTime) > MIN_BEAT_INTERVAL_MS) {

            isBeat = true;
            beatIntensity = Math.min(1.0f, volumeLevel / smoothedVolume - 1.0f);
            lastBeatTime = now;

            Log.v(TAG, "[MusicVisualizer] 🎵 BEAT! Intensity: " + String.format("%.2f", beatIntensity));
        } else {
            isBeat = false;
            beatIntensity *= 0.9f;  // Decay
        }

        captureCount++;
    }

    /**
     * Procesa datos FFT para extraer niveles de frecuencias
     * FFT data format: [0]=DC, [1]=real[1], [2]=imag[1], ..., [n-2]=real[n/2], [n-1]=imag[n/2]
     *
     * NUEVO: Extrae 32 bandas de frecuencia para un ecualizador más realista
     */
    private void processFft(byte[] fft) {
        if (fft == null || fft.length < 4) return;

        // ════════════════════════════════════════════════════════════════════════
        // EXTRAER 32 BANDAS DE FRECUENCIA (escala logarítmica como ecualizadores reales)
        // Frecuencias bajas tienen más resolución que las altas
        // ════════════════════════════════════════════════════════════════════════

        int fftSize = fft.length / 2;  // Número de bins de frecuencia

        // Distribución logarítmica de bandas (más resolución en bajos)
        // Cada banda cubre un rango de frecuencias que crece exponencialmente
        for (int band = 0; band < NUM_BANDS; band++) {
            // Calcular rango de bins para esta banda (escala logarítmica)
            float startRatio = (float) Math.pow(band / (float) NUM_BANDS, 1.5f);
            float endRatio = (float) Math.pow((band + 1) / (float) NUM_BANDS, 1.5f);

            int startBin = Math.max(1, (int) (startRatio * fftSize));
            int endBin = Math.min(fftSize - 1, (int) (endRatio * fftSize));

            if (endBin <= startBin) endBin = startBin + 1;

            // Calcular magnitud promedio para esta banda
            float sum = 0f;
            int count = 0;
            for (int i = startBin; i <= endBin && i * 2 + 1 < fft.length; i++) {
                float real = (float) fft[i * 2];
                float imag = (float) fft[i * 2 + 1];
                float magnitude = (float) Math.sqrt(real * real + imag * imag);
                sum += magnitude;
                count++;
            }

            float avgMagnitude = count > 0 ? sum / count : 0f;

            // Normalizar con compensación por frecuencia (las altas necesitan MUCHO más boost)
            // Las frecuencias altas tienen mucha menos energía naturalmente
            // Las últimas 7 bandas (25-31) reciben boost extra exponencial
            float freqCompensation;
            if (band >= 25) {
                // Boost AGRESIVO para las últimas 7 barras (treble alto)
                float extraBoost = 1.0f + ((band - 25) / 6.0f) * 4.0f;  // 1x a 5x extra
                freqCompensation = 3.5f + extraBoost;  // Total: 4.5x a 8.5x
            } else if (band >= 20) {
                // Boost alto para treble medio
                freqCompensation = 2.5f + (band - 20) * 0.2f;  // 2.5x a 3.5x
            } else {
                // Boost normal para bass y mid
                freqCompensation = 1.0f + (band / (float) NUM_BANDS) * 2.0f;
            }
            float normalized = (avgMagnitude / 128f) * freqCompensation;

            // Aplicar curva de compresión para mejor rango dinámico
            // Esto hace que valores bajos sean más visibles sin saturar los altos
            float compressed = (float) Math.pow(normalized, 0.6f);

            // Limitar al rango 0-1
            frequencyBands[band] = Math.min(1.0f, compressed);
        }

        // Suavizar cada banda individualmente (decay más lento que subida)
        for (int band = 0; band < NUM_BANDS; band++) {
            if (frequencyBands[band] > smoothedBands[band]) {
                // Subida rápida
                smoothedBands[band] = smoothedBands[band] * 0.3f + frequencyBands[band] * 0.7f;
            } else {
                // Bajada más lenta (efecto "caída" de ecualizador)
                smoothedBands[band] = smoothedBands[band] * 0.85f + frequencyBands[band] * 0.15f;
            }
        }

        // ════════════════════════════════════════════════════════════════════════
        // CALCULAR BASS, MID, TREBLE (para compatibilidad con código existente)
        // ════════════════════════════════════════════════════════════════════════
        float bassSum = 0f, midSum = 0f, trebleSum = 0f;

        // Bass: bandas 0-7 (frecuencias más bajas)
        for (int i = 0; i < 8; i++) bassSum += smoothedBands[i];
        bassLevel = Math.min(1.0f, bassSum / 5f);

        // Mid: bandas 8-20
        for (int i = 8; i < 20; i++) midSum += smoothedBands[i];
        midLevel = Math.min(1.0f, midSum / 8f);

        // Treble: bandas 20-31
        for (int i = 20; i < NUM_BANDS; i++) trebleSum += smoothedBands[i];
        trebleLevel = Math.min(1.0f, trebleSum / 8f);

        // Suavizar valores legacy
        smoothedBass = smoothedBass * 0.6f + bassLevel * 0.4f;
        smoothedMid = smoothedMid * 0.6f + midLevel * 0.4f;
        smoothedTreble = smoothedTreble * 0.6f + trebleLevel * 0.4f;

        // Detectar audio significativo
        float totalEnergy = smoothedBass + smoothedMid + smoothedTreble;
        if (totalEnergy > SILENCE_THRESHOLD) {
            lastSignificantAudioTime = System.currentTimeMillis();
            hasReceivedSignificantAudio = true;
        }

        // Log cada 5 segundos
        long now = System.currentTimeMillis();
        if (now - lastLogTime > 5000) {
            boolean hasSignificant = (totalEnergy > SILENCE_THRESHOLD);
            String audioStatus = hasSignificant ? "✓AUDIO" : "✗silencio";
            if (hasSignificant || (now - lastLogTime > 10000)) {
                Log.d(TAG, String.format("🎵 Bass:%.2f Mid:%.2f Treble:%.2f Energy:%.2f %s",
                    smoothedBass, smoothedMid, smoothedTreble, totalEnergy, audioStatus));
            }
            lastLogTime = now;
        }
    }

    // ========== GETTERS PARA VALORES DE AUDIO ==========

    /**
     * Obtiene las 32 bandas de frecuencia suavizadas
     * Ideal para ecualizadores con muchas barras
     */
    public float[] getFrequencyBands() {
        return smoothedBands;
    }

    /**
     * Obtiene el número de bandas disponibles
     */
    public int getNumBands() {
        return NUM_BANDS;
    }

    /**
     * Obtiene una banda específica (0 a NUM_BANDS-1)
     */
    public float getBand(int index) {
        if (index >= 0 && index < NUM_BANDS) {
            return smoothedBands[index];
        }
        return 0f;
    }

    public float getBassLevel() {
        return smoothedBass;
    }

    public float getMidLevel() {
        return smoothedMid;
    }

    public float getTrebleLevel() {
        return smoothedTreble;
    }

    public float getVolumeLevel() {
        return smoothedVolume;
    }

    public float getBeatIntensity() {
        return beatIntensity;
    }

    public boolean isBeat() {
        return isBeat;
    }

    /**
     * Obtiene un nivel de energía general (combinación de todas las frecuencias)
     */
    public float getEnergyLevel() {
        return (smoothedBass * 0.5f + smoothedMid * 0.3f + smoothedTreble * 0.2f);
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Verifica si el visualizer está realmente recibiendo audio SIGNIFICATIVO
     * Retorna false si solo está recibiendo silencio
     */
    public boolean isReceivingAudio() {
        if (!isEnabled || !hasReceivedData) return false;

        // Verificar que haya recibido datos recientes
        long timeSinceLastData = System.currentTimeMillis() - lastAudioDataTime;
        if (timeSinceLastData >= AUDIO_TIMEOUT_MS) return false;

        // IMPORTANTE: Verificar que haya recibido audio REAL, no solo silencio
        // Si nunca ha recibido audio significativo, considerarlo como "no recibiendo"
        if (!hasReceivedSignificantAudio) {
            // Log para debug
            if (hasReceivedData && System.currentTimeMillis() % 5000 < 50) {
                Log.d(TAG, "[MusicVisualizer] ⚠️ Recibiendo datos pero SOLO SILENCIO");
            }
            return false;
        }

        // Verificar que el audio significativo sea reciente (últimos 3 segundos)
        long timeSinceSignificantAudio = System.currentTimeMillis() - lastSignificantAudioTime;
        if (timeSinceSignificantAudio >= 3000) {
            Log.d(TAG, String.format("[MusicVisualizer] ⚠️ Audio perdido hace %.1fs", timeSinceSignificantAudio/1000f));
            return false;
        }

        return true;
    }

    /**
     * Intenta reconectar el visualizer (útil cuando pierde conexión)
     * Retorna true si la reconexión fue exitosa
     * ⚡ Remueve listener correctamente antes de reconectar
     * ⚡ Auto-resume de música si estaba sonando
     */
    public boolean reconnect() {
        Log.d(TAG, "[MusicVisualizer] 🔄 Intentando reconectar...");

        // 🎵 Detectar si hay música antes de desconectar
        boolean wasMusicPlaying = false;
        if (audioManager != null) {
            wasMusicPlaying = audioManager.isMusicActive();
            Log.d(TAG, "[MusicVisualizer] 🎵 Música activa antes de reconectar: " + wasMusicPlaying);
        }

        // Liberar visualizer existente
        if (visualizer != null) {
            try {
                // ⚡ CRÍTICO: Remover listener antes de liberar
                visualizer.setDataCaptureListener(null, 0, false, false);
                visualizer.setEnabled(false);
                visualizer.release();
            } catch (Exception e) {
                Log.w(TAG, "[MusicVisualizer] Error liberando visualizer anterior: " + e.getMessage());
            }
            visualizer = null;
        }

        // Reinicializar flags
        isEnabled = false;
        hasReceivedData = false;
        hasReceivedSignificantAudio = false;
        lastAudioDataTime = System.currentTimeMillis();
        lastSignificantAudioTime = 0;

        // Reinicializar (esto también manejará el auto-resume internamente)
        boolean result = initializeInternal(wasMusicPlaying);
        return result;
    }

    /**
     * Versión interna de initialize que recibe el estado de música previo
     */
    private boolean initializeInternal(boolean wasMusicPlayingBefore) {
        // 🔒 Evitar inicialización concurrente
        synchronized (initLock) {
            if (isInitializing) {
                Log.w(TAG, "[MusicVisualizer] ⚠️ Inicialización (internal) ya en progreso...");
                return false;
            }
            isInitializing = true;
        }

        try {
            // Si no teníamos el estado previo, detectar ahora
            boolean wasMusicPlaying = wasMusicPlayingBefore;
            if (!wasMusicPlaying && audioManager != null) {
                wasMusicPlaying = audioManager.isMusicActive();
            }

            // Crear visualizer usando session ID 0 (mezcla de audio del sistema)
            visualizer = new Visualizer(0);
            visualizer.setCaptureSize(CAPTURE_SIZE);

            visualizer.setDataCaptureListener(
                new Visualizer.OnDataCaptureListener() {
                    @Override
                    public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                        processWaveform(waveform);
                    }

                    @Override
                    public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                        processFft(fft);
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                true,
                true
            );

            visualizer.setEnabled(true);
            isEnabled = true;

            Log.d(TAG, "[MusicVisualizer] ✓ Inicializado correctamente (internal)");

            // 🎵 AUTO-RESUME con reintentos
            // 🚫 DISABLED: if (wasMusicPlaying && context != null) {
                // Log.d(TAG, "[MusicVisualizer] 🎵 Programando auto-resume de música (3 intentos)...");
                // handler.postDelayed(this::sendMediaPlayCommand, 800);
                // handler.postDelayed(this::sendMediaPlayCommand, 1500);
                // handler.postDelayed(this::sendMediaPlayCommand, 2500);
            // }

            isInitializing = false;  // 🔒 Reset flag
            return true;

        } catch (Exception e) {
            Log.e(TAG, "[MusicVisualizer] ✗ Error inicializando (internal): " + e.getMessage());
            isInitializing = false;  // 🔒 Reset flag
            return false;
        }
    }

    /**
     * Libera recursos del visualizer
     * IMPORTANTE: Llamar cuando el wallpaper se pause o destruya
     * ⚡ Remueve listener ANTES de liberar para evitar memory leaks
     */
    public void release() {
        if (visualizer != null) {
            try {
                // ⚡ CRÍTICO: Remover listener ANTES de liberar
                // Evita que el listener siga referenciando este objeto
                visualizer.setDataCaptureListener(null, 0, false, false);
                visualizer.setEnabled(false);
                visualizer.release();
                isEnabled = false;
                Log.d(TAG, "[MusicVisualizer] ✓ Recursos liberados (listener removido)");
            } catch (Exception e) {
                Log.e(TAG, "[MusicVisualizer] Error liberando recursos: " + e.getMessage());
            } finally {
                visualizer = null;  // Asegurar que no queden referencias
            }
        }
    }

    /**
     * Pausa la captura de audio
     * Optimizado para pausas frecuentes (sistema Play/Stop del wallpaper)
     */
    public void pause() {
        if (visualizer != null && isEnabled) {
            try {
                visualizer.setEnabled(false);
                isEnabled = false;
                Log.d(TAG, "[MusicVisualizer] Pausado (ahorrando batería)");
            } catch (IllegalStateException e) {
                Log.w(TAG, "[MusicVisualizer] Error al pausar (ya estaba pausado): " + e.getMessage());
                isEnabled = false;
            }
        }
    }

    /**
     * Reanuda la captura de audio
     * Optimizado para reanudaciones frecuentes con reconexión automática si falla
     */
    public void resume() {
        // 🔧 SIMPLIFICADO: Siempre intentar habilitar, sin importar el estado previo
        // Esto evita problemas con estados inconsistentes después de ciclos rápidos pause/resume

        if (visualizer == null) {
            // Visualizer fue liberado, reinicializar completamente
            Log.d(TAG, "[MusicVisualizer] Reinicializando visualizer (era null)...");
            initialize();
            return;
        }

        try {
            // Siempre intentar habilitar (es idempotente si ya está habilitado)
            if (!visualizer.getEnabled()) {
                visualizer.setEnabled(true);
                Log.d(TAG, "[MusicVisualizer] Reanudado correctamente");
            }
            isEnabled = true;
            // Reset timestamps para evitar falsos positivos de "sin audio"
            lastAudioDataTime = System.currentTimeMillis();
        } catch (IllegalStateException e) {
            Log.w(TAG, "[MusicVisualizer] Error al reanudar: " + e.getMessage());
            // El visualizer puede estar en estado inválido, reconectar
            Log.d(TAG, "[MusicVisualizer] Reconectando por estado inválido...");
            reconnect();
        } catch (Exception e) {
            Log.e(TAG, "[MusicVisualizer] Error inesperado al reanudar: " + e.getMessage());
            // Forzar reinicialización completa
            visualizer = null;
            initialize();
        }
    }

    /**
     * Resetea los valores de audio a cero (útil al pausar para evitar valores residuales)
     */
    public void resetLevels() {
        smoothedBass = 0f;
        smoothedMid = 0f;
        smoothedTreble = 0f;
        smoothedVolume = 0f;
        beatIntensity = 0f;
        isBeat = false;
    }
}
