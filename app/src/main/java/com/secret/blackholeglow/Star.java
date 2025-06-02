package com.secret.blackholeglow;

import android.opengl.GLES20;
import android.util.Log;

import java.util.Random;
import java.nio.FloatBuffer;

/*
╔════════════════════════════════════════════════════════════════════════╗
║                                                                        ║
║      🌟🚀  Star.java – Clase de Estrella Animada  🚀🌟                     ║
║                                                                        ║
║  ASCII Art:                                                             ║
║         .      .       ✦           .       .         .                  ║
║        *        ✦    .     ✦   .        .    ✦        ✦   .             ║
║     .      🌠             *     🌠        *       🌠      .             ║
║        .        ✦    .      ✦      .       ✦     .          .           ║
║                                                                        ║
║   🔍 Descripción General:                                               ║
║   • Representa una estrella individual dentro de la escena 3D.         ║
║   • Gestiona su posición (x,y,z), tamaño, rotación (ángulo), velocidad ║
║     y transparencia (alpha).                                            ║
║   • Puede dibujarse como un punto simple o con textura (si existe).     ║
║   • Se integra en StarField y complementa el fondo túnel de StarTunnel. ║
║                                                                        ║
╚════════════════════════════════════════════════════════════════════════╝
*/

/**
 * Star - Clase que modela y dibuja una estrella flotante en el espacio 3D simulado.
 *
 * 📝 Propósito:
 *   - Mantener atributos de estado: posición (x, y, z), ángulo de rotación, velocidad,
 *     tamaño base y transparencia (alpha).
 *   - Actualizar posición y rotación con el método update(), simulando que las estrellas
 *     emergen del centro y se alejan hacia “el infinito” (z decrece).
 *   - Dibujar la estrella en pantalla, ya sea como un punto simple (modo fallback) o con
 *     una textura animada si textureId es válido.
 *
 * 🎨 Flujo Interno:
 *   1. En el constructor (Star(int textureId) o Star()), se asigna textureId y se invoca reset().
 *   2. reset(): genera aleatoriamente tamaño base (baseSize), ángulo inicial, posición en z y velocidad,
 *      así como alfa (opacidad) para cada estrella.
 *   3. update(deltaTime, tunnelAngle): actualiza la posición en z (z -= speed * deltaTime) y el ángulo
 *      de rotación (angle += deltaTime * 1.5f). Cuando z < 0.1f, invoca reset() para “reaparecer” la estrella.
 *      Luego calcula la proyección en 2D (x, y) según perspectiva y radio.
 *   4. draw(): elige entre drawAsPoint() o drawWithTexture(), dependiendo de textureId.
 *   5. drawAsPoint(): compila (si no está) un shader muy simple que dibuja un gl_Point de color blanco.
 *   6. drawWithTexture(): compila (si no está) StarShader (vertex+fragment), crea un quad centrado en (x,y),
 *      ajusta tamaño en z, configura atributos y dibuja un sprite texturado con glDrawArrays.
 *   7. release(): método estático que libera el programa GL si fue creado (para que no queden residuos al cambiar
 *      contexto).
 *
 * 🔗 Relación con StarTunnelBackground y SceneRenderer:
 *   • StarTunnelBackground no usa directamente Star.java; en cambio, StarField (no mostrado aquí) instancia varias
 *     Stars y las actualiza/dibuja. Así, las estrellas generadas en StarField flotan dentro del túnel renderizado
 *     por StarTunnelBackground.
 *   • SceneRenderer, al agregar un StarField, indirectamente usa Star.java para poblar la escena con estrellas.
 *
 * 📚 Buenas Prácticas:
 *   - Separar la lógica de actualización (update) de la de dibujo (draw).
 *   - Utilizar Random de manera estática (rand) para generar valores no repetitivos.
 *   - Liberar recursos GL con release() cuando se cambian escenas o contextos.
 */
public class Star {
    // ╔════════════════════════════════════════════════════════════════╗
    // ║            🔧 Atributos Estáticos para el Shader GL           ║
    // ╚════════════════════════════════════════════════════════════════╝

