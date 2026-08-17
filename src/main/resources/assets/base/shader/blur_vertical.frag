layout(set = 0, binding = 0) readonly uniform GlobalUBO {
    mat4 view;
    mat4 proj;
    mat4 viewPrev;
    mat4 projPrev;
    ivec4 renderToggles;
    vec4 skylight;
    vec3 sun;
    int hdr;
    float time;
    ivec2 res;
} globalUbo;
layout(set = 0, binding = 10) uniform sampler2D ddaDepth;
layout(set = 0, binding = 11) uniform sampler2D ddaNormals;
layout(set = 0, binding = 18) uniform sampler2D colors;
layout(location = 0) in vec2 uv;

layout(location = 0) out vec4 outColor;

const int SAMPLE_COUNT = 9;

const float OFFSETS[9] = float[9](
-7.385486338269373,
-5.415332322090894,
-3.4458098836553415,
-1.4767017588568079,
0.492228282731395,
2.4612181104350137,
4.4305055426526785,
6.400317149797591,
8
);

const float WEIGHTS[9] = float[9](
0.036514415685046854,
0.0809315020373954,
0.1404066727610046,
0.190680554683392,
0.20271650855234985,
0.16870974611035225,
0.1099127158139171,
0.056052075960067727,
0.014075808396474473
);

const float Z_NEAR = 0.01f;
void main() {
    vec4 baseColor = texelFetch(colors, ivec2(gl_FragCoord.xy), 0);
    vec4 baseNormal = texelFetch(ddaNormals, ivec2(gl_FragCoord.xy), 0);
    vec4 color = vec4(0);
    for (int i = 0; i < SAMPLE_COUNT; ++i) {
        vec2 offset = vec2(0, OFFSETS[i]);
        float weight = WEIGHTS[i];
        vec2 samplePos = vec2(clamp(gl_FragCoord.xy + offset, vec2(0), globalUbo.res-1)+0.25f);
        vec4 newResult = textureLod(colors, samplePos/(globalUbo.res/2.f), 0);
        color += newResult * weight;
    }
    outColor = color;
}