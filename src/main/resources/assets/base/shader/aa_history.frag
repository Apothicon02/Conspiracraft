#extension GL_EXT_nonuniform_qualifier : require
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
    int lods;
    int lightChunks;
    int lights;
} pushUbo;
layout(set = 0, binding = 2) uniform sampler2D Sampler2D[];
layout(location = 0) in vec2 uv;

layout(location = 0) out vec4 outColor;

void main() {
    outColor = textureLod(Sampler2D[nonuniformEXT(pushUbo.tex.y)], uv, 0);
    gl_FragDepth = textureLod(Sampler2D[nonuniformEXT(pushUbo.tex.z)], uv, 0).r;
}