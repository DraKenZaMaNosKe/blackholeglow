package com.secret.blackholeglow;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   ✨ PlayPauseButton - Botón Mágico con Efectos Visuales ✨       ║
 * ║   OpenGL ES 3.0 Version                                           ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * Efectos visuales:
 * - Anillo exterior con partículas orbitando
 * - Glow pulsante tipo "respiración"
 * - Destellos de luz ocasionales
 * - Gradiente de colores cambiantes
 * - Icono con efecto holográfico
 */
public class PlayPauseButton implements SceneObject {
    private static final String TAG = "PlayPauseButton";

    // Estado
    private boolean isPlaying = false;
    private boolean isVisible = true;

    // Posición y tamaño
    private float centerX = 0.0f;
    private float centerY = 0.0f;
    private float size = 0.18f;
    private float hitAreaMultiplier = 1.5f;

    // Animación
    private float time = 0f;
    private static final float TIME_CYCLE = 62.831853f;  // 10 * TWO_PI - ciclo completo para evitar overflow
    private float fadeTimer = 0f;
    private float fadeAlpha = 1.0f;
    private static final float AUTO_HIDE_DELAY = 4.0f;
    private static final float FADE_DURATION = 1.5f;

    // OpenGL ES 3.0 - NO usar static para evitar problemas con contexto GL
    private int shaderProgram = 0;
    private int aPositionLoc = -1;
    private int uTimeLoc = -1;
    private int uCenterLoc = -1;
    private int uSizeLoc = -1;
    private int uShapeLoc = -1;
    private int uAspectLoc = -1;
    private int uAlphaLoc = -1;

    private FloatBuffer vertexBuffer;
    private static final float[] QUAD_VERTICES = {
        -1.0f, -1.0f,
         1.0f, -1.0f,
        -1.0f,  1.0f,
         1.0f,  1.0f
    };

    private boolean shadersCompiled = false;
    private float aspectRatio = 1.0f;

    public PlayPauseButton() {
        compileShaders();
    }

