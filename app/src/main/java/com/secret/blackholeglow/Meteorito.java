package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.util.ProceduralSphere;
import com.secret.blackholeglow.util.ObjLoader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Meteorito individual con física y efectos visuales
 * Parte del sistema de lluvia de meteoritos
 */
public class Meteorito implements SceneObject, CameraAware {
    private static final String TAG = "depurar";

    // Estado del meteorito
    public enum Estado {
        INACTIVO,      // En el pool, esperando ser usado
        CAYENDO,       // Viajando por el espacio
        IMPACTANDO,    // Colisionó, mostrando explosión
        DESVANECIENDO  // Terminando explosión, volviendo al pool
    }

    private Estado estado = Estado.INACTIVO;

    // Posición y movimiento
    private float[] posicion = new float[3];
    private float[] velocidad = new float[3];
    private float[] direccion = new float[3];
    private float velocidadBase;
    private float tamaño;

    // Rotación
    private float rotacionX = 0;
    private float rotacionY = 0;
    private float rotacionZ = 0;
    private float velocidadRotacion;

    // Efectos visuales
    private float brillo = 1.0f;
    private float opacidad = 1.0f;
    private float tiempoVida = 0;
    private float tiempoImpacto = 0;
    private boolean tieneEstela = true;

    // Color del meteorito (puede variar)
    private float[] color = {1.0f, 0.6f, 0.2f, 1.0f}; // Naranja ardiente

    // Referencias
    private CameraController camera;
    private final Context context;
    private final TextureManager textureManager;

    // Estela del meteorito
    private MeteorTrail trail;

    // Sistema de explosión - DESACTIVADO
    // private MeteorExplosion explosion;

    // Modelo 3D (compartido entre todos los meteoritos)
    private static MeteoritoMesh sharedMesh = null;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // Shader program
    private final int programId;
    private final int aPositionLoc;
    private final int aTexCoordLoc;
    private final int uMvpLoc;
    private final int uTextureLoc;     // ✅ NUEVO: Para la textura
    private final int uColorLoc;
    private final int uOpacityLoc;
    private final int uTimeLoc;
    private final int uSpeedLoc;
    private final int uTemperatureLoc;
    private final int uImpactPowerLoc;

    public Meteorito(Context context, TextureManager textureManager) {
        this.context = context;
        this.textureManager = textureManager;

        // Crear mesh compartido si no existe
        if (sharedMesh == null) {
            sharedMesh = new MeteoritoMesh(context);
        }

        // ✅ Crear shader program SIMPLE para mostrar solo la textura (sin efectos)
        programId = ShaderUtils.createProgramFromAssets(context,
            "shaders/meteorito_vertex.glsl",
            "shaders/meteorito_lit_fragment.glsl");

        if (programId == 0) {
            Log.e(TAG, "[Meteorito] Error creando shader program!");
        } else {
            Log.d(TAG, "[Meteorito] Shader creado exitosamente, programId=" + programId);
        }

        // Obtener locations
        aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexCoordLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uMvpLoc = GLES20.glGetUniformLocation(programId, "u_MVP");
        uTextureLoc = GLES20.glGetUniformLocation(programId, "u_Texture");  // ✅ NUEVO
        uColorLoc = GLES20.glGetUniformLocation(programId, "u_Color");
        uOpacityLoc = GLES20.glGetUniformLocation(programId, "u_Opacity");
        uTimeLoc = GLES20.glGetUniformLocation(programId, "u_Time");
        uSpeedLoc = GLES20.glGetUniformLocation(programId, "u_Speed");
        uTemperatureLoc = GLES20.glGetUniformLocation(programId, "u_Temperature");
        uImpactPowerLoc = GLES20.glGetUniformLocation(programId, "u_ImpactPower");

        // Crear estela con tipo aleatorio
        MeteorTrail.TrailType trailType = Math.random() < 0.5 ?
            MeteorTrail.TrailType.FIRE : MeteorTrail.TrailType.PLASMA;
        trail = new MeteorTrail(trailType);
        trail.setContext(context);

        // Asegurar que el shader se inicialice correctamente
        trail.invalidateShader();

        // Sistema de explosión - DESACTIVADO
        // explosion = new MeteorExplosion();

        Log.d(TAG, "[Meteorito] Creado con estela tipo: " + trailType);
        Log.d(TAG, "[Meteorito] Shader locations - Pos:" + aPositionLoc + " Tex:" + aTexCoordLoc +
                   " MVP:" + uMvpLoc + " Color:" + uColorLoc + " Opacity:" + uOpacityLoc);
    }

