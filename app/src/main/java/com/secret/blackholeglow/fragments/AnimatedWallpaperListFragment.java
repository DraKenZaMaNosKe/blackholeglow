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

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.adapters.WallpaperAdapter;
import com.secret.blackholeglow.image.ImageDownloadManager;
import com.secret.blackholeglow.model.ModelDownloadManager;
import com.secret.blackholeglow.models.WallpaperCategory;
import com.secret.blackholeglow.models.WallpaperItem;
import com.secret.blackholeglow.systems.DynamicCatalog;
import com.secret.blackholeglow.systems.SubscriptionManager;
import com.secret.blackholeglow.systems.WallpaperCatalog;
import com.secret.blackholeglow.systems.WallpaperNotificationManager;
import com.secret.blackholeglow.video.VideoDownloadManager;
import com.secret.blackholeglow.core.PanelResources;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import androidx.appcompat.view.ContextThemeWrapper;

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
 * - controlxbox_texture.obj (modelo del control Xbox)
 * - controlxbox_texture.png (textura del control)
 * - huevo_zerg.png (LikeButton ABYSSIA)
 * - fire_orb.png (LikeButton PYRALIS)
 *
 * v5.0.8: Gaming Controller reemplaza ArcaneGrimoire
 */
public class AnimatedWallpaperListFragment extends Fragment {
    private static final String TAG = "WallpaperListFragment";

    // ╔═══════════════════════════════════════════════════════════════════════╗
    // ║  📦 RECURSOS DEL PANEL DE CONTROL                                     ║
    // ║  Todos estos deben estar descargados antes de mostrar cualquier scene ║
    // ║  v5.0.8: Gaming Controller reemplaza ArcaneGrimoire                   ║
    // ║  Recursos centralizados en PanelResources.java                        ║
    // ╚═══════════════════════════════════════════════════════════════════════╝

    private List<WallpaperItem> wallpaperItems;
    private WallpaperAdapter adapter;
    private RecyclerView recyclerViewRef;
    private ChipGroup chipGroupCategories;
    private WallpaperCategory selectedCategory = WallpaperCategory.ALL;
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
        recyclerViewRef = recyclerView;
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setInitialPrefetchItemCount(2);
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(2);

        androidx.recyclerview.widget.RecyclerView.RecycledViewPool viewPool =
            new androidx.recyclerview.widget.RecyclerView.RecycledViewPool();
        viewPool.setMaxRecycledViews(0, 5);
        recyclerView.setRecycledViewPool(viewPool);
        recyclerView.setNestedScrollingEnabled(true);

        SubscriptionManager.init(requireContext());
        // Load dynamic wallpapers from cache (offline-safe)
        WallpaperCatalog.get().loadDynamicEntries(requireContext());
        // Refresh dynamic catalog in background
        refreshDynamicCatalogAsync();
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

        // Setup category chips
        chipGroupCategories = view.findViewById(R.id.chip_group_categories);
        setupCategoryChips();

        // Si los recursos no están listos, iniciar descarga
        if (!isPanelReady) {
            startPanelResourcesDownload();
        }

