package com.secret.blackholeglow.video;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * DeathBeamFX - Efecto visual del Death Beam de Frieza.
 *
 * Genera geometria procedural (esfera, cono, anillos) y aplica
 * shaders animados para crear el efecto de energia.
 *
 * Componentes:
 * 1. Energy Sphere: bola de energia con voronoi + noise animado
 * 2. Beam Core: rayo conico con energia fluyendo
 * 3. Energy Rings: anillos girando alrededor de la esfera
 */
public class DeathBeamFX {
    private static final String TAG = "DeathBeamFX";

    // ==========================================
    // ENERGY SPHERE
    // ==========================================
    private FloatBuffer sphereVertices;
    private ShortBuffer sphereIndices;
    private int sphereIndexCount;
    private int sphereProgram;

    // ==========================================
    // BEAM CORE
    // ==========================================
    private FloatBuffer beamVertices;
    private ShortBuffer beamIndices;
    private int beamIndexCount;
    private int beamProgram;

    // ==========================================
    // ENERGY RINGS
    // ==========================================
    private FloatBuffer ringVertices;
    private ShortBuffer ringIndices;
    private int ringIndexCount;
    private int ringProgram;

    // Shared state
    private float time = 0f;
    private int screenWidth = 1, screenHeight = 1;

    // Transform - sphere position (at Frieza's fist)
    private float sphereX = 0f, sphereY = 0f, sphereZ = 0f;
    private float sphereScale = 0.12f;
    // Beam direction (normalized)
    private float beamDirX = 0.77f, beamDirY = 0.63f, beamDirZ = 0.11f;
    private float beamLength = 3.0f;
    private float beamRadius = 0.06f;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private final float[] tempMatrix = new float[16];

    // ==========================================
    // ENERGY SPHERE SHADER
    // ==========================================
    private static final String SPHERE_VS =
        "precision highp float;\n" +
        "attribute vec3 aPosition;\n" +
        "uniform mat4 uMVP;\n" +
        "varying vec3 vPos;\n" +
        "void main() {\n" +
        "    gl_Position = uMVP * vec4(aPosition, 1.0);\n" +
        "    vPos = aPosition;\n" +
        "}\n";