    /**
     * Activa el meteorito con parámetros específicos
     */
    public void activar(float x, float y, float z, float vx, float vy, float vz, float size) {
        estado = Estado.CAYENDO;

        posicion[0] = x;
        posicion[1] = y;
        posicion[2] = z;

        velocidad[0] = vx;
        velocidad[1] = vy;
        velocidad[2] = vz;

        tamaño = size;
        velocidadBase = (float) Math.sqrt(vx*vx + vy*vy + vz*vz);

        // Rotación aleatoria
        velocidadRotacion = (float) (Math.random() * 200 + 50);

        // Reset efectos
        opacidad = 1.0f;
        brillo = 1.0f;
        tiempoVida = 0;
        tiempoImpacto = 0;

        // ✅ Color blanco (sin tinte) para mostrar textura original del asteroide
        // Dejamos solo la textura, sin efectos de color
        color[0] = 1.0f;  // R = blanco
        color[1] = 1.0f;  // G = blanco
        color[2] = 1.0f;  // B = blanco

        // Limpiar la estela anterior
        trail.clear();

        Log.d(TAG, "[Meteorito] Activado en pos(" + x + "," + y + "," + z + "), vel(" + vx + "," + vy + "," + vz + ")");
    }

    /**
     * Desactiva y devuelve al pool
     */
    public void desactivar() {
        estado = Estado.INACTIVO;
        opacidad = 0;
    }

    /**
     * Inicia la explosión del impacto - DESACTIVADO (solo desaparece)
     */
    public void impactar() {
        if (estado == Estado.CAYENDO) {
            // Desactivar inmediatamente sin explosión
            desactivar();
            Log.d(TAG, "[Meteorito] ¡IMPACTO! Meteorito desaparece sin explosión");
        }
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    @Override
    public void update(float deltaTime) {
        if (estado == Estado.INACTIVO) return;

        tiempoVida += deltaTime;

        switch (estado) {
            case CAYENDO:
                // Actualizar posición
                posicion[0] += velocidad[0] * deltaTime;
                posicion[1] += velocidad[1] * deltaTime;
                posicion[2] += velocidad[2] * deltaTime;

                // Actualizar rotación
                rotacionX += velocidadRotacion * deltaTime;
                rotacionY += velocidadRotacion * deltaTime * 0.7f;
                rotacionZ += velocidadRotacion * deltaTime * 0.3f;

                // Añadir algo de gravedad hacia el centro (0,0,0)
                float distCentro = (float) Math.sqrt(
                    posicion[0] * posicion[0] +
                    posicion[1] * posicion[1] +
                    posicion[2] * posicion[2]
                );

                if (distCentro > 0.1f) {
                    float gravedad = 2.0f / (distCentro * distCentro);
                    velocidad[0] += -posicion[0] * gravedad * deltaTime;
                    velocidad[1] += -posicion[1] * gravedad * deltaTime;
                    velocidad[2] += -posicion[2] * gravedad * deltaTime;
                }

                // Aumentar brillo al acercarse
                brillo = 1.0f + (5.0f - distCentro) * 0.2f;
                brillo = Math.max(1.0f, Math.min(2.0f, brillo));

                // Desactivar si sale muy lejos
                if (distCentro > 20.0f || distCentro < 0.1f) {
                    desactivar();
                }
                break;

            case IMPACTANDO:
                tiempoImpacto += deltaTime;

                // Expansión de la explosión
                tamaño *= (1.0f + deltaTime * 5.0f);

                // Desvanecer
                opacidad = 1.0f - (tiempoImpacto / 0.5f);

                if (tiempoImpacto > 0.5f) {
                    estado = Estado.DESVANECIENDO;
                }
                break;

            case DESVANECIENDO:
                opacidad -= deltaTime * 2.0f;
                if (opacidad <= 0) {
                    desactivar();
                }
                break;
        }

        // Actualizar la estela
        boolean isActive = (estado == Estado.CAYENDO);
        trail.update(deltaTime, posicion[0], posicion[1], posicion[2], tamaño, isActive);

        // Explosión desactivada - los meteoritos solo desaparecen
        // explosion.update(deltaTime);
    }

    @Override
    public void draw() {
        if (estado == Estado.INACTIVO || camera == null) return;

        // Dibujar la estela primero (detrás del meteorito)
        trail.draw(camera);

        // Explosión desactivada - no se dibuja
        // explosion.draw(camera);

        GLES20.glUseProgram(programId);

        // Desactivar culling para ver el meteorito desde todos los ángulos
        GLES20.glDisable(GLES20.GL_CULL_FACE);

        // Habilitar blending para transparencia
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);  // Blending normal

        // Construir matriz modelo
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, posicion[0], posicion[1], posicion[2]);

