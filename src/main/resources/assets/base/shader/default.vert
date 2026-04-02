layout (location = 0) in vec3 position;
layout (location = 1) in vec3 normal;

layout(location = 0) out vec3 pos;
layout(location = 1) out vec3 norm;

void main() {
    pos = position-vec3(0.5);
    norm = normal;
    gl_Position = vec4(pos, 1.0f);
}