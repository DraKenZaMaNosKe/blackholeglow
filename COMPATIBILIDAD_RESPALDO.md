# 🔄 Configuración de Compatibilidad de Respaldo

## Si necesitas versiones más antiguas (para Android Studio desactualizado)

### Opción 1: Versiones conservadoras (Compatible con AS 2023.x)

Edita `gradle/libs.versions.toml`:

```toml
[versions]
agp = "8.7.3"  # En lugar de 8.12.3
# ... resto igual
```

Edita `gradle/wrapper/gradle-wrapper.properties`:

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
```

---

### Opción 2: Versiones LTS (Máxima compatibilidad)

Para Android Studio 2022.x o anterior:

**gradle/libs.versions.toml:**
```toml
[versions]
agp = "8.1.4"
compileSdk = 34
targetSdk = 34
# ... resto igual
```

**gradle/wrapper/gradle-wrapper.properties:**
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.2-bin.zip
```

---

## ⚠️ IMPORTANTE:

**NO hagas estos cambios a menos que sea absolutamente necesario.**

La mejor solución es actualizar Android Studio en ambas computadoras para usar las versiones más recientes.

---

## Matriz de Compatibilidad:

| Android Studio Version | AGP Version | Gradle Version | Fecha |
|------------------------|-------------|----------------|--------|
| Ladybug 2024.2.1+      | 8.7 - 8.12  | 8.9 - 8.13     | Actual |
| Jellyfish 2023.3.1     | 8.3 - 8.6   | 8.4 - 8.9      | 2024   |
| Iguana 2023.2.1        | 8.2 - 8.4   | 8.2 - 8.6      | 2023   |
| Hedgehog 2023.1.1      | 8.1 - 8.3   | 8.0 - 8.4      | 2023   |

**Tu configuración actual:**
- AGP: **8.12.3** → Requiere Android Studio **Ladybug 2024.2.1+**
- Gradle: **8.13**

---

## Comandos para cambiar versiones:

### Downgrade a AGP 8.7.3 (si es necesario):

```bash
# 1. Editar gradle/libs.versions.toml
# Cambiar línea 2: agp = "8.7.3"

# 2. Editar gradle/wrapper/gradle-wrapper.properties
# Cambiar línea 4: distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip

# 3. Limpiar y reconstruir
gradlew.bat clean
gradlew.bat assembleDebug
```

---

**Creado:** 2025-01-13
**Propósito:** Respaldo de emergencia si Android Studio no puede actualizarse
