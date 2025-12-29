# TravelingShip - Documentación Técnica

## Resumen del Sistema

La nave `TravelingShip` es un objeto 3D con efectos de shader avanzados que incluye:
- Patrón de vuelo en figura de 8 (lemniscata)
- Llamas de plasma procedurales en las turbinas
- Efecto Fresnel Rim Light para iluminación cinematográfica
- Banking realista y estabilización de vuelo

---

## 1. Integración Blender - Llamas de Turbina

### Problema Original
Los efectos de llamas como sprites 2D o objetos separados nunca sincronizaban perfectamente con la nave debido a:
- Matrices de transformación separadas
- Errores de punto flotante acumulados
- Perspectiva de cámara diferente

### Solución: Geometría Integrada
```
ANTES (2 objetos):              DESPUÉS (1 objeto):
┌─────────┐   ┌─────┐          ┌─────────────────┐
│  NAVE   │ + │LLAMA│    →     │ NAVE + LLAMAS   │
│ matriz1 │   │matriz2│         │  matriz única   │
└─────────┘   └─────┘          └─────────────────┘
```

### Proceso en Blender
1. Importar `human_interceptor.obj`
2. Crear conos (`Shift+A` → Mesh → Cone)
3. Posicionar detrás de cada turbina
4. Unir todo (`Ctrl+J`)
5. Aplicar transformaciones (`Ctrl+A` → All Transforms)
6. Exportar como `human_interceptor_flames.obj`

### Resultado
- **Archivo**: `human_interceptor_flames.obj`
- **Triángulos**: 3,120
- **Materiales**: 2 (nave + conos)
- **Sincronización**: Perfecta (misma matriz de transformación)

---

## 2. Shader de Llamas Plasma

### Detección de Geometría de Llamas
Los conos de llamas están posicionados en `X > 0.65` del modelo:

```glsl
float flameArea = smoothstep(0.65, 0.75, vPosition.x);
```

### Gradiente de Color Animado
Transición: **Cyan → Naranja → Amarillo**

```glsl
// Distancia normalizada en el cono (0=base, 1=punta)
float dist = clamp((vPosition.x - 0.65) * 2.0, 0.0, 1.0);

// Gradiente sin branches (optimizado)
vec3 flameColor = mix(
    mix(vec3(0.2, 0.8, 1.0), vec3(1.0, 0.5, 0.1), dist * 2.0),      // cyan→naranja
    mix(vec3(1.0, 0.5, 0.1), vec3(1.0, 0.9, 0.3), dist * 2.0 - 1.0), // naranja→amarillo
    step(0.5, dist)  // Selector sin branch
);
```

### Ondulación Animada
Una sola onda combinada para rendimiento:

```glsl
float wave = sin(vPosition.y * 15.0 + vPosition.z * 10.0 + uTime * 8.0) * 0.5 + 0.5;
```

### Transparencia Gradual
```glsl
// Sólido en base, transparente en punta
float flameAlpha = (1.0 - dist * 0.4) * (0.8 + wave * 0.2);
```

---

## 3. Fresnel Rim Light

### Concepto Matemático
El efecto Fresnel describe cómo la luz se refleja en superficies según el ángulo de visión.
En los bordes (ángulo rasante), hay más reflexión.

### Fórmula
```
Fresnel = (1 - |N · V|)^n
```
Donde:
- `N` = Normal de la superficie
- `V` = Dirección hacia la cámara
- `n` = Exponente (controla nitidez del borde)

### Cálculo de Normales sin Datos de Normal
Usamos derivadas parciales para calcular normales aproximadas:

```glsl
// Derivadas de posición en screen space
vec3 dPdx = dFdx(vWorldPos);  // Cambio en X
vec3 dPdy = dFdy(vWorldPos);  // Cambio en Y

// Normal = producto cruz de las derivadas
vec3 normal = normalize(cross(dPdx, dPdy));
```

