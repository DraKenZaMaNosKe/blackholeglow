package com.secret.blackholeglow.scenes;

import android.content.Context;
import android.util.Log;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.TextureManager;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   🐚 OceanPearlScene - Escena del Fondo del Mar                  ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * Una escena tranquila en el fondo del oceano con:
 * - Ostra abierta con perla brillante
 * - Rayos de luz atravesando el agua
 * - Peces nadando suavemente
 * - Burbujas subiendo
 * - Plantas marinas meciendose
 * - Particulas de plancton flotando
 *
 * MOOD: Relajante, misterioso, hermoso
 */
public class OceanPearlScene extends WallpaperScene {

    private static final String TAG = "OceanPearlScene";

    // ═══════════════════════════════════════════════════════════════
    // OBJETOS DE LA ESCENA
    // ═══════════════════════════════════════════════════════════════
    private OceanBackground oceanBackground;
    private OysterWithPearl oysterWithPearl;
    private SunRays sunRays;
    private Fish[] fishes;
    private BubbleSystem bubbles;
    private SeaweedGroup seaweed;
    private PlanktonParticles plankton;

    // ═══════════════════════════════════════════════════════════════
    // CONFIGURACION
    // ═══════════════════════════════════════════════════════════════
    private static final int NUM_FISHES = 3;
    private static final int NUM_BUBBLES = 20;
    private static final int NUM_SEAWEED = 5;

    @Override
    public String getName() {
        return "Ocean Pearl";
    }

    @Override
    public String getDescription() {
        return "Sumérgete en las profundidades del océano. Una perla mágica brilla dentro de una ostra mientras peces danzan entre rayos de luz solar.";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.agujero_negro;  // TODO: Crear preview del oceano
    }

    @Override
    protected void setupScene() {
        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   🐚 CREANDO ESCENA OCEAN PEARL        ║");
        Log.d(TAG, "╚════════════════════════════════════════╝");

        // 1. FONDO OCEANICO - Gradiente azul profundo
        try {
            oceanBackground = new OceanBackground(context);
            addSceneObject(oceanBackground);
            Log.d(TAG, "  ✓ 🌊 Fondo oceánico creado");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando fondo: " + e.getMessage());
        }

        // 2. RAYOS DE LUZ SOLAR - Atraviesan el agua desde arriba
        try {
            sunRays = new SunRays(context);
            addSceneObject(sunRays);
            Log.d(TAG, "  ✓ ☀️ Rayos de luz creados");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando rayos: " + e.getMessage());
        }

        // 3. PARTICULAS DE PLANCTON - Flotan en el agua
        try {
            plankton = new PlanktonParticles(context, 50);  // 50 particulas
            addSceneObject(plankton);
            Log.d(TAG, "  ✓ ✨ Partículas de plancton creadas");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando plancton: " + e.getMessage());
        }

        // 4. ALGAS MARINAS - En el fondo, meciendose
        try {
            seaweed = new SeaweedGroup(context, NUM_SEAWEED);
            addSceneObject(seaweed);
            Log.d(TAG, "  ✓ 🌿 Algas marinas creadas");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando algas: " + e.getMessage());
        }

        // 5. OSTRA CON PERLA - El elemento principal
        try {
            oysterWithPearl = new OysterWithPearl(context, textureManager);
            oysterWithPearl.setPosition(0f, -0.3f, 0f);  // Centro-abajo
            addSceneObject(oysterWithPearl);
            Log.d(TAG, "  ✓ 🐚💎 Ostra con perla creada");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando ostra: " + e.getMessage());
        }

        // 6. PECES - Nadando tranquilamente
        try {
            fishes = new Fish[NUM_FISHES];
            for (int i = 0; i < NUM_FISHES; i++) {
                fishes[i] = new Fish(context, textureManager, i);
                addSceneObject(fishes[i]);
            }
            Log.d(TAG, "  ✓ 🐠 " + NUM_FISHES + " peces creados");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando peces: " + e.getMessage());
        }

        // 7. BURBUJAS - Subiendo lentamente
        try {
            bubbles = new BubbleSystem(context, NUM_BUBBLES);
            addSceneObject(bubbles);
            Log.d(TAG, "  ✓ 🫧 Sistema de burbujas creado");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando burbujas: " + e.getMessage());
        }

        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   ✅ ESCENA OCEAN PEARL LISTA          ║");
        Log.d(TAG, "║   Objetos: " + sceneObjects.size());
        Log.d(TAG, "╚════════════════════════════════════════╝");
    }

