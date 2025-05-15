package com.secret.blackholeglow.adapters;

import android.content.Context;
import android.util.Log;
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
import com.secret.blackholeglow.opengl.NeonBorderTextureView;

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

        // Información visual
        holder.textTitle.setText(item.getNombre());
        holder.textDescription.setText(item.getDescripcion());
        holder.imagePreview.setImageResource(item.getResourceIdPreview());

        holder.buttonApply.setOnClickListener(v -> listener.onApplyClicked(item));

        // NeonBorderTextureView: el shader se activa automáticamente
        NeonBorderTextureView neonView = holder.itemView.findViewById(R.id.neon_border_effect);
        if (neonView != null) {
            Log.d("WallpaperAdapter", "✨ Neon Border encontrado en posición: " + position);
            // No es necesario hacer nada más: el NeonBorderTextureView maneja el render animado solo
        } else {
            Log.w("WallpaperAdapter", "⚠️ NeonBorderTextureView NO encontrado en posición: " + holder.getAdapterPosition());
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
