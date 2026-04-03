layout(set = 0, binding = 0) readonly uniform UniformBufferObject {
    mat4 view;
    mat4 proj;
    vec3 sun;
} ubo;

layout(location = 0) in vec3 pos;
layout(location = 1) in vec3 norm;
layout(location = 2) in vec2 uv;
layout(location = 3) in vec4 inColor;

layout(location = 0) out vec4 outColor;

void main() {
    if (inColor.r > 1 || inColor.g > 1 || inColor.b > 1) {
        outColor = inColor;
    } else {
        outColor = inColor*((dot(norm, normalize(ubo.sun))*0.225f)+0.45f);
    }
}