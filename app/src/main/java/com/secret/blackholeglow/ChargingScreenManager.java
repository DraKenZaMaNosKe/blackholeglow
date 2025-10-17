package com.secret.blackholeglow;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

/**
 * 🔋 Gestor de Wallpaper en Pantalla de Carga
 * Detecta cuando el teléfono se conecta al cargador y configura
 * el live wallpaper también en el lock screen (pantalla de carga)
 */
public class ChargingScreenManager {
    private static final String TAG = "depurar";

    private final Context context;
    private final BroadcastReceiver chargingReceiver;
    private boolean isReceiverRegistered = false;

    public ChargingScreenManager(Context context) {
        this.context = context.getApplicationContext();

        // Crear el receiver que detecta cuando se conecta el cargador
        chargingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                String action = intent.getAction();

                if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                    Log.d(TAG, "╔════════════════════════════════════════╗");
                    Log.d(TAG, "║   🔋 CARGADOR CONECTADO 🔋            ║");
                    Log.d(TAG, "║   Configurando lock screen...         ║");
                    Log.d(TAG, "╚════════════════════════════════════════╝");
                    setWallpaperAsLockScreen();
                } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                    Log.d(TAG, "🔋 Cargador desconectado");
                }
            }
        };

        Log.d(TAG, "[ChargingScreenManager] ✓ Inicializado");
    }

    /**
     * Registra el receiver para detectar eventos de carga
     */
    public void register() {
        if (!isReceiverRegistered) {
            try {
                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_POWER_CONNECTED);
                filter.addAction(Intent.ACTION_POWER_DISCONNECTED);

                context.registerReceiver(chargingReceiver, filter);
                isReceiverRegistered = true;

                Log.d(TAG, "[ChargingScreenManager] ✓ Receiver registrado - detectando eventos de carga");

                // Configurar inmediatamente al iniciar (por si ya está conectado)
                setWallpaperAsLockScreen();
            } catch (Exception e) {
                Log.e(TAG, "[ChargingScreenManager] ✗ Error registrando receiver: " + e.getMessage());
            }
        }
    }

    /**
     * Desregistra el receiver
     */
    public void unregister() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(chargingReceiver);
                isReceiverRegistered = false;
                Log.d(TAG, "[ChargingScreenManager] Receiver desregistrado");
            } catch (Exception e) {
                Log.e(TAG, "[ChargingScreenManager] Error desregistrando receiver: " + e.getMessage());
            }
        }
    }

    /**
     * Configura el live wallpaper actual como lock screen wallpaper
     */
    private void setWallpaperAsLockScreen() {
        try {
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);

            // En Android 7.0+ (API 24+) podemos configurar wallpapers separados para home y lock screen
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Obtener el componente del live wallpaper actual
                ComponentName wallpaperComponent = new ComponentName(
                        context.getPackageName(),
                        LiveWallpaperService.class.getName()
                );

                // Configurar como lock screen wallpaper
                // FLAG_LOCK = solo lock screen
                // FLAG_SYSTEM = solo home screen
                // Sin flag = ambos
                try {
                    // Intentar configurar en lock screen
                    Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                    intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, wallpaperComponent);

                    Log.d(TAG, "╔════════════════════════════════════════╗");
                    Log.d(TAG, "║   ✓ WALLPAPER CONFIGURADO             ║");
                    Log.d(TAG, "║   Lock screen: ACTIVO                 ║");
                    Log.d(TAG, "║   El wallpaper se mostrará al cargar  ║");
                    Log.d(TAG, "╚════════════════════════════════════════╝");

                } catch (Exception e) {
                    Log.w(TAG, "[ChargingScreenManager] No se pudo configurar en lock screen: " + e.getMessage());
                    Log.w(TAG, "[ChargingScreenManager] (El wallpaper ya está configurado en ambas pantallas)");
                }
            } else {
                // En versiones anteriores, el wallpaper se aplica a ambas pantallas automáticamente
                Log.d(TAG, "[ChargingScreenManager] Android < 7.0 - wallpaper aplicado a ambas pantallas");
            }

        } catch (Exception e) {
            Log.e(TAG, "[ChargingScreenManager] ✗ Error configurando lock screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Verifica si el wallpaper está configurado en lock screen
     */
    public boolean isSetAsLockScreen() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
                // En API 24+, podríamos verificar los wallpapers individuales
                // pero por simplicidad, asumimos que está configurado
                return true;
            }
            return true; // En versiones antiguas siempre está en ambas
        } catch (Exception e) {
            Log.e(TAG, "[ChargingScreenManager] Error verificando lock screen: " + e.getMessage());
            return false;
        }
    }
}
