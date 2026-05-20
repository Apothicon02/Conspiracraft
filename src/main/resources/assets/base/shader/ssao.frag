layout(set = 0, binding = 0) readonly uniform GlobalUBO {
    mat4 view;
    mat4 proj;
    ivec4 renderToggles;
    vec4 skylight;
    vec3 sun;
    int hdr;
    float time;
} globalUbo;
layout(set = 0, binding = 9) uniform sampler2D ddaColors;
layout(set = 0, binding = 10) uniform sampler2D ddaDepth;
layout(set = 0, binding = 11) uniform sampler2D ddaNormals;
layout(location = 0) in vec2 uv;

layout(location = 0) out vec4 outColor;

vec3 hash(vec3 vec) {
    vec3 p = vec;
    p = vec3(dot(p, vec3(127.1,311.7, 74.7)), dot(p, vec3(269.5,183.3,246.1)), dot(p, vec3(113.5,271.9,124.6)));
    return fract(sin(p)*43758.5453123);
}
const float PI = 3.141592653589793f;
vec3 randomVec(vec3 vec) {
    vec3 hash = hash(vec);
    float theta = hash.x * 2.0f * PI;
    float phi = acos(2.0f * hash.y - 1.0f);
    float r = pow(hash.z, 0.333333333f);
    float sinTheta = sin(theta);
    float cosTheta = cos(theta);
    float sinPhi = sin(phi);
    float cosPhi = cos(phi);
    return vec3(r * sinPhi * cosTheta, r * sinPhi * sinTheta, r * cosPhi);
}
const float AO_RADIUS = 1.f;
const float AO_STRENGTH = 2.f;
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
    vec3 randVec = randomVec(vec3(uv, 64));
    vec3 tangent = normalize(randVec - normalVS * dot(randVec, normalVS));
    vec3 bitangent = cross(normalVS, tangent);
    mat3 TBN = mat3(tangent, bitangent, normalVS);
    float occlusion = 0.f;
    for (int i = 0; i < KERNEL_SIZE; i++) {
        vec3 sampleVec = TBN*abs(randomVec(vec3(uv, i)));
        sampleVec = posVS + sampleVec * AO_RADIUS;
        vec4 offset = globalUbo.proj * vec4(sampleVec, 1.0);
        offset.xyz /= offset.w;
        vec2 sampleUV = (offset.xy*0.5)+0.5;
        if (!(sampleUV.x < 0 || sampleUV.x > 1 || sampleUV.y < 0 || sampleUV.y > 1)) {
            vec3 sampleVS = reconstructViewPos(sampleUV, texture(ddaDepth, sampleUV).r);
            float rangeCheck = smoothstep(0.f, 1.f, (AO_RADIUS/AO_STRENGTH)/length(sampleVS-posVS));
            vec3 viewDirVS = normalize(posVS);
            bool occluded = sampleVS.z <= sampleVec.z-0.05f;
            occlusion += (occluded ? 1.f : 0.f) * rangeCheck * AO_STRENGTH;
        }
    }
    occlusion = 1.0 - (occlusion / KERNEL_SIZE);
    return clamp(pow(occlusion, AO_STRENGTH*2), 0.2f, 1);
}
void main() {
    vec4 color = texture(ddaColors, uv);
    float depth = texture(ddaDepth, uv).r;
    vec4 normal = texture(ddaNormals, uv);
//    vec3 randVec = hash()/1.732f;
//    if (length(randVec) > 1.f) {outColor.rgb = vec3(1, 0, 0);} else {outColor.rgb = vec3(0, 1, 0);}
//    outColor.a = 1;
    outColor = vec4(vec3(getAO(depth, normal.xyz)), 1);
    //outColor = vec4(color.rgb*mix(getAO(depth, normal.xyz), 1, normal.a), 1); //normal.a is fogginess
}