        // Aplicar escala según estado
        float escalaFinal = tamaño;
        if (estado == Estado.IMPACTANDO) {
            escalaFinal *= (1.0f + tiempoImpacto * 3.0f);
        }
        Matrix.scaleM(modelMatrix, 0, escalaFinal, escalaFinal, escalaFinal);

        // Aplicar rotaciones
        Matrix.rotateM(modelMatrix, 0, rotacionX, 1, 0, 0);
        Matrix.rotateM(modelMatrix, 0, rotacionY, 0, 1, 0);
        Matrix.rotateM(modelMatrix, 0, rotacionZ, 0, 0, 1);

        // Calcular MVP
        camera.computeMvp(modelMatrix, mvpMatrix);
        GLES20.glUniformMatrix4fv(uMvpLoc, 1, false, mvpMatrix, 0);

        // Configurar color y opacidad
        float[] colorFinal = color.clone();
        if (estado == Estado.IMPACTANDO) {
            // Hacer más brillante durante impacto
            colorFinal[0] = Math.min(1.0f, color[0] * 2.0f);
            colorFinal[1] = Math.min(1.0f, color[1] * 2.0f);
            colorFinal[2] = Math.min(1.0f, color[2] * 1.5f);
        }
        GLES20.glUniform4fv(uColorLoc, 1, colorFinal, 0);
        GLES20.glUniform1f(uOpacityLoc, opacidad);
        GLES20.glUniform1f(uTimeLoc, tiempoVida);

        // Nuevos uniforms para efectos mejorados (solo si existen en el shader)
        if (uSpeedLoc >= 0) {
            GLES20.glUniform1f(uSpeedLoc, velocidadBase);
        }

        // Temperatura basada en el tipo de estela
        if (uTemperatureLoc >= 0) {
            float temperature = (trail != null &&
                trail.toString().contains("PLASMA")) ? 0.8f : 0.2f;
            GLES20.glUniform1f(uTemperatureLoc, temperature);
        }

        // Poder de impacto (más alto durante impacto)
        if (uImpactPowerLoc >= 0) {
            float impactPower = estado == Estado.IMPACTANDO ? 2.0f : 1.0f;
            GLES20.glUniform1f(uImpactPowerLoc, impactPower);
        }

