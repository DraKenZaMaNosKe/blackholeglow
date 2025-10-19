package com.secret.blackholeglow;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Sistema de explosión de meteoritos
 * Genera fragmentos que salen expulsados en todas direcciones al impactar
 */
public class MeteorExplosion {
    private static final String TAG = "depurar";

    // Configuración
    private static final int MAX_FRAGMENTOS = 20;  // Número de fragmentos por explosión
    private static final float DURACION_EXPLOSION = 2.0f;  // Duración total en segundos

    // Lista de fragmentos activos
    private final List<Fragmento> fragmentos = new ArrayList<>();

    // Estado
    private boolean activo = false;
    private float tiempo = 0f;

    // Shader program compartido
    private static int shaderProgram = 0;
    private static int aPositionLoc;
    private static int uMvpLoc;
    private static int uColorLoc;
    private static int uAlphaLoc;

    // Buffers para dibujar cuadrados (fragmentos)
    private static FloatBuffer vertexBuffer;

    public MeteorExplosion() {
        // Inicializar shader si no existe
        if (shaderProgram == 0) {
            initShader();
        }

        // Crear fragmentos en el pool
        for (int i = 0; i < MAX_FRAGMENTOS; i++) {
            fragmentos.add(new Fragmento());
        }
    }

    /**
     * Activa la explosión en una posición con tamaño y color específicos
     */
    public void explotar(float x, float y, float z, float tamaño, float[] color) {
        activo = true;
        tiempo = 0f;

        // Activar todos los fragmentos con direcciones aleatorias
        for (Fragmento f : fragmentos) {
            // Dirección aleatoria en todas direcciones (esfera uniforme)
            float theta = (float) (Math.random() * Math.PI * 2);  // 0-360°
            float phi = (float) (Math.acos(2 * Math.random() - 1));  // Distribución uniforme

            // Convertir a coordenadas cartesianas
            float dirX = (float) (Math.sin(phi) * Math.cos(theta));
            float dirY = (float) (Math.sin(phi) * Math.sin(theta));
            float dirZ = (float) Math.cos(phi);

            // Velocidad variable (algunos fragmentos más rápidos que otros)
            float velocidad = 1.5f + (float) (Math.random() * 2.5f);  // 1.5-4.0 unidades/seg

            float velX = dirX * velocidad;
            float velY = dirY * velocidad;
            float velZ = dirZ * velocidad;

            // Tamaño variable de fragmentos (más pequeños que el meteorito original)
            float tamañoFragmento = tamaño * (0.1f + (float) Math.random() * 0.15f);  // 10%-25% del original

            // Velocidad de rotación aleatoria
            float velocidadRot = 200f + (float) (Math.random() * 400f);  // 200-600 deg/seg

            f.activar(x, y, z, velX, velY, velZ, tamañoFragmento, velocidadRot, color);
        }
    }

    /**
     * Actualiza la explosión
     */
    public void update(float deltaTime) {
        if (!activo) return;

        tiempo += deltaTime;

        // Actualizar todos los fragmentos
        for (Fragmento f : fragmentos) {
            f.update(deltaTime);
        }

        // Desactivar cuando termine
        if (tiempo >= DURACION_EXPLOSION) {
            activo = false;
        }
    }

    /**
     * Dibuja la explosión
     */
    public void draw(CameraController camera) {
        if (!activo || camera == null) return;

        GLES20.glUseProgram(shaderProgram);

        // Habilitar blending
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);  // Aditivo para brillo

        // Desactivar depth write (pero mantener depth test)
        GLES20.glDepthMask(false);

        // Dibujar cada fragmento
        for (Fragmento f : fragmentos) {
            f.draw(camera, shaderProgram, aPositionLoc, uMvpLoc, uColorLoc, uAlphaLoc);
        }

        // Restaurar estado
        GLES20.glDepthMask(true);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * @return true si la explosión está activa
     */
    public boolean estaActivo() {
        return activo;
    }

