package com.secret.blackholeglow.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.adapters.WallpaperAdapter;
import com.secret.blackholeglow.image.ImageDownloadManager;
import com.secret.blackholeglow.model.ModelDownloadManager;
import com.secret.blackholeglow.models.WallpaperItem;
import com.secret.blackholeglow.systems.SubscriptionManager;
import com.secret.blackholeglow.systems.WallpaperCatalog;
import com.secret.blackholeglow.video.VideoDownloadManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Fragment que muestra la lista de wallpapers disponibles.
 *
 * IMPORTANTE: Los botones "VER WALLPAPER" están DESHABILITADOS hasta que
 * TODOS los recursos del panel de control estén descargados:
 * - grimoire.obj (modelo del libro)
 * - grimoire_texture.png (textura del libro)
 * - huevo_zerg.png (LikeButton ABYSSIA)
 * - fire_orb.png (LikeButton PYRALIS)
 *
 * NOTA: Video del panel (thehouse.mp4) eliminado en v5.0.7
 */
public class AnimatedWallpaperListFragment extends Fragment {
    private static final String TAG = "WallpaperListFragment";

    // ╔═══════════════════════════════════════════════════════════════════════╗
    // ║  📦 RECURSOS DEL PANEL DE CONTROL                                     ║
    // ║  Todos estos deben estar descargados antes de mostrar cualquier scene ║
    // ║  NOTA: Video del panel (thehouse.mp4) eliminado en v5.0.7             ║
    // ╚═══════════════════════════════════════════════════════════════════════╝
    private static final String PANEL_MODEL = "grimoire.obj";
    private static final String[] PANEL_IMAGES = {
        "grimoire_texture.png",  // Textura del libro mágico
        "huevo_zerg.png",        // LikeButton para ABYSSIA
        "fire_orb.png"           // LikeButton para PYRALIS
    };

    private List<WallpaperItem> wallpaperItems;
    private WallpaperAdapter adapter;
    private VideoDownloadManager videoManager;
    private ImageDownloadManager imageManager;
    private ModelDownloadManager modelManager;
    private Handler mainHandler;

    // Estado de descarga
    private int totalResources;
    private int downloadedResources;
    private boolean isDownloading = false;

