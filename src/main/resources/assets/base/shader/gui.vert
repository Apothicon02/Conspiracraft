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
} pushUbo;
layout(location = 0) in vec3 position;

layout(location = 0) out vec2 uv;
layout(location = 1) out vec3 pos;
layout(location = 2) out vec4 color;

void main() {
    color = pushUbo.color;
    pos = (pushUbo.model*vec4(position.xy, 0.f, 1.f)).xyz;
    uv = (pos.xy+1)/2;
    gl_Position = vec4(pos, 1.0);
}