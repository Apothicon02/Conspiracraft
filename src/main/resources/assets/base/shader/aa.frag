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
    ivec4 tex2;
    ivec4 writeTex;
    int atlas;
    int materials;
    int noises;
    int blueNoise;
    int chunks;
    int voxels;
    int lightChunks;
    int lights;
} pushUbo;
layout(set = 0, binding = 2) uniform sampler2D Sampler2D[];
layout(location = 0) in vec2 uv;

layout(location = 0) out vec4 outColor;

mat4 unjitter(mat4 matrix) {
    matrix[2][0] = 0.f;
    matrix[2][1] = 0.f;
    return matrix;
}

vec2 reproject(vec3 worldPos) {
    vec4 projectionVec = unjitter(globalUbo.projPrev) * globalUbo.viewPrev * vec4(worldPos, 1.0f);
    projectionVec.xyz /= projectionVec.w;
    projectionVec.xy = projectionVec.xy * 0.5f + 0.5f;
    return projectionVec.xy;
}

vec4 bicubicSample(sampler2D tex, vec2 uv, vec2 res) {
    vec2 samplePos = uv * res;
    vec2 texPos1 = floor(samplePos - 0.5) + 0.5;
    vec2 f = samplePos - texPos1;

    vec2 w0 = f * (-0.5 + f * (1.0 - 0.5 * f));
    vec2 w1 = 1.0 + f * f * (-2.5 + 1.5 * f);
    vec2 w2 = f * (0.5 + f * (2.0 - 1.5 * f));
    vec2 w3 = f * f * (-0.5 + 0.5 * f);

    vec2 w12 = w1 + w2;
    vec2 offset12 = w2 / w12;

    vec2 texPos0  = (texPos1 - 1.0) / res;
    vec2 texPos3  = (texPos1 + 2.0) / res;
    vec2 texPos12 = (texPos1 + offset12) / res;

    vec4 result = vec4(0.0);
    result += textureLod(tex, vec2(texPos12.x, texPos0.y),  0.0) * w12.x * w0.y;
    result += textureLod(tex, vec2(texPos0.x,  texPos12.y), 0.0) * w0.x  * w12.y;
    result += textureLod(tex, vec2(texPos12.x, texPos12.y), 0.0) * w12.x * w12.y;
    result += textureLod(tex, vec2(texPos3.x,  texPos12.y), 0.0) * w3.x  * w12.y;
    result += textureLod(tex, vec2(texPos12.x, texPos3.y),  0.0) * w12.x * w3.y;
    float wSum = w12.x*w0.y + w0.x*w12.y + w12.x*w12.y + w3.x*w12.y + w12.x*w3.y;
    return max(result / wSum, 0.0);
}

const float Z_NEAR = 0.01f;
void main() {
    float scale = globalUbo.renderToggles.z == 1 ? 0.5f : 1.f;
    vec2 scaledCoords = gl_FragCoord.xy*scale;
    ivec2 scaledCoordsI = ivec2(scaledCoords);
    vec2 scaledUv = uv*scale;
    float baseDepth = texelFetch(Sampler2D[nonuniformEXT(pushUbo.tex.z)], scaledCoordsI, 0).r;
    vec4 baseColor = textureLod(Sampler2D[nonuniformEXT(pushUbo.tex.y)], scaledUv, 0);
    vec4 baseNormal = texelFetch(Sampler2D[nonuniformEXT(pushUbo.tex.w)], scaledCoordsI, 0);
    vec4 color = baseColor;
    vec2 uvNdc = (uv * 2.0) - 1.0;
    vec4 ndc = vec4(uvNdc, baseDepth, 1.0);
    vec4 viewPos = inverse(unjitter(globalUbo.proj)) * ndc;
    viewPos /= viewPos.w;
    vec4 worldPos = inverse(globalUbo.view) * viewPos;
    worldPos /= worldPos.w;
    vec2 reprojectedPos = reproject(worldPos.xyz);
    if (reprojectedPos.x >= 0.f && reprojectedPos.x < scale && reprojectedPos.y >= 0.f && reprojectedPos.y < scale && baseDepth < 0.9f) { // dont do taa on objects too close to the camera. if (baseDepth < 0.9f && (abs(oldDepth-baseDepth)/baseDepth < 0.1f || (baseDepth < 0.00000001f && oldDepth < 0.00000001f))) {
        vec4 oldColor = textureLod(Sampler2D[nonuniformEXT(pushUbo.writeTex.x)], reprojectedPos, 0);
        float oldDepth = texelFetch(Sampler2D[nonuniformEXT(pushUbo.writeTex.y)], ivec2(reprojectedPos*globalUbo.res), 0).r;
        float depthRej = 19*clamp(0.1f*((oldDepth-baseDepth)/baseDepth), 0.f, 0.05f);
        if (depthRej >= 0.95f) {
            outColor = baseColor;
        } else {
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
            oldColor = clamp(oldColor, boxMin, boxMax);
            if (baseDepth < 0.00000000005f) {
                outColor = mix(baseColor, oldColor, 0.95f);//oldColor.r <= 0 && oldColor.g <= 0 && oldColor.b <= 0 && oldColor.a <= 0 ? baseColor : mix(baseColor, oldColor, 0.95f);
            } else {
                float depthDif = 10 * (abs(oldDepth - baseDepth) / baseDepth);
                outColor = mix(baseColor, oldColor * vec4(1 + (max(vec3(0), (baseColor.rgb - oldColor.rgb) - depthDif)), 1), 0.95f - depthRej);
            }
        }
    } else {
        outColor = baseColor;
    }
}