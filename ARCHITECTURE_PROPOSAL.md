# Propuesta de Arquitectura Escalable - Orbix iA

## Estado Actual

### Lo que funciona bien:
- Sistema de estados: `PANEL_MODE` / `LOADING_MODE` / `WALLPAPER_MODE`
- Play/Stop funciona correctamente
- Anti-flickering implementado
- Panel de control ligero

### Problemas para escalar:
- `SceneRenderer.java` tiene **2400+ lineas** - hace TODO
- No hay separacion entre Panel de Control y Escenas 3D
- Agregar un nuevo wallpaper requiere modificar SceneRenderer
- No hay liberacion de recursos entre wallpapers

---

## Propuesta: Arquitectura por Capas

```
┌─────────────────────────────────────────────────────────────┐
│                    LiveWallpaperService                      │
│                  (Ciclo de vida Android)                     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     SceneRenderer                            │
│            (Coordinador - Decide que dibujar)                │
│                                                              │
│   ┌──────────────┐    ┌──────────────┐                      │
│   │  PANEL_MODE  │    │WALLPAPER_MODE│                      │
│   │              │    │              │                      │
│   │ PanelScene   │    │ WallpaperScene                      │
│   │ (Control)    │    │ (Abstracta)  │                      │
│   └──────────────┘    └──────────────┘                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                 WallpaperScene (Abstracta)                   │
│                                                              │
│   + onCreate(Context, TextureManager)                        │
│   + onResume()                                               │
│   + onPause()                                                │
│   + onDestroy()  ← LIBERA TODOS LOS RECURSOS                │
│   + update(deltaTime)                                        │
│   + draw(CameraController)                                   │
│   + getSceneObjects(): List<SceneObject>                     │
│   + getName(): String                                        │
└─────────────────────────────────────────────────────────────┘
          ▲                    ▲                    ▲
          │                    │                    │
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ UniverseScene   │  │ BlackHoleScene  │  │ FutureScene3    │
│                 │  │                 │  │                 │
│ - Tierra        │  │ - Agujero negro │  │ - ???           │
│ - Sol           │  │ - Disco         │  │                 │
│ - OVNI          │  │ - Particulas    │  │                 │
│ - Meteoritos    │  │                 │  │                 │
│ - Ecualizador   │  │                 │  │                 │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

---

## Clase Base: WallpaperScene

```java
/**
 * Clase base abstracta para todas las escenas de wallpaper.
 * Cada wallpaper nuevo extiende esta clase.
 */
public abstract class WallpaperScene {

    protected Context context;
    protected TextureManager textureManager;
    protected CameraController camera;
    protected List<SceneObject> sceneObjects = new ArrayList<>();
    protected boolean isLoaded = false;
    protected boolean isPaused = true;

    // ══════════════════════════════════════════════════════════
    // CICLO DE VIDA - Obligatorio implementar
    // ══════════════════════════════════════════════════════════

    /**
     * Carga todos los recursos de la escena.
     * LLAMAR EN BACKGROUND THREAD si es pesado.
     */
    public abstract void onCreate(Context context, TextureManager textureManager);

    /**
     * Libera TODOS los recursos (texturas, buffers, shaders).
     * MUY IMPORTANTE para evitar memory leaks.
     */
    public abstract void onDestroy();

    /**
     * Nombre unico de la escena (para identificacion).
     */
    public abstract String getName();

    /**
     * Icono/preview de la escena (resource ID).
     */
    public abstract int getPreviewResourceId();

    // ══════════════════════════════════════════════════════════
    // METODOS COMUNES - Ya implementados
    // ══════════════════════════════════════════════════════════

    public void onResume() {
        isPaused = false;
        for (SceneObject obj : sceneObjects) {
            // Reanudar animaciones si aplica
        }
    }

    public void onPause() {
        isPaused = true;
    }

    public void update(float deltaTime) {
        if (isPaused || !isLoaded) return;
        for (SceneObject obj : sceneObjects) {
            obj.update(deltaTime);
        }
    }

    public void draw() {
        if (!isLoaded) return;
        for (SceneObject obj : sceneObjects) {
            obj.draw();
        }
    }

    public void setCameraController(CameraController camera) {
        this.camera = camera;
        for (SceneObject obj : sceneObjects) {
            if (obj instanceof CameraAware) {
                ((CameraAware) obj).setCameraController(camera);
            }
        }
    }

