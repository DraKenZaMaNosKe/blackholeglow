# 📸 Scripts de Captura en Python

Sistema profesional de captura automatizada para **Black Hole Glow** usando Python con interfaz hermosa.

---

## 🚀 Instalación Rápida

### 1. Instalar dependencias

```bash
pip install -r requirements.txt
```

Esto instalará:
- `rich` - Interfaz hermosa con colores y paneles
- `tqdm` - Barras de progreso animadas
- `colorama` - Soporte de colores en terminal

### 2. Ejecutar el menú principal

```bash
python menu_capturas.py
```

**¡Eso es todo!** 🎉

---

## 📁 Archivos del Sistema

```
blackholeglow/
├── capture_utils.py          # Módulo con funciones comunes
├── captura_rapida.py          # Script de captura rápida
├── capturar_app.py            # Script completo con instalación
├── grabar_video.py            # Script de grabación de video
├── menu_capturas.py           # Menú interactivo principal ⭐
├── requirements.txt           # Dependencias
└── README_CAPTURAS_PYTHON.md  # Este archivo
```

---

## 🎯 Uso

### Opción 1: Menú Interactivo (Recomendado)

```bash
python menu_capturas.py
```

Verás un menú hermoso con 7 opciones:

```
╔══════════════════════════════════════════════════╗
║   📱 BLACK HOLE GLOW - Menú de Capturas        ║
╚══════════════════════════════════════════════════╝

1  📸 Captura Rápida           Solo 1 screenshot (3 seg)
2  🔄 Captura + Instalación    Instala APK + capturas (15-30 seg)
3  🎥 Grabar Video             Graba animaciones (10-35 seg)
4  📂 Abrir carpeta            Abre D:\img\
5  📖 Documentación            Ver guía de uso
6  ℹ️  Info del sistema        Verificar ADB y dispositivo
7  ❌ Salir                    Cerrar el menú
```

### Opción 2: Scripts Individuales

#### Captura Rápida (3 segundos)

```bash
python captura_rapida.py
```

- Toma 1 screenshot inmediato
- Lo guarda en `D:\img\blackhole_TIMESTAMP.png`
- Abre la imagen automáticamente
- Muestra el comando para Claude

#### Captura Completa (15-30 segundos)

```bash
python capturar_app.py
```

- Instala APK actualizado (opcional)
- Lanza la app automáticamente
- Toma múltiples capturas (1-10)
- Intervalo de 2 segundos entre capturas
- Abre la carpeta con resultados

#### Grabar Video (10-35 segundos)

```bash
python grabar_video.py
```

- Graba de 5 a 30 segundos
- Ideal para mostrar animaciones
- Guarda en `D:\img\blackhole_TIMESTAMP.mp4`
- Abre el video automáticamente

---

## 🎨 Características de la Interfaz

### ✨ Progress Bars Animadas

```
[●●●●●●●●●●] Capturando pantalla... 100%
```

### 🎨 Colores y Símbolos

- ✓ Verde = Éxito
- ❌ Rojo = Error
- ⚠️  Amarillo = Advertencia
- ℹ️  Cyan = Información

### 📊 Paneles Informativos

```
╔════════════════════════════════════════╗
║  📸 Captura Rápida                   ║
║  Screenshot instantáneo del celular  ║
╚════════════════════════════════════════╝
```

---

## 🔧 Requisitos del Sistema

### Software

- **Python 3.7+** (probado con 3.10+)
- **ADB (Android Debug Bridge)** instalado
- **Celular Android** con USB Debugging habilitado

### Librerías Python

Ver `requirements.txt`:
- `colorama>=0.4.6`
- `rich>=14.0.0`
- `tqdm>=4.67.0`

---

## 📂 Carpeta de Destino

Todos los archivos se guardan en: **`D:\img\`**

### Formato de nombres:

**Screenshots:**
```
blackhole_20251019_143522.png       # Captura única
blackhole_20251019_143522_1.png     # Múltiples capturas
blackhole_20251019_143522_2.png
blackhole_20251019_143522_3.png
```

**Videos:**
```
blackhole_20251019_144522.mp4
```

---

## 💡 Ventajas sobre .BAT

| Característica | .BAT | Python |
|---------------|------|--------|
| **Interfaz** | ❌ Texto plano | ✅ Colores, paneles, tablas |
| **Progress bars** | ❌ No | ✅ Animadas con tqdm |
| **Manejo de errores** | ❌ Básico | ✅ Robusto con try/except |
| **Validación de input** | ❌ Limitada | ✅ Completa |
| **Multiplataforma** | ❌ Solo Windows | ✅ Win/Mac/Linux |
| **Mantenibilidad** | ❌ Difícil | ✅ Fácil (OOP) |
| **Extensibilidad** | ❌ Limitada | ✅ Ilimitada |

---

## 🔍 Arquitectura del Código

### Módulo `capture_utils.py`

Clase principal: `ADBHelper`

**Métodos clave:**
```python
check_device()           # Verifica dispositivo conectado
screenshot(path)         # Toma screenshot
record_video(dur, path)  # Graba video
install_apk(path)        # Instala APK
launch_app(pkg, act)     # Lanza aplicación
get_timestamp()          # Genera timestamp
```

**Funciones auxiliares:**
```python
print_header(title)      # Header bonito
print_success(msg)       # Mensaje de éxito
print_error(msg)         # Mensaje de error
get_int_input(...)       # Input validado
```

### Diseño Modular

```
menu_capturas.py
    ↓
    Llama a →  captura_rapida.py
    Llama a →  capturar_app.py
    Llama a →  grabar_video.py
                ↓
                Todos usan → capture_utils.py
