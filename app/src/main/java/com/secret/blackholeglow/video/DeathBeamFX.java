package com.secret.blackholeglow.video;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import android.app.ActivityManager;
import android.content.Context;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * DeathBeamFX - Efecto visual del Death Beam de Frieza.
 *
 * Componentes:
 * 1. Energy Sphere: bola de energia con radial gradient + noise animado
 * 2. Beam Core: rayo conico (finito->ancho) con energia fluyendo
 */
public class DeathBeamFX {
    private static final String TAG = "DeathBeamFX";

    // Sphere geometry
    private FloatBuffer sphereVertices;
    private ShortBuffer sphereIndices;
    private int sphereIndexCount;
    private int sphereProgram;

    // Beam geometry
    private FloatBuffer beamVertices;
    private ShortBuffer beamIndices;
    private int beamIndexCount;
    private int beamProgram;

    // State
    private float time = 0f;
    private int screenWidth = 1, screenHeight = 1;

    // Cached uniform/attrib locations (avoid per-frame lookups)
    private int sphereMVPLoc = -1, sphereTimeLoc = -1, spherePosLoc = -1;
    private int beamMVPLoc = -1, beamTimeLoc = -1, beamPosLoc = -1;

    // Sphere transform
    private float sphereX, sphereY, sphereZ;
    private float sphereScale = 0.034f;

    // Beam transform
    private float beamDirX = -0.77f, beamDirY = -0.48f, beamDirZ = 0.41f;
    private float beamLength = 4.47f;
    private float beamRadius = 0.21f;

    // Matrices (pre-allocated, no per-frame GC)
    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private final float[] tempMatrix = new float[16];
    private final float[] beamRotMatrix = new float[16];  // cached beam direction rotation
    private final float[] beamTempResult = new float[16]; // reusable multiply buffer
    private boolean beamRotDirty = true;

    // ==========================================
    // SPHERE SHADER - Intense energy ball
    // ==========================================
    private static final String SPHERE_VS =
        "precision highp float;\n" +
        "attribute vec3 aPosition;\n" +
        "uniform mat4 uMVP;\n" +
        "varying vec3 vNormal;\n" +
        "void main() {\n" +
        "    gl_Position = uMVP * vec4(aPosition, 1.0);\n" +
        "    vNormal = normalize(aPosition);\n" +
        "}\n";

    private static final String SPHERE_FS =
        "precision mediump float;\n" +
        "uniform float uTime;\n" +
        "varying vec3 vNormal;\n" +
        "void main() {\n" +
        // Smooth radial gradient: all red tones
        "    float facing = abs(dot(normalize(vNormal), vec3(0.0, 0.0, 1.0)));\n" +
        "    float core = pow(facing, 1.5);\n" +
        "    float pulse = 0.9 + 0.1 * sin(uTime * 5.0);\n" +
        // Dark red edge -> bright crimson -> hot pink-red center
        "    vec3 col;\n" +
        "    if (core < 0.3) {\n" +
        "        col = mix(vec3(0.3,0.0,0.0), vec3(0.7,0.02,0.02), core/0.3);\n" +
        "    } else if (core < 0.6) {\n" +
        "        col = mix(vec3(0.7,0.02,0.02), vec3(1.0,0.12,0.08), (core-0.3)/0.3);\n" +
        "    } else {\n" +
        "        col = mix(vec3(1.0,0.12,0.08), vec3(1.0,0.35,0.3), (core-0.6)/0.4);\n" +
        "    }\n" +
        // Deep red glow aura on edges
        "    float edge = pow(1.0 - facing, 3.0);\n" +
        "    col += vec3(0.8, 0.02, 0.0) * edge * 1.5;\n" +
        "    float brightness = 1.6 + 0.4 * sin(uTime * 4.0);\n" +
        "    gl_FragColor = vec4(col * brightness * pulse, 1.0);\n" +
        "}\n";

    // ==========================================
    // BEAM SHADER - Flowing energy cone
    // ==========================================
    private static final String BEAM_VS =
        "precision highp float;\n" +
        "attribute vec3 aPosition;\n" +
        "uniform mat4 uMVP;\n" +
        "varying vec3 vPos;\n" +
        "varying float vAlongBeam;\n" +
        "void main() {\n" +
        "    gl_Position = uMVP * vec4(aPosition, 1.0);\n" +
        "    vPos = aPosition;\n" +
        "    vAlongBeam = aPosition.y;\n" +
        "}\n";

