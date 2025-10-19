# 📸 Scripts de Captura Automatizada

## Guía Rápida de Uso

### 🎯 ¿Cuál script usar?

| Script | Cuándo usarlo | Tiempo estimado |
|--------|---------------|-----------------|
| `captura_rapida.bat` | Solo quiero 1 screenshot rápido | 3 segundos |
| `capturar_app.bat` | Quiero instalar APK nuevo + capturas múltiples | 15-30 segundos |
| `grabar_video.bat` | Quiero mostrar animación o movimiento | 10-35 segundos |

---

## 📋 Descripción de cada script

### 1️⃣ `captura_rapida.bat` - **MÁS USADO**

**Para:** Capturar lo que estás viendo AHORA en el celular

**Proceso:**
1. Doble clic en el archivo
2. Captura automática
3. Se abre la imagen
4. Listo para mostrar a Claude

**Resultado:**
- `D:\img\blackhole_YYYYMMDD_HHMMSS.png`

**Ejemplo de uso:**
```
Usuario: "Ejecuta captura_rapida.bat"
[Se abre la imagen]
Usuario a Claude: "La captura está en D:\img\blackhole_20251019_143022.png"
```

---

### 2️⃣ `capturar_app.bat` - **COMPLETO**

**Para:** Cuando modificaste código y quieres ver el resultado en el celular

**Proceso:**
1. Compila APK nuevo (si existe)
2. Lo instala en el celular
3. Abre la app automáticamente
4. Te pregunta: "¿Cuántas capturas? (1-10)"
5. Toma capturas cada 2 segundos
6. Abre la carpeta D:\img\

**Resultado:**
- `D:\img\blackhole_YYYYMMDD_HHMMSS_1.png`
- `D:\img\blackhole_YYYYMMDD_HHMMSS_2.png`
- `D:\img\blackhole_YYYYMMDD_HHMMSS_3.png`
- etc.

**Ejemplo de uso:**
```
Usuario: "Ejecuta capturar_app.bat"
Script: "¿Cuántas capturas? (1-10)"
Usuario: "3"
[Toma 3 capturas con 2 segundos entre cada una]
Usuario a Claude: "Las capturas están en D:\img\blackhole_20251019_143522_*.png"
```

---

### 3️⃣ `grabar_video.bat` - **PARA ANIMACIONES**

**Para:** Cuando quieres mostrar movimiento, transiciones, efectos animados

**Proceso:**
1. Te pregunta: "¿Cuántos segundos? (5-30)"
2. Graba la pantalla del celular
3. Descarga el video MP4
4. Lo abre automáticamente

**Resultado:**
- `D:\img\blackhole_YYYYMMDD_HHMMSS.mp4`

**Ejemplo de uso:**
```
Usuario: "Ejecuta grabar_video.bat"
Script: "¿Cuántos segundos? (default=10)"
Usuario: "15"
[Graba 15 segundos]
Usuario a Claude: "El video está en D:\img\blackhole_20251019_144022.mp4"
```

**💡 VENTAJA:** Claude puede ver videos directamente, ¡no necesitas extraer frames con Python!

---

## 🔧 Requisitos

✅ Celular conectado via USB con **USB Debugging habilitado**
✅ ADB configurado (ya está en tu PC)
✅ Carpeta `D:\img\` (se crea automáticamente)

---

## 🚀 Workflow Recomendado

### Caso 1: "Modifiqué código, quiero ver cambios"

```bash
# 1. Compilar APK
./gradlew.bat assembleDebug

# 2. Instalar y capturar
capturar_app.bat
# Responder: 2 capturas

# 3. Mostrar a Claude
"Claude, las capturas están en D:\img\blackhole_20251019_*.png"
```

### Caso 2: "Solo quiero mostrar algo rápido"

```bash
# 1. Asegúrate de que la app esté abierta en el celular
# 2. Ejecutar
captura_rapida.bat

# 3. Mostrar a Claude
"Claude, la captura está en D:\img\blackhole_20251019_143022.png"
```

### Caso 3: "Quiero mostrar una animación"

```bash
# 1. Preparar la escena en el celular
# 2. Ejecutar
grabar_video.bat
# Responder: 10 segundos

# 3. Mostrar a Claude
"Claude, el video está en D:\img\blackhole_20251019_144522.mp4"
```

---

## 📝 Notas Importantes

### Formato de archivos

**Screenshots:**
- Formato: `blackhole_YYYYMMDD_HHMMSS[_N].png`
- Ejemplo: `blackhole_20251019_143522_1.png`

**Videos:**
- Formato: `blackhole_YYYYMMDD_HHMMSS.mp4`
- Ejemplo: `blackhole_20251019_144522.mp4`

### ¿Por qué ya NO necesitas Python para extraer frames?

Antes:
1. ❌ Grabar video con app
2. ❌ Enviarlo por WhatsApp
3. ❌ Descargarlo
4. ❌ Correr script Python para extraer frames
5. ❌ Cortar imágenes
6. ❌ Mover a D:\img\

Ahora:
1. ✅ Ejecutar `capturar_app.bat` o `captura_rapida.bat`
2. ✅ Listo

**Ahorro de tiempo: ~5 minutos → 5 segundos** 🚀

---

## 🛠️ Personalización

Si quieres cambiar algo:

**Cambiar carpeta de destino:**
Edita la línea `set "IMG_DIR=D:\img"` en cada script

**Cambiar intervalo entre capturas:**
En `capturar_app.bat`, línea: `timeout /t 2 /nobreak >nul`
Cambia `2` por el número de segundos que quieras

**Cambiar duración máxima de video:**
En `grabar_video.bat`, línea: `if %DURACION% GTR 30 set DURACION=30`
Cambia `30` por el máximo que quieras

---

## 🐛 Solución de Problemas

**Error: "No hay dispositivo conectado"**
- Conecta el celular via USB
- Habilita "USB Debugging" en Opciones de Desarrollador
- Acepta el prompt de "Allow USB debugging" en el celular

**El APK no se instala:**
- Verifica que compiló correctamente: `./gradlew.bat assembleDebug`
- El APK debe estar en: `app\build\outputs\apk\debug\app-debug.apk`

**La captura sale negra:**
- Algunas apps bloquean capturas por seguridad
- Intenta con otro método o verifica permisos de la app

---

## 💬 Cómo hablarle a Claude después de capturar

### Forma correcta (específica):
```
"Claude, las capturas están en D:\img\blackhole_20251019_143522_*.png"
```

### Forma correcta (corta):
```
"D:\img\blackhole_20251019_143522_1.png"
```

### Forma correcta (muy corta):
```
"La captura más reciente en D:\img\"
```

Claude puede leer archivos directamente de esa ruta.

---

**Fecha de creación:** 2025-10-19
**Versión:** 1.0
**Autor:** Claude Code
