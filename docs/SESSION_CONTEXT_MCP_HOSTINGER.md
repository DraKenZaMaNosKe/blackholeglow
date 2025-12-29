# Contexto de Sesión - MCP Hostinger + Videos Remotos

**Fecha:** 29 Diciembre 2024
**Objetivo:** Reducir tamaño de APK moviendo videos a servidor web

---

## ✅ LO QUE YA LOGRAMOS

### 1. Token API de Hostinger Creado
```
Token Name: ClaudeCode-Blackholeglow
Expiration: Never expires
Token: R0CTj2AyZ7NCFLz88dRG6NCsxuO20HVqn7RKjsRY6bef16e8
```

### 2. Conexión API Verificada
```bash
# Este comando funciona:
curl -X GET "https://developers.hostinger.com/api/hosting/v1/websites" \
  -H "Authorization: Bearer R0CTj2AyZ7NCFLz88dRG6NCsxuO20HVqn7RKjsRY6bef16e8" \
  -H "Content-Type: application/json"
```

**Respuesta exitosa:**
```json
{
  "domain": "intrapcsolutions.com",
  "username": "u781896435",
  "root_directory": "/home/u781896435/domains/intrapcsolutions.com/public_html"
}
```

### 3. Archivo MCP Configurado
**Ubicación:** `C:\Users\lalo\.claude\mcp_settings.json`

**Contenido:**
```json
{
  "inputs": [
    {
      "id": "api_token",
      "type": "promptString",
      "description": "Enter your Hostinger API token (required)"
    }
  ],
  "servers": {
    "hostinger-mcp": {
      "type": "stdio",
      "command": "npx",
      "args": [
        "hostinger-api-mcp@latest"
      ],
      "env": {
        "API_TOKEN": "R0CTj2AyZ7NCFLz88dRG6NCsxuO20HVqn7RKjsRY6bef16e8"
      }
    }
  }
}
```

---

## 🎯 OBJETIVO: Arquitectura de Videos Remotos

### Problema Actual
- APK muy grande (~40MB+) porque incluye videos en assets
- Los usuarios descargan videos que quizás nunca usen

### Solución Propuesta
```
┌─────────────────────┐          ┌─────────────────────────────┐
│     APP ANDROID     │          │   intrapcsolutions.com      │
│      (~5 MB)        │          │                             │
│                     │          │  /public/                   │
│  1. Consulta ───────┼─────────→│    └─ api/                 │
│     catálogo        │          │        └─ wallpapers.json  │
│                     │          │                             │
│  2. Descarga ───────┼─────────→│    └─ videos/              │
│     video           │          │        ├─ cielo.mp4        │
│                     │          │        ├─ oceano.mp4       │
│                     │          │        └─ space.mp4        │
│  3. Guarda en       │          │                             │
│     cache local     │          │                             │
│                     │          │                             │
│  4. Reproduce       │          │                             │
│     desde cache     │          │                             │
│                     │          │                             │
│  5. Al cambiar →    │          │                             │
│     elimina video   │          │                             │
└─────────────────────┘          └─────────────────────────────┘
```

---

## 📁 Estructura a Crear en Servidor

```
intrapcsolutions.com/
└── public/
    ├── api/
    │   └── wallpapers.json    ← Catálogo de wallpapers
    └── videos/
        ├── cielo_volando.mp4
        ├── oceano_profundo.mp4
        └── portal_cosmico.mp4
```

### Formato del wallpapers.json
```json
{
  "version": 1,
  "wallpapers": [
    {
      "id": "lab_scene",
      "name": "Portal Cósmico",
      "description": "Nubes de fuego con nave espacial",
      "videoUrl": "https://intrapcsolutions.com/videos/cielo_volando.mp4",
      "previewUrl": "https://intrapcsolutions.com/previews/cielo_preview.jpg",
      "sizeMB": 8.5,
      "scene": "LabScene"
    }
  ]
}
```

---

## 📱 Componentes Android a Crear

| Archivo | Función |
|---------|---------|
| `VideoDownloadManager.java` | Descarga + cache + eliminación |
| `WallpaperCatalog.java` | Modelo de datos del JSON |
| `WallpaperRepository.java` | Consulta API del servidor |
| `DownloadProgressDialog.java` | UI de progreso |
| Modificar `MediaCodecVideoRenderer` | Leer de cache local |

---

## 🔄 PRÓXIMOS PASOS (al reiniciar)

### Paso 1: Verificar MCP
Después de reiniciar Claude Code, ejecutar:
```
/mcp
```
Debería mostrar `hostinger-mcp` como servidor disponible.

### Paso 2: Probar Herramientas MCP
Buscar herramientas disponibles:
```
MCPSearch con query "hostinger"
```

### Paso 3: Crear Estructura en Servidor
Si MCP funciona:
- Crear carpeta `/public/videos/`
- Crear carpeta `/public/api/`
- Crear `wallpapers.json`

### Paso 4: Subir Video de Prueba
- Subir `cielovolando.mp4` al servidor
- Verificar acceso: `https://intrapcsolutions.com/videos/cielovolando.mp4`

### Paso 5: Implementar en Android
- Crear `VideoDownloadManager.java`
- Modificar escenas para usar videos remotos

---

## 📊 Información del Hosting

| Campo | Valor |
|-------|-------|
| Dominio | intrapcsolutions.com |
| Usuario | u781896435 |
| Root | /home/u781896435/domains/intrapcsolutions.com/public_html |
| Almacenamiento | 50 GB SSD |
| Plataforma | Hostinger Horizons (React/JSX) |

---

## ⚠️ NOTAS IMPORTANTES

1. **Token expuesto** - Después de configurar todo, regenerar el token por seguridad
2. **VPS NO necesario** - El hosting normal es suficiente para servir videos estáticos
3. **Videos actuales en assets:**
   - `cielovolando.mp4` (LabScene)
   - Otros videos de escenas

---

## 🛸 BONUS: OVNI para Segunda Nave

El usuario generó una imagen conceptual de OVNI con Gemini:
- Estilo: Platillo metálico con anillos cyan brillantes
- Reflejos naranjas del ambiente de fuego
- Perfecto para complementar la nave humana en LabScene

**Prompt usado:**
```
3D alien spacecraft UFO for mobile game, sleek metallic saucer design
with glowing cyan/teal energy ring around the edge, smooth chrome surface
with subtle orange reflections from fire clouds environment...
```

El usuario está editando el modelo 3D en Meshy.

---

## 🔧 Comandos Útiles

### Probar API de Hostinger
```bash
curl -X GET "https://developers.hostinger.com/api/hosting/v1/websites" \
  -H "Authorization: Bearer R0CTj2AyZ7NCFLz88dRG6NCsxuO20HVqn7RKjsRY6bef16e8" \
  -H "Content-Type: application/json"
```

### Build Android
```bash
cd D:/Orbix/blackholeglow && ./gradlew assembleDebug
```

### Instalar APK
```bash
timeout 60 D:/adb/platform-tools/adb.exe install -r D:/Orbix/blackholeglow/app/build/outputs/apk/debug/app-debug.apk
```

---

*Contexto guardado para continuar después del reinicio*
