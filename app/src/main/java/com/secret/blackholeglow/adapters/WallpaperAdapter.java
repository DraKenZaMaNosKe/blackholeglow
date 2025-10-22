package com.secret.blackholeglow.adapters;

import android.content.Context;
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
import com.secret.blackholeglow.opengl.AnimatedBorderTextureView;
import com.secret.blackholeglow.activities.WallpaperPreviewActivity;

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
        this.context = context;
        this.wallpapers = wallpapers;
        this.listener = listener;
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

        // ✨ Asignar título y descripción
        holder.textTitle.setText(item.getNombre());
        holder.textDescription.setText(item.getDescripcion());

        // ╔═════════════════════════════════════════════════════════╗
        // ║  🎨 ASIGNAR GRADIENTE ÚNICO POR TEMA                   ║
        // ╚═════════════════════════════════════════════════════════╝
        int gradientResource;
        switch (position) {
            case 0: gradientResource = R.drawable.preview_space; break;
            case 1: gradientResource = R.drawable.preview_forest; break;
            case 2: gradientResource = R.drawable.preview_cyberpunk; break;
            case 3: gradientResource = R.drawable.preview_beach; break;
            case 4: gradientResource = R.drawable.preview_safari; break;
            case 5: gradientResource = R.drawable.preview_rain; break;
            case 6: gradientResource = R.drawable.preview_retro; break;
            case 7: gradientResource = R.drawable.preview_blackhole; break;
            case 8: gradientResource = R.drawable.preview_zen; break;
            case 9: gradientResource = R.drawable.preview_storm; break;
            default: gradientResource = R.drawable.preview_space; break;
        }
        holder.imagePreview.setImageResource(gradientResource);

        // ╔═════════════════════════════════════════════════════════╗
        // ║  🎯 BOTÓN "VER WALLPAPER" - Va a preview              ║
        // ╚═════════════════════════════════════════════════════════╝
        holder.buttonPreview.setOnClickListener(v -> {
            // Ir directamente a WallpaperPreviewActivity
            Intent intent = new Intent(context, com.secret.blackholeglow.activities.WallpaperPreviewActivity.class);
            // ✨ TODOS instalan el wallpaper "🌌 Viaje Espacial" por ahora
            intent.putExtra("WALLPAPER_PREVIEW_ID", R.drawable.universo03);
            intent.putExtra("WALLPAPER_ID", "🌌 Viaje Espacial");
            context.startActivity(intent);
        });
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
                intent.putExtra("WALLPAPER_ID", item.getNombre());


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
     * ║  📦 WallpaperViewHolder          ║
     * ╚═════════════════════════════════╝
     *  • Patrón ViewHolder: guarda referencias a vistas.
     *  • Optimiza rendimiento evitando findViewById repetido.
     *  • Layout fullscreen: preview + título + descripción + botón
     */
    public static class WallpaperViewHolder extends RecyclerView.ViewHolder {
        ImageView imagePreview;
        TextView textTitle;
        TextView textDescription;
        Button buttonPreview;

        public WallpaperViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePreview = itemView.findViewById(R.id.image_preview);
            textTitle = itemView.findViewById(R.id.text_title);
            textDescription = itemView.findViewById(R.id.text_description);
            buttonPreview = itemView.findViewById(R.id.button_preview);
        }
    }
}