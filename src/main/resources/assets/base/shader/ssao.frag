#extension GL_EXT_nonuniform_qualifier : require
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
layout(push_constant) uniform PushUBO {
    mat4 model;
    vec4 color;
    int instanced;
    ivec2 atlasOffset;
    ivec2 size;
    int layer;
    ivec4 tex;
    ivec4 writeTex;
    int atlas;
    int materials;
    int noises;
    int blueNoise;
    int regions;
    int chunks;
    int voxels;
    int lightChunks;
    int lights;
} pushUbo;
layout(set = 0, binding = 2) uniform sampler2D Sampler2D[];
layout(location = 0) in vec2 uv;

layout(location = 0) out vec4 outColor;

vec3 hash(vec3 vec) {
    vec3 p = vec;
    p = vec3(dot(p, vec3(127.1,311.7, 74.7)), dot(p, vec3(269.5,183.3,246.1)), dot(p, vec3(113.5,271.9,124.6)));
    return fract(sin(p)*43758.5453123);
}
vec3 noise(ivec2 coords) {
    return texelFetch(Sampler2D[nonuniformEXT(pushUbo.tex.x)], ivec2(coords.x&63, coords.y&63), 0).rgb;
}
const float PI = 3.141592653589793f;
vec3 randomVec(ivec2 coords) {
    vec3 noise = noise(coords);
    vec3 randDir = vec3(normalize(noise.xy * 2.0 - 1.0), noise.z);
    return randDir;
}
const float AO_RADIUS = 1.f;
vec3 SSAO_KERNEL[32] = vec3[](vec3( 0.011,  0.015,  0.021), vec3(-0.032,  0.021,  0.039),vec3( 0.025, -0.041,  0.048), vec3(-0.014, -0.062,  0.071),vec3( 0.073,  0.033,  0.082), vec3(-0.089,  0.051,  0.113),vec3( 0.042, -0.112,  0.129), vec3(-0.061, -0.124,  0.142),vec3( 0.141,  0.082,  0.163), vec3(-0.162,  0.094,  0.185),vec3( 0.091, -0.192,  0.211), vec3(-0.115, -0.211,  0.242),vec3( 0.231,  0.141,  0.268), vec3(-0.252,  0.163,  0.291),vec3( 0.152, -0.311,  0.334), vec3(-0.191, -0.332,  0.371),vec3( 0.341,  0.212,  0.412), vec3(-0.372,  0.231,  0.448),vec3( 0.221, -0.442,  0.491), vec3(-0.271, -0.471,  0.532),vec3( 0.462,  0.301,  0.581), vec3(-0.502,  0.322,  0.623), vec3( 0.311, -0.612,  0.672), vec3(-0.382, -0.641,  0.714), vec3( 0.602,  0.411,  0.763), vec3(-0.642,  0.432,  0.812), vec3( 0.412, -0.802,  0.864), vec3(-0.512, -0.831,  0.911), vec3( 0.732,  0.531,  0.942), vec3(-0.782,  0.562,  0.968), vec3( 0.522, -0.951,  0.985), vec3(-0.621, -0.972,  0.998));
const int KERNEL_SIZE = 32;
//const float Z_NEAR = 0.01f;
//const float ASPECT = 1.7977529f;
//const float FOCAL_LENGTH = 1.3514224f;
mat4 invProj = inverse(globalUbo.proj);
vec3 reconstructViewPos(vec2 uvPos, float depth) {
    vec4 clip = vec4((uvPos*2)-1, depth, 1.f);
    vec4 view = invProj*clip;
    return view.xyz/view.w;
}
vec4 normal = vec4(0);
float getAO(float depth) {
    float radius = AO_RADIUS;//mix(AO_RADIUS, AO_RADIUS*10, normal.a*2);
    vec3 normalVS = normalize((globalUbo.view * vec4(normal.xyz, 0.f)).xyz);
    vec3 posVS = reconstructViewPos(uv, depth)+(normalize(normalVS)*0.1f);
    vec3 randVec = randomVec(ivec2(gl_FragCoord.xy));
    vec3 tangent = normalize(randVec - normalVS * dot(randVec, normalVS));
    vec3 bitangent = cross(normalVS, tangent);
    mat3 TBN = mat3(tangent, bitangent, normalVS);
    float occlusion = 0.f;
    for (int i = 0; i < KERNEL_SIZE; i++) {
        vec3 sampleVec = TBN*SSAO_KERNEL[i];//randomVec(ivec2(gl_FragCoord.x+(i*2), gl_FragCoord.y+i));
        sampleVec = posVS + sampleVec * radius;
        vec4 offset = globalUbo.proj * vec4(sampleVec, 1.0);
        offset.xyz /= offset.w;
        vec2 sampleUV = (offset.xy*0.5)+0.5;
        if (!(sampleUV.x < 0 || sampleUV.x > 1 || sampleUV.y < 0 || sampleUV.y > 1)) {
            vec3 sampleVS = reconstructViewPos(sampleUV, textureLod(Sampler2D[nonuniformEXT(pushUbo.tex.z)], sampleUV, 0).r);
            float rangeCheck = smoothstep(0, 1, radius/(length(posVS-sampleVS)+0.001f));
            if (sampleVS.z+0.01f < sampleVec.z) {
                occlusion += rangeCheck;
            }
        }
    }
    occlusion = 1.0 - occlusion / KERNEL_SIZE;
    return pow(clamp(occlusion, 0.f, 1), 2.25f);
}
float shade = 1.f;
bool sampleShade(int x, int y) {
    ivec2 offCoords = ivec2(gl_FragCoord.x+x, gl_FragCoord.y+y);
    vec4 offNormal = texelFetch(Sampler2D[nonuniformEXT(pushUbo.tex.w)], offCoords, 0);
    if (dot(offNormal.xyz, normal.xyz) >= 0.9f) {
        float potentialShade = texelFetch(Sampler2D[nonuniformEXT(pushUbo.tex.y)], offCoords, 0).a;
        if (potentialShade <= 1) {
            shade = potentialShade;
            return false;
        }
    }
    return true;
}
void main() {
    vec4 color = textureLod(Sampler2D[nonuniformEXT(pushUbo.tex.y)], uv, 0);
    float depth = textureLod(Sampler2D[nonuniformEXT(pushUbo.tex.z)], uv, 0).r;
    normal = textureLod(Sampler2D[nonuniformEXT(pushUbo.tex.w)], uv, 0);
    shade = color.a;
    if (shade > 2) { // && scaledCoords.x > 0 && scaledCoords.y > 0 && scaledCoords.x < globalUbo.res.x-1 && scaledCoords.y < globalUbo.res.y-1
        shade = 1.f;
        if (sampleShade(-1, 0)) {
            if (sampleShade(0, -1)) {
                if (sampleShade(1, 0)) {
                    sampleShade(0, 1);
                }
            }
        }
    }
    //if (shade < 1) {shade = 0.1f;}
    color.rgb*=min(1, shade);
//    outColor.rgb = color.rgb;
//    outColor.a = 1;
//    vec3 randVec = hash()/1.732f;
//    if (length(randVec) > 1.f) {outColor.rgb = vec3(1, 0, 0);} else {outColor.rgb = vec3(0, 1, 0);}
//    outColor.a = 1;
    //outColor = vec4(vec3(getAO(depth, normal.xyz)), 1);
    float antiAO = normal.a;
    outColor = antiAO > 0.95f ? vec4(color.rgb, 1) : vec4(color.rgb, mix(getAO(depth), 1, antiAO));
}