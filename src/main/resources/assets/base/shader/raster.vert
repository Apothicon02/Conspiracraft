layout(set = 0, binding = 0) readonly uniform GlobalUBO {
    mat4 view;
    mat4 proj;
    ivec4 renderToggles;
    vec4 skylight;
    vec3 sun;
    int hdr;
    float time;
} globalUbo;
layout(push_constant) uniform PushUBO {
    mat4 model;
    vec4 color;
    int instanced;
    ivec2 atlasOffset;
    ivec2 size;
    int layer;
    int tex;
} pushUbo;
layout(location = 0) in vec3 position;

layout(location = 0) out vec3 localPos;
layout(location = 1) out vec3 pos;
layout(location = 2) out vec4 color;

void main() {
    localPos = -(position-0.5f);
    color = pushUbo.color;
    mat4 untranslatedModel = pushUbo.model;
    untranslatedModel[3] = vec4(0, 0, 0, 1);
    pos = (untranslatedModel*vec4(position, 1.f)).xyz;
    vec4 worldPos = pushUbo.model*vec4(position, 1.f);
    vec4 clipPos = globalUbo.proj*globalUbo.view*worldPos;
    gl_Position = clipPos;
}