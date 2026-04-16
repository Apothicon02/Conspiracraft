layout(set = 0, binding = 0) readonly uniform GlobalUBO {
    mat4 view;
    mat4 proj;
    vec4 skylight;
    vec3 sun;
    int hdr;
    double time;
} globalUbo;
layout(set = 0, binding = 6) uniform sampler2D ddaColors;
layout(set = 0, binding = 7) uniform sampler2D ddaDepth;
layout(set = 0, binding = 8) uniform sampler2D ddaNormals;
layout(location = 0) in vec2 uv;

layout(location = 0) out vec4 outColor;

const vec3 SSAO_NOISE[16] = vec3[](
    vec3( 0.7071,  0.7071, 0.0), vec3(-0.3827,  0.9239, 0.0), vec3( 0.9808, -0.1951, 0.0), vec3(-0.8315, -0.5556, 0.0),
    vec3( 0.1951, -0.9808, 0.0), vec3( 0.9239,  0.3827, 0.0), vec3(-0.5556,  0.8315, 0.0), vec3(-0.9808,  0.1951, 0.0),
    vec3( 0.3827, -0.9239, 0.0), vec3( 0.8315,  0.5556, 0.0), vec3(-0.7071, -0.7071, 0.0), vec3(-0.1951,  0.9808, 0.0),
    vec3( 0.5556, -0.8315, 0.0), vec3( 0.9808,  0.1951, 0.0), vec3(-0.3827, -0.9239, 0.0), vec3(-0.9239, -0.3827, 0.0));
vec3 randomVec() {
    int x = int(mod(gl_FragCoord.x, 4.0));
    int y = int(mod(gl_FragCoord.y, 4.0));
    return SSAO_NOISE[x * 4 + y];
}
const float AO_RADIUS = 1.f;
const float AO_STRENGTH = 1.5f;
const int KERNEL_SIZE = 32; //reduce to something like 8 when taa is enabled.
vec3 SSAO_KERNEL[32] = vec3[](vec3( 0.021,  0.183,  0.512), vec3( 0.392,  0.041,  0.734), vec3(-0.221,  0.114,  0.612), vec3( 0.134, -0.287,  0.553), vec3(-0.341, -0.102,  0.487), vec3( 0.287,  0.331,  0.682), vec3(-0.129,  0.412,  0.731), vec3( 0.512, -0.221,  0.612), vec3(-0.412, -0.331,  0.682), vec3( 0.221,  0.129,  0.341), vec3(-0.183, -0.021,  0.512), vec3( 0.331, -0.412,  0.731), vec3(-0.041,  0.392,  0.734), vec3( 0.102, -0.341,  0.487), vec3(-0.287,  0.134,  0.553), vec3( 0.412,  0.287,  0.612), vec3(-0.512, -0.183,  0.512), vec3( 0.341, -0.102,  0.487), vec3(-0.129,  0.221,  0.341), vec3( 0.183,  0.412,  0.731), vec3(-0.392,  0.041,  0.734), vec3( 0.102, -0.512,  0.612), vec3(-0.221, -0.392,  0.682), vec3( 0.331,  0.129,  0.341), vec3(-0.041, -0.183,  0.512), vec3( 0.287, -0.412,  0.731), vec3(-0.134,  0.341,  0.487), vec3( 0.412, -0.287,  0.612), vec3(-0.512,  0.183,  0.512), vec3( 0.341,  0.102,  0.487), vec3(-0.129, -0.221,  0.341), vec3( 0.183, -0.412,  0.731));
const float Z_NEAR = 0.01f;
//const float ASPECT = 1.7977529f;
//const float FOCAL_LENGTH = 1.3514224f;
vec3 reconstructViewPos(vec2 uvPos, float depth) {
    vec4 clip = vec4((uvPos*2)-1, depth, 1.f);
    vec4 view = inverse(globalUbo.proj)*clip;
    return view.xyz/view.w;
}

float getAO(float depth, vec3 normal) {
    vec3 posVS = reconstructViewPos(uv.xy, depth);
    vec3 normalVS = normalize((globalUbo.view * vec4(normal.rgb, 0.f)).xyz);
    vec3 randVec = randomVec();
    vec3 tangent = normalize(randVec - normalVS * dot(randVec, normalVS));
    vec3 bitangent = cross(normalVS, tangent);
    mat3 TBN = mat3(tangent, bitangent, normalVS);
    float occlusion = 0.f;
    for (int i = 0; i < KERNEL_SIZE; i++) {
        vec3 sampleVec = TBN * SSAO_KERNEL[i];
        sampleVec = posVS + sampleVec * AO_RADIUS;
        vec4 offset = globalUbo.proj * vec4(sampleVec, 1.0);
        offset.xyz /= offset.w;
        vec2 sampleUV = (offset.xy*0.5)+0.5;
        if (!(sampleUV.x < 0 || sampleUV.x > 1 || sampleUV.y < 0 || sampleUV.y > 1)) {
            vec3 sampleVS = reconstructViewPos(sampleUV, texture(ddaDepth, sampleUV).r);
            float rangeCheck = smoothstep(0.f, 1.f, (AO_RADIUS/AO_STRENGTH)/length(sampleVS-posVS));
            vec3 viewDirVS = normalize(posVS);
            bool occluded = dot(sampleVS - posVS, viewDirVS) < dot(sampleVec - posVS, viewDirVS);
            occlusion += (occluded ? 1.f : 0.f) * rangeCheck * AO_STRENGTH;
        }
    }
    occlusion = 1.0 - (occlusion / KERNEL_SIZE);
    return pow(occlusion, AO_STRENGTH);
}
void main() {
    vec4 color = texture(ddaColors, uv.xy);
    float depth = texture(ddaDepth, uv.xy).r;
    vec4 normal = texture(ddaNormals, uv.xy);
    color.rgb = vec3(getAO(depth, normal.xyz));//color.rgb*=mix(getAO(depth, normal.xyz), 1, normal.a); //normal.a is fogginess
    color.rgb = pow(color.rgb, vec3(2.2)); //gamma
    if (globalUbo.hdr == 1) {
        color.rgb = (color.rgb*400)/80;//exposure
    }
    outColor = vec4(color.rgb, 1);
}