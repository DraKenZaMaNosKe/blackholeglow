# 🌟 Black Hole Glow Shader Library

Librería modular de funciones GLSL basada en "The Book of Shaders" para crear efectos visuales procedurales ultra-optimizados.

## 📚 Módulos

### core.glsl
Funciones fundamentales para ruido y aleatoriedad.
- `random(vec2)` - Ruido pseudo-aleatorio de 2D
- `noise(vec2)` - Ruido suave interpolado
- `fbm(vec2)` - Fractal Brownian Motion (ruido fractal)

### color.glsl
Sistemas de color y conversiones.
- `hsb2rgb(vec3)` - Conversión HSB/HSV a RGB
- `rgb2hsb(vec3)` - Conversión RGB a HSB/HSV
- `yuv2rgb(vec3)` - Conversión YUV a RGB (cinematográfico)
- `rgb2yuv(vec3)` - Conversión RGB a YUV
- `palette(float, vec3, vec3, vec3, vec3)` - Paletas procedurales Inigo Quilez

### shapes.glsl
Distance Fields para formas geométricas perfectas.
- `sdCircle(vec2, float)` - Círculo signed distance
- `sdBox(vec2, vec2)` - Rectángulo signed distance
- `sdPolygon(vec2, int)` - Polígono regular N-lados
- `polarCoords(vec2)` - Conversión a coordenadas polares

### effects.glsl
Efectos avanzados y patrones complejos.
- `cellularNoise(vec2)` - Ruido celular tipo Worley
- `voronoi(vec2)` - Diagrama de Voronoi
- `grid(vec2, float)` - Grid procedural
- `rotate2d(float)` - Matriz de rotación 2D

## 🎯 Uso

Para incluir un módulo en tu shader:

```glsl
// Al inicio del fragment shader (después de defines)
// Nota: Los #include no son nativos en GLSL ES,
// hay que copiar las funciones manualmente o usar preprocesador

// Ejemplo de uso directo:
vec3 color = hsb2rgb(vec3(u_Time * 0.1, 0.8, 0.9));
```

## ⚡ Optimización

Todas las funciones están optimizadas para:
- GPU móvil (OpenGL ES 2.0)
- Precisión `mediump` cuando es posible
- Mínimo número de operaciones
- Sin texturas innecesarias

## 📖 Referencias

- [The Book of Shaders](https://thebookofshaders.com/)
- [Inigo Quilez - Shapes](https://iquilezles.org/articles/distfunctions2d/)
- [Patricio Gonzalez Vivo - GLSL Noise](https://gist.github.com/patriciogonzalezvivo/670c22f3966e662d2f83)

---

**Última actualización**: 2025-11-02
**Versión**: 1.0.0
**Proyecto**: Black Hole Glow - Orbix IA
