# 🛡️ PARÁMETROS DEL CAMPO DE FUERZA - GUÍA COMPLETA

## 📍 UBICACIÓN DE LOS ARCHIVOS

### 1. **SceneRenderer.java** (Creación y Configuración Inicial)
**Ruta**: `D:\Orbix\blackholeglow\app\src\main\java\com\secret\blackholeglow\SceneRenderer.java`

**Líneas 651-660** - CONFIGURACIÓN DEL FORCEFIELD:

```java
forceField = new ForceField(
    context, textureManager,
    0.0f, 0.0f, 0.0f,   // 🎯 Posición (X, Y, Z)
    1.70f,              // 🛡️ Tamaño (radio)
    R.drawable.fondo_transparente,  // Textura
    new float[]{0.3f, 0.9f, 1.0f},  // Color RGB
    0.0f,               // ✨ Transparencia base (alpha)
    0.03f,              // Pulsación (amplitud)
    0.3f                // Velocidad de pulsación
);
```

---

## 🎯 PARÁMETROS MODIFICABLES

### 1️⃣ POSICIÓN DEL CAMPO DE FUERZA (Línea 653)

```java
0.0f, 0.0f, 0.0f,   // X, Y, Z
```

- **X** = Horizontal (- = izquierda / + = derecha)
- **Y** = Vertical (- = abajo / + = arriba)
- **Z** = Profundidad (- = fondo / + = cerca)

**Configuración actual**: `(0.0, 0.0, 0.0)` = Centrado con la Tierra

**Ejemplos:**
```java
// Centrado (normal)
0.0f, 0.0f, 0.0f,

// Desplazado a la izquierda
-2.0f, 0.0f, 0.0f,

// Más alto
0.0f, 3.0f, 0.0f,

// Al fondo
0.0f, 0.0f, -5.0f,
```

---

### 2️⃣ TAMAÑO DEL CAMPO DE FUERZA (Línea 654)

```java
1.70f,              // Radio de la esfera
```

Este valor controla cuán grande es el campo de fuerza alrededor de la Tierra.

**Configuración actual**: `1.70f` (envuelve la Tierra con espacio generoso)

**Valores recomendados:**
- `1.3f` = Campo ajustado (cerca de la Tierra)
- `1.5f` = Campo estándar
- `1.70f` = Campo grande (actual)
- `2.0f` = Campo muy grande
- `2.5f` = Campo gigante

**Ejemplo:**
```java
// Campo más pequeño y pegado a la Tierra
1.4f,
```

---

### 3️⃣ COLOR DEL CAMPO DE FUERZA (Línea 656)

```java
new float[]{0.3f, 0.9f, 1.0f},  // RGB (Rojo, Verde, Azul)
```

Cada valor va de 0.0 (nada) a 1.0 (máximo).

**Configuración actual**: Azul eléctrico suave `(0.3, 0.9, 1.0)`
- R=0.3 (30% rojo)
- G=0.9 (90% verde)
- B=1.0 (100% azul)

**Paletas de colores:**

```java
// 🔵 AZUL ELÉCTRICO (actual)
new float[]{0.3f, 0.9f, 1.0f},

// 💚 VERDE ESMERALDA
new float[]{0.2f, 1.0f, 0.5f},

// 💜 MORADO ENERGÉTICO
new float[]{0.8f, 0.3f, 1.0f},

// 🔴 ROJO PELIGRO
new float[]{1.0f, 0.2f, 0.3f},

// 💛 AMARILLO DORADO
new float[]{1.0f, 0.9f, 0.3f},

// 🩷 ROSA NEÓN
new float[]{1.0f, 0.3f, 0.7f},

// 🧡 NARANJA FUEGO
new float[]{1.0f, 0.5f, 0.1f},

// 🤍 BLANCO PURO
new float[]{1.0f, 1.0f, 1.0f},
```

---

### 4️⃣ TRANSPARENCIA BASE (Alpha) (Línea 657)

```java
0.0f,               // Transparencia base
```

Controla cuán visible es el campo cuando NO hay impactos.

**Configuración actual**: `0.0f` (casi invisible, solo se ve con impactos)

**Valores recomendados:**
- `0.0f` = Invisible (solo impactos visibles) **(ACTUAL)**
- `0.1f` = Apenas visible
- `0.3f` = Levemente visible
- `0.5f` = Moderadamente visible
- `0.7f` = Claramente visible
- `1.0f` = Totalmente opaco

