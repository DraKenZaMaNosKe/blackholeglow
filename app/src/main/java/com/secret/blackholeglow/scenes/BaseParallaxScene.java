package com.secret.blackholeglow.scenes;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import com.secret.blackholeglow.Battery3D;
import com.secret.blackholeglow.Clock3D;
import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.GyroscopeManager;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.core.MemoryPressureLevel;
import com.secret.blackholeglow.image.ImageDownloadManager;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                                      ║
 * ║                    🖼️ BASE PARALLAX SCENE - CLASE BASE ABSTRACTA                     ║
 * ║                                                                                      ║
 * ║         Para escenas con IMÁGENES ESTÁTICAS + DEPTH MAP DISPLACEMENT                 ║
 * ║                                                                                      ║
 * ╠══════════════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                                      ║
 * ║  ╔════════════════════════════════════════════════════════════════════════════════╗  ║
 * ║  ║                     ⚠️ NO BORRAR POR FAVOR - DOCUMENTACIÓN ⚠️                  ║  ║
 * ║  ║                                                                                ║  ║
 * ║  ║  Esta documentación es CRÍTICA para que Claude Code entienda cómo             ║  ║
 * ║  ║  crear nuevas escenas parallax. NO ELIMINAR estos comentarios.                ║  ║
 * ║  ╚════════════════════════════════════════════════════════════════════════════════╝  ║
 * ║                                                                                      ║
 * ╠══════════════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                                      ║
 * ║  📖 ¿QUÉ ES UNA ESCENA PARALLAX?                                                     ║
 * ║  ════════════════════════════════                                                    ║
 * ║                                                                                      ║
 * ║  Una escena parallax usa IMÁGENES ESTÁTICAS (PNG/WEBP) en lugar de video.            ║
 * ║  El efecto 3D se logra mediante:                                                     ║
 * ║                                                                                      ║
 * ║  1. DEPTH MAPS: Imágenes en escala de grises donde:                                  ║
 * ║     - BLANCO (255) = Objeto CERCA → Se mueve MUCHO con giroscopio                    ║
 * ║     - NEGRO (0) = Objeto LEJOS → Se mueve POCO con giroscopio                        ║
 * ║                                                                                      ║
 * ║  2. GIROSCOPIO: El sensor del dispositivo detecta inclinación y                      ║
 * ║     desplaza los píxeles según su profundidad en el depth map.                       ║
 * ║                                                                                      ║
 * ║  3. CAPAS: Múltiples imágenes PNG superpuestas, cada una puede tener                 ║
 * ║     su propio depth map o ser estática.                                              ║
 * ║                                                                                      ║
 * ╠══════════════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                                      ║
 * ║  📖 DIFERENCIA CON BaseVideoScene:                                                   ║
 * ║  ═══════════════════════════════════                                                 ║
 * ║                                                                                      ║
 * ║  ┌─────────────────────┬──────────────────────┬──────────────────────┐               ║
 * ║  │ Característica      │ BaseVideoScene       │ BaseParallaxScene    │               ║
 * ║  ├─────────────────────┼──────────────────────┼──────────────────────┤               ║
 * ║  │ Fondo               │ Video MP4            │ Imágenes PNG/WEBP    │               ║
 * ║  │ Efecto 3D           │ No (video plano)     │ Depth displacement   │               ║
 * ║  │ Giroscopio          │ Opcional             │ ESENCIAL             │               ║
 * ║  │ Consumo batería     │ Alto (MediaCodec)    │ Bajo (solo GPU)      │               ║
 * ║  │ Uso de memoria      │ Alto (buffer video)  │ Medio (texturas)     │               ║
 * ║  │ Capas               │ 1 (el video)         │ Múltiples            │               ║
 * ║  └─────────────────────┴──────────────────────┴──────────────────────┘               ║
 * ║                                                                                      ║
 * ╠══════════════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                                      ║
 * ║  📖 ARQUITECTURA DE CAPAS:                                                           ║
 * ║  ═══════════════════════════                                                         ║
 * ║                                                                                      ║
 * ║  Las escenas parallax se componen de CAPAS dibujadas de atrás hacia adelante:        ║
 * ║                                                                                      ║
 * ║     ┌───────────────────────────────────────────┐                                    ║
 * ║     │  CAPA 4: UI (Ecualizador, Reloj, Batería) │  ← Siempre al frente               ║
 * ║     ├───────────────────────────────────────────┤                                    ║
 * ║     │  CAPA 3: Objetos 3D (Link, etc)           │  ← Opcionales                      ║
 * ║     ├───────────────────────────────────────────┤                                    ║
 * ║     │  CAPA 2: Primer plano (PNG estático)      │  ← Sin depth, no se mueve          ║
 * ║     ├───────────────────────────────────────────┤                                    ║
 * ║     │  CAPA 1: Fondo con DEPTH MAP              │  ← Se mueve con giroscopio         ║
 * ║     └───────────────────────────────────────────┘                                    ║
 * ║                                                                                      ║
 * ╠══════════════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                                      ║
 * ║  📖 CÓMO FUNCIONA EL DEPTH DISPLACEMENT:                                             ║
 * ║  ═════════════════════════════════════════                                           ║
 * ║                                                                                      ║
 * ║  El shader lee dos texturas:                                                         ║
 * ║  1. u_Texture: La imagen de color (lo que se ve)                                     ║
 * ║  2. u_DepthMap: El mapa de profundidad (escala de grises)                            ║
 * ║                                                                                      ║
 * ║  Para cada píxel:                                                                    ║
 * ║  ┌──────────────────────────────────────────────────────────────────────────┐        ║
 * ║  │  float depth = texture2D(u_DepthMap, uv).r;  // 0.0 a 1.0               │        ║
 * ║  │  vec2 offset = u_GyroOffset * depth * u_DepthScale;                     │        ║
 * ║  │  vec2 finalUV = uv + offset;                                            │        ║
 * ║  │  gl_FragColor = texture2D(u_Texture, finalUV);                          │        ║
 * ║  └──────────────────────────────────────────────────────────────────────────┘        ║
 * ║                                                                                      ║
 * ║  Resultado: Los objetos "cercanos" (depth alto) se mueven más que los "lejanos".     ║
 * ║                                                                                      ║
 * ╠══════════════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                                      ║
 * ║  📖 CÓMO CREAR UN DEPTH MAP:                                                         ║
 * ║  ═════════════════════════════                                                       ║
 * ║                                                                                      ║
 * ║  1. Abre tu imagen en Photoshop/GIMP                                                 ║
 * ║  2. Crea una nueva capa en escala de grises                                          ║
 * ║  3. Pinta con:                                                                       ║
 * ║     - BLANCO: Objetos en primer plano (se moverán mucho)                             ║
 * ║     - GRIS: Objetos a distancia media                                                ║
 * ║     - NEGRO: Objetos de fondo (casi no se moverán)                                   ║
 * ║  4. Guarda como PNG con el sufijo "_depth" (ej: fondo_depth.png)                     ║
 * ║                                                                                      ║
 * ║  HERRAMIENTAS AUTOMÁTICAS:                                                           ║
 * ║  - IA de Meshy: Genera depth maps desde fotos                                        ║
 * ║  - MiDaS (Python): modelo de ML para depth estimation                                ║
 * ║  - Photoshop: Filtro > 3D > Generar mapa de profundidad                              ║
 * ║                                                                                      ║
 * ╠══════════════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                                      ║
 * ║  📖 SISTEMA DE HOOKS - PUNTOS DE EXTENSIÓN:                                          ║
 * ║  ═════════════════════════════════════════════                                       ║
 * ║                                                                                      ║
 * ║  Las subclases DEBEN implementar estos métodos abstractos:                           ║
 * ║                                                                                      ║
 * ║  ┌────────────────────────────────────────────────────────────────────────────────┐  ║
 * ║  │ MÉTODO                      │ PROPÓSITO                                       │  ║
 * ║  ├────────────────────────────────────────────────────────────────────────────────┤  ║
 * ║  │ getName()                   │ Nombre único de la escena (ej: "ZELDA_BOTW")    │  ║
 * ║  │ getDescription()            │ Descripción para UI (ej: "Zelda BOTW Parallax") │  ║
 * ║  │ getPreviewResourceId()      │ Drawable para preview (ej: R.drawable.preview)  │  ║
 * ║  │ getTheme()                  │ Tema del ecualizador                            │  ║
 * ║  │ getLayers()                 │ CRÍTICO: Define las capas de la escena          │  ║
 * ║  └────────────────────────────────────────────────────────────────────────────────┘  ║
 * ║                                                                                      ║
 * ║  Las subclases PUEDEN sobrescribir estos hooks opcionales:                           ║
 * ║                                                                                      ║
 * ║  ┌────────────────────────────────────────────────────────────────────────────────┐  ║
 * ║  │ HOOK                            │ CUÁNDO SE LLAMA                              │  ║
 * ║  ├────────────────────────────────────────────────────────────────────────────────┤  ║
 * ║  │ setupSceneSpecific()            │ Después de cargar texturas y shaders         │  ║
 * ║  │ updateSceneSpecific(deltaTime)  │ Cada frame, para objetos 3D propios          │  ║
 * ║  │ drawSceneSpecific()             │ Después de capas, antes de UI                │  ║
 * ║  │ releaseSceneSpecificResources() │ Al destruir la escena                        │  ║
 * ║  │ onPauseSceneSpecific()          │ Cuando el wallpaper se oculta                │  ║
 * ║  │ onResumeSceneSpecific()         │ Cuando el wallpaper vuelve a verse           │  ║
 * ║  │ onTouchEventRaw(MotionEvent)    │ Para calibración de objetos 3D               │  ║
 * ║  └────────────────────────────────────────────────────────────────────────────────┘  ║
 * ║                                                                                      ║
 * ╠══════════════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                                      ║
 * ║  📖 EJEMPLO DE IMPLEMENTACIÓN MÍNIMA:                                                ║
 * ║  ══════════════════════════════════════                                              ║
 * ║                                                                                      ║
 * ║  ┌──────────────────────────────────────────────────────────────────────────────┐    ║
 * ║  │  public class MiEscenaParallax extends BaseParallaxScene {                   │    ║
 * ║  │                                                                              │    ║
 * ║  │      @Override public String getName() { return "MI_ESCENA"; }               │    ║
 * ║  │                                                                              │    ║
 * ║  │      @Override public String getDescription() {                              │    ║
 * ║  │          return "Mi escena parallax 3D";                                     │    ║
 * ║  │      }                                                                       │    ║
 * ║  │                                                                              │    ║
 * ║  │      @Override public int getPreviewResourceId() {                           │    ║
 * ║  │          return R.drawable.preview_mi_escena;                                │    ║
 * ║  │      }                                                                       │    ║
 * ║  │                                                                              │    ║
 * ║  │      @Override protected EqualizerBarsDJ.Theme getTheme() {                  │    ║
 * ║  │          return EqualizerBarsDJ.Theme.DEFAULT;                               │    ║
 * ║  │      }                                                                       │    ║
 * ║  │                                                                              │    ║
 * ║  │      @Override protected ParallaxLayer[] getLayers() {                       │    ║
 * ║  │          return new ParallaxLayer[] {                                        │    ║
 * ║  │              // Fondo con depth map (se mueve con giroscopio)                │    ║
 * ║  │              new ParallaxLayer("fondo.png", "fondo_depth.png", 0.1f),        │    ║
 * ║  │              // Primer plano estático                                        │    ║
 * ║  │              new ParallaxLayer("frente.png", null, 0f)                       │    ║
 * ║  │          };                                                                  │    ║
 * ║  │      }                                                                       │    ║
 * ║  │  }                                                                           │    ║
 * ║  └──────────────────────────────────────────────────────────────────────────────┘    ║
 * ║                                                                                      ║
 * ╠══════════════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                                      ║
 * ║  📖 EJEMPLO CON OBJETO 3D (como Link):                                               ║
 * ║  ═════════════════════════════════════                                               ║
 * ║                                                                                      ║
 * ║  ┌──────────────────────────────────────────────────────────────────────────────┐    ║
 * ║  │  public class ZeldaScene extends BaseParallaxScene {                         │    ║
 * ║  │      private Link3D link3D;                                                  │    ║
 * ║  │                                                                              │    ║
 * ║  │      // ... métodos abstractos obligatorios ...                              │    ║
 * ║  │                                                                              │    ║
 * ║  │      @Override                                                               │    ║
 * ║  │      protected void setupSceneSpecific() {                                   │    ║
 * ║  │          link3D = new Link3D(context);                                       │    ║
 * ║  │          link3D.setPosition(-0.5f, -1.0f, 0f);                               │    ║
 * ║  │          link3D.setScale(0.45f);                                             │    ║
 * ║  │      }                                                                       │    ║
 * ║  │                                                                              │    ║
 * ║  │      @Override                                                               │    ║
 * ║  │      protected void updateSceneSpecific(float deltaTime) {                   │    ║
 * ║  │          if (link3D != null) link3D.update(deltaTime);                       │    ║
 * ║  │      }                                                                       │    ║
 * ║  │                                                                              │    ║
 * ║  │      @Override                                                               │    ║
 * ║  │      protected void drawSceneSpecific() {                                    │    ║
 * ║  │          if (link3D != null) {                                               │    ║
 * ║  │              GLES30.glEnable(GLES30.GL_DEPTH_TEST);                          │    ║
 * ║  │              link3D.draw();                                                  │    ║
 * ║  │              GLES30.glDisable(GLES30.GL_DEPTH_TEST);                         │    ║
 * ║  │          }                                                                   │    ║
 * ║  │      }                                                                       │    ║
 * ║  │                                                                              │    ║
 * ║  │      @Override                                                               │    ║
 * ║  │      protected void releaseSceneSpecificResources() {                        │    ║
 * ║  │          if (link3D != null) { link3D.dispose(); link3D = null; }            │    ║
 * ║  │      }                                                                       │    ║
 * ║  │  }                                                                           │    ║
 * ║  └──────────────────────────────────────────────────────────────────────────────┘    ║
 * ║                                                                                      ║
 * ╠══════════════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                                      ║
 * ║  📖 ESTRUCTURA DE ParallaxLayer:                                                     ║
 * ║  ═════════════════════════════════                                                   ║
 * ║                                                                                      ║
 * ║  Cada capa se define con:                                                            ║
 * ║  - colorFile: Nombre del archivo de imagen (ej: "fondo.png")                         ║
 * ║  - depthFile: Nombre del depth map (ej: "fondo_depth.png") o null si es estática     ║
 * ║  - depthScale: Intensidad del efecto (0.0 = sin movimiento, 0.1 = sutil)             ║
 * ║  - alpha: Transparencia de la capa (1.0 = opaca)                                     ║
 * ║  - useCoverMode: true para ajustar aspect ratio (solo fondo)                         ║
 * ║                                                                                      ║
 * ║  IMPORTANTE: Las capas se dibujan EN ORDEN del array (primera = más atrás)           ║
 * ║                                                                                      ║
 * ╠══════════════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                                      ║
 * ║  📖 CONFIGURACIÓN DEL GIROSCOPIO:                                                    ║
 * ║  ═════════════════════════════════                                                   ║
 * ║                                                                                      ║
 * ║  El giroscopio se configura automáticamente con:                                     ║
 * ║  - Sensibilidad: 1.5f (moderada, ajustable con getGyroSensitivity())                 ║
 * ║  - Smoothing: 0.15f (suavizado de movimiento)                                        ║
 * ║  - Max offset: 0.5f (máximo desplazamiento)                                          ║
 * ║                                                                                      ║
 * ║  Para personalizar, sobrescribe:                                                     ║
 * ║  - getGyroSensitivity() → float entre 0.5f y 3.0f                                    ║
 * ║  - getSmoothFactor() → float entre 0.05f y 0.3f                                      ║
 * ║  - getMaxOffset() → float entre 0.2f y 1.0f                                          ║
 * ║                                                                                      ║
 * ╠══════════════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                                      ║
 * ║  📖 CICLO DE VIDA:                                                                   ║
 * ║  ═════════════════                                                                   ║
 * ║                                                                                      ║
 * ║  ┌─────────────────────────────────────────────────────────────────────────────┐     ║
 * ║  │                                                                             │     ║
 * ║  │   setupScene()                                                              │     ║
 * ║  │       │                                                                     │     ║
 * ║  │       ├──► Crear giroscopio                                                 │     ║
 * ║  │       ├──► Crear shaders (depth + simple)                                   │     ║
 * ║  │       ├──► Crear buffers del quad                                           │     ║
 * ║  │       ├──► Cargar texturas de capas                                         │     ║
 * ║  │       ├──► Crear UI (ecualizador, reloj, batería)                           │     ║
 * ║  │       └──► setupSceneSpecific() ← HOOK para objetos 3D                      │     ║
 * ║  │                                                                             │     ║
 * ║  │   update(deltaTime)                                                         │     ║
 * ║  │       │                                                                     │     ║
 * ║  │       ├──► Leer giroscopio y suavizar offset                                │     ║
 * ║  │       ├──► Actualizar UI                                                    │     ║
 * ║  │       └──► updateSceneSpecific(deltaTime) ← HOOK                            │     ║
 * ║  │                                                                             │     ║
 * ║  │   draw()                                                                    │     ║
 * ║  │       │                                                                     │     ║
 * ║  │       ├──► Clear con color de fondo                                         │     ║
 * ║  │       ├──► Dibujar capas (depth o estáticas)                                │     ║
 * ║  │       ├──► drawSceneSpecific() ← HOOK para objetos 3D                       │     ║
 * ║  │       └──► Dibujar UI                                                       │     ║
 * ║  │                                                                             │     ║
 * ║  │   onPause()                                                                 │     ║
 * ║  │       │                                                                     │     ║
 * ║  │       ├──► Pausar giroscopio (ahorro batería)                               │     ║
 * ║  │       └──► onPauseSceneSpecific() ← HOOK                                    │     ║
 * ║  │                                                                             │     ║
 * ║  │   onResume()                                                                │     ║
 * ║  │       │                                                                     │     ║
 * ║  │       ├──► Reanudar giroscopio                                              │     ║
 * ║  │       └──► onResumeSceneSpecific() ← HOOK                                   │     ║
 * ║  │                                                                             │     ║
 * ║  │   releaseSceneResources()                                                   │     ║
 * ║  │       │                                                                     │     ║
 * ║  │       ├──► Liberar texturas                                                 │     ║
 * ║  │       ├──► Liberar shaders                                                  │     ║
 * ║  │       ├──► Liberar giroscopio                                               │     ║
 * ║  │       ├──► Liberar UI                                                       │     ║
 * ║  │       └──► releaseSceneSpecificResources() ← HOOK                           │     ║
 * ║  │                                                                             │     ║
 * ║  └─────────────────────────────────────────────────────────────────────────────┘     ║
 * ║                                                                                      ║
 * ╠══════════════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                                      ║
 * ║  📖 DESCARGA AUTOMÁTICA DE IMÁGENES:                                                 ║
 * ║  ═════════════════════════════════════                                               ║
 * ║                                                                                      ║
 * ║  Las imágenes se descargan automáticamente de Supabase si no existen localmente.     ║
 * ║  El sistema usa ImageDownloadManager para:                                           ║
 * ║  1. Verificar si el archivo existe en cache local                                    ║
 * ║  2. Si no existe, descargarlo de Supabase                                            ║
 * ║  3. Cargar la textura desde el archivo local                                         ║
 * ║                                                                                      ║
 * ║  UBICACIÓN DE ARCHIVOS EN SUPABASE:                                                  ║
 * ║  bucket: "images" → carpeta según escena                                             ║
 * ║  Ejemplo: images/zelda/zelda_fondo.png                                               ║
 * ║                                                                                      ║
 * ╚══════════════════════════════════════════════════════════════════════════════════════╝
 *
 * @author Claude Code - Optimización de arquitectura
 * @version 1.0.0
 * @see WallpaperScene clase padre
 * @see EqualizerBarsDJ para temas de visualización
 * @see GyroscopeManager para control por inclinación
 */
