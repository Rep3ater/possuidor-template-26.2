#version 330

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform FishEyeConfig {
    float Slider;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 p = gl_FragCoord.xy / OutSize.x;
    float prop = OutSize.x / OutSize.y;
    vec2 m = vec2(0.5, 0.5 / prop);
    vec2 d = p - m;
    float r = sqrt(dot(d, d));
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

    vec3 col = texture(InSampler, vec2(uv.x, uv.y * prop)).rgb;
    fragColor = vec4(col, 1.0);
}