    @Override
    protected void releaseSceneResources() {
        Log.d(TAG, "🗑️ Liberando recursos de Ocean Pearl...");

        // Los objetos se liberan automaticamente en WallpaperScene.onDestroy()
        // Aqui solo limpiamos referencias locales
        oceanBackground = null;
        oysterWithPearl = null;
        sunRays = null;
        fishes = null;
        bubbles = null;
        seaweed = null;
        plankton = null;

        Log.d(TAG, "✓ Recursos de Ocean Pearl liberados");
    }

    // ═══════════════════════════════════════════════════════════════
    // CLASES INTERNAS - Objetos de la escena
    // ═══════════════════════════════════════════════════════════════
    // Estas clases estan definidas aqui para simplicidad inicial.
    // Pueden moverse a archivos separados cuando crezcan.
    // ═══════════════════════════════════════════════════════════════

    /**
     * 🌊 Fondo del oceano - Gradiente azul profundo con causticas
     */
    public static class OceanBackground extends BaseOceanObject {
        public OceanBackground(Context context) {
            super(context, "ocean_background");
        }

        @Override
        protected String getVertexShaderSource() {
            return
                "attribute vec4 a_Position;\n" +
                "attribute vec2 a_TexCoord;\n" +
                "varying vec2 v_TexCoord;\n" +
                "void main() {\n" +
                "    gl_Position = a_Position;\n" +
                "    v_TexCoord = a_TexCoord;\n" +
                "}\n";
        }

        @Override
        protected String getFragmentShaderSource() {
            return
                "precision mediump float;\n" +
                "varying vec2 v_TexCoord;\n" +
                "uniform float u_Time;\n" +
                "\n" +
                "void main() {\n" +
                "    vec2 uv = v_TexCoord;\n" +
                "    \n" +
                "    // Gradiente vertical: azul profundo abajo, azul claro arriba\n" +
                "    vec3 deepBlue = vec3(0.0, 0.05, 0.15);\n" +
                "    vec3 lightBlue = vec3(0.0, 0.3, 0.5);\n" +
                "    vec3 baseColor = mix(deepBlue, lightBlue, uv.y);\n" +
                "    \n" +
                "    // Causticas (patron de luz refractada)\n" +
                "    float caustic1 = sin(uv.x * 20.0 + u_Time * 0.5) * sin(uv.y * 15.0 + u_Time * 0.3);\n" +
                "    float caustic2 = sin(uv.x * 15.0 - u_Time * 0.4) * sin(uv.y * 20.0 - u_Time * 0.2);\n" +
                "    float caustics = (caustic1 + caustic2) * 0.5 + 0.5;\n" +
                "    caustics = pow(caustics, 3.0) * 0.15 * uv.y;  // Mas fuerte arriba\n" +
                "    \n" +
                "    vec3 finalColor = baseColor + vec3(caustics * 0.3, caustics * 0.5, caustics * 0.7);\n" +
                "    \n" +
                "    gl_FragColor = vec4(finalColor, 1.0);\n" +
                "}\n";
        }
    }

    /**
     * ☀️ Rayos de luz solar atravesando el agua
     */
    public static class SunRays extends BaseOceanObject {
        public SunRays(Context context) {
            super(context, "sun_rays");
        }

        @Override
        protected String getVertexShaderSource() {
            return
                "attribute vec4 a_Position;\n" +
                "attribute vec2 a_TexCoord;\n" +
                "varying vec2 v_TexCoord;\n" +
                "void main() {\n" +
                "    gl_Position = a_Position;\n" +
                "    v_TexCoord = a_TexCoord;\n" +
                "}\n";
        }

