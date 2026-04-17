layout(set = 0, binding = 0) readonly uniform GlobalUBO {
    mat4 view;
    mat4 proj;
    vec4 skylight;
    vec3 sun;
    int hdr;
    double time;
} globalUbo;
layout(set = 0, binding = 6) uniform sampler2D colors;
layout(location = 0) in vec2 uv;

layout(location = 0) out vec4 outColor;

void main() {
    vec4 color = texture(colors, uv.xy);
    color.rgb = pow(color.rgb, vec3(2.2)); //gamma
    if (globalUbo.hdr == 1) {
        color.rgb = (color.rgb*400)/80;//exposure
    }
    outColor = vec4(color.rgb, 1);
}