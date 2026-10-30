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
layout(location = 0) in vec3 localPos;
layout(location = 1) in vec3 pos;
//layout(location = 2) pervertexEXT in vec3 vPos[];

layout(location = 0) out vec4 outColor;
layout(location = 1) out vec4 outNorm;

void main() {
    vec3 normal = normalize(cross(dFdx(pos), dFdy(pos)));
    vec3 absNorm = abs(normal);
    vec4 localNorm = vec4(absNorm.x > max(absNorm.y, absNorm.z) ? vec3(sign(normal.x), 0, 0) : (absNorm.y > absNorm.z ? vec3(0, sign(normal.y), 0) : vec3(0, 0, sign(normal.z))), 1);
    outNorm = localNorm;
    outColor = pushUbo.color;
    if (pushUbo.tex.x >= 0) {
        vec3 localLocalPos = (localPos+0.5f)-floor(localPos+0.5f);
        int sideOffset = 0;
        vec2 uv = vec2(0);
        if (abs(localNorm.x) > max(abs(localNorm.y), abs(localNorm.z))) {
            uv = localLocalPos.zy;
            if (localNorm.x > 0) {
                uv.x = 1-abs(uv.x);
                sideOffset = pushUbo.size.x*2;
            } else {
                sideOffset = pushUbo.size.x*4;
            }
        } else if (abs(localNorm.y) > max(abs(localNorm.x), abs(localNorm.z))) {
            uv = localLocalPos.xz;
            if (localNorm.y > 0) {
                uv.x = 1-uv.x;
                sideOffset = 0;
            } else {
                sideOffset = pushUbo.size.x*5;
            }
        } else if (abs(localNorm.z) > max(abs(localNorm.x), abs(localNorm.y))) {
            uv = localLocalPos.xy;
            if (localNorm.z > 0) {
                uv.x = 1-uv.x;
                sideOffset = pushUbo.size.x;
            } else {
                sideOffset = pushUbo.size.x*3;
            }
        }
        uv = abs(uv);

        ivec2 coords = ivec2(pushUbo.atlasOffset.x+(uv.x*pushUbo.size.x), pushUbo.atlasOffset.y+(uv.y*pushUbo.size.y)+sideOffset);
        outColor = texelFetch(Sampler2D[nonuniformEXT(pushUbo.tex.x)], coords, 0)*vec4(0.9995f, 0.9995f, 0.9995f, 1.f)*outColor;
        if (outColor.a <= 0) {
            discard;
        }
    }
}