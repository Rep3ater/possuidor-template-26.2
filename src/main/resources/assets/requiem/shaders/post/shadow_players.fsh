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
    fragColor = vec4(mix(vec3(dot(tex.rgb, vec3(0.2125, 0.7154, 0.0721))), tex.rgb, 1.0 - playerTex.r), tex.a);
}
