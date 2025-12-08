package com.secret.blackholeglow;

import android.opengl.GLES30;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Sistema de explosión de meteoritos SIMPLE
 * Genera fragmentos pequeños que salen disparados en todas direcciones al impactar
 * SIN efectos visuales complejos - solo fragmentos físicos
 */
public class MeteorExplosion {
    private static final String TAG = "depurar";

    // Configuración
    private static final int MAX_FRAGMENTOS = 15;  // Número de fragmentos por explosión
    private static final float DURACION_EXPLOSION = 3.0f;  // Duración total en segundos

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
            float velocidad = 0.8f + (float) (Math.random() * 1.2f);  // 0.8-2.0 unidades/seg

            float velX = dirX * velocidad;
            float velY = dirY * velocidad;
            float velZ = dirZ * velocidad;

            // Tamaño variable de fragmentos (pequeños)
            float tamañoFragmento = tamaño * (0.05f + (float) Math.random() * 0.08f);  // 5%-13% del original

            // Velocidad de rotación aleatoria
            float velocidadRot = 100f + (float) (Math.random() * 200f);  // 100-300 deg/seg

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

        GLES30.glUseProgram(shaderProgram);

        // Habilitar blending normal (sin efectos de brillo)
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Dibujar cada fragmento
        for (Fragmento f : fragmentos) {
            f.draw(camera, shaderProgram, aPositionLoc, uMvpLoc, uColorLoc, uAlphaLoc);
        }

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
        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        uMvpLoc = GLES30.glGetUniformLocation(shaderProgram, "u_MVP");
        uColorLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Color");
        uAlphaLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Alpha");

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

            // Actualizar posición - solo movimiento lineal simple
            posicion[0] += velocidad[0] * deltaTime;
            posicion[1] += velocidad[1] * deltaTime;
            posicion[2] += velocidad[2] * deltaTime;

            // Actualizar rotación
            rotacion += velocidadRotacion * deltaTime;

            // Desaparecer al final de la duración
            if (edad > DURACION_EXPLOSION * 0.7f) {
                float tiempoRestante = DURACION_EXPLOSION - edad;
                alpha = tiempoRestante / (DURACION_EXPLOSION * 0.3f);
                alpha = Math.max(0f, Math.min(1f, alpha));
            }
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
            GLES30.glUniformMatrix4fv(mvpLoc, 1, false, mvpMatrix, 0);
            GLES30.glUniform4fv(colorLoc, 1, color, 0);
            GLES30.glUniform1f(alphaLoc, alpha);

            // Dibujar
            GLES30.glEnableVertexAttribArray(posLoc);
            GLES30.glVertexAttribPointer(posLoc, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
            GLES30.glDisableVertexAttribArray(posLoc);
        }
    }
}
