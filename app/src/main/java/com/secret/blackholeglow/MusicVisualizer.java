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
     */
    public boolean reconnect() {
        Log.d(TAG, "[MusicVisualizer] 🔄 Intentando reconectar...");

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

        return initialize();
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
        if (visualizer == null) {
            // Visualizer fue liberado, reinicializar
            Log.d(TAG, "[MusicVisualizer] Reinicializando visualizer (era null)...");
            initialize();
            return;
        }

        if (!isEnabled) {
            try {
                visualizer.setEnabled(true);
                isEnabled = true;
                // Reset timestamps para evitar falsos positivos de "sin audio"
                lastAudioDataTime = System.currentTimeMillis();
                Log.d(TAG, "[MusicVisualizer] Reanudado correctamente");
            } catch (IllegalStateException e) {
                Log.w(TAG, "[MusicVisualizer] Error al reanudar: " + e.getMessage());
                // El visualizer puede estar en estado inválido, reconectar
                Log.d(TAG, "[MusicVisualizer] Intentando reconectar...");
                reconnect();
            } catch (Exception e) {
                Log.e(TAG, "[MusicVisualizer] Error inesperado al reanudar: " + e.getMessage());
                // Forzar reinicialización completa
                visualizer = null;
                initialize();
            }
        } else {
            Log.d(TAG, "[MusicVisualizer] Ya estaba habilitado, no hace falta reanudar");
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