        @Override
        protected String getFragmentShaderSource() {
            return
                "precision mediump float;\n" +
                "varying vec2 v_TexCoord;\n" +
                "uniform float u_Time;\n" +
                "\n" +
                "void main() {\n" +
                "    vec2 uv = v_TexCoord;\n" +
                "    \n" +
                "    // Centro de los rayos (arriba)\n" +
                "    vec2 center = vec2(0.5, 1.2);\n" +
                "    vec2 toCenter = uv - center;\n" +
                "    float angle = atan(toCenter.y, toCenter.x);\n" +
                "    float dist = length(toCenter);\n" +
                "    \n" +
                "    // Rayos radiales\n" +
                "    float rays = sin(angle * 8.0 + u_Time * 0.2) * 0.5 + 0.5;\n" +
                "    rays = pow(rays, 2.0);\n" +
                "    \n" +
                "    // Atenuar con distancia y posicion Y\n" +
                "    float fade = (1.0 - uv.y) * 0.8;  // Mas fuerte arriba\n" +
                "    fade *= smoothstep(1.5, 0.0, dist);  // Atenuar lejos del centro\n" +
                "    \n" +
                "    // Variacion temporal\n" +
                "    float shimmer = sin(u_Time * 0.5 + uv.y * 5.0) * 0.1 + 0.9;\n" +
                "    \n" +
                "    float intensity = rays * fade * shimmer * 0.3;\n" +
                "    \n" +
                "    // Color dorado/amarillo suave\n" +
                "    vec3 rayColor = vec3(1.0, 0.95, 0.7) * intensity;\n" +
                "    \n" +
                "    gl_FragColor = vec4(rayColor, intensity);\n" +
                "}\n";
        }
    }

    /**
     * 🐚💎 Ostra abierta con perla brillante
     */
    public static class OysterWithPearl extends BaseOceanObject {
        private float posX, posY, posZ;
        private float pearlGlow = 0f;

        public OysterWithPearl(Context context, TextureManager tm) {
            super(context, "oyster_pearl");
        }

        public void setPosition(float x, float y, float z) {
            this.posX = x;
            this.posY = y;
            this.posZ = z;
        }

        @Override
        public void update(float deltaTime) {
            super.update(deltaTime);
            // Pulso del brillo de la perla
            pearlGlow = (float)(Math.sin(time * 2.0) * 0.3 + 0.7);
        }

        @Override
        protected String getVertexShaderSource() {
            return
                "attribute vec4 a_Position;\n" +
                "attribute vec2 a_TexCoord;\n" +
                "varying vec2 v_TexCoord;\n" +
                "uniform vec3 u_Position;\n" +
                "void main() {\n" +
                "    vec4 pos = a_Position;\n" +
                "    pos.xy = pos.xy * 0.4 + u_Position.xy;\n" +  // Escala y posicion
                "    gl_Position = pos;\n" +
                "    v_TexCoord = a_TexCoord;\n" +
                "}\n";
        }

