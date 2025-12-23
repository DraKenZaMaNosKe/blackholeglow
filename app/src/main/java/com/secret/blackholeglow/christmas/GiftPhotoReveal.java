package com.secret.blackholeglow.christmas;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

/**
 * GiftPhotoReveal - Revelacion Magica de Fotos Navidenas
 * Uses GLES20 API with ES 2.0 compatible shaders (backwards compatible with ES 3.0 context)
 *
 * IMPORTANT: trigger() can be called from UI thread, but OpenGL operations
 * are deferred to draw() which runs on GL thread.
 */
public class GiftPhotoReveal {
    private static final String TAG = "GiftPhotoReveal";

    public enum State {
        IDLE,
        PENDING_TEXTURE,  // Bitmap loaded, waiting for GL thread to create texture
        APPEARING,
        VISIBLE,
        DISAPPEARING
    }

    private static final float APPEAR_DURATION = 1.5f;
    private static final float VISIBLE_DURATION = 5.0f;
    private static final float DISAPPEAR_DURATION = 1.5f;

    private State state = State.IDLE;
    private float stateTime = 0f;
    private float totalTime = 0f;

    private int photoTextureId = 0;
    private boolean hasPhoto = false;
    private Bitmap pendingBitmap = null;  // Bitmap waiting to be uploaded to GPU

    private int shaderProgram;
    private int frameShaderProgram;
    private FloatBuffer quadBuffer;
    private boolean initialized = false;

    private int uTextureLoc, uTimeLoc, uProgressLoc, uAlphaLoc, uAspectLoc;
    private int uYOffsetLoc, uScaleLoc;  // Animation uniforms for photo
    private int uFrameTimeLoc, uFrameProgressLoc, uFrameAlphaLoc, uFrameAspectLoc;
    private int uFrameYOffsetLoc, uFrameScaleLoc;  // Animation uniforms for frame

    private static final int NUM_PARTICLES = 50;
    private float[] particleX = new float[NUM_PARTICLES];
    private float[] particleY = new float[NUM_PARTICLES];
    private float[] particleVX = new float[NUM_PARTICLES];
    private float[] particleVY = new float[NUM_PARTICLES];
    private float[] particleLife = new float[NUM_PARTICLES];
    private float[] particleSize = new float[NUM_PARTICLES];
    private int particleShader;
    private FloatBuffer particleBuffer;
    private int pPositionLoc, pSizeLoc, pAlphaLoc, pAspectLoc;

    private float aspectRatio = 1.0f;
    private int screenWidth = 1080;
    private int screenHeight = 1920;
    private Random random = new Random();
    private Context context;
    private PhotoGalleryManager galleryManager;

    // ES 2.0 compatible shaders (work in ES 3.0 context too)
    // Animated entry from top, floating, exit effect
    private static final String PHOTO_VERTEX =
        "attribute vec2 a_Position;\n" +
        "attribute vec2 a_TexCoord;\n" +
        "uniform float u_Progress;\n" +
        "uniform float u_Aspect;\n" +
        "uniform float u_YOffset;\n" +
        "uniform float u_Scale;\n" +
        "varying vec2 v_TexCoord;\n" +
        "void main() {\n" +
        "    vec2 pos = a_Position;\n" +
        "    pos *= u_Scale * 0.07;\n" +
        "    pos.x /= u_Aspect;\n" +
        "    pos.y += u_YOffset;\n" +
        "    v_TexCoord = a_TexCoord;\n" +
        "    gl_Position = vec4(pos, 0.0, 1.0);\n" +
        "}\n";

    private static final String PHOTO_FRAGMENT =
        "precision mediump float;\n" +
        "varying vec2 v_TexCoord;\n" +
        "uniform sampler2D u_Texture;\n" +
        "uniform float u_Time;\n" +
        "uniform float u_Alpha;\n" +
        "void main() {\n" +
        "    vec4 texColor = texture2D(u_Texture, v_TexCoord);\n" +
        "    vec3 warm = texColor.rgb;\n" +
        "    warm.r = min(1.0, warm.r * 1.1 + 0.05);\n" +
        "    warm.g = warm.g * 1.02;\n" +
        "    warm.b = warm.b * 0.9;\n" +
        "    vec2 uv = v_TexCoord - 0.5;\n" +
        "    float vignette = 1.0 - dot(uv, uv) * 0.5;\n" +
        "    warm *= vignette;\n" +
        "    float glow = 1.0 + sin(u_Time * 2.0) * 0.03;\n" +
        "    warm *= glow;\n" +
        "    gl_FragColor = vec4(warm, u_Alpha);\n" +
        "}\n";

