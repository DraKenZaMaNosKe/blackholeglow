package com.secret.blackholeglow;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ⚡ PLASMA BEAM WEAPON - Arma de Plasma con 3 Fases Épicas
 *
 * FASE 1: CARGA (~0.4s)
 *   - Esfera de energía formándose en el interceptor
 *   - Partículas girando y concentrándose
 *   - Crece mientras carga poder
 *
 * FASE 2: DISPARO (~0.3s)
 *   - Rayo viaja del interceptor al objetivo
 *   - Estela de partículas detrás
 *   - Núcleo brillante con halo
 *
 * FASE 3: IMPACTO (~0.6s)
 *   - Explosión de plasma en el objetivo
 *   - Anillos expandiéndose
 */
public class PlasmaBeamWeapon implements SceneObject, CameraAware {
    private static final String TAG = "PlasmaBeamWeapon";

    // ═══════════════════════════════════════════════════════════════════════
    // ⏱️ TIEMPOS DE CADA FASE
    // ═══════════════════════════════════════════════════════════════════════
    private static final float CHARGE_DURATION = 0.4f;
    private static final float TRAVEL_DURATION = 0.3f;
    private static final float IMPACT_DURATION = 0.6f;
    private static final float TOTAL_DURATION = CHARGE_DURATION + TRAVEL_DURATION + IMPACT_DURATION;

    // ═══════════════════════════════════════════════════════════════════════
    // 📐 CONFIGURACIÓN VISUAL
    // ═══════════════════════════════════════════════════════════════════════
    private static final float CHARGE_MAX_SIZE = 0.06f;      // Tamaño máximo de la esfera de carga (reducido)
    private static final float BEAM_WIDTH = 0.05f;           // Ancho del rayo (más delgado)
    private static final float IMPACT_MAX_RADIUS = 0.35f;    // Radio máximo de la explosión (reducido)
    private static final int CHARGE_PARTICLES = 12;          // Partículas orbitando durante carga
    private static final int TRAIL_PARTICLES = 8;            // Partículas en la estela
    private static final int CIRCLE_SEGMENTS = 20;

    // ═══════════════════════════════════════════════════════════════════════
    // 📊 ESTADO
    // ═══════════════════════════════════════════════════════════════════════
    public enum Phase {
        INACTIVE,
        CHARGING,
        TRAVELING,
        IMPACT
    }

    private Phase currentPhase = Phase.INACTIVE;
    private float phaseTime = 0f;
    private float totalTime = 0f;

    // Posiciones
    private float sourceX, sourceY, sourceZ;   // Posición del interceptor
    private float targetX, targetY, targetZ;   // Posición del objetivo
    private float beamX, beamY, beamZ;         // Posición actual del frente del rayo

    // Partículas de carga (ángulos orbitales)
    private final float[] chargeAngles = new float[CHARGE_PARTICLES];
    private final float[] chargeRadii = new float[CHARGE_PARTICLES];
    private final float[] chargeSpeeds = new float[CHARGE_PARTICLES];

    // ═══════════════════════════════════════════════════════════════════════
    // 🎨 OPENGL
    // ═══════════════════════════════════════════════════════════════════════
    private CameraController camera;
    private int shaderProgram;
    private int positionHandle;
    private int colorHandle;
    private int mvpMatrixHandle;

    private FloatBuffer circleBuffer;
    private FloatBuffer beamBuffer;
    private FloatBuffer particleBuffer;

    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private final float[] colorCache = new float[4];

    // Shaders
    private static final String VERTEX_SHADER =
        "uniform mat4 uMVPMatrix;" +
        "attribute vec4 vPosition;" +
        "void main() {" +
        "    gl_Position = uMVPMatrix * vPosition;" +
        "    gl_PointSize = 8.0;" +
        "}";

    private static final String FRAGMENT_SHADER =
        "precision mediump float;" +
        "uniform vec4 vColor;" +
        "void main() {" +
        "    gl_FragColor = vColor;" +
        "}";

    // ═══════════════════════════════════════════════════════════════════════
    // 🔧 CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════
    public PlasmaBeamWeapon() {
        initBuffers();
        initShaders();
        Log.d(TAG, "⚡ PlasmaBeamWeapon inicializado");
    }

