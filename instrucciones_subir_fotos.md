# Instrucciones: Subir Nuevos Wallpapers al Catálogo Dinámico

> **Para Claude**: Lee este archivo completo antes de empezar. Contiene TODO lo necesario
> para agregar nuevas imágenes y videos al catálogo dinámico sin tocar código Java.

## Resumen
La app Black Hole Glow tiene un sistema de catálogo dinámico que descarga
`dynamic_catalog.json` de Supabase al iniciar. Para agregar wallpapers nuevos
solo hay que: optimizar asset → subir a Supabase → actualizar el JSON.

Soporta dos tipos:
- **IMAGE**: Imagen estática con efecto parallax
- **VIDEO**: Video en loop con equalizer bars, reloj y batería overlay

## Requisitos
- `ffmpeg` instalado (para convertir PNG → WebP)
- `curl` disponible
- Imágenes fuente en PNG (idealmente 1024x1536 portrait)

## Paso 1: Convertir PNG a WebP

Por cada imagen, generar 2 archivos:

```bash
# Full size (max 1080px ancho, calidad 85)
ffmpeg -y -i "RUTA/imagen.png" -vf "scale='min(1080,iw)':-1" -quality 85 -compression_level 6 /tmp/webp_output/dyn_ID.webp

# Preview (max 512px ancho, calidad 75)
ffmpeg -y -i "RUTA/imagen.png" -vf "scale='min(512,iw)':-1" -quality 75 -compression_level 6 /tmp/webp_output/dyn_ID_preview.webp
```

**Naming convention**: `dyn_` + id en snake_case + `.webp`
Ejemplo: id="gatito_ninja" → `dyn_gatito_ninja.webp` + `dyn_gatito_ninja_preview.webp`

## Paso 2: Subir a Supabase

```bash
# Token de servicio (service role)
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ6dXd2c21seWlnanRzZWFyeHltIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc1ODY0ODcwOSwiZXhwIjoyMDc0MjI0NzA5fQ.xDs_HCkdqcEVJktzTdjGIXnG-V--j86jbkrUA4SjAOs"

BUCKET_URL="https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/wallpaper-images"

# Subir cada archivo (full + preview)
curl -s -X POST "$BUCKET_URL/dyn_ID.webp" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: image/webp" \
  -H "x-upsert: true" \
  --data-binary @/tmp/webp_output/dyn_ID.webp

curl -s -X POST "$BUCKET_URL/dyn_ID_preview.webp" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: image/webp" \
  -H "x-upsert: true" \
  --data-binary @/tmp/webp_output/dyn_ID_preview.webp
```

Respuesta exitosa: `{"Key":"wallpaper-images/dyn_ID.webp","Id":"uuid..."}`

## Paso 3: Actualizar dynamic_catalog.json

### 3a. Descargar el JSON actual
```bash
curl -s "https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/public/wallpaper-images/dynamic_catalog.json" > /tmp/catalog.json
```

### 3b. Agregar nueva entrada al array "wallpapers"
```json
{
  "id": "mi_nuevo_id",
  "type": "IMAGE",
  "name": "MI NOMBRE DISPLAY",
  "description": "Descripción para el usuario en inglés.",
  "imageFile": "dyn_mi_nuevo_id.webp",
  "previewFile": "dyn_mi_nuevo_id_preview.webp",
  "imageSize": 123456,
  "previewSize": 45678,
  "glowColor": "#FF4500",
  "badge": "NEW",
  "sortOrder": 10,
  "category": "ANIME"
}
```

**Categorías válidas**: ANIME, GAMING, SCENES, ANIMALS, NATURE, UNIVERSE, MISC, CHRISTMAS, SUMMER, AUTUMN, WINTER, SPECIAL

**Obtener tamaños en bytes**:
```bash
stat --printf="%s" /tmp/webp_output/dyn_ID.webp
stat --printf="%s" /tmp/webp_output/dyn_ID_preview.webp
```

**Colores glow sugeridos**:
- Dorado: #FFD700  | Rojo: #DC143C    | Naranja: #FF4500
- Azul: #00BFFF    | Púrpura: #8B00FF | Verde: #00FF7F
- Rosa: #FF1493    | Turquesa: #00CED1

### 3c. Subir JSON actualizado
```bash
curl -s -X POST "$BUCKET_URL/dynamic_catalog.json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "x-upsert: true" \
  --data-binary @"RUTA_AL_JSON_EDITADO"
```

### 3d. Verificar
```bash
curl -s "https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/public/wallpaper-images/dynamic_catalog.json" | python -m json.tool
```

## Paso 4: Verificar en la app
- Abrir la app → los nuevos wallpapers aparecen automáticamente
- Si no aparecen: limpiar datos de la app (`adb shell pm clear com.secret.blackholeglow`)
- La app cachea el catálogo por 6 horas, pero si el cache está vacío hace refresh inmediato

## Notas importantes
- **NO se necesita tocar código Java** — el sistema dinámico maneja todo
- **NO se necesita rebuild** del APK/AAB para agregar wallpapers
- Las imágenes PNG fuente pueden estar en cualquier carpeta del PC
- El `sortOrder` define el orden (menor = aparece primero)
- El `badge` aparece como etiqueta en la card del wallpaper
- Si se sube una versión para Play Store, hay que bumper `versionCode`/`versionName` en `app/build.gradle.kts`

