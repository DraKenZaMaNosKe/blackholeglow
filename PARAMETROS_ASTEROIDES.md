# 🪨 GUÍA COMPLETA DE PARÁMETROS DE ASTEROIDES

## 📍 ARCHIVOS PRINCIPALES

### 1. **MeteorShower.java** - Tamaños y Comportamiento
**Ruta**: `D:\Orbix\blackholeglow\app\src\main\java\com\secret\blackholeglow\MeteorShower.java`

### 2. **Meteorito.java** - Visuales y Shaders
**Ruta**: `D:\Orbix\blackholeglow\app\src\main\java\com\secret\blackholeglow\Meteorito.java`

### 3. **meteorito_fragment.glsl** - Efectos Visuales
**Ruta**: `D:\Orbix\blackholeglow\app\src\main\assets\shaders\meteorito_fragment.glsl`

---

## 🔧 PARTE 1: TAMAÑOS DE LOS ASTEROIDES

### 📏 A) METEORITOS NORMALES (líneas 345-361 en MeteorShower.java)

**TAMAÑOS ACTUALES (MUY GRANDES):**
```java
// Pequeños (50%)
tamaño = 0.04f + (float) Math.random() * 0.06f;  // 0.04-0.10

// Medianos (30%)
tamaño = 0.10f + (float) Math.random() * 0.08f;  // 0.10-0.18

// Grandes (20%)
tamaño = 0.18f + (float) Math.random() * 0.10f;  // 0.18-0.28
```

**✅ TAMAÑOS REDUCIDOS (RECOMENDADOS):**
```java
// Pequeños (50%)
tamaño = 0.02f + (float) Math.random() * 0.03f;  // 0.02-0.05

// Medianos (30%)
tamaño = 0.05f + (float) Math.random() * 0.04f;  // 0.05-0.09

// Grandes (20%)
tamaño = 0.09f + (float) Math.random() * 0.05f;  // 0.09-0.14
```

**💡 Para ajustar:**
- Primer número = Tamaño mínimo
- Segundo número (en `Math.random() *`) = Rango de variación
- Suma de ambos = Tamaño máximo

---

### 🔫 B) PROYECTILES DEL ARMA (líneas 415-431 en MeteorShower.java)

**TAMAÑOS ACTUALES:**
```java
// Pequeños (50%)
tamaño = 0.08f + (float) Math.random() * 0.06f;  // 0.08-0.14

// Medianos (30%)
tamaño = 0.14f + (float) Math.random() * 0.08f;  // 0.14-0.22

// Grandes (20%)
tamaño = 0.22f + (float) Math.random() * 0.10f;  // 0.22-0.32
```

**✅ TAMAÑOS REDUCIDOS (RECOMENDADOS):**
```java
// Pequeños (50%)
tamaño = 0.03f + (float) Math.random() * 0.02f;  // 0.03-0.05

// Medianos (30%)
tamaño = 0.05f + (float) Math.random() * 0.03f;  // 0.05-0.08

// Grandes (20%)
tamaño = 0.08f + (float) Math.random() * 0.04f;  // 0.08-0.12
```

---

### 💥 C) METEORITOS A PANTALLA (GRIETAS) - líneas 518-531

**TAMAÑOS ACTUALES (GIGANTES):**
```java
// Grandes (50%)
tamaño = 0.25f + (float) Math.random() * 0.10f;  // 0.25-0.35

// Muy grandes (30%)
tamaño = 0.35f + (float) Math.random() * 0.10f;  // 0.35-0.45

// Gigantes (20%)
tamaño = 0.45f + (float) Math.random() * 0.15f;  // 0.45-0.60
```

**✅ TAMAÑOS REDUCIDOS (RECOMENDADOS):**
```java
// Grandes (50%)
tamaño = 0.12f + (float) Math.random() * 0.05f;  // 0.12-0.17

// Muy grandes (30%)
tamaño = 0.17f + (float) Math.random() * 0.05f;  // 0.17-0.22

// Gigantes (20%)
tamaño = 0.22f + (float) Math.random() * 0.08f;  // 0.22-0.30
```

---

### 🌟 D) METEORITOS ÉPICOS (COMBO x10) - líneas 932-934

