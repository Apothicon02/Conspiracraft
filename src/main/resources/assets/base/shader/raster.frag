#extension GL_EXT_nonuniform_qualifier : require
//#extension GL_EXT_fragment_shader_barycentric : enable
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
layout(location = 0) in vec3 localPos;
layout(location = 1) in vec3 pos;
layout(location = 2) in vec3 scale;
//layout(location = 2) pervertexEXT in vec3 vPos[];

layout(location = 0) out vec4 outColor;
layout(location = 1) out vec4 outNormal;

void main() {
    outNormal = vec4(normalize(cross(dFdx(pos), dFdy(pos))), 0);//vec4(normalize(cross(vPos[1] - vPos[0], vPos[2] - vPos[0])), 0);
    outColor = pushUbo.color;
    if (pushUbo.tex.x >= 0) {
        ivec2 absSize = abs(pushUbo.size);
        vec2 uv;
        if (pushUbo.size.x > -1) {
            uv = localPos.xy+0.5f;
        } else {
            vec4 absNorm = abs(vec4(normalize(cross(dFdx(localPos), dFdy(localPos))), 0));
            if (absNorm.x > absNorm.y && absNorm.x > absNorm.z) {
                uv = localPos.yz*(scale.yz*absSize);
            } else if (absNorm.y > absNorm.z) {
                uv = localPos.xz*(scale.xz*absSize);
            } else {
                uv = localPos.xy*(scale.xy*absSize);
            }
            uv = fract(uv);
        }
        ivec2 coords = ivec2(pushUbo.atlasOffset.x+(uv.x*absSize.x), pushUbo.atlasOffset.y+(uv.y*absSize.y));
        outColor = texelFetch(Sampler2D[nonuniformEXT(pushUbo.tex.x)], coords, 0)*outColor;
        if (outColor.a <= 0) {
            discard;
        }
    }
}