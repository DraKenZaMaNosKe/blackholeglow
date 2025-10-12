package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.util.ObjLoader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.List;

/**
 * EstrelaBailarina - Estrella espectacular que reacciona a la música
 * Es la ESTRELLA PRINCIPAL del show, respondiendo dramáticamente a:
 * - BAJOS: Pulsa y crece intensamente
 * - BEATS: Destellos explosivos
 * - ENERGÍA: Partículas y brillo
 */
public class EstrelaBailarina extends BaseShaderProgram implements SceneObject, CameraAware, MusicReactive {
    private static final String TAG = "EstrellaBailarina";

    // Geometría
    private final FloatBuffer vertexBuffer, texCoordBuffer;
    private final ShortBuffer indexBuffer;
    private final int indexCount;

    // Shader locations
    private final int aPosLoc, aTexLoc;
    private final int uTexLoc, uSolidColorLoc, uAlphaLoc;
    private final int uBassBoostLoc, uBeatPulseLoc, uMusicEnergyLoc;

    // Configuración
    private final int textureId;
    private final float[] position;  // Posición dinámica (se mueve)
    private final float baseScale;
    private float rotation = 0f;
    private final float spinSpeed;

    // ===== SISTEMA DE MOVIMIENTO ALEATORIO TIPO MARIPOSA =====
    private float velocityX = 0f;
    private float velocityY = 0f;
    private float velocityZ = 0f;
    private float targetVelocityX = 0f;
    private float targetVelocityY = 0f;
    private float targetVelocityZ = 0f;
    private float directionChangeTimer = 0f;
    private final float baseSpeed = 1.5f;  // Velocidad base de movimiento
    private float movementPhase = 0f;  // Para movimiento ondulante

    // Modificadores de velocidad por música
    private float musicSpeedMultiplier = 1.0f;  // Multiplica velocidad base (1.0 = normal, 3.0 = 3x más rápido)
    private float musicWaveIntensity = 1.0f;    // Intensifica ondulaciones (1.0 = normal, 2.0 = 2x más intenso)

    // Límites de la escena (más contenidos para mantenerla visible en pantalla)
    private static final float MIN_X = -3.0f;
    private static final float MAX_X = 3.0f;
    private static final float MIN_Y = -2.0f;
    private static final float MAX_Y = 2.0f;
    private static final float MIN_Z = -3.0f;
    private static final float MAX_Z = 3.0f;

    // Random para movimientos aleatorios
    private final java.util.Random random = new java.util.Random();

    // ===== ESTELA MÁGICA =====
    private final MeteorTrail trail;

    // Camera
    private CameraController camera;

    // Matrices
    private final float[] model = new float[16];
    private final float[] mvp = new float[16];

    // ===== REACTIVIDAD MUSICAL =====
    private boolean musicReactive = true;
    private float bassBoost = 0f;           // 0-1
    private float beatPulse = 0f;           // 0-1
    private float musicEnergy = 0f;         // 0-1
    private float scaleBoost = 0f;          // Boost de escala por música

    // Constantes de efectos
    private static final float MAX_SCALE_BOOST = 0.6f;   // Hasta +60% de tamaño con bajos
    private static final float BEAT_FLASH_MULTIPLIER = 1.2f;

    // Log de música
    private long lastMusicLogTime = 0;