    private void compileShaders() {
        if (shadersCompiled) return;

        // ═══════════════════════════════════════════════════════════════
        // 🔷 VERTEX SHADER - OpenGL ES 3.0 (GLSL 300 es)
        // ═══════════════════════════════════════════════════════════════
        String vertexShaderCode =
            "#version 300 es\n" +
            "in vec2 a_Position;\n" +
            "out vec2 v_TexCoord;\n" +
            "void main() {\n" +
            "    v_TexCoord = a_Position * 0.5 + 0.5;\n" +
            "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
            "}\n";

        // ═══════════════════════════════════════════════════════════════
        // 🔷 FRAGMENT SHADER - OpenGL ES 3.0 (GLSL 300 es)
        // ═══════════════════════════════════════════════════════════════
        String fragmentShaderCode =
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "in vec2 v_TexCoord;\n" +
            "out vec4 fragColor;\n" +
            "uniform float u_Time;\n" +
            "uniform vec2 u_Center;\n" +
            "uniform float u_Size;\n" +
            "uniform float u_Shape;\n" +  // 0 = play, 1 = stop
            "uniform float u_Aspect;\n" +
            "uniform float u_Alpha;\n" +
            "\n" +
            "#define PI 3.14159265359\n" +
            "#define TWO_PI 6.28318530718\n" +
            "\n" +
            "// ⚡ Tiempo normalizado para evitar overflow en floats\n" +
            "// El tiempo ya viene acotado desde Java, pero usamos mod() extra por seguridad\n" +
            "#define SAFE_TIME(t) mod(t, TWO_PI * 10.0)\n" +
            "\n" +
            "// Suavizado de bordes\n" +
            "float smoothEdge(float d, float w) {\n" +
            "    return smoothstep(w, -w, d);\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "    // ⚡ Tiempo seguro - ya viene acotado pero aplicamos mod extra\n" +
            "    float t = SAFE_TIME(u_Time);\n" +
            "    \n" +
            "    // Coordenadas ajustadas por aspect ratio\n" +
            "    vec2 uv = v_TexCoord;\n" +
            "    uv.x = (uv.x - 0.5) * u_Aspect + 0.5;\n" +
            "    \n" +
            "    vec2 center = u_Center;\n" +
            "    center.x = (center.x - 0.5) * u_Aspect + 0.5;\n" +
            "    \n" +
            "    vec2 delta = uv - center;\n" +
            "    float dist = length(delta);\n" +
            "    float angle = atan(delta.y, delta.x);\n" +
            "    \n" +
            "    vec3 finalColor = vec3(0.0);\n" +
            "    float finalAlpha = 0.0;\n" +
            "    \n" +
            "    // ═══════════════════════════════════════════════════════════\n" +
            "    // 🌟 AURA EXTERIOR - Resplandor místico pulsante\n" +
            "    // ═══════════════════════════════════════════════════════════\n" +
            "    float auraSize = u_Size * 2.5;\n" +
            "    float auraPulse = sin(t * 1.5) * 0.15 + 1.0;\n" +
            "    float aura = smoothstep(auraSize * auraPulse, u_Size * 0.8, dist);\n" +
            "    \n" +
            "    // Color del aura según estado (cyan para play, magenta para stop)\n" +
            "    vec3 auraColorPlay = vec3(0.0, 0.8, 1.0);   // Cyan brillante\n" +
            "    vec3 auraColorStop = vec3(1.0, 0.2, 0.6);   // Magenta\n" +
            "    vec3 auraColor = mix(auraColorPlay, auraColorStop, u_Shape);\n" +
            "    \n" +
            "    // Gradiente de color que rota\n" +
            "    float colorShift = sin(angle * 2.0 + t * 2.0) * 0.5 + 0.5;\n" +
            "    vec3 auraColor2 = mix(auraColor, vec3(0.5, 0.0, 1.0), colorShift * 0.3);\n" +
            "    \n" +
            "    finalColor += auraColor2 * aura * 0.4;\n" +
            "    finalAlpha += aura * 0.3;\n" +
            "    \n" +
            "    // ═══════════════════════════════════════════════════════════\n" +
            "    // ✨ PARTÍCULAS ORBITANDO - 6 partículas mágicas\n" +
            "    // ═══════════════════════════════════════════════════════════\n" +
            "    float orbitRadius = u_Size * 1.3;\n" +
            "    for (int i = 0; i < 6; i++) {\n" +
            "        float fi = float(i);\n" +
            "        float particleAngle = fi * (TWO_PI / 6.0) + t * (0.8 + fi * 0.1);\n" +
            "        vec2 particlePos = center + vec2(cos(particleAngle), sin(particleAngle)) * orbitRadius;\n" +
            "        \n" +
            "        float particleDist = length(uv - particlePos);\n" +
            "        float particleSize = 0.012 + sin(t * 3.0 + fi) * 0.004;\n" +
            "        float particle = smoothstep(particleSize, 0.0, particleDist);\n" +
            "        \n" +
            "        // Color de partícula (arcoíris rotando)\n" +
            "        vec3 particleColor = vec3(\n" +
            "            sin(fi * 1.0 + t) * 0.5 + 0.5,\n" +
            "            sin(fi * 1.0 + t + 2.094) * 0.5 + 0.5,\n" +
            "            sin(fi * 1.0 + t + 4.188) * 0.5 + 0.5\n" +
            "        );\n" +
            "        \n" +
            "        finalColor += particleColor * particle * 1.5;\n" +
            "        finalAlpha += particle * 0.8;\n" +
            "    }\n" +
            "    \n" +
            "    // ═══════════════════════════════════════════════════════════\n" +
            "    // 🔮 ANILLO EXTERIOR - Borde brillante con ondas\n" +
            "    // ═══════════════════════════════════════════════════════════\n" +
            "    float ringRadius = u_Size * 1.05;\n" +
            "    float ringWidth = 0.012;\n" +
            "    float wave = sin(angle * 8.0 + t * 4.0) * 0.003;\n" +
            "    float ring = smoothEdge(abs(dist - ringRadius - wave) - ringWidth, 0.008);\n" +
            "    \n" +
            "    // Color del anillo (gradiente rotativo)\n" +
            "    vec3 ringColor1 = mix(vec3(0.2, 1.0, 0.8), vec3(0.8, 0.2, 1.0), u_Shape);\n" +
            "    vec3 ringColor2 = mix(vec3(0.0, 0.5, 1.0), vec3(1.0, 0.5, 0.0), u_Shape);\n" +
            "    float ringGradient = sin(angle * 2.0 - t * 3.0) * 0.5 + 0.5;\n" +
            "    vec3 ringColor = mix(ringColor1, ringColor2, ringGradient);\n" +
            "    \n" +
            "    finalColor += ringColor * ring * 1.2;\n" +
            "    finalAlpha += ring * 0.9;\n" +
            "    \n" +
            "    // ═══════════════════════════════════════════════════════════\n" +
            "    // 🌑 CÍRCULO DE FONDO - Cristal oscuro con reflejos\n" +
            "    // ═══════════════════════════════════════════════════════════\n" +
            "    float bgRadius = u_Size;\n" +
            "    float bg = smoothEdge(dist - bgRadius, 0.015);\n" +
            "    \n" +
            "    // Fondo con gradiente sutil\n" +
            "    vec3 bgColor = vec3(0.02, 0.02, 0.05);\n" +
            "    float bgGradient = 1.0 - dist / bgRadius;\n" +
            "    bgColor += vec3(0.05, 0.02, 0.08) * bgGradient * bg;\n" +
            "    \n" +
            "    // Reflejo de luz en el borde superior\n" +
            "    float highlight = smoothstep(0.3, 0.7, (delta.y / u_Size + 1.0) * 0.5);\n" +
            "    bgColor += vec3(0.1, 0.1, 0.15) * highlight * bg * 0.5;\n" +
            "    \n" +
            "    finalColor = mix(finalColor, bgColor, bg * 0.85);\n" +
            "    finalAlpha = mix(finalAlpha, 0.92, bg);\n" +
            "    \n" +
            "    // ═══════════════════════════════════════════════════════════\n" +
            "    // ▶️ ICONO PLAY/STOP - Con efecto holográfico\n" +
            "    // ═══════════════════════════════════════════════════════════\n" +
            "    vec2 localUV = delta / u_Size;\n" +
            "    float iconMask = 0.0;\n" +
            "    \n" +
            "    if (u_Shape < 0.5) {\n" +
            "        // PLAY TRIANGLE ▶\n" +
            "        vec2 p = localUV * 1.8;\n" +
            "        p.x -= 0.15;  // Centrar visualmente\n" +
            "        \n" +
            "        float edge1 = p.x + 0.45;\n" +
            "        float edge2 = 0.65 - p.x - abs(p.y) * 1.1;\n" +
            "        float triDist = min(edge1, edge2);\n" +
            "        iconMask = smoothstep(0.0, 0.1, triDist);\n" +
            "    } else {\n" +
            "        // STOP SQUARE ■\n" +
            "        vec2 p = abs(localUV) * 1.5;\n" +
            "        float sqDist = max(p.x, p.y);\n" +
            "        iconMask = smoothstep(0.45, 0.35, sqDist);\n" +
            "    }\n" +
            "    \n" +
            "    // Efecto holográfico en el icono\n" +
            "    float holoWave = sin(localUV.y * 30.0 + t * 5.0) * 0.5 + 0.5;\n" +
            "    float holoShimmer = sin(t * 8.0 + angle * 4.0) * 0.3 + 0.7;\n" +
            "    \n" +
            "    // Color del icono\n" +
            "    vec3 iconColorPlay = vec3(0.3, 1.0, 0.5);   // Verde neón\n" +
            "    vec3 iconColorStop = vec3(1.0, 0.3, 0.4);   // Rojo suave\n" +
            "    vec3 iconColor = mix(iconColorPlay, iconColorStop, u_Shape);\n" +
            "    \n" +
            "    // Agregar variación holográfica\n" +
            "    iconColor += vec3(0.2, 0.1, 0.3) * holoWave * 0.3;\n" +
            "    iconColor *= holoShimmer;\n" +
            "    \n" +
            "    // Glow del icono\n" +
            "    float iconGlow = iconMask * (0.8 + sin(t * 2.0) * 0.2);\n" +
            "    \n" +
            "    finalColor = mix(finalColor, iconColor, iconMask * bg);\n" +
            "    finalColor += iconColor * iconGlow * 0.3 * bg;\n" +
            "    finalAlpha = max(finalAlpha, iconMask * bg);\n" +
            "    \n" +
            "    // ═══════════════════════════════════════════════════════════\n" +
            "    // ⚡ DESTELLOS OCASIONALES - Sparkles mágicos\n" +
            "    // ═══════════════════════════════════════════════════════════\n" +
            "    float sparkleTime = mod(t, 3.0);\n" +
            "    if (sparkleTime < 0.3) {\n" +
            "        float sparklePhase = sparkleTime / 0.3;\n" +
            "        float sparkleIntensity = sin(sparklePhase * PI) * 2.0;\n" +
            "        \n" +
            "        // Posición del destello (rota alrededor) - usar mod para limitar\n" +
            "        float sparkleAngle = mod(t / 3.0, 20.0) * 2.5;\n" +
            "        vec2 sparklePos = center + vec2(cos(sparkleAngle), sin(sparkleAngle)) * u_Size * 0.7;\n" +
            "        float sparkleDist = length(uv - sparklePos);\n" +
            "        float sparkle = smoothstep(0.03, 0.0, sparkleDist) * sparkleIntensity;\n" +
            "        \n" +
            "        finalColor += vec3(1.0, 1.0, 1.0) * sparkle;\n" +
            "        finalAlpha += sparkle * 0.5;\n" +
            "    }\n" +
            "    \n" +
            "    // ═══════════════════════════════════════════════════════════\n" +
            "    // 🌈 BORDE INTERIOR LUMINOSO\n" +
            "    // ═══════════════════════════════════════════════════════════\n" +
            "    float innerRing = smoothEdge(abs(dist - u_Size * 0.95) - 0.006, 0.005);\n" +
            "    vec3 innerColor = mix(vec3(0.5, 1.0, 0.8), vec3(1.0, 0.5, 0.8), u_Shape);\n" +
            "    innerColor *= 0.8 + sin(t * 4.0) * 0.2;\n" +
            "    \n" +
            "    finalColor += innerColor * innerRing * 0.6;\n" +
            "    finalAlpha += innerRing * 0.4;\n" +
            "    \n" +
            "    // Aplicar alpha global\n" +
            "    finalAlpha *= u_Alpha;\n" +
            "    \n" +
            "    fragColor = vec4(finalColor, finalAlpha);\n" +
            "}\n";

        int vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode);

