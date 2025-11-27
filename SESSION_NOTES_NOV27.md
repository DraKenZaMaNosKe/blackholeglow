# Session Notes - November 27, 2024

## Resumen de la Sesión

Esta sesión se enfocó en implementar saludos inteligentes con IA y mejorar la estabilidad del wallpaper.

---

## 1. Google Gemini AI - Saludos Inteligentes

### Implementación Completada
- **GeminiService.java** (NUEVO): Servicio singleton para llamadas a la API de Gemini
  - API Key: `AIzaSyDOXpSaswyL2nOg1YKp_rpuN3PWRsARiD4`
  - Modelo: `gemini-2.0-flash` (el único que funcionó, `gemini-1.5-flash` y `gemini-pro` daban 404)
  - URL: `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent`

### Funcionalidad
- Genera saludos creativos y personalizados usando IA
- Se llama UNA SOLA VEZ por sesión (optimización de rendimiento)
- El usuario NO puede responder (sin chat interactivo - deshabilitado por petición del usuario)

### Archivos Relacionados (Chat deshabilitado pero presentes)
- `GeminiChatActivity.java` - Activity de chat (NO se usa actualmente)
- `activity_gemini_chat.xml` - Layout del chat (NO se usa)
- Recursos de chat: `chat_bubble_*.xml`, `chat_input_bg.xml`, `btn_send_bg.xml`

---

## 2. OrbixGreeting - Optimizaciones de Rendimiento

### Mejoras Implementadas
```java
// Vertex cache para evitar allocations
private final float[] vertexCache = new float[16];

// Throttling de actualización del reloj
private long lastClockUpdate = 0;
private static final long CLOCK_UPDATE_INTERVAL = 16; // ~60fps

// Gemini solo una vez por sesión
private boolean hasUsedGeminiThisSession = false;
```

### Optimizaciones en draw()
- Vertex attributes se habilitan UNA VEZ al inicio del frame
- Uniforms se setean UNA VEZ (no por cada quad)
- Se deshabilitan al final del frame

---

## 3. LiveWallpaperService - Auto-STOP Inteligente

### Problema Resuelto
El wallpaper se quedaba "trabado" en estado animado cuando:
- Se presionaba Home
- Se abría App History/Recents
- Se apagaba la pantalla
- Se abría otra app

### Solución Implementada
**BroadcastReceiver** (`ScreenStateReceiver`) que detecta eventos del sistema:

```java
private class ScreenStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        switch (action) {
            case Intent.ACTION_SCREEN_OFF:
                forceStopAnimation();
                break;
            case Intent.ACTION_CLOSE_SYSTEM_DIALOGS:
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason) || "recentapps".equals(reason)) {
                    forceStopAnimation();
                }
                break;
        }
    }
}
```

### Fix para Android 13+ (API 33)
```java
// Android 13+ requiere especificar RECEIVER_NOT_EXPORTED
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    context.registerReceiver(screenStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
} else {
    context.registerReceiver(screenStateReceiver, filter);
}
```

---

## 4. Archivos Modificados

### Archivos Nuevos
- `GeminiService.java` - Servicio de IA para saludos
- `OrbixGreeting.java` - Sistema de saludos con reloj
- `PlayPauseButton.java` - Botón play/pause visual
- `BackgroundWorker.java` - Worker para tareas en background
- `TimeManager.java` - Gestor de tiempo
- `GeminiChatActivity.java` - Chat (deshabilitado)
- Shaders GL3: `app/src/main/assets/shaders/gl3/`
- Clases GL3: `app/src/main/java/com/secret/blackholeglow/gl3/`

### Archivos Modificados Principales
- `LiveWallpaperService.java` - Auto-STOP + BroadcastReceiver
- `SceneRenderer.java` - Integración de OrbixGreeting
- `MainActivity.java` - Navegación actualizada
- `LoginActivity.java` - Mejoras de autenticación
- `UserManager.java` - Gestión de usuarios mejorada

---

## 5. Configuración de Google Cloud

### API Habilitada
- **Generative Language API** (Gemini)
- Proyecto existente de Firebase

### Credenciales
- API Key para Gemini: `AIzaSyDOXpSaswyL2nOg1YKp_rpuN3PWRsARiD4`

---

## 6. Problemas Encontrados y Soluciones

| Problema | Causa | Solución |
|----------|-------|----------|
| Gemini API 404 | Modelo `gemini-1.5-flash` no existe | Usar `gemini-2.0-flash` |
| App crasheaba | `SecurityException` en `registerReceiver` | Agregar `RECEIVER_NOT_EXPORTED` para Android 13+ |
| App lenta | Muchas allocations en OrbixGreeting | Cache de vertices, throttling |
| Wallpaper trabado | No detectaba eventos del sistema | BroadcastReceiver para SCREEN_OFF, CLOSE_SYSTEM_DIALOGS |

---

## 7. Próximas Mejoras Potenciales

- [ ] Sistema de armas láser para el OVNI (disparar a la Tierra)
- [ ] Impactos visuales de láser en el planeta
- [ ] IA de ataque del OVNI
- [ ] Más variedad de saludos con Gemini
- [ ] Integración con calendario para saludos contextuales

---

## 8. Comandos Útiles

```bash
# Compilar
./gradlew assembleDebug

# Instalar
D:/adb/platform-tools/adb.exe install -r app/build/outputs/apk/debug/app-debug.apk

# Ver logs de crash
D:/adb/platform-tools/adb.exe logcat -d -s AndroidRuntime:E

# Ver logs del app
D:/adb/platform-tools/adb.exe logcat -d -s LiveWallpaperService:D SceneRenderer:D
```

---

## 9. Estado Actual

- **Branch**: `beta1.0`
- **Versión**: 4.0.0 (desarrollo)
- **FPS**: 36-43 FPS estable
- **Funcionalidades activas**:
  - Saludos inteligentes con Gemini (una vez por sesión)
  - Auto-STOP en eventos del sistema
  - OVNI con IA de exploración
  - Ecualizador visual mejorado
  - Sistema de meteoritos (barra oculta)
