package com.secret.blackholeglow;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * 🎮 SISTEMA DE ESTADÍSTICAS DEL JUGADOR
 *
 * Registra TODOS los logros épicos del jugador:
 * - Meteoritos disparados
 * - Impactos en el sol/campo de fuerza
 * - Soles destruidos
 * - Combo máximo
 * - Tiempo de juego total
 * - Puntuación acumulada
 *
 * ⚡ OPTIMIZADO: Sin overhead, guardado eficiente
 */
public class PlayerStats {
    private static final String TAG = "PlayerStats";
    private static final String PREFS_NAME = "blackholeglow_stats";

    // Contador de eventos
    private int totalMeteorsLaunched = 0;      // Total de meteoritos disparados
    private int totalImpacts = 0;               // Total de impactos
    private int totalSunImpacts = 0;            // Impactos al sol
    private int totalForceFieldImpacts = 0;     // Impactos al campo de fuerza
    private int sunsDestroyed = 0;              // Soles destruidos (HP = 0)

    // Sistema de combos
    private int currentCombo = 0;               // Combo actual
    private int maxCombo = 0;                   // Combo máximo alcanzado
    private long lastImpactTime = 0;            // Timestamp del último impacto
    private static final long COMBO_TIMEOUT = 5000; // 5 segundos para mantener combo (incrementado)

    // Sistema de límite de duración del combo
    private long comboStartTime = 0;            // Cuándo empezó el combo actual
    private static final long MAX_COMBO_DURATION = 10000; // 10 segundos máximo de duración
    private static final long COMBO_COOLDOWN = 2000;     // 2 segundos de cooldown después
    private long comboCooldownEndTime = 0;     // Cuándo termina el cooldown
    private boolean comboInCooldown = false;   // Si el combo está en cooldown

    // Puntuación
    private int totalScore = 0;
    private static final int POINTS_PER_IMPACT = 10;
    private static final int POINTS_COMBO_2X = 25;
    private static final int POINTS_COMBO_3X = 50;
    private static final int POINTS_SUN_DESTROYED = 1000;

    // Tiempo de juego
    private long totalPlayTimeMs = 0;
    private long sessionStartTime = 0;

    // Contexto para guardar
    private Context context;

    // Singleton
    private static PlayerStats instance;

    // 🔐 Firebase Stats Manager (sincronización en la nube)
    private FirebaseStatsManager firebaseManager;

    // ⏰ Sincronización periódica con Firebase
    private long lastFirebaseSync = 0;
    private static final long FIREBASE_SYNC_INTERVAL = 60000; // 1 minuto en ms

    private PlayerStats(Context context) {
        this.context = context.getApplicationContext();
        this.firebaseManager = FirebaseStatsManager.getInstance();
        loadStats();
        syncWithFirebase();  // Sincronizar al iniciar
        lastFirebaseSync = System.currentTimeMillis();
    }

    public static PlayerStats getInstance(Context context) {
        if (instance == null) {
            instance = new PlayerStats(context);
        }
        return instance;
    }

    /**
     * 🎯 REGISTRAR DISPARO DE METEORITO
     */
    public void onMeteorLaunched() {
        totalMeteorsLaunched++;
        Log.d(TAG, "🚀 Meteorito #" + totalMeteorsLaunched + " disparado");
    }

    /**
     * 💥 REGISTRAR IMPACTO
     * @param onSun true si impactó el sol, false si fue campo de fuerza
     * @return puntos ganados por este impacto
     */
    public int onImpact(boolean onSun) {
        totalImpacts++;

        if (onSun) {
            totalSunImpacts++;
        } else {
            totalForceFieldImpacts++;
        }

        // Verificar si estamos en cooldown
        long now = System.currentTimeMillis();
        if (comboInCooldown) {
            if (now < comboCooldownEndTime) {
                // Aún en cooldown, no contar este impacto para el combo
                Log.d(TAG, String.format("⏳ COOLDOWN ACTIVO! Quedan %.1fs - impacto no cuenta para combo",
                        (comboCooldownEndTime - now) / 1000.0f));
                return POINTS_PER_IMPACT;
            } else {
                // Cooldown terminó
                comboInCooldown = false;
                Log.d(TAG, "✓ Cooldown terminado - combos habilitados nuevamente");
            }
        }

        // Calcular combo
        if (now - lastImpactTime < COMBO_TIMEOUT) {
            // Verificar límite de duración del combo
            long comboDuration = now - comboStartTime;
            if (comboDuration > MAX_COMBO_DURATION) {
                // Combo expiró por tiempo máximo
                Log.d(TAG, "⏱️ COMBO EXPIRADO! Duración máxima alcanzada (10 segundos)");
                resetComboWithCooldown(now);
                currentCombo = 1;
                comboStartTime = now;
            } else {
                currentCombo++;
            }
        } else {
            // Nuevo combo
            currentCombo = 1;
            comboStartTime = now;
        }
        lastImpactTime = now;

        // Actualizar combo máximo
        if (currentCombo > maxCombo) {
            maxCombo = currentCombo;
        }

        // Calcular puntos según combo
        int points = POINTS_PER_IMPACT;
        if (currentCombo >= 5) {
            points = POINTS_COMBO_3X;
        } else if (currentCombo >= 3) {
            points = POINTS_COMBO_2X;
        }

        totalScore += points;

        Log.d(TAG, String.format("💥 IMPACTO! Combo: x%d | +%d pts | Total: %d pts",
                                 currentCombo, points, totalScore));

        return points;
    }

