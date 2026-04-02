layout(location = 0) in vec3 pos;
layout(location = 1) in vec3 norm;

layout(location = 0) out vec4 outColor;

const vec3 sun = vec3(2553, 166, 512);

void main() {
    outColor = vec4(norm, 1)*((dot(-norm, sun)*-0.0001f)+0.6f);
}