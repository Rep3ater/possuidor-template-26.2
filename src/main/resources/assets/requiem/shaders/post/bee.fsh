#version 330

#define PI 3.14159265359
#define TWO_PI 6.28318530718
#define SIDES 6.0

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BeeConfig {
    float TileCount;
    float TileRatioX;
};

in vec2 texCoord;
out vec4 fragColor;

float hexagon(vec2 st) {
    float a = atan(st.x, st.y) + PI;
    float r = TWO_PI / SIDES;
    float d = cos(floor(0.5 + a / r) * r - a) * length(st);
    return 1.0 - smoothstep(0.84, 0.98, d);
}

float linearstep(float edge0, float edge1, float x) {
    return clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
}

vec2 tile(in vec2 _st, in vec2 shift) {
    float gapx = (1.0 - TileRatioX) / 2.0;
    vec2 st = fract(_st * vec2(TileCount * TileRatioX, TileCount) + shift);
    st.x = linearstep(gapx, 1.0 - gapx, st.x);
    return st * 2.0 - 1.0;
}

void main() {
    vec2 p = gl_FragCoord.xy / OutSize.x;
    float prop = OutSize.x / OutSize.y;
    vec2 m = vec2(0.5, 0.5 / prop);
    vec2 d = p - m;
    float r = sqrt(dot(d, d));
    float Slider = 0.42;
    float power = (2.0 * 3.141592 / (2.0 * sqrt(dot(m, m)))) * (Slider - 0.5);
    float bind;
    if (power > 0.0) bind = sqrt(dot(m, m));
    else bind = (prop < 1.0) ? m.x : m.y;

    vec2 uv;
    if (power > 0.0 && r > 0.00001)
        uv = m + normalize(d) * tan(r * power) * bind / tan(bind * power);
    else if (power < 0.0 && r > 0.00001)
        uv = m + normalize(d) * atan(r * -power * 10.0) * bind / atan(-power * bind * 10.0);
    else
        uv = p;

    vec2 st = vec2(uv.x, uv.y * prop);
    float paving = hexagon(tile(st, vec2(0.0))) + hexagon(tile(st, vec2(0.5)));

    vec3 col = texture(InSampler, texCoord).rgb;
    vec3 Gray = vec3(0.3, 0.59, 0.11);
    float Saturation = 1.4;
    float Luma = dot(col, Gray);
    vec3 Chroma = col - Luma;
    col = (Chroma * Saturation) + Luma * paving;
    fragColor = vec4(col, 1.0);
}