    private static final String BEAM_FS =
        "precision mediump float;\n" +
        "uniform float uTime;\n" +
        "varying vec3 vPos;\n" +
        "varying float vAlongBeam;\n" +
        // 3D noise functions
        "vec3 hash3(vec3 p) {\n" +
        "    p = vec3(dot(p,vec3(127.1,311.7,74.7)),\n" +
        "            dot(p,vec3(269.5,183.3,246.1)),\n" +
        "            dot(p,vec3(113.5,271.9,124.6)));\n" +
        "    return -1.0 + 2.0 * fract(sin(p) * 43758.5453);\n" +
        "}\n" +
        "float noise3d(vec3 p) {\n" +
        "    vec3 i = floor(p);\n" +
        "    vec3 f = fract(p);\n" +
        "    vec3 u = f*f*(3.0-2.0*f);\n" +
        "    return mix(mix(mix(dot(hash3(i),f),\n" +
        "                       dot(hash3(i+vec3(1,0,0)),f-vec3(1,0,0)),u.x),\n" +
        "                   mix(dot(hash3(i+vec3(0,1,0)),f-vec3(0,1,0)),\n" +
        "                       dot(hash3(i+vec3(1,1,0)),f-vec3(1,1,0)),u.x),u.y),\n" +
        "               mix(mix(dot(hash3(i+vec3(0,0,1)),f-vec3(0,0,1)),\n" +
        "                       dot(hash3(i+vec3(1,0,1)),f-vec3(1,0,1)),u.x),\n" +
        "                   mix(dot(hash3(i+vec3(0,1,1)),f-vec3(0,1,1)),\n" +
        "                       dot(hash3(i+vec3(1,1,1)),f-vec3(1,1,1)),u.x),u.y),u.z);\n" +
        "}\n" +
        "void main() {\n" +
        // Radial distance from beam center
        "    float radial = length(vPos.xz);\n" +
        "    float coreFade = smoothstep(1.0, 0.0, radial * 10.0);\n" +
        "    float angle = atan(vPos.z, vPos.x);\n" +
        //
        // === PLASMA / FIRE FLOW (directional, like dense flame) ===
        // Fast primary flow along beam direction
        "    float flow1 = vAlongBeam * 6.0 - uTime * 8.0;\n" +
        // Slower secondary flow for large-scale turbulence
        "    float flow2 = vAlongBeam * 3.0 - uTime * 5.0;\n" +
        // Very fast fine detail
        "    float flow3 = vAlongBeam * 12.0 - uTime * 11.0;\n" +
        //
        // Multi-scale plasma turbulence (like fire, flows in one direction)
        "    float plasma1 = noise3d(vec3(angle * 2.0, flow1, uTime * 1.5)) * 0.5 + 0.5;\n" +
        "    float plasma2 = noise3d(vec3(angle * 4.0 + 1.7, flow2, uTime * 0.8)) * 0.5 + 0.5;\n" +
        "    float plasma3 = noise3d(vec3(vPos.xz * 15.0, flow3)) * 0.5 + 0.5;\n" +
        //
        // Combine: large swirls + medium detail + fine grain
        "    float energy = coreFade * (plasma1 * 0.45 + plasma2 * 0.35 + plasma3 * 0.2);\n" +
        "    energy = pow(energy, 0.35);\n" +
        //
        // Traveling pulse waves (energy surges along the beam)
        "    float surge1 = 0.75 + 0.25 * sin(uTime * 7.0 + vAlongBeam * 12.0);\n" +
        "    float surge2 = 0.85 + 0.15 * sin(uTime * 11.0 - vAlongBeam * 8.0);\n" +
        "    float surge3 = 0.9 + 0.1 * sin(uTime * 4.5 + vAlongBeam * 20.0);\n" +
        "    energy *= surge1 * surge2 * surge3;\n" +
        //
        // Flickering intensity (like flame)
        "    float flicker = 0.85 + 0.15 * noise3d(vec3(vAlongBeam * 5.0, uTime * 6.0, 0.0));\n" +
        "    energy *= flicker;\n" +
        //
        // === COLOR: red tones matching sphere (NO orange/yellow) ===
        "    vec3 col;\n" +
        "    if (energy < 0.2) {\n" +
        "        col = mix(vec3(0.25,0.0,0.0), vec3(0.6,0.01,0.01), energy/0.2);\n" +
        "    } else if (energy < 0.45) {\n" +
        "        col = mix(vec3(0.6,0.01,0.01), vec3(0.9,0.06,0.03), (energy-0.2)/0.25);\n" +
        "    } else if (energy < 0.7) {\n" +
        "        col = mix(vec3(0.9,0.06,0.03), vec3(1.0,0.15,0.1), (energy-0.45)/0.25);\n" +
        "    } else {\n" +
        "        col = mix(vec3(1.0,0.15,0.1), vec3(1.0,0.35,0.28), (energy-0.7)/0.3);\n" +
        "    }\n" +
        //
        // === ELECTRICITY ARCS (red/pink-white, not yellow) ===
        "    float arc1 = noise3d(vec3(angle * 3.0, vAlongBeam * 15.0 - uTime * 9.0, uTime * 2.5));\n" +
        "    arc1 = pow(max(arc1, 0.0), 8.0);\n" +
        "    float arc2 = noise3d(vec3(angle * 5.5 + 2.0, vAlongBeam * 22.0 + uTime * 11.0, uTime * 3.5));\n" +
        "    arc2 = pow(max(arc2, 0.0), 10.0);\n" +
        "    float arc3 = noise3d(vec3(angle * 7.0 - 1.5, vAlongBeam * 12.0 - uTime * 13.0, uTime * 1.8));\n" +
        "    arc3 = pow(max(arc3, 0.0), 6.0);\n" +
        "    float electricity = (arc1 + arc2 * 0.7 + arc3 * 0.5) * coreFade;\n" +
        // Red-pink-white arcs (matching red theme)
        "    col += vec3(1.0, 0.6, 0.55) * electricity * 3.5;\n" +
        //
        // === EDGE GLOW (red aura at beam surface) ===
        "    float edgeGlow = pow(smoothstep(0.0, 0.8, radial * 8.0) * (1.0 - smoothstep(0.8, 1.0, radial * 8.0)), 2.0);\n" +
        "    col += vec3(0.7, 0.02, 0.0) * edgeGlow * (0.8 + 0.4 * sin(uTime * 5.0 + vAlongBeam * 8.0));\n" +
        //
        // Final brightness + alpha
        "    float brightness = 1.8 + 0.4 * sin(uTime * 4.0);\n" +
        "    float alpha = energy * (0.85 + 0.15 * surge1) + electricity * 0.5;\n" +
        "    gl_FragColor = vec4(col * brightness, clamp(alpha, 0.0, 1.0));\n" +
        "}\n";

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public DeathBeamFX() {
        this(null);
    }