    // ID del programa OpenGL que dibuja la estrella (compilado desde shaders)
    private static int program = -1;

    // Ubicaciones de atributos en el shader: posición (a_Position) y coordenadas de textura (a_TexCoord)
    private static int aPositionLocation;
    private static int aTexCoordLocation;

    // Ubicación del uniform de textura (u_Texture) en el shader
    private static int uTextureLocation;

    // ╔════════════════════════════════════════════════════════════════╗
    // ║        Atributos de Instancia: Estado de cada Estrella         ║
    // ╚════════════════════════════════════════════════════════════════╝

    // ID de textura OpenGL (si > 0, dibuja con textura; si 0, dibuja como punto)
    private int textureId;

    // Tamaño base de la estrella, variará en función de la profundidad z
    private float baseSize;

    // Posición 3D: x e y en coordenadas de clip espacio normalizado después de proyección;
    // z representa la “profundidad” o distancia al observador.
    public float x, y, z;

    // Ángulo de rotación (rad) para animar movimiento circular de la estrella
    public float angle;

    // Velocidad con que la estrella se acerca al observador (en unidades de z por segundo)
    public float speed;

    // Transparencia de la estrella (alpha) en [0.2, 0.8]
    public float alpha;

    // Generador de números aleatorios compartido (para valores uniformemente distribuidos)
    private static final Random rand = new Random();

    /**
     * Constructor Star con textura:
     * Asigna textureId y llama a reset() para inicializar posición, velocidad, tamaño y alfa.
     *
     * @param textureId - ID OpenGL de la textura que representa la estrella.
     */
    public Star(int textureId) {
        this.textureId = textureId;
        reset();
    }

    /**
     * Constructor Star sin textura (modo fallback):
     * Asigna textureId=0 (flag de “sin textura”) y llama a reset().
     */
    public Star() {
        this.textureId = 0; // textura desactivada
        reset();
    }

    /**
     * release - Método estático que libera el programa OpenGL si fue creado.
     * Se usa para evitar fugas de recursos cuando cambia el contexto o se destruye la app.
     *
     * ASCII Art:
     *   ╔═════════════════════════════════╗
     *   ║    🚮 Liberando programa GL 🚮   ║
     *   ╚═════════════════════════════════╝
     */
    public static void release() {
        if (program != -1) {
            GLES20.glDeleteProgram(program);
            program = -1;
        }
    }

    /**
     * reset - Inicializa o “reinicia” los valores de la estrella para simular que reaparece
     * en el fondo del túnel:
     *   • baseSize: tamaño aleatorio entre 2.0 y 5.0 unidades.
     *   • angle: ángulo aleatorio en [0, 2π], para giro circular.
     *   • z: distancia inicial aleatoria en [1.0, 11.0], cuanto mayor z, más lejos.
     *   • speed: velocidad de acercamiento aleatoria en [1.0, 3.5].
     *   • alpha: transparencia aleatoria en [0.2, 0.8].
     *
     * ASCII Art:
     *   ╔═════════════════════════════════════════════════╗
     *   ║   🔄 reset(): Reposicionando estrella en túnel   ║
     *   ╚═════════════════════════════════════════════════╝
     *       ▪ baseSize = 2.0 + rand.nextFloat() * 3.0
     *       ▪ angle    = rand.nextFloat() * 2π
     *       ▪ z        = 1.0 + rand.nextFloat() * 10.0
     *       ▪ speed    = 1.0 + rand.nextFloat() * 2.5
     *       ▪ alpha    = 0.2 + rand.nextFloat() * 0.6
     */
    public void reset() {
        baseSize = 2.0f + rand.nextFloat() * 3.0f;
        angle = rand.nextFloat() * (float)(2 * Math.PI);
        z = 1.0f + rand.nextFloat() * 10.0f;
        speed = 1.0f + rand.nextFloat() * 2.5f;
        alpha = 0.2f + rand.nextFloat() * 0.6f;
    }

