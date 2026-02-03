# WALLPAPER_DEVELOPMENT.md

Guia completa para crear nuevos wallpapers en Black Hole Glow.
Documento de referencia para continuar desarrollando escenas.

---

## Supabase Storage - Configuracion

### Proyecto
- **URL**: `https://vzuwvsmlyigjtsearxym.supabase.co`
- **Bucket**: `wallpaper-videos` (publico, para videos de wallpapers)
- **Bucket URL base**: `https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/public/wallpaper-videos/`

### API Keys
- **Anon/Public**: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ6dXd2c21seWlnanRzZWFyeHltIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTg2NDg3MDksImV4cCI6MjA3NDIyNDcwOX0.xxx` (ver dashboard)
- **Service Role (para uploads)**: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ6dXd2c21seWlnanRzZWFyeHltIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc1ODY0ODcwOSwiZXhwIjoyMDc0MjI0NzA5fQ.xDs_HCkdqcEVJktzTdjGIXnG-V--j86jbkrUA4SjAOs`

### Subir video con curl
```bash
curl -X POST "https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/wallpaper-videos/NOMBRE_VIDEO.mp4" \
  -H "Authorization: Bearer <SERVICE_ROLE_KEY>" \
  -H "Content-Type: video/mp4" \
  -H "x-upsert: true" \
  -T "ruta/al/video.mp4" \
  --max-time 120
```

### Otros buckets en Supabase
- `wallpaper-images` - Texturas e imagenes (PNG, WebP)
- `wallpaper-models` - Modelos 3D (OBJ)

---

## Como Crear un Nuevo Wallpaper (Video Scene)

### Requisitos previos
1. **Video MP4** - Formato portrait (9:16), loop seamless, max 540x800px para optimizar RAM
2. **Preview WebP** - Imagen de preview ~210KB, se coloca en `res/drawable/`
3. **Video subido a Supabase** - Bucket `wallpaper-videos`

### Paso a paso - 6 archivos a modificar

#### 1. Crear la clase Scene (`scenes/NuevaEscenaScene.java`)

```java
package com.secret.blackholeglow.scenes;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

public class NuevaEscenaScene extends BaseVideoScene {

    @Override
    public String getName() {
        return "NUEVA_ESCENA";  // ID unico, MAYUSCULAS, sin espacios
    }

    @Override
    public String getDescription() {
        return "Descripcion de la escena";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_nueva_escena;  // WebP en res/drawable/
    }

    @Override
    protected String getVideoFileName() {
        return "video_nueva_escena.mp4";  // Mismo nombre que en Supabase
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        // Temas disponibles: COSMIC, OCEAN, FIRE, WALKING_DEAD, NEON, GOLD, etc.
        return EqualizerBarsDJ.Theme.COSMIC;
    }
}
```

#### 2. Registrar video en `VideoConfig.java`

```java
// En el bloque static {}
VIDEOS.put("video_nueva_escena.mp4", new ResourceInfo(
    SUPABASE_VIDEOS_URL + "video_nueva_escena.mp4",
    12_345_678L,  // Tamano en bytes (ver propiedades del archivo)
    "Nombre Display",
    1  // version (incrementar si reemplazas el video)
));
```

#### 3. Registrar en `SceneFactory.java`

```java
// Import arriba:
import com.secret.blackholeglow.scenes.NuevaEscenaScene;

// En registerDefaultScenes():
registerScene("NUEVA_ESCENA", NuevaEscenaScene.class);
```

#### 4. Registrar en `WallpaperCatalog.java`

```java
// En initializeCatalog():
catalog.add(new WallpaperItem.Builder("NOMBRE DISPLAY")
        .descripcion("Descripcion para el usuario.")
        .preview(R.drawable.preview_nueva_escena)
        .sceneName("NUEVA_ESCENA")
        .tier(WallpaperTier.FREE)
        .badge("EMOJI BADGE")
        .glow(0xFFCOLOR)  // Color ARGB del glow
        .featured()
        .build());
```

#### 5. Registrar en `ResourcePreloader.java` (3 lugares)

**a) En `prepareTasksForScene()` switch:**
```java
case "NUEVA_ESCENA":
    prepareNuevaEscenaSceneTasks();
    break;
```

