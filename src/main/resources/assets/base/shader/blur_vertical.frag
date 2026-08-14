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

const int SAMPLE_COUNT = 5;

const float OFFSETS[5] = float[5](
-3.4458098836553415,
-1.4767017588568079,
0.492228282731395,
2.4612181104350137,
4
);

const float WEIGHTS[5] = float[5](
0.1835121872508657,
0.2492203893736597,
0.26495143816720684,
0.2205044383606221,
0.08181154684764567
);

const float Z_NEAR = 0.01f;
void main() {
    vec4 baseColor = texelFetch(colors, ivec2(gl_FragCoord.xy), 0);
    vec4 baseNormal = texelFetch(ddaNormals, ivec2(gl_FragCoord.xy), 0);
    vec4 color = vec4(0);
    for (int i = 0; i < SAMPLE_COUNT; ++i) {
        vec2 offset = vec2(0, OFFSETS[i])/2;
        float weight = WEIGHTS[i];
        vec2 samplePos = vec2(clamp(gl_FragCoord.xy + offset, vec2(0), globalUbo.res-1)+0.25f);
        vec4 newResult = textureLod(colors, samplePos/(globalUbo.res/2.f), 0);
        color += newResult * weight;
    }
    outColor = color;
}