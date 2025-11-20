package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.util.ObjLoader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

/**
 * 🪨 ASTEROIDE REALISTA
 * Carga el modelo 3D detallado del asteroide desde assets
 */
public class AsteroideRealista extends BaseShaderProgram implements SceneObject, CameraAware {
    private static final String TAG = "AsteroideRealista";

    // Buffers
    private final FloatBuffer vertexBuffer;
    private final FloatBuffer uvBuffer;
    private final IntBuffer indexBuffer;
    private final int indexCount;

    // Uniform locations
    private final int aPosLoc;
    private final int aTexLoc;
    private final int uTexLoc;

    // Textura
    private final int textureId;

    // Camera
    private CameraController camera;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // Estado del asteroide
    public enum Estado {
        INACTIVO,      // En el pool, esperando ser usado
        ACTIVO         // Viajando por el espacio
    }
    private Estado estado = Estado.INACTIVO;

    // Transformaciones
    private float posX = 0f, posY = 0f, posZ = 0f;
    private float scale = 1.0f;
    private float rotationX = 0f;
    private float rotationY = 0f;
    private float rotationZ = 0f;
    private float spinSpeedX = 15.0f;
    private float spinSpeedY = 20.0f;
    private float spinSpeedZ = 10.0f;

    // Movimiento
    private float velocityX = 0f;
    private float velocityY = 0f;
    private float velocityZ = 0f;

    public AsteroideRealista(Context context, TextureManager textureManager) {
        super(context, "shaders/asteroide_vertex.glsl", "shaders/asteroide_textured_fragment.glsl");

        Log.d(TAG, "═══════════════════════════════════════════════");
        Log.d(TAG, "🪨 Cargando Asteroide Realista desde assets");
        Log.d(TAG, "═══════════════════════════════════════════════");

        // Cargar textura (nombre del archivo: matasteroide.png sin 'r')
        int textureResourceId = R.drawable.matasteroide;
        Log.d(TAG, "📦 Intentando cargar textura - Resource ID: " + textureResourceId);
        textureId = textureManager.getTexture(textureResourceId);
        Log.d(TAG, "✓ Textura cargada - Texture ID OpenGL: " + textureId);

        // Cargar modelo OBJ
        ObjLoader.Mesh mesh = null;
        try {
            mesh = ObjLoader.loadObj(context, "AsteroideRealista.obj");
            Log.d(TAG, "✅ Modelo cargado: " + mesh.vertexCount + " vértices, " + mesh.faces.size() + " caras");
        } catch (IOException e) {
            Log.e(TAG, "❌ Error cargando AsteroideRealista.obj", e);
            throw new RuntimeException("No se pudo cargar el modelo del asteroide", e);
        }

        // Usar los buffers ya preparados por ObjLoader
        this.vertexBuffer = mesh.vertexBuffer;
        this.uvBuffer = mesh.uvBuffer;

        // Construir buffer de índices desde las caras usando fan triangulation
        int totalIndices = 0;
        for (int[] face : mesh.faces) {
            // Cada cara con N vértices produce N-2 triángulos
            totalIndices += (face.length - 2) * 3;
        }

        int[] indices = new int[totalIndices];
        int idx = 0;
        for (int[] face : mesh.faces) {
            // Fan triangulation: (v0, v1, v2), (v0, v2, v3), (v0, v3, v4), ...
            int v0 = face[0];
            for (int i = 1; i < face.length - 1; i++) {
                indices[idx++] = v0;
                indices[idx++] = face[i];
                indices[idx++] = face[i + 1];
            }
        }

        this.indexCount = totalIndices;

        // Crear IntBuffer para los índices
        ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 4);
        ibb.order(ByteOrder.nativeOrder());
        indexBuffer = ibb.asIntBuffer();
        indexBuffer.put(indices);
        indexBuffer.position(0);

        // Obtener uniform locations
        aPosLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uTexLoc = GLES20.glGetUniformLocation(programId, "u_Texture");

