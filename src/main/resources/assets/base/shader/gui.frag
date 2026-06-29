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
layout(location = 0) in vec2 uv;
layout(location = 1) in vec2 localUV;

layout(location = 0) out vec4 outColor;

vec3 fromLinear(vec3 linearRGB){
    bvec3 cutoff = lessThan(linearRGB, vec3(0.0031308));
    vec3 higher = vec3(1.055)*pow(linearRGB, vec3(1.0/2.4)) - vec3(0.055);
    vec3 lower = linearRGB * vec3(12.92);
    return vec3(mix(higher, lower, cutoff));
}
const int radius = 5;
const int samples = ((radius*2)+1)*((radius*2)+1);
void main() {
    vec4 bgColor = texture(colors, uv);
    vec4 blurredBgColor = texture(blurred, uv);
    bgColor.rgb*=max(vec3(0.088f, 0.0934f, 0.1f)*2, vec3(pow(min(blurredBgColor.a, bgColor.a), 1.2f)));
    bgColor.rgb = mix(bgColor.rgb, max(bgColor.rgb, blurredBgColor.rgb), clamp(max(blurredBgColor.r, max(blurredBgColor.g, blurredBgColor.b))-1, 0, 1)); //bloom
    blurredBgColor.rgb*=blurredBgColor.a;
    //bgColor.rgb = vec3(blurredBgColor.a);
    if (pushUbo.color.a == -1.f) {
        outColor = bgColor;
    } else {
        ivec2 coords = ivec2(pushUbo.atlasOffset.x+(localUV.x*pushUbo.size.x), pushUbo.atlasOffset.y+(localUV.y*pushUbo.size.y));
        vec4 guiColor = pushUbo.tex == 0 ? texelFetch(gui, ivec3(coords, pushUbo.layer), 0) : texelFetch(items, coords, 0);
        guiColor.rgb = fromLinear(guiColor.rgb);
        guiColor *= pushUbo.color;
        if (guiColor.a > 0) {
            outColor = vec4(mix(blurredBgColor.rgb/max(1, max(blurredBgColor.r, max(blurredBgColor.g, blurredBgColor.b))), guiColor.rgb, guiColor.a), 1.f);
        } else {
            discard;
        }
    }
}