---

# Pipeline de Videos

## Requisitos adicionales
- `ffmpeg` con soporte H.264 (libx264)
- Video fuente en MP4, portrait (1080x1920), loop-friendly

## Paso V1: Optimizar video (720p, sin audio)

```bash
ffmpeg -y -i "RUTA/video_original.mp4" \
  -vf "scale=720:-2" \
  -c:v libx264 -crf 23 \
  -an -movflags +faststart \
  /tmp/video_output/dyn_ID.mp4
```

**Parámetros**:
- `scale=720:-2` — reduce a 720px ancho (mantiene aspect ratio, altura par)
- `-crf 23` — calidad buena con buen ratio de compresión (~1-3MB para 6s)
- `-an` — elimina audio (no se usa en wallpaper)
- `-movflags +faststart` — optimiza para streaming/carga rápida

**Naming convention**: `dyn_` + id en snake_case + `.mp4`

## Paso V2: Extraer preview frame → WebP

```bash
ffmpeg -y -i "RUTA/video_original.mp4" \
  -ss 00:00:02 \
  -frames:v 1 \
  -vf "scale=512:-1" \
  -quality 75 \
  /tmp/video_output/dyn_ID_preview.webp
```

**Parámetros**:
- `-ss 00:00:02` — captura frame a los 2 segundos (ajustar si el frame no es representativo)
- `scale=512:-1` — preview a 512px ancho

## Paso V3: Subir a Supabase

```bash
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ6dXd2c21seWlnanRzZWFyeHltIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc1ODY0ODcwOSwiZXhwIjoyMDc0MjI0NzA5fQ.xDs_HCkdqcEVJktzTdjGIXnG-V--j86jbkrUA4SjAOs"

# Video → bucket wallpaper-videos
curl -s -X POST "https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/wallpaper-videos/dyn_ID.mp4" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: video/mp4" \
  -H "x-upsert: true" \
  --data-binary @/tmp/video_output/dyn_ID.mp4

# Preview → bucket wallpaper-images (mismo bucket que las imágenes)
curl -s -X POST "https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/wallpaper-images/dyn_ID_preview.webp" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: image/webp" \
  -H "x-upsert: true" \
  --data-binary @/tmp/video_output/dyn_ID_preview.webp
```

## Paso V4: Actualizar dynamic_catalog.json

Agregar entrada con `"type": "VIDEO"` al array `wallpapers`:

```json
{
  "id": "mi_video_id",
  "type": "VIDEO",
  "name": "MI VIDEO DISPLAY",
  "description": "Description for the user in English.",
  "videoFile": "dyn_mi_video_id.mp4",
  "previewFile": "dyn_mi_video_id_preview.webp",
  "videoSize": 1251959,
  "previewSize": 25534,
  "glowColor": "#00FF7F",
  "badge": "NEW",
  "sortOrder": 10,
  "category": "SCENES"
}
```

**Diferencias vs IMAGE**:
| Campo | IMAGE | VIDEO |
|-------|-------|-------|
| type | `"IMAGE"` | `"VIDEO"` |
| archivo principal | `imageFile` (.webp) | `videoFile` (.mp4) |
| tamaño principal | `imageSize` | `videoSize` |
| bucket principal | wallpaper-images | wallpaper-videos |
| preview | wallpaper-images | wallpaper-images (mismo) |

**Obtener tamaños en bytes**:
```bash
stat --printf="%s" /tmp/video_output/dyn_ID.mp4
stat --printf="%s" /tmp/video_output/dyn_ID_preview.webp
```

## Paso V5: Verificar en la app
- Limpiar cache: `adb shell pm clear com.secret.blackholeglow`
- Abrir app → el video aparece en catálogo con preview estático
- Tap → descarga video (~1-3MB) → instalar → video loop con equalizer/clock/battery
- DynamicVideoScene auto-incluye: reproductor de video, equalizer bars, reloj, batería

## Ejemplo completo: ovni_scene (primera video scene dinámica)

```bash
# 1. Optimizar (9.1MB → 1.2MB)
ffmpeg -y -i ovni_scene.mp4 -vf "scale=720:-2" -c:v libx264 -crf 23 -an -movflags +faststart dyn_ovni_scene.mp4

# 2. Preview
ffmpeg -y -i ovni_scene.mp4 -ss 00:00:02 -frames:v 1 -vf "scale=512:-1" -quality 75 dyn_ovni_scene_preview.webp

# 3. Upload
curl -s -X POST ".../wallpaper-videos/dyn_ovni_scene.mp4" ... --data-binary @dyn_ovni_scene.mp4
curl -s -X POST ".../wallpaper-images/dyn_ovni_scene_preview.webp" ... --data-binary @dyn_ovni_scene_preview.webp

# 4. JSON entry: type=VIDEO, videoFile=dyn_ovni_scene.mp4, videoSize=1251959, glowColor=#00FF7F, category=SCENES
```
