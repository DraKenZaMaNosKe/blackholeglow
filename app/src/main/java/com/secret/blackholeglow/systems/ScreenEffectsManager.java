package com.secret.blackholeglow.systems;

import com.secret.blackholeglow.ShaderUtils;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║ 💥 ScreenEffectsManager - Efectos de Impacto en Pantalla         ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  • Flash blanco de impacto                                       ║
 * ║  • Grietas procedurales animadas                                 ║
 * ║  • Optimizado con shaders lazy-loaded                            ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class ScreenEffectsManager {
    private static final String TAG = "ScreenEffects";

    // ===== 💥 SISTEMA DE IMPACTO EN PANTALLA (FLASH) 💥 =====
    private float impactFlashAlpha = 0f;
    private float impactFlashTimer = 0f;
    private int flashShaderProgramId = 0;
    private int flashAPositionLoc = -1;
    private int flashAColorLoc = -1;

    // ===== 💥 SISTEMA DE PANTALLA ROTA (GRIETAS) 💥 =====
    private float crackAlpha = 0f;
    private float crackTimer = 0f;
    private float crackX = 0.5f;
    private float crackY = 0.5f;
    private int crackShaderProgramId = 0;
    private int crackAPositionLoc = -1;
    private int crackATexCoordLoc = -1;
    private int crackUTimeLoc = -1;
    private int crackUImpactPosLoc = -1;
    private int crackUAlphaLoc = -1;

    // Buffers reutilizables (evita allocations en runtime)
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private FloatBuffer uvBuffer;

    public ScreenEffectsManager() {
        initBuffers();
    }

    private void initBuffers() {
        // Vértices en NDC que cubren toda la pantalla
        float[] vertices = {
            -1.0f, -1.0f,
             1.0f, -1.0f,
            -1.0f,  1.0f,
             1.0f,  1.0f
        };

        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // Buffer de colores (4 vértices x 4 componentes RGBA)
        ByteBuffer cbb = ByteBuffer.allocateDirect(16 * 4);
        cbb.order(ByteOrder.nativeOrder());
        colorBuffer = cbb.asFloatBuffer();

        // UV coordinates
        float[] uvs = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
        };

        ByteBuffer ubb = ByteBuffer.allocateDirect(uvs.length * 4);
        ubb.order(ByteOrder.nativeOrder());
        uvBuffer = ubb.asFloatBuffer();
        uvBuffer.put(uvs);
        uvBuffer.position(0);
    }

    /**
     * 💥 Activa efecto de flash blanco
     */
    public void triggerScreenImpact(float intensity) {
        impactFlashAlpha = intensity * 0.6f;
        impactFlashTimer = 0.25f;
        Log.d(TAG, String.format("💥 IMPACTO EN PANTALLA! Intensidad: %.0f%%", intensity * 100));
    }

    /**
     * 💥💥 Activa efecto de PANTALLA ROTA con grietas procedurales
     */
    public void triggerScreenCrack(float screenX, float screenY, float intensity) {
        // Flash blanco MÁS INTENSO
        impactFlashAlpha = intensity * 0.8f;
        impactFlashTimer = 0.4f;

        // GRIETAS
        crackX = screenX;
        crackY = screenY;
        crackTimer = 0.01f;
        crackAlpha = 0f;

        Log.d(TAG, "╔════════════════════════════════════════════════════════╗");
        Log.d(TAG, "║    💥💥💥 ¡PANTALLA ROTA! 💥💥💥                      ║");
        Log.d(TAG, String.format("║    Impacto en: (%.2f, %.2f) - Intensidad: %.0f%%        ║", screenX, screenY, intensity * 100));
        Log.d(TAG, "╚════════════════════════════════════════════════════════╝");
    }

    /**
     * Actualiza los efectos (llamar cada frame)
     */
    public void update(float deltaTime) {
        // Actualizar flash
        if (impactFlashTimer > 0) {
            impactFlashTimer -= deltaTime;
            impactFlashAlpha = Math.max(0, impactFlashAlpha - deltaTime * 3.0f);
        }

        // Actualizar grietas
        if (crackTimer > 0) {
            crackTimer += deltaTime;
            // Fade in rápido, luego fade out más lento
            if (crackTimer < 0.3f) {
                crackAlpha = Math.min(1.0f, crackTimer / 0.1f);
            } else if (crackTimer < 2.5f) {
                crackAlpha = 1.0f - (crackTimer - 0.3f) / 2.2f;
            } else {
                crackAlpha = 0f;
                crackTimer = 0f;
            }
        }
    }

    /**
     * Dibuja los efectos activos
     */
    public void draw() {
        if (impactFlashAlpha > 0.01f) {
            drawImpactFlash();
        }

        if (crackAlpha > 0.01f) {
            drawScreenCracks();
        }
    }

    /**
     * Dibuja un flash blanco semi-transparente en toda la pantalla
     */
    private void drawImpactFlash() {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Inicialización lazy del shader
        if (flashShaderProgramId == 0) {
            initFlashShader();
            if (flashShaderProgramId == 0) {
                GLES20.glEnable(GLES20.GL_DEPTH_TEST);
                return;
            }
        }

        if (flashShaderProgramId > 0 && GLES20.glIsProgram(flashShaderProgramId)) {
            GLES20.glUseProgram(flashShaderProgramId);

            // Actualizar buffer de colores con alpha actual
            float[] colors = new float[16];
            for (int i = 0; i < 4; i++) {
                colors[i * 4] = 1.0f;      // R
                colors[i * 4 + 1] = 1.0f;  // G
                colors[i * 4 + 2] = 1.0f;  // B
                colors[i * 4 + 3] = impactFlashAlpha;  // A
            }
            colorBuffer.clear();
            colorBuffer.put(colors);
            colorBuffer.position(0);

            // Configurar atributos
            GLES20.glEnableVertexAttribArray(flashAPositionLoc);
            GLES20.glVertexAttribPointer(flashAPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

            GLES20.glEnableVertexAttribArray(flashAColorLoc);
            GLES20.glVertexAttribPointer(flashAColorLoc, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            GLES20.glDisableVertexAttribArray(flashAPositionLoc);
            GLES20.glDisableVertexAttribArray(flashAColorLoc);
        }

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    private void initFlashShader() {
        String vertexShader =
            "attribute vec2 a_Position;\n" +
            "attribute vec4 a_Color;\n" +
            "varying vec4 v_Color;\n" +
            "void main() {\n" +
            "    v_Color = a_Color;\n" +
            "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
            "}\n";

        String fragmentShader =
            "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec4 v_Color;\n" +
            "void main() {\n" +
            "    gl_FragColor = v_Color;\n" +
            "}\n";

        int vShader = ShaderUtils.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        int fShader = ShaderUtils.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        flashShaderProgramId = GLES20.glCreateProgram();
        GLES20.glAttachShader(flashShaderProgramId, vShader);
        GLES20.glAttachShader(flashShaderProgramId, fShader);
        GLES20.glLinkProgram(flashShaderProgramId);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(flashShaderProgramId, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "💥 Flash shader link failed: " + GLES20.glGetProgramInfoLog(flashShaderProgramId));
            flashShaderProgramId = 0;
            return;
        }

        GLES20.glDeleteShader(vShader);
        GLES20.glDeleteShader(fShader);

        flashAPositionLoc = GLES20.glGetAttribLocation(flashShaderProgramId, "a_Position");
        flashAColorLoc = GLES20.glGetAttribLocation(flashShaderProgramId, "a_Color");

        Log.d(TAG, "💥 Flash shader creado - ID: " + flashShaderProgramId);
    }

    /**
     * 💥💥 Dibuja grietas procedurales en la pantalla
     */
    private void drawScreenCracks() {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Inicialización lazy del shader
        if (crackShaderProgramId == 0) {
            initCrackShader();
            if (crackShaderProgramId == 0) {
                GLES20.glEnable(GLES20.GL_DEPTH_TEST);
                return;
            }
        }

        if (crackShaderProgramId > 0 && GLES20.glIsProgram(crackShaderProgramId)) {
            GLES20.glUseProgram(crackShaderProgramId);

            // Configurar uniforms
            GLES20.glUniform1f(crackUTimeLoc, crackTimer);
            GLES20.glUniform2f(crackUImpactPosLoc, crackX, crackY);
            GLES20.glUniform1f(crackUAlphaLoc, crackAlpha);

            // Configurar atributos
            GLES20.glEnableVertexAttribArray(crackAPositionLoc);
            GLES20.glVertexAttribPointer(crackAPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

            GLES20.glEnableVertexAttribArray(crackATexCoordLoc);
            GLES20.glVertexAttribPointer(crackATexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            GLES20.glDisableVertexAttribArray(crackAPositionLoc);
            GLES20.glDisableVertexAttribArray(crackATexCoordLoc);
        }

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    private void initCrackShader() {
        String vertexShader =
            "attribute vec2 a_Position;\n" +
            "attribute vec2 a_TexCoord;\n" +
            "varying vec2 v_TexCoord;\n" +
            "void main() {\n" +
            "    v_TexCoord = a_TexCoord;\n" +
            "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
            "}\n";

        String fragmentShader =
            "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec2 v_TexCoord;\n" +
            "uniform float u_Time;\n" +
            "uniform vec2 u_ImpactPos;\n" +
            "uniform float u_Alpha;\n" +
            "\n" +
            "float hash(float n) {\n" +
            "    return fract(sin(n) * 43758.5453);\n" +
            "}\n" +
            "\n" +
            "float noise(vec2 p) {\n" +
            "    vec2 i = floor(p);\n" +
            "    vec2 f = fract(p);\n" +
            "    f = f * f * (3.0 - 2.0 * f);\n" +
            "    float n = i.x + i.y * 57.0;\n" +
            "    return mix(mix(hash(n), hash(n + 1.0), f.x),\n" +
            "               mix(hash(n + 57.0), hash(n + 58.0), f.x), f.y);\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 uv = v_TexCoord;\n" +
            "    vec2 toImpact = uv - u_ImpactPos;\n" +
            "    float dist = length(toImpact);\n" +
            "    float angle = atan(toImpact.y, toImpact.x);\n" +
            "    \n" +
            "    // GRIETAS PRINCIPALES (8 rayos)\n" +
            "    float numCracks = 8.0;\n" +
            "    float crackPattern = 0.0;\n" +
            "    \n" +
            "    for (float i = 0.0; i < numCracks; i++) {\n" +
            "        float crackAngle = (i / numCracks) * 6.28318 + hash(i) * 0.3;\n" +
            "        float angleDiff = abs(mod(angle - crackAngle + 3.14159, 6.28318) - 3.14159);\n" +
            "        \n" +
            "        float crackNoise = noise(vec2(dist * 30.0, i)) * 0.5 + 0.5;\n" +
            "        float crackWidth = 0.004 + crackNoise * 0.003;\n" +
            "        float crack = smoothstep(crackWidth, 0.0, angleDiff);\n" +
            "        \n" +
            "        float branch = noise(vec2(dist * 15.0 + i, angle * 8.0));\n" +
            "        crack *= (0.7 + branch * 0.3);\n" +
            "        \n" +
            "        float distFade = smoothstep(1.0, 0.0, dist);\n" +
            "        crack *= distFade;\n" +
            "        \n" +
            "        float expansion = smoothstep(dist * 2.0, dist * 2.0 + 0.15, u_Time * 3.0);\n" +
            "        crack *= expansion;\n" +
            "        \n" +
            "        crackPattern = max(crackPattern, crack);\n" +
            "    }\n" +
            "    \n" +
            "    // GRIETAS SECUNDARIAS (3 rayos sutiles)\n" +
            "    float secondaryCracks = 0.0;\n" +
            "    for (float i = 0.0; i < 3.0; i++) {\n" +
            "        float offset = hash(i + 10.0) * 6.28318;\n" +
            "        float crackAngle = (i / 3.0) * 6.28318 + offset;\n" +
            "        float angleDiff = abs(mod(angle - crackAngle + 3.14159, 6.28318) - 3.14159);\n" +
            "        \n" +
            "        float crack = smoothstep(0.003, 0.0, angleDiff);\n" +
            "        float distFade = smoothstep(0.6, 0.0, dist);\n" +
            "        crack *= distFade;\n" +
            "        \n" +
            "        float expansion = smoothstep(dist * 2.0, dist * 2.0 + 0.15, u_Time * 3.0);\n" +
            "        crack *= expansion * 0.4;\n" +
            "        \n" +
            "        secondaryCracks = max(secondaryCracks, crack);\n" +
            "    }\n" +
            "    \n" +
            "    crackPattern = max(crackPattern, secondaryCracks);\n" +
            "    \n" +
            "    // DESTELLO EN PUNTO DE IMPACTO\n" +
            "    float impactGlow = 0.0;\n" +
            "    if (dist < 0.15) {\n" +
            "        impactGlow = (1.0 - dist / 0.15) * smoothstep(0.3, 0.0, u_Time);\n" +
            "        impactGlow = pow(impactGlow, 2.0);\n" +
            "    }\n" +
            "    \n" +
            "    // COLOR ENERGÉTICO (azul eléctrico/cyan)\n" +
            "    vec3 crackColor = mix(\n" +
            "        vec3(0.3, 0.8, 1.0),\n" +
            "        vec3(0.9, 0.95, 1.0),\n" +
            "        crackPattern * 0.6\n" +
            "    );\n" +
            "    \n" +
            "    crackColor = mix(crackColor, vec3(1.0, 0.7, 0.3), impactGlow * 0.8);\n" +
            "    \n" +
            "    float finalAlpha = (crackPattern + impactGlow) * u_Alpha * 0.7;\n" +
            "    gl_FragColor = vec4(crackColor, finalAlpha);\n" +
            "}\n";

        int vShader = ShaderUtils.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        int fShader = ShaderUtils.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        crackShaderProgramId = GLES20.glCreateProgram();
        GLES20.glAttachShader(crackShaderProgramId, vShader);
        GLES20.glAttachShader(crackShaderProgramId, fShader);
        GLES20.glLinkProgram(crackShaderProgramId);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(crackShaderProgramId, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "💥 Crack shader link failed: " + GLES20.glGetProgramInfoLog(crackShaderProgramId));
            crackShaderProgramId = 0;
            return;
        }

        GLES20.glDeleteShader(vShader);
        GLES20.glDeleteShader(fShader);

        crackAPositionLoc = GLES20.glGetAttribLocation(crackShaderProgramId, "a_Position");
        crackATexCoordLoc = GLES20.glGetAttribLocation(crackShaderProgramId, "a_TexCoord");
        crackUTimeLoc = GLES20.glGetUniformLocation(crackShaderProgramId, "u_Time");
        crackUImpactPosLoc = GLES20.glGetUniformLocation(crackShaderProgramId, "u_ImpactPos");
        crackUAlphaLoc = GLES20.glGetUniformLocation(crackShaderProgramId, "u_Alpha");

        Log.d(TAG, "💥 Crack shader creado - ID: " + crackShaderProgramId);
    }

    /**
     * Libera recursos OpenGL
     */
    public void release() {
        if (flashShaderProgramId != 0) {
            GLES20.glDeleteProgram(flashShaderProgramId);
            flashShaderProgramId = 0;
        }
        if (crackShaderProgramId != 0) {
            GLES20.glDeleteProgram(crackShaderProgramId);
            crackShaderProgramId = 0;
        }
        Log.d(TAG, "💥 ScreenEffectsManager liberado");
    }

    // Getters para verificar estado
    public boolean hasActiveEffects() {
        return impactFlashAlpha > 0.01f || crackAlpha > 0.01f;
    }
}
