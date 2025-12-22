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
 * ║   🎄 ChristmasPanelBackground - Fondo Estático del Panel Navideño        ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Renderiza la imagen christmas_background.png como fondo del panel.
 * Sin shaders complejos, solo una textura estática = RÁPIDO y BONITO.
 */
public class ChristmasPanelBackground {
    private static final String TAG = "ChristmasPanelBg";

    // OpenGL
    private int shaderProgram;
    private int textureId;
    private FloatBuffer vertexBuffer;

    // Uniforms
    private int uTextureLoc;
    private int uAlphaLoc;
    private int aPositionLoc;
    private int aTexCoordLoc;

    // Estado
    private boolean initialized = false;
    private boolean visible = true;
    private float alpha = 1.0f;
    private float aspectRatio = 1.0f;

    // Context para cargar recursos
    private Context context;

    // Vertex shader simple (GLES 3.0)
    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "in vec2 a_Position;\n" +
        "in vec2 a_TexCoord;\n" +
        "out vec2 v_TexCoord;\n" +
        "void main() {\n" +
        "    v_TexCoord = a_TexCoord;\n" +
        "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
        "}\n";

    // Fragment shader simple (GLES 3.0)
    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "in vec2 v_TexCoord;\n" +
        "uniform sampler2D u_Texture;\n" +
        "uniform float u_Alpha;\n" +
        "out vec4 fragColor;\n" +
        "void main() {\n" +
        "    vec4 texColor = texture(u_Texture, v_TexCoord);\n" +
        "    fragColor = vec4(texColor.rgb, texColor.a * u_Alpha);\n" +
        "}\n";

    public ChristmasPanelBackground(Context context) {
        this.context = context;
        init();
    }

    private void init() {
        // Crear shader program
        int vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        if (vertexShader == 0 || fragmentShader == 0) {
            Log.e(TAG, "Error compilando shaders");
            return;
        }

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vertexShader);
        GLES30.glAttachShader(shaderProgram, fragmentShader);
        GLES30.glLinkProgram(shaderProgram);

        // Verificar linkeo
        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(shaderProgram, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Error linkeando programa");
            GLES30.glDeleteProgram(shaderProgram);
            return;
        }

        // Obtener locations
        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        aTexCoordLoc = GLES30.glGetAttribLocation(shaderProgram, "a_TexCoord");
        uTextureLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Texture");
        uAlphaLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Alpha");

        // Crear vertex buffer (fullscreen quad)
        // Vertices: posición (x,y) + texcoord (u,v)
        float[] vertices = {
            // Posición      // TexCoord
            -1.0f, -1.0f,    0.0f, 1.0f,  // Bottom-left
             1.0f, -1.0f,    1.0f, 1.0f,  // Bottom-right
            -1.0f,  1.0f,    0.0f, 0.0f,  // Top-left
             1.0f,  1.0f,    1.0f, 0.0f   // Top-right
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // Cargar textura
        loadTexture();

        // Eliminar shaders (ya están en el programa)
        GLES30.glDeleteShader(vertexShader);
        GLES30.glDeleteShader(fragmentShader);

        initialized = true;
        Log.d(TAG, "✅ ChristmasPanelBackground inicializado");
    }

    private void loadTexture() {
        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        textureId = textures[0];

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);

        // Parámetros de textura
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        // Cargar imagen
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;  // No escalar

        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),
            R.drawable.christmas_bg, options);

        if (bitmap != null) {
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();
            Log.d(TAG, "✅ Textura christmas_bg cargada");
        } else {
            Log.e(TAG, "❌ Error cargando christmas_bg");
        }
    }

    private int compileShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Error compilando shader: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    public void draw() {
        if (!initialized || !visible || shaderProgram == 0) return;

        GLES30.glUseProgram(shaderProgram);

        // Bind textura
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(uTextureLoc, 0);
        GLES30.glUniform1f(uAlphaLoc, alpha);

        // Configurar vertex attributes
        vertexBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);

        vertexBuffer.position(2);
        GLES30.glEnableVertexAttribArray(aTexCoordLoc);
        GLES30.glVertexAttribPointer(aTexCoordLoc, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);

        // Dibujar
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        // Cleanup
        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aTexCoordLoc);
    }

    public void show() {
        visible = true;
    }

    public void hide() {
        visible = false;
    }

    public void setAlpha(float alpha) {
        this.alpha = Math.max(0f, Math.min(1f, alpha));
    }

    public void setAspectRatio(float ratio) {
        this.aspectRatio = ratio;
    }

    public void dispose() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        if (textureId != 0) {
            int[] textures = {textureId};
            GLES30.glDeleteTextures(1, textures, 0);
            textureId = 0;
        }
        initialized = false;
        Log.d(TAG, "🗑️ ChristmasPanelBackground liberado");
    }
}
