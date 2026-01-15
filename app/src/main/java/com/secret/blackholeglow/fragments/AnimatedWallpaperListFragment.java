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

/**
 * Fragment que muestra la lista de wallpapers disponibles.
 *
 * IMPORTANTE: Los botones "VER WALLPAPER" están DESHABILITADOS hasta que
 * TODOS los recursos del panel de control estén descargados:
 * - thehouse.mp4 (video de fondo)
 * - grimoire.obj (modelo del libro)
 * - grimoire_texture.png (textura del libro)
 * - huevo_zerg.png (LikeButton ABYSSIA)
 * - fire_orb.png (LikeButton PYRALIS)
 */
public class AnimatedWallpaperListFragment extends Fragment {
    private static final String TAG = "WallpaperListFragment";

    // ╔═══════════════════════════════════════════════════════════════════════╗
    // ║  📦 RECURSOS DEL PANEL DE CONTROL                                     ║
    // ║  Todos estos deben estar descargados antes de mostrar cualquier scene ║
    // ╚═══════════════════════════════════════════════════════════════════════╝
    private static final String PANEL_VIDEO = "thehouse.mp4";
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
     */
    private boolean arePanelResourcesReady() {
        // 1. Video
        if (!videoManager.isVideoAvailable(PANEL_VIDEO)) {
            Log.d(TAG, "❌ Falta: " + PANEL_VIDEO);
            return false;
        }

        // 2. Modelo
        if (!modelManager.isModelAvailable(PANEL_MODEL)) {
            Log.d(TAG, "❌ Falta: " + PANEL_MODEL);
            return false;
        }

        // 3. Imágenes
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
     */
    private int countMissingResources() {
        int missing = 0;
        if (!videoManager.isVideoAvailable(PANEL_VIDEO)) missing++;
        if (!modelManager.isModelAvailable(PANEL_MODEL)) missing++;
        for (String img : PANEL_IMAGES) {
            if (!imageManager.isImageAvailable(img)) missing++;
        }
        return missing;
    }

    /**
     * Inicia la descarga de TODOS los recursos del panel en background.
     * Descarga secuencialmente: video → modelo → imágenes
     * Cuando termina, actualiza el adapter para habilitar los botones.
     */
    private void startPanelResourcesDownload() {
        if (isDownloading) return;
        isDownloading = true;

        totalResources = 1 + 1 + PANEL_IMAGES.length; // video + model + images
        downloadedResources = 0;

        Log.d(TAG, "📥 Iniciando descarga de " + countMissingResources() + " recursos del panel...");

        new Thread(() -> {
            // 1. VIDEO
            if (!videoManager.isVideoAvailable(PANEL_VIDEO)) {
                Log.d(TAG, "📥 Descargando video: " + PANEL_VIDEO);
                updateProgress("Descargando video...");
                videoManager.downloadVideoSync(PANEL_VIDEO, percent -> {
                    int globalPercent = calculateGlobalProgress(downloadedResources, percent);
                    updateAdapterProgress(globalPercent);
                });
            }
            downloadedResources++;

            // 2. MODELO
            if (!modelManager.isModelAvailable(PANEL_MODEL)) {
                Log.d(TAG, "📥 Descargando modelo: " + PANEL_MODEL);
                updateProgress("Descargando modelo...");
                modelManager.downloadModelSync(PANEL_MODEL, percent -> {
                    int globalPercent = calculateGlobalProgress(downloadedResources, percent);
                    updateAdapterProgress(globalPercent);
                });
            }
            downloadedResources++;

            // 3. IMÁGENES
            for (String img : PANEL_IMAGES) {
                if (!imageManager.isImageAvailable(img)) {
                    Log.d(TAG, "📥 Descargando imagen: " + img);
                    updateProgress("Descargando texturas...");
                    imageManager.downloadImageSync(img, percent -> {
                        int globalPercent = calculateGlobalProgress(downloadedResources, percent);
                        updateAdapterProgress(globalPercent);
                    });
                }
                downloadedResources++;
            }

            // Todo listo
            Log.d(TAG, "✅ Todos los recursos del panel descargados");
            mainHandler.post(() -> {
                isDownloading = false;
                if (adapter != null && isAdded()) {
                    adapter.setPanelVideoReady(true);
                }
            });
        }).start();
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
