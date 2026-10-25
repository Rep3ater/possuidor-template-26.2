#version 330

uniform sampler2D InSampler;
uniform sampler2D OverlaySampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
    vec2 OverlaySize;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 texDiffuse = texture(InSampler, texCoord);
    vec4 texOverlay = texture(OverlaySampler, texCoord);
    fragColor = vec4(mix(texDiffuse.rgb, texOverlay.rgb, texOverlay.a), 1.0);
}