    private static final String SPHERE_FS =
        "precision mediump float;\n" +
        "uniform float uTime;\n" +
        "varying vec3 vPos;\n" +
        // Simplex-style hash for noise
        "vec3 hash3(vec3 p) {\n" +
        "    p = vec3(dot(p,vec3(127.1,311.7,74.7)),\n" +
        "            dot(p,vec3(269.5,183.3,246.1)),\n" +
        "            dot(p,vec3(113.5,271.9,124.6)));\n" +
        "    return -1.0 + 2.0 * fract(sin(p) * 43758.5453);\n" +
        "}\n" +
        // 3D noise
        "float noise3d(vec3 p) {\n" +
        "    vec3 i = floor(p);\n" +
        "    vec3 f = fract(p);\n" +
        "    vec3 u = f*f*(3.0-2.0*f);\n" +
        "    return mix(mix(mix(dot(hash3(i+vec3(0,0,0)),f-vec3(0,0,0)),\n" +
        "                       dot(hash3(i+vec3(1,0,0)),f-vec3(1,0,0)),u.x),\n" +
        "                   mix(dot(hash3(i+vec3(0,1,0)),f-vec3(0,1,0)),\n" +
        "                       dot(hash3(i+vec3(1,1,0)),f-vec3(1,1,0)),u.x),u.y),\n" +
        "               mix(mix(dot(hash3(i+vec3(0,0,1)),f-vec3(0,0,1)),\n" +
        "                       dot(hash3(i+vec3(1,0,1)),f-vec3(1,0,1)),u.x),\n" +
        "                   mix(dot(hash3(i+vec3(0,1,1)),f-vec3(0,1,1)),\n" +
        "                       dot(hash3(i+vec3(1,1,1)),f-vec3(1,1,1)),u.x),u.y),u.z);\n" +
        "}\n" +
        // Voronoi distance
        "float voronoi(vec3 p) {\n" +
        "    vec3 n = floor(p);\n" +
        "    vec3 f = fract(p);\n" +
        "    float md = 8.0;\n" +
        "    for(int k=-1; k<=1; k++)\n" +
        "    for(int j=-1; j<=1; j++)\n" +
        "    for(int i=-1; i<=1; i++) {\n" +
        "        vec3 g = vec3(float(i),float(j),float(k));\n" +
        "        vec3 o = 0.5 + 0.5*sin(uTime*0.8 + 6.2831*hash3(n+g));\n" +
        "        vec3 r = g + o - f;\n" +
        "        float d = dot(r,r);\n" +
        "        md = min(md, d);\n" +
        "    }\n" +
        "    return sqrt(md);\n" +
        "}\n" +
        "void main() {\n" +
        "    vec3 p = vPos * 5.0;\n" +
        // Rotate over time for swirling effect
        "    float ct = cos(uTime*0.5);\n" +
        "    float st = sin(uTime*0.5);\n" +
        "    p.xz = mat2(ct,-st,st,ct) * p.xz;\n" +
        // Voronoi cells (electric pattern)
        "    float v = voronoi(p);\n" +
        // Noise overlay (turbulence)
        "    float n = noise3d(p * 3.0 + uTime * 1.5) * 0.5 + 0.5;\n" +
        // Combine
        "    float energy = v * 0.6 + n * 0.4;\n" +
        "    energy = pow(energy, 0.6);\n" +
        // Pulsation
        "    float pulse = 0.8 + 0.2 * sin(uTime * 4.0);\n" +
        "    energy *= pulse;\n" +
        // Color: deep purple -> magenta -> hot pink -> white
        "    vec3 col;\n" +
        "    if (energy < 0.3) {\n" +
        "        col = mix(vec3(0.15,0.0,0.35), vec3(0.7,0.0,0.5), energy/0.3);\n" +
        "    } else if (energy < 0.6) {\n" +
        "        col = mix(vec3(0.7,0.0,0.5), vec3(1.0,0.2,0.7), (energy-0.3)/0.3);\n" +
        "    } else {\n" +
        "        col = mix(vec3(1.0,0.2,0.7), vec3(1.0,1.0,1.0), (energy-0.6)/0.4);\n" +
        "    }\n" +
        // Fresnel rim glow
        "    vec3 nrm = normalize(vPos);\n" +
        "    float fresnel = pow(1.0 - abs(dot(nrm, vec3(0.0,0.0,1.0))), 2.0);\n" +
        "    col += vec3(0.6, 0.2, 1.0) * fresnel * pulse;\n" +
        // Emission brightness
        "    float brightness = 1.5 + 0.5 * sin(uTime * 3.0);\n" +
        "    gl_FragColor = vec4(col * brightness, 1.0);\n" +
        "}\n";

    // ==========================================
    // BEAM SHADER
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
        "    vAlongBeam = aPosition.y;\n" + // Y goes along beam length
        "}\n";

    private static final String BEAM_FS =
        "precision mediump float;\n" +
        "uniform float uTime;\n" +
        "varying vec3 vPos;\n" +
        "varying float vAlongBeam;\n" +
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
        // Flow along beam (UV scrolling)
        "    float flow = vAlongBeam * 6.0 - uTime * 4.0;\n" +
        // Noise for electric crackling
        "    float n1 = noise3d(vec3(vPos.xz * 10.0, flow)) * 0.5 + 0.5;\n" +
        "    float n2 = noise3d(vec3(vPos.xz * 20.0, flow * 1.5 + 3.14)) * 0.5 + 0.5;\n" +
        // Combine: core brightness + electric overlay
        "    float radial = length(vPos.xz);\n" +
        "    float coreFade = smoothstep(1.0, 0.0, radial * 12.0);\n" +
        "    float energy = coreFade * (n1 * 0.7 + n2 * 0.3);\n" +
        "    energy = pow(energy, 0.5);\n" +
        // Pulsation
        "    float pulse = 0.85 + 0.15 * sin(uTime * 5.0 + vAlongBeam * 3.0);\n" +
        "    energy *= pulse;\n" +
        // Color gradient: purple -> pink -> white (center is brighter)
        "    vec3 col;\n" +
        "    if (energy < 0.3) {\n" +
        "        col = mix(vec3(0.2,0.0,0.4), vec3(0.8,0.0,0.6), energy/0.3);\n" +
        "    } else if (energy < 0.6) {\n" +
        "        col = mix(vec3(0.8,0.0,0.6), vec3(1.0,0.3,0.8), (energy-0.3)/0.3);\n" +
        "    } else {\n" +
        "        col = mix(vec3(1.0,0.3,0.8), vec3(1.0,1.0,1.0), (energy-0.6)/0.4);\n" +
        "    }\n" +
        "    float alpha = energy * (0.7 + 0.3 * pulse);\n" +
        "    gl_FragColor = vec4(col * 1.5, alpha);\n" +
        "}\n";

