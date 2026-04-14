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

void main() {
    vec4 color = texture(ddaResult, vec2(uv));
//    vec3 randVec = randomVec();
//    color.rgb = vec3(color.a*1000);
    color.rgb = pow(color.rgb, vec3(2.2)); //gamma
    if (globalUbo.hdr == 1) {
        color.rgb = (color.rgb*400)/80;//exposure
    }
    outColor = vec4(color.rgb, 1);
}