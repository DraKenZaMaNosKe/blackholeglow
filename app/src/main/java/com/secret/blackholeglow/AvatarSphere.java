package com.secret.blackholeglow;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * AvatarSphere - Esfera 3D que muestra el avatar del usuario
 *
 * ✨ Características:
 * - Carga la foto de perfil del usuario y la aplica como textura
 * - Gira y flota en el espacio
 * - Órbita alrededor del sol
 * - Se ve hermoso en el wallpaper
 *
 * 🎨 Uso:
 *   AvatarSphere avatar = new AvatarSphere(context, textureManager, avatarBitmap);
 *   avatar.setCameraController(camera);
 *   avatar.update(deltaTime);
 *   avatar.draw();
 */
public class AvatarSphere implements SceneObject, CameraAware {
    private static final String TAG = "depurar";  // Mismo TAG que SceneRenderer para consistencia

    // Shader program
    private int programId;
    private int aPositionLoc;
    private int aTexCoordLoc;
    private int uMVPLoc;
    private int uTextureLoc;
    private int uTimeLoc;
    private int uUseSolidColorLoc;
    private int uSolidColorLoc;
    private int uAlphaLoc;

    // Geometría
    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;
    private int vertexCount;

    // Textura del avatar
    private int textureId = -1;
    private Bitmap avatarBitmap;
    private Bitmap pendingBitmap = null;  // Bitmap pendiente de subir al GL thread
    private boolean needsTextureUpdate = false;

    // Transform
    private float[] modelMatrix = new float[16];
    private float[] mvpMatrix = new float[16];

    // Órbita
    private float orbitRadius = 1.8f;  // Radio de órbita
    private float orbitSpeed = 0.2f;   // Velocidad de órbita
    private float orbitAngle = 0f;

    // Rotación de la esfera
    private float rotationY = 0f;

    // Tamaño
    private float scale = 0.12f;  // Tamaño del avatar (reducido: más pequeño)

    // Referencias
    private final Context context;
    private final TextureManager textureManager;
    private CameraController camera;
    private float time = 0f;

    // Debug
    private int drawCallCount = 0;

    /**
     * Constructor
     */
    public AvatarSphere(Context context, TextureManager textureManager, Bitmap avatarBitmap) {
        this.context = context;
        this.textureManager = textureManager;
        this.avatarBitmap = avatarBitmap;

        initShader();
        setupGeometry();

        if (avatarBitmap != null) {
            createTextureFromBitmap(avatarBitmap);
        }

        Log.d(TAG, "✓ AvatarSphere creado con avatar de " +
                   (avatarBitmap != null ? avatarBitmap.getWidth() + "x" + avatarBitmap.getHeight() : "null"));
    }

    /**
     * Inicializa el shader
     */
    private void initShader() {
        programId = ShaderUtils.createProgramFromAssets(
                context,
                "shaders/planeta_vertex.glsl",
                "shaders/planeta_fragment.glsl");

        if (programId == 0) {
            Log.e(TAG, "✗ Error creando shader");
            return;
        }

        // Obtener locations
        aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexCoordLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uMVPLoc = GLES20.glGetUniformLocation(programId, "u_MVP");
        uTextureLoc = GLES20.glGetUniformLocation(programId, "u_Texture");
        uTimeLoc = GLES20.glGetUniformLocation(programId, "u_Time");
        uUseSolidColorLoc = GLES20.glGetUniformLocation(programId, "u_UseSolidColor");
        uSolidColorLoc = GLES20.glGetUniformLocation(programId, "u_SolidColor");
        uAlphaLoc = GLES20.glGetUniformLocation(programId, "u_Alpha");

        Log.d(TAG, "[AvatarSphere] ✓ Shader inicializado - programId=" + programId +
                   ", locations: pos=" + aPositionLoc + ", tex=" + aTexCoordLoc +
                   ", MVP=" + uMVPLoc + ", texture=" + uTextureLoc);
    }

