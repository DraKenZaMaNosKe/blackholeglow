package com.secret.blackholeglow.adapters;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.AnimationDrawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
/*
        // 🔥 Iniciar animaciones de fuego en los 4 bordes
        ImageView[] fires = {
                holder.fireTop, holder.fireBottom,
                holder.fireLeft, holder.fireRight
        };

        for (ImageView fire : fires) {
            if (fire != null && fire.getBackground() instanceof AnimationDrawable) {
                AnimationDrawable anim = (AnimationDrawable) fire.getBackground();
                fire.post(anim::start);
            }
        }
/*
        // 🔥 Animación de fuego
        AnimationDrawable anim = (AnimationDrawable) holder.fireAnim.getBackground();
        if (anim != null) {
            holder.fireAnim.post(anim::start);
        }*/

        // Información visual
        holder.textTitle.setText(item.getNombre());
        holder.textDescription.setText(item.getDescripcion());
        holder.imagePreview.setImageResource(item.getResourceIdPreview());

        holder.buttonApply.setOnClickListener(v -> listener.onApplyClicked(item));

// 1. Dimensiones en píxeles:
        int flameW = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 20, context.getResources().getDisplayMetrics());
        int flameH = flameW; // si el sprite es cuadrado; o usa 40dp×40dp

// 2. Rellena cada contenedor:
        populateFlames(holder.fireTopContainer,    true,  3);
        populateFlames(holder.fireBottomContainer, true,  1);
        populateFlames(holder.fireLeftContainer,   false, 1);
        populateFlames(holder.fireRightContainer,  false, 1);

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

    /**
     * Rellena el container con N llamas uniformes usando layout_weight.
     * @param container  el LinearLayout a rellenar
     * @param horizontal true si es borde superior/inferior, false si es izquierdo/derecho
     * @param flameCount cuántas llamas poner (p.ej. 4)
     */
    private void populateFlames(LinearLayout container, boolean horizontal, int flameCount) {
        container.removeAllViews();
        // Aseguramos que el container reparta en flameCount trozos
        container.setWeightSum(flameCount);

        for (int i = 0; i < flameCount; i++) {
            ImageView iv = new ImageView(context);
            LinearLayout.LayoutParams lp;
            if (horizontal) {
                // ancho 0 + weight=1 para que se reparta equitativamente
                lp = new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1f
                );
            } else {
                // altura 0 + weight=1
                lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                );
            }
            iv.setLayoutParams(lp);
            //iv.setBackgroundResource(R.drawable.fire_animation);
            iv.setImageResource(R.drawable.fire_animation);
            container.addView(iv);
            // Arrancamos la animación frame-list
           // AnimationDrawable anim = (AnimationDrawable) iv.getBackground();
            AnimationDrawable anim = (AnimationDrawable) iv.getDrawable();
            iv.post(anim::start);
        }
    }

    @Override
    public int getItemCount() {
        return wallpapers.size();
    }

    public static class WallpaperViewHolder extends RecyclerView.ViewHolder {
        LinearLayout fireTopContainer, fireBottomContainer, fireLeftContainer, fireRightContainer;

        ImageView imagePreview;

        ImageView fireTop;
        ImageView fireBottom;
        ImageView fireLeft;
        ImageView fireRight;

        TextView textTitle;
        TextView textDescription;
        Button buttonApply;

        public WallpaperViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePreview = itemView.findViewById(R.id.image_preview);
            fireTopContainer    = itemView.findViewById(R.id.fire_top_container);
            fireBottomContainer = itemView.findViewById(R.id.fire_bottom_container);
            fireLeftContainer   = itemView.findViewById(R.id.fire_left_container);
            fireRightContainer  = itemView.findViewById(R.id.fire_right_container);
            textTitle = itemView.findViewById(R.id.text_title);
            textDescription = itemView.findViewById(R.id.text_description);
            buttonApply = itemView.findViewById(R.id.button_apply);
        }
    }
}
