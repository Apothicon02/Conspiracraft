layout(binding = 0) uniform UniformBufferObject {
    mat4 model;
    mat4 view;
    mat4 proj;
} ubo;
layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;

layout(location = 0) out vec3 pos;
layout(location = 1) out vec3 norm;

void main() {
    vec4 transformedPos = ubo.proj*ubo.view*ubo.model*vec4(position.xy, 0, 1);
    pos = transformedPos.xyz;
    norm = normal;
    gl_Position = transformedPos;
}