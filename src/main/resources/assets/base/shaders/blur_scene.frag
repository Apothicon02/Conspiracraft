#version 460

uniform vec2 dir;
uniform ivec2 lowRes;
uniform ivec2 res;

layout(binding = 1) uniform sampler2D scene_lighting;
layout(binding = 2, rgba32f) uniform image2D scene_lighting_blurred;

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

void main() {
    //uint64_t startTime = clockARB();

    vec2 texCoord = gl_FragCoord.xy/res;

    vec4 result = vec4(0.0);
    for (int i = 0; i < SAMPLE_COUNT; ++i) {
        vec2 offset = dir * OFFSETS[i] / res;
        float weight = WEIGHTS[i];
        result += texture(scene_lighting, texCoord + offset) * weight;
    }

    imageStore(scene_lighting_blurred, ivec2(gl_FragCoord.xy), result);

    //fragColor = mix(fragColor, vec4(float(clockARB() - startTime) * 0.0000005, 0.0, 1.0, 1.0), 0.95f);
}