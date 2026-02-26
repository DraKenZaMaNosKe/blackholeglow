package com.secret.blackholeglow.scenes;

import android.content.Context;
import android.util.Log;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.systems.DynamicCatalog;

/**
 * DynamicImageScene - Renders any single image as wallpaper.
 * Created at runtime by SceneFactory for DYN_IMG_* scene names.
 * Gets equalizer + clock + battery for free from BaseParallaxScene.
 */
public class DynamicImageScene extends BaseParallaxScene {
    private static final String TAG = "DynamicImageScene";

    private String dynamicId = "";
    private String imageFile = null;

    public DynamicImageScene() {
        // No-arg constructor required by SceneFactory reflection
    }

    public void setDynamicId(String id, Context context) {
        if (id == null || id.isEmpty()) {
            Log.e(TAG, "setDynamicId called with null/empty id");
            return;
        }
        this.dynamicId = id;
        DynamicCatalog.DynamicEntry entry = DynamicCatalog.get().getEntryById(id, context);
        if (entry != null) {
            this.imageFile = entry.imageFile;
            if (this.imageFile == null) {
                Log.e(TAG, "Entry '" + id + "' has no imageFile defined");
            }
        } else {
            Log.w(TAG, "No catalog entry found for dynamic id: " + id);
        }
    }

    @Override
    public String getName() {
        return "DYN_IMG_" + dynamicId;
    }

    @Override
    public String getDescription() {
        return "Dynamic Image: " + dynamicId;
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_placeholder;
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.DEFAULT;
    }

    @Override
    protected ParallaxLayer[] getLayers() {
        if (imageFile == null) {
            return new ParallaxLayer[0];
        }
        // Single fullscreen layer, no depth map, cover mode
        return new ParallaxLayer[] {
            new ParallaxLayer(imageFile, null, 0f, 1.0f, true)
        };
    }
}
