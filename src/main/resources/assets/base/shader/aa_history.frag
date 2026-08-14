layout(set = 0, binding = 9) uniform sampler2D colors;
layout(set = 0, binding = 10) uniform sampler2D depth;
layout(location = 0) in vec2 uv;

layout(location = 0) out vec4 outColor;

void main() {
    outColor = textureLod(colors, uv, 0);
    gl_FragDepth = textureLod(depth, uv, 0).r;
}