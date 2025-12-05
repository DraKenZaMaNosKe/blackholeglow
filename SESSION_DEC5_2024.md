# Session Notes - December 5, 2024

## 🎯 Trabajo Completado

### 1. ✅ Efecto de Remolino en MusicStars
**Archivo**: `MusicStars.java`

Se agregó un efecto de "polvo estelar mezclándose como pintura" a las estrellas estáticas que pulsan con la música.

**Cambios realizados**:
- Agregados campos a la clase `Star`: `baseColor`, `colorPhase`, `swirlAngle`
- Nueva paleta de colores `SWIRL_COLORS` (azul, rosa, púrpura, dorado, cyan, naranja)
- Variable `globalTime` para animación
- Método `update()` modificado para:
  - Animar `colorPhase` y `swirlAngle` por estrella
  - Mezclar color base con colores del remolino (60-80% color base)
  - La música intensifica la mezcla de colores (hasta 50%)

**Estado**: ✅ FUNCIONANDO - El usuario confirmó "si se ve muy bien"

---

### 2. ⚠️ Estrellas de Fondo Parpadeantes (PENDIENTE)
**Archivo**: `BackgroundStars.java` (NUEVO)

Se creó un sistema de estrellas pequeñas parpadeantes para dar efecto de profundidad.

**Características implementadas**:
- 60 estrellas pequeñas distribuidas en pantalla
- Parpadeo suave con ondas sinusoidales (cada estrella tiene fase única)
- Colores: 60% blancas, 20% azuladas, 20% amarillentas
- UN solo draw call para todas las estrellas (optimizado)
- Actualiza parpadeo cada 2 frames

**Archivos modificados**:
- `BackgroundStars.java` - Clase nueva
- `BatallaCosmicaScene.java` - Import y creación en `setupBackground()`

**Estado**: ⚠️ NO SE VEN - Necesita debugging

**Posibles causas a investigar mañana**:
1. El shader puede no estar compilando correctamente en el dispositivo
2. Las posiciones NDC (-1 a 1) pueden estar fuera del viewport
3. El blending aditivo puede estar haciendo las estrellas invisibles contra el fondo
4. El tamaño de punto (4.0f) puede ser muy pequeño
5. El orden de dibujado puede estar mal (se dibujan detrás del fondo)

**Próximos pasos para debugging**:
```java
// Agregar logs en draw() para verificar que se está llamando
Log.d(TAG, "Drawing " + NUM_STARS + " stars, programId=" + programId);

// Probar con tamaño más grande
GLES20.glUniform1f(uPointSizeLoc, 10.0f);

// Probar sin blending aditivo
GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

// Verificar que las estrellas se dibujan DESPUÉS del fondo pero ANTES de los objetos 3D
```

---

### 3. ✅ Sol con Plasma Sutil (Sesión Anterior)
**Archivos**: `SolMeshy.java`, `sol_plasma_fragment.glsl`

- Sol usa textura como base (90%)
- Plasma aparece intermitentemente (cada ~8 segundos)
- Distorsión de calor sutil
- Corona muy sutil en bordes

**Estado**: ✅ FUNCIONANDO - Usuario confirmó "si se ve bien"

---

## 📁 Archivos Clave

```
MusicStars.java          - Estrellas que pulsan con música + efecto remolino ✅
BackgroundStars.java     - Estrellas de fondo parpadeantes ⚠️ (no se ven)
BatallaCosmicaScene.java - Escena principal (importa BackgroundStars)
SolMeshy.java            - Sol con shaders de plasma
sol_plasma_fragment.glsl - Shader del sol con efectos sutiles
```

---

## 🐛 Bug Pendiente: BackgroundStars no visibles

### Código actual del shader:
```glsl
// Vertex
attribute vec2 a_Position;
attribute vec4 a_Color;
uniform float u_PointSize;
varying vec4 v_Color;
void main() {
    v_Color = a_Color;
    gl_Position = vec4(a_Position, 0.0, 1.0);
    gl_PointSize = u_PointSize;
}

// Fragment
varying vec4 v_Color;
void main() {
    vec2 coord = gl_PointCoord - vec2(0.5);
    float dist = length(coord);
    float alpha = v_Color.a * (1.0 - smoothstep(0.0, 0.5, dist));
    float glow = 1.0 + (1.0 - dist * 2.0) * 0.5;
    gl_FragColor = vec4(v_Color.rgb * glow, alpha);
}
```

### Ideas para mañana:
1. **Verificar orden de dibujado** - Las estrellas deben dibujarse DESPUÉS del fondo `StarryBackground`
2. **Aumentar tamaño** - Probar con `gl_PointSize = 20.0` para ver si aparecen
3. **Cambiar blending** - El blending aditivo `GL_ONE` puede no funcionar bien contra el fondo oscuro
4. **Usar colores sólidos** - Probar con color rojo brillante para debugging
5. **Verificar profundidad** - El `z = 0.0` puede estar detrás de algo

---

## 📝 Solicitud Original del Usuario

> "estrellas al fondo, que prendan y apaguen, ya tenemos muchas en la imagen de fondo si, pero para que no se vea tan estatico estaria super, que pusieramos estrellas chiquitas a los lejos dandole un efecto de profundidad a la escena, que te parece, algunas estrellas parpadeando con algun efecto bonito, sin saturar el gpu porfas amigo"

---

## 🔧 Comandos Útiles

```bash
# Build
./gradlew assembleDebug

# Install
D:/adb/platform-tools/adb.exe install -r app/build/outputs/apk/debug/app-debug.apk

# Logs de BackgroundStars
adb logcat | grep -i "BackgroundStars"
```
