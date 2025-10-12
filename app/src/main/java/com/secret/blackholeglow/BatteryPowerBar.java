package com.secret.blackholeglow;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.opengl.GLES20;
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

    // Posición de la barra (esquina superior derecha)
    private static final float BAR_WIDTH = 0.3f;
    private static final float BAR_HEIGHT = 0.05f;
    private static final float BAR_X = 0.65f;
    private static final float BAR_Y = 0.9f;

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

    // Vertex shader para la barra
    private static final String VERTEX_SHADER =
        "attribute vec2 a_Position;\n" +
        "attribute vec4 a_Color;\n" +
        "varying vec4 v_Color;\n" +
        "void main() {\n" +
        "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
        "    v_Color = a_Color;\n" +
        "}\n";

    // Fragment shader con efectos de poder y carga
    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "uniform float u_Time;\n" +
        "uniform float u_ChargingGlow;\n" +
        "uniform float u_BatteryLevel;\n" +
        "varying vec4 v_Color;\n" +
        "void main() {\n" +
        "    vec3 finalColor = v_Color.rgb;\n" +
        "    \n" +
        "    // Efecto de carga - ondas de energía\n" +
        "    if (u_ChargingGlow > 0.0) {\n" +
        "        // Onda de energía que recorre la barra\n" +
        "        float wave = sin(u_Time * 5.0 - gl_FragCoord.x * 0.02) * 0.5 + 0.5;\n" +
        "        wave *= u_ChargingGlow;\n" +
        "        \n" +
        "        // Brillo pulsante\n" +
        "        float pulse = sin(u_Time * 8.0) * 0.3 + 0.7;\n" +
        "        \n" +
        "        // Color de carga (dorado/blanco brillante)\n" +
        "        vec3 chargeColor = vec3(1.0, 0.9, 0.5) * wave * pulse;\n" +
        "        finalColor += chargeColor * u_ChargingGlow;\n" +
        "        \n" +
        "        // Destellos ocasionales\n" +
        "        float sparkle = sin(u_Time * 30.0 + gl_FragCoord.x) * sin(u_Time * 20.0 + gl_FragCoord.y);\n" +
        "        if (sparkle > 0.8) {\n" +
        "            finalColor += vec3(1.0, 1.0, 0.8) * u_ChargingGlow;\n" +
        "        }\n" +
        "    }\n" +
        "    \n" +
        "    // Pulsación cuando está lleno\n" +
        "    if (u_BatteryLevel > 0.95) {\n" +
        "        float fullPulse = sin(u_Time * 4.0) * 0.2 + 0.8;\n" +
        "        finalColor *= fullPulse;\n" +
        "    }\n" +
        "    \n" +
        "    // Efecto de energía sutil\n" +
        "    float energy = sin(u_Time * 10.0 + gl_FragCoord.x * 0.1) * 0.05;\n" +
        "    finalColor += vec3(energy) * v_Color.a;\n" +
        "    \n" +
        "    gl_FragColor = vec4(finalColor, v_Color.a);\n" +
        "}\n";

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

        // Crear shader program
        programId = ShaderUtils.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aColorLoc = GLES20.glGetAttribLocation(programId, "a_Color");
        uTimeLoc = GLES20.glGetUniformLocation(programId, "u_Time");
        uChargingGlowLoc = GLES20.glGetUniformLocation(programId, "u_ChargingGlow");
        uBatteryLevelLoc = GLES20.glGetUniformLocation(programId, "u_BatteryLevel");

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
        GLES20.glUseProgram(programId);

        // Configurar blending
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Desactivar depth test para UI
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        // Configurar uniforms
        GLES20.glUniform1f(uTimeLoc, pulseTime);
        GLES20.glUniform1f(uChargingGlowLoc, chargingGlow);
        GLES20.glUniform1f(uBatteryLevelLoc, batteryLevel);

        // Configurar atributos
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aColorLoc);
        GLES20.glVertexAttribPointer(aColorLoc, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);

        // Dibujar marco
        GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, 4);

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
            GLES20.glVertexAttribPointer(aColorLoc, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
        }

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aColorLoc);

        // Restaurar estados
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
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