    /**
     * update - Actualiza la posición y el ángulo de la estrella en función del tiempo y la lógica del túnel.
     *
     * @param deltaTime   - Tiempo transcurrido (en segundos) desde el último frame.
     * @param tunnelAngle - (No usado actualmente, reservado para rotación adicional del túnel).
     *
     * Funcionamiento:
     *   1. Decrementa z según speed*y deltaTime (z -= speed * deltaTime), simulando acercamiento.
     *   2. Incrementa angle en función de deltaTime (angle += deltaTime * 1.5f) para rotación continua.
     *   3. Si z < 0.1f, invoca reset() para “reaparecer” la estrella atrás del túnel.
     *   4. Calcula perspective = 1/z para simular proyección: cuanto menor z, más grande se ve.
     *   5. Calcula radius = 1.5f * (1 - z/20.0f) para definir un radio variable
     *      (estrellas más cerca se alejan más del centro en proyección).
     *   6. Proyecta x = radius*cos(angle)*perspective, y = radius*sin(angle)*perspective,
     *      para obtener coordenadas de clip-space en [-1,1].
     *
     * ASCII Art:
     *   ╔════════════════════════════════════════════════════════╗
     *   ║           🔄 update(deltaTime, tunnelAngle)           ║
     *   ╚════════════════════════════════════════════════════════╝
     *      ┌────────────────────────────────────────────────┐
     *      │ z = z - speed * deltaTime                     │
     *      │ angle = angle + deltaTime * 1.5                │
     *      │ if (z < 0.1) reset();                         │
     *      │ float perspective = 1.0 / z;                   │
     *      │ float radius = 1.5 * (1.0 - z/20.0);            │
     *      │ x = radius * cos(angle) * perspective;         │
     *      │ y = radius * sin(angle) * perspective;         │
     *      └────────────────────────────────────────────────┘
     */
    public void update(float deltaTime, float tunnelAngle) {
        // 1️⃣ Reducir z para acercar la estrella al observador
        z -= speed * deltaTime;
        // 2️⃣ Rotar la estrella alrededor del centro
        angle += deltaTime * 1.5f;

        // 3️⃣ Si la estrella ha “cruzado” el observador, resetearla
        if (z < 0.1f) reset();

        // 4️⃣ Calcular factor de perspectiva (inverso de z)
        float perspective = 1.0f / z;

        // 5️⃣ Calcular radio del “túnel” según distancia z
        float radius = 1.5f * (1.0f - z / 20.0f);

        // 6️⃣ Proyectar posición 2D en pantallas (clip-space en [-1,1])
        x = radius * (float)Math.cos(angle) * perspective;
        y = radius * (float)Math.sin(angle) * perspective;
    }

    /**
     * draw - Dibuja la estrella en pantalla. Elige entre renderizar como punto o con textura.
     *
     * Funcionamiento:
     *   • Si textureId <= 0, usa drawAsPoint() (un punto blanco de tamaño fijo).
     *   • Si textureId > 0 y es una textura válida, usa drawWithTexture(), dibujando un quad
     *     texturado centrado en (x,y), del tamaño apropiado según z. Si la textura no es válida,
     *     cae al modo punto.
     *
     * ASCII Art:
     *   ╔════════════════════════════════════════════════╗
     *   ║               🌠 draw()                       ║
     *   ╚════════════════════════════════════════════════╝
     *      if (textureId <= 0) drawAsPoint();
     *      else drawWithTexture();
     */
    public void draw() {
        if (textureId <= 0) {
            // 🪐 Modo punto si no hay textura asignada
            Log.d("Star", "🌠 Dibujando estrella como PUNTO con textureId=" + textureId);
            drawAsPoint();
        } else {
            // 🌌 Modo textura
            Log.d("Star", "🌠 Dibujando estrella con TEXTURA id=" + textureId);
            drawWithTexture();
        }
    }