```

---

## 🐛 Solución de Problemas

### Error: "ADB no encontrado"

**Solución:**
1. Verifica que ADB esté instalado:
   ```bash
   where adb
   ```
2. Si no está en PATH, edita `capture_utils.py` línea 35:
   ```python
   possible_paths = [
       r"C:\TU\RUTA\A\adb.exe",  # Agrega tu ruta
       ...
   ]
   ```

### Error: "No hay dispositivo conectado"

**Solución:**
1. Conecta el celular vía USB
2. Habilita "USB Debugging" en Opciones de Desarrollador
3. Acepta el prompt "Allow USB debugging" en el celular
4. Verifica con: `adb devices`

### Error: "ModuleNotFoundError: No module named 'rich'"

**Solución:**
```bash
pip install -r requirements.txt
```

### La captura sale negra

**Posibles causas:**
- Algunas apps tienen protección contra screenshots
- El dispositivo está bloqueado
- Permisos de seguridad del sistema

**Solución:**
- Desbloquea el celular
- Verifica que la app esté visible en pantalla

---

## 📊 Comparación: Workflow ANTES vs AHORA

### ❌ **ANTES (proceso manual):**

```
1. Ejecutar app en celular               (manual)
2. Abrir app de grabación de pantalla    (manual)
3. Grabar pantalla                        (30-60 seg)
4. Detener grabación                      (manual)
5. Enviar video por WhatsApp a ti mismo  (manual)
6. Abrir WhatsApp en PC                   (manual)
7. Descargar video                        (manual)
8. Ejecutar script Python extraer_frames (manual)
9. Cortar imágenes manualmente            (manual)
10. Mover a D:\img\                       (manual)
11. Decirle a Claude dónde están          (manual)

⏱️ Tiempo total: ~5-8 minutos
😓 Esfuerzo: ALTO
```

### ✅ **AHORA (Python automatizado):**

```
1. python captura_rapida.py
2. Decir a Claude: "D:\img\blackhole_20251019_150522.png"

⏱️ Tiempo total: ~5 segundos
😎 Esfuerzo: MÍNIMO
```

**Ahorro: 95% de tiempo y 100% menos frustración** 🚀

---

## 🎬 Ejemplos de Uso Real

### Ejemplo 1: "Modifiqué el shader del sol"

```bash
# 1. Compilar APK
./gradlew.bat assembleDebug

# 2. Capturar con instalación
python capturar_app.py
# → Eliges: instalar APK = Sí
# → Eliges: 3 capturas

# 3. Claude recibe:
"Claude, las capturas están en D:\img\blackhole_20251019_150522_*.png"

# 4. Claude analiza las 3 imágenes y da feedback
```

### Ejemplo 2: "Solo quiero mostrar algo rápido"

```bash
# 1. Abrir app en el celular (manual)

# 2. Captura rápida
python captura_rapida.py

# 3. Claude recibe:
"D:\img\blackhole_20251019_151022.png"

# 4. Claude responde inmediatamente
```

### Ejemplo 3: "Quiero mostrar la animación del agujero negro"

```bash
# 1. Preparar escena en el celular (manual)

# 2. Grabar video
python grabar_video.py
# → Eliges: 15 segundos

# 3. Claude recibe:
"D:\img\blackhole_20251019_152022.mp4"

# 4. Claude ve el video completo y analiza la animación
```

---

## 📝 Notas de Desarrollo

### Extensibilidad

El código está diseñado para ser fácilmente extensible:

**Ejemplo: Agregar captura de logs**

```python
# En capture_utils.py, agregar método:
def capture_logcat(self, output_path: Path, filter_tag: str = None):
    """Captura logs de LogCat"""
    args = ["logcat", "-d"]
    if filter_tag:
        args.extend(["-s", filter_tag])

    success, stdout, stderr = self.run_command(args)

    if success:
        output_path.write_text(stdout, encoding='utf-8')
        return True
    return False
```

### Testing

Para probar sin dispositivo conectado, puedes simular:

```python
# En capture_utils.py, modificar temporalmente:
def check_device(self) -> bool:
    return True  # Simular dispositivo conectado
```

---

## 🆚 Comparación con Scripts .BAT

Los scripts `.bat` siguen disponibles en:
- `captura_rapida.bat`
- `capturar_app.bat`
- `grabar_video.bat`
- `menu_capturas.bat`

**¿Cuándo usar .BAT?**
- Si no tienes Python instalado
- Si prefieres scripts más simples
- Si solo necesitas funcionalidad básica

**¿Cuándo usar Python?**
- Interfaz hermosa con colores ✅
- Progress bars animadas ✅
- Mejor manejo de errores ✅
- Más robusto y mantenible ✅

**Recomendación:** Usa Python para desarrollo activo, .BAT como fallback.

---

## 📚 Referencias

- **ADB Documentation:** https://developer.android.com/tools/adb
- **Rich Library:** https://rich.readthedocs.io/
- **tqdm Library:** https://tqdm.github.io/

---

## 🙏 Créditos

- **Autor:** Claude Code
- **Fecha:** 2025-10-19
- **Versión:** 1.0
- **Proyecto:** Black Hole Glow

---

**¿Preguntas o problemas?** Revisa este README o ejecuta:

```bash
python menu_capturas.py
# → Opción 6: Info del sistema
```

¡Disfruta de las capturas automatizadas! 📸🚀
