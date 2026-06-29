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
layout(set = 0, binding = 10) uniform sampler2D depth;
layout(set = 0, binding = 11) uniform sampler2D normals;
layout(set = 0, binding = 12) uniform sampler2D colors;
layout(set = 0, binding = 22) uniform sampler2D colorsOld;
layout(set = 0, binding = 23) uniform sampler2D depthOld;
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
    float baseDepth = texture(depth, uv).r;
    vec4 baseColor = texture(colors, uv);
    vec4 baseNormal = texture(normals, uv);
    vec4 color = baseColor;
    vec2 uvNdc = (uv * 2.0) - 1.0;
    vec4 ndc = vec4(uvNdc, baseDepth, 1.0);
    vec4 viewPos = inverse(globalUbo.proj) * ndc;
    viewPos /= viewPos.w;
    vec4 worldPos = inverse(globalUbo.view) * viewPos;
    worldPos /= worldPos.w;
    vec2 reprojectedPos = reproject(worldPos.xyz);
    if (reprojectedPos.x >= 0.f && reprojectedPos.x < 1.f && reprojectedPos.y >= 0.f && reprojectedPos.y < 1.f) {
        float oldDepth = texture(depthOld, reprojectedPos).r;
        if (abs(oldDepth-baseDepth)/baseDepth < 0.1f) {
            float velocity = distance((reprojectedPos*globalUbo.res), gl_FragCoord.xy);
            int radius = velocity < 0.6f ? 2 : 1;
            vec4 boxMin = vec4(1);
            vec4 boxMax = vec4(0);
            for (int x = int(gl_FragCoord.x-radius); x < gl_FragCoord.x+radius; x++) {
                for (int y = int(gl_FragCoord.y-radius); y < gl_FragCoord.y+radius; y++) {
                    vec4 nearColor = texelFetch(colors, ivec2(x, y), 0);
                    boxMin = min(boxMin, nearColor);
                    boxMax = max(boxMax, nearColor);
                }
            }
            vec4 oldColor = texture(colorsOld, reprojectedPos);
            oldColor = clamp(max(baseColor*vec4(0.95f, 0.95f, 0.95f, 0.f), oldColor), boxMin, boxMax);
            vec3 comparedColors = baseColor.rgb-oldColor.rgb;
            outColor = vec4(mix(baseColor, oldColor, 0.95f));
        } else {
            outColor = baseColor;
        }
    } else {
        outColor = baseColor;
    }
}