layout(push_constant) uniform Push {
    mat4 model;
    vec4 color;
} push;
layout(binding = 0) uniform UniformBufferObject {
    mat4 view;
    mat4 proj;
} ubo;
layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;

layout(location = 0) out vec3 pos;
layout(location = 1) out vec3 norm;
layout(location = 2) out vec4 color;

void main() {
    pos = position;
    norm = normal;
    color = push.color;
    vec4 worldPos = push.model*vec4(position, 1);
    vec4 clipPos = ubo.proj*ubo.view*worldPos;
    gl_Position = clipPos;
}