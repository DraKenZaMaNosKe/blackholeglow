# Export to Android TV - Black Hole Glow

## Guía de Integración para Android TV (D:\Orbix\blackholeglow4tv)

Este documento contiene toda la información necesaria para integrar el wallpaper actual de Black Hole Glow en la versión de Android TV.

---

## 1. Archivos Core a Copiar

### Renderizado Principal
```
app/src/main/java/com/secret/blackholeglow/
├── SceneRenderer.java          # Renderer principal (GLSurfaceView.Renderer)
├── CameraController.java       # Sistema de cámara 3D
├── TextureManager.java         # Gestor de texturas (implementa TextureLoader)
├── BaseShaderProgram.java      # Clase base para shaders
└── ShaderUtils.java            # Utilidades de compilación de shaders
```

### Objetos de Escena
```
├── SceneObject.java            # Interface para objetos de escena
├── CameraAware.java            # Interface para objetos que necesitan cámara
├── Planeta.java                # Planetas con texturas y shaders
├── StarryBackground.java       # Fondo estrellado animado
├── SolProcedural.java          # Sol con shader de plasma (576 triángulos)
├── EstrellaBailarina.java      # Estrellas decorativas animadas
├── EarthShield.java            # Escudo de la Tierra (muestra impactos)
├── Spaceship3D.java            # 🛸 OVNI con IA + armas láser
├── UfoLaser.java               # 🔫 Rayos láser del OVNI
└── MeteorShower.java           # Sistema de meteoritos
```

### Utilidades
```
├── util/ObjLoader.java         # Cargador de modelos .obj
├── util/ProceduralSphere.java  # Generador de esferas procedurales
```

---

## 2. Shaders a Copiar

Ubicación: `app/src/main/assets/shaders/`

### Esenciales
```
shaders/
├── tierra_vertex.glsl          # Vertex shader de la Tierra
├── tierra_fragment.glsl        # Fragment shader de la Tierra (océanos, nubes, atmósfera)
├── sol_vertex.glsl             # Vertex shader del Sol
├── sol_lava_fragment.glsl      # Fragment shader del Sol (plasma animado)
├── starry_vertex.glsl          # Vertex shader del fondo
├── starry_fragment.glsl        # Fragment shader del fondo estrellado
├── planeta_vertex.glsl         # Vertex shader genérico para planetas
└── planeta_fragment.glsl       # Fragment shader genérico
```

---

## 3. Texturas a Copiar

Ubicación: `app/src/main/res/drawable/`

```
drawable/
├── texturaplanetatierra.png    # Textura HD de la Tierra
├── materialdelsol.png          # Textura base del Sol
├── universo001.png             # Fondo del universo
├── universo03.png              # Fondo alternativo
├── forerunnercentralplates.png # Textura del OVNI
└── [otras texturas de planetas si se usan]
```

---

## 4. Modelos 3D a Copiar

Ubicación: `app/src/main/assets/`

```
assets/
├── Spaceships.obj              # Modelo 3D del OVNI
├── planeta.obj                 # Modelo de planeta (opcional, usa procedural)
└── [otros modelos .obj]
```

---

## 5. Configuración de la Escena Universo

### En SceneRenderer.setupUniverseScene():

```java
// ORDEN DE CREACIÓN DE OBJETOS:
1. StarryBackground      - Fondo con estrellas
2. Planeta (Tierra)      - Centro de la escena (0,0,0)
3. EarthShield          - Escudo invisible para impactos
4. EstrellaBailarina x3  - Decoración animada
5. SolProcedural        - Sol en (-8, 4, -15)
6. Spaceship3D (OVNI)   - Exploración libre con IA
7. MeteorShower         - Sistema de meteoritos
8. MusicIndicator       - Ecualizador visual (opcional para TV)
```

### Configuración del OVNI:
```java
Spaceship3D ovni = new Spaceship3D(
    context,
    textureManager,
    1.8f, 1.5f, -1.0f,    // Posición inicial
    0.07f                  // Escala
);
ovni.setCameraController(sharedCamera);
ovni.setEarthPosition(0f, 0f, 0f);
ovni.setOrbitParams(1.5f, 0.35f, 0.0f);
ovni.setEarthShield(earthShield);  // Para impactos de láser
```

---

## 6. Sistema del OVNI (Spaceship3D.java)

### Características:
- **IA de exploración libre**: Deambula orgánicamente por la escena
- **Esquiva la Tierra**: Nunca atraviesa el planeta (safeDistance = 1.8)
- **Disparo automático**: Cada 3-7 segundos dispara láser a la Tierra
- **Sistema de vida**: 3 HP, destruido por meteoritos
- **Respawn**: Reaparece 8 segundos después de destrucción