        @Override
        protected String getFragmentShaderSource() {
            return
                "precision mediump float;\n" +
                "varying vec2 v_TexCoord;\n" +
                "uniform float u_Time;\n" +
                "uniform float u_PearlGlow;\n" +
                "\n" +
                "void main() {\n" +
                "    vec2 uv = v_TexCoord;\n" +
                "    vec2 center = vec2(0.5, 0.5);\n" +
                "    \n" +
                "    // === CONCHA (forma de ostra abierta) ===\n" +
                "    float shellDist = length((uv - center) * vec2(1.0, 0.6));\n" +
                "    float shell = smoothstep(0.5, 0.48, shellDist);\n" +
                "    \n" +
                "    // Textura de concha (lineas radiales)\n" +
                "    float angle = atan(uv.y - 0.5, uv.x - 0.5);\n" +
                "    float shellLines = sin(angle * 20.0) * 0.5 + 0.5;\n" +
                "    \n" +
                "    vec3 shellColor = mix(\n" +
                "        vec3(0.6, 0.5, 0.4),  // Marron claro\n" +
                "        vec3(0.8, 0.7, 0.6),  // Beige\n" +
                "        shellLines\n" +
                "    );\n" +
                "    \n" +
                "    // Interior nacarado\n" +
                "    float innerDist = length((uv - center) * vec2(1.0, 0.7));\n" +
                "    float inner = smoothstep(0.35, 0.33, innerDist);\n" +
                "    vec3 innerColor = vec3(0.95, 0.9, 0.85);  // Nacar\n" +
                "    float iridescence = sin(angle * 10.0 + u_Time) * 0.1;\n" +
                "    innerColor += vec3(iridescence, iridescence * 0.5, -iridescence * 0.5);\n" +
                "    \n" +
                "    // === PERLA ===\n" +
                "    vec2 pearlCenter = vec2(0.5, 0.45);\n" +
                "    float pearlDist = length(uv - pearlCenter);\n" +
                "    float pearl = smoothstep(0.12, 0.10, pearlDist);\n" +
                "    \n" +
                "    // Gradiente de la perla (esferica)\n" +
                "    vec2 pearlUV = (uv - pearlCenter) / 0.12;\n" +
                "    float pearlShade = 1.0 - length(pearlUV + vec2(-0.3, -0.3)) * 0.5;\n" +
                "    pearlShade = clamp(pearlShade, 0.0, 1.0);\n" +
                "    \n" +
                "    vec3 pearlColor = vec3(0.95, 0.92, 0.88) * pearlShade;\n" +
                "    \n" +
                "    // Brillo especular de la perla\n" +
                "    float specular = pow(max(0.0, 1.0 - length(pearlUV + vec2(-0.4, -0.4))), 8.0);\n" +
                "    pearlColor += vec3(1.0) * specular * u_PearlGlow;\n" +
                "    \n" +
                "    // Resplandor alrededor de la perla\n" +
                "    float glow = smoothstep(0.2, 0.08, pearlDist) * u_PearlGlow * 0.5;\n" +
                "    vec3 glowColor = vec3(1.0, 0.95, 0.8) * glow;\n" +
                "    \n" +
                "    // === COMBINAR ===\n" +
                "    vec3 finalColor = shellColor * shell;\n" +
                "    finalColor = mix(finalColor, innerColor, inner);\n" +
                "    finalColor = mix(finalColor, pearlColor, pearl);\n" +
                "    finalColor += glowColor;\n" +
                "    \n" +
                "    float alpha = max(shell, glow * 0.5);\n" +
                "    \n" +
                "    gl_FragColor = vec4(finalColor, alpha);\n" +
                "}\n";
        }
    }

    /**
     * 🐠 Pez nadando
     */
    public static class Fish extends BaseOceanObject {
        private int fishIndex;
        private float posX, posY;
        private float speed;
        private float amplitude;
        private float phase;
        private float direction = 1f;  // 1 = derecha, -1 = izquierda

        public Fish(Context context, TextureManager tm, int index) {
            super(context, "fish_" + index);
            this.fishIndex = index;

            // Posiciones y velocidades variadas
            switch (index) {
                case 0:
                    posX = -0.8f; posY = 0.3f; speed = 0.15f; amplitude = 0.1f;
                    break;
                case 1:
                    posX = 0.6f; posY = 0.0f; speed = 0.1f; amplitude = 0.15f;
                    direction = -1f;
                    break;
                case 2:
                    posX = 0.0f; posY = -0.5f; speed = 0.12f; amplitude = 0.08f;
                    break;
                default:
                    posX = 0f; posY = 0f; speed = 0.1f; amplitude = 0.1f;
            }
            phase = index * 2.0f;
        }

        @Override
        public void update(float deltaTime) {
            super.update(deltaTime);

            // Movimiento horizontal
            posX += speed * direction * deltaTime;

            // Rebote en los bordes
            if (posX > 1.2f) { posX = 1.2f; direction = -1f; }
            if (posX < -1.2f) { posX = -1.2f; direction = 1f; }

            // Movimiento vertical ondulante
            posY += (float)Math.sin(time * 2.0 + phase) * amplitude * deltaTime;
            posY = Math.max(-0.8f, Math.min(0.6f, posY));  // Limites
        }

        @Override
        protected String getVertexShaderSource() {
            return
                "attribute vec4 a_Position;\n" +
                "attribute vec2 a_TexCoord;\n" +
                "varying vec2 v_TexCoord;\n" +
                "uniform vec2 u_Position;\n" +
                "uniform float u_Direction;\n" +
                "void main() {\n" +
                "    vec4 pos = a_Position;\n" +
                "    pos.x = pos.x * 0.15 * u_Direction + u_Position.x;\n" +
                "    pos.y = pos.y * 0.08 + u_Position.y;\n" +
                "    gl_Position = pos;\n" +
                "    v_TexCoord = a_TexCoord;\n" +
                "    if (u_Direction < 0.0) v_TexCoord.x = 1.0 - v_TexCoord.x;\n" +
                "}\n";
        }

