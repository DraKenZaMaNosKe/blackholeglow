package com.secret.blackholeglow.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.fragments.WallpaperInfoDialogFragment;
import com.secret.blackholeglow.models.WallpaperItem;
import com.secret.blackholeglow.models.WallpaperTier;
import com.secret.blackholeglow.activities.WallpaperPreviewActivity;
import com.secret.blackholeglow.WallpaperPreferences;
import com.secret.blackholeglow.ui.GradientTextView;
import com.secret.blackholeglow.image.ImageDownloadManager;
import com.secret.blackholeglow.core.SceneRequirements;
import com.secret.blackholeglow.core.PreFlightCheck;
import android.app.AlertDialog;
import android.graphics.BitmapFactory;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║                     WallpaperAdapter.java                    ║
 * ║       Patron ViewHolder + OpenGL ES Neon Border Frame         ║
 * ╚════════════════════════════════════════════════════════════════╝
 * • adaptador para RecyclerView que muestra items de fondos animados,
 *   resaltados con un borde neón OpenGL (NeonBorderTextureView).
 * • Implementa animaciones, clics y diálogos de info.
 */
public class WallpaperAdapter extends RecyclerView.Adapter<WallpaperAdapter.WallpaperViewHolder> {

    // ╔═════════════════════════════════╗
    // ║  🗂️ Datos y Contexto            ║
    // ╚═════════════════════════════════╝
    private final List<WallpaperItem> wallpapers;    // lista de modelos
    private final Context context;                    // contexto para inflar y lanzar intents
    private final OnWallpaperClickListener listener; // callback al aplicar fondo

    // ╔═════════════════════════════════════════════════════════════════╗
    // ║  📦 Estado de recursos del panel de control                     ║
    // ║  Botones DESHABILITADOS hasta que los recursos estén listos     ║
    // ║  (Gaming Controller + texturas LikeButton)                      ║
    // ╚═════════════════════════════════════════════════════════════════╝
    private boolean isPanelResourcesReady = false;
    private int downloadProgress = 0;
    private boolean downloadFailed = false;  // ⚠️ Estado de error en descarga

    // ⚡ DEBOUNCE: Prevenir doble-click en botones
    private long lastClickTime = 0;
    private static final long CLICK_DEBOUNCE_MS = 800; // 800ms entre clicks

    // 🏷️ Badge "INSTALADO" - solo el wallpaper activo en el sistema
    private String installedSceneName = null;

    /**
     * Interface para notificar evento "aplicar wallpaper".
     */
    public interface OnWallpaperClickListener {
        void onApplyClicked(WallpaperItem item);
    }

    /**
     * ╔═════════════════════════════════╗
     * ║  🔧 Constructor                   ║
     * ╚═════════════════════════════════╝
     * @param context     Contexto actual (Activity/Fragment).
     * @param wallpapers  Lista de ítems a mostrar.
     * @param listener    Callback para acción "aplicar".
     */
    public WallpaperAdapter(Context context, List<WallpaperItem> wallpapers,
                            OnWallpaperClickListener listener) {
        this(context, wallpapers, listener, true); // Por defecto, video listo (compatibilidad)
    }

    /**
     * Constructor con estado de recursos del panel.
     * @param isPanelResourcesReady true si los recursos del panel están listos (controller, texturas)
     */
    public WallpaperAdapter(Context context, List<WallpaperItem> wallpapers,
                            OnWallpaperClickListener listener, boolean isPanelResourcesReady) {
        this.context = context;
        this.wallpapers = new ArrayList<>(wallpapers);
        this.listener = listener;
        this.isPanelResourcesReady = isPanelResourcesReady;
    }

    /**
     * Actualiza el estado de recursos del panel y refresca la lista.
     */
    public void setPanelResourcesReady(boolean ready) {
        this.isPanelResourcesReady = ready;
        this.downloadProgress = ready ? 100 : 0;
        // ⚡ OPTIMIZACIÓN: Solo actualizar los botones, no reconstruir todo
        notifyItemRangeChanged(0, getItemCount(), "BUTTON_UPDATE");
        Log.d("WallpaperAdapter", "📦 Panel resources ready: " + ready);
    }

