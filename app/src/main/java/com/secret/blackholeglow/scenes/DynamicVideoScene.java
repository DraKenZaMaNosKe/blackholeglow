package com.secret.blackholeglow.scenes;

import android.content.Context;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.systems.DynamicCatalog;

/**
 * DynamicVideoScene - Plays any video as wallpaper.
 * Created at runtime by SceneFactory for DYN_VID_* scene names.
 * Gets equalizer + clock + battery for free from BaseVideoScene.
 */
public class DynamicVideoScene extends BaseVideoScene {

    private String dynamicId = "";
    private String videoFileName = null;

    public DynamicVideoScene() {
        // No-arg constructor required by SceneFactory reflection
    }

    public void setDynamicId(String id, Context context) {
        this.dynamicId = id;
        DynamicCatalog.DynamicEntry entry = DynamicCatalog.get().getEntryById(id, context);
        if (entry != null) {
            this.videoFileName = entry.videoFile;
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