        @Override
        protected String getFragmentShaderSource() {
            return
                "precision mediump float;\n" +
                "varying vec2 v_TexCoord;\n" +
                "uniform float u_Time;\n" +
                "uniform float u_FishIndex;\n" +
                "\n" +
                "void main() {\n" +
                "    vec2 uv = v_TexCoord;\n" +
                "    \n" +
                "    // Forma del pez (elipse con cola)\n" +
                "    vec2 bodyCenter = vec2(0.4, 0.5);\n" +
                "    float bodyDist = length((uv - bodyCenter) * vec2(1.0, 2.0));\n" +
                "    float body = smoothstep(0.35, 0.33, bodyDist);\n" +
                "    \n" +
                "    // Cola (triangulo)\n" +
                "    float tailX = uv.x;\n" +
                "    float tailWidth = (1.0 - tailX) * 0.8;\n" +
                "    float tail = step(0.65, tailX) * step(abs(uv.y - 0.5), tailWidth * 0.3);\n" +
                "    \n" +
                "    // Aleta\n" +
                "    float finDist = length((uv - vec2(0.35, 0.7)) * vec2(1.0, 0.5));\n" +
                "    float fin = smoothstep(0.15, 0.13, finDist);\n" +
                "    \n" +
                "    // Ojo\n" +
                "    float eyeDist = length(uv - vec2(0.25, 0.55));\n" +
                "    float eye = smoothstep(0.04, 0.03, eyeDist);\n" +
                "    \n" +
                "    // Colores segun indice del pez\n" +
                "    vec3 fishColor;\n" +
                "    if (u_FishIndex < 0.5) {\n" +
                "        fishColor = vec3(1.0, 0.5, 0.2);  // Naranja\n" +
                "    } else if (u_FishIndex < 1.5) {\n" +
                "        fishColor = vec3(0.2, 0.6, 1.0);  // Azul\n" +
                "    } else {\n" +
                "        fishColor = vec3(1.0, 1.0, 0.3);  // Amarillo\n" +
                "    }\n" +
                "    \n" +
                "    // Gradiente en el cuerpo\n" +
                "    fishColor *= 0.8 + uv.y * 0.4;\n" +
                "    \n" +
                "    float shape = max(body, max(tail, fin));\n" +
                "    vec3 finalColor = mix(fishColor, vec3(0.0), eye);  // Ojo negro\n" +
                "    \n" +
                "    gl_FragColor = vec4(finalColor, shape);\n" +
                "}\n";
        }
    }

    /**
     * 🫧 Sistema de burbujas
     */
    public static class BubbleSystem extends BaseOceanObject {
        private int bubbleCount;

        public BubbleSystem(Context context, int count) {
            super(context, "bubbles");
            this.bubbleCount = count;
        }

        @Override
        protected String getVertexShaderSource() {
            return
                "attribute vec4 a_Position;\n" +
                "attribute vec2 a_TexCoord;\n" +
                "varying vec2 v_TexCoord;\n" +
                "void main() {\n" +
                "    gl_Position = a_Position;\n" +
                "    v_TexCoord = a_TexCoord;\n" +
                "}\n";
        }

