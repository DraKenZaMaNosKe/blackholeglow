package com.secret.blackholeglow.video;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.image.ImageDownloadManager;
import com.secret.blackholeglow.model.ModelDownloadManager;
import com.secret.blackholeglow.util.ObjLoader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * ╔════════════════════════════════════════════════════════════════════════════╗
 * ║            AbyssalLeviathan3D - Bestia Marina Colosal (Meshy AI)           ║
 * ╠════════════════════════════════════════════════════════════════════════════╣
 * ║  Modelo 3D del Abyssal Leviathan con 7,067 triángulos.                     ║
 * ║  Criatura cristalina gigante que patrulla lento el fondo del abismo.       ║
 * ║  No responde a touch - movimiento autónomo majestuoso.                     ║
 * ║  Estética: Espinas de hielo, cristales cyan, presencia imponente.          ║
 * ╚════════════════════════════════════════════════════════════════════════════╝
 */
public class AbyssalLeviathan3D {
    private static final String TAG = "AbyssalLeviathan3D";
    private static final String OBJ_FILE = "abyssal_leviathan.obj";

    private Context context;

    // ═══════════════════════════════════════════════════════════════════════════
    // BUFFERS DEL MODELO
    // ═══════════════════════════════════════════════════════════════════════════
    private FloatBuffer vertexBuffer;
    private FloatBuffer uvBuffer;
    private IntBuffer indexBuffer;
    private int indexCount;

    // ═══════════════════════════════════════════════════════════════════════════
    // ESTADO DE LA BESTIA - Calibrado mirando hacia cámara (horizonte atrás)
    // ═══════════════════════════════════════════════════════════════════════════
    private float posX = 0.02f;
    private float posY = 0.76f;
    private float posZ = -0.18f;
    private float scale = 0.442f;
    private float time = 0f;
    private float rotationY = 2.0f;      // Mirando hacia cámara
    private float rotationX = 6.8f;
    private float rotationZ = -3.7f;

    // 🎛️ MODO CALIBRACIÓN
    private boolean calibrationMode = false;

    // ═══════════════════════════════════════════════════════════════════════════
    // 🧠 SISTEMA DE IA - Estados de comportamiento
    // ═══════════════════════════════════════════════════════════════════════════
    private static final int STATE_PATROL = 0;    // Viaje completo horizonte↔cerca
    private static final int STATE_EXPLORE = 1;   // Viaje corto, cambia a mitad
    private static final int STATE_HOVER = 2;     // Flota en lugar, mira alrededor
    private static final int STATE_CURIOUS = 3;   // Se acerca mucho a cámara
    private static final int STATE_SPIRAL = 4;    // Nada en espiral

    private int currentState = STATE_PATROL;
    private float stateTimer = 0f;
    private float stateDuration = 10f;
    private String[] STATE_NAMES = {"PATROL", "EXPLORE", "HOVER", "CURIOUS", "SPIRAL"};

    // ═══════════════════════════════════════════════════════════════════════════
    // PARÁMETROS DE MOVIMIENTO (varían según estado)
    // ═══════════════════════════════════════════════════════════════════════════
    private static final float Y_CLOSE = -0.6f;           // Cerca de cámara (abajo)
    private static final float Y_FAR = 1.3f;              // Lejos en horizonte (arriba)
    private static final float Y_VERY_CLOSE = -0.9f;      // MUY cerca (estado CURIOUS)
    private static final float SCALE_CLOSE = 0.55f;       // Grande cuando cerca
    private static final float SCALE_FAR = 0.12f;         // Pequeño cuando lejos
    private static final float Z_POSITION = -0.3f;

    // Variables dinámicas del estado actual
    private float targetY = Y_FAR;            // Destino Y actual
    private float speedMultiplier = 1.0f;     // 0.7x a 1.2x (rango reducido)
    private float xDirection = 1f;            // 1 o -1 (izq/der)
    private float xAmplitude = 0.4f;          // Amplitud ondulación X
    private float spiralPhase = 0f;           // Fase para espiral

    // Estado de la trayectoria - MOVIMIENTO CONTINUO
    private float baseSpeed = 0.15f;          // Velocidad base MÁS LENTA
    private float currentVelX = 0f;           // Velocidad actual X
    private float currentVelY = 0f;           // Velocidad actual Y
    private float targetPosX = 0f;            // Posición objetivo X
    private float targetPosY = 0f;            // Posición objetivo Y
    private boolean goingToHorizon = true;    // true=subiendo, false=bajando

    // ═══════════════════════════════════════════════════════════════════════════
    // HOVER - Flotación en lugar
    // ═══════════════════════════════════════════════════════════════════════════
    private float hoverY = 0f;                // Posición Y durante hover
    private float hoverLookAngle = 0f;        // Ángulo de "mirar alrededor"
    private float hoverLookSpeed = 0f;

    // ═══════════════════════════════════════════════════════════════════════════
    // BARREL ROLL (GIRO COCODRILO)
    // ═══════════════════════════════════════════════════════════════════════════
    private static final float ROLL_DURATION = 1.2f;
    private boolean isBarrelRolling = false;
    private float barrelRollProgress = 0f;
    private float barrelRollAngle = 0f;
    private float rollTimer = 0f;
    private float nextRollTime = 8f;

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
    // SHADERS - Animación de criatura cristalina masiva
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
        "    // ALAS/ALETAS - Ambos lados, movimiento majestuoso\n" +
        "    float wingDist = abs(p.x);\n" +
        "    float wingWave = wingDist * wingDist * 0.3;\n" +
        "    float wingDir = sign(p.x);\n" +
        "    float wingMove = sin(u_Time * 2.0) * wingWave * wingDir;\n" +
        "    // ESPINAS/CRISTALES - Vibracion sutil\n" +
        "    float spineMove = sin(u_Time * 2.5 + p.x * 3.0) * 0.015;\n" +
        "    float spineY = spineMove * max(0.0, p.y);\n" +
        "    // COLA MASIVA - Oscilacion lenta\n" +
        "    float tailDist = max(0.0, -p.z - 0.3);\n" +
        "    float tailWave = tailDist * tailDist * 0.8;\n" +
        "    float tailMove = sin(u_Time * 1.5) * tailWave;\n" +
        "    // FLOTACION - Movimiento corporal\n" +
        "    float floatY = sin(u_Time * 1.0) * 0.025;\n" +
        "    float bodyWave = sin(u_Time * 1.2 - p.z * 1.5) * 0.02;\n" +
        "    // Aplicar\n" +
        "    p.y += wingMove + spineY + floatY + bodyWave;\n" +
        "    p.x += tailMove * 0.4;\n" +
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
        "    float brightness = dot(tex.rgb, vec3(0.299, 0.587, 0.114));\n" +
        "    \n" +
        "    // ════════════════════════════════════════════════════════\n" +
        "    // 💎 CRISTALES PÚRPURA PULSANTES\n" +
        "    // Detecta partes brillantes (cristales) y agrega glow púrpura\n" +
        "    // ════════════════════════════════════════════════════════\n" +
        "    float isCrystal = smoothstep(0.5, 0.8, brightness);\n" +
        "    float crystalPulse = sin(u_Time * 2.0) * 0.5 + 0.5;\n" +
        "    vec3 crystalGlow = vec3(0.6, 0.2, 1.0) * crystalPulse * 0.7;\n" +
        "    tex.rgb += crystalGlow * isCrystal;\n" +
        "    \n" +
        "    // ════════════════════════════════════════════════════════\n" +
        "    // ⚡ BIO-ELECTRICIDAD - Ondas suaves que recorren el cuerpo\n" +
        "    // ════════════════════════════════════════════════════════\n" +
        "    float wave = sin(vUV.y * 8.0 - u_Time * 1.5) * 0.5 + 0.5;\n" +
        "    wave *= sin(vUV.x * 6.0 + u_Time * 1.0) * 0.5 + 0.5;\n" +
        "    float electricPulse = pow(wave, 4.0) * 0.25;\n" +
        "    vec3 electricColor = vec3(0.4, 0.85, 1.0);\n" +
        "    tex.rgb += electricColor * electricPulse * smoothstep(0.35, 0.6, brightness);\n" +
        "    \n" +
        "    // ════════════════════════════════════════════════════════\n" +
        "    // 💚 PUNTAS VENENOSAS - Sutil brillo verde en bordes\n" +
        "    // ════════════════════════════════════════════════════════\n" +
        "    float isDark = smoothstep(0.25, 0.1, brightness);\n" +
        "    float venomPulse = sin(u_Time * 1.8) * 0.3 + 0.4;\n" +
        "    vec3 venomGlow = vec3(0.15, 0.8, 0.3) * venomPulse * 0.25;\n" +
        "    tex.rgb += venomGlow * isDark;\n" +
        "    \n" +
        "    // ════════════════════════════════════════════════════════\n" +
        "    // 👁️ OJOS AMENAZANTES - Glow intenso\n" +
        "    // ════════════════════════════════════════════════════════\n" +
        "    float isEye = smoothstep(0.85, 0.95, brightness);\n" +
        "    float eyePulse = sin(u_Time * 2.5) * 0.3 + 0.7;\n" +
        "    vec3 eyeGlow = vec3(1.0, 0.5, 0.8) * eyePulse;\n" +
        "    tex.rgb += eyeGlow * isEye;\n" +
        "    \n" +
        "    // CAUSTICAS - Luz ondulante del agua\n" +
        "    float c1 = sin(vUV.x * 6.0 + u_Time * 1.2);\n" +
        "    float c2 = sin(vUV.y * 5.0 + u_Time * 1.0);\n" +
        "    float caustic = c1 * c2 * 0.12;\n" +
        "    tex.rgb += vec3(caustic * 0.2, caustic * 0.4, caustic * 0.7);\n" +
        "    \n" +
        "    // TINTE AGUA PROFUNDA\n" +
        "    tex.rgb *= vec3(0.7, 0.85, 1.0);\n" +
        "    \n" +
        "    // DESATURACION SUBMARINA (menos en partes brillantes)\n" +
        "    float lum = dot(tex.rgb, vec3(0.3, 0.6, 0.1));\n" +
        "    float desatAmount = mix(0.15, 0.0, max(isCrystal, isEye));\n" +
        "    tex.rgb = mix(tex.rgb, vec3(lum), desatAmount);\n" +
        "    \n" +
        "    // NIEBLA OCEANICA suave\n" +
        "    float edge = abs(vUV.x - 0.5) + abs(vUV.y - 0.5);\n" +
        "    tex.rgb = mix(tex.rgb, vec3(0.1, 0.18, 0.3), edge * 0.15);\n" +
        "    \n" +
        "    gl_FragColor = tex;\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════
    public AbyssalLeviathan3D(Context context) {
        this.context = context;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INICIALIZACIÓN
    // ═══════════════════════════════════════════════════════════════════════════
    public void initialize() {
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

        // Matrices fijas
        Matrix.setIdentityM(viewMatrix, 0);
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f);
        Matrix.orthoM(projMatrix, 0, -1f, 1f, -16f/9f, 16f/9f, 0.1f, 100f);

        initialized = true;
        Log.d(TAG, "Leviathan initialized - " + indexCount/3 + " triangles");
    }

    private void loadModel() {
        try {
            // Intentar cargar desde archivos descargados, fallback a assets
            ObjLoader.Mesh mesh;
            ModelDownloadManager modelMgr = ModelDownloadManager.getInstance(context);
            String modelPath = modelMgr.getModelPath(OBJ_FILE);

            if (modelPath != null) {
                Log.d(TAG, "🌐 Cargando modelo desde descarga: " + modelPath);
                mesh = ObjLoader.loadObjFromFile(modelPath, true);
            } else {
                Log.d(TAG, "📂 Cargando modelo desde assets (fallback)");
                mesh = ObjLoader.loadObj(context, OBJ_FILE, true);
            }

            this.vertexBuffer = mesh.vertexBuffer;
            this.uvBuffer = mesh.uvBuffer;

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

            Log.d(TAG, "Model loaded: " + indexCount/3 + " triangles");

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

        Bitmap bitmap = null;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;

        // Cargar desde archivos descargados (ResourcePreloader garantiza disponibilidad)
        ImageDownloadManager imageMgr = ImageDownloadManager.getInstance(context);
        String texturePath = imageMgr.getImagePath("abyssal_leviathan_texture.png");

        if (texturePath != null) {
            Log.d(TAG, "🌐 Cargando textura desde descarga: " + texturePath);
            bitmap = BitmapFactory.decodeFile(texturePath, opts);
        } else {
            Log.e(TAG, "❌ Textura no disponible: abyssal_leviathan_texture.png");
        }

        if (bitmap != null) {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();
            Log.d(TAG, "✅ Texture loaded");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE - Sistema de IA con múltiples comportamientos
    // ═══════════════════════════════════════════════════════════════════════════
    public void update(float deltaTime) {
        time += deltaTime;
        if (time > 62.83f) time -= 62.83f;

        if (calibrationMode) return;

        // ─────────────────────────────────────────────────────────────────────────
        // MÁQUINA DE ESTADOS - Cambiar comportamiento
        // ─────────────────────────────────────────────────────────────────────────
        stateTimer += deltaTime;
        if (stateTimer >= stateDuration) {
            changeToNewState();
        }

        // ─────────────────────────────────────────────────────────────────────────
        // EJECUTAR COMPORTAMIENTO ACTUAL
        // ─────────────────────────────────────────────────────────────────────────
        switch (currentState) {
            case STATE_PATROL:
                updatePatrol(deltaTime);
                break;
            case STATE_EXPLORE:
                updateExplore(deltaTime);
                break;
            case STATE_HOVER:
                updateHover(deltaTime);
                break;
            case STATE_CURIOUS:
                updateCurious(deltaTime);
                break;
            case STATE_SPIRAL:
                updateSpiral(deltaTime);
                break;
        }

        // ─────────────────────────────────────────────────────────────────────────
        // BARREL ROLL (solo en ciertos estados)
        // ─────────────────────────────────────────────────────────────────────────
        if (currentState != STATE_HOVER) {
            updateBarrelRoll(deltaTime);
        }
        rotationZ = barrelRollAngle;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CAMBIO DE ESTADO - Transición suave, sin saltos
    // ═══════════════════════════════════════════════════════════════════════════
    private void changeToNewState() {
        int previousState = currentState;

        // Elegir nuevo estado (evitar repetir)
        do {
            currentState = (int)(Math.random() * 5);
        } while (currentState == previousState && Math.random() > 0.3);

        // Parámetros más conservadores - SIN CAMBIAR POSICIÓN
        stateTimer = 0f;
        speedMultiplier = 0.7f + (float)(Math.random() * 0.5f);  // 0.7x a 1.2x (más lento)
        xDirection = Math.random() > 0.5 ? 1f : -1f;
        xAmplitude = 0.25f + (float)(Math.random() * 0.25f);  // Menos amplitud

        switch (currentState) {
            case STATE_PATROL:
                stateDuration = 15f + (float)(Math.random() * 10f);  // 15-25 seg
                goingToHorizon = posY < 0.3f;  // Decide según posición actual
                break;

            case STATE_EXPLORE:
                stateDuration = 10f + (float)(Math.random() * 8f);  // 10-18 seg
                goingToHorizon = posY < 0.3f;
                break;

            case STATE_HOVER:
                stateDuration = 6f + (float)(Math.random() * 6f);  // 6-12 seg
                hoverY = posY;
                hoverLookSpeed = 0.3f + (float)(Math.random() * 0.5f);
                hoverLookAngle = rotationY;
                break;

            case STATE_CURIOUS:
                stateDuration = 8f + (float)(Math.random() * 5f);  // 8-13 seg
                goingToHorizon = false;
                break;

            case STATE_SPIRAL:
                stateDuration = 12f + (float)(Math.random() * 8f);  // 12-20 seg
                spiralPhase = (float)(Math.random() * Math.PI * 2);
                goingToHorizon = posY < 0.3f;
                break;
        }

        Log.d(TAG, "🐉 " + STATE_NAMES[currentState] + " (" +
                   String.format("%.0f", stateDuration) + "s)");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PATROL - Viaje suave horizonte↔cerca
    // ═══════════════════════════════════════════════════════════════════════════
    private void updatePatrol(float deltaTime) {
        // Regreso más lento (0.4x) para disfrutar el viaje
        float returnMultiplier = goingToHorizon ? 1.0f : 0.4f;
        float speed = baseSpeed * speedMultiplier * returnMultiplier;

        // Calcular destino Y
        if (goingToHorizon) {
            targetPosY = Y_FAR;
            if (posY >= Y_FAR - 0.1f) goingToHorizon = false;
        } else {
            targetPosY = Y_CLOSE;
            if (posY <= Y_CLOSE + 0.1f) goingToHorizon = true;
        }

        // Mover suavemente hacia destino
        float dy = targetPosY - posY;
        posY += dy * speed * deltaTime * 2f;

        // Ondulación X suave
        targetPosX = xAmplitude * xDirection * (float)Math.sin(time * 0.5f);
        posX += (targetPosX - posX) * speed * deltaTime * 3f;

        posZ = Z_POSITION;

        updateScaleByY();
        updateOrientationSmooth(dy, deltaTime);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXPLORE - Viaje corto con ondulación
    // ═══════════════════════════════════════════════════════════════════════════
    private void updateExplore(float deltaTime) {
        // Regreso más lento (0.35x)
        float returnMultiplier = goingToHorizon ? 1.0f : 0.35f;
        float speed = baseSpeed * speedMultiplier * returnMultiplier;

        // Viaje más corto - solo hasta la mitad
        float midPoint = Y_CLOSE + (Y_FAR - Y_CLOSE) * 0.5f;

        if (goingToHorizon) {
            targetPosY = midPoint;
            if (posY >= midPoint - 0.1f) goingToHorizon = false;
        } else {
            targetPosY = Y_CLOSE;
            if (posY <= Y_CLOSE + 0.1f) goingToHorizon = true;
        }

        float dy = targetPosY - posY;
        posY += dy * speed * deltaTime * 2.5f;

        // Más ondulación lateral
        targetPosX = xAmplitude * 1.3f * xDirection * (float)Math.sin(time * 0.7f);
        posX += (targetPosX - posX) * speed * deltaTime * 3f;

        posZ = Z_POSITION + 0.05f * (float)Math.sin(time * 0.4f);

        updateScaleByY();
        updateOrientationSmooth(dy, deltaTime);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HOVER - Flota suavemente en lugar
    // ═══════════════════════════════════════════════════════════════════════════
    private void updateHover(float deltaTime) {
        // Flotación muy suave
        float floatOffset = 0.03f * (float)Math.sin(time * 1.0f);
        targetPosY = hoverY + floatOffset;
        posY += (targetPosY - posY) * 0.5f * deltaTime;

        // Deriva lateral mínima
        targetPosX = posX + 0.01f * (float)Math.sin(time * 0.5f);
        targetPosX = Math.max(-0.6f, Math.min(0.6f, targetPosX));
        posX += (targetPosX - posX) * 0.3f * deltaTime;

        posZ = Z_POSITION;

        updateScaleByY();

        // Mira alrededor muy lentamente
        hoverLookAngle += (float)Math.sin(time * hoverLookSpeed) * 15f * deltaTime;
        float rotYDiff = hoverLookAngle - rotationY;
        if (rotYDiff > 180f) rotYDiff -= 360f;
        if (rotYDiff < -180f) rotYDiff += 360f;
        rotationY += rotYDiff * 0.5f * deltaTime;

        rotationX = 3f * (float)Math.sin(time * 0.8f);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CURIOUS - Se acerca lentamente a la cámara
    // ═══════════════════════════════════════════════════════════════════════════
    private void updateCurious(float deltaTime) {
        float speed = baseSpeed * speedMultiplier * 0.5f;  // MUY lento

        // Se acerca gradualmente
        targetPosY = Y_VERY_CLOSE;
        float dy = targetPosY - posY;
        posY += dy * speed * deltaTime * 1.5f;

        // Se centra lentamente
        posX += (0f - posX) * 0.3f * deltaTime;

        posZ = Z_POSITION + 0.1f * (1f - (posY - Y_VERY_CLOSE) / (Y_FAR - Y_VERY_CLOSE));

        updateScaleByY();

        // Siempre nos mira
        float targetRotY = 0f;
        float rotYDiff = targetRotY - rotationY;
        if (rotYDiff > 180f) rotYDiff -= 360f;
        if (rotYDiff < -180f) rotYDiff += 360f;
        rotationY += rotYDiff * 1.0f * deltaTime;

        // Cabeceo curioso muy sutil
        rotationX = -5f + 4f * (float)Math.sin(time * 1.2f);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SPIRAL - Espiral suave mientras sube/baja
    // ═══════════════════════════════════════════════════════════════════════════
    private void updateSpiral(float deltaTime) {
        // Regreso más lento (0.4x)
        float returnMultiplier = goingToHorizon ? 1.0f : 0.4f;
        float speed = baseSpeed * speedMultiplier * returnMultiplier;
        spiralPhase += deltaTime * 1.2f * (goingToHorizon ? 1.0f : 0.6f);

        // Movimiento vertical
        if (goingToHorizon) {
            targetPosY = Y_FAR;
            if (posY >= Y_FAR - 0.1f) goingToHorizon = false;
        } else {
            targetPosY = Y_CLOSE;
            if (posY <= Y_CLOSE + 0.1f) goingToHorizon = true;
        }

        float dy = targetPosY - posY;
        posY += dy * speed * deltaTime * 2f;

        // Espiral más suave
        float normalizedY = (posY - Y_CLOSE) / (Y_FAR - Y_CLOSE);
        float spiralRadius = 0.2f + 0.25f * (1f - normalizedY);
        targetPosX = spiralRadius * (float)Math.cos(spiralPhase) * xDirection;
        posX += (targetPosX - posX) * 2f * deltaTime;

        posZ = Z_POSITION + spiralRadius * 0.2f * (float)Math.sin(spiralPhase);

        updateScaleByY();
        updateOrientationSmooth(dy, deltaTime);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS - Funciones auxiliares
    // ═══════════════════════════════════════════════════════════════════════════
    private void updateScaleByY() {
        float normalizedY = (posY - Y_CLOSE) / (Y_FAR - Y_CLOSE);
        normalizedY = Math.max(0f, Math.min(1f, normalizedY));
        scale = SCALE_CLOSE + (SCALE_FAR - SCALE_CLOSE) * normalizedY;
    }

    private void updateOrientationSmooth(float dy, float deltaTime) {
        // Determinar hacia dónde debe mirar según movimiento
        float targetRotY;
        if (dy > 0.02f) {
            targetRotY = 180f;  // Subiendo - da la espalda
        } else if (dy < -0.02f) {
            targetRotY = 0f;    // Bajando - nos mira
        } else {
            targetRotY = rotationY;  // Sin cambio
        }

        // Interpolar suavemente
        float rotYDiff = targetRotY - rotationY;
        if (rotYDiff > 180f) rotYDiff -= 360f;
        if (rotYDiff < -180f) rotYDiff += 360f;
        rotationY += rotYDiff * 1.2f * deltaTime;  // Más lento

        // Mantener en rango
        if (rotationY < 0f) rotationY += 360f;
        if (rotationY > 360f) rotationY -= 360f;

        // Inclinación suave según velocidad vertical
        float targetRotX = Math.max(-20f, Math.min(20f, dy * 15f));
        rotationX += (targetRotX - rotationX) * 2f * deltaTime;
    }

    private void updateBarrelRoll(float deltaTime) {
        if (isBarrelRolling) {
            barrelRollProgress += deltaTime / ROLL_DURATION;
            if (barrelRollProgress >= 1f) {
                barrelRollProgress = 0f;
                barrelRollAngle = 0f;
                isBarrelRolling = false;
                nextRollTime = 8f + (float)(Math.random() * 10f);
                rollTimer = 0f;
            } else {
                float ease = (float)(1 - Math.cos(barrelRollProgress * Math.PI)) / 2f;
                barrelRollAngle = ease * 360f;
            }
        } else {
            rollTimer += deltaTime;
            if (rollTimer >= nextRollTime) {
                isBarrelRolling = true;
                barrelRollProgress = 0f;
                Log.d(TAG, "🐉 BARREL ROLL!");
            }
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

        // Matriz de modelo
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, posX, posY, posZ);
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f);
        Matrix.rotateM(modelMatrix, 0, rotationX, 1f, 0f, 0f);
        Matrix.rotateM(modelMatrix, 0, rotationZ, 0f, 0f, 1f);
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

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎛️ CALIBRACIÓN - Setters para ajustar en tiempo real
    // ═══════════════════════════════════════════════════════════════════════════

    public void setCalibrationMode(boolean enabled) {
        this.calibrationMode = enabled;
        if (enabled) logCalibration("MODE ON");
    }

    public boolean isCalibrationMode() { return calibrationMode; }

    public void setPosition(float x, float y, float z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    public void setRotation(float rx, float ry, float rz) {
        this.rotationX = rx;
        this.rotationY = ry;
        this.rotationZ = rz;
    }

    public void setScale(float s) { this.scale = s; }

    public void adjustPosition(float dx, float dy, float dz) {
        this.posX += dx;
        this.posY += dy;
        this.posZ += dz;
    }

    public void adjustRotation(float drx, float dry, float drz) {
        this.rotationX += drx;
        this.rotationY += dry;
        this.rotationZ += drz;
    }

    public void adjustScale(float ds) {
        this.scale += ds;
        if (this.scale < 0.01f) this.scale = 0.01f;
    }

    /** Filtro logcat: "CALIBRATE" */
    public void logCalibration(String event) {
        Log.d("CALIBRATE", "🐉 LEVIATHAN [" + event + "] " +
            String.format("POS(%.2f,%.2f,%.2f) ROT(%.1f,%.1f,%.1f) SCALE:%.3f",
            posX, posY, posZ, rotationX, rotationY, rotationZ, scale));
    }

    public float getPosX() { return posX; }
    public float getPosY() { return posY; }
    public float getPosZ() { return posZ; }
    public float getRotX() { return rotationX; }
    public float getRotY() { return rotationY; }
    public float getRotZ() { return rotationZ; }
    public float getScale() { return scale; }

    /**
     * 🫧 Posición de la boca del Leviathan (para burbujas)
     * El Leviathan es más grande, offset mayor
     */
    public float getMouthX() {
        float mouthOffset = scale * 0.3f;
        float radY = (float)Math.toRadians(rotationY);
        return posX + mouthOffset * (float)Math.cos(radY);
    }

    public float getMouthY() {
        float mouthOffset = scale * 0.3f;
        float radY = (float)Math.toRadians(rotationY);
        return posY + mouthOffset * (float)Math.sin(radY) * 0.3f + scale * 0.12f;
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