**Ejemplo para campo siempre visible:**
```java
0.4f,  // Campo moderadamente visible siempre
```

---

### 5️⃣ PULSACIÓN DEL CAMPO (Líneas 658-659)

```java
0.03f,              // Amplitud de pulsación (3%)
0.3f                // Velocidad de pulsación
```

#### Amplitud (línea 658):
Controla cuánto crece/decrece el campo al pulsar.

- `0.01f` = Pulsación muy sutil (1%)
- `0.03f` = Pulsación sutil (3%) **(ACTUAL)**
- `0.05f` = Pulsación moderada (5%)
- `0.10f` = Pulsación notable (10%)
- `0.20f` = Pulsación dramática (20%)

#### Velocidad (línea 659):
Controla cuán rápido pulsa.

- `0.1f` = Muy lento (respiración)
- `0.3f` = Lento **(ACTUAL)**
- `0.5f` = Moderado
- `1.0f` = Rápido
- `2.0f` = Muy rápido

**Ejemplo para pulsación dramática:**
```java
0.08f,  // 8% de variación
1.0f    // Pulso rápido
```

---

### 6️⃣ VELOCIDAD DE ROTACIÓN

**Archivo**: `ForceField.java`
**Línea 94**:

```java
this.rotationSpeed = 5.0f;
```

Controla cuán rápido gira el campo de fuerza.

**Valores:**
- `0.0f` = Sin rotación (estático)
- `5.0f` = Rotación lenta **(ACTUAL)**
- `10.0f` = Rotación moderada
- `20.0f` = Rotación rápida
- `50.0f` = Rotación muy rápida

---

## 🎨 PARÁMETROS DEL SHADER (Apariencia Visual)

### Archivo: `plasma_forcefield_fragment.glsl`
**Ruta**: `D:\Orbix\blackholeglow\app\src\main\assets\shaders\plasma_forcefield_fragment.glsl`

---

### 7️⃣ GROSOR DE LAS LÍNEAS HEXAGONALES (Línea 384)

```glsl
float hexGrid = hexagonalGrid(uv, hexScale, 0.08);
```

El último número (`0.08`) controla el grosor de las líneas hexagonales.

**Valores recomendados:**
- `0.04` = Líneas muy finas (delicadas)
- `0.08` = Líneas normales **(ACTUAL)**
- `0.12` = Líneas gruesas
- `0.20` = Líneas muy gruesas

**Ejemplo:**
```glsl
// Líneas hexagonales más gruesas y visibles
float hexGrid = hexagonalGrid(uv, hexScale, 0.15);
```

---

### 8️⃣ CANTIDAD DE HEXÁGONOS (Línea 381)

```glsl
float hexScale = 8.0 + musicIntensity * 2.0;
```

El primer número (`8.0`) controla cuántos hexágonos hay.

**Valores:**
- `4.0` = Hexágonos muy grandes (pocos)
- `8.0` = Hexágonos normales **(ACTUAL)**
- `12.0` = Hexágonos pequeños (muchos)
- `16.0` = Hexágonos muy pequeños (muchos más)

**Ejemplo:**
```glsl
// Más hexágonos pequeños (patrón más denso)
float hexScale = 12.0 + musicIntensity * 2.0;
```

---

### 9️⃣ COLOR DE LOS HEXÁGONOS (Líneas 391-392)

```glsl
vec3 hexColor = vec3(0.2, 0.7, 1.0);  // Azul cyan brillante
vec3 hexGlow = vec3(0.4, 0.9, 1.0);   // Azul claro brillante para bordes
```

**Paletas recomendadas:**

```glsl
// 💚 VERDE MATRIX
vec3 hexColor = vec3(0.0, 0.8, 0.3);
vec3 hexGlow = vec3(0.2, 1.0, 0.5);

// 💜 MORADO ENERGÉTICO
vec3 hexColor = vec3(0.7, 0.2, 1.0);
vec3 hexGlow = vec3(0.9, 0.4, 1.0);

// 🔴 ROJO ALERTA
vec3 hexColor = vec3(1.0, 0.2, 0.2);
vec3 hexGlow = vec3(1.0, 0.4, 0.4);

// 💛 DORADO ÉLITE
vec3 hexColor = vec3(1.0, 0.8, 0.2);
vec3 hexGlow = vec3(1.0, 1.0, 0.4);

// 🤍 BLANCO SCI-FI
vec3 hexColor = vec3(0.8, 0.9, 1.0);
vec3 hexGlow = vec3(1.0, 1.0, 1.0);
```

