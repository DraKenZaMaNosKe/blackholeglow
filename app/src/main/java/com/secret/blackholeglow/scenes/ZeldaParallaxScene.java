package com.secret.blackholeglow.scenes;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.Battery3D;
import com.secret.blackholeglow.Clock3D;
import com.secret.blackholeglow.Link3D;
import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.GyroscopeManager;
import com.secret.blackholeglow.image.ImageDownloadManager;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║     🗡️ ZELDA PARALLAX 3D SCENE - Depth Map Displacement Effect          ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  TECNOLOGÍA: Depth Map Displacement                                      ║
 * ║  - Cada capa tiene una imagen COLOR + una imagen DEPTH (grayscale)       ║
 * ║  - El shader desplaza píxeles según su profundidad                       ║
 * ║  - Blanco en depth map = cerca = se mueve MÁS                            ║
 * ║  - Negro en depth map = lejos = se mueve MENOS                           ║
 * ║                                                                          ║
 * ║  CAPAS CON DEPTH MAPS:                                                   ║
 * ║  1. Cielo     (fondo.png + fondo_depth.png)                              ║
 * ║  2. Paisaje   (paisaje.png + paisaje_depth.png)                          ║
 * ║  3. Piedra    (piedra.png + piedra_depth.png)                            ║
 * ║  4. Link      (link.png) - sin depth, parallax simple                    ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class ZeldaParallaxScene extends WallpaperScene {
    private static final String TAG = "ZeldaParallax3D";

    // ═══════════════════════════════════════════════════════════════════════
    // 🖼️ TEXTURAS - COLOR + DEPTH MAPS
    // ═══════════════════════════════════════════════════════════════════════

    // Texturas de color
    private int texCielo = -1;
    private int texPaisaje = -1;
    private int texPiedra = -1;
    private int texLink = -1;

    // Textura de profundidad (solo fondo tiene parallax)
    private int texCieloDepth = -1;

    // Nombres de archivos
    private static final String FILE_CIELO = "zelda_fondo.png";
    private static final String FILE_PAISAJE = "zelda_paisaje.png";
    private static final String FILE_PIEDRA = "zelda_piedra.png";
    private static final String FILE_LINK = "zelda_link.png";

    private static final String FILE_CIELO_DEPTH = "zelda_fondo_depth.png";

    // ═══════════════════════════════════════════════════════════════════════
    // 📐 CONFIGURACIÓN DE PROFUNDIDAD 3D - Solo fondo tiene parallax
    // ═══════════════════════════════════════════════════════════════════════

    // Escala de profundidad del fondo (cuánto afecta el depth map)
    private static final float DEPTH_SCALE_CIELO = 0.08f;    // Efecto sutil

    // Máximo desplazamiento base
    private static final float MAX_OFFSET = 0.5f;

    // ═══════════════════════════════════════════════════════════════════════
    // 🎮 GIROSCOPIO
    // ═══════════════════════════════════════════════════════════════════════
    private GyroscopeManager gyroscope;
    private float offsetX = 0f;
    private float offsetY = 0f;

    // Smoothing - más rápido para respuesta inmediata
    private static final float SMOOTH_FACTOR = 0.15f;

    // ═══════════════════════════════════════════════════════════════════════
    // 🎨 SHADERS
    // ═══════════════════════════════════════════════════════════════════════

    // Shader con depth map displacement
    private int depthShaderProgram = -1;
    private int depthPosLoc, depthTexCoordLoc;
    private int depthTextureLoc, depthMapLoc;
    private int depthOffsetLoc, depthScaleLoc, depthAlphaLoc;

    // Shader simple (para Link sin depth map)
    private int simpleShaderProgram = -1;
    private int simplePosLoc, simpleTexCoordLoc;
    private int simpleTextureLoc, simpleOffsetLoc, simpleAlphaLoc;

    // Buffers
    private FloatBuffer quadVertexBuffer;
    private FloatBuffer quadTexCoordBuffer;        // UV normal (fullscreen)
    private FloatBuffer quadTexCoordBufferCover;   // UV con aspect ratio (solo para fondo)

    // ═══════════════════════════════════════════════════════════════════════
    // 🎵 UI COMPONENTS
    // ═══════════════════════════════════════════════════════════════════════
    private EqualizerBarsDJ equalizerDJ;
    private Clock3D clock;
    private Battery3D battery;

    // ═══════════════════════════════════════════════════════════════════════
    // 🗡️ LINK 3D MODEL
    // ═══════════════════════════════════════════════════════════════════════
    private Link3D link3D;
    private boolean useLink3D = true;  // Toggle para probar 2D vs 3D


    @Override
    public String getName() {
        return "ZELDA_BOTW";
    }

    @Override
    public String getDescription() {
        return "Zelda BOTW - 3D Depth Parallax";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_zelda;
    }

    @Override
    protected void setupScene() {
        Log.d(TAG, "🗡️ Configurando Zelda 3D Parallax Scene...");

        // Inicializar giroscopio - SENSIBILIDAD SUAVE
        gyroscope = new GyroscopeManager(context);
        gyroscope.setSensitivity(1.5f);  // Sensibilidad moderada para efecto sutil
        gyroscope.start();

        // Crear shaders
        createDepthShader();
        createSimpleShader();

        // Crear buffers del quad
        createQuadBuffers();

        // Cargar texturas
        loadTextures();

        // 🎵 Ecualizador con tema ZELDA
        try {
            equalizerDJ = new EqualizerBarsDJ();
            equalizerDJ.initialize();
            equalizerDJ.setTheme(EqualizerBarsDJ.Theme.ZELDA);
            equalizerDJ.setScreenSize(screenWidth, screenHeight);
            Log.d(TAG, "✅ Ecualizador ZELDA activado");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error EqualizerBarsDJ: " + e.getMessage());
        }

        // ⏰ Reloj con tema ZELDA
        try {
            clock = new Clock3D(context, Clock3D.THEME_ZELDA, 0f, 0.8f);
            clock.setShowMilliseconds(false);
            Log.d(TAG, "✅ Reloj ZELDA activado");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error Clock3D: " + e.getMessage());
        }

        // 🔋 Batería con tema ZELDA
        try {
            battery = new Battery3D(context, Battery3D.THEME_ZELDA, 0.81f, -0.34f);
            Log.d(TAG, "✅ Batería ZELDA activada");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error Battery3D: " + e.getMessage());
        }

        // 🗡️ Link 3D Model - Valores finales ajustados
        if (useLink3D) {
            try {
                link3D = new Link3D(context);
                link3D.setPosition(-0.490f, -1.045f, 0.000f);  // Posición final
                link3D.setScale(0.439f);                        // Escala final
                link3D.setRotationY(9.2f);                      // Rotación final
                link3D.setTouchEnabled(false);                  // Desactivar ajuste interactivo
                link3D.setScreenSize(screenWidth, screenHeight);
                Log.d(TAG, "✅ Link 3D activado (valores fijos)");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error Link3D: " + e.getMessage());
                useLink3D = false;
            }
        }

        Log.d(TAG, "🗡️ Zelda 3D Parallax Scene lista!");
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════
     * 🎨 SHADER CON DEPTH MAP DISPLACEMENT
     * ═══════════════════════════════════════════════════════════════════════
     * Este shader lee el depth map y desplaza los píxeles según su profundidad.
     * - Píxeles blancos (depth=1) se mueven MUCHO con el giroscopio
     * - Píxeles negros (depth=0) casi no se mueven
     */
    private void createDepthShader() {
        String vertexShader =
            "attribute vec4 a_Position;\n" +
            "attribute vec2 a_TexCoord;\n" +
            "varying vec2 v_TexCoord;\n" +
            "void main() {\n" +
            "    gl_Position = a_Position;\n" +
            "    v_TexCoord = a_TexCoord;\n" +
            "}\n";

        String fragmentShader =
            "precision mediump float;\n" +
            "uniform sampler2D u_Texture;\n" +      // Imagen de color
            "uniform sampler2D u_DepthMap;\n" +     // Depth map grayscale
            "uniform vec2 u_Offset;\n" +            // Offset del giroscopio
            "uniform float u_DepthScale;\n" +       // Escala del efecto
            "uniform float u_Alpha;\n" +
            "varying vec2 v_TexCoord;\n" +
            "\n" +
            "void main() {\n" +
            "    // Leer profundidad (blanco=1=cerca, negro=0=lejos)\n" +
            "    float depth = texture2D(u_DepthMap, v_TexCoord).r;\n" +
            "    \n" +
            "    // Calcular desplazamiento basado en profundidad\n" +
            "    // Objetos cercanos (depth alto) se mueven más\n" +
            "    vec2 displacement = u_Offset * depth * u_DepthScale;\n" +
            "    \n" +
            "    // Aplicar desplazamiento a las coordenadas UV\n" +
            "    vec2 displacedUV = v_TexCoord + displacement;\n" +
            "    \n" +
            "    // Clamp para evitar artefactos en los bordes\n" +
            "    displacedUV = clamp(displacedUV, 0.005, 0.995);\n" +
            "    \n" +
            "    // Samplear color con UV desplazado\n" +
            "    vec4 color = texture2D(u_Texture, displacedUV);\n" +
            "    \n" +
            "    gl_FragColor = vec4(color.rgb, color.a * u_Alpha);\n" +
            "}\n";

        depthShaderProgram = createProgram(vertexShader, fragmentShader);

        depthPosLoc = GLES30.glGetAttribLocation(depthShaderProgram, "a_Position");
        depthTexCoordLoc = GLES30.glGetAttribLocation(depthShaderProgram, "a_TexCoord");
        depthTextureLoc = GLES30.glGetUniformLocation(depthShaderProgram, "u_Texture");
        depthMapLoc = GLES30.glGetUniformLocation(depthShaderProgram, "u_DepthMap");
        depthOffsetLoc = GLES30.glGetUniformLocation(depthShaderProgram, "u_Offset");
        depthScaleLoc = GLES30.glGetUniformLocation(depthShaderProgram, "u_DepthScale");
        depthAlphaLoc = GLES30.glGetUniformLocation(depthShaderProgram, "u_Alpha");

        Log.d(TAG, "✅ Depth Shader creado");
    }

    /**
     * Shader simple para capas sin depth map (ej: Link)
     */
    private void createSimpleShader() {
        String vertexShader =
            "attribute vec4 a_Position;\n" +
            "attribute vec2 a_TexCoord;\n" +
            "uniform vec2 u_Offset;\n" +
            "varying vec2 v_TexCoord;\n" +
            "void main() {\n" +
            "    gl_Position = a_Position + vec4(u_Offset, 0.0, 0.0);\n" +
            "    v_TexCoord = a_TexCoord;\n" +
            "}\n";

        String fragmentShader =
            "precision mediump float;\n" +
            "uniform sampler2D u_Texture;\n" +
            "uniform float u_Alpha;\n" +
            "varying vec2 v_TexCoord;\n" +
            "void main() {\n" +
            "    vec4 color = texture2D(u_Texture, v_TexCoord);\n" +
            "    gl_FragColor = vec4(color.rgb, color.a * u_Alpha);\n" +
            "}\n";

        simpleShaderProgram = createProgram(vertexShader, fragmentShader);

        simplePosLoc = GLES30.glGetAttribLocation(simpleShaderProgram, "a_Position");
        simpleTexCoordLoc = GLES30.glGetAttribLocation(simpleShaderProgram, "a_TexCoord");
        simpleTextureLoc = GLES30.glGetUniformLocation(simpleShaderProgram, "u_Texture");
        simpleOffsetLoc = GLES30.glGetUniformLocation(simpleShaderProgram, "u_Offset");
        simpleAlphaLoc = GLES30.glGetUniformLocation(simpleShaderProgram, "u_Alpha");

        Log.d(TAG, "✅ Simple Shader creado");
    }

    /**
     * Crea los buffers para un quad fullscreen
     */
    private void createQuadBuffers() {
        float[] vertices = {
            -1f, -1f,  // Bottom-left
             1f, -1f,  // Bottom-right
            -1f,  1f,  // Top-left
             1f,  1f   // Top-right
        };

        // UV coords normales (fullscreen, para paisaje/piedra/link)
        float[] texCoords = {
            0f, 1f,  // Bottom-left
            1f, 1f,  // Bottom-right
            0f, 0f,  // Top-left
            1f, 0f   // Top-right
        };

        quadVertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        quadVertexBuffer.put(vertices).position(0);

        // Buffer UV normal (fullscreen)
        quadTexCoordBuffer = ByteBuffer.allocateDirect(texCoords.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        quadTexCoordBuffer.put(texCoords).position(0);

        // Buffer UV para cover (solo fondo) - se actualiza en setScreenSize
        quadTexCoordBufferCover = ByteBuffer.allocateDirect(texCoords.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        quadTexCoordBufferCover.put(texCoords).position(0);

        Log.d(TAG, "✅ Quad buffers creados");
    }

    /**
     * Ajusta las coordenadas UV del FONDO para modo "contain" centrado
     * Muestra la imagen COMPLETA centrada, puede tener bordes si no coincide el aspect ratio
     * Solo afecta al fondo (cielo), las otras capas usan fullscreen
     */
    private void updateQuadForAspectRatio() {
        if (screenWidth <= 0 || screenHeight <= 0 || quadTexCoordBufferCover == null) return;

        // Aspect ratio de la imagen de fondo (~16:9 landscape)
        float imageAspect = 16f / 9f;
        float screenAspect = (float) screenWidth / screenHeight;

        // MODO CONTAIN CENTRADO: Mostrar imagen completa, centrada
        // En portrait con imagen landscape: imagen ocupa ancho completo,
        // se centra verticalmente (espacios arriba/abajo se rellenan con clear color)

        float uMin = 0f, uMax = 1f;
        float vMin = 0f, vMax = 1f;

        // Para contain: NO recortamos la imagen, mostramos todo
        // El quad sigue siendo fullscreen, solo ajustamos UVs para no estirar
        // PERO para simplificar: mostrar imagen completa sin crop

        // Alternativa: Mostrar más de la imagen (crop mínimo)
        // Crop suave: solo 20% máximo en cada lado
        if (screenAspect < imageAspect) {
            // Pantalla portrait con imagen landscape
            // En lugar de crop agresivo, hacemos un crop suave
            float maxCrop = 0.15f; // Máximo 15% de crop en cada lado
            float visibleWidth = screenAspect / imageAspect;
            float naturalCrop = (1f - visibleWidth) / 2f;

            // Usar el menor crop entre natural y máximo permitido
            float offset = Math.min(naturalCrop, maxCrop);
            uMin = offset;
            uMax = 1f - offset;

            // Para compensar verticalmente, centrar la imagen
            // Si no llenamos el ancho, necesitamos llenar el alto
            if (offset < naturalCrop) {
                // Estamos mostrando más ancho del que "cabe",
                // así que la imagen no llenará la pantalla verticalmente
                // Calcular qué porción del alto usar
                float effectiveImageWidth = 1f - 2f * offset;
                float imageHeightInScreen = effectiveImageWidth / screenAspect * imageAspect;
                if (imageHeightInScreen < 1f) {
                    // Centrar verticalmente
                    float vOffset = (1f - imageHeightInScreen) / 2f;
                    // Invertir porque OpenGL tiene Y invertido
                    vMin = vOffset;
                    vMax = 1f - vOffset;
                }
            }
        } else {
            // Pantalla landscape o cuadrada con imagen landscape
            // Crop vertical suave
            float maxCrop = 0.15f;
            float visibleHeight = imageAspect / screenAspect;
            float naturalCrop = (1f - visibleHeight) / 2f;
            float offset = Math.min(naturalCrop, maxCrop);
            vMin = offset;
            vMax = 1f - offset;
        }

        // Solo actualizar el buffer de cover (para el fondo)
        float[] texCoords = {
            uMin, vMax,
            uMax, vMax,
            uMin, vMin,
            uMax, vMin
        };

        quadTexCoordBufferCover.clear();
        quadTexCoordBufferCover.put(texCoords).position(0);

        Log.d(TAG, "📐 Fondo CONTAIN: u[" + uMin + "," + uMax + "] v[" + vMin + "," + vMax + "] (crop máx 15%)");
    }

    /**
     * Carga todas las texturas (color + depth maps)
     * DESCARGA AUTOMÁTICA: Si las imágenes no existen, las descarga de Supabase
     */
    private void loadTextures() {
        ImageDownloadManager imageManager = ImageDownloadManager.getInstance(context);

        Log.d(TAG, "╔══════════════════════════════════════════════════════════════╗");
        Log.d(TAG, "║              📥 DESCARGANDO TEXTURAS ZELDA                   ║");
        Log.d(TAG, "╚══════════════════════════════════════════════════════════════╝");

        // ═══════════════════════════════════════════════════════════════
        // CAPA 1: CIELO - Descargar si no existe
        // ═══════════════════════════════════════════════════════════════
        downloadIfNeeded(imageManager, FILE_CIELO, "Cielo");
        downloadIfNeeded(imageManager, FILE_CIELO_DEPTH, "Cielo Depth");
        texCielo = loadTextureFromFile(imageManager.getImagePath(FILE_CIELO), "Cielo");
        texCieloDepth = loadTextureFromFile(imageManager.getImagePath(FILE_CIELO_DEPTH), "Cielo Depth");

        if (texCielo == -1) {
            texCielo = loadTextureFromResource(R.drawable.preview_zelda, "Cielo fallback");
        }

        // ═══════════════════════════════════════════════════════════════
        // CAPA 2: PAISAJE (estática, sin depth map)
        // ═══════════════════════════════════════════════════════════════
        downloadIfNeeded(imageManager, FILE_PAISAJE, "Paisaje");
        texPaisaje = loadTextureFromFile(imageManager.getImagePath(FILE_PAISAJE), "Paisaje");

        // ═══════════════════════════════════════════════════════════════
        // CAPA 3: PIEDRA (estática, sin depth map)
        // ═══════════════════════════════════════════════════════════════
        downloadIfNeeded(imageManager, FILE_PIEDRA, "Piedra");
        texPiedra = loadTextureFromFile(imageManager.getImagePath(FILE_PIEDRA), "Piedra");

        // ═══════════════════════════════════════════════════════════════
        // CAPA 4: LINK (sin depth map)
        // ═══════════════════════════════════════════════════════════════
        downloadIfNeeded(imageManager, FILE_LINK, "Link");
        texLink = loadTextureFromFile(imageManager.getImagePath(FILE_LINK), "Link");

        Log.d(TAG, "╔══════════════════════════════════════════════════════════════╗");
        Log.d(TAG, "║                    📸 TEXTURAS CARGADAS                      ║");
        Log.d(TAG, "╠══════════════════════════════════════════════════════════════╣");
        Log.d(TAG, "║ Cielo:   ID=" + texCielo + " | Depth ID=" + texCieloDepth + (texCieloDepth > 0 ? " ✅ PARALLAX" : " ❌"));
        Log.d(TAG, "║ Paisaje: ID=" + texPaisaje + " (estático)");
        Log.d(TAG, "║ Piedra:  ID=" + texPiedra + " (estático)");
        Log.d(TAG, "║ Link:    ID=" + texLink + " (estático)");
        Log.d(TAG, "╚══════════════════════════════════════════════════════════════╝");

        if (texCieloDepth > 0) {
            Log.d(TAG, "🎯 DEPTH MAP LOADED! El efecto 3D debería funcionar");
        } else {
            Log.w(TAG, "⚠️ DEPTH MAP NO CARGADO - Solo parallax simple disponible");
        }
    }

    /**
     * Descarga una imagen si no está disponible localmente
     */
    private void downloadIfNeeded(ImageDownloadManager manager, String fileName, String displayName) {
        if (manager.getImagePath(fileName) == null) {
            Log.d(TAG, "📥 Descargando: " + displayName + " (" + fileName + ")");
            boolean success = manager.downloadImageSync(fileName, percent -> {
                Log.d(TAG, "📥 " + displayName + ": " + percent + "%");
            });
            if (success) {
                Log.d(TAG, "✅ " + displayName + " descargado correctamente");
            } else {
                Log.e(TAG, "❌ Error descargando: " + displayName);
            }
        } else {
            Log.d(TAG, "✅ " + displayName + " ya disponible localmente");
        }
    }

    /**
     * Carga una textura desde archivo
     */
    private int loadTextureFromFile(String path, String name) {
        if (path == null) {
            Log.w(TAG, "⚠️ Ruta nula para " + name);
            return -1;
        }

        File file = new File(path);
        if (!file.exists() || !file.canRead()) {
            Log.w(TAG, "⚠️ Archivo no disponible: " + name);
            return -1;
        }

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(path, options);

            if (bitmap == null) {
                Log.e(TAG, "❌ No se pudo decodificar: " + name);
                return -1;
            }

            int[] textureIds = new int[1];
            GLES30.glGenTextures(1, textureIds, 0);
            int textureId = textureIds[0];

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();

            Log.d(TAG, "✅ " + name + " (ID:" + textureId + ")");
            return textureId;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error cargando " + name + ": " + e.getMessage());
            return -1;
        }
    }

    /**
     * Carga textura desde recursos drawable
     */
    private int loadTextureFromResource(int resourceId, String name) {
        try {
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId);
            if (bitmap == null) return -1;

            int[] textureIds = new int[1];
            GLES30.glGenTextures(1, textureIds, 0);
            int textureId = textureIds[0];

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);

            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();

            Log.d(TAG, "✅ Recurso: " + name + " (ID:" + textureId + ")");
            return textureId;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error recurso " + name + ": " + e.getMessage());
            return -1;
        }
    }

    @Override
    public void update(float deltaTime) {
        if (isPaused || isDisposed) return;

        // Obtener valores del giroscopio
        if (gyroscope != null) {
            float targetX = gyroscope.getTiltX() * MAX_OFFSET;
            float targetY = gyroscope.getTiltY() * MAX_OFFSET;

            // Suavizar el movimiento
            offsetX = offsetX + (targetX - offsetX) * SMOOTH_FACTOR;
            offsetY = offsetY + (targetY - offsetY) * SMOOTH_FACTOR;
        }

        // Actualizar UI
        if (equalizerDJ != null) equalizerDJ.update(deltaTime);
        if (clock != null) clock.update(deltaTime);
        if (battery != null) battery.update(deltaTime);

        // Actualizar Link 3D
        if (link3D != null) link3D.update(deltaTime);

        super.update(deltaTime);
    }

    @Override
    public void draw() {
        if (isDisposed) return;

        // Clear con color de Hyrule
        GLES30.glClearColor(0.4f, 0.6f, 0.8f, 1.0f);  // Azul cielo
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        // Configurar para 2D
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // ═══════════════════════════════════════════════════════════════
        // DIBUJAR CAPAS CON DEPTH MAP 3D
        // ═══════════════════════════════════════════════════════════════

        // Capa 1: Fondo - CON DEPTH MAP 3D + ASPECT RATIO COVER
        if (texCielo > 0) {
            if (texCieloDepth > 0) {
                // ¡Depth map cargado! Usar efecto 3D real con cover mode
                drawLayerWithDepth(texCielo, texCieloDepth, DEPTH_SCALE_CIELO, 1.0f, true);
            } else {
                // Fallback: parallax simple
                drawLayerSimple(texCielo, 0.08f, 1.0f);
            }
        }

        // Capa 2: Paisaje - ESTÁTICO (se queda quieto)
        if (texPaisaje > 0) {
            drawLayerStatic(texPaisaje, 1.0f);
        }

        // Capa 3: Piedra - ESTÁTICO
        if (texPiedra > 0) {
            drawLayerStatic(texPiedra, 1.0f);
        }

        // Capa 4: Link - 3D o 2D según configuración
        if (useLink3D && link3D != null) {
            // 🗡️ Dibujar Link 3D
            GLES30.glEnable(GLES30.GL_DEPTH_TEST);
            GLES30.glDepthFunc(GLES30.GL_LEQUAL);
            link3D.draw();
            GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        } else if (texLink > 0) {
            // Fallback: Link 2D sprite
            drawLayerStatic(texLink, 1.0f);
        }

        // ═══════════════════════════════════════════════════════════════
        // UI ELEMENTS
        // ═══════════════════════════════════════════════════════════════
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);

        if (equalizerDJ != null) equalizerDJ.draw();
        if (clock != null) clock.draw();
        if (battery != null) battery.draw();

        super.draw();
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════
     * 🎨 DIBUJA CAPA CON DEPTH MAP DISPLACEMENT
     * ═══════════════════════════════════════════════════════════════════════
     * @param useCoverMode true para usar aspect ratio (solo fondo), false para fullscreen
     */
    private void drawLayerWithDepth(int colorTexture, int depthTexture, float depthScale, float alpha, boolean useCoverMode) {
        GLES30.glUseProgram(depthShaderProgram);

        // Uniforms
        GLES30.glUniform2f(depthOffsetLoc, offsetX, offsetY);
        GLES30.glUniform1f(depthScaleLoc, depthScale);
        GLES30.glUniform1f(depthAlphaLoc, alpha);

        // Texture unit 0: Color
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, colorTexture);
        GLES30.glUniform1i(depthTextureLoc, 0);

        // Texture unit 1: Depth map
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthTexture);
        GLES30.glUniform1i(depthMapLoc, 1);

        // Vertices
        GLES30.glEnableVertexAttribArray(depthPosLoc);
        GLES30.glVertexAttribPointer(depthPosLoc, 2, GLES30.GL_FLOAT, false, 0, quadVertexBuffer);

        // UV coords: usar cover para fondo, fullscreen para otras capas
        FloatBuffer uvBuffer = useCoverMode ? quadTexCoordBufferCover : quadTexCoordBuffer;
        GLES30.glEnableVertexAttribArray(depthTexCoordLoc);
        GLES30.glVertexAttribPointer(depthTexCoordLoc, 2, GLES30.GL_FLOAT, false, 0, uvBuffer);

        // Draw
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(depthPosLoc);
        GLES30.glDisableVertexAttribArray(depthTexCoordLoc);
    }

    /**
     * Dibuja capa ESTÁTICA (sin ningún movimiento)
     */
    private void drawLayerStatic(int textureId, float alpha) {
        GLES30.glUseProgram(simpleShaderProgram);

        // Sin offset - completamente estático
        GLES30.glUniform2f(simpleOffsetLoc, 0f, 0f);
        GLES30.glUniform1f(simpleAlphaLoc, alpha);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(simpleTextureLoc, 0);

        GLES30.glEnableVertexAttribArray(simplePosLoc);
        GLES30.glVertexAttribPointer(simplePosLoc, 2, GLES30.GL_FLOAT, false, 0, quadVertexBuffer);

        GLES30.glEnableVertexAttribArray(simpleTexCoordLoc);
        GLES30.glVertexAttribPointer(simpleTexCoordLoc, 2, GLES30.GL_FLOAT, false, 0, quadTexCoordBuffer);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(simplePosLoc);
        GLES30.glDisableVertexAttribArray(simpleTexCoordLoc);
    }

    /**
     * Dibuja capa con parallax simple (sin depth map)
     */
    private void drawLayerSimple(int textureId, float parallaxFactor, float alpha) {
        GLES30.glUseProgram(simpleShaderProgram);

        float layerOffsetX = offsetX * parallaxFactor;
        float layerOffsetY = offsetY * parallaxFactor;

        GLES30.glUniform2f(simpleOffsetLoc, layerOffsetX, layerOffsetY);
        GLES30.glUniform1f(simpleAlphaLoc, alpha);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(simpleTextureLoc, 0);

        GLES30.glEnableVertexAttribArray(simplePosLoc);
        GLES30.glVertexAttribPointer(simplePosLoc, 2, GLES30.GL_FLOAT, false, 0, quadVertexBuffer);

        GLES30.glEnableVertexAttribArray(simpleTexCoordLoc);
        GLES30.glVertexAttribPointer(simpleTexCoordLoc, 2, GLES30.GL_FLOAT, false, 0, quadTexCoordBuffer);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(simplePosLoc);
        GLES30.glDisableVertexAttribArray(simpleTexCoordLoc);
    }

    @Override
    protected void releaseSceneResources() {
        Log.d(TAG, "🗑️ Liberando recursos Zelda 3D...");

        // Liberar texturas de color
        int[] colorTextures = {texCielo, texPaisaje, texPiedra, texLink};
        for (int tex : colorTextures) {
            if (tex > 0) {
                GLES30.glDeleteTextures(1, new int[]{tex}, 0);
            }
        }

        // Liberar depth map del fondo
        if (texCieloDepth > 0) {
            GLES30.glDeleteTextures(1, new int[]{texCieloDepth}, 0);
        }

        texCielo = texPaisaje = texPiedra = texLink = -1;
        texCieloDepth = -1;

        // Liberar shaders
        if (depthShaderProgram > 0) {
            GLES30.glDeleteProgram(depthShaderProgram);
            depthShaderProgram = -1;
        }
        if (simpleShaderProgram > 0) {
            GLES30.glDeleteProgram(simpleShaderProgram);
            simpleShaderProgram = -1;
        }

        // Liberar giroscopio
        if (gyroscope != null) {
            gyroscope.stop();
            gyroscope = null;
        }

        // Liberar UI
        if (equalizerDJ != null) {
            equalizerDJ.release();
            equalizerDJ = null;
        }
        if (clock != null) {
            clock.dispose();
            clock = null;
        }
        if (battery != null) {
            battery.dispose();
            battery = null;
        }

        // Liberar Link 3D
        if (link3D != null) {
            link3D.dispose();
            link3D = null;
        }

        Log.d(TAG, "✅ Recursos Zelda 3D liberados");
    }

    @Override
    public void setScreenSize(int width, int height) {
        super.setScreenSize(width, height);
        if (equalizerDJ != null) equalizerDJ.setScreenSize(width, height);
        if (link3D != null) link3D.setScreenSize(width, height);
        // Actualizar aspect ratio para las imágenes
        updateQuadForAspectRatio();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (gyroscope != null) gyroscope.stop();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (gyroscope != null) gyroscope.start();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🎵 MÚSICA
    // ═══════════════════════════════════════════════════════════════════════

    public void updateMusicBands(float[] bands) {
        if (equalizerDJ != null && bands != null && bands.length > 0) {
            equalizerDJ.updateFromBands(bands);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 👆 TOUCH PARA AJUSTAR LINK 3D
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public boolean onTouchEventRaw(android.view.MotionEvent event) {
        if (link3D != null && useLink3D) {
            int action = event.getActionMasked();
            int pointerCount = event.getPointerCount();

            float x1 = event.getX(0);
            float y1 = event.getY(0);

            float x2 = -1, y2 = -1;
            if (pointerCount >= 2) {
                x2 = event.getX(1);
                y2 = event.getY(1);
            }

            link3D.onTouch(action, x1, y1, x2, y2, pointerCount);
            return true;
        }
        return super.onTouchEventRaw(event);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🛠️ UTILIDADES SHADER
    // ═══════════════════════════════════════════════════════════════════════

    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource);

        int program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragmentShader);
        GLES30.glLinkProgram(program);

        return program;
    }

    private int loadShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);
        return shader;
    }
}