### Implementación Completa
```glsl
// Calcular normal desde derivadas
vec3 normal = normalize(cross(dFdx(vWorldPos), dFdy(vWorldPos)));

// Dirección hacia la cámara
vec3 viewDir = normalize(uCameraPos - vWorldPos);

// Fresnel con exponente 3 (borde nítido)
float fresnel = pow(1.0 - abs(dot(normal, viewDir)), 3.0);

// Color del rim: naranja/dorado del ambiente
vec3 rimColor = vec3(1.0, 0.6, 0.2) * fresnel * 1.2;

// Aplicar solo a la nave (no a las llamas)
vec3 shipColor = texColor.rgb + rimColor * (1.0 - flameArea);
```

### Resultado Visual
```
    Sin Fresnel:          Con Fresnel:
    ┌────────┐            ╭━━━━━━━━╮
    │  NAVE  │     →     ╱ ░░NAVE░░ ╲
    │        │           │          │
    └────────┘            ╲ ░░░░░░ ╱
                          ╰━━━━━━━━╯
                     Borde brillante naranja/dorado
```

---

## 4. Patrón de Vuelo Figura de 8

### Ecuaciones Paramétricas
La figura de 8 se genera con funciones trigonométricas:

```
X(t) = A · sin(t)           → Oscilación lateral (2 ciclos)
Y(t) = B · sin(t/2)         → Movimiento vertical (1 ciclo)
Z(t) = -C · sin(t/2)        → Profundidad (hacia horizonte)
```

Donde `t` va de `0` a `2π` para completar el patrón.

### Visualización del Patrón
```
         ╭─────╮
        /   ↑   \     ← Subiendo (Y aumenta, Z disminuye)
       │    │    │
        \   │   /
         ╲  │  ╱
          ╲ │ ╱
           ╲│╱        ← CRUCE (centro del 8)
           ╱│╲
          ╱ │ ╲
         ╱  │  ╲
        /   │   \
       │    ↓    │    ← Bajando (Y disminuye, Z aumenta)
        \       /
         ╰─────╯
           ▲
         ORIGEN
```

### Implementación Optimizada
```java
// Cálculos cacheados (evita múltiples Math.sin/cos)
float sinT = (float) Math.sin(t);
float cosT = (float) Math.cos(t);
float sinHalfT = (float) Math.sin(t * 0.5f);
float cosHalfT = (float) Math.cos(t * 0.5f);

// Posición del 8
x = ORIGIN_X + FIGURE_8_WIDTH * sinT;
y = ORIGIN_Y + FIGURE_8_HEIGHT * sinHalfT;
z = ORIGIN_Z - FIGURE_8_DEPTH * sinHalfT;

// Escala según distancia (perspectiva)
scale = ORIGIN_SCALE - (ORIGIN_SCALE - 0.15f) * sinHalfT;
```

### Rotación Automática
La nave gira 180° cuando cambia de subir a bajar:

```java
// Derivada de Y determina dirección
float dy = (FIGURE_8_HEIGHT * 0.5f) * cosHalfT;

// Subiendo → mira al horizonte, Bajando → mira a cámara
targetRotationY = (dy >= 0) ? BASE_ROTATION_Y : BASE_ROTATION_Y + 180f;
```

---

## 5. Banking Realista

### Concepto
Los aviones reales inclinan las alas al girar. Esto se llama "banking" o "roll".

### Implementación
```java
// Derivada de X = velocidad lateral
float dx = FIGURE_8_WIDTH * cosT;

// Roll proporcional a velocidad lateral (hasta 30°)
float rollIntensity = dx * 30f;

// Añadir micro-estabilización (como viento)
float sinStab = (float) Math.sin(time * 4.0f);
rotationZ = 4.0f + rollIntensity + sinStab * 2.5f;

// Pitch basado en si sube o baja
rotationX = dy * 8f + sinStab * 0.5f;
```

---

## 6. Optimizaciones de Rendimiento

