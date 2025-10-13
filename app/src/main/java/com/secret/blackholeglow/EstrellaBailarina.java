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
public class EstrellaBailarina extends BaseShaderProgram implements SceneObject, CameraAware, MusicReactive {
    private static final String TAG = "depurar";  // Mismo TAG que SceneRenderer para debugging

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

    // ===== GEOMETRÍA SIMPLE PARA PARTÍCULAS =====
    private FloatBuffer particleVertexBuffer;
    private FloatBuffer particleTexCoordBuffer;

    // ===== SISTEMA DE EXPLOSIÓN DE PARTÍCULAS =====
    private static class Particle {
        float x, y, z;           // Posición
        float vx, vy, vz;        // Velocidad
        float life;              // Vida restante (1.0 = recién creada, 0.0 = muerta)
        float size;              // Tamaño
        float[] color;           // Color RGB
        boolean active;          // Está activa?

        Particle() {
            color = new float[3];
            active = false;
        }
    }

    private static final int MAX_PARTICLES = 12;  // Máximo de partículas por estrella (optimizado)
    private final Particle[] particles = new Particle[MAX_PARTICLES];
    private float explosionCooldown = 0f;        // Tiempo hasta próxima explosión
    private static final float MIN_EXPLOSION_INTERVAL = 0.5f;  // MUY FRECUENTE: 0.5 segundos
    private float timeSinceLastExplosion = 0f;

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

    // Contador de frames para logs
    private int drawCallCount = 0;

    // ===== SISTEMA INTELIGENTE DE EXPLOSIONES MUSICALES =====
    private float lastBassLevel = 0f;        // Para detectar picos de bajos
    private float lastEnergyLevel = 0f;      // Para detectar cambios bruscos
    private static final float BASS_SPIKE_THRESHOLD = 0.25f;   // MÁS SENSIBLE (0.3 → 0.25)
    private static final float ENERGY_CHANGE_THRESHOLD = 0.2f;  // MÁS SENSIBLE (0.25 → 0.2)

    // Offset aleatorio para que cada estrella explote en momentos diferentes
    private final float randomOffset;

    public EstrellaBailarina(Context ctx, TextureManager texMgr,
                             float x, float y, float z, float scale, float spinSpeed) {
        // IMPORTANTE: Usar shaders simples de planeta para empezar
        super(ctx, "shaders/planeta_vertex.glsl",
                   "shaders/planeta_fragment.glsl");

        this.position = new float[]{x, y, z};
        this.baseScale = scale;
        this.spinSpeed = spinSpeed;
        // Textura simple (no la usaremos, usaremos color sólido)
        this.textureId = texMgr.getTexture(R.drawable.fondo_transparente);

        // Offset aleatorio (0.0 - 1.0) para que cada estrella sea única
        this.randomOffset = random.nextFloat();

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

        // ===== INICIALIZAR SISTEMA DE PARTÍCULAS =====
        for (int i = 0; i < MAX_PARTICLES; i++) {
            particles[i] = new Particle();
        }

        // ===== CREAR GEOMETRÍA SIMPLE PARA PARTÍCULAS (CUADRADO BILLBOARD) =====
        createParticleGeometry();

        Log.d(TAG, "✨ EstrelaBailarina INICIALIZADA CORRECTAMENTE ✨");
        Log.d(TAG, "   Program ID: " + programId);
        Log.d(TAG, "   Vertex count: " + indexCount);
        Log.d(TAG, "   Estela mágica: ACTIVADA (tipo RAINBOW)");
        Log.d(TAG, "   Sistema de partículas: " + MAX_PARTICLES + " partículas listas 💥");
        Log.d(TAG, "   🔥🔥🔥 VERSIÓN CON EXPLOSIONES v2.0 🔥🔥🔥");
    }

