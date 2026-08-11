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
layout(set = 0, binding = 12) uniform sampler2D colors;
layout(location = 0) in vec2 uv;

layout(location = 0) out vec4 outColor;

const int SAMPLE_COUNT = 11;

const float OFFSETS[11] = float[11](
-9.47074270360747,
-7.4768919324809096,
-5.4830484388316485,
-3.4892105931983437,
-1.4953752517922876,
0.4984572288165547,
2.4922929486601455,
4.486128941894109,
6.479969366444332,
8.473816318234265,
10
);

const float WEIGHTS[11] = float[11](
0.06713590953428764,
0.08274259114380782,
0.0970834630671909,
0.10844310372695917,
0.11531852921840198,
0.11674539038922478,
0.11251753407230614,
0.10323896289874948,
0.09017933222573615,
0.07499144416001882,
0.03160373956331709
);

const float Z_NEAR = 0.01f;
void main() {
//    float baseDepth = Z_NEAR/texelFetch(ddaDepth, ivec2(gl_FragCoord.xy), 0).r;
    vec4 baseColor = texelFetch(colors, ivec2(gl_FragCoord.xy*4), 0);
    vec4 baseNormal = texelFetch(ddaNormals, ivec2(gl_FragCoord.xy*4), 0);
    vec3 color = vec3(0);
    for (int i = 0; i < SAMPLE_COUNT; ++i) {
        vec2 offset = vec2(OFFSETS[i], 0);
        float weight = WEIGHTS[i];
        ivec2 samplePos = ivec2(clamp(gl_FragCoord.xy + offset, vec2(0), globalUbo.res-1)+0.5f);
        vec4 newResult = texelFetch(colors, samplePos*4, 0);
        color += newResult.rgb * weight;
//        float sampleDepth = Z_NEAR/textureLod(ddaDepth, samplePos, 0).r;
//        vec4 sampleNormal = textureLod(ddaNormals, samplePos, 0);
//        if (dot(sampleNormal.xyz, baseNormal.xyz) >= 0.9f && abs(sampleDepth-baseDepth) < baseDepth*0.005f) {
//            color.a += newResult.a*weight;
//        } else {
//            color.a += baseColor.a*weight;
//        }
    }
    outColor = vec4(color, baseColor.a);
}