---

### 🔟 GROSOR DE LOS RAYOS ELÉCTRICOS (Línea 88)

```glsl
float boltWidth = 0.0008;  // Extremadamente delgado
```

Controla cuán gruesos son los rayos eléctricos.

**Valores:**
- `0.0005` = Rayos super finos (casi invisibles)
- `0.0008` = Rayos finos **(ACTUAL)**
- `0.0015` = Rayos medianos
- `0.003` = Rayos gruesos
- `0.005` = Rayos muy gruesos

**Ejemplo:**
```glsl
// Rayos más visibles y gruesos
float boltWidth = 0.002;
```

---

### 1️⃣1️⃣ CANTIDAD DE RAYOS ELÉCTRICOS (Línea 110)

```glsl
int numRays = 3 + int(musicIntensity * 2.0); // 3-5 rayos
```

El primer número (`3`) es la cantidad mínima de rayos.

**Valores:**
- `2` = Pocos rayos (minimalista)
- `3` = Cantidad normal **(ACTUAL)**
- `5` = Muchos rayos
- `8` = Abundantes rayos

**Ejemplo:**
```glsl
// Más rayos eléctricos
int numRays = 5 + int(musicIntensity * 3.0); // 5-8 rayos
```

---

### 1️⃣2️⃣ INTENSIDAD DE LOS RAYOS (Línea 284)

```glsl
rays *= 0.25;  // Solo 25% de intensidad
```

Controla cuán brillantes son los rayos.

**Valores:**
- `0.15` = Rayos apenas visibles
- `0.25` = Rayos sutiles **(ACTUAL)**
- `0.5` = Rayos moderados
- `0.8` = Rayos brillantes
- `1.5` = Rayos muy brillantes

**Ejemplo:**
```glsl
// Rayos más brillantes y visibles
rays *= 0.6;
```

---

### 1️⃣3️⃣ TRANSPARENCIA DE LOS HEXÁGONOS (Línea 446)

```glsl
finalAlpha += hexGrid * 0.35; // Líneas hexagonales visibles (35% en líneas)
```

El último número (`0.35`) controla cuán opacas son las líneas.

**Valores:**
- `0.2` = Líneas muy transparentes
- `0.35` = Líneas moderadamente visibles **(ACTUAL)**
- `0.5` = Líneas claramente visibles
- `0.7` = Líneas muy opacas
- `1.0` = Líneas totalmente sólidas

**Ejemplo:**
```glsl
// Hexágonos más visibles y sólidos
finalAlpha += hexGrid * 0.6;
```

---

### 1️⃣4️⃣ TAMAÑO DE LAS ONDAS DE IMPACTO (Línea 340)

```glsl
float impactRadius = 0.9;  // Radio más grande para ondas más visibles
```

Controla cuán grandes son las ondas cuando impactan meteoritos.

**Valores:**
- `0.5` = Ondas pequeñas (localizadas)
- `0.9` = Ondas grandes **(ACTUAL)**
- `1.2` = Ondas muy grandes
- `1.5` = Ondas gigantes (cubren todo)

**Ejemplo:**
```glsl
// Ondas de impacto más dramáticas
float impactRadius = 1.3;
```

---

### 1️⃣5️⃣ VELOCIDAD DE LAS ONDAS (Líneas 347-349)

```glsl
float wave1 = sin(impactDist * 18.0 - effectiveTime * 18.0) * 0.5 + 0.5;
float wave2 = sin(impactDist * 28.0 - effectiveTime * 24.0) * 0.5 + 0.5;
float wave3 = sin(impactDist * 38.0 - effectiveTime * 30.0) * 0.5 + 0.5;
```

Los números después de `effectiveTime *` controlan la velocidad de expansión:

**Valores actuales**: `18.0`, `24.0`, `30.0` (rápido)

