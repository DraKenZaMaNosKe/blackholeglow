package com.secret.blackholeglow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   ✨ MagicLeaderboard - Leaderboard con Efectos Mágicos           ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * CARACTERÍSTICAS:
 * - Aparece gradualmente con fade in
 * - Se desvanece en partículas de polvo estelar
 * - Aparece al inicio y cada cierto tiempo
 * - Partículas brillantes que flotan al desvanecerse
 * - Colores de medallas (oro, plata, bronce)
 */
public class MagicLeaderboard implements SceneObject {

    private static final String TAG = "MagicLeaderboard";

    // Estados del leaderboard
    private enum State {
        HIDDEN,         // No visible
        FADING_IN,      // Apareciendo
        VISIBLE,        // Completamente visible
        FADING_OUT,     // Desvaneciéndose en partículas
        PARTICLES_ONLY  // Solo partículas flotando
    }

    private State currentState = State.HIDDEN;
    private final Context context;

    // Timing
    private static final float FADE_IN_DURATION = 1.5f;      // Segundos para aparecer
    private static final float VISIBLE_DURATION = 10.0f;     // Segundos visible (un poco más)
    private static final float FADE_OUT_DURATION = 2.0f;     // Segundos para desvanecerse
    private static final float PARTICLES_DURATION = 3.0f;    // Segundos de partículas flotando
    private static final float SHOW_INTERVAL = 1800.0f;      // Cada 30 minutos (1800 segundos)

    private float stateTimer = 0f;
    private float timeSinceLastShow = 0f;
    private float alpha = 0f;
    private boolean showOnStart = true;

    // Datos del leaderboard
    private String[] entries = new String[3];
    private int[] colors = {
        0xFFFFD700,  // Oro
        0xFFC0C0C0,  // Plata
        0xFFCD7F32   // Bronce
    };

    // OpenGL
    private int programId;
    private int textureId = -1;
    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;
    private Bitmap leaderboardBitmap;
    private boolean needsTextureUpdate = true;

    // Posición y tamaño
    private float posX = -0.85f;
    private float posY = 0.55f;
    private float width = 0.70f;
    private float height = 0.35f;

    // Partículas de polvo estelar
    private List<StarDustParticle> particles = new ArrayList<>();
    private Random random = new Random();
    private static final int MAX_PARTICLES = 150;

    // Shader sources
    private static final String VERTEX_SHADER =
        "attribute vec4 a_Position;\n" +
        "attribute vec2 a_TexCoord;\n" +
        "varying vec2 v_TexCoord;\n" +
        "uniform mat4 u_MVP;\n" +
        "void main() {\n" +
        "    gl_Position = u_MVP * a_Position;\n" +
        "    v_TexCoord = a_TexCoord;\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "varying vec2 v_TexCoord;\n" +
        "uniform sampler2D u_Texture;\n" +
        "uniform float u_Alpha;\n" +
        "void main() {\n" +
        "    vec4 color = texture2D(u_Texture, v_TexCoord);\n" +
        "    gl_FragColor = vec4(color.rgb, color.a * u_Alpha);\n" +
        "}\n";

    // Shader para partículas
    private int particleProgramId;
    private FloatBuffer particleVertexBuffer;

    private static final String PARTICLE_VERTEX_SHADER =
        "attribute vec4 a_Position;\n" +
        "attribute vec4 a_Color;\n" +
        "varying vec4 v_Color;\n" +
        "uniform mat4 u_MVP;\n" +
        "uniform float u_PointSize;\n" +
        "void main() {\n" +
        "    gl_Position = u_MVP * a_Position;\n" +
        "    gl_PointSize = u_PointSize * a_Color.a;\n" +
        "    v_Color = a_Color;\n" +
        "}\n";

    private static final String PARTICLE_FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "varying vec4 v_Color;\n" +
        "void main() {\n" +
        "    vec2 center = gl_PointCoord - vec2(0.5);\n" +
        "    float dist = length(center);\n" +
        "    float alpha = 1.0 - smoothstep(0.3, 0.5, dist);\n" +
        "    gl_FragColor = vec4(v_Color.rgb, v_Color.a * alpha);\n" +
        "}\n";

