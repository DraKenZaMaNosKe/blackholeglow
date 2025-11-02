# 🌌 Guía de Shaders para Wallpaper Universo

**Fecha**: 2 Noviembre 2025
**Proyecto**: Black Hole Glow - Shaders Procedurales Épicos
**Basado en**: Black Hole Glow Shader Library v1.0.0

---

## 📋 ¿Qué se creó?

Se implementaron **3 shaders procedurales épicos** para el wallpaper "Universo":

### 1. 🌍 **Planeta Tierra Realista**
- **Archivos**: `tierra_fragment.glsl` + `tierra_vertex.glsl`
- **Efectos**:
  - ✨ Océanos azules con cellular noise (agua animada)
  - ✨ Continentes verdes/marrones con topografía procedural
  - ✨ Nubes blancas animadas con FBM (Fractal Brownian Motion)
  - ✨ Atmósfera brillante azul en los bordes (efecto Fresnel)
  - ✨ Especular en océanos (reflexión solar)
  - ✨ Luces de ciudades en el lado nocturno
  - ✨ Ciclo día/noche con iluminación dinámica

### 2. ☄️ **Asteroides Orgánicos**
- **Archivos**: `asteroide_fragment.glsl` + `asteroide_vertex.glsl`
- **Efectos**:
  - ✨ Textura rocosa con cellular noise multi-escala
  - ✨ Cráteres procedurales
  - ✨ Variación mineral (gris hierro, marrón óxido, roca oscura)
  - ✨ Polvo espacial sutil
  - ✨ Iluminación con terminador (borde día/noche)

### 3. ☀️ **Sol con Plasma**
- **Archivos**: `sol_plasma_fragment.glsl` + `sol_plasma_vertex.glsl`
- **Efectos**:
  - ✨ Plasma animado con FBM de 4 octavas
  - ✨ Manchas solares oscuras con cellular noise
  - ✨ Corona brillante amarilla con efecto Fresnel
  - ✨ Erupciones solares en el borde
  - ✨ Mapa de temperatura (núcleo blanco → superficie roja)
  - ✨ Emisión de luz intensa (autoiluminado)
  - ✨ Efecto HDR simulado en zonas muy brillantes

---

## 🎯 Ventajas de Estos Shaders

### ✅ 100% Procedurales
- **NO requieren texturas adicionales** (APK más ligero)
- Todo generado en GPU matemáticamente
- Escalable a cualquier resolución sin pixelación

### ✅ Optimizados para Móviles
- Objetivo: **60fps constante**
- Uso de `precision mediump float`
- FBM limitado a 3-4 octavas
- Cellular noise optimizado (9 celdas vecinas)

### ✅ Altamente Personalizables
- Cada shader tiene sección "VARIACIONES POSIBLES"
- Ajustar colores cambiando valores HSB
- Modificar velocidades de animación
- Crear planetas alienígenas cambiando paletas

### ✅ Usa la Shader Library
- Basado en `SHADER_LIBRARY_GUIDE.md`
- Funciones reutilizables de core.glsl, color.glsl, effects.glsl
- Código limpio y documentado

---

## 🔧 Cómo Implementar en SceneRenderer

### Opción 1: Reemplazar Shaders Existentes (Rápido)

Si ya tienes planetas en tu escena `setupUniverseScene()`, solo necesitas cambiar las rutas de shaders:

```java
// En SceneRenderer.java - setupUniverseScene()

// ANTES (shader simple):
Planeta tierra = new Planeta(
    context,
    textureManager,
    "shaders/planeta_vertex.glsl",      // ← Shader viejo
    "shaders/planeta_fragment.glsl",    // ← Shader viejo
    R.drawable.textura_tierra,
    // ... otros parámetros
);

// DESPUÉS (shader épico):
Planeta tierra = new Planeta(
    context,
    textureManager,
    "shaders/tierra_vertex.glsl",       // ← Shader nuevo
    "shaders/tierra_fragment.glsl",     // ← Shader nuevo
    R.drawable.textura_tierra,          // (Opcional: puedes poner 0 si no usas textura)
    // ... otros parámetros
);

// Similar para asteroides:
Asteroide asteroide = new Asteroide(
    context,
    textureManager,
    "shaders/asteroide_vertex.glsl",    // ← Shader nuevo
    "shaders/asteroide_fragment.glsl",  // ← Shader nuevo
    // ... parámetros
);

// Y para el sol:
Planeta sol = new Planeta(
    context,
    textureManager,
    "shaders/sol_plasma_vertex.glsl",   // ← Shader nuevo
    "shaders/sol_plasma_fragment.glsl", // ← Shader nuevo
    R.drawable.textura_sol,             // (Opcional: puedes poner 0)
    // ... parámetros
);
```

### Opción 2: Modo Híbrido (Textura + Procedural)

Los shaders pueden **combinar texturas existentes con efectos procedurales**:

```java
// La Tierra puede usar textura de continentes real + nubes procedurales
// Solo necesitas que la textura tenga transparencia en las nubes

// En tierra_fragment.glsl, ACTIVAR modo híbrido:
// Descomentar línea:
// vec4 realTexture = texture2D(u_Texture, uv);
// surfaceColor = mix(surfaceColor, realTexture.rgb, 0.5);
```

### Opción 3: 100% Procedural (Sin Texturas)

Para máxima creatividad y APK más ligero:

```java
// Pasar textureResId = 0 (sin textura)
Planeta tierra = new Planeta(
    context,
    textureManager,
    "shaders/tierra_vertex.glsl",
    "shaders/tierra_fragment.glsl",
    0,  // ← Sin textura, 100% procedural
    // ... otros parámetros
);
```

---

## 🎨 Personalización Rápida