        if (vertexShader == 0 || fragmentShader == 0) {
            Log.e(TAG, "Error compilando shaders GL3.0");
            return;
        }

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vertexShader);
        GLES30.glAttachShader(shaderProgram, fragmentShader);
        GLES30.glLinkProgram(shaderProgram);

        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(shaderProgram, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Error linking: " + GLES30.glGetProgramInfoLog(shaderProgram));
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
            return;
        }

        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Time");
        uCenterLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Center");
        uSizeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Size");
        uShapeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Shape");
        uAspectLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Aspect");
        uAlphaLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Alpha");

        ByteBuffer bb = ByteBuffer.allocateDirect(QUAD_VERTICES.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(QUAD_VERTICES);
        vertexBuffer.position(0);

        shadersCompiled = true;
        Log.d(TAG, "✨ PlayPauseButton shaders GL3.0 compilados");
    }

    private int compileShader(int type, String code) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, code);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader error: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    @Override
    public void update(float dt) {
        // ⚡ CRÍTICO: Mantener time acotado para evitar pérdida de precisión en GPU
        time += dt;
        if (time > TIME_CYCLE) {
            time -= TIME_CYCLE;  // Wrap suave sin saltos visuales
        }

        // Auto-hide cuando está animando
        if (isPlaying) {
            fadeTimer += dt;
            if (fadeTimer > AUTO_HIDE_DELAY) {
                float progress = (fadeTimer - AUTO_HIDE_DELAY) / FADE_DURATION;
                fadeAlpha = Math.max(0.0f, 1.0f - progress);
            }
        } else {
            fadeTimer = 0f;
            fadeAlpha = 1.0f;
        }
    }

    @Override
    public void draw() {
        if (!isVisible || !shadersCompiled || shaderProgram == 0) return;
        if (fadeAlpha <= 0.01f) return;

        GLES30.glUseProgram(shaderProgram);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        vertexBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        // Uniforms
        GLES30.glUniform1f(uTimeLoc, time);
        GLES30.glUniform2f(uCenterLoc, (centerX + 1.0f) * 0.5f, (centerY + 1.0f) * 0.5f);
        GLES30.glUniform1f(uSizeLoc, size * 0.5f);
        GLES30.glUniform1f(uShapeLoc, isPlaying ? 1.0f : 0.0f);
        GLES30.glUniform1f(uAspectLoc, aspectRatio);
        GLES30.glUniform1f(uAlphaLoc, fadeAlpha);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        // Limpiar estado GL
        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glUseProgram(0);
    }

    public boolean isInside(float touchX, float touchY) {
        float hitSize = size * hitAreaMultiplier;
        float dx = (touchX - centerX) / aspectRatio;
        float dy = touchY - centerY;
        return (dx * dx + dy * dy) <= hitSize * hitSize;
    }

    public boolean toggle() {
        isPlaying = !isPlaying;
        fadeTimer = 0f;
        fadeAlpha = 1.0f;
        Log.d(TAG, isPlaying ? "▶ PLAY" : "■ STOP");
        return isPlaying;
    }

    public void show() {
        fadeTimer = 0f;
        fadeAlpha = 1.0f;
    }

    public boolean isPlaying() { return isPlaying; }
    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        fadeTimer = 0f;
        fadeAlpha = 1.0f;
    }

    /**
     * ⚡ Resetea el tiempo interno - llamar cuando cambia de estado de visibilidad
     * para evitar acumulación de tiempo que causa pérdida de precisión
     */
    public void resetTime() {
        time = 0f;
    }
    public void setPosition(float x, float y) { centerX = x; centerY = y; }
    public void setSize(float size) { this.size = size; }
    public void setAspectRatio(float aspect) { this.aspectRatio = aspect; }
    public void setVisible(boolean visible) { this.isVisible = visible; }
    public boolean isVisible() { return isVisible && fadeAlpha > 0.01f; }
    public void setAlpha(float alpha) { this.fadeAlpha = alpha; }
}
