#version 330

uniform sampler2D InSampler;
layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec3 color = texture(InSampler, texCoord).rgb;
    float gray = dot(color, vec3(0.299, 0.587, 0.114));
    vec3 redMemory = vec3(gray * 1.34 + color.r * 0.12, gray * 0.16, gray * 0.105);
    redMemory = pow(max(redMemory, vec3(0.0)), vec3(0.86));
    vec2 centered = texCoord - vec2(0.5);
    float vignette = 1.0 - smoothstep(0.20, 0.80, length(centered));
    redMemory *= 0.72 + 0.28 * vignette;
    fragColor = vec4(redMemory, 1.0);
}
