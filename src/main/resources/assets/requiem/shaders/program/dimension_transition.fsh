#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec3 scene = texture(DiffuseSampler, texCoord).rgb;
    float aspect = InSize.x / max(InSize.y, 1.0);
    vec2 centered = (texCoord - vec2(0.5)) * vec2(aspect, 1.0);
    float radius = length(centered) / length(vec2(aspect * 0.5, 0.5));
    float cyan = smoothstep(0.15, 0.85, radius);
    float ember = 1.0 - smoothstep(0.12, 0.95, radius);
    vec3 betweenWorlds = mix(scene, vec3(0.0, 0.78, 1.0), cyan * 0.30);
    betweenWorlds = mix(betweenWorlds, vec3(0.95, 0.24, 0.12), ember * 0.13);
    fragColor = vec4(betweenWorlds, 1.0);
}
