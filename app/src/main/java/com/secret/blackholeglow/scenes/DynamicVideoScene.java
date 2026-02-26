package com.secret.blackholeglow.scenes;

import android.content.Context;
import android.util.Log;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.systems.DynamicCatalog;

/**
 * DynamicVideoScene - Plays any video as wallpaper.
 * Created at runtime by SceneFactory for DYN_VID_* scene names.
 * Gets equalizer + clock + battery for free from BaseVideoScene.
 */
public class DynamicVideoScene extends BaseVideoScene {
    private static final String TAG = "DynamicVideoScene";

    private String dynamicId = "";
    private String videoFileName = null;

    public DynamicVideoScene() {
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
            this.videoFileName = entry.videoFile;
            if (this.videoFileName == null) {
                Log.e(TAG, "Entry '" + id + "' has no videoFile defined");
            }
        } else {
            Log.w(TAG, "No catalog entry found for dynamic id: " + id);
        }
    }

    @Override
    public String getName() {
        return "DYN_VID_" + dynamicId;
    }

    @Override
    public String getDescription() {
        return "Dynamic Video: " + dynamicId;
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_placeholder;
    }

    @Override
    protected String getVideoFileName() {
        return videoFileName;
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.DEFAULT;
    }
}
