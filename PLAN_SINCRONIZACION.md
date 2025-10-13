# 🔧 Plan de Sincronización entre Casa y Oficina

## ❌ Problema detectado:
- **Casa:** Android Studio actualizado, AGP 8.12.3, Gradle 8.13
- **Oficina:** Android Studio desactualizado, no puede procesar AGP 8.12.3

---

## ✅ Solución paso a paso para MAÑANA en la oficina:

### **Paso 1: Verificar versión de Android Studio**
1. Abre Android Studio
2. Ve a: **Help → About**
3. Anota la versión (ej: "Ladybug 2024.2.1" o "Hedgehog 2023.1.1")

**Si es anterior a Ladybug (2024.2.x):** Necesitas actualizar

---

### **Paso 2: Actualizar Android Studio (RECOMENDADO)**
1. **Help → Check for Updates**
2. Si hay actualización disponible, instala
3. Reinicia Android Studio
4. Abre el proyecto
5. Click en "Sync Project with Gradle Files" (icono de elefante)

✅ Esto debería resolver todo automáticamente

---

### **Paso 3: Si no puedes actualizar, usa terminal**

Abre Git Bash o PowerShell:

```bash
cd C:\Users\TU_USUARIO\AndroidStudioProjects\blackholeglow

# Limpiar y compilar
gradlew.bat clean assembleDebug

# Si falla por Java, configura JAVA_HOME primero:
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%PATH%
gradlew.bat clean assembleDebug
```

✅ Esto compila sin depender del Android Studio

---

## 🛡️ Prevención futura:

### **1. Mantén ambas PCs actualizadas**
- Android Studio se actualiza cada ~2 meses
- Configura actualizaciones automáticas:
  - **File → Settings → Appearance & Behavior → System Settings → Updates**
  - Activa: "Check for updates automatically"

### **2. Usa Git correctamente**
- ✅ **SÍ subir:** `build.gradle.kts`, `libs.versions.toml`, `gradle-wrapper.properties`
- ❌ **NO subir:** `local.properties`, `.gradle/`, `build/`

Estos archivos ya están en tu `.gitignore`, así que está bien.

### **3. Verifica compatibilidad antes de push**

Crea este script en el proyecto: `verificar-compatibilidad.bat`

```batch
@echo off
echo ========================================
echo Verificación de Compatibilidad Gradle
echo ========================================
echo.

echo ✓ Java version:
java -version
echo.

echo ✓ Gradle version:
gradlew.bat --version
echo.

echo ✓ Intentando compilar...
gradlew.bat assembleDebug --dry-run

if %errorlevel% == 0 (
    echo.
    echo ✅ COMPATIBILIDAD OK - Seguro hacer push
) else (
    echo.
    echo ❌ ERROR - NO hacer push, revisar errores
)

pause
```

Ejecuta esto antes de hacer `git push` para asegurar que todo funciona.

---

## 📊 Configuración actual (Casa):

- **Android Gradle Plugin (AGP):** 8.12.3
- **Gradle:** 8.13
- **Java:** 11
- **compileSdk:** 35 (Android 15)
- **minSdk:** 24 (Android 7.0)
- **targetSdk:** 35

**Requisito mínimo para compilar:**
- Android Studio: **Ladybug 2024.2.1+**
- Gradle: **8.9+** (se descarga automáticamente)
- Java: **11** (incluido en Android Studio)

---

## 🆘 Si nada funciona mañana:

**Contacta a Claude con este mensaje:**

```
Claude, estoy en la oficina y el proyecto sigue sin compilar.
El error es: [PEGA EL ERROR COMPLETO AQUÍ]
Mi versión de Android Studio es: [VERSION]

¿Puedes crear una rama de compatibilidad con AGP 8.7.3?
```

Te crearé una rama temporal con versiones más antiguas que funcionen en tu oficina.

---

## 📝 Checklist para mañana:

- [ ] Abrir Android Studio en la oficina
- [ ] Verificar versión (Help → About)
- [ ] Si es antigua: Actualizar (Help → Check for Updates)
- [ ] Hacer `git pull origin version-3.0.0`
- [ ] Click en "Sync Project with Gradle Files"
- [ ] Si falla: Intentar `gradlew.bat assembleDebug` desde terminal
- [ ] Si sigue fallando: Contactar a Claude con el error completo

---

**Fecha de creación:** 2025-01-13
**Versión del proyecto:** 3.0.0
**PC donde funciona:** Casa
**PC donde falla:** Oficina
