# ☀️ PARÁMETROS DEL SOL REALISTA - GUÍA DE MODIFICACIÓN

## 📍 UBICACIÓN DE LOS ARCHIVOS

### 1. **SceneRenderer.java** (Posición y Tamaño en la Escena)
**Ruta**: `D:\Orbix\blackholeglow\app\src\main\java\com\secret\blackholeglow\SceneRenderer.java`

**Líneas 553-554** - POSICIÓN Y TAMAÑO DEL SOL:

```java
solRealista.setPosition(-2.0f, 3.5f, -8.0f);  // ☀️ Arriba-izquierda-fondo
solRealista.setScale(0.3f);                    // ☀️ Sol pequeño
```

#### 📍 Modificar POSICIÓN (línea 553):
```java
solRealista.setPosition(X, Y, Z);
```

- **X** = Horizontal (- = izquierda / + = derecha)
  - Valores típicos: -5.0 (muy izquierda) a 5.0 (muy derecha)
  - Actual: `-2.0f` (ligeramente a la izquierda)

- **Y** = Vertical (- = abajo / + = arriba)
  - Valores típicos: -5.0 (muy abajo) a 8.0 (muy arriba)
  - Actual: `3.5f` (arriba)

- **Z** = Profundidad (- = fondo / + = cerca)
  - Valores típicos: -15.0 (muy al fondo) a -1.0 (cerca)
  - Actual: `-8.0f` (al fondo)

**Ejemplos de posiciones cinematográficas:**
```java
// Sol en el horizonte izquierdo (amanecer)
solRealista.setPosition(-4.0f, 2.0f, -12.0f);

// Sol arriba centrado (mediodía)
solRealista.setPosition(0.0f, 6.0f, -10.0f);

// Sol horizonte derecho (atardecer)
solRealista.setPosition(4.0f, 2.5f, -12.0f);

// Sol pequeño al fondo (estrella lejana)
solRealista.setPosition(0.0f, 4.0f, -20.0f);
```

#### 📏 Modificar TAMAÑO (línea 554):
```java
solRealista.setScale(TAMAÑO);
```

- **TAMAÑO** = Escala uniforme del modelo 3D
  - `0.2f` = Muy pequeño (estrella lejana)
  - `0.3f` = Pequeño (actual)
  - `0.5f` = Mediano
  - `1.0f` = Grande
  - `1.5f` = Muy grande (protagonista)
  - `2.0f` = Gigante

**Ejemplo:**
```java
// Sol protagonista grande
solRealista.setScale(1.2f);

// Sol discreto pequeño al fondo
solRealista.setScale(0.25f);
```

---

### 2. **SolRealista.java** (Velocidad de Rotación)
**Ruta**: `D:\Orbix\blackholeglow\app\src\main\java\com\secret\blackholeglow\SolRealista.java`

**Línea 49** - VELOCIDAD DE ROTACIÓN:

```java
private float spinSpeed = 10.0f;
```

#### 🔄 Modificar VELOCIDAD DE ROTACIÓN:
- **spinSpeed** = Grados por segundo
  - `0.0f` = Sin rotación (estático)
  - `5.0f` = Rotación muy lenta
  - `10.0f` = Rotación lenta (actual)
  - `30.0f` = Rotación moderada
  - `80.0f` = Rotación rápida
  - `200.0f` = Rotación muy rápida

**Ejemplo:**
```java
private float spinSpeed = 15.0f;  // Rotación suave y visible
```

---

### 3. **sol_lava_fragment.glsl** (Apariencia Visual - Shader)
**Ruta**: `D:\Orbix\blackholeglow\app\src\main\assets\shaders\sol_lava_fragment.glsl`

#### 🎨 Modificar COLORES DEL SOL (líneas 64-66):

```glsl
vec3 coreColor = vec3(1.5, 1.3, 1.0);        // Centro: MUY brillante
vec3 midColor = vec3(1.2, 1.0, 0.7);         // Medio: amarillo-dorado
vec3 edgeColor = vec3(1.0, 0.5, 0.3);        // Borde: naranja-rojo intenso
```

**Formato**: `vec3(ROJO, VERDE, AZUL)` - valores de 0.0 a 2.0+

**Ejemplos de paletas:**

```glsl
// ☀️ SOL DORADO BRILLANTE (más amarillo)
vec3 coreColor = vec3(2.0, 1.8, 1.2);
vec3 midColor = vec3(1.5, 1.3, 0.8);
vec3 edgeColor = vec3(1.2, 0.8, 0.3);

// 🔥 SOL ROJIZO INTENSO (tipo enana roja)
vec3 coreColor = vec3(1.8, 1.0, 0.5);
vec3 midColor = vec3(1.5, 0.7, 0.3);
vec3 edgeColor = vec3(1.2, 0.4, 0.2);

// ⭐ SOL AZULADO (tipo estrella caliente)
vec3 coreColor = vec3(1.2, 1.5, 2.0);
vec3 midColor = vec3(0.9, 1.2, 1.6);
vec3 edgeColor = vec3(0.7, 1.0, 1.4);

// 🌅 SOL ANARANJADO SUAVE (atardecer)
vec3 coreColor = vec3(1.8, 1.4, 0.8);
vec3 midColor = vec3(1.5, 1.0, 0.5);
vec3 edgeColor = vec3(1.3, 0.6, 0.2);
```