### Shader - Antes vs Después

| Aspecto | Antes | Después | Mejora |
|---------|-------|---------|--------|
| Funciones sin() | 6+ | 1 | -83% |
| Branches if/else | 3 | 0 | -100% |
| Instrucciones GPU | ~45 | ~20 | -55% |

### Técnicas Aplicadas

1. **Eliminar branches**: Usar `mix()` + `step()` en lugar de `if/else`
2. **Cachear cálculos**: Calcular `sin(t)` una vez, reutilizar
3. **Reducir ondas**: 3 ondas → 1 onda combinada
4. **Listener de frame**: Evitar try-catch en cada draw()

### CPU - Cálculos Cacheados
```java
// ANTES: 10+ llamadas Math.sin/cos
float drift = Math.sin(time * 0.5f) * 0.7f + Math.sin(time * 0.75f) * 0.2f + ...

// DESPUÉS: 5 llamadas cacheadas
float sinT1 = (float) Math.sin(t1);
float cosT1 = (float) Math.cos(t1);
float drift = sinT1 * 0.8f + (float) Math.sin(t1 * 1.5f) * 0.2f;
```

---

## 7. Estructura de Archivos

```
assets/
├── human_interceptor_flames.obj    # Nave + conos de llamas (3120 tris)
├── human_interceptor_flames.mtl    # Materiales
└── human_interceptor.obj           # Nave original (referencia)

java/com/secret/blackholeglow/
├── TravelingShip.java              # Clase principal (~600 líneas)
├── CameraController.java           # Control de cámara
└── scenes/
    └── LabScene.java               # Escena del portal cósmico
```

---

## 8. Parámetros Configurables

```java
// Dimensiones del patrón de vuelo
FIGURE_8_WIDTH = 0.8f;      // Ancho lateral
FIGURE_8_HEIGHT = 3.5f;     // Altura vertical
FIGURE_8_DEPTH = 4.0f;      // Profundidad (Z)

// Duración aleatoria
FIGURE_8_DURATION_MIN = 20f;  // segundos
FIGURE_8_DURATION_MAX = 35f;

// Hover entre ciclos
HOVER_DURATION_MIN = 4f;
HOVER_DURATION_MAX = 10f;

// Posición de origen
ORIGIN_X = -2.5f;
ORIGIN_Y = -4.9f;
ORIGIN_Z = -2.5f;
ORIGIN_SCALE = 0.8f;

// Rotación base (calibrada por touch)
BASE_ROTATION_Y = 304.7f;
```

---

## 9. Uniforms del Shader

| Uniform | Tipo | Descripción |
|---------|------|-------------|
| `uMVPMatrix` | mat4 | Matriz Model-View-Projection |
| `uModelMatrix` | mat4 | Matriz de modelo (para world pos) |
| `uCameraPos` | vec3 | Posición de cámara (para Fresnel) |
| `uTexture` | sampler2D | Textura de la nave |
| `uTime` | float | Tiempo para animaciones |
| `uEngineGlow` | float | Intensidad del brillo de motores |

---

## 10. Referencias Matemáticas

### Fresnel-Schlick Approximation
```
F(θ) = F₀ + (1 - F₀)(1 - cos(θ))⁵
```
Simplificado para rim light:
```
F(θ) = (1 - |N·V|)ⁿ
```

### Lemniscata (Figura de 8)
Ecuación paramétrica clásica:
```
x = a·sin(t)
y = a·sin(t)·cos(t) = a·sin(2t)/2
```

Adaptada para 3D vertical:
```
x = A·sin(t)
y = B·sin(t/2)
z = C·sin(t/2)
```

### Derivadas para Normales
```
N = normalize(∂P/∂x × ∂P/∂y)
```
En GLSL:
```glsl
vec3 N = normalize(cross(dFdx(P), dFdy(P)));
```

---

*Documentación generada: Diciembre 2024*
*Proyecto: Black Hole Glow - LabScene*