    // ⏱️ TIMEOUT PROTECTION
    private static final int DOWNLOAD_TIMEOUT_SECONDS = 60;  // 1 minuto por recurso
    private static final int MAX_RETRY_ATTEMPTS = 2;
    private ExecutorService downloadExecutor;
    private Future<?> downloadTask;
    private int retryCount = 0;
    private boolean downloadFailed = false;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_animated_wallpapers,
                container,
                false
        );

        mainHandler = new Handler(Looper.getMainLooper());
        videoManager = VideoDownloadManager.getInstance(requireContext());
        imageManager = ImageDownloadManager.getInstance(requireContext());
        modelManager = ModelDownloadManager.getInstance(requireContext());

        RecyclerView recyclerView = view.findViewById(R.id.wallpaper_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setInitialPrefetchItemCount(3);
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(3);

        androidx.recyclerview.widget.RecyclerView.RecycledViewPool viewPool =
            new androidx.recyclerview.widget.RecyclerView.RecycledViewPool();
        viewPool.setMaxRecycledViews(0, 5);
        recyclerView.setRecycledViewPool(viewPool);
        recyclerView.setNestedScrollingEnabled(true);

        SubscriptionManager.init(requireContext());
        wallpaperItems = WallpaperCatalog.get().getAll();

        // Verificar si TODOS los recursos del panel están disponibles
        boolean isPanelReady = arePanelResourcesReady();
        Log.d(TAG, "📦 Recursos del panel disponibles: " + isPanelReady);

        // Crear adapter con estado de recursos
        adapter = new WallpaperAdapter(
                getContext(),
                wallpaperItems,
                item -> { },
                isPanelReady
        );
        recyclerView.setAdapter(adapter);

        // Si los recursos no están listos, iniciar descarga
        if (!isPanelReady) {
            startPanelResourcesDownload();
        }

        return view;
    }

    /**
     * Verifica si TODOS los recursos del panel están descargados.
     * NOTA: Video del panel eliminado en v5.0.7
     */
    private boolean arePanelResourcesReady() {
        // 1. Modelo
        if (!modelManager.isModelAvailable(PANEL_MODEL)) {
            Log.d(TAG, "❌ Falta: " + PANEL_MODEL);
            return false;
        }

        // 2. Imágenes
        for (String img : PANEL_IMAGES) {
            if (!imageManager.isImageAvailable(img)) {
                Log.d(TAG, "❌ Falta: " + img);
                return false;
            }
        }

        Log.d(TAG, "✅ Todos los recursos del panel disponibles");
        return true;
    }

    /**
     * Cuenta cuántos recursos faltan por descargar.
     * NOTA: Video del panel eliminado en v5.0.7
     */
    private int countMissingResources() {
        int missing = 0;
        if (!modelManager.isModelAvailable(PANEL_MODEL)) missing++;
        for (String img : PANEL_IMAGES) {
            if (!imageManager.isImageAvailable(img)) missing++;
        }
        return missing;
    }

    /**
     * Inicia la descarga de TODOS los recursos del panel en background.
     * Descarga secuencialmente: modelo → imágenes
     * Cuando termina, actualiza el adapter para habilitar los botones.
     *
     * ⏱️ PROTECCIÓN: Timeout de 60 segundos por recurso + retry automático
     * NOTA: Video del panel eliminado en v5.0.7
     */
    private void startPanelResourcesDownload() {
        if (isDownloading) return;
        isDownloading = true;
        downloadFailed = false;

        totalResources = 1 + PANEL_IMAGES.length; // model + images (sin video)
        downloadedResources = 0;

        Log.d(TAG, "📥 Iniciando descarga de " + countMissingResources() + " recursos del panel...");

        // 🛡️ Usar ExecutorService para poder cancelar/timeout
        if (downloadExecutor != null) {
            downloadExecutor.shutdownNow();
        }
        downloadExecutor = Executors.newSingleThreadExecutor();

        downloadTask = downloadExecutor.submit(() -> {
            try {
                // 1. MODELO (con timeout)
                if (!downloadResourceWithTimeout("modelo", PANEL_MODEL, () -> {
                    if (!modelManager.isModelAvailable(PANEL_MODEL)) {
                        Log.d(TAG, "📥 Descargando modelo: " + PANEL_MODEL);
                        updateProgress("Descargando modelo...");
                        return modelManager.downloadModelSync(PANEL_MODEL, percent -> {
                            int globalPercent = calculateGlobalProgress(downloadedResources, percent);
                            updateAdapterProgress(globalPercent);
                        });
                    }
                    return true;
                })) {
                    handleDownloadFailure("modelo: " + PANEL_MODEL);
                    return;
                }
                downloadedResources++;

                // 2. IMÁGENES (con timeout cada una)
                for (String img : PANEL_IMAGES) {
                    final String currentImg = img;
                    if (!downloadResourceWithTimeout("imagen", img, () -> {
                        if (!imageManager.isImageAvailable(currentImg)) {
                            Log.d(TAG, "📥 Descargando imagen: " + currentImg);
                            updateProgress("Descargando texturas...");
                            return imageManager.downloadImageSync(currentImg, percent -> {
                                int globalPercent = calculateGlobalProgress(downloadedResources, percent);
                                updateAdapterProgress(globalPercent);
                            });
                        }
                        return true;
                    })) {
                        handleDownloadFailure("imagen: " + img);
                        return;
                    }
                    downloadedResources++;
                }

                // ✅ Todo listo
                Log.d(TAG, "✅ Todos los recursos del panel descargados");
                mainHandler.post(() -> {
                    isDownloading = false;
                    retryCount = 0;
                    if (adapter != null && isAdded()) {
                        adapter.setPanelResourcesReady(true);
                    }
                });

            } catch (InterruptedException e) {
                Log.w(TAG, "⚠️ Descarga interrumpida");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                Log.e(TAG, "❌ Error en descarga: " + e.getMessage());
                handleDownloadFailure(e.getMessage());
            }
        });
    }

    /**
     * ⏱️ Ejecuta una descarga con timeout.
     * @return true si la descarga fue exitosa, false si timeout o error
     */
    private boolean downloadResourceWithTimeout(String type, String name, DownloadTask task)
            throws InterruptedException {
        ExecutorService singleTask = Executors.newSingleThreadExecutor();
        Future<Boolean> future = singleTask.submit(task::execute);

        try {
            Boolean result = future.get(DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return result != null && result;
        } catch (TimeoutException e) {
            Log.e(TAG, "⏱️ TIMEOUT descargando " + type + ": " + name);
            future.cancel(true);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "❌ Error descargando " + type + ": " + e.getMessage());
            return false;
        } finally {
            singleTask.shutdownNow();
        }
    }

    /**
     * 🔄 Maneja el fallo de descarga con retry automático.
     */
    private void handleDownloadFailure(String failedResource) {
        downloadFailed = true;
        isDownloading = false;

        mainHandler.post(() -> {
            if (!isAdded()) return;

            if (retryCount < MAX_RETRY_ATTEMPTS) {
                retryCount++;
                Log.w(TAG, "🔄 Reintentando descarga (intento " + retryCount + "/" + MAX_RETRY_ATTEMPTS + ")");

                // Esperar 2 segundos y reintentar
                mainHandler.postDelayed(() -> {
                    if (isAdded()) {
                        startPanelResourcesDownload();
                    }
                }, 2000);
            } else {
                Log.e(TAG, "❌ Descarga fallida después de " + MAX_RETRY_ATTEMPTS + " intentos: " + failedResource);
                // Notificar al adapter para mostrar botón de retry manual
                if (adapter != null) {
                    adapter.setDownloadFailed(true);
                }
            }
        });
    }

    /**
     * 🔄 Permite reintentar la descarga manualmente (llamado desde adapter).
     */
    public void retryDownload() {
        retryCount = 0;
        downloadFailed = false;
        if (adapter != null) {
            adapter.setDownloadFailed(false);
        }
        startPanelResourcesDownload();
    }

    /**
     * Interface funcional para las tareas de descarga.
     */
    @FunctionalInterface
    private interface DownloadTask {
        boolean execute();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 🛡️ Cancelar descargas pendientes al destruir el fragment
        if (downloadTask != null) {
            downloadTask.cancel(true);
        }
        if (downloadExecutor != null) {
            downloadExecutor.shutdownNow();
        }
    }

    /**
     * Calcula el progreso global considerando todos los recursos.
     */
    private int calculateGlobalProgress(int completedResources, int currentResourcePercent) {
        float progress = (completedResources + currentResourcePercent / 100f) / totalResources * 100f;
        return (int) progress;
    }

    /**
     * Actualiza el progreso en el adapter.
     */
    private void updateAdapterProgress(int percent) {
        mainHandler.post(() -> {
            if (adapter != null && isAdded()) {
                adapter.setDownloadProgress(percent);
            }
        });
    }

    /**
     * Muestra mensaje de estado (para debugging).
     */
    private void updateProgress(String status) {
        Log.d(TAG, "📊 " + status + " (" + downloadedResources + "/" + totalResources + ")");
    }
}
