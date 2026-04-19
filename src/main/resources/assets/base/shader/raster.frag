layout(set = 0, binding = 0) readonly uniform GlobalUBO {
    mat4 view;
    mat4 proj;
    vec4 skylight;
    vec3 sun;
    int hdr;
    double time;
} globalUbo;
layout(push_constant) uniform PushUBO {
    mat4 model;
    vec4 color;
    int instanced;
    ivec2 atlasOffset;
    ivec2 size;
    int layer;
    int tex;
} pushUbo;
layout(set = 0, binding = 14) uniform sampler2D entities;
layout(location = 0) in vec3 localPos;
layout(location = 1) in vec3 pos;
layout(location = 2) in vec4 inColor;

layout(location = 0) out vec4 outColor;
layout(location = 1) out vec4 outNormal;

void main() {
    vec3 normal = normalize(cross(dFdx(pos), dFdy(pos)));
    outNormal = vec4(normal, 0);

    if (pushUbo.layer == -1) {
        outColor = inColor;
    } else {
        vec3 localNorm = normalize(cross(dFdx(localPos), dFdy(localPos)));
        int sideOffset = 0;
        vec2 uv = vec2(0);
        if (abs(localNorm.x) > max(abs(localNorm.y), abs(localNorm.z))) {
            uv = localPos.zy;
            if (localNorm.x > 0) {
                uv.x = 1-abs(uv.x);
                sideOffset = 16;
            } else {
                sideOffset = 32;
            }
        } else if (abs(localNorm.y) > max(abs(localNorm.x), abs(localNorm.z))) {
            uv = localPos.xz;
            if (localNorm.y > 0) {
                uv.x = 1-uv.x;
                sideOffset = 0;
            } else {
                sideOffset = 40;
            }
        } else if (abs(localNorm.z) > max(abs(localNorm.x), abs(localNorm.y))) {
            uv = localPos.xy;
            if (localNorm.z > 0) {
                uv.x = 1-uv.x;
                sideOffset = 8;
            } else {
                sideOffset = 24;
            }
        }
        uv = abs(uv);

        ivec2 coords = ivec2(pushUbo.atlasOffset.x+(uv.x*pushUbo.size.x)+(pushUbo.layer*8), pushUbo.atlasOffset.y+(uv.y*pushUbo.size.y)+sideOffset);
        outColor = texelFetch(entities, coords, 0)*inColor;
    }
}