    private void initBuffers() {
        // Círculo para esfera de carga e impacto
        float[] circle = new float[(CIRCLE_SEGMENTS + 1) * 3];
        for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
            float angle = (float) (i * 2 * Math.PI / CIRCLE_SEGMENTS);
            circle[i * 3] = (float) Math.cos(angle);
            circle[i * 3 + 1] = (float) Math.sin(angle);
            circle[i * 3 + 2] = 0;
        }
        ByteBuffer cb = ByteBuffer.allocateDirect(circle.length * 4);
        cb.order(ByteOrder.nativeOrder());
        circleBuffer = cb.asFloatBuffer();
        circleBuffer.put(circle);
        circleBuffer.position(0);

        // Buffer para el rayo (rectángulo que se estira)
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * 3 * 4);
        bb.order(ByteOrder.nativeOrder());
        beamBuffer = bb.asFloatBuffer();

        // Buffer para partículas
        ByteBuffer pb = ByteBuffer.allocateDirect(Math.max(CHARGE_PARTICLES, TRAIL_PARTICLES) * 3 * 4);
        pb.order(ByteOrder.nativeOrder());
        particleBuffer = pb.asFloatBuffer();
    }

    private void initShaders() {
        int vs = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER);
        GLES30.glShaderSource(vs, VERTEX_SHADER);
        GLES30.glCompileShader(vs);

        int fs = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER);
        GLES30.glShaderSource(fs, FRAGMENT_SHADER);
        GLES30.glCompileShader(fs);

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        positionHandle = GLES30.glGetAttribLocation(shaderProgram, "vPosition");
        colorHandle = GLES30.glGetUniformLocation(shaderProgram, "vColor");
        mvpMatrixHandle = GLES30.glGetUniformLocation(shaderProgram, "uMVPMatrix");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🎯 FIRE - Iniciar el disparo
    // ═══════════════════════════════════════════════════════════════════════
    public void fire(float srcX, float srcY, float srcZ, float tgtX, float tgtY, float tgtZ) {
        this.sourceX = srcX;
        this.sourceY = srcY;
        this.sourceZ = srcZ;
        this.targetX = tgtX;
        this.targetY = tgtY;
        this.targetZ = tgtZ;

        this.beamX = srcX;
        this.beamY = srcY;
        this.beamZ = srcZ;

        this.currentPhase = Phase.CHARGING;
        this.phaseTime = 0f;
        this.totalTime = 0f;

        // Inicializar partículas de carga con órbitas aleatorias
        for (int i = 0; i < CHARGE_PARTICLES; i++) {
            chargeAngles[i] = (float) (Math.random() * Math.PI * 2);
            chargeRadii[i] = 0.3f + (float) Math.random() * 0.2f;
            chargeSpeeds[i] = 3f + (float) Math.random() * 4f;
        }

        Log.d(TAG, "⚡ CARGANDO PLASMA BEAM...");
    }

    public boolean isActive() {
        return currentPhase != Phase.INACTIVE;
    }

    public Phase getCurrentPhase() {
        return currentPhase;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🔗 CAMERA AWARE
    // ═══════════════════════════════════════════════════════════════════════
    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🔄 UPDATE
    // ═══════════════════════════════════════════════════════════════════════
    @Override
    public void update(float deltaTime) {
        if (currentPhase == Phase.INACTIVE) return;

        phaseTime += deltaTime;
        totalTime += deltaTime;

        switch (currentPhase) {
            case CHARGING:
                if (phaseTime >= CHARGE_DURATION) {
                    currentPhase = Phase.TRAVELING;
                    phaseTime = 0f;
                    Log.d(TAG, "🚀 ¡PLASMA BEAM LANZADO!");
                }
                break;

            case TRAVELING:
                // Interpolar posición del rayo
                float travelProgress = phaseTime / TRAVEL_DURATION;
                beamX = sourceX + (targetX - sourceX) * travelProgress;
                beamY = sourceY + (targetY - sourceY) * travelProgress;
                beamZ = sourceZ + (targetZ - sourceZ) * travelProgress;

                if (phaseTime >= TRAVEL_DURATION) {
                    currentPhase = Phase.IMPACT;
                    phaseTime = 0f;
                    Log.d(TAG, "💥 ¡IMPACTO!");
                }
                break;

            case IMPACT:
                if (phaseTime >= IMPACT_DURATION) {
                    currentPhase = Phase.INACTIVE;
                    phaseTime = 0f;
                    Log.d(TAG, "✓ Plasma Beam completado");
                }
                break;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🎨 DRAW
    // ═══════════════════════════════════════════════════════════════════════
    @Override
    public void draw() {
        if (currentPhase == Phase.INACTIVE || camera == null) return;

        GLES30.glUseProgram(shaderProgram);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE); // Additive blending
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

        switch (currentPhase) {
            case CHARGING:
                drawChargingPhase();
                break;
            case TRAVELING:
                drawTravelingPhase();
                break;
            case IMPACT:
                drawImpactPhase();
                break;
        }

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🔋 FASE 1: CARGA
    // ═══════════════════════════════════════════════════════════════════════
    private void drawChargingPhase() {
        float progress = phaseTime / CHARGE_DURATION;

        // La esfera central crece
        float coreSize = CHARGE_MAX_SIZE * progress;

        // 1. Esfera de energía central (blanco brillante)
        float coreAlpha = 0.7f + 0.3f * progress;
        drawCircleAt(sourceX, sourceY, sourceZ, coreSize, 1f, 1f, 1f, coreAlpha, true);

        // 2. Halo cyan exterior
        float haloSize = coreSize * 1.5f;
        drawCircleAt(sourceX, sourceY, sourceZ, haloSize, 0.2f, 0.8f, 1f, 0.4f * progress, false);

        // 3. Partículas orbitando y acercándose
        drawChargeParticles(progress);

        // 4. Destello sutil al final de la carga (muy pequeño)
        if (progress > 0.85f) {
            float flashProgress = (progress - 0.85f) / 0.15f;
            float flashSize = coreSize * (1f + flashProgress * 0.3f);  // Más pequeño
            float flashAlpha = 0.25f * flashProgress; // Muy sutil
            drawCircleAt(sourceX, sourceY, sourceZ, flashSize, 1f, 1f, 1f, flashAlpha, true);
        }
    }

    private void drawChargeParticles(float progress) {
        // Las partículas empiezan lejos y se acercan al centro
        float approachFactor = 1f - progress * 0.7f; // Se acercan conforme carga

        float[] verts = new float[CHARGE_PARTICLES * 3];
        for (int i = 0; i < CHARGE_PARTICLES; i++) {
            // Actualizar ángulo (giran más rápido al final)
            float speedMultiplier = 1f + progress * 2f;
            chargeAngles[i] += 0.05f * chargeSpeeds[i] * speedMultiplier;

            float radius = chargeRadii[i] * approachFactor;
            float angle = chargeAngles[i];

            verts[i * 3] = (float) Math.cos(angle) * radius;
            verts[i * 3 + 1] = (float) Math.sin(angle) * radius;
            verts[i * 3 + 2] = (float) Math.sin(angle * 0.5f) * radius * 0.3f;
        }

        particleBuffer.clear();
        particleBuffer.put(verts);
        particleBuffer.position(0);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, sourceX, sourceY, sourceZ);

        float[] vpMatrix = camera.getViewProjectionMatrix();
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);

        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        // Partículas cyan brillantes
        setColor(0.4f, 0.9f, 1f, 0.8f);

        GLES30.glEnableVertexAttribArray(positionHandle);
        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 0, particleBuffer);
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, CHARGE_PARTICLES);
        GLES30.glDisableVertexAttribArray(positionHandle);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🚀 FASE 2: VIAJE
    // ═══════════════════════════════════════════════════════════════════════
    private void drawTravelingPhase() {
        float progress = phaseTime / TRAVEL_DURATION;

        // Calcular dirección del rayo
        float dx = targetX - sourceX;
        float dy = targetY - sourceY;
        float dz = targetZ - sourceZ;
        float length = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);

        // Normalizar
        dx /= length;
        dy /= length;
        dz /= length;

        // Posición actual del frente del rayo
        float frontDist = length * progress;
        float frontX = sourceX + dx * frontDist;
        float frontY = sourceY + dy * frontDist;
        float frontZ = sourceZ + dz * frontDist;

        // Cola del rayo (se va desvaneciendo desde el origen)
        float tailProgress = Math.max(0, progress - 0.3f) / 0.7f;
        float tailDist = length * tailProgress;
        float tailX = sourceX + dx * tailDist;
        float tailY = sourceY + dy * tailDist;
        float tailZ = sourceZ + dz * tailDist;

        // 1. Rayo principal (línea gruesa)
        drawBeamLine(tailX, tailY, tailZ, frontX, frontY, frontZ, BEAM_WIDTH,
                     0.3f, 0.9f, 1f, 0.9f);

        // 2. Núcleo brillante (más fino, más blanco)
        drawBeamLine(tailX, tailY, tailZ, frontX, frontY, frontZ, BEAM_WIDTH * 0.4f,
                     1f, 1f, 1f, 1f);

        // 3. Cabeza brillante del rayo
        float headSize = BEAM_WIDTH * 2f;
        drawCircleAt(frontX, frontY, frontZ, headSize, 1f, 1f, 1f, 0.9f, true);
        drawCircleAt(frontX, frontY, frontZ, headSize * 1.5f, 0.2f, 0.8f, 1f, 0.5f, false);

        // 4. Estela de partículas
        drawTrailParticles(tailX, tailY, tailZ, frontX, frontY, frontZ, progress);

        // 5. Pequeño destello en el origen al disparar (solo al inicio, más pequeño)
        if (progress < 0.2f) {
            float flashAlpha = 0.3f * (1f - progress / 0.2f);
            drawCircleAt(sourceX, sourceY, sourceZ, CHARGE_MAX_SIZE * 1.2f,
                        1f, 1f, 1f, flashAlpha, true);
        }
    }

    private void drawBeamLine(float x1, float y1, float z1, float x2, float y2, float z2,
                              float width, float r, float g, float b, float a) {
        // Calcular vector perpendicular para el grosor
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len < 0.001f) return;

        // Perpendicular simple (en Y para visualización)
        float perpX = -dy / len * width;
        float perpY = dx / len * width;

        float[] verts = {
            x1 - perpX, y1 - perpY, z1,
            x1 + perpX, y1 + perpY, z1,
            x2 + perpX, y2 + perpY, z2,
            x2 - perpX, y2 - perpY, z2
        };

        beamBuffer.clear();
        beamBuffer.put(verts);
        beamBuffer.position(0);

        Matrix.setIdentityM(modelMatrix, 0);
        float[] vpMatrix = camera.getViewProjectionMatrix();
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);

        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        setColor(r, g, b, a);

        GLES30.glEnableVertexAttribArray(positionHandle);
        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 0, beamBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 4);
        GLES30.glDisableVertexAttribArray(positionHandle);
    }

    private void drawTrailParticles(float x1, float y1, float z1,
                                    float x2, float y2, float z2, float progress) {
        float[] verts = new float[TRAIL_PARTICLES * 3];

        for (int i = 0; i < TRAIL_PARTICLES; i++) {
            float t = (float) i / (TRAIL_PARTICLES - 1);
            // Agregar un poco de dispersión
            float spread = 0.05f * (1f - t); // Más dispersión atrás
            float offsetX = (float) (Math.random() - 0.5) * spread;
            float offsetY = (float) (Math.random() - 0.5) * spread;

            verts[i * 3] = x1 + (x2 - x1) * t + offsetX;
            verts[i * 3 + 1] = y1 + (y2 - y1) * t + offsetY;
            verts[i * 3 + 2] = z1 + (z2 - z1) * t;
        }

        particleBuffer.clear();
        particleBuffer.put(verts);
        particleBuffer.position(0);

        Matrix.setIdentityM(modelMatrix, 0);
        float[] vpMatrix = camera.getViewProjectionMatrix();
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);

        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        setColor(0.5f, 0.9f, 1f, 0.7f);

        GLES30.glEnableVertexAttribArray(positionHandle);
        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 0, particleBuffer);
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, TRAIL_PARTICLES);
        GLES30.glDisableVertexAttribArray(positionHandle);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 💥 FASE 3: IMPACTO
    // ═══════════════════════════════════════════════════════════════════════
    private void drawImpactPhase() {
        float progress = phaseTime / IMPACT_DURATION;
        float alpha = 1f - progress;

        // 1. Núcleo brillante (se encoge)
        if (progress < 0.4f) {
            float coreProgress = progress / 0.4f;
            float coreSize = CHARGE_MAX_SIZE * (1f - coreProgress * 0.5f);
            drawCircleAt(targetX, targetY, targetZ, coreSize, 1f, 1f, 1f,
                        (1f - coreProgress) * 0.8f, true);
        }

        // 2. Anillo cyan expandiéndose
        float ring1Size = 0.05f + progress * IMPACT_MAX_RADIUS;
        drawCircleAt(targetX, targetY, targetZ, ring1Size, 0.2f, 0.9f, 1f, alpha * 0.8f, false);

        // 3. Segundo anillo magenta (un poco retrasado)
        if (progress > 0.1f) {
            float ring2Progress = (progress - 0.1f) / 0.9f;
            float ring2Size = ring2Progress * IMPACT_MAX_RADIUS * 0.7f;
            drawCircleAt(targetX, targetY, targetZ, ring2Size, 1f, 0.3f, 0.8f,
                        alpha * 0.6f, false);
        }

        // 4. Tercer anillo amarillo
        if (progress > 0.2f) {
            float ring3Progress = (progress - 0.2f) / 0.8f;
            float ring3Size = ring3Progress * IMPACT_MAX_RADIUS * 0.5f;
            drawCircleAt(targetX, targetY, targetZ, ring3Size, 1f, 0.9f, 0.3f,
                        alpha * 0.5f, false);
        }

        // 5. Partículas de impacto
        drawImpactParticles(progress, alpha);
    }

    private void drawImpactParticles(float progress, float alpha) {
        int numParticles = 16;
        float[] verts = new float[numParticles * 3];

        for (int i = 0; i < numParticles; i++) {
            float angle = (float) (i * Math.PI * 2 / numParticles);
            float dist = progress * IMPACT_MAX_RADIUS * (0.8f + (float)Math.random() * 0.4f);

            verts[i * 3] = (float) Math.cos(angle) * dist;
            verts[i * 3 + 1] = (float) Math.sin(angle) * dist;
            verts[i * 3 + 2] = 0;
        }

        particleBuffer.clear();
        particleBuffer.put(verts);
        particleBuffer.position(0);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, targetX, targetY, targetZ);

        float[] vpMatrix = camera.getViewProjectionMatrix();
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);

        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        setColor(1f, 0.9f, 0.4f, alpha * 0.9f);

        GLES30.glEnableVertexAttribArray(positionHandle);
        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 0, particleBuffer);
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, numParticles);
        GLES30.glDisableVertexAttribArray(positionHandle);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🛠️ UTILIDADES
    // ═══════════════════════════════════════════════════════════════════════
    private void drawCircleAt(float x, float y, float z, float radius,
                              float r, float g, float b, float a, boolean filled) {
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        Matrix.scaleM(modelMatrix, 0, radius, radius, radius);

        float[] vpMatrix = camera.getViewProjectionMatrix();
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);

        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        setColor(r, g, b, a);

        circleBuffer.position(0);
        GLES30.glEnableVertexAttribArray(positionHandle);
        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 0, circleBuffer);

        if (filled) {
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, CIRCLE_SEGMENTS + 1);
        } else {
            GLES30.glLineWidth(2.5f);
            GLES30.glDrawArrays(GLES30.GL_LINE_LOOP, 0, CIRCLE_SEGMENTS + 1);
            GLES30.glLineWidth(1f);
        }

        GLES30.glDisableVertexAttribArray(positionHandle);
    }

    private void setColor(float r, float g, float b, float a) {
        colorCache[0] = r;
        colorCache[1] = g;
        colorCache[2] = b;
        colorCache[3] = a;
        GLES30.glUniform4fv(colorHandle, 1, colorCache, 0);
    }

    /**
     * Actualiza la posición de origen (para seguir al interceptor durante la carga)
     */
    public void updateSourcePosition(float x, float y, float z) {
        if (currentPhase == Phase.CHARGING) {
            this.sourceX = x;
            this.sourceY = y;
            this.sourceZ = z;
        }
    }
}