    public boolean isLoaded() {
        return isLoaded;
    }
}
```

---

## Ejemplo: UniverseScene

```java
public class UniverseScene extends WallpaperScene {

    private static final String TAG = "UniverseScene";

    // Objetos especificos de esta escena
    private Planeta tierra;
    private SolProcedural sol;
    private Spaceship3D ovni;
    private MeteorShower meteorShower;
    private MusicIndicator musicIndicator;
    // ... etc

    @Override
    public String getName() {
        return "Universo";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.universo03;
    }

    @Override
    public void onCreate(Context context, TextureManager textureManager) {
        this.context = context;
        this.textureManager = textureManager;

        Log.d(TAG, "Cargando escena Universo...");

        // Crear todos los objetos de la escena
        tierra = new Planeta(context, textureManager, ...);
        sol = new SolProcedural(context);
        ovni = new Spaceship3D(context, textureManager);
        meteorShower = new MeteorShower(context, textureManager);
        musicIndicator = new MusicIndicator(context, ...);

        // Agregar a la lista comun
        sceneObjects.add(tierra);
        sceneObjects.add(sol);
        sceneObjects.add(ovni);
        sceneObjects.add(meteorShower);
        sceneObjects.add(musicIndicator);

        isLoaded = true;
        Log.d(TAG, "Escena Universo cargada!");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Liberando recursos de Universo...");

        // Liberar cada objeto
        // tierra.dispose();
        // sol.dispose();
        // etc...

        sceneObjects.clear();
        isLoaded = false;

        Log.d(TAG, "Recursos liberados!");
    }
}
```

---

## SceneRenderer Simplificado

```java
public class SceneRenderer implements GLSurfaceView.Renderer {

    // ══════════════════════════════════════════════════════════
    // ESCENAS DISPONIBLES
    // ══════════════════════════════════════════════════════════
    private Map<String, Class<? extends WallpaperScene>> availableScenes = new HashMap<>();
    private WallpaperScene currentScene = null;
    private String currentSceneName = null;

    // Panel de control (siempre cargado, super ligero)
    private PanelControlScene panelScene;

    public SceneRenderer(Context context) {
        // Registrar escenas disponibles
        availableScenes.put("Universo", UniverseScene.class);
        availableScenes.put("Agujero Negro", BlackHoleScene.class);
        // availableScenes.put("Futuro", FutureScene.class);

        // Panel de control siempre listo
        panelScene = new PanelControlScene();
        panelScene.onCreate(context, textureManager);
    }

    /**
     * Cambiar a una escena diferente.
     * LIBERA la escena anterior antes de cargar la nueva.
     */
    public void loadScene(String sceneName) {
        // 1. Liberar escena actual
        if (currentScene != null) {
            currentScene.onDestroy();
            currentScene = null;
        }

        // 2. Crear nueva escena
        Class<? extends WallpaperScene> sceneClass = availableScenes.get(sceneName);
        if (sceneClass != null) {
            try {
                currentScene = sceneClass.newInstance();
                currentScene.onCreate(context, textureManager);
                currentScene.setCameraController(camera);
                currentSceneName = sceneName;
            } catch (Exception e) {
                Log.e(TAG, "Error cargando escena: " + sceneName, e);
            }
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Limpiar pantalla
        GLES20.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if (currentRenderMode == RenderMode.PANEL_MODE) {
            // Solo dibujar panel de control (SUPER LIGERO)
            panelScene.draw();
        } else if (currentRenderMode == RenderMode.WALLPAPER_MODE) {
            // Dibujar escena 3D actual
            if (currentScene != null && currentScene.isLoaded()) {
                currentScene.update(deltaTime);
                currentScene.draw();
            }
        }
    }

    public void switchToWallpaperMode() {
        // Cargar la escena seleccionada si no esta cargada
        if (currentScene == null) {
            loadScene(selectedWallpaperName);
        }
        currentRenderMode = RenderMode.WALLPAPER_MODE;
    }

    public void switchToPanelMode() {
        // IMPORTANTE: Liberar escena 3D para ahorrar memoria
        if (currentScene != null) {
            currentScene.onDestroy();
            currentScene = null;
        }
        currentRenderMode = RenderMode.PANEL_MODE;
    }
}
```

---

## Panel de Control Ligero

El panel de control debe ser **SUPER RAPIDO**. Propuesta:

```java
public class PanelControlScene extends WallpaperScene {

