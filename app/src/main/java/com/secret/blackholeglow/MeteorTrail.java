package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Sistema de estela/trail para meteoritos
 * Inspirado en los efectos de Street Fighter - deja un rastro de fuego/plasma
 */
public class MeteorTrail {
    private static final String TAG = "depurar";

    // Configuración de la estela OPTIMIZADA
    private static final int MAX_TRAIL_POINTS = 8;  // REDUCIDO de 20 a 8 para rendimiento
    private static final float TRAIL_SEGMENT_TIME = 0.04f;  // DUPLICADO para menos puntos

    // Posiciones de la estela
    private List<TrailPoint> trailPoints = new ArrayList<>();
    private float timeSinceLastPoint = 0;

    // Tipo de estela
    public enum TrailType {
        FIRE,     // Naranja/rojo como fuego
        PLASMA,   // Azul/cyan como plasma
        RAINBOW   // Multicolor épico
    }
    private TrailType type;

    // Buffer para dibujar
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private boolean needsUpdate = true;

    // ⚡ OPTIMIZACIÓN: Reutilizar lista para evitar allocations en update()
    private final List<TrailPoint> toRemoveCache = new ArrayList<>();

    // ⚡ OPTIMIZACIÓN: Matrices reutilizables para draw()
    private final float[] modelMatrixCache = new float[16];
    private final float[] mvpMatrixCache = new float[16];

    // Shader program (NO estático para evitar problemas de contexto GL)
    private int programId = -1;
    private int aPositionLoc;
    private int aColorLoc;
    private int aAgeLoc;
    private int uMvpLoc;
    private int uTimeLoc;
    private int uTrailTypeLoc;
    private int uTrailLengthLoc;

    // Referencias
    private Context context;
    private boolean shaderInitialized = false;

    // Clase interna para puntos de la estela
    private class TrailPoint {
        float x, y, z;
        float size;
        float alpha;
        float age;
        float[] color = new float[3];

        TrailPoint(float x, float y, float z, float size) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.size = size;
            this.alpha = 1.0f;
            this.age = 0;

            // Color según el tipo de estela (MEJORADO - verde translúcido)
            switch (type) {
                case FIRE:
                    // Verde translúcido en lugar de naranja
                    color[0] = 0.2f + (float)Math.random() * 0.2f;  // Verde
                    color[1] = 0.8f + (float)Math.random() * 0.2f;  // Verde brillante
                    color[2] = 0.3f;
                    break;
                case PLASMA:
                    // Verde-cyan translúcido
                    color[0] = 0.2f;
                    color[1] = 0.8f + (float)Math.random() * 0.2f;  // Verde-cyan
                    color[2] = 0.5f + (float)Math.random() * 0.3f;
                    break;
                case RAINBOW:
                    // Color arcoíris basado en la edad
                    float hue = age * 3.0f;
                    color[0] = (float)Math.sin(hue) * 0.5f + 0.5f;
                    color[1] = (float)Math.sin(hue + 2.0f) * 0.5f + 0.5f;
                    color[2] = (float)Math.sin(hue + 4.0f) * 0.5f + 0.5f;
                    break;
            }
        }

        void update(float dt) {
            age += dt;
            alpha = Math.max(0, 1.0f - age * 3.0f) * 0.4f;  // Más translúcido (40% alpha max)
            size *= 0.95f;  // Reducir tamaño más rápido (más delgado)

            // Para el arcoíris, actualizar color
            if (type == TrailType.RAINBOW) {
                float hue = age * 6.0f;
                color[0] = (float)Math.sin(hue) * 0.5f + 0.5f;
                color[1] = (float)Math.sin(hue + 2.0f) * 0.5f + 0.5f;
                color[2] = (float)Math.sin(hue + 4.0f) * 0.5f + 0.5f;
            }
        }

