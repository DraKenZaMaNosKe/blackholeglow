package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * CosmicNebula - Nube de gas cósmico procedural que atraviesa la escena
 *
 * ☁️ Características:
 * - Genera nebulosas con shaders procedurales (sin textura)
 * - Movimiento lento a través de la escena
 * - Transparencia alfa variable
 * - Colores configurables (azul, púrpura, naranja, rosa)
 * - MUY EFICIENTE: solo 2 triángulos (1 quad)
 *
 * 🎨 Uso:
 *   CosmicNebula nebula = new CosmicNebula(context, color, speed, startX);
 *   nebula.setCameraController(camera);
 *   nebula.update(deltaTime);
 *   nebula.draw();
 */
public class CosmicNebula implements SceneObject, CameraAware {
    private static final String TAG = "depurar";

    // Shader program
    private int programId;
    private int aPositionLoc;
    private int aTexCoordLoc;
    private int uMVPLoc;
    private int uTimeLoc;
    private int uNebulaColorLoc;
    private int uOffsetLoc;

    // Geometría (quad simple)
    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;
    private ShortBuffer indexBuffer;

    // Transform
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // Movimiento
    private float positionX;
    private float positionY;
    private float positionZ;
    private float speed;           // Velocidad de desplazamiento
    private float offset = 0f;     // Desplazamiento acumulado para animación

    // Tamaño del plano - MÁS GRANDE para mejor visibilidad
    private float width = 10.0f;    // Ancho de la nebulosa (era 6.0)
    private float height = 12.0f;   // Alto de la nebulosa (era 8.0)

    // Color
    private float[] nebulaColor;

    // Referencias
    private final Context context;
    private CameraController camera;
    private float time = 0f;

    /**
     * Constructor
     *
     * @param context  Contexto de Android
     * @param color    Color base de la nebulosa [R, G, B] (0.0 - 1.0)
     * @param speed    Velocidad de movimiento (recomendado: 0.1 - 0.5)
     * @param startX   Posición X inicial
     */
    public CosmicNebula(Context context, float[] color, float speed, float startX) {
        this.context = context;
        this.nebulaColor = color;
        this.speed = speed;
        this.positionX = startX;
        this.positionY = 0f;        // Centrada verticalmente
        this.positionZ = -1.5f;     // MÁS CERCA - más visible (era -3f)

        initShader();
        setupGeometry();

        Log.d(TAG, "☁️ CosmicNebula creada - Color: [" +
                   color[0] + "," + color[1] + "," + color[2] + "], Speed: " + speed);
    }

    private void initShader() {
        programId = ShaderUtils.createProgramFromAssets(
                context,
                "shaders/nebula_vertex.glsl",
                "shaders/nebula_fragment.glsl");

        if (programId == 0) {
            Log.e(TAG, "✗ Error creando shader de nebulosa");
            return;
        }

        // Obtener locations
        aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexCoordLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uMVPLoc = GLES20.glGetUniformLocation(programId, "u_MVP");
        uTimeLoc = GLES20.glGetUniformLocation(programId, "u_Time");
        uNebulaColorLoc = GLES20.glGetUniformLocation(programId, "u_NebulaColor");
        uOffsetLoc = GLES20.glGetUniformLocation(programId, "u_Offset");

        Log.d(TAG, "☁️ Shader de nebulosa inicializado - programId=" + programId);
    }

    /**
     * Crear geometría de quad (plano rectangular)
     */
    private void setupGeometry() {
        float halfW = width / 2f;
        float halfH = height / 2f;

        // Vértices del quad (centrado en origen)
        float[] vertices = {
                -halfW, -halfH, 0f,  // Bottom-left
                halfW, -halfH, 0f,  // Bottom-right
                halfW,  halfH, 0f,  // Top-right
                -halfW,  halfH, 0f   // Top-left
        };

        // Coordenadas de textura
        float[] texCoords = {
                0f, 0f,  // Bottom-left
                1f, 0f,  // Bottom-right
                1f, 1f,  // Top-right
                0f, 1f   // Top-left
        };

        // Índices (2 triángulos)
        short[] indices = {
                0, 1, 2,  // Primer triángulo
                0, 2, 3   // Segundo triángulo
        };

        // Crear buffers
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        ByteBuffer tbb = ByteBuffer.allocateDirect(texCoords.length * 4);
        tbb.order(ByteOrder.nativeOrder());
        texCoordBuffer = tbb.asFloatBuffer();
        texCoordBuffer.put(texCoords);
        texCoordBuffer.position(0);

        ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        indexBuffer = ibb.asShortBuffer();
        indexBuffer.put(indices);
        indexBuffer.position(0);

        Log.d(TAG, "☁️ Geometría de nebulosa creada - " + width + "x" + height);
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    @Override
    public void update(float deltaTime) {
        time += deltaTime;

        // Mover la nebulosa lentamente
        positionX += speed * deltaTime;
        offset += deltaTime;

        // Wrapping: cuando sale por la derecha, reaparece por la izquierda
        if (positionX > 12f) {
            positionX = -12f;
            offset = 0f;  // Reset offset para variación visual
        }

        // Construir matriz de modelo
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, positionX, positionY, positionZ);

        // Opcional: rotación muy lenta para más dinamismo
        Matrix.rotateM(modelMatrix, 0, time * 2f, 0, 0, 1);
    }

    // Debug counter
    private int drawCallCount = 0;

    @Override
    public void draw() {
        if (camera == null) {
            if (drawCallCount == 0) {
                Log.e(TAG, "☁️✗ Nebula draw() - camera es null!");
            }
            return;
        }

        if (programId == 0) {
            if (drawCallCount == 0) {
                Log.e(TAG, "☁️✗ Nebula draw() - programId=0 (shader falló)!");
            }
            return;
        }

        // Log cada 60 frames (~1 segundo a 60fps)
        if (drawCallCount % 60 == 0) {
            Log.d(TAG, "☁️ Nebula draw #" + drawCallCount + " - pos(" +
                    String.format("%.1f", positionX) + "," +
                    String.format("%.1f", positionY) + "," +
                    String.format("%.1f", positionZ) + ")");
        }
        drawCallCount++;

        GLES20.glUseProgram(programId);

        // Habilitar blending para transparencia
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Desactivar depth write (para que no oculte objetos detrás)
        GLES20.glDepthMask(false);

        // Calcular MVP
        camera.computeMvp(modelMatrix, mvpMatrix);
        GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, mvpMatrix, 0);

        // Pasar uniforms
        GLES20.glUniform1f(uTimeLoc, time);
        GLES20.glUniform3fv(uNebulaColorLoc, 1, nebulaColor, 0);
        GLES20.glUniform1f(uOffsetLoc, offset);

        // Pasar atributos
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        texCoordBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aTexCoordLoc);
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        // Dibujar
        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        // Cleanup
        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aTexCoordLoc);

        // Restaurar depth write
        GLES20.glDepthMask(true);
    }
}