    // Frame - animated with photo
    private static final String FRAME_VERTEX =
        "attribute vec2 a_Position;\n" +
        "uniform float u_Progress;\n" +
        "uniform float u_Aspect;\n" +
        "uniform float u_YOffset;\n" +
        "uniform float u_Scale;\n" +
        "varying vec2 v_Pos;\n" +
        "void main() {\n" +
        "    vec2 pos = a_Position;\n" +
        "    pos *= u_Scale * 0.08;\n" +
        "    pos.x /= u_Aspect;\n" +
        "    pos.y += u_YOffset;\n" +
        "    v_Pos = a_Position;\n" +
        "    gl_Position = vec4(pos, 0.0, 1.0);\n" +
        "}\n";

    // Simple thin gold frame
    private static final String FRAME_FRAGMENT =
        "precision mediump float;\n" +
        "varying vec2 v_Pos;\n" +
        "uniform float u_Time;\n" +
        "uniform float u_Alpha;\n" +
        "void main() {\n" +
        "    vec2 uv = v_Pos * 0.5 + 0.5;\n" +
        "    float borderWidth = 0.025;\n" +
        "    float innerEdge = borderWidth;\n" +
        "    float outerEdge = 1.0 - borderWidth;\n" +
        "    if (uv.x > innerEdge && uv.x < outerEdge && uv.y > innerEdge && uv.y < outerEdge) {\n" +
        "        discard;\n" +
        "    }\n" +
        "    vec3 gold = vec3(0.95, 0.8, 0.4);\n" +
        "    float shimmer = sin(u_Time * 2.0) * 0.05 + 0.95;\n" +
        "    gl_FragColor = vec4(gold * shimmer, u_Alpha);\n" +
        "}\n";

    private static final String PARTICLE_VERTEX =
        "attribute vec2 a_Position;\n" +
        "attribute float a_Size;\n" +
        "attribute float a_Alpha;\n" +
        "uniform float u_Aspect;\n" +
        "varying float v_Alpha;\n" +
        "void main() {\n" +
        "    vec2 pos = a_Position;\n" +
        "    pos.x /= u_Aspect;\n" +
        "    gl_Position = vec4(pos, 0.0, 1.0);\n" +
        "    gl_PointSize = a_Size;\n" +
        "    v_Alpha = a_Alpha;\n" +
        "}\n";

    private static final String PARTICLE_FRAGMENT =
        "precision mediump float;\n" +
        "varying float v_Alpha;\n" +
        "void main() {\n" +
        "    vec2 coord = gl_PointCoord - 0.5;\n" +
        "    float dist = length(coord);\n" +
        "    float glow = 1.0 - smoothstep(0.0, 0.5, dist);\n" +
        "    vec3 gold = vec3(1.0, 0.85, 0.3);\n" +
        "    gl_FragColor = vec4(gold * glow, glow * v_Alpha);\n" +
        "}\n";

    // Dark overlay to dim background
    private static final String OVERLAY_VERTEX =
        "attribute vec2 a_Position;\n" +
        "void main() {\n" +
        "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
        "}\n";

    private static final String OVERLAY_FRAGMENT =
        "precision mediump float;\n" +
        "uniform float u_Alpha;\n" +
        "void main() {\n" +
        "    gl_FragColor = vec4(0.0, 0.0, 0.0, u_Alpha * 0.85);\n" +
        "}\n";

    private int overlayShader;
    private int uOverlayAlphaLoc;
    private FloatBuffer overlayBuffer;

    public GiftPhotoReveal(Context context) {
        this.context = context.getApplicationContext();
        this.galleryManager = new PhotoGalleryManager(context);
        initParticles();
        Log.d(TAG, "🎁 GiftPhotoReveal inicializado");
    }

    private void initParticles() {
        for (int i = 0; i < NUM_PARTICLES; i++) {
            particleLife[i] = -1;
        }
    }