    // ==========================================
    // RING SHADER (simple emissive)
    // ==========================================
    private static final String RING_VS =
        "precision highp float;\n" +
        "attribute vec3 aPosition;\n" +
        "uniform mat4 uMVP;\n" +
        "void main() {\n" +
        "    gl_Position = uMVP * vec4(aPosition, 1.0);\n" +
        "}\n";

    private static final String RING_FS =
        "precision mediump float;\n" +
        "uniform float uTime;\n" +
        "void main() {\n" +
        "    float pulse = 0.7 + 0.3 * sin(uTime * 5.0);\n" +
        "    float r = 0.6 + 0.4 * sin(uTime * 1.2);\n" +
        "    float g = 0.1 + 0.2 * sin(uTime * 1.5);\n" +
        "    float b = 0.8 + 0.2 * cos(uTime * 1.0);\n" +
        "    gl_FragColor = vec4(vec3(r, g, b) * pulse * 2.0, 0.8 * pulse);\n" +
        "}\n";

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public DeathBeamFX() {
        generateSphere(16, 16);
        generateCone(12, 1.0f, 0.0f);
        generateRing(24, 12, 1.0f, 0.15f);
        sphereProgram = buildProgram(SPHERE_VS, SPHERE_FS);
        beamProgram = buildProgram(BEAM_VS, BEAM_FS);
        ringProgram = buildProgram(RING_VS, RING_FS);
        Log.d(TAG, "DeathBeamFX initialized");
    }

    // ==========================================
    // GEOMETRY GENERATION
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
                float x = sinT * (float)Math.cos(phi);
                float y = cosT;
                float z = sinT * (float)Math.sin(phi);
                verts[vi++] = x;
                verts[vi++] = y;
                verts[vi++] = z;
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

