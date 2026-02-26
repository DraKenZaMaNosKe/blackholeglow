# Instrucciones: Subir Nuevas Fotos al Catálogo Dinámico

> **Para Claude**: Lee este archivo completo antes de empezar. Contiene TODO lo necesario
> para agregar nuevas imágenes al catálogo dinámico sin tocar código Java.

## Resumen
La app Black Hole Glow tiene un sistema de catálogo dinámico que descarga
`dynamic_catalog.json` de Supabase al iniciar. Para agregar wallpapers nuevos
solo hay que: optimizar imagen → subir a Supabase → actualizar el JSON.

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

## Para subir VIDEO scenes (futuro)
- Mismo proceso pero: subir mp4 a bucket `wallpaper-videos/`
- En JSON: `"type": "VIDEO"`, usar `"videoFile"` y `"videoSize"` en vez de imageFile/imageSize
- DynamicVideoScene auto-incluye: reproductor de video, equalizer bars, reloj, batería
