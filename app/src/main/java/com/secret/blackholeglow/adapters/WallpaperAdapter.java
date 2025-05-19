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
import com.secret.blackholeglow.opengl.NeonBorderTextureView;
import com.secret.blackholeglow.activities.WallpaperPreviewActivity;

import java.util.List;

/**
 * Adaptador para la lista de wallpapers, usando un shader neón animado como marco.
 */
public class WallpaperAdapter extends RecyclerView.Adapter<WallpaperAdapter.WallpaperViewHolder> {

    private final List<WallpaperItem> wallpapers;
    private final Context context;
    private final OnWallpaperClickListener listener;

    public interface OnWallpaperClickListener {
        void onApplyClicked(WallpaperItem item);
    }

    public WallpaperAdapter(Context context, List<WallpaperItem> wallpapers, OnWallpaperClickListener listener) {
        this.context = context;
        this.wallpapers = wallpapers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WallpaperViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_wallpaper_card_textureview, parent, false);
        return new WallpaperViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WallpaperViewHolder holder, int position) {
        WallpaperItem item = wallpapers.get(position);

        holder.textTitle.setText(item.getNombre());
        holder.textDescription.setText(item.getDescripcion());
        holder.imagePreview.setImageResource(item.getResourceIdPreview());

        // Al tocar la tarjeta o el marco neón, muestra el modal
        View.OnClickListener showModal = v -> showInfoDialog(item);

        holder.itemView.setOnClickListener(showModal);

        NeonBorderTextureView neonView = holder.itemView.findViewById(R.id.neon_border_effect);
        if (neonView != null) neonView.setOnClickListener(showModal);

        // Al tocar el botón, animación y muestra el modal (no aplica directo)
        holder.buttonApply.setOnClickListener(v -> {
            // Animación bounce
            v.animate()
                    .scaleX(0.92f)
                    .scaleY(0.92f)
                    .setDuration(80)
                    .withEndAction(() -> {
                        v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start();
                    })
                    .start();

            // Vibración suave
            Vibrator vib = (Vibrator) v.getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vib != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vib.vibrate(android.os.VibrationEffect.createOneShot(40, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vib.vibrate(40);
                }
            }

            showInfoDialog(item);
        });
    }

    private void showInfoDialog(WallpaperItem item) {
        if (context instanceof FragmentActivity) {
            WallpaperInfoDialogFragment dialog = WallpaperInfoDialogFragment.newInstance(item);
            dialog.setOnApplyClickListener(() -> {
                Intent intent = new Intent(context, WallpaperPreviewActivity.class);
                intent.putExtra("WALLPAPER_PREVIEW_ID", item.getResourceIdPreview());
                context.startActivity(intent);

                if (listener != null) listener.onApplyClicked(item);
            });
            dialog.show(((FragmentActivity) context).getSupportFragmentManager(), "wallpaper_info");
        } else {
            Log.w("WallpaperAdapter", "Context NO es FragmentActivity, no se puede mostrar el modal.");
        }
    }

    @Override
    public int getItemCount() {
        return wallpapers.size();
    }

    public static class WallpaperViewHolder extends RecyclerView.ViewHolder {
        ImageView imagePreview;
        TextView textTitle;
        TextView textDescription;
        Button buttonApply;

        public WallpaperViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePreview = itemView.findViewById(R.id.image_preview);
            textTitle = itemView.findViewById(R.id.text_title);
            textDescription = itemView.findViewById(R.id.text_description);
            buttonApply = itemView.findViewById(R.id.button_apply);
        }
    }
}
