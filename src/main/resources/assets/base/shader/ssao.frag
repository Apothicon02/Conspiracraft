layout(set = 0, binding = 0) readonly uniform GlobalUBO {
    mat4 view;
    mat4 proj;
    ivec4 renderToggles;
    vec4 skylight;
    vec3 sun;
    int hdr;
    float time;
    ivec2 res;
} globalUbo;
layout(set = 0, binding = 9) uniform sampler2D ddaColors;
layout(set = 0, binding = 10) uniform sampler2D ddaDepth;
layout(set = 0, binding = 11) uniform sampler2D ddaNormals;
layout(set = 0, binding = 20) uniform sampler2D blueNoise;
layout(location = 0) in vec2 uv;

layout(location = 0) out vec4 outColor;

vec3 hash(vec3 vec) {
    vec3 p = vec;
    p = vec3(dot(p, vec3(127.1,311.7, 74.7)), dot(p, vec3(269.5,183.3,246.1)), dot(p, vec3(113.5,271.9,124.6)));
    return fract(sin(p)*43758.5453123);
}
vec3 noise(ivec2 coords) {
    return texelFetch(blueNoise, ivec2(coords.x&63, coords.y&63), 0).rgb;
}
const float PI = 3.141592653589793f;
vec3 randomVec(ivec2 coords) {
    vec3 noise = noise(coords);
    vec2 xy = noise.xy * 2.0 - 1.0;
    float r2 = dot(xy, xy);
    if (r2 > 1.0) {
        xy = normalize(xy);
        r2 = 1.0;
    }
    float hemisphere = sqrt(1.0 - r2);
    float radius = noise.z;
    return vec3(xy, hemisphere) * radius;
}
const float AO_RADIUS = 2.f;
const float AO_STRENGTH = 2.f;
vec3 SSAO_KERNEL[32] = vec3[](vec3( 0.021,  0.183,  0.512), vec3( 0.392,  0.041,  0.734), vec3(-0.221,  0.114,  0.612), vec3( 0.134, -0.287,  0.553), vec3(-0.341, -0.102,  0.487), vec3( 0.287,  0.331,  0.682), vec3(-0.129,  0.412,  0.731), vec3( 0.512, -0.221,  0.612), vec3(-0.412, -0.331,  0.682), vec3( 0.221,  0.129,  0.341), vec3(-0.183, -0.021,  0.512), vec3( 0.331, -0.412,  0.731), vec3(-0.041,  0.392,  0.734), vec3( 0.102, -0.341,  0.487), vec3(-0.287,  0.134,  0.553), vec3( 0.412,  0.287,  0.612), vec3(-0.512, -0.183,  0.512), vec3( 0.341, -0.102,  0.487), vec3(-0.129,  0.221,  0.341), vec3( 0.183,  0.412,  0.731), vec3(-0.392,  0.041,  0.734), vec3( 0.102, -0.512,  0.612), vec3(-0.221, -0.392,  0.682), vec3( 0.331,  0.129,  0.341), vec3(-0.041, -0.183,  0.512), vec3( 0.287, -0.412,  0.731), vec3(-0.134,  0.341,  0.487), vec3( 0.412, -0.287,  0.612), vec3(-0.512,  0.183,  0.512), vec3( 0.341,  0.102,  0.487), vec3(-0.129, -0.221,  0.341), vec3( 0.183, -0.412,  0.731));
const int KERNEL_SIZE = 8;
//const float Z_NEAR = 0.01f;
//const float ASPECT = 1.7977529f;
//const float FOCAL_LENGTH = 1.3514224f;
vec3 reconstructViewPos(vec2 uvPos, float depth) {
    vec4 clip = vec4((uvPos*2)-1, depth, 1.f);
    vec4 view = inverse(globalUbo.proj)*clip;
    return view.xyz/view.w;
}
float getAO(float depth, vec3 normal) {
    vec3 posVS = reconstructViewPos(uv, depth);
    vec3 normalVS = normalize((globalUbo.view * vec4(normal.xyz, 0.f)).xyz);
    vec3 randVec = randomVec(ivec2(gl_FragCoord.xy));
    vec3 tangent = normalize(randVec - normalVS * dot(randVec, normalVS));
    vec3 bitangent = cross(normalVS, tangent);
    mat3 TBN = mat3(tangent, bitangent, normalVS);
    float occlusion = 0.f;
    for (int i = 0; i < KERNEL_SIZE; i++) {
        vec3 sampleVec = TBN*randomVec(ivec2(gl_FragCoord.x+(i*2), gl_FragCoord.y+i));
        sampleVec = posVS + sampleVec * AO_RADIUS;
        vec4 offset = globalUbo.proj * vec4(sampleVec, 1.0);
        offset.xyz /= offset.w;
        vec2 sampleUV = (offset.xy*0.5)+0.5;
        if (!(sampleUV.x < 0 || sampleUV.x > 1 || sampleUV.y < 0 || sampleUV.y > 1)) {
            vec3 sampleVS = reconstructViewPos(sampleUV, texture(ddaDepth, sampleUV).r);
            float rangeCheck = smoothstep(0.f, 1.f, (AO_RADIUS/AO_STRENGTH)/length(sampleVS-posVS));
            vec3 viewDirVS = normalize(posVS);
            bool occluded = sampleVS.z <= sampleVec.z-0.05f;
            occlusion += (occluded ? AO_STRENGTH : 0.f) * rangeCheck;
        }
    }
    occlusion = 1.0 - (occlusion / KERNEL_SIZE);
    return clamp(pow(occlusion, AO_STRENGTH*1.5f), 0.2f, 1);
}
void main() {
    vec4 color = texture(ddaColors, uv);
    float depth = texture(ddaDepth, uv).r;
    vec4 normal = texture(ddaNormals, uv);
//    vec3 randVec = hash()/1.732f;
//    if (length(randVec) > 1.f) {outColor.rgb = vec3(1, 0, 0);} else {outColor.rgb = vec3(0, 1, 0);}
//    outColor.a = 1;
    //outColor = vec4(vec3(getAO(depth, normal.xyz)), 1);
    outColor = vec4(color.rgb, mix(getAO(depth, normal.xyz), 1, normal.a)); //normal.a is fogginess
}