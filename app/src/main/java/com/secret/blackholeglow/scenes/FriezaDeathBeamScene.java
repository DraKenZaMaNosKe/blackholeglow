package com.secret.blackholeglow.scenes;

import android.opengl.GLES20;
import android.util.Log;
import android.view.MotionEvent;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.video.DeathBeamFX;
import com.secret.blackholeglow.video.Frieza3D;
import com.secret.blackholeglow.video.FriezaBackground;

/**
 * FriezaDeathBeamScene - Frieza Final Form disparando Death Beam.
 *
 * ╔══════════════════════════════════════════════════════════════╗
 * ║              CALIBRATION MODE (EDIT_MODE = true)             ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  TAP mitad superior  = siguiente propiedad                  ║
 * ║  TAP mitad inferior  = propiedad anterior                   ║
 * ║  DOBLE TAP           = cambiar objeto (FRIEZA / DEATHBEAM)  ║
 * ║  DRAG arriba/abajo   = ajustar valor                        ║
 * ║  LogCat tag: "FZ_CALIB" para filtrar facilmente              ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
public class FriezaDeathBeamScene extends BaseVideoScene {
    private static final String TAG = "FriezaDeathBeam";
    private static final String CALIB_TAG = "FZ_CALIB";

    // Components
    private Frieza3D frieza;
    private DeathBeamFX deathBeam;
    // Background disabled during calibration
    // private FriezaBackground background;

    // ═══════════════════════════════════════════════════════════════
    // CALIBRATION SYSTEM - Dual Object
    // ═══════════════════════════════════════════════════════════════
    private static final boolean EDIT_MODE = true;

    // Current object: 0 = FRIEZA, 1 = DEATHBEAM
    // Empieza en DEATHBEAM (Frieza ya calibrado)
    private int editObject = 1;
    private static final String[] OBJECT_NAMES = {"FRIEZA", "DEATHBEAM"};

    // ── FRIEZA properties (7 modes) ──
    private int friezaMode = 0;
    private static final int FZ_MODES = 7;
    private static final String[] FZ_LABEL = {
        "FZ POS X  (horizontal)",
        "FZ POS Y  (vertical)",
        "FZ POS Z  (profundidad)",
        "FZ ROT X  (pitch)",
        "FZ ROT Y  (yaw)",
        "FZ ROT Z  (roll)",
        "FZ SCALE  (tamano)"
    };
    private static final String[] FZ_KEY = {
        "FZ_POS_X", "FZ_POS_Y", "FZ_POS_Z",
        "FZ_ROT_X", "FZ_ROT_Y", "FZ_ROT_Z", "FZ_SCALE"
    };
    private static final float[] FZ_SENS = {
        0.003f, 0.003f, 0.003f,   // pos
        0.4f, 0.4f, 0.4f,         // rot (degrees)
        0.001f                      // scale
    };

    // ── DEATHBEAM properties (9 modes) ──
    private int dbMode = 0;
    private static final int DB_MODES = 9;
    private static final String[] DB_LABEL = {
        "DB POS X  (horizontal)",
        "DB POS Y  (vertical)",
        "DB POS Z  (profundidad)",
        "DB DIR X  (beam dir X)",
        "DB DIR Y  (beam dir Y)",
        "DB DIR Z  (beam dir Z)",
        "DB SPHERE SCALE",
        "DB BEAM LENGTH",
        "DB BEAM RADIUS"
    };
    private static final String[] DB_KEY = {
        "DB_POS_X", "DB_POS_Y", "DB_POS_Z",
        "DB_DIR_X", "DB_DIR_Y", "DB_DIR_Z",
        "DB_SPHERE_SCALE", "DB_BEAM_LEN", "DB_BEAM_RAD"
    };
    private static final float[] DB_SENS = {
        0.003f, 0.003f, 0.003f,   // pos
        0.003f, 0.003f, 0.003f,   // dir
        0.001f,                    // sphere scale
        0.006f,                    // beam length
        0.0008f                    // beam radius
    };

    // Touch state (raw pixel coordinates)
    private float lastRawY = 0f;
    private boolean isDragging = false;
    private long lastTapTime = 0;
    private static final long DOUBLE_TAP_MS = 400;
    private static final float DRAG_THRESHOLD = 12f; // pixels

    // ═══════════════════════════════════════════════════════════════
    // ABSTRACT IMPLEMENTATIONS
    // ═══════════════════════════════════════════════════════════════

    @Override
    public String getName() { return "FRIEZA_DEATHBEAM"; }

    @Override
    public String getDescription() { return "Frieza - Death Beam"; }

    @Override
    public int getPreviewResourceId() { return R.drawable.preview_frieza_deathbeam; }

    @Override
    protected String getVideoFileName() { return null; }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.ABYSSIA;
    }

    // ═══════════════════════════════════════════════════════════════
    // SETUP
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected void setupSceneSpecific() {
        Log.d(TAG, "Setting up Frieza Death Beam scene...");

        try {
            frieza = new Frieza3D(context);
            frieza.setScreenSize(screenWidth, screenHeight);
            // Valores calibrados (no modificar)
            frieza.setPosition(0.0510f, 0.0780f, 1.2780f);
            frieza.setScale(0.2000f);
        } catch (Exception e) {
            Log.w(TAG, "Frieza3D not ready: " + e.getMessage());
        }

        try {
            deathBeam = new DeathBeamFX();
            deathBeam.setScreenSize(screenWidth, screenHeight);
            deathBeam.setSpherePosition(0.0f, 0.0f, 0.0f);
            deathBeam.setSphereScale(0.12f);
            deathBeam.setBeamDirection(0.77f, 0.63f, 0.11f);
            deathBeam.setBeamLength(3.0f);
            deathBeam.setBeamRadius(0.06f);
        } catch (Exception e) {
            Log.w(TAG, "DeathBeamFX not ready: " + e.getMessage());
        }

        if (EDIT_MODE) {
            logBanner();
        }

        Log.d(TAG, "Frieza scene ready!");
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE & DRAW
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected void updateSceneSpecific(float deltaTime) {
        if (frieza != null) frieza.update(deltaTime);
        if (deathBeam != null) deathBeam.update(deltaTime);
    }

    @Override
    protected void drawSceneSpecific() {
        GLES20.glClearColor(0.02f, 0.01f, 0.05f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        if (frieza != null) frieza.draw();

        if (deathBeam != null) deathBeam.draw();
    }

    // ═══════════════════════════════════════════════════════════════
    // TOUCH - Using RAW MotionEvent (pixel coordinates)
    // ═══════════════════════════════════════════════════════════════

    @Override
    public boolean onTouchEventRaw(MotionEvent event) {
        if (!EDIT_MODE) return false;

        int action = event.getActionMasked();
        float rawX = event.getX();
        float rawY = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastRawY = rawY;
                isDragging = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                float dy = rawY - lastRawY;
                if (Math.abs(dy) > DRAG_THRESHOLD) {
                    isDragging = true;
                    float sens = (editObject == 0) ? FZ_SENS[friezaMode] : DB_SENS[dbMode];
                    float delta = -dy * sens;
                    applyEdit(delta);
                    lastRawY = rawY;
                    logLiveValue();
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (!isDragging) {
                    long now = System.currentTimeMillis();
                    if (now - lastTapTime < DOUBLE_TAP_MS) {
                        // ══ DOBLE TAP: cambiar objeto ══
                        editObject = (editObject + 1) % 2;
                        Log.d(CALIB_TAG, "");
                        Log.d(CALIB_TAG, "████████████████████████████████████████████████");
                        Log.d(CALIB_TAG, "██  CAMBIADO A: " + OBJECT_NAMES[editObject] + "                          ██");
                        Log.d(CALIB_TAG, "████████████████████████████████████████████████");
                        logSelectedMode();
                        lastTapTime = 0;
                    } else {
                        // ══ SINGLE TAP: siguiente/anterior propiedad ══
                        lastTapTime = now;
                        boolean upperHalf = rawY < (screenHeight / 2f);
                        if (editObject == 0) {
                            friezaMode = upperHalf
                                ? (friezaMode + 1) % FZ_MODES
                                : (friezaMode - 1 + FZ_MODES) % FZ_MODES;
                        } else {
                            dbMode = upperHalf
                                ? (dbMode + 1) % DB_MODES
                                : (dbMode - 1 + DB_MODES) % DB_MODES;
                        }
                        // Solo mostrar la propiedad seleccionada, limpio y claro
                        logSelectedMode();
                    }
                } else {
                    // Solto despues de DRAG: mostrar resumen completo con CODE
                    logFullStatus();
                }
                return true;
        }
        return false;
    }

    // Override simple onTouchEvent to do nothing (we use onTouchEventRaw)
    @Override
    public boolean onTouchEvent(float normalizedX, float normalizedY, int action) {
        return false;
    }

    // ═══════════════════════════════════════════════════════════════
    // APPLY EDIT
    // ═══════════════════════════════════════════════════════════════

    private void applyEdit(float delta) {
        if (editObject == 0 && frieza != null) {
            switch (friezaMode) {
                case 0: frieza.setPosition(frieza.getPosX() + delta, frieza.getPosY(), frieza.getPosZ()); break;
                case 1: frieza.setPosition(frieza.getPosX(), frieza.getPosY() + delta, frieza.getPosZ()); break;
                case 2: frieza.setPosition(frieza.getPosX(), frieza.getPosY(), frieza.getPosZ() + delta); break;
                case 3: frieza.setRotationX(frieza.getRotationX() + delta); break;
                case 4: frieza.setRotationY(frieza.getRotationY() + delta); break;
                case 5: frieza.setRotationZ(frieza.getRotationZ() + delta); break;
                case 6: frieza.setScale(Math.max(0.01f, frieza.getScale() + delta)); break;
            }
        } else if (editObject == 1 && deathBeam != null) {
            switch (dbMode) {
                case 0: deathBeam.setSpherePosition(deathBeam.getSphereX() + delta, deathBeam.getSphereY(), deathBeam.getSphereZ()); break;
                case 1: deathBeam.setSpherePosition(deathBeam.getSphereX(), deathBeam.getSphereY() + delta, deathBeam.getSphereZ()); break;
                case 2: deathBeam.setSpherePosition(deathBeam.getSphereX(), deathBeam.getSphereY(), deathBeam.getSphereZ() + delta); break;
                case 3: deathBeam.setBeamDirection(deathBeam.getBeamDirX() + delta, deathBeam.getBeamDirY(), deathBeam.getBeamDirZ()); break;
                case 4: deathBeam.setBeamDirection(deathBeam.getBeamDirX(), deathBeam.getBeamDirY() + delta, deathBeam.getBeamDirZ()); break;
                case 5: deathBeam.setBeamDirection(deathBeam.getBeamDirX(), deathBeam.getBeamDirY(), deathBeam.getBeamDirZ() + delta); break;
                case 6: deathBeam.setSphereScale(Math.max(0.01f, deathBeam.getSphereScale() + delta)); break;
                case 7: deathBeam.setBeamLength(Math.max(0.1f, deathBeam.getBeamLength() + delta)); break;
                case 8: deathBeam.setBeamRadius(Math.max(0.005f, deathBeam.getBeamRadius() + delta)); break;
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // LOGCAT OUTPUT - Tag: FZ_CALIB
    // ═══════════════════════════════════════════════════════════════

    private void logBanner() {
        Log.d(CALIB_TAG, "");
        Log.d(CALIB_TAG, "╔═══════════════════════════════════════════════════════════╗");
        Log.d(CALIB_TAG, "║         FRIEZA CALIBRATION MODE - DUAL OBJECT             ║");
        Log.d(CALIB_TAG, "╠═══════════════════════════════════════════════════════════╣");
        Log.d(CALIB_TAG, "║  TAP arriba   = siguiente propiedad                      ║");
        Log.d(CALIB_TAG, "║  TAP abajo    = propiedad anterior                       ║");
        Log.d(CALIB_TAG, "║  DOBLE TAP    = cambiar FRIEZA <-> DEATHBEAM             ║");
        Log.d(CALIB_TAG, "║  DRAG arriba  = aumentar valor                           ║");
        Log.d(CALIB_TAG, "║  DRAG abajo   = disminuir valor                          ║");
        Log.d(CALIB_TAG, "╠═══════════════════════════════════════════════════════════╣");
        Log.d(CALIB_TAG, "║  OBJETO ACTIVO: " + OBJECT_NAMES[editObject] + "                                     ║");
        Log.d(CALIB_TAG, "╚═══════════════════════════════════════════════════════════╝");
        logSelectedMode();
    }

    /** Muestra que propiedad esta seleccionada - claro y prominente */
    private void logSelectedMode() {
        String obj = OBJECT_NAMES[editObject];
        String label = (editObject == 0) ? FZ_LABEL[friezaMode] : DB_LABEL[dbMode];
        int idx = (editObject == 0) ? friezaMode : dbMode;
        int total = (editObject == 0) ? FZ_MODES : DB_MODES;
        String value = getCurrentValueStr();
        Log.d(CALIB_TAG, "");
        Log.d(CALIB_TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        Log.d(CALIB_TAG, ">>>  [" + obj + "]  " + (idx + 1) + "/" + total + "  :  " + label);
        Log.d(CALIB_TAG, ">>>  VALOR ACTUAL = " + value);
        Log.d(CALIB_TAG, ">>>  (DRAG para modificar, TAP para siguiente)");
        Log.d(CALIB_TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    }

    /** Log en vivo mientras se arrastra */
    private void logLiveValue() {
        String key = (editObject == 0) ? FZ_KEY[friezaMode] : DB_KEY[dbMode];
        Log.d(CALIB_TAG, "  >> " + key + " = " + getCurrentValueStr());
    }

    /** Obtiene el valor actual de la propiedad seleccionada como String */
    private String getCurrentValueStr() {
        if (editObject == 0 && frieza != null) {
            switch (friezaMode) {
                case 0: return String.format("%.4f", frieza.getPosX());
                case 1: return String.format("%.4f", frieza.getPosY());
                case 2: return String.format("%.4f", frieza.getPosZ());
                case 3: return String.format("%.1f", frieza.getRotationX());
                case 4: return String.format("%.1f", frieza.getRotationY());
                case 5: return String.format("%.1f", frieza.getRotationZ());
                case 6: return String.format("%.4f", frieza.getScale());
            }
        } else if (editObject == 1 && deathBeam != null) {
            switch (dbMode) {
                case 0: return String.format("%.4f", deathBeam.getSphereX());
                case 1: return String.format("%.4f", deathBeam.getSphereY());
                case 2: return String.format("%.4f", deathBeam.getSphereZ());
                case 3: return String.format("%.4f", deathBeam.getBeamDirX());
                case 4: return String.format("%.4f", deathBeam.getBeamDirY());
                case 5: return String.format("%.4f", deathBeam.getBeamDirZ());
                case 6: return String.format("%.4f", deathBeam.getSphereScale());
                case 7: return String.format("%.4f", deathBeam.getBeamLength());
                case 8: return String.format("%.4f", deathBeam.getBeamRadius());
            }
        }
        return "N/A";
    }

    /** Log completo con todos los valores de ambos objetos + linea CODE */
    private void logFullStatus() {
        Log.d(CALIB_TAG, "");

        // ── FRIEZA ──
        if (frieza != null) {
            String active = (editObject == 0) ? "  <<<  ACTIVO" : "";
            Log.d(CALIB_TAG, "╔═══════════════════════════════════════════════════╗");
            Log.d(CALIB_TAG, "║  FRIEZA" + active);
            Log.d(CALIB_TAG, "╠═══════════════════════════════════════════════════╣");
            Log.d(CALIB_TAG, String.format("║  POS X  = %+.4f", frieza.getPosX()));
            Log.d(CALIB_TAG, String.format("║  POS Y  = %+.4f", frieza.getPosY()));
            Log.d(CALIB_TAG, String.format("║  POS Z  = %+.4f", frieza.getPosZ()));
            Log.d(CALIB_TAG, String.format("║  ROT X  = %+.1f°", frieza.getRotationX()));
            Log.d(CALIB_TAG, String.format("║  ROT Y  = %+.1f°", frieza.getRotationY()));
            Log.d(CALIB_TAG, String.format("║  ROT Z  = %+.1f°", frieza.getRotationZ()));
            Log.d(CALIB_TAG, String.format("║  SCALE  = %.4f", frieza.getScale()));
            Log.d(CALIB_TAG, "╚═══════════════════════════════════════════════════╝");
            Log.d(CALIB_TAG, String.format("FRIEZA_CODE: setPosition(%.4ff, %.4ff, %.4ff); setScale(%.4ff); setRotationX(%.1ff); setRotationY(%.1ff); setRotationZ(%.1ff);",
                frieza.getPosX(), frieza.getPosY(), frieza.getPosZ(), frieza.getScale(),
                frieza.getRotationX(), frieza.getRotationY(), frieza.getRotationZ()));
        }

        Log.d(CALIB_TAG, "");

        // ── DEATHBEAM ──
        if (deathBeam != null) {
            String active = (editObject == 1) ? "  <<<  ACTIVO" : "";
            Log.d(CALIB_TAG, "╔═══════════════════════════════════════════════════╗");
            Log.d(CALIB_TAG, "║  DEATHBEAM" + active);
            Log.d(CALIB_TAG, "╠═══════════════════════════════════════════════════╣");
            Log.d(CALIB_TAG, String.format("║  POS X   = %+.4f", deathBeam.getSphereX()));
            Log.d(CALIB_TAG, String.format("║  POS Y   = %+.4f", deathBeam.getSphereY()));
            Log.d(CALIB_TAG, String.format("║  POS Z   = %+.4f", deathBeam.getSphereZ()));
            Log.d(CALIB_TAG, String.format("║  DIR X   = %+.4f", deathBeam.getBeamDirX()));
            Log.d(CALIB_TAG, String.format("║  DIR Y   = %+.4f", deathBeam.getBeamDirY()));
            Log.d(CALIB_TAG, String.format("║  DIR Z   = %+.4f", deathBeam.getBeamDirZ()));
            Log.d(CALIB_TAG, String.format("║  SCALE   = %.4f", deathBeam.getSphereScale()));
            Log.d(CALIB_TAG, String.format("║  LENGTH  = %.4f", deathBeam.getBeamLength()));
            Log.d(CALIB_TAG, String.format("║  RADIUS  = %.4f", deathBeam.getBeamRadius()));
            Log.d(CALIB_TAG, "╚═══════════════════════════════════════════════════╝");
            Log.d(CALIB_TAG, String.format("DEATHBEAM_CODE: setSpherePosition(%.4ff, %.4ff, %.4ff); setSphereScale(%.4ff); setBeamDirection(%.4ff, %.4ff, %.4ff); setBeamLength(%.4ff); setBeamRadius(%.4ff);",
                deathBeam.getSphereX(), deathBeam.getSphereY(), deathBeam.getSphereZ(), deathBeam.getSphereScale(),
                deathBeam.getBeamDirX(), deathBeam.getBeamDirY(), deathBeam.getBeamDirZ(),
                deathBeam.getBeamLength(), deathBeam.getBeamRadius()));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SCREEN SIZE & RELEASE
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void setScreenSize(int width, int height) {
        super.setScreenSize(width, height);
        if (frieza != null) frieza.setScreenSize(width, height);
        if (deathBeam != null) deathBeam.setScreenSize(width, height);
    }

    @Override
    protected void releaseSceneSpecificResources() {
        if (frieza != null) { frieza.release(); frieza = null; }
        if (deathBeam != null) { deathBeam.release(); deathBeam = null; }
        Log.d(TAG, "Frieza Death Beam resources released");
    }

    @Override
    public boolean isReady() {
        return !isDisposed;
    }
}
