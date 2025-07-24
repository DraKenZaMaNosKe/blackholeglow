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
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_wallpaper_card_textureview, parent, false);
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

        holder.textTitle.setText(item.getNombre());
        holder.textDescription.setText(item.getDescripcion());
        holder.imagePreview.setImageResource(item.getResourceIdPreview());


        // listener común para mostrar diálogo de info
        View.OnClickListener showModal = v -> showInfoDialog(item);

        holder.itemView.setOnClickListener(showModal);


        AnimatedBorderTextureView vista = holder.itemView.findViewById(R.id.border_effect);


        if (position  == 0) {
            vista.setShaderAssets(
                    "shaders/test_efecto_vertex.glsl",
                    "shaders/test_efecto_fragment.glsl"
            );
        }

        if (vista != null) vista.setOnClickListener(showModal);

        // animación "bounce" + vibración + diálogo info
        holder.buttonApply.setOnClickListener(v -> {
            v.animate().scaleX(0.92f).scaleY(0.92f)
                    .setDuration(80)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f)
                            .setDuration(100).start())
                    .start();
            Vibrator vib = (Vibrator) v.getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vib != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vib.vibrate(android.os.VibrationEffect.createOneShot(40,
                            android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vib.vibrate(40);
                }
            }
            showInfoDialog(item);
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
     */
    public static class WallpaperViewHolder extends RecyclerView.ViewHolder {
        ImageView imagePreview;
        TextView textTitle;
        TextView textDescription;
        Button buttonApply;

        public WallpaperViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePreview   = itemView.findViewById(R.id.image_preview);
            textTitle      = itemView.findViewById(R.id.text_title);
            textDescription= itemView.findViewById(R.id.text_description);
            buttonApply    = itemView.findViewById(R.id.button_apply);
        }
    }
}