        @Override
        protected String getFragmentShaderSource() {
            return
                "precision mediump float;\n" +
                "varying vec2 v_TexCoord;\n" +
                "uniform float u_Time;\n" +
                "\n" +
                "float random(vec2 st) {\n" +
                "    return fract(sin(dot(st, vec2(12.9898, 78.233))) * 43758.5453);\n" +
                "}\n" +
                "\n" +
                "void main() {\n" +
                "    vec2 uv = v_TexCoord;\n" +
                "    vec3 color = vec3(0.0);\n" +
                "    float alpha = 0.0;\n" +
                "    \n" +
                "    // Generar multiples burbujas\n" +
                "    for (int i = 0; i < 15; i++) {\n" +
                "        float fi = float(i);\n" +
                "        \n" +
                "        // Posicion base de la burbuja\n" +
                "        float bx = random(vec2(fi, 0.0)) * 2.0 - 1.0;\n" +
                "        float by = mod(random(vec2(fi, 1.0)) + u_Time * (0.05 + random(vec2(fi, 2.0)) * 0.05), 2.0) - 1.0;\n" +
                "        \n" +
                "        // Movimiento ondulante horizontal\n" +
                "        bx += sin(by * 3.0 + u_Time + fi) * 0.05;\n" +
                "        \n" +
                "        // Tamano de la burbuja\n" +
                "        float size = 0.02 + random(vec2(fi, 3.0)) * 0.03;\n" +
                "        \n" +
                "        // Distancia al centro de la burbuja\n" +
                "        float dist = length((uv - vec2(bx * 0.5 + 0.5, by * 0.5 + 0.5)) / size);\n" +
                "        \n" +
                "        // Burbuja con borde y reflejo\n" +
                "        float bubble = smoothstep(1.0, 0.8, dist) * (1.0 - smoothstep(0.7, 0.9, dist));\n" +
                "        float highlight = smoothstep(0.5, 0.3, length((uv - vec2(bx * 0.5 + 0.48, by * 0.5 + 0.52)) / size));\n" +
                "        \n" +
                "        color += vec3(0.7, 0.85, 1.0) * bubble * 0.3;\n" +
                "        color += vec3(1.0) * highlight * 0.5;\n" +
                "        alpha += bubble * 0.4;\n" +
                "    }\n" +
                "    \n" +
                "    gl_FragColor = vec4(color, min(alpha, 0.6));\n" +
                "}\n";
        }
    }

    /**
     * 🌿 Grupo de algas marinas
     */
    public static class SeaweedGroup extends BaseOceanObject {
        private int seaweedCount;

        public SeaweedGroup(Context context, int count) {
            super(context, "seaweed");
            this.seaweedCount = count;
        }

        @Override
        protected String getVertexShaderSource() {
            return
                "attribute vec4 a_Position;\n" +
                "attribute vec2 a_TexCoord;\n" +
                "varying vec2 v_TexCoord;\n" +
                "void main() {\n" +
                "    gl_Position = a_Position;\n" +
                "    v_TexCoord = a_TexCoord;\n" +
                "}\n";
        }

        @Override
        protected String getFragmentShaderSource() {
            return
                "precision mediump float;\n" +
                "varying vec2 v_TexCoord;\n" +
                "uniform float u_Time;\n" +
                "\n" +
                "float random(float x) {\n" +
                "    return fract(sin(x * 12.9898) * 43758.5453);\n" +
                "}\n" +
                "\n" +
                "void main() {\n" +
                "    vec2 uv = v_TexCoord;\n" +
                "    vec3 color = vec3(0.0);\n" +
                "    float alpha = 0.0;\n" +
                "    \n" +
                "    // Solo en la parte inferior\n" +
                "    if (uv.y > 0.4) {\n" +
                "        gl_FragColor = vec4(0.0);\n" +
                "        return;\n" +
                "    }\n" +
                "    \n" +
                "    // Generar algas\n" +
                "    for (int i = 0; i < 7; i++) {\n" +
                "        float fi = float(i);\n" +
                "        float baseX = random(fi) * 0.9 + 0.05;\n" +
                "        \n" +
                "        // Ondulacion del alga\n" +
                "        float wave = sin(uv.y * 10.0 + u_Time * 1.5 + fi * 2.0) * (0.4 - uv.y) * 0.15;\n" +
                "        float seaweedX = baseX + wave;\n" +
                "        \n" +
                "        // Forma del alga (linea vertical con grosor variable)\n" +
                "        float width = 0.015 * (1.0 - uv.y * 2.0);  // Mas ancha abajo\n" +
                "        width = max(width, 0.005);\n" +
                "        \n" +
                "        float dist = abs(uv.x - seaweedX);\n" +
                "        float seaweed = smoothstep(width, width * 0.5, dist);\n" +
                "        seaweed *= smoothstep(0.0, 0.05, uv.y);  // Fade hacia abajo\n" +
                "        seaweed *= smoothstep(0.4, 0.35, uv.y);  // Fade hacia arriba\n" +
                "        \n" +
                "        // Color verde con variacion\n" +
                "        vec3 seaweedColor = vec3(0.1, 0.4 + random(fi + 10.0) * 0.2, 0.15);\n" +
                "        \n" +
                "        color += seaweedColor * seaweed;\n" +
                "        alpha = max(alpha, seaweed);\n" +
                "    }\n" +
                "    \n" +
                "    gl_FragColor = vec4(color, alpha * 0.8);\n" +
                "}\n";
        }
    }