    public EstrelaBailarina(Context ctx, TextureManager texMgr,
                           float x, float y, float z, float scale, float spinSpeed) {
        // IMPORTANTE: Usar shaders simples de planeta para empezar
        super(ctx, "shaders/planeta_vertex.glsl",
                   "shaders/planeta_fragment.glsl");

        this.position = new float[]{x, y, z};
        this.baseScale = scale;
        this.spinSpeed = spinSpeed;
        // Textura simple (no la usaremos, usaremos color sólido)
        this.textureId = texMgr.getTexture(R.drawable.fondo_transparente);

        Log.d(TAG, "═══════════════════════════════════════════════");
        Log.d(TAG, "✨ CREANDO ESTRELABAILARINA ✨");
        Log.d(TAG, String.format("   Posición: (%.2f, %.2f, %.2f)", x, y, z));
        Log.d(TAG, String.format("   Escala: %.2f", scale));
        Log.d(TAG, "═══════════════════════════════════════════════");

        // Cargar geometría
        ObjLoader.Mesh mesh;
        try {
            mesh = ObjLoader.loadObj(ctx, "planeta.obj");
            Log.d(TAG, "✓ Malla cargada: " + mesh.vertexCount + " vértices");
        } catch (IOException e) {
            throw new RuntimeException("Error cargando planeta.obj para estrella", e);
        }

        vertexBuffer = mesh.vertexBuffer;
        texCoordBuffer = mesh.uvBuffer;

        // Construir índices
        List<short[]> faces = mesh.faces;
        int triCount = 0;
        for (short[] f : faces) triCount += f.length - 2;
        indexCount = triCount * 3;

        ShortBuffer ib = ByteBuffer
                .allocateDirect(indexCount * Short.BYTES)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        for (short[] f : faces) {
            short v0 = f[0];
            for (int i = 1; i < f.length - 1; i++) {
                ib.put(v0).put(f[i]).put(f[i + 1]);
            }
        }
        ib.position(0);
        indexBuffer = ib;

        // Obtener uniform locations (usando shader de planeta)
        aPosLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uTexLoc = GLES20.glGetUniformLocation(programId, "u_Texture");
        uSolidColorLoc = GLES20.glGetUniformLocation(programId, "u_SolidColor");
        uAlphaLoc = GLES20.glGetUniformLocation(programId, "u_Alpha");

        // Por ahora no usamos uniforms musicales personalizados
        uBassBoostLoc = -1;
        uBeatPulseLoc = -1;
        uMusicEnergyLoc = -1;

        // ===== CREAR ESTELA MÁGICA (ARCOÍRIS) =====
        trail = new MeteorTrail(MeteorTrail.TrailType.RAINBOW);
        trail.setContext(ctx);

        Log.d(TAG, "✨ EstrelaBailarina INICIALIZADA CORRECTAMENTE ✨");
        Log.d(TAG, "   Program ID: " + programId);
        Log.d(TAG, "   Vertex count: " + indexCount);
        Log.d(TAG, "   Estela mágica: ACTIVADA (tipo RAINBOW)");
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    @Override
    public void update(float dt) {
        // Rotación constante + boost musical
        float currentSpinSpeed = spinSpeed;
        if (musicReactive && musicEnergy > 0) {
            currentSpinSpeed *= (1.0f + musicEnergy * 2.0f);  // Hasta 3x más rápido con música
        }
        rotation = (rotation + dt * currentSpinSpeed) % 360f;

        // Decay suave del pulso de beat
        beatPulse *= 0.92f;

        // Suavizar el boost de escala
        float targetScale = bassBoost * MAX_SCALE_BOOST;
        scaleBoost = scaleBoost * 0.85f + targetScale * 0.15f;

        // ===== MOVIMIENTO TIPO MARIPOSA/PARTÍCULA MÁGICA =====
        updateButterflyMovement(dt);

        // ===== ACTUALIZAR ESTELA MÁGICA =====
        if (trail != null) {
            trail.update(dt, position[0], position[1], position[2], baseScale, true);
        }
    }

    /**
     * Actualiza el movimiento aleatorio tipo mariposa
     */
    private void updateButterflyMovement(float dt) {
        // Cambiar de dirección cada 1.5-3 segundos
        directionChangeTimer += dt;
        if (directionChangeTimer >= 1.5f + random.nextFloat() * 1.5f) {
            directionChangeTimer = 0f;
            pickNewDirection();
        }

        // Suavizar velocidad hacia la velocidad objetivo (movimiento fluido)
        float smoothing = 0.92f;
        velocityX = velocityX * smoothing + targetVelocityX * (1f - smoothing);
        velocityY = velocityY * smoothing + targetVelocityY * (1f - smoothing);
        velocityZ = velocityZ * smoothing + targetVelocityZ * (1f - smoothing);

        // Añadir movimiento ondulante (como alas de mariposa) CON INTENSIDAD MUSICAL
        movementPhase += dt * 3.0f * musicSpeedMultiplier;  // La música acelera las ondas
        float waveX = (float)Math.sin(movementPhase) * 0.3f * musicWaveIntensity;
        float waveY = (float)Math.cos(movementPhase * 1.3f) * 0.4f * musicWaveIntensity;
        float waveZ = (float)Math.sin(movementPhase * 0.7f) * 0.3f * musicWaveIntensity;

        // Actualizar posición CON VELOCIDAD MULTIPLICADA POR MÚSICA
        position[0] += (velocityX * musicSpeedMultiplier + waveX) * dt;
        position[1] += (velocityY * musicSpeedMultiplier + waveY) * dt;
        position[2] += (velocityZ * musicSpeedMultiplier + waveZ) * dt;

        // Aplicar límites (rebote suave en los bordes)
        if (position[0] < MIN_X) {
            position[0] = MIN_X;
            targetVelocityX = Math.abs(targetVelocityX);  // Rebotar hacia la derecha
        } else if (position[0] > MAX_X) {
            position[0] = MAX_X;
            targetVelocityX = -Math.abs(targetVelocityX);  // Rebotar hacia la izquierda
        }

        if (position[1] < MIN_Y) {
            position[1] = MIN_Y;
            targetVelocityY = Math.abs(targetVelocityY);  // Rebotar hacia arriba
        } else if (position[1] > MAX_Y) {
            position[1] = MAX_Y;
            targetVelocityY = -Math.abs(targetVelocityY);  // Rebotar hacia abajo
        }

        if (position[2] < MIN_Z) {
            position[2] = MIN_Z;
            targetVelocityZ = Math.abs(targetVelocityZ);  // Rebotar hacia adelante
        } else if (position[2] > MAX_Z) {
            position[2] = MAX_Z;
            targetVelocityZ = -Math.abs(targetVelocityZ);  // Rebotar hacia atrás
        }

        // Log periódico de posición (cada 5 segundos)
        if (directionChangeTimer < dt) {
            Log.v(TAG, String.format("🦋 MARIPOSA MÁGICA en (%.2f, %.2f, %.2f)",
                position[0], position[1], position[2]));
        }
    }

    /**
     * Elige una nueva dirección aleatoria de movimiento
     */
    private void pickNewDirection() {
        // Generar velocidad aleatoria en cada eje (-1 a 1) * velocidad base
        targetVelocityX = (random.nextFloat() * 2f - 1f) * baseSpeed;
        targetVelocityY = (random.nextFloat() * 2f - 1f) * baseSpeed;
        targetVelocityZ = (random.nextFloat() * 2f - 1f) * baseSpeed;

        Log.v(TAG, String.format("🦋 Nueva dirección: (%.2f, %.2f, %.2f)",
            targetVelocityX, targetVelocityY, targetVelocityZ));
    }

    @Override
    public void draw() {
        if (camera == null) {
            Log.e(TAG, "✗ ERROR: CameraController no asignado a EstrelaBailarina!");
            return;
        }

        // ===== DIBUJAR ESTELA PRIMERO (detrás de la estrella) =====
        if (trail != null) {
            trail.draw(camera);
        }

        if (!GLES20.glIsProgram(programId)) {
            Log.e(TAG, "✗ ERROR: Program ID inválido!");
            return;
        }

        useProgram();

        // Configuración OpenGL
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // ===== MATRIZ MODELO SIMPLE =====
        Matrix.setIdentityM(model, 0);

        // 1. Posición
        Matrix.translateM(model, 0, position[0], position[1], position[2]);

        // 2. Escala (SIN boost musical por ahora)
        float finalScale = baseScale;
        Matrix.scaleM(model, 0, finalScale, finalScale, finalScale);

        // 3. Rotación simple
        Matrix.rotateM(model, 0, rotation, 0, 1, 0);

        // ===== MVP =====
        camera.computeMvp(model, mvp);
        setMvpAndResolution(mvp, SceneRenderer.screenWidth, SceneRenderer.screenHeight);

        // ===== COLOR ROJO SÓLIDO =====
        // Activar modo de color sólido
        int uUseSolidColorLoc = GLES20.glGetUniformLocation(programId, "u_UseSolidColor");
        GLES20.glUniform1i(uUseSolidColorLoc, 1);  // 1 = usar color sólido

        // Color ROJO BRILLANTE
        GLES20.glUniform4f(uSolidColorLoc, 1.0f, 0.0f, 0.0f, 1.0f);  // ROJO puro
        GLES20.glUniform1f(uAlphaLoc, 1.0f);  // Completamente opaco

        // Textura (requerida pero no se usa)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTexLoc, 0);

        // Atributos
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        if (aTexLoc >= 0) {
            texCoordBuffer.position(0);
            GLES20.glEnableVertexAttribArray(aTexLoc);
            GLES20.glVertexAttribPointer(aTexLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);
        }

        // Dibujar
        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPosLoc);
        if (aTexLoc >= 0) {
            GLES20.glDisableVertexAttribArray(aTexLoc);
        }
    }