    /**
     * 🔒 Resetea el combo y activa el cooldown
     */
    private void resetComboWithCooldown(long now) {
        currentCombo = 0;
        comboInCooldown = true;
        comboCooldownEndTime = now + COMBO_COOLDOWN;
        Log.d(TAG, "🔒 Combo reseteado - Cooldown de 2 segundos activado");
    }

    /**
     * ☀️ REGISTRAR SOL DESTRUIDO
     */
    public void onSunDestroyed() {
        sunsDestroyed++;
        totalScore += POINTS_SUN_DESTROYED;

        Log.d(TAG, "╔═══════════════════════════════════════╗");
        Log.d(TAG, "║  ☀️💥 SOL DESTRUIDO! BONUS: +1000 pts ║");
        Log.d(TAG, "║  Total soles: " + sunsDestroyed + "                        ║");
        Log.d(TAG, "╚═══════════════════════════════════════╝");

        saveStats(); // Guardar inmediatamente este logro épico

        // 🔐 Sincronizar con Firebase (nube)
        if (firebaseManager != null) {
            firebaseManager.incrementSunsDestroyed(sunsDestroyed);
        }
    }

    /**
     * ⏱️ INICIAR SESIÓN DE JUEGO
     */
    public void startSession() {
        sessionStartTime = System.currentTimeMillis();
        Log.d(TAG, "⏱️ Sesión iniciada");
    }

    /**
     * ⏱️ FINALIZAR SESIÓN DE JUEGO
     */
    public void endSession() {
        if (sessionStartTime > 0) {
            long sessionDuration = System.currentTimeMillis() - sessionStartTime;
            totalPlayTimeMs += sessionDuration;
            sessionStartTime = 0;
            saveStats();
            Log.d(TAG, "⏱️ Sesión finalizada. Tiempo total: " + (totalPlayTimeMs / 1000) + " segundos");
        }
    }

    /**
     * 💾 GUARDAR ESTADÍSTICAS
     */
    public void saveStats() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt("totalMeteorsLaunched", totalMeteorsLaunched);
        editor.putInt("totalImpacts", totalImpacts);
        editor.putInt("totalSunImpacts", totalSunImpacts);
        editor.putInt("totalForceFieldImpacts", totalForceFieldImpacts);
        editor.putInt("sunsDestroyed", sunsDestroyed);
        editor.putInt("maxCombo", maxCombo);
        editor.putInt("totalScore", totalScore);
        editor.putLong("totalPlayTimeMs", totalPlayTimeMs);

        editor.apply();

