package com.secret.blackholeglow.scenes;

import android.content.Context;
import android.util.Log;

import com.secret.blackholeglow.CameraAware;
import com.secret.blackholeglow.CameraController;
import com.secret.blackholeglow.SceneObject;
import com.secret.blackholeglow.TextureManager;
import com.secret.blackholeglow.systems.EventBus;
import com.secret.blackholeglow.systems.ResourceManager;

import java.util.ArrayList;
import java.util.List;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   🎬 WallpaperScene - Clase Base Abstracta para Escenas          ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * Cada wallpaper animado extiende esta clase.
 *
 * ARQUITECTURA:
 * ┌─────────────────────────────────────────────────────────────────┐
 * │                      WallpaperScene                             │
 * │                        (Abstracta)                              │
 * └─────────────────────────────────────────────────────────────────┘
 *          ▲                    ▲                    ▲
 *          │                    │                    │
 * ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
 * │ UniverseScene   │  │ OceanPearlScene │  │ FutureScene     │
 * │ (Universo)      │  │ (Mar + Perla)   │  │ (Proximos)      │
 * └─────────────────┘  └─────────────────┘  └─────────────────┘
 *
 * CICLO DE VIDA:
 * 1. onCreate()  - Cargar recursos (texturas, modelos, shaders)
 * 2. onResume()  - Reanudar animaciones
 * 3. update()    - Actualizar logica cada frame
 * 4. draw()      - Dibujar la escena
 * 5. onPause()   - Pausar animaciones
 * 6. onDestroy() - LIBERAR TODOS LOS RECURSOS (muy importante!)
 */
public abstract class WallpaperScene implements Disposable {

    private static final String TAG = "WallpaperScene";

    // ═══════════════════════════════════════════════════════════════
    // RECURSOS COMPARTIDOS
    // ═══════════════════════════════════════════════════════════════
    protected Context context;
    protected TextureManager textureManager;
    protected CameraController camera;
    protected ResourceManager resourceManager;  // Nuevo sistema de recursos

    // ═══════════════════════════════════════════════════════════════
    // OBJETOS DE LA ESCENA
    // ═══════════════════════════════════════════════════════════════
    protected List<SceneObject> sceneObjects = new ArrayList<>();

    // ═══════════════════════════════════════════════════════════════
    // ESTADO
    // ═══════════════════════════════════════════════════════════════
    protected boolean isLoaded = false;
    protected boolean isPaused = true;
    protected boolean isDisposed = false;

    // ═══════════════════════════════════════════════════════════════
    // DIMENSIONES DE PANTALLA
    // ═══════════════════════════════════════════════════════════════
    protected int screenWidth = 1080;
    protected int screenHeight = 1920;

    // ═══════════════════════════════════════════════════════════════
    // METODOS ABSTRACTOS - Obligatorio implementar
    // ═══════════════════════════════════════════════════════════════

    /**
     * Nombre unico de la escena.
     * Se usa para identificar y seleccionar la escena.
     *
     * @return Nombre de la escena (ej: "Universo", "Ocean Pearl")
     */
    public abstract String getName();

    /**
     * Descripcion corta de la escena para mostrar al usuario.
     *
     * @return Descripcion (ej: "Explora el cosmos infinito...")
     */
    public abstract String getDescription();

    /**
     * Resource ID del preview/thumbnail de la escena.
     *
     * @return R.drawable.xxx
     */
    public abstract int getPreviewResourceId();

    /**
     * Crea e inicializa todos los objetos de la escena.
     * LLAMAR DESDE GL THREAD.
     *
     * Aqui debes:
     * - Crear planetas, naves, particulas, etc.
     * - Cargar texturas
     * - Configurar shaders
     * - Agregar objetos a sceneObjects
     */
    protected abstract void setupScene();

    /**
     * Libera recursos especificos de esta escena.
     * Se llama desde onDestroy() despues de limpiar sceneObjects.
     */
    protected abstract void releaseSceneResources();

    // ═══════════════════════════════════════════════════════════════
    // CICLO DE VIDA - Implementacion base
    // ═══════════════════════════════════════════════════════════════

    /**
     * Inicializa la escena con el contexto y recursos necesarios.
     * DEBE llamarse desde el GL thread.
     *
     * @param context        Contexto de Android
     * @param textureManager Manager de texturas compartido
     * @param camera         Controlador de camara
     */
    public void onCreate(Context context, TextureManager textureManager, CameraController camera) {
        if (isLoaded) {
            Log.w(TAG, getName() + " ya estaba cargada, ignorando onCreate");
            return;
        }

        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   🎬 CREANDO ESCENA: " + getName());
        Log.d(TAG, "╚════════════════════════════════════════╝");

        this.context = context;
        this.textureManager = textureManager;
        this.camera = camera;
        this.isDisposed = false;

        // Llamar al setup especifico de la escena
        setupScene();

        // Asignar camara a todos los objetos
        assignCameraToObjects();

        isLoaded = true;
        isPaused = false;

        Log.d(TAG, "✓ Escena " + getName() + " cargada con " + sceneObjects.size() + " objetos");
    }

    /**
     * Reanuda las animaciones de la escena.
     */
    public void onResume() {
        if (!isLoaded || isDisposed) return;

        isPaused = false;
        Log.d(TAG, "▶️ " + getName() + " resumida");
    }

    /**
     * Pausa las animaciones de la escena.
     */
    public void onPause() {
        isPaused = true;
        Log.d(TAG, "⏸️ " + getName() + " pausada");
    }