    /**
     * Actualiza el progreso de descarga de recursos.
     * ⚡ OPTIMIZACIÓN: Solo actualiza cada 5% para evitar lag
     */
    private int lastNotifiedProgress = -1;
    public void setDownloadProgress(int progress) {
        this.downloadProgress = progress;
        // Solo notificar si cambió al menos 5% (evita lag por updates frecuentes)
        if (Math.abs(progress - lastNotifiedProgress) >= 5 || progress == 100) {
            lastNotifiedProgress = progress;
            notifyItemRangeChanged(0, getItemCount(), "BUTTON_UPDATE");
        }
    }

    /**
     * ⚠️ Indica si la descarga falló para mostrar opción de reintento.
     */
    public void setDownloadFailed(boolean failed) {
        this.downloadFailed = failed;
        notifyItemRangeChanged(0, getItemCount(), "BUTTON_UPDATE");
        Log.d("WallpaperAdapter", "❌ Download failed: " + failed);
    }

    /**
     * Establece el wallpaper actualmente instalado en el sistema.
     * Solo ese item mostrará el badge "INSTALADO".
     * @param sceneName sceneName del wallpaper activo, o null si ninguno
     */
    public void setInstalledWallpaper(String sceneName) {
        if ((installedSceneName == null && sceneName == null) ||
            (installedSceneName != null && installedSceneName.equals(sceneName))) {
            return; // Sin cambios
        }
        this.installedSceneName = sceneName;
        notifyItemRangeChanged(0, getItemCount(), "INSTALLED_UPDATE");
        Log.d("WallpaperAdapter", "🏷️ Wallpaper instalado actualizado: " + sceneName);
    }

    /**
     * Updates the displayed wallpaper list (for category filtering).
     * Replaces current items and refreshes the RecyclerView.
     */
    public void updateItems(List<WallpaperItem> newItems) {
        this.wallpapers.clear();
        this.wallpapers.addAll(newItems);
        notifyDataSetChanged();
    }

