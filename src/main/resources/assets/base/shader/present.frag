layout(set = 0, binding = 0) readonly uniform GlobalUBO {
    mat4 view;
    mat4 proj;
    ivec4 renderToggles;
    vec4 skylight;
    vec3 sun;
    int hdr;
    float time;
    ivec2 res;
} globalUbo;
layout(set = 0, binding = 9) uniform sampler2D colors;
layout(location = 0) in vec2 uv;

layout(location = 0) out vec4 outColor;

void main() {
    vec4 color = texture(colors, uv.xy);
    color.rgb = pow(color.rgb, vec3(2.2)); //gamma
    if (color.r > 1 || color.g > 1 || color.b > 1) { color.rgb /= max(color.r, max(color.g, color.b)); }
    if (globalUbo.hdr == 1) {
        color.rgb = (color.rgb*400)/80;//exposure
    }
    outColor = vec4(color.rgb, 1);
}