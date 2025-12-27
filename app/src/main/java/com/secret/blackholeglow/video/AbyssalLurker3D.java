package com.secret.blackholeglow.video;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.util.ObjLoader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * ╔════════════════════════════════════════════════════════════════════════════╗
 * ║              AbyssalLurker3D - Pez Alienígena 3D (Meshy AI)                ║
 * ╠════════════════════════════════════════════════════════════════════════════╣
 * ║  Modelo 3D del Abyssal Lurker con 5,327 triángulos.                        ║
 * ║  Usa proyección ortográfica para integrarse al video 2D.                   ║
 * ║  Animación de nado con profundidad simulada.                               ║
 * ║  Estética Zerg: espinas, ojos cyan, cuerpo biomecánico.                    ║
 * ╚════════════════════════════════════════════════════════════════════════════╝
 */
public class AbyssalLurker3D {
    private static final String TAG = "AbyssalLurker3D";
    private static final String OBJ_FILE = "abyssal_lurker.obj";

    private Context context;

    // ═══════════════════════════════════════════════════════════════════════════
    // BUFFERS DEL MODELO
    // ═══════════════════════════════════════════════════════════════════════════
    private FloatBuffer vertexBuffer;
    private FloatBuffer uvBuffer;
    private IntBuffer indexBuffer;
    private int indexCount;

    // ═══════════════════════════════════════════════════════════════════════════
    // ESTADO DEL PEZ
    // ═══════════════════════════════════════════════════════════════════════════
    private float posX = -1.0f;
    private float posY = 0.0f;
    private float scale = 0.25f;
    private float time = 0f;
    private boolean movingRight = true;   // Mirando a la derecha
    private float rotationY = 90f;   // Mirando a la derecha (90°)
    private float rotationX = 0f;    // Inclinación arriba/abajo

    // ═══════════════════════════════════════════════════════════════════════════
    // 👆 SISTEMA DE TOUCH - Nada hacia donde toques
    // ═══════════════════════════════════════════════════════════════════════════
    private boolean hasTarget = false;
    private float targetX = 0f;
    private float targetY = 0f;
    private static final float SWIM_SPEED = 0.3f;         // Velocidad de nado
    private static final float TURN_SPEED = 2.5f;         // Velocidad de giro
    private static final float ARRIVAL_DISTANCE = 0.1f;   // Distancia para "llegar"

    // ═══════════════════════════════════════════════════════════════════════════
    // 🐟 DIRECCIÓN DE NADO LIBRE (después del touch)
    // ═══════════════════════════════════════════════════════════════════════════
    private float swimDirX = 1f;          // Dirección actual X (-1 a 1)
    private float swimDirY = 0f;          // Dirección actual Y (-1 a 1)
    private float dirChangeTimer = 0f;    // Timer para cambiar dirección
    private float nextDirChange = 3f;     // Segundos hasta próximo cambio

    // ═══════════════════════════════════════════════════════════════════════════
    // ESCALA (basada en posición Y - arriba=lejos, abajo=cerca)
    // ═══════════════════════════════════════════════════════════════════════════
    private static final float SCALE_CLOSE = 0.28f;   // Abajo = grande
    private static final float SCALE_FAR = 0.15f;     // Arriba = pequeño
    private static final float Y_MIN = -0.6f;         // Límite inferior
    private static final float Y_MAX = 0.5f;          // Límite superior

