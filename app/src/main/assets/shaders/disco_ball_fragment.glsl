// ============================================
// DISCO BALL FRAGMENT SHADER
// Features: Mirror tiles, color cycling, reflections
// ============================================

precision mediump float;

uniform float u_Time;
uniform sampler2D u_Texture;
uniform vec2 u_Resolution;

varying vec2 v_TexCoord;
varying vec3 v_Normal;
varying vec3 v_WorldPos;
varying float v_Pulse;

// ============================================
// UTILITY FUNCTIONS
// ============================================

// Hash function for pseudo-random values
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

// Smooth color cycling (RGB transitions)
vec3 colorCycle(float time) {
    return vec3(
        0.5 + 0.5 * sin(time),
        0.5 + 0.5 * sin(time + 2.094),  // 120° phase shift
        0.5 + 0.5 * sin(time + 4.189)   // 240° phase shift
    );
}

void main() {
    // ============================================
    // MIRROR TILE GRID
    // ============================================
    // Create grid of mirror tiles (20x20)
    vec2 gridSize = vec2(20.0, 20.0);
    vec2 grid = floor(v_TexCoord * gridSize);
    vec2 gridUV = fract(v_TexCoord * gridSize);

    // Each tile has unique random value
    float tileHash = hash(grid);

    // ============================================
    // TILE BORDER (dark gaps between mirrors)
    // ============================================
    float borderWidth = 0.08;
    float borderX = step(borderWidth, gridUV.x) * step(borderWidth, 1.0 - gridUV.x);
    float borderY = step(borderWidth, gridUV.y) * step(borderWidth, 1.0 - gridUV.y);
    float tileMask = borderX * borderY;

    // ============================================
    // BASE MIRROR COLOR (silver/chrome)
    // ============================================
    vec3 baseColor = vec3(0.8, 0.85, 0.9);  // Slight blue tint (chrome)

    // ============================================
    // REFLECTION SIMULATION
    // ============================================
    // Fake environment reflection based on normal
    vec3 normal = normalize(v_Normal);
    float reflectionPattern = abs(normal.x) * 0.3 + abs(normal.y) * 0.4 + abs(normal.z) * 0.3;

    // Add some variation per tile
    reflectionPattern += tileHash * 0.2;

    // ============================================
    // COLOR CYCLING (rainbow effect)
    // ============================================
    vec3 cycleColor = colorCycle(u_Time * 2.0 + tileHash * 6.28);

    // Mix chrome with cycling colors
    vec3 tileColor = mix(baseColor, cycleColor, 0.4);

    // ============================================
    // SPARKLE EFFECT
    // ============================================
    // Random tiles sparkle bright white
    float sparkle = step(0.98, hash(grid + floor(u_Time * 3.0)));
    float sparkleIntensity = sparkle * (0.5 + 0.5 * sin(u_Time * 10.0));

    tileColor += vec3(1.0) * sparkleIntensity * 0.8;

    // ============================================
    // LIGHTING (brighter tiles based on orientation)
    // ============================================
    vec3 lightDir = normalize(vec3(1.0, 1.0, 1.0));
    float diffuse = max(0.0, dot(normal, lightDir));

    // Add ambient
    float lighting = 0.3 + diffuse * 0.7;

    // Boost brightness when pulsing
    lighting += (v_Pulse - 1.0) * 2.0;

    // ============================================
    // FINAL COLOR ASSEMBLY
    // ============================================
    vec3 finalColor = tileColor * lighting * tileMask;

    // Add reflection highlights
    finalColor += vec3(1.0) * reflectionPattern * 0.3;

    // Black borders between tiles
    finalColor = mix(vec3(0.05), finalColor, tileMask);

    // ============================================
    // OUTPUT
    // ============================================
    gl_FragColor = vec4(finalColor, 1.0);
}
