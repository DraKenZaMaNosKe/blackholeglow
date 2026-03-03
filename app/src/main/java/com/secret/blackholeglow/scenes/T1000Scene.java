package com.secret.blackholeglow.scenes;

import android.opengl.GLES30;
import android.util.Log;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.ShaderUtils;

/**
 * T-1000 Terminator scene with liquid metal shader effect.
 * Uses custom GLSL shaders for chrome distortion, eye glow,
 * mercury drops, and water ripple effects over the base image.
 */
public class T1000Scene extends BaseParallaxScene {

    private static final String TAG = "T1000Scene";

    // Custom shader
    private int t1000Program = -1;
    private int t1000PosLoc;
    private int t1000TexCoordLoc;
    private int t1000TimeLoc;
    private int t1000ResolutionLoc;
    private int t1000TextureLoc;
    private float elapsedTime = 0f;

    @Override
    public String getName() {
        return "T1000";
    }

    @Override
    public String getDescription() {
        return "T-1000 Liquid Metal";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_t1000;
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.ABYSSIA;
    }

    @Override
    protected ParallaxLayer[] getLayers() {
        return new ParallaxLayer[] {
            new ParallaxLayer("dyn_t1000.webp", null, 0f, 1.0f, true)
        };
    }

    @Override
    protected float getGyroSensitivity() {
        return 0f;
    }

    @Override
    protected void setupSceneSpecific() {
        // Compile custom T1000 shader from assets
        t1000Program = ShaderUtils.createProgramFromAssets(
                context, "shaders/gl3/t1000_vertex.glsl", "shaders/gl3/t1000_fragment.glsl");

        if (t1000Program > 0) {
            t1000PosLoc = GLES30.glGetAttribLocation(t1000Program, "a_Position");
            t1000TexCoordLoc = GLES30.glGetAttribLocation(t1000Program, "a_TexCoord");
            t1000TimeLoc = GLES30.glGetUniformLocation(t1000Program, "u_Time");
            t1000ResolutionLoc = GLES30.glGetUniformLocation(t1000Program, "u_Resolution");
            t1000TextureLoc = GLES30.glGetUniformLocation(t1000Program, "u_Texture");
            Log.d(TAG, "✅ T1000 shader compiled (program=" + t1000Program + ")");
        } else {
            Log.e(TAG, "❌ Failed to compile T1000 shader");
        }
    }

    @Override
    protected void updateSceneSpecific(float deltaTime) {
        elapsedTime += deltaTime;
        // Wrap at ~1 hour to prevent float precision loss in shader sin()/fract()
        if (elapsedTime > 3600f) elapsedTime -= 3600f;
    }

    @Override
    protected void drawLayer(ParallaxLayer layer) {
        if (t1000Program <= 0) {
            // Fallback to default rendering if shader failed
            super.drawLayer(layer);
            return;
        }

        GLES30.glUseProgram(t1000Program);

        // Uniforms
        GLES30.glUniform1f(t1000TimeLoc, elapsedTime);
        GLES30.glUniform2f(t1000ResolutionLoc, (float) screenWidth, (float) screenHeight);

        // Texture
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, layer.colorTextureId);
        GLES30.glUniform1i(t1000TextureLoc, 0);

        // Vertices
        GLES30.glEnableVertexAttribArray(t1000PosLoc);
        GLES30.glVertexAttribPointer(t1000PosLoc, 2, GLES30.GL_FLOAT, false, 0, quadVertexBuffer);

        // UV coords (use cover mode for full screen)
        java.nio.FloatBuffer uvBuffer = layer.useCoverMode ? quadTexCoordBufferCover : quadTexCoordBuffer;
        GLES30.glEnableVertexAttribArray(t1000TexCoordLoc);
        GLES30.glVertexAttribPointer(t1000TexCoordLoc, 2, GLES30.GL_FLOAT, false, 0, uvBuffer);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(t1000PosLoc);
        GLES30.glDisableVertexAttribArray(t1000TexCoordLoc);
    }

    @Override
    protected void releaseSceneSpecificResources() {
        if (t1000Program > 0) {
            GLES30.glDeleteProgram(t1000Program);
            t1000Program = -1;
            Log.d(TAG, "🗑️ T1000 shader released");
        }
    }
}
