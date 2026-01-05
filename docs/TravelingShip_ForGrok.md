# TravelingShip - Documentación Completa para Revisión

## CONTEXTO PARA EL REVISOR (GROK)

Este documento contiene el código fuente completo del sistema de vuelo de una nave 3D en un Live Wallpaper de Android (OpenGL ES 3.0). Necesitamos tu ayuda para:

1. **Revisar los shaders** - Optimizar el fragment shader de llamas y efecto Fresnel
2. **Mejorar el sistema de vuelo** - La figura de 8 podría ser más orgánica
3. **Optimizar el rendimiento** - Reducir cálculos Math.sin/cos
4. **Sugerir mejoras visuales** - Efectos adicionales que podrían verse bien

---

## ARQUITECTURA DEL SISTEMA

```
┌─────────────────────────────────────────────────────────────────┐
│                        LabScene.java                             │
│  (Escena principal del wallpaper PYRALIS)                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   ┌─────────────────┐     ┌─────────────────────┐               │
│   │ GyroscopeManager│────▶│    TravelingShip    │               │
│   │   (Sensores)    │     │   (Nave 3D + Vuelo) │               │
│   └─────────────────┘     └─────────────────────┘               │
│          │                         │                             │
│          │                         ▼                             │
│          │                ┌─────────────────┐                   │
│          │                │  VERTEX SHADER  │                   │
│          │                │  FRAGMENT SHADER│                   │
│          │                │  (Llamas+Fresnel)│                   │
│          │                └─────────────────┘                   │
│          │                         │                             │
│          ▼                         ▼                             │
│   ┌─────────────────────────────────────────┐                   │
│   │            OpenGL ES 3.0 Render          │                   │
│   └─────────────────────────────────────────┘                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 1. TravelingShip.java - CÓDIGO FUENTE COMPLETO

```java
package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.util.ObjLoader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   TravelingShip - Nave viajando hacia el sol (LabScene)                  ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  CARACTERÍSTICAS:                                                         ║
 * ║  • Posición fija en la parte inferior de la pantalla                     ║
 * ║  • Apunta hacia arriba (hacia el sol/destino)                            ║
 * ║  • Sutil animación de balanceo (bobbing)                                 ║
 * ║  • Efecto de propulsión/engine glow                                      ║
 * ║  • Usa human_interceptor_flames.obj (nave + llamas integradas)           ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class TravelingShip implements SceneObject, CameraAware {
    private static final String TAG = "TravelingShip";

    private final Context context;
    private final TextureLoader textureLoader;

    // Buffers del modelo
    private FloatBuffer vertexBuffer;
    private FloatBuffer uvBuffer;
    private IntBuffer indexBuffer;
    private int indexCount;

    // Textura
    private int textureId;

    // Shader
    private int shaderProgram;
    private int aPositionHandle;
    private int aTexCoordHandle;
    private int uMVPMatrixHandle;
    private int uModelMatrixHandle;
    private int uCameraPosHandle;
    private int uTextureHandle;
    private int uTimeHandle;
    private int uEngineGlowHandle;

    // Transformación - POSICIÓN FIJA (calibrada)
    private float x = -2.5f;         // Centro horizontal
    private float y = -4.9f;         // Parte inferior
    private float z = -2.5f;         // Profundidad
    private float scale = 0.8f;      // Tamaño

    // Rotación
    private float rotationX = 0f;
    private float rotationY = 304.7f;  // Ángulo calibrado hacia horizonte
    private float rotationZ = 4.0f;

    // Tiempo para efectos
    private float time = 0f;

    // Engine glow
    private float engineGlow = 1.0f;

    // CONTROL POR GIROSCOPIO
    private boolean gyroEnabled = false;
    private float gyroTiltX = 0f;  // -1 (izquierda) a 1 (derecha)
    private float gyroTiltY = 0f;  // -1 (hacia usuario) a 1 (alejándose)
    private static final float GYRO_X_INFLUENCE = 1.5f;
    private static final float GYRO_ROLL_INFLUENCE = 15f;

    // Constantes de posición
    private static final float ADJUST_X = -2.5f;
    private static final float ADJUST_Y = -4.9f;
    private static final float ADJUST_Z = -2.5f;
    private static final float ADJUST_SCALE = 0.8f;

    // Cámara
    private CameraController camera;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // ═══════════════════════════════════════════════════════════════════════
    // VERTEX SHADER
    // ═══════════════════════════════════════════════════════════════════════

    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "precision highp float;\n" +
        "in vec3 aPosition;\n" +
        "in vec2 aTexCoord;\n" +
        "uniform mat4 uMVPMatrix;\n" +
        "uniform mat4 uModelMatrix;\n" +
        "out vec2 vTexCoord;\n" +
        "out vec3 vPosition;\n" +
        "out vec3 vWorldPos;\n" +
        "void main() {\n" +
        "    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);\n" +
        "    vTexCoord = aTexCoord;\n" +
        "    vPosition = aPosition;\n" +
        "    vWorldPos = (uModelMatrix * vec4(aPosition, 1.0)).xyz;\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════════════
    // FRAGMENT SHADER CON FRESNEL RIM LIGHT + LLAMAS
    // ═══════════════════════════════════════════════════════════════════════

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "uniform sampler2D uTexture;\n" +
        "uniform float uTime;\n" +
        "uniform float uEngineGlow;\n" +
        "uniform vec3 uCameraPos;\n" +
        "in vec2 vTexCoord;\n" +
        "in vec3 vPosition;\n" +
        "in vec3 vWorldPos;\n" +
        "out vec4 fragColor;\n" +
        "\n" +
        "void main() {\n" +
        "    // Detección de llamas (conos X > 0.65)\n" +
        "    float flameArea = smoothstep(0.65, 0.75, vPosition.x);\n" +
        "    \n" +
        "    // Textura base\n" +
        "    vec4 texColor = texture(uTexture, vTexCoord);\n" +
        "    \n" +
        "    // ═══════════════════════════════════════════════════════════\n" +
        "    // FRESNEL RIM LIGHT (solo en nave, no en llamas)\n" +
        "    // ═══════════════════════════════════════════════════════════\n" +
        "    vec3 normal = normalize(cross(dFdx(vWorldPos), dFdy(vWorldPos)));\n" +
        "    vec3 viewDir = normalize(uCameraPos - vWorldPos);\n" +
        "    float fresnel = pow(1.0 - abs(dot(normal, viewDir)), 3.0);\n" +
        "    \n" +
        "    // Color del rim: naranja/dorado del ambiente de fuego\n" +
        "    vec3 rimColor = vec3(1.0, 0.6, 0.2) * fresnel * 1.2;\n" +
        "    \n" +
        "    // Aplicar rim solo a la nave (no a las llamas)\n" +
        "    vec3 shipColor = texColor.rgb + rimColor * (1.0 - flameArea);\n" +
        "    \n" +
        "    // ═══════════════════════════════════════════════════════════\n" +
        "    // LLAMAS DE PLASMA\n" +
        "    // ═══════════════════════════════════════════════════════════\n" +
        "    float dist = clamp((vPosition.x - 0.65) * 2.0, 0.0, 1.0);\n" +
        "    float wave = sin(vPosition.y * 15.0 + vPosition.z * 10.0 + uTime * 8.0) * 0.5 + 0.5;\n" +
        "    \n" +
        "    vec3 flameColor = mix(\n" +
        "        mix(vec3(0.2, 0.8, 1.0), vec3(1.0, 0.5, 0.1), dist * 2.0),\n" +
        "        mix(vec3(1.0, 0.5, 0.1), vec3(1.0, 0.9, 0.3), dist * 2.0 - 1.0),\n" +
        "        step(0.5, dist)\n" +
        "    );\n" +
        "    \n" +
        "    float intensity = (1.0 - dist * 0.5) * uEngineGlow * (0.85 + wave * 0.15);\n" +
        "    float flameAlpha = (1.0 - dist * 0.4) * (0.8 + wave * 0.2);\n" +
        "    \n" +
        "    // Mezclar nave (con rim) y llamas\n" +
        "    vec3 finalColor = mix(shipColor, flameColor * intensity * 1.3, flameArea);\n" +
        "    float finalAlpha = mix(texColor.a, flameAlpha, flameArea);\n" +
        "    \n" +
        "    fragColor = vec4(finalColor, finalAlpha);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════

    public TravelingShip(Context context, TextureLoader textureLoader) {
        this.context = context;
        this.textureLoader = textureLoader;

        loadModel();
        loadTexture();
        compileShader();

        Log.d(TAG, "TravelingShip creada - viajando hacia el sol");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PARÁMETROS DE VUELO
    // ═══════════════════════════════════════════════════════════════════════

    // Posición de ORIGEN
    private static final float ORIGIN_X = -2.5f;
    private static final float ORIGIN_Y = -4.9f;
    private static final float ORIGIN_Z = -2.5f;
    private static final float ORIGIN_SCALE = 0.8f;

    // Estados del viaje
    private static final int STATE_HOVERING = 0;    // Planeando en origen
    private static final int STATE_FIGURE_8 = 1;    // Vuelo en figura de 8
    private int travelState = STATE_HOVERING;

    // Parámetros de la figura de 8
    private static final float FIGURE_8_DURATION_MIN = 20f;
    private static final float FIGURE_8_DURATION_MAX = 35f;
    private float currentFigure8Duration = 25f;
    private float figure8Phase = 0f;  // 0 a 2π

    // Dimensiones del 8
    private static final float FIGURE_8_WIDTH = 0.8f;   // Ancho lateral
    private static final float FIGURE_8_HEIGHT = 3.5f;  // Altura vertical (Y)
    private static final float FIGURE_8_DEPTH = 4.0f;   // Profundidad (Z)

    // Tiempo de planeo
    private static final float HOVER_DURATION_MIN = 4f;
    private static final float HOVER_DURATION_MAX = 10f;
    private float currentHoverDuration = 6f;
    private float hoverTime = 0f;

    // Rotación base
    private static final float BASE_ROTATION_Y = 304.7f;
    private float targetRotationY = BASE_ROTATION_Y;

    // Parámetros de vuelo orgánico
    private static final float ROLL_FACTOR = 10f;
    private static final float HOVER_DRIFT_AMPLITUDE = 0.75f;
    private static final float HOVER_DRIFT_SPEED = 0.5f;

    private final java.util.Random random = new java.util.Random();

    // ═══════════════════════════════════════════════════════════════════════
    // UPDATE - Máquina de estados del vuelo
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void update(float deltaTime) {
        time += deltaTime;

        switch (travelState) {
            case STATE_HOVERING:
                // Planeando en el origen antes de partir
                hoverTime += deltaTime;
                updateHovering(deltaTime);
                if (hoverTime >= currentHoverDuration) {
                    hoverTime = 0f;
                    figure8Phase = 0f;
                    // Generar duración aleatoria para el 8
                    currentFigure8Duration = FIGURE_8_DURATION_MIN +
                        random.nextFloat() * (FIGURE_8_DURATION_MAX - FIGURE_8_DURATION_MIN);
                    travelState = STATE_FIGURE_8;
                }
                break;

            case STATE_FIGURE_8:
                // Vuelo en figura de 8 continuo
                float phaseSpeed = (float)(2.0 * Math.PI) / currentFigure8Duration;
                figure8Phase += deltaTime * phaseSpeed;
                updateFigure8(deltaTime);

                // Cuando completa el 8 (2π), vuelve a hover
                if (figure8Phase >= 2.0f * Math.PI) {
                    figure8Phase = 0f;
                    travelState = STATE_HOVERING;
                    targetRotationY = BASE_ROTATION_Y;
                    currentHoverDuration = HOVER_DURATION_MIN +
                        random.nextFloat() * (HOVER_DURATION_MAX - HOVER_DURATION_MIN);
                }
                break;
        }
    }

    /**
     * Actualiza el movimiento cuando está planeando en el origen
     * Responde a inclinación del giroscopio
     */
    private void updateHovering(float deltaTime) {
        z = ORIGIN_Z;
        scale = ORIGIN_SCALE;

        // Rotación mirando al horizonte
        rotationY = lerpAngle(rotationY, BASE_ROTATION_Y, deltaTime * 3f);

        // Cálculos cacheados
        float t1 = time * HOVER_DRIFT_SPEED;
        float sinT1 = (float) Math.sin(t1);
        float cosT1 = (float) Math.cos(t1);
        float sinT2 = (float) Math.sin(time * 1.2f);
        float sinT3 = (float) Math.sin(time * 4.0f);

        // Drift lateral
        float drift = sinT1 * 0.8f + (float) Math.sin(t1 * 1.5f) * 0.2f;
        float baseX = ORIGIN_X + drift * HOVER_DRIFT_AMPLITUDE;

        // GIROSCOPIO: Añade movimiento basado en inclinación
        float gyroOffsetX = 0f;
        float gyroRoll = 0f;
        if (gyroEnabled) {
            gyroOffsetX = gyroTiltX * GYRO_X_INFLUENCE;
            gyroRoll = gyroTiltX * GYRO_ROLL_INFLUENCE;
        }

        x = baseX + gyroOffsetX;

        // Bobbing vertical
        y = ORIGIN_Y + sinT2 * 0.05f;

        // Roll basado en drift + giroscopio
        float driftVelocity = cosT1 * HOVER_DRIFT_SPEED * 0.9f;
        float mainRoll = driftVelocity * ROLL_FACTOR * 2.5f;
        float stabilization = sinT3 * 3.5f;
        rotationZ = 4.0f + mainRoll + stabilization + gyroRoll;

        // Pitch
        rotationX = (float) Math.sin(time * 0.7f) * 4.0f;

        // Engine glow
        engineGlow = 1.0f + sinT3 * 0.15f;
    }

    /**
     * Actualiza el movimiento en figura de 8
     */
    private void updateFigure8(float deltaTime) {
        float t = figure8Phase;

        // Cálculos cacheados
        float sinT = (float) Math.sin(t);
        float cosT = (float) Math.cos(t);
        float sinHalfT = (float) Math.sin(t * 0.5f);
        float cosHalfT = (float) Math.cos(t * 0.5f);

        // Posición del 8
        x = ORIGIN_X + FIGURE_8_WIDTH * sinT;
        y = ORIGIN_Y + FIGURE_8_HEIGHT * sinHalfT;
        z = ORIGIN_Z - FIGURE_8_DEPTH * sinHalfT;

        // Escala según distancia
        scale = ORIGIN_SCALE - (ORIGIN_SCALE - 0.15f) * sinHalfT;

        // Derivadas para dirección
        float dx = FIGURE_8_WIDTH * cosT;
        float dy = (FIGURE_8_HEIGHT * 0.5f) * cosHalfT;

        // Rotación Y según dirección vertical
        targetRotationY = (dy >= 0) ? BASE_ROTATION_Y : BASE_ROTATION_Y + 180f;
        rotationY = lerpAngle(rotationY, targetRotationY, deltaTime * 4f);

        // Banking
        float sinStab = (float) Math.sin(time * 4.0f);
        rotationZ = 4.0f + dx * 30f + sinStab * 2.5f;

        // Pitch
        rotationX = dy * 8f + sinStab * 0.5f;

        // Engine glow
        engineGlow = 1.3f + Math.abs(dx) * 0.5f + sinStab * 0.1f;
    }

    /** Interpola ángulos correctamente (maneja wrap de 360°) */
    private float lerpAngle(float from, float to, float t) {
        float diff = to - from;
        while (diff > 180f) diff -= 360f;
        while (diff < -180f) diff += 360f;
        return from + diff * Math.min(1f, t);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DRAW
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void draw() {
        if (shaderProgram == 0 || camera == null || vertexBuffer == null) return;

        GLES30.glUseProgram(shaderProgram);

        // Construir matriz de modelo
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        Matrix.rotateM(modelMatrix, 0, rotationX, 1f, 0f, 0f);  // Pitch
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f);  // Yaw
        Matrix.rotateM(modelMatrix, 0, rotationZ, 0f, 0f, 1f);  // Roll
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        camera.computeMvp(modelMatrix, mvpMatrix);

        // Uniforms
        GLES30.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES30.glUniformMatrix4fv(uModelMatrixHandle, 1, false, modelMatrix, 0);
        GLES30.glUniform3f(uCameraPosHandle, 4f, 3f, 6f);
        GLES30.glUniform1f(uTimeHandle, time);
        GLES30.glUniform1f(uEngineGlowHandle, engineGlow);

        // Textura
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(uTextureHandle, 0);

        // Vértices
        GLES30.glEnableVertexAttribArray(aPositionHandle);
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        // UVs
        GLES30.glEnableVertexAttribArray(aTexCoordHandle);
        GLES30.glVertexAttribPointer(aTexCoordHandle, 2, GLES30.GL_FLOAT, false, 0, uvBuffer);

        // Dibujar
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, indexBuffer);

        GLES30.glDisableVertexAttribArray(aPositionHandle);
        GLES30.glDisableVertexAttribArray(aTexCoordHandle);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONTROL POR GIROSCOPIO
    // ═══════════════════════════════════════════════════════════════════════

    public void setGyroEnabled(boolean enabled) {
        this.gyroEnabled = enabled;
    }

    public void setTiltInput(float tiltX, float tiltY) {
        this.gyroTiltX = tiltX;
        this.gyroTiltY = tiltY;
    }

    // ... (métodos auxiliares: loadModel, loadTexture, compileShader, release, etc.)
}
```

---

## 2. GyroscopeManager.java - CÓDIGO FUENTE COMPLETO

```java
package com.secret.blackholeglow;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * GyroscopeManager - Control por inclinación del dispositivo
 *
 * CARACTERÍSTICAS:
 * - Lee sensores de acelerómetro y magnetómetro
 * - Calcula orientación del dispositivo (pitch, roll)
 * - Suavizado de valores para movimiento fluido
 * - Valores normalizados -1 a 1 para fácil integración
 */
public class GyroscopeManager implements SensorEventListener {
    private static final String TAG = "GyroscopeManager";

    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final Sensor magnetometer;

    // Datos de sensores
    private float[] gravity = new float[3];
    private float[] geomagnetic = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientation = new float[3];

    // Valores de inclinación (-1 a 1)
    private float tiltX = 0f;  // Roll (inclinación lateral)
    private float tiltY = 0f;  // Pitch (inclinación adelante/atrás)

    // Suavizado
    private static final float SMOOTHING_FACTOR = 0.15f;
    private float smoothTiltX = 0f;
    private float smoothTiltY = 0f;

    // Estado
    private boolean isEnabled = false;
    private boolean hasAccelerometer = false;
    private boolean hasMagnetometer = false;

    // Sensibilidad y zona muerta
    private float sensitivity = 1.0f;
    private static final float DEAD_ZONE = 0.05f;

    public GyroscopeManager(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        hasAccelerometer = (accelerometer != null);
        hasMagnetometer = (magnetometer != null);
    }

    public void start() {
        if (isEnabled) return;
        if (hasAccelerometer) {
            sensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_GAME);
        }
        if (hasMagnetometer) {
            sensorManager.registerListener(this, magnetometer,
                SensorManager.SENSOR_DELAY_GAME);
        }
        isEnabled = true;
    }

    public void stop() {
        if (!isEnabled) return;
        sensorManager.unregisterListener(this);
        isEnabled = false;
        tiltX = 0f;
        tiltY = 0f;
        smoothTiltX = 0f;
        smoothTiltY = 0f;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Low-pass filter
            gravity[0] = gravity[0] * 0.8f + event.values[0] * 0.2f;
            gravity[1] = gravity[1] * 0.8f + event.values[1] * 0.2f;
            gravity[2] = gravity[2] * 0.8f + event.values[2] * 0.2f;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic[0] = event.values[0];
            geomagnetic[1] = event.values[1];
            geomagnetic[2] = event.values[2];
        }

        // Calcular orientación
        if (hasAccelerometer && hasMagnetometer) {
            boolean success = SensorManager.getRotationMatrix(
                rotationMatrix, null, gravity, geomagnetic);

            if (success) {
                SensorManager.getOrientation(rotationMatrix, orientation);

                // Convertir de radianes a valores normalizados (-1 a 1)
                float rawPitch = (float) Math.toDegrees(orientation[1]);
                tiltY = clamp(rawPitch / 45f, -1f, 1f);

                float rawRoll = (float) Math.toDegrees(orientation[2]);
                tiltX = clamp(rawRoll / 45f, -1f, 1f);

                // Aplicar zona muerta
                if (Math.abs(tiltX) < DEAD_ZONE) tiltX = 0f;
                if (Math.abs(tiltY) < DEAD_ZONE) tiltY = 0f;

                // Suavizado exponencial
                smoothTiltX += (tiltX - smoothTiltX) * SMOOTHING_FACTOR;
                smoothTiltY += (tiltY - smoothTiltY) * SMOOTHING_FACTOR;
            }
        }
    }

    public float getTiltX() { return smoothTiltX * sensitivity; }
    public float getTiltY() { return smoothTiltY * sensitivity; }
    public boolean isEnabled() { return isEnabled; }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public void release() { stop(); }
}
```

---

## 3. SHADERS EN DETALLE

### Vertex Shader

```glsl
#version 300 es
precision highp float;

