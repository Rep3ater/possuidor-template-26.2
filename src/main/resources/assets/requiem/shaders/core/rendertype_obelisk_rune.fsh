#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in float saturation;

out vec4 fragColor;

vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = c.g < c.b ? vec4(c.bg, K.wz) : vec4(c.gb, K.xy);
    vec4 q = c.r < p.x ? vec4(p.xyw, c.r) : vec4(c.r, p.yzx);

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a < 0.5) {
        discard;
    }
    vec3 hsv = rgb2hsv(color.rgb);
    hsv.y = saturation;
    color.rgb = hsv2rgb(hsv);

    // Emissive glow: bright rune pixels pulse with a cool cyan-violet halo and stay visible through fog
    float pulse = 0.8 + 0.2 * sin(GameTime * 1200.0 * 2.0 + texCoord0.x * 20.0);
    float brightness = max(color.r, max(color.g, color.b));
    vec3 glowTint = vec3(0.45, 0.7, 1.0);
    vec3 glow = mix(color.rgb, glowTint, 0.35) * smoothstep(0.4, 1.0, brightness) * pulse * 0.6;
    color.rgb += glow * 0.5;

    vec4 fogged = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
    fogged.rgb += glow * 0.5;
    fragColor = fogged;
}
