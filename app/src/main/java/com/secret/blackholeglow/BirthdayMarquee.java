package com.secret.blackholeglow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   🎂 BirthdayMarquee - Marquesina de Cumpleaños                   ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * CARACTERÍSTICAS:
 * - Texto desplazándose horizontalmente
 * - Efectos de brillo y partículas de confeti
 * - Muestra nombre + edad del cumpleañero
 * - Aparece periódicamente (no constantemente)
 * - Permite dar like con tap
 *
 * DISEÑO:
 * "🎂 ¡FELIZ CUMPLEAÑOS [NOMBRE]! 🎉 Cumple [EDAD] años 🎈"
 */
public class BirthdayMarquee implements SceneObject {
    private static final String TAG = "BirthdayMarquee";

    // Timing
    private static final float SHOW_DURATION = 12.0f;        // 12 segundos visible
    private static final float SHOW_INTERVAL = 180.0f;       // Cada 3 minutos
    private static final float SCROLL_SPEED = 0.15f;         // Velocidad de scroll
    private static final float FADE_DURATION = 1.0f;

    // Estado
    private enum State { HIDDEN, FADING_IN, SCROLLING, FADING_OUT }
    private State currentState = State.HIDDEN;
    private float stateTimer = 0f;
    private float timeSinceLastShow = 0f;
    private float alpha = 0f;
    private float scrollOffset = 1.5f;  // Empieza fuera de pantalla derecha
    private float time = 0f;

    // Datos del cumpleañero actual
    private String currentText = "";
    private String currentName = "";
    private int currentAge = 0;
    private String currentOdiseasId = "";
    private int currentLikes = 0;
    private int birthdayIndex = 0;

    // Texturas
    private int textTextureId = -1;
    private Bitmap textBitmap;
    private Canvas textCanvas;
    private static final int TEX_WIDTH = 1024;
    private static final int TEX_HEIGHT = 80;

    // OpenGL
    private int shaderProgram = 0;
    private FloatBuffer vertexBuffer;
    private float aspectRatio = 1.0f;

    // Posición vertical de la marquesina
    private float posY = 0.65f;  // Parte superior de la pantalla
    private float height = 0.08f;

    // Referencias
    private final Context context;
    private BirthdayManager birthdayManager;
    private boolean needsTextureUpdate = true;
    private boolean hasContent = false;

    // Shader sources
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
        "out vec4 fragColor;\n" +
        "uniform sampler2D u_Texture;\n" +
        "uniform float u_Alpha;\n" +
        "uniform float u_Time;\n" +
        "\n" +
        "void main() {\n" +
        "    vec4 texColor = texture(u_Texture, v_TexCoord);\n" +
        "    \n" +
        "    // Colores de fiesta\n" +
        "    vec3 color1 = vec3(1.0, 0.3, 0.5);  // Rosa\n" +
        "    vec3 color2 = vec3(1.0, 0.8, 0.2);  // Dorado\n" +
        "    vec3 color3 = vec3(0.3, 0.8, 1.0);  // Cyan\n" +
        "    \n" +
        "    // Gradiente animado\n" +
        "    float t = sin(u_Time * 2.0 + v_TexCoord.x * 6.28) * 0.5 + 0.5;\n" +
        "    vec3 gradient = mix(color1, color2, t);\n" +
        "    gradient = mix(gradient, color3, sin(u_Time * 1.5) * 0.3 + 0.3);\n" +
        "    \n" +
        "    // Sparkle effect\n" +
        "    float sparkle = sin(v_TexCoord.x * 50.0 + u_Time * 10.0) * 0.5 + 0.5;\n" +
        "    sparkle *= sin(v_TexCoord.y * 20.0 - u_Time * 5.0) * 0.5 + 0.5;\n" +
        "    sparkle = pow(sparkle, 4.0) * 0.5;\n" +
        "    \n" +
        "    vec3 finalColor = gradient * texColor.rgb;\n" +
        "    finalColor += vec3(1.0) * sparkle * texColor.a;\n" +
        "    \n" +
        "    fragColor = vec4(finalColor, texColor.a * u_Alpha);\n" +
        "}\n";

    // Modo de prueba para desarrollo
    private static final boolean TEST_MODE = true;  // ⚠️ Cambiar a false en producción

    public BirthdayMarquee(Context context) {
        this.context = context;
        this.birthdayManager = BirthdayManager.getInstance(context);

        initBitmaps();
        initOpenGL();

        if (TEST_MODE) {
            // Crear datos de prueba
            setupTestData();
        } else {
            // Cargar cumpleañeros reales
            loadBirthdays();
        }

        Log.d(TAG, "🎂 BirthdayMarquee inicializado" + (TEST_MODE ? " (MODO PRUEBA)" : ""));
    }

