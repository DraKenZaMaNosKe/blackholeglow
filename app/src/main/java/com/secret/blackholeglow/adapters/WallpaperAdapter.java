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

        // ╔═════════════════════════════════════════════════════════╗
        // ║  🎨 ASIGNAR GRADIENTE ÚNICO POR TEMA                   ║
        // ╚═════════════════════════════════════════════════════════╝
        int gradientResource;
        switch (position) {
            case 0: gradientResource = R.drawable.preview_space; break;      // Espacio
            case 1: gradientResource = R.drawable.preview_forest; break;     // Bosque
            case 2: gradientResource = R.drawable.preview_cyberpunk; break;  // Cyberpunk
            case 3: gradientResource = R.drawable.preview_beach; break;      // Playa
            case 4: gradientResource = R.drawable.preview_safari; break;     // Safari
            case 5: gradientResource = R.drawable.preview_rain; break;       // Lluvia
            case 6: gradientResource = R.drawable.preview_retro; break;      // Retro
            case 7: gradientResource = R.drawable.preview_blackhole; break;  // Agujero Negro
            case 8: gradientResource = R.drawable.preview_zen; break;        // Zen
            case 9: gradientResource = R.drawable.preview_storm; break;      // Tormenta
            default: gradientResource = R.drawable.preview_space; break;     // Por defecto
        }
        holder.imagePreview.setImageResource(gradientResource);


        // listener común para mostrar diálogo de info
        View.OnClickListener showModal = v -> showInfoDialog(item);

        holder.itemView.setOnClickListener(showModal);


        // ╔═════════════════════════════════════════════════════════╗
        // ║  🎨 ASIGNAR SHADERS ÚNICOS POR TEMA                    ║
        // ║  Cada wallpaper tiene efectos OpenGL personalizados    ║
        // ╚═════════════════════════════════════════════════════════╝
        AnimatedBorderTextureView vista = holder.itemView.findViewById(R.id.border_effect);

        if (vista != null) {
            // ╔═════════════════════════════════════════════════════════╗
            // ║  ✨ MARCOS ANIMADOS CON u_Time - CADA UNO ES ÚNICO    ║
            // ║  Cada item tiene "vida propia" con efectos en tiempo   ║
            // ║  real que reflejan su tema                             ║
            // ╚═════════════════════════════════════════════════════════╝
            switch (position) {
                case 0: // 🌌 Viaje Espacial - Estrellas pulsantes en los bordes
                    vista.setShaderAssets(
                            "shaders/frame_vertex.glsl",
                            "shaders/frame_space_fragment.glsl"
                    );
                    break;

                case 1: // 🌲 Bosque Encantado - Luciérnagas flotando alrededor
                    vista.setShaderAssets(
                            "shaders/frame_vertex.glsl",
                            "shaders/frame_forest_fragment.glsl"
                    );
                    break;

                case 2: // 🏙️ Neo Tokyo 2099 - Neón corriendo por el marco
                    vista.setShaderAssets(
                            "shaders/frame_vertex.glsl",
                            "shaders/frame_cyberpunk_fragment.glsl"
                    );
                    break;

                case 3: // 🏖️ Paraíso Dorado - Rayos de luz (usar shader beam)
                    vista.setShaderAssets(
                            "shaders/beam_vertex.glsl",
                            "shaders/beam_fragment.glsl"
                    );
                    break;

                case 4: // 🦁 Safari Salvaje - Partículas de polvo
                    vista.setShaderAssets(
                            "shaders/particula_vertex.glsl",
                            "shaders/particula_fragment.glsl"
                    );
                    break;

                case 5: // 🌧️ Lluvia Mística - Gotas cayendo en los bordes
                    vista.setShaderAssets(
                            "shaders/frame_vertex.glsl",
                            "shaders/frame_rain_fragment.glsl"
                    );
                    break;

                case 6: // 🎮 Pixel Quest - Píxeles parpadeando (usar battery)
                    vista.setShaderAssets(
                            "shaders/battery_vertex.glsl",
                            "shaders/battery_fragment.glsl"
                    );
                    break;

                case 7: // 🕳️ Portal Infinito - Distorsión gravitacional
                    vista.setShaderAssets(
                            "shaders/forcefield_vertex.glsl",
                            "shaders/forcefield_fragment.glsl"
                    );
                    break;

                case 8: // 🌸 Jardín Zen - Ondas suaves (usar test_border)
                    vista.setShaderAssets(
                            "shaders/test_border_vertex.glsl",
                            "shaders/test_border_fragment.glsl"
                    );
                    break;

                case 9: // ⚡ Furia Celestial - Rayos eléctricos en los bordes
                    vista.setShaderAssets(
                            "shaders/frame_vertex.glsl",
                            "shaders/frame_storm_fragment.glsl"
                    );
                    break;

                default:
                    // Shader por defecto
                    vista.setShaderAssets(
                            "shaders/frame_vertex.glsl",
                            "shaders/frame_space_fragment.glsl"
                    );
                    break;
            }

            vista.setOnClickListener(showModal);
        }

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