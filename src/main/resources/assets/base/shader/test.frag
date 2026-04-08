layout(set = 0, binding = 0) readonly uniform UniformBufferObject {
    mat4 view;
    mat4 proj;
    vec4 skylight;
    int hdr;
} ubo;
layout(location = 0) in vec2 uv;

layout(location = 0) out vec4 outColor;

void main() {
    vec3 color = vec3(0.f);
    color.rb = uv;
    color = pow(color, vec3(2.2)); //gamma
    if (ubo.hdr == 1) {
        color = (color*400)/80;//exposure
    }
    outColor = vec4(color, 1);
}