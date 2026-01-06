# NeonCity Scene - DeLorean 3D Documentation

## Overview

**NeonCity** es un wallpaper estilo synthwave/retrowave con:
- Video de fondo: carretera infinita con grid neón hacia un sol gigante
- Modelo 3D: DeLorean con efectos de underglow neón
- Tema visual: PYRALIS (colores fuego/naranja compatibles con synthwave)

## Files Structure

```
app/src/main/
├── assets/
│   ├── delorean.obj          # Modelo 3D (~5,000 triángulos, 15,384 vértices)
│   └── delorean.mtl          # Material file (referencia delorean_texture.png)
├── java/com/secret/blackholeglow/
│   ├── DeLorean3D.java       # Clase principal del carro 3D
│   └── scenes/
│       └── NeonCityScene.java # Escena completa
└── res/drawable/
    ├── delorean_texture.png   # Textura del carro (4 MB)
    └── preview_neoncity.webp  # Preview para catálogo (79 KB)
```

## Video Background

- **File**: `neoncity.mp4`
- **Size**: 13.04 MB
- **URL**: `https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/public/wallpaper-videos/neoncity.mp4`
- **Content**: Animación loop de carretera synthwave con grid neón, palmeras, sol gigante
- **Registered in**: `VideoConfig.java` lines 65-70

## DeLorean3D.java

### Transform (Calibrated 2026-01-06)

```java
private float x = 0f;       // Centrado en la carretera
private float y = -4.0f;    // Sobre la carretera (abajo)
private float z = -4.0f;    // Profundidad
private float scale = 1.2f;

private float rotationX = 15f;   // Inclinado hacia adelante
private float rotationY = -90f;  // Rotado para ver parte trasera
private float rotationZ = 0f;
```

### Shader Effects

El fragment shader tiene dos efectos principales:

1. **Underglow Neón** (parte inferior del carro):
   - Pulsa entre cyan (#00FFFF) y magenta (#FF00FF)
   - Frecuencia: 3Hz
   - Área: `vPosition.y < 0`

2. **Luces Traseras** (parte posterior):
   - Color: Rojo (#FF1A1A)
   - Pulso suave a 2Hz
   - Área: `vPosition.z > 0.3`

```glsl
// Underglow
float underglowArea = smoothstep(-0.3, -0.1, vPosition.y) * (1.0 - smoothstep(-0.1, 0.1, vPosition.y));
vec3 glowColor1 = vec3(0.0, 1.0, 1.0);  // Cyan
vec3 glowColor2 = vec3(1.0, 0.0, 1.0);  // Magenta
float pulse = sin(uTime * 3.0) * 0.5 + 0.5;
vec3 glowColor = mix(glowColor1, glowColor2, pulse) * 0.5;

// Tail lights
float tailLightArea = smoothstep(0.3, 0.5, vPosition.z) * smoothstep(-0.2, 0.0, vPosition.y);
vec3 tailLightColor = vec3(1.0, 0.1, 0.1) * (sin(uTime * 2.0) * 0.2 + 0.8);
```

### Bobbing Animation

El carro tiene un movimiento sutil de "flotación":
- Amplitud: 0.02 unidades
- Frecuencia: 2Hz
- Aplicado en `update(deltaTime)`

```java
float bobOffset = (float) Math.sin(time * 2.0) * 0.02f;
// Applied to Y position in draw()
```

### Touch Positioning (Debug Mode)

El carro tiene un modo de ajuste por touch:

```java
private boolean adjustMode = false;  // Cambiar a true para calibrar

public boolean onTouchEvent(float normalizedX, float normalizedY) {
    if (!adjustMode) return false;
    this.x = normalizedX * 3.0f;
    this.y = normalizedY * 2.5f - 2.5f;
    return true;
}
```

Para recalibrar:
1. Cambiar `adjustMode = true`
2. Build e instalar
3. Tocar pantalla donde quieras el carro
4. Ver logs: `adb logcat -s "DeLorean3D"`
5. Copiar valores de x, y a las variables
6. Cambiar `adjustMode = false`

## NeonCityScene.java

### Components

| Component | Class | Theme |
|-----------|-------|-------|
| Video Background | MediaCodecVideoRenderer | - |
| 3D Car | DeLorean3D | - |
| Equalizer | EqualizerBarsDJ | PYRALIS |
| Clock | Clock3D | PYRALIS |
| Battery | Battery3D | PYRALIS |

### Draw Order

```java
// 1. Video de fondo (sin depth test)
GLES30.glDisable(GLES30.GL_DEPTH_TEST);
videoBackground.draw();

// 2. DeLorean 3D (con depth test)
GLES30.glEnable(GLES30.GL_DEPTH_TEST);
delorean.draw();

// 3. UI Elements (con blending)
GLES30.glEnable(GLES30.GL_BLEND);
equalizerDJ.draw();
clock.draw();
battery.draw();
```

## Registration Points

Para que una escena funcione, debe registrarse en **4 lugares**:

1. **SceneFactory.java** (line ~114):
   ```java
   registerScene("NEON_CITY", NeonCityScene.class);
   ```

2. **WallpaperCatalog.java** (lines 297-307):
   ```java
   catalog.add(new WallpaperItem.Builder("✧ NEON CITY ✧")
       .sceneName("NEON_CITY")
       .preview(R.drawable.preview_neoncity)
       // ...
       .build());
   ```

3. **ResourcePreloader.java**:
   - Switch case en `prepareForScene()` (lines 109-110)
   - Método `prepareNeonCitySceneTasks()` (lines 192-203)

4. **WallpaperPreferences.java** (line ~72):
   ```java
   "NEON_CITY",  // En VALID_WALLPAPERS HashSet
   ```

## Known Issues & TODO

### Current Issues
- [ ] El carro podría necesitar más rotación para verse mejor desde atrás
- [ ] Underglow effect podría ser más visible
- [ ] Considerar tema SYNTHWAVE específico (pink/cyan) en vez de PYRALIS

### Future Improvements
- [ ] Añadir estelas de luz detrás del carro
- [ ] Efecto de velocidad/motion blur
- [ ] Partículas de polvo/humo
- [ ] Tema SYNTHWAVE para equalizer/clock/battery (colores pink/cyan/purple)

## CameraController API Reference

Para objetos 3D que necesitan MVP matrix:

```java
// CORRECTO - usar VP matrix combinada
float[] vpMatrix = camera.getViewProjectionMatrix();
Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);

// ALTERNATIVA - usar helper
camera.computeMvp(modelMatrix, mvpMatrix);

// NO EXISTE - getProjectionMatrix() separado
// camera.getProjectionMatrix() ❌
```

## Build & Test Commands

```bash
# Build debug
./gradlew assembleDebug

# Install on Samsung
timeout 60 D:/adb/platform-tools/adb.exe -s RF8X903KZ3K install -r app/build/outputs/apk/debug/app-debug.apk

# Monitor DeLorean logs
timeout 30 D:/adb/platform-tools/adb.exe -s RF8X903KZ3K logcat -s "DeLorean3D"

# Monitor NeonCity scene
timeout 30 D:/adb/platform-tools/adb.exe -s RF8X903KZ3K logcat -s "NeonCityScene"
```

## Asset Sources

- **Video**: Generado con Grok (prompt en CLAUDE.md principal)
- **3D Model**: Meshy AI (Image-to-3D)
- **Preview**: Captura del video procesada en GIMP

## Version History

| Date | Change |
|------|--------|
| 2026-01-06 | Initial creation - video working |
| 2026-01-06 | DeLorean3D integrated |
| 2026-01-06 | Touch positioning calibrated (x=0, y=-4.0) |
| 2026-01-06 | Adjust mode disabled, position fixed |
