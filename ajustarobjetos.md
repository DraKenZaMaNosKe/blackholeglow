# Sistema de Ajuste de Objetos 3D en Pantalla

Este documento explica cómo habilitar y usar el sistema táctil para ajustar posiciones de objetos 3D en escenas de Black Hole Glow.

---

## Cómo Habilitar el Modo de Edición

1. Abre la escena que quieres editar (ej: `MoonlitCatScene.java`)

2. Busca la constante `EDIT_MODE` y cámbiala a `true`:
```java
private static final boolean EDIT_MODE = true;  // Habilitar edición
```

3. Compila e instala la app

---

## Cómo Usar el Sistema Táctil

### Controles:

| Acción | Efecto |
|--------|--------|
| **TAP** | Cambiar al siguiente modo de edición |
| **Arrastrar ↑** | Aumentar el valor actual |
| **Arrastrar ↓** | Disminuir el valor actual |

### Secuencia de Modos (tap para avanzar):

**Objetos 3D:**
1. 🐱 CAT X - Posición horizontal del gato
2. 🐱 CAT Y - Posición vertical del gato
3. 🐱 CAT Z - Profundidad del gato
4. 🐱 CAT SCALE - Tamaño del gato
5. 🐱 CAT ROT - Rotación del gato
6. 🧱 WALL X - Posición horizontal de la barda
7. 🧱 WALL Y - Posición vertical de la barda
8. 🧱 WALL Z - Profundidad de la barda
9. 🧱 WALL SCALE - Tamaño de la barda
10. 🧱 WALL ROT - Rotación de la barda

**Elementos 2D (Shader/Textura):**
11. 🌙 MOON X - Posición horizontal de la luna (0-1, UV coords)
12. 🌙 MOON Y - Posición vertical de la luna (0-1, UV coords)
13. 🌙 MOON RADIUS - Tamaño de la luna
14. 🏘️ BLDG OFFSET - Posición vertical de los edificios
15. 🏘️ BLDG HEIGHT - Altura de la capa de edificios

*(después del 15 vuelve al 1)*

---

## Ver los Valores en LogCat

Filtra LogCat por el TAG de la escena (ej: `MoonlitCatScene`):

```bash
adb logcat | grep "MoonlitCatScene"
```

Verás logs como:
```
🎮 MODO: 🐱 CAT X (arrastra arriba/abajo para cambiar)
🎮 🐱 CAT X = 0.150
🎮 🐱 CAT Y = -0.350
```

Al soltar el dedo, se muestra un resumen completo:
```
╔══════════════════════════════════════════════════════════════════════════╗
║  📍 POSICIONES ACTUALES - Copia estos valores al código:                 ║
╠══════════════════════════════════════════════════════════════════════════╣
║  🐱 CAT:  pos(-0.260, 0.122, 1.224) scale=0.158 rot=78.6
║  🧱 WALL: pos(0.000, -0.386, 1.312) scale=0.360 rot=-4.2
║  🌙 MOON: pos(0.720, 0.780) radius=0.0700
║  🏘️ BLDG: yOffset=0.250 height=0.550
╚══════════════════════════════════════════════════════════════════════════╝
```

---

## Fijar las Posiciones Finales

1. Copia los valores del log

2. Actualiza el código en `setupSceneSpecific()`:
```java
// Ejemplo para MoonlitCatScene
// 3D Objects
cat.setPosition(-0.260f, 0.122f, 1.224f);
cat.setScale(0.158f);
cat.setRotationY(78.6f);

wall.setPosition(0.000f, -0.386f, 1.312f);
wall.setScale(0.360f);
wall.setRotationY(-4.2f);

// Moon (en NightSkyRenderer, coordenadas UV 0-1)
// Se configura con valores por defecto en la clase, o:
// nightSky.setMoonPosition(0.72f, 0.78f);
// nightSky.setMoonRadius(0.07f);

// Buildings (en BuildingsSilhouette2D)
buildings.setYOffset(0.25f);
buildings.setHeight(0.55f);
```

3. Desactiva el modo de edición:
```java
private static final boolean EDIT_MODE = false;  // ✅ Posiciones fijadas
```

4. Compila e instala la versión final

---

## Código Necesario en la Escena

### 1. Variables de estado:
```java
private static final boolean EDIT_MODE = true;

private int editMode = 0;
private static final String[] MODE_NAMES = {
    "🐱 CAT X", "🐱 CAT Y", "🐱 CAT Z", "🐱 CAT SCALE", "🐱 CAT ROT",
    "🧱 WALL X", "🧱 WALL Y", "🧱 WALL Z", "🧱 WALL SCALE", "🧱 WALL ROT"
};

private float lastTouchY = 0f;
private boolean isDragging = false;

private static final float POS_SENSITIVITY = 0.002f;
private static final float SCALE_SENSITIVITY = 0.002f;
private static final float ROT_SENSITIVITY = 0.3f;
```

### 2. Los objetos 3D necesitan getters:
```java
public float getPosX() { return posX; }
public float getPosY() { return posY; }
public float getPosZ() { return posZ; }
public float getScale() { return scale; }
public float getRotationY() { return rotationY; }
```

### 3. Override de onTouchEvent (ver MoonlitCatScene.java como referencia)

---

## Sensibilidades Recomendadas

| Tipo | Valor | Descripción |
|------|-------|-------------|
| `POS_SENSITIVITY` | 0.002f | Movimiento suave en X/Y/Z (3D) |
| `SCALE_SENSITIVITY` | 0.002f | Escalado gradual (3D) |
| `ROT_SENSITIVITY` | 0.3f | Rotación fluida (3D) |
| `MOON_POS_SENSITIVITY` | 0.0005f | Posición de la luna (UV 0-1) |
| `MOON_RADIUS_SENSITIVITY` | 0.0002f | Tamaño de la luna |
| `BLDG_SENSITIVITY` | 0.001f | Offset/altura de edificios |

Ajusta estos valores si el movimiento es muy rápido o muy lento.

---

## Referencia: MoonlitCatScene

Escena de ejemplo que implementa este sistema:
- Archivo: `app/src/main/java/com/secret/blackholeglow/scenes/MoonlitCatScene.java`
- Objetos 3D: BlackCat3D, BrickWall3D (modelos de Meshy AI)
- Shader: NightSkyRenderer (cielo nocturno procedural + luna texturizada)
- Overlay: BuildingsSilhouette2D (silueta de edificios PNG)
- Última actualización: 2026-02-05

### Componentes de la Escena:
1. **NightSkyRenderer** - Cielo con gradiente, estrellas procedurales y luna
2. **BuildingsSilhouette2D** - Imagen PNG de edificios con transparencia
3. **BrickWall3D** - Modelo 3D de barda de ladrillos
4. **BlackCat3D** - Modelo 3D de gato negro

---

*Creado para Black Hole Glow v5.x*