    // Solo elementos 2D, sin escena 3D
    private PlayPauseButton playButton;
    private GreetingText greeting;
    private CircularLoadingRing loadingRing;
    private LifeClockDisplay lifeClock;

    // Background: Color solido o gradiente simple (NO textura pesada)
    private int backgroundColor = 0xFF000000;  // Negro puro

    @Override
    public void onCreate(Context context, TextureManager tm) {
        // Elementos 2D minimos
        playButton = new PlayPauseButton(context);
        greeting = new GreetingText(context);
        loadingRing = new CircularLoadingRing(context);
        lifeClock = new LifeClockDisplay(context);

        sceneObjects.add(playButton);
        sceneObjects.add(greeting);
        sceneObjects.add(loadingRing);
        sceneObjects.add(lifeClock);

        isLoaded = true;
    }

    @Override
    public void draw() {
        // Fondo negro solido (1 llamada GL)
        GLES20.glClearColor(0f, 0f, 0f, 1f);

        // Solo dibujar elementos 2D
        super.draw();
    }
}
```

---

## Sistema de Liberacion de Recursos

### Interfaz Disposable

```java
public interface Disposable {
    /**
     * Libera todos los recursos OpenGL (texturas, buffers, shaders).
     * DEBE llamarse desde el GL thread.
     */
    void dispose();

    /**
     * @return true si los recursos ya fueron liberados
     */
    boolean isDisposed();
}
```

### Implementacion en SceneObject

```java
public abstract class BaseSceneObject implements SceneObject, Disposable {

    protected boolean disposed = false;
    protected int[] textureIds = new int[0];
    protected int programId = 0;

    @Override
    public void dispose() {
        if (disposed) return;

        // Liberar texturas
        if (textureIds.length > 0) {
            GLES20.glDeleteTextures(textureIds.length, textureIds, 0);
        }

        // Liberar shader program
        if (programId != 0) {
            GLES20.glDeleteProgram(programId);
            programId = 0;
        }

        disposed = true;
        Log.d(getClass().getSimpleName(), "Recursos liberados");
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
```

---

## Medicion de FPS

### Agregar al Panel de Control

```java
public class FPSCounter {
    private long lastTime = System.nanoTime();
    private int frameCount = 0;
    private float currentFPS = 0f;

    public void tick() {
        frameCount++;
        long now = System.nanoTime();
        long elapsed = now - lastTime;

        // Actualizar cada segundo
        if (elapsed >= 1_000_000_000L) {
            currentFPS = frameCount * 1_000_000_000f / elapsed;
            frameCount = 0;
            lastTime = now;
        }
    }

    public float getFPS() {
        return currentFPS;
    }

    public String getFPSString() {
        return String.format("%.1f FPS", currentFPS);
    }
}
```

---

## Plan de Implementacion

### Fase 1: Refactoring Base (1-2 dias)
1. Crear `WallpaperScene` abstracta
2. Crear `UniverseScene` extrayendo codigo de SceneRenderer
3. Crear `PanelControlScene` ligero
4. Probar que funcione igual que antes

### Fase 2: Sistema de Recursos (1 dia)
1. Implementar `Disposable` en todos los SceneObjects
2. Agregar liberacion de recursos en `onDestroy()`
3. Verificar que no hay memory leaks

### Fase 3: Multi-Wallpaper (1 dia)
1. Crear `BlackHoleScene` como segundo wallpaper
2. Probar cambio entre escenas
3. Verificar liberacion de recursos al cambiar

### Fase 4: Optimizacion Panel (1 dia)
1. Agregar FPSCounter
2. Optimizar Panel de Control para 60 FPS
3. Medir consumo de bateria

---

## Beneficios

| Antes | Despues |
|-------|---------|
| 2400+ lineas en SceneRenderer | ~500 lineas + escenas separadas |
| Agregar wallpaper = modificar SceneRenderer | Agregar wallpaper = crear nueva clase |
| Sin liberacion de recursos | Dispose() automatico al cambiar |
| Panel y escena mezclados | Panel ligero separado |
| Dificil de testear | Cada escena testeable independiente |

---

## Preguntas para Eduardo

1. Quieres que empiece con esta refactorizacion?
2. Prefieres implementar primero el panel ligero o la arquitectura completa?
3. Hay algun wallpaper especifico que quieras agregar despues del Universo?

---

*Documento creado por Claude - Noviembre 29, 2024*