    /**
     * drawAsPoint - Dibuja la estrella como un simple gl_Point blanco de tamaño fijo (5.0).
     *
     *   • Si program == -1, compila un shader mínimo que toma a_Position y define gl_PointSize=5.0.
     *   • Obtiene la ubicación del atributo a_Position.
     *   • Crea un FloatBuffer con la posición (x,y,0).
     *   • Habilita y configura el atributo de vértices.
     *   • Llama a glDrawArrays(GL_POINTS, 0, 1).
     *   • Deshabilita el atributo de vértices.
     *
     * ASCII Art:
     *   ╔════════════════════════════════════════════════╗
     *   ║             🔵 drawAsPoint()                   ║
     *   ╚════════════════════════════════════════════════╝
     *       ┌──────────────────────────────────────┐
     *       │ if (program == -1) compile shader     │
     *       │   “attribute vec4 a_Position;”         │
     *       │   “void main(){gl_Position=a_Position; │
     *       │    gl_PointSize=5.0;}”                 │
     *       │   “precision mediump float;”           │
     *       │   “void main(){gl_FragColor=vec4(1);}” │
     *       │ float[] point = {x, y, 0.0f};           │
     *       │ glEnableVertexAttribArray(aPosition);   │
     *       │ glVertexAttribPointer(..., pointBuf);   │
     *       │ glDrawArrays(GL_POINTS, 0, 1);          │
     *       │ glDisableVertexAttribArray(aPosition);  │
     *       └──────────────────────────────────────┘
     */
    private void drawAsPoint() {
        Log.d("Star", "drawAsPoint() - dibujando como punto blanco");
        if (program == -1) {
            program = ShaderUtils.createProgram(
                    "attribute vec4 a_Position;\n" +
                            "void main() {\n" +
                            "    gl_Position = a_Position;\n" +
                            "    gl_PointSize = 5.0;\n" +
                            "}",
                    "precision mediump float;\n" +
                            "void main() {\n" +
                            "    gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);\n" +
                            "}"
            );
            aPositionLocation = GLES20.glGetAttribLocation(program, "a_Position");
        }

        GLES20.glUseProgram(program);

        // Coordenada del punto en clip-space: (x, y, 0)
        float[] point = { x, y, 0f };
        FloatBuffer pointBuffer = ShaderUtils.createFloatBuffer(point);

        // Habilitar y configurar atributo de posición
        GLES20.glEnableVertexAttribArray(aPositionLocation);
        GLES20.glVertexAttribPointer(aPositionLocation, 3, GLES20.GL_FLOAT, false, 0, pointBuffer);

        // Dibujar un solo punto
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);

