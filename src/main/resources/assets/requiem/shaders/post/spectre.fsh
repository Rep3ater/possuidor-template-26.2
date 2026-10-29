#version 330

#moj_import <minecraft:globals.glsl>

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform SpectreConfig {
    float STime;
    float SolidIntensity;
    float RaysIntensity;
    float Zoom;
    vec3 OverlayColor;
};

in vec2 texCoord;
out vec4 fragColor;

vec2 random2(vec2 st) {
    st = vec2(dot(st, vec2(127.1, 311.7)), dot(st, vec2(269.5, 183.3)));
    return -1.0 + 2.0 * fract(sin(st) * 43758.5453123);
}

float noise(vec2 st) {
    vec2 i = floor(st);
    vec2 f = fract(st);
    vec2 u = f * f * (3.0 - 2.0 * f);

    return mix(
        mix(dot(random2(i + vec2(0.0, 0.0)), f - vec2(0.0, 0.0)),
            dot(random2(i + vec2(1.0, 0.0)), f - vec2(1.0, 0.0)), u.x),
        mix(dot(random2(i + vec2(0.0, 1.0)), f - vec2(0.0, 1.0)),
            dot(random2(i + vec2(1.0, 1.0)), f - vec2(1.0, 1.0)), u.x),
        u.y
    );
}

float shape(vec2 st, float a, float radius, float time) {
    float r = length(st) * 2.0;
    float m = abs(mod(a + time / 4.0, 6.28) - 3.14) / 4.6;
    float f = radius;
    m += noise(st + time * 0.1) * 0.5;
    f += sin(a) * noise(st + time * 0.2) * 0.5;
    f += sin(a * 20.0) * 0.1 * pow(m, 2.0);
    return f * f * smoothstep(f, f + 1.0, r);
}

float streaks(vec2 st, float a, float radius, float time) {
    float f = radius + sin(a * 20.0) * noise(st + time * 0.2) * 0.5;
    return pow(max(f, 0.0), 10.0);
}

void main() {
    float time = STime > 0.0 ? STime : GameTime * 1200.0;
    vec2 st = gl_FragCoord.xy / OutSize.xy;
    vec3 tex = texture(InSampler, st).rgb;

    // Desaturate the scene into an eerie gray spirit vision
    float gray = dot(tex, vec3(0.299, 0.587, 0.114));
    vec3 scene = mix(tex, vec3(gray), 0.75);

    st = (vec2(0.5) - st) * Zoom;
    float a = atan(st.y, st.x);
    float value = shape(st, a, 0.8, time);
    vec3 color = mix(scene, OverlayColor, value * SolidIntensity);
    color += streaks(st, a, 0.8, time) * value * RaysIntensity;
    // Emissive core: a soft bloom of the overlay color where the rays are strongest
    color += OverlayColor * pow(clamp(value, 0.0, 1.0), 3.0) * 0.35 * (0.85 + 0.15 * sin(time * 2.0));
    fragColor = vec4(color, 1.0);
}