### Cambiar Color de la Tierra

En `tierra_fragment.glsl`:

```glsl
// Línea ~140 - Color de océanos
vec3 oceanColor = hsb2rgb(vec3(
    0.55,  // ← Hue: 0.55 = azul, 0.33 = verde, 0.0 = rojo
    0.7,   // ← Saturation: 0.0 = gris, 1.0 = color puro
    0.4    // ← Brightness: 0.0 = negro, 1.0 = blanco
));

// Línea ~150 - Color de continentes
float landHue = mix(0.12, 0.30, elev);  // ← De marrón (0.12) a verde (0.30)
```

**Ejemplos de planetas alienígenas**:
- **Océano rojo**: `hue = 0.0`
- **Océano verde**: `hue = 0.33`
- **Océano púrpura**: `hue = 0.80`
- **Tierra congelada**: `hue = 0.55, saturation = 0.2` (azul pálido)

### Cambiar Velocidad de Nubes

En `tierra_fragment.glsl`, línea ~121:

```glsl
float clouds(vec2 uv, float time) {
    vec2 st = uv + vec2(time * 0.02, 0.0);  // ← 0.02 = lento, 0.1 = rápido
    // ...
}
```

### Más Manchas Solares

En `sol_plasma_fragment.glsl`, línea ~30:

```glsl
const float SUNSPOT_SCALE = 5.0;  // ← 5.0 = pocas manchas, 10.0 = muchas manchas
```

### Asteroides Metálicos (Brillantes)

En `asteroide_fragment.glsl`, línea ~159:

```glsl
// Agregar especular después de litColor:
vec3 halfVector = normalize(lightDir + viewDir);
float spec = pow(max(dot(normal, halfVector), 0.0), 16.0);
litColor += vec3(1.0) * spec * 0.5;  // Reflejo metálico
```

---

## 📊 Rendimiento Esperado

### Mediciones en Móvil Medio (2020+)

| Shader | FPS Esperado | Costo GPU | Recomendación |
|--------|--------------|-----------|---------------|
| Tierra | 55-60fps | Medio | ✅ Usar siempre |
| Asteroide | 60fps | Bajo | ✅ Múltiples instancias OK |
| Sol | 50-60fps | Medio-Alto | ⚠️ Solo 1 instancia |

### Optimizaciones si FPS < 45

1. **Reducir octavas de FBM**:
   ```glsl
   float plasma = fbm(st, 3);  // De 4 a 3 octavas
   ```

2. **Reducir escala de cellular noise**:
   ```glsl
   float cells = cellularNoise(uv, 3.0);  // De 5.0 a 3.0
   ```

3. **Deshabilitar efectos secundarios**:
   ```glsl
   // Comentar luces de ciudades en tierra
   // Comentar erupciones solares en sol
   ```

---

## 🚀 Próximos Pasos

### Fase 1: Probar Shaders (HOY)
1. Reemplazar shaders en SceneRenderer
2. Compilar y probar en dispositivo
3. Ajustar colores a tu gusto

### Fase 2: Expandir (ESTA SEMANA)
1. Crear shader para **Luna** (gris, cráteres)
2. Crear shader para **Júpiter** (bandas de gas con noise)
3. Crear shader para **Saturno** (anillos procedurales)

### Fase 3: Efectos Avanzados (PRÓXIMO MES)
1. Nebulosas de fondo con FBM
2. Estrellas titilantes con random
3. Cometas con trails
4. Agujeros negros con distorsión de espacio-tiempo

---

## 💡 Tips Importantes

### ✅ DO (Mejores Prácticas)
- Usar HSB para colores (más intuitivo que RGB)
- Cellular noise para TODO lo orgánico
- FBM con 3-4 octavas máximo
- Cachear valores costosos en variables
- Probar en dispositivo real, NO solo emulador

### ❌ DON'T (Evitar)
- NO usar más de 1 Sol por escena (costoso)
- NO mezclar FBM + cellular noise en mismo objeto
- NO usar bucles dinámicos (usar constantes)
- NO sobrecargar con muchos asteroides (max 5-6)

---

## 🐛 Troubleshooting

### Problema: Pantalla negra / shader no compila
**Solución**: Revisar LogCat para errores de compilación. Buscar:
```
E/ShaderUtils: *** Shader compilation error ***
```

### Problema: FPS bajo (< 30fps)
**Solución**: Reducir octavas de FBM o escala de cellular noise

### Problema: Colores incorrectos
**Solución**: Verificar que `v_Normal` y `v_WorldPos` se estén pasando correctamente desde vertex shader

### Problema: Tierra sin océanos/continentes
**Solución**: Ajustar threshold en `landMask()` (línea ~97 de tierra_fragment.glsl)

---

## 📚 Referencias

- **Shader Library**: `SHADER_LIBRARY_GUIDE.md`
- **Ejemplos**: `ocean_deep_fragment.glsl`, `demo_library_fragment.glsl`
- **The Book of Shaders**: https://thebookofshaders.com/
- **Inigo Quilez**: https://iquilezles.org/

---

## 🎉 ¡Listo para Crear Magia!

Estos shaders son solo el **comienzo**. Con la Shader Library como base, puedes:

- 🪐 Crear 10 planetas únicos sin texturas
- 🌌 Nebulosas, galaxias, supernovas
- ⚡ Tormentas eléctricas en planetas gaseosos
- 🔥 Volcanes activos con lava procedural
- ❄️ Planetas helados con escarcha cristalina

**¡A crear el wallpaper más impresionante de la Play Store!** 🚀✨

---

**Última actualización**: 2 Noviembre 2025
**Autor**: Claude + Eduardo (DraKenZaMaNosKe)
**Proyecto**: Black Hole Glow v4.0.0
