layout(set = 0, binding = 0) readonly uniform GlobalUBO {
    mat4 view;
    mat4 proj;
    vec4 skylight;
    vec3 sun;
    int hdr;
    double time;
} globalUbo;
layout(set = 0, binding = 6) uniform sampler2D ddaResult;
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
vec3 reconstructViewPos(vec2 uvPos, float depth) {
    vec4 clip = vec4(uvPos*2.0f-1.0f, depth, 1.0f);
    vec4 view = inverse(globalUbo.proj)*clip;
    return view.xyz/view.w;
}
const int width = 1600;
const int height = 890;
void main() {
    vec2 uvPos = vec2(uv);
    vec4 color = texture(ddaResult, uvPos);
    vec3 viewPos = reconstructViewPos(uvPos, color.a);//color.a is depth
    //color.rgb = (viewPos*0.002f)+0.5f;
//    float depthC = texture(ddaResult, uvPos).a;
//    float depthR = texture(ddaResult, uvPos + vec2(1.0/width, 0)).a;
//    float depthU = texture(ddaResult, uvPos + vec2(0, 1.0/height)).a;
//
//    vec3 p  = reconstructViewPos(uvPos, depthC);
//    vec3 px = reconstructViewPos(uvPos + vec2(1.0/width, 0), depthR);
//    vec3 py = reconstructViewPos(uvPos + vec2(0, 1.0/height), depthU);
//
//    vec3 normal = normalize(cross(px - p, py - p));
//    vec3 normal = imageLoad(normalTex, gl_FragCoord.xy).xyz;
//    color.rgb = (normal+1)/2;

    vec3 randVec = randomVec();

    color.rgb = pow(color.rgb, vec3(2.2)); //gamma
    if (globalUbo.hdr == 1) {
        color.rgb = (color.rgb*400)/80;//exposure
    }
    outColor = vec4(color.rgb, 1);
}