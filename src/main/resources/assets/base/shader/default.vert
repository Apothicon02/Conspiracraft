layout(push_constant) uniform Push {
    mat4 model;
    vec4 color;
    int instanced;
} push;
layout(set = 0, binding = 0) readonly uniform UniformBufferObject {
    mat4 view;
    mat4 proj;
    vec4 skylight;
} ubo;
struct InstanceData {mat4 model; vec4 color;};
layout(std430, set = 0, binding = 1) readonly buffer InstanceBuffer {
    InstanceData data[];
} instances;

layout(location = 0) in vec3 position;

layout(location = 0) out vec3 pos;
layout(location = 1) out vec4 color;

void main() {
    pos = position;
    mat4 model;
    if (push.instanced == 0) {
        model = push.model;
        color = push.color;
    } else {
        InstanceData data = instances.data[gl_InstanceIndex];
        model = data.model;
        color = data.color;
    }
    vec4 worldPos = model*vec4(position, 1);
    vec4 clipPos = ubo.proj*ubo.view*worldPos;
    gl_Position = clipPos;
}