package com.secret.blackholeglow.adapters;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import android.graphics.BitmapFactory;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import java.util.List;

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
    // ║  (grimoire.obj, texturas) - Video eliminado en v5.0.7           ║
    // ╚═════════════════════════════════════════════════════════════════╝
    private boolean isPanelResourcesReady = false;
    private int downloadProgress = 0;
    private boolean downloadFailed = false;  // ⚠️ Estado de error en descarga

    // ⚡ DEBOUNCE: Prevenir doble-click en botones
    private long lastClickTime = 0;
    private static final long CLICK_DEBOUNCE_MS = 800; // 800ms entre clicks

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
     * @param isPanelResourcesReady true si los recursos del panel están listos (grimoire, texturas)
     */
    public WallpaperAdapter(Context context, List<WallpaperItem> wallpapers,
                            OnWallpaperClickListener listener, boolean isPanelResourcesReady) {
        this.context = context;
        this.wallpapers = wallpapers;
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

        // ✨ Asignar título con gradiente épico
        holder.textTitle.setText(item.getNombre());

        // Crear gradiente basado en el color glow del wallpaper
        int glowColor = item.getGlowColor();
        int lighterColor = lightenColor(glowColor, 0.4f);
        holder.textTitle.setGradientColors(Color.WHITE, lighterColor, glowColor, lighterColor, Color.WHITE);

        holder.textDescription.setText(item.getDescripcion());

        // ╔═════════════════════════════════════════════════════════╗
        // ║  🎨 ASIGNAR IMAGEN DE PREVIEW DESDE WallpaperItem      ║
        // ╚═════════════════════════════════════════════════════════╝
        loadPreviewImage(holder.imagePreview, item);

        // ╔═════════════════════════════════════════════════════════╗
        // ║  🏷️ BADGE - Mostrar etiqueta si tiene                 ║
        // ╚═════════════════════════════════════════════════════════╝
        if (item.hasBadge() && holder.textBadge != null) {
            holder.textBadge.setText(item.getBadge());
            holder.textBadge.setVisibility(View.VISIBLE);
        } else if (holder.textBadge != null) {
            holder.textBadge.setVisibility(View.GONE);
        }

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
        // ║  no están descargados (grimoire.obj, texturas)        ║
        // ╚═════════════════════════════════════════════════════════╝

        // ⚠️ ESTADO DE ERROR: Mostrar botón de reintento
        if (downloadFailed) {
            holder.buttonPreview.setEnabled(true);
            holder.buttonPreview.setAlpha(1.0f);
            holder.buttonPreview.setText("⚠️ REINTENTAR");
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
        // PRIMERO: Verificar si los recursos del panel están listos
        else if (!isPanelResourcesReady) {
            // Recursos del panel NO disponibles - botón deshabilitado con progreso
            holder.buttonPreview.setEnabled(false);
            holder.buttonPreview.setAlpha(0.6f);
            if (downloadProgress > 0 && downloadProgress < 100) {
                holder.buttonPreview.setText("📥 Preparando " + downloadProgress + "%");
            } else {
                holder.buttonPreview.setText("📥 Preparando...");
            }
            holder.buttonPreview.setOnClickListener(null);
        }
        // SEGUNDO: Verificar si el wallpaper está disponible
        else if (item.isAvailable()) {
            // Wallpaper disponible Y recursos del panel listos - botón habilitado
            holder.buttonPreview.setEnabled(true);
            holder.buttonPreview.setAlpha(1.0f);
            holder.buttonPreview.setText("✨ VER WALLPAPER");
            holder.buttonPreview.setOnClickListener(v -> {
                // 🧹 LIMPIEZA AUTOMÁTICA: Limpiar wallpaper anterior antes de cargar nuevo
                // Esto evita conflictos de estado entre escenas (ej: Christmas → Arcade)
                clearCurrentWallpaperAsync(() -> {
                    // ✅ Guardar preferencia INMEDIATAMENTE al seleccionar
                    WallpaperPreferences.getInstance(context).setSelectedWallpaper(item.getSceneName());
                    Log.d("WallpaperAdapter", "💾 Wallpaper seleccionado: " + item.getSceneName());

                    // Ir a WallpaperLoadingActivity para precarga de recursos
                    Intent intent = new Intent(context, com.secret.blackholeglow.activities.WallpaperLoadingActivity.class);

                    // Pasar datos del wallpaper (sceneName para SceneFactory)
                    intent.putExtra("WALLPAPER_PREVIEW_ID", item.getResourceIdPreview());
                    intent.putExtra("WALLPAPER_ID", item.getSceneName());  // Nombre interno para SceneFactory
                    intent.putExtra("WALLPAPER_DISPLAY_NAME", item.getNombre());  // Nombre bonito para UI
                    context.startActivity(intent);
                });
            });
        } else {
            // Wallpaper NO disponible - botón deshabilitado con texto especial
            holder.buttonPreview.setEnabled(false);
            holder.buttonPreview.setAlpha(0.6f);
            holder.buttonPreview.setText("🔒 PRÓXIMAMENTE");
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
            // Con payload = actualización parcial (solo botón)
            WallpaperItem item = wallpapers.get(position);
            updateButtonState(holder, item);
        }
    }

    /**
     * Actualiza solo el estado del botón (para actualizaciones parciales).
     */
    private void updateButtonState(WallpaperViewHolder holder, WallpaperItem item) {
        // ⚠️ ESTADO DE ERROR: Mostrar botón de reintento
        if (downloadFailed) {
            holder.buttonPreview.setEnabled(true);  // Habilitado para permitir retry
            holder.buttonPreview.setAlpha(1.0f);
            holder.buttonPreview.setText("⚠️ REINTENTAR");
            holder.buttonPreview.setOnClickListener(v -> {
                // Notificar al fragment para reintentar
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
        // ESTADO NORMAL: Descargando
        else if (!isPanelResourcesReady) {
            holder.buttonPreview.setEnabled(false);
            holder.buttonPreview.setAlpha(0.6f);
            holder.buttonPreview.setOnClickListener(null);
            if (downloadProgress > 0 && downloadProgress < 100) {
                holder.buttonPreview.setText("📥 Preparando " + downloadProgress + "%");
            } else {
                holder.buttonPreview.setText("📥 Preparando...");
            }
        } else if (item.isAvailable()) {
            holder.buttonPreview.setEnabled(true);
            holder.buttonPreview.setAlpha(1.0f);
            holder.buttonPreview.setText("✨ VER WALLPAPER");
            // ⚡ FIX: Asignar click listener en actualización parcial
            holder.buttonPreview.setOnClickListener(v -> {
                clearCurrentWallpaperAsync(() -> {
                    WallpaperPreferences.getInstance(context).setSelectedWallpaper(item.getSceneName());
                    Log.d("WallpaperAdapter", "💾 Wallpaper seleccionado: " + item.getSceneName());
                    Intent intent = new Intent(context, com.secret.blackholeglow.activities.WallpaperLoadingActivity.class);
                    intent.putExtra("WALLPAPER_PREVIEW_ID", item.getResourceIdPreview());
                    intent.putExtra("WALLPAPER_ID", item.getSceneName());
                    intent.putExtra("WALLPAPER_DISPLAY_NAME", item.getNombre());
                    context.startActivity(intent);
                });
            });
        } else {
            holder.buttonPreview.setEnabled(false);
            holder.buttonPreview.setAlpha(0.6f);
            holder.buttonPreview.setOnClickListener(null);
            holder.buttonPreview.setText("🔒 PRÓXIMAMENTE");
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
    @Override
    public void onViewAttachedToWindow(@NonNull WallpaperViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        // Reanudar animaciones cuando la vista vuelve a pantalla
        if (holder.animatedBorder != null) {
            holder.animatedBorder.resumeAnimation();
        }
        if (holder.buttonPreview != null) {
            holder.buttonPreview.resumeAnimation();
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull WallpaperViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        // Pausar animaciones cuando la vista sale de pantalla
        if (holder.animatedBorder != null) {
            holder.animatedBorder.pauseAnimation();
        }
        if (holder.buttonPreview != null) {
            holder.buttonPreview.pauseAnimation();
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
        com.secret.blackholeglow.ui.AnimatedGlowButton buttonPreview;
        com.secret.blackholeglow.ui.AnimatedGlowCard animatedBorder;

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
    // ║  🧹 LIMPIEZA AUTOMÁTICA DE WALLPAPER                           ║
    // ╠═════════════════════════════════════════════════════════════════╣
    // ║  Limpia el wallpaper actual antes de cargar uno nuevo.          ║
    // ║  Esto fuerza a Android a destruir el LiveWallpaperService       ║
    // ║  anterior y evita conflictos de estado entre escenas.           ║
    // ╚═════════════════════════════════════════════════════════════════╝

    /**
     * Limpia el wallpaper actual de forma asíncrona y ejecuta el callback al terminar.
     * Esto evita el bug de conflicto de estado al cambiar entre escenas.
     *
     * @param onComplete Callback a ejecutar después de la limpieza
     */
    private void clearCurrentWallpaperAsync(Runnable onComplete) {
        new Thread(() -> {
            try {
                WallpaperManager wm = WallpaperManager.getInstance(context);

                // Verificar si hay un live wallpaper activo de nuestra app
                android.app.WallpaperInfo info = wm.getWallpaperInfo();
                if (info != null && info.getPackageName().equals(context.getPackageName())) {
                    Log.d("WallpaperAdapter", "🧹 Limpiando wallpaper anterior: " + info.getServiceName());

                    // Crear un bitmap negro temporal para "resetear" el wallpaper
                    Bitmap blackBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                    blackBitmap.eraseColor(Color.BLACK);
                    wm.setBitmap(blackBitmap);
                    blackBitmap.recycle();

                    // Pequeña pausa para que Android destruya el servicio anterior
                    Thread.sleep(200);

                    Log.d("WallpaperAdapter", "✅ Wallpaper anterior limpiado correctamente");
                } else {
                    Log.d("WallpaperAdapter", "ℹ️ No hay live wallpaper activo de nuestra app, continuando...");
                }
            } catch (Exception e) {
                Log.w("WallpaperAdapter", "⚠️ Error limpiando wallpaper (no crítico): " + e.getMessage());
                // No es crítico, continuamos de todos modos
            }

            // Ejecutar callback en el UI thread
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(onComplete);
            } else {
                // Fallback: ejecutar directamente (podría causar problemas de UI)
                onComplete.run();
            }
        }).start();
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

    /**
     * Carga la imagen de preview del wallpaper.
     * Si tiene preview remoto descargado, lo usa. Sino, usa drawable local.
     * OPTIMIZADO: Escala imágenes grandes para evitar OutOfMemoryError
     */
    private void loadPreviewImage(ImageView imageView, WallpaperItem item) {
        String remoteFile = REMOTE_PREVIEWS.get(item.getSceneName());

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
                        Log.d("WallpaperAdapter", "📥 Preview remoto cargado: " + remoteFile);
                        return;
                    }
                }
            }
        }

        // Fallback: usar drawable local con escalado para evitar OOM
        Bitmap bitmap = decodeSampledBitmapFromResource(item.getResourceIdPreview(), 512, 512);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            // Ultra fallback si todo falla
            imageView.setImageResource(item.getResourceIdPreview());
        }
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
