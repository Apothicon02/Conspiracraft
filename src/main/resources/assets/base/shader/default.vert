layout(set = 0, binding = 0) readonly uniform GlobalUBO {
    mat4 view;
    mat4 proj;
    vec4 skylight;
    int hdr;
} globalUbo;
vec3 positions[3] = vec3[](
    vec3(0, -1, 0),
    vec3(1, 1, 0),
    vec3(-1, 1, 0)
);
const mat4 model = mat4(
    1, 0, 0, 0,
    0, 1, 0, 0,
    0, 0, 1, 0,
    0, 0, 0, 1
);

layout(location = 0) out vec2 uv;

void main() {
    vec3 pos = positions[gl_VertexIndex];
    uv = (pos.xy+1)/2;
    vec4 worldPos = model*vec4(pos, 1.0);
    vec4 clipPos = globalUbo.proj*globalUbo.view*worldPos;
    gl_Position = clipPos;
}