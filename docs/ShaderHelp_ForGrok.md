# Ayuda para Shader Cosmos - Saint Seiya Scene

## Contexto del Proyecto

**Tipo:** Android Live Wallpaper
**Lenguaje:** Java + GLSL (OpenGL ES 3.0)
**Dispositivo:** Samsung Galaxy (ARM Mali GPU)
**Min SDK:** 24 | **Target SDK:** 35

## El Problema

Estamos creando un fondo animado estilo "cosmos" (nebulosa + estrellas) para un wallpaper de Saint Seiya.

**Lo que funciona:**
- El gradiente radial (dorado centro → púrpura → azul bordes) SE VE BIEN
- La nebulosa animada con sin() funciona

**Lo que NO funciona:**
- Las estrellas NO SE VEN en absoluto
- Hemos probado múltiples técnicas y ninguna muestra las estrellas

## Arquitectura del Sistema

El shader se define como String en Java y se compila en runtime:

```java
// En SaintSeiyaScene.java
private void setupCosmosShader() {
    String vertexShader = "...";
    String fragmentShader = "...";  // <-- El shader que necesita ayuda

    cosmosShaderProgram = createProgram(vertexShader, fragmentShader);
    cosmosPositionLoc = GLES30.glGetAttribLocation(cosmosShaderProgram, "aPosition");
    cosmosTimeLoc = GLES30.glGetUniformLocation(cosmosShaderProgram, "uTime");
    cosmosResolutionLoc = GLES30.glGetUniformLocation(cosmosShaderProgram, "uResolution");
}
```

### Uniforms disponibles:
- `uTime` (float) - Tiempo en segundos, se incrementa cada frame
- `uResolution` (vec2) - Resolución de pantalla (ej: 1080x2400)

### Vertex Shader (funciona bien):
```glsl
attribute vec4 aPosition;
void main() {
    gl_Position = aPosition;
}
```

### Geometría:
- Quad fullscreen: 4 vértices (-1,-1), (1,-1), (-1,1), (1,1)
- Se dibuja con `GL_TRIANGLE_STRIP`

## Código Actual del Fragment Shader

```glsl
precision mediump float;
uniform float uTime;
uniform vec2 uResolution;

float rand(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec2 uv = gl_FragCoord.xy / uResolution.xy;
    vec2 center = uv - 0.5;
    float dist = length(center);

    // Gradiente radial cosmos (FUNCIONA BIEN)
    vec3 gold = vec3(0.4, 0.3, 0.05);
    vec3 purple = vec3(0.15, 0.0, 0.3);
    vec3 blue = vec3(0.0, 0.05, 0.2);
    vec3 bg = mix(gold, purple, smoothstep(0.0, 0.5, dist));
    bg = mix(bg, blue, smoothstep(0.4, 0.8, dist));

    // Nebulosa simple animada (FUNCIONA)
    float nebula = sin(uv.x * 8.0 + uTime * 0.2) * sin(uv.y * 6.0 - uTime * 0.15);
    nebula = nebula * 0.5 + 0.5;
    bg += vec3(0.1, 0.0, 0.15) * nebula * 0.3;

    // ============================================
    // ESTRELLAS - NO SE VEN (NECESITA AYUDA)
    // ============================================
    vec3 starColor = vec3(0.0);

    // Intento actual: estrellas en cada celda con glow
    vec2 cell = floor(uv * 20.0);
    vec2 f = fract(uv * 20.0);
    float r = rand(cell);
    vec2 starPos = vec2(rand(cell + 1.0), rand(cell + 2.0));
    float d = length(f - starPos);
    float brightness = r * r;
    float glow = brightness * 0.015 / (d * d + 0.001);
    float twinkle = 0.7 + 0.3 * sin(uTime * 2.0 + r * 6.28);
    starColor = vec3(1.0, 0.95, 0.8) * glow * twinkle;

    // Segunda capa
    vec2 cell2 = floor(uv * 40.0);
    vec2 f2 = fract(uv * 40.0);
    float r2 = rand(cell2 + 50.0);
    vec2 starPos2 = vec2(rand(cell2 + 51.0), rand(cell2 + 52.0));
    float d2 = length(f2 - starPos2);
    float glow2 = r2 * r2 * r2 * 0.008 / (d2 * d2 + 0.002);
    starColor += vec3(0.8, 0.85, 1.0) * glow2;

    starColor = min(starColor, vec3(1.5));

    gl_FragColor = vec4(bg + starColor, 1.0);
}
```

## Lo que hemos intentado (todo falló):

### 1. Estrellas con step():
```glsl
float star = step(0.98, rand(floor(uv * 50.0)));
// Resultado: No se ven
```

### 2. Estrellas con condicionales:
```glsl
if (rand(cell) > 0.92) {
    starColor += vec3(1.0);
}
// Resultado: No se ven
```

### 3. Simplex noise complejo (ShaderToy):
```glsl
// Código de https://www.shadertoy.com/view/MtB3zW
// Resultado: El celular se trababa mucho (muy pesado)
```

### 4. Glow sin condicionales:
```glsl
float glow = 0.015 / (d * d + 0.001);
// Resultado: No se ven (código actual)
```

## Lo que queremos lograr:

1. **Estrellas visibles** - Puntos brillantes dispersos por el fondo
2. **Efecto twinkle** - Que parpadeen suavemente
3. **Múltiples capas** - Estrellas grandes pocas, pequeñas muchas
4. **Rendimiento** - Debe correr fluido en móvil (sin loops pesados)
5. **Colores cosmos** - Doradas, blancas, algunas azuladas/púrpuras

## Referencia visual deseada:

- Fondo: Gradiente dorado-púrpura-azul (YA FUNCIONA)
- Estrellas: Como el cielo nocturno de Saint Seiya (cosmos)
- Animación: Estrellas que titilan suavemente

## Restricciones técnicas:

1. **OpenGL ES 3.0** (no todas las funciones de desktop OpenGL)
2. **precision mediump float** (no highp en fragment shader para compatibilidad)
3. **Sin loops pesados** - Máximo 3-4 iteraciones
4. **Sin texturas** - Solo matemáticas procedurales
5. **60 FPS target** - Debe ser eficiente

## Pregunta para Grok:

¿Por qué las estrellas no se ven? El gradiente y la nebulosa funcionan perfectamente, pero cualquier técnica de estrellas que probamos no aparece en pantalla.

¿Podrías:
1. Identificar qué está mal en el código de estrellas?
2. Proporcionar un shader de estrellas que funcione en OpenGL ES 3.0?
3. Que sea eficiente para móvil?

## Cómo probar cambios:

El shader se pega directamente en el String de Java:

```java
String fragmentShader =
    "precision mediump float;\n" +
    "uniform float uTime;\n" +
    "uniform vec2 uResolution;\n" +
    // ... resto del shader con \n al final de cada línea
    "}\n";
```

Cada línea debe terminar con `\n` y el string debe concatenarse con `+`.
