package com.secret.blackholeglow;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

/**
 * 📡 UTILIDAD DE VERIFICACIÓN DE CONECTIVIDAD
 *
 * Verifica si hay conexión a internet antes de intentar
 * operaciones de Firebase para evitar bloqueos del wallpaper.
 *
 * ⚡ OPTIMIZADO para no bloquear el render
 */
public class NetworkUtils {
    private static final String TAG = "NetworkUtils";

    /**
     * 📡 Verifica si hay conexión a internet disponible
     *
     * @param context Contexto de la aplicación
     * @return true si hay conexión, false si no
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) {
            Log.w(TAG, "⚠️ Context es null, asumiendo sin conexión");
            return false;
        }

        try {
            ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (connectivityManager == null) {
                Log.w(TAG, "⚠️ ConnectivityManager no disponible");
                return false;
            }

            // API 23+ (Android 6.0+)
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                Log.d(TAG, "📡 Sin red activa");
                return false;
            }

            NetworkCapabilities capabilities =
                connectivityManager.getNetworkCapabilities(network);

            if (capabilities == null) {
                Log.d(TAG, "📡 Sin capacidades de red");
                return false;
            }

            // Verificar si tiene conexión real (WiFi, Cellular, o Ethernet)
            boolean hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

            if (hasInternet) {
                Log.d(TAG, "✅ Conexión a internet disponible");
            } else {
                Log.d(TAG, "❌ Sin conexión a internet");
            }

            return hasInternet;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error verificando conectividad: " + e.getMessage());
            return false;
        }
    }

    /**
     * 📊 Obtiene el tipo de red actual (para debug)
     */
    public static String getNetworkType(Context context) {
        try {
            ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (cm == null) return "Unknown";

            Network network = cm.getActiveNetwork();
            if (network == null) return "None";

            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            if (capabilities == null) return "None";

            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return "WiFi";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return "Cellular";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return "Ethernet";
            }

            return "Other";
        } catch (Exception e) {
            return "Error";
        }
    }
}