    /**
     * ✨ Particulas de plancton flotando
     */
    public static class PlanktonParticles extends BaseOceanObject {
        private int particleCount;

        public PlanktonParticles(Context context, int count) {
            super(context, "plankton");
            this.particleCount = count;
        }

        @Override
        protected String getVertexShaderSource() {
            return
                "attribute vec4 a_Position;\n" +
                "attribute vec2 a_TexCoord;\n" +
                "varying vec2 v_TexCoord;\n" +
                "void main() {\n" +
                "    gl_Position = a_Position;\n" +
                "    v_TexCoord = a_TexCoord;\n" +
                "}\n";
        }

        @Override
        protected String getFragmentShaderSource() {
            return
                "precision mediump float;\n" +
                "varying vec2 v_TexCoord;\n" +
                "uniform float u_Time;\n" +
                "\n" +
                "float random(vec2 st) {\n" +
                "    return fract(sin(dot(st, vec2(12.9898, 78.233))) * 43758.5453);\n" +
                "}\n" +
                "\n" +
                "void main() {\n" +
                "    vec2 uv = v_TexCoord;\n" +
                "    float alpha = 0.0;\n" +
                "    vec3 color = vec3(0.0);\n" +
                "    \n" +
                "    // Muchas particulas pequenas\n" +
                "    for (int i = 0; i < 30; i++) {\n" +
                "        float fi = float(i);\n" +
                "        \n" +
                "        // Posicion con movimiento lento\n" +
                "        float px = random(vec2(fi, 0.0)) + sin(u_Time * 0.1 + fi) * 0.05;\n" +
                "        float py = random(vec2(fi, 1.0)) + sin(u_Time * 0.15 + fi * 0.5) * 0.03;\n" +
                "        px = mod(px, 1.0);\n" +
                "        py = mod(py, 1.0);\n" +
                "        \n" +
                "        float size = 0.003 + random(vec2(fi, 2.0)) * 0.005;\n" +
                "        float dist = length(uv - vec2(px, py));\n" +
                "        \n" +
                "        float particle = smoothstep(size, size * 0.3, dist);\n" +
                "        \n" +
                "        // Parpadeo suave\n" +
                "        float twinkle = sin(u_Time * 2.0 + fi * 3.0) * 0.3 + 0.7;\n" +
                "        particle *= twinkle;\n" +
                "        \n" +
                "        color += vec3(0.8, 0.9, 1.0) * particle;\n" +
                "        alpha += particle * 0.5;\n" +
                "    }\n" +
                "    \n" +
                "    gl_FragColor = vec4(color, min(alpha, 0.4));\n" +
                "}\n";
        }
    }

    /**
     * Clase base para objetos del oceano con shaders simples
     */
    public static abstract class BaseOceanObject implements com.secret.blackholeglow.SceneObject, Disposable {
        protected Context context;
        protected String name;
        protected float time = 0f;
        protected boolean disposed = false;

        // OpenGL
        protected int programId = 0;
        protected int aPositionLoc, aTexCoordLoc;
        protected int uTimeLoc;
        protected java.nio.FloatBuffer vertexBuffer;

        private static final float[] QUAD_VERTICES = {
            // Posicion (x,y)   TexCoord (u,v)
            -1f, -1f,          0f, 0f,
             1f, -1f,          1f, 0f,
             1f,  1f,          1f, 1f,
            -1f, -1f,          0f, 0f,
             1f,  1f,          1f, 1f,
            -1f,  1f,          0f, 1f,
        };

        public BaseOceanObject(Context context, String name) {
            this.context = context;
            this.name = name;
            initGL();
        }