        Log.d(TAG, "✅ Asteroide Realista inicializado correctamente");
        Log.d(TAG, "   Vértices: " + mesh.vertexCount);
        Log.d(TAG, "   Caras: " + mesh.faces.size());
        Log.d(TAG, "   Índices: " + indexCount);
        Log.d(TAG, "═══════════════════════════════════════════════");
    }

    public void setPosition(float x, float y, float z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setRotation(float x, float y, float z) {
        this.rotationX = x;
        this.rotationY = y;
        this.rotationZ = z;
    }

    public void setSpinSpeed(float speedX, float speedY, float speedZ) {
        this.spinSpeedX = speedX;
        this.spinSpeedY = speedY;
        this.spinSpeedZ = speedZ;
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    /**
     * Activa el asteroide con parámetros específicos (compatible con MeteorShower)
     */
    public void activar(float x, float y, float z, float vx, float vy, float vz, float size) {
        estado = Estado.ACTIVO;

        posX = x;
        posY = y;
        posZ = z;

        velocityX = vx;
        velocityY = vy;
        velocityZ = vz;

        scale = size;

        // Rotación aleatoria
        rotationX = (float)(Math.random() * 360);
        rotationY = (float)(Math.random() * 360);
        rotationZ = (float)(Math.random() * 360);

        spinSpeedX = (float)(Math.random() * 40 + 20);  // 20-60 deg/s
        spinSpeedY = (float)(Math.random() * 40 + 20);
        spinSpeedZ = (float)(Math.random() * 40 + 20);

        Log.d(TAG, "[AsteroideRealista] Activado en pos(" + x + "," + y + "," + z + "), vel(" + vx + "," + vy + "," + vz + ")");
    }

    /**
     * Desactiva y devuelve al pool
     */
    public void desactivar() {
        estado = Estado.INACTIVO;
    }

    /**
     * Inicia el impacto (simplemente desactiva)
     */
    public void impactar() {
        if (estado == Estado.ACTIVO) {
            desactivar();
            Log.d(TAG, "[AsteroideRealista] ¡IMPACTO! Asteroide desaparece");
        }
    }

    // Getters para el sistema de colisiones
    public float[] getPosicion() {
        return new float[]{posX, posY, posZ};
    }

    public float getTamaño() {
        return scale;
    }

    public Estado getEstado() {
        return estado;
    }

    public boolean estaActivo() {
        return estado != Estado.INACTIVO;
    }

    @Override
    public void update(float dt) {
        if (estado == Estado.INACTIVO) return;

        // Actualizar posición con velocidad
        posX += velocityX * dt;
        posY += velocityY * dt;
        posZ += velocityZ * dt;

        // Rotación en múltiples ejes para efecto tumbling realista
        rotationX += spinSpeedX * dt;
        rotationY += spinSpeedY * dt;
        rotationZ += spinSpeedZ * dt;

        if (rotationX > 360f) rotationX -= 360f;
        if (rotationY > 360f) rotationY -= 360f;
        if (rotationZ > 360f) rotationZ -= 360f;

        // Añadir algo de gravedad hacia el centro (0,0,0)
        float distCentro = (float) Math.sqrt(posX * posX + posY * posY + posZ * posZ);

        if (distCentro > 0.1f) {
            float gravedad = 2.0f / (distCentro * distCentro);
            velocityX += -posX * gravedad * dt;
            velocityY += -posY * gravedad * dt;
            velocityZ += -posZ * gravedad * dt;
        }

        // Desactivar si sale muy lejos o llega muy cerca del centro
        if (distCentro > 20.0f || distCentro < 0.1f) {
            desactivar();
        }
    }

    @Override
    public void draw() {
        if (estado == Estado.INACTIVO || camera == null) {
            return;
        }

        GLES20.glUseProgram(programId);

        // Construir matriz de modelo
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, posX, posY, posZ);
        Matrix.rotateM(modelMatrix, 0, rotationX, 1f, 0f, 0f);  // Rotación X
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f);  // Rotación Y
        Matrix.rotateM(modelMatrix, 0, rotationZ, 0f, 0f, 1f);  // Rotación Z
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        // Calcular MVP
        camera.computeMvp(modelMatrix, mvpMatrix);

        // Pasar MVP al vertex shader
        int uMVPLoc = GLES20.glGetUniformLocation(programId, "u_MVP");
        GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, mvpMatrix, 0);

        // Pasar tiempo
        setTime((System.currentTimeMillis() % 60000) / 1000.0f);

        // Pasar alpha (opaco)
        int uAlphaLoc = GLES20.glGetUniformLocation(programId, "u_Alpha");
        GLES20.glUniform1f(uAlphaLoc, 1.0f);

        // Activar textura
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTexLoc, 0);

        // Debug: Log texture binding cada 60 frames (~1 segundo)
        if (System.currentTimeMillis() % 1000 < 17) {
            Log.d(TAG, "🎨 Dibujando asteroide - TextureID: " + textureId + " | uTexLoc: " + uTexLoc);
        }

        // Configurar atributos
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aTexLoc);
        GLES20.glVertexAttribPointer(aTexLoc, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);

        // Dibujar con índices
        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_INT, indexBuffer);

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPosLoc);
        GLES20.glDisableVertexAttribArray(aTexLoc);
    }
}