        // Deshabilitar atributo
        GLES20.glDisableVertexAttribArray(aPositionLocation);
    }

    /**
     * drawWithTexture - Dibuja la estrella usando un sprite texturado.
     *
     * Pasos:
     *   1. Verifica que textureId > 0 y sea un texture válido (glIsTexture). Si no, cae a drawAsPoint().
     *   2. Si program == -1, compila StarShader.VERTEX_SHADER y StarShader.FRAGMENT_SHADER:
     *       • Obtiene ubicaciones de atributos: a_Position, a_TexCoord.
     *       • Obtiene ubicación de uniform: u_Texture.
     *   3. Calcula “size” en función de baseSize y profundidad z:
     *       float size = baseSize * (1.0f / z) * 0.1f;
     *       size = clamp(size, 0.01f, 0.05f);   // limita tamaño visible.
     *   4. Construye un array de “vertices” para un quad centrado en (x, y), en plano Z=0:
     *       { x-size, y-size, 0,   x+size, y-size, 0,   x-size, y+size, 0,   x+size, y+size, 0 }
     *   5. Construye array de “texCoords” en [0,1]: {0,0, 1,0, 0,1, 1,1}.
     *   6. Crea FloatBuffers para vertices y texCoords (ShaderUtils.createFloatBuffer).
     *   7. Habilita atributos y conecta buffers:
     *       • glVertexAttribPointer(aPositionLocation, 3, ... vertexBuffer)
     *       • glVertexAttribPointer(aTexCoordLocation, 2, ... texCoordBuffer)
     *   8. Activa la unidad de textura GL_TEXTURE0, asocia textureId, y pasa uniform u_Texture = 0.
     *   9. Llama a glDrawArrays(GL_TRIANGLE_STRIP, 0, 4) para dibujar dos triángulos formando el quad.
     *  10. Deshabilita atributos aPositionLocation y aTexCoordLocation.
     *
     * ASCII Art:
     *   ╔══════════════════════════════════════════════════════╗
     *   ║            🎨 drawWithTexture(): Sprite 3D           ║
     *   ╚══════════════════════════════════════════════════════╝
     *       ┌──────────────────────────────────────────────────┐
     *       │ if (textureId <=0 || !glIsTexture) drawAsPoint();│
     *       │ if (program == -1) compilar StarShader           │
     *       │ float size = baseSize*(1/z)*0.1; clamp entre      │
     *       │ 0.01 y 0.05                                       │
     *       │ float[] vertices = {x-size, y-size, 0, ...}       │
     *       │ float[] texCoords = {0,0, 1,0, 0,1, 1,1}           │
     *       │ Crear FloatBuffers                                │
     *       │ glEnableVertexAttribArray(aPosition, aTexCoord)   │
     *       │ glVertexAttribPointer(...)                        │
     *       │ glActiveTexture(GL_TEXTURE0); glBindTexture(...);  │
     *       │ glUniform1i(uTextureLocation, 0);                  │
     *       │ glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);             │
     *       │ glDisableVertexAttribArray(aPosition, aTexCoord)   │
     *       └──────────────────────────────────────────────────┘
     */
    private void drawWithTexture() {
        Log.d("Star", "drawWithTexture() - dibujando como textura");
        // Verificar validez de la textura
        if (textureId <= 0 || !GLES20.glIsTexture(textureId)) {
            // Caer al modo punto si no hay textura válida
            Log.w("Star", "⚠️ Textura no válida. textureId=" + textureId + ", glIsTexture=" + GLES20.glIsTexture(textureId));
            drawAsPoint();
            return;
        }

        // Compilar el shader si no existe
        if (program == -1) {
            program = ShaderUtils.createProgram(StarShader.VERTEX_SHADER, StarShader.FRAGMENT_SHADER);
            aPositionLocation  = GLES20.glGetAttribLocation(program, "a_Position");
            aTexCoordLocation  = GLES20.glGetAttribLocation(program, "a_TexCoord");
            uTextureLocation    = GLES20.glGetUniformLocation(program, "u_Texture");
        }

        GLES20.glUseProgram(program);

        // Calcular tamaño final de la estrella (perspectiva / z)
        float size = baseSize * (1.0f / z) * 0.1f;
        // Limitar tamaño para evitar que sea muy pequeño o muy grande
        size = Math.max(0.01f, Math.min(size, 0.05f));

        // Definir vertices de un quad centrado en (x, y), Z=0
        float[] vertices = {
                x - size, y - size, 0f,
                x + size, y - size, 0f,
                x - size, y + size, 0f,
                x + size, y + size, 0f
        };

        // Coordenadas de textura en (u, v) para mapeo de sprite completo
        float[] texCoords = {
                0f, 0f,
                1f, 0f,
                0f, 1f,
                1f, 1f
        };

        // Crear buffers de vértices y texCoords
        FloatBuffer vertexBuffer    = ShaderUtils.createFloatBuffer(vertices);
        FloatBuffer texCoordBuffer  = ShaderUtils.createFloatBuffer(texCoords);

        // Habilitar y configurar atributo a_Position (3 floats)
        GLES20.glEnableVertexAttribArray(aPositionLocation);
        GLES20.glVertexAttribPointer(aPositionLocation, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        // Habilitar y configurar atributo a_TexCoord (2 floats)
        GLES20.glEnableVertexAttribArray(aTexCoordLocation);
        GLES20.glVertexAttribPointer(aTexCoordLocation, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        // Activar y enlazar textura en unidad 0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        // Pasar uniform u_Texture = 0 (GL_TEXTURE0)
        GLES20.glUniform1i(uTextureLocation, 0);

        // Dibujar el quad con GL_TRIANGLE_STRIP (4 vértices → 2 triángulos)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Deshabilitar atributos habilitados
        GLES20.glDisableVertexAttribArray(aPositionLocation);
        GLES20.glDisableVertexAttribArray(aTexCoordLocation);
    }
}