public abstract class BaseParallaxScene extends WallpaperScene {

    // ═══════════════════════════════════════════════════════════════════════════════════
    // ⚠️ NO BORRAR POR FAVOR - TAG para logging
    // ═══════════════════════════════════════════════════════════════════════════════════
    private static final String TAG = "BaseParallaxScene";

    // ═══════════════════════════════════════════════════════════════════════════════════
    // ⚠️ NO BORRAR POR FAVOR - ESTRUCTURA DE CAPA PARALLAX
    // ═══════════════════════════════════════════════════════════════════════════════════
    /**
     * ╔════════════════════════════════════════════════════════════════════════════════╗
     * ║  📖 ParallaxLayer - Define una capa de la escena parallax                      ║
     * ╠════════════════════════════════════════════════════════════════════════════════╣
     * ║                                                                                ║
     * ║  CAMPOS:                                                                       ║
     * ║  • colorFile: Nombre del archivo de imagen (ej: "fondo.png")                   ║
     * ║  • depthFile: Nombre del depth map o null si es capa estática                  ║
     * ║  • depthScale: Intensidad del efecto depth (0.0 - 1.0)                         ║
     * ║  • alpha: Transparencia (1.0 = opaco, 0.0 = invisible)                         ║
     * ║  • useCoverMode: true para ajustar aspect ratio (solo fondo)                   ║
     * ║                                                                                ║
     * ║  EJEMPLO:                                                                      ║
     * ║  new ParallaxLayer("cielo.png", "cielo_depth.png", 0.08f, 1.0f, true)          ║
     * ║                                                                                ║
     * ╚════════════════════════════════════════════════════════════════════════════════╝
     */
    public static class ParallaxLayer {
        public final String colorFile;      // Archivo de imagen de color
        public final String depthFile;      // Archivo de depth map (null = estático)
        public final float depthScale;      // Intensidad del efecto depth
        public final float alpha;           // Transparencia de la capa
        public final boolean useCoverMode;  // Ajustar aspect ratio