**TAMAÑOS ACTUALES:**
```java
tamaño = 0.25f + (float) Math.random() * 0.2f;  // 0.25-0.45
```

**✅ TAMAÑOS REDUCIDOS (RECOMENDADOS):**
```java
tamaño = 0.12f + (float) Math.random() * 0.10f;  // 0.12-0.22
```

---

## 🎨 PARTE 2: COLORES Y EFECTOS VISUALES

### 🌈 A) COLOR BASE DEL ASTEROIDE (Meteorito.java líneas 157-163)

**Código actual:**
```java
// Color aleatorio (variaciones de fuego)
float r = 0.8f + (float) Math.random() * 0.2f;  // Rojo: 0.8-1.0
float g = 0.4f + (float) Math.random() * 0.3f;  // Verde: 0.4-0.7
float b = 0.1f + (float) Math.random() * 0.2f;  // Azul: 0.1-0.3
```

**Variaciones de color:**

```java
// 🔥 ROJO LAVA (ardiente)
float r = 1.0f;
float g = 0.3f + (float) Math.random() * 0.2f;  // 0.3-0.5
float b = 0.0f;

// 🌑 GRIS OSCURO (asteroide rocoso)
float r = 0.4f + (float) Math.random() * 0.2f;  // 0.4-0.6
float g = 0.4f + (float) Math.random() * 0.2f;  // 0.4-0.6
float b = 0.4f + (float) Math.random() * 0.2f;  // 0.4-0.6

// 💎 AZUL CRISTAL (hielo espacial)
float r = 0.2f + (float) Math.random() * 0.3f;  // 0.2-0.5
float g = 0.5f + (float) Math.random() * 0.3f;  // 0.5-0.8
float b = 0.8f + (float) Math.random() * 0.2f;  // 0.8-1.0

// 🟢 VERDE TÓXICO (radioactivo)
float r = 0.2f + (float) Math.random() * 0.2f;  // 0.2-0.4
float g = 0.8f + (float) Math.random() * 0.2f;  // 0.8-1.0
float b = 0.2f + (float) Math.random() * 0.2f;  // 0.2-0.4

// 🟣 MORADO MÍSTICO (energía arcana)
float r = 0.6f + (float) Math.random() * 0.3f;  // 0.6-0.9
float g = 0.2f + (float) Math.random() * 0.2f;  // 0.2-0.4
float b = 0.8f + (float) Math.random() * 0.2f;  // 0.8-1.0

// 🟡 DORADO BRILLANTE (metal precioso)
float r = 1.0f;
float g = 0.8f + (float) Math.random() * 0.2f;  // 0.8-1.0
float b = 0.2f + (float) Math.random() * 0.2f;  // 0.2-0.4
```

---

### ✨ B) VELOCIDAD DE ROTACIÓN (Meteorito.java línea 149)

**Código actual:**
```java
velocidadRotacion = (float) (Math.random() * 200 + 50);  // 50-250°/seg
```

**Variaciones:**
```java
// Rotación lenta (asteroides grandes)
velocidadRotacion = (float) (Math.random() * 50 + 20);   // 20-70°/seg

// Rotación normal
velocidadRotacion = (float) (Math.random() * 100 + 50);  // 50-150°/seg

// Rotación rápida (fragmentos pequeños)
velocidadRotacion = (float) (Math.random() * 300 + 100); // 100-400°/seg

// Rotación errática (caótica)
velocidadRotacion = (float) (Math.random() * 500 + 200); // 200-700°/seg
```

---

## 🔥 PARTE 3: EFECTOS DEL SHADER

### 📄 Archivo: `meteorito_fragment.glsl`

El shader actual usa estos uniforms que puedes modificar en el código Java:

#### 1. **u_Speed** (velocidad del meteorito)
Afecta la intensidad de los efectos visuales

#### 2. **u_Temperature** (temperatura del meteorito)
- `0.0-0.3` = Frío (tonos azules/grises)
- `0.4-0.6` = Templado (naranja)
- `0.7-1.0` = Caliente (rojo brillante)

**Modificar en Meteorito.java líneas 328-332:**
```java
// Temperatura basada en el tipo de estela
if (uTemperatureLoc >= 0) {
    float temperature = (trail != null &&
        trail.toString().contains("PLASMA")) ? 0.8f : 0.2f;
    GLES20.glUniform1f(uTemperatureLoc, temperature);
}
```

