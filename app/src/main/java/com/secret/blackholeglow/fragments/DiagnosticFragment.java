package com.secret.blackholeglow.fragments;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.adapters.DiagnosticAdapter;
import com.secret.blackholeglow.core.DeviceProfile;
import com.secret.blackholeglow.diagnostic.DiagnosticData;
import com.secret.blackholeglow.diagnostic.DiagnosticData.CompatLevel;
import com.secret.blackholeglow.models.SceneWeight;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Panel de diagnostico que muestra info del dispositivo, almacenamiento,
 * wallpaper activo y compatibilidad de todos los wallpapers.
 * La RAM disponible se refresca cada 2 segundos.
 */
public class DiagnosticFragment extends Fragment {

    private static final long RAM_REFRESH_INTERVAL = 2000; // ms

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Views de RAM (refresco en vivo)
    private TextView ramAvailableText;
    private ProgressBar ramBar;
    private long totalRamGB;

    private final Runnable ramRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isAdded()) return;
            long availMB = DeviceProfile.get().getAvailableRamMB();
            if (availMB >= 0) {
                long totalMB = totalRamGB * 1024;
                long usedMB = totalMB - availMB;
                ramAvailableText.setText(String.format(Locale.US,
                        "%d MB free / %d MB total", availMB, totalMB));
                if (totalMB > 0) {
                    ramBar.setMax((int) totalMB);
                    ramBar.setProgress((int) usedMB);
                }
            }
            handler.postDelayed(this, RAM_REFRESH_INTERVAL);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_diagnostic, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Guardar refs de RAM para refresco
        ramAvailableText = view.findViewById(R.id.diag_ram_available);
        ramBar = view.findViewById(R.id.diag_ram_bar);

        // Recolectar datos en background
        executor.execute(() -> {
            DiagnosticData data = DiagnosticData.collect(requireContext());
            if (isAdded()) {
                handler.post(() -> bindData(view, data));
            }
        });
    }

    private void bindData(View root, DiagnosticData d) {
        if (!isAdded()) return;

        // ═══ TARJETA 1: DISPOSITIVO ═══
        ((TextView) root.findViewById(R.id.diag_device_model)).setText(d.deviceModel);
        ((TextView) root.findViewById(R.id.diag_android_version)).setText(d.androidVersion);

        // RAM tier badge
        TextView tierBadge = root.findViewById(R.id.diag_ram_tier_badge);
        String tierLabel;
        int tierColor;
        switch (d.memoryTier) {
            case LOW:
                tierLabel = "LOW";
                tierColor = 0xFFF44336;
                break;
            case HIGH:
                tierLabel = "HIGH";
                tierColor = 0xFF4CAF50;
                break;
            default:
                tierLabel = "MED";
                tierColor = 0xFFFFC107;
                break;
        }
        tierBadge.setText(tierLabel);
        tierBadge.setTextColor(tierColor);
        GradientDrawable tierBg = new GradientDrawable();
        tierBg.setCornerRadius(8f);
        tierBg.setColor(tierColor & 0x33FFFFFF); // 20% alpha
        tierBadge.setBackground(tierBg);

        // RAM en vivo
        totalRamGB = d.totalRamGB;
        handler.post(ramRefreshRunnable);

        // Textura
        ((TextView) root.findViewById(R.id.diag_texture_info)).setText(
                String.format(Locale.US, "Max textura: %dpx | inSampleSize: %d",
                        d.maxTextureDim, d.inSampleSize));

        // ═══ TARJETA 2: ALMACENAMIENTO ═══
        long totalMB = d.getTotalCacheBytes() / (1024 * 1024);
        ((TextView) root.findViewById(R.id.diag_storage_total)).setText(
                String.format(Locale.US, "%.1f MB total", d.getTotalCacheBytes() / (1024f * 1024f)));

        fillStorageRow(root, R.id.diag_images_info, R.id.diag_images_bar,
                "Images", d.imageCacheCount, d.imageCacheBytes, d.getTotalCacheBytes());
        fillStorageRow(root, R.id.diag_videos_info, R.id.diag_videos_bar,
                "Videos", d.videoCacheCount, d.videoCacheBytes, d.getTotalCacheBytes());
        fillStorageRow(root, R.id.diag_models_info, R.id.diag_models_bar,
                "3D Models", d.modelCacheCount, d.modelCacheBytes, d.getTotalCacheBytes());

        // ═══ TARJETA 3: WALLPAPER ACTIVO ═══
        TextView activeName = root.findViewById(R.id.diag_active_name);
        TextView activeWeight = root.findViewById(R.id.diag_active_weight);
        TextView activeCompat = root.findViewById(R.id.diag_active_compat);
        TextView activeMessage = root.findViewById(R.id.diag_active_message);

        if (d.activeWallpaperWeight != null) {
            activeName.setText(d.activeWallpaperName);
            applyWeightBadge(activeWeight, d.activeWallpaperWeight);
            applyCompatBadge(activeCompat, d.activeWallpaperCompat);
            activeMessage.setText(getCompatMessage(d.activeWallpaperCompat));
        } else {
            activeName.setText(d.activeWallpaperName != null
                    ? d.activeWallpaperName : getString(R.string.diag_no_active));
            activeWeight.setVisibility(View.GONE);
            activeCompat.setVisibility(View.GONE);
            activeMessage.setText(R.string.diag_no_active_hint);
        }

        // ═══ TARJETA 4: COMPATIBILIDAD ═══
        RecyclerView recycler = root.findViewById(R.id.diag_compat_recycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(new DiagnosticAdapter(d.wallpaperCompatList));
    }

    private void fillStorageRow(View root, int textId, int barId,
                                String label, int count, long bytes, long totalBytes) {
        float mb = bytes / (1024f * 1024f);
        ((TextView) root.findViewById(textId)).setText(
                String.format(Locale.US, "%s: %d archivos (%.1f MB)", label, count, mb));

        ProgressBar bar = root.findViewById(barId);
        if (totalBytes > 0) {
            bar.setMax(1000);
            bar.setProgress((int) (bytes * 1000 / totalBytes));
        } else {
            bar.setMax(1);
            bar.setProgress(0);
        }
    }

    private void applyWeightBadge(TextView tv, SceneWeight weight) {
        String label;
        int color;
        switch (weight) {
            case LIGHT:
                label = "LIGHT";
                color = 0xFF4CAF50;
                break;
            case HEAVY:
                label = "HEAVY";
                color = 0xFFF44336;
                break;
            default:
                label = "MEDIUM";
                color = 0xFFFFC107;
                break;
        }
        tv.setText(label);
        tv.setTextColor(color);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(8f);
        bg.setColor(color & 0x33FFFFFF);
        tv.setBackground(bg);
    }

    private void applyCompatBadge(TextView tv, CompatLevel level) {
        String label;
        int color;
        switch (level) {
            case OPTIMAL:
                label = "OPTIMAL";
                color = 0xFF4CAF50;
                break;
            case NOT_RECOMMENDED:
                label = "NOT RECOMMENDED";
                color = 0xFFF44336;
                break;
            default:
                label = "MODERATE";
                color = 0xFFFFC107;
                break;
        }
        tv.setText(label);
        tv.setTextColor(color);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(8f);
        bg.setColor(color & 0x33FFFFFF);
        tv.setBackground(bg);
    }

    private String getCompatMessage(CompatLevel level) {
        if (level == null) return "";
        switch (level) {
            case OPTIMAL:
                return getString(R.string.diag_compat_optimal_msg);
            case NOT_RECOMMENDED:
                return getString(R.string.diag_compat_not_recommended_msg);
            default:
                return getString(R.string.diag_compat_moderate_msg);
        }
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(ramRefreshRunnable);
        super.onDestroyView();
    }
}