    public DeathBeamFX(Context context) {
        // Adaptive geometry quality based on device RAM
        int sphereDetail = 32;
        int coneSegments = 12;
        if (context != null) {
            try {
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(memInfo);
                long totalGB = memInfo.totalMem / (1024L * 1024L * 1024L);
                if (totalGB < 4) {
                    sphereDetail = 16;  // 289 verts vs 1089 (73% less)
                    coneSegments = 8;   // 18 verts vs 26 (30% less)
                    Log.d(TAG, "LOW RAM: reduced geometry (sphere=" + sphereDetail + ", cone=" + coneSegments + ")");
                }
            } catch (Exception e) {
                Log.w(TAG, "RAM detection failed, using full detail");
            }
        }

        generateSphere(sphereDetail, sphereDetail);
        generateCone(coneSegments, 1.0f);
        sphereProgram = buildProgram(SPHERE_VS, SPHERE_FS);
        beamProgram = buildProgram(BEAM_VS, BEAM_FS);

        // Cache uniform/attrib locations (avoid per-frame glGet* calls)
        if (sphereProgram != 0) {
            sphereMVPLoc = GLES20.glGetUniformLocation(sphereProgram, "uMVP");
            sphereTimeLoc = GLES20.glGetUniformLocation(sphereProgram, "uTime");
            spherePosLoc = GLES20.glGetAttribLocation(sphereProgram, "aPosition");
        }
        if (beamProgram != 0) {
            beamMVPLoc = GLES20.glGetUniformLocation(beamProgram, "uMVP");
            beamTimeLoc = GLES20.glGetUniformLocation(beamProgram, "uTime");
            beamPosLoc = GLES20.glGetAttribLocation(beamProgram, "aPosition");
        }
        Log.d(TAG, "DeathBeamFX initialized");
    }

    // ==========================================
    // GEOMETRY
    // ==========================================