    /**
     * Inicializa el shader compartido
     */
    private static void initShader() {
        // Vertex shader simple
        String vertexShaderCode =
            "uniform mat4 u_MVP;\n" +
            "attribute vec4 a_Position;\n" +
            "void main() {\n" +
            "    gl_Position = u_MVP * a_Position;\n" +
            "}\n";

        // Fragment shader simple con color uniforme
        String fragmentShaderCode =
            "precision mediump float;\n" +
            "uniform vec4 u_Color;\n" +
            "uniform float u_Alpha;\n" +
            "void main() {\n" +
            "    gl_FragColor = vec4(u_Color.rgb, u_Alpha);\n" +
            "}\n";

        shaderProgram = ShaderUtils.createProgram(vertexShaderCode, fragmentShaderCode);

        // Obtener locations
        aPositionLoc = GLES20.glGetAttribLocation(shaderProgram, "a_Position");
        uMvpLoc = GLES20.glGetUniformLocation(shaderProgram, "u_MVP");
        uColorLoc = GLES20.glGetUniformLocation(shaderProgram, "u_Color");
        uAlphaLoc = GLES20.glGetUniformLocation(shaderProgram, "u_Alpha");

        // Crear buffer para un cuadrado simple
        float[] vertices = {
            -0.5f, -0.5f, 0.0f,
             0.5f, -0.5f, 0.0f,
            -0.5f,  0.5f, 0.0f,
             0.5f,  0.5f, 0.0f
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
    }

    /**
     * Clase interna para un fragmento individual
     */
    private static class Fragmento {
        private float[] posicion = new float[3];
        private float[] velocidad = new float[3];
        private float[] color = new float[4];
        private float tamaño;
        private float rotacion = 0f;
        private float velocidadRotacion;
        private float alpha = 1.0f;
        private float edad = 0f;

        private final float[] modelMatrix = new float[16];
        private final float[] mvpMatrix = new float[16];

        public void activar(float x, float y, float z, float vx, float vy, float vz,
                           float tamaño, float velocidadRot, float[] colorBase) {
            posicion[0] = x;
            posicion[1] = y;
            posicion[2] = z;
            velocidad[0] = vx;
            velocidad[1] = vy;
            velocidad[2] = vz;
            this.tamaño = tamaño;
            this.velocidadRotacion = velocidadRot;
            this.rotacion = (float) (Math.random() * 360);
            this.edad = 0f;
            this.alpha = 1.0f;

            // Copiar color con variación
            color[0] = Math.min(1.0f, colorBase[0] + (float) (Math.random() * 0.2f - 0.1f));
            color[1] = Math.min(1.0f, colorBase[1] + (float) (Math.random() * 0.2f - 0.1f));
            color[2] = Math.min(1.0f, colorBase[2] + (float) (Math.random() * 0.2f - 0.1f));
            color[3] = 1.0f;
        }

        public void update(float deltaTime) {
            edad += deltaTime;

            // Actualizar posición con física
            posicion[0] += velocidad[0] * deltaTime;
            posicion[1] += velocidad[1] * deltaTime;
            posicion[2] += velocidad[2] * deltaTime;

            // Aplicar "gravedad" suave hacia abajo
            velocidad[1] -= 0.5f * deltaTime;

            // Fricción del aire (desacelerar)
            velocidad[0] *= 0.98f;
            velocidad[1] *= 0.98f;
            velocidad[2] *= 0.98f;

            // Actualizar rotación
            rotacion += velocidadRotacion * deltaTime;

            // Fade-out GRADUAL
            // Empieza a desaparecer después de 1 segundo, termina en 2 segundos
            if (edad > 1.0f) {
                alpha = 1.0f - ((edad - 1.0f) / 1.0f);  // De 1.0 a 0.0 en el segundo restante
                alpha = Math.max(0f, alpha);
            }

            // Reducir tamaño gradualmente
            tamaño *= 0.99f;
        }

        public void draw(CameraController camera, int program, int posLoc, int mvpLoc,
                        int colorLoc, int alphaLoc) {
            // Construir matriz de modelo
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, posicion[0], posicion[1], posicion[2]);

            // Billboard: hacer que siempre mire a la cámara
            float[] cameraPos = camera.getPosition();
            float dx = cameraPos[0] - posicion[0];
            float dy = cameraPos[1] - posicion[1];
            float dz = cameraPos[2] - posicion[2];
            float angleY = (float) Math.toDegrees(Math.atan2(dx, dz));
            Matrix.rotateM(modelMatrix, 0, -angleY, 0, 1, 0);

            // Rotación en Z (spin del fragmento)
            Matrix.rotateM(modelMatrix, 0, rotacion, 0, 0, 1);

            // Escala
            Matrix.scaleM(modelMatrix, 0, tamaño, tamaño, tamaño);

            // Calcular MVP
            camera.computeMvp(modelMatrix, mvpMatrix);

            // Enviar uniforms
            GLES20.glUniformMatrix4fv(mvpLoc, 1, false, mvpMatrix, 0);
            GLES20.glUniform4fv(colorLoc, 1, color, 0);
            GLES20.glUniform1f(alphaLoc, alpha);

            // Dibujar
            GLES20.glEnableVertexAttribArray(posLoc);
            GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            GLES20.glDisableVertexAttribArray(posLoc);
        }
    }
}