### Parámetros importantes:
```java
// Límites de movimiento (para portrait, ajustar para TV landscape)
private float minX = -2.0f, maxX = 2.0f;
private float minY = -1.8f, maxY = 2.5f;
private float minZ = -3.0f, maxZ = 2.0f;

// Para Android TV (pantalla más ancha):
// Sugerencia: minX = -4.0f, maxX = 4.0f (ajustar según aspect ratio)

// Velocidad
private float maxSpeed = 0.6f;
private float minSpeed = 0.2f;

// Disparos
private float minShootInterval = 3.0f;
private float maxShootInterval = 7.0f;

// Vida
private int health = 3;
private float respawnDelay = 8.0f;
```

---

## 7. Adaptaciones para Android TV

### Cambios necesarios:

1. **Aspect Ratio**: TV es 16:9 landscape, ajustar límites del OVNI
2. **Cámara**: Posiblemente ajustar FOV o posición para pantalla grande
3. **Sin touch**: TV no tiene touch, todo es automático (ya está listo)
4. **Sin MusicIndicator**: Opcional, TV puede no tener visualizador de audio
5. **Resolución**: TV puede ser 1080p o 4K, las texturas HD deben verse bien

### Código de adaptación sugerido:
```java
// Detectar si es Android TV
boolean isTV = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);

if (isTV) {
    // Ajustar límites del OVNI para pantalla ancha
    ovni.setMovementLimits(-4.0f, 4.0f, -2.0f, 2.0f, -4.0f, 2.0f);
    // Desactivar MusicIndicator si no hay audio
}
```

---

## 8. Dependencias

### build.gradle.kts
```kotlin
android {
    compileSdk = 35
    defaultConfig {
        minSdk = 24  // Android TV mínimo
        targetSdk = 35
    }
}

dependencies {
    // No hay dependencias externas para el rendering
    // Todo es OpenGL ES 2.0 nativo
}
```

### AndroidManifest.xml (para TV)
```xml
<manifest>
    <!-- Requerido para Android TV -->
    <uses-feature android:name="android.software.leanback" android:required="true"/>
    <uses-feature android:name="android.hardware.touchscreen" android:required="false"/>

    <!-- OpenGL ES 2.0 -->
    <uses-feature android:glEsVersion="0x00020000" android:required="true"/>

    <!-- Banner para Android TV -->
    <application android:banner="@drawable/banner">
        <!-- DreamService para screensaver de TV -->
        <service
            android:name=".DreamService"
            android:exported="true"
            android:permission="android.permission.BIND_DREAM_SERVICE">
            <intent-filter>
                <action android:name="android.service.dreams.DreamService"/>
                <category android:name="android.intent.category.DEFAULT"/>
            </intent-filter>
        </service>
    </application>
</manifest>
```

---

## 9. DreamService para Android TV

Para que funcione como protector de pantalla en Android TV, necesitas implementar `DreamService`:

```java
public class SpaceDreamService extends android.service.dreams.DreamService {
    private GLSurfaceView glView;
    private SceneRenderer renderer;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);

        glView = new GLSurfaceView(this);
        glView.setEGLContextClientVersion(2);

        renderer = new SceneRenderer(this, null);
        glView.setRenderer(renderer);

        setContentView(glView);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        // El wallpaper comienza a renderizar
    }

    @Override
    public void onDreamingStopped() {
        super.onDreamingStopped();
        // Limpiar recursos si es necesario
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (glView != null) {
            glView.onPause();
        }
    }
}
```

---

## 10. Checklist de Integración

- [ ] Copiar todos los archivos Java listados en sección 1
- [ ] Copiar todos los shaders de sección 2
- [ ] Copiar texturas de sección 3
- [ ] Copiar modelos .obj de sección 4
- [ ] Ajustar package name si es diferente
- [ ] Implementar DreamService para TV
- [ ] Ajustar límites de movimiento del OVNI para landscape
- [ ] Probar en emulador de Android TV
- [ ] Verificar rendimiento en TV real

---

## 11. Notas de Rendimiento

- **FPS actual**: 36-43 FPS en dispositivo móvil
- **Triángulos Sol**: 576 (optimizado de 7,936)
- **OVNI**: Modelo 3D con ~2000 triángulos
- **Optimizaciones aplicadas**:
  - Cache de valores random cada 10 frames
  - Esferas procedurales en lugar de modelos OBJ
  - Texturas comprimidas

---

## 12. Archivos Modificados Recientemente (Nov 23, 2024)

| Archivo | Cambios |
|---------|---------|
| `Spaceship3D.java` | IA de exploración + armas láser + sistema HP |
| `UfoLaser.java` | NUEVO - Proyectiles láser |
| `SceneRenderer.java` | OVNI habilitado + conexión EarthShield |
| `MusicIndicator.java` | Ecualizador visual mejorado |

---

**Versión**: 4.0.0
**Fecha**: Noviembre 23, 2024
**Branch**: beta1.0