        boolean isAlive() {
            return alpha > 0.01f;
        }
    }

    public MeteorTrail(TrailType type) {
        this.type = type;
    }

    // Inicialización del shader (forzar recreación si es necesario)
    private void initShader(Context context) {
        if (context == null) {
            Log.e(TAG, "[MeteorTrail] initShader - context es null!");
            return;
        }

        // SIEMPRE recrear el shader si no está inicializado o programId es inválido
        if (!shaderInitialized || programId <= 0 || !GLES20.glIsProgram(programId)) {
            Log.d(TAG, "[MeteorTrail] Creando/Recreando shader (programId anterior: " + programId + ")");

            programId = ShaderUtils.createProgramFromAssets(context,
                "shaders/trail_vertex.glsl",
                "shaders/trail_fragment.glsl");

            if (programId > 0) {
                aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position");
                aColorLoc = GLES20.glGetAttribLocation(programId, "a_Color");
                aAgeLoc = GLES20.glGetAttribLocation(programId, "a_Age");
                uMvpLoc = GLES20.glGetUniformLocation(programId, "u_MVP");
                uTimeLoc = GLES20.glGetUniformLocation(programId, "u_Time");
                uTrailTypeLoc = GLES20.glGetUniformLocation(programId, "u_TrailType");
                uTrailLengthLoc = GLES20.glGetUniformLocation(programId, "u_TrailLength");

                shaderInitialized = true;

                Log.d(TAG, "[MeteorTrail] ✓ Shader inicializado - programId:" + programId);
                Log.d(TAG, "[MeteorTrail] Shader locations - Pos:" + aPositionLoc + " Color:" + aColorLoc +
                           " Age:" + aAgeLoc + " MVP:" + uMvpLoc);
            } else {
                Log.e(TAG, "[MeteorTrail] ✗ ERROR: Shader NO se pudo crear!");
                shaderInitialized = false;
            }
        }
    }

    public void setContext(Context context) {
        this.context = context;
        initShader(context);
    }

    /**
     * Invalida el shader para forzar recreación (útil cuando se pierde contexto GL)
     */
    public void invalidateShader() {
        Log.d(TAG, "[MeteorTrail] invalidateShader() llamado");
        shaderInitialized = false;
        programId = -1;
    }

    /**
     * Añade un nuevo punto a la estela
     */
    public void addPoint(float x, float y, float z, float size) {
        // Limitar el número de puntos
        while (trailPoints.size() >= MAX_TRAIL_POINTS) {
            trailPoints.remove(0);
        }

        trailPoints.add(new TrailPoint(x, y, z, size));
        needsUpdate = true;
    }

    /**
     * Actualiza la estela
     */
    public void update(float deltaTime, float meteorX, float meteorY, float meteorZ, float meteorSize, boolean isActive) {
        timeSinceLastPoint += deltaTime;

        // Añadir nuevo punto si el meteorito está activo
        if (isActive && timeSinceLastPoint > TRAIL_SEGMENT_TIME) {
            addPoint(meteorX, meteorY, meteorZ, meteorSize * 0.8f);
            timeSinceLastPoint = 0;
        }

        // Actualizar todos los puntos (⚡ OPTIMIZADO: reutilizar lista)
        toRemoveCache.clear();
        for (TrailPoint point : trailPoints) {
            point.update(deltaTime);
            if (!point.isAlive()) {
                toRemoveCache.add(point);
            }
        }
        trailPoints.removeAll(toRemoveCache);

        if (!toRemoveCache.isEmpty()) {
            needsUpdate = true;
        }
    }

    // Buffer adicional para ages
    private FloatBuffer ageBuffer;

    /**
     * Construye los buffers para dibujar
     */
    private void buildBuffers() {
        if (trailPoints.isEmpty()) {
            vertexBuffer = null;
            colorBuffer = null;
            ageBuffer = null;
            return;
        }

        // Crear triángulos conectando los puntos (como un ribbon)
        int numPoints = trailPoints.size();
        float[] vertices = new float[(numPoints - 1) * 6 * 3];  // 2 triángulos por segmento
        float[] colors = new float[(numPoints - 1) * 6 * 4];    // RGBA
        float[] ages = new float[(numPoints - 1) * 6];      // Age por vértice

        int vIdx = 0;
        int cIdx = 0;
        int aIdx = 0;

        for (int i = 0; i < numPoints - 1; i++) {
            TrailPoint p1 = trailPoints.get(i);
            TrailPoint p2 = trailPoints.get(i + 1);

            // Calcular perpendicular para el ancho
            float dx = p2.x - p1.x;
            float dy = p2.y - p1.y;
            float dz = p2.z - p1.z;
            float len = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (len > 0) {
                dx /= len;
                dy /= len;
                dz /= len;
            }

            // Vector perpendicular (simplificado)
            float px = -dy * p1.size;
            float py = dx * p1.size;
            float pz = 0;

            // Primer triángulo
            vertices[vIdx++] = p1.x - px;
            vertices[vIdx++] = p1.y - py;
            vertices[vIdx++] = p1.z - pz;

            vertices[vIdx++] = p1.x + px;
            vertices[vIdx++] = p1.y + py;
            vertices[vIdx++] = p1.z + pz;

            vertices[vIdx++] = p2.x - px * p2.size / p1.size;
            vertices[vIdx++] = p2.y - py * p2.size / p1.size;
            vertices[vIdx++] = p2.z - pz;

            // Segundo triángulo
            vertices[vIdx++] = p1.x + px;
            vertices[vIdx++] = p1.y + py;
            vertices[vIdx++] = p1.z + pz;

            vertices[vIdx++] = p2.x + px * p2.size / p1.size;
            vertices[vIdx++] = p2.y + py * p2.size / p1.size;
            vertices[vIdx++] = p2.z + pz;

            vertices[vIdx++] = p2.x - px * p2.size / p1.size;
            vertices[vIdx++] = p2.y - py * p2.size / p1.size;
            vertices[vIdx++] = p2.z - pz;

            // Colores y ages para los 6 vértices
            for (int j = 0; j < 3; j++) {
                colors[cIdx++] = p1.color[0];
                colors[cIdx++] = p1.color[1];
                colors[cIdx++] = p1.color[2];
                colors[cIdx++] = p1.alpha;
                ages[aIdx++] = p1.age / 0.5f;  // Normalizar age (0-1)
            }
            for (int j = 0; j < 3; j++) {
                colors[cIdx++] = p2.color[0];
                colors[cIdx++] = p2.color[1];
                colors[cIdx++] = p2.color[2];
                colors[cIdx++] = p2.alpha;
                ages[aIdx++] = p2.age / 0.5f;  // Normalizar age (0-1)
            }
        }

        // Crear buffers
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
        cbb.order(ByteOrder.nativeOrder());
        colorBuffer = cbb.asFloatBuffer();
        colorBuffer.put(colors);
        colorBuffer.position(0);

        ByteBuffer abb = ByteBuffer.allocateDirect(ages.length * 4);
        abb.order(ByteOrder.nativeOrder());
        ageBuffer = abb.asFloatBuffer();
        ageBuffer.put(ages);
        ageBuffer.position(0);

        needsUpdate = false;
    }

    // Contador para logs periódicos
    private static int drawCallCount = 0;

    /**
     * Dibuja la estela con shaders animados
     */
    public void draw(CameraController camera) {
        drawCallCount++;

        if (trailPoints.size() < 2) {
            if (drawCallCount % 300 == 0) {
                Log.d(TAG, "[MeteorTrail] draw() - No hay suficientes puntos: " + trailPoints.size());
            }
            return;
        }

        if (camera == null) {
            Log.w(TAG, "[MeteorTrail] draw() - camera es null!");
            return;
        }

        // Verificar si el shader es válido (puede perder contexto en wallpaper)
        if (context != null && (!GLES20.glIsProgram(programId) || !shaderInitialized)) {
            Log.w(TAG, "[MeteorTrail] Shader inválido, recreando... (programId: " + programId + ")");
            shaderInitialized = false;  // Forzar recreación
            initShader(context);
        }

        if (programId <= 0 || !shaderInitialized) {
            if (drawCallCount % 60 == 0) {  // Log cada segundo
                Log.e(TAG, "[MeteorTrail] draw() - Shader no disponible! programId:" + programId +
                           " initialized:" + shaderInitialized);
            }
            return;
        }

        // Log periódico cada 5 segundos
        if (drawCallCount % 300 == 0) {
            Log.d(TAG, "[MeteorTrail] Dibujando estela con " + trailPoints.size() + " puntos, programId=" + programId);
        }

        if (needsUpdate) {
            buildBuffers();
        }

        if (vertexBuffer == null) return;

        GLES20.glUseProgram(programId);

        // Configurar blending para efecto translúcido (NO aditivo)
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);  // Translúcido

        // Desactivar depth write para transparencia
        GLES20.glDepthMask(false);

        // Calcular MVP (⚡ OPTIMIZADO: usar matrices cacheadas)
        Matrix.setIdentityM(modelMatrixCache, 0);
        camera.computeMvp(modelMatrixCache, mvpMatrixCache);
        GLES20.glUniformMatrix4fv(uMvpLoc, 1, false, mvpMatrixCache, 0);

        // Configurar uniforms para efectos animados
        // ⚡ OPTIMIZACIÓN: Usar TimeManager
        float time = TimeManager.getTime() % 10.0f;
        GLES20.glUniform1f(uTimeLoc, time);

        // Tipo de estela (0 = fuego, 0.5 = plasma, 1 = arcoíris)
        float trailTypeValue = 0.0f;
        if (type == TrailType.PLASMA) trailTypeValue = 0.5f;
        else if (type == TrailType.RAINBOW) trailTypeValue = 1.0f;
        GLES20.glUniform1f(uTrailTypeLoc, trailTypeValue);

        GLES20.glUniform1f(uTrailLengthLoc, (float)trailPoints.size());

        // Configurar atributos
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aColorLoc);
        GLES20.glVertexAttribPointer(aColorLoc, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);

        // Configurar atributo age si existe
        if (aAgeLoc >= 0 && ageBuffer != null) {
            GLES20.glEnableVertexAttribArray(aAgeLoc);
            GLES20.glVertexAttribPointer(aAgeLoc, 1, GLES20.GL_FLOAT, false, 0, ageBuffer);
        }

        // Dibujar
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, (trailPoints.size() - 1) * 6);

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aColorLoc);
        if (aAgeLoc >= 0) {
            GLES20.glDisableVertexAttribArray(aAgeLoc);
        }

        // Restaurar estados
        GLES20.glDepthMask(true);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * Limpia la estela completamente
     */
    public void clear() {
        trailPoints.clear();
        needsUpdate = true;
    }
}