    /**
     * Libera TODOS los recursos de la escena.
     * MUY IMPORTANTE llamar esto al cambiar de wallpaper.
     * DEBE llamarse desde el GL thread.
     */
    public void onDestroy() {
        if (isDisposed) {
            Log.w(TAG, getName() + " ya estaba disposed");
            return;
        }

        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   🗑️ DESTRUYENDO ESCENA: " + getName());
        Log.d(TAG, "╚════════════════════════════════════════╝");

        // Pausar primero
        isPaused = true;

        // Liberar objetos que implementan Disposable
        for (SceneObject obj : sceneObjects) {
            if (obj instanceof Disposable) {
                try {
                    ((Disposable) obj).dispose();
                } catch (Exception e) {
                    Log.e(TAG, "Error disposing " + obj.getClass().getSimpleName(), e);
                }
            }
        }

        // Limpiar lista
        sceneObjects.clear();

        // Liberar recursos especificos de la escena
        releaseSceneResources();

        // Marcar como disposed
        isLoaded = false;
        isDisposed = true;

        Log.d(TAG, "✓ Escena " + getName() + " destruida completamente");
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE & DRAW - Llamados cada frame
    // ═══════════════════════════════════════════════════════════════

    /**
     * Actualiza la logica de todos los objetos de la escena.
     *
     * @param deltaTime Tiempo transcurrido desde el ultimo frame (segundos)
     */
    public void update(float deltaTime) {
        if (isPaused || !isLoaded || isDisposed) return;

        for (SceneObject obj : sceneObjects) {
            try {
                obj.update(deltaTime);
            } catch (Exception e) {
                Log.e(TAG, "Error updating " + obj.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Dibuja todos los objetos de la escena.
     * Se asume que el contexto GL ya esta configurado.
     */
    public void draw() {
        if (!isLoaded || isDisposed) return;

        // Usar copia para evitar ConcurrentModificationException
        final int size = sceneObjects.size();
        for (int i = 0; i < size; i++) {
            try {
                if (i < sceneObjects.size()) {
                    sceneObjects.get(i).draw();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error drawing object at " + i, e);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Asigna el CameraController a todos los objetos CameraAware.
     */
    protected void assignCameraToObjects() {
        for (SceneObject obj : sceneObjects) {
            if (obj instanceof CameraAware) {
                ((CameraAware) obj).setCameraController(camera);
            }
        }
    }

    /**
     * Agrega un objeto a la escena y le asigna la camara si es necesario.
     *
     * @param obj Objeto a agregar
     */
    protected void addSceneObject(SceneObject obj) {
        if (obj == null) return;

        sceneObjects.add(obj);

        if (obj instanceof CameraAware && camera != null) {
            ((CameraAware) obj).setCameraController(camera);
        }
    }

    /**
     * Configura las dimensiones de pantalla.
     */
    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    /**
     * Actualiza el CameraController.
     */
    public void setCameraController(CameraController camera) {
        this.camera = camera;
        assignCameraToObjects();
    }

    // ═══════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════

    public boolean isLoaded() {
        return isLoaded;
    }

    /**
     * Verifica si la escena está lista para renderizar.
     * Override en subclases que necesitan verificar recursos (ej: video primer frame).
     * @return true si todos los recursos están listos
     */
    public boolean isReady() {
        return isLoaded && !isDisposed;
    }

    public boolean isPaused() {
        return isPaused;
    }

    @Override
    public boolean isDisposed() {
        return isDisposed;
    }

    @Override
    public void dispose() {
        onDestroy();
    }

    public List<SceneObject> getSceneObjects() {
        return sceneObjects;
    }

    public int getObjectCount() {
        return sceneObjects.size();
    }

    // ═══════════════════════════════════════════════════════════════
    // INTEGRACIÓN CON NUEVOS SISTEMAS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Inyectar ResourceManager (nuevo sistema)
     */
    public void setResourceManager(ResourceManager rm) {
        this.resourceManager = rm;
    }

    /**
     * Obtener ResourceManager
     */
    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    /**
     * Suscribirse a un evento del EventBus
     */
    protected EventBus.SubscriptionToken subscribe(String eventType, EventBus.EventListener listener) {
        return EventBus.get().subscribe(eventType, listener);
    }

    /**
     * Publicar un evento al EventBus
     */
    protected void publish(String eventType, EventBus.EventData data) {
        EventBus.get().publish(eventType, data);
    }

    /**
     * Manejar evento de touch (override en subclases)
     * @param normalizedX X en rango -1 a 1
     * @param normalizedY Y en rango -1 a 1
     * @param action MotionEvent.ACTION_*
     * @return true si el evento fue consumido
     */
    public boolean onTouchEvent(float normalizedX, float normalizedY, int action) {
        return false;  // Override en subclases si necesitan touch
    }

    /**
     * Manejar evento de touch con datos completos (multi-touch)
     * Override en subclases que necesitan pinch/zoom/rotate
     * @param event MotionEvent completo con datos multi-touch
     * @return true si el evento fue consumido
     */
    public boolean onTouchEventRaw(android.view.MotionEvent event) {
        // Por defecto, delegar al método simple
        float nx = (event.getX() / Math.max(1, screenWidth)) * 2f - 1f;
        float ny = -((event.getY() / Math.max(1, screenHeight)) * 2f - 1f);
        return onTouchEvent(nx, ny, event.getAction());
    }
}