    public MagicLeaderboard(Context context) {
        this.context = context;
        initGL();
        initBuffers();

        // Inicializar entradas vacías
        for (int i = 0; i < 3; i++) {
            entries[i] = "---";
        }

        Log.d(TAG, "✨ MagicLeaderboard creado");
    }

    private void initGL() {
        // Shader principal para el texto
        programId = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);

        // Shader para partículas
        particleProgramId = createProgram(PARTICLE_VERTEX_SHADER, PARTICLE_FRAGMENT_SHADER);
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource);

        int program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragmentShader);
        GLES30.glLinkProgram(program);

        return program;
    }

    private int loadShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);
        return shader;
    }

    private void initBuffers() {
        // Quad para el leaderboard
        float[] vertices = {
            -1f, -1f, 0f,
             1f, -1f, 0f,
            -1f,  1f, 0f,
             1f,  1f, 0f
        };

        float[] texCoords = {
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
        };

        vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices);
        vertexBuffer.position(0);

        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(texCoords);
        texCoordBuffer.position(0);

        // Buffer para partículas (posición + color = 7 floats por partícula)
        particleVertexBuffer = ByteBuffer.allocateDirect(MAX_PARTICLES * 7 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
    }

    /**
     * Actualiza las entradas del leaderboard
     */
    public void updateEntries(List<LeaderboardManager.LeaderboardEntry> top3) {
        if (top3 == null || top3.isEmpty()) return;

        for (int i = 0; i < Math.min(top3.size(), 3); i++) {
            LeaderboardManager.LeaderboardEntry entry = top3.get(i);
            String medal = (i == 0) ? "🥇" : (i == 1) ? "🥈" : "🥉";
            String name = entry.displayName;
            if (name.length() > 8) {
                name = name.substring(0, 7) + "..";
            }
            entries[i] = medal + " " + name + " " + entry.planetsDestroyed;
        }

        needsTextureUpdate = true;
        Log.d(TAG, "📊 Leaderboard actualizado");
    }

    /**
     * Fuerza mostrar el leaderboard ahora
     */
    public void show() {
        if (currentState == State.HIDDEN || currentState == State.PARTICLES_ONLY) {
            currentState = State.FADING_IN;
            stateTimer = 0f;
            alpha = 0f;
            Log.d(TAG, "✨ Leaderboard apareciendo...");
        }
    }

    /**
     * Fuerza ocultar el leaderboard
     */
    public void hide() {
        if (currentState == State.VISIBLE || currentState == State.FADING_IN) {
            startFadeOut();
        }
    }

    private void startFadeOut() {
        currentState = State.FADING_OUT;
        stateTimer = 0f;

        // Crear partículas de polvo estelar
        createStarDustParticles();

        Log.d(TAG, "✨ Leaderboard desvaneciéndose en polvo estelar...");
    }

    private void createStarDustParticles() {
        particles.clear();

        // Crear partículas distribuidas por todo el leaderboard
        for (int i = 0; i < MAX_PARTICLES; i++) {
            float px = posX + random.nextFloat() * width;
            float py = posY - random.nextFloat() * height;

            // Color aleatorio entre los colores del leaderboard
            int colorIndex = random.nextInt(3);
            int baseColor = colors[colorIndex];

            // Añadir variación al color
            float r = ((baseColor >> 16) & 0xFF) / 255f + (random.nextFloat() - 0.5f) * 0.2f;
            float g = ((baseColor >> 8) & 0xFF) / 255f + (random.nextFloat() - 0.5f) * 0.2f;
            float b = (baseColor & 0xFF) / 255f + (random.nextFloat() - 0.5f) * 0.2f;

            // Algunas partículas blancas brillantes
            if (random.nextFloat() < 0.3f) {
                r = g = b = 1.0f;
            }

            particles.add(new StarDustParticle(
                px, py,
                (random.nextFloat() - 0.5f) * 0.3f,  // velocidad X
                random.nextFloat() * 0.2f + 0.05f,   // velocidad Y (hacia arriba)
                r, g, b,
                random.nextFloat() * 0.5f + 0.5f,    // alpha inicial
                random.nextFloat() * 8f + 4f         // tamaño
            ));
        }
    }

    @Override
    public void update(float deltaTime) {
        timeSinceLastShow += deltaTime;

        // Mostrar al inicio
        if (showOnStart && timeSinceLastShow > 1.0f) {
            showOnStart = false;
            show();
        }

        // Mostrar periódicamente
        if (currentState == State.HIDDEN && timeSinceLastShow >= SHOW_INTERVAL) {
            timeSinceLastShow = 0f;
            show();
        }

        // Actualizar estado
        switch (currentState) {
            case FADING_IN:
                stateTimer += deltaTime;
                alpha = Math.min(1f, stateTimer / FADE_IN_DURATION);
                if (stateTimer >= FADE_IN_DURATION) {
                    currentState = State.VISIBLE;
                    stateTimer = 0f;
                    alpha = 1f;
                }
                break;

            case VISIBLE:
                stateTimer += deltaTime;
                if (stateTimer >= VISIBLE_DURATION) {
                    startFadeOut();
                }
                break;

            case FADING_OUT:
                stateTimer += deltaTime;
                alpha = Math.max(0f, 1f - (stateTimer / FADE_OUT_DURATION));

                // Actualizar partículas
                updateParticles(deltaTime);

                if (stateTimer >= FADE_OUT_DURATION) {
                    currentState = State.PARTICLES_ONLY;
                    stateTimer = 0f;
                    alpha = 0f;
                }
                break;

            case PARTICLES_ONLY:
                stateTimer += deltaTime;
                updateParticles(deltaTime);

                if (stateTimer >= PARTICLES_DURATION) {
                    currentState = State.HIDDEN;
                    particles.clear();
                }
                break;
        }
    }

    private void updateParticles(float deltaTime) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            StarDustParticle p = particles.get(i);
            p.update(deltaTime);

            // Remover partículas muertas
            if (p.alpha <= 0) {
                particles.remove(i);
            }
        }
    }

    @Override
    public void draw() {
        if (currentState == State.HIDDEN) return;

        // Dibujar el texto del leaderboard
        if (alpha > 0 && currentState != State.PARTICLES_ONLY) {
            drawLeaderboard();
        }

        // Dibujar partículas
        if (!particles.isEmpty()) {
            drawParticles();
        }
    }

    private void drawLeaderboard() {
        // Actualizar textura si es necesario
        if (needsTextureUpdate || textureId == -1) {
            updateTexture();
            needsTextureUpdate = false;
        }

        if (textureId == -1) return;

        GLES30.glUseProgram(programId);

        // Matriz MVP
        float[] mvp = new float[16];
        Matrix.setIdentityM(mvp, 0);
        Matrix.translateM(mvp, 0, posX + width/2, posY - height/2, 0);
        Matrix.scaleM(mvp, 0, width/2, height/2, 1);

        int mvpLoc = GLES30.glGetUniformLocation(programId, "u_MVP");
        GLES30.glUniformMatrix4fv(mvpLoc, 1, false, mvp, 0);

        int alphaLoc = GLES30.glGetUniformLocation(programId, "u_Alpha");
        GLES30.glUniform1f(alphaLoc, alpha);

        // Textura
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        int texLoc = GLES30.glGetUniformLocation(programId, "u_Texture");
        GLES30.glUniform1i(texLoc, 0);

        // Atributos
        int posLoc = GLES30.glGetAttribLocation(programId, "a_Position");
        int texCoordLoc = GLES30.glGetAttribLocation(programId, "a_TexCoord");

        GLES30.glEnableVertexAttribArray(posLoc);
        GLES30.glEnableVertexAttribArray(texCoordLoc);

        vertexBuffer.position(0);
        GLES30.glVertexAttribPointer(posLoc, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        texCoordBuffer.position(0);
        GLES30.glVertexAttribPointer(texCoordLoc, 2, GLES30.GL_FLOAT, false, 0, texCoordBuffer);

        // Blending
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(posLoc);
        GLES30.glDisableVertexAttribArray(texCoordLoc);
    }

    private void drawParticles() {
        if (particles.isEmpty()) return;

        GLES30.glUseProgram(particleProgramId);

        // Preparar datos de partículas
        particleVertexBuffer.clear();
        for (StarDustParticle p : particles) {
            particleVertexBuffer.put(p.x);
            particleVertexBuffer.put(p.y);
            particleVertexBuffer.put(0f);
            particleVertexBuffer.put(p.r);
            particleVertexBuffer.put(p.g);
            particleVertexBuffer.put(p.b);
            particleVertexBuffer.put(p.alpha);
        }
        particleVertexBuffer.position(0);

        // MVP (identidad para coordenadas NDC)
        float[] mvp = new float[16];
        Matrix.setIdentityM(mvp, 0);

        int mvpLoc = GLES30.glGetUniformLocation(particleProgramId, "u_MVP");
        GLES30.glUniformMatrix4fv(mvpLoc, 1, false, mvp, 0);

        int sizeLoc = GLES30.glGetUniformLocation(particleProgramId, "u_PointSize");
        GLES30.glUniform1f(sizeLoc, 20f);

        // Atributos
        int posLoc = GLES30.glGetAttribLocation(particleProgramId, "a_Position");
        int colorLoc = GLES30.glGetAttribLocation(particleProgramId, "a_Color");

        GLES30.glEnableVertexAttribArray(posLoc);
        GLES30.glEnableVertexAttribArray(colorLoc);

        int stride = 7 * 4; // 7 floats * 4 bytes

        particleVertexBuffer.position(0);
        GLES30.glVertexAttribPointer(posLoc, 3, GLES30.GL_FLOAT, false, stride, particleVertexBuffer);

        particleVertexBuffer.position(3);
        GLES30.glVertexAttribPointer(colorLoc, 4, GLES30.GL_FLOAT, false, stride, particleVertexBuffer);

        // Blending aditivo para brillo
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE);

        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, particles.size());

        GLES30.glDisableVertexAttribArray(posLoc);
        GLES30.glDisableVertexAttribArray(colorLoc);

        // Restaurar blending normal
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void updateTexture() {
        // Crear bitmap con el texto del leaderboard
        int bitmapWidth = 600;
        int bitmapHeight = 280;

        if (leaderboardBitmap == null || leaderboardBitmap.getWidth() != bitmapWidth) {
            if (leaderboardBitmap != null) leaderboardBitmap.recycle();
            leaderboardBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(leaderboardBitmap);
        canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);

        float padding = 25f;
        float contentWidth = bitmapWidth - (padding * 2);

        // ═══════════════════════════════════════════════════════════
        // FONDO SEMI-TRANSPARENTE CON GRADIENTE
        // ═══════════════════════════════════════════════════════════
        Paint bgPaint = new Paint();
        bgPaint.setShader(new LinearGradient(0, 0, 0, bitmapHeight,
            new int[]{0x00000000, 0x44000022, 0x44000022, 0x00000000},
            new float[]{0f, 0.15f, 0.85f, 1f},
            android.graphics.Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, bitmapWidth, bitmapHeight, bgPaint);

        // ═══════════════════════════════════════════════════════════
        // TÍTULO CON GLOW
        // ═══════════════════════════════════════════════════════════
        Paint titleGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
        titleGlow.setColor(0xFFFFD700);  // Dorado
        titleGlow.setTextSize(32);
        titleGlow.setTypeface(Typeface.DEFAULT_BOLD);
        titleGlow.setTextAlign(Paint.Align.CENTER);
        titleGlow.setMaskFilter(new android.graphics.BlurMaskFilter(8, android.graphics.BlurMaskFilter.Blur.NORMAL));

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(32);
        titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setShadowLayer(4, 2, 2, 0xFF000000);

        float titleY = 45;
        canvas.drawText("TOP DEFENSORES", bitmapWidth / 2f, titleY, titleGlow);
        canvas.drawText("TOP DEFENSORES", bitmapWidth / 2f, titleY, titlePaint);

        // Línea decorativa
        Paint linePaint = new Paint();
        linePaint.setColor(0x66FFD700);
        linePaint.setStrokeWidth(2);
        canvas.drawLine(padding, titleY + 12, bitmapWidth - padding, titleY + 12, linePaint);

        // ═══════════════════════════════════════════════════════════
        // ENTRADAS DEL LEADERBOARD
        // ═══════════════════════════════════════════════════════════
        String[] medals = {"🥇", "🥈", "🥉"};
        int[] glowColors = {0xFFFFD700, 0xFFC0C0C0, 0xFFCD7F32};  // Oro, Plata, Bronce

        float entryY = titleY + 55;
        float entrySpacing = 58;

        for (int i = 0; i < 3; i++) {
            String entry = entries[i];
            if (entry == null || entry.equals("---")) continue;

            // Parsear entrada: "🥇 nombre 123"
            String[] parts = entry.split(" ");
            String name = "";
            String score = "";

            if (parts.length >= 2) {
                // Extraer nombre (puede tener espacios) y puntuación
                StringBuilder nameBuilder = new StringBuilder();
                for (int j = 1; j < parts.length - 1; j++) {
                    if (nameBuilder.length() > 0) nameBuilder.append(" ");
                    nameBuilder.append(parts[j]);
                }
                name = nameBuilder.toString();
                score = parts[parts.length - 1];

                // Limitar nombre
                if (name.length() > 12) {
                    name = name.substring(0, 11) + "..";
                }
            } else {
                name = entry;
            }

            float y = entryY + (i * entrySpacing);

            // Glow de la medalla
            Paint medalGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
            medalGlow.setColor(glowColors[i]);
            medalGlow.setTextSize(36);
            medalGlow.setMaskFilter(new android.graphics.BlurMaskFilter(6, android.graphics.BlurMaskFilter.Blur.NORMAL));

            // Medalla
            Paint medalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            medalPaint.setTextSize(36);
            canvas.drawText(medals[i], padding, y, medalGlow);
            canvas.drawText(medals[i], padding, y, medalPaint);

            // Nombre con glow del color de medalla
            Paint nameGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
            nameGlow.setColor(glowColors[i]);
            nameGlow.setTextSize(28);
            nameGlow.setTypeface(Typeface.DEFAULT_BOLD);
            nameGlow.setMaskFilter(new android.graphics.BlurMaskFilter(4, android.graphics.BlurMaskFilter.Blur.NORMAL));

            Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            namePaint.setColor(Color.WHITE);
            namePaint.setTextSize(28);
            namePaint.setTypeface(Typeface.DEFAULT_BOLD);
            namePaint.setShadowLayer(3, 1, 1, 0xFF000000);

            float nameX = padding + 50;
            canvas.drawText(name, nameX, y, nameGlow);
            canvas.drawText(name, nameX, y, namePaint);

            // Puntuación (alineada a la derecha)
            Paint scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            scorePaint.setColor(colors[i]);
            scorePaint.setTextSize(26);
            scorePaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            scorePaint.setTextAlign(Paint.Align.RIGHT);
            scorePaint.setShadowLayer(3, 1, 1, 0xFF000000);

            Paint scoreGlow = new Paint(scorePaint);
            scoreGlow.setMaskFilter(new android.graphics.BlurMaskFilter(4, android.graphics.BlurMaskFilter.Blur.NORMAL));

            canvas.drawText(score, bitmapWidth - padding, y, scoreGlow);
            canvas.drawText(score, bitmapWidth - padding, y, scorePaint);
        }

        // Subir textura a OpenGL
        if (textureId == -1) {
            int[] texIds = new int[1];
            GLES30.glGenTextures(1, texIds, 0);
            textureId = texIds[0];
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, leaderboardBitmap, 0);
    }

    public void release() {
        if (textureId != -1) {
            int[] texIds = {textureId};
            GLES30.glDeleteTextures(1, texIds, 0);
            textureId = -1;
        }
        if (leaderboardBitmap != null) {
            leaderboardBitmap.recycle();
            leaderboardBitmap = null;
        }
        particles.clear();
    }

    /**
     * Partícula de polvo estelar
     */
    private static class StarDustParticle {
        float x, y;
        float vx, vy;
        float r, g, b;
        float alpha;
        float size;
        float life;
        float maxLife;

        StarDustParticle(float x, float y, float vx, float vy, float r, float g, float b, float alpha, float size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.r = r;
            this.g = g;
            this.b = b;
            this.alpha = alpha;
            this.size = size;
            this.life = 0;
            this.maxLife = 3f + (float)Math.random() * 2f;
        }

        void update(float dt) {
            life += dt;

            // Mover
            x += vx * dt;
            y += vy * dt;

            // Desacelerar
            vx *= 0.98f;
            vy *= 0.98f;

            // Añadir ondulación
            x += (float)Math.sin(life * 3f) * 0.002f;

            // Desvanecer
            float progress = life / maxLife;
            alpha = Math.max(0, 1f - progress);

            // Encoger al final
            if (progress > 0.7f) {
                size *= 0.95f;
            }
        }
    }
}
