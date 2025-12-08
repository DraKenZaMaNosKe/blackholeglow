package com.secret.blackholeglow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Calendar;

/**
 * GreetingText - Saludo personalizado que aparece ocasionalmente
 * Muestra "Buenos días/tardes/noches: [nombre de usuario]"
 */
public class GreetingText implements SceneObject {
    private static final String TAG = "GreetingText";

    private final Context context;
    private int programId;
    private int textureId;
    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;

    // Estado de visibilidad
    private float alpha = 0f;  // 0 = invisible, 1 = visible
    private float displayTimer = 0f;
    private float cycleTimer = 0f;

    // Tiempos de ciclo (en segundos)
    private static final float CYCLE_DURATION = 30.0f;  // Cada 30 segundos
    private static final float FADE_IN_TIME = 1.0f;      // 1 segundo para aparecer
    private static final float DISPLAY_TIME = 5.0f;      // 5 segundos visible
    private static final float FADE_OUT_TIME = 1.0f;     // 1 segundo para desaparecer

    private enum State {
        HIDDEN,
        FADING_IN,
        DISPLAYING,
        FADING_OUT
    }

    private State currentState = State.HIDDEN;

    // Posición y tamaño - CENTRADO ARRIBA (debajo del sol)
    private float x = 0.15f;   // Centrado horizontalmente
    private float y = 0.60f;   // Arriba de la escena (debajo del sol)
    private float width = 0.7f;   // 70% del ancho de pantalla
    private float height = 0.10f; // 10% de altura

    private String greetingText = "";
    private String userName = "";

    public GreetingText(Context context) {
        this.context = context;
        initShader();
        setupGeometry();
        getUserName();
        updateGreetingText();
        createTextTexture();
    }

    private void initShader() {
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
            "uniform sampler2D u_Texture;\n" +
            "uniform float u_Alpha;\n" +
            "void main() {\n" +
            "    vec4 texColor = texture2D(u_Texture, v_TexCoord);\n" +
            "    gl_FragColor = vec4(texColor.rgb, texColor.a * u_Alpha);\n" +
            "}\n";

        int vShader = ShaderUtils.compileShader(GLES30.GL_VERTEX_SHADER, vertexShader);
        int fShader = ShaderUtils.compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShader);

