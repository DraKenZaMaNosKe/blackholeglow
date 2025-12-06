package com.secret.blackholeglow;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   🔫 Laser - Proyectil de energía para batalla espacial                  ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  CARACTERÍSTICAS:                                                         ║
 * ║  • Renderizado con efecto glow brillante                                 ║
 * ║  • Dos equipos: HUMAN (azul) y ALIEN (verde)                             ║
 * ║  • Viaja en línea recta hacia el objetivo                                ║
 * ║  • Se desactiva al impactar o salir de la escena                         ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class Laser {
    private static final String TAG = "Laser";

    // Equipos
    public static final int TEAM_HUMAN = 0;  // Azul
    public static final int TEAM_ALIEN = 1;  // Verde

    // Posición y movimiento
    public float x, y, z;
    public float vx, vy, vz;  // Velocidad
    private float speed = 8.0f;  // Unidades por segundo

    // Propiedades
    private int team;
    private boolean active = false;
    private float lifetime = 0f;
    private static final float MAX_LIFETIME = 3.0f;  // 3 segundos máximo

    // Dimensiones del láser
    private static final float LENGTH = 0.5f;   // Largo del rayo
    private static final float WIDTH = 0.08f;   // Ancho del rayo

    // ═══════════════════════════════════════════════════════════════════════
    // 💥 SISTEMA DE EFECTOS DE IMPACTO
    // ═══════════════════════════════════════════════════════════════════════
    private boolean impactActive = false;
    private float impactX, impactY, impactZ;
    private float impactTimer = 0f;
    private static final float IMPACT_DURATION = 0.4f;
    private static final int IMPACT_PARTICLES = 12;
    private float[] impactParticleX = new float[IMPACT_PARTICLES];
    private float[] impactParticleY = new float[IMPACT_PARTICLES];
    private float[] impactParticleZ = new float[IMPACT_PARTICLES];
    private float[] impactParticleVX = new float[IMPACT_PARTICLES];
    private float[] impactParticleVY = new float[IMPACT_PARTICLES];
    private float[] impactParticleVZ = new float[IMPACT_PARTICLES];
    private float[] impactParticleLife = new float[IMPACT_PARTICLES];

    // Shader compartido (estático para eficiencia)
    private static int shaderProgram = 0;
    private static int aPositionLoc;
    private static int uMVPLoc;
    private static int uColorLoc;
    private static int uTimeLoc;
    private static FloatBuffer vertexBuffer;

    // Colores por equipo
    private static final float[] COLOR_HUMAN = {0.3f, 0.6f, 1.0f, 1.0f};  // Azul brillante
    private static final float[] COLOR_ALIEN = {0.2f, 1.0f, 0.4f, 1.0f};  // Verde brillante

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // Referencia a cámara
    private CameraController camera;

    // Tiempo para animación
    private static long startTime = System.currentTimeMillis();

    public Laser(int team) {
        this.team = team;

        // Inicializar shader si no existe
        if (shaderProgram == 0) {
            initShader();
        }
    }

    /**
     * Inicializa el shader compartido para todos los láseres
     */
    private static void initShader() {
        Log.d(TAG, "🔫 Inicializando shader de láser...");

        // Vertex shader simple
        String vertexShaderCode =
            "attribute vec4 a_Position;\n" +
            "uniform mat4 u_MVP;\n" +
            "void main() {\n" +
            "    gl_Position = u_MVP * a_Position;\n" +
            "}\n";

        // Fragment shader con efecto glow
        String fragmentShaderCode =
            "precision mediump float;\n" +
            "uniform vec4 u_Color;\n" +
            "uniform float u_Time;\n" +
            "void main() {\n" +
            "    // Pulso de brillo\n" +
            "    float pulse = 0.8 + sin(u_Time * 20.0) * 0.2;\n" +
            "    vec3 glowColor = u_Color.rgb * pulse * 1.5;\n" +
            "    gl_FragColor = vec4(glowColor, u_Color.a);\n" +
            "}\n";

        // Compilar shaders
        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        if (vertexShader == 0 || fragmentShader == 0) {
            Log.e(TAG, "❌ Error compilando shaders de láser");
            return;
        }

        // Crear programa
        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShader);
        GLES20.glAttachShader(shaderProgram, fragmentShader);
        GLES20.glLinkProgram(shaderProgram);

        // Obtener ubicaciones
        aPositionLoc = GLES20.glGetAttribLocation(shaderProgram, "a_Position");
        uMVPLoc = GLES20.glGetUniformLocation(shaderProgram, "u_MVP");
        uColorLoc = GLES20.glGetUniformLocation(shaderProgram, "u_Color");
        uTimeLoc = GLES20.glGetUniformLocation(shaderProgram, "u_Time");

        // Crear buffer de vértices (rectángulo para el láser)
        // El láser apunta en dirección Z positiva por defecto
        float[] vertices = {
            // Cara frontal (rectángulo alargado)
            -WIDTH/2, -WIDTH/2, 0,
             WIDTH/2, -WIDTH/2, 0,
             WIDTH/2,  WIDTH/2, 0,
            -WIDTH/2,  WIDTH/2, 0,
            // Cara trasera
            -WIDTH/2, -WIDTH/2, LENGTH,
             WIDTH/2, -WIDTH/2, LENGTH,
             WIDTH/2,  WIDTH/2, LENGTH,
            -WIDTH/2,  WIDTH/2, LENGTH,
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        Log.d(TAG, "✅ Shader de láser inicializado (program=" + shaderProgram + ")");
    }

    private static int compileShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Error compilando shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    /**
     * Dispara el láser desde una posición hacia un objetivo
     */
    public void fire(float startX, float startY, float startZ,
                     float targetX, float targetY, float targetZ) {
        this.x = startX;
        this.y = startY;
        this.z = startZ;

        // Calcular dirección normalizada
        float dx = targetX - startX;
        float dy = targetY - startY;
        float dz = targetZ - startZ;
        float dist = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);

        if (dist > 0.01f) {
            this.vx = (dx / dist) * speed;
            this.vy = (dy / dist) * speed;
            this.vz = (dz / dist) * speed;
        } else {
            // Dirección por defecto si están muy cerca
            this.vx = 0;
            this.vy = 0;
            this.vz = speed;
        }

        this.active = true;
        this.lifetime = 0f;

        Log.d(TAG, "🔫 Láser " + (team == TEAM_HUMAN ? "AZUL" : "VERDE") +
                   " disparado desde (" + String.format("%.1f,%.1f,%.1f", x, y, z) + ")");
    }

    /**
     * Actualiza la posición del láser
     */
    public void update(float deltaTime) {
        // Siempre actualizar efecto de impacto
        if (impactActive) {
            updateImpact(deltaTime);
        }

        if (!active) return;

        // Mover
        x += vx * deltaTime;
        y += vy * deltaTime;
        z += vz * deltaTime;

        // Incrementar tiempo de vida
        lifetime += deltaTime;

        // Desactivar si excede tiempo de vida
        if (lifetime > MAX_LIFETIME) {
            active = false;
            return;
        }

        // Desactivar si sale de la escena
        if (Math.abs(x) > 10 || Math.abs(y) > 10 || Math.abs(z) > 10) {
            active = false;
        }
    }

    /**
     * 💥 Actualiza el efecto de impacto
     */
    private void updateImpact(float deltaTime) {
        impactTimer += deltaTime;

        for (int i = 0; i < IMPACT_PARTICLES; i++) {
            // Mover partículas
            impactParticleX[i] += impactParticleVX[i] * deltaTime;
            impactParticleY[i] += impactParticleVY[i] * deltaTime;
            impactParticleZ[i] += impactParticleVZ[i] * deltaTime;

            // Desacelerar
            impactParticleVX[i] *= 0.92f;
            impactParticleVY[i] *= 0.92f;
            impactParticleVZ[i] *= 0.92f;

            // Reducir vida
            impactParticleLife[i] -= deltaTime / IMPACT_DURATION;
            if (impactParticleLife[i] < 0) impactParticleLife[i] = 0;
        }

        if (impactTimer >= IMPACT_DURATION) {
            impactActive = false;
        }
    }

    /**
     * 💥 Inicia el efecto de impacto
     */
    private void startImpact() {
        impactActive = true;
        impactTimer = 0f;
        impactX = x;
        impactY = y;
        impactZ = z;

        // Crear partículas de explosión
        for (int i = 0; i < IMPACT_PARTICLES; i++) {
            impactParticleX[i] = impactX;
            impactParticleY[i] = impactY;
            impactParticleZ[i] = impactZ;

            // Velocidad aleatoria esférica
            float theta = (float)(Math.random() * Math.PI * 2);
            float phi = (float)(Math.random() * Math.PI);
            float speed = 2.0f + (float)(Math.random() * 3.0f);

            impactParticleVX[i] = speed * (float)(Math.sin(phi) * Math.cos(theta));
            impactParticleVY[i] = speed * (float)(Math.sin(phi) * Math.sin(theta));
            impactParticleVZ[i] = speed * (float)(Math.cos(phi));

            impactParticleLife[i] = 1.0f;
        }

        Log.d(TAG, "💥 Efecto de impacto iniciado!");
    }

    /**
     * Dibuja el láser y sus efectos
     */
    public void draw() {
        // Siempre dibujar impacto aunque el láser esté inactivo
        if (impactActive && camera != null) {
            drawImpact();
        }

        if (!active || shaderProgram == 0 || camera == null) return;

        GLES20.glUseProgram(shaderProgram);

        // Configurar blending aditivo para efecto glow
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);  // Siempre visible

        // Calcular rotación para que apunte en dirección de movimiento
        float[] modelMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);

        // Rotar para apuntar en dirección de velocidad
        float speed = (float) Math.sqrt(vx*vx + vy*vy + vz*vz);
        if (speed > 0.01f) {
            // Calcular ángulos de rotación
            float yaw = (float) Math.toDegrees(Math.atan2(vx, vz));
            float pitch = (float) Math.toDegrees(Math.asin(-vy / speed));

            Matrix.rotateM(modelMatrix, 0, yaw, 0, 1, 0);
            Matrix.rotateM(modelMatrix, 0, pitch, 1, 0, 0);
        }

        // Escalar el láser (más largo en Z)
        Matrix.scaleM(modelMatrix, 0, 1.0f, 1.0f, 1.0f);

        // Calcular MVP
        camera.computeMvp(modelMatrix, mvpMatrix);

        // Uniforms
        GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, mvpMatrix, 0);

        float[] color = (team == TEAM_HUMAN) ? COLOR_HUMAN : COLOR_ALIEN;
        GLES20.glUniform4fv(uColorLoc, 1, color, 0);

        float time = ((System.currentTimeMillis() - startTime) / 1000.0f) % 60.0f;
        GLES20.glUniform1f(uTimeLoc, time);

        // Dibujar como línea gruesa (GL_LINES con múltiples pasadas para grosor)
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        // Dibujar las caras del "tubo" de láser
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);  // Frente
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 4, 4);  // Atrás

        GLES20.glDisableVertexAttribArray(aPositionLoc);

        // 💥 DIBUJAR EFECTO DE IMPACTO
        drawImpact();

        // Restaurar estado
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * 💥 Dibuja las partículas del impacto
     */
    private void drawImpact() {
        if (!impactActive || camera == null) return;

        GLES20.glUseProgram(shaderProgram);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        float[] color = (team == TEAM_HUMAN) ? COLOR_HUMAN : COLOR_ALIEN;

        // Dibujar cada partícula como un pequeño quad
        for (int i = 0; i < IMPACT_PARTICLES; i++) {
            if (impactParticleLife[i] <= 0) continue;

            float[] particleModel = new float[16];
            Matrix.setIdentityM(particleModel, 0);
            Matrix.translateM(particleModel, 0, impactParticleX[i], impactParticleY[i], impactParticleZ[i]);

            // Escala basada en vida (se hace más pequeño)
            float particleScale = impactParticleLife[i] * 0.08f;
            Matrix.scaleM(particleModel, 0, particleScale, particleScale, particleScale);

            float[] particleMvp = new float[16];
            camera.computeMvp(particleModel, particleMvp);

            GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, particleMvp, 0);

            // Color con alpha basado en vida
            float[] particleColor = {
                color[0] * 1.5f,
                color[1] * 1.5f,
                color[2] * 1.5f,
                impactParticleLife[i]
            };
            GLES20.glUniform4fv(uColorLoc, 1, particleColor, 0);

            float time = ((System.currentTimeMillis() - startTime) / 1000.0f) % 60.0f;
            GLES20.glUniform1f(uTimeLoc, time);

            // Dibujar partícula
            vertexBuffer.position(0);
            GLES20.glEnableVertexAttribArray(aPositionLoc);
            GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
            GLES20.glDisableVertexAttribArray(aPositionLoc);
        }
    }

    /**
     * Verifica colisión con un punto (esfera)
     */
    public boolean checkCollision(float targetX, float targetY, float targetZ, float radius) {
        if (!active) return false;

        float dx = x - targetX;
        float dy = y - targetY;
        float dz = z - targetZ;
        float dist = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);

        // Radio de colisión del láser + radio del objetivo
        float collisionRadius = 0.15f + radius;

        return dist < collisionRadius;
    }

    /**
     * Desactiva el láser (al impactar) y activa efecto de impacto
     */
    public void deactivate() {
        if (active) {
            startImpact();  // Crear efecto visual antes de desactivar
        }
        active = false;
    }

    // Getters
    public boolean isActive() { return active; }
    public boolean hasActiveEffect() { return active || impactActive; }  // Para saber si tiene efecto visual
    public int getTeam() { return team; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }

    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }
}