        return view;
    }

    /**
     * Creates category filter chips from available categories.
     * Only categories with at least 1 wallpaper are shown.
     */
    private void setupCategoryChips() {
        if (chipGroupCategories == null) return;

        List<WallpaperCategory> categories = WallpaperCatalog.get().getAvailableCategories();
        // Wrap context with Material theme for Chip compatibility (app uses AppCompat)
        android.content.Context chipContext = new ContextThemeWrapper(
                requireContext(), com.google.android.material.R.style.Theme_MaterialComponents);

        for (WallpaperCategory cat : categories) {
            Chip chip = new Chip(chipContext);
            chip.setText(cat.getDisplayLabel());
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            chip.setTextSize(13);
            chip.setTypeface(null, Typeface.BOLD);

            // Dark theme styling
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#1A1A2E")));
            chip.setTextColor(getResources().getColorStateList(R.color.chip_category_colors, null));
            chip.setChipStrokeWidth(1f);
            chip.setChipStrokeColor(ColorStateList.valueOf(Color.parseColor("#333355")));
            chip.setRippleColor(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")));

            // Select "TODOS" by default
            if (cat == WallpaperCategory.ALL) {
                chip.setChecked(true);
            }

            chip.setTag(cat);
            chipGroupCategories.addView(chip);
        }

        chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            Chip selected = group.findViewById(checkedIds.get(0));
            if (selected == null) return;

            WallpaperCategory cat = (WallpaperCategory) selected.getTag();
            if (cat == null) cat = WallpaperCategory.ALL;

            selectedCategory = cat;
            List<WallpaperItem> filtered = WallpaperCatalog.get().getByCategory(cat);
            if (adapter != null) {
                adapter.updateItems(filtered);
            }
        });
    }

    /**
     * Verifica si TODOS los recursos del panel están descargados.
     * NOTA: Video del panel eliminado en v5.0.7
     */
    private boolean arePanelResourcesReady() {
        // 1. Modelo
        if (!modelManager.isModelAvailable(PanelResources.PRIMARY_MODEL)) {
            Log.d(TAG, "❌ Falta: " + PanelResources.PRIMARY_MODEL);
            return false;
        }

        // 2. Imágenes
        for (String img : PanelResources.IMAGES_ARRAY) {
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
        if (!modelManager.isModelAvailable(PanelResources.PRIMARY_MODEL)) missing++;
        for (String img : PanelResources.IMAGES_ARRAY) {
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

        totalResources = 1 + PanelResources.IMAGES_ARRAY.length; // model + images (sin video)
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
                if (!downloadResourceWithTimeout("modelo", PanelResources.PRIMARY_MODEL, () -> {
                    if (!modelManager.isModelAvailable(PanelResources.PRIMARY_MODEL)) {
                        Log.d(TAG, "📥 Descargando modelo: " + PanelResources.PRIMARY_MODEL);
                        updateProgress("Descargando modelo...");
                        return modelManager.downloadModelSync(PanelResources.PRIMARY_MODEL, percent -> {
                            int globalPercent = calculateGlobalProgress(downloadedResources, percent);
                            updateAdapterProgress(globalPercent);
                        });
                    }
                    return true;
                })) {
                    handleDownloadFailure("modelo: " + PanelResources.PRIMARY_MODEL);
                    return;
                }
                downloadedResources++;

                // 2. IMÁGENES (con timeout cada una)
                for (String img : PanelResources.IMAGES_ARRAY) {
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
     * Refreshes the dynamic catalog from Supabase in background.
     * If new entries are found, reloads the catalog and updates the adapter.
     */
    private void refreshDynamicCatalogAsync() {
        new Thread(() -> {
            try {
                DynamicCatalog dc = DynamicCatalog.get();
                boolean hasNoEntries = dc.getCachedEntries(requireContext()).isEmpty();
                boolean needsRefresh = hasNoEntries || dc.isCacheStale(requireContext());
                if (needsRefresh) {
                    boolean updated = dc.refresh(requireContext());
                    if (updated && isAdded()) {
                        mainHandler.post(() -> {
                            if (!isAdded()) return;
                            WallpaperCatalog.get().loadDynamicEntries(requireContext());
                            // Refresh with current category filter
                            if (adapter != null) {
                                List<WallpaperItem> filtered = WallpaperCatalog.get().getByCategory(selectedCategory);
                                adapter.updateItems(filtered);
                            }
                            // Rebuild chips in case new categories appeared
                            if (chipGroupCategories != null) {
                                chipGroupCategories.removeAllViews();
                                setupCategoryChips();
                            }
                            Log.d(TAG, "Dynamic catalog updated: " + dc.getCachedEntries(requireContext()).size() + " entries");
                        });
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Dynamic catalog refresh failed: " + e.getMessage());
            }
        }, "DynamicCatalogRefresh").start();
    }

    /**
     * Interface funcional para las tareas de descarga.
     */
    @FunctionalInterface
    private interface DownloadTask {
        boolean execute();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Verificar qué wallpaper está activo en el sistema y actualizar badge
        if (adapter != null && isAdded()) {
            try {
                WallpaperNotificationManager.InstalledWallpaperInfo info =
                        WallpaperNotificationManager.getInstance(requireContext())
                                .verifyInstalledWallpaper();
                if (info.isOurWallpaperActive) {
                    adapter.setInstalledWallpaper(info.sceneName);
                    Log.d(TAG, "🏷️ Badge INSTALADO actualizado: " + info.sceneName);
                } else {
                    adapter.setInstalledWallpaper(null);
                    Log.d(TAG, "🏷️ Ningún wallpaper nuestro activo - sin badge");
                }
            } catch (Exception e) {
                Log.w(TAG, "Error verificando wallpaper activo: " + e.getMessage());
            }
        }
    }

    @Override
    public void onDestroyView() {
        // 🧠 FIX GL LEAK: Reciclar TODOS los bitmaps para liberar texturas GPU
        if (adapter != null && recyclerViewRef != null) {
            adapter.releaseAllBitmaps(recyclerViewRef);
        }
        if (recyclerViewRef != null) {
            recyclerViewRef.setAdapter(null);
            recyclerViewRef = null;
        }
        adapter = null;

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