    private void generateSphere(int latBands, int lonBands) {
        int numVerts = (latBands + 1) * (lonBands + 1);
        float[] verts = new float[numVerts * 3];
        int vi = 0;
        for (int lat = 0; lat <= latBands; lat++) {
            float theta = (float)(lat * Math.PI / latBands);
            float sinT = (float)Math.sin(theta);
            float cosT = (float)Math.cos(theta);
            for (int lon = 0; lon <= lonBands; lon++) {
                float phi = (float)(lon * 2.0 * Math.PI / lonBands);
                verts[vi++] = sinT * (float)Math.cos(phi);
                verts[vi++] = cosT;
                verts[vi++] = sinT * (float)Math.sin(phi);
            }
        }
        int numIdx = latBands * lonBands * 6;
        short[] idx = new short[numIdx];
        int ii = 0;
        for (int lat = 0; lat < latBands; lat++) {
            for (int lon = 0; lon < lonBands; lon++) {
                int first = lat * (lonBands + 1) + lon;
                int second = first + lonBands + 1;
                idx[ii++] = (short) first;
                idx[ii++] = (short) second;
                idx[ii++] = (short) (first + 1);
                idx[ii++] = (short) second;
                idx[ii++] = (short) (second + 1);
                idx[ii++] = (short) (first + 1);
            }
        }
        sphereVertices = createFloatBuffer(verts);
        sphereIndices = createShortBuffer(idx);
        sphereIndexCount = numIdx;
    }

    private void generateCone(int segments, float length) {
        // Hollow cone surface: thin at y=0 (sphere), wide at y=length (tip)
        // Normalized radii - beamRadius scales in drawBeam()
        float baseRadius = 0.02f;
        float tipRadius = 1.0f;
        int numVerts = (segments + 1) * 2;
        float[] verts = new float[numVerts * 3];
        int vi = 0;
        for (int i = 0; i <= segments; i++) {
            float angle = (float)(i * 2.0 * Math.PI / segments);
            float cosA = (float)Math.cos(angle);
            float sinA = (float)Math.sin(angle);
            verts[vi++] = cosA * baseRadius;
            verts[vi++] = 0f;
            verts[vi++] = sinA * baseRadius;
            verts[vi++] = cosA * tipRadius;
            verts[vi++] = length;
            verts[vi++] = sinA * tipRadius;
        }
        int numIdx = segments * 6;
        short[] idx = new short[numIdx];
        int ii = 0;
        for (int i = 0; i < segments; i++) {
            int b0 = i * 2, t0 = b0 + 1, b1 = b0 + 2, t1 = b0 + 3;
            idx[ii++] = (short) b0;
            idx[ii++] = (short) t0;
            idx[ii++] = (short) b1;
            idx[ii++] = (short) t0;
            idx[ii++] = (short) t1;
            idx[ii++] = (short) b1;
        }
        beamVertices = createFloatBuffer(verts);
        beamIndices = createShortBuffer(idx);
        beamIndexCount = numIdx;
    }

    // ==========================================
    // UPDATE & DRAW
    // ==========================================

    public void update(float deltaTime) {
        time += deltaTime;
        // Wrap at ~10*2*PI. Highest freq is sin(time*13.0) → max 817, safe for mediump
        if (time > 62.83f) time -= 62.83f;
    }

    public void draw() {
        float aspect = (float) screenWidth / screenHeight;
        Matrix.perspectiveM(projectionMatrix, 0, 45f, aspect, 0.1f, 100f);
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE); // Additive

