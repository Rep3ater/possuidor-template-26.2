#version 330

uniform sampler2D InSampler;
layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 pixel = 1.0 / max(InSize, vec2(1.0));
    vec3 center = texture(InSampler, texCoord).rgb;
    vec3 blur = center * 0.48;
    blur += texture(InSampler, texCoord + vec2(pixel.x, 0.0)).rgb * 0.08;
    blur += texture(InSampler, texCoord - vec2(pixel.x, 0.0)).rgb * 0.08;
    blur += texture(InSampler, texCoord + vec2(0.0, pixel.y)).rgb * 0.08;
    blur += texture(InSampler, texCoord - vec2(0.0, pixel.y)).rgb * 0.08;
    blur += texture(InSampler, texCoord + pixel).rgb * 0.05;
    blur += texture(InSampler, texCoord - pixel).rgb * 0.05;
    blur += texture(InSampler, texCoord + vec2(pixel.x, -pixel.y)).rgb * 0.05;
    blur += texture(InSampler, texCoord + vec2(-pixel.x, pixel.y)).rgb * 0.05;

    // The astral plane is intentionally neutral and low contrast: graveyard stone,
    // pale morning mist, and the quiet of a meditation hall rather than a neon spirit realm.
    float gray = dot(blur, vec3(0.299, 0.587, 0.114));
    vec3 stone = vec3(0.43) + vec3(gray - 0.42) * 0.47;
    stone = clamp(stone, vec3(0.30), vec3(0.70));

    // Only strongly saturated, bright greens are allowed to read as living foliage.
    // This keeps dull grass/ground from becoming a major visual feature.
    float plantSignal = max(0.0, blur.g - max(blur.r, blur.b) * 1.24);
    float plantMask = smoothstep(0.14, 0.29, plantSignal) * smoothstep(0.17, 0.40, blur.g);
    vec3 plantGlow = vec3(0.73, 0.82, 0.77) * (0.78 + min(0.30, blur.g * 0.25));
    vec3 dream = mix(stone, plantGlow, clamp(plantMask * 1.20, 0.0, 0.88));

    // The virtual rootways use a much brighter pale-cyan/ivory filament, but avoid hard neon contrast.
    float cyan = min(smoothstep(0.055, 0.22, blur.g - blur.r), smoothstep(0.055, 0.24, blur.b - blur.r));
    cyan *= smoothstep(0.045, 0.21, max(blur.g, blur.b));
    vec3 rootGlow = min(vec3(0.96), center * vec3(1.02, 1.08, 1.08) + vec3(0.11, 0.15, 0.15));
    dream = mix(dream, rootGlow, clamp(cyan * 1.1, 0.0, 1.0));

    // A uniform high-mist veil softens silhouettes and removes harsh black/white edges.
    vec3 mist = vec3(0.72, 0.74, 0.73);
    dream = mix(dream, mist, 0.22);
    fragColor = vec4(dream, 1.0);
}
