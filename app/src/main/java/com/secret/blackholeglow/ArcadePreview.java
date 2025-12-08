package com.secret.blackholeglow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   🖼️ ARCADE PREVIEW - Imagen promocional de la batalla                  ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Muestra una imagen estática de las naves en combate                     ║
 * ║  Con borde brillante estilo arcade                                       ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class ArcadePreview implements SceneObject {
    private static final String TAG = "ArcadePreview";

    // Textura
    private int textureId = 0;
    private int borderTextureId = 0;
    private Bitmap previewBitmap;

    // OpenGL
    private int shaderProgram = 0;
    private int borderShaderProgram = 0;
    private FloatBuffer vertexBuffer;
    private int aPositionLoc, aTexCoordLoc;
    private int uTextureLoc, uAlphaLoc, uTimeLoc;

    // Border shader locs
    private int bAPositionLoc, bUTimeLoc, bUAspectLoc;

    // Estado
    private float alpha = 1.0f;
    private float time = 0f;
    private float aspectRatio = 1.0f;
    private boolean initialized = false;
    private boolean isVisible = true;
    private Context context;

    // Posición y tamaño
    private float posY = -0.42f;
    private float width = 0.85f;
    private float height = 0.22f;

    public ArcadePreview(Context context) {
        this.context = context;
        Log.d(TAG, "🖼️ Creando ArcadePreview...");
        initOpenGL();
        loadPreviewTexture();
    }

    private void initOpenGL() {
        // Shader para la imagen
        String vertexShader =
            "attribute vec2 a_Position;\n" +
            "attribute vec2 a_TexCoord;\n" +
            "varying vec2 v_TexCoord;\n" +
            "void main() {\n" +
            "    v_TexCoord = a_TexCoord;\n" +
            "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
            "}\n";

        String fragmentShader =
            "precision mediump float;\n" +
            "varying vec2 v_TexCoord;\n" +
            "uniform sampler2D u_Texture;\n" +
            "uniform float u_Alpha;\n" +
            "uniform float u_Time;\n" +
            "void main() {\n" +
            "    vec4 texColor = texture2D(u_Texture, v_TexCoord);\n" +
            "    // Efecto de scanlines retro\n" +
            "    float scanline = sin(v_TexCoord.y * 200.0) * 0.03 + 0.97;\n" +
            "    texColor.rgb *= scanline;\n" +
            "    gl_FragColor = vec4(texColor.rgb, texColor.a * u_Alpha);\n" +
            "}\n";

        int vs = compileShader(GLES30.GL_VERTEX_SHADER, vertexShader);
        int fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShader);

        if (vs == 0 || fs == 0) {
            Log.e(TAG, "Error compilando shaders de imagen");
            return;
        }

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        aTexCoordLoc = GLES30.glGetAttribLocation(shaderProgram, "a_TexCoord");
        uTextureLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Texture");
        uAlphaLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Alpha");
        uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Time");

        // Shader para el borde brillante
        String borderVS =
            "attribute vec2 a_Position;\n" +
            "varying vec2 v_UV;\n" +
            "void main() {\n" +
            "    v_UV = a_Position * 0.5 + 0.5;\n" +
            "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
            "}\n";

        String borderFS =
            "precision mediump float;\n" +
            "varying vec2 v_UV;\n" +
            "uniform float u_Time;\n" +
            "uniform float u_Aspect;\n" +
            "void main() {\n" +
            "    vec2 uv = v_UV;\n" +
            "    uv.x *= u_Aspect;\n" +
            "    \n" +
            "    // Calcular distancia al borde\n" +
            "    float borderWidth = 0.03;\n" +
            "    float edgeX = min(uv.x, u_Aspect - uv.x);\n" +
            "    float edgeY = min(uv.y, 1.0 - uv.y);\n" +
            "    float edge = min(edgeX, edgeY);\n" +
            "    \n" +
            "    // Solo dibujar el borde\n" +
            "    if (edge > borderWidth) discard;\n" +
            "    \n" +
            "    // Color del borde (cyan brillante animado)\n" +
            "    float pulse = sin(u_Time * 3.0) * 0.3 + 0.7;\n" +
            "    float travel = sin((uv.x + uv.y) * 10.0 - u_Time * 5.0) * 0.5 + 0.5;\n" +
            "    vec3 color = mix(vec3(0.0, 0.8, 1.0), vec3(0.5, 0.0, 1.0), travel);\n" +
            "    color *= pulse;\n" +
            "    \n" +
            "    float a = 1.0 - (edge / borderWidth);\n" +
            "    gl_FragColor = vec4(color, a * 0.9);\n" +
            "}\n";

        int bvs = compileShader(GLES30.GL_VERTEX_SHADER, borderVS);
        int bfs = compileShader(GLES30.GL_FRAGMENT_SHADER, borderFS);

        if (bvs != 0 && bfs != 0) {
            borderShaderProgram = GLES30.glCreateProgram();
            GLES30.glAttachShader(borderShaderProgram, bvs);
            GLES30.glAttachShader(borderShaderProgram, bfs);
            GLES30.glLinkProgram(borderShaderProgram);

            bAPositionLoc = GLES30.glGetAttribLocation(borderShaderProgram, "a_Position");
            bUTimeLoc = GLES30.glGetUniformLocation(borderShaderProgram, "u_Time");
            bUAspectLoc = GLES30.glGetUniformLocation(borderShaderProgram, "u_Aspect");
        }

        // Vertex buffer
        float[] vertices = {
            -1f, -1f, 0f, 1f,
             1f, -1f, 1f, 1f,
            -1f,  1f, 0f, 0f,
             1f,  1f, 1f, 0f
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // Crear textura
        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        textureId = textures[0];

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        initialized = true;
        Log.d(TAG, "✅ OpenGL inicializado");
    }

    private void loadPreviewTexture() {
        if (context == null) return;

        try {
            // Intentar cargar imagen de preview (si existe)
            int resId = context.getResources().getIdentifier(
                "preview_batalla", "drawable", context.getPackageName());

            if (resId != 0) {
                previewBitmap = BitmapFactory.decodeResource(context.getResources(), resId);
            }

            if (previewBitmap == null) {
                // Crear bitmap procedural si no hay imagen
                previewBitmap = createProceduralPreview();
            }

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, previewBitmap, 0);
            Log.d(TAG, "✅ Textura de preview cargada");

        } catch (Exception e) {
            Log.e(TAG, "Error cargando preview: " + e.getMessage());
            previewBitmap = createProceduralPreview();
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, previewBitmap, 0);
        }
    }

    /**
     * Crea una imagen procedural si no hay textura
     */
    private Bitmap createProceduralPreview() {
        int w = 512;
        int h = 256;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);

        // Fondo espacial con gradiente
        android.graphics.Paint bgPaint = new android.graphics.Paint();
        android.graphics.LinearGradient gradient = new android.graphics.LinearGradient(
            0, 0, 0, h,
            new int[]{0xFF000022, 0xFF000044, 0xFF000033},
            new float[]{0f, 0.5f, 1f},
            android.graphics.Shader.TileMode.CLAMP
        );
        bgPaint.setShader(gradient);
        canvas.drawRect(0, 0, w, h, bgPaint);

        // Estrellas
        android.graphics.Paint starPaint = new android.graphics.Paint();
        starPaint.setColor(0xFFFFFFFF);
        java.util.Random rand = new java.util.Random(42);
        for (int i = 0; i < 50; i++) {
            float x = rand.nextFloat() * w;
            float y = rand.nextFloat() * h;
            float size = rand.nextFloat() * 2 + 1;
            canvas.drawCircle(x, y, size, starPaint);
        }

        // Texto "BATTLE PREVIEW"
        android.graphics.Paint textPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF00FFFF);
        textPaint.setTextSize(36);
        textPaint.setTextAlign(android.graphics.Paint.Align.CENTER);
        textPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));

        // Glow
        android.graphics.Paint glowPaint = new android.graphics.Paint(textPaint);
        glowPaint.setColor(0xFF0088AA);
        glowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(8, android.graphics.BlurMaskFilter.Blur.NORMAL));

        canvas.drawText("⚔ BATTLE PREVIEW ⚔", w/2f, h/2f, glowPaint);
        canvas.drawText("⚔ BATTLE PREVIEW ⚔", w/2f, h/2f, textPaint);

        // Subtítulo
        textPaint.setTextSize(20);
        textPaint.setColor(0xFFFFAA00);
        canvas.drawText("Defend Earth from Alien Invasion!", w/2f, h/2f + 40, textPaint);

        return bmp;
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
    public void update(float deltaTime) {
        time += deltaTime;
        if (time > 100f) time -= 100f;
    }

    @Override
    public void draw() {
        if (!initialized || shaderProgram == 0 || !isVisible) return;

        float halfWidth = width / aspectRatio;
        float halfHeight = height;

        float[] vertices = {
            -halfWidth, posY - halfHeight, 0f, 1f,
             halfWidth, posY - halfHeight, 1f, 1f,
            -halfWidth, posY + halfHeight, 0f, 0f,
             halfWidth, posY + halfHeight, 1f, 0f
        };

        vertexBuffer.clear();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // Dibujar imagen
        GLES30.glUseProgram(shaderProgram);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glEnableVertexAttribArray(aTexCoordLoc);

        vertexBuffer.position(0);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);
        vertexBuffer.position(2);
        GLES30.glVertexAttribPointer(aTexCoordLoc, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(uTextureLoc, 0);
        GLES30.glUniform1f(uAlphaLoc, alpha);
        GLES30.glUniform1f(uTimeLoc, time);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aTexCoordLoc);

        // Dibujar borde brillante
        if (borderShaderProgram != 0) {
            GLES30.glUseProgram(borderShaderProgram);

            float[] borderVerts = {
                -halfWidth, posY - halfHeight,
                 halfWidth, posY - halfHeight,
                -halfWidth, posY + halfHeight,
                 halfWidth, posY + halfHeight
            };

            ByteBuffer bb = ByteBuffer.allocateDirect(borderVerts.length * 4);
            bb.order(ByteOrder.nativeOrder());
            FloatBuffer borderBuffer = bb.asFloatBuffer();
            borderBuffer.put(borderVerts);
            borderBuffer.position(0);

            GLES30.glEnableVertexAttribArray(bAPositionLoc);
            GLES30.glVertexAttribPointer(bAPositionLoc, 2, GLES30.GL_FLOAT, false, 0, borderBuffer);

            GLES30.glUniform1f(bUTimeLoc, time);
            GLES30.glUniform1f(bUAspectLoc, width / height);

            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

            GLES30.glDisableVertexAttribArray(bAPositionLoc);
        }
    }

    public void setAspectRatio(float ratio) {
        this.aspectRatio = ratio;
    }

    public void show() {
        this.isVisible = true;
    }

    public void hide() {
        this.isVisible = false;
    }

    public void dispose() {
        if (textureId != 0) {
            int[] textures = {textureId};
            GLES30.glDeleteTextures(1, textures, 0);
        }
        if (previewBitmap != null) {
            previewBitmap.recycle();
        }
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
        }
        if (borderShaderProgram != 0) {
            GLES30.glDeleteProgram(borderShaderProgram);
        }
        Log.d(TAG, "🧹 ArcadePreview disposed");
    }
}