        drawBeam();
        drawSphere();

        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void drawSphere() {
        if (sphereProgram == 0) return;
        GLES20.glUseProgram(sphereProgram);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, sphereX, sphereY, sphereZ);
        Matrix.scaleM(modelMatrix, 0, sphereScale, sphereScale, sphereScale);

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);

        GLES20.glUniformMatrix4fv(sphereMVPLoc, 1, false, mvpMatrix, 0);
        GLES20.glUniform1f(sphereTimeLoc, time);

        sphereVertices.position(0);
        GLES20.glEnableVertexAttribArray(spherePosLoc);
        GLES20.glVertexAttribPointer(spherePosLoc, 3, GLES20.GL_FLOAT, false, 0, sphereVertices);
        sphereIndices.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, sphereIndexCount, GLES20.GL_UNSIGNED_SHORT, sphereIndices);
        GLES20.glDisableVertexAttribArray(spherePosLoc);
    }

    private void drawBeam() {
        if (beamProgram == 0) return;
        GLES20.glUseProgram(beamProgram);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, sphereX, sphereY, sphereZ);

        // Align Y-axis to beam direction (cached, only recompute when direction changes)
        if (beamRotDirty) {
            alignYToDirection(beamDirX, beamDirY, beamDirZ, beamRotMatrix);
            beamRotDirty = false;
        }
        Matrix.multiplyMM(beamTempResult, 0, modelMatrix, 0, beamRotMatrix, 0);
        System.arraycopy(beamTempResult, 0, modelMatrix, 0, 16);

        Matrix.scaleM(modelMatrix, 0, beamRadius, beamLength, beamRadius);

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);

        GLES20.glUniformMatrix4fv(beamMVPLoc, 1, false, mvpMatrix, 0);
        GLES20.glUniform1f(beamTimeLoc, time);

        beamVertices.position(0);
        GLES20.glEnableVertexAttribArray(beamPosLoc);
        GLES20.glVertexAttribPointer(beamPosLoc, 3, GLES20.GL_FLOAT, false, 0, beamVertices);
        beamIndices.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, beamIndexCount, GLES20.GL_UNSIGNED_SHORT, beamIndices);
        GLES20.glDisableVertexAttribArray(beamPosLoc);
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private void alignYToDirection(float dx, float dy, float dz, float[] mat) {
        Matrix.setIdentityM(mat, 0);
        float len = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len < 0.001f) return;
        dx /= len; dy /= len; dz /= len;

        float upX = 0f, upY = 0f, upZ = 1f;
        if (Math.abs(dz) > 0.9f) { upX = 1f; upZ = 0f; }
        float xx = dy * upZ - dz * upY;
        float xy = dz * upX - dx * upZ;
        float xz = dx * upY - dy * upX;
        float xlen = (float)Math.sqrt(xx*xx + xy*xy + xz*xz);
        if (xlen > 0.001f) { xx /= xlen; xy /= xlen; xz /= xlen; }
        float zx = xy * dz - xz * dy;
        float zy = xz * dx - xx * dz;
        float zz = xx * dy - xy * dx;

        mat[0] = xx; mat[1] = xy; mat[2] = xz;
        mat[4] = dx; mat[5] = dy; mat[6] = dz;
        mat[8] = zx; mat[9] = zy; mat[10] = zz;
    }

    private int buildProgram(String vertSrc, String fragSrc) {
        int vs = compileShader(GLES20.GL_VERTEX_SHADER, vertSrc);
        int fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragSrc);
        if (vs == 0 || fs == 0) return 0;
        int prog = GLES20.glCreateProgram();
        GLES20.glAttachShader(prog, vs);
        GLES20.glAttachShader(prog, fs);
        GLES20.glLinkProgram(prog);
        int[] linked = new int[1];
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            Log.e(TAG, "Program link error: " + GLES20.glGetProgramInfoLog(prog));
            GLES20.glDeleteProgram(prog);
            return 0;
        }
        GLES20.glDeleteShader(vs);
        GLES20.glDeleteShader(fs);
        return prog;
    }

    private int compileShader(int type, String source) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, source);
        GLES20.glCompileShader(s);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader compile error: " + GLES20.glGetShaderInfoLog(s));
            GLES20.glDeleteShader(s);
            return 0;
        }
        return s;
    }

    private FloatBuffer createFloatBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data);
        fb.position(0);
        return fb;
    }

    private ShortBuffer createShortBuffer(short[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 2);
        bb.order(ByteOrder.nativeOrder());
        ShortBuffer sb = bb.asShortBuffer();
        sb.put(data);
        sb.position(0);
        return sb;
    }

    // ==========================================
    // SETTERS
    // ==========================================

    public void setSpherePosition(float x, float y, float z) { sphereX = x; sphereY = y; sphereZ = z; }
    public void setSphereScale(float s) { sphereScale = s; }
    public void setBeamDirection(float dx, float dy, float dz) {
        float len = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len > 0.001f) { beamDirX = dx/len; beamDirY = dy/len; beamDirZ = dz/len; beamRotDirty = true; }
    }
    public void setBeamLength(float l) { beamLength = l; }
    public void setBeamRadius(float r) { beamRadius = r; }
    public void setScreenSize(int w, int h) { screenWidth = w; screenHeight = h; }

    public void release() {
        if (sphereProgram != 0) GLES20.glDeleteProgram(sphereProgram);
        if (beamProgram != 0) GLES20.glDeleteProgram(beamProgram);
        sphereProgram = beamProgram = 0;
    }
}
