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
    int tex;
} pushUbo;
layout(set = 0, binding = 12) uniform sampler2D colors;
layout(set = 0, binding = 15) uniform sampler3D gui;
layout(set = 0, binding = 16) uniform sampler2D items;
layout(set = 0, binding = 19) uniform sampler2D blurred;
layout(set = 0, binding = 26) uniform sampler2D bloom;
//layout(set = 0, binding = 24) uniform sampler2D materials;layout(set = 0, binding = 7) uniform sampler3D atlas;
layout(location = 0) in vec2 uv;
layout(location = 1) in vec2 localUV;

layout(location = 0) out vec4 outColor;

const int radius = 5;
const int samples = ((radius*2)+1)*((radius*2)+1);
void main() {
    vec4 bgColor = textureLod(colors, uv, 0);
    bgColor.rgb *= max(vec3(0.088f, 0.0934f, 0.1f) * 2, vec3(pow(bgColor.a, 1.2f)));
    vec3 ogColor = bgColor.rgb;
//    bool xOdd = bool(int(gl_FragCoord.x) % 2 == 1);
//    bool yOdd = bool(int(gl_FragCoord.y) % 2 == 1);
//    bool isChecker = (xOdd && yOdd) || (!xOdd && !yOdd);
//    if (isChecker) {
//        vec3 bloomColor = textureLod(bloom, uv / 2, 0).rgb;
//        bgColor.rgb += bloomColor;
//    }
    vec3 bloomColor = textureLod(bloom, uv / 2, 0).rgb;
    if (bloomColor.r > 1 || bloomColor.g > 1 || bloomColor.b > 1) { bloomColor.rgb /= max(bloomColor.r, max(bloomColor.g, bloomColor.b)); }
    bgColor.rgb += bloomColor*0.5f;
    vec4 blurredBgColor = textureLod(blurred, uv/2, 0);
    blurredBgColor.rgb*=max(vec3(0.088f, 0.0934f, 0.1f)*2, vec3(pow(blurredBgColor.a, 1.2f)));
    if (pushUbo.color.a == -1.f) {
        outColor = bgColor;
    } else {
        ivec2 coords = ivec2(pushUbo.atlasOffset.x+(localUV.x*pushUbo.size.x), pushUbo.atlasOffset.y+(localUV.y*pushUbo.size.y));
        vec4 guiColor = pushUbo.tex < 0 ? vec4(1) : (pushUbo.tex == 0 ? texelFetch(gui, ivec3(coords, pushUbo.layer), 0) : texelFetch(items, coords, 0));
        guiColor *= pushUbo.color;
        if (guiColor.a > 0) {
            outColor = vec4(mix(blurredBgColor.rgb/max(1, max(blurredBgColor.r, max(blurredBgColor.g, blurredBgColor.b))), guiColor.rgb, guiColor.a), 1.f);
        } else {
            discard;
        }
    }
}