        // ✅ BIND TEXTURA DEL ASTEROIDE
        if (uTextureLoc >= 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                textureManager.getTexture(R.drawable.matasteroide));
            GLES20.glUniform1i(uTextureLoc, 0);
        }

        // Dibujar el mesh
        sharedMesh.draw(aPositionLoc, aTexCoordLoc);

        // Restaurar blending normal
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    // Getters para el sistema de colisiones
    public float[] getPosicion() { return posicion; }
    public float getTamaño() { return tamaño; }
    public Estado getEstado() { return estado; }
    public boolean estaActivo() { return estado != Estado.INACTIVO; }

    /**
     * ════════════════════════════════════════════════════════════
     * 🪨 MODELO 3D REALISTA DE ASTEROIDE (OBJ)
     * ════════════════════════════════════════════════════════════
     * Reemplaza esfera procedural por modelo 3D de alta calidad
     * ════════════════════════════════════════════════════════════
     */
    private static class MeteoritoMesh {
        private final FloatBuffer vertexBuffer;
        private final FloatBuffer texCoordBuffer;
        private final IntBuffer indexBuffer;  // INT porque el modelo puede tener >32k vértices
        private final int vertexCount;
        private final int indexCount;

        public MeteoritoMesh(Context context) {
            Log.d(TAG, "[Meteorito] 🪨 Cargando modelo 3D AsteroideRealista.obj...");

            ObjLoader.Mesh mesh = null;
            try {
                mesh = ObjLoader.loadObj(context, "AsteroideRealista.obj");
                Log.d(TAG, "[Meteorito] ✅ Modelo cargado: " + mesh.vertexCount + " vértices, " + mesh.faces.size() + " caras");
            } catch (IOException e) {
                Log.e(TAG, "[Meteorito] ❌ Error cargando modelo 3D, fallback a esfera procedural", e);

                // Fallback a esfera procedural si falla la carga
                ProceduralSphere.Mesh fallbackMesh = ProceduralSphere.generateLowPoly(0.5f);
                vertexBuffer = fallbackMesh.vertexBuffer;
                texCoordBuffer = fallbackMesh.uvBuffer;

                // Convertir ShortBuffer a IntBuffer para compatibilidad
                ShortBuffer shortBuf = fallbackMesh.indexBuffer;
                short[] shortIndices = new short[fallbackMesh.indexCount];
                shortBuf.position(0);
                shortBuf.get(shortIndices);

                int[] intIndices = new int[shortIndices.length];
                for (int i = 0; i < shortIndices.length; i++) {
                    intIndices[i] = shortIndices[i] & 0xFFFF;
                }

                indexBuffer = ByteBuffer.allocateDirect(intIndices.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asIntBuffer();
                indexBuffer.put(intIndices);
                indexBuffer.position(0);

                vertexCount = fallbackMesh.vertexCount;
                indexCount = fallbackMesh.indexCount;

                Log.d(TAG, "[Meteorito] ⚠️ Usando esfera procedural fallback");
                return;
            }

            // Usar buffers del OBJ
            this.vertexBuffer = mesh.vertexBuffer;
            this.texCoordBuffer = mesh.uvBuffer;
            this.vertexCount = mesh.vertexCount;

            // Construir buffer de índices con fan triangulation
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

            // Crear IntBuffer
            ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 4);
            ibb.order(ByteOrder.nativeOrder());
            indexBuffer = ibb.asIntBuffer();
            indexBuffer.put(indices);
            indexBuffer.position(0);

            Log.d(TAG, "[Meteorito] ✓ Modelo 3D cargado - vértices: " + vertexCount +
                       ", índices: " + indexCount);
        }

        public void draw(int positionLoc, int texCoordLoc) {
            GLES20.glEnableVertexAttribArray(positionLoc);
            GLES20.glVertexAttribPointer(positionLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

            if (texCoordLoc >= 0) {
                GLES20.glEnableVertexAttribArray(texCoordLoc);
                GLES20.glVertexAttribPointer(texCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);
            }

            // Dibujar con índices INT (glDrawElements usa GL_UNSIGNED_INT)
            indexBuffer.position(0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_INT, indexBuffer);

            GLES20.glDisableVertexAttribArray(positionLoc);
            if (texCoordLoc >= 0) {
                GLES20.glDisableVertexAttribArray(texCoordLoc);
            }
        }
    }
}