    /**
     * Initialize OpenGL resources. MUST be called from GL thread.
     */
    private void initOpenGL() {
        if (initialized) return;

        Log.d(TAG, "🎁 Iniciando shaders OpenGL (GL thread)...");

        shaderProgram = createProgram(PHOTO_VERTEX, PHOTO_FRAGMENT, "Photo");
        if (shaderProgram == 0) {
            Log.e(TAG, "Failed to create photo shader");
            return;
        }

        uTextureLoc = GLES20.glGetUniformLocation(shaderProgram, "u_Texture");
        uTimeLoc = GLES20.glGetUniformLocation(shaderProgram, "u_Time");
        uProgressLoc = GLES20.glGetUniformLocation(shaderProgram, "u_Progress");
        uAlphaLoc = GLES20.glGetUniformLocation(shaderProgram, "u_Alpha");
        uAspectLoc = GLES20.glGetUniformLocation(shaderProgram, "u_Aspect");
        uYOffsetLoc = GLES20.glGetUniformLocation(shaderProgram, "u_YOffset");
        uScaleLoc = GLES20.glGetUniformLocation(shaderProgram, "u_Scale");

        frameShaderProgram = createProgram(FRAME_VERTEX, FRAME_FRAGMENT, "Frame");
        if (frameShaderProgram == 0) {
            Log.e(TAG, "Failed to create frame shader");
            return;
        }

        uFrameTimeLoc = GLES20.glGetUniformLocation(frameShaderProgram, "u_Time");
        uFrameProgressLoc = GLES20.glGetUniformLocation(frameShaderProgram, "u_Progress");
        uFrameAlphaLoc = GLES20.glGetUniformLocation(frameShaderProgram, "u_Alpha");
        uFrameAspectLoc = GLES20.glGetUniformLocation(frameShaderProgram, "u_Aspect");
        uFrameYOffsetLoc = GLES20.glGetUniformLocation(frameShaderProgram, "u_YOffset");
        uFrameScaleLoc = GLES20.glGetUniformLocation(frameShaderProgram, "u_Scale");

        particleShader = createProgram(PARTICLE_VERTEX, PARTICLE_FRAGMENT, "Particle");
        if (particleShader != 0) {
            pPositionLoc = GLES20.glGetAttribLocation(particleShader, "a_Position");
            pSizeLoc = GLES20.glGetAttribLocation(particleShader, "a_Size");
            pAlphaLoc = GLES20.glGetAttribLocation(particleShader, "a_Alpha");
            pAspectLoc = GLES20.glGetUniformLocation(particleShader, "u_Aspect");
        }

        // Dark overlay shader
        overlayShader = createProgram(OVERLAY_VERTEX, OVERLAY_FRAGMENT, "Overlay");
        if (overlayShader != 0) {
            uOverlayAlphaLoc = GLES20.glGetUniformLocation(overlayShader, "u_Alpha");
        }

        float[] quadVerts = {
            -1f, -1f,  0f, 1f,
             1f, -1f,  1f, 1f,
            -1f,  1f,  0f, 0f,
             1f,  1f,  1f, 0f
        };
        ByteBuffer bb = ByteBuffer.allocateDirect(quadVerts.length * 4);
        bb.order(ByteOrder.nativeOrder());
        quadBuffer = bb.asFloatBuffer();
        quadBuffer.put(quadVerts);
        quadBuffer.position(0);

        // Full screen quad for overlay (no texture coords needed)
        float[] overlayVerts = { -1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f };
        ByteBuffer ob = ByteBuffer.allocateDirect(overlayVerts.length * 4);
        ob.order(ByteOrder.nativeOrder());
        overlayBuffer = ob.asFloatBuffer();
        overlayBuffer.put(overlayVerts);
        overlayBuffer.position(0);

        ByteBuffer pb = ByteBuffer.allocateDirect(NUM_PARTICLES * 4 * 4);
        pb.order(ByteOrder.nativeOrder());
        particleBuffer = pb.asFloatBuffer();

        initialized = true;
        Log.d(TAG, "✅ GiftPhotoReveal shaders inicializados OK");
    }