    /**
     * Crea geometría simple de cuadrado para partículas (más eficiente que esfera completa)
     */
    private void createParticleGeometry() {
        // Cuadrado simple (2 triángulos = 6 vértices)
        float[] vertices = {
            -0.5f, -0.5f, 0.0f,  // Abajo-izquierd
             0.5f, -0.5f, 0.0f,  // Abajo-derecha
             0.5f,  0.5f, 0.0f,  // Arriba-derecha

            -0.5f, -0.5f, 0.0f,  // Abajo-izquierda
             0.5f,  0.5f, 0.0f,  // Arriba-derecha
            -0.5f,  0.5f, 0.0f   // Arriba-izquierda
        };

        // UV coordinates
        float[] texCoords = {
            0.0f, 0.0f,  // Abajo-izquierda
            1.0f, 0.0f,  // Abajo-derecha
            1.0f, 1.0f,  // Arriba-derecha

            0.0f, 0.0f,  // Abajo-izquierda
            1.0f, 1.0f,  // Arriba-derecha
            0.0f, 1.0f   // Arriba-izquierda
        };

        // Crear buffers
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        particleVertexBuffer = vbb.asFloatBuffer();
        particleVertexBuffer.put(vertices);
        particleVertexBuffer.position(0);

        ByteBuffer tbb = ByteBuffer.allocateDirect(texCoords.length * 4);
        tbb.order(ByteOrder.nativeOrder());
        particleTexCoordBuffer = tbb.asFloatBuffer();
        particleTexCoordBuffer.put(texCoords);
        particleTexCoordBuffer.position(0);

        Log.d(TAG, "✓ Geometría de partículas creada (cuadrado de 6 vértices)");
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

        // ===== ACTUALIZAR SISTEMA DE PARTÍCULAS =====
        updateParticles(dt);

        // ===== COOLDOWN DE EXPLOSIÓN =====
        timeSinceLastExplosion += dt;
        if (explosionCooldown > 0) {
            explosionCooldown -= dt;
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
            Log.d(TAG, String.format("🦋 MARIPOSA MÁGICA en (%.2f, %.2f, %.2f)",
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

        Log.d(TAG, String.format("🦋 Nueva dirección: (%.2f, %.2f, %.2f)",
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

        // ===== COLOR SUTIL Y BONITO =====
        // Usar textura para que el shader planet haga efectos bonitos
        int uUseSolidColorLoc = GLES20.glGetUniformLocation(programId, "u_UseSolidColor");
        GLES20.glUniform1i(uUseSolidColorLoc, 0);  // 0 = usar textura (permite efectos del shader)

        // Color base SUAVE - Amarillo/blanco cálido (el shader lo modulará con glow)
        GLES20.glUniform4f(uSolidColorLoc, 1.0f, 0.95f, 0.7f, 1.0f);  // Amarillo cálido suave
        GLES20.glUniform1f(uAlphaLoc, 0.85f);  // Ligeramente transparente para suavidad

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

        // ===== DIBUJAR PARTÍCULAS DE EXPLOSIÓN (encima de la estrella) =====
        drawParticles();

        // Incrementar contador de frames
        drawCallCount++;
    }

    /**
     * Actualiza todas las partículas activas
     */
    private void updateParticles(float dt) {
        for (Particle p : particles) {
            if (!p.active) continue;

            // Actualizar posición con velocidad
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.z += p.vz * dt;

            // Aplicar gravedad sutil
            p.vy -= 0.5f * dt;

            // Reducir velocidad (fricción del aire)
            p.vx *= 0.97f;
            p.vy *= 0.97f;
            p.vz *= 0.97f;

            // Reducir vida (desvanecimiento)
            p.life -= dt * 0.8f;  // Desaparece en ~1.25 segundos

            // Desactivar si murió
            if (p.life <= 0f) {
                p.active = false;
            }
        }
    }

    /**
     * Crea una explosión de partículas en la posición actual
     */
    private void triggerExplosion(float intensity) {
        // No explotar si está en cooldown
        if (explosionCooldown > 0) {
            Log.d(TAG, "[triggerExplosion] ⏸️ Bloqueado por cooldown: " + String.format("%.2f", explosionCooldown));
            return;
        }

        Log.d(TAG, "[triggerExplosion] 💥✨ ¡EXPLOSIÓN! Intensidad: " + String.format("%.2f", intensity) +
                   ", Pos=(" + String.format("%.2f,%.2f,%.2f", position[0], position[1], position[2]) + ")");

        // Colores vibrantes para las partículas
        float[][] colors = {
            {1.0f, 0.9f, 0.3f},    // Amarillo dorado
            {1.0f, 0.5f, 0.2f},    // Naranja
            {0.3f, 0.9f, 1.0f},    // Cyan
            {1.0f, 0.3f, 0.8f},    // Rosa
            {0.8f, 1.0f, 0.3f}     // Verde lima
        };

        int particlesSpawned = 0;
        for (Particle p : particles) {
            if (p.active) continue;  // Solo usar partículas inactivas

            // Activar partícula
            p.active = true;
            p.x = position[0];
            p.y = position[1];
            p.z = position[2];

            // Velocidad aleatoria radial (explosión en todas direcciones)
            float angle = random.nextFloat() * (float)Math.PI * 2f;
            float elevation = (random.nextFloat() - 0.5f) * (float)Math.PI;
            float speed = 1.5f + random.nextFloat() * 2.5f * intensity;  // Más rápido con más intensidad

            p.vx = (float)(Math.cos(angle) * Math.cos(elevation)) * speed;
            p.vy = (float)(Math.sin(elevation)) * speed;
            p.vz = (float)(Math.sin(angle) * Math.cos(elevation)) * speed;

            // Vida y tamaño
            p.life = 0.8f + random.nextFloat() * 0.4f;  // 0.8 - 1.2 segundos
            p.size = 0.03f + random.nextFloat() * 0.04f * intensity;

            // Color aleatorio
            float[] selectedColor = colors[random.nextInt(colors.length)];
            System.arraycopy(selectedColor, 0, p.color, 0, 3);

            particlesSpawned++;
            if (particlesSpawned >= 8 + (int)(intensity * 4)) break;  // 8-12 partículas según intensidad
        }

        // Establecer cooldown
        explosionCooldown = MIN_EXPLOSION_INTERVAL;
        timeSinceLastExplosion = 0f;

        Log.d(TAG, "[triggerExplosion] ✅ Creadas " + particlesSpawned + " partículas, cooldown=" + MIN_EXPLOSION_INTERVAL + "s");
    }

    /**
     * Dibuja todas las partículas activas
     */
    private void drawParticles() {
        if (camera == null) return;

        // Contar partículas activas para log
        int activeCount = 0;
        for (Particle p : particles) {
            if (p.active && p.life > 0) activeCount++;
        }

        // Log reducido (cada 30 frames)
        if (activeCount > 0 && drawCallCount % 30 == 0) {
            Log.d(TAG, "[drawParticles] 🎨 DIBUJANDO " + activeCount + " PARTÍCULAS ACTIVAS");
        }

        // Configuración OpenGL para partículas
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);  // Blending aditivo para brillo
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);  // Sin depth test para partículas

        useProgram();

        for (Particle p : particles) {
            if (!p.active || p.life <= 0) continue;

            // Matriz de modelo para esta partícula
            Matrix.setIdentityM(model, 0);
            Matrix.translateM(model, 0, p.x, p.y, p.z);

            // Billboard: rotar para que siempre mire a la cámara
            Matrix.scaleM(model, 0, p.size, p.size, p.size);

            // Calcular MVP
            camera.computeMvp(model, mvp);
            setMvpAndResolution(mvp, SceneRenderer.screenWidth, SceneRenderer.screenHeight);

            // Color con alpha basado en vida (fade out)
            float alpha = p.life * 0.9f;  // Fade out suave
            int uUseSolidColorLoc = GLES20.glGetUniformLocation(programId, "u_UseSolidColor");
            GLES20.glUniform1i(uUseSolidColorLoc, 1);  // Usar color sólido
            GLES20.glUniform4f(uSolidColorLoc, p.color[0], p.color[1], p.color[2], alpha);
            GLES20.glUniform1f(uAlphaLoc, alpha);

            // Textura
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(uTexLoc, 0);

            // Atributos - USAR GEOMETRÍA SIMPLE DE CUADRADO
            particleVertexBuffer.position(0);
            GLES20.glEnableVertexAttribArray(aPosLoc);
            GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, 0, particleVertexBuffer);

            if (aTexLoc >= 0) {
                particleTexCoordBuffer.position(0);
                GLES20.glEnableVertexAttribArray(aTexLoc);
                GLES20.glVertexAttribPointer(aTexLoc, 2, GLES20.GL_FLOAT, false, 0, particleTexCoordBuffer);
            }

            // Dibujar partícula como cuadrado simple (6 vértices = 2 triángulos)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        }

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPosLoc);
        if (aTexLoc >= 0) {
            GLES20.glDisableVertexAttribArray(aTexLoc);
        }

        // Restaurar estados OpenGL
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    // ===== IMPLEMENTACIÓN DE MUSICREACTIVE =====

    @Override
    public void onMusicData(float bassLevel, float midLevel, float trebleLevel,
                            float volumeLevel, float beatIntensity, boolean isBeat) {
        if (!musicReactive) {
            Log.d(TAG, "[onMusicData] ⚠️ musicReactive está DESACTIVADO!");
            return;
        }

        // Log periódico para confirmar que recibe datos (cada 5 segundos)
        if (drawCallCount % 300 == 0) {
            Log.d(TAG, "[onMusicData] 🎵 Recibiendo datos musicales - Bass:" + String.format("%.2f", bassLevel) +
                       " Mid:" + String.format("%.2f", midLevel) +
                       " Treble:" + String.format("%.2f", trebleLevel));
        }

        // BAJOS → Efecto principal (pulso y crecimiento)
        bassBoost = bassLevel;

        // ENERGÍA GENERAL → Brillo y partículas
        musicEnergy = bassLevel * 0.5f + midLevel * 0.3f + trebleLevel * 0.2f;

        // ===== MOVIMIENTO REACTIVO A LA MÚSICA =====
        // MEDIOS → Velocidad de movimiento (1.0x a 3.0x más rápido)
        musicSpeedMultiplier = 1.0f + midLevel * 2.0f;

        // AGUDOS → Intensidad de ondulaciones (1.0x a 2.5x más intenso)
        musicWaveIntensity = 1.0f + trebleLevel * 1.5f;

        // ===== 💥 SISTEMA INTELIGENTE DE EXPLOSIONES 💥 =====
        // Solo si no está en cooldown
        if (explosionCooldown <= 0) {
            boolean shouldExplode = false;
            float explosionIntensity = 0f;
            String explosionReason = "";

            // Usar randomOffset para que cada estrella reaccione diferente
            float personalProbability = 0.7f + (randomOffset * 0.3f); // 70% - 100%

            // 1️⃣ BEATS FUERTES (MÁS SENSIBLE: >0.45)
            if (isBeat && beatIntensity > 0.45f && random.nextFloat() < personalProbability) {
                shouldExplode = true;
                explosionIntensity = beatIntensity;
                explosionReason = "BEAT";
            }

            // 2️⃣ PICOS DE BAJOS (cuando el bajo sube de repente)
            float bassDiff = bassLevel - lastBassLevel;
            if (bassDiff > BASS_SPIKE_THRESHOLD && bassLevel > 0.4f && random.nextFloat() < personalProbability) {
                shouldExplode = true;
                explosionIntensity = Math.max(explosionIntensity, bassLevel);
                explosionReason = "BAJO";
            }

            // 3️⃣ CAMBIOS BRUSCOS DE ENERGÍA
            float energyDiff = Math.abs(musicEnergy - lastEnergyLevel);
            if (energyDiff > ENERGY_CHANGE_THRESHOLD && musicEnergy > 0.3f && random.nextFloat() < personalProbability) {
                shouldExplode = true;
                explosionIntensity = Math.max(explosionIntensity, musicEnergy);
                explosionReason = "ENERGÍA";
            }

            // 4️⃣ EXPLOSIONES ALEATORIAS (sin música, para mantener vida)
            // Cada estrella tiene chance aleatoria cada frame si hay algo de energía
            if (musicEnergy > 0.2f && random.nextFloat() < 0.002f * randomOffset) {
                shouldExplode = true;
                explosionIntensity = 0.5f;
                explosionReason = "RANDOM";
            }

            // Ejecutar explosión si se cumple alguna condición
            if (shouldExplode) {
                triggerExplosion(Math.max(explosionIntensity, 0.5f));
                Log.d(TAG, "[💥 " + explosionReason + "] Intensidad: " + String.format("%.2f", explosionIntensity));
            }
        }

        // Guardar niveles para próxima comparación
        lastBassLevel = bassLevel;
        lastEnergyLevel = musicEnergy;

        // CAMBIO DE DIRECCIÓN con beats fuertes
        if (isBeat && beatIntensity > 0.7f) {
            beatPulse = beatIntensity * BEAT_FLASH_MULTIPLIER;
            pickNewDirection();
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