**b) En `getSceneVideos()` switch:**
```java
case "NUEVA_ESCENA":
    return Arrays.asList("video_nueva_escena.mp4");
```

**c) En `getSceneVideosStatic()` switch:**
```java
case "NUEVA_ESCENA":
    return Arrays.asList("video_nueva_escena.mp4");
```

**d) Crear metodo de preparacion:**
```java
public void prepareNuevaEscenaSceneTasks() {
    tasks.clear();
    addVideoDownloadTask("Video Nueva Escena", "video_nueva_escena.mp4", 10);
    addTextureTask("Preparando escena", R.drawable.preview_nueva_escena, 2);
    calculateTotalWeight();
}
```

#### 6. Registrar en `WallpaperPreferences.java`

**CRITICO** - Sin esto, la seleccion se rechaza silenciosamente:

```java
// En VALID_WALLPAPERS set:
"NUEVA_ESCENA",  // Descripcion
```

### Checklist rapido

- [ ] Video MP4 subido a Supabase bucket `wallpaper-videos`
- [ ] Preview WebP copiado a `app/src/main/res/drawable/`
- [ ] `scenes/NuevaEscenaScene.java` creado
- [ ] `VideoConfig.java` - entrada con URL, tamano, version
- [ ] `SceneFactory.java` - import + registerScene()
- [ ] `WallpaperCatalog.java` - entrada en catalogo
- [ ] `ResourcePreloader.java` - 3 switches + metodo prepare
- [ ] `WallpaperPreferences.java` - nombre en VALID_WALLPAPERS
- [ ] Build exitoso (`gradlew assembleDebug`)
- [ ] Probado en dispositivo

---

## Escenas Actuales (v5.0.14)

| Escena | Video | Resolucion | Tamano | Theme |
|--------|-------|-----------|--------|-------|
| PYRALIS | cielovolando.mp4 | 360x360 | 9.9 MB | COSMIC |
| ABYSSIA | marZerg.mp4 | 360x360 | 9.5 MB | OCEAN |
| GOKU | gokufinalkamehamehaHD.mp4 | 540x800 | 11.4 MB | FIRE |
| ADVENTURE_TIME | escenaHDA.mp4 | 360x534 | 11.3 MB | FIRE |
| NEON_CITY | neoncityScene.mp4 | 360x360 | 11.4 MB | NEON |
| WALKING_DEAD | walkingdeathscene.mp4 | 540x956 | 6.0 MB | WALKING_DEAD |
| SUPERMAN | superman_scene.mp4 | 540x800 | 7.5 MB | COSMIC |
| AOT | erenEscena01.mp4 | 540x800 | 3.2 MB | FIRE |
| SPIDER | spiderscene.mp4 | 540x800 | 3.2 MB | WALKING_DEAD |
| LOST_ATLANTIS | lostatlanstis.mp4 | 540x800 | 3.3 MB | OCEAN |
| THE_HUMAN_PREDATOR | guerrerovsleon.mp4 | original | 15.6 MB | WALKING_DEAD |

### Escenas sin video (parallax/imagen)
- **SAINT_SEIYA** - 2 capas parallax (fondo + personaje) con depth maps
- **ZELDA_BOTW** - 4 capas parallax + modelo 3D de Link

---

## Optimizacion de Videos

### Formato recomendado
- **Resolucion**: 540x800 (portrait 9:16) o 360x360 (cuadrado)
- **Codec**: H.264 (maxima compatibilidad)
- **Bitrate**: 1.5-2.5 Mbps
- **Loop**: El video debe hacer loop seamless (inicio = fin)
- **Duracion**: 5-15 segundos tipico

### Re-encoding con FFmpeg
```bash
# Portrait 540x800
ffmpeg -i input.mp4 -vf "scale=540:800" -c:v libx264 -b:v 2.5M -an output.mp4

# Cuadrado 360x360
ffmpeg -i input.mp4 -vf "scale=360:360" -c:v libx264 -b:v 1.5M -an output.mp4
```

### Impacto en RAM del decoder
- 360x360 @ 1.5Mbps = ~15-20 MB RAM en decoder
- 540x800 @ 2.5Mbps = ~25-35 MB RAM en decoder
- 1080x1600 @ 5Mbps = ~60-80 MB RAM en decoder (evitar)