**Variaciones:**
```java
// Asteroide frío (hielo)
float temperature = 0.1f;

// Asteroide rocoso (normal)
float temperature = 0.4f;

// Asteroide ardiente (lava)
float temperature = 0.9f;

// Temperatura aleatoria
float temperature = (float) Math.random();
```

#### 3. **u_ImpactPower** (poder de impacto)
Controla el brillo durante el impacto

**Modificar en Meteorito.java líneas 335-338:**
```java
if (uImpactPowerLoc >= 0) {
    float impactPower = estado == Estado.IMPACTANDO ? 2.0f : 1.0f;
    GLES20.glUniform1f(uImpactPowerLoc, impactPower);
}
```

---

## 💡 PROPUESTAS DE EFECTOS ESPECIALES

### 1. **ASTEROIDE PULSANTE**
Añadir variación de tamaño en el tiempo (en Meteorito.java método `update`):
```java
// Después de línea 232 (en el método update)
float pulse = 1.0f + 0.1f * (float) Math.sin(tiempoVida * 5.0f);
// Usar este 'pulse' al escalar en el draw()
```

### 2. **ASTEROIDE CON TRAIL DE PARTÍCULAS**
Ya existe el sistema `MeteorTrail`, puedes modificar:
- `MeteorTrail.TrailType.FIRE` = Estela de fuego
- `MeteorTrail.TrailType.PLASMA` = Estela de plasma

**Modificar en Meteorito.java líneas 115-117:**
```java
// Cambiar probabilidad de tipos
MeteorTrail.TrailType trailType = Math.random() < 0.7 ?  // 70% fuego
    MeteorTrail.TrailType.FIRE : MeteorTrail.TrailType.PLASMA;
```

### 3. **ASTEROIDE EXPLOSIVO (al impactar)**
Ya existe el sistema `MeteorExplosion` (línea 188 en Meteorito.java)

Puedes modificar la clase `MeteorExplosion` para:
- Más fragmentos
- Fragmentos más grandes
- Colores diferentes
- Velocidad de fragmentos

### 4. **ASTEROIDE CON GLOW (BRILLO)**
Añadir en el shader `meteorito_fragment.glsl`:
```glsl
// Agregar al final del shader, antes del gl_FragColor
vec3 glow = baseColor * 0.5 * u_Temperature;
finalColor += glow;
```

### 5. **ASTEROIDE CON AURA ELÉCTRICA**
Modificar el shader para añadir rayos eléctricos alrededor

---

## 🚀 CÓMO APLICAR LOS CAMBIOS

1. **Edita el archivo** correspondiente según la tabla de arriba
2. **Compila**:
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

**O todo en un comando:**
```bash
./gradlew.bat assembleDebug --no-daemon && D:/adb/platform-tools/adb.exe install -r "D:/Orbix/blackholeglow/app/build/outputs/apk/debug/app-debug.apk" && D:/adb/platform-tools/adb.exe shell am start -n com.secret.blackholeglow/.LoginActivity
```

---

## 📊 TABLA RESUMEN DE PARÁMETROS

| Parámetro | Archivo | Línea | Qué Controla |
|-----------|---------|-------|--------------|
| Tamaño meteoritos normales | MeteorShower.java | 345-361 | Tamaño base de asteroides |
| Tamaño proyectiles arma | MeteorShower.java | 415-431 | Tamaño de proyectiles disparados |
| Tamaño meteoritos pantalla | MeteorShower.java | 518-531 | Asteroides que causan grietas |
| Tamaño meteoritos épicos | MeteorShower.java | 932-934 | Combo x10 |
| Color del asteroide | Meteorito.java | 157-163 | RGB del asteroide |
| Velocidad de rotación | Meteorito.java | 149 | Grados por segundo |
| Temperatura visual | Meteorito.java | 328-332 | Efecto de calor en shader |
| Poder de impacto | Meteorito.java | 335-338 | Brillo al impactar |
| Tipo de estela | Meteorito.java | 115-117 | FIRE o PLASMA |

---

¡Experimenta y diviértete ajustando los asteroides! 🪨✨
