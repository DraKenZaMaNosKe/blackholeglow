package com.secret.blackholeglow.christmas;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║   🎁 PhotoGalleryManager - Gestor de Fotos de Galería                     ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║   - Obtiene fotos aleatorias del dispositivo                              ║
 * ║   - Maneja permisos de Android 13+ y versiones anteriores                 ║
 * ║   - Carga y redimensiona bitmaps para OpenGL                              ║
 * ║   - Corrige orientación EXIF automáticamente                              ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */
public class PhotoGalleryManager {
    private static final String TAG = "PhotoGalleryManager";

    private static final int MAX_TEXTURE_SIZE = 1024;  // Máximo para OpenGL
    private static final int MIN_PHOTOS_TO_CACHE = 10; // Mínimo de fotos para cachear URIs

    private final Context context;
    private final Random random = new Random();
    private List<Uri> cachedPhotoUris = null;
    private long lastCacheTime = 0;
    private static final long CACHE_VALIDITY_MS = 60000; // 1 minuto

    public PhotoGalleryManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Verifica si tenemos permiso para leer fotos
     */
    public boolean hasGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 12 y anteriores
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Obtiene el permiso necesario según la versión de Android
     */
    public String getRequiredPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            return Manifest.permission.READ_EXTERNAL_STORAGE;
        }
    }

    /**
     * Obtiene una foto aleatoria de la galería como Bitmap
     * @return Bitmap redimensionado para OpenGL, o null si no hay fotos/permisos
     */
    public Bitmap getRandomPhoto() {
        if (!hasGalleryPermission()) {
            Log.w(TAG, "🚫 No hay permiso para leer galería");
            return null;
        }

        try {
            Uri randomUri = getRandomPhotoUri();
            if (randomUri == null) {
                Log.w(TAG, "📷 No se encontraron fotos en la galería");
                return null;
            }

            return loadAndProcessBitmap(randomUri);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error obteniendo foto aleatoria: " + e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene una URI de foto aleatoria
     */
    private Uri getRandomPhotoUri() {
        // Usar cache si es válido
        long now = System.currentTimeMillis();
        if (cachedPhotoUris != null && !cachedPhotoUris.isEmpty()
                && (now - lastCacheTime) < CACHE_VALIDITY_MS) {
            return cachedPhotoUris.get(random.nextInt(cachedPhotoUris.size()));
        }

        // Obtener lista de fotos
        List<Uri> photoUris = getAllPhotoUris();
        if (photoUris.isEmpty()) {
            return null;
        }

        // Cachear si hay suficientes fotos
        if (photoUris.size() >= MIN_PHOTOS_TO_CACHE) {
            cachedPhotoUris = photoUris;
            lastCacheTime = now;
        }

        return photoUris.get(random.nextInt(photoUris.size()));
    }

    /**
     * Obtiene todas las URIs de fotos de la galería
     */
    private List<Uri> getAllPhotoUris() {
        List<Uri> uris = new ArrayList<>();
        final int MAX_PHOTOS = 500;

        Uri collection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        String[] projection = {MediaStore.Images.Media._ID};

        // Ordenar por fecha, más recientes primero (sin LIMIT en sortOrder - no compatible con Android 10+)
        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

        Cursor cursor = null;
        try {
            // Android 11+ (API 30): usar Bundle para LIMIT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.os.Bundle queryArgs = new android.os.Bundle();
                queryArgs.putString(android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder);
                queryArgs.putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, MAX_PHOTOS);
                cursor = context.getContentResolver().query(collection, projection, queryArgs, null);
            } else {
                // Android 10 y anteriores: query normal sin LIMIT
                cursor = context.getContentResolver().query(collection, projection, null, null, sortOrder);
            }

            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int count = 0;

                while (cursor.moveToNext() && count < MAX_PHOTOS) {
                    long id = cursor.getLong(idColumn);
                    Uri contentUri = Uri.withAppendedPath(collection, String.valueOf(id));
                    uris.add(contentUri);
                    count++;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error consultando galería: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Log.d(TAG, "📷 Encontradas " + uris.size() + " fotos en galería");
        return uris;
    }

    /**
     * Carga y procesa un bitmap desde URI
     */
    private Bitmap loadAndProcessBitmap(Uri uri) {
        try {
            ContentResolver resolver = context.getContentResolver();

            // Primero obtener dimensiones sin cargar el bitmap
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            try (InputStream is = resolver.openInputStream(uri)) {
                BitmapFactory.decodeStream(is, null, options);
            }

            // Calcular sample size para redimensionar
            options.inSampleSize = calculateInSampleSize(options, MAX_TEXTURE_SIZE, MAX_TEXTURE_SIZE);
            options.inJustDecodeBounds = false;

            // Cargar bitmap redimensionado
            Bitmap bitmap;
            try (InputStream is = resolver.openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(is, null, options);
            }

            if (bitmap == null) {
                Log.e(TAG, "❌ No se pudo decodificar imagen");
                return null;
            }

            // Corregir orientación EXIF
            bitmap = correctOrientation(resolver, uri, bitmap);

            // Asegurar que sea cuadrado (o al menos con aspect ratio razonable)
            bitmap = cropToSquare(bitmap);

            Log.d(TAG, "✅ Foto cargada: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            return bitmap;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error cargando bitmap: " + e.getMessage());
            return null;
        }
    }

    /**
     * Calcula el tamaño de muestra para redimensionar
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Corrige la orientación según datos EXIF
     */
    private Bitmap correctOrientation(ContentResolver resolver, Uri uri, Bitmap bitmap) {
        try {
            InputStream is = resolver.openInputStream(uri);
            if (is == null) return bitmap;

            ExifInterface exif = new ExifInterface(is);
            is.close();

            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.postScale(1, -1);
                    break;
                default:
                    return bitmap;
            }

            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            if (rotated != bitmap) {
                bitmap.recycle();
            }

            return rotated;
        } catch (Exception e) {
            Log.w(TAG, "⚠️ No se pudo corregir orientación: " + e.getMessage());
            return bitmap;
        }
    }

    /**
     * Recorta el bitmap a un cuadrado centrado
     */
    private Bitmap cropToSquare(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width == height) return bitmap;

        int size = Math.min(width, height);
        int x = (width - size) / 2;
        int y = (height - size) / 2;

        Bitmap cropped = Bitmap.createBitmap(bitmap, x, y, size, size);

        if (cropped != bitmap) {
            bitmap.recycle();
        }

        return cropped;
    }

    /**
     * Limpia el cache de URIs
     */
    public void clearCache() {
        cachedPhotoUris = null;
        lastCacheTime = 0;
    }
}
