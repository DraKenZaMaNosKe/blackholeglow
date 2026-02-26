package com.secret.blackholeglow.systems;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.image.ImageConfig;
import com.secret.blackholeglow.models.SceneWeight;
import com.secret.blackholeglow.models.WallpaperCategory;
import com.secret.blackholeglow.models.WallpaperItem;
import com.secret.blackholeglow.models.WallpaperTier;
import com.secret.blackholeglow.video.VideoConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DynamicCatalog - Downloads and caches dynamic_catalog.json from Supabase.
 *
 * Allows adding new wallpapers (image or video) without code changes:
 * 1. Upload assets to Supabase
 * 2. Edit dynamic_catalog.json
 * 3. Wallpaper appears in app automatically
 */
public class DynamicCatalog {
    private static final String TAG = "DynamicCatalog";

    private static final String SUPABASE_IMAGES_URL =
        "https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/public/wallpaper-images/";
    private static final String SUPABASE_VIDEOS_URL =
        "https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/public/wallpaper-videos/";

    private static final String CATALOG_URL = SUPABASE_IMAGES_URL + "dynamic_catalog.json";
    private static final String PREFS_NAME = "dynamic_catalog_cache";
    private static final String KEY_JSON = "catalog_json";
    private static final String KEY_TIMESTAMP = "catalog_timestamp";
    private static final long CACHE_TTL_MS = 6L * 60 * 60 * 1000; // 6 hours

    private static DynamicCatalog instance;
    private volatile List<DynamicEntry> cachedEntries;
    private volatile boolean resourcesRegistered = false;

    public static synchronized DynamicCatalog get() {
        if (instance == null) {
            instance = new DynamicCatalog();
        }
        return instance;
    }

    public static class DynamicEntry {
        public final String id;
        public final String type; // "IMAGE" or "VIDEO"
        public final String name;
        public final String description;
        public final String imageFile;     // for IMAGE type
        public final String videoFile;     // for VIDEO type
        public final String previewFile;
        public final long imageSize;
        public final long videoSize;
        public final long previewSize;
        public final int glowColor;
        public final String badge;
        public final int sortOrder;
        public final WallpaperCategory category;

        DynamicEntry(JSONObject json) {
            this.id = json.optString("id", "");
            this.type = json.optString("type", "IMAGE");
            this.name = json.optString("name", "Dynamic");
            this.description = json.optString("description", "");
            this.imageFile = json.optString("imageFile", null);
            this.videoFile = json.optString("videoFile", null);
            this.previewFile = json.optString("previewFile", null);
            this.imageSize = json.optLong("imageSize", 0);
            this.videoSize = json.optLong("videoSize", 0);
            this.previewSize = json.optLong("previewSize", 0);
            this.badge = json.optString("badge", null);
            this.sortOrder = json.optInt("sortOrder", 99);
            this.category = WallpaperCategory.fromString(json.optString("category", "MISC"));

            // Parse hex color string like "#FF8800" or "#80FF8800" (ARGB)
            String colorStr = json.optString("glowColor", "#FFFFFF");
            int parsed = 0xFFFFFFFF;
            try {
                if (colorStr.startsWith("#")) {
                    String hex = colorStr.substring(1);
                    if (hex.length() == 8) {
                        // Already has alpha: #AARRGGBB
                        parsed = (int) Long.parseLong(hex, 16);
                    } else if (hex.length() == 6) {
                        // No alpha: #RRGGBB — prepend FF
                        parsed = (int) Long.parseLong("FF" + hex, 16);
                    }
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid glowColor: " + colorStr);
            }
            this.glowColor = parsed;
        }

        boolean isValid() {
            if (id.isEmpty()) return false;
            if (!"IMAGE".equals(type) && !"VIDEO".equals(type)) return false;
            if ("IMAGE".equals(type) && imageFile == null) return false;
            if ("VIDEO".equals(type) && videoFile == null) return false;
            return true;
        }

        /** Scene name used in SceneFactory / WallpaperPreferences */
        public String getSceneName() {
            return "VIDEO".equals(type) ? "DYN_VID_" + id : "DYN_IMG_" + id;
        }
    }

    /**
     * Refresh catalog from Supabase. Call from background thread.
     * @return true if new data was fetched
     */
    public boolean refresh(Context context) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(CATALOG_URL).openConnection();
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);
            conn.setRequestProperty("Cache-Control", "no-cache");