        programId = GLES30.glCreateProgram();
        GLES30.glAttachShader(programId, vShader);
        GLES30.glAttachShader(programId, fShader);
        GLES30.glLinkProgram(programId);

        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Shader link failed: " + GLES30.glGetProgramInfoLog(programId));
        }

        GLES30.glDeleteShader(vShader);
        GLES30.glDeleteShader(fShader);
    }

    private void setupGeometry() {
        // Convertir coordenadas 0-1 a NDC (-1 a 1)
        float ndcX = x * 2.0f - 1.0f;
        float ndcY = y * 2.0f - 1.0f;
        float ndcW = width * 2.0f;
        float ndcH = height * 2.0f;

        float[] vertices = {
            ndcX, ndcY,               // Bottom-left
            ndcX + ndcW, ndcY,        // Bottom-right
            ndcX, ndcY + ndcH,        // Top-left
            ndcX + ndcW, ndcY + ndcH  // Top-right
        };

        float[] texCoords = {
            0.0f, 1.0f,  // Bottom-left
            1.0f, 1.0f,  // Bottom-right
            0.0f, 0.0f,  // Top-left
            1.0f, 0.0f   // Top-right
        };

        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        ByteBuffer tbb = ByteBuffer.allocateDirect(texCoords.length * 4);
        tbb.order(ByteOrder.nativeOrder());
        texCoordBuffer = tbb.asFloatBuffer();
        texCoordBuffer.put(texCoords);
        texCoordBuffer.position(0);
    }

    private void getUserName() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                userName = displayName;
            } else {
                String email = currentUser.getEmail();
                if (email != null) {
                    userName = email.split("@")[0];  // Usar parte antes del @
                } else {
                    userName = "Usuario";
                }
            }
        } else {
            userName = "Usuario";
        }
        Log.d(TAG, "👤 Nombre de usuario: " + userName);
    }

    private void updateGreetingText() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;
        String emoji;
        if (hour >= 6 && hour < 12) {
            greeting = "Buenos días";
            emoji = "☀️";
        } else if (hour >= 12 && hour < 20) {
            greeting = "Buenas tardes";
            emoji = "🌅";
        } else {
            greeting = "Buenas noches";
            emoji = "🌙";
        }

        // Formato más elegante con emojis
        greetingText = emoji + " " + greeting + ", " + userName + " " + emoji;
        Log.d(TAG, "👋 Saludo: " + greetingText);
    }

    private void createTextTexture() {
        // Crear bitmap con el texto
        int textureWidth = 1024;
        int textureHeight = 256;

        Bitmap bitmap = Bitmap.createBitmap(textureWidth, textureHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Fondo transparente
        canvas.drawColor(Color.TRANSPARENT);

        // Configurar paint para el texto
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextSize(64);  // Tamaño más pequeño para que quepa mejor
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);  // Centrar texto

        // Sombra suave para legibilidad
        paint.setShadowLayer(12f, 2f, 2f, Color.BLACK);

        // Color cyan brillante (más moderno que dorado)
        paint.setColor(Color.rgb(0, 230, 255));  // Cyan brillante

        // Centrar texto verticalmente
        Paint.FontMetrics fm = paint.getFontMetrics();
        float y = (textureHeight / 2f) - ((fm.descent + fm.ascent) / 2f);

        // Dibujar texto centrado
        canvas.drawText(greetingText, textureWidth / 2f, y, paint);

        // Crear textura OpenGL
        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        textureId = textures[0];

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);

        bitmap.recycle();

        Log.d(TAG, "✓ Textura de saludo creada");
    }

    @Override
    public void update(float deltaTime) {
        cycleTimer += deltaTime;

        switch (currentState) {
            case HIDDEN:
                if (cycleTimer >= CYCLE_DURATION) {
                    cycleTimer = 0f;
                    displayTimer = 0f;
                    currentState = State.FADING_IN;
                    updateGreetingText();  // Actualizar saludo según hora actual
                    createTextTexture();   // Recrear textura con nuevo texto
                }
                break;

            case FADING_IN:
                displayTimer += deltaTime;
                alpha = Math.min(1.0f, displayTimer / FADE_IN_TIME);
                if (displayTimer >= FADE_IN_TIME) {
                    displayTimer = 0f;
                    currentState = State.DISPLAYING;
                }
                break;

            case DISPLAYING:
                displayTimer += deltaTime;
                alpha = 1.0f;
                if (displayTimer >= DISPLAY_TIME) {
                    displayTimer = 0f;
                    currentState = State.FADING_OUT;
                }
                break;

            case FADING_OUT:
                displayTimer += deltaTime;
                alpha = Math.max(0.0f, 1.0f - (displayTimer / FADE_OUT_TIME));
                if (displayTimer >= FADE_OUT_TIME) {
                    displayTimer = 0f;
                    cycleTimer = 0f;
                    currentState = State.HIDDEN;
                }
                break;
        }
    }

    @Override
    public void draw() {
        if (alpha <= 0.001f) return;  // No dibujar si invisible

        GLES30.glUseProgram(programId);

        // Desactivar depth test para UI
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Bind texture
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);

        // Set uniforms
        int uTextureLoc = GLES30.glGetUniformLocation(programId, "u_Texture");
        int uAlphaLoc = GLES30.glGetUniformLocation(programId, "u_Alpha");
        GLES30.glUniform1i(uTextureLoc, 0);
        GLES30.glUniform1f(uAlphaLoc, alpha);

        // Set attributes
        int aPosLoc = GLES30.glGetAttribLocation(programId, "a_Position");
        int aTexLoc = GLES30.glGetAttribLocation(programId, "a_TexCoord");

        GLES30.glEnableVertexAttribArray(aPosLoc);
        GLES30.glVertexAttribPointer(aPosLoc, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        GLES30.glEnableVertexAttribArray(aTexLoc);
        GLES30.glVertexAttribPointer(aTexLoc, 2, GLES30.GL_FLOAT, false, 0, texCoordBuffer);

        // Draw
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        // Cleanup
        GLES30.glDisableVertexAttribArray(aPosLoc);
        GLES30.glDisableVertexAttribArray(aTexLoc);

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }
}
