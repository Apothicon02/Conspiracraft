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
layout(set = 0, binding = 9) uniform sampler2D colors;
layout(set = 0, binding = 10) uniform sampler2D depth;
layout(location = 0) in vec2 uv;

layout(location = 0) out vec4 outColor;

void main() {
    outColor = textureLod(colors, uv, 0);
    gl_FragDepth = textureLod(depth, uv, 0).r;
}