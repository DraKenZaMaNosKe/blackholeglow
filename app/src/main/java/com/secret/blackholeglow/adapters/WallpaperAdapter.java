package com.secret.blackholeglow.adapters;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.models.WallpaperItem;
import com.secret.blackholeglow.opengl.AuraRendererThread;
import com.secret.blackholeglow.opengl.AuraTextureView;

import java.util.List;

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

        AuraTextureView auraView = holder.itemView.findViewById(R.id.aura_effect);
        if (auraView != null) {
            Log.d("WallpaperAdapter", "🎨 Aura encontrada en posición: " + position);

            if (auraView.isAvailable()) {
                Log.d("WallpaperAdapter", "🌟 SurfaceTexture ya disponible, iniciando render ahora");
                Surface surface = new Surface(auraView.getSurfaceTexture());
                AuraRendererThread thread = new AuraRendererThread(surface, auraView.getWidth(), auraView.getHeight(), context);
                thread.start();
            } else {
                auraView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
                        Log.d("WallpaperAdapter", "🌀 SurfaceTexture listener activado en posición: " + holder.getAdapterPosition());
                        Surface surface = new Surface(surfaceTexture);
                        AuraRendererThread thread = new AuraRendererThread(surface, width, height, context);
                        thread.start();
                    }

                    @Override public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {}
                    @Override public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) { return true; }
                    @Override public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {}
                });
            }
        } else {
            Log.w("WallpaperAdapter", "⚠️ AuraTextureView NO encontrada en posición: " + holder.getAdapterPosition());
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