    /**
     * Crea una esfera procedural simple
     */
    private void setupGeometry() {
        // Crear esfera de baja resolución (12 segmentos, 8 anillos)
        int segments = 12;
        int rings = 8;

        java.util.ArrayList<Float> vertices = new java.util.ArrayList<>();
        java.util.ArrayList<Float> texCoords = new java.util.ArrayList<>();

        for (int ring = 0; ring < rings; ring++) {
            float theta1 = (float)ring / rings * (float)Math.PI;
            float theta2 = (float)(ring + 1) / rings * (float)Math.PI;

            for (int seg = 0; seg < segments; seg++) {
                float phi1 = (float)seg / segments * 2.0f * (float)Math.PI;
                float phi2 = (float)(seg + 1) / segments * 2.0f * (float)Math.PI;

                // Vértice 1
                float x1 = (float)(Math.sin(theta1) * Math.cos(phi1));
                float y1 = (float)(Math.cos(theta1));
                float z1 = (float)(Math.sin(theta1) * Math.sin(phi1));

                // Vértice 2
                float x2 = (float)(Math.sin(theta1) * Math.cos(phi2));
                float y2 = (float)(Math.cos(theta1));
                float z2 = (float)(Math.sin(theta1) * Math.sin(phi2));

                // Vértice 3
                float x3 = (float)(Math.sin(theta2) * Math.cos(phi2));
                float y3 = (float)(Math.cos(theta2));
                float z3 = (float)(Math.sin(theta2) * Math.sin(phi2));

                // Vértice 4
                float x4 = (float)(Math.sin(theta2) * Math.cos(phi1));
                float y4 = (float)(Math.cos(theta2));
                float z4 = (float)(Math.sin(theta2) * Math.sin(phi1));

                // Triángulo 1
                vertices.add(x1); vertices.add(y1); vertices.add(z1);
                vertices.add(x2); vertices.add(y2); vertices.add(z2);
                vertices.add(x3); vertices.add(y3); vertices.add(z3);

                // Triángulo 2
                vertices.add(x1); vertices.add(y1); vertices.add(z1);
                vertices.add(x3); vertices.add(y3); vertices.add(z3);
                vertices.add(x4); vertices.add(y4); vertices.add(z4);

                // UVs para triángulo 1
                float u1 = (float)seg / segments;
                float u2 = (float)(seg + 1) / segments;
                float v1 = (float)ring / rings;
                float v2 = (float)(ring + 1) / rings;

                texCoords.add(u1); texCoords.add(v1);
                texCoords.add(u2); texCoords.add(v1);
                texCoords.add(u2); texCoords.add(v2);

                // UVs para triángulo 2
                texCoords.add(u1); texCoords.add(v1);
                texCoords.add(u2); texCoords.add(v2);
                texCoords.add(u1); texCoords.add(v2);
            }
        }

        // Convertir a arrays
        vertexCount = vertices.size() / 3;
        float[] vertexArray = new float[vertices.size()];
        float[] texCoordArray = new float[texCoords.size()];

        for (int i = 0; i < vertices.size(); i++) {
            vertexArray[i] = vertices.get(i);
        }
        for (int i = 0; i < texCoords.size(); i++) {
            texCoordArray[i] = texCoords.get(i);
        }

        // Crear buffers
        vertexBuffer = createFloatBuffer(vertexArray);
        texCoordBuffer = createFloatBuffer(texCoordArray);

        Log.d(TAG, "Geometría creada - vértices: " + vertexCount);
    }

