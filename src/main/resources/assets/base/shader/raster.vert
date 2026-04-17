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
    mat4 untranslatedModel = pushUbo.model;
    untranslatedModel[3] = vec4(0, 0, 0, 1);
    pos = (untranslatedModel*vec4(position, 1.f)).xyz;
    uv = (pos.xy+1)/2;
    vec4 worldPos = pushUbo.model*vec4(position, 1.f);
    vec4 clipPos = globalUbo.proj*globalUbo.view*worldPos;
    gl_Position = clipPos;
}