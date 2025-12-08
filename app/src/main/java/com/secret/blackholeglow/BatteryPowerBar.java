package com.secret.blackholeglow;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.opengl.GLES30;
import android.os.BatteryManager;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Sistema de barra de poder basada en el nivel de batería del dispositivo
 * Cuando la batería está llena, los efectos son más espectaculares
 */
public class BatteryPowerBar implements SceneObject {
    private static final String TAG = "BatteryPowerBar";

    // Estado de batería
    private float batteryLevel = 1.0f;  // 0 a 1
    private boolean isCharging = false;
    private float powerMultiplier = 1.0f;  // Multiplicador de poder para efectos

    // Animación
    private float pulseTime = 0;
    private float glowIntensity = 0;
    private float chargingGlow = 0;  // Efecto especial cuando carga

    // Posición de la barra (esquina superior derecha - más pequeña y elegante)
    private static final float BAR_WIDTH = 0.20f;   // Más angosta
    private static final float BAR_HEIGHT = 0.04f;  // Más delgada
    private static final float BAR_X = 0.78f;       // Posición X
    private static final float BAR_Y = 0.92f;       // Arriba

    // Buffers
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;

    // Shader program
    private int programId;
    private int aPositionLoc;
    private int aColorLoc;
    private int uTimeLoc;
    private int uChargingGlowLoc;
    private int uBatteryLevelLoc;

    // Context para el BroadcastReceiver
    private final Context context;
    private BatteryReceiver batteryReceiver;

    // BroadcastReceiver para monitorear la batería
    private class BatteryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

            if (level != -1 && scale != -1) {
                batteryLevel = (float) level / (float) scale;
            }

            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL;

            // Calcular multiplicador de poder
            if (batteryLevel > 0.8f) {
                powerMultiplier = 1.5f;  // SUPER PODER!
                glowIntensity = 1.0f;
            } else if (batteryLevel > 0.5f) {
                powerMultiplier = 1.2f;  // Poder normal
                glowIntensity = 0.5f;
            } else if (batteryLevel > 0.2f) {
                powerMultiplier = 0.8f;  // Poder reducido
                glowIntensity = 0.2f;
            } else {
                powerMultiplier = 0.5f;  // Poder mínimo
                glowIntensity = 0.1f;
            }

            Log.d(TAG, String.format("Battery: %.0f%% | Charging: %s | Power: x%.1f",
                batteryLevel * 100, isCharging, powerMultiplier));

