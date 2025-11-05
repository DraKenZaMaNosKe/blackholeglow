# 🎨 GUÍA DE POSICIONES - BLACK HOLE GLOW

## 📌 Coordenadas 2D (UI Elements - SimpleTextRenderer)
Para elementos de interfaz 2D como textos, usamos coordenadas **normalizadas (0.0 a 1.0)**:
- **X**: 0.0 = Izquierda, 1.0 = Derecha
- **Y**: 0.0 = Arriba, 1.0 = Abajo

**Archivo**: `SceneRenderer.java`

---

## 🪐 1. CONTADOR DE PLANETAS DESTRUIDOS
**Línea**: ~825-831
```java
planetsDestroyedCounter = new SimpleTextRenderer(
    context,
    0.50f,    // X: Posición horizontal (0.0=izquierda, 1.0=derecha)
    0.88f,    // Y: Posición vertical (0.0=arriba, 1.0=abajo)
    0.40f,    // Ancho
    0.10f     // Alto
);
```

---

## 🏆 2. LEADERBOARD (Top 3)
**Línea**: ~857-895
```java
// Primera posición del leaderboard
leaderboardTexts[0] = new SimpleTextRenderer(
    context,
    0.05f,    // X: Borde izquierdo
    0.18f,    // Y: Primera posición
    0.35f,    // Ancho
    0.08f     // Alto
);

// Segunda posición
leaderboardTexts[1] = new SimpleTextRenderer(
    context,
    0.05f,    // X: Borde izquierdo
    0.28f,    // Y: Segunda posición (más abajo)
    0.35f,    // Ancho
    0.08f     // Alto
);

// Tercera posición
leaderboardTexts[2] = new SimpleTextRenderer(
    context,
    0.05f,    // X: Borde izquierdo
    0.38f,    // Y: Tercera posición (más abajo)
    0.35f,    // Ancho
    0.08f     // Alto
);
```

---

## 🎵 3. INDICADOR DE MÚSICA
**Línea**: ~915-921
```java
musicIndicator = new SimpleTextRenderer(
    context,
    0.005f,   // X: Esquina superior izquierda
    0.75f,    // Y: Parte superior
    0.20f,    // Ancho
    0.08f     // Alto
);
```

---

## 💪 4. BARRAS DE HP (Planeta y Escudo)
**Línea**: ~710-738

### Barra HP del Planeta (Tierra):
```java
hpBarSun = new HPBar(
    context,
    0.05f,    // X: Posición horizontal
    0.78f,    // Y: Posición vertical
    0.30f,    // Ancho
    0.04f,    // Alto
    "🌍 TIERRA"  // Etiqueta
);
```

### Barra HP del Escudo:
```java
hpBarForceField = new HPBar(
    context,
    0.05f,    // X: Misma posición horizontal que HP Tierra
    0.87f,    // Y: Debajo de la barra de Tierra
    0.30f,    // Ancho
    0.04f,    // Alto
    "🛡️ ESCUDO"  // Etiqueta
);
```

---

## 📌 Coordenadas 3D (Objetos en el espacio)
Para objetos 3D, usamos coordenadas **del mundo 3D**:
- **X**: Horizontal (negativo=izquierda, positivo=derecha)
- **Y**: Vertical (negativo=abajo, positivo=arriba)
- **Z**: Profundidad (negativo=atrás, positivo=adelante)

---

## 👤 5. AVATAR SPHERE (Foto del usuario)
**Archivo**: `AvatarSphere.java`
**Línea**: ~63-66
```java
private float fixedX = -1.65f;  // X: Posición horizontal
private float fixedY = 3.10f;   // Y: Posición vertical
private float fixedZ = 0.10f;   // Z: Profundidad
private float scale = 0.18f;    // Tamaño del avatar
```

---

## 🌍 6. PLANETA TIERRA (Centro de la escena)
**Archivo**: `SceneRenderer.java`
**Línea**: ~465-481
```java
sol = new Planeta(
    context, textureManager,
    "shaders/planeta_vertex.glsl",
    "shaders/planeta_iluminado_fragment.glsl",
    R.drawable.texturaplanetatierra,
    0.8f,              // orbitRadiusX (posición X de la órbita)
    0.0f,              // orbitRadiusZ (posición Z de la órbita)
    0.0f,              // orbitSpeed (velocidad de órbita, 0=fijo)
    0.0f,              // orbitOffsetY (altura Y)
    0.0f,              // scaleAmplitude
    1.0f,              // instanceScale (TAMAÑO del planeta)
    80.0f,             // spinSpeed (velocidad de rotación)
    false, null, 1.0f, null, 1.0f
);
```
**Nota**: La Tierra está casi en el centro (X=0.8, Y=0.0, Z=0.0)

---

## ☀️ 7. SOL (Fondo, arriba y atrás)
**Archivo**: `SceneRenderer.java`
**Línea**: ~554-573
```java
Planeta planetaSol = new Planeta(
    context, textureManager,
    "shaders/planeta_vertex.glsl",
    "shaders/planeta_iluminado_fragment.glsl",
    R.drawable.texturasolvolcanico,
    0.0f,              // orbitRadiusX (centrado en X)
    -10.0f,            // orbitRadiusZ (ATRÁS, en el fondo)
    0.0f,              // orbitSpeed
    5.0f,              // orbitOffsetY (ARRIBA, altura 5.0)
    0.0f,              // scaleAmplitude
    1.5f,              // instanceScale (TAMAÑO grande)
    35.0f,             // spinSpeed
    false, null, 1.0f, null, 1.0f
);
```

