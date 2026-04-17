layout(set = 0, binding = 0) readonly uniform GlobalUBO {
    mat4 view;
    mat4 proj;
    vec4 skylight;
    vec3 sun;
    int hdr;
    double time;
} globalUbo;
layout(set = 0, binding = 9) uniform sampler2D colors;
layout(location = 0) in vec2 uv;
layout(location = 1) in vec3 pos;
layout(location = 2) in vec4 inColor;

layout(location = 0) out vec4 outColor;

void main() {
    vec4 bgColor = texture(colors, uv);
    outColor = mix(bgColor, inColor, inColor.a);
}