    // ═══════════════════════════════════════════════════════════════════════════
    // OPENGL
    // ═══════════════════════════════════════════════════════════════════════════
    private int shaderProgram;
    private int aPosLoc, aTexLoc;
    private int uMVPLoc, uTextureLoc, uTimeLoc;
    private int textureId = -1;
    private boolean initialized = false;
    private boolean modelLoaded = false;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] projMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private final float[] tempMatrix = new float[16];

    // ═══════════════════════════════════════════════════════════════════════════
    // SHADERS - Con animación de cola y aletas
    // ═══════════════════════════════════════════════════════════════════════════
    private static final String VERTEX_SHADER =
        "precision mediump float;\n" +
        "attribute vec3 a_Position;\n" +
        "attribute vec2 a_TexCoord;\n" +
        "uniform mat4 u_MVP;\n" +
        "uniform float u_Time;\n" +
        "varying vec2 vUV;\n" +
        "void main() {\n" +
        "    vec3 p = a_Position;\n" +
        "    // Aletas de nadar (X negativo)\n" +
        "    float tf = max(0.0, -p.x);\n" +
        "    float swimWave = tf * tf * 0.35;\n" +
        "    float w1 = sin(u_Time * 4.0 - p.x * 4.0) * swimWave;\n" +
        "    // Cola trasera (Z < -0.5 = la cola real)\n" +
        "    float tb = max(0.0, -p.z - 0.5);\n" +
        "    float tailFlap = tb * tb * 1.2;\n" +
        "    float w2 = sin(u_Time * 5.0 + p.z * 3.0) * tailFlap;\n" +
        "    // Boquita (Z > 0.4 = cabeza, Y < 0 = mandibula)\n" +
        "    float isHead = max(0.0, p.z - 0.4);\n" +
        "    float isJaw = max(0.0, -p.y);\n" +
        "    float jawMove = isHead * isJaw * 0.5;\n" +
        "    float w3 = sin(u_Time * 2.5) * 0.5 + 0.5;\n" +
        "    p.y += w1 + w2 - jawMove * w3;\n" +
        "    p.z += w1 * 0.8;\n" +
        "    gl_Position = u_MVP * vec4(p, 1.0);\n" +
        "    vUV = a_TexCoord;\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "uniform sampler2D u_Texture;\n" +
        "uniform float u_Time;\n" +
        "varying vec2 vUV;\n" +
        "void main() {\n" +
        "    vec4 tex = texture2D(u_Texture, vUV);\n" +
        "    // Pulso bioluminiscente en zonas cyan\n" +
        "    float isCyan = step(0.4, tex.b) * step(tex.r, 0.6);\n" +
        "    float pulse = sin(u_Time * 3.0) * 0.25 + 0.25;\n" +
        "    tex.rgb += vec3(0.1, 0.3, 0.5) * pulse * isCyan;\n" +
        "    gl_FragColor = tex;\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════
    public AbyssalLurker3D(Context context) {
        this.context = context;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INICIALIZACIÓN
    // ═══════════════════════════════════════════════════════════════════════════
    public void initialize() {
        // Compilar shaders
        int vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vs);
        GLES20.glAttachShader(shaderProgram, fs);
        GLES20.glLinkProgram(shaderProgram);

        int[] linked = new int[1];
        GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            Log.e(TAG, "Link error: " + GLES20.glGetProgramInfoLog(shaderProgram));
        }

        aPosLoc = GLES20.glGetAttribLocation(shaderProgram, "a_Position");
        aTexLoc = GLES20.glGetAttribLocation(shaderProgram, "a_TexCoord");
        uMVPLoc = GLES20.glGetUniformLocation(shaderProgram, "u_MVP");
        uTextureLoc = GLES20.glGetUniformLocation(shaderProgram, "u_Texture");
        uTimeLoc = GLES20.glGetUniformLocation(shaderProgram, "u_Time");

        loadModel();
        loadTexture();

        // Matrices fijas (calculadas una sola vez)
        Matrix.setIdentityM(viewMatrix, 0);
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f);
        Matrix.orthoM(projMatrix, 0, -1f, 1f, -16f/9f, 16f/9f, 0.1f, 100f);

        initialized = true;
    }

    private void loadModel() {
        try {
            ObjLoader.Mesh mesh = ObjLoader.loadObj(context, OBJ_FILE, true);
            this.vertexBuffer = mesh.vertexBuffer;
            this.uvBuffer = mesh.uvBuffer;

            // Construir índices (triangular fan)
            int totalIndices = 0;
            for (int[] face : mesh.faces) {
                totalIndices += (face.length - 2) * 3;
            }

            int[] indices = new int[totalIndices];
            int idx = 0;
            for (int[] face : mesh.faces) {
                int v0 = face[0];
                for (int i = 1; i < face.length - 1; i++) {
                    indices[idx++] = v0;
                    indices[idx++] = face[i];
                    indices[idx++] = face[i + 1];
                }
            }
            this.indexCount = totalIndices;

            ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 4);
            ibb.order(ByteOrder.nativeOrder());
            indexBuffer = ibb.asIntBuffer();
            indexBuffer.put(indices);
            indexBuffer.position(0);
            modelLoaded = true;

        } catch (IOException e) {
            Log.e(TAG, "Error modelo: " + e.getMessage());
            modelLoaded = false;
        }
    }

    private void loadTexture() {
        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        textureId = tex[0];

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),
            R.drawable.abyssal_lurker_texture, opts);

        if (bitmap != null) {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 👆 SETTER PARA TOUCH TARGET
    // ═══════════════════════════════════════════════════════════════════════════
    public void setTargetPosition(float x, float y) {
        this.targetX = x;
        this.targetY = y;
        this.hasTarget = true;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE - Nadar hacia touch o libremente
    // ═══════════════════════════════════════════════════════════════════════════

    public void update(float deltaTime) {
        time += deltaTime;
        if (time > 62.83f) time -= 62.83f;

        if (hasTarget) {
            // ════════════════════════════════════════════════════════════════════
            // 👆 NADAR HACIA DONDE TOCÓ EL USUARIO
            // ════════════════════════════════════════════════════════════════════
            float dx = targetX - posX;
            float dy = targetY - posY;
            float dist = (float)Math.sqrt(dx * dx + dy * dy);

            if (dist < ARRIVAL_DISTANCE) {
                // Llegó → elegir dirección aleatoria y seguir nadando
                hasTarget = false;
                pickRandomDirection();
            } else {
                // Nadar hacia el target
                float dirX = dx / dist;
                float dirY = dy / dist;

                posX += dirX * SWIM_SPEED * deltaTime;
                posY += dirY * SWIM_SPEED * deltaTime;

                // Girar hacia el target (Y = izquierda/derecha)
                movingRight = dx > 0;
                float targetRotY = movingRight ? 90f : -90f;
                rotationY += (targetRotY - rotationY) * TURN_SPEED * deltaTime;

                // Inclinar hacia arriba/abajo (X = pitch)
                // arriba (dirY > 0) = muestra espalda (+), abajo (dirY < 0) = muestra frente (-)
                float targetRotX = -dirY * 35f;  // Max 35 grados
                rotationX += (targetRotX - rotationX) * TURN_SPEED * deltaTime;
            }

        } else {
            // ════════════════════════════════════════════════════════════════════
            // 🐟 NADAR LIBREMENTE EN LA DIRECCIÓN ACTUAL
            // ════════════════════════════════════════════════════════════════════

            // Mover en la dirección actual (mismo speed que hacia el touch)
            posX += swimDirX * SWIM_SPEED * deltaTime;
            posY += swimDirY * SWIM_SPEED * deltaTime;

            // Girar hacia la dirección de nado (Y = izquierda/derecha)
            movingRight = swimDirX > 0;
            float targetRotY = movingRight ? 90f : -90f;
            rotationY += (targetRotY - rotationY) * TURN_SPEED * deltaTime;

            // Inclinar hacia arriba/abajo (X = pitch)
            float targetRotX = -swimDirY * 35f;  // Max 35 grados
            rotationX += (targetRotX - rotationX) * TURN_SPEED * deltaTime;

            // Cambiar dirección gradualmente cada cierto tiempo
            dirChangeTimer += deltaTime;
            if (dirChangeTimer > nextDirChange) {
                dirChangeTimer = 0f;
                nextDirChange = 2f + (float)(Math.random() * 4f);

                // Ajustar dirección ligeramente (no cambio brusco)
                swimDirX += ((float)Math.random() - 0.5f) * 0.5f;
                swimDirY += ((float)Math.random() - 0.5f) * 0.3f;
                normalizeDirection();
            }

            // Rebotar en los bordes suavemente
            if (posX > 1.3f) {
                swimDirX = -Math.abs(swimDirX);  // Ir hacia la izquierda
            } else if (posX < -1.3f) {
                swimDirX = Math.abs(swimDirX);   // Ir hacia la derecha
            }

            if (posY > Y_MAX) {
                swimDirY = -Math.abs(swimDirY);  // Ir hacia abajo
            } else if (posY < Y_MIN) {
                swimDirY = Math.abs(swimDirY);   // Ir hacia arriba
            }
        }

        // Limitar posición
        posX = Math.max(-1.5f, Math.min(1.5f, posX));
        posY = Math.max(Y_MIN - 0.1f, Math.min(Y_MAX + 0.1f, posY));

        // Escala basada en Y (arriba=lejos/pequeño, abajo=cerca/grande)
        float t = (posY - Y_MIN) / (Y_MAX - Y_MIN);  // 0=abajo, 1=arriba
        scale = SCALE_CLOSE + (SCALE_FAR - SCALE_CLOSE) * t;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎲 ELEGIR DIRECCIÓN ALEATORIA
    // ═══════════════════════════════════════════════════════════════════════════
    private void pickRandomDirection() {
        // Ángulo aleatorio entre -60° y 60° (no ir directo arriba/abajo)
        float angle = (float)((Math.random() - 0.5) * Math.PI * 0.6);

        // Si estaba yendo a la derecha, continuar mayormente a la derecha (o viceversa)
        if (Math.random() > 0.3) {
            // 70% probabilidad de mantener dirección general
            swimDirX = movingRight ? (float)Math.cos(angle) : -(float)Math.cos(angle);
        } else {
            // 30% probabilidad de cambiar dirección
            swimDirX = movingRight ? -(float)Math.cos(angle) : (float)Math.cos(angle);
        }
        swimDirY = (float)Math.sin(angle);

        normalizeDirection();
    }

    private void normalizeDirection() {
        float len = (float)Math.sqrt(swimDirX * swimDirX + swimDirY * swimDirY);
        if (len > 0.01f) {
            swimDirX /= len;
            swimDirY /= len;
        } else {
            swimDirX = 1f;
            swimDirY = 0f;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DRAW
    // ═══════════════════════════════════════════════════════════════════════════
    public void draw() {
        if (!initialized || !modelLoaded) return;

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glUseProgram(shaderProgram);

        // Matriz de modelo (posición, rotación, escala)
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, posX, posY, 0f);
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f);
        Matrix.rotateM(modelMatrix, 0, rotationX, 1f, 0f, 0f);
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        // MVP = Proj * View * Model
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, tempMatrix, 0);

        GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, mvpMatrix, 0);
        GLES20.glUniform1f(uTimeLoc, time);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTextureLoc, 0);

        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        uvBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aTexLoc);
        GLES20.glVertexAttribPointer(aTexLoc, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);

        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_INT, indexBuffer);

        GLES20.glDisableVertexAttribArray(aPosLoc);
        GLES20.glDisableVertexAttribArray(aTexLoc);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
    }

    private int compileShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader error: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    public void release() {
        if (textureId != -1) {
            GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
            textureId = -1;
        }
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        initialized = false;
    }
}