---

## 🔴 8. MARTE (Órbita lejana)
**Archivo**: `SceneRenderer.java`
**Línea**: ~593-608
```java
Planeta planetaMarte = new Planeta(
    context, textureManager,
    "shaders/planeta_vertex.glsl",
    "shaders/planeta_iluminado_fragment.glsl",
    R.drawable.textura_marte,
    4.5f,              // orbitRadiusX (órbita en X)
    4.0f,              // orbitRadiusZ (órbita en Z)
    0.30f,             // orbitSpeed (velocidad de órbita)
    0.0f,              // orbitOffsetY (altura)
    0.0f,              // scaleAmplitude
    0.40f,             // instanceScale (TAMAÑO pequeño)
    50.0f,             // spinSpeed
    false, null, 1.0f, null, 1.0f
);
```

---

## 🌙 9. LUNA (Orbita la Tierra)
**Archivo**: `SceneRenderer.java`
**Línea**: ~629-643
```java
Planeta planetaLuna = new Planeta(
    context, textureManager,
    "shaders/planeta_vertex.glsl",
    "shaders/planeta_iluminado_fragment.glsl",
    R.drawable.textura_luna,
    1.8f,              // orbitRadiusX (órbita relativa a la Tierra)
    1.5f,              // orbitRadiusZ (órbita relativa a la Tierra)
    1.0f,              // orbitSpeed (velocidad)
    0.0f,              // orbitOffsetY (altura)
    0.0f,              // scaleAmplitude
    0.22f,             // instanceScale (TAMAÑO pequeño)
    15.0f,             // spinSpeed
    false, null, 1.0f, null, 1.0f
);
```
**Nota**: La Luna orbita alrededor de la Tierra (setParentPlanet en línea ~651)

---

## 🛡️ 10. CAMPO DE FUERZA (Alrededor de la Tierra)
**Archivo**: `SceneRenderer.java`
**Línea**: ~685-695
```java
forceField = new ForceField(
    context,
    textureManager,
    0.70f,             // scaleAmplitude (tamaño base)
    70.0f              // pulseSpeed (velocidad de pulsación)
);
```
**Nota**: El ForceField se posiciona automáticamente en (0, 0, 0) y orbita alrededor de la Tierra

---

## ✨ 11. ESTRELLAS BAILARINAS (Partículas con estela)
**Archivo**: `SceneRenderer.java`
**Línea**: ~516-546

### Estrella 1 (Superior derecha):
```java
EstrellaBailarina estrella1 = new EstrellaBailarina(
    context, textureManager,
    1.8f,              // X: Derecha
    0.8f,              // Y: Arriba
    0.5f,              // Z: Adelante
    0.02f,             // Escala (tamaño)
    45.0f              // Rotación
);
```

### Estrella 2 (Izquierda):
```java
EstrellaBailarina estrella2 = new EstrellaBailarina(
    context, textureManager,
    -2.0f,             // X: Izquierda
    1.2f,              // Y: Arriba
    0.3f,              // Z: Adelante
    0.02f,             // Escala
    35.0f              // Rotación
);
```

### Estrella 3 (Centro abajo):
```java
EstrellaBailarina estrella3 = new EstrellaBailarina(
    context, textureManager,
    0.2f,              // X: Centro
    -1.5f,             // Y: Abajo
    0.4f,              // Z: Adelante
    0.02f,             // Escala
    50.0f              // Rotación
);
```

---

## 🌌 12. FONDO DEL UNIVERSO
**Archivo**: `SceneRenderer.java`
**Línea**: ~437-445
```java
UniverseBackground background = new UniverseBackground(
    context,
    textureManager,
    R.drawable.universo03,
    3.0f               // baseScale (tamaño del fondo)
);
```
**Nota**: El fondo se renderiza en la posición (0, 0, -5) automáticamente

---

## 📝 NOTAS IMPORTANTES:

### Para objetos 2D (UI):
- Los valores van de **0.0 a 1.0**
- Se modifican en **SceneRenderer.java**
- Formato: `new SimpleTextRenderer(context, X, Y, ancho, alto)`

### Para objetos 3D (Planetas, Avatar):
- Los valores pueden ser **negativos o positivos**
- Para **Planetas**: Se modifican en **SceneRenderer.java** (parámetros del constructor)
- Para **AvatarSphere**: Se modifican en **AvatarSphere.java** (variables fixedX, fixedY, fixedZ)
- Formato planeta: `new Planeta(..., orbitX, orbitZ, speed, offsetY, ..., scale, ...)`

### Sistema de coordenadas 3D:
```
      Y (arriba)
      |
      |
      +------ X (derecha)
     /
    /
   Z (adelante hacia la cámara)
```

---

## 🎯 EJEMPLOS DE AJUSTES COMUNES:

### Mover contador de planetas a la izquierda:
```java
planetsDestroyedCounter = new SimpleTextRenderer(
    context,
    0.10f,    // Cambiar de 0.50 a 0.10 (más a la izquierda)
    0.88f,
    0.40f,
    0.10f
);
```

### Hacer la Tierra más grande:
```java
sol = new Planeta(
    ...,
    1.5f,     // Cambiar instanceScale de 1.0 a 1.5
    ...
);
```

### Subir el avatar:
```java
// En AvatarSphere.java
private float fixedY = 2.50f;   // Cambiar de 3.10 a 2.50 (más arriba)
```

---

**Última actualización**: 2025-10-25
