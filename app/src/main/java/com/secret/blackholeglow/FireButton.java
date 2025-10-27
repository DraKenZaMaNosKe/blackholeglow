package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Botón de disparo visual - SIMPLE usando BaseShaderProgram como Planeta
 */
public class FireButton extends BaseShaderProgram implements SceneObject, CameraAware {
    private static final String TAG = "FireButton";

    // Constantes del botón
    private static final float BUTTON_RADIUS = 0.055f;
    private static final float BUTTON_X = 0.0f;
    private static final float BUTTON_Y = -0.5f;
    private static final float COOLDOWN_TIME = 0.3f;  // 300ms

    // Buffers
    private FloatBuffer vertexBuffer;

    // Uniform locations
    private int uButtonPosLoc;
    private int uRadiusLoc;
    private int uResolutionLoc;
    private int uPressedLoc;
    private int uCooldownProgressLoc;

    // Attribute location
    private int aPositionLoc;

    // Estado
    private boolean isReady = true;
    private float cooldownTimer = 0.0f;
    private float currentTime = 0.0f;

    // Estado visual
    private boolean isPressed = false;
    private float pressedTimer = 0.0f;
    private static final float PRESSED_DURATION = 0.15f;  // 150ms de feedback visual

    public FireButton(Context context) {
        super(context, "shaders/firebutton_vertex.glsl", "shaders/firebutton_fragment.glsl");

        setupBuffers();
        setupUniforms();

        Log.d(TAG, "✓ FireButton inicializado con BaseShaderProgram");
    }

    private void setupBuffers() {
        // Quad simple (-1 a 1)
        float[] vertices = {
            -1.0f, -1.0f,
             1.0f, -1.0f,
            -1.0f,  1.0f,
             1.0f,  1.0f
        };

        vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices);
        vertexBuffer.position(0);
    }

    private void setupUniforms() {
        // Obtener locations de uniforms
        uButtonPosLoc = GLES20.glGetUniformLocation(programId, "u_ButtonPos");
        uRadiusLoc = GLES20.glGetUniformLocation(programId, "u_Radius");
        uResolutionLoc = GLES20.glGetUniformLocation(programId, "u_Resolution");
        uPressedLoc = GLES20.glGetUniformLocation(programId, "u_Pressed");
        uCooldownProgressLoc = GLES20.glGetUniformLocation(programId, "u_CooldownProgress");

        // Obtener location de atributo
        aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position");

        Log.d(TAG, "✓ Uniform locations: pos=" + uButtonPosLoc + " radius=" + uRadiusLoc +
                   " res=" + uResolutionLoc + " pressed=" + uPressedLoc +
                   " cooldownProgress=" + uCooldownProgressLoc);
    }

    @Override
    public void update(float deltaTime) {
        currentTime += deltaTime;

        // Actualizar cooldown
        if (!isReady) {
            cooldownTimer += deltaTime;
            if (cooldownTimer >= COOLDOWN_TIME) {
                isReady = true;
                Log.d(TAG, "🟢 Botón listo para disparar");
            }
        }

        // Actualizar efecto visual de presionado
        if (isPressed) {
            pressedTimer += deltaTime;
            if (pressedTimer >= PRESSED_DURATION) {
                isPressed = false;
                pressedTimer = 0.0f;
            }
        }
    }

    @Override
    public void draw() {
        // Usar shader program
        GLES20.glUseProgram(programId);

        // Deshabilitar depth test para UI
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        // Habilitar blending
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Calcular progreso del cooldown (0.0 = inicio, 1.0 = listo)
        float cooldownProgress = isReady ? 1.0f : Math.min(cooldownTimer / COOLDOWN_TIME, 1.0f);

        // Pasar uniforms
        GLES20.glUniform2f(uButtonPosLoc, BUTTON_X, BUTTON_Y);
        GLES20.glUniform1f(uRadiusLoc, BUTTON_RADIUS);
        GLES20.glUniform2f(uResolutionLoc, SceneRenderer.screenWidth, SceneRenderer.screenHeight);
        GLES20.glUniform1f(uPressedLoc, isPressed ? 1.0f : 0.0f);
        GLES20.glUniform1f(uCooldownProgressLoc, cooldownProgress);

        // Pasar vértices
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        // Dibujar
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPositionLoc);

        // Restaurar depth test
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    /**
     * Verifica si el toque está dentro del botón
     */
    public boolean isTouchInside(float normalizedX, float normalizedY) {
        float dx = normalizedX - BUTTON_X;
        float dy = normalizedY - BUTTON_Y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return distance <= BUTTON_RADIUS;
    }

    /**
     * ¿Está el botón listo para disparar?
     */
    public boolean isReady() {
        return isReady;
    }

    /**
     * Activa el cooldown del botón
     */
    public void startCooldown() {
        isReady = false;
        cooldownTimer = 0.0f;

        // Activar efecto visual de presionado
        isPressed = true;
        pressedTimer = 0.0f;

        Log.d(TAG, "🟡 Cooldown iniciado + efecto visual");
    }

    @Override
    public void setCameraController(CameraController camera) {
        // No necesitamos cámara para UI 2D
    }
}
