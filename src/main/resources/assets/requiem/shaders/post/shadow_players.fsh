#version 330

uniform sampler2D InSampler;
uniform sampler2D PlayerSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
    vec2 PlayerSize;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 tex = texture(InSampler, texCoord);
    vec4 playerTex = texture(PlayerSampler, texCoord);
    float luma = dot(tex.rgb, vec3(0.2125, 0.7154, 0.0721));
    // Neutral, very slightly cool tone for the desaturated areas (no warm/red tint)
    vec3 aged = vec3(luma) * vec3(0.97, 1.0, 1.03);
    // Soft vignette that darkens the screen edges
    vec2 centered = texCoord - vec2(0.5);
    float vignette = 1.0 - smoothstep(0.35, 0.95, length(centered)) * 0.35;
    vec3 graded = mix(aged, tex.rgb, 1.0 - playerTex.r) * vignette;
    fragColor = vec4(graded, tex.a);
}