in vec3 aPosition;
in vec2 aTexCoord;

uniform mat4 uMVPMatrix;
uniform mat4 uModelMatrix;

out vec2 vTexCoord;
out vec3 vPosition;       // Posición local del modelo (para detectar llamas)
out vec3 vWorldPos;       // Posición en mundo (para Fresnel)

void main() {
    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
    vTexCoord = aTexCoord;
    vPosition = aPosition;
    vWorldPos = (uModelMatrix * vec4(aPosition, 1.0)).xyz;
}
```

### Fragment Shader (con comentarios detallados)

```glsl
#version 300 es
precision mediump float;

uniform sampler2D uTexture;
uniform float uTime;
uniform float uEngineGlow;
uniform vec3 uCameraPos;

in vec2 vTexCoord;
in vec3 vPosition;    // Posición local
in vec3 vWorldPos;    // Posición mundo

out vec4 fragColor;

void main() {
    // ═══════════════════════════════════════════════════════════════
    // PASO 1: Detectar área de llamas
    // Los conos de llamas están en X > 0.65 del modelo OBJ
    // ═══════════════════════════════════════════════════════════════
    float flameArea = smoothstep(0.65, 0.75, vPosition.x);

    // Textura base de la nave
    vec4 texColor = texture(uTexture, vTexCoord);

    // ═══════════════════════════════════════════════════════════════
    // PASO 2: Fresnel Rim Light (solo en nave, no en llamas)
    //
    // El efecto Fresnel hace que los bordes de la nave brillen
    // como si estuvieran iluminados por el ambiente de fuego
    // ═══════════════════════════════════════════════════════════════

    // Calcular normal aproximada desde derivadas de posición
    // (El modelo no tiene normales, así que las calculamos)
    vec3 normal = normalize(cross(dFdx(vWorldPos), dFdy(vWorldPos)));

    // Dirección desde fragmento hacia cámara
    vec3 viewDir = normalize(uCameraPos - vWorldPos);

    // Fresnel: más brillo en bordes (ángulo rasante)
    // pow(..., 3.0) hace el borde más nítido
    float fresnel = pow(1.0 - abs(dot(normal, viewDir)), 3.0);

    // Color del rim: naranja/dorado para ambiente de fuego
    vec3 rimColor = vec3(1.0, 0.6, 0.2) * fresnel * 1.2;

    // Aplicar rim solo a la nave (multiplicar por 1.0 - flameArea)
    vec3 shipColor = texColor.rgb + rimColor * (1.0 - flameArea);

    // ═══════════════════════════════════════════════════════════════
    // PASO 3: Llamas de plasma animadas
    //
    // Gradiente: Cyan (base) → Naranja (medio) → Amarillo (punta)
    // Animación: Onda sinusoidal para efecto de turbulencia
    // ═══════════════════════════════════════════════════════════════

    // Distancia normalizada en el cono (0=base, 1=punta)
    float dist = clamp((vPosition.x - 0.65) * 2.0, 0.0, 1.0);

    // Onda animada (combina Y, Z y tiempo)
    float wave = sin(vPosition.y * 15.0 + vPosition.z * 10.0 + uTime * 8.0) * 0.5 + 0.5;

    // Gradiente de color SIN branches (optimizado)
    vec3 flameColor = mix(
        mix(vec3(0.2, 0.8, 1.0), vec3(1.0, 0.5, 0.1), dist * 2.0),      // Cyan→Naranja
        mix(vec3(1.0, 0.5, 0.1), vec3(1.0, 0.9, 0.3), dist * 2.0 - 1.0), // Naranja→Amarillo
        step(0.5, dist)  // Selector binario
    );

    // Intensidad: más brillante en base, modulado por engine glow
    float intensity = (1.0 - dist * 0.5) * uEngineGlow * (0.85 + wave * 0.15);

    // Alpha: más opaco en base, transparente en punta
    float flameAlpha = (1.0 - dist * 0.4) * (0.8 + wave * 0.2);

    // ═══════════════════════════════════════════════════════════════
    // PASO 4: Mezclar nave y llamas
    // ═══════════════════════════════════════════════════════════════
    vec3 finalColor = mix(shipColor, flameColor * intensity * 1.3, flameArea);
    float finalAlpha = mix(texColor.a, flameAlpha, flameArea);

    fragColor = vec4(finalColor, finalAlpha);
}
```

---

## 4. PREGUNTAS PARA GROK

### Sobre el Shader:

1. **¿Hay forma de optimizar el cálculo de normales con `dFdx/dFdy`?**
   - Actualmente calculamos normales cada frame
   - ¿Sería mejor precalcularlas o usar un mapa de normales?

2. **¿El efecto Fresnel podría mejorar con un exponente diferente?**
   - Actualmente usamos `pow(..., 3.0)`
   - ¿Qué valores recomiendas para diferentes efectos?

3. **¿Cómo podríamos añadir un efecto de "heat distortion" alrededor de las llamas?**
   - Similar a lo que se ve en turbinas de aviones reales

4. **¿El gradiente de llamas podría usar ruido procedural en lugar de sin()?**
   - Para un look menos repetitivo

### Sobre el Sistema de Vuelo:

1. **¿La figura de 8 paramétrica podría ser más orgánica?**
   - Actualmente usa `sin(t)` y `sin(t/2)`
   - ¿Hay curvas más naturales?

2. **¿Cómo añadir turbulencia al vuelo sin que se vea errático?**

3. **¿El banking (rotationZ) está bien calculado?**
   - Actualmente: `rotationZ = dx * 30f`
   - ¿Hay fórmulas más realistas de aerodinámica?

4. **¿El control por giroscopio podría mejorar?**
   - Actualmente es lineal: `gyroOffsetX = gyroTiltX * GYRO_X_INFLUENCE`
   - ¿Curvas de respuesta no lineales serían mejores?

---

## 5. MÉTRICAS DE RENDIMIENTO

| Métrica | Valor Actual |
|---------|--------------|
| FPS promedio | 36-43 |
| Llamadas Math.sin/cos en update() | 5-8 |
| Instrucciones GPU estimadas | ~25 |
| Memoria de texturas | 512KB |
| Triángulos del modelo | 3,120 |

---

## 6. MODELO 3D

El modelo `human_interceptor_flames.obj` incluye:
- **Nave principal**: Fuselaje con UVs mapeados
- **Conos de llamas**: 3 conos integrados (X > 0.65)
- **Materiales**: 2 (nave + conos)

```
Estructura del modelo:
        ┌──────────────────┐
        │    FUSELAJE      │  X < 0.65
        │   (textura nave) │
        ├──────────────────┤
        │ CONOS DE LLAMAS  │  X > 0.65
        │ (shader plasma)  │
        └──────────────────┘
```

---

*Documento generado para revisión por Grok - Enero 2025*
*Proyecto: Black Hole Glow - Wallpaper PYRALIS*
