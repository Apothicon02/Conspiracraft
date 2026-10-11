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
    vec4 atmosphere;
    vec4 nightAtmosphere;
    vec4 sunsetAtmosphere;
    vec4 deepSunsetAtmosphere;
    float fogginess;
    vec4 skylightMul;
    int season;
} globalUbo;
layout(push_constant) uniform PushUBO {
    mat4 model;
    vec4 color;
    int instanced;
    ivec2 atlasOffset;
    ivec2 size;
    int layer;
    ivec4 tex;
    ivec4 tex2;
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

void main() {
    ivec2 coords = ivec2(uv*globalUbo.res);
    outColor = texelFetch(Sampler2D[nonuniformEXT(pushUbo.tex.y)], coords, 0);
    gl_FragDepth = texelFetch(Sampler2D[nonuniformEXT(pushUbo.tex.z)], coords, 0).r;
}