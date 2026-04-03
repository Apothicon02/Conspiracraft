layout(location = 0) in vec3 pos;
layout(location = 1) in vec3 norm;

layout(location = 0) out vec4 outColor;

const vec3 sun = vec3(2000, 2560, 512);

void main() {
    outColor = vec4(1)*((dot(norm, normalize(sun))*0.225f)+0.45f);
}