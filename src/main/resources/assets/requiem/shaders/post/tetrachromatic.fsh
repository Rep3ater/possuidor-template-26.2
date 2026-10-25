#version 330

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform TetrachromaticConfig {
    float Contrast;
    float Saturation;
    vec3 Gray;
    vec3 RedMatrix;
    vec3 GreenMatrix;
    vec3 BlueMatrix;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 InTexel = texture(InSampler, texCoord);

    float RedValue = dot(InTexel.rgb, RedMatrix);
    float GreenValue = dot(InTexel.rgb, GreenMatrix);
    float BlueValue = dot(InTexel.rgb, BlueMatrix);
    vec3 OutColor = vec3(RedValue, GreenValue, BlueValue);

    OutColor = (OutColor - 0.5) * Contrast + Contrast * 0.5;

    float Luma = dot(OutColor, Gray);
    vec3 Chroma = OutColor - Luma;
    OutColor = (Chroma * (1.0 + Chroma * Saturation)) + Luma;

    fragColor = vec4(OutColor, 1.0);
}
