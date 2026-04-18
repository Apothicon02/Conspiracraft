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
} pushUbo;
layout(set = 0, binding = 9) uniform sampler2D colors;
layout(set = 0, binding = 12) uniform sampler3D gui;
layout(location = 0) in vec2 uv;
layout(location = 1) in vec2 localUV;

layout(location = 0) out vec4 outColor;

vec3 fromLinear(vec3 linearRGB){
    bvec3 cutoff = lessThan(linearRGB, vec3(0.0031308));
    vec3 higher = vec3(1.055)*pow(linearRGB, vec3(1.0/2.4)) - vec3(0.055);
    vec3 lower = linearRGB * vec3(12.92);
    return vec3(mix(higher, lower, cutoff));
}

void main() {
    vec4 bgColor = texture(colors, uv);
    if (pushUbo.color.a == -1.f) {
        outColor = bgColor;
    } else {
        vec4 guiColor = texelFetch(gui, ivec3(pushUbo.atlasOffset.x+(localUV.x*pushUbo.size.x), pushUbo.atlasOffset.y+(localUV.y*pushUbo.size.y), pushUbo.layer), 0);
        guiColor.rgb = fromLinear(guiColor.rgb);
        guiColor *= pushUbo.color;
        if (guiColor.a > 0) {
            outColor = vec4(mix(bgColor.rgb, guiColor.rgb, guiColor.a), 1.f);
        } else {
            discard;
        }
    }
}