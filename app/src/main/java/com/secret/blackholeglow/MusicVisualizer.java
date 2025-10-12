package com.secret.blackholeglow;

import android.media.AudioManager;
import android.media.audiofx.Visualizer;
import android.util.Log;

/**
 * MusicVisualizer - Sistema de captura y análisis de audio en tiempo real
 * Captura el audio del sistema y extrae datos de frecuencias para efectos visuales
 *
 * IMPORTANTE: NO GRABA AUDIO, solo lee datos para visualización
 */
public class MusicVisualizer {
    private static final String TAG = "depurar";

    // Visualizer de Android
    private Visualizer visualizer;
    private boolean isEnabled = false;

    // Datos de audio extraídos
    private float bassLevel = 0f;        // Bajos (0.0 - 1.0)
    private float midLevel = 0f;         // Medios (0.0 - 1.0)
    private float trebleLevel = 0f;      // Agudos (0.0 - 1.0)
    private float volumeLevel = 0f;      // Volumen general (0.0 - 1.0)

    // Detección de beats
    private float beatIntensity = 0f;    // Intensidad del beat actual
    private boolean isBeat = false;      // ¿Hay un beat ahora?
    private long lastBeatTime = 0;

    // Configuración
    private static final int CAPTURE_SIZE = 512;  // Tamaño de captura (debe ser potencia de 2)
    private static final float BEAT_THRESHOLD = 1.5f;  // Umbral para detectar beats
    private static final int MIN_BEAT_INTERVAL_MS = 200;  // Mínimo tiempo entre beats

    // Suavizado de valores (para evitar cambios bruscos)
    private static final float SMOOTHING_FACTOR = 0.3f;
    private float smoothedBass = 0f;
    private float smoothedMid = 0f;
    private float smoothedTreble = 0f;
    private float smoothedVolume = 0f;

    // Estados para debugging
    private int captureCount = 0;
    private long lastLogTime = 0;

    // Monitoreo de conexión (detectar si perdimos audio)
    private long lastAudioDataTime = 0;
    private long lastSignificantAudioTime = 0;  // Última vez que recibimos audio REAL (no silencio)
    private static final long AUDIO_TIMEOUT_MS = 5000;  // 5 segundos sin datos = reconectar
    private static final float SILENCE_THRESHOLD = 0.02f;  // Umbral MUY BAJO para detectar cualquier audio
    private boolean hasReceivedData = false;
    private boolean hasReceivedSignificantAudio = false;

    /**
     * Constructor - Inicializa el visualizador
     */
    public MusicVisualizer() {
        Log.d(TAG, "[MusicVisualizer] Inicializando...");
    }