    private void generateCone(int segments, float length, float topRadius) {
        // Cone along Y axis: base at y=0, tip at y=length
        float baseRadius = beamRadius;
        int numVerts = (segments + 1) * 2;
        float[] verts = new float[numVerts * 3];
        int vi = 0;
        for (int i = 0; i <= segments; i++) {
            float angle = (float)(i * 2.0 * Math.PI / segments);
            float cosA = (float)Math.cos(angle);
            float sinA = (float)Math.sin(angle);
            // Base ring
            verts[vi++] = cosA * baseRadius;
            verts[vi++] = 0f;
            verts[vi++] = sinA * baseRadius;
            // Top ring (narrow or point)
            verts[vi++] = cosA * topRadius;
            verts[vi++] = length;
            verts[vi++] = sinA * topRadius;
        }

        int numIdx = segments * 6;
        short[] idx = new short[numIdx];
        int ii = 0;
        for (int i = 0; i < segments; i++) {
            int b0 = i * 2;
            int t0 = b0 + 1;
            int b1 = b0 + 2;
            int t1 = b0 + 3;
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

    private void generateRing(int ringSegs, int tubeSegs, float ringRadius, float tubeRadius) {
        int numVerts = (ringSegs + 1) * (tubeSegs + 1);
        float[] verts = new float[numVerts * 3];
        int vi = 0;
        for (int i = 0; i <= ringSegs; i++) {
            float theta = (float)(i * 2.0 * Math.PI / ringSegs);
            float ct = (float)Math.cos(theta);
            float st = (float)Math.sin(theta);
            for (int j = 0; j <= tubeSegs; j++) {
                float phi = (float)(j * 2.0 * Math.PI / tubeSegs);
                float cp = (float)Math.cos(phi);
                float sp = (float)Math.sin(phi);
                float x = (ringRadius + tubeRadius * cp) * ct;
                float y = (ringRadius + tubeRadius * cp) * st;
                float z = tubeRadius * sp;
                verts[vi++] = x;
                verts[vi++] = y;
                verts[vi++] = z;
            }
        }

        int numIdx = ringSegs * tubeSegs * 6;
        short[] idx = new short[numIdx];
        int ii = 0;
        for (int i = 0; i < ringSegs; i++) {
            for (int j = 0; j < tubeSegs; j++) {
                int a = i * (tubeSegs + 1) + j;
                int b = a + tubeSegs + 1;
                idx[ii++] = (short) a;
                idx[ii++] = (short) b;
                idx[ii++] = (short) (a + 1);
                idx[ii++] = (short) b;
                idx[ii++] = (short) (b + 1);
                idx[ii++] = (short) (a + 1);
            }
        }

        ringVertices = createFloatBuffer(verts);
        ringIndices = createShortBuffer(idx);
        ringIndexCount = numIdx;
    }

    // ==========================================
    // UPDATE & DRAW
    // ==========================================

    public void update(float deltaTime) {
        time += deltaTime;
        if (time > 1000f) time -= 1000f;
    }

    public void draw() {
        float aspect = (float) screenWidth / screenHeight;
        Matrix.perspectiveM(projectionMatrix, 0, 45f, aspect, 0.1f, 100f);
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f);

        // Enable additive blending for energy effects
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE); // Additive!

        drawBeam();
        drawSphere();
        drawRings();

        // Restore normal blending
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

        int mvpLoc = GLES20.glGetUniformLocation(sphereProgram, "uMVP");
        int timeLoc = GLES20.glGetUniformLocation(sphereProgram, "uTime");
        int posLoc = GLES20.glGetAttribLocation(sphereProgram, "aPosition");

        GLES20.glUniformMatrix4fv(mvpLoc, 1, false, mvpMatrix, 0);
        GLES20.glUniform1f(timeLoc, time);

        sphereVertices.position(0);
        GLES20.glEnableVertexAttribArray(posLoc);
        GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 0, sphereVertices);

