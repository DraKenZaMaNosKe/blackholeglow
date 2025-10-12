package com.secret.blackholeglow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

/**
 * AvatarLoader - Descarga y carga el avatar del usuario para OpenGL
 *
 * Usa Glide para descargar la imagen de forma asíncrona
 * y la convierte a Bitmap para usarla como textura OpenGL
 */
public class AvatarLoader {
    private static final String TAG = "AvatarLoader";

    /**
     * Interfaz de callback para recibir el Bitmap cargado
     */
    public interface AvatarLoadListener {
        void onAvatarLoaded(Bitmap bitmap);
        void onAvatarLoadFailed();
    }

    /**
     * Carga el avatar del usuario desde URL
     *
     * @param context Contexto de la aplicación
     * @param photoUrl URL de la foto de perfil de Google
     * @param listener Callback para recibir el resultado
     */
    public static void loadAvatar(Context context, String photoUrl, final AvatarLoadListener listener) {
        if (photoUrl == null || photoUrl.isEmpty()) {
            Log.w(TAG, "⚠ photoUrl es nulo o vacío");
            if (listener != null) {
                listener.onAvatarLoadFailed();
            }
            return;
        }

        Log.d(TAG, "Descargando avatar desde: " + photoUrl);

        // Usar Glide para descargar y convertir a Bitmap
        Glide.with(context)
                .asBitmap()
                .load(photoUrl)
                .override(256, 256)  // Tamaño óptimo para textura
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        Log.d(TAG, "✓ Avatar descargado - " + resource.getWidth() + "x" + resource.getHeight());
                        if (listener != null) {
                            listener.onAvatarLoaded(resource);
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // No hacer nada
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        Log.e(TAG, "✗ Error descargando avatar");
                        if (listener != null) {
                            listener.onAvatarLoadFailed();
                        }
                    }
                });
    }

    /**
     * Carga el avatar del usuario actual desde UserManager
     *
     * @param context Contexto de la aplicación
     * @param listener Callback para recibir el resultado
     */
    public static void loadCurrentUserAvatar(Context context, final AvatarLoadListener listener) {
        UserManager userManager = UserManager.getInstance(context);

        if (!userManager.isLoggedIn()) {
            Log.w(TAG, "⚠ No hay usuario logueado");
            if (listener != null) {
                listener.onAvatarLoadFailed();
            }
            return;
        }

        String photoUrl = userManager.getUserPhotoUrl();
        loadAvatar(context, photoUrl, listener);
    }
}
