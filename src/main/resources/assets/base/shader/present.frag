layout(set = 0, binding = 0) readonly uniform GlobalUBO {
    mat4 view;
    mat4 proj;
    vec4 skylight;
    vec3 sun;
    int hdr;
    double time;
} globalUbo;
layout(set = 0, binding = 6) uniform sampler2D ddaResult;
layout(location = 0) in vec2 uv;

layout(location = 0) out vec4 outColor;

void main() {
    vec3 color = texture(ddaResult, vec2(uv)).rgb;
    color = pow(color, vec3(2.2)); //gamma
    if (globalUbo.hdr == 1) {
        color = (color*400)/80;//exposure
    }
    outColor = vec4(color, 1);
}