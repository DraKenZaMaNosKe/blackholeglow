package com.secret.blackholeglow.christmas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import com.secret.blackholeglow.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║   🎄 ChristmasPanelBackground - Fondo con Humo Animado                    ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║   Distorsiona suavemente el área del humo para dar efecto de movimiento  ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */
public class ChristmasPanelBackground {
    private static final String TAG = "ChristmasPanelBg";

    // 📍 Área del humo en coordenadas de textura (0-1)
    // Ajustar estos valores para mover el área de distorsión
    private static final float SMOKE_CENTER_X = 0.72f;  // Posición X del humo en la imagen
    private static final float SMOKE_CENTER_Y = 0.22f;  // Posición Y del humo (arriba = 0)
    private static final float SMOKE_RADIUS = 0.15f;    // Radio del área afectada

    private int shaderProgram;
    private int textureId;
    private FloatBuffer vertexBuffer;

    private int uTextureLoc, uAlphaLoc, uTimeLoc, aPositionLoc, aTexCoordLoc;

    private boolean initialized = false;
    private boolean visible = true;
    private float alpha = 1.0f;
    private float time = 0f;
    private Context context;

    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "in vec2 a_Position;\n" +
        "in vec2 a_TexCoord;\n" +
        "out vec2 v_TexCoord;\n" +
        "void main() {\n" +
        "    v_TexCoord = a_TexCoord;\n" +
        "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "in vec2 v_TexCoord;\n" +
        "uniform sampler2D u_Texture;\n" +
        "uniform float u_Alpha;\n" +
        "uniform float u_Time;\n" +
        "out vec4 fragColor;\n" +
        "\n" +
        "void main() {\n" +
        "    vec2 uv = v_TexCoord;\n" +
        "    \n" +
        "    // 🔧 Tiempo con ciclo muy largo (10 minutos = 600s)\n" +
        "    float t = mod(u_Time, 600.0);\n" +
        "    \n" +
        "    // ═══════════════════════════════════════════════════════\n" +
        "    // 🌌 AURORAS BOREALES (zona superior + color verde/cyan)\n" +
        "    // ═══════════════════════════════════════════════════════\n" +
        "    if (uv.y < 0.40) {\n" +
        "        vec4 col = texture(u_Texture, uv);\n" +
        "        \n" +
        "        // Detectar colores de aurora: verde o cyan o magenta\n" +
        "        float hasGreen = col.g;\n" +
        "        float hasColor = max(col.g, col.b) - col.r * 0.3;\n" +
        "        \n" +
        "        // Zona vertical (más fuerte arriba)\n" +
        "        float zone = smoothstep(0.40, 0.08, uv.y);\n" +
        "        \n" +
        "        // Combinar\n" +
        "        float auroraFactor = zone * smoothstep(0.2, 0.5, hasColor);\n" +
        "        \n" +
        "        // Movimiento orgánico\n" +
        "        float wave = sin(uv.x * 3.5 + t * 0.3) * 0.010\n" +
        "                   + sin(uv.x * 6.0 - t * 0.2 + 2.0) * 0.007;\n" +
        "        uv.y += wave * auroraFactor;\n" +
        "    }\n" +
        "    \n" +
        "    // ═══════════════════════════════════════════════════════\n" +
        "    // 🌫️ HUMO: Solo base, área muy pequeña, transición MUY suave\n" +
        "    // ═══════════════════════════════════════════════════════\n" +
        "    vec2 smokeBase = vec2(0.77, 0.30);\n" +
        "    \n" +
        "    // Distancia elíptica (más alto que ancho)\n" +
        "    vec2 diff = uv - smokeBase;\n" +
        "    diff.x *= 2.5;  // Más angosto horizontalmente\n" +
        "    float dist = length(diff);\n" +
        "    \n" +
        "    // Transición MUY suave desde el centro hacia afuera\n" +
        "    float smokeFactor = 1.0 - smoothstep(0.0, 0.08, dist);\n" +
        "    smokeFactor = smokeFactor * smokeFactor;  // Curva cuadrática = más suave\n" +
        "    \n" +
        "    // Solo aplicar si hay efecto significativo\n" +
        "    if (smokeFactor > 0.001) {\n" +
        "        // Onda horizontal muy sutil\n" +
        "        float wave = sin(uv.y * 20.0 - t * 1.0) * 0.004 * smokeFactor;\n" +
        "        uv.x += wave;\n" +
        "    }\n" +
        "    \n" +
        "    vec4 texColor = texture(u_Texture, uv);\n" +
        "    fragColor = vec4(texColor.rgb, texColor.a * u_Alpha);\n" +
        "}\n";

    public ChristmasPanelBackground(Context context) {
        this.context = context;
    }

    public void update(float dt) {
        time += dt;
        if (time > 1000f) time = 0f;  // Reset para evitar overflow
    }

    private void initOpenGL() {
        if (initialized) return;

        Log.d(TAG, "🔧 Inicializando OpenGL con distorsión de humo...");

        int vs = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        if (vs == 0 || fs == 0) {
            Log.e(TAG, "❌ Error en shaders");
            return;
        }

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        int[] status = new int[1];
        GLES30.glGetProgramiv(shaderProgram, GLES30.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            Log.e(TAG, "❌ Error link: " + GLES30.glGetProgramInfoLog(shaderProgram));
            GLES30.glDeleteProgram(shaderProgram);
            return;
        }

        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        aTexCoordLoc = GLES30.glGetAttribLocation(shaderProgram, "a_TexCoord");
        uTextureLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Texture");
        uAlphaLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Alpha");
        uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Time");

        float[] vertices = {
            -1.0f, -1.0f,  0.0f, 1.0f,
             1.0f, -1.0f,  1.0f, 1.0f,
            -1.0f,  1.0f,  0.0f, 0.0f,
             1.0f,  1.0f,  1.0f, 0.0f
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        loadTexture();

        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        initialized = true;
        Log.d(TAG, "✅ Fondo con humo animado inicializado");
    }

    private void loadTexture() {
        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        textureId = textures[0];

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;

        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.christmas_bg, options);
        if (bitmap != null) {
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();
            Log.d(TAG, "✅ Textura cargada");
        }
    }

    private int compileShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
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

    public void draw() {
        if (!visible) return;

        if (!initialized) {
            initOpenGL();
            if (!initialized) return;
        }

        GLES30.glUseProgram(shaderProgram);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(uTextureLoc, 0);
        GLES30.glUniform1f(uAlphaLoc, alpha);
        GLES30.glUniform1f(uTimeLoc, time);

        vertexBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);

        vertexBuffer.position(2);
        GLES30.glEnableVertexAttribArray(aTexCoordLoc);
        GLES30.glVertexAttribPointer(aTexCoordLoc, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aTexCoordLoc);
    }

    public void show() { visible = true; }
    public void hide() { visible = false; }
    public void setAlpha(float a) { alpha = Math.max(0f, Math.min(1f, a)); }
    public void setAspectRatio(float r) { }

    public void dispose() {
        if (shaderProgram != 0) { GLES30.glDeleteProgram(shaderProgram); shaderProgram = 0; }
        if (textureId != 0) { GLES30.glDeleteTextures(1, new int[]{textureId}, 0); textureId = 0; }
        initialized = false;
    }
}