        Log.d(TAG, "💾 Estadísticas guardadas");
    }

    /**
     * 📂 CARGAR ESTADÍSTICAS
     */
    private void loadStats() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        totalMeteorsLaunched = prefs.getInt("totalMeteorsLaunched", 0);
        totalImpacts = prefs.getInt("totalImpacts", 0);
        totalSunImpacts = prefs.getInt("totalSunImpacts", 0);
        totalForceFieldImpacts = prefs.getInt("totalForceFieldImpacts", 0);
        sunsDestroyed = prefs.getInt("sunsDestroyed", 0);
        maxCombo = prefs.getInt("maxCombo", 0);
        totalScore = prefs.getInt("totalScore", 0);
        totalPlayTimeMs = prefs.getLong("totalPlayTimeMs", 0);

        Log.d(TAG, "📂 Estadísticas cargadas: " + totalScore + " pts, " + sunsDestroyed + " soles destruidos");
    }

    /**
     * 📊 IMPRIMIR ESTADÍSTICAS
     */
    public void printStats() {
        Log.d(TAG, "╔═══════════════════════════════════════════════╗");
        Log.d(TAG, "║       🎮 ESTADÍSTICAS DEL JUGADOR 🎮         ║");
        Log.d(TAG, "╠═══════════════════════════════════════════════╣");
        Log.d(TAG, String.format("║ 🚀 Meteoritos disparados:  %-18d║", totalMeteorsLaunched));
        Log.d(TAG, String.format("║ 💥 Impactos totales:       %-18d║", totalImpacts));
        Log.d(TAG, String.format("║ ☀️  Impactos al sol:       %-18d║", totalSunImpacts));
        Log.d(TAG, String.format("║ 🛡️  Impactos campo fuerza: %-18d║", totalForceFieldImpacts));
        Log.d(TAG, String.format("║ 💀 Soles destruidos:       %-18d║", sunsDestroyed));
        Log.d(TAG, String.format("║ 🔥 Combo máximo:           x%-17d║", maxCombo));
        Log.d(TAG, String.format("║ 🏆 Puntuación total:       %-18d║", totalScore));
        Log.d(TAG, String.format("║ ⏱️  Tiempo jugado:         %-13s seg ║", totalPlayTimeMs / 1000));
        Log.d(TAG, "╚═══════════════════════════════════════════════╝");
    }

    // ===== GETTERS =====
    public int getTotalMeteorsLaunched() { return totalMeteorsLaunched; }
    public int getTotalImpacts() { return totalImpacts; }
    public int getTotalSunImpacts() { return totalSunImpacts; }
    public int getTotalForceFieldImpacts() { return totalForceFieldImpacts; }
    public int getSunsDestroyed() { return sunsDestroyed; }
    public int getCurrentCombo() { return currentCombo; }
    public int getMaxCombo() { return maxCombo; }
    public int getTotalScore() { return totalScore; }
    public long getTotalPlayTimeMs() { return totalPlayTimeMs; }

    /**
     * 🔄 RESETEAR COMBO (cuando pasa mucho tiempo sin impactar o excede duración máxima)
     */
    public void updateCombo() {
        long now = System.currentTimeMillis();

        // Verificar cooldown
        if (comboInCooldown && now >= comboCooldownEndTime) {
            comboInCooldown = false;
        }

        // Verificar timeout normal
        if (now - lastImpactTime > COMBO_TIMEOUT) {
            if (currentCombo > 0) {
                Log.d(TAG, "⏱️ Combo perdido (timeout)");
                currentCombo = 0;
            }
        }

        // Verificar límite de duración (si hay combo activo)
        if (currentCombo > 0 && comboStartTime > 0) {
            long comboDuration = now - comboStartTime;
            if (comboDuration > MAX_COMBO_DURATION) {
                Log.d(TAG, "⏱️ COMBO EXPIRADO por duración máxima (10 segundos)");
                resetComboWithCooldown(now);
            }
        }

        // ⏰ SINCRONIZACIÓN PERIÓDICA CON FIREBASE (cada minuto)
        if (now - lastFirebaseSync >= FIREBASE_SYNC_INTERVAL) {
            syncStatsToFirebase();
            lastFirebaseSync = now;
        }
    }

    /**
     * 📤 Sincroniza estadísticas a Firebase (sin callback)
     * Se llama periódicamente cada minuto
     */
    private void syncStatsToFirebase() {
        if (firebaseManager != null && sunsDestroyed > 0) {
            firebaseManager.incrementSunsDestroyed(sunsDestroyed);
            Log.d(TAG, "⏰ Sincronización periódica con Firebase: " + sunsDestroyed + " soles");
        }
    }

    /**
     * 🌟 REGISTRA UN ATAQUE ESPECIAL (disparo épico del jugador)
     */
    public void onSpecialAttack(String attackType) {
        Log.d(TAG, "╔═══════════════════════════════════════════════╗");
        Log.d(TAG, "║  🌟💥 ATAQUE ESPECIAL: " + attackType + " 💥🌟");
        Log.d(TAG, "║  ¡Bonus de " + (currentCombo * 100) + " puntos!         ");
        Log.d(TAG, "╚═══════════════════════════════════════════════╝");

        // Dar puntos bonus por usar el ataque especial
        totalScore += currentCombo * 100;

        // Activar cooldown de 2 segundos
        long now = System.currentTimeMillis();
        resetComboWithCooldown(now);

        saveStats();
    }

    /**
     * 🔄 SINCRONIZAR CON FIREBASE
     * Compara datos locales con la nube y toma el mayor
     */
    private void syncWithFirebase() {
        if (firebaseManager == null) {
            Log.w(TAG, "⚠️ FirebaseManager no disponible, saltando sincronización");
            return;
        }

        firebaseManager.syncStats(sunsDestroyed, new FirebaseStatsManager.StatsCallback() {
            @Override
            public void onSuccess(int remoteSuns) {
                if (remoteSuns > sunsDestroyed) {
                    // Firebase tiene más - actualizar local
                    Log.d(TAG, "📥 SINCRONIZACIÓN: Firebase tiene " + remoteSuns + " soles, local tiene " + sunsDestroyed);
                    sunsDestroyed = remoteSuns;
                    saveStats();
                    Log.d(TAG, "✅ Estadísticas actualizadas desde Firebase: " + remoteSuns + " soles");

                    // Notificar cambio a quien esté escuchando
                    notifySyncCompleted();
                } else if (sunsDestroyed > 0) {
                    Log.d(TAG, "📊 SINCRONIZACIÓN: Local tiene " + sunsDestroyed + " soles, Firebase tiene " + remoteSuns);
                    Log.d(TAG, "✓ No se necesita actualizar (local >= remoto)");
                } else {
                    Log.d(TAG, "📊 SINCRONIZACIÓN: Primera vez jugando, ambos en 0");
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error sincronizando con Firebase: " + error);
            }
        });
    }

    // Listener para notificar cuando la sincronización termina
    public interface SyncListener {
        void onSyncCompleted(int sunsDestroyed);
    }

    private SyncListener syncListener;

    public void setSyncListener(SyncListener listener) {
        this.syncListener = listener;
    }

    private void notifySyncCompleted() {
        if (syncListener != null) {
            syncListener.onSyncCompleted(sunsDestroyed);
        }
    }
}