    // ===== IMPLEMENTACIÓN DE MUSICREACTIVE =====

    @Override
    public void onMusicData(float bassLevel, float midLevel, float trebleLevel,
                            float volumeLevel, float beatIntensity, boolean isBeat) {
        if (!musicReactive) return;

        // BAJOS → Efecto principal (pulso y crecimiento)
        bassBoost = bassLevel;

        // ENERGÍA GENERAL → Brillo y partículas
        musicEnergy = bassLevel * 0.5f + midLevel * 0.3f + trebleLevel * 0.2f;

        // ===== MOVIMIENTO REACTIVO A LA MÚSICA =====
        // MEDIOS → Velocidad de movimiento (1.0x a 3.0x más rápido)
        musicSpeedMultiplier = 1.0f + midLevel * 2.0f;

        // AGUDOS → Intensidad de ondulaciones (1.0x a 2.5x más intenso)
        musicWaveIntensity = 1.0f + trebleLevel * 1.5f;

        // BEATS → Destellos explosivos + cambio repentino de dirección
        if (isBeat && beatIntensity > 0.5f) {
            beatPulse = beatIntensity * BEAT_FLASH_MULTIPLIER;

            // ¡CAMBIO DE DIRECCIÓN con beats fuertes!
            if (beatIntensity > 0.7f) {
                pickNewDirection();
                Log.v(TAG, "✨💥 ¡BEAT! Nueva dirección por beat fuerte");
            }

            // Log cuando hay beat fuerte
            Log.v(TAG, "✨💥 ¡BEAT EXPLOSIVO! Intensidad: " + String.format("%.2f", beatIntensity));
        }

        // Log periódico de estado musical (cada 3 segundos)
        long now = System.currentTimeMillis();
        if (now - lastMusicLogTime > 3000 && musicEnergy > 0.1f) {
            Log.d(TAG, String.format("✨🎵 [BAILANDO] Speed:%.1fx Wave:%.1fx Energy:%.2f",
                    musicSpeedMultiplier, musicWaveIntensity, musicEnergy));
            lastMusicLogTime = now;
        }
    }

    @Override
    public void setMusicReactive(boolean enabled) {
        this.musicReactive = enabled;
        if (!enabled) {
            bassBoost = 0f;
            beatPulse = 0f;
            musicEnergy = 0f;
            scaleBoost = 0f;
        }
        Log.d(TAG, "✨ EstrelaBailarina música " + (enabled ? "ACTIVADA 🎵" : "DESACTIVADA"));
    }

    @Override
    public boolean isMusicReactive() {
        return musicReactive;
    }
}
