#version 330

vec4 sample_lightmap(sampler2D lightMap, ivec2 uv) {
    return texture(lightMap, (vec2(uv) + vec2(0.5)) / 256.0);
}
