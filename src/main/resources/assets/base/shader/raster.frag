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
    int lods;
    int lightChunks;
    int lights;
} pushUbo;
layout(set = 0, binding = 2) uniform sampler2D Sampler2D[];
layout(location = 0) in vec3 localPos;
layout(location = 1) in vec3 pos;

layout(location = 0) out vec4 outColor;
layout(location = 1) out vec4 outNormal;

void main() {
    vec3 normal = normalize(cross(dFdx(pos), dFdy(pos)));
    outNormal = vec4(normal, 0);
    outColor = pushUbo.color;
    if (pushUbo.tex.x >= 0) {
        vec2 uv = localPos.xy+0.5f;
        ivec2 coords = ivec2(pushUbo.atlasOffset.x+(uv.x*pushUbo.size.x), pushUbo.atlasOffset.y+(uv.y*pushUbo.size.y));
        outColor = texelFetch(Sampler2D[nonuniformEXT(pushUbo.tex.x)], coords, 0)*outColor;
        if (outColor.a <= 0) {
            discard;
        }
    }
}