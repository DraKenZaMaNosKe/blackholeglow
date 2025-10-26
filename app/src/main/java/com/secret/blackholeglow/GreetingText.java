package com.secret.blackholeglow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES20;
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

    // Posición y tamaño
    private float x = 0.08f;  // Parte inferior izquierda (5% desde la izquierda)
    private float y = 0.8f;  // 15% desde abajo
    private float width = 0.5f;   // 50% del ancho de pantalla
    private float height = 0.08f; // 8% de altura

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

        int vShader = ShaderUtils.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        int fShader = ShaderUtils.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        programId = GLES20.glCreateProgram();
        GLES20.glAttachShader(programId, vShader);
        GLES20.glAttachShader(programId, fShader);
        GLES20.glLinkProgram(programId);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Shader link failed: " + GLES20.glGetProgramInfoLog(programId));
        }

        GLES20.glDeleteShader(vShader);
        GLES20.glDeleteShader(fShader);
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
        if (hour >= 6 && hour < 12) {
            greeting = "Buenos días";
        } else if (hour >= 12 && hour < 20) {
            greeting = "Buenas tardes";
        } else {
            greeting = "Buenas noches";
        }

        greetingText = greeting + ": " + userName;
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
        paint.setTextSize(80);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // Color dorado brillante con sombra
        paint.setShadowLayer(8f, 0f, 0f, Color.BLACK);
        paint.setColor(Color.rgb(255, 215, 0));  // Dorado

        // Centrar texto verticalmente
        Paint.FontMetrics fm = paint.getFontMetrics();
        float textHeight = fm.descent - fm.ascent;
        float y = (textureHeight / 2f) - ((fm.descent + fm.ascent) / 2f);

        // Dibujar texto
        canvas.drawText(greetingText, 40, y, paint);

        // Crear textura OpenGL
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        textureId = textures[0];

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

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

        GLES20.glUseProgram(programId);

        // Desactivar depth test para UI
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Bind texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        // Set uniforms
        int uTextureLoc = GLES20.glGetUniformLocation(programId, "u_Texture");
        int uAlphaLoc = GLES20.glGetUniformLocation(programId, "u_Alpha");
        GLES20.glUniform1i(uTextureLoc, 0);
        GLES20.glUniform1f(uAlphaLoc, alpha);

        // Set attributes
        int aPosLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        int aTexLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord");

        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aTexLoc);
        GLES20.glVertexAttribPointer(aTexLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        // Draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Cleanup
        GLES20.glDisableVertexAttribArray(aPosLoc);
        GLES20.glDisableVertexAttribArray(aTexLoc);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }
}
