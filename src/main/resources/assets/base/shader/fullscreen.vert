layout(set = 0, binding = 0) readonly uniform GlobalUBO {
    mat4 view;
    mat4 proj;
    vec4 skylight;
    vec3 sun;
    int hdr;
    double time;
} globalUbo;
vec2 positions[3] = vec2[](
    vec2(0.0, -3),
    vec2(-3, 3),
    vec2(3, 3)
);

layout(location = 0) out vec2 uv;

void main() {
    vec2 pos = positions[gl_VertexIndex];
    gl_Position = vec4(pos, 0, 1.0);
    uv = (pos+1)/2;
}