        // Texturas OpenGL (se llenan al cargar)
        public int colorTextureId = -1;
        public int depthTextureId = -1;
        public int imageWidth = 0;   // Actual image dimensions (set during loading)
        public int imageHeight = 0;

        /**
         * Constructor completo
         */
        public ParallaxLayer(String colorFile, String depthFile, float depthScale, float alpha, boolean useCoverMode) {
            this.colorFile = colorFile;
            this.depthFile = depthFile;
            this.depthScale = depthScale;
            this.alpha = alpha;
            this.useCoverMode = useCoverMode;
        }

        /**
         * Constructor simplificado para capa con depth
         */
        public ParallaxLayer(String colorFile, String depthFile, float depthScale) {
            this(colorFile, depthFile, depthScale, 1.0f, depthFile != null);
        }

        /**
         * Constructor para capa estática (sin depth map)
         */
        public ParallaxLayer(String colorFile) {
            this(colorFile, null, 0f, 1.0f, false);
        }

        /**
         * ¿Esta capa tiene depth map?
         */
        public boolean hasDepthMap() {
            return depthFile != null && depthTextureId > 0;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // ⚠️ NO BORRAR POR FAVOR - COMPONENTES DE LA ESCENA
    // ═══════════════════════════════════════════════════════════════════════════════════

    /** Capas de la escena (se inicializan desde getLayers()) */
    protected ParallaxLayer[] layers;

    /** Giroscopio para control por inclinación */
    protected GyroscopeManager gyroscope;

    /** Offset actual del giroscopio (suavizado) */
    protected float offsetX = 0f;
    protected float offsetY = 0f;

    // ═══════════════════════════════════════════════════════════════════════════════════
    // ⚠️ NO BORRAR POR FAVOR - SHADERS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /** Shader con depth map displacement */
    private int depthShaderProgram = -1;
    private int depthPosLoc, depthTexCoordLoc;
    private int depthTextureLoc, depthMapLoc;
    private int depthOffsetLoc, depthScaleLoc, depthAlphaLoc;

    /** Shader simple (capas sin depth map) */
    private int simpleShaderProgram = -1;
    private int simplePosLoc, simpleTexCoordLoc;
    private int simpleTextureLoc, simpleOffsetLoc, simpleAlphaLoc;

    // ═══════════════════════════════════════════════════════════════════════════════════
    // ⚠️ NO BORRAR POR FAVOR - BUFFERS DEL QUAD FULLSCREEN
    // ═══════════════════════════════════════════════════════════════════════════════════

    protected FloatBuffer quadVertexBuffer;
    protected FloatBuffer quadTexCoordBuffer;       // UV normal (fullscreen)
    protected FloatBuffer quadTexCoordBufferCover;  // UV con aspect ratio (cover mode)

    // ═══════════════════════════════════════════════════════════════════════════════════
    // ⚠️ NO BORRAR POR FAVOR - UI COMPONENTS
    // ═══════════════════════════════════════════════════════════════════════════════════

    protected EqualizerBarsDJ equalizerDJ;
    protected Clock3D clock;
    protected Battery3D battery;

    // ═══════════════════════════════════════════════════════════════════════════════════
    // ⚠️ NO BORRAR POR FAVOR - CONSTANTES DE CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════════════════════════

    /** Suavizado del giroscopio (menor = más suave) */
    private static final float DEFAULT_SMOOTH_FACTOR = 0.15f;

    /** Máximo desplazamiento del giroscopio */
    private static final float DEFAULT_MAX_OFFSET = 0.5f;

    /** Sensibilidad del giroscopio */
    private static final float DEFAULT_GYRO_SENSITIVITY = 1.5f;

    // ═══════════════════════════════════════════════════════════════════════════════════
    //
    //                    📖 MÉTODOS ABSTRACTOS - OBLIGATORIOS
    //
    //                    ⚠️ NO BORRAR POR FAVOR - DOCUMENTACIÓN ⚠️
    //
    //  Las subclases DEBEN implementar estos métodos para definir la escena.
    //  Sin estos métodos, la escena no funcionará.
    //
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * ╔════════════════════════════════════════════════════════════════════════════════╗
     * ║  📖 getTheme() - OBLIGATORIO                                                   ║
     * ╠════════════════════════════════════════════════════════════════════════════════╣
     * ║                                                                                ║
     * ║  Retorna el tema visual para el ecualizador, reloj y batería.                  ║
     * ║                                                                                ║
     * ║  TEMAS DISPONIBLES:                                                            ║
     * ║  • DEFAULT    - Azul brillante                                                 ║
     * ║  • ABYSSIA    - Verde agua/cyan (fondo del mar)                                ║
     * ║  • PYRALIS    - Rojo/naranja (fuego cósmico)                                   ║
     * ║  • KAMEHAMEHA - Azul energía Ki (Dragon Ball)                                  ║
     * ║  • SYNTHWAVE  - Rosa/púrpura neón (cyberpunk)                                  ║
     * ║  • COSMOS     - Dorado/naranja (Saint Seiya)                                   ║
     * ║  • WALKING_DEAD - Rojo sangre/gris                                             ║
     * ║  • ZELDA      - Verde Hyrule/dorado                                            ║
     * ║                                                                                ║
     * ║  EJEMPLO:                                                                      ║
     * ║  @Override                                                                     ║
     * ║  protected EqualizerBarsDJ.Theme getTheme() {                                  ║
     * ║      return EqualizerBarsDJ.Theme.ZELDA;                                       ║
     * ║  }                                                                             ║
     * ║                                                                                ║
     * ╚════════════════════════════════════════════════════════════════════════════════╝
     */
    protected abstract EqualizerBarsDJ.Theme getTheme();

    /**
     * ╔════════════════════════════════════════════════════════════════════════════════╗
     * ║  📖 getLayers() - OBLIGATORIO - CRÍTICO                                        ║
     * ╠════════════════════════════════════════════════════════════════════════════════╣
     * ║                                                                                ║
     * ║  Retorna las capas de la escena en orden de dibujo (atrás → adelante).         ║
     * ║                                                                                ║
     * ║  IMPORTANTE:                                                                   ║
     * ║  • Primera capa = más atrás (fondo)                                            ║
     * ║  • Última capa = más adelante (primer plano)                                   ║
     * ║  • Los objetos 3D se dibujan DESPUÉS de las capas (en drawSceneSpecific)       ║
     * ║                                                                                ║
     * ║  EJEMPLO CON DEPTH MAP:                                                        ║
     * ║  @Override                                                                     ║
     * ║  protected ParallaxLayer[] getLayers() {                                       ║
     * ║      return new ParallaxLayer[] {                                              ║
     * ║          // Fondo con depth map - se mueve con giroscopio                      ║
     * ║          new ParallaxLayer("fondo.png", "fondo_depth.png", 0.08f, 1.0f, true), ║
     * ║          // Capa media estática                                                ║
     * ║          new ParallaxLayer("medio.png"),                                       ║
     * ║          // Primer plano estático                                              ║
     * ║          new ParallaxLayer("frente.png")                                       ║
     * ║      };                                                                        ║
     * ║  }                                                                             ║
     * ║                                                                                ║
     * ╚════════════════════════════════════════════════════════════════════════════════╝
     */
    protected abstract ParallaxLayer[] getLayers();

    // ═══════════════════════════════════════════════════════════════════════════════════
    //
    //                    📖 HOOKS OPCIONALES - PUNTOS DE EXTENSIÓN
    //
    //                    ⚠️ NO BORRAR POR FAVOR - DOCUMENTACIÓN ⚠️
    //
    //  Estos métodos tienen implementación vacía por defecto.
    //  Las subclases pueden sobrescribirlos para agregar funcionalidad.
    //
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * ╔════════════════════════════════════════════════════════════════════════════════╗
     * ║  📖 setupSceneSpecific() - HOOK OPCIONAL                                       ║
     * ╠════════════════════════════════════════════════════════════════════════════════╣
     * ║                                                                                ║
     * ║  Se llama DESPUÉS de cargar texturas y crear UI.                               ║
     * ║  Usa este hook para crear objetos 3D propios de la escena.                     ║
     * ║                                                                                ║
     * ║  EJEMPLO:                                                                      ║
     * ║  @Override                                                                     ║
     * ║  protected void setupSceneSpecific() {                                         ║
     * ║      link3D = new Link3D(context);                                             ║
     * ║      link3D.setPosition(-0.5f, -1.0f, 0f);                                     ║
     * ║      link3D.setScale(0.45f);                                                   ║
     * ║  }                                                                             ║
     * ║                                                                                ║
     * ╚════════════════════════════════════════════════════════════════════════════════╝
     */
    protected void setupSceneSpecific() {
        // Hook vacío - las subclases pueden sobrescribir
    }

    /**
     * ╔════════════════════════════════════════════════════════════════════════════════╗
     * ║  📖 updateSceneSpecific(deltaTime) - HOOK OPCIONAL                             ║
     * ╠════════════════════════════════════════════════════════════════════════════════╣
     * ║                                                                                ║
     * ║  Se llama cada frame DESPUÉS de actualizar el giroscopio y UI.                 ║
     * ║  Usa este hook para actualizar objetos 3D propios.                             ║
     * ║                                                                                ║
     * ║  @param deltaTime Tiempo transcurrido desde el último frame (segundos)         ║
     * ║                                                                                ║
     * ║  EJEMPLO:                                                                      ║
     * ║  @Override                                                                     ║
     * ║  protected void updateSceneSpecific(float deltaTime) {                         ║
     * ║      if (link3D != null) link3D.update(deltaTime);                             ║
     * ║  }                                                                             ║
     * ║                                                                                ║
     * ╚════════════════════════════════════════════════════════════════════════════════╝
     */
    protected void updateSceneSpecific(float deltaTime) {
        // Hook vacío - las subclases pueden sobrescribir
    }

    /**
     * ╔════════════════════════════════════════════════════════════════════════════════╗
     * ║  📖 drawSceneSpecific() - HOOK OPCIONAL                                        ║
     * ╠════════════════════════════════════════════════════════════════════════════════╣
     * ║                                                                                ║
     * ║  Se llama DESPUÉS de dibujar las capas parallax, ANTES de la UI.               ║
     * ║  Usa este hook para dibujar objetos 3D propios.                                ║
     * ║                                                                                ║
     * ║  NOTA: El depth test está DESHABILITADO cuando se llama este método.           ║
     * ║  Si dibujas objetos 3D, habilítalo temporalmente.                              ║
     * ║                                                                                ║
     * ║  EJEMPLO:                                                                      ║
     * ║  @Override                                                                     ║
     * ║  protected void drawSceneSpecific() {                                          ║
     * ║      if (link3D != null) {                                                     ║
     * ║          GLES30.glEnable(GLES30.GL_DEPTH_TEST);                                ║
     * ║          link3D.draw();                                                        ║
     * ║          GLES30.glDisable(GLES30.GL_DEPTH_TEST);                               ║
     * ║      }                                                                         ║
     * ║  }                                                                             ║
     * ║                                                                                ║
     * ╚════════════════════════════════════════════════════════════════════════════════╝
     */
    protected void drawSceneSpecific() {
        // Hook vacío - las subclases pueden sobrescribir
    }

    /**
     * ╔════════════════════════════════════════════════════════════════════════════════╗
     * ║  📖 releaseSceneSpecificResources() - HOOK OPCIONAL                            ║
     * ╠════════════════════════════════════════════════════════════════════════════════╣
     * ║                                                                                ║
     * ║  Se llama al destruir la escena.                                               ║
     * ║  Usa este hook para liberar objetos 3D y recursos propios.                     ║
     * ║                                                                                ║
     * ║  EJEMPLO:                                                                      ║
     * ║  @Override                                                                     ║
     * ║  protected void releaseSceneSpecificResources() {                              ║
     * ║      if (link3D != null) {                                                     ║
     * ║          link3D.dispose();                                                     ║
     * ║          link3D = null;                                                        ║
     * ║      }                                                                         ║
     * ║  }                                                                             ║
     * ║                                                                                ║
     * ╚════════════════════════════════════════════════════════════════════════════════╝
     */
    protected void releaseSceneSpecificResources() {
        // Hook vacío - las subclases pueden sobrescribir
    }

    /**
     * ╔════════════════════════════════════════════════════════════════════════════════╗
     * ║  📖 onPauseSceneSpecific() - HOOK OPCIONAL                                     ║
     * ╠════════════════════════════════════════════════════════════════════════════════╣
     * ║                                                                                ║
     * ║  Se llama cuando el wallpaper se oculta (pantalla apagada, otra app, etc).     ║
     * ║  Usa este hook para pausar recursos adicionales y ahorrar batería.             ║
     * ║                                                                                ║
     * ║  NOTA: El giroscopio ya se pausa automáticamente.                              ║
     * ║                                                                                ║
     * ╚════════════════════════════════════════════════════════════════════════════════╝
     */
    protected void onPauseSceneSpecific() {
        // Hook vacío - las subclases pueden sobrescribir
    }

    /**
     * ╔════════════════════════════════════════════════════════════════════════════════╗
     * ║  📖 onResumeSceneSpecific() - HOOK OPCIONAL                                    ║
     * ╠════════════════════════════════════════════════════════════════════════════════╣
     * ║                                                                                ║
     * ║  Se llama cuando el wallpaper vuelve a ser visible.                            ║
     * ║  Usa este hook para reanudar recursos pausados.                                ║
     * ║                                                                                ║
     * ║  NOTA: El giroscopio ya se reanuda automáticamente.                            ║
     * ║                                                                                ║
     * ╚════════════════════════════════════════════════════════════════════════════════╝
     */
    protected void onResumeSceneSpecific() {
        // Hook vacío - las subclases pueden sobrescribir
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    //
    //                    📖 MÉTODOS DE CONFIGURACIÓN OPCIONALES
    //
    //                    ⚠️ NO BORRAR POR FAVOR - DOCUMENTACIÓN ⚠️
    //
    //  Estos métodos retornan valores por defecto que las subclases pueden sobrescribir.
    //
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * Sensibilidad del giroscopio (default: 1.5f)
     * Valores más altos = más sensible
     */
    protected float getGyroSensitivity() {
        return DEFAULT_GYRO_SENSITIVITY;
    }

    /**
     * Factor de suavizado del giroscopio (default: 0.15f)
     * Valores más bajos = movimiento más suave
     */
    protected float getSmoothFactor() {
        return DEFAULT_SMOOTH_FACTOR;
    }

    /**
     * Máximo desplazamiento del giroscopio (default: 0.5f)
     */
    protected float getMaxOffset() {
        return DEFAULT_MAX_OFFSET;
    }

    @Override
    public boolean hasRenderableContent() {
        if (!isReady() || layers == null) return false;
        for (ParallaxLayer layer : layers) {
            if (layer.colorTextureId > 0) return true;
        }
        return false;
    }

    /**
     * Color de fondo (clear color) - RGBA
     * Por defecto: negro oscuro (safe fallback when no texture loads)
     */
    protected float[] getBackgroundColor() {
        return new float[] { 0.04f, 0.04f, 0.08f, 1.0f };
    }


    /**
     * Tema del reloj (por defecto usa el tema del ecualizador)
     */
    protected int getClockTheme() {
        return getThemeIndex(getTheme());
    }

    /**
     * Tema de la batería (por defecto usa el tema del ecualizador)
     */
    protected int getBatteryTheme() {
        return getThemeIndex(getTheme());
    }

    /**
     * Posición Y del reloj (default: 0.8f = arriba)
     */
    protected float getClockY() {
        return 0.8f;
    }

    /**
     * Posición del indicador de batería
     */
    protected float getBatteryX() { return 0.81f; }
    protected float getBatteryY() { return -0.34f; }

    // ═══════════════════════════════════════════════════════════════════════════════════
    //
    //                    📖 IMPLEMENTACIÓN DEL CICLO DE VIDA
    //
    //                    ⚠️ NO BORRAR POR FAVOR - CÓDIGO CORE ⚠️
    //
    //  Este código maneja todo el ciclo de vida de la escena.
    //  NO modificar a menos que sepas lo que haces.
    //
    // ═══════════════════════════════════════════════════════════════════════════════════

    @Override
    protected void setupScene() {
        Log.d(TAG, "╔══════════════════════════════════════════════════════════════╗");
        Log.d(TAG, "║        🖼️ INICIANDO " + getName());
        Log.d(TAG, "╚══════════════════════════════════════════════════════════════╝");

        // ═══════════════════════════════════════════════════════════════════════
        // PASO 1: Inicializar giroscopio
        // ═══════════════════════════════════════════════════════════════════════
        try {
            gyroscope = new GyroscopeManager(context);
            gyroscope.setSensitivity(getGyroSensitivity());
            gyroscope.start();
            Log.d(TAG, "✅ Giroscopio iniciado (sensibilidad: " + getGyroSensitivity() + ")");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error creando giroscopio: " + e.getMessage());
        }

        // ═══════════════════════════════════════════════════════════════════════
        // PASO 2: Crear shaders
        // ═══════════════════════════════════════════════════════════════════════
        createDepthShader();
        createSimpleShader();

        // ═══════════════════════════════════════════════════════════════════════
        // PASO 3: Crear buffers del quad
        // ═══════════════════════════════════════════════════════════════════════
        createQuadBuffers();

        // ═══════════════════════════════════════════════════════════════════════
        // PASO 4: Obtener y cargar capas
        // ═══════════════════════════════════════════════════════════════════════
        layers = getLayers();
        if (layers != null) {
            loadLayerTextures();
        }

        // ═══════════════════════════════════════════════════════════════════════
        // PASO 5: Crear UI
        // ═══════════════════════════════════════════════════════════════════════
        setupUI();

        // ═══════════════════════════════════════════════════════════════════════
        // PASO 6: Hook para subclases
        // ═══════════════════════════════════════════════════════════════════════
        setupSceneSpecific();

        Log.d(TAG, "✅ " + getName() + " lista!");
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * ⚠️ NO BORRAR POR FAVOR - SHADER DE DEPTH DISPLACEMENT
     * ═══════════════════════════════════════════════════════════════════════════
     *
     * Este shader es el CORAZÓN del efecto parallax 3D.
     * Lee el depth map y desplaza los píxeles según su profundidad.
     */
    private void createDepthShader() {
        String vertexShader =
            "attribute vec4 a_Position;\n" +
            "attribute vec2 a_TexCoord;\n" +
            "varying vec2 v_TexCoord;\n" +
            "void main() {\n" +
            "    gl_Position = a_Position;\n" +
            "    v_TexCoord = a_TexCoord;\n" +
            "}\n";

        // ⚠️ NO BORRAR - Este fragment shader implementa el depth displacement
        String fragmentShader =
            "precision mediump float;\n" +
            "uniform sampler2D u_Texture;\n" +      // Imagen de color
            "uniform sampler2D u_DepthMap;\n" +     // Depth map grayscale
            "uniform vec2 u_Offset;\n" +            // Offset del giroscopio
            "uniform float u_DepthScale;\n" +       // Escala del efecto
            "uniform float u_Alpha;\n" +
            "varying vec2 v_TexCoord;\n" +
            "\n" +
            "void main() {\n" +
            "    // Leer profundidad (blanco=1=cerca, negro=0=lejos)\n" +
            "    float depth = texture2D(u_DepthMap, v_TexCoord).r;\n" +
            "    \n" +
            "    // Calcular desplazamiento basado en profundidad\n" +
            "    // Objetos cercanos (depth alto) se mueven más\n" +
            "    vec2 displacement = u_Offset * depth * u_DepthScale;\n" +
            "    \n" +
            "    // Aplicar desplazamiento a las coordenadas UV\n" +
            "    vec2 displacedUV = v_TexCoord + displacement;\n" +
            "    \n" +
            "    // Clamp para evitar artefactos en los bordes\n" +
            "    displacedUV = clamp(displacedUV, 0.005, 0.995);\n" +
            "    \n" +
            "    // Samplear color con UV desplazado\n" +
            "    vec4 color = texture2D(u_Texture, displacedUV);\n" +
            "    \n" +
            "    gl_FragColor = vec4(color.rgb, color.a * u_Alpha);\n" +
            "}\n";

        depthShaderProgram = createProgram(vertexShader, fragmentShader);

        depthPosLoc = GLES30.glGetAttribLocation(depthShaderProgram, "a_Position");
        depthTexCoordLoc = GLES30.glGetAttribLocation(depthShaderProgram, "a_TexCoord");
        depthTextureLoc = GLES30.glGetUniformLocation(depthShaderProgram, "u_Texture");
        depthMapLoc = GLES30.glGetUniformLocation(depthShaderProgram, "u_DepthMap");
        depthOffsetLoc = GLES30.glGetUniformLocation(depthShaderProgram, "u_Offset");
        depthScaleLoc = GLES30.glGetUniformLocation(depthShaderProgram, "u_DepthScale");
        depthAlphaLoc = GLES30.glGetUniformLocation(depthShaderProgram, "u_Alpha");

        Log.d(TAG, "✅ Depth Shader creado");
    }

    /**
     * ⚠️ NO BORRAR POR FAVOR - SHADER SIMPLE
     * Para capas sin depth map (estáticas o parallax simple)
     */
    private void createSimpleShader() {
        String vertexShader =
            "attribute vec4 a_Position;\n" +
            "attribute vec2 a_TexCoord;\n" +
            "uniform vec2 u_Offset;\n" +
            "varying vec2 v_TexCoord;\n" +
            "void main() {\n" +
            "    gl_Position = a_Position + vec4(u_Offset, 0.0, 0.0);\n" +
            "    v_TexCoord = a_TexCoord;\n" +
            "}\n";

        String fragmentShader =
            "precision mediump float;\n" +
            "uniform sampler2D u_Texture;\n" +
            "uniform float u_Alpha;\n" +
            "varying vec2 v_TexCoord;\n" +
            "void main() {\n" +
            "    vec4 color = texture2D(u_Texture, v_TexCoord);\n" +
            "    gl_FragColor = vec4(color.rgb, color.a * u_Alpha);\n" +
            "}\n";

        simpleShaderProgram = createProgram(vertexShader, fragmentShader);

        simplePosLoc = GLES30.glGetAttribLocation(simpleShaderProgram, "a_Position");
        simpleTexCoordLoc = GLES30.glGetAttribLocation(simpleShaderProgram, "a_TexCoord");
        simpleTextureLoc = GLES30.glGetUniformLocation(simpleShaderProgram, "u_Texture");
        simpleOffsetLoc = GLES30.glGetUniformLocation(simpleShaderProgram, "u_Offset");
        simpleAlphaLoc = GLES30.glGetUniformLocation(simpleShaderProgram, "u_Alpha");

        Log.d(TAG, "✅ Simple Shader creado");
    }

    /**
     * ⚠️ NO BORRAR POR FAVOR - BUFFERS DEL QUAD
     */
    private void createQuadBuffers() {
        float[] vertices = {
            -1f, -1f,  // Bottom-left
             1f, -1f,  // Bottom-right
            -1f,  1f,  // Top-left
             1f,  1f   // Top-right
        };

        float[] texCoords = {
            0f, 1f,  // Bottom-left
            1f, 1f,  // Bottom-right
            0f, 0f,  // Top-left
            1f, 0f   // Top-right
        };

        quadVertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        quadVertexBuffer.put(vertices).position(0);

        quadTexCoordBuffer = ByteBuffer.allocateDirect(texCoords.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        quadTexCoordBuffer.put(texCoords).position(0);

        quadTexCoordBufferCover = ByteBuffer.allocateDirect(texCoords.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        quadTexCoordBufferCover.put(texCoords).position(0);

        Log.d(TAG, "✅ Quad buffers creados");
    }

    /**
     * ⚠️ NO BORRAR POR FAVOR - CARGA DE TEXTURAS DE CAPAS
     */
    private void loadLayerTextures() {
        ImageDownloadManager imageManager = ImageDownloadManager.getInstance(context);

        Log.d(TAG, "╔══════════════════════════════════════════════════════════════╗");
        Log.d(TAG, "║              📥 CARGANDO TEXTURAS DE CAPAS                   ║");
        Log.d(TAG, "╚══════════════════════════════════════════════════════════════╝");

        for (int i = 0; i < layers.length; i++) {
            ParallaxLayer layer = layers[i];

            // Cargar textura de color
            downloadIfNeeded(imageManager, layer.colorFile, "Capa " + i + " Color");
            String colorPath = imageManager.getImagePath(layer.colorFile);
            layer.colorTextureId = loadTextureFromFile(colorPath, "Capa " + i + " Color");

            // Store image dimensions for cover mode aspect ratio
            if (colorPath != null && layer.useCoverMode) {
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(colorPath, bounds);
                layer.imageWidth = bounds.outWidth;
                layer.imageHeight = bounds.outHeight;
            }

            // Cargar depth map si existe
            if (layer.depthFile != null) {
                downloadIfNeeded(imageManager, layer.depthFile, "Capa " + i + " Depth");
                layer.depthTextureId = loadTextureFromFile(
                    imageManager.getImagePath(layer.depthFile),
                    "Capa " + i + " Depth"
                );
            }

            Log.d(TAG, "║ Capa " + i + ": Color=" + layer.colorTextureId +
                      (layer.hasDepthMap() ? " Depth=" + layer.depthTextureId + " ✅ PARALLAX" : " (estática)") +
                      (layer.imageWidth > 0 ? " [" + layer.imageWidth + "x" + layer.imageHeight + "]" : ""));
        }

        // Recalculate cover UV coords now that we know actual image dimensions
        updateQuadForAspectRatio();

        Log.d(TAG, "╚══════════════════════════════════════════════════════════════╝");
    }

    /**
     * Descarga imagen si no existe localmente
     */
    private void downloadIfNeeded(ImageDownloadManager manager, String fileName, String displayName) {
        if (manager.getImagePath(fileName) == null) {
            Log.d(TAG, "📥 Descargando: " + displayName + " (" + fileName + ")");
            boolean success = manager.downloadImageSync(fileName, percent -> {
                Log.d(TAG, "📥 " + displayName + ": " + percent + "%");
            });
            if (success) {
                Log.d(TAG, "✅ " + displayName + " descargado");
            } else {
                Log.e(TAG, "❌ Error descargando: " + displayName);
            }
        }
    }

    /**
     * Carga textura desde archivo
     */
    private int loadTextureFromFile(String path, String name) {
        if (path == null) {
            Log.w(TAG, "⚠️ Ruta nula para " + name);
            return -1;
        }

        File file = new File(path);
        if (!file.exists() || !file.canRead()) {
            Log.w(TAG, "⚠️ Archivo no disponible: " + name);
            return -1;
        }

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            // 🔧 FIX MEMORY: RGB_565 = 50% less GPU per opaque texture.
            // Decoder auto-upgrades to ARGB_8888 if image has alpha channel.
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bitmap = BitmapFactory.decodeFile(path, options);

            if (bitmap == null) {
                Log.e(TAG, "❌ No se pudo decodificar: " + name);
                return -1;
            }

            int[] textureIds = new int[1];
            GLES30.glGenTextures(1, textureIds, 0);
            int textureId = textureIds[0];

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();

            Log.d(TAG, "✅ " + name + " (ID:" + textureId + ")");
            return textureId;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error cargando " + name + ": " + e.getMessage());
            return -1;
        }
    }

    /**
     * ⚠️ NO BORRAR POR FAVOR - CONFIGURACIÓN DE UI
     */
    private void setupUI() {
        EqualizerBarsDJ.Theme theme = getTheme();

        // 🎵 Ecualizador
        try {
            equalizerDJ = new EqualizerBarsDJ();
            equalizerDJ.initialize();
            equalizerDJ.setTheme(theme);
            equalizerDJ.setScreenSize(screenWidth, screenHeight);
            Log.d(TAG, "✅ Ecualizador " + theme.name() + " activado");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error EqualizerBarsDJ: " + e.getMessage());
        }

        // ⏰ Reloj
        try {
            clock = new Clock3D(context, getClockTheme(), 0f, getClockY());
            clock.setShowMilliseconds(false);
            Log.d(TAG, "✅ Reloj activado");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error Clock3D: " + e.getMessage());
        }

        // 🔋 Batería
        try {
            battery = new Battery3D(context, getBatteryTheme(), getBatteryX(), getBatteryY());
            Log.d(TAG, "✅ Batería activada");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error Battery3D: " + e.getMessage());
        }
    }

    /**
     * Convierte tema de ecualizador a índice para Clock3D/Battery3D
     */
    private int getThemeIndex(EqualizerBarsDJ.Theme theme) {
        switch (theme) {
            case ABYSSIA: return Clock3D.THEME_ABYSSIA;
            case PYRALIS: return Clock3D.THEME_PYRALIS;
            case KAMEHAMEHA: return Clock3D.THEME_KAMEHAMEHA;
            case SYNTHWAVE: return Clock3D.THEME_SYNTHWAVE;
            case COSMOS: return Clock3D.THEME_COSMOS;
            case WALKING_DEAD: return Clock3D.THEME_WALKING_DEAD;
            case ZELDA: return Clock3D.THEME_ZELDA;
            default: return Clock3D.THEME_ABYSSIA;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // ⚠️ NO BORRAR POR FAVOR - UPDATE
    // ═══════════════════════════════════════════════════════════════════════════════════

    @Override
    public void update(float deltaTime) {
        if (isPaused || isDisposed) return;

        // Obtener y suavizar valores del giroscopio
        if (gyroscope != null) {
            float maxOffset = getMaxOffset();
            float smoothFactor = getSmoothFactor();

            float targetX = gyroscope.getTiltX() * maxOffset;
            float targetY = gyroscope.getTiltY() * maxOffset;

            offsetX = offsetX + (targetX - offsetX) * smoothFactor;
            offsetY = offsetY + (targetY - offsetY) * smoothFactor;
        }

        // Actualizar UI
        if (equalizerDJ != null) equalizerDJ.update(deltaTime);
        if (clock != null) clock.update(deltaTime);
        if (battery != null) battery.update(deltaTime);

        // Hook para subclases
        updateSceneSpecific(deltaTime);

        super.update(deltaTime);
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // ⚠️ NO BORRAR POR FAVOR - DRAW
    // ═══════════════════════════════════════════════════════════════════════════════════

    @Override
    public void draw() {
        if (isDisposed) return;

        // Clear con color de fondo
        float[] bg = getBackgroundColor();
        GLES30.glClearColor(bg[0], bg[1], bg[2], bg[3]);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        // Configurar para 2D
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Dibujar capas
        if (layers != null) {
            for (ParallaxLayer layer : layers) {
                if (layer.colorTextureId > 0) {
                    drawLayer(layer);
                }
            }
        }


        // Hook para objetos 3D
        drawSceneSpecific();

        // Dibujar UI
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        if (equalizerDJ != null) equalizerDJ.draw();
        if (clock != null) clock.draw();
        if (battery != null) battery.draw();

        super.draw();
    }

    /**
     * ⚠️ NO BORRAR POR FAVOR - DIBUJA CAPA CON DEPTH MAP
     */
    /**
     * Draws a single layer. Override in subclasses for custom shader effects.
     */
    protected void drawLayer(ParallaxLayer layer) {
        if (layer.hasDepthMap()) {
            drawLayerWithDepth(layer);
        } else {
            drawLayerStatic(layer);
        }
    }

    private void drawLayerWithDepth(ParallaxLayer layer) {
        GLES30.glUseProgram(depthShaderProgram);

        GLES30.glUniform2f(depthOffsetLoc, offsetX, offsetY);
        GLES30.glUniform1f(depthScaleLoc, layer.depthScale);
        GLES30.glUniform1f(depthAlphaLoc, layer.alpha);

        // Texture unit 0: Color
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, layer.colorTextureId);
        GLES30.glUniform1i(depthTextureLoc, 0);

        // Texture unit 1: Depth map
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, layer.depthTextureId);
        GLES30.glUniform1i(depthMapLoc, 1);

        // Vertices
        GLES30.glEnableVertexAttribArray(depthPosLoc);
        GLES30.glVertexAttribPointer(depthPosLoc, 2, GLES30.GL_FLOAT, false, 0, quadVertexBuffer);

        // UV coords
        FloatBuffer uvBuffer = layer.useCoverMode ? quadTexCoordBufferCover : quadTexCoordBuffer;
        GLES30.glEnableVertexAttribArray(depthTexCoordLoc);
        GLES30.glVertexAttribPointer(depthTexCoordLoc, 2, GLES30.GL_FLOAT, false, 0, uvBuffer);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(depthPosLoc);
        GLES30.glDisableVertexAttribArray(depthTexCoordLoc);
    }

    /**
     * ⚠️ NO BORRAR POR FAVOR - DIBUJA CAPA ESTÁTICA
     */
    private void drawLayerStatic(ParallaxLayer layer) {
        GLES30.glUseProgram(simpleShaderProgram);

        GLES30.glUniform2f(simpleOffsetLoc, offsetX * layer.depthScale, offsetY * layer.depthScale);
        GLES30.glUniform1f(simpleAlphaLoc, layer.alpha);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, layer.colorTextureId);
        GLES30.glUniform1i(simpleTextureLoc, 0);

        GLES30.glEnableVertexAttribArray(simplePosLoc);
        GLES30.glVertexAttribPointer(simplePosLoc, 2, GLES30.GL_FLOAT, false, 0, quadVertexBuffer);

        // Use cover mode UV coords when layer requests it (fills entire screen)
        FloatBuffer uvBuffer = layer.useCoverMode ? quadTexCoordBufferCover : quadTexCoordBuffer;
        GLES30.glEnableVertexAttribArray(simpleTexCoordLoc);
        GLES30.glVertexAttribPointer(simpleTexCoordLoc, 2, GLES30.GL_FLOAT, false, 0, uvBuffer);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(simplePosLoc);
        GLES30.glDisableVertexAttribArray(simpleTexCoordLoc);
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // ⚠️ NO BORRAR POR FAVOR - CICLO DE VIDA
    // ═══════════════════════════════════════════════════════════════════════════════════

    @Override
    public void onPause() {
        super.onPause();
        if (gyroscope != null) {
            gyroscope.stop();
            Log.d(TAG, "⏸️ Giroscopio pausado");
        }
        onPauseSceneSpecific();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (gyroscope != null) {
            gyroscope.start();
            Log.d(TAG, "▶️ Giroscopio reanudado");
        }
        onResumeSceneSpecific();
    }

    @Override
    protected void releaseSceneResources() {
        Log.d(TAG, "🗑️ Liberando recursos " + getName() + "...");

        // Liberar texturas de capas
        if (layers != null) {
            for (ParallaxLayer layer : layers) {
                if (layer.colorTextureId > 0) {
                    GLES30.glDeleteTextures(1, new int[]{layer.colorTextureId}, 0);
                }
                if (layer.depthTextureId > 0) {
                    GLES30.glDeleteTextures(1, new int[]{layer.depthTextureId}, 0);
                }
            }
            layers = null;
        }

        // Liberar shaders
        if (depthShaderProgram > 0) {
            GLES30.glDeleteProgram(depthShaderProgram);
            depthShaderProgram = -1;
        }
        if (simpleShaderProgram > 0) {
            GLES30.glDeleteProgram(simpleShaderProgram);
            simpleShaderProgram = -1;
        }

        // Liberar giroscopio
        if (gyroscope != null) {
            gyroscope.stop();
            gyroscope = null;
        }

        // Liberar UI
        if (equalizerDJ != null) {
            equalizerDJ.release();
            equalizerDJ = null;
        }
        if (clock != null) {
            clock.dispose();
            clock = null;
        }
        if (battery != null) {
            battery.dispose();
            battery = null;
        }

        // Hook para subclases
        releaseSceneSpecificResources();

        Log.d(TAG, "✅ Recursos " + getName() + " liberados");
    }

    @Override
    public void setScreenSize(int width, int height) {
        super.setScreenSize(width, height);
        if (equalizerDJ != null) equalizerDJ.setScreenSize(width, height);
        updateQuadForAspectRatio();
    }

    /**
     * Actualiza UV coords para aspect ratio (cover mode)
     */
    private void updateQuadForAspectRatio() {
        if (screenWidth <= 0 || screenHeight <= 0 || quadTexCoordBufferCover == null) return;

        // Use actual image aspect from first cover-mode layer, fallback to 9:16 portrait
        float imageAspect = 9f / 16f;  // Default portrait
        if (layers != null) {
            for (ParallaxLayer layer : layers) {
                if (layer.useCoverMode && layer.imageWidth > 0 && layer.imageHeight > 0) {
                    imageAspect = (float) layer.imageWidth / layer.imageHeight;
                    break;
                }
            }
        }

        float screenAspect = (float) screenWidth / screenHeight;

        float uMin = 0f, uMax = 1f;
        float vMin = 0f, vMax = 1f;

        // True cover mode: always fill the screen completely (crop excess)
        if (screenAspect > imageAspect) {
            // Screen is wider than image → crop top/bottom
            float visibleHeight = imageAspect / screenAspect;
            float crop = (1f - visibleHeight) / 2f;
            vMin = crop;
            vMax = 1f - crop;
        } else if (screenAspect < imageAspect) {
            // Screen is taller than image → crop left/right
            float visibleWidth = screenAspect / imageAspect;
            float crop = (1f - visibleWidth) / 2f;
            uMin = crop;
            uMax = 1f - crop;
        }

        float[] texCoords = {
            uMin, vMax,
            uMax, vMax,
            uMin, vMin,
            uMax, vMin
        };

        quadTexCoordBufferCover.clear();
        quadTexCoordBufferCover.put(texCoords).position(0);
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // ADAPTIVE MEMORY SYSTEM
    // ═══════════════════════════════════════════════════════════════════════════════════

    @Override
    public void onMemoryPressure(MemoryPressureLevel level) {
        super.onMemoryPressure(level);
        Log.d(TAG, getName() + " memoria: " + level);

        switch (level) {
            case WARNING:
                if (equalizerDJ != null) equalizerDJ.setReducedMode(true);
                if (gyroscope != null) gyroscope.stop();
                if (clock != null) clock.setVisible(false);
                if (battery != null) battery.setVisible(false);
                break;
            case CRITICAL:
                if (equalizerDJ != null) {
                    equalizerDJ.setReducedMode(true);
                    equalizerDJ.setEnabled(false);
                }
                if (gyroscope != null) gyroscope.stop();
                if (clock != null) clock.setVisible(false);
                if (battery != null) battery.setVisible(false);
                break;
            case NORMAL:
            default:
                if (equalizerDJ != null) {
                    equalizerDJ.setEnabled(true);
                    equalizerDJ.setReducedMode(false);
                }
                if (gyroscope != null && !isPaused) gyroscope.start();
                if (clock != null) clock.setVisible(true);
                if (battery != null) battery.setVisible(true);
                break;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // ⚠️ NO BORRAR POR FAVOR - MÚSICA
    // ═══════════════════════════════════════════════════════════════════════════════════

    public void updateMusicBands(float[] bands) {
        if (equalizerDJ != null && bands != null && bands.length > 0) {
            equalizerDJ.updateFromBands(bands);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // ⚠️ NO BORRAR POR FAVOR - UTILIDADES SHADER
    // ═══════════════════════════════════════════════════════════════════════════════════

    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource);

        int program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragmentShader);
        GLES30.glLinkProgram(program);

        return program;
    }

    private int loadShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);
        return shader;
    }
}
