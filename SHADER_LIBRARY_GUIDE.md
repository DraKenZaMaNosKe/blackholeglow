# 🌟 Black Hole Glow Shader Library
## Guía Completa de Implementación

**Fecha de creación**: 2025-11-02
**Versión**: 1.0.0
**Proyecto**: Black Hole Glow - Orbix IA

---

## 📋 **ÍNDICE**

1. [¿Qué es esta librería?](#qué-es-esta-librería)
2. [Estructura de archivos](#estructura-de-archivos)
3. [Módulos disponibles](#módulos-disponibles)
4. [Cómo usar en shaders](#cómo-usar-en-shaders)
5. [Ejemplos prácticos](#ejemplos-prácticos)
6. [Optimización para móviles](#optimización-para-móviles)
7. [Próximos pasos](#próximos-pasos)

---

## 🎯 **¿Qué es esta librería?**

Una colección modular de funciones GLSL basada en **"The Book of Shaders"** que permite crear efectos visuales procedurales **impresionantes** sin usar texturas.

### **Ventajas:**
- ✅ **0 texturas** = APK más ligero
- ✅ **100% GPU** = Rendimiento máximo
- ✅ **Infinitamente escalable** = Sin pixelación
- ✅ **Fácil de animar** = Todo es matemática
- ✅ **Modular** = Combina funciones como LEGO

### **Inspiración:**
- The Book of Shaders (Patricio Gonzalez Vivo)
- Inigo Quilez (Shadertoy)
- GPU Gems (NVIDIA)

---

## 📂 **Estructura de Archivos**

```
app/src/main/assets/shaders/
├── lib/
│   ├── README.md              # Documentación de la librería
│   ├── core.glsl              # Funciones base (random, noise, fbm)
│   ├── color.glsl             # HSB, YUV, paletas
│   ├── shapes.glsl            # Distance fields (círculos, polígonos)
│   └── effects.glsl           # Cellular noise, patterns, efectos
├── ocean_deep_vertex.glsl     # Ejemplo: Océano profundo (vertex)
├── ocean_deep_fragment.glsl   # Ejemplo: Océano profundo (fragment)
├── demo_library_vertex.glsl   # Demo de la librería (vertex)
└── demo_library_fragment.glsl # Demo de la librería (fragment)
```

---

## 📚 **Módulos Disponibles**

### **1. core.glsl** - Fundamentos
| Función | Descripción | Costo |
|---------|-------------|-------|
| `random(vec2)` | Ruido pseudo-aleatorio | Bajo |
| `noise(vec2)` | Ruido suave (value noise) | Medio |
| `fbm(vec2, int)` | Ruido fractal multi-octava | Alto |
| `map()` | Mapear valor entre rangos | Bajo |
| `smoothPulse()` | Pulso suave (respiración) | Bajo |

### **2. color.glsl** - Sistemas de Color
| Función | Descripción | Costo |
|---------|-------------|-------|
| `hsb2rgb(vec3)` | HSB → RGB | Bajo |
| `rgb2hsb(vec3)` | RGB → HSB | Bajo |
| `yuv2rgb(vec3)` | YUV → RGB (cinematográfico) | Bajo |
| `palette()` | Paletas procedurales (Inigo Quilez) | Bajo |
| `adjustSaturation()` | Ajustar saturación | Bajo |

### **3. shapes.glsl** - Formas Geométricas
| Función | Descripción | Costo |
|---------|-------------|-------|
| `sdCircle()` | Círculo perfecto (SDF) | Bajo |
| `sdBox()` | Rectángulo (SDF) | Bajo |
| `sdPolygon()` | Polígono N-lados | Medio |
| `sdStar()` | Estrella procedural | Medio |
| `rotate2d()` | Rotación 2D | Bajo |
| `opSmoothUnion()` | Unión suave de formas | Medio |

### **4. effects.glsl** - Efectos Avanzados
| Función | Descripción | Costo |
|---------|-------------|-------|
| `cellularNoise()` | Ruido celular (burbujas, células) | **ALTO** |
| `cellularNoise2()` | Cellular con 2 distancias (bordes) | **ALTO** |
| `gridPattern()` | Cuadrícula procedural | Bajo |
| `starfield()` | Campo de estrellas aleatorio | Medio |
| `radialWaves()` | Ondas radiales concéntricas | Bajo |

---

## 💻 **Cómo Usar en Shaders**

### **Método 1: Copiar funciones directamente**

```glsl
// En tu fragment shader, copiar las funciones que necesites:

// De core.glsl
float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

// De color.glsl
vec3 hsb2rgb(vec3 c) {
    vec3 rgb = clamp(abs(mod(c.x * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);
    rgb = rgb * rgb * (3.0 - 2.0 * rgb);
    return c.z * mix(vec3(1.0), rgb, c.y);
}

// Luego usar en main()
void main() {
    vec2 st = gl_FragCoord.xy / u_Resolution.xy;
    vec3 color = hsb2rgb(vec3(st.x, 0.8, 0.9));
    gl_FragColor = vec4(color, 1.0);
}
```

### **Método 2: Usar preprocesador (futuro)**
```glsl
#include "lib/core.glsl"
#include "lib/color.glsl"
```
*(Requiere configurar sistema de preprocesamiento)*

---

## 🎨 **Ejemplos Prácticos**

### **Ejemplo 1: Arcoíris Animado (HSB)**
```glsl
vec2 st = gl_FragCoord.xy / u_Resolution.xy;
vec3 rainbow = hsb2rgb(vec3(st.x + u_Time * 0.1, 0.8, 0.9));
gl_FragColor = vec4(rainbow, 1.0);
```

### **Ejemplo 2: Burbujas Orgánicas (Cellular Noise)**
```glsl
vec2 st = gl_FragCoord.xy / u_Resolution.xy;
float cells = cellularNoise(st * 5.0 + u_Time * 0.1, 5.0);
vec3 color = hsb2rgb(vec3(0.55 + cells * 0.1, 0.7, 0.8));
gl_FragColor = vec4(color, 1.0);
```

### **Ejemplo 3: Hexágono Rotante (Shapes)**
```glsl
vec2 st = gl_FragCoord.xy / u_Resolution.xy - 0.5;
st = rotate2d(u_Time) * st;
float hex = sdPolygon(st, 6);
float shape = smoothstep(0.01, 0.0, hex);
vec3 color = hsb2rgb(vec3(u_Time * 0.1, 0.8, shape));
gl_FragColor = vec4(color, 1.0);
```

### **Ejemplo 4: Paleta Procedural (Inigo Quilez)**
```glsl
vec2 st = gl_FragCoord.xy / u_Resolution.xy - 0.5;
float t = length(st) + u_Time * 0.2;
vec3 color = palette(t,
    vec3(0.5), vec3(0.5), vec3(1.0), vec3(0.0, 0.33, 0.67)
);
gl_FragColor = vec4(color, 1.0);
```

---

## ⚡ **Optimización para Móviles**

### **DO ✅**
- Usar `precision mediump float` (no `highp`)
- Limitar bucles `for` a 3-5 iteraciones
- Cellular noise: escala 3-8 (no más de 10)
- FBM: 3-4 octavas máximo
- Cachear cálculos costosos en variables

### **DON'T ❌**
- NO usar `cellularNoise()` en múltiples capas
- NO hacer bucles dinámicos (usar constantes)
- NO abusar de `smoothstep()` innecesario
- NO mezclar muchos efectos en un solo shader

### **Tabla de Rendimiento**
| Efecto | FPS Esperado | Uso Recomendado |
|--------|--------------|-----------------|
| HSB colors | 60fps | ✅ Siempre |
| Distance fields | 60fps | ✅ Siempre |
| Noise básico | 60fps | ✅ Siempre |
| Cellular noise | 45-60fps | ⚠️ Con moderación |
| FBM (4 octavas) | 50fps | ⚠️ Solo cuando sea necesario |
| Cellular + FBM | 30-40fps | ❌ Evitar combinación |

---

## 🚀 **Próximos Pasos**

### **Fase 1: Implementación Actual** ✅
- [x] Crear estructura de librería
- [x] Implementar core.glsl
- [x] Implementar color.glsl
- [x] Implementar shapes.glsl
- [x] Implementar effects.glsl
- [x] Crear shader demo
- [x] Crear ejemplo: Océano Profundo

### **Fase 2: Integración con Wallpapers** (Siguiente)
- [ ] Actualizar wallpaper "Bosque Encantado" (luciérnagas con starfield)
- [ ] Actualizar "Neo Tokyo 2099" (grid cyberpunk + neon)
- [ ] Crear "Cellular Dreams" (nuevo wallpaper)
- [ ] Crear "Polar Mandala" (nuevo wallpaper)

### **Fase 3: Optimización Avanzada** (Futuro)
- [ ] Sistema de preprocesador para #include
- [ ] Versiones "lite" de funciones costosas
- [ ] Shader analyzer para detectar cuellos de botella
- [ ] LOD system (Level of Detail) para shaders

### **Fase 4: Expansión** (Largo plazo)
- [ ] Módulo 3D (transformaciones 3D, lighting)
- [ ] Módulo de física (fluidos, partículas)
- [ ] Módulo de post-processing (bloom, blur)
- [ ] Port a Unity para futuros juegos

---

## 📖 **Referencias y Recursos**

### **Libros y Tutoriales**
- [The Book of Shaders](https://thebookofshaders.com/) - Patricio Gonzalez Vivo
- [Inigo Quilez Articles](https://iquilezles.org/articles/) - Técnicas avanzadas
- [Shadertoy](https://www.shadertoy.com/) - Inspiración y ejemplos

### **Funciones Específicas**
- [Distance Functions 2D](https://iquilezles.org/articles/distfunctions2d/)
- [Palette Generator](https://iquilezles.org/articles/palettes/)
- [Cellular Noise](https://thebookofshaders.com/12/)

### **Herramientas**
- [GLSL Sandbox](http://glslsandbox.com/) - Probar shaders online
- [ShaderToy](https://www.shadertoy.com/) - Compartir y explorar
- [LYGIA Shader Library](https://lygia.xyz/) - Más funciones

---

## 🎯 **Casos de Uso Recomendados**

### **Para Fondos de Pantalla:**
1. **Océano/Agua** → Cellular noise + HSB azules
2. **Espacio** → Starfield + noise para nebulosas
3. **Cyberpunk** → Grid + paletas neón
4. **Orgánico** → Cellular noise + formas suaves
5. **Geométrico** → Distance fields + rotaciones

### **Para Efectos Especiales:**
1. **Transiciones** → Noise + smoothstep
2. **Partículas** → Random + movimiento
3. **Glow** → Distance fields con alpha
4. **Distorsión** → Noise offset en UVs

---

## 💡 **Tips y Trucos**

### **HSB es tu mejor amigo**
```glsl
// En lugar de hardcodear RGB:
vec3 color = vec3(0.2, 0.6, 0.8);  // ❌ Difícil de ajustar

// Usar HSB:
vec3 color = hsb2rgb(vec3(0.55, 0.7, 0.8));  // ✅ Intuitivo
```

### **Cellular noise para TODO orgánico**
```glsl
// Agua, burbujas, células, piedras, lava...
float organic = cellularNoise(st * scale, 5.0);
vec3 color = hsb2rgb(vec3(hue, 0.7, organic));
```

### **Distance fields para formas perfectas**
```glsl
// En lugar de texturas de círculos:
float circle = sdCircle(st, 0.2);
float shape = smoothstep(0.01, 0.0, circle);
```

### **Paletas procedurales para variedad**
```glsl
// 1 función = infinitos colores
float t = st.x + u_Time;
vec3 color = palette(t, a, b, c, d);  // Cambiar a,b,c,d = nueva paleta
```

---

## 🎉 **Conclusión**

Esta librería te da **superpoderes** para crear wallpapers increíbles:
- **Menos código** (funciones reutilizables)
- **Mejor rendimiento** (todo en GPU)
- **Creatividad infinita** (combina funciones)
- **APK más ligero** (sin texturas)

**¡Úsala, experimenta y crea wallpapers ÉPICOS!** 🚀

---

**Última actualización**: 2025-11-02
**Autor**: Claude + Equipo Orbix IA
**Licencia**: Uso interno del proyecto Black Hole Glow
