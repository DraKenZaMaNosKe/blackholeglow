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
import com.secret.blackholeglow.models.WallpaperItem;
import com.secret.blackholeglow.systems.SubscriptionManager;
import com.secret.blackholeglow.systems.WallpaperCatalog;
import com.secret.blackholeglow.video.VideoDownloadManager;

import java.util.List;

/**
 * Fragment que muestra la lista de wallpapers disponibles.
 *
 * IMPORTANTE: Los botones "VER WALLPAPER" están DESHABILITADOS hasta que
 * el video del panel de control (thehouse.mp4) esté descargado.
 */
public class AnimatedWallpaperListFragment extends Fragment {
    private static final String TAG = "WallpaperListFragment";
    private static final String PANEL_VIDEO_FILE = "thehouse.mp4";

    private List<WallpaperItem> wallpaperItems;
    private WallpaperAdapter adapter;
    private VideoDownloadManager videoDownloadManager;
    private Handler mainHandler;

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
        videoDownloadManager = VideoDownloadManager.getInstance(requireContext());

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

        // Verificar si el video del panel está disponible
        boolean isPanelVideoReady = videoDownloadManager.isVideoAvailable(PANEL_VIDEO_FILE);
        Log.d(TAG, "📹 Video del panel disponible: " + isPanelVideoReady);

        // Crear adapter con estado del video
        adapter = new WallpaperAdapter(
                getContext(),
                wallpaperItems,
                item -> { },
                isPanelVideoReady  // Nuevo parámetro
        );
        recyclerView.setAdapter(adapter);

        // Si el video no está listo, iniciar descarga
        if (!isPanelVideoReady) {
            startPanelVideoDownload();
        }

        return view;
    }

    /**
     * Inicia la descarga del video del panel en background.
     * Cuando termina, actualiza el adapter para habilitar los botones.
     */
    private void startPanelVideoDownload() {
        Log.d(TAG, "📥 Iniciando descarga del video del panel...");

        videoDownloadManager.downloadVideo(PANEL_VIDEO_FILE, new VideoDownloadManager.DownloadCallback() {
            @Override
            public void onProgress(int percent, long downloadedBytes, long totalBytes) {
                // Actualizar progreso en el adapter
                mainHandler.post(() -> {
                    if (adapter != null) {
                        adapter.setDownloadProgress(percent);
                    }
                });
            }

            @Override
            public void onComplete(String filePath) {
                Log.d(TAG, "✅ Video del panel descargado: " + filePath);
                mainHandler.post(() -> {
                    if (adapter != null) {
                        adapter.setPanelVideoReady(true);
                    }
                });
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "❌ Error descargando video del panel: " + message);
                // En caso de error, intentar de nuevo en 5 segundos
                mainHandler.postDelayed(() -> {
                    if (isAdded() && !videoDownloadManager.isVideoAvailable(PANEL_VIDEO_FILE)) {
                        startPanelVideoDownload();
                    }
                }, 5000);
            }
        });
    }
}