    /**
     * Crea un FloatBuffer desde un array
     */
    private FloatBuffer createFloatBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer buffer = bb.asFloatBuffer();
        buffer.put(data);
        buffer.position(0);
        return buffer;
    }

    /**
     * Crea una textura OpenGL desde un Bitmap
     */
    private void createTextureFromBitmap(Bitmap bitmap) {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        textureId = textures[0];

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        // Configurar parámetros de textura
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        // Cargar bitmap a la textura
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        Log.d(TAG, "✓ Textura creada desde bitmap - textureId=" + textureId);
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    @Override
    public void update(float deltaTime) {
        time += deltaTime;

        // Actualizar ángulo de órbita
        orbitAngle += orbitSpeed * deltaTime;

        // Calcular posición en órbita
        float x = (float)(Math.cos(orbitAngle) * orbitRadius);
        float z = (float)(Math.sin(orbitAngle) * orbitRadius);
        float y = 0.65f + (float)(Math.sin(time * 0.5) * 0.08);  // Más arriba, debajo de las barras HP

        // Rotación de la esfera sobre sí misma
        rotationY += deltaTime * 20f;  // 20 grados por segundo

        // Crear matriz de modelo
        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, x, y, z);
        android.opengl.Matrix.rotateM(modelMatrix, 0, rotationY, 0, 1, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, scale, scale, scale);
    }

    @Override
    public void draw() {
        // ⚡ PROCESAR BITMAP PENDIENTE (thread-safe)
        // Si hay un bitmap esperando, crear la textura AQUÍ en el GL thread
        if (needsTextureUpdate && pendingBitmap != null) {
            synchronized (this) {
                if (needsTextureUpdate && pendingBitmap != null) {
                    Log.d(TAG, "🔄 [draw] Procesando bitmap pendiente en GL thread...");

                    // Eliminar textura anterior si existe
                    if (textureId != -1) {
                        int[] textures = {textureId};
                        GLES20.glDeleteTextures(1, textures, 0);
                        Log.d(TAG, "🔄 [draw] Textura anterior eliminada: " + textureId);
                    }

                    // Crear nueva textura desde el bitmap pendiente
                    createTextureFromBitmap(pendingBitmap);
                    avatarBitmap = pendingBitmap;
                    pendingBitmap = null;
                    needsTextureUpdate = false;

                    Log.d(TAG, "✅ [draw] Textura del avatar creada exitosamente - textureId=" + textureId);
                }
            }
        }

        // 🔍 DEBUG: Verificar condiciones para dibujar
        if (camera == null) {
            if (drawCallCount % 120 == 0) {
                Log.w(TAG, "[AvatarSphere] ⚠️ NO DIBUJANDO: camera es null");
            }
            drawCallCount++;
            return;
        }
        if (programId == 0) {
            if (drawCallCount % 120 == 0) {
                Log.w(TAG, "[AvatarSphere] ⚠️ NO DIBUJANDO: programId es 0");
            }
            drawCallCount++;
            return;
        }
        if (textureId == -1) {
            if (drawCallCount % 120 == 0) {
                Log.w(TAG, "[AvatarSphere] ⚠️ NO DIBUJANDO: textureId es -1 (textura no cargada)");
            }
            drawCallCount++;
            return;
        }

        // 🎨 Dibujar el avatar
        GLES20.glUseProgram(programId);

        // Calcular MVP usando CameraController
        camera.computeMvp(modelMatrix, mvpMatrix);

        // Pasar uniforms
        GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, mvpMatrix, 0);
        GLES20.glUniform1f(uTimeLoc, time);

        // Uniforms adicionales requeridos por planeta_fragment.glsl
        GLES20.glUniform1i(uUseSolidColorLoc, 0);  // 0 = usar textura
        GLES20.glUniform4f(uSolidColorLoc, 1.0f, 1.0f, 1.0f, 1.0f);  // blanco (no afecta)
        GLES20.glUniform1f(uAlphaLoc, 1.0f);  // alpha completo

        // Bind textura
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTextureLoc, 0);

        // Pasar atributos
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        texCoordBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aTexCoordLoc);
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        // Dibujar
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        // Deshabilitar atributos
        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aTexCoordLoc);

        // 🔍 DEBUG: Confirmar que se dibujó (solo cada 60 frames para no saturar)
        drawCallCount++;
        if (drawCallCount % 60 == 0) {
            Log.d(TAG, "[AvatarSphere] ✓ Dibujando avatar - textureId=" + textureId +
                       ", pos=(" + String.format("%.2f", modelMatrix[12]) +
                       "," + String.format("%.2f", modelMatrix[13]) +
                       "," + String.format("%.2f", modelMatrix[14]) + ")");
        }
    }

    /**
     * Actualiza el bitmap del avatar (thread-safe)
     * IMPORTANTE: Este método puede ser llamado desde cualquier thread (ej: Glide callback).
     * El bitmap se guarda y la textura OpenGL se crea en el próximo draw() (GL thread).
     */
    public void updateAvatar(Bitmap newBitmap) {
        if (newBitmap != null) {
            Log.d(TAG, "📥 [updateAvatar] Recibiendo nuevo bitmap - tamaño: " +
                       newBitmap.getWidth() + "x" + newBitmap.getHeight());

            // Thread-safe: guardar el bitmap y activar flag
            synchronized (this) {
                this.pendingBitmap = newBitmap;
                this.needsTextureUpdate = true;
            }

            Log.d(TAG, "✓ [updateAvatar] Bitmap guardado, esperando GL thread para crear textura...");
        } else {
            Log.w(TAG, "⚠️ [updateAvatar] Bitmap es null, no se puede actualizar");
        }
    }
}