    private int createProgram(String vertexSrc, String fragmentSrc, String name) {
        int vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexSrc, name + " vertex");
        int fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSrc, name + " fragment");
        if (vs == 0 || fs == 0) {
            Log.e(TAG, "❌ Failed to compile shaders for " + name);
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program == 0) {
            Log.e(TAG, "❌ glCreateProgram returned 0 for " + name);
            return 0;
        }

        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);
        GLES20.glLinkProgram(program);

        int[] status = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            String error = GLES20.glGetProgramInfoLog(program);
            Log.e(TAG, "❌ Link error for " + name + ": " + error);
            GLES20.glDeleteProgram(program);
            return 0;
        }

        GLES20.glDeleteShader(vs);
        GLES20.glDeleteShader(fs);

        Log.d(TAG, "✅ Shader " + name + " compilado OK");
        return program;
    }

    private int compileShader(int type, String source, String name) {
        int shader = GLES20.glCreateShader(type);
        if (shader == 0) {
            Log.e(TAG, "❌ glCreateShader returned 0 for " + name);
            return 0;
        }

        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            String error = GLES20.glGetShaderInfoLog(shader);
            Log.e(TAG, "❌ Shader compile error for " + name + ": " + error);
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    /**
     * Trigger photo reveal. Can be called from any thread.
     * Bitmap loading happens here, but OpenGL operations are deferred to draw().
     */
    public void trigger() {
        if (state != State.IDLE) {
            Log.d(TAG, "🎁 trigger() ignorado - estado actual: " + state);
            return;
        }

        Log.d(TAG, "🎁 trigger() - Obteniendo foto de galeria...");

        Bitmap photo = galleryManager.getRandomPhoto();
        if (photo == null) {
            Log.w(TAG, "⚠️ No se pudo obtener foto de galeria");
            return;
        }

        Log.d(TAG, "🎁 Foto obtenida: " + photo.getWidth() + "x" + photo.getHeight());

        // Store bitmap for later - will be uploaded to GPU on GL thread
        pendingBitmap = photo;
        state = State.PENDING_TEXTURE;
        stateTime = 0f;
        spawnParticles();

        Log.d(TAG, "🎁 Foto lista para subir a GPU");
    }

    /**
     * Upload pending bitmap to GPU texture. MUST be called from GL thread.
     */
    private void uploadPendingTexture() {
        if (pendingBitmap == null) return;

        if (!initialized) {
            initOpenGL();
            if (!initialized) {
                Log.e(TAG, "❌ No se pudieron inicializar shaders");
                pendingBitmap.recycle();
                pendingBitmap = null;
                state = State.IDLE;
                return;
            }
        }

        if (photoTextureId != 0) {
            GLES20.glDeleteTextures(1, new int[]{photoTextureId}, 0);
        }

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        photoTextureId = textures[0];

        if (photoTextureId == 0) {
            Log.e(TAG, "❌ glGenTextures returned 0");
            pendingBitmap.recycle();
            pendingBitmap = null;
            state = State.IDLE;
            return;
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, photoTextureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, pendingBitmap, 0);

        Log.d(TAG, "✅ Textura de foto cargada: " + pendingBitmap.getWidth() + "x" + pendingBitmap.getHeight() + ", ID=" + photoTextureId);

        pendingBitmap.recycle();
        pendingBitmap = null;
        hasPhoto = true;

        // Now transition to APPEARING state
        state = State.APPEARING;
        stateTime = 0f;

        Log.d(TAG, "🎁 ¡Abriendo regalo con foto! Estado: APPEARING");
    }

    private void spawnParticles() {
        for (int i = 0; i < NUM_PARTICLES; i++) {
            particleX[i] = (random.nextFloat() - 0.5f) * 0.3f;
            particleY[i] = (random.nextFloat() - 0.5f) * 0.3f;
            float angle = random.nextFloat() * (float)(Math.PI * 2);
            float speed = 0.5f + random.nextFloat() * 1.0f;
            particleVX[i] = (float)Math.cos(angle) * speed;
            particleVY[i] = (float)Math.sin(angle) * speed;
            particleLife[i] = 1.5f + random.nextFloat() * 0.5f;
            particleSize[i] = 15f + random.nextFloat() * 25f;
        }
    }

    public void update(float dt) {
        if (state == State.IDLE) return;

        totalTime += dt;
        stateTime += dt;

        for (int i = 0; i < NUM_PARTICLES; i++) {
            if (particleLife[i] > 0) {
                particleLife[i] -= dt;
                particleX[i] += particleVX[i] * dt;
                particleY[i] += particleVY[i] * dt;
                particleVY[i] -= dt * 0.5f;
                particleSize[i] *= (1.0f - dt * 0.3f);
            }
        }

        switch (state) {
            case PENDING_TEXTURE:
                // Waiting for GL thread to upload texture - handled in draw()
                break;
            case APPEARING:
                if (stateTime >= APPEAR_DURATION) {
                    state = State.VISIBLE;
                    stateTime = 0f;
                    Log.d(TAG, "🎁 Estado: VISIBLE");
                }
                break;
            case VISIBLE:
                if (stateTime >= VISIBLE_DURATION) {
                    state = State.DISAPPEARING;
                    stateTime = 0f;
                    Log.d(TAG, "🎁 Estado: DISAPPEARING");
                }
                break;
            case DISAPPEARING:
                if (stateTime >= DISAPPEAR_DURATION) {
                    state = State.IDLE;
                    stateTime = 0f;
                    hasPhoto = false;
                    Log.d(TAG, "🎁 Estado: IDLE");
                }
                break;
        }
    }

    public void draw() {
        if (state == State.IDLE) return;

        // Handle pending texture upload on GL thread
        if (state == State.PENDING_TEXTURE && pendingBitmap != null) {
            uploadPendingTexture();
            if (state == State.IDLE) return; // Upload failed
        }

        if (!hasPhoto) return;

        if (!initialized) {
            initOpenGL();
            if (!initialized) return;
        }

        float progress = 1.0f;
        float alpha = 1.0f;
        float yOffset = 0.0f;
        float scale = 1.0f;

        switch (state) {
            case APPEARING:
                // Descend from top
                progress = stateTime / APPEAR_DURATION;
                float t = progress * progress * (3.0f - 2.0f * progress); // smooth step
                yOffset = 1.2f * (1.0f - t);  // Start from top, descend to center
                scale = t;
                alpha = progress;
                break;
            case VISIBLE:
                // Gentle floating motion
                progress = 1.0f;
                alpha = 1.0f;
                scale = 1.0f;
                yOffset = (float) Math.sin(totalTime * 1.5) * 0.02f;  // Subtle bob
                break;
            case DISAPPEARING:
                // Shrink and fade with sparkle burst
                progress = 1.0f;
                float disappearT = stateTime / DISAPPEAR_DURATION;
                alpha = 1.0f - disappearT;
                scale = 1.0f - disappearT * 0.5f;  // Shrink to 50%
                yOffset = -disappearT * 0.3f;  // Drift down slightly
                break;
        }

        // Reset GL state for fullscreen overlay
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glViewport(0, 0, screenWidth, screenHeight);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Skip dark overlay - user prefers no background dimming
        // drawOverlay(alpha);
        drawFrame(progress, alpha, yOffset, scale);
        drawPhoto(progress, alpha, yOffset, scale);
        drawParticles();
    }

    private void drawOverlay(float alpha) {
        if (overlayShader == 0 || overlayBuffer == null) return;

        GLES20.glUseProgram(overlayShader);
        GLES20.glUniform1f(uOverlayAlphaLoc, alpha);

        int posLoc = GLES20.glGetAttribLocation(overlayShader, "a_Position");
        overlayBuffer.position(0);
        GLES20.glEnableVertexAttribArray(posLoc);
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 0, overlayBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(posLoc);
    }

    private void drawFrame(float progress, float alpha, float yOffset, float scale) {
        if (frameShaderProgram == 0) return;

        GLES20.glUseProgram(frameShaderProgram);
        GLES20.glUniform1f(uFrameTimeLoc, totalTime);
        GLES20.glUniform1f(uFrameProgressLoc, progress);
        GLES20.glUniform1f(uFrameAlphaLoc, alpha);
        GLES20.glUniform1f(uFrameAspectLoc, aspectRatio);
        GLES20.glUniform1f(uFrameYOffsetLoc, yOffset);
        GLES20.glUniform1f(uFrameScaleLoc, scale);

        int posLoc = GLES20.glGetAttribLocation(frameShaderProgram, "a_Position");
        quadBuffer.position(0);
        GLES20.glEnableVertexAttribArray(posLoc);
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 16, quadBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(posLoc);
    }

    private void drawPhoto(float progress, float alpha, float yOffset, float scale) {
        if (shaderProgram == 0) return;

        GLES20.glUseProgram(shaderProgram);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, photoTextureId);
        GLES20.glUniform1i(uTextureLoc, 0);
        GLES20.glUniform1f(uTimeLoc, totalTime);
        GLES20.glUniform1f(uProgressLoc, progress);
        GLES20.glUniform1f(uAlphaLoc, alpha);
        GLES20.glUniform1f(uAspectLoc, aspectRatio);
        GLES20.glUniform1f(uYOffsetLoc, yOffset);
        GLES20.glUniform1f(uScaleLoc, scale);

        int posLoc = GLES20.glGetAttribLocation(shaderProgram, "a_Position");
        int texLoc = GLES20.glGetAttribLocation(shaderProgram, "a_TexCoord");

        quadBuffer.position(0);
        GLES20.glEnableVertexAttribArray(posLoc);
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 16, quadBuffer);

        quadBuffer.position(2);
        GLES20.glEnableVertexAttribArray(texLoc);
        GLES20.glVertexAttribPointer(texLoc, 2, GLES20.GL_FLOAT, false, 16, quadBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(posLoc);
        GLES20.glDisableVertexAttribArray(texLoc);
    }

    private void drawParticles() {
        if (particleShader == 0) return;

        particleBuffer.clear();
        int count = 0;
        for (int i = 0; i < NUM_PARTICLES; i++) {
            if (particleLife[i] > 0) {
                float a = Math.min(1.0f, particleLife[i]);
                particleBuffer.put(particleX[i]);
                particleBuffer.put(particleY[i]);
                particleBuffer.put(particleSize[i]);
                particleBuffer.put(a);
                count++;
            }
        }

        if (count == 0) return;

        particleBuffer.position(0);

        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);
        GLES20.glUseProgram(particleShader);
        GLES20.glUniform1f(pAspectLoc, aspectRatio);

        int stride = 4 * 4;

        particleBuffer.position(0);
        GLES20.glEnableVertexAttribArray(pPositionLoc);
        GLES20.glVertexAttribPointer(pPositionLoc, 2, GLES20.GL_FLOAT, false, stride, particleBuffer);

        particleBuffer.position(2);
        GLES20.glEnableVertexAttribArray(pSizeLoc);
        GLES20.glVertexAttribPointer(pSizeLoc, 1, GLES20.GL_FLOAT, false, stride, particleBuffer);

        particleBuffer.position(3);
        GLES20.glEnableVertexAttribArray(pAlphaLoc);
        GLES20.glVertexAttribPointer(pAlphaLoc, 1, GLES20.GL_FLOAT, false, stride, particleBuffer);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, count);

        GLES20.glDisableVertexAttribArray(pPositionLoc);
        GLES20.glDisableVertexAttribArray(pSizeLoc);
        GLES20.glDisableVertexAttribArray(pAlphaLoc);

        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void setScreenSize(int w, int h) {
        screenWidth = w;
        screenHeight = h;
        aspectRatio = (float) w / h;
    }

    public boolean isActive() {
        return state != State.IDLE;
    }

    public boolean hasGalleryPermission() {
        return galleryManager.hasGalleryPermission();
    }

    public String getRequiredPermission() {
        return galleryManager.getRequiredPermission();
    }

    public void dispose() {
        if (pendingBitmap != null) {
            pendingBitmap.recycle();
            pendingBitmap = null;
        }
        if (photoTextureId != 0) {
            GLES20.glDeleteTextures(1, new int[]{photoTextureId}, 0);
            photoTextureId = 0;
        }
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        if (frameShaderProgram != 0) {
            GLES20.glDeleteProgram(frameShaderProgram);
            frameShaderProgram = 0;
        }
        if (particleShader != 0) {
            GLES20.glDeleteProgram(particleShader);
            particleShader = 0;
        }
        initialized = false;
        hasPhoto = false;
        state = State.IDLE;
        Log.d(TAG, "🎁 GiftPhotoReveal disposed");
    }
}