    /**
     * ╔═════════════════════════════════╗
     * ║  📐 onCreateViewHolder           ║
     * ╚═════════════════════════════════╝
     *  • Infla el layout XML de cada item y crea el ViewHolder.
     */
    @NonNull
    @Override
    public WallpaperViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // ✨ Usar layout fullscreen SIMPLE (sin OpenGL)
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_wallpaper_fullscreen, parent, false);
        return new WallpaperViewHolder(view);
    }

    /**
     * ╔═════════════════════════════════╗
     * ║  🔄 onBindViewHolder             ║
     * ╚═════════════════════════════════╝
     *  • Vincula datos con vistas: título, descripción, imagen.
     *  • Configura listeners: clic en tarjeta, borde neón y botón.
     */
    @Override
    public void onBindViewHolder(@NonNull WallpaperViewHolder holder, int position) {
        WallpaperItem item = wallpapers.get(position);

        // Ocultar nombre y descripción — dejar que los usuarios imaginen
        holder.textTitle.setVisibility(View.GONE);
        holder.textDescription.setVisibility(View.GONE);

        // ╔═════════════════════════════════════════════════════════╗
        // ║  🎨 ASIGNAR IMAGEN DE PREVIEW DESDE WallpaperItem      ║
        // ╚═════════════════════════════════════════════════════════╝
        loadPreviewImage(holder.imagePreview, item);

        // ╔═════════════════════════════════════════════════════════╗
        // ║  🏷️ BADGE - Prioridad: INSTALADO > badge propio       ║
        // ╚═════════════════════════════════════════════════════════╝
        updateBadge(holder, item);

        // ╔═════════════════════════════════════════════════════════╗
        // ║  🔒 OVERLAY COMING SOON - Para wallpapers en desarrollo ║
        // ╚═════════════════════════════════════════════════════════╝
        boolean isComingSoon = item.getTier() == WallpaperTier.COMING_SOON;
        if (holder.overlayComingSoon != null) {
            holder.overlayComingSoon.setVisibility(isComingSoon ? View.VISIBLE : View.GONE);
        }

        // ╔═════════════════════════════════════════════════════════╗
        // ║  🎯 BOTÓN "VER WALLPAPER" - Va a preview              ║
        // ║  IMPORTANTE: Deshabilitado si los recursos del panel  ║
        // ║  no están descargados (controller, texturas)          ║
        // ╚═════════════════════════════════════════════════════════╝

        // ⚠️ ESTADO DE ERROR: Mostrar botón de reintento
        if (downloadFailed) {
            holder.buttonPreview.setEnabled(true);
            holder.buttonPreview.setAlpha(1.0f);
            holder.buttonPreview.setText("⚠️ RETRY");
            holder.buttonPreview.setOnClickListener(v -> {
                if (context instanceof FragmentActivity) {
                    androidx.fragment.app.Fragment fragment = ((FragmentActivity) context)
                            .getSupportFragmentManager()
                            .findFragmentById(R.id.fragment_container);
                    if (fragment instanceof com.secret.blackholeglow.fragments.AnimatedWallpaperListFragment) {
                        ((com.secret.blackholeglow.fragments.AnimatedWallpaperListFragment) fragment).retryDownload();
                    }
                }
            });
        }
        // Verificar si los recursos del panel están listos
        else if (!isPanelResourcesReady) {
            holder.buttonPreview.setEnabled(false);
            holder.buttonPreview.setAlpha(0.6f);
            if (downloadProgress > 0 && downloadProgress < 100) {
                holder.buttonPreview.setText("📥 Preparing " + downloadProgress + "%");
            } else {
                holder.buttonPreview.setText("📥 Preparing...");
            }
            holder.buttonPreview.setOnClickListener(null);
        }
        // Wallpaper disponible - botón habilitado
        else if (item.isAvailable()) {
            holder.buttonPreview.setEnabled(true);
            holder.buttonPreview.setAlpha(1.0f);
            holder.buttonPreview.setText("✨ VIEW WALLPAPER");
            holder.buttonPreview.setOnClickListener(v -> runPreFlightAndLaunch(item));
        } else {
            holder.buttonPreview.setEnabled(false);
            holder.buttonPreview.setAlpha(0.6f);
            holder.buttonPreview.setText("🔒 COMING SOON");
            holder.buttonPreview.setOnClickListener(null);
        }
    }

    /**
     * ╔═════════════════════════════════════════════════════════════════════╗
     * ║  ⚡ OPTIMIZACIÓN: Actualización parcial con payloads              ║
     * ║  Solo actualiza el botón sin reconstruir toda la vista           ║
     * ╚═════════════════════════════════════════════════════════════════════╝
     */
    @Override
    public void onBindViewHolder(@NonNull WallpaperViewHolder holder, int position, @NonNull java.util.List<Object> payloads) {
        if (payloads.isEmpty()) {
            // Sin payload = actualización completa
            onBindViewHolder(holder, position);
        } else {
            WallpaperItem item = wallpapers.get(position);
            for (Object payload : payloads) {
                if ("INSTALLED_UPDATE".equals(payload)) {
                    // Solo actualizar badge
                    updateBadge(holder, item);
                } else {
                    // BUTTON_UPDATE u otro = actualizar botón
                    updateButtonState(holder, item);
                }
            }
        }
    }

    /**
     * Actualiza solo el estado del botón (para actualizaciones parciales).
     */
    private void updateButtonState(WallpaperViewHolder holder, WallpaperItem item) {
        // ⚠️ ESTADO DE ERROR: Mostrar botón de reintento
        if (downloadFailed) {
            holder.buttonPreview.setEnabled(true);
            holder.buttonPreview.setAlpha(1.0f);
            holder.buttonPreview.setText("⚠️ RETRY");
            holder.buttonPreview.setOnClickListener(v -> {
                if (context instanceof FragmentActivity) {
                    androidx.fragment.app.Fragment fragment = ((FragmentActivity) context)
                            .getSupportFragmentManager()
                            .findFragmentById(R.id.fragment_container);
                    if (fragment instanceof com.secret.blackholeglow.fragments.AnimatedWallpaperListFragment) {
                        ((com.secret.blackholeglow.fragments.AnimatedWallpaperListFragment) fragment).retryDownload();
                    }
                }
            });
        }
        // Descargando recursos
        else if (!isPanelResourcesReady) {
            holder.buttonPreview.setEnabled(false);
            holder.buttonPreview.setAlpha(0.6f);
            holder.buttonPreview.setOnClickListener(null);
            if (downloadProgress > 0 && downloadProgress < 100) {
                holder.buttonPreview.setText("📥 Preparing " + downloadProgress + "%");
            } else {
                holder.buttonPreview.setText("📥 Preparing...");
            }
        } else if (item.isAvailable()) {
            holder.buttonPreview.setEnabled(true);
            holder.buttonPreview.setAlpha(1.0f);
            holder.buttonPreview.setText("✨ VIEW WALLPAPER");
            holder.buttonPreview.setOnClickListener(v -> runPreFlightAndLaunch(item));
        } else {
            holder.buttonPreview.setEnabled(false);
            holder.buttonPreview.setAlpha(0.6f);
            holder.buttonPreview.setOnClickListener(null);
            holder.buttonPreview.setText("🔒 COMING SOON");
        }
    }

    /**
     * Actualiza el badge del item: prioriza INSTALADO > RAM/DOWNLOAD status > badge propio.
     */
    private void updateBadge(WallpaperViewHolder holder, WallpaperItem item) {
        if (holder.textBadge == null) return;

        boolean isInstalled = installedSceneName != null &&
                item.getSceneName().equals(installedSceneName);

        if (isInstalled) {
            holder.textBadge.setText("INSTALLED");
            holder.textBadge.setBackgroundColor(Color.parseColor("#2E7D32"));
            holder.textBadge.setVisibility(View.VISIBLE);
        } else {
            // Evaluar estado de la escena
            SceneRequirements.SceneStatus status = SceneRequirements.evaluate(
                    context, item.getSceneName(), item.getSceneWeight());

            if (status == SceneRequirements.SceneStatus.NOT_RECOMMENDED) {
                holder.textBadge.setText("RAM");
                holder.textBadge.setBackgroundColor(Color.parseColor("#E65100"));
                holder.textBadge.setVisibility(View.VISIBLE);
            } else if (status == SceneRequirements.SceneStatus.NEEDS_DOWNLOAD) {
                holder.textBadge.setText("DOWNLOAD");
                holder.textBadge.setBackgroundColor(Color.parseColor("#1565C0"));
                holder.textBadge.setVisibility(View.VISIBLE);
            } else if (item.hasBadge()) {
                holder.textBadge.setText(item.getBadge());
                holder.textBadge.setBackgroundResource(R.drawable.badge_background);
                holder.textBadge.setVisibility(View.VISIBLE);
            } else {
                holder.textBadge.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Muestra un dialogo con información del wallpaper y lanza Preview.
     */
    private void showInfoDialog(WallpaperItem item) {
        if (context instanceof FragmentActivity) {
            WallpaperInfoDialogFragment dialog = WallpaperInfoDialogFragment.newInstance(item);
            dialog.setOnApplyClickListener(() -> {
                Intent intent = new Intent(context, WallpaperPreviewActivity.class);

                intent.putExtra("WALLPAPER_PREVIEW_ID", item.getResourceIdPreview());
                intent.putExtra("WALLPAPER_ID", item.getSceneName());


                context.startActivity(intent);
                if (listener != null) listener.onApplyClicked(item);
            });
            dialog.show(((FragmentActivity) context).getSupportFragmentManager(), "wallpaper_info");
        } else {
            Log.w("WallpaperAdapter", "Context NO es FragmentActivity");
        }
    }

    /**
     * ╔═════════════════════════════════╗
     * ║  📏 getItemCount                ║
     * ╚═════════════════════════════════╝
     *  • Retorna el número de ítems en la lista.
     */
    @Override
    public int getItemCount() {
        return wallpapers.size();
    }

    /**
     * ╔═════════════════════════════════╗
     * ║  🎬 PAUSAR animaciones fuera    ║
     * ║     de pantalla (optimización)  ║
     * ╚═════════════════════════════════╝
     */
    /**
     * 🧠 Reciclar bitmap cuando la vista sale del RecyclerView.
     * Libera la textura GL que hardware acceleration creó.
     */
    @Override
    public void onViewRecycled(@NonNull WallpaperViewHolder holder) {
        super.onViewRecycled(holder);
        recycleImageView(holder.imagePreview);
    }

    /**
     * 🧹 Libera TODOS los bitmaps de todas las vistas visibles.
     * Llamar desde Fragment.onDestroyView() antes de destruir el adapter.
     */
    public void releaseAllBitmaps(RecyclerView recyclerView) {
        if (recyclerView == null) return;
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            RecyclerView.ViewHolder vh = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
            if (vh instanceof WallpaperViewHolder) {
                recycleImageView(((WallpaperViewHolder) vh).imagePreview);
            }
        }
        // Also clear cached views
        recyclerView.getRecycledViewPool().clear();
        Log.d("WallpaperAdapter", "🧹 All preview bitmaps released");
    }

    /**
     * Recicla el bitmap de un ImageView y libera GPU memory.
     */
    private static void recycleImageView(ImageView imageView) {
        if (imageView == null) return;
        Drawable drawable = imageView.getDrawable();
        imageView.setImageDrawable(null);
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    /**
     * ╔═════════════════════════════════╗
     * ║  📦 WallpaperViewHolder          ║
     * ╚═════════════════════════════════╝
     *  • Patrón ViewHolder: guarda referencias a vistas.
     *  • Optimiza rendimiento evitando findViewById repetido.
     *  • Layout fullscreen: preview + título + descripción + botón
     */
    public static class WallpaperViewHolder extends RecyclerView.ViewHolder {
        ImageView imagePreview;
        GradientTextView textTitle;
        TextView textDescription;
        TextView textBadge;
        View overlayComingSoon;
        Button buttonPreview;
        View animatedBorder;

        public WallpaperViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePreview = itemView.findViewById(R.id.image_preview);
            textTitle = itemView.findViewById(R.id.text_title);
            textDescription = itemView.findViewById(R.id.text_description);
            textBadge = itemView.findViewById(R.id.text_badge);
            overlayComingSoon = itemView.findViewById(R.id.overlay_coming_soon);
            buttonPreview = itemView.findViewById(R.id.button_preview);
            animatedBorder = itemView.findViewById(R.id.animated_border);
        }
    }

    // ╔═════════════════════════════════════════════════════════════════╗
    // ║  🛡️ PRE-FLIGHT CHECK + LAUNCH                                   ║
    // ╚═════════════════════════════════════════════════════════════════╝

    /**
     * Ejecuta PreFlightCheck y decide si lanzar directo, mostrar warnings, o bloquear.
     */
    private void runPreFlightAndLaunch(WallpaperItem item) {
        PreFlightCheck.Result result = PreFlightCheck.run(context, item.getSceneName());

        if (result.isClean()) {
            launchWallpaper(item);
        } else if (result.canProceed) {
            showPreFlightWarnings(result, () -> launchWallpaper(item));
        } else {
            showPreFlightBlockers(result);
        }
    }

    /**
     * Lanza el wallpaper: ad + WallpaperLoadingActivity.
     */
    private void launchWallpaper(WallpaperItem item) {
        String previousWallpaper = WallpaperPreferences.getInstance(context).getSelectedWallpaperSync();

        com.secret.blackholeglow.systems.AdsManager.get().showInterstitialAd(
                (android.app.Activity) context, shown -> {
            Log.d("WallpaperAdapter", "Ad completado: " + shown + ", abriendo loading...");
            Intent intent = new Intent(context, com.secret.blackholeglow.activities.WallpaperLoadingActivity.class);
            intent.putExtra("WALLPAPER_PREVIEW_ID", item.getResourceIdPreview());
            intent.putExtra("WALLPAPER_ID", item.getSceneName());
            intent.putExtra("WALLPAPER_DISPLAY_NAME", item.getNombre());
            intent.putExtra("PREVIOUS_WALLPAPER_ID", previousWallpaper);
            context.startActivity(intent);
        });
    }

    /**
     * Muestra dialog con warnings y opciones "Continuar" / "Cancelar".
     */
    private void showPreFlightWarnings(PreFlightCheck.Result result, Runnable onProceed) {
        StringBuilder msg = new StringBuilder();
        for (PreFlightCheck.Issue issue : result.getWarnings()) {
            msg.append("• ").append(issue.titulo).append("\n");
            msg.append("  ").append(issue.detalle).append("\n");
            msg.append("  ➜ ").append(issue.accionSugerida).append("\n\n");
        }

        new AlertDialog.Builder(context)
                .setTitle("⚠️ Warning")
                .setMessage(msg.toString().trim())
                .setPositiveButton("Continue", (d, w) -> onProceed.run())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Muestra dialog con errores bloqueantes (no se puede continuar).
     */
    private void showPreFlightBlockers(PreFlightCheck.Result result) {
        StringBuilder msg = new StringBuilder();
        for (PreFlightCheck.Issue issue : result.getBlockers()) {
            msg.append("• ").append(issue.titulo).append("\n");
            msg.append("  ").append(issue.detalle).append("\n");
            msg.append("  ➜ ").append(issue.accionSugerida).append("\n\n");
        }
        // Tambien incluir warnings si los hay
        for (PreFlightCheck.Issue issue : result.getWarnings()) {
            msg.append("• ").append(issue.titulo).append("\n");
            msg.append("  ").append(issue.detalle).append("\n\n");
        }

        new AlertDialog.Builder(context)
                .setTitle("❌ Cannot continue")
                .setMessage(msg.toString().trim())
                .setPositiveButton("OK", null)
                .show();
    }

    // ╔═════════════════════════════════════════════════════════════════╗
    // ║  🎨 UTILIDADES DE COLOR                                         ║
    // ╚═════════════════════════════════════════════════════════════════╝

    /**
     * Aclara un color mezclándolo con blanco
     * @param color Color original
     * @param factor Factor de aclarado (0-1, mayor = más claro)
     * @return Color aclarado
     */
    private int lightenColor(int color, float factor) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        r = (int) (r + (255 - r) * factor);
        g = (int) (g + (255 - g) * factor);
        b = (int) (b + (255 - b) * factor);

        return Color.rgb(r, g, b);
    }

    // ╔═════════════════════════════════════════════════════════════════╗
    // ║  📥 CARGA DE PREVIEW CON SOPORTE REMOTO                        ║
    // ║  Si el wallpaper tiene preview remoto descargado, lo usa       ║
    // ║  Sino, usa el drawable local como fallback                     ║
    // ╚═════════════════════════════════════════════════════════════════╝

    // Mapeo de sceneName -> nombre de archivo remoto (para futuros previews remotos)
    private static final Map<String, String> REMOTE_PREVIEWS = new HashMap<>();
    static {
        // Preview de Adventure Time ahora es local (hdapreview.webp)
        // Agregar más wallpapers con preview remoto aquí si es necesario
    }

    // Track which previews are being lazy-downloaded to avoid duplicate threads
    private final java.util.Set<String> downloadingPreviews = new java.util.HashSet<>();
    private final Handler lazyHandler = new Handler(Looper.getMainLooper());

    /**
     * Carga la imagen de preview del wallpaper.
     * Si tiene preview remoto descargado, lo usa. Sino, usa drawable local.
     * Dynamic previews are lazy-downloaded: only when the item scrolls into view.
     * OPTIMIZADO: Escala imágenes grandes para evitar OutOfMemoryError
     * 🧠 FIX GL LEAK: Software layer prevents HWUI from caching bitmaps as GPU textures.
     */
    private void loadPreviewImage(ImageView imageView, WallpaperItem item) {
        // Prevent hardware renderer from uploading bitmap as GL texture
        imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // Check WallpaperItem's remote preview (dynamic wallpapers)
        String remoteFile = item.hasRemotePreview()
            ? item.getRemotePreviewFile()
            : REMOTE_PREVIEWS.get(item.getSceneName());

        if (remoteFile != null) {
            // Verificar si el preview remoto está descargado
            ImageDownloadManager downloader = ImageDownloadManager.getInstance(context);
            String localPath = downloader.getImagePath(remoteFile);

            if (localPath != null) {
                // Preview remoto disponible - cargar desde archivo
                File file = new File(localPath);
                if (file.exists()) {
                    Bitmap bitmap = decodeSampledBitmapFromFile(localPath, 512, 512);
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                        return;
                    }
                }
            }

            // Not downloaded yet - show placeholder and lazy-download in background
            imageView.setImageResource(item.getResourceIdPreview());
            lazyDownloadPreview(remoteFile);
            return;
        }

        // Fallback: usar drawable local con escalado para evitar OOM
        int resId = item.getResourceIdPreview();
        if (resId == 0) return;
        Bitmap bitmap = decodeSampledBitmapFromResource(resId, 512, 512);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            // Ultra fallback si todo falla
            imageView.setImageResource(resId);
        }
    }

    /**
     * Lazy-downloads a remote preview in background (only when scrolled into view).
     * When done, notifies adapter so the preview image refreshes.
     */
    private void lazyDownloadPreview(String remoteFile) {
        synchronized (downloadingPreviews) {
            if (downloadingPreviews.contains(remoteFile)) return;
            downloadingPreviews.add(remoteFile);
        }

        new Thread(() -> {
            try {
                ImageDownloadManager downloader = ImageDownloadManager.getInstance(context);
                boolean ok = downloader.downloadImageSync(remoteFile, null);
                if (ok) {
                    lazyHandler.post(() -> notifyDataSetChanged());
                }
            } catch (Exception e) {
                Log.w("WallpaperAdapter", "Preview download failed: " + remoteFile);
            } finally {
                synchronized (downloadingPreviews) {
                    downloadingPreviews.remove(remoteFile);
                }
            }
        }, "PreviewDL").start();
    }

    /**
     * Decodifica un bitmap desde recursos con tamaño reducido
     * Evita OutOfMemoryError en dispositivos con poca RAM
     */
    private Bitmap decodeSampledBitmapFromResource(int resId, int reqWidth, int reqHeight) {
        try {
            // Primero obtener dimensiones sin cargar el bitmap
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(context.getResources(), resId, options);

            // Calcular inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // Decodificar con el tamaño reducido
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565;  // 🔧 FIX OOM: 50% less RAM
            return BitmapFactory.decodeResource(context.getResources(), resId, options);
        } catch (Exception e) {
            Log.e("WallpaperAdapter", "Error decodificando recurso: " + e.getMessage());
            return null;
        }
    }

    /**
     * Decodifica un bitmap desde archivo con tamaño reducido
     */
    private Bitmap decodeSampledBitmapFromFile(String filePath, int reqWidth, int reqHeight) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(filePath, options);
        } catch (Exception e) {
            Log.e("WallpaperAdapter", "Error decodificando archivo: " + e.getMessage());
            return null;
        }
    }

    /**
     * Calcula el factor de escala óptimo para reducir memoria
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
