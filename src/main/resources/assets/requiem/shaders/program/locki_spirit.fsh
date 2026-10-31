#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 source = texture(DiffuseSampler, texCoord);
    float gray = dot(source.rgb, vec3(0.299, 0.587, 0.114));
    vec3 astral = mix(source.rgb, vec3(gray * 0.88 + 0.10), 0.48);
    float aspect = InSize.x / max(InSize.y, 1.0);
    vec2 centered = (texCoord - vec2(0.5)) * vec2(aspect, 1.0);
    float radius = length(centered) / length(vec2(aspect * 0.5, 0.5));
    float edgeTint = smoothstep(0.18, 1.0, radius) * 0.12;
    astral = mix(astral, vec3(0.62, 0.72, 0.78), edgeTint);
    fragColor = vec4(astral, 1.0);
}
