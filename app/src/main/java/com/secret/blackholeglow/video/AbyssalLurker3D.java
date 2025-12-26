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
    private float posX = -1.0f;      // Empieza a la izquierda
    private float posY = 0.0f;       // Centro
    private float speedX = 0.25f;    // Velocidad de nado
    private float scale = 0.25f;     // Escala más pequeña
    private float time = 0f;
    private boolean movingRight = true;   // Mirando a la derecha
    private float rotationY = 90f;   // Mirando a la derecha (90°)

    // ═══════════════════════════════════════════════════════════════════════════
    // PROFUNDIDAD (Z simulado)
    // ═══════════════════════════════════════════════════════════════════════════
    private float depth = 0.5f;
    private float targetDepth = 0.5f;
    private float depthSpeed = 0.2f;
    private float depthTimer = 0f;
    private float nextDepthChange = 4f;

    private static final float SCALE_CLOSE = 0.28f;   // Cerca = un poco más grande
    private static final float SCALE_FAR = 0.15f;     // Lejos = más pequeño
    private static final float Y_CLOSE = -0.1f;       // Centro-abajo
    private static final float Y_FAR = 0.15f;         // Centro-arriba

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
        Log.d(TAG, "═══════════════════════════════════════════════");
        Log.d(TAG, "🐟 Inicializando Abyssal Lurker 3D");
        Log.d(TAG, "═══════════════════════════════════════════════");

        // Compilar shaders
        int vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        Log.d(TAG, "Compiled: vs=" + vs + " fs=" + fs);

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vs);
        GLES20.glAttachShader(shaderProgram, fs);
        GLES20.glLinkProgram(shaderProgram);

        // Verificar link
        int[] linked = new int[1];
        GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            Log.e(TAG, "Link error: " + GLES20.glGetProgramInfoLog(shaderProgram));
        }

        // Obtener locations
        aPosLoc = GLES20.glGetAttribLocation(shaderProgram, "a_Position");
        aTexLoc = GLES20.glGetAttribLocation(shaderProgram, "a_TexCoord");
        uMVPLoc = GLES20.glGetUniformLocation(shaderProgram, "u_MVP");
        uTextureLoc = GLES20.glGetUniformLocation(shaderProgram, "u_Texture");
        uTimeLoc = GLES20.glGetUniformLocation(shaderProgram, "u_Time");

        Log.d(TAG, "🔧 Locations: pos=" + aPosLoc + " tex=" + aTexLoc + " mvp=" + uMVPLoc);

        // Cargar modelo OBJ
        loadModel();

        // Cargar textura
        loadTexture();

        // Configurar matrices de vista/proyección fijas (ortográfica)
        Matrix.setIdentityM(viewMatrix, 0);
        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, 3f,   // Cámara
            0f, 0f, 0f,   // Target
            0f, 1f, 0f);  // Up

        initialized = true;
        Log.d(TAG, "✅ Abyssal Lurker 3D listo");
    }

    private void loadModel() {
        try {
            // flipV=true para modelos Meshy AI (texturas invertidas)
            ObjLoader.Mesh mesh = ObjLoader.loadObj(context, OBJ_FILE, true);
            Log.d(TAG, "📦 Modelo: " + mesh.vertexCount + " vértices, " + mesh.faces.size() + " caras");

            this.vertexBuffer = mesh.vertexBuffer;
            this.uvBuffer = mesh.uvBuffer;

            // Construir índices
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
            Log.d(TAG, "✅ Modelo cargado: " + indexCount + " índices");

        } catch (IOException e) {
            Log.e(TAG, "❌ Error cargando modelo: " + e.getMessage());
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

        // Cargar desde drawable
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),
            R.drawable.abyssal_lurker_texture, opts);

        if (bitmap != null) {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            Log.d(TAG, "🎨 Textura: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            bitmap.recycle();
        } else {
            Log.e(TAG, "❌ No se pudo cargar textura");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE - Movimiento con profundidad
    // ═══════════════════════════════════════════════════════════════════════════
    public void update(float deltaTime) {
        time += deltaTime;
        if (time > 62.83f) time -= 62.83f;
        depthTimer += deltaTime;

        // Cambio de profundidad cada cierto tiempo
        if (depthTimer > nextDepthChange) {
            depthTimer = 0f;
            nextDepthChange = 3f + (float)(Math.random() * 5f);

            if (depth < 0.3f) {
                targetDepth = (float)(Math.random() * 0.5f + 0.4f);
            } else if (depth > 0.7f) {
                targetDepth = (float)(Math.random() * 0.5f);
            } else {
                targetDepth = (float)(Math.random());
            }
        }

        // Interpolar profundidad suavemente
        float depthDiff = targetDepth - depth;
        depth += depthDiff * depthSpeed * deltaTime;
        depth = Math.max(0f, Math.min(1f, depth));

        // Escala según profundidad (lejos = pequeño)
        scale = SCALE_CLOSE + (SCALE_FAR - SCALE_CLOSE) * depth;

        // Posición Y según profundidad (lejos = arriba)
        float baseY = Y_CLOSE + (Y_FAR - Y_CLOSE) * depth;

        // Velocidad según profundidad (lejos = más lento)
        float currentSpeed = speedX * (1.0f - depth * 0.5f);

        // Movimiento horizontal
        if (movingRight) {
            posX += currentSpeed * deltaTime;
            if (posX > 1.4f) {
                movingRight = false;
            }
        } else {
            posX -= currentSpeed * deltaTime;
            if (posX < -1.4f) {
                movingRight = true;
            }
        }

        // Rotación suave para voltear
        float targetRotY = movingRight ? 90f : -90f;
        rotationY += (targetRotY - rotationY) * 3f * deltaTime;

        // Ondulación Y
        float waveY = (float)Math.sin(time * 1.2) * 0.015f * (1f - depth * 0.5f);
        posY = baseY + waveY;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DRAW
    // ═══════════════════════════════════════════════════════════════════════════
    public void draw() {
        if (!initialized || !modelLoaded || textureId == -1) return;

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glUseProgram(shaderProgram);

        // Proyección ortográfica
        float aspect = 9f / 16f;  // Portrait
        Matrix.orthoM(projMatrix, 0, -1f, 1f, -1f/aspect, 1f/aspect, 0.1f, 100f);

        // Matriz de modelo
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, posX, posY, 0f);
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f);  // Flip horizontal
        Matrix.rotateM(modelMatrix, 0, -15f, 1f, 0f, 0f);       // Inclinación
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        // MVP = Proj * View * Model
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, tempMatrix, 0);

        // Uniforms
        GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, mvpMatrix, 0);
        GLES20.glUniform1f(uTimeLoc, time);

        // Textura
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTextureLoc, 0);

        // Atributos
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        uvBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aTexLoc);
        GLES20.glVertexAttribPointer(aTexLoc, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);

        // Dibujar
        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_INT, indexBuffer);

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPosLoc);
        GLES20.glDisableVertexAttribArray(aTexLoc);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════════════════
    private int compileShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "❌ Shader error: " + GLES20.glGetShaderInfoLog(shader));
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
        Log.d(TAG, "🗑️ AbyssalLurker3D liberado");
    }

    public boolean isModelLoaded() { return modelLoaded; }
}