            int code = conn.getResponseCode();
            if (code != 200) {
                Log.w(TAG, "HTTP " + code + " fetching catalog");
                // Drain error stream to allow connection reuse
                drainStream(conn.getErrorStream());
                return false;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            String json = sb.toString();
            List<DynamicEntry> entries = parseJson(json);
            if (entries == null) return false;

            // Cache it
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                .putString(KEY_JSON, json)
                .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                .apply();

            cachedEntries = entries;
            resourcesRegistered = false;
            registerResources();

            Log.d(TAG, "Catalog refreshed: " + entries.size() + " dynamic wallpapers");
            return true;

        } catch (Exception e) {
            Log.w(TAG, "Error refreshing catalog: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static void drainStream(InputStream stream) {
        if (stream == null) return;
        try {
            byte[] buf = new byte[1024];
            while (stream.read(buf) != -1) { /* drain */ }
            stream.close();
        } catch (Exception ignored) {}
    }

    /**
     * Get cached entries (works offline). Returns empty list if never fetched.
     */
    public List<DynamicEntry> getCachedEntries(Context context) {
        if (cachedEntries != null) {
            ensureResourcesRegistered();
            return Collections.unmodifiableList(cachedEntries);
        }

        // Load from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_JSON, null);
        if (json == null) return new ArrayList<>();

        cachedEntries = parseJson(json);
        if (cachedEntries == null) {
            cachedEntries = new ArrayList<>();
        }
        ensureResourcesRegistered();
        return Collections.unmodifiableList(cachedEntries);
    }

    /**
     * Whether the cache is stale and needs refreshing.
     */
    public boolean isCacheStale(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long timestamp = prefs.getLong(KEY_TIMESTAMP, 0);
        return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
    }

    /**
     * Convert cached entries to WallpaperItem list for the catalog.
     */
    public List<WallpaperItem> toWallpaperItems(Context context) {
        List<DynamicEntry> entries = getCachedEntries(context);
        List<WallpaperItem> items = new ArrayList<>();

        for (DynamicEntry entry : entries) {
            items.add(new WallpaperItem.Builder(entry.name)
                .descripcion(entry.description)
                .preview(R.drawable.preview_placeholder)
                .sceneName(entry.getSceneName())
                .tier(WallpaperTier.FREE)
                .badge(entry.badge)
                .glow(entry.glowColor)
                .weight(SceneWeight.LIGHT)
                .remotePreview(entry.previewFile)
                .category(entry.category)
                .featured()
                .build());
        }
        return items;
    }

    /**
     * Look up a dynamic entry by its ID.
     * If cache is not loaded, auto-loads from SharedPreferences.
     */
    public DynamicEntry getEntryById(String id, Context context) {
        if (cachedEntries == null && context != null) {
            getCachedEntries(context);
        }
        return getEntryById(id);
    }

    /**
     * Look up a dynamic entry by its ID (cache must already be loaded).
     */
    public DynamicEntry getEntryById(String id) {
        if (id == null || cachedEntries == null) return null;
        for (DynamicEntry entry : cachedEntries) {
            if (id.equals(entry.id)) return entry;
        }
        return null;
    }

    // =========================================================================
    // INTERNAL
    // =========================================================================

    private List<DynamicEntry> parseJson(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            JSONObject root = new JSONObject(json);
            JSONArray arr = root.optJSONArray("wallpapers");
            if (arr == null) return null;

            List<DynamicEntry> entries = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj == null) {
                    Log.w(TAG, "Skipping null entry at index " + i);
                    continue;
                }
                try {
                    DynamicEntry entry = new DynamicEntry(obj);
                    if (entry.isValid()) {
                        entries.add(entry);
                    } else {
                        Log.w(TAG, "Skipping invalid entry: id='" + entry.id + "' type='" + entry.type + "'");
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Skipping malformed entry at index " + i + ": " + e.getMessage());
                }
            }
            return entries;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing catalog JSON: " + e.getMessage());
            return null;
        }
    }

    private void ensureResourcesRegistered() {
        if (!resourcesRegistered) {
            registerResources();
        }
    }

    /**
     * Register dynamic image/video URLs into ImageConfig/VideoConfig
     * so the download managers know where to fetch them.
     */
    private void registerResources() {
        if (cachedEntries == null) return;

        for (DynamicEntry entry : cachedEntries) {
            // Register preview image
            if (entry.previewFile != null) {
                ImageConfig.registerDynamic(
                    entry.previewFile,
                    SUPABASE_IMAGES_URL + entry.previewFile,
                    entry.previewSize > 0 ? entry.previewSize : 100_000
                );
            }

            if ("VIDEO".equals(entry.type) && entry.videoFile != null) {
                VideoConfig.registerDynamic(
                    entry.videoFile,
                    SUPABASE_VIDEOS_URL + entry.videoFile,
                    entry.videoSize > 0 ? entry.videoSize : 5_000_000
                );
            } else if ("IMAGE".equals(entry.type) && entry.imageFile != null) {
                ImageConfig.registerDynamic(
                    entry.imageFile,
                    SUPABASE_IMAGES_URL + entry.imageFile,
                    entry.imageSize > 0 ? entry.imageSize : 500_000
                );
            }
        }
        resourcesRegistered = true;
        Log.d(TAG, "Dynamic resources registered: " + cachedEntries.size() + " entries");
    }

    /** Reset singleton (for testing) */
    public static synchronized void reset() {
        instance = null;
    }
}