        sphereIndices.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, sphereIndexCount, GLES20.GL_UNSIGNED_SHORT, sphereIndices);

        GLES20.glDisableVertexAttribArray(posLoc);
    }

    private void drawBeam() {
        if (beamProgram == 0) return;
        GLES20.glUseProgram(beamProgram);

        // Position beam at sphere, oriented along beam direction
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, sphereX, sphereY, sphereZ);

        // Rotate to align Y-axis with beam direction
        // Calculate rotation from (0,1,0) to (beamDirX, beamDirY, beamDirZ)
        float[] rotMat = alignYToDirection(beamDirX, beamDirY, beamDirZ);
        float[] result = new float[16];
        Matrix.multiplyMM(result, 0, modelMatrix, 0, rotMat, 0);
        System.arraycopy(result, 0, modelMatrix, 0, 16);

        Matrix.scaleM(modelMatrix, 0, 1f, beamLength, 1f);

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);

        int mvpLoc = GLES20.glGetUniformLocation(beamProgram, "uMVP");
        int timeLoc = GLES20.glGetUniformLocation(beamProgram, "uTime");
        int posLoc = GLES20.glGetAttribLocation(beamProgram, "aPosition");

        GLES20.glUniformMatrix4fv(mvpLoc, 1, false, mvpMatrix, 0);
        GLES20.glUniform1f(timeLoc, time);

        beamVertices.position(0);
        GLES20.glEnableVertexAttribArray(posLoc);
        GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 0, beamVertices);

        beamIndices.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, beamIndexCount, GLES20.GL_UNSIGNED_SHORT, beamIndices);

        GLES20.glDisableVertexAttribArray(posLoc);
    }

    private void drawRings() {
        if (ringProgram == 0) return;
        GLES20.glUseProgram(ringProgram);

        int mvpLoc = GLES20.glGetUniformLocation(ringProgram, "uMVP");
        int timeLoc = GLES20.glGetUniformLocation(ringProgram, "uTime");
        int posLoc = GLES20.glGetAttribLocation(ringProgram, "aPosition");

        GLES20.glUniform1f(timeLoc, time);

        // Draw 3 rings at different angles and rotation speeds
        float[][] ringParams = {
            {45f, 30f, 0.18f, 1.0f},   // tiltX, tiltZ, scale, speed
            {-30f, 60f, 0.22f, -1.3f},
            {70f, -20f, 0.15f, 1.7f}
        };

        for (float[] params : ringParams) {
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, sphereX, sphereY, sphereZ);
            Matrix.rotateM(modelMatrix, 0, params[0], 1f, 0f, 0f);
            Matrix.rotateM(modelMatrix, 0, params[1], 0f, 0f, 1f);
            Matrix.rotateM(modelMatrix, 0, time * 60f * params[3], 0f, 0f, 1f);
            Matrix.scaleM(modelMatrix, 0, params[2], params[2], params[2]);

            Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);

            GLES20.glUniformMatrix4fv(mvpLoc, 1, false, mvpMatrix, 0);

            ringVertices.position(0);
            GLES20.glEnableVertexAttribArray(posLoc);
            GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 0, ringVertices);

            ringIndices.position(0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, ringIndexCount, GLES20.GL_UNSIGNED_SHORT, ringIndices);
        }

        GLES20.glDisableVertexAttribArray(posLoc);
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private float[] alignYToDirection(float dx, float dy, float dz) {
        // Build rotation matrix that aligns Y-axis to the given direction
        float[] mat = new float[16];
        Matrix.setIdentityM(mat, 0);

        float len = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len < 0.001f) return mat;
        dx /= len; dy /= len; dz /= len;

        // Y-axis = direction
        // Find perpendicular X-axis
        float upX = 0f, upY = 0f, upZ = 1f;
        if (Math.abs(dz) > 0.9f) { upX = 1f; upZ = 0f; }
        // X = cross(dir, up)
        float xx = dy * upZ - dz * upY;
        float xy = dz * upX - dx * upZ;
        float xz = dx * upY - dy * upX;
        float xlen = (float)Math.sqrt(xx*xx + xy*xy + xz*xz);
        if (xlen > 0.001f) { xx /= xlen; xy /= xlen; xz /= xlen; }
        // Z = cross(x, dir)
        float zx = xx * dz - xz * dx + xy * dy - xy * dy;
        // Actually, use proper cross product
        zx = xy * dz - xz * dy;
        float zy = xz * dx - xx * dz;
        float zz = xx * dy - xy * dx;

        mat[0] = xx; mat[1] = xy; mat[2] = xz;
        mat[4] = dx; mat[5] = dy; mat[6] = dz;
        mat[8] = zx; mat[9] = zy; mat[10] = zz;

        return mat;
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
            GLES20.glDeleteShader(vs);
            GLES20.glDeleteShader(fs);
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

    public void setSpherePosition(float x, float y, float z) {
        sphereX = x; sphereY = y; sphereZ = z;
    }

    public void setSphereScale(float s) { sphereScale = s; }

    public void setBeamDirection(float dx, float dy, float dz) {
        float len = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len > 0.001f) {
            beamDirX = dx/len; beamDirY = dy/len; beamDirZ = dz/len;
        }
    }

    public void setBeamLength(float l) { beamLength = l; }
    public void setBeamRadius(float r) { beamRadius = r; }
    public void setScreenSize(int w, int h) { screenWidth = w; screenHeight = h; }

    // Getters for calibration
    public float getSphereX() { return sphereX; }
    public float getSphereY() { return sphereY; }
    public float getSphereZ() { return sphereZ; }
    public float getSphereScale() { return sphereScale; }
    public float getBeamDirX() { return beamDirX; }
    public float getBeamDirY() { return beamDirY; }
    public float getBeamDirZ() { return beamDirZ; }
    public float getBeamLength() { return beamLength; }
    public float getBeamRadius() { return beamRadius; }

    public void release() {
        if (sphereProgram != 0) GLES20.glDeleteProgram(sphereProgram);
        if (beamProgram != 0) GLES20.glDeleteProgram(beamProgram);
        if (ringProgram != 0) GLES20.glDeleteProgram(ringProgram);
        sphereProgram = beamProgram = ringProgram = 0;
        Log.d(TAG, "DeathBeamFX released");
    }
}
