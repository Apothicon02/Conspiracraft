layout(set = 0, binding = 0) readonly uniform UniformBufferObject {
    mat4 view;
    mat4 proj;
    vec4 skylight;
} ubo;

layout(location = 0) in vec3 pos;
layout(location = 1) in vec4 inColor;

layout(location = 0) out vec4 outColor;

void main() {
    if (inColor.r > 1 || inColor.g > 1 || inColor.b > 1) {
        outColor = inColor;
    } else {
        vec3 normal = -normalize(cross(dFdx(pos), dFdy(pos)));
        vec4 skylight = ubo.skylight;
        outColor = inColor*((dot(normal, normalize(skylight.xyz))*0.225f)+(0.45f*skylight.a));
    }
}