package com.secret.blackholeglow.scenes;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.Battery3D;
import com.secret.blackholeglow.Clock3D;
import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.image.ImageDownloadManager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║           ⭐ SAINT SEIYA SCENE - Doble Capa 3D                           ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  CAPA 1: Fondo Cosmos (mesh 3D con depth - movimiento sutil)             ║
 * ║  CAPA 2: Seiya 3D (mesh con depth - movimiento principal)                ║
 * ║                                                                          ║
 * ║  Ambas capas usan mesh 3D con desplazamiento de vértices.                ║
 * ║  El fondo se mueve menos que Seiya para crear profundidad.               ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class SaintSeiyaScene extends WallpaperScene {
    private static final String TAG = "SaintSeiyaScene";

    // UI Components
    private EqualizerBarsDJ equalizerDJ;
    private Clock3D clock;
    private Battery3D battery;
    private float time = 0f;

    // ═══════════════════════════════════════════════════════════════════════
    // 🖼️ TEXTURAS - 2 CAPAS
    // ═══════════════════════════════════════════════════════════════════════
    private int texBackground = -1;
    private int texBackgroundDepth = -1;
    private int texSeiya = -1;
    private int texSeiyaDepth = -1;

    // ═══════════════════════════════════════════════════════════════════════
    // 🎲 MESH 3D - Compartido por ambas capas
    // ═══════════════════════════════════════════════════════════════════════
    private static final int GRID_SIZE = 50;
    private static final int VERTEX_COUNT = GRID_SIZE * GRID_SIZE;
    private static final int INDEX_COUNT = (GRID_SIZE - 1) * (GRID_SIZE - 1) * 6;

    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;
    private ShortBuffer indexBuffer;

    private int meshShaderProgram = -1;
    private int positionLoc, texCoordLoc, mvpMatrixLoc, imageLoc, depthLoc, depthStrengthLoc;
    private int timeLoc, heatDistortionLoc, cosmosAuraLoc, fistPosLoc, fistPowerLoc, fist2PosLoc;

    // ═══════════════════════════════════════════════════════════════════════
    // 📐 MATRICES 3D
    // ═══════════════════════════════════════════════════════════════════════
    private float[] projectionMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] modelMatrix = new float[16];
    private float[] mvpMatrix = new float[16];
    private float[] tempMatrix = new float[16];

    // ═══════════════════════════════════════════════════════════════════════
    // 🎭 CONFIGURACIÓN 3D - Ángulos separados para cada capa
    // ═══════════════════════════════════════════════════════════════════════
    private float cameraAngleX = 0f;  // Para Seiya (automático)
    private float cameraAngleY = 0f;

    private float bgAngleX = 0f;      // Para Fondo (controlado por touch)
    private float bgAngleY = 0f;

    // Configuración para cada capa
    private static final float BG_DEPTH_STRENGTH = 0.15f;    // Fondo: relieve sutil
    private static final float BG_CAMERA_DISTANCE = 2.8f;    // Fondo: más lejos
    private static final float BG_ANGLE_MULT = 0.4f;         // Fondo: se mueve menos
    private static final float BG_SCALE = 0.72f;             // Fondo: más compacto
    private static final float BG_HEAT_DISTORTION = 0.5f;    // Fondo: distorsión sutil

    private static final float SEIYA_DEPTH_STRENGTH = 0.25f; // Seiya: relieve más pronunciado
    private static final float SEIYA_CAMERA_DISTANCE = 2.0f; // Seiya: más cerca
    private static final float SEIYA_SCALE = 0.48f;          // Seiya: un poco más lejos
    private static final float SEIYA_HEAT_DISTORTION = 0.0f; // Seiya: SIN distorsión (nítido)
    private static final float SEIYA_COSMOS_AURA = 0.0f;     // Seiya: AURA DE COSMOS desactivada (cuerpo)
    private static final float SEIYA_FIST_POWER = 1.0f;      // Seiya: PUÑO PODER activado

    // Ángulos base SEIYA (calibrado)
    private static final float BASE_ANGLE_X = 13.4f;
    private static final float BASE_ANGLE_Y = 18.0f;

    // Ángulos base FONDO (calibrado)
    private static final float BG_BASE_ANGLE_X = 11.6f;
    private static final float BG_BASE_ANGLE_Y = -2.3f;

    // Movimiento sutil
    private static final float SWAY_ANGLE_X = 3f;
    private static final float SWAY_ANGLE_Y = 2f;
    private static final float SPEED_X = 0.15f;
    private static final float SPEED_Y = 0.12f;

    // Touch para calibración
    private boolean touchMode = false;  // DESHABILITADO - ambos puños calibrados
    private float fistX = 0.724f, fistY = 0.239f;  // Posición del puño PODER (rayos)
    private float fist2X = 0.117f, fist2Y = 0.529f;  // Posición del puño COSMOS (aura)
    private boolean calibratingSeiya = true;
    private float touchX = 0f, touchY = 0f;
    private float lastTouchX = 0f, lastTouchY = 0f;

    // ═══════════════════════════════════════════════════════════════════════
    // ✨ PARTÍCULAS MÁGICAS - Sistema simple y eficiente
    // ═══════════════════════════════════════════════════════════════════════
    private static final int MAX_PARTICLES = 30;  // Máximo de partículas
    private float[] particleX = new float[MAX_PARTICLES];
    private float[] particleY = new float[MAX_PARTICLES];
    private float[] particleVX = new float[MAX_PARTICLES];
    private float[] particleVY = new float[MAX_PARTICLES];
    private float[] particleLife = new float[MAX_PARTICLES];  // 0-1, cuando llega a 0 muere
    private float[] particleSize = new float[MAX_PARTICLES];
    private int particleShader = -1;
    private int particlePosLoc, particleColorLoc, particlePointSizeLoc;
    // 🔧 FIX GC: Pre-allocated buffers for particle drawing (was allocating ~900 ByteBuffers/sec)
    private final float[] particlePos = new float[2];
    private final java.nio.FloatBuffer particleFB = java.nio.ByteBuffer.allocateDirect(8)
            .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
    private java.util.Random random = new java.util.Random();

    @Override
    public String getName() {
        return "SAINT_SEIYA";
    }

    @Override
    public String getDescription() {
        return "Saint Seiya - Doble Capa 3D";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_saintseiya;
    }

    @Override
    protected void setupScene() {
        Log.d(TAG, "⭐ Configurando Saint Seiya DOBLE CAPA 3D...");

        // 1. Crear mesh 3D (compartido)
        createMesh();

        // 2. Shader con depth displacement
        setupMeshShader();

        // 3. Cargar texturas
        loadTextures();

        // 4. Inicializar matrices
        Matrix.setIdentityM(modelMatrix, 0);

        // 🎵 Ecualizador
        try {
            equalizerDJ = new EqualizerBarsDJ();
            equalizerDJ.initialize();
            equalizerDJ.setTheme(EqualizerBarsDJ.Theme.COSMOS);
            equalizerDJ.setScreenSize(screenWidth, screenHeight);
        } catch (Exception e) {
            Log.e(TAG, "Error EqualizerBarsDJ: " + e.getMessage());
        }

        // ⏰ Reloj
        try {
            clock = new Clock3D(context, Clock3D.THEME_COSMOS, 0f, 0.75f);
            clock.setShowMilliseconds(true);  // ⏱️ Mostrar HH:MM:SS.mmm
        } catch (Exception e) {
            Log.e(TAG, "Error Clock3D: " + e.getMessage());
        }

        // 🔋 Batería
        try {
            battery = new Battery3D(context, Battery3D.THEME_COSMOS, 0.81f, -0.34f);
        } catch (Exception e) {
            Log.e(TAG, "Error Battery3D: " + e.getMessage());
        }

        Log.d(TAG, "⭐ Saint Seiya DOBLE CAPA 3D listo!");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🎲 CREAR MESH 3D
    // ═══════════════════════════════════════════════════════════════════════

    private void createMesh() {
        float[] positions = new float[VERTEX_COUNT * 2];
        float[] texCoords = new float[VERTEX_COUNT * 2];
        short[] indices = new short[INDEX_COUNT];

        int posIndex = 0, texIndex = 0;

        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                float px = (float) x / (GRID_SIZE - 1) * 2f - 1f;
                float py = (float) y / (GRID_SIZE - 1) * 2f - 1f;
                positions[posIndex++] = px;
                positions[posIndex++] = py;

                float u = (float) x / (GRID_SIZE - 1);
                float v = 1f - (float) y / (GRID_SIZE - 1);
                texCoords[texIndex++] = u;
                texCoords[texIndex++] = v;
            }
        }

        int idx = 0;
        for (int y = 0; y < GRID_SIZE - 1; y++) {
            for (int x = 0; x < GRID_SIZE - 1; x++) {
                int tl = y * GRID_SIZE + x;
                int tr = tl + 1;
                int bl = (y + 1) * GRID_SIZE + x;
                int br = bl + 1;
                indices[idx++] = (short) tl;
                indices[idx++] = (short) bl;
                indices[idx++] = (short) tr;
                indices[idx++] = (short) tr;
                indices[idx++] = (short) bl;
                indices[idx++] = (short) br;
            }
        }

        ByteBuffer bb1 = ByteBuffer.allocateDirect(positions.length * 4);
        bb1.order(ByteOrder.nativeOrder());
        vertexBuffer = bb1.asFloatBuffer();
        vertexBuffer.put(positions);
        vertexBuffer.position(0);

        ByteBuffer bb2 = ByteBuffer.allocateDirect(texCoords.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        texCoordBuffer = bb2.asFloatBuffer();
        texCoordBuffer.put(texCoords);
        texCoordBuffer.position(0);

        ByteBuffer bb3 = ByteBuffer.allocateDirect(indices.length * 2);
        bb3.order(ByteOrder.nativeOrder());
        indexBuffer = bb3.asShortBuffer();
        indexBuffer.put(indices);
        indexBuffer.position(0);

        Log.d(TAG, "✅ Mesh 3D: " + VERTEX_COUNT + " vértices");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🎨 SHADER - OPTIMIZADO
    // ═══════════════════════════════════════════════════════════════════════

    private void setupMeshShader() {
        String vertexShader =
            "#version 300 es\n" +
            "precision highp float;\n" +
            "in vec2 aPosition;\n" +
            "in vec2 aTexCoord;\n" +
            "out vec2 vTexCoord;\n" +
            "uniform mat4 uMVPMatrix;\n" +
            "uniform sampler2D uDepth;\n" +
            "uniform float uDepthStrength;\n" +
            "\n" +
            "void main() {\n" +
            "    vTexCoord = aTexCoord;\n" +
            "    float depth = texture(uDepth, aTexCoord).r;\n" +
            "    float z = (depth - 0.5) * uDepthStrength;\n" +
            "    vec4 position = vec4(aPosition.x, aPosition.y, z, 1.0);\n" +
            "    gl_Position = uMVPMatrix * position;\n" +
            "}\n";

        // ═══════════════════════════════════════════════════════════════════════
        // 🎨 FRAGMENT SHADER - COSMOS AURA + PUÑO PODER
        // ═══════════════════════════════════════════════════════════════════════
        String fragmentShader =
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "in vec2 vTexCoord;\n" +
            "out vec4 fragColor;\n" +
            "uniform sampler2D uImage;\n" +
            "uniform float uTime;\n" +
            "uniform float uHeatDistortion;\n" +
            "uniform float uCosmosAura;\n" +
            "uniform float uFistPower;  // Efecto del puño PODER (rayos)\n" +
            "uniform vec2 uFistPos;  // Posición del puño PODER\n" +
            "uniform vec2 uFist2Pos; // Posición del puño COSMOS (aura)\n" +
            "\n" +
            "// HSB to RGB\n" +
            "vec3 hsb2rgb(vec3 c) {\n" +
            "    vec3 rgb = clamp(abs(mod(c.x*6.0+vec3(0.0,4.0,2.0),6.0)-3.0)-1.0, 0.0, 1.0);\n" +
            "    return c.z * mix(vec3(1.0), rgb*rgb*(3.0-2.0*rgb), c.y);\n" +
            "}\n" +
            "\n" +
            "const float GLOW = 0.015;\n" +
            "const float FIST_RADIUS = 0.20;\n" +
            "const vec2 D[8] = vec2[](vec2(1,0),vec2(.707,.707),vec2(0,1),vec2(-.707,.707),\n" +
            "                         vec2(-1,0),vec2(-.707,-.707),vec2(0,-1),vec2(.707,-.707));\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 uv = vTexCoord;\n" +
            "    float t = mod(uTime, 62.83);\n" +
            "    \n" +
            "    // Heat distortion (solo fondo)\n" +
            "    if (uHeatDistortion > 0.0) {\n" +
            "        float m = 1.0 - smoothstep(0.09, 0.18, distance(uv, vec2(0.75, 0.55)));\n" +
            "        if (m > 0.01) {\n" +
            "            float s = 0.002 * uHeatDistortion * m;\n" +
            "            uv.x += sin(uv.y * 30.0 + t * 2.5) * s;\n" +
            "            uv.y += cos(uv.x * 35.0 + t * 2.2) * s * 0.4;\n" +
            "        }\n" +
            "    }\n" +
            "    \n" +
            "    vec4 color = texture(uImage, uv);\n" +
            "    \n" +
            "    // Variable para el efecto del puño (se aplica al final como capa extra)\n" +
            "    vec4 fistEffect = vec4(0.0);\n" +
            "    \n" +
            "    // ═══════════════════════════════════════════════════════════════\n" +
            "    // 👊 PUÑO PODER - Cosmos Concentrado + Rayos (capa separada)\n" +
            "    // ═══════════════════════════════════════════════════════════════\n" +
            "    if (uFistPower > 0.0) {\n" +
            "        float fistDist = distance(uv, uFistPos);\n" +
            "        \n" +
            "        if (fistDist < FIST_RADIUS) {\n" +
            "            // Intensidad del cosmos concentrado\n" +
            "            float fistIntensity = 1.0 - smoothstep(0.0, FIST_RADIUS, fistDist);\n" +
            "            fistIntensity *= fistIntensity;\n" +
            "            \n" +
            "            // Pulso de energía\n" +
            "            float pulse = sin(t * 6.0) * 0.3 + 0.7;\n" +
            "            \n" +
            "            // ⚡ RAYOS - líneas de energía\n" +
            "            vec2 toFist = uv - uFistPos;\n" +
            "            float angle = atan(toFist.y, toFist.x);\n" +
            "            \n" +
            "            // 6 rayos que rotan\n" +
            "            float rays = 0.0;\n" +
            "            for (float i = 0.0; i < 6.0; i++) {\n" +
            "                float rayAngle = i * 1.047 + t * 3.0;\n" +
            "                float rayDiff = abs(mod(angle - rayAngle + 3.14159, 6.28318) - 3.14159);\n" +
            "                float ray = smoothstep(0.25, 0.0, rayDiff) * (1.0 - fistDist / FIST_RADIUS);\n" +
            "                rays += ray;\n" +
            "            }\n" +
            "            rays = min(rays, 1.0);\n" +
            "            \n" +
            "            // Colores del poder\n" +
            "            vec3 coreColor = vec3(0.9, 0.97, 1.0);   // Blanco-cyan brillante\n" +
            "            vec3 rayColor = vec3(0.4, 0.7, 1.0);     // Azul eléctrico\n" +
            "            vec3 outerColor = vec3(0.2, 0.5, 0.9);   // Azul\n" +
            "            \n" +
            "            // Mix de colores\n" +
            "            vec3 fistColor = mix(outerColor, rayColor, rays);\n" +
            "            fistColor = mix(fistColor, coreColor, fistIntensity * pulse);\n" +
            "            \n" +
            "            // Guardar como capa separada (OPACA)\n" +
            "            float fistAlpha = (fistIntensity * 0.7 + rays * 0.5) * pulse * uFistPower;\n" +
            "            fistEffect = vec4(fistColor, fistAlpha);\n" +
            "        }\n" +
            "    }\n" +
            "    \n" +
            "    // ⭐ COSMOS AURA - Solo alrededor del SEGUNDO PUÑO (gancho)\n" +
            "    float fist2Dist = distance(uv, uFist2Pos);\n" +
            "    float FIST2_RADIUS = 0.15;\n" +
            "    if (fist2Dist < FIST2_RADIUS && color.a < 0.1) {\n" +
            "        float totalAlpha = 0.0;\n" +
            "        \n" +
            "        for (int i = 0; i < 8; i++) {\n" +
            "            float a = texture(uImage, uv + D[i] * GLOW).a;\n" +
            "            if (a > 0.3) {\n" +
            "                totalAlpha += a;\n" +
            "            }\n" +
            "        }\n" +
            "        \n" +
            "        if (totalAlpha > 0.0) {\n" +
            "            float intensity = smoothstep(0.0, 4.0, totalAlpha);\n" +
            "            float proximityFade = 1.0 - smoothstep(0.0, FIST2_RADIUS, fist2Dist);\n" +
            "            \n" +
            "            vec3 coreColor = vec3(0.4, 0.7, 1.0);\n" +
            "            vec3 outerColor = vec3(0.15, 0.25, 0.6);\n" +
            "            \n" +
            "            float hueShift = sin(t * 1.5) * 0.5 + 0.5;\n" +
            "            vec3 aura = mix(outerColor, coreColor, intensity * hueShift);\n" +
            "            \n" +
            "            float pulse = sin(t * 3.0) * 0.2 + 0.8;\n" +
            "            aura += coreColor * intensity * intensity * 0.4 * pulse;\n" +
            "            \n" +
            "            float alpha = intensity * proximityFade * 0.8;\n" +
            "            color = vec4(aura, alpha);\n" +
            "        }\n" +
            "    }\n" +
            "    \n" +
            "    // ═══════════════════════════════════════════════════════════════\n" +
            "    // 🔝 APLICAR PUÑO COMO CAPA ENCIMA (additive + overlay)\n" +
            "    // ═══════════════════════════════════════════════════════════════\n" +
            "    if (fistEffect.a > 0.0) {\n" +
            "        // Blend: la capa del puño se suma encima\n" +
            "        color.rgb = color.rgb + fistEffect.rgb * fistEffect.a;\n" +
            "        color.a = max(color.a, fistEffect.a);\n" +
            "    }\n" +
            "    \n" +
            "    if (color.a < 0.02) discard;\n" +
            "    fragColor = color;\n" +
            "}\n";

        meshShaderProgram = createProgram(vertexShader, fragmentShader);
        positionLoc = GLES30.glGetAttribLocation(meshShaderProgram, "aPosition");
        texCoordLoc = GLES30.glGetAttribLocation(meshShaderProgram, "aTexCoord");
        mvpMatrixLoc = GLES30.glGetUniformLocation(meshShaderProgram, "uMVPMatrix");
        imageLoc = GLES30.glGetUniformLocation(meshShaderProgram, "uImage");
        depthLoc = GLES30.glGetUniformLocation(meshShaderProgram, "uDepth");
        depthStrengthLoc = GLES30.glGetUniformLocation(meshShaderProgram, "uDepthStrength");
        timeLoc = GLES30.glGetUniformLocation(meshShaderProgram, "uTime");
        heatDistortionLoc = GLES30.glGetUniformLocation(meshShaderProgram, "uHeatDistortion");
        cosmosAuraLoc = GLES30.glGetUniformLocation(meshShaderProgram, "uCosmosAura");
        fistPowerLoc = GLES30.glGetUniformLocation(meshShaderProgram, "uFistPower");
        fistPosLoc = GLES30.glGetUniformLocation(meshShaderProgram, "uFistPos");
        fist2PosLoc = GLES30.glGetUniformLocation(meshShaderProgram, "uFist2Pos");

        Log.d(TAG, "✅ Shader 3D con DOS PUÑOS compilado (PODER + COSMOS)");

        // ═══════════════════════════════════════════════════════════════════════
        // ✨ SHADER DE PARTÍCULAS - Ultra simple
        // ═══════════════════════════════════════════════════════════════════════
        String particleVS =
            "#version 300 es\n" +
            "in vec2 aPosition;\n" +
            "uniform float uPointSize;\n" +
            "void main() {\n" +
            "    gl_Position = vec4(aPosition, 0.0, 1.0);\n" +
            "    gl_PointSize = uPointSize;\n" +
            "}\n";

        String particleFS =
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "uniform vec4 uColor;\n" +
            "out vec4 fragColor;\n" +
            "void main() {\n" +
            "    vec2 center = gl_PointCoord - vec2(0.5);\n" +
            "    float dist = length(center);\n" +
            "    float alpha = smoothstep(0.5, 0.2, dist) * uColor.a;\n" +
            "    fragColor = vec4(uColor.rgb, alpha);\n" +
            "}\n";

        particleShader = createProgram(particleVS, particleFS);
        particlePosLoc = GLES30.glGetAttribLocation(particleShader, "aPosition");
        particleColorLoc = GLES30.glGetUniformLocation(particleShader, "uColor");
        particlePointSizeLoc = GLES30.glGetUniformLocation(particleShader, "uPointSize");

        Log.d(TAG, "✅ Shader de partículas compilado");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ✨ MÉTODOS DE PARTÍCULAS
    // ═══════════════════════════════════════════════════════════════════════

    private void spawnParticles(float x, float y, int count) {
        for (int i = 0; i < MAX_PARTICLES && count > 0; i++) {
            if (particleLife[i] <= 0) {
                // Posición inicial (convertir UV a coordenadas normalizadas -1 a 1)
                particleX[i] = x * 2f - 1f;
                particleY[i] = -(y * 2f - 1f);  // Invertir Y

                // Velocidad aleatoria (dispersión suave)
                float angle = random.nextFloat() * 6.28318f;
                float speed = 0.3f + random.nextFloat() * 0.5f;
                particleVX[i] = (float)Math.cos(angle) * speed;
                particleVY[i] = (float)Math.sin(angle) * speed + 0.2f;  // Subir un poco

                // Vida y tamaño
                particleLife[i] = 0.8f + random.nextFloat() * 0.4f;
                particleSize[i] = 8f + random.nextFloat() * 12f;

                count--;
            }
        }
    }

    private void updateParticles(float dt) {
        for (int i = 0; i < MAX_PARTICLES; i++) {
            if (particleLife[i] > 0) {
                // Mover
                particleX[i] += particleVX[i] * dt;
                particleY[i] += particleVY[i] * dt;

                // Desacelerar
                particleVX[i] *= 0.98f;
                particleVY[i] *= 0.98f;

                // Reducir vida
                particleLife[i] -= dt * 1.5f;
            }
        }
    }

    private void drawParticles() {
        if (particleShader <= 0) return;

        GLES30.glUseProgram(particleShader);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE);  // Additive

        GLES30.glEnableVertexAttribArray(particlePosLoc);

        for (int i = 0; i < MAX_PARTICLES; i++) {
            if (particleLife[i] > 0) {
                particlePos[0] = particleX[i];
                particlePos[1] = particleY[i];

                // Color cyan brillante con fade
                float alpha = particleLife[i];
                GLES30.glUniform4f(particleColorLoc, 0.5f, 0.8f, 1.0f, alpha);
                GLES30.glUniform1f(particlePointSizeLoc, particleSize[i] * alpha);

                // 🔧 Reuse pre-allocated FloatBuffer (was: new ByteBuffer.allocateDirect per particle per frame)
                particleFB.clear();
                particleFB.put(particlePos).position(0);

                GLES30.glVertexAttribPointer(particlePosLoc, 2, GLES30.GL_FLOAT, false, 0, particleFB);
                GLES30.glDrawArrays(GLES30.GL_POINTS, 0, 1);
            }
        }

        GLES30.glDisableVertexAttribArray(particlePosLoc);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 📥 CARGAR TEXTURAS
    // ═══════════════════════════════════════════════════════════════════════

    private void loadTextures() {
        // Usar ImageDownloadManager para obtener el directorio correcto de imágenes
        java.io.File imagesDir = ImageDownloadManager.getInstance(context).getImageDirectory();

        texBackground = loadTextureFromFile(new java.io.File(imagesDir, "fondouniverso.png"));
        texBackgroundDepth = loadTextureFromFile(new java.io.File(imagesDir, "fondouniverso3d.png"));
        texSeiya = loadTextureFromFile(new java.io.File(imagesDir, "seiya_solo.png"));
        texSeiyaDepth = loadTextureFromFile(new java.io.File(imagesDir, "seiya_depth.png"));

        Log.d(TAG, "✅ Texturas: bg=" + texBackground + ", bgDepth=" + texBackgroundDepth +
                   ", seiya=" + texSeiya + ", seiyaDepth=" + texSeiyaDepth);
    }

    private int loadTextureFromFile(java.io.File file) {
        if (!file.exists()) {
            Log.e(TAG, "❌ No existe: " + file.getName());
            return -1;
        }
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565;  // 🔧 FIX MEMORY: 50% less GPU per opaque texture
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);

            if (bitmap != null) {
                int[] textures = new int[1];
                GLES30.glGenTextures(1, textures, 0);
                int texId = textures[0];

                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
                GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);

                Log.d(TAG, "📥 " + file.getName() + " (" + bitmap.getWidth() + "x" + bitmap.getHeight() + ")");
                bitmap.recycle();
                return texId;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }
        return -1;
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vs = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER);
        GLES30.glShaderSource(vs, vertexSource);
        GLES30.glCompileShader(vs);

        int fs = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER);
        GLES30.glShaderSource(fs, fragmentSource);
        GLES30.glCompileShader(fs);

        int program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vs);
        GLES30.glAttachShader(program, fs);
        GLES30.glLinkProgram(program);
        return program;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🔄 UPDATE & DRAW
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    protected void releaseSceneResources() {
        if (texBackground > 0) GLES30.glDeleteTextures(1, new int[]{texBackground}, 0);
        if (texBackgroundDepth > 0) GLES30.glDeleteTextures(1, new int[]{texBackgroundDepth}, 0);
        if (texSeiya > 0) GLES30.glDeleteTextures(1, new int[]{texSeiya}, 0);
        if (texSeiyaDepth > 0) GLES30.glDeleteTextures(1, new int[]{texSeiyaDepth}, 0);

        if (meshShaderProgram > 0) GLES30.glDeleteProgram(meshShaderProgram);
        if (particleShader > 0) GLES30.glDeleteProgram(particleShader);

        if (clock != null) clock.dispose();
        if (battery != null) battery.dispose();
        if (equalizerDJ != null) equalizerDJ.release();
    }

    @Override
    public void update(float deltaTime) {
        // Tiempo con wrap seguro (ciclo cada ~100 segundos)
        time += deltaTime;
        if (time > 100.0f) time -= 100.0f;

        float swayX = (float)(Math.sin(time * SPEED_X) * SWAY_ANGLE_X);
        float swayY = (float)(Math.sin(time * SPEED_Y + 1.5) * SWAY_ANGLE_Y);

        // SEIYA: Touch o automático
        if (touchMode && calibratingSeiya) {
            cameraAngleX = touchX * 25f;
            cameraAngleY = touchY * 18f;
            Log.d(TAG, "📐 SEIYA CALIBRACIÓN: angleX=" + String.format("%.1f", cameraAngleX) +
                  "° angleY=" + String.format("%.1f", cameraAngleY) + "°");
        } else {
            cameraAngleX = BASE_ANGLE_X + swayX;
            cameraAngleY = BASE_ANGLE_Y + swayY;
        }

        // FONDO: Touch o automático
        if (touchMode && !calibratingSeiya) {
            bgAngleX = touchX * 20f;
            bgAngleY = touchY * 15f;
            Log.d(TAG, "📐 BG CALIBRACIÓN: angleX=" + String.format("%.1f", bgAngleX) +
                  "° angleY=" + String.format("%.1f", bgAngleY) + "°");
        } else {
            bgAngleX = BG_BASE_ANGLE_X + swayX * 0.3f;
            bgAngleY = BG_BASE_ANGLE_Y + swayY * 0.3f;
        }

        if (equalizerDJ != null) equalizerDJ.update(deltaTime);
        if (clock != null) clock.update(deltaTime);
        if (battery != null) battery.update(deltaTime);

        // ✨ Actualizar partículas
        updateParticles(deltaTime);

        super.update(deltaTime);
    }

    @Override
    public void draw() {
        if (isDisposed) return;

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glDisable(GLES30.GL_CULL_FACE);

        // ═══════════════════════════════════════════════════════════════
        // CAPA 1: FONDO 3D (sin aura)
        // ═══════════════════════════════════════════════════════════════
        GLES30.glDisable(GLES30.GL_BLEND);
        drawLayerWithAngles(texBackground, texBackgroundDepth,
                  BG_CAMERA_DISTANCE, BG_DEPTH_STRENGTH, bgAngleX, bgAngleY,
                  BG_SCALE, BG_HEAT_DISTORTION, 0.0f, 0.0f);  // Sin aura, sin puño

        // ═══════════════════════════════════════════════════════════════
        // CAPA 2: SEIYA 3D (con AURA DE COSMOS!)
        // ═══════════════════════════════════════════════════════════════
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT);
        drawLayerWithAngles(texSeiya, texSeiyaDepth,
                  SEIYA_CAMERA_DISTANCE, SEIYA_DEPTH_STRENGTH, cameraAngleX, cameraAngleY,
                  SEIYA_SCALE, SEIYA_HEAT_DISTORTION, SEIYA_COSMOS_AURA, SEIYA_FIST_POWER);  // 👊 PUÑO PODER

        // ═══════════════════════════════════════════════════════════════
        // UI Elements
        // ═══════════════════════════════════════════════════════════════
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        if (equalizerDJ != null) equalizerDJ.draw();
        if (clock != null) clock.draw();
        if (battery != null) battery.draw();

        // ✨ Partículas mágicas (encima de todo)
        drawParticles();

        super.draw();
    }

    private void drawLayerWithAngles(int texImage, int texDepth,
                          float cameraDistance, float depthStrength,
                          float angleX, float angleY,
                          float scale, float heatDistortion, float cosmosAura, float fistPower) {
        if (texImage <= 0 || meshShaderProgram <= 0) return;

        GLES30.glUseProgram(meshShaderProgram);

        // Calcular MVP con parámetros específicos de esta capa
        float aspect = (float) screenWidth / screenHeight;
        Matrix.perspectiveM(projectionMatrix, 0, 45f, aspect, 0.1f, 100f);

        // Usar ángulos directamente (ya calculados por capa)
        float angleXRad = (float) Math.toRadians(angleX);
        float angleYRad = (float) Math.toRadians(angleY);

        float camX = (float)(cameraDistance * Math.sin(angleXRad) * Math.cos(angleYRad));
        float camY = (float)(cameraDistance * Math.sin(angleYRad));
        float camZ = (float)(cameraDistance * Math.cos(angleXRad) * Math.cos(angleYRad));

        Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, 0f, 0f, 0f, 0f, 1f, 0f);
        Matrix.setIdentityM(modelMatrix, 0);
        // Aplicar escala - ajustar Y para pantallas altas
        float scaleX = scale;
        float scaleY = scale / aspect;  // Estirar en Y para cubrir pantallas altas
        Matrix.scaleM(modelMatrix, 0, scaleX, scaleY, scale);
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);

        GLES30.glUniformMatrix4fv(mvpMatrixLoc, 1, false, mvpMatrix, 0);
        GLES30.glUniform1f(depthStrengthLoc, depthStrength);

        // Pasar tiempo, distorsión, aura, puño poder y posiciones de puños al shader
        GLES30.glUniform1f(timeLoc, time);
        GLES30.glUniform1f(heatDistortionLoc, heatDistortion);
        GLES30.glUniform1f(cosmosAuraLoc, cosmosAura);
        GLES30.glUniform1f(fistPowerLoc, fistPower);
        GLES30.glUniform2f(fistPosLoc, fistX, fistY);      // Puño PODER (rayos)
        GLES30.glUniform2f(fist2PosLoc, fist2X, fist2Y);   // Puño COSMOS (aura)

        // Texturas
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texImage);
        GLES30.glUniform1i(imageLoc, 0);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texDepth > 0 ? texDepth : texImage);
        GLES30.glUniform1i(depthLoc, 1);

        // Dibujar mesh
        vertexBuffer.position(0);
        GLES30.glEnableVertexAttribArray(positionLoc);
        GLES30.glVertexAttribPointer(positionLoc, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        texCoordBuffer.position(0);
        GLES30.glEnableVertexAttribArray(texCoordLoc);
        GLES30.glVertexAttribPointer(texCoordLoc, 2, GLES30.GL_FLOAT, false, 0, texCoordBuffer);

        indexBuffer.position(0);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, INDEX_COUNT, GLES30.GL_UNSIGNED_SHORT, indexBuffer);

        GLES30.glDisableVertexAttribArray(positionLoc);
        GLES30.glDisableVertexAttribArray(texCoordLoc);
    }

    @Override
    public void setScreenSize(int width, int height) {
        super.setScreenSize(width, height);
        if (equalizerDJ != null) equalizerDJ.setScreenSize(width, height);
    }

    public void updateMusicBands(float[] bands) {
        if (equalizerDJ != null && bands != null) {
            equalizerDJ.updateFromBands(bands);
        }
    }

    @Override
    public boolean onTouchEvent(float normalizedX, float normalizedY, int action) {
        // Convertir de coordenadas normalizadas (-1 a 1) a UV (0 a 1)
        float uvX = (normalizedX + 1f) / 2f;
        float uvY = 1f - ((normalizedY + 1f) / 2f);  // Invertir Y para UV

        // ✨ PARTÍCULAS MÁGICAS - Spawnar en cualquier toque
        if (action == 0) {  // ACTION_DOWN - toque inicial
            spawnParticles(uvX, uvY, 5);  // 5 partículas por toque
        } else if (action == 2) {  // ACTION_MOVE - arrastrando
            spawnParticles(uvX, uvY, 2);  // 2 partículas al arrastrar
        }

        // Modo calibración del SEGUNDO puño (cosmos aura)
        if (touchMode && (action == 0 || action == 2)) {
            fist2X = uvX;
            fist2Y = uvY;
            Log.d(TAG, "👊 PUÑO COSMOS MOVIDO A: x=" + String.format("%.3f", fist2X) +
                       " y=" + String.format("%.3f", fist2Y));
        }

        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
