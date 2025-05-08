package com.secret.blackholeglow.adapters;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.models.WallpaperItem;
import java.util.List;

/**
 * Adaptador para mostrar una lista de fondos de pantalla animados en un RecyclerView.
 */
public class WallpaperAdapter extends RecyclerView.Adapter<WallpaperAdapter.WallpaperViewHolder> {

    private final List<WallpaperItem> wallpapers;
    private final Context context;
    private final OnWallpaperClickListener listener;

    /**
     * Interfaz para manejar clics en el botón de aplicar fondo.
     */
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
        View view = LayoutInflater.from(context).inflate(R.layout.item_wallpaper_card, parent, false);
        return new WallpaperViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WallpaperViewHolder holder, int position) {
        WallpaperItem item = wallpapers.get(position);
        holder.imagePreview.setImageResource(item.getResourceIdPreview());
        holder.textTitle.setText(item.getNombre());
        holder.textDescription.setText(item.getDescripcion());
        holder.buttonApply.setOnClickListener(v -> listener.onApplyClicked(item));
        View animatedFrame = holder.itemView.findViewById(R.id.animated_frame);
        if (animatedFrame.getBackground() instanceof AnimationDrawable) {
            AnimationDrawable anim = (AnimationDrawable) animatedFrame.getBackground();
            anim.setEnterFadeDuration(500);
            anim.setExitFadeDuration(500);
            anim.start();
        }
    }

    @Override
    public int getItemCount() {
        return wallpapers.size();
    }

    /**
     * ViewHolder que contiene la tarjeta del fondo de pantalla.
     */
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
