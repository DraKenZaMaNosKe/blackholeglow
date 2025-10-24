package com.secret.blackholeglow;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import com.secret.blackholeglow.util.ProceduralSphere;
import com.secret.blackholeglow.util.TextureConfig;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

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
    private ShortBuffer indexBuffer;
    private int vertexCount;
    private int indexCount;

    // Textura del avatar
    private int textureId = -1;
    private Bitmap avatarBitmap;
    private Bitmap pendingBitmap = null;  // Bitmap pendiente de subir al GL thread
    private boolean needsTextureUpdate = false;

    // Transform
    private float[] modelMatrix = new float[16];
    private float[] mvpMatrix = new float[16];

    // Posición fija (borde izquierdo, EXACTAMENTE alineado con el borde izquierdo de las barras HP)
    private float fixedX = -1.85f;  // Alineado con el BORDE IZQUIERDO de las barras HP (que están en X=0.05)
    private float fixedY = 2.70f;   // Debajo de hpBarForceField (Y=0.87)
    private float fixedZ = 0.10f;    // En el plano frontal

    // Rotación de la esfera (sobre su eje Y, como el sol)
    private float rotationY = 0f;

    // Tamaño
    private float scale = 0.18f;  // Tamaño incrementado del avatar para mejor visibilidad

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
     * ════════════════════════════════════════════════════════════
     * ✅ SHADER SIMPLE PARA AVATAR (SIN EFECTOS)
     * ════════════════════════════════════════════════════════════
     * Usa shaders dedicados que solo muestran la textura fija
     * sin destellos, brillos ni animaciones
     * ════════════════════════════════════════════════════════════
     */
    private void initShader() {
        Log.d(TAG, "[AvatarSphere] Cargando shaders simples para avatar...");

        programId = ShaderUtils.createProgramFromAssets(
                context,
                "shaders/avatar_vertex.glsl",
                "shaders/avatar_fragment.glsl");

        if (programId == 0) {
            Log.e(TAG, "✗ Error creando shader de avatar");
            return;
        }

        // Obtener locations (shader simple solo necesita MVP, texture y alpha)
        aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexCoordLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uMVPLoc = GLES20.glGetUniformLocation(programId, "u_MVP");
        uTextureLoc = GLES20.glGetUniformLocation(programId, "u_Texture");
        uAlphaLoc = GLES20.glGetUniformLocation(programId, "u_Alpha");

        // Ya no usamos estos uniforms (eran del shader de planeta con efectos)
        uTimeLoc = -1;
        uUseSolidColorLoc = -1;
        uSolidColorLoc = -1;

        Log.d(TAG, "[AvatarSphere] ✓ Shader simple de avatar inicializado - programId=" + programId);
        Log.d(TAG, "[AvatarSphere]   Sin efectos, solo textura fija");
    }

    /**
     * ════════════════════════════════════════════════════════════
     * ✅ USAR PROCEDURAL SPHERE HIGH POLY CON CLAMP_TO_EDGE
     * ════════════════════════════════════════════════════════════
     * Para el avatar usamos ProceduralSphere.generateHighPoly() porque:
     *  - Tiene UVs esféricos que cubren 0..1 perfectamente
     *  - Usando GL_CLAMP_TO_EDGE la textura NO se repite
     *  - HighPoly da mejor calidad visual para el avatar del usuario
     *  - Incluye indexBuffer listo para glDrawElements
     * ════════════════════════════════════════════════════════════
     */
    private void setupGeometry() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Log.d(TAG, "✨ Usando ProceduralSphere HighPoly para avatar");
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        ProceduralSphere.Mesh mesh = ProceduralSphere.generateHigh(1.0f);

        vertexBuffer = mesh.vertexBuffer;
        texCoordBuffer = mesh.uvBuffer;
        indexBuffer = mesh.indexBuffer;
        vertexCount = mesh.vertexCount;
        indexCount = mesh.indexCount;

        Log.d(TAG, "✓ Avatar mesh preparada desde ProceduralSphere:");
        Log.d(TAG, "  Vértices: " + vertexCount);
        Log.d(TAG, "  Índices: " + indexCount);
        Log.d(TAG, "  Triángulos: " + (indexCount / 3));
        Log.d(TAG, "  UVs esféricos con CLAMP_TO_EDGE (no repetición)");
    }

    /**
     * Crea una textura OpenGL desde un Bitmap usando TextureConfig
     * ════════════════════════════════════════════════════════════════
     * ✅ REFACTORIZADO: Usa TextureConfig.Type.AVATAR
     * ════════════════════════════════════════════════════════════════
     * La configuración de wrapping y filtering ahora está centralizada
     * en TextureConfig para mantener consistencia en toda la app.
     */
    private void createTextureFromBitmap(Bitmap bitmap) {
        // Generar ID de textura
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        textureId = textures[0];

        // Bind y cargar bitmap
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        // ════════════════════════════════════════════════════════════
        // ✅ CONFIGURAR COMO TEXTURA TIPO AVATAR
        // ════════════════════════════════════════════════════════════
        // Esto aplica: CLAMP_TO_EDGE (no repetir), LINEAR filtering
        TextureConfig.configure(textureId, TextureConfig.Type.AVATAR);

        Log.d(TAG, "✓ Textura avatar creada usando TextureConfig.AVATAR - textureId=" + textureId);
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    @Override
    public void update(float deltaTime) {
        time += deltaTime;

        // Rotación de la esfera sobre su propio eje Y (como el sol, pero más rápido)
        rotationY += deltaTime * 15f;  // 15 grados por segundo (diferente al sol que rota a 3°/seg)

        // Crear matriz de modelo - POSICIÓN FIJA
        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, fixedX, fixedY, fixedZ);
        android.opengl.Matrix.rotateM(modelMatrix, 0, rotationY, 0, 1, 0);  // Solo rotación en Y
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

        // ════════════════════════════════════════════════════════════
        // ✅ DESACTIVAR FACE CULLING (como en Planeta)
        // ════════════════════════════════════════════════════════════
        // Esto evita que se vean "agujeros" o caras transparentes en la esfera
        GLES20.glDisable(GLES20.GL_CULL_FACE);

        // Asegurar depth test activo
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);

        // Habilitar blending para transparencia
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Calcular MVP usando CameraController
        camera.computeMvp(modelMatrix, mvpMatrix);

        // ════════════════════════════════════════════════════════════
        // ✅ UNIFORMS SIMPLES (solo MVP, texture y alpha)
        // ════════════════════════════════════════════════════════════
        GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, mvpMatrix, 0);
        GLES20.glUniform1f(uAlphaLoc, 1.0f);  // alpha completo (opaco)

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

        // ════════════════════════════════════════════════════════════
        // ✅ DIBUJAR CON glDrawElements (usando indexBuffer)
        // ════════════════════════════════════════════════════════════
        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

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