#### ✨ Modificar INTENSIDAD DEL PLASMA (línea 97):
```glsl
baseColor += vec3(plasmaTotal * 0.2, plasmaTotal * 0.15, plasmaTotal * 0.08);
```

- Primer número (0.2) = Intensidad rojo del plasma
- Segundo número (0.15) = Intensidad verde del plasma
- Tercer número (0.08) = Intensidad azul del plasma

**Para más plasma visible:**
```glsl
baseColor += vec3(plasmaTotal * 0.4, plasmaTotal * 0.3, plasmaTotal * 0.15);
```

**Para menos plasma:**
```glsl
baseColor += vec3(plasmaTotal * 0.1, plasmaTotal * 0.08, plasmaTotal * 0.04);
```

#### 💫 Modificar INTENSIDAD DE LA CORONA (línea 112):
```glsl
baseColor += coronaColor * coronaMask * coronaPulse * 0.4;
```

El último número (0.4) controla cuán visible es la corona:
- `0.2` = Corona muy sutil
- `0.4` = Corona moderada (actual)
- `0.8` = Corona brillante
- `1.5` = Corona muy prominente

#### 🔆 Modificar BRILLO DEL NÚCLEO (línea 119):
```glsl
baseColor += vec3(1.2, 1.1, 0.9) * coreBrightness * 0.5;
```

El último número (0.5) controla el brillo del centro:
- `0.3` = Núcleo menos brillante
- `0.5` = Núcleo moderado (actual)
- `1.0` = Núcleo muy brillante
- `2.0` = Núcleo super brillante (puede saturar)

---

## 🚀 CÓMO APLICAR LOS CAMBIOS

1. **Edita el archivo** que quieres modificar
2. **Compila la app**:
   ```bash
   ./gradlew.bat assembleDebug --no-daemon
   ```

3. **Instala en el dispositivo**:
   ```bash
   D:/adb/platform-tools/adb.exe install -r "D:/Orbix/blackholeglow/app/build/outputs/apk/debug/app-debug.apk"
   ```

4. **Inicia la app**:
   ```bash
   D:/adb/platform-tools/adb.exe shell am start -n com.secret.blackholeglow/.LoginActivity
   ```

**O todo en un comando:**
```bash
./gradlew.bat assembleDebug --no-daemon && D:/adb/platform-tools/adb.exe install -r "D:/Orbix/blackholeglow/app/build/outputs/apk/debug/app-debug.apk" && D:/adb/platform-tools/adb.exe shell am start -n com.secret.blackholeglow/.LoginActivity
```

---

## 📊 VALORES ACTUALES (Configuración Cinematográfica)

### Posición y Tamaño:
```java
// SceneRenderer.java línea 553-554
solRealista.setPosition(-2.0f, 3.5f, -8.0f);  // Arriba-izquierda-fondo
solRealista.setScale(0.3f);                    // Sol pequeño
```

### Rotación:
```java
// SolRealista.java línea 49
private float spinSpeed = 10.0f;  // Rotación lenta
```

### Apariencia:
```glsl
// sol_lava_fragment.glsl líneas 64-66
vec3 coreColor = vec3(1.5, 1.3, 1.0);    // Centro dorado brillante
vec3 midColor = vec3(1.2, 1.0, 0.7);     // Medio amarillo
vec3 edgeColor = vec3(1.0, 0.5, 0.3);    // Borde naranja-rojo
```

---

## 💡 TIPS PARA EXPERIM ENTAR

1. **Para un Sol dramático tipo atardecer:**
   - Posición: Horizontal cerca del borde (X = ±4.0), bajo (Y = 2.0), al fondo (Z = -12.0)
   - Tamaño: Mediano-grande (0.8 - 1.2)
   - Colores: Rojizos/anaranjados intensos

2. **Para un Sol discreto al fondo:**
   - Posición: Centrado (X = 0.0), alto (Y = 5.0), muy al fondo (Z = -20.0)
   - Tamaño: Muy pequeño (0.15 - 0.25)
   - Colores: Amarillos brillantes

3. **Para un Sol protagonista:**
   - Posición: Cerca (Z = -5.0), centrado o ligeramente lateral
   - Tamaño: Grande (1.0 - 1.5)
   - Colores: Dorados intensos con corona prominente

---

¡Experimenta y diviértete ajustando el Sol! ☀️