        protected void initGL() {
            // Crear buffer de vertices
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocateDirect(QUAD_VERTICES.length * 4);
            bb.order(java.nio.ByteOrder.nativeOrder());
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(QUAD_VERTICES);
            vertexBuffer.position(0);

            // Compilar shaders
            String vertexSource = getVertexShaderSource();
            String fragmentSource = getFragmentShaderSource();

            int vertexShader = compileShader(android.opengl.GLES30.GL_VERTEX_SHADER, vertexSource);
            int fragmentShader = compileShader(android.opengl.GLES30.GL_FRAGMENT_SHADER, fragmentSource);

            if (vertexShader == 0 || fragmentShader == 0) {
                Log.e("BaseOceanObject", "Error compilando shaders para " + name);
                return;
            }

            programId = android.opengl.GLES30.glCreateProgram();
            android.opengl.GLES30.glAttachShader(programId, vertexShader);
            android.opengl.GLES30.glAttachShader(programId, fragmentShader);
            android.opengl.GLES30.glLinkProgram(programId);

            // Verificar link
            int[] linkStatus = new int[1];
            android.opengl.GLES30.glGetProgramiv(programId, android.opengl.GLES30.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] == 0) {
                Log.e("BaseOceanObject", "Error linking program: " + android.opengl.GLES30.glGetProgramInfoLog(programId));
                android.opengl.GLES30.glDeleteProgram(programId);
                programId = 0;
                return;
            }

            // Obtener locations
            aPositionLoc = android.opengl.GLES30.glGetAttribLocation(programId, "a_Position");
            aTexCoordLoc = android.opengl.GLES30.glGetAttribLocation(programId, "a_TexCoord");
            uTimeLoc = android.opengl.GLES30.glGetUniformLocation(programId, "u_Time");

            // Limpiar shaders (ya estan linkeados)
            android.opengl.GLES30.glDeleteShader(vertexShader);
            android.opengl.GLES30.glDeleteShader(fragmentShader);
        }

        protected int compileShader(int type, String source) {
            int shader = android.opengl.GLES30.glCreateShader(type);
            android.opengl.GLES30.glShaderSource(shader, source);
            android.opengl.GLES30.glCompileShader(shader);

            int[] compiled = new int[1];
            android.opengl.GLES30.glGetShaderiv(shader, android.opengl.GLES30.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e("BaseOceanObject", "Shader compile error (" + name + "): " +
                    android.opengl.GLES30.glGetShaderInfoLog(shader));
                android.opengl.GLES30.glDeleteShader(shader);
                return 0;
            }
            return shader;
        }

        protected abstract String getVertexShaderSource();
        protected abstract String getFragmentShaderSource();

        @Override
        public void update(float deltaTime) {
            time += deltaTime;
            if (time > 1000f) time -= 1000f;  // Evitar overflow
        }

        @Override
        public void draw() {
            if (programId == 0 || disposed) return;

            android.opengl.GLES30.glUseProgram(programId);

            // Habilitar blending
            android.opengl.GLES30.glEnable(android.opengl.GLES30.GL_BLEND);
            android.opengl.GLES30.glBlendFunc(
                android.opengl.GLES30.GL_SRC_ALPHA,
                android.opengl.GLES30.GL_ONE_MINUS_SRC_ALPHA
            );

            // Pasar tiempo
            android.opengl.GLES30.glUniform1f(uTimeLoc, time);

            // Configurar vertices
            vertexBuffer.position(0);
            android.opengl.GLES30.glEnableVertexAttribArray(aPositionLoc);
            android.opengl.GLES30.glVertexAttribPointer(aPositionLoc, 2, android.opengl.GLES30.GL_FLOAT, false, 16, vertexBuffer);

            vertexBuffer.position(2);
            android.opengl.GLES30.glEnableVertexAttribArray(aTexCoordLoc);
            android.opengl.GLES30.glVertexAttribPointer(aTexCoordLoc, 2, android.opengl.GLES30.GL_FLOAT, false, 16, vertexBuffer);

            // Dibujar
            android.opengl.GLES30.glDrawArrays(android.opengl.GLES30.GL_TRIANGLES, 0, 6);

            // Limpiar
            android.opengl.GLES30.glDisableVertexAttribArray(aPositionLoc);
            android.opengl.GLES30.glDisableVertexAttribArray(aTexCoordLoc);
        }

        @Override
        public void dispose() {
            if (disposed) return;

            if (programId != 0) {
                android.opengl.GLES30.glDeleteProgram(programId);
                programId = 0;
            }

            disposed = true;
            Log.d("BaseOceanObject", "Disposed: " + name);
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }
    }
}