    /**
     * Inicializa y activa el visualizador de audio
     * Debe llamarse desde el hilo de OpenGL o UI thread
     */
    public boolean initialize() {
        try {
            // Crear visualizer usando session ID 0 (mezcla de audio del sistema)
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

            return true;

        } catch (Exception e) {
            Log.e(TAG, "[MusicVisualizer] ✗ Error inicializando: " + e.getMessage());
            Log.e(TAG, "[MusicVisualizer] Es posible que falten permisos de audio");
            return false;
        }
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
     * Procesa datos FFT para extraer niveles de frecuencias (bass, mid, treble)
     * FFT data format: [0]=DC, [1]=real[1], [2]=imag[1], ..., [n-2]=real[n/2], [n-1]=imag[n/2]
     */
    private void processFft(byte[] fft) {
        if (fft == null || fft.length < 4) return;

        // Rangos de frecuencias (índices en el array FFT)
        // Nota: La frecuencia = (índice * sampleRate) / captureSize
        // Asumiendo sampleRate ~44100 Hz, captureSize=512

        int bassEnd = 8;       // ~0-690 Hz (bajos profundos)
        int midEnd = 32;       // ~690-2760 Hz (medios)
        int trebleEnd = 64;    // ~2760-5520 Hz (agudos)

        float bassSum = 0f;
        float midSum = 0f;
        float trebleSum = 0f;

        // Calcular magnitud para cada rango de frecuencias
        // Magnitud = sqrt(real^2 + imag^2)

        // BAJOS (índices 2 a bassEnd)
        for (int i = 2; i < Math.min(bassEnd * 2, fft.length); i += 2) {
            float real = (float) fft[i];
            float imag = (i + 1 < fft.length) ? (float) fft[i + 1] : 0f;
            bassSum += Math.sqrt(real * real + imag * imag);
        }

        // MEDIOS (índices bassEnd a midEnd)
        for (int i = bassEnd * 2; i < Math.min(midEnd * 2, fft.length); i += 2) {
            float real = (float) fft[i];
            float imag = (i + 1 < fft.length) ? (float) fft[i + 1] : 0f;
            midSum += Math.sqrt(real * real + imag * imag);
        }

        // AGUDOS (índices midEnd a trebleEnd)
        for (int i = midEnd * 2; i < Math.min(trebleEnd * 2, fft.length); i += 2) {
            float real = (float) fft[i];
            float imag = (i + 1 < fft.length) ? (float) fft[i + 1] : 0f;
            trebleSum += Math.sqrt(real * real + imag * imag);
        }

        // Normalizar (dividir por número de muestras y escalar)
        bassLevel = Math.min(1.0f, bassSum / (bassEnd * 128f));
        midLevel = Math.min(1.0f, midSum / ((midEnd - bassEnd) * 128f));
        trebleLevel = Math.min(1.0f, trebleSum / ((trebleEnd - midEnd) * 128f));

        // Suavizar valores
        smoothedBass = smoothedBass * (1f - SMOOTHING_FACTOR) + bassLevel * SMOOTHING_FACTOR;
        smoothedMid = smoothedMid * (1f - SMOOTHING_FACTOR) + midLevel * SMOOTHING_FACTOR;
        smoothedTreble = smoothedTreble * (1f - SMOOTHING_FACTOR) + trebleLevel * SMOOTHING_FACTOR;

        // Detectar audio significativo (no solo silencio)
        float totalEnergy = smoothedBass + smoothedMid + smoothedTreble;
        if (totalEnergy > SILENCE_THRESHOLD) {
            lastSignificantAudioTime = System.currentTimeMillis();
            hasReceivedSignificantAudio = true;
        }

        // Log SOLO cada 5 segundos para evitar overhead
        long now = System.currentTimeMillis();
        if (now - lastLogTime > 5000) {
            boolean hasSignificant = (totalEnergy > SILENCE_THRESHOLD);
            String audioStatus = hasSignificant ? "✓AUDIO" : "✗silencio";

            // Log simplificado
            if (hasSignificant || (now - lastLogTime > 10000)) {  // Log silencio cada 10s
                Log.d(TAG, String.format("🎵 Bass:%.2f Mid:%.2f Treble:%.2f Energy:%.2f %s",
                    smoothedBass, smoothedMid, smoothedTreble, totalEnergy, audioStatus));
            }
            lastLogTime = now;
        }
    }

    // ========== GETTERS PARA VALORES DE AUDIO ==========

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
     */
    public boolean reconnect() {
        Log.d(TAG, "[MusicVisualizer] 🔄 Intentando reconectar...");

        // Liberar visualizer existente
        if (visualizer != null) {
            try {
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

        return initialize();
    }

    /**
     * Libera recursos del visualizer
     * IMPORTANTE: Llamar cuando el wallpaper se pause o destruya
     */
    public void release() {
        if (visualizer != null) {
            try {
                visualizer.setEnabled(false);
                visualizer.release();
                isEnabled = false;
                Log.d(TAG, "[MusicVisualizer] ✓ Recursos liberados");
            } catch (Exception e) {
                Log.e(TAG, "[MusicVisualizer] Error liberando recursos: " + e.getMessage());
            }
        }
    }

    /**
     * Pausa la captura de audio
     */
    public void pause() {
        if (visualizer != null && isEnabled) {
            visualizer.setEnabled(false);
            isEnabled = false;
            Log.d(TAG, "[MusicVisualizer] Pausado");
        }
    }

    /**
     * Reanuda la captura de audio
     */
    public void resume() {
        if (visualizer != null && !isEnabled) {
            visualizer.setEnabled(true);
            isEnabled = true;
            Log.d(TAG, "[MusicVisualizer] Reanudado");
        }
    }
}