---

## Arquitectura del Sistema de Video

### Flujo de reproduccion
```
WallpaperCatalog -> WallpaperPreferences -> LiveWallpaperService
    -> WallpaperDirector -> SceneFactory -> TheScene (BaseVideoScene)
        -> MediaCodecVideoRenderer -> Surface -> OpenGL texture
```

### Clases clave
- **BaseVideoScene** - Clase base para todas las escenas de video. Maneja ciclo de vida del renderer.
- **MediaCodecVideoRenderer** - Decodifica MP4 usando MediaCodec hardware. Incluye fallback codec search.
- **VideoConfig** - Mapeo de nombre de archivo -> URL de Supabase + metadata
- **VideoDownloadManager** - Descarga videos desde Supabase, cache local, verificacion de integridad
- **ResourcePreloader** - Precarga recursos antes de mostrar escena, reporta progreso
- **EqualizerBarsDJ** - Ecualizador visual que reacciona a la musica del dispositivo

### Themes del ecualizador (EqualizerBarsDJ.Theme)
- `COSMIC` - Azul/morado cosmico
- `OCEAN` - Turquesa/verde oceano
- `FIRE` - Naranja/rojo fuego
- `WALKING_DEAD` - Verde toxico/rojo sangre
- `NEON` - Rosa/cyan synthwave
- `GOLD` - Dorado

---

## Dispositivos de Prueba

| Dispositivo | ADB Serial | Tipo APK | RAM | Notas |
|------------|-----------|---------|-----|-------|
| Samsung | RF8X903KZ3K | Debug | 6+ GB | Principal |
| Huawei | G2R4C17516000149 | Release | 1.8 GB | Low-end, probar OOM |

### Comandos utiles
```bash
# Instalar en Samsung (debug)
adb -s RF8X903KZ3K install -r app/build/outputs/apk/debug/app-debug.apk

# Instalar en Huawei (release, firma diferente)
adb -s G2R4C17516000149 install -r app/build/outputs/apk/release/app-release.apk

# Ver memoria del proceso
adb shell dumpsys meminfo com.secret.blackholeglow

# Ver logs en tiempo real
adb logcat -s WallpaperDirector SceneFactory MediaCodecVideoRenderer BaseVideoScene
```

---

## Fixes Aplicados en v5.0.14

### OOM Prevention (Huawei)
- `android:largeHeap="true"` en AndroidManifest
- `inSampleSize` downsampling en: GamingController3D, WallpaperAdapter, LikeButton, CloudFrame, LoadingBar, DecorationSprite
- `RGB_565` en lugar de `ARGB_8888` donde sea posible (50% menos RAM)

### Compatibilidad Multi-Dispositivo
- **MusicVisualizer**: Check `RECORD_AUDIO` permission + `getCaptureSizeRange()` validation + session ID fallback
- **MediaCodecVideoRenderer**: Fallback codec search via `MediaCodecList` cuando el codec primario falla
- **WallpaperDirector**: `onTrimMemory` agresivo desde nivel 5 (antes nivel 15)
- **LiveWallpaperService**: Debounce 300ms en `onVisibilityChanged` para evitar stuttering en Samsung

---

## Workflow para Crear Contenido

### 1. Generar imagen base
- Usar **Gemini** o **Grok** para generar la imagen del wallpaper
- Iterar sobre el prompt hasta obtener la composicion deseada
- Formato portrait (9:16)

### 2. Animar como video
- Usar **DeeVid AI** (deevid.ai) con opcion "Between Images" para crear loops
- Frame01 y Frame02 con diferencias sutiles de animacion
- Formato 9:16 portrait

### 3. Optimizar video
- Re-encode a 540x800 con FFmpeg si es necesario
- Verificar que el loop sea seamless
- Target: menos de 15 MB

### 4. Crear preview
- Captura un frame representativo del video
- Convertir a WebP (~200KB)
- Colocar en `res/drawable/preview_nombre.webp`

### 5. Integrar en la app
- Seguir los 6 pasos de la seccion "Como Crear un Nuevo Wallpaper"
- Build + test en dispositivo
- Subir AAB a Play Console
