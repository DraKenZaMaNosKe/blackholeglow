package com.secret.blackholeglow;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   ⚙️ FlightConfig - Configuración externa para parámetros de vuelo      ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  GROK SUGGESTION: Mover constantes a SharedPreferences                  ║
 * ║                                                                          ║
 * ║  BENEFICIOS:                                                             ║
 * ║  • Tweaks sin recompilar                                                ║
 * ║  • Persistencia entre sesiones                                          ║
 * ║  • Posibilidad de UI para ajustes                                       ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class FlightConfig {
    private static final String TAG = "FlightConfig";
    private static final String PREFS_NAME = "flight_config";

    // Keys para SharedPreferences
    private static final String KEY_ORIGIN_X = "origin_x";
    private static final String KEY_ORIGIN_Y = "origin_y";
    private static final String KEY_ORIGIN_Z = "origin_z";
    private static final String KEY_ORIGIN_SCALE = "origin_scale";

    private static final String KEY_FIGURE8_WIDTH = "figure8_width";
    private static final String KEY_FIGURE8_HEIGHT = "figure8_height";
    private static final String KEY_FIGURE8_DEPTH = "figure8_depth";

    private static final String KEY_HOVER_AMPLITUDE = "hover_amplitude";
    private static final String KEY_HOVER_SPEED = "hover_speed";

    private static final String KEY_BASE_ROTATION_Y = "base_rotation_y";
    private static final String KEY_GYRO_X_INFLUENCE = "gyro_x_influence";
    private static final String KEY_GYRO_ROLL_INFLUENCE = "gyro_roll_influence";

    // ═══════════════════════════════════════════════════════════════════════
    // VALORES DEFAULT (los originales de TravelingShip)
    // ═══════════════════════════════════════════════════════════════════════

    public static final float DEFAULT_ORIGIN_X = -2.5f;
    public static final float DEFAULT_ORIGIN_Y = -4.9f;
    public static final float DEFAULT_ORIGIN_Z = -2.5f;
    public static final float DEFAULT_ORIGIN_SCALE = 0.8f;

    public static final float DEFAULT_FIGURE8_WIDTH = 0.8f;
    public static final float DEFAULT_FIGURE8_HEIGHT = 3.5f;
    public static final float DEFAULT_FIGURE8_DEPTH = 4.0f;

    public static final float DEFAULT_HOVER_AMPLITUDE = 0.75f;
    public static final float DEFAULT_HOVER_SPEED = 0.5f;

    public static final float DEFAULT_BASE_ROTATION_Y = 304.7f;
    public static final float DEFAULT_GYRO_X_INFLUENCE = 1.5f;
    public static final float DEFAULT_GYRO_ROLL_INFLUENCE = 15f;

    // ═══════════════════════════════════════════════════════════════════════
    // VALORES ACTUALES
    // ═══════════════════════════════════════════════════════════════════════

    public float originX, originY, originZ, originScale;
    public float figure8Width, figure8Height, figure8Depth;
    public float hoverAmplitude, hoverSpeed;
    public float baseRotationY;
    public float gyroXInfluence, gyroRollInfluence;

    private final SharedPreferences prefs;

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════

    public FlightConfig(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        load();
        Log.d(TAG, "⚙️ FlightConfig cargada");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOAD / SAVE
    // ═══════════════════════════════════════════════════════════════════════

    public void load() {
        originX = prefs.getFloat(KEY_ORIGIN_X, DEFAULT_ORIGIN_X);
        originY = prefs.getFloat(KEY_ORIGIN_Y, DEFAULT_ORIGIN_Y);
        originZ = prefs.getFloat(KEY_ORIGIN_Z, DEFAULT_ORIGIN_Z);
        originScale = prefs.getFloat(KEY_ORIGIN_SCALE, DEFAULT_ORIGIN_SCALE);

        figure8Width = prefs.getFloat(KEY_FIGURE8_WIDTH, DEFAULT_FIGURE8_WIDTH);
        figure8Height = prefs.getFloat(KEY_FIGURE8_HEIGHT, DEFAULT_FIGURE8_HEIGHT);
        figure8Depth = prefs.getFloat(KEY_FIGURE8_DEPTH, DEFAULT_FIGURE8_DEPTH);

        hoverAmplitude = prefs.getFloat(KEY_HOVER_AMPLITUDE, DEFAULT_HOVER_AMPLITUDE);
        hoverSpeed = prefs.getFloat(KEY_HOVER_SPEED, DEFAULT_HOVER_SPEED);

        baseRotationY = prefs.getFloat(KEY_BASE_ROTATION_Y, DEFAULT_BASE_ROTATION_Y);
        gyroXInfluence = prefs.getFloat(KEY_GYRO_X_INFLUENCE, DEFAULT_GYRO_X_INFLUENCE);
        gyroRollInfluence = prefs.getFloat(KEY_GYRO_ROLL_INFLUENCE, DEFAULT_GYRO_ROLL_INFLUENCE);
    }

    public void save() {
        SharedPreferences.Editor editor = prefs.edit();

        editor.putFloat(KEY_ORIGIN_X, originX);
        editor.putFloat(KEY_ORIGIN_Y, originY);
        editor.putFloat(KEY_ORIGIN_Z, originZ);
        editor.putFloat(KEY_ORIGIN_SCALE, originScale);

        editor.putFloat(KEY_FIGURE8_WIDTH, figure8Width);
        editor.putFloat(KEY_FIGURE8_HEIGHT, figure8Height);
        editor.putFloat(KEY_FIGURE8_DEPTH, figure8Depth);

        editor.putFloat(KEY_HOVER_AMPLITUDE, hoverAmplitude);
        editor.putFloat(KEY_HOVER_SPEED, hoverSpeed);

        editor.putFloat(KEY_BASE_ROTATION_Y, baseRotationY);
        editor.putFloat(KEY_GYRO_X_INFLUENCE, gyroXInfluence);
        editor.putFloat(KEY_GYRO_ROLL_INFLUENCE, gyroRollInfluence);

        editor.apply();
        Log.d(TAG, "💾 FlightConfig guardada");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RESET A DEFAULTS
    // ═══════════════════════════════════════════════════════════════════════

    public void resetToDefaults() {
        originX = DEFAULT_ORIGIN_X;
        originY = DEFAULT_ORIGIN_Y;
        originZ = DEFAULT_ORIGIN_Z;
        originScale = DEFAULT_ORIGIN_SCALE;

        figure8Width = DEFAULT_FIGURE8_WIDTH;
        figure8Height = DEFAULT_FIGURE8_HEIGHT;
        figure8Depth = DEFAULT_FIGURE8_DEPTH;

        hoverAmplitude = DEFAULT_HOVER_AMPLITUDE;
        hoverSpeed = DEFAULT_HOVER_SPEED;

        baseRotationY = DEFAULT_BASE_ROTATION_Y;
        gyroXInfluence = DEFAULT_GYRO_X_INFLUENCE;
        gyroRollInfluence = DEFAULT_GYRO_ROLL_INFLUENCE;

        save();
        Log.d(TAG, "🔄 FlightConfig reseteada a defaults");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // APLICAR A FLIGHT CONTROLLER
    // ═══════════════════════════════════════════════════════════════════════

    public void applyTo(FlightController controller) {
        controller.setOrigin(originX, originY, originZ, originScale);
        controller.setFigure8Dimensions(figure8Width, figure8Height, figure8Depth);
        controller.setHoverParams(hoverAmplitude, hoverSpeed);
        controller.setBaseRotationY(baseRotationY);
        Log.d(TAG, "✅ Configuración aplicada a FlightController");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DEBUG
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public String toString() {
        return String.format(
            "FlightConfig{\n" +
            "  origin=(%.2f, %.2f, %.2f) scale=%.2f\n" +
            "  figure8=(%.2f x %.2f x %.2f)\n" +
            "  hover=(amp=%.2f, speed=%.2f)\n" +
            "  rotY=%.1f°, gyro=(%.2f, %.2f)\n" +
            "}",
            originX, originY, originZ, originScale,
            figure8Width, figure8Height, figure8Depth,
            hoverAmplitude, hoverSpeed,
            baseRotationY, gyroXInfluence, gyroRollInfluence
        );
    }
}
