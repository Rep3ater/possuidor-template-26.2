#version 330

uniform sampler2D InSampler;
layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec3 source = texture(InSampler, texCoord).rgb;
    float gray = dot(source, vec3(0.299, 0.587, 0.114));
    vec3 astral = mix(source, vec3(gray * 0.86 + 0.10), 0.54);

    // Warm, saturated details (including the current red/yellow corruption cues) become gold.
    float warmSignal = max(0.0, min(source.r, source.g) - source.b * 0.62);
    float warmMask = smoothstep(0.045, 0.22, warmSignal) * smoothstep(0.12, 0.42, max(source.r, source.g));
    astral = mix(astral, vec3(1.0, 0.82, 0.20), clamp(warmMask * 0.78, 0.0, 0.82));

    // Dark upper-screen pixels become a magenta astral sky at night. Blue hues reinforce the
    // effect, while the dark mask keeps bright clouds from being painted pink.
    float blueSignal = max(0.0, source.b - max(source.r, source.g) * 1.10);
    float darkSky = 1.0 - smoothstep(0.12, 0.42, gray);
    float upperSky = smoothstep(0.48, 0.94, texCoord.y);
    float blueMask = smoothstep(0.025, 0.18, blueSignal);
    float magentaMask = max(blueMask, darkSky * 0.78) * darkSky * upperSky;
    astral = mix(astral, vec3(0.92, 0.19, 0.88), clamp(magentaMask * 0.92, 0.0, 0.92));

    // Brighten a central yellow apparition without completely covering the crosshair.
    vec2 delta = texCoord - vec2(0.5, 0.50);
    float centre = exp(-dot(delta, delta) * 115.0) * 0.095;
    astral += vec3(1.0, 0.76, 0.14) * centre;
    fragColor = vec4(clamp(astral, 0.0, 1.0), 1.0);
}