    /**
     * Configura datos de prueba para desarrollo
     */
    private void setupTestData() {
        hasContent = true;
        currentName = "Eduardo";
        currentAge = 25;
        currentOdiseasId = "test_user_123";
        currentLikes = 7;

        currentText = "🎂 ¡FELIZ CUMPLEAÑOS " + currentName.toUpperCase() + "! 🎉 " +
                     "Cumple " + currentAge + " años 🎈 ❤️ " + currentLikes + " felicitaciones";

        needsTextureUpdate = true;

        // Mostrar inmediatamente para probar
        timeSinceLastShow = SHOW_INTERVAL;  // Forzar que se muestre pronto

        Log.d(TAG, "🎂 [TEST] Datos de prueba configurados");
    }

    private void initBitmaps() {
        textBitmap = Bitmap.createBitmap(TEX_WIDTH, TEX_HEIGHT, Bitmap.Config.ARGB_8888);
        textCanvas = new Canvas(textBitmap);
    }

    private void initOpenGL() {
        int vs = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        if (vs == 0 || fs == 0) return;

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

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

        int[] texIds = new int[1];
        GLES30.glGenTextures(1, texIds, 0);
        textTextureId = texIds[0];

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textTextureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
    }

    private int compileShader(int type, String code) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, code);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader error: " + GLES30.glGetShaderInfoLog(shader));
            return 0;
        }
        return shader;
    }

    /**
     * Carga los cumpleañeros del día
     */
    private void loadBirthdays() {
        birthdayManager.getTodaysBirthdays(new BirthdayManager.BirthdayCallback() {
            @Override
            public void onBirthdaysLoaded(List<BirthdayManager.BirthdayPerson> birthdays) {
                if (birthdays != null && !birthdays.isEmpty()) {
                    hasContent = true;
                    birthdayIndex = 0;
                    showNextBirthday(birthdays);
                    Log.d(TAG, "🎂 " + birthdays.size() + " cumpleañeros cargados");
                } else {
                    hasContent = false;
                    Log.d(TAG, "🎂 No hay cumpleañeros hoy");
                }
            }

            @Override
            public void onError(String error) {
                hasContent = false;
                Log.w(TAG, "Error cargando cumpleañeros: " + error);
            }
        });
    }

    /**
     * Muestra el siguiente cumpleañero
     */
    private void showNextBirthday(List<BirthdayManager.BirthdayPerson> birthdays) {
        if (birthdays == null || birthdays.isEmpty()) return;

        BirthdayManager.BirthdayPerson person = birthdays.get(birthdayIndex % birthdays.size());
        birthdayIndex++;

        currentName = person.displayName;
        currentAge = person.age;
        currentOdiseasId = person.odiseasId;
        currentLikes = person.likes;

        // Crear texto de la marquesina
        currentText = "🎂 ¡FELIZ CUMPLEAÑOS " + currentName.toUpperCase() + "! 🎉 " +
                     "Cumple " + currentAge + " años 🎈 " +
                     (currentLikes > 0 ? "❤️ " + currentLikes + " felicitaciones" : "¡Dale like para felicitar!");

        needsTextureUpdate = true;
    }

    /**
     * Fuerza mostrar la marquesina ahora
     */
    public void show() {
        if (!hasContent) {
            // Recargar cumpleañeros
            loadBirthdays();
            return;
        }

        if (currentState == State.HIDDEN) {
            currentState = State.FADING_IN;
            stateTimer = 0f;
            scrollOffset = 1.5f;  // Reset scroll
            Log.d(TAG, "🎂 Mostrando cumpleaños de " + currentName);
        }
    }

    @Override
    public void update(float deltaTime) {
        time += deltaTime;
        if (time > 62.83f) time -= 62.83f;

        timeSinceLastShow += deltaTime;

        // Mostrar periódicamente
        if (currentState == State.HIDDEN && hasContent && timeSinceLastShow >= SHOW_INTERVAL) {
            timeSinceLastShow = 0f;
            show();
        }

        switch (currentState) {
            case FADING_IN:
                stateTimer += deltaTime;
                alpha = Math.min(1f, stateTimer / FADE_DURATION);
                if (stateTimer >= FADE_DURATION) {
                    currentState = State.SCROLLING;
                    stateTimer = 0f;
                    alpha = 1f;
                }
                break;

            case SCROLLING:
                stateTimer += deltaTime;
                scrollOffset -= SCROLL_SPEED * deltaTime;

                // Cuando el texto sale por la izquierda, reiniciar
                if (scrollOffset < -1.5f) {
                    scrollOffset = 1.5f;
                }

                if (stateTimer >= SHOW_DURATION) {
                    currentState = State.FADING_OUT;
                    stateTimer = 0f;
                }
                break;

            case FADING_OUT:
                stateTimer += deltaTime;
                alpha = Math.max(0f, 1f - (stateTimer / FADE_DURATION));
                if (stateTimer >= FADE_DURATION) {
                    currentState = State.HIDDEN;
                    alpha = 0f;

                    // Preparar siguiente cumpleañero
                    List<BirthdayManager.BirthdayPerson> birthdays = birthdayManager.getTodaysBirthdays();
                    if (!birthdays.isEmpty()) {
                        showNextBirthday(birthdays);
                    }
                }
                break;
        }
    }

    @Override
    public void draw() {
        if (currentState == State.HIDDEN || alpha <= 0.01f || !hasContent) return;
        if (shaderProgram == 0) return;

        if (needsTextureUpdate) {
            updateTexture();
            needsTextureUpdate = false;
        }

        GLES30.glUseProgram(shaderProgram);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        int aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        int aTexCoordLoc = GLES30.glGetAttribLocation(shaderProgram, "a_TexCoord");
        int uTextureLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Texture");
        int uAlphaLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Alpha");
        int uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Time");

        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glEnableVertexAttribArray(aTexCoordLoc);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glUniform1i(uTextureLoc, 0);
        GLES30.glUniform1f(uAlphaLoc, alpha);
        GLES30.glUniform1f(uTimeLoc, time);

        // Dibujar texto desplazándose
        drawScrollingQuad(aPositionLoc, aTexCoordLoc);

        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aTexCoordLoc);
    }

    private void drawScrollingQuad(int aPos, int aTex) {
        float width = 1.2f;  // Ancho del texto
        float halfH = height;

        float left = scrollOffset;
        float right = scrollOffset + width;

        float[] verts = {
            left / aspectRatio, posY - halfH, 0f, 1f,
            right / aspectRatio, posY - halfH, 1f, 1f,
            left / aspectRatio, posY + halfH, 0f, 0f,
            right / aspectRatio, posY + halfH, 1f, 0f
        };

        vertexBuffer.clear();
        vertexBuffer.put(verts);
        vertexBuffer.position(0);

        GLES30.glVertexAttribPointer(aPos, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);
        vertexBuffer.position(2);
        GLES30.glVertexAttribPointer(aTex, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textTextureId);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
    }

    private void updateTexture() {
        if (textBitmap == null) return;

        textBitmap.eraseColor(0x00000000);

        // Fondo semi-transparente con gradiente
        Paint bgPaint = new Paint();
        bgPaint.setShader(new LinearGradient(0, 0, TEX_WIDTH, 0,
            new int[]{0x00000000, 0x88000000, 0x88000000, 0x00000000},
            new float[]{0f, 0.1f, 0.9f, 1f},
            Shader.TileMode.CLAMP));
        textCanvas.drawRect(0, 0, TEX_WIDTH, TEX_HEIGHT, bgPaint);

        // Texto principal
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(36);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // Glow
        Paint glowPaint = new Paint(textPaint);
        glowPaint.setColor(0xFFFF66AA);
        glowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(8, android.graphics.BlurMaskFilter.Blur.NORMAL));

        float y = TEX_HEIGHT / 2f + 12;

        textCanvas.drawText(currentText, 20, y, glowPaint);
        textCanvas.drawText(currentText, 20, y, textPaint);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textTextureId);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, textBitmap, 0);
    }

    /**
     * Verifica si el tap está sobre la marquesina (para dar like)
     */
    public boolean isInside(float nx, float ny) {
        if (currentState == State.HIDDEN || alpha < 0.5f) return false;

        float top = posY + height;
        float bottom = posY - height;

        return ny >= bottom && ny <= top;
    }

    /**
     * Envía un like al cumpleañero actual
     */
    public void sendLike(BirthdayManager.LikeCallback callback) {
        if (currentOdiseasId.isEmpty()) {
            callback.onError("No hay cumpleañero seleccionado");
            return;
        }

        birthdayManager.sendBirthdayLike(currentOdiseasId, new BirthdayManager.LikeCallback() {
            @Override
            public void onLikeSent(int totalLikes) {
                currentLikes = totalLikes;
                currentText = "🎂 ¡FELIZ CUMPLEAÑOS " + currentName.toUpperCase() + "! 🎉 " +
                             "Cumple " + currentAge + " años 🎈 ❤️ " + currentLikes + " felicitaciones";
                needsTextureUpdate = true;
                callback.onLikeSent(totalLikes);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void setAspectRatio(float aspect) {
        this.aspectRatio = aspect;
    }

    public boolean isVisible() {
        return currentState != State.HIDDEN && alpha > 0.01f;
    }

    public void release() {
        if (textTextureId != -1) {
            int[] texIds = {textTextureId};
            GLES30.glDeleteTextures(1, texIds, 0);
        }
        if (textBitmap != null) textBitmap.recycle();
        if (shaderProgram != 0) GLES30.glDeleteProgram(shaderProgram);
    }
}