**Para ondas más lentas:**
```glsl
float wave1 = sin(impactDist * 18.0 - effectiveTime * 10.0) * 0.5 + 0.5;  // Más lento
float wave2 = sin(impactDist * 28.0 - effectiveTime * 14.0) * 0.5 + 0.5;
float wave3 = sin(impactDist * 38.0 - effectiveTime * 18.0) * 0.5 + 0.5;
```

**Para ondas más rápidas:**
```glsl
float wave1 = sin(impactDist * 18.0 - effectiveTime * 30.0) * 0.5 + 0.5;  // Muy rápido
float wave2 = sin(impactDist * 28.0 - effectiveTime * 40.0) * 0.5 + 0.5;
float wave3 = sin(impactDist * 38.0 - effectiveTime * 50.0) * 0.5 + 0.5;
```

---

## 🚀 CÓMO APLICAR LOS CAMBIOS

1. **Edita el archivo** correspondiente
2. **Compila la app**:
   ```bash
   ./gradlew.bat assembleDebug --no-daemon
   ```

3. **Instala**:
   ```bash
   D:/adb/platform-tools/adb.exe install -r "D:/Orbix/blackholeglow/app/build/outputs/apk/debug/app-debug.apk"
   ```

4. **Ejecuta**:
   ```bash
   D:/adb/platform-tools/adb.exe shell am start -n com.secret.blackholeglow/.LoginActivity
   ```

**Todo en un comando:**
```bash
./gradlew.bat assembleDebug --no-daemon && D:/adb/platform-tools/adb.exe install -r "D:/Orbix/blackholeglow/app/build/outputs/apk/debug/app-debug.apk" && D:/adb/platform-tools/adb.exe shell am start -n com.secret.blackholeglow/.LoginActivity
```

---

## 📊 CONFIGURACIÓN ACTUAL

### Posición y Tamaño:
```java
// SceneRenderer.java líneas 653-654
0.0f, 0.0f, 0.0f,   // Centrado con la Tierra
1.70f,              // Campo grande
```

### Color y Transparencia:
```java
// SceneRenderer.java líneas 656-657
new float[]{0.3f, 0.9f, 1.0f},  // Azul eléctrico
0.0f,                           // Casi invisible (solo impactos)
```

### Hexágonos:
```glsl
// plasma_forcefield_fragment.glsl
float hexScale = 8.0;           // Cantidad normal
float hexGrid = ... 0.08);      // Líneas normales
finalAlpha += hexGrid * 0.35;   // 35% visibles
```

### Rayos:
```glsl
// plasma_forcefield_fragment.glsl
int numRays = 3;                // 3-5 rayos
float boltWidth = 0.0008;       // Rayos finos
rays *= 0.25;                   // 25% intensidad
```

---

## 💡 CONFIGURACIONES RECOMENDADAS

### 🔵 CAMPO DEFENSIVO SUTIL (Actual):
```java
1.70f,  // Grande
new float[]{0.3f, 0.9f, 1.0f},  // Azul
0.0f,  // Invisible hasta impacto
```

### 💚 CAMPO MATRIX (Verde brillante):
```java
1.6f,  // Mediano
new float[]{0.2f, 1.0f, 0.4f},  // Verde neón
0.3f,  // Siempre visible
```
```glsl
// Hexágonos verdes
vec3 hexColor = vec3(0.0, 0.9, 0.3);
vec3 hexGlow = vec3(0.2, 1.0, 0.5);
```

### 🔴 CAMPO ALERTA (Rojo peligro):
```java
1.8f,  // Grande
new float[]{1.0f, 0.2f, 0.2f},  // Rojo intenso
0.4f,  // Bien visible
```
```glsl
// Hexágonos rojos
vec3 hexColor = vec3(1.0, 0.2, 0.2);
vec3 hexGlow = vec3(1.0, 0.4, 0.3);
// Más rayos
int numRays = 6 + int(musicIntensity * 3.0);
```

### 💜 CAMPO ALIENÍGENA (Morado místico):
```java
2.0f,  // Muy grande
new float[]{0.8f, 0.3f, 1.0f},  // Morado
0.2f,  // Levemente visible
```
```glsl
// Hexágonos morados
vec3 hexColor = vec3(0.7, 0.2, 1.0);
vec3 hexGlow = vec3(0.9, 0.4, 1.0);
// Rayos más gruesos
float boltWidth = 0.002;
```

---

¡Experimenta y crea tu campo de fuerza perfecto! 🛡️⚡
