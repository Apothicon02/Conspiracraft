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
layout(location = 0) in vec2 uv;

layout(location = 0) out vec4 outColor;

vec2 reproject(vec3 worldPos) {
    vec4 projectionVec = globalUbo.projPrev * globalUbo.viewPrev * vec4(worldPos, 1.0f);
    projectionVec.xyz /= projectionVec.w;
    projectionVec.xy = projectionVec.xy * 0.5f + 0.5f;
    return projectionVec.xy;
}

const float Z_NEAR = 0.01f;
void main() {
    float scale = globalUbo.renderToggles.z == 1 ? 0.5f : 1.f;
    vec2 scaledCoords = gl_FragCoord.xy*scale;
    vec2 scaledUv = uv*scale;
    float baseDepth = textureLod(Sampler2D[nonuniformEXT(pushUbo.tex.z)], scaledUv, 0).r;
    vec4 baseColor = textureLod(Sampler2D[nonuniformEXT(pushUbo.tex.y)], scaledUv, 0);
    vec4 baseNormal = textureLod(Sampler2D[nonuniformEXT(pushUbo.tex.w)], scaledUv, 0);
    vec4 color = baseColor;
    vec2 uvNdc = (uv * 2.0) - 1.0;
    vec4 ndc = vec4(uvNdc, baseDepth, 1.0);
    vec4 viewPos = inverse(globalUbo.proj) * ndc;
    viewPos /= viewPos.w;
    vec4 worldPos = inverse(globalUbo.view) * viewPos;
    worldPos /= worldPos.w;
    vec2 reprojectedPos = reproject(worldPos.xyz);
    if (!(reprojectedPos.x >= 0.f && reprojectedPos.x < scale && reprojectedPos.y >= 0.f && reprojectedPos.y < scale)) { reprojectedPos = uv; }
    float oldDepth = textureLod(Sampler2D[nonuniformEXT(pushUbo.writeTex.y)], reprojectedPos, 0).r;
    if (abs(oldDepth-baseDepth)/baseDepth < 0.1f || (baseDepth < 0.00000001f && oldDepth < 0.00000001f)) {
        float velocity = distance((reprojectedPos*globalUbo.res), gl_FragCoord.xy);
        int radius = velocity < 0.6f ? 2 : 1;
        vec4 boxMin = vec4(1000);
        vec4 boxMax = vec4(-1000);
        for (int x = int(scaledCoords.x-radius); x <= scaledCoords.x+radius; x++) {
            for (int y = int(scaledCoords.y-radius); y <= scaledCoords.y+radius; y++) {
                vec4 nearColor = texelFetch(Sampler2D[nonuniformEXT(pushUbo.tex.y)], ivec2(x, y), 0);
                boxMin = min(boxMin, nearColor);
                boxMax = max(boxMax, nearColor);
            }
        }
        vec4 oldColor = textureLod(Sampler2D[nonuniformEXT(pushUbo.writeTex.x)], reprojectedPos, 0);
        oldColor = clamp(max(baseColor*vec4(0.95f, 0.95f, 0.95f, 0.f), oldColor), boxMin, boxMax);
        vec3 comparedColors = baseColor.rgb-oldColor.rgb;
        outColor = vec4(mix(baseColor, oldColor, 0.95f));
    } else {
        outColor = baseColor;
    }
}