            updateBuffers();
        }
    }

    public BatteryPowerBar(Context context) {
        this.context = context;

        // Registrar receiver de batería
        batteryReceiver = new BatteryReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(batteryReceiver, filter);

        // Obtener estado inicial
        if (batteryStatus != null) {
            batteryReceiver.onReceive(context, batteryStatus);
        }

        // Crear shader program desde archivos
        programId = ShaderUtils.createProgramFromAssets(context,
            "shaders/battery_vertex.glsl",
            "shaders/battery_fragment.glsl");
        aPositionLoc = GLES30.glGetAttribLocation(programId, "a_Position");
        aColorLoc = GLES30.glGetAttribLocation(programId, "a_Color");
        uTimeLoc = GLES30.glGetUniformLocation(programId, "u_Time");
        uChargingGlowLoc = GLES30.glGetUniformLocation(programId, "u_ChargingGlow");
        uBatteryLevelLoc = GLES30.glGetUniformLocation(programId, "u_BatteryLevel");

        // Crear buffers iniciales
        createBuffers();
        updateBuffers();

        Log.d(TAG, "Battery power bar initialized");
    }

    private void createBuffers() {
        // Vértices para el marco y la barra de relleno
        float[] vertices = new float[24];  // 12 vértices * 2 coords

        // Marco exterior (4 vértices)
        vertices[0] = BAR_X - BAR_WIDTH/2; vertices[1] = BAR_Y - BAR_HEIGHT/2;
        vertices[2] = BAR_X + BAR_WIDTH/2; vertices[3] = BAR_Y - BAR_HEIGHT/2;
        vertices[4] = BAR_X + BAR_WIDTH/2; vertices[5] = BAR_Y + BAR_HEIGHT/2;
        vertices[6] = BAR_X - BAR_WIDTH/2; vertices[7] = BAR_Y + BAR_HEIGHT/2;

        // Barra de relleno (8 vértices más)
        float padding = 0.01f;
        for (int i = 0; i < 8; i++) {
            vertices[8 + i*2] = vertices[i*2 % 8];
            vertices[8 + i*2 + 1] = vertices[i*2 % 8 + 1];
        }

        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
    }

    private void updateBuffers() {
        // Actualizar colores basados en el nivel de batería
        float[] colors = new float[48];  // 12 vértices * 4 colores

        // Color del marco (gris oscuro)
        for (int i = 0; i < 4; i++) {
            colors[i*4] = 0.2f;
            colors[i*4 + 1] = 0.2f;
            colors[i*4 + 2] = 0.2f;
            colors[i*4 + 3] = 0.8f;
        }

        // Color del relleno (cambia según el nivel)
        float r, g, b;
        if (batteryLevel > 0.8f) {
            // Verde brillante con efecto dorado cuando está lleno
            r = 0.2f + glowIntensity * 0.8f;
            g = 1.0f;
            b = 0.2f + (isCharging ? 0.3f : 0);
        } else if (batteryLevel > 0.5f) {
            // Verde normal
            r = 0.2f;
            g = 0.8f;
            b = 0.2f;
        } else if (batteryLevel > 0.2f) {
            // Amarillo
            r = 1.0f;
            g = 0.8f;
            b = 0.2f;
        } else {
            // Rojo peligro
            r = 1.0f;
            g = 0.2f;
            b = 0.2f;
        }

        // Aplicar color al relleno
        for (int i = 4; i < 12; i++) {
            colors[i*4] = r;
            colors[i*4 + 1] = g;
            colors[i*4 + 2] = b;
            colors[i*4 + 3] = 0.9f;
        }

        if (colorBuffer == null) {
            ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
            cbb.order(ByteOrder.nativeOrder());
            colorBuffer = cbb.asFloatBuffer();
        }
        colorBuffer.clear();
        colorBuffer.put(colors);
        colorBuffer.position(0);
    }

    @Override
    public void update(float deltaTime) {
        pulseTime += deltaTime;

        // Actualizar efectos especiales si está cargando
        if (isCharging) {
            glowIntensity = Math.min(1.0f, glowIntensity + deltaTime);
            chargingGlow = Math.min(1.0f, chargingGlow + deltaTime * 2.0f);  // Efecto rápido
        } else {
            glowIntensity = Math.max(0.0f, glowIntensity - deltaTime * 0.5f);
            chargingGlow = Math.max(0.0f, chargingGlow - deltaTime * 3.0f);  // Desvanece rápido
        }
    }

    @Override
    public void draw() {
        GLES30.glUseProgram(programId);

        // Configurar blending
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Desactivar depth test para UI
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

        // Configurar uniforms
        GLES30.glUniform1f(uTimeLoc, pulseTime);
        GLES30.glUniform1f(uChargingGlowLoc, chargingGlow);
        GLES30.glUniform1f(uBatteryLevelLoc, batteryLevel);

        // Configurar atributos
        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        GLES30.glEnableVertexAttribArray(aColorLoc);
        GLES30.glVertexAttribPointer(aColorLoc, 4, GLES30.GL_FLOAT, false, 0, colorBuffer);

        // Dibujar marco
        GLES30.glDrawArrays(GLES30.GL_LINE_LOOP, 0, 4);

        // Dibujar relleno (solo la parte proporcional al nivel de batería)
        if (batteryLevel > 0.01f) {
            float fillWidth = BAR_WIDTH * batteryLevel;
            // Actualizar vértices del relleno
            float[] fillVertices = new float[8];
            fillVertices[0] = BAR_X - BAR_WIDTH/2 + 0.01f;
            fillVertices[1] = BAR_Y - BAR_HEIGHT/2 + 0.01f;
            fillVertices[2] = BAR_X - BAR_WIDTH/2 + fillWidth - 0.01f;
            fillVertices[3] = BAR_Y - BAR_HEIGHT/2 + 0.01f;
            fillVertices[4] = BAR_X - BAR_WIDTH/2 + fillWidth - 0.01f;
            fillVertices[5] = BAR_Y + BAR_HEIGHT/2 - 0.01f;
            fillVertices[6] = BAR_X - BAR_WIDTH/2 + 0.01f;
            fillVertices[7] = BAR_Y + BAR_HEIGHT/2 - 0.01f;

            vertexBuffer.position(8);
            vertexBuffer.put(fillVertices);
            vertexBuffer.position(8);

            colorBuffer.position(16);
            GLES30.glVertexAttribPointer(aColorLoc, 4, GLES30.GL_FLOAT, false, 0, colorBuffer);
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 4);
        }

        // Limpiar
        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aColorLoc);

        // Restaurar estados
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }

    /**
     * Obtiene el multiplicador de poder actual para afectar otros efectos
     */
    public float getPowerMultiplier() {
        return powerMultiplier;
    }

    public float getBatteryLevel() {
        return batteryLevel;
    }

    public boolean isCharging() {
        return isCharging;
    }

    /**
     * Limpieza al destruir
     */
    public void release() {
        if (batteryReceiver != null) {
            try {
                context.unregisterReceiver(batteryReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering battery receiver", e);
            }
        }
    }
}