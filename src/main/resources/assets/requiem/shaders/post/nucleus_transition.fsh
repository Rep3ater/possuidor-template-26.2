#version 330
uniform sampler2D InSampler;
layout(std140) uniform SamplerInfo { vec2 OutSize; vec2 InSize; };
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec3 source = texture(InSampler, texCoord).rgb;
    float gray = dot(source, vec3(0.299, 0.587, 0.114));
    vec3 gold = mix(source, vec3(1.0, 0.78, 0.12), 0.68);
    vec2 d = texCoord - vec2(0.5, 0.5);
    float center = exp(-dot(d, d) * 5.0);
    gold += vec3(0.16, 0.12, 0.01) * center;
    gold = mix(gold, vec3(gray * 0.85 + 0.15, gray * 0.70 + 0.12, gray * 0.12), 0.12);
    fragColor = vec4(clamp(gold, 0.0, 1.0), 1.0);
}
