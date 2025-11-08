# 🎵 GUÍA COMPLETA DE PERSONALIZACIÓN DEL ECUALIZADOR

**Fecha**: Noviembre 2025
**Versión**: 4.0.0
**Archivo**: `MusicIndicator.java`

---

## 📋 TABLA DE CONTENIDOS

1. [Configuración Básica](#configuración-básica)
2. [Rangos de Frecuencia](#rangos-de-frecuencia)
3. [Posición y Tamaño](#posición-y-tamaño)
4. [Colores y Gradientes](#colores-y-gradientes)
5. [Sensibilidad y Reactividad](#sensibilidad-y-reactividad)
6. [Efectos Visuales](#efectos-visuales)
7. [Troubleshooting](#troubleshooting)

---

## 🎯 CONFIGURACIÓN BÁSICA

### Ubicación del Archivo
```
app/src/main/java/com/secret/blackholeglow/MusicIndicator.java
```

### Parámetros Principales (Líneas 19-21)

```java
private static final int NUM_BARRAS = 6;      // Número de barras del ecualizador
private static final int LEDS_POR_BARRA = 14; // LEDs/segmentos por barra (altura)
```

#### ¿Qué hace cada parámetro?

| Parámetro | Valor Actual | Descripción | Rango Recomendado |
|-----------|--------------|-------------|-------------------|
| `NUM_BARRAS` | 6 | Número de barras verticales | 3-10 |
| `LEDS_POR_BARRA` | 14 | Segmentos de cada barra (resolución vertical) | 8-20 |

**Ejemplo: Ecualizador más detallado**
```java
private static final int NUM_BARRAS = 10;     // Más barras = más detalle
private static final int LEDS_POR_BARRA = 20; // Más LEDs = animación más suave
```

**Ejemplo: Ecualizador retro/pixelado**
```java
private static final int NUM_BARRAS = 4;      // Pocas barras = estilo retro
private static final int LEDS_POR_BARRA = 8;  // Pocos LEDs = estilo 8-bit
```

---

## 🎼 RANGOS DE FRECUENCIA

### Configuración Actual (6 Barras)

```java
// Líneas 40-49
// Barra 0: SUB-BASS    60-250 Hz    (Bombo, bajo profundo) 🥁
// Barra 1: BASS        250-500 Hz   (Bajo, guitarra baja) 🎸
// Barra 2: MID-LOW     500-2000 Hz  (Voces graves, piano) 🎤
// Barra 3: MID-HIGH    2000-4000 Hz (Voces agudas, trompeta) 🎺
// Barra 4: PRESENCE    4000-8000 Hz (Violín, claridad vocal) 🎻
// Barra 5: TREBLE      8000-16000 Hz (Platillos, brillo) ✨
```

### Distribución de Frecuencias (Líneas 145-161)

```java
// Barra 0: SUB-BASS (graves extremos)
barLevels[0] = bass * 1.2f;

// Barra 1: BASS (graves normales)
barLevels[1] = bass * 0.7f + mid * 0.3f;

// Barra 2: MID-LOW (medios graves)
barLevels[2] = bass * 0.2f + mid * 0.8f;

// Barra 3: MID-HIGH (medios agudos)
barLevels[3] = mid * 0.6f + treble * 0.4f;

// Barra 4: PRESENCE (presencia)
barLevels[4] = mid * 0.3f + treble * 0.7f;

// Barra 5: TREBLE (agudos extremos)
barLevels[5] = treble * 1.2f;
```

### 🎨 Cómo Personalizar las Frecuencias

#### Ejemplo 1: Énfasis en BAJOS (música electrónica)
```java
barLevels[0] = bass * 1.5f;  // SUB-BASS muy amplificado
barLevels[1] = bass * 1.0f;  // BASS puro
barLevels[2] = bass * 0.5f + mid * 0.5f;
barLevels[3] = mid * 0.7f + treble * 0.3f;
barLevels[4] = mid * 0.4f + treble * 0.6f;
barLevels[5] = treble * 0.8f;  // TREBLE reducido
```

#### Ejemplo 2: Énfasis en VOCES (pop, vocal)
```java
barLevels[0] = bass * 0.8f;  // BASS reducido
barLevels[1] = bass * 0.5f + mid * 0.5f;
barLevels[2] = mid * 1.2f;  // MID-LOW amplificado (voces)
barLevels[3] = mid * 1.3f + treble * 0.2f;  // MID-HIGH amplificado
barLevels[4] = mid * 0.5f + treble * 0.8f;
barLevels[5] = treble * 0.9f;
```

#### Ejemplo 3: Balance Equilibrado (rock, clásica)
```java
barLevels[0] = bass * 1.0f;
barLevels[1] = bass * 0.6f + mid * 0.4f;
barLevels[2] = bass * 0.3f + mid * 0.7f;
barLevels[3] = mid * 0.7f + treble * 0.3f;
barLevels[4] = mid * 0.4f + treble * 0.6f;
barLevels[5] = treble * 1.0f;
```

---

## 📐 POSICIÓN Y TAMAÑO

### Dónde se Configura

**Archivo**: `SceneRenderer.java`
**Líneas**: ~798-804

```java
musicIndicator = new MusicIndicator(
    context,
    -0.15f,   // X: Posición horizontal (centrado izquierda)
    0.75f,    // Y: Posición vertical (arriba)
    0.30f,    // Ancho del ecualizador
    0.08f     // Alto del ecualizador
);
```

### Sistema de Coordenadas OpenGL (NDC)

```
        (-1, 1) ┌────────────────────┐ (1, 1)
                │                    │
                │    PANTALLA        │
                │                    │
       (-1, -1) └────────────────────┘ (1, -1)
```

### Parámetros de Posición

| Parámetro | Valor Actual | Descripción | Rango |
|-----------|--------------|-------------|-------|
| **X** | -0.15 | Horizontal: Izquierda (-1) ← Centro (0) → Derecha (1) | -1.0 a 1.0 |
| **Y** | 0.75 | Vertical: Abajo (-1) ← Centro (0) → Arriba (1) | -1.0 a 1.0 |
| **Ancho** | 0.30 | Tamaño horizontal (0.30 = 30% del ancho de pantalla) | 0.1 a 2.0 |
| **Alto** | 0.08 | Tamaño vertical (0.08 = 8% del alto de pantalla) | 0.05 a 0.5 |

### 🎨 Ejemplos de Posiciones

#### Ecualizador Centrado Arriba (como imagen de referencia)
```java
musicIndicator = new MusicIndicator(
    context,
    0.0f,    // X: Centro horizontal
    0.80f,   // Y: Parte superior
    0.60f,   // Ancho: 60% de pantalla (más ancho)
    0.10f    // Alto: 10% de pantalla
);
```

#### Ecualizador Parte Inferior
```java
musicIndicator = new MusicIndicator(
    context,
    0.0f,    // X: Centro
    -0.70f,  // Y: Parte inferior (negativo = abajo)
    0.40f,   // Ancho
    0.08f    // Alto
);
```

#### Ecualizador Esquina Superior Derecha
```java
musicIndicator = new MusicIndicator(
    context,
    0.50f,   // X: Derecha
    0.75f,   // Y: Arriba
    0.35f,   // Ancho
    0.07f    // Alto
);
```

#### Ecualizador Grande (Pantalla Completa)
```java
musicIndicator = new MusicIndicator(
    context,
    0.0f,    // X: Centro
    0.0f,    // Y: Centro
    1.80f,   // Ancho: Casi toda la pantalla
    0.20f    // Alto: 20% de pantalla (muy alto)
);
```

---

## 🌈 COLORES Y GRADIENTES

### Sistema de Colores Actual

**Archivo**: `MusicIndicator.java`
**Método**: `draw()` (líneas ~200-250)

El ecualizador usa un **gradiente vertical** por altura:
- **Verde** (bajo) → **Amarillo** (medio) → **Rojo** (alto)

### Código de Colores (buscar en `draw()`)

```java
// Ejemplo simplificado del código de colores
float greenIntensity = ledHeight;     // Verde en base
float yellowIntensity = ledHeight * ledHeight; // Amarillo en medio
float redIntensity = ledHeight * ledHeight * ledHeight; // Rojo en punta
```

### 🎨 Cómo Cambiar Colores

#### Ejemplo 1: Azul → Cyan → Blanco (estilo "frío")
```java
// En el método draw(), reemplazar:
float r = ledHeight * ledHeight * ledHeight;  // Rojo
float g = ledHeight;                          // Verde
float b = 0f;                                 // Azul

// Por:
float r = 0f;                                 // Sin rojo
float g = ledHeight;                          // Cyan
float b = 1.0f - (ledHeight * 0.5f);         // Azul fuerte en base
```

#### Ejemplo 2: Morado → Rosa → Blanco (estilo "neón")
```java
float r = 0.8f + ledHeight * 0.2f;           // Rosa constante
float g = ledHeight * 0.5f;                  // Verde sutil
float b = 1.0f - ledHeight * 0.3f;          // Morado en base
```

#### Ejemplo 3: Arcoíris por Barra
```java
// Colorear cada barra diferente según su índice
float hue = (float)barIndex / NUM_BARRAS;  // 0.0 - 1.0
// Convertir HSV a RGB (necesitas agregar función de conversión)
```

---

## ⚡ SENSIBILIDAD Y REACTIVIDAD

### Suavizado de Animación (Líneas ~180-190)

```java
// Factor de suavizado (0.0 = instantáneo, 1.0 = muy suave)
private static final float SMOOTHING_FACTOR = 0.75f;

// Aplicación del suavizado
smoothedLevels[i] = smoothedLevels[i] * SMOOTHING_FACTOR +
                    barLevels[i] * (1.0f - SMOOTHING_FACTOR);
```

### 🎨 Ajustar Reactividad

| Factor | Efecto | Uso Recomendado |
|--------|--------|-----------------|
| 0.5 | Muy reactivo, sigue beat | Música electrónica, drum & bass |
| 0.75 | Balance (ACTUAL) | Pop, rock, general |
| 0.9 | Muy suave, fluido | Música clásica, ambiente |

#### Ejemplo: Ecualizador Ultra-Reactivo
```java
private static final float SMOOTHING_FACTOR = 0.4f;  // Muy bajo = muy reactivo
```

#### Ejemplo: Ecualizador Suave
```java
private static final float SMOOTHING_FACTOR = 0.95f;  // Muy alto = muy suave
```

### Amplificación de Niveles

En `updateMusicLevels()` (líneas 145-161):

```java
barLevels[0] = bass * 1.2f;   // 1.2 = amplificado 20%
barLevels[5] = treble * 1.2f; // 1.2 = amplificado 20%
```

#### Más Sensible (reacciona a sonidos bajos)
```java
barLevels[0] = bass * 1.5f;   // Amplificado 50%
barLevels[5] = treble * 1.5f;
```

#### Menos Sensible (solo reacciona a sonidos fuertes)
```java
barLevels[0] = bass * 0.8f;   // Reducido 20%
barLevels[5] = treble * 0.8f;
```

---

## ✨ EFECTOS VISUALES

### Efecto de Brillo LED (Shader)

**Archivo**: `MusicIndicator.java`
**Método**: `initShader()` (líneas 82-98)

```glsl
// Fragment shader actual
if (color.a > 0.5) {
    color.rgb *= 1.3;  // Brillo extra para LEDs encendidos
}
```

### 🎨 Personalizar Brillo

#### Brillo Intenso (estilo "neón")
```glsl
if (color.a > 0.5) {
    color.rgb *= 2.0;  // Doble brillo
}
```

#### Brillo Sutil (estilo "minimalista")
```glsl
if (color.a > 0.5) {
    color.rgb *= 1.1;  // Solo 10% más brillo
}
```

#### Efecto Glow/Resplandor
```glsl
if (color.a > 0.5) {
    color.rgb *= 1.5;
    color.rgb += vec3(0.1, 0.1, 0.1);  // Añadir blanco para glow
}
```

### Espaciado Entre Barras

Buscar en `draw()` el código que genera las posiciones:

```java
float barSpacing = 0.02f;  // Espacio entre barras (ajustable)
```

#### Barras Pegadas (sin espacio)
```java
float barSpacing = 0.0f;
```

#### Barras Separadas
```java
float barSpacing = 0.05f;  // Más espacio
```

---

## 🐛 TROUBLESHOOTING

### Problema: No se ven las barras

**Solución:**
1. Verifica que hay música sonando
2. Revisa LogCat: `adb logcat -s depurar:D`
3. Verifica permisos de audio en la app

### Problema: Barras no reaccionan a la música

**Solución:**
```bash
# Ver logs del ecualizador
adb logcat -s depurar:D | grep MusicIndicator
```

Verifica que aparezcan logs con valores de Bass/Mid/Treble > 0.

### Problema: Ecualizador fuera de pantalla

**Solución:**
Verifica que X e Y estén en rango -1.0 a 1.0:
```java
musicIndicator = new MusicIndicator(
    context,
    0.0f,    // X: SIEMPRE entre -1.0 y 1.0
    0.0f,    // Y: SIEMPRE entre -1.0 y 1.0
    0.30f,
    0.08f
);
```

### Problema: FPS bajo con ecualizador

**Solución:**
Reduce el número de LEDs:
```java
private static final int LEDS_POR_BARRA = 8;  // En lugar de 14
```

---

## 📝 CHECKLIST DE PERSONALIZACIÓN

- [ ] Cambié `NUM_BARRAS` según mis preferencias
- [ ] Ajusté `LEDS_POR_BARRA` para rendimiento/calidad
- [ ] Modifiqué la distribución de frecuencias en `updateMusicLevels()`
- [ ] Ajusté posición (X, Y) en `SceneRenderer.java`
- [ ] Cambié tamaño (ancho, alto) en `SceneRenderer.java`
- [ ] Personalicé colores en `draw()`
- [ ] Ajusté `SMOOTHING_FACTOR` para reactividad
- [ ] Modifiqué amplificación de niveles (1.2f, etc.)
- [ ] Compilé y probé: `./gradlew.bat assembleDebug`
- [ ] Instalé APK en dispositivo
- [ ] Probé con música real

---

## 🚀 EJEMPLOS DE CONFIGURACIONES COMPLETAS

### Configuración 1: "Estilo Winamp" (clásico)
```java
// MusicIndicator.java
private static final int NUM_BARRAS = 10;
private static final int LEDS_POR_BARRA = 12;
private static final float SMOOTHING_FACTOR = 0.6f;

// SceneRenderer.java
musicIndicator = new MusicIndicator(context, 0.0f, 0.80f, 0.70f, 0.12f);

// Colores: Verde → Rojo (actual, sin cambios)
```

### Configuración 2: "Minimalista" (4 barras)
```java
// MusicIndicator.java
private static final int NUM_BARRAS = 4;
private static final int LEDS_POR_BARRA = 10;
private static final float SMOOTHING_FACTOR = 0.85f;

// SceneRenderer.java
musicIndicator = new MusicIndicator(context, 0.0f, -0.75f, 0.40f, 0.08f);
```

### Configuración 3: "Club/DJ" (muy reactivo)
```java
// MusicIndicator.java
private static final int NUM_BARRAS = 8;
private static final int LEDS_POR_BARRA = 16;
private static final float SMOOTHING_FACTOR = 0.4f;

// Amplificación extra en graves
barLevels[0] = bass * 1.8f;
barLevels[1] = bass * 1.5f;

// SceneRenderer.java
musicIndicator = new MusicIndicator(context, 0.0f, 0.0f, 1.50f, 0.25f);
```

---

**Autor**: Claude
**Última Actualización**: Noviembre 2025
**Proyecto**: Black Hole Glow v4.0.0
