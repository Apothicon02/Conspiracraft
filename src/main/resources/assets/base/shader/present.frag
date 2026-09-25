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
    int lightChunks;
    int lights;
} pushUbo;
layout(set = 0, binding = 2) uniform sampler2D Sampler2D[];
layout(location = 0) in vec2 uv;

layout(location = 0) out vec4 outColor;

void main() {
    vec4 color = textureLod(Sampler2D[nonuniformEXT(pushUbo.tex.x)], uv.xy, 0);
//    vec2 sampCoords = gl_FragCoord.xy;
//    sampCoords.x+=cos((sampCoords.y+50000)/500.f)*500;
//    sampCoords.y+=cos((sampCoords.x+25000)/200.f)*200;
//    color.rg = abs((cos(sampCoords.xy/100.f)+cos((sampCoords.yx+10000)/100.f))*0.5f);
//    color.rg = min(color.rg, 0.1f)*10;
//    float noiseValue = max(color.r, color.g);
//    color.rgb = vec3(max(color.r, color.g));//vec3(0.5f*(color.r+color.g));
//    sampCoords = gl_FragCoord.yx;
//    sampCoords.x+=cos((sampCoords.y+50000)/500.f)*500;
//    sampCoords.y+=cos((sampCoords.x+25000)/200.f)*200;
//    color.rg = abs((cos(sampCoords.xy/100.f)+cos((sampCoords.yx+10000)/100.f))*0.5f);
//    color.rg = min(color.rg, 0.1f)*10;
//    color.rgb = vec3(min(noiseValue, max(color.r, color.g)));
    color.rgb = pow(color.rgb, vec3(2.2)); //gamma
    if (color.r > 1 || color.g > 1 || color.b > 1) { color.rgb /= max(color.r, max(color.g, color.b)); }
    if (globalUbo.hdr == 1) {
        color.rgb = (color.rgb*400)/80;//exposure
    }
    outColor = vec4(color.rgb, 1);
}