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
    // ESTADO DEL PEZ - Calibrado mirando hacia la cámara (horizonte atrás)
    // ═══════════════════════════════════════════════════════════════════════════
    private float posX = -0.06f;
    private float posY = -0.76f;
    private float posZ = -0.21f;
    private float scale = 0.379f;
    private float time = 0f;
    private int aiFrameCounter = 0;    // Throttle AI a cada 3 frames
    private float rotationY = 361.9f;  // Mirando hacia cámara
    private float rotationX = 1.4f;
    private float rotationZ = -0.8f;

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎛️ MODO CALIBRACIÓN
    // ═══════════════════════════════════════════════════════════════════════════
    private boolean calibrationMode = false;

    // ═══════════════════════════════════════════════════════════════════════════
    // 🧠 SISTEMA DE IA - Estados de comportamiento
    // ═══════════════════════════════════════════════════════════════════════════
    private static final int STATE_EXPLORE = 0;       // Explora hacia horizonte y regresa
    private static final int STATE_PATROL_H = 1;      // Patrulla horizontal
    private static final int STATE_FOLLOW_TOUCH = 2;  // 👆 Sigue el dedo del usuario

    private int currentState = STATE_PATROL_H;
    private int previousState = STATE_PATROL_H;       // Para volver después de touch
    private float stateTimer = 0f;
    private float stateDuration = 12f;
    private String[] STATE_NAMES = {"EXPLORE", "PATROL_H", "FOLLOW_TOUCH"};

    // ═══════════════════════════════════════════════════════════════════════════
    // 👆 SISTEMA DE TOUCH - Nadar hacia donde el usuario toca
    // ═══════════════════════════════════════════════════════════════════════════
    private float touchTargetX = 0f;
    private float touchTargetY = 0f;
    private float touchTimeout = 0f;
    private static final float TOUCH_FOLLOW_DURATION = 3.0f;  // Sigue el touch por 3 segundos
    private static final float TOUCH_ARRIVE_THRESHOLD = 0.1f; // Distancia para "llegar"

    // ═══════════════════════════════════════════════════════════════════════════
    // PARÁMETROS DE MOVIMIENTO
    // ═══════════════════════════════════════════════════════════════════════════
    // Zona del Lurker (parte baja de la escena)
    private static final float Y_CLOSE = -0.85f;      // Muy abajo (cerca cámara)
    private static final float Y_FAR = 0.3f;          // Hasta medio (horizonte parcial)
    private static final float X_MIN = -0.7f;         // Límite izquierdo
    private static final float X_MAX = 0.7f;          // Límite derecho

    // Escala dinámica
    private static final float SCALE_CLOSE = 0.35f;   // Grande cuando cerca
    private static final float SCALE_FAR = 0.18f;     // Pequeño cuando lejos
    private static final float Z_POSITION = -0.21f;

    // Velocidades
    private static final float BASE_SPEED = 0.12f;    // Más lento que Leviathan
    private float speedMultiplier = 1.0f;
    private float xDirection = 1f;

    // Estado de movimiento
    private boolean goingToHorizon = false;
    private boolean movingRight = true;
    private float targetPosX = 0f;
    private float targetPosY = 0f;

    // ═══════════════════════════════════════════════════════════════════════════
    // 😰 SISTEMA DE HUIDA - Evita al Leviathan
    // ═══════════════════════════════════════════════════════════════════════════
    private static final float FLEE_DISTANCE = 0.6f;      // Distancia para empezar a huir
    private static final float FLEE_SPEED = 0.25f;        // Velocidad de huida
    private boolean isFleeing = false;
    private float leviathanX = 0f;
    private float leviathanY = 0f;

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
        "    // ALETAS - Ambos lados, movimiento opuesto\n" +
        "    float finDist = abs(p.x);\n" +
        "    float finWave = finDist * finDist * 0.4;\n" +
        "    float finDir = sign(p.x);\n" +
        "    float finMove = sin(u_Time * 3.0) * finWave * finDir;\n" +
        "    // COLA - Oscilacion trasera\n" +
        "    float tailDist = max(0.0, -p.z - 0.3);\n" +
        "    float tailWave = tailDist * tailDist * 1.0;\n" +
        "    float tailMove = sin(u_Time * 4.0 + p.z * 2.0) * tailWave;\n" +
        "    // MANDIBULA - Respiracion\n" +
        "    float isHead = max(0.0, p.z - 0.4);\n" +
        "    float isJaw = max(0.0, -p.y);\n" +
        "    float jawMove = isHead * isJaw * 0.3;\n" +
        "    float breathe = sin(u_Time * 2.0) * 0.5 + 0.5;\n" +
        "    // FLOTACION - Cuerpo sube/baja\n" +
        "    float floatY = sin(u_Time * 1.5) * 0.02;\n" +
        "    // Aplicar\n" +
        "    p.y += finMove + tailMove - jawMove * breathe + floatY;\n" +
        "    p.x += tailMove * 0.3;\n" +
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
        "    \n" +
        "    // ════════════════════════════════════════════════════════\n" +
        "    // 👁️ OJOS BIOLUMINISCENTES\n" +
        "    // Detecta pixeles brillantes (ojos) y agrega glow pulsante\n" +
        "    // ════════════════════════════════════════════════════════\n" +
        "    float brightness = dot(tex.rgb, vec3(0.299, 0.587, 0.114));\n" +
        "    // Ojos = pixeles muy brillantes (blancos/claros)\n" +
        "    float isEye = smoothstep(0.7, 0.9, brightness);\n" +
        "    // Pulso lento y suave para los ojos\n" +
        "    float eyePulse = sin(u_Time * 1.8) * 0.5 + 0.5;\n" +
        "    // Glow cyan intenso en los ojos\n" +
        "    vec3 eyeGlow = vec3(0.3, 0.9, 1.0) * eyePulse * 0.8;\n" +
        "    tex.rgb += eyeGlow * isEye;\n" +
        "    \n" +
        "    // TINTE SUBMARINO PROFUNDO\n" +
        "    tex.rgb *= vec3(0.6, 0.8, 1.0);\n" +
        "    // DESATURACION SUBMARINA (menos en ojos)\n" +
        "    float lum = dot(tex.rgb, vec3(0.3, 0.6, 0.1));\n" +
        "    float desatAmount = mix(0.25, 0.0, isEye);\n" +
        "    tex.rgb = mix(tex.rgb, vec3(lum), desatAmount);\n" +
        "    // BIOLUMINISCENCIA GENERAL - Pulso cyan en partes cyan\n" +
        "    float isCyan = step(0.35, tex.b) * step(tex.r, 0.4);\n" +
        "    float pulse = sin(u_Time * 2.5) * 0.3 + 0.3;\n" +
        "    tex.rgb += vec3(0.0, 0.2, 0.35) * pulse * isCyan * (1.0 - isEye);\n" +
        "    // NIEBLA ABISMAL - Mas azul en bordes (menos en ojos)\n" +
        "    float edge = abs(vUV.x - 0.5) + abs(vUV.y - 0.5);\n" +
        "    tex.rgb = mix(tex.rgb, vec3(0.1, 0.2, 0.35), edge * 0.3 * (1.0 - isEye));\n" +
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

        Bitmap bitmap = null;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;  // 🔧 FIX MEMORY: 50% less GPU for opaque 3D texture

        // Cargar desde archivos descargados (ResourcePreloader garantiza disponibilidad)
        ImageDownloadManager imageMgr = ImageDownloadManager.getInstance(context);
        String texturePath = imageMgr.getImagePath("abyssal_lurker_texture.png");

        if (texturePath != null) {
            Log.d(TAG, "🌐 Cargando textura desde descarga: " + texturePath);
            bitmap = BitmapFactory.decodeFile(texturePath, opts);
        } else {
            Log.e(TAG, "❌ Textura no disponible: abyssal_lurker_texture.png");
        }

        if (bitmap != null) {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();
            Log.d(TAG, "✅ Textura cargada");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SETTER - Posición del Leviathan (para huir)
    // ═══════════════════════════════════════════════════════════════════════════
    public void setLeviathanPosition(float x, float y) {
        this.leviathanX = x;
        this.leviathanY = y;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE - Sistema de IA con huida del Leviathan
    // ═══════════════════════════════════════════════════════════════════════════
    public void update(float deltaTime) {
        time += deltaTime;
        if (time > 62.83f) time -= 62.83f;

        if (calibrationMode) return;

        // Throttle AI: solo actualizar cada 3 frames
        aiFrameCounter++;
        if (aiFrameCounter % 3 != 0) return;

        // ─────────────────────────────────────────────────────────────────────────
        // DETECCIÓN DE PELIGRO - ¿El Leviathan está cerca?
        // ─────────────────────────────────────────────────────────────────────────
        float distToLeviathan = (float)Math.sqrt(
            (posX - leviathanX) * (posX - leviathanX) +
            (posY - leviathanY) * (posY - leviathanY)
        );

        if (distToLeviathan < FLEE_DISTANCE) {
            // ¡HUIR!
            if (!isFleeing) {
                isFleeing = true;
                Log.d(TAG, "🐟 ¡HUYENDO del Leviathan!");
            }
            updateFlee(deltaTime, distToLeviathan);
            return;  // No ejecutar comportamiento normal
        } else {
            isFleeing = false;
        }

        // ─────────────────────────────────────────────────────────────────────────
        // 👆 SEGUIR TOUCH (prioridad media - después de FLEE, antes de IA normal)
        // ─────────────────────────────────────────────────────────────────────────
        if (currentState == STATE_FOLLOW_TOUCH) {
            touchTimeout -= deltaTime;

            // ¿Llegó al destino o timeout?
            float distToTarget = (float)Math.sqrt(
                (posX - touchTargetX) * (posX - touchTargetX) +
                (posY - touchTargetY) * (posY - touchTargetY)
            );

            if (touchTimeout <= 0 || distToTarget < TOUCH_ARRIVE_THRESHOLD) {
                // Volver al estado anterior
                currentState = previousState;
                stateTimer = 0f;
                Log.d(TAG, "🐟 Volviendo a " + STATE_NAMES[currentState]);
            } else {
                updateFollowTouch(deltaTime);
                return;  // No ejecutar IA normal mientras sigue touch
            }
        }

        // ─────────────────────────────────────────────────────────────────────────
        // MÁQUINA DE ESTADOS (comportamiento normal)
        // ─────────────────────────────────────────────────────────────────────────
        stateTimer += deltaTime;
        if (stateTimer >= stateDuration) {
            changeToNewState();
        }

        switch (currentState) {
            case STATE_EXPLORE:
                updateExplore(deltaTime);
                break;
            case STATE_PATROL_H:
                updatePatrolH(deltaTime);
                break;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FLEE - Huir del Leviathan (movimiento SUTIL y suave)
    // ═══════════════════════════════════════════════════════════════════════════
    private void updateFlee(float deltaTime, float distance) {
        // Dirección opuesta al Leviathan
        float dx = posX - leviathanX;
        float dy = posY - leviathanY;

        // Normalizar
        float len = (float)Math.sqrt(dx * dx + dy * dy);
        if (len > 0.01f) {
            dx /= len;
            dy /= len;
        }

        // Velocidad de huida MÁS LENTA y suave
        float speed = BASE_SPEED * 1.5f;  // Solo 1.5x velocidad normal

        // Calcular destino (no muy lejos)
        targetPosX = posX + dx * 0.2f;
        targetPosY = posY + dy * 0.2f;

        // Limitar a zona válida
        targetPosX = Math.max(X_MIN, Math.min(X_MAX, targetPosX));
        targetPosY = Math.max(Y_CLOSE, Math.min(Y_FAR, targetPosY));

        // Interpolar MUY suavemente (sin saltos)
        posX += (targetPosX - posX) * speed * deltaTime;
        posY += (targetPosY - posY) * speed * deltaTime;

        posZ = Z_POSITION;

        updateScaleByY();

        // Orientación SUAVE: mira hacia donde huye
        float targetRotY;
        if (Math.abs(dx) > Math.abs(dy)) {
            targetRotY = dx > 0 ? 90f : -90f;
        } else {
            targetRotY = dy > 0 ? 180f : 0f;
        }

        float rotYDiff = targetRotY - rotationY;
        if (rotYDiff > 180f) rotYDiff -= 360f;
        if (rotYDiff < -180f) rotYDiff += 360f;
        rotationY += rotYDiff * 1.5f * deltaTime;  // Giro más lento

        // Inclinación y balanceo SUTILES
        float targetRotX = dy * 15f;
        rotationX += (targetRotX - rotationX) * 2f * deltaTime;

        rotationZ = 4f * (float)Math.sin(time * 2f);  // Balanceo suave, no nervioso
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 👆 FOLLOW TOUCH - Nadar hacia donde el usuario tocó
    // ═══════════════════════════════════════════════════════════════════════════
    private void updateFollowTouch(float deltaTime) {
        // Dirección hacia el target
        float dx = touchTargetX - posX;
        float dy = touchTargetY - posY;

        // Normalizar
        float len = (float)Math.sqrt(dx * dx + dy * dy);
        if (len > 0.01f) {
            dx /= len;
            dy /= len;
        }

        // Velocidad de seguimiento (un poco más rápida que patrulla normal)
        float speed = BASE_SPEED * 1.8f;

        // Mover suavemente hacia el target
        targetPosX = posX + dx * 0.3f;
        targetPosY = posY + dy * 0.3f;

        // Limitar a zona válida
        targetPosX = Math.max(X_MIN, Math.min(X_MAX, targetPosX));
        targetPosY = Math.max(Y_CLOSE, Math.min(Y_FAR, targetPosY));

        // Interpolar suavemente
        posX += (targetPosX - posX) * speed * deltaTime;
        posY += (targetPosY - posY) * speed * deltaTime;

        posZ = Z_POSITION;
        updateScaleByY();

        // Orientación: mira hacia donde nada
        float targetRotY;
        if (Math.abs(dx) > Math.abs(dy)) {
            targetRotY = dx > 0 ? 90f : -90f;  // Lateral
        } else {
            targetRotY = dy > 0 ? 180f : 0f;   // Vertical
        }

        float rotYDiff = targetRotY - rotationY;
        if (rotYDiff > 180f) rotYDiff -= 360f;
        if (rotYDiff < -180f) rotYDiff += 360f;
        rotationY += rotYDiff * 2.5f * deltaTime;

        // Inclinación basada en dirección
        float targetRotX = dy * 20f;
        rotationX += (targetRotX - rotationX) * 3f * deltaTime;

        // Balanceo alegre (más animado que en patrulla)
        rotationZ = 5f * (float)Math.sin(time * 2.5f);
    }

    /**
     * 👆 Establece un nuevo target de touch para que el Lurker nade hacia él
     * @param worldX Coordenada X en espacio mundo (-1 a 1 aprox)
     * @param worldY Coordenada Y en espacio mundo (-1 a 1 aprox)
     */
    public void setTouchTarget(float worldX, float worldY) {
        // Guardar estado actual para volver después
        if (currentState != STATE_FOLLOW_TOUCH) {
            previousState = currentState;
        }

        // Limitar target a zona válida del Lurker
        touchTargetX = Math.max(X_MIN, Math.min(X_MAX, worldX));
        touchTargetY = Math.max(Y_CLOSE, Math.min(Y_FAR, worldY));

        // Activar estado de seguimiento
        currentState = STATE_FOLLOW_TOUCH;
        touchTimeout = TOUCH_FOLLOW_DURATION;

        Log.d(TAG, String.format("🐟👆 Touch target: (%.2f, %.2f) - Nadando hacia el dedo!", touchTargetX, touchTargetY));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CAMBIO DE ESTADO
    // ═══════════════════════════════════════════════════════════════════════════
    private void changeToNewState() {
        // Alternar entre estados (70% PATROL_H, 30% EXPLORE)
        if (currentState == STATE_PATROL_H) {
            currentState = STATE_EXPLORE;
            stateDuration = 12f + (float)(Math.random() * 8f);  // 12-20 seg
            goingToHorizon = true;
        } else {
            currentState = STATE_PATROL_H;
            stateDuration = 10f + (float)(Math.random() * 10f);  // 10-20 seg
            movingRight = Math.random() > 0.5;
        }

        stateTimer = 0f;
        speedMultiplier = 0.8f + (float)(Math.random() * 0.4f);  // 0.8x a 1.2x

        Log.d(TAG, "🐟 " + STATE_NAMES[currentState] + " (" +
                   String.format("%.0f", stateDuration) + "s)");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXPLORE - Nada hacia horizonte y regresa (zona baja)
    // ═══════════════════════════════════════════════════════════════════════════
    private void updateExplore(float deltaTime) {
        // Regreso más lento para disfrutar
        float returnMultiplier = goingToHorizon ? 1.0f : 0.4f;
        float speed = BASE_SPEED * speedMultiplier * returnMultiplier;

        // Destino vertical
        if (goingToHorizon) {
            targetPosY = Y_FAR;
            if (posY >= Y_FAR - 0.1f) goingToHorizon = false;
        } else {
            targetPosY = Y_CLOSE;
            if (posY <= Y_CLOSE + 0.1f) goingToHorizon = true;
        }

        // Mover suavemente
        float dy = targetPosY - posY;
        posY += dy * speed * deltaTime * 2f;

        // Ondulación horizontal mientras explora
        float waveX = 0.3f * (float)Math.sin(time * 0.4f);
        posX += (waveX - posX) * speed * deltaTime * 2f;

        posZ = Z_POSITION;

        updateScaleByY();
        updateOrientationExplore(dy, deltaTime);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PATROL_H - Patrulla horizontal cerca del fondo
    // ═══════════════════════════════════════════════════════════════════════════
    private void updatePatrolH(float deltaTime) {
        float speed = BASE_SPEED * speedMultiplier;

        // Mantener Y cerca del fondo con pequeña flotación
        float baseY = Y_CLOSE + 0.15f;  // Un poco arriba del fondo
        float floatY = 0.05f * (float)Math.sin(time * 1.2f);
        targetPosY = baseY + floatY;
        posY += (targetPosY - posY) * 2f * deltaTime;

        // Movimiento horizontal de lado a lado
        if (movingRight) {
            targetPosX = X_MAX;
            if (posX >= X_MAX - 0.1f) movingRight = false;
        } else {
            targetPosX = X_MIN;
            if (posX <= X_MIN + 0.1f) movingRight = true;
        }

        float dx = targetPosX - posX;
        posX += dx * speed * deltaTime * 2.5f;

        posZ = Z_POSITION;

        updateScaleByY();
        updateOrientationPatrol(dx, deltaTime);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ORIENTACIÓN - Explore (mira arriba/abajo)
    // ═══════════════════════════════════════════════════════════════════════════
    private void updateOrientationExplore(float dy, float deltaTime) {
        float targetRotY;
        if (dy > 0.02f) {
            targetRotY = 180f;  // Subiendo - da la espalda
        } else if (dy < -0.02f) {
            targetRotY = 0f;    // Bajando - nos mira
        } else {
            targetRotY = rotationY;
        }

        float rotYDiff = targetRotY - rotationY;
        if (rotYDiff > 180f) rotYDiff -= 360f;
        if (rotYDiff < -180f) rotYDiff += 360f;
        rotationY += rotYDiff * 1.5f * deltaTime;

        if (rotationY < 0f) rotationY += 360f;
        if (rotationY > 360f) rotationY -= 360f;

        float targetRotX = Math.max(-25f, Math.min(25f, dy * 20f));
        rotationX += (targetRotX - rotationX) * 2f * deltaTime;

        rotationZ = 3f * (float)Math.sin(time * 0.8f);  // Ligero balanceo
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ORIENTACIÓN - Patrol (mira izq/der)
    // ═══════════════════════════════════════════════════════════════════════════
    private void updateOrientationPatrol(float dx, float deltaTime) {
        // Girar hacia la dirección de movimiento
        float targetRotY;
        if (dx > 0.02f) {
            targetRotY = 90f;   // Mirando a la derecha
        } else if (dx < -0.02f) {
            targetRotY = -90f;  // Mirando a la izquierda
        } else {
            targetRotY = rotationY;
        }

        float rotYDiff = targetRotY - rotationY;
        if (rotYDiff > 180f) rotYDiff -= 360f;
        if (rotYDiff < -180f) rotYDiff += 360f;
        rotationY += rotYDiff * 2f * deltaTime;

        // Ligera inclinación al nadar
        rotationX = 5f * (float)Math.sin(time * 1.5f);
        rotationZ = 8f * (float)Math.sin(time * 0.6f);  // Balanceo lateral
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ESCALA DINÁMICA
    // ═══════════════════════════════════════════════════════════════════════════
    private void updateScaleByY() {
        float normalizedY = (posY - Y_CLOSE) / (Y_FAR - Y_CLOSE);
        normalizedY = Math.max(0f, Math.min(1f, normalizedY));
        scale = SCALE_CLOSE + (SCALE_FAR - SCALE_CLOSE) * normalizedY;
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
        if (enabled) {
            logCalibration("MODE ON");
        }
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
        Log.d("CALIBRATE", "🐟 LURKER [" + event + "] " +
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
     * 🫧 Posición de la boca del pez (para burbujas)
     * Calcula offset basado en rotación y escala
     */
    public float getMouthX() {
        // Offset de la boca desde el centro (en modelo, boca está al frente +X)
        float mouthOffset = scale * 0.25f;
        float radY = (float)Math.toRadians(rotationY);
        return posX + mouthOffset * (float)Math.cos(radY);
    }

    public float getMouthY() {
        // La boca está ligeramente arriba del centro
        float mouthOffset = scale * 0.25f;
        float radY = (float)Math.toRadians(rotationY);
        // Componente Y del offset + pequeño offset vertical
        return posY + mouthOffset * (float)Math.sin(radY) * 0.3f + scale * 0.1f;
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
