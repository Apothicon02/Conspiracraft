vec2 positions[3] = vec2[](
    vec2(0.0, -3),
    vec2(3, 3),
    vec2(-3, 3)
);
vec4 colors[3] = vec4[](
    vec4(1.0, 0.0, 0.0, 1.0),
    vec4(0.0, 1.0, 0.0, 1.0),
    vec4(0.0, 0.0, 1.0, 1.0)
);

layout(location = 0) out vec4 vertColor;

void main() {
    gl_Position = vec4(positions[gl_VertexIndex], 0.0, 1.0);
    vertColor = colors[gl_VertexIndex];
}