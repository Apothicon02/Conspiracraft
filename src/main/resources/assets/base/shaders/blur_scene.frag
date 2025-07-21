#version 460

uniform vec2 dir;
uniform ivec2 lowRes;
uniform ivec2 res;

layout(binding = 1) uniform sampler2D scene_lighting;
layout(binding = 2, rgba32f) uniform writeonly image2D scene_lighting_blurred;

in vec4 gl_FragCoord;

const int SAMPLE_COUNT = 25;

const float OFFSETS[25] = float[25](
-12,
-11,
-10,
-9,
-8,
-7,
-6,
-5,
-4,
-3,
-2,
-1,
0,
1,
2,
3,
4,
5,
6,
7,
8,
9,
10,
11,
12
);

const float WEIGHTS[25] = float[25](
0.01837418050306995,
0.02198585126447698,
0.025900103643447376,
0.03003881497204258,
0.03429946212220176,
0.03855805474868844,
0.042674276910692206,
0.046498643867443484,
0.049881229062569324,
0.05268129310571659,
0.054776987761201906,
0.056074242700892014,
0.0565137186751149,
0.056074242700892014,
0.054776987761201906,
0.05268129310571659,
0.049881229062569324,
0.046498643867443484,
0.042674276910692206,
0.03855805474868844,
0.03429946212220176,
0.03003881497204258,
0.025900103643447376,
0.02198585126447698,
0.01837418050306995
);

const int SAMPLE_COUNT_AO = 5;

const float OFFSETS_AO[5] = float[5](
-3.4458098836553415,
-1.4767017588568079,
0.492228282731395,
2.4612181104350137,
4
);

const float WEIGHTS_AO[5] = float[5](
0.1835121872508657,
0.2492203893736597,
0.26495143816720684,
0.2205044383606221,
0.08181154684764567
);


void main() {
    //uint64_t startTime = clockARB();

    vec2 texCoord = gl_FragCoord.xy/res;

    vec3 result = vec3(0.0);
    for (int i = 0; i < SAMPLE_COUNT; ++i) {
        vec2 offset = dir * OFFSETS[i] / res;
        float weight = WEIGHTS[i];
        result += texture(scene_lighting, texCoord + offset).rgb * weight;
    }
    float AO = 0.0f;
    for (int i = 0; i < SAMPLE_COUNT_AO; ++i) {
        vec2 offset = dir * OFFSETS_AO[i] / res;
        float weight = WEIGHTS_AO[i];
        AO += texture(scene_lighting, texCoord + offset).a * weight;
    }

    imageStore(scene_lighting_blurred, ivec2(gl_FragCoord.xy), vec4(result, AO));

    //fragColor = mix(fragColor, vec4(float(clockARB() - startTime) * 0.0000005, 0.0, 1.0, 